package com.lastalibi.algo;

import java.util.*;

/** Flow network with Edmonds-Karp (BFS), Ford-Fulkerson (DFS) and min-cut side extraction. */
public class Flow {
    private final int n;
    private final List<int[]> e = new ArrayList<>();           // {to, cap}
    private final List<List<Integer>> g = new ArrayList<>();

    public Flow(int n) {
        this.n = n;
        for (int i = 0; i < n; i++) g.add(new ArrayList<>());
    }

    public void add(int u, int v, int c) {
        g.get(u).add(e.size()); e.add(new int[]{v, c});
        g.get(v).add(e.size()); e.add(new int[]{u, 0});
    }

    /** Edmonds-Karp: shortest augmenting paths. */
    public int ek(int s, int t) {
        int f = 0;
        for (;;) {
            int[] pe = new int[n];
            Arrays.fill(pe, -1);
            pe[s] = -2;
            ArrayDeque<Integer> q = new ArrayDeque<>();
            q.add(s);
            while (!q.isEmpty() && pe[t] == -1) {
                int u = q.poll();
                for (int id : g.get(u)) {
                    int[] ed = e.get(id);
                    if (ed[1] > 0 && pe[ed[0]] == -1) { pe[ed[0]] = id; q.add(ed[0]); }
                }
            }
            if (pe[t] == -1) return f;
            int b = Integer.MAX_VALUE;
            for (int v = t; v != s; v = e.get(pe[v] ^ 1)[0]) b = Math.min(b, e.get(pe[v])[1]);
            for (int v = t; v != s; v = e.get(pe[v] ^ 1)[0]) {
                e.get(pe[v])[1] -= b;
                e.get(pe[v] ^ 1)[1] += b;
            }
            f += b;
        }
    }

    /** Ford-Fulkerson: any augmenting path found by DFS. */
    public int ff(int s, int t) {
        int f = 0, d;
        while ((d = dfs(s, t, Integer.MAX_VALUE, new boolean[n])) > 0) f += d;
        return f;
    }

    private int dfs(int u, int t, int f, boolean[] vis) {
        if (u == t) return f;
        vis[u] = true;
        for (int id : g.get(u)) {
            int[] ed = e.get(id);
            if (ed[1] > 0 && !vis[ed[0]]) {
                int d = dfs(ed[0], t, Math.min(f, ed[1]), vis);
                if (d > 0) { ed[1] -= d; e.get(id ^ 1)[1] += d; return d; }
            }
        }
        return 0;
    }

    /** Source side of the min cut (call after a max-flow run). */
    public boolean[] side(int s) {
        boolean[] seen = new boolean[n];
        seen[s] = true;
        ArrayDeque<Integer> q = new ArrayDeque<>();
        q.add(s);
        while (!q.isEmpty()) {
            int u = q.poll();
            for (int id : g.get(u)) {
                int[] ed = e.get(id);
                if (ed[1] > 0 && !seen[ed[0]]) { seen[ed[0]] = true; q.add(ed[0]); }
            }
        }
        return seen;
    }
}
