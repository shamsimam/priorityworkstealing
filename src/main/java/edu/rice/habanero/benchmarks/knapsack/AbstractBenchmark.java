/*
 * Cilk program to solve the 0-1 knapsack problem using a branch-and-bound technique.
 * Test for aborts.
 */
package edu.rice.habanero.benchmarks.knapsack;

import edu.rice.habanero.benchmarks.Benchmark;
import edu.rice.habanero.benchmarks.BenchmarkRunner;
import edu.rice.habanero.benchmarks.util.ThreadLocalCounter;
import edu.rice.habanero.concurrent.executors.TaskExecutor;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;

import static edu.rice.habanero.concurrent.util.TaskExecutorUtil.async;
import static edu.rice.habanero.concurrent.util.TaskExecutorUtil.kernel;

/**
 * Source: http://www.cs.cmu.edu/afs/cs.cmu.edu/project/scandal-2/misc/Cilk-4/examples/knapsack.cilk
 *
 * @author <a href="http://shams.web.rice.edu/">Shams Imam</a> (shams@rice.edu) [Ported to HJlib]
 */
public abstract class AbstractBenchmark extends Benchmark {

    public static final int INFEASIBLE_VALUE = -1;
    private static final AtomicInteger best_so_far = new AtomicInteger(INFEASIBLE_VALUE);
    private static final ThreadLocalCounter taskCounter = new ThreadLocalCounter();

    private static KnapsackItem[] allItems = null;
    private static long randomSeed = 1010101L;
    private static int maxItemWeight = 100;
    private static int maxItemValue = 1_000;
    private static int capacity = 2500;
    private static int threshold = 5;
    private static int numItems = threshold + 23;

    /**
     * package protected constructor.
     */
    AbstractBenchmark() {
        super();
    }

    @Override
    public void initialize(final String[] args) throws IOException {
        for (int i = 0; i < args.length - 1; i++) {
            switch (args[i]) {
                case "-n":
                    numItems = Integer.parseInt(args[i + 1]);
                    i++;
                    break;
                case "-c":
                    capacity = Integer.parseInt(args[i + 1]);
                    i++;
                    break;
                case "-mw":
                    maxItemWeight = Integer.parseInt(args[i + 1]);
                    i++;
                    break;
                case "-mv":
                    maxItemValue = Integer.parseInt(args[i + 1]);
                    i++;
                    break;
                case "-s":
                    randomSeed = Long.parseLong(args[i + 1]);
                    i++;
                    break;
                case "-t":
                    threshold = Integer.parseInt(args[i + 1]);
                    i++;
                    break;
                default:
                    System.out.println("Wrong option specified: " + args[i]);
                    break;
            }
        }

        BenchmarkRunner.MAX_PRIORITY = numItems - threshold + 1;
    }

    @Override
    public void preIteration(final boolean firstIteration) {
        initialize();
    }

    private void initialize() {
        best_so_far.set(INFEASIBLE_VALUE);
        taskCounter.reset();

        // randomly initialize the items
        final Random random = new Random(randomSeed);
        allItems = new KnapsackItem[numItems];
        for (int i = 0; i < numItems; i++) {
            final int value = Math.max(1, random.nextInt(maxItemValue));
            final int loopMaxWeight = maxItemWeight + maxItemValue - value;
            final int weight = (int) Math.max(maxItemWeight / 10,
                                              maxItemWeight * (1.0 * random.nextInt(loopMaxWeight) / loopMaxWeight));
            allItems[i] = new KnapsackItem(value, weight);
        }

        // sort the items on decreasing order of value/weight
        Arrays.sort(allItems);
    }

    @Override
    public void printArgInfo() {
        System.out.printf(BenchmarkRunner.argOutputFormat, "Num items", numItems);
        System.out.printf(BenchmarkRunner.argOutputFormat, "Threshold", threshold);
        System.out.printf(BenchmarkRunner.argOutputFormat, "Capacity", capacity);
        System.out.printf(BenchmarkRunner.argOutputFormat, "Max Item Weight", maxItemWeight);
        System.out.printf(BenchmarkRunner.argOutputFormat, "Max Item Value", maxItemValue);
        System.out.printf(BenchmarkRunner.argOutputFormat, "Random Seed", randomSeed);
    }

    protected abstract TaskExecutor createTaskExecutor();

    @Override
    public void runIteration() {
        final TaskExecutor taskExecutor = createTaskExecutor();

        final CallbackResult knapsackResult = new CallbackResult() {
            @Override
            public void run(final int arg) {
                set(arg);
                System.out.printf(BenchmarkRunner.argOutputFormat, "Best Value", arg);
            }
        };
        kernel(taskExecutor, new Runnable() {
            @Override
            public void run() {
                try {
                    knapsack(allItems, 0, capacity, numItems, 0, knapsackResult, 0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void cleanupIteration(final boolean lastIteration, final double execTimeMillis) {
        System.out.printf(BenchmarkRunner.argOutputFormat, "Num Tasks", taskCounter.get());
    }

    private static void knapsack(
            final KnapsackItem[] e, final int index,
            final int capacity, final int itemsLeft,
            final int valueSoFar, final CallbackResult knapsackResult,
            final int itemsTaken) {

        if (capacity <= 0) {
            /* base case: full knapsack, infeasible solution */
            knapsackResult.run(INFEASIBLE_VALUE);
            return;
        }

        final double ub = (double) valueSoFar + ((1.0 * capacity * e[index].value) / e[index].weight);
        if (ub < best_so_far.get()) {
            /* prune ! */
            knapsackResult.run(INFEASIBLE_VALUE);
            return;
        }

        if (itemsLeft <= threshold) {
            final int result = knapsackSeq(e, index, capacity, itemsLeft, valueSoFar);
            knapsackResult.run(result);
            return;
        }

        // counter to track the number of subtasks executed
        final AtomicInteger subTaskCounter = new AtomicInteger(0);

        // the callback will be lazily initialized, the content should be non-null before use
        final Runnable[] callBackRef = {null};

        class CallbackResultLocal extends CallbackResult {
            @Override
            public void run(final int arg) {

                set(arg);

                final int tasksCompleted = subTaskCounter.incrementAndGet();
                if (tasksCompleted == 2) {
                    // callBackRef[0] is guaranteed to be non-null!
                    callBackRef[0].run();
                }
            }
        }

        final CallbackResult withFuture = new CallbackResultLocal();
        final CallbackResult withoutFuture = new CallbackResultLocal();

        callBackRef[0] = new Runnable() {
            @Override
            public void run() {
                final int with = withFuture.get();
                final int without = withoutFuture.get();

                final int best = with > without ? with : without;

                 /*
                  * notice the right condition here. The program is still
                  * correct, in the sense that the best solution so far
                  * is at least best_so_far. Moreover best_so_far gets updated
                  * when returning, so eventually it should get the right
                  * value. The program is highly non-deterministic.
                  */
                updateBestSoFar(best);

                final int locIndex = index;
                knapsackResult.run(best);
            }
        };

        taskCounter.increment();
        async(index, new Runnable() {
            @Override
            public void run() {
                try {
                    knapsack(e, index + 1, capacity - e[index].weight, itemsLeft - 1, valueSoFar + e[index].value, withFuture, itemsTaken + 1);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        });

        taskCounter.increment();
        async(index, new Runnable() {
            @Override
            public void run() {
                try {
                    knapsack(e, index + 1, capacity - e[index].weight, itemsLeft - 1, valueSoFar, withoutFuture, itemsTaken);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        });
    }

    /**
     * @return the optimal solution
     */
    private static int knapsackSeq(
            final KnapsackItem[] e, final int argIndex,
            final int argCapacity, final int argItemsLeft, final int argValueSoFar) {

        final KnapsackArguments arguments = new KnapsackArguments(argIndex, argCapacity, argItemsLeft, argValueSoFar);
        final Stack<KnapsackArguments> workQueue = new Stack<>();
        workQueue.add(arguments);

        int localBest = INFEASIBLE_VALUE;
        while (!workQueue.isEmpty()) {
            final KnapsackArguments loopArg = workQueue.pop();

            if (loopArg.capacity <= 0) {
                /* base case: full knapsack, infeasible solution */
                continue;
            }

            final int loopValueSoFar = loopArg.valueSoFar;
            if (loopArg.itemsLeft == 0) {
                /* base case: no more items, feasible solution, with value v */
                if (localBest < loopValueSoFar) {
                    localBest = loopValueSoFar;
                }
                continue;
            }

            final KnapsackItem loopItem = e[loopArg.index];
            final double ub = (double) loopValueSoFar + ((1.0 * loopArg.capacity * loopItem.value) / loopItem.weight);
            if (ub < localBest || ub < best_so_far.get()) {
                /* prune ! */
                continue;
            }

            final int newNumItems = loopArg.itemsLeft - 1;
            final int newIndex = loopArg.index + 1;

            // compute the best solution without the current item in the knapsack
            final KnapsackArguments without = new KnapsackArguments(newIndex, loopArg.capacity, newNumItems, loopValueSoFar);
            workQueue.push(without);

            // compute the best solution with the current item in the knapsack
            final KnapsackArguments with = new KnapsackArguments(newIndex, loopArg.capacity - loopItem.weight, newNumItems, loopValueSoFar + loopItem.value);
            workQueue.push(with);

        }

        /*
          * notice the right condition here. The program is still
          * correct, in the sense that the best solution so far
          * is at least best_so_far. Moreover best_so_far gets updated
          * when returning, so eventually it should get the right
          * value. The program is highly non-deterministic.
          */
        updateBestSoFar(localBest);


        return localBest;
    }

    private static void updateBestSoFar(final int newBest) {

        boolean keepLooping = true;

        while (keepLooping) {
            keepLooping = false;
            final int curVal = best_so_far.get();
            if (newBest > curVal) {
                final boolean success = best_so_far.compareAndSet(curVal, newBest);
                if (!success) {
                    keepLooping = true;
                }
            }
        }

    }

    private static class KnapsackItem implements Comparable<KnapsackItem> {
        /* every item in the knapsack has a weight and a value */

        private final int value;
        private final int weight;

        private KnapsackItem(final int value, final int weight) {
            this.value = value;
            this.weight = weight;
        }

        @Override
        public int compareTo(final KnapsackItem other) {
            final double c = ((1.0 * this.value) / this.weight) - ((1.0 * other.value) / other.weight);

            if (c > 0) {
                return -1;
            }
            if (c < 0) {
                return 1;
            }
            return 0;
        }

        @Override
        public String toString() {
            return "KnapsackItem{" +
                    "value=" + value +
                    ", weight=" + weight +
                    '}';
        }
    }

    private static class KnapsackArguments {
        final int index;
        final int capacity;
        final int itemsLeft;
        final int valueSoFar;

        private KnapsackArguments(final int index, final int capacity, final int itemsLeft, final int valueSoFar) {
            this.index = index;
            this.capacity = capacity;
            this.itemsLeft = itemsLeft;
            this.valueSoFar = valueSoFar;
        }
    }

    private static abstract class CallbackResult {
        private final int initialValue = -1_000;
        private AtomicInteger resultRef = new AtomicInteger(initialValue);

        public final boolean set(final int result) {
            return resultRef.compareAndSet(initialValue, result);
        }

        public final int get() {
            return resultRef.get();
        }

        public abstract void run(int arg);
    }
}
