package edu.rice.habanero.benchmarks.utsqos;

import java.util.Arrays;

/**
 * Source: http://cs.anu.edu.au/~vivek/ws-vee-2014/ForkJoin/UTS.java
 */
public final class Sha1Generator {
    // internal constants
    private final static int POS_MASK = 0x7fffffff;
    private final static int LOWBYTE = 0xFF;
    private final static int SHA1_DIGEST_SIZE = 20;

    // internal rng state
    private final byte[] state = new byte[SHA1_DIGEST_SIZE];    // 160 bit output representation

    // new rng from seed
    public Sha1Generator(final int seedarg) {
        final byte[] seedstate = new byte[20];
        for (int i = 0; i < 16; i++) {
            seedstate[i] = 0;
        }
        seedstate[16] = (byte) (LOWBYTE & (seedarg >>> 24));
        seedstate[17] = (byte) (LOWBYTE & (seedarg >>> 16));
        seedstate[18] = (byte) (LOWBYTE & (seedarg >>> 8));
        seedstate[19] = (byte) (LOWBYTE & (seedarg));
        final Sha1Compiler sha1 = new Sha1Compiler();
        sha1.hash(seedstate, 20);
        sha1.digest(state);
    }

    // New rng from existing rng
    public Sha1Generator(final Sha1Generator parent, final int spawnnumber) {
        final byte[] seedstate = new byte[4];
        seedstate[0] = (byte) (LOWBYTE & (spawnnumber >>> 24));
        seedstate[1] = (byte) (LOWBYTE & (spawnnumber >>> 16));
        seedstate[2] = (byte) (LOWBYTE & (spawnnumber >>> 8));
        seedstate[3] = (byte) (LOWBYTE & (spawnnumber));
        final Sha1Compiler sha1 = new Sha1Compiler();
        sha1.hash(parent.state, 20);
        sha1.hash(seedstate, 4);
        sha1.digest(state);
    }

    // Return next random number
    public final int nextrand() {
        int d;
        final Sha1Compiler sha1 = new Sha1Compiler();
        sha1.hash(state, 20);
        sha1.digest(state);
        return POS_MASK & (((LOWBYTE & (int) state[16]) << 24) | ((LOWBYTE & (int) state[17]) << 16)
                | ((LOWBYTE & (int) state[18]) << 8) | ((LOWBYTE & (int) state[19])));
    }

    // return current random number (no advance)
    public final int rand() {
        int d;
        return POS_MASK & (((LOWBYTE & (int) state[16]) << 24) | ((LOWBYTE & (int) state[17]) << 16)
                | ((LOWBYTE & (int) state[18]) << 8) | ((LOWBYTE & (int) state[19])));
    }

    // describe the state of the RNG
    public String showstate() {
        final String[] hex = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F"};
        String sha1state = "SHA1 state=|";
        for (int i = 0; i < 20; i++) {
            sha1state += hex[((state[i] >> 4) & 0x0F)];
            sha1state += hex[((state[i] >> 0) & 0x0F)];
            sha1state += "|";
        }
        return sha1state;
    }

    // describe the RNG
    public String showtype() {
        return ("SHA-1 160 bits");
    }

    @Override
    public boolean equals(final Object o) {

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Sha1Generator that = (Sha1Generator) o;
        return Arrays.equals(state, that.state);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(state);
    }
}