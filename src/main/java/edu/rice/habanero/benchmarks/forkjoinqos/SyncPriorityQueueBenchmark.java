package edu.rice.habanero.benchmarks.forkjoinqos;

import edu.rice.habanero.benchmarks.BenchmarkRunner;
import edu.rice.habanero.concurrent.executors.SynchronizedPriorityQueueTaskExecutor;
import edu.rice.habanero.concurrent.executors.TaskExecutor;

import java.util.concurrent.TimeUnit;

/**
 * @author <a href="http://shams.web.rice.edu/">Shams Imam</a> (shams@rice.edu)
 */
public class SyncPriorityQueueBenchmark extends AbstractBenchmark {

    public static void main(final String[] args) {
        BenchmarkRunner.runBenchmark(args, new SyncPriorityQueueBenchmark());
    }

    @Override
    protected TaskExecutor createTaskExecutor() {
        final int numThreads = BenchmarkRunner.numThreads();
        final int minPriorityInc = BenchmarkRunner.minPriority();
        final int maxPriorityInc = BenchmarkRunner.maxPriority();
        return new SynchronizedPriorityQueueTaskExecutor(
                numThreads, numThreads, 0L, TimeUnit.MILLISECONDS,
                minPriorityInc, maxPriorityInc);
    }
}
