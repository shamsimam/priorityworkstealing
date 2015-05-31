package edu.rice.habanero.benchmarks.astar;

import edu.rice.habanero.benchmarks.BenchmarkRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static edu.rice.habanero.benchmarks.BenchmarkRunner.maxPriority;
import static edu.rice.habanero.benchmarks.BenchmarkRunner.minPriority;

/**
 * @author <a href="http://shams.web.rice.edu/">Shams Imam</a> (shams@rice.edu)
 */
public final class AStarConfig {

    protected static int GRID_SIZE = 200;
    private static GridNode[] allNodes = null;

    protected static void parseArgs(final String[] args) {
        int i = 0;
        while (i < args.length) {
            final String loopOptionKey = args[i];
            switch (loopOptionKey) {
                case "-g":
                    i += 1;
                    final int userInput = Integer.parseInt(args[i]);
                    final int allowedMax = 40_000;
                    GRID_SIZE = Math.min(userInput, allowedMax);
                    break;
            }
            i += 1;
        }
    }

    protected static void printArgs() {
        System.out.printf(BenchmarkRunner.argOutputFormat, "Grid Size", GRID_SIZE);
        System.out.printf(BenchmarkRunner.argOutputFormat, "Variant", "20150203");
    }

    protected static void initializeData() {
        if (allNodes == null) {
            allNodes = new GridNode[GRID_SIZE * GRID_SIZE * GRID_SIZE];
            initializeNodes();
            connectNodes();
        }

        // clear distance and parent values
        for (final GridNode gridNode : allNodes) {
            gridNode.distanceFromRoot.set(gridNode.id == 0 ? 0 : Integer.MAX_VALUE);
            gridNode.parentInPath.set(null);
        }
    }

    private static void initializeNodes() {

        final long startTime = System.nanoTime();
        final ExecutorService executorService = Executors.newFixedThreadPool(BenchmarkRunner.numThreads());

        for (int ii = 0; ii < GRID_SIZE; ii++) {

            final int i = ii;
            executorService.submit(new Runnable() {
                @Override
                public void run() {

                    for (int j = 0; j < GRID_SIZE; j++) {
                        for (int k = 0; k < GRID_SIZE; k++) {
                            final int id = (((i * GRID_SIZE) + j) * GRID_SIZE) + k;
                            final GridNode gridNode = new GridNode(id, i, j, k);
                            allNodes[gridNode.id] = gridNode;
                        }
                    }

                }
            });
        }

        executorService.shutdown();
        while (!executorService.isTerminated()) {
            try {
                executorService.awaitTermination(1L, TimeUnit.DAYS);
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }

        final long endTime = System.nanoTime();
        final long execTimeMillis = (long) ((endTime - startTime) / 1e6);
        System.out.println("  Allocated all nodes in " + execTimeMillis + " ms.");
    }

    private static void connectNodes() {

        final int numThreads = BenchmarkRunner.numThreads();
        final long startTime = System.nanoTime();
        final ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        for (int n = 0; n < numThreads; n++) {
            final int increment = allNodes.length / numThreads;
            final int start = n * increment;
            final int end = Math.min(allNodes.length, start + increment);

            executorService.submit(new Runnable() {
                @Override
                public void run() {

                    for (int a = start; a < end; a++) {
                        final GridNode gridNode = allNodes[a];
                        final Random random = new Random(gridNode.id);
                        int iterCount = 0;
                        int neighborCount = 0;
                        for (int i = 0; i < 2; i++) {
                            for (int j = 0; j < 2; j++) {
                                for (int k = 0; k < 2; k++) {

                                    iterCount++;
                                    if (iterCount == 1 || iterCount == 8) {
                                        continue;
                                    }

                                    final boolean addNeighbor = (iterCount == 7 && neighborCount == 0) || random.nextBoolean();
                                    if (addNeighbor) {
                                        final int newI = Math.min(GRID_SIZE - 1, gridNode.i + i);
                                        final int newJ = Math.min(GRID_SIZE - 1, gridNode.j + j);
                                        final int newK = Math.min(GRID_SIZE - 1, gridNode.k + k);

                                        final int newId = (GRID_SIZE * GRID_SIZE * newI) + (GRID_SIZE * newJ) + newK;
                                        final GridNode newNode = allNodes[newId];
                                        neighborCount = addNeighbor(gridNode, neighborCount, newId, newNode);
                                    }

                                }
                            }
                            // add random connections
                            for (int n = 0; n < 10; n++) {
                                final int newId = random.nextInt(allNodes.length - GRID_SIZE);
                                final GridNode newNode = allNodes[newId];
                                neighborCount = addNeighbor(gridNode, neighborCount, newId, newNode);
                            }
                        }
                    }
                }

                private int addNeighbor(final GridNode gridNode, final int neighborCount, final int newId, final GridNode newNode) {
                    final GridNode leftNode = gridNode.id <= newId ? gridNode : newNode;
                    final GridNode rightNode = gridNode.id > newId ? gridNode : newNode;

                    synchronized (leftNode) {
                        synchronized (rightNode) {
                            if (gridNode.addNeighbor(newNode)) {
                                return neighborCount + 1;
                            }
                        }
                    }
                    return neighborCount;
                }
            });

        }

        executorService.shutdown();
        while (!executorService.isTerminated()) {
            try {
                executorService.awaitTermination(1L, TimeUnit.DAYS);
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }

        final long endTime = System.nanoTime();
        final long execTimeMillis = (long) ((endTime - startTime) / 1e6);
        System.out.println("  Connected nodes in " + execTimeMillis + " ms.");
    }

    protected static boolean validate() {

        GridNode parentNode = targetNode();
        while (parentNode.parentInPath.get() != null) {
            parentNode = parentNode.parentInPath.get();
        }

        final GridNode rootNode = allNodes[0];
        return (parentNode == rootNode);
    }

    protected static int priority(final GridNode gridNode, final GridNode targetNode, final GridNode originNode) {
        final int availablePriorities = maxPriority() - minPriority() + 1;
        final double nodeDistanceFromTarget = gridNode.distanceFrom(targetNode);
        final double originDistanceFromTarget = originNode.distanceFrom(targetNode);
        final double resultPriority = (1 - (nodeDistanceFromTarget / originDistanceFromTarget)) * availablePriorities;
        return (int) (resultPriority + minPriority());
    }

    protected static GridNode originNode() {
        return allNodes[0];
    }

    protected static GridNode targetNode() {

        final int axisVal = (int) (0.80 * GRID_SIZE);
        final int targetId = (axisVal * GRID_SIZE * GRID_SIZE) + (axisVal * GRID_SIZE) + axisVal;
        final GridNode gridNode = allNodes[targetId];
        return gridNode;
    }

    protected static final class GridNode {

        public final int id;
        public final int i;
        public final int j;
        public final int k;

        private final List<GridNode> neighbors;
        private final AtomicReference<GridNode> parentInPath;
        // fields used in computing distance
        private AtomicInteger distanceFromRoot;

        public GridNode(final int id, final int i, final int j, final int k) {
            this.id = id;
            this.i = i;
            this.j = j;
            this.k = k;

            this.neighbors = new ArrayList<>();
            distanceFromRoot = new AtomicInteger(id == 0 ? 0 : Integer.MAX_VALUE);
            parentInPath = new AtomicReference<>(null);
        }

        private boolean addNeighbor(final GridNode node) {
            if (node == this) {
                return false;
            }
            if (!neighbors.contains(node)) {
                neighbors.add(node);
                return true;
            }
            return false;
        }

        protected int numNeighbors() {
            return neighbors.size();
        }

        protected GridNode neighbor(final int n) {
            return neighbors.get(n);
        }

        protected boolean tryUpdateParent(final GridNode node) {
            final int distanceFromNode = (int) distanceFrom(node);
            final int newDistance = node.distanceFromRoot.get() + distanceFromNode;

            boolean keepLooping = true;
            while (keepLooping) {
                keepLooping = false;

                final int currentDistance = this.distanceFromRoot.get();
                if (newDistance < currentDistance) {
                    final boolean success = this.distanceFromRoot.compareAndSet(currentDistance, newDistance);
                    if (success) {
                        this.parentInPath.set(node);
                        return true;
                    } else {
                        keepLooping = true;
                    }
                } else {
                    return false;
                }
            }
            return false;
        }

        protected double distanceFrom(final GridNode node) {
            final int iDiff = i - node.i;
            final int jDiff = j - node.j;
            final int kDiff = k - node.k;
            return Math.sqrt((iDiff * iDiff) + (jDiff * jDiff) + (kDiff * kDiff));
        }
    }
}
