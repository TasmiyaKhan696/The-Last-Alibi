package com.lastalibi.algo;

/** Disjoint-set union with path compression and union by rank. */
public class DSU {
    private final int[] p, rk;
    public int steps = 0;

    public DSU(int n) {
        p = new int[n];
        rk = new int[n];
        for (int i = 0; i < n; i++) p[i] = i;
    }

    public int find(int x) {
        steps++;
        if (p[x] != x) p[x] = find(p[x]);
        return p[x];
    }

    public boolean union(int a, int b) {
        int x = find(a), y = find(b);
        if (x == y) return false;
        if (rk[x] < rk[y]) { int t = x; x = y; y = t; }
        p[y] = x;
        if (rk[x] == rk[y]) rk[x]++;
        return true;
    }
}
