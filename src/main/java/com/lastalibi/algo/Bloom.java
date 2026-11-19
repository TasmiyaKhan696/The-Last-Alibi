package com.lastalibi.algo;

/** Bloom filter: no false negatives, tunable false-positive rate. */
public class Bloom {
    private final int m, k;
    private final boolean[] b;
    private int n = 0;

    public Bloom(int m, int k) { this.m = m; this.k = k; b = new boolean[m]; }

    private int h(int x, int i) {
        long a = (x * 0x9E3779B1) & 0xFFFFFFFFL;
        long bb = (((x ^ (x >>> 15)) * 0x85EBCA6B) & 0xFFFFFFFFL) | 1;
        return (int) (((a + i * bb) & 0xFFFFFFFFL) % m);
    }

    public void add(int x) { for (int i = 0; i < k; i++) b[h(x, i)] = true; n++; }

    public boolean has(int x) {
        for (int i = 0; i < k; i++) if (!b[h(x, i)]) return false;
        return true;
    }

    /** Theoretical false-positive probability. */
    public double fp() { return Math.pow(1 - Math.exp(-(double) k * n / m), k); }
}
