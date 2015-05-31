package edu.rice.habanero.concurrent.wstdeque;

/**
 * @author <a href="http://shams.web.rice.edu/">Shams Imam</a> (shams@rice.edu)
 */
public interface WorkStealingPool {

    Object EMPTY = null;

    boolean isEmpty();

    void pushBottom(Object o);

    Object popBottom();

    Object steal();
}
