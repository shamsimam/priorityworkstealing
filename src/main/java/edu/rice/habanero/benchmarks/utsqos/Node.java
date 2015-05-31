package edu.rice.habanero.benchmarks.utsqos;

import java.util.concurrent.ExecutorService;

import static edu.rice.habanero.benchmarks.BenchmarkRunner.maxPriority;
import static edu.rice.habanero.benchmarks.BenchmarkRunner.minPriority;

/**
 * Source: http://cs.anu.edu.au/~vivek/ws-vee-2014/ForkJoin/UTS.java
 */
public final class Node {

    private static int numChildren(final int height, final int type, final Sha1Generator state) { // generic
        int nChildren;

        switch (UtsConfig.treeType) {
            case UtsConfig.BIN:
                nChildren = numChildren_bin(height, state);
                break;
            case UtsConfig.GEO:
                nChildren = numChildren_geo(height, state);
                break;
            case UtsConfig.HYBRID:
                if (height < UtsConfig.shiftDepth * UtsConfig.gen_mx) {
                    nChildren = numChildren_geo(height, state);
                } else {
                    nChildren = numChildren_bin(height, state);
                }
                break;
            default:
                throw new IllegalStateException("Node:numChildren(): Unknown tree type");
        }

        if (height == 0 && type == UtsConfig.BIN) {    // only BIN root can have more than MAX_NUM_CHILDREN
            final int rootBF = (int) Math.ceil(UtsConfig.b_0);
            if (nChildren > rootBF) {
                System.out.println("*** Number of children of root truncated from "
                                           + nChildren + " to " + rootBF);
                nChildren = rootBF;
            }
        } else {
            if (nChildren > UtsConfig.MAX_NUM_CHILDREN) {
                System.out.println("*** Number of children truncated from "
                                           + nChildren + " to " + UtsConfig.MAX_NUM_CHILDREN);
                nChildren = UtsConfig.MAX_NUM_CHILDREN;
            }
        }
        return nChildren;
    }

    private static int numChildren_bin(final int height, final Sha1Generator state) {            // Binomial: distribution is identical below root
        final int nc;
        if (height == 0) {
            nc = (int) Math.floor(UtsConfig.b_0);
        } else if (rng_toProb(state.rand()) < UtsConfig.nonLeafProb) {
            nc = UtsConfig.nonLeafBF;
        } else {
            nc = 0;
        }
        return nc;
    }

    private static int numChildren_geo(final int height, final Sha1Generator state) {            // Geometric: distribution controlled by shape and height
        double b_i = UtsConfig.b_0;
        if (height > 0) {
            switch (UtsConfig.shape_fn) {    // use shape function to compute target b_i
                case UtsConfig.EXPDEC:        // expected size polynomial in height
                    b_i = UtsConfig.b_0 * Math.pow((double) height, -Math.log(UtsConfig.b_0) / Math.log((double) UtsConfig.gen_mx));
                    break;
                case UtsConfig.CYCLIC:        // cyclic tree
                    if (height > 5 * UtsConfig.gen_mx) {
                        b_i = 0.0;
                        break;
                    }
                    b_i = Math.pow(UtsConfig.b_0, Math.sin(UtsConfig.TWO_PI * (double) height / (double) UtsConfig.gen_mx));
                    break;
                case UtsConfig.FIXED:        // identical distribution at all nodes up to max height
                    b_i = (height < UtsConfig.gen_mx) ? UtsConfig.b_0 : 0;
                    break;
                case UtsConfig.LINEAR:        // linear decrease in b_i
                default:
                    b_i = UtsConfig.b_0 * (1.0 - ((double) height / (double) UtsConfig.gen_mx));
                    break;
            }
        }
        final double p = 1.0 / (1.0 + b_i);            // probability corresponding to target b_i
        final int h = state.rand();
        final double u = rng_toProb(h);        // get uniform random number on [0,1)
        final int nChildren = (int) Math.floor(Math.log(1.0 - u) / Math.log(1.0 - p));
        // return max number of children at this cumulative probability
        return nChildren;
    }

    private static double rng_toProb(final int n) {         // convert a random number on [0,2^31) to one on [0.1)
        return ((n < 0) ? 0.0 : ((double) n) / 2147483648.0);
    }

    private static int childCount(final Sha1Generator state, final int type, final int height) {
        return height >= UtsConfig.maxHeight ? 0 : Math.max(1 + (height % 2), numChildren(height, type, state));
    }

    // Node State
    final Sha1Generator state;
    final int type;
    final int height;
    final int nChildren;
    public final int priority;
    final Node[] children;

    Node(final ExecutorService executorService, final int rootID) {                    // root constructor: count the nodes as they are created
        this(executorService, new Sha1Generator(rootID), 0);
    }

    Node(final ExecutorService executorService, final Node parent, final int spawn) {                // child constructor: count the nodes as they are created
        this(executorService, new Sha1Generator(parent.state, spawn), parent.height + 1);
    }

    Node(final ExecutorService executorService, final Sha1Generator state, final int height) {
        this.state = state;
        this.type = UtsConfig.treeType;
        this.height = height;
        this.nChildren = childCount(state, type, height);
        this.children = new Node[this.nChildren];
        this.priority = randomPriority(state);
        if (height == 3) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    initializeChildren(executorService);
                }
            });
        } else {
            initializeChildren(executorService);
        }
    }

    private int randomPriority(final Sha1Generator state) {
        final int randomValue = Math.abs(state.nextrand());
        final int priorityLevels = (maxPriority() - minPriority()) + 1;
        return (randomValue % priorityLevels);
    }

    private void initializeChildren(final ExecutorService executorService) {
        for (int c = 0; c < nChildren; c++) {
            children[c] = new Node(executorService, this, c);
        }
    }

    int numChildren() {
        return nChildren;
    }

    Node child(final int index) {
        return children[index];
    }

    int childType() {             // determine what kind of children this node will have
        switch (type) {
            case UtsConfig.BIN:
                return UtsConfig.BIN;
            case UtsConfig.GEO:
                return UtsConfig.GEO;
            case UtsConfig.HYBRID:
                if (height < UtsConfig.shiftDepth * UtsConfig.gen_mx) {
                    return UtsConfig.GEO;
                } else {
                    return UtsConfig.BIN;
                }
            default:
                throw new IllegalStateException("uts_get_childtype(): Unknown tree type");
        }
    }

    @Override
    public int hashCode() {
        int result = state.hashCode();
        result = 31 * result + type;
        result = 31 * result + height;
        result = 31 * result + nChildren;
        return result;
    }

    @Override
    public boolean equals(final Object other) {
        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        final Node node = (Node) other;

        final boolean heightEqual = height == node.height;
        final boolean numChildrenEqual = nChildren == node.nChildren;
        final boolean typeEqual = type == node.type;
        final boolean stateEqual = state.equals(node.state);

        return (heightEqual && numChildrenEqual && typeEqual && stateEqual);
    }
}