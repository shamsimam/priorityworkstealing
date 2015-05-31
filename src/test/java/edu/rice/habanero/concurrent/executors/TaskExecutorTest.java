package edu.rice.habanero.concurrent.executors;

import edu.rice.habanero.concurrent.wstdeque.PoolType;
import junit.framework.TestCase;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static edu.rice.habanero.concurrent.util.TaskExecutorUtil.async;
import static edu.rice.habanero.concurrent.util.TaskExecutorUtil.kernel;

/**
 * @author <a href="http://shams.web.rice.edu/">Shams Imam</a> (shams@rice.edu)
 */
public class TaskExecutorTest extends TestCase {

    protected static final int minPriorityInc = Thread.MIN_PRIORITY;
    protected static final int maxPriorityInc = 2 * Thread.MAX_PRIORITY;

    private static void busyWait(final int taskId, final PriorityCounter priorityCounter) {
        final int waitNum = (5_000 + (taskId < 25 ? 30_000 : 0));
        for (int i = 0; i < waitNum; i++) {
            // dummy method calls
            Math.random();
        }
    }

    private static void executeTasks(
            final int numTasks, final TaskExecutor taskExecutor,
            final int minPriorityInc, final int maxPriorityInc,
            final double tolerancePercent, final int availableProcessors) {

        System.out.println("  " + taskExecutor.getClass().getSimpleName() + ":: Available processors = " + availableProcessors);

        final PriorityCounter priorityCounter = new PriorityCounter(minPriorityInc, maxPriorityInc);
        final int numPriorities = maxPriorityInc - minPriorityInc + 1;

        final long startTimeNanos = System.nanoTime();
        kernel(taskExecutor, new Runnable() {
            @Override
            public void run() {

                for (int i = 0; i < numTasks; i++) {
                    final int ii = i;
                    final int priority = minPriorityInc + (ii % numPriorities);

                    priorityCounter.registerSubmission(priority);
                    async(priority, new Runnable() {
                        @Override
                        public void run() {
                            priorityCounter.registerExecution(priority);
                            busyWait(ii, priorityCounter);
                            priorityCounter.registerCompletion(priority);
                        }
                    });
                }
            }
        });
        final long endTimeNanos = System.nanoTime();
        final long execTimeMillis = (endTimeNanos - startTimeNanos) / 1_000_000;
        System.out.println("  " + taskExecutor.getClass().getSimpleName() + ":: Execution Time = " + execTimeMillis + " ms.");

        final int[] processedResults = priorityCounter.process(availableProcessors);
        {
            final long actualTasks = processedResults[0];
            final String message1 = "Executed number of tasks: actual = " + actualTasks + ", expected = " + numTasks;
            System.out.println("  " + taskExecutor.getClass().getSimpleName() + ":: " + message1);
            assertEquals(message1, numTasks, actualTasks);
        }
        {
            final long toleranceOutOfOrder = (long) (tolerancePercent * numTasks);
            final long actualOutOfOrder = processedResults[1];
            final String message2 = "Tasks executed out of priority: actual = " + actualOutOfOrder + ", tolerance = " + toleranceOutOfOrder;
            System.out.println("  " + taskExecutor.getClass().getSimpleName() + ":: " + message2);
            assertTrue(message2, actualOutOfOrder < toleranceOutOfOrder);
        }
    }


    public void testGenericForkJoinTaskExecutor() {

        System.out.println("TaskExecutorTest.testGenericForkJoinTaskExecutor: starts...");

        final int availableProcessors = Runtime.getRuntime().availableProcessors();

        final ExecutorService executorService = new ForkJoinPool(availableProcessors);
        final TaskExecutor taskExecutor = new GenericTaskExecutor(minPriorityInc, maxPriorityInc, executorService);
        final int numTasks = 500;

        executeTasks(numTasks, taskExecutor, minPriorityInc, maxPriorityInc, 0.95, availableProcessors);

        System.out.println("TaskExecutorTest.testGenericForkJoinTaskExecutor: ends.");
    }

    public void testGenericThreadPoolTaskExecutor() {

        System.out.println("TaskExecutorTest.testGenericThreadPoolTaskExecutor: starts...");

        final int availableProcessors = Runtime.getRuntime().availableProcessors();

        final ExecutorService executorService = Executors.newFixedThreadPool(availableProcessors);
        final TaskExecutor taskExecutor = new GenericTaskExecutor(minPriorityInc, maxPriorityInc, executorService);
        final int numTasks = 500;

        executeTasks(numTasks, taskExecutor, minPriorityInc, maxPriorityInc, 0.95, availableProcessors);

        System.out.println("TaskExecutorTest.testGenericThreadPoolTaskExecutor: ends.");
    }

    public void testPriorityBlockingQueueTaskExecutor() {

        System.out.println("TaskExecutorTest.testPriorityBlockingQueueTaskExecutor: starts...");

        final int availableProcessors = Runtime.getRuntime().availableProcessors();

        final TaskExecutor taskExecutor = new PriorityBlockingQueueTaskExecutor(
                availableProcessors, availableProcessors,
                0L, TimeUnit.MILLISECONDS,
                minPriorityInc, maxPriorityInc);
        final int numTasks = 500;

        executeTasks(numTasks, taskExecutor, minPriorityInc, maxPriorityInc, 0.30, availableProcessors);

        System.out.println("TaskExecutorTest.testPriorityBlockingQueueTaskExecutor: ends.");
    }

    public void testPriorityWstTaskExecutorCilkDeque() {

        System.out.println("TaskExecutorTest.testPriorityWstTaskExecutorCilkDeque: starts...");

        final int availableProcessors = Runtime.getRuntime().availableProcessors();

        final TaskExecutor taskExecutor = new PriorityWstTaskExecutor(
                PoolType.CILK, availableProcessors, minPriorityInc, maxPriorityInc);
        final int numTasks = 500;

        executeTasks(numTasks, taskExecutor, minPriorityInc, maxPriorityInc, 0.30, availableProcessors);

        System.out.println("TaskExecutorTest.testPriorityWstTaskExecutorCilkDeque: ends.");
    }

    public void testPriorityWstTaskExecutorJdkDeque() {

        System.out.println("TaskExecutorTest.testPriorityWstTaskExecutorJdkDeque: starts...");

        final int availableProcessors = Runtime.getRuntime().availableProcessors();

        final TaskExecutor taskExecutor = new PriorityWstTaskExecutor(
                PoolType.JDK, availableProcessors, minPriorityInc, maxPriorityInc);
        final int numTasks = 500;

        executeTasks(numTasks, taskExecutor, minPriorityInc, maxPriorityInc, 0.30, availableProcessors);

        System.out.println("TaskExecutorTest.testPriorityWstTaskExecutorJdkDeque: ends.");
    }

    public void testPriorityWstTaskExecutorX10Deque() {

        System.out.println("TaskExecutorTest.testPriorityWstTaskExecutorX10Deque: starts...");

        final int availableProcessors = Runtime.getRuntime().availableProcessors();

        final TaskExecutor taskExecutor = new PriorityWstTaskExecutor(
                PoolType.X10, availableProcessors, minPriorityInc, maxPriorityInc);
        final int numTasks = 500;

        executeTasks(numTasks, taskExecutor, minPriorityInc, maxPriorityInc, 0.30, availableProcessors);

        System.out.println("TaskExecutorTest.testPriorityWstTaskExecutorX10Deque: ends.");
    }

    public void testStandardWstTaskExecutorCilkDeque() {

        System.out.println("TaskExecutorTest.testStandardWstTaskExecutorCilkDeque: starts...");

        final int availableProcessors = Runtime.getRuntime().availableProcessors();

        final TaskExecutor taskExecutor = new StandardWstTaskExecutor(PoolType.CILK, availableProcessors);
        final int numTasks = 500;

        executeTasks(numTasks, taskExecutor, minPriorityInc, maxPriorityInc, 0.95, availableProcessors);

        System.out.println("TaskExecutorTest.testStandardWstTaskExecutorCilkDeque: ends.");
    }

    public void testStandardWstTaskExecutorJdkDeque() {

        System.out.println("TaskExecutorTest.testStandardWstTaskExecutorJdkDeque: starts...");

        final int availableProcessors = Runtime.getRuntime().availableProcessors();

        final TaskExecutor taskExecutor = new StandardWstTaskExecutor(PoolType.JDK, availableProcessors);
        final int numTasks = 500;

        executeTasks(numTasks, taskExecutor, minPriorityInc, maxPriorityInc, 0.95, availableProcessors);

        System.out.println("TaskExecutorTest.testStandardWstTaskExecutorJdkDeque: ends.");
    }

    public void testStandardWstTaskExecutorX10Deque() {

        System.out.println("TaskExecutorTest.testStandardWstTaskExecutorX10Deque: starts...");

        final int availableProcessors = Runtime.getRuntime().availableProcessors();

        final TaskExecutor taskExecutor = new StandardWstTaskExecutor(PoolType.X10, availableProcessors);
        final int numTasks = 500;

        executeTasks(numTasks, taskExecutor, minPriorityInc, maxPriorityInc, 0.95, availableProcessors);

        System.out.println("TaskExecutorTest.testStandardWstTaskExecutorX10Deque: ends.");
    }

    public void testSynchronizedPriorityQueueTaskExecutor() {

        System.out.println("TaskExecutorTest.testSynchronizedPriorityQueueTaskExecutor: starts...");

        final int availableProcessors = Runtime.getRuntime().availableProcessors();

        final TaskExecutor taskExecutor = new SynchronizedPriorityQueueTaskExecutor(
                availableProcessors, availableProcessors,
                0L, TimeUnit.MILLISECONDS,
                minPriorityInc, maxPriorityInc);
        final int numTasks = 500;

        executeTasks(numTasks, taskExecutor, minPriorityInc, maxPriorityInc, 0.30, availableProcessors);

        System.out.println("TaskExecutorTest.testSynchronizedPriorityQueueTaskExecutor: ends.");
    }

    private static class PriorityCounter {

        private final int minPriorityInc;
        private final int maxPriorityInc;
        private final Queue<EventLog> eventLogQueue = new ConcurrentLinkedQueue<>();

        public PriorityCounter(final int minPriorityInc, final int maxPriorityInc) {

            this.minPriorityInc = minPriorityInc;
            this.maxPriorityInc = maxPriorityInc;
        }

        public void registerSubmission(final int priority) {
            eventLogQueue.add(new EventLog(Event.SPAWN, priority));
            ;
        }

        public void registerExecution(final int priority) {
            eventLogQueue.add(new EventLog(Event.EXECUTING, priority));
        }

        public void registerCompletion(final int priority) {
            eventLogQueue.add(new EventLog(Event.DONE, priority));
            ;
            ;
        }

        public int[] process(final int availableProcessors) {
            final List<EventLog> allEvents = new ArrayList<>(eventLogQueue);
            Collections.sort(allEvents);

            int numTasksCounter = 0;
            int outOfPriorityCounter = 0;

            final Map<Integer, AtomicLong> priorityTracker = new ConcurrentHashMap<>();
            for (int i = minPriorityInc; i <= maxPriorityInc; i++) {
                priorityTracker.put(i, new AtomicLong());
            }

            for (final EventLog eventLog : allEvents) {
                // System.out.println("   Processing: " + eventLog);
                if (Event.SPAWN.equals(eventLog.event)) {
                    numTasksCounter++;
                    priorityTracker.get(eventLog.priority).incrementAndGet();
                } else if (Event.EXECUTING.equals(eventLog.event)) {
                    long higherPriorityTaskCount = 0;
                    for (int i = Math.max(eventLog.priority + 1, minPriorityInc); i <= maxPriorityInc; i++) {
                        final long higherPriorityCount = priorityTracker.get(i).get();
                        higherPriorityTaskCount += higherPriorityCount;
                    }
                    if (higherPriorityTaskCount > (0.50 * availableProcessors)) {
                        outOfPriorityCounter += 1;
                    }
                } else if (Event.DONE.equals(eventLog.event)) {
                    priorityTracker.get(eventLog.priority).decrementAndGet();
                }
            }


            return new int[]{numTasksCounter, outOfPriorityCounter};
        }
    }

    private static enum Event {
        SPAWN,
        EXECUTING,
        DONE;
    }

    private static class EventLog implements Comparable<EventLog> {

        private static AtomicInteger ID_GEN = new AtomicInteger(0);

        private final int id;
        private final Event event;
        private final int priority;

        public EventLog(final Event event, final int priority) {
            this.id = ID_GEN.incrementAndGet();
            this.event = event;
            this.priority = priority;
        }

        @Override
        public int compareTo(final EventLog other) {
            return id - other.id;
        }

        @Override
        public String toString() {
            return "EventLog{" +
                    "id=" + id +
                    ", event=" + event +
                    ", priority=" + priority +
                    '}';
        }
    }
}
