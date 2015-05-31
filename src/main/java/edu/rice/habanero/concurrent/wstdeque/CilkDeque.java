package edu.rice.habanero.concurrent.wstdeque;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The Implementation of the Cilk-5 Multithreaded Language. Matteo Frigo, Charles Leiserson, Keith Randall.
 * <p/>
 * Simplified THE Protocol.
 *
 * @author <a href="http://shams.web.rice.edu/">Shams Imam</a> (shams@rice.edu) [current version]
 */
public class CilkDeque implements WorkStealingPool {

    private final AtomicInteger T = new AtomicInteger(0);
    private final AtomicInteger H = new AtomicInteger(0);
    private final ReentrantLock L = new ReentrantLock();
    private volatile CircularArray v;

    public CilkDeque() {
        v = new CircularArray(13);
    }

    @Override
    public boolean isEmpty() {
        final int t = T.get();
        final int h = H.get();
        final int size = t - h;
        return size == 0;
    }

    @Override
    public void pushBottom(final Object f) {
        final int index = T.getAndIncrement();
        CircularArray a = this.v;
        final int t = T.get();
        final int h = H.get();
        final int size = t - h;
        if (size >= a.size() - 1) {
            a = a.grow(t, h);
            this.v = a;
        }
        a.put(index, f);
    }

    @Override
    public Object popBottom() {
        int index = T.decrementAndGet();
        if (H.get() > T.get()) {
            T.incrementAndGet();
            try {
                L.lock();
                index = T.decrementAndGet();
                if (H.get() > T.get()) {
                    T.incrementAndGet();
                    return EMPTY;
                }
            } finally {
                L.unlock();
            }
        }
        return v.get(index);
    }

    @Override
    public Object steal() {
        try {
            L.lock();
            int index = H.getAndIncrement();
            if (H.get() > T.get()) {
                H.decrementAndGet();
                return EMPTY;
            }
            return v.get(index);
        } finally {
            L.unlock();
        }
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
