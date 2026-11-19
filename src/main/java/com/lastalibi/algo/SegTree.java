package com.lastalibi.algo;

import java.util.Arrays;

/** Iterative min segment tree with point update and range-minimum query. */
public class SegTree {
    private final int n;
    private final int[] t;

    public SegTree(int[] a) {
        n = a.length;
        t = new int[2 * n];
        System.arraycopy(a, 0, t, n, n);
        for (int i = n - 1; i > 0; i--) t[i] = Math.min(t[2 * i], t[2 * i + 1]);
    }

    public void update(int i, int v) {
        i += n;
        t[i] = v;
        for (; i > 1; i >>= 1) t[i >> 1] = Math.min(t[i], t[i ^ 1]);
    }

    /** Minimum over the inclusive range [l, r]. */
    public int min(int l, int r) {
        int res = Integer.MAX_VALUE;
        for (l += n, r += n + 1; l < r; l >>= 1, r >>= 1) {
            if ((l & 1) == 1) res = Math.min(res, t[l++]);
            if ((r & 1) == 1) res = Math.min(res, t[--r]);
        }
        return res;
    }

    @Override public String toString() { return Arrays.toString(t); }
}
