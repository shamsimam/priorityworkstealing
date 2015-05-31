package edu.rice.habanero.benchmarks.astar;

import edu.rice.habanero.benchmarks.Benchmark;
import edu.rice.habanero.benchmarks.BenchmarkRunner;
import edu.rice.habanero.benchmarks.astar.AStarConfig.GridNode;
import edu.rice.habanero.concurrent.executors.TaskExecutor;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static edu.rice.habanero.concurrent.util.TaskExecutorUtil.async;
import static edu.rice.habanero.concurrent.util.TaskExecutorUtil.kernel;

/**
 * @author <a href="http://shams.web.rice.edu/">Shams Imam</a> (shams@rice.edu)
 */
public abstract class AbstractBenchmark extends Benchmark {

    protected AbstractBenchmark() {
        super();
    }

    @Override
    public void initialize(final String[] args) throws IOException {
        AStarConfig.parseArgs(args);
    }

    @Override
    public void printArgInfo() {
        AStarConfig.printArgs();
    }

    @Override
    public void preIteration(final boolean firstIteration) {
        AStarConfig.initializeData();
    }

    protected abstract TaskExecutor createTaskExecutor();

    @Override
    public void runIteration() {

        final TaskExecutor taskExecutor = createTaskExecutor();

        final AtomicInteger nodesExplored = new AtomicInteger(0);
        final AtomicReference<Double> pathCost = new AtomicReference<Double>(null);

        kernel(taskExecutor, new Runnable() {

            private final GridNode targetNode = AStarConfig.targetNode();
            private final GridNode originNode = AStarConfig.originNode();

            private final AtomicBoolean pathFound = new AtomicBoolean(false);

            @Override
            public void run() {
                final GridNode rootNode = AStarConfig.originNode();
                if (rootNode == targetNode) {
                    pathFound.compareAndSet(false, true);
                    return;
                }
                performSearch(rootNode);
            }

            private void performSearch(final GridNode loopNode) {

                final int numNeighbors = loopNode.numNeighbors();
                for (int i = 0; i < numNeighbors; i++) {
                    final GridNode loopNeighbor = loopNode.neighbor(i);
                    final boolean success = loopNeighbor.tryUpdateParent(loopNode);
                    if (success) {
                        if (loopNeighbor == targetNode) {
                            final boolean successCas = pathFound.compareAndSet(false, true);
                            if (successCas) {
                                final double distance = loopNeighbor.distanceFrom(originNode);
                                pathCost.set(distance);
                            }
                            return;
                        } else {
                            spawnTask(loopNeighbor);
                        }
                    }
                }
            }

            private void spawnTask(final GridNode node) {
                final int priorityIndex = AStarConfig.priority(node, targetNode, originNode);
                async(priorityIndex, new Runnable() {
                    @Override
                    public void run() {
                        if (pathFound.get()) {
                            // early termination
                            return;
                        }
                        if (BenchmarkRunner.DEBUG) {
                            nodesExplored.incrementAndGet();
                        }
                        performSearch(node);
                    }
                });
            }
        });

        System.out.printf(BenchmarkRunner.argOutputFormat, "Solution Path Cost", pathCost.get());
        if (BenchmarkRunner.DEBUG) {
            System.out.printf(BenchmarkRunner.argOutputFormat, "Nodes Explored", nodesExplored.get());
        }
    }

    @Override
    public void cleanupIteration(final boolean lastIteration, final double execTimeMillis) {

        final boolean valid = AStarConfig.validate();
        System.out.printf(BenchmarkRunner.argOutputFormat, "Result valid", valid);
    }
}
