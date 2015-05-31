package edu.rice.habanero.benchmarks.trapezoid;

import edu.rice.habanero.benchmarks.BenchmarkRunner;

/**
 * @author <a href="http://shams.web.rice.edu/">Shams Imam</a> (shams@rice.edu)
 */
public final class TrapezoidalConfig {

    protected static int N = 100_000; // num pieces
    protected static double L = 0; // left end-point
    protected static double R = 100; // right end-point
    protected static boolean debug = false;

    protected static void parseArgs(final String[] args) {
        int i = 0;
        while (i < args.length) {
            final String loopOptionKey = args[i];
            switch (loopOptionKey) {
                case "-n":
                    i += 1;
                    N = Integer.parseInt(args[i]);
                    break;
                case "-l":
                    i += 1;
                    L = Double.parseDouble(args[i]);
                    break;
                case "-r":
                    i += 1;
                    R = Double.parseDouble(args[i]);
                    break;
                case "-debug":
                case "-verbose":
                    debug = true;
                    break;
            }
            i += 1;
        }
    }

    protected static void printArgs() {
        System.out.printf(BenchmarkRunner.argOutputFormat, "N (num trapezoids)", N);
        System.out.printf(BenchmarkRunner.argOutputFormat, "L (left end-point)", L);
        System.out.printf(BenchmarkRunner.argOutputFormat, "R (right end-point)", R);
        System.out.printf(BenchmarkRunner.argOutputFormat, "debug", debug);
    }

    protected static double fx(final double x) {

        final double a = Math.sqrt(Math.pow(x, 3) - 1);
        final double b = Math.cosh(x + 1);
        final double c = Math.pow(Math.sinh(a / b), 3.5);
        final double d = Math.sqrt(1 + Math.exp(Math.sqrt(2 * x)));
        final double r = 100.0 * Math.cbrt(c * d);
        return r;
    }
}