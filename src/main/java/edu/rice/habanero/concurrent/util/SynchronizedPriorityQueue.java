package edu.rice.habanero.concurrent.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author <a href="http://shams.web.rice.edu/">Shams Imam</a> (shams@rice.edu)
 */
public class SynchronizedPriorityQueue<T> implements BlockingQueue<T> {

    private final PriorityQueue<T> priorityQueue;
    private final AtomicBoolean terminated;

    public SynchronizedPriorityQueue(final int initialCapacity, final Comparator<? super T> comparator) {
        this.priorityQueue = new PriorityQueue<>(initialCapacity, comparator);
        this.terminated = new AtomicBoolean(false);
    }

    public void terminate() {
        terminated.set(true);
    }

    @Override
    public boolean add(final T t) {
        synchronized (this) {
            return priorityQueue.add(t);
        }
    }

    @Override
    public boolean offer(final T t) {
        synchronized (this) {
            return priorityQueue.offer(t);
        }
    }

    @Override
    public void put(final T t) throws InterruptedException {
        synchronized (this) {
            priorityQueue.offer(t);
        }
    }

    @Override
    public boolean offer(final T t, final long timeout, final TimeUnit unit) throws InterruptedException {
        synchronized (this) {
            return priorityQueue.offer(t);
        }
    }

    @Override
    public T take() throws InterruptedException {
        while (true) {
            synchronized (this) {
                final T pollElem = priorityQueue.poll();
                if (pollElem != null) {
                    return pollElem;
                }
                if (terminated.get()) {
                    return null;
                }
            }
        }
    }

    @Override
    public T poll(final long timeout, final TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException("poll");
    }

    @Override
    public int remainingCapacity() {
        throw new UnsupportedOperationException("remainingCapacity");
    }

    @Override
    public boolean remove(final Object o) {
        synchronized (this) {
            return priorityQueue.remove(o);
        }
    }

    @Override
    public boolean contains(final Object o) {
        synchronized (this) {
            return priorityQueue.contains(o);
        }
    }

    @Override
    public int drainTo(final Collection<? super T> c) {
        throw new UnsupportedOperationException("drainTo");
    }

    @Override
    public int drainTo(final Collection<? super T> c, final int maxElements) {
        throw new UnsupportedOperationException("drainTo");
    }

    @Override
    public T remove() {
        synchronized (this) {
            return priorityQueue.remove();
        }
    }

    @Override
    public T poll() {
        synchronized (this) {
            return priorityQueue.poll();
        }
    }

    @Override
    public T element() {
        synchronized (this) {
            return priorityQueue.element();
        }
    }

    @Override
    public T peek() {
        synchronized (this) {
            return priorityQueue.peek();
        }
    }

    @Override
    public int size() {
        synchronized (this) {
            return priorityQueue.size();
        }
    }

    @Override
    public boolean isEmpty() {
        synchronized (this) {
            return priorityQueue.isEmpty();
        }
    }

    @Override
    public Iterator<T> iterator() {
        throw new UnsupportedOperationException("iterator");
    }

    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException("toArray");
    }

    @Override
    public <T1> T1[] toArray(final T1[] a) {
        throw new UnsupportedOperationException("toArray");
    }

    @Override
    public boolean containsAll(final Collection<?> c) {
        synchronized (this) {
            return priorityQueue.containsAll(c);
        }
    }

    @Override
    public boolean addAll(final Collection<? extends T> c) {
        synchronized (this) {
            return priorityQueue.addAll(c);
        }
    }

    @Override
    public boolean removeAll(final Collection<?> c) {
        synchronized (this) {
            return priorityQueue.removeAll(c);
        }
    }

    @Override
    public boolean retainAll(final Collection<?> c) {
        synchronized (this) {
            return priorityQueue.retainAll(c);
        }
    }

    @Override
    public void clear() {
        synchronized (this) {
            priorityQueue.clear();
        }
    }
}
