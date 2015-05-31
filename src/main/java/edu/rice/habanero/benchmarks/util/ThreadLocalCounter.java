package edu.rice.habanero.benchmarks.util;

import java.util.Vector;

/**
 * @author <a href="http://shams.web.rice.edu/">Shams Imam</a> (shams@rice.edu)
 */
public class ThreadLocalCounter {

    private static class LongCounter {
        public long counter = 0;
    }

    private final Vector<LongCounter> fragments = new Vector<>();
    private final ThreadLocal<LongCounter> counter = new ThreadLocal<LongCounter>() {
        @Override
        protected LongCounter initialValue() {
            final LongCounter longCounter = new LongCounter();
            fragments.add(longCounter);
            return longCounter;
        }
    };

    public void increment() {
        final LongCounter longCounter = counter.get();
        longCounter.counter++;
    }

    public void increment(final long value) {
        final LongCounter longCounter = counter.get();
        longCounter.counter += value;
    }

    public long get() {
        long result = 0;
        for (final LongCounter longCounter : fragments) {
            result += longCounter.counter;
        }
        return result;
    }

    public void reset() {
        for (final LongCounter longCounter : fragments) {
            longCounter.counter = 0;
        }
    }

}