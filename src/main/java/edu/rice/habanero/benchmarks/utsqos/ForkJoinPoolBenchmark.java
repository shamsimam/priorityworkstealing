package edu.rice.habanero.benchmarks.utsqos;

import edu.rice.habanero.benchmarks.BenchmarkRunner;
import edu.rice.habanero.concurrent.executors.GenericTaskExecutor;
import edu.rice.habanero.concurrent.executors.TaskExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

/**
 * @author <a href="http://shams.web.rice.edu/">Shams Imam</a> (shams@rice.edu)
 */
public class ForkJoinPoolBenchmark extends AbstractBenchmark {

    public static void main(final String[] args) {
        BenchmarkRunner.runBenchmark(args, new ForkJoinPoolBenchmark());
    }

    @Override
    protected TaskExecutor createTaskExecutor() {
        final ExecutorService executorService = new ForkJoinPool(BenchmarkRunner.numThreads());
        final int minPriorityInc = BenchmarkRunner.minPriority();
        final int maxPriorityInc = BenchmarkRunner.maxPriority();
        return new GenericTaskExecutor(minPriorityInc, maxPriorityInc, executorService);
    }
}
