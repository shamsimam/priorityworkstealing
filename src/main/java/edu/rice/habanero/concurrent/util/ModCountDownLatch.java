/*
 *
 * (C) Copyright IBM Corporation 2006
 *
 *  This file is part of Hj Language.
 *
 */
package edu.rice.habanero.concurrent.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * Extension of JCU CountDownLatch to allow for increments (so as to support finish).
 *
 * @author Raj Barik, Vivek Sarkar
 */
public class ModCountDownLatch {

    /**
     * A synchronizer that wraps a single atomic int value to represent state of active activities.
     */
    private final Sync sync;

    /**
     * Constructs a <tt>CountDownLatch</tt> initialized with the given count.
     *
     * @param count the number of times {@link #countDown} must be invoked before threads can pass through {@link
     *              #await}.
     * @throws IllegalArgumentException if <tt>count</tt> is less than zero.
     */
    public ModCountDownLatch(final int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count (" + count + ") is negative");
        }
        this.sync = new Sync(count);
    }

    /**
     * Causes the current thread to wait until the latch has counted down to zero, unless the thread is {@link
     * Thread#interrupt interrupted}.
     * <p/>
     * <p>If the current {@link #getCount count} is zero then this method returns immediately. <p>If the current {@link
     * #getCount count} is greater than zero then the current thread becomes disabled for thread scheduling purposes and
     * lies dormant until one of two things happen: <ul> <li>The count reaches zero due to invocations of the {@link
     * #countDown} method; or <li>Some other thread {@link Thread#interrupt interrupts} the current thread. </ul> <p>If
     * the current thread: <ul> <li>has its interrupted status set on entry to this method; or <li>is {@link
     * Thread#interrupt interrupted} while waiting, </ul> then {@link InterruptedException} is thrown and the current
     * thread's interrupted status is cleared.
     *
     * @throws InterruptedException if the current thread is interrupted while waiting.
     */
    public void await() throws InterruptedException {
        sync.acquireSharedInterruptibly(1);
    }

    /**
     * Causes the current thread to wait until the latch has counted down to zero, unless the thread is {@link
     * Thread#interrupt interrupted}, or the specified waiting time elapses.
     * <p/>
     * <p>If the current {@link #getCount count} is zero then this method returns immediately with the value
     * <tt>true</tt>.
     * <p/>
     * <p>If the current {@link #getCount count} is greater than zero then the current thread becomes disabled for
     * thread scheduling purposes and lies dormant until one of three things happen: <ul> <li>The count reaches zero due
     * to invocations of the {@link #countDown} method; or <li>Some other thread {@link Thread#interrupt interrupts} the
     * current thread; or <li>The specified waiting time elapses. </ul> <p>If the count reaches zero then the method
     * returns with the value <tt>true</tt>. <p>If the current thread: <ul> <li>has its interrupted status set on entry
     * to this method; or <li>is {@link Thread#interrupt interrupted} while waiting, </ul> then {@link
     * InterruptedException} is thrown and the current thread's interrupted status is cleared.
     * <p/>
     * <p>If the specified waiting time elapses then the value <tt>false</tt> is returned. If the time is less than or
     * equal to zero, the method will not wait at all.
     *
     * @param timeout the maximum time to wait
     * @param unit    the time unit of the <tt>timeout</tt> argument.
     * @return <tt>true</tt> if the count reached zero and <tt>false</tt> if the waiting time elapsed before the count
     * reached zero.
     * @throws InterruptedException if the current thread is interrupted while waiting.
     */
    public boolean await(final long timeout, final TimeUnit unit)
            throws InterruptedException {
        return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
    }

    /**
     * Decrements the count of the latch, releasing all waiting threads if the count reaches zero. <p>If the current
     * {@link #getCount count} is greater than zero then it is decremented. If the new count is zero then all waiting
     * threads are re-enabled for thread scheduling purposes. <p>If the current {@link #getCount count} equals zero then
     * nothing happens.
     */
    public void countDown() {
        sync.releaseShared(1);
    }

    /**
     * Updates the counter of the synch variable dynamically Update only if the counter value is > 0
     */
    public void updateCount() {
        sync.updateCount();
    }

    /**
     * Returns the current count. <p>This method is typically used for debugging and testing purposes.
     *
     * @return the current count.
     */
    public long getCount() {
        return sync.getCount();
    }

    /**
     * Returns a string identifying this latch, as well as its state. The state, in brackets, includes the String
     * &quot;Count =&quot; followed by the current count.
     *
     * @return a string identifying this latch, as well as its state
     */
    public String toString() {
        return super.toString() + "[Count = " + sync.getCount() + "]";
    }

    /**
     * Use AQS state to represent the count.
     */
    private static final class Sync extends AbstractQueuedSynchronizer {

        /**
         * Constructor.
         *
         * @param count The initial count.
         */
        private Sync(final int count) {
            setState(count);
        }

        /**
         * @return
         */
        public int getCount() {
            return getState();
        }

        /**
         * Update the state
         */
        public void updateCount() {
            // Increment count -- loop till the update is successful
            // Modeled after tryReleaseShared()
            while (true) {
                int c = getState();
                int nextc = c + 1;
                if (compareAndSetState(c, nextc)) {
                    return; // Success!
                }
            }
        }

        /**
         * @param acquires
         * @return
         */
        public int tryAcquireShared(final int acquires) {
            return getState() == 0 ? 1 : -1;
        }

        /**
         * @param releases
         * @return
         */
        public boolean tryReleaseShared(final int releases) {
            // Decrement count; signal when transition to zero
            while (true) {
                int c = getState();
                if (c == 0) {
                    return false;
                }
                int nextc = c - 1;
                if (compareAndSetState(c, nextc)) {
                    return nextc == 0;
                }
            }
        }
    }

}
