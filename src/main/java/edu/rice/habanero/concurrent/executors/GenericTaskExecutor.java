package edu.rice.habanero.concurrent.executors;

import edu.rice.habanero.concurrent.util.ModCountDownLatch;
import edu.rice.habanero.concurrent.util.PriorityTask;

import java.util.concurrent.ExecutorService;

/**
 * @author <a href="http://shams.web.rice.edu/">Shams Imam</a> (shams@rice.edu)
 */
public class GenericTaskExecutor implements TaskExecutor {

    private final int minPriorityInc;
    private final int maxPriorityInc;
    private final ModCountDownLatch countDownLatch;
    private final ExecutorService executor;

    public GenericTaskExecutor(
            final int minPriorityInc, final int maxPriorityInc,
            final ExecutorService executor) {
        this.minPriorityInc = minPriorityInc;
        this.maxPriorityInc = maxPriorityInc;
        this.countDownLatch = new ModCountDownLatch(1);
        this.executor = executor;
    }

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
    }

    @Override
    public void submit(final Runnable runnable) {
        submit(maxPriorityInc, runnable);
    }

    @Override
    public void submit(final int priority, final Runnable runnable) {
        final PriorityTask priorityTask = new PriorityTask(priority, runnable, countDownLatch);
        executor.submit(priorityTask);
    }

}
