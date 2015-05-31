package edu.rice.habanero.concurrent.wstdeque;

/**
 * @author <a href="http://shams.web.rice.edu/">Shams Imam</a> (shams@rice.edu)
 */
public enum PoolType {
    CIRCULAR() {
        @Override
        public WorkStealingPool newInstance() {
            return new CircularWorkStealingDeque();
        }
    },
    CILK() {
        @Override
        public WorkStealingPool newInstance() {
            return new CilkDeque();
        }
    },
    JDK() {
        @Override
        public WorkStealingPool newInstance() {
            return new JdkQueue();
        }
    },
    LOCK_FREE() {
        @Override
        public WorkStealingPool newInstance() {
            return new LockFreeWorkStealingQueue();
        }
    },
    PRIORITY_QUEUE() {
        @Override
        public WorkStealingPool newInstance() {
            return new PriorityQueue();
        }

        @Override
        public boolean supportsPriority() {
            return true;
        }
    },
    X10() {
        @Override
        public WorkStealingPool newInstance() {
            return new X10WorkStealingDeque();
        }
    };

    public abstract WorkStealingPool newInstance();

    public boolean supportsPriority() {
        return false;
    }
}
