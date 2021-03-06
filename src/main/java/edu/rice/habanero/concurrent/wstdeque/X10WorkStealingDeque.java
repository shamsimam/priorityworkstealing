/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2006-2015.
 */
package edu.rice.habanero.concurrent.wstdeque;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * Ported from X10 implementation.
 */
public class X10WorkStealingDeque implements WorkStealingPool {

    /**
     * Capacity of work-stealing queue array upon initialization. Must be a power of two. Initial size must be at least
     * 2, but is padded to minimize cache effects.
     */
    private static final int INITIAL_QUEUE_CAPACITY = 1 << 13;

    /**
     * Maximum work-stealing queue array size.  Must be less than or equal to 1 << 28 to ensure lack of index
     * wraparound. (This is less than usual bounds, because we need leftshift by 3 to be in int range).
     */
    private static final int MAXIMUM_QUEUE_CAPACITY = 1 << 28;

    /**
     * The work-stealing queue array. Size must be a power of two. Initialized when thread starts, to improve memory
     * locality.
     */
    private Object[] queue;

    /**
     * Index (mod queue.length) of next queue slot to push to or pop from. It is written only by owner thread, via
     * ordered store. Both sp and base are allowed to wrap around on overflow, but (sp - base) still estimates size.
     */
    private volatile int sp;

    /**
     * Index (mod queue.length) of least valid queue slot, which is always the next position to steal from if nonempty.
     */
    private volatile int base;

    /**
     * Creates a Deque.
     */
    public X10WorkStealingDeque() {
        // Allocate while starting to improve chances of thread-local
        // isolation
        queue = new Object[INITIAL_QUEUE_CAPACITY];
    }

    // Intrinsics-based support for queue operations.

    /**
     * Add in store-order the given task at given slot of q to null. Caller must ensure q is nonnull and index is in
     * range.
     */
    private void setSlot(final Object[] q, final int i, final Object t) {
        // DAVE G: Egregious hack to get code running on IBM SDKs that don't have putOrderedObject.
        // _unsafe.putOrderedObject(q, (i << qShift) + qBase, t);
        _unsafe.putObject(q, (i << qShift) + qBase, t);
    }

    /**
     * CAS given slot of q to null. Caller must ensure q is nonnull and index is in range.
     */
    private boolean casSlotNull(final Object[] q, final int i, final Object t) {
        return _unsafe.compareAndSwapObject(q, (i << qShift) + qBase, t, null);
    }

    /**
     * Sets sp in store-order.
     */
    private void storeSp(final int s) {
        // DAVE G: Egregious hack to get code running on IBM SDKs that don't have putOrderedInt.
        // _unsafe.putOrderedInt(this, spOffset, s);
        _unsafe.putInt(this, spOffset, s);
    }

    // Main queue methods


    /**
     * Uses an estimate of the number of tasks in the queue.
     */
    @Override
    public boolean isEmpty() {
        // suppress momentarily negative values
        final int n = sp - base;
        return n <= 0;
    }

    /**
     * Pushes a task. Called only by current thread.
     *
     * @param t the task. Caller must ensure nonnull
     */
    @Override
    public final void pushBottom(final Object t) {
        final Object[] q = queue;
        final int mask = q.length - 1;
        int s = sp;
        setSlot(q, s & mask, t);
        storeSp(++s);
        if ((s -= base) == 1) {
            ;
        } else if (s >= mask) {
            growQueue();
        }
    }

    /**
     * Returns a popped task, or EMPTY if empty. Ensures active status if nonnull. Called only by current thread.
     */
    @Override
    public final Object popBottom() {
        final int s = sp;
        while (s != base) {
            final Object[] q = queue;
            final int mask = q.length - 1;
            final int i = (s - 1) & mask;
            final Object t = q[i];
            if (t == null || !casSlotNull(q, i, t)) {
                break;
            }
            storeSp(s - 1);
            return t;
        }
        return EMPTY;
    }

    /**
     * Tries to take a task from the base of the queue, failing if either empty or contended.
     *
     * @return a task, or EMPTY if none or contended.
     */
    @Override
    public final Object steal() {
        final Object t;
        final Object[] q;
        final int i;
        final int b;
        if (sp != (b = base) &&
                (q = queue) != null && // must read q after b
                (t = q[i = (q.length - 1) & b]) != null &&
                casSlotNull(q, i, t)) {
            base = b + 1;
            return t;
        }
        return EMPTY;
    }

    /**
     * Doubles queue array size. Transfers elements by emulating steals (deqs) from old array and placing, oldest first,
     * into new array.
     */
    private void growQueue() {
        final Object[] oldQ = queue;
        final int oldSize = oldQ.length;
        final int newSize = oldSize << 1;
        if (newSize > MAXIMUM_QUEUE_CAPACITY) {
            throw new RuntimeException("Queue capacity exceeded");
        }
        final Object[] newQ = queue = new Object[newSize];

        int b = base;
        final int bf = b + oldSize;
        final int oldMask = oldSize - 1;
        final int newMask = newSize - 1;
        do {
            final int oldIndex = b & oldMask;
            Object t = oldQ[oldIndex];
            if (t != null && !casSlotNull(oldQ, oldIndex, t)) {
                t = null;
            }
            setSlot(newQ, b & newMask, t);
        } while (++b != bf);
    }


    // Temporary Unsafe mechanics for preliminary release

    static final Unsafe _unsafe;
    static final long baseOffset;
    static final long spOffset;
    static final long qBase;
    static final int qShift;

    static {
        try {
            if (X10WorkStealingDeque.class.getClassLoader() != null) {
                final Field f = Unsafe.class.getDeclaredField("theUnsafe");
                f.setAccessible(true);
                _unsafe = (Unsafe) f.get(null);
            } else {
                _unsafe = Unsafe.getUnsafe();
            }
            baseOffset = _unsafe.objectFieldOffset(X10WorkStealingDeque.class.getDeclaredField("base"));
            spOffset = _unsafe.objectFieldOffset(X10WorkStealingDeque.class.getDeclaredField("sp"));
            qBase = _unsafe.arrayBaseOffset(Object[].class);
            final int s = _unsafe.arrayIndexScale(Object[].class);
            if ((s & (s - 1)) != 0) {
                throw new Error("data type scale not a power of two");
            }
            qShift = 31 - Integer.numberOfLeadingZeros(s);
        } catch (final Exception e) {
            throw new RuntimeException("Could not initialize intrinsics", e);
        }
    }

}
