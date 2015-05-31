package edu.rice.habanero.benchmarks.fibonacci;

import edu.rice.habanero.benchmarks.Benchmark;
import edu.rice.habanero.concurrent.executors.TaskExecutor;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static edu.rice.habanero.benchmarks.BenchmarkRunner.maxPriority;
import static edu.rice.habanero.benchmarks.BenchmarkRunner.minPriority;
import static edu.rice.habanero.concurrent.util.TaskExecutorUtil.async;
import static edu.rice.habanero.concurrent.util.TaskExecutorUtil.kernel;

/**
 * @author <a href="http://shams.web.rice.edu/">Shams Imam</a> (shams@rice.edu)
 */
public abstract class AbstractBenchmark extends Benchmark {

    /**
     * package protected constructor.
     */
    AbstractBenchmark() {
        super();
    }

    @Override
    public final void initialize(final String[] args) throws IOException {
        FibonacciConfig.parseArgs(args);
    }

    @Override
    public final void printArgInfo() {
        FibonacciConfig.printArgs();
    }

    @Override
    public final void preIteration(final boolean firstIteration) {
        // nothing to do
    }

    @Override
    public final void runIteration() {
        final TaskExecutor taskExecutor = createTaskExecutor();

        final AtomicInteger resultAcc = new AtomicInteger(0);
        kernel(taskExecutor, new Runnable() {
            @Override
            public void run() {
                final int numLevels = maxPriority() - minPriority() + 1;
                async(0, new Runnable() {
                    @Override
                    public void run() {
                        fibonacciRecusion(1, numLevels, FibonacciConfig.N, resultAcc);
                    }
                });
            }
        });
    }

    private void fibonacciRecusion(final int id, final int numLevels, final int number, final AtomicInteger resultAcc) {
        if (number == 1 || number == 2) {
            resultAcc.addAndGet(1);
            return;
        }

        final int leftId = 2 * id;
        async(leftId % numLevels, new Runnable() {
            @Override
            public void run() {
                fibonacciRecusion(leftId, numLevels, number - 1, resultAcc);
            }
        });

        final int rightId = leftId + 1;
        async(rightId % numLevels, new Runnable() {
            @Override
            public void run() {
                fibonacciRecusion(rightId, numLevels, number - 2, resultAcc);
            }
        });
    }

    protected abstract TaskExecutor createTaskExecutor();

    @Override
    public final void cleanupIteration(final boolean lastIteration, final double execTimeMillis) {
        // nothing to do
    }
}
