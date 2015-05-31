package edu.rice.habanero.benchmarks.nqueens;

import edu.rice.habanero.benchmarks.Benchmark;
import edu.rice.habanero.benchmarks.BenchmarkRunner;

/**
 * @author <a href="http://shams.web.rice.edu/">Shams Imam</a> (shams@rice.edu)
 */
public final class NQueensConfig {

    protected static final long[] SOLUTIONS = {
            1,
            0,
            0,
            2,
            10,     /* 5 */
            4,
            40,
            92,
            352,
            724,    /* 10 */
            2680,
            14200,
            73712,
            365596,
            2279184, /* 15 */
            14772512,
            95815104,
            666090624,
            4968057848L,
            39029188884L, /* 20 */
    };
    private static final int MAX_SOLUTIONS = SOLUTIONS.length;

    protected static int NUM_WORKERS = 20;
    protected static int SIZE = 12;
    protected static int THRESHOLD = 13;
    protected static int PRIORITIES = 10;
    protected static long SOLUTIONS_LIMIT = 6_000;
    protected static int granularity = 25;
    protected static boolean count = false;

    protected static void parseArgs(final String[] args) {
        int i = 0;
        while (i < args.length) {
            final String loopOptionKey = args[i];
            switch (loopOptionKey) {
                case "-n":
                    i += 1;
                    SIZE = Math.max(1, Math.min(Integer.parseInt(args[i]), MAX_SOLUTIONS));
                    break;
                case "-t":
                    i += 1;
                    THRESHOLD = Math.max(1, Math.min(Integer.parseInt(args[i]), MAX_SOLUTIONS));
                    break;
                case "-w":
                    i += 1;
                    NUM_WORKERS = Integer.parseInt(args[i]);
                    break;
                case "-s":
                    i += 1;
                    SOLUTIONS_LIMIT = Long.parseLong(args[i]);
                    break;
                case "-g":
                    i += 1;
                    granularity = Integer.parseInt(args[i]);
                    break;
                case "-count":
                case "-work":
                    count = true;
                    break;
            }
            i += 1;
        }

        THRESHOLD = Math.min(THRESHOLD, SIZE - 1);
    }

    protected static void printArgs() {
        System.out.printf(BenchmarkRunner.argOutputFormat, "Num Workers", NUM_WORKERS);
        System.out.printf(BenchmarkRunner.argOutputFormat, "Size", SIZE);
        System.out.printf(BenchmarkRunner.argOutputFormat, "Max Solutions", SOLUTIONS_LIMIT);
        System.out.printf(BenchmarkRunner.argOutputFormat, "Threshold", THRESHOLD);
        System.out.printf(BenchmarkRunner.argOutputFormat, "Priorities", PRIORITIES);
        System.out.printf(BenchmarkRunner.argOutputFormat, "Granularity", granularity);
    }

    protected static int[] extendRight(final int[] src, final int newValue) {
        final int[] res = new int[src.length + 1];

        // array too small for JNI crossing (System.arraycopy)
        for (int i = 0; i < src.length; i++) {
            res[i] = src[i];
        }
        res[src.length] = newValue;

        return res;
    }

    /*
     * <a> contains array of <n> queen positions.  Returns 1
     * if none of the queens conflict, and returns 0 otherwise.
     */
    protected static boolean boardValid(final int n, final int[] a) {
        int i, j;
        int p, q;

        for (i = 0; i < n; i++) {
            p = a[i];

            for (j = (i + 1); j < n; j++) {
                q = a[j];
                if (q == p || q == p - (j - i) || q == p + (j - i)) {
                    return false;
                }
            }
        }

        return true;
    }

    public static void validate(final Benchmark benchmark, final long result) {
        System.out.printf(BenchmarkRunner.argOutputFormat, "Solutions found", result);
        final long expectedSolutions = SOLUTIONS[SIZE - 1];
        if (result != expectedSolutions && SOLUTIONS_LIMIT >= expectedSolutions) {
            System.out.println("ERROR: Expected " + expectedSolutions + " actual: " + result);
        } else if (SOLUTIONS_LIMIT < expectedSolutions && (result < SOLUTIONS_LIMIT || result > expectedSolutions)) {
            System.out.println("ERROR: Expected at least " + SOLUTIONS_LIMIT + " actual: " + result);
        }
    }
}
