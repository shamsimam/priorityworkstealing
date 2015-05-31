package edu.rice.habanero.benchmarks.utsqos;

/**
 * Source: http://cs.anu.edu.au/~vivek/ws-vee-2014/ForkJoin/UTS.java
 */
public final class Sha1Compiler {
    // internal constants
    private final static int SHA1_DIGEST_SIZE = 20;
    private final static int SHA1_BLOCK_SIZE = 64;
    private final static int SHA1_MASK = SHA1_BLOCK_SIZE - 1;
    // internal rng state
    private int[] digest = new int[SHA1_DIGEST_SIZE / 4];    // 160 bit internal representation
    private int[] msgBlock = new int[SHA1_BLOCK_SIZE / 4];    // 64 byte internal working buffer
    private long count = 0;                    // 64 bit counter of bytes processed

    Sha1Compiler() {
        digest[0] = (int) 0x67452301l;
        digest[1] = (int) 0xefcdab89l;
        digest[2] = (int) 0x98badcfel;
        digest[3] = (int) 0x10325476l;
        digest[4] = (int) 0xc3d2e1f0l;
    }

    public final void hash(final byte[] data, final int length) {
        int bytePos = 0;                // byte position in data[]
        final int pos = (int) (count & SHA1_MASK);    // byte position in msgBlock
        int wordPos = pos >>> 2;            // word position in msgBlock
        int space = SHA1_BLOCK_SIZE - pos;    // bytes left in msgBlock
        int len = length;            // number of bytes left to process in data
        count += len;            // total number of bytes processed since begin
        while (len >= space) {
            for (; wordPos < (SHA1_BLOCK_SIZE >>> 2); bytePos += 4) {    // "int" aligned (byte)memory to (int)memory copy
                msgBlock[wordPos++] = (((int) data[bytePos] & 0xFF) << 24) | (((int) data[bytePos + 1] & 0xFF) << 16) | (((int) data[bytePos + 2] & 0xFF) << 8) | ((int) data[bytePos + 3] & 0xFF);
            }
            compile();
            len -= space;
            space = SHA1_BLOCK_SIZE;
            wordPos = 0;
        }
        for (; bytePos < length; bytePos += 4) {        // this is the "int" aligned (byte)memory to (int)memory copy
            msgBlock[wordPos++] = (((int) data[bytePos] & 0xFF) << 24) | (((int) data[bytePos + 1] & 0xFF) << 16)
                    | (((int) data[bytePos + 2] & 0xFF) << 8) | (((int) data[bytePos + 3] & 0xFF));
        }
    }

    public final void digest(final byte[] output) {
        int i = (int) (count & SHA1_MASK);    // how many bytes already in msgBlock[]?
        msgBlock[i >> 2] &= (int) (0xffffff80l << 8 * (~i & 3));
        msgBlock[i >> 2] |= (int) (0x00000080l << 8 * (~i & 3));
        if (i > SHA1_BLOCK_SIZE - 9) {
            if (i < 60) {
                msgBlock[15] = 0;
            }
            compile();
            i = 0;
        } else {
            i = (i >> 2) + 1;
        }
        while (i < 14) {
            msgBlock[i++] = 0;
        }
        msgBlock[14] = (int) ((count >> 29));
        msgBlock[15] = (int) ((count << 3));
        compile(); // THIS call accounts for 50% of the program execution time...
        for (i = 0; i < SHA1_DIGEST_SIZE; ++i) {
            output[i] = (byte) (digest[i >> 2] >> (8 * (~i & 3)));
        }
    }

    private static int rotl32(final int x, final int n) {
        return ((x) << n) | ((x) >>> (32 - n));
    }

    private static int rotr32(final int x, final int n) {
        return ((x) >>> n) | ((x) << (32 - n));
    }

    private static int bswap_32(final int x) {
        return ((rotr32((x), 24) & (int) 0x00ff00ff) | (rotr32((x), 8) & (int) 0xff00ff00));
    }

    private static void bsw_32(final int[] p, final int n) {
        int _i = n;
        while (_i-- != 0) {
            p[_i] = bswap_32(p[_i]);
        }
    }

    private static int ch(final int x, final int y, final int z) {
        return ((z) ^ ((x) & ((y) ^ (z))));
    }

    private static int parity(final int x, final int y, final int z) {
        return ((x) ^ (y) ^ (z));
    }

    private static int maj(final int x, final int y, final int z) {
        return (((x) & (y)) | ((z) & ((x) ^ (y))));
    }

    private static int hf(final int[] w, final int i, final boolean hf_basic) {
        if (hf_basic) {
            return w[i];
        } else {
            final int x = i & 15;
            w[x] = rotl32(w[((i) + 13) & 15] ^ w[((i) + 8) & 15] ^ w[((i) + 2) & 15] ^ w[(i) & 15], 1);
            return w[x];
        }
    }

    private static void one_cycle(final int[] v, final int a, final int b, final int c,
                                  final int d, final int e, final String f, final int k, final int h) {

        if (f.equals("ch")) {
            v[e] += (rotr32(v[a], 27) + ch(v[b], v[c], v[d]) + k + h);
        } else if (f.equals("maj")) {
            v[e] += (rotr32(v[a], 27) + maj(v[b], v[c], v[d]) + k + h);
        } else if (f.equals("parity")) {
            v[e] += (rotr32(v[a], 27) + parity(v[b], v[c], v[d]) + k + h);
        } else {
            System.out.println("one_cycle(): error as unknown function type -->" + f);
            System.exit(-1);
        }
        v[b] = rotr32(v[b], 2);
    }

    private static void five_cycle(final int[] w, final boolean hf_basic, final int[] v,
                                   final String f, final int k, final int i) {
        one_cycle(v, 0, 1, 2, 3, 4, f, k, hf(w, i, hf_basic));
        one_cycle(v, 4, 0, 1, 2, 3, f, k, hf(w, i + 1, hf_basic));
        one_cycle(v, 3, 4, 0, 1, 2, f, k, hf(w, i + 2, hf_basic));
        one_cycle(v, 2, 3, 4, 0, 1, f, k, hf(w, i + 3, hf_basic));
        one_cycle(v, 1, 2, 3, 4, 0, f, k, hf(w, i + 4, hf_basic));
    }

    private void compile() {
        int v0, v1, v2, v3, v4;
        final int[] v = new int[5];
        System.arraycopy(digest, 0, v, 0, 5);

        five_cycle(msgBlock, true, v, "ch", (int) 0x5a827999, 0);
        five_cycle(msgBlock, true, v, "ch", (int) 0x5a827999, 5);
        five_cycle(msgBlock, true, v, "ch", (int) 0x5a827999, 10);
        one_cycle(v, 0, 1, 2, 3, 4, "ch", (int) 0x5a827999, hf(msgBlock, 15, true));

        one_cycle(v, 4, 0, 1, 2, 3, "ch", (int) 0x5a827999, hf(msgBlock, 16, false));
        one_cycle(v, 3, 4, 0, 1, 2, "ch", (int) 0x5a827999, hf(msgBlock, 17, false));
        one_cycle(v, 2, 3, 4, 0, 1, "ch", (int) 0x5a827999, hf(msgBlock, 18, false));
        one_cycle(v, 1, 2, 3, 4, 0, "ch", (int) 0x5a827999, hf(msgBlock, 19, false));

        five_cycle(msgBlock, false, v, "parity", (int) 0x6ed9eba1, 20);
        five_cycle(msgBlock, false, v, "parity", (int) 0x6ed9eba1, 25);
        five_cycle(msgBlock, false, v, "parity", (int) 0x6ed9eba1, 30);
        five_cycle(msgBlock, false, v, "parity", (int) 0x6ed9eba1, 35);

        five_cycle(msgBlock, false, v, "maj", (int) 0x8f1bbcdc, 40);
        five_cycle(msgBlock, false, v, "maj", (int) 0x8f1bbcdc, 45);
        five_cycle(msgBlock, false, v, "maj", (int) 0x8f1bbcdc, 50);
        five_cycle(msgBlock, false, v, "maj", (int) 0x8f1bbcdc, 55);

        five_cycle(msgBlock, false, v, "parity", (int) 0xca62c1d6, 60);
        five_cycle(msgBlock, false, v, "parity", (int) 0xca62c1d6, 65);
        five_cycle(msgBlock, false, v, "parity", (int) 0xca62c1d6, 70);
        five_cycle(msgBlock, false, v, "parity", (int) 0xca62c1d6, 75);
        digest[0] += v[0];
        digest[1] += v[1];
        digest[2] += v[2];
        digest[3] += v[3];
        digest[4] += v[4];
    }

    static String toHex(final int data) {
        String result = java.lang.Integer.toHexString(data);
        for (int i = result.length(); i < 8; i++) {
            result = "0" + result;
        }
        return result;
    }

    static String toHex(final long data) {
        String result = java.lang.Long.toHexString(data);
        for (int i = result.length(); i < 16; i++) {
            result = "0" + result;
        }
        return result;
    }

    private void showDigest() {
        for (int i = 0; i < 5; i++) {
            System.out.print(toHex(digest[i]) + " ");
        }
        System.out.println(" ");
    }

    private void showMsgBlock() {
        for (int i = 0; i < 16; i++) {
            System.out.print(toHex(msgBlock[i]) + " ");
        }
        System.out.println(" ");
    }
}