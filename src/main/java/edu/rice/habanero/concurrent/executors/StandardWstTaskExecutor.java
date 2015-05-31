package edu.rice.habanero.concurrent.executors;

import edu.rice.habanero.concurrent.util.ModCountDownLatch;
import edu.rice.habanero.concurrent.util.PriorityTask;
import edu.rice.habanero.concurrent.wstdeque.PoolType;
import edu.rice.habanero.concurrent.wstdeque.WorkStealingPool;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="http://shams.web.rice.edu/">Shams Imam</a> (shams@rice.edu)
 */
public class StandardWstTaskExecutor implements TaskExecutor {

    private final PoolType poolType;
    private final int parallelism;
    private final ModCountDownLatch countDownLatch;
    private final WorkerThread[] workerThreads;
    private boolean allThreadsStarted = false;

    private final AtomicInteger startedThreadsCounter = new AtomicInteger(-1);

    public StandardWstTaskExecutor(final PoolType poolType, final int parallelism) {
        this.poolType = poolType;
        this.parallelism = parallelism;
        this.countDownLatch = new ModCountDownLatch(1);
        this.workerThreads = new WorkerThread[parallelism];
        for (int i = 0; i < parallelism; i++) {
            this.workerThreads[i] = new WorkerThread(i, poolType, workerThreads, countDownLatch);
        }
    }

    @Override
    public void submit(final int priority, final Runnable runnable) {

        final Runnable task = poolType.supportsPriority() ? new PriorityTask<Void>(priority, runnable, countDownLatch) : runnable;

        countDownLatch.updateCount();
        final Thread currentThread = Thread.currentThread();
        if (currentThread instanceof WorkerThread) {
            ((WorkerThread) currentThread).pushTask(task);
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
                workerThreads[numActiveThreads].pushTask(task);
            } else {
                throw new RuntimeException("All threads active, cannot accept task from non-worker thread");
            }
            if (numActiveThreads < parallelism) {
                workerThreads[numActiveThreads].start();
            }
        }
    }

    @Override
    public void submit(final Runnable task) {
        // ignore priority
        submit(0, task);
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
        private int previousVictim;
        private final WorkerThread[] workerThreads;
        private final WorkStealingPool myPool;
        private final ModCountDownLatch countDownLatch;

        private WorkerThread(
                final int threadSeqNum, final PoolType poolType,
                final WorkerThread[] workerThreads,
                final ModCountDownLatch countDownLatch) {
            this.parallelism = workerThreads.length;
            this.threadSeqNum = threadSeqNum;
            this.countDownLatch = countDownLatch;
            this.previousVictim = nextVictim(threadSeqNum);
            this.workerThreads = workerThreads;
            this.myPool = poolType.newInstance();
            ;
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
                            final Object lastTaskAttempt = findTask();
                            if (lastTaskAttempt == null) {
                                // no more tasks in queues
                                while (countDownLatch.getCount() > 0) {
                                    countDownLatch.countDown();
                                }
                            } else {
                                counter = 0;
                                pushTask(lastTaskAttempt);
                            }
                        }
                    }
                    keepLooping = !stopped();
                }
            } catch (final Throwable ex) {
                ex.printStackTrace();
            }
        }

        private Object findTask() {
            {
                final Object localTask = myPool.popBottom();
                if (localTask != WorkStealingPool.EMPTY) {
                    // found a local task to execute
                    return localTask;
                }
            }
            // search for task from victim
            while (!stopped()) {
                final WorkerThread victimThread = workerThreads[previousVictim];
                final Object stolenTask = victimThread.steal();
                if (stolenTask != WorkStealingPool.EMPTY) {
                    // found a stolen task to execute
                    return stolenTask;
                }
                // update victim
                previousVictim = nextVictim(previousVictim);
                if (previousVictim == threadSeqNum) {
                    previousVictim = nextVictim(previousVictim);
                }
            }

            return WorkStealingPool.EMPTY;
        }

        private void pushTask(final Object item) {
            myPool.pushBottom(item);
        }

        private Object steal() {
            return myPool.steal();
        }

        private boolean stopped() {
            return countDownLatch.getCount() <= 0;
        }
    }
}
