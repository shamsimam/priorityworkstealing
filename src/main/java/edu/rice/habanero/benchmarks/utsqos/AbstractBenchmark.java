package edu.rice.habanero.benchmarks.utsqos;

import edu.rice.habanero.benchmarks.Benchmark;
import edu.rice.habanero.benchmarks.BenchmarkRunner;
import edu.rice.habanero.concurrent.executors.TaskExecutor;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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
        UtsConfig.parseArgs(args);
    }

    @Override
    public final void printArgInfo() {
        UtsConfig.printArgs();
    }

    @Override
    public final void preIteration(final boolean firstIteration) {
        // nothing to do
    }

    @Override
    public final void runIteration() {

        final AtomicInteger tasksExecCounter = new AtomicInteger(0);
        final AtomicLong prioritySumCounter = new AtomicLong(0);

        final TaskExecutor taskExecutor = createTaskExecutor();

        kernel(taskExecutor, new Runnable() {
            @Override
            public void run() {

                final Node rootNode = UtsConfig.rootNode;
                traverseNode(rootNode);

            }

            protected void spawnNode(final Node node) {
                taskExecutor.submit(node.priority, new Runnable() {
                    @Override
                    public void run() {
                        final int tasksExec = tasksExecCounter.get();
                        if (tasksExec > UtsConfig.L) {
                            return;
                        } else {
                            tasksExecCounter.incrementAndGet();
                        }

                        prioritySumCounter.addAndGet(node.priority);
                        traverseNode(node);
                    }
                });
            }

            protected void traverseNode(final Node node) {
                final int numChildren = node.numChildren();
                for (int i = 0; i < numChildren; i++) {
                    final Node childNode = node.child(i);
                    spawnNode(childNode);
                }
            }
        });

        final double actualTasksExec = tasksExecCounter.get();
        final double actualPrioritySum = prioritySumCounter.get();
        final double actualPriorityQos = actualPrioritySum / actualTasksExec;

        System.out.printf(BenchmarkRunner.statDataOutputFormat, "", "Actual Tasks Exec", actualTasksExec);
        System.out.printf(BenchmarkRunner.statDataOutputFormat, "", "Priority of Tasks", actualPrioritySum);
        System.out.printf(BenchmarkRunner.statDataOutputFormat, "", "Avg. Priority QoS", actualPriorityQos);

        track("AvgTaskPriority", actualPriorityQos);
    }

    protected abstract TaskExecutor createTaskExecutor();

    @Override
    public final void cleanupIteration(final boolean lastIteration, final double execTimeMillis) {
        // nothing to do
    }
}
