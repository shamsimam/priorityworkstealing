package edu.rice.habanero.concurrent.wstdeque;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author <a href="http://shams.web.rice.edu/">Shams Imam</a> (shams@rice.edu)
 */
public class JdkQueue implements WorkStealingPool {

    private final ConcurrentLinkedQueue<Object> queue = new ConcurrentLinkedQueue<>();

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    @Override
    public void pushBottom(final Object o) {
        queue.offer(o);
    }

    @Override
    public Object popBottom() {
        return queue.poll();
    }

    @Override
    public Object steal() {
        return queue.poll();
    }
}
