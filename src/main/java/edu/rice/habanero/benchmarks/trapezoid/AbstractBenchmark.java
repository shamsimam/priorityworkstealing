package edu.rice.habanero.benchmarks.trapezoid;

import edu.rice.habanero.benchmarks.Benchmark;
import edu.rice.habanero.benchmarks.BenchmarkRunner;
import edu.rice.habanero.benchmarks.util.ThreadLocalCounter;
import edu.rice.habanero.concurrent.executors.TaskExecutor;

import java.io.IOException;

import static edu.rice.habanero.benchmarks.BenchmarkRunner.maxPriority;
import static edu.rice.habanero.benchmarks.BenchmarkRunner.numThreads;
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
        TrapezoidalConfig.parseArgs(args);
    }

    @Override
    public final void printArgInfo() {
        TrapezoidalConfig.printArgs();
    }

    @Override
    public final void preIteration(final boolean firstIteration) {
        // nothing to do
    }

    @Override
    public final void runIteration() {

        final ThreadLocalCounter resultSum = new ThreadLocalCounter();
        final TaskExecutor taskExecutor = createTaskExecutor();

        final int scaleFactor = 1_000_000_000;

        kernel(taskExecutor, new Runnable() {
            @Override
            public void run() {

                final int numProducers = 100 * numThreads();
                final int chunkSize = TrapezoidalConfig.N / numProducers;

                final double increment = (TrapezoidalConfig.R - TrapezoidalConfig.L) / TrapezoidalConfig.N;

                for (int p = 0; p < numProducers; p++) {
                    final int pp = p;
                    async(maxPriority(), new Runnable() {
                        @Override
                        public void run() {
                            for (int i = 0; i < chunkSize; i++) {
                                async(maxPriority(), new Runnable() {
                                    @Override
                                    public void run() {

                                        final int limit = 100;
                                        final double localIncrement = increment / limit;

                                        for (int i = 0; i < limit; i++) {
                                            final double lx = TrapezoidalConfig.L + (pp * chunkSize * increment);
                                            final double rx = lx + localIncrement;

                                            final double ly = TrapezoidalConfig.fx(lx);
                                            final double ry = TrapezoidalConfig.fx(rx);
                                            final double area = 0.5 * (ly + ry) * localIncrement;

                                            final long areaLong = (long) (area * scaleFactor);
                                            resultSum.increment(areaLong);
                                        }
                                    }
                                });
                            }
                        }
                    });

                }
            }
        });

        final double computedArea = (1.0 * resultSum.get()) / scaleFactor;
        System.out.printf(BenchmarkRunner.statDataOutputFormat, "", "Computed Area", computedArea);
    }

    protected abstract TaskExecutor createTaskExecutor();

    @Override
    public final void cleanupIteration(final boolean lastIteration, final double execTimeMillis) {
        // nothing to do
    }
}
