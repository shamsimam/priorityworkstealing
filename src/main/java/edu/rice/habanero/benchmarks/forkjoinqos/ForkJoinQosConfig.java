package edu.rice.habanero.benchmarks.forkjoinqos;

import edu.rice.habanero.benchmarks.BenchmarkRunner;

/**
 * @author <a href="http://shams.web.rice.edu/">Shams Imam</a> (shams@rice.edu)
 */
public final class ForkJoinQosConfig {

    protected static int N = 40_000;
    protected static int L = 10_000;

    protected static void parseArgs(final String[] args) {
        int i = 0;
        while (i < args.length) {
            final String loopOptionKey = args[i];
            if ("-n".equals(loopOptionKey)) {
                i += 1;
                N = Integer.parseInt(args[i]);
            } else if ("-l".equals(loopOptionKey)) {
                i += 1;
                L = Integer.parseInt(args[i]);
            }
            i += 1;
        }
    }

    protected static void printArgs() {
        System.out.printf(BenchmarkRunner.argOutputFormat, "N (total tasks)", N);
        System.out.printf(BenchmarkRunner.argOutputFormat, "L (exec. tasks)", L);
    }

    protected static void performComputation(final double theta) {
        final double sint = Math.sin(theta);
        final double res = sint * sint;
        // defeat dead code elimination
        if (res <= 0) {
            throw new IllegalStateException("Benchmark exited with unrealistic res value " + res);
        }
    }
}
