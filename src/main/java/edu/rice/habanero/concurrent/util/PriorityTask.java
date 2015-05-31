package edu.rice.habanero.concurrent.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author <a href="http://shams.web.rice.edu/">Shams Imam</a> (shams@rice.edu)
 * @author Vivek Sarkar
 */
public final class PriorityTask<T> implements Comparable<PriorityTask>, RunnableFuture<T> {

    private final int priority;
    private final Runnable runnable;
    private final ModCountDownLatch latch;

    public PriorityTask(final int priority, final Runnable runnable, final ModCountDownLatch latch) {
        this.priority = priority;
        this.runnable = runnable;
        this.latch = latch;

        this.latch.updateCount();
    }

    @Override
    public void run() {
        try {
            runnable.run();
        } finally {
            latch.countDown();
        }
    }

    @Override
    public int compareTo(final PriorityTask otherTask) {
        return otherTask.priority - priority;
    }

    @Override
    public boolean cancel(final boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException("PriorityTask#cancel(mayInterruptIfRunning)");
    }

    @Override
    public boolean isCancelled() {
        throw new UnsupportedOperationException("PriorityTask#cancel()");
    }

    @Override
    public boolean isDone() {
        throw new UnsupportedOperationException("PriorityTask#isCancelled()");
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        throw new UnsupportedOperationException("PriorityTask#get()");
    }

    @Override
    public T get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        throw new UnsupportedOperationException("PriorityTask#get(timeout, unit)");
    }

    @Override
    public String toString() {
        return "PriorityTask{" +
                "priority=" + priority +
                '}';
    }
}
