package edu.rice.habanero.benchmarks.utsqos;

import edu.rice.habanero.benchmarks.BenchmarkRunner;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Unbalanced Cobwebbed Tree benchmark.
 *
 * @author <a href="http://shams.web.rice.edu/">Shams Imam</a> (shams@rice.edu)
 */
public final class UtsConfig {

    // TREE TYPE AND SHAPE CONSTANTS
    protected static final int BIN = 0;                // TYPE: binomial tree
    protected static final int GEO = 1;                // TYPE: geometric tree
    protected static final int HYBRID = 2;                // TYPE: hybrid tree, start geometric, shift to binomial

    protected static final int LINEAR = 0;                // SHAPE: linearly decreasing geometric tree
    protected static final int EXPDEC = 1;                // SHAPE: exponentially decreasing geometric tree
    protected static final int CYCLIC = 2;                // SHAPE: cyclic geometric tree
    protected static final int FIXED = 3;                // SHAPE: fixed branching factor geometric tree

    protected static final int UNSETI = -1;                // sentinel for unset integer values
    protected static final double UNSETD = -1.0;            // sentinel for unset double values

    // misc constants
    protected static final double TWO_PI = 2.0 * Math.PI;
    protected static final int MAX_NUM_CHILDREN = 10;        // max number of children for BIN tree

    // UTS parameters and defaults
    protected static int treeType = GEO;            // UTS Type: Default = GEO
    protected static int shape_fn = LINEAR;            // GEOMETRIC TREE: shape function: Default = LINEAR
    protected static boolean debug = false;

    protected static String type = "T3";
    protected static int maxHeight = 10;
    protected static int L = 100_000;

    protected static double b_0 = 4.0;            // branching factor for root node
    protected static int rootId = 0;            // RNG seed for root node
    protected static int nonLeafBF = 4;            // BINOMIAL TREE: branching factor for nonLeaf nodes
    protected static double nonLeafProb = 15.0 / 64.0;        // BINOMIAL TREE: probability a node is a nonLeaf
    protected static int gen_mx = 6;            // GEOMETRIC TREE: maximum number of generations
    protected static double shiftDepth = 0.5;            // HYBRID TREE: Depth fraction for shift from GEO to BIN

    protected static Node rootNode = null;

    protected static void parseArgs(final String[] args) {

        int i = 0;
        while (i < args.length) {
            final String loopOptionKey = args[i];

            switch (loopOptionKey) {
                case "-type":
                    i += 1;
                    type = args[i];
                    break;
                case "-m":
                    i += 1;
                    maxHeight = Integer.parseInt(args[i]);
                    break;
                case "-l":
                    i += 1;
                    L = Integer.parseInt(args[i]);
                    break;
                case "-debug":
                case "-verbose":
                    debug = true;
                    break;
            }

            i += 1;
        }

        final long startTime = System.nanoTime();
        initialize(type);
        final long endTime = System.nanoTime();

        final double execTimeMillis = (endTime - startTime) / 1e6;
        System.out.printf(BenchmarkRunner.argOutputFormat, "Tree Generation", execTimeMillis);
    }

    private static void initialize(final String type) {

        if ("T1".equalsIgnoreCase(type)) {
            // T1="-t 1 -a 3 -d 10 -b 4 -r 19"
            rootId = 19;
            treeType = 1;
            shape_fn = 3;
            b_0 = 4;
            gen_mx = 10;
        } else if ("T1L".equalsIgnoreCase(type)) {
            // T1L="-t 1 -a 3 -d 13 -b 4 -r 29"
            rootId = 29;
            treeType = 1;
            shape_fn = 3;
            b_0 = 4;
            gen_mx = 13;
        } else if ("T2".equalsIgnoreCase(type)) {
            // T2="-t 1 -a 2 -d 16 -b 6 -r 502"
            rootId = 502;
            treeType = 1;
            shape_fn = 2;
            b_0 = 6;
            gen_mx = 16;
        } else if ("T3".equalsIgnoreCase(type)) {
            // T3="-t 0 -b 2000 -q 0.124875 -m 8 -r 42"
            rootId = 42;
            nonLeafProb = 0.124875;
            nonLeafBF = 8;
            treeType = 0;
            b_0 = 2000;
        } else if ("T4".equalsIgnoreCase(type)) {
            // T4="-t 2 -a 0 -d 16 -b 6 -r 1 -q 0.234375 -m 4"
            rootId = 1;
            nonLeafProb = 0.234375;
            nonLeafBF = 4;
            treeType = 2;
            shape_fn = 0;
            b_0 = 6;
            gen_mx = 16;
        } else if ("T5".equalsIgnoreCase(type)) {
            // T5="-t 1 -a 0 -d 20 -b 4 -r 34"
            rootId = 34;
            treeType = 1;
            shape_fn = 0;
            b_0 = 4;
            gen_mx = 20;
        } else {
            throw new IllegalStateException("Unsupported type: " + type);
        }

        gen_mx = maxHeight;

        final ThreadPoolExecutor executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        rootNode = new Node(executorService, rootId);
        executorService.shutdown();
        while (!executorService.isTerminated()) {
            try {
                executorService.awaitTermination(1, TimeUnit.DAYS);
            } catch (final Exception e) {
                // ignore
            }
        }

        System.out.printf(BenchmarkRunner.argOutputFormat, "Tree Tasks", executorService.getCompletedTaskCount());
    }

    protected static void printArgs() {
        System.out.printf(BenchmarkRunner.argOutputFormat, "Type", type);
        System.out.printf(BenchmarkRunner.argOutputFormat, "Max Height", maxHeight);
        System.out.printf(BenchmarkRunner.argOutputFormat, "Root Id", rootId);
        System.out.printf(BenchmarkRunner.argOutputFormat, "Tree type", treeType);
        System.out.printf(BenchmarkRunner.argOutputFormat, "Total nodes", countNodes(rootNode));
        System.out.printf(BenchmarkRunner.argOutputFormat, "Limit on exec. tasks", L);
        System.out.printf(BenchmarkRunner.argOutputFormat, "debug", debug);
    }

    private static int countNodes(final Node node) {

        int result = 0;

        final int numChildren = node.numChildren();
        for (int n = 0; n < numChildren; n++) {
            final Node childNode = node.child(n);
            result += countNodes(childNode);
        }

        return 1 + result;
    }
}