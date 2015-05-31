package edu.rice.habanero.concurrent.wstdeque;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Source: http://badamczewski.blogspot.com/2012/08/lock-free-work-stealing-queue.html
 *
 * @author Bartosz Adamczewski
 * @author <a href="http://shams.web.rice.edu/">Shams Imam</a> (shams@rice.edu) [translated to Java]
 */
public class LockFreeWorkStealingQueue implements WorkStealingPool {

    /*
    * You may ask yourself why volatile is here, well the
    * main reason for this when we don't do explicit locking
    * we don't get the memory barier safety so instructions
    * might get reordered.
    *
    * NOTE: having volatile code in here is just a workaround
    * as we get a performance hit, instead we need to put mem bariers
    * when they are actually needed!
    */
    private final AtomicReference<Node> head;
    private final AtomicReference<Node> tail;

    protected LockFreeWorkStealingQueue() {
        tail = new AtomicReference<>(new Node());
        head = new AtomicReference<>(tail.get());
    }

    /**
     * Gets the Unsafe Count (A count that will not necessarily provide the correct actual value). This property is very
     * handy when trying to issue a steal depending on a certain window.
     */
    @Override
    public boolean isEmpty() {
        final int count = tail.get().id - head.get().id;
        return count <= 0;
    }

    public int count() {
        final int[] count = {0};
        evaluateCount(new IntPredicate() {
            @Override
            public boolean apply(final int x) {
                return false;
            }
        }, count);
        return count[0];
    }

    /**
     * Starts counting nodes utils a certain condition has been met.
     *
     * @param value       the condition
     * @param actualCount the actual counted number of elements.
     * @return the value indication that the condition was met or not.
     */
    private boolean evaluateCount(final IntPredicate value, final int[] actualCount) {
        int count = 0;
        for (Node current = head.get().next.get();
             current != null; current = current.next.get()) {
            count++;

            if (value.apply(count)) {
                actualCount[0] = count;
                return true;
            }
        }
        actualCount[0] = count;
        return false;
    }

    /**
     * Get's the value indicating if the Queue is empty.
     */
    private boolean IsEmpty() {
        return head.get().next == null;
    }

    /**
     * Get's the tail.
     * <p/>
     * In order to achieve correctness we need to keep track of the tail, accessing tail.next will not do as some other
     * thread might just moved it so in order to catch the tail we need to do a subtle form of a spin lock that will use
     * CompareAndSet atomic instruction ( Interlocked.CompareExchange ) and set ourselves to the tail if it had been
     * moved.
     */
    private Node GetTail() {
        Node localTail = tail.get();
        Node localNext = localTail.next.get();

        //if some other thread moved the tail we need to set to the right possition.
        while (localNext != null) {
            //set the tail.
            tail.compareAndSet(localTail, localNext);
            localTail = tail.get();
            localNext = localTail;
        }

        return tail.get();
    }

    /**
     * Attempts to reset the Couner id.
     */
    private void TryResetCounter() {
        if (tail.get().id >= Integer.MAX_VALUE) {
            final int res = (tail.get().id - head.get().id);
            head.get().id = 0;
            tail.get().id = res;
        }
    }

    /**
     * Puts a new item on the Queue.
     *
     * @param obj The value to be queued.
     */
    public void pushBottom(final Object obj) {
        Node localTail = null;
        final Node newNode = new Node();
        newNode.val = obj;

        TryResetCounter();

        do {
            //get the tail.
            localTail = GetTail();

            //TODO: This should be atomic.
            newNode.next.set(localTail.next.get());
            newNode.id = localTail.id + 1;
            newNode.prev.set(localTail);

            // if we arent null, then this means that some other
            // thread interfered with our plans (sic!) and we need to
            // start over.
        } while (localTail.next.compareAndSet(null, newNode));


        // if we finally are at the tail and we are the same,
        // then we switch the values to the new node, phew! :)
        tail.compareAndSet(localTail, newNode);
    }

    public Object popBottom() {
        // keep spinning until we catch the proper head.
        while (true) {
            final Node localHead = head.get();
            final Node localNext = localHead.next.get();
            final Node localTail = tail.get();

            // if the queue is empty then return the default
            if (localNext == null) {
                return WorkStealingPool.EMPTY;
            } else if (localHead == localTail) {
                // our tail is lagging behind so we need to swing it.
                tail.compareAndSet(localTail, localHead);
            } else {
                localNext.prev.set(localHead.prev.get());
                // if no other thread changed the head then we are good to
                // go and we can return the local value;
                if (!head.compareAndSet(localHead, localNext)) {
                    return localNext.val;
                }
            }
        }
    }

    public Object steal() {
        Node localTail;
        Node localPrev;
        final Node swapNode = new Node();

        do {
            //get the tail.
            localTail = GetTail();
            localPrev = localTail.prev.get();

            if (localPrev == null) {
                return WorkStealingPool.EMPTY;
            } else if (localPrev.prev == null) {
                return WorkStealingPool.EMPTY;
            } else if (localPrev.prev == head) {
                return WorkStealingPool.EMPTY;
            } else if (localTail == null) {
                return WorkStealingPool.EMPTY;
            }

            // Set the swap node values that will exchange the element
            // in a sense that it will skip right through it.
            swapNode.next.set(localTail.next.get());
            swapNode.prev.set(localPrev.prev.get());
            swapNode.val = localPrev.val;
            swapNode.id = localPrev.id;
        }
        // In order for this to be actualy *thread safe* we need to subscribe ourselfs
        // to the same logic as the enque and create a blockade by setting the next value
        // of the tail!
        while (localTail.next.compareAndSet(null, localTail));


        // do a double exchange, if we get interrupted between we should be still fine as,
        // all we need to do after the first echange is to swing the prev element to point at the
        // correct tail.
        tail.compareAndSet(tail.get(), swapNode);
        tail.get().prev.compareAndSet(tail.get().prev.get().next.get(), swapNode);

        return localTail.val;
    }


    private static interface IntPredicate {
        boolean apply(int input);
    }

    /**
     * Internal node class for the use of internal double linked list structure.
     */
    private static class Node {
        public Object val;
        public final AtomicReference<Node> next = new AtomicReference<Node>();
        public final AtomicReference<Node> prev = new AtomicReference<Node>();
        public int id;
    }

}
