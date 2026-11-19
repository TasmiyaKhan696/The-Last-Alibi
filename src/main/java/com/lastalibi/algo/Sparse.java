package com.lastalibi.algo;

import java.util.*;

/** Sparse table: O(n log n) build, O(1) range-minimum query on a static array. */
public class Sparse {
    private final int[] lg;
    private final List<int[]> st = new ArrayList<>();

    public Sparse(int[] a) {
        int n = a.length, K = 1;
        while ((1 << K) <= n) K++;
        lg = new int[n + 2];
        for (int i = 2; i <= n + 1; i++) lg[i] = lg[i >> 1] + 1;
        st.add(a.clone());
        for (int k = 1; k < K; k++) {
            int[] p = st.get(k - 1);
            int len = Math.max(0, n - (1 << k) + 1);
            int[] r = new int[len];
            for (int i = 0; i < len; i++) r[i] = Math.min(p[i], p[i + (1 << (k - 1))]);
            st.add(r);
        }
    }

    public int min(int l, int r) {
        int k = lg[r - l + 1];
        return Math.min(st.get(k)[l], st.get(k)[r - (1 << k) + 1]);
    }
}
