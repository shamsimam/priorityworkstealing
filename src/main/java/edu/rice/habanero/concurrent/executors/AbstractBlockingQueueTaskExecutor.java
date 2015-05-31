package edu.rice.habanero.concurrent.executors;

import edu.rice.habanero.concurrent.util.ModCountDownLatch;
import edu.rice.habanero.concurrent.util.PriorityTask;

import java.util.concurrent.*;

/**
 * @author <a href="http://shams.web.rice.edu/">Shams Imam</a> (shams@rice.edu)
 */
public abstract class AbstractBlockingQueueTaskExecutor implements TaskExecutor {

    protected final int minPriorityInc;
    protected final int maxPriorityInc;
    protected final ModCountDownLatch countDownLatch;
    protected final ExecutorService executor;

    public AbstractBlockingQueueTaskExecutor(
            final int corePoolSize, final int maximumPoolSize,
            final long keepAliveTime, final TimeUnit timeUnit,
            final int minPriorityInc, final int maxPriorityInc) {

        this.minPriorityInc = minPriorityInc;
        this.maxPriorityInc = maxPriorityInc;
        this.countDownLatch = new ModCountDownLatch(1);
        this.executor = executorServiceFactory(
                corePoolSize, maximumPoolSize, keepAliveTime, timeUnit,
                minPriorityInc, maxPriorityInc, countDownLatch);
    }

    protected ExecutorService executorServiceFactory(
            final int corePoolSize, final int maximumPoolSize,
            final long keepAliveTime, final TimeUnit timeUnit,
            final int minPriorityInc, final int maxPriorityInc,
            final ModCountDownLatch countDownLatch) {
        final BlockingQueue<Runnable> workQueue = priorityQueueFactory(corePoolSize);
        return new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, timeUnit, workQueue) {
            @Override
            protected <T> RunnableFuture<T> newTaskFor(final Runnable runnable, final T value) {
                if (runnable instanceof PriorityTask) {
                    @SuppressWarnings("unchecked")
                    final RunnableFuture<T> runnableFuture = (RunnableFuture<T>) runnable;
                    return runnableFuture;
                } else {
                    final int priority = (maxPriorityInc + minPriorityInc) / 2;
                    return new PriorityTask<T>(priority, runnable, countDownLatch);
                }
            }
        };
    }

    protected abstract BlockingQueue<Runnable> priorityQueueFactory(int corePoolSize);

    @Override
    public void triggerShutdown() {
        countDownLatch.countDown();
    }

    @Override
    public void awaitTermination() {
        boolean keepLooping = true;
        while (keepLooping) {
            try {
                countDownLatch.await();
                keepLooping = false;
            } catch (final InterruptedException ex) {
                // do nothing
            }
        }
        executor.shutdown();
        onShutdown();
    }

    protected abstract void onShutdown();

    @Override
    public void submit(final Runnable runnable) {
        final int priority = (maxPriorityInc + minPriorityInc) / 2;
        submit(priority, runnable);
    }

    @Override
    public void submit(final int priority, final Runnable runnable) {
        final int taskPriority = Math.max(minPriorityInc, Math.min(priority, maxPriorityInc));
        final PriorityTask priorityTask = new PriorityTask(taskPriority, runnable, countDownLatch);
        executor.submit(priorityTask);
    }
}
