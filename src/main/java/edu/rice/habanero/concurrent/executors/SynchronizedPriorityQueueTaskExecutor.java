package edu.rice.habanero.concurrent.executors;

import edu.rice.habanero.concurrent.util.PriorityTaskComparator;
import edu.rice.habanero.concurrent.util.SynchronizedPriorityQueue;

import java.util.concurrent.TimeUnit;

/**
 * @author <a href="http://shams.web.rice.edu/">Shams Imam</a> (shams@rice.edu)
 * @author Vivek Sarkar
 */
public class SynchronizedPriorityQueueTaskExecutor extends AbstractBlockingQueueTaskExecutor {

    private SynchronizedPriorityQueue<Runnable> priorityQueue;

    public SynchronizedPriorityQueueTaskExecutor(
            final int corePoolSize, final int maximumPoolSize,
            final long keepAliveTime, final TimeUnit timeUnit,
            final int minPriorityInc, final int maxPriorityInc) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, timeUnit,
              minPriorityInc, maxPriorityInc);
    }

    @Override
    protected void onShutdown() {
        priorityQueue.terminate();
    }

    @Override
    protected SynchronizedPriorityQueue<Runnable> priorityQueueFactory(final int corePoolSize) {
        priorityQueue = new SynchronizedPriorityQueue<>(2_048, new PriorityTaskComparator());
        return priorityQueue;
    }

}
