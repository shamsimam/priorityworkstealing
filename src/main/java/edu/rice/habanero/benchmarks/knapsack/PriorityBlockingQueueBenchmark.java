package edu.rice.habanero.benchmarks.knapsack;

import edu.rice.habanero.benchmarks.BenchmarkRunner;
import edu.rice.habanero.concurrent.executors.PriorityBlockingQueueTaskExecutor;
import edu.rice.habanero.concurrent.executors.TaskExecutor;

import java.util.concurrent.TimeUnit;

/**
 * @author <a href="http://shams.web.rice.edu/">Shams Imam</a> (shams@rice.edu)
 */
public class PriorityBlockingQueueBenchmark extends AbstractBenchmark {

    public static void main(final String[] args) {
        BenchmarkRunner.runBenchmark(args, new PriorityBlockingQueueBenchmark());
    }

    @Override
    protected TaskExecutor createTaskExecutor() {
        final int numThreads = BenchmarkRunner.numThreads();
        final int minPriorityInc = BenchmarkRunner.minPriority();
        final int maxPriorityInc = BenchmarkRunner.maxPriority();
        return new PriorityBlockingQueueTaskExecutor(
                numThreads, numThreads, 0L, TimeUnit.MILLISECONDS,
                minPriorityInc, maxPriorityInc);
    }
}
