package edu.rice.habanero.concurrent.wstdeque;

/**
 * @author <a href="http://shams.web.rice.edu/">Shams Imam</a> (shams@rice.edu)
 */
public class PriorityQueue implements WorkStealingPool {

    private final java.util.PriorityQueue<Object> queue = new java.util.PriorityQueue<>();

    @Override
    public boolean isEmpty() {
        synchronized (queue) {
            return queue.isEmpty();
        }
    }

    @Override
    public void pushBottom(final Object o) {
        synchronized (queue) {
            queue.offer(o);
        }
    }

    @Override
    public Object popBottom() {
        synchronized (queue) {
            return queue.poll();
        }
    }

    @Override
    public Object steal() {
        synchronized (queue) {
            return queue.poll();
        }
    }
}
