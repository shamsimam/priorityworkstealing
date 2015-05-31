package edu.rice.habanero.concurrent.executors;

/**
 * @author <a href="http://shams.web.rice.edu/">Shams Imam</a> (shams@rice.edu)
 */
public interface TaskExecutor {

    void submit(Runnable task);

    void submit(int priority, Runnable task);

    void triggerShutdown();

    void awaitTermination();
}
