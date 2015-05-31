package edu.rice.habanero.concurrent.wstdeque;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Dynamic Circular Work-Stealing Deque. David Chase and Yossi Lev.
 */
public class CircularWorkStealingDeque implements WorkStealingPool {

    private final static int LogInitialSize = 13;

    private volatile int bottom;
    private final AtomicInteger top;

    private volatile CircularArray activeArray;

    protected CircularWorkStealingDeque() {
        super();
        this.bottom = 0;
        this.top = new AtomicInteger(0);
        this.activeArray = new CircularArray(LogInitialSize);
    }

    @Override
    public boolean isEmpty() {
        final int b = this.bottom;
        final int t = this.top.get();
        final int size = b - t;
        return size <= 0;
    }

    @Override
    public void pushBottom(final Object o) {
        final int b = this.bottom;
        final int t = this.top.get();
        CircularArray a = this.activeArray;
        final int size = b - t;
        if (size >= a.size() - 1) {
            a = a.grow(b, t);
            this.activeArray = a;
        }
        a.put(b, o);
        bottom = b + 1;
    }

    @Override
    public Object popBottom() {
        int b = this.bottom;
        final CircularArray a = this.activeArray;
        b = b - 1;
        this.bottom = b;
        final int t = this.top.get();
        final int size = b - t;
        if (size < 0) {
            bottom = t;
            return EMPTY;
        }
        Object o = a.get(b);
        if (size > 0) {
            return o;
        }
        if (!top.compareAndSet(t, t + 1)) {
            o = EMPTY;
        }
        this.bottom = t + 1;
        return o;
    }

    @Override
    public Object steal() {
        final int t = this.top.get();
        final int b = this.bottom;
        final CircularArray a = this.activeArray;
        final long size = b - t;
        if (size <= 0) {
            return EMPTY;
        }
        final Object o = a.get(t);
        if (!top.compareAndSet(t, t + 1)) {
            return EMPTY;
        }
        return o;
    }

    private static class CircularArray {

        private int logArraySize;
        private Object[] segment;

        CircularArray(final int logArraySize) {
            this.logArraySize = logArraySize;
            this.segment = new Object[1 << this.logArraySize];
        }

        int size() {
            return 1 << this.logArraySize;
        }

        Object get(final int i) {
            final int index = i % size();
            final Object result = this.segment[index];
            this.segment[index] = EMPTY;
            return result;
        }

        void put(final int i, final Object o) {
            final int index = i % size();
            this.segment[index] = o;
        }

        CircularArray grow(final int b, final int t) {
            final CircularArray a = new CircularArray(this.logArraySize + 1);
            for (int i = t; i < b; i++) {
                a.put(i, this.get(i));
            }
            return a;
        }
    }

}
