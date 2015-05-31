package edu.rice.habanero.benchmarks.forkjoinqos;

import edu.rice.habanero.benchmarks.Benchmark;
import edu.rice.habanero.benchmarks.BenchmarkRunner;
import edu.rice.habanero.concurrent.executors.TaskExecutor;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static edu.rice.habanero.benchmarks.BenchmarkRunner.*;
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
        ForkJoinQosConfig.parseArgs(args);
    }

    @Override
    public final void printArgInfo() {
        ForkJoinQosConfig.printArgs();
    }

    @Override
    public final void preIteration(final boolean firstIteration) {
        // nothing to do
    }

    @Override
    public final void runIteration() {
        final TaskExecutor taskExecutor = createTaskExecutor();

        final AtomicInteger tasksExecCounter = new AtomicInteger(0);
        final AtomicLong prioritySumCounter = new AtomicLong(0);

        kernel(taskExecutor, new Runnable() {
            @Override
            public void run() {
                final int priorityLevels = maxPriority() - minPriority() + 1;
                final int numProducers = 100 * numThreads();
                final int chunkSize = ForkJoinQosConfig.N / numProducers;
                for (int p = 0; p < numProducers; p++) {
                    async(maxPriority(), new Runnable() {
                        @Override
                        public void run() {
                            for (int i = 0; i < chunkSize; i++) {
                                final int priority = i % (priorityLevels - 1);
                                async(priority, new Runnable() {
                                    @Override
                                    public void run() {

                                        final int tasksExec = tasksExecCounter.get();
                                        if (tasksExec > ForkJoinQosConfig.L) {
                                            return;
                                        } else {
                                            tasksExecCounter.incrementAndGet();
                                        }

                                        prioritySumCounter.addAndGet(priority);

                                        final double theta = (37.2 * priority) + 1;
                                        ForkJoinQosConfig.performComputation(theta);
                                    }
                                });
                            }
                        }
                    });

                }
            }
        });

        final double actualTasksExec = tasksExecCounter.get();
        final double actualPrioritySum = prioritySumCounter.get();
        final double actualPriorityQos = actualPrioritySum / actualTasksExec;

        System.out.printf(BenchmarkRunner.statDataOutputFormat, "", "Actual Tasks Exec", actualTasksExec);
        System.out.printf(BenchmarkRunner.statDataOutputFormat, "", "Priority of Tasks", actualPrioritySum);
        System.out.printf(BenchmarkRunner.statDataOutputFormat, "", "Avg. Priority QoS", actualPriorityQos);
    }

    protected abstract TaskExecutor createTaskExecutor();

    @Override
    public final void cleanupIteration(final boolean lastIteration, final double execTimeMillis) {
        // nothing to do
    }
}
