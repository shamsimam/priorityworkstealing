package edu.rice.habanero.concurrent.executors;

import edu.rice.habanero.concurrent.util.PriorityTaskComparator;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="http://shams.web.rice.edu/">Shams Imam</a> (shams@rice.edu)
 * @author Vivek Sarkar
 */
public class PriorityBlockingQueueTaskExecutor extends AbstractBlockingQueueTaskExecutor {

    public PriorityBlockingQueueTaskExecutor(
            final int corePoolSize, final int maximumPoolSize,
            final long keepAliveTime, final TimeUnit timeUnit,
            final int minPriorityInc, final int maxPriorityInc) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, timeUnit,
              minPriorityInc, maxPriorityInc);
    }

    @Override
    protected void onShutdown() {
        // do nothing
    }

    @Override
    protected PriorityBlockingQueue<Runnable> priorityQueueFactory(final int corePoolSize) {
        return new PriorityBlockingQueue<>(2_048, new PriorityTaskComparator());
    }

}
