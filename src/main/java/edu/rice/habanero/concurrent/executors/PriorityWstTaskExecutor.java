package edu.rice.habanero.concurrent.executors;

import edu.rice.habanero.concurrent.util.ModCountDownLatch;
import edu.rice.habanero.concurrent.wstdeque.PoolType;
import edu.rice.habanero.concurrent.wstdeque.WorkStealingPool;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="http://shams.web.rice.edu/">Shams Imam</a> (shams@rice.edu)
 * @author Vivek Sarkar
 */
public class PriorityWstTaskExecutor implements TaskExecutor {

    private final int parallelism;
    private final int minPriorityInc;
    private final int maxPriorityInc;
    private final ModCountDownLatch countDownLatch;

    private final GlobalPriorityState globalPriorityState;
    private final WorkerThread[] workerThreads;
    private boolean allThreadsStarted = false;

    private final AtomicInteger startedThreadsCounter = new AtomicInteger(-1);

    public PriorityWstTaskExecutor(
            final PoolType poolType, final int parallelism,
            final int minPriorityInc, final int maxPriorityInc) {

        if (maxPriorityInc < minPriorityInc) {
            final String message = "Min Priority (" + minPriorityInc +
                    ") must be less than or equal to Max Priority (" + maxPriorityInc + ")";
            throw new IllegalArgumentException(message);
        }

        this.parallelism = parallelism;
        this.minPriorityInc = minPriorityInc;
        this.maxPriorityInc = maxPriorityInc;
        this.countDownLatch = new ModCountDownLatch(1);

        this.globalPriorityState = new GlobalPriorityState(maxPriorityInc - minPriorityInc + 1);

        this.workerThreads = new WorkerThread[parallelism];
        for (int i = 0; i < parallelism; i++) {
            this.workerThreads[i] = new WorkerThread(
                    i, poolType, workerThreads, globalPriorityState,
                    countDownLatch, minPriorityInc, maxPriorityInc);
        }
    }

    @Override
    public void submit(final Runnable task) {
        final int priority = (maxPriorityInc + minPriorityInc) / 2;
        submit(priority, task);
    }

    @Override
    public void submit(final int priority, final Runnable task) {

        final int sanitizedPriority = Math.max(minPriorityInc, Math.min(priority, maxPriorityInc));

        countDownLatch.updateCount();
        final Thread currentThread = Thread.currentThread();
        if (currentThread instanceof WorkerThread) {
            ((WorkerThread) currentThread).pushTask(sanitizedPriority, task);
            if (!allThreadsStarted) {
                final int numActiveThreads = startedThreadsCounter.incrementAndGet();
                if (numActiveThreads < parallelism) {
                    workerThreads[numActiveThreads].start();
                } else {
                    allThreadsStarted = true;
                }
            }
        } else {
            final int numActiveThreads = startedThreadsCounter.incrementAndGet();
            if (numActiveThreads < parallelism) {
                workerThreads[numActiveThreads].pushTask(sanitizedPriority, task);
            } else {
                throw new RuntimeException("All threads active, cannot accept task from non-worker thread");
            }
            if (numActiveThreads < parallelism) {
                workerThreads[numActiveThreads].start();
            }
        }
    }

    @Override
    public void triggerShutdown() {
        countDownLatch.countDown();
    }

    @Override
    public void awaitTermination() {
        boolean keepLooping = true;
        while (keepLooping) {
            try {
                countDownLatch.await();
            } catch (final InterruptedException ex) {
                // ignore
            } finally {
                keepLooping = !stopped();
            }
        }
    }

    private boolean stopped() {
        return countDownLatch.getCount() <= 0;
    }

    private static class WorkerThread extends Thread {

        private final int parallelism;
        private final int threadSeqNum;
        private final int maxPriorityInc;
        private int previousVictim;
        private int localMaxPriorityIndex;

        private final int priorityLevels;
        private final WorkStealingPool[] myPools;
        private final boolean[] localPriorityState;

        private final WorkerThread[] allWorkerThreads;
        private final GlobalPriorityState globalPriorityState;
        private final ModCountDownLatch countDownLatch;

        private WorkerThread(
                final int threadSeqNum, final PoolType poolType,
                final WorkerThread[] workerThreads,
                final GlobalPriorityState globalPriorityState,
                final ModCountDownLatch countDownLatch,
                final int minPriorityInc, final int maxPriorityInc) {

            this.parallelism = workerThreads.length;
            this.threadSeqNum = threadSeqNum;
            this.maxPriorityInc = maxPriorityInc;
            this.previousVictim = nextVictim(threadSeqNum);
            this.localMaxPriorityIndex = maxPriorityInc - minPriorityInc + 1;

            this.allWorkerThreads = workerThreads;
            this.globalPriorityState = globalPriorityState;
            this.countDownLatch = countDownLatch;

            this.priorityLevels = maxPriorityInc - minPriorityInc + 1;
            this.localPriorityState = new boolean[priorityLevels];
            this.myPools = new WorkStealingPool[priorityLevels];
            for (int i = 0; i < myPools.length; i++) {
                myPools[i] = poolType.newInstance();
            }
        }

        private int nextVictim(final int threadSeqNum) {
            return (threadSeqNum + 1) % parallelism;
        }

        @Override
        public void run() {
            try {
                // loop trying to execute tasks unit executor has been stopped
                boolean keepLooping = true;
                int counter = 0;
                while (keepLooping) {
                    final Object myTask = findTask();
                    if (myTask instanceof Runnable) {
                        counter = 0;
                        try {
                            ((Runnable) myTask).run();
                        } finally {
                            countDownLatch.countDown();
                        }
                    } else {
                        counter++;
                        if (counter > 1_000_000) {
                            final int availableIndex = globalPriorityState.nextAvailableIndex(0);
                            if (availableIndex >= priorityLevels) {
                                // no more tasks in queues
                                while (countDownLatch.getCount() > 0) {
                                    countDownLatch.countDown();
                                }
                            } else {
                                counter = 0;
                            }
                        }
                    }
                    keepLooping = !stopped();
                }
            } catch (final Throwable ex) {
                ex.printStackTrace();
            }
            return;
        }

        private Object findTask() {

            // first search for highest priority item
            if (localPriorityState[0]) {
                // our local flags claim we have a task with higher priority!
                final Object localTask = myPools[0].popBottom();
                if (localTask != WorkStealingPool.EMPTY) {
                    // found a local task to execute with highest priority
                    localMaxPriorityIndex = 0;
                    return localTask;
                } else {
                    localPriorityState[0] = false;
                }
            }

            final int priorityIndex = globalPriorityState.nextAvailableIndex(0);
            // ensure we do not have any local task with a higher priority (in case global state is out of sync)
            for (int loopIndex = localMaxPriorityIndex; loopIndex < priorityIndex; loopIndex++) {
                if (localPriorityState[loopIndex]) {
                    // our local flags claim we have a task with higher priority!
                    final WorkStealingPool myPool = myPools[loopIndex];
                    final Object localTask = myPool.popBottom();
                    if (localTask != WorkStealingPool.EMPTY) {
                        // found a local task to execute with higher priority
                        if (!myPool.isEmpty()) {
                            globalPriorityState.set(loopIndex, true);
                            localMaxPriorityIndex = loopIndex;
                        } else {
                            localPriorityState[loopIndex] = false;
                        }
                        return localTask;
                    } else {
                        localPriorityState[loopIndex] = false;
                    }
                }
            }

            // exhaustively search local and global pools, attempting steals
            int loopPriorityIndex = priorityIndex;
            while (loopPriorityIndex < priorityLevels) {
                {
                    final Object localTask = myPools[loopPriorityIndex].popBottom();
                    if (localTask != WorkStealingPool.EMPTY) {
                        // found a local task to execute
                        localMaxPriorityIndex = loopPriorityIndex;
                        return localTask;
                    }
                }
                for (int i = 0; i < parallelism; i++) {
                    // find victim index and try to steal from there
                    final WorkerThread victimThread = allWorkerThreads[previousVictim];
                    if (victimThread.claimsTaskWithPriority(loopPriorityIndex)) {
                        final Object stolenTask = victimThread.steal(loopPriorityIndex);
                        if (stolenTask != WorkStealingPool.EMPTY) {
                            // found a stolen task to execute
                            return stolenTask;
                        }
                    }
                    // Current victim couldn't provide task, update victim
                    previousVictim = nextVictim(previousVictim);
                    if (previousVictim == threadSeqNum) {
                        previousVictim = nextVictim(previousVictim);
                    }
                }

                // no task with specified priority found, attempt to update global state
                globalPriorityState.set(loopPriorityIndex, false);

                // try and search for task with next available priority
                loopPriorityIndex = globalPriorityState.nextAvailableIndex(loopPriorityIndex + 1);
            }
            // found no task to execute :(
            return WorkStealingPool.EMPTY;
        }

        private void pushTask(final int priorityLevel, final Object item) {
            final int priorityIndex = maxPriorityInc - priorityLevel;
            final boolean priorityAvailable = localPriorityState[priorityIndex];

            myPools[priorityIndex].pushBottom(item);
            if (!priorityAvailable) {
                // need to update both global and local flags
                globalPriorityState.set(priorityIndex, true);
                localPriorityState[priorityIndex] = true;
            }
            localMaxPriorityIndex = Math.min(localMaxPriorityIndex, priorityIndex);
        }

        private Object steal(final int priorityIndex) {
            // rely on the worker to update its own flags that might be out of sync due to a steal
            return myPools[priorityIndex].steal();
        }

        private boolean claimsTaskWithPriority(final int priorityIndex) {
            return localPriorityState[priorityIndex];
        }

        private boolean stopped() {
            return countDownLatch.getCount() <= 0;
        }
    }

    private static class GlobalPriorityState {
        private final boolean[] statusArray;

        private GlobalPriorityState(final int levels) {
            this.statusArray = new boolean[levels];
            for (int i = 0; i < statusArray.length; i++) {
                statusArray[i] = false;
            }
        }

        public void set(final int index, final boolean value) {
            statusArray[index] = (value);
        }

        public int nextAvailableIndex(final int startIndex) {
            int i = startIndex;
            for (; i < statusArray.length; i++) {
                if (statusArray[i]) {
                    return i;
                }
            }
            return i;
        }
    }
}
