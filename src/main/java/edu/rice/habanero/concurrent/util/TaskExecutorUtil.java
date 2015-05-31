package edu.rice.habanero.concurrent.util;

import edu.rice.habanero.concurrent.executors.TaskExecutor;

/**
 * @author <a href="http://shams.web.rice.edu/">Shams Imam</a> (shams@rice.edu)
 * @author Vivek Sarkar
 */
public class TaskExecutorUtil {

    private static final ThreadLocal<TaskExecutor> executorTracker = new ThreadLocal<>();

    public static void kernel(final TaskExecutor taskExecutor, final Runnable runnable) {
        final Runnable runnableTask = wrapRunnable(taskExecutor, runnable);
        taskExecutor.submit(runnableTask);
        taskExecutor.triggerShutdown();
        taskExecutor.awaitTermination();
    }

    public static void async(final Runnable runnable) {
        final TaskExecutor taskExecutor = executorTracker.get();
        final Runnable runnableTask = wrapRunnable(taskExecutor, runnable);
        taskExecutor.submit(runnableTask);
    }

    public static void async(final int priority, final Runnable runnable) {
        final TaskExecutor taskExecutor = executorTracker.get();
        final Runnable runnableTask = wrapRunnable(taskExecutor, runnable);
        taskExecutor.submit(priority, runnableTask);
    }

    private static Runnable wrapRunnable(final TaskExecutor taskExecutor, final Runnable runnable) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    executorTracker.set(taskExecutor);
                    runnable.run();
                } catch (final Exception ex) {
                    ex.printStackTrace();
                } finally {
                    executorTracker.set(null);
                }
            }
        };
    }

}
