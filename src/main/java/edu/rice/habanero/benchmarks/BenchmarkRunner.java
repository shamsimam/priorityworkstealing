package edu.rice.habanero.benchmarks;

import java.util.*;
import java.util.Map.Entry;

/**
 * @author <a href="http://shams.web.rice.edu/">Shams Imam</a> (shams@rice.edu)
 */
public class BenchmarkRunner {

    public static final String statDataOutputFormat = "%23s %20s: %12.3f \n";
    public static final String execTimeOutputFormat = "%23s %20s: %9.3f ms \n";
    public static final String argOutputFormat = "%35s = %-10s \n";
    protected static final double tolerance = 0.20;

    public static int ITERATIONS = 12;
    public static int NUM_THREADS = Runtime.getRuntime().availableProcessors();
    public static int MIN_PRIORITY = Thread.MIN_PRIORITY;
    public static int MAX_PRIORITY = Thread.MAX_PRIORITY;
    public static boolean DEBUG = false;

    public static int numThreads() {
        return NUM_THREADS;
    }

    public static int minPriority() {
        return MIN_PRIORITY;
    }

    public static int maxPriority() {
        return MAX_PRIORITY;
    }

    private static void parseArgs(final String[] args) throws Exception {

        final String numWorkersStr = System.getProperty("hj.numWorkers");
        if (numWorkersStr != null) {
            NUM_THREADS = Integer.parseInt(numWorkersStr);
        }

        for (int i = 0; i < args.length; i++) {
            final String argName = args[i];
            final String argValue = i + 1 >= args.length ? "0" : args[i + 1];

            if ("-iter".equalsIgnoreCase(argName)) {
                ITERATIONS = Integer.parseInt(argValue);
            } else if ("-threads".equalsIgnoreCase(argName)) {
                NUM_THREADS = Integer.parseInt(argValue);
            } else if ("-maxPriority".equalsIgnoreCase(argName)) {
                MAX_PRIORITY = Integer.parseInt(argValue);
            } else if ("-minPriority".equalsIgnoreCase(argName)) {
                MIN_PRIORITY = Integer.parseInt(argValue);
            } else if ("-debug".equalsIgnoreCase(argName) || "-verbose".equalsIgnoreCase(argName)) {
                DEBUG = true;
            }
        }
    }

    private static void printArgs() {
        System.out.printf(BenchmarkRunner.argOutputFormat, "Iterations", ITERATIONS);
        System.out.printf(BenchmarkRunner.argOutputFormat, "Num Threads", NUM_THREADS);
        System.out.printf(BenchmarkRunner.argOutputFormat, "Min Priority", MIN_PRIORITY);
        System.out.printf(BenchmarkRunner.argOutputFormat, "Max Priority", MAX_PRIORITY);
        System.out.printf(BenchmarkRunner.argOutputFormat, "Debug/Verbose", DEBUG);
    }

    public static void runBenchmark(final String[] args, final Benchmark benchmark) {
        try {
            parseArgs(args);
            benchmark.initialize(args);
        } catch (final Exception e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }

        System.out.println("Runtime: " + benchmark.runtimeInfo());
        System.out.println("Benchmark: " + benchmark.name());
        System.out.println("Args: ");
        printArgs();
        benchmark.printArgInfo();
        System.out.println();

        final List<Double> rawExecTimes = new ArrayList<>(ITERATIONS);

        System.out.println("Execution - Iterations: ");
        for (int i = 0; i < ITERATIONS; i++) {
            System.out.println();
            benchmark.preIteration(i == 0);

            final long startTime = System.nanoTime();
            benchmark.runIteration();
            final long endTime = System.nanoTime();

            final double execTimeMillis = (endTime - startTime) / 1e6;
            rawExecTimes.add(execTimeMillis);

            benchmark.cleanupIteration(i + 1 == ITERATIONS, execTimeMillis);
            System.out.printf(execTimeOutputFormat, benchmark.name(), " Iteration-" + i, execTimeMillis);
        }
        System.out.println();

        final Map<String, List<Double>> customAttrs = benchmark.customAttrs;
        if (!customAttrs.isEmpty()) {
            System.out.println("Attributes - Summary: ");
            for (final Entry<String, List<Double>> loopEntry : customAttrs.entrySet()) {
                final String attrName = loopEntry.getKey();
                final List<Double> attrValues = loopEntry.getValue();
                System.out.printf(statDataOutputFormat, benchmark.name(), " " + attrName, arithmeticMean(attrValues));
            }
        }

        Collections.sort(rawExecTimes);
        final List<Double> execTimes = sanitize(rawExecTimes);
        System.out.println("Execution - Summary: ");
        System.out.printf(argOutputFormat, "Total executions", rawExecTimes.size());
        System.out.printf(argOutputFormat, "Filtered executions", execTimes.size());
        System.out.printf(execTimeOutputFormat, benchmark.name(), " Best Time", execTimes.get(0));
        System.out.printf(execTimeOutputFormat, benchmark.name(), " Worst Time", execTimes.get(execTimes.size() - 1));
        System.out.printf(execTimeOutputFormat, benchmark.name(), " Median", median(execTimes));
        System.out.printf(execTimeOutputFormat, benchmark.name(), " Arith. Mean Time", arithmeticMean(execTimes));
        System.out.printf(execTimeOutputFormat, benchmark.name(), " Geo. Mean Time", geometricMean(execTimes));
        System.out.printf(execTimeOutputFormat, benchmark.name(), " Harmonic Mean Time", harmonicMean(execTimes));
        System.out.printf(execTimeOutputFormat, benchmark.name(), " Std. Dev Time", standardDeviation(execTimes));
        System.out.printf(execTimeOutputFormat, benchmark.name(), " Lower Confidence", confidenceLow(execTimes));
        System.out.printf(execTimeOutputFormat, benchmark.name(), " Higher Confidence", confidenceHigh(execTimes));
        System.out.printf(execTimeOutputFormat.trim() + " (%4.3f percent) \n", benchmark.name(), " Error Window",
                          confidenceHigh(execTimes) - arithmeticMean(execTimes),
                          100 * (confidenceHigh(execTimes) - arithmeticMean(execTimes)) / arithmeticMean(execTimes));
        System.out.printf(statDataOutputFormat, benchmark.name(), " Coeff. of Variation", coefficientOfVariation(execTimes));
        System.out.printf(statDataOutputFormat, benchmark.name(), " Skewness", skewness(execTimes));

        System.out.println();
    }

    private static List<Double> sanitize(final List<Double> rawList) {
        if (rawList.isEmpty()) {
            return new ArrayList<>(0);
        }

        Collections.sort(rawList);
        final int rawListSize = rawList.size();

        final List<Double> resultList = new ArrayList<>();
        final double median = rawList.get(rawListSize / 2);
        final double allowedMin = (1 - tolerance) * median;
        final double allowedMax = (1 + tolerance) * median;

        for (final double loopVal : rawList) {
            if (loopVal >= allowedMin && loopVal <= allowedMax) {
                resultList.add(loopVal);
            }
        }
        return resultList;
    }

    private static double arithmeticMean(final Collection<Double> execTimes) {

        double sum = 0;

        for (final double execTime : execTimes) {
            sum += execTime;
        }

        return (sum / execTimes.size());
    }

    private static double median(final List<Double> execTimes) {

        if (execTimes.isEmpty()) {
            return 0;
        }

        final int size = execTimes.size();
        final int middle = size / 2;
        if (size % 2 == 1) {
            return execTimes.get(middle);
        } else {
            return (execTimes.get(middle - 1) + execTimes.get(middle)) / 2.0;
        }
    }

    private static double geometricMean(final Collection<Double> execTimes) {
        double lgProd = 0;

        for (final double execTime : execTimes) {
            lgProd += Math.log10(execTime);
        }

        return Math.pow(10, lgProd / execTimes.size());
    }

    private static double harmonicMean(final Collection<Double> execTimes) {
        double denom = 0;

        for (final double execTime : execTimes) {
            denom += (1 / execTime);
        }

        return (execTimes.size() / denom);
    }

    private static double standardDeviation(final Collection<Double> execTimes) {

        final double mean = arithmeticMean(execTimes);

        double temp = 0;
        for (final double execTime : execTimes) {
            temp += ((mean - execTime) * (mean - execTime));
        }

        return Math.sqrt(temp / execTimes.size());
    }

    private static double coefficientOfVariation(final Collection<Double> execTimes) {
        final double mean = arithmeticMean(execTimes);
        final double sd = standardDeviation(execTimes);

        return (sd / mean);
    }

    private static double confidenceLow(final Collection<Double> execTimes) {
        final double mean = arithmeticMean(execTimes);
        final double sd = standardDeviation(execTimes);

        return mean - (1.96d * sd / Math.sqrt(execTimes.size()));
    }

    private static double confidenceHigh(final Collection<Double> execTimes) {
        final double mean = arithmeticMean(execTimes);
        final double sd = standardDeviation(execTimes);

        return mean + (1.96d * sd / Math.sqrt(execTimes.size()));
    }

    /**
     * Returns the sample Skewness measure of asymmetry of an array of numbers. Source:
     * http://socr.googlecode.com/svn/trunk/SOCR2.0/src/org/jfree/data/statistics/Statistics.java
     *
     * @return the sample Skewness measure of asymmetry of an array of numbers.
     */
    private static double skewness(final List<Double> execTimes) {
        final double mean = arithmeticMean(execTimes);
        final double sd = standardDeviation(execTimes);
        double sum = 0.0;
        int count = 0;
        if (execTimes.size() > 1) {
            for (final Double execTime : execTimes) {
                final double current = execTime;
                final double diff = current - mean;
                sum = sum + diff * diff * diff;
                count++;
            }
            return sum / ((count - 1) * sd * sd * sd);
        } else {
            return 0.0;
        }
    }
}
