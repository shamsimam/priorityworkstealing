package edu.rice.habanero.concurrent.util;

import java.util.Comparator;

/**
 * @author <a href="http://shams.web.rice.edu/">Shams Imam</a> (shams@rice.edu)
 * @author Vivek Sarkar
 */
public class PriorityTaskComparator implements Comparator<Runnable> {
    @Override
    public int compare(final Runnable left, final Runnable right) {
        final PriorityTask leftTask = (PriorityTask) left;
        final PriorityTask rightTask = (PriorityTask) right;
        return leftTask.compareTo(rightTask);
    }
}
