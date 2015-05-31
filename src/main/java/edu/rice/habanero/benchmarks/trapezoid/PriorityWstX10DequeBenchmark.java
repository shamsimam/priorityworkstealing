package edu.rice.habanero.benchmarks.trapezoid;

import edu.rice.habanero.benchmarks.BenchmarkRunner;
import edu.rice.habanero.concurrent.executors.PriorityWstTaskExecutor;
import edu.rice.habanero.concurrent.executors.TaskExecutor;
import edu.rice.habanero.concurrent.wstdeque.PoolType;

/**
 * @author <a href="http://shams.web.rice.edu/">Shams Imam</a> (shams@rice.edu)
 */
public class PriorityWstX10DequeBenchmark extends AbstractBenchmark {

    public static void main(final String[] args) {
        BenchmarkRunner.runBenchmark(args, new PriorityWstX10DequeBenchmark());
    }

    @Override
    protected TaskExecutor createTaskExecutor() {
        final int numThreads = BenchmarkRunner.numThreads();
        final int minPriorityInc = BenchmarkRunner.minPriority();
        final int maxPriorityInc = BenchmarkRunner.maxPriority();
        return new PriorityWstTaskExecutor(
                PoolType.X10, numThreads, minPriorityInc, maxPriorityInc);
    }
}
