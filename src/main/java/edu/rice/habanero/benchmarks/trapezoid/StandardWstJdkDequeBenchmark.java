package edu.rice.habanero.benchmarks.trapezoid;

import edu.rice.habanero.benchmarks.BenchmarkRunner;
import edu.rice.habanero.concurrent.executors.StandardWstTaskExecutor;
import edu.rice.habanero.concurrent.executors.TaskExecutor;
import edu.rice.habanero.concurrent.wstdeque.PoolType;

/**
 * @author <a href="http://shams.web.rice.edu/">Shams Imam</a> (shams@rice.edu)
 */
public class StandardWstJdkDequeBenchmark extends AbstractBenchmark {

    public static void main(final String[] args) {
        BenchmarkRunner.runBenchmark(args, new StandardWstJdkDequeBenchmark());
    }

    @Override
    protected TaskExecutor createTaskExecutor() {
        final int numThreads = BenchmarkRunner.numThreads();
        return new StandardWstTaskExecutor(PoolType.JDK, numThreads);
    }
}
