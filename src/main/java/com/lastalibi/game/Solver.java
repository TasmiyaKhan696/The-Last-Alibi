package com.lastalibi.game;

import com.lastalibi.algo.Alg;

import java.util.*;

/**
 * Backtracking constraint solver. Suspects are ordered by how few squares they may use
 * (counting sort); after every placement a bipartite matching checks that the remaining
 * suspects can still get distinct rows and distinct columns.
 */
public final class Solver {
    private final World world;
    /** Total search nodes visited across all solve() calls since the last reset. */
    public long nodes = 0;

    public Solver(World world) { this.world = world; }

    /** Squares each suspect may stand on, judging only by that suspect's own unary clues. */
    public List<int[]> domains(List<Clue> clues) {
        List<List<Clue>> un = new ArrayList<>();
        for (int i = 0; i < 9; i++) un.add(new ArrayList<>());
        for (Clue k : clues) if (k.unary) un.get(k.w).add(k);
        List<int[]> out = new ArrayList<>();
        for (int s = 0; s < 9; s++) {
            int[] T = new int[9];
            Arrays.fill(T, -1);
            List<Integer> d = new ArrayList<>();
            for (int p = 0; p < 81; p++) {
                if (world.block(p)) continue;
                T[s] = p;
                boolean ok = true;
                for (Clue k : un.get(s)) if (!k.f(T)) { ok = false; break; }
                if (ok) d.add(p);
            }
            out.add(d.stream().mapToInt(Integer::intValue).toArray());
        }
        return out;
    }

    /** Finds up to {@code lim} placements that satisfy every clue. */
    public List<int[]> solve(List<Clue> clues, int lim) {
        List<int[]> out = new ArrayList<>();
        int[] P = new int[9];
        Arrays.fill(P, -1);
        boolean[] R = new boolean[World.N], C = new boolean[World.N];
        List<List<Clue>> rl = new ArrayList<>();
        for (int i = 0; i < 9; i++) rl.add(new ArrayList<>());
        List<Clue> gl = new ArrayList<>();
        for (Clue k : clues) {
            if (k.unary) continue;
            if (k.o >= 0) { rl.get(k.w).add(k); rl.get(k.o).add(k); }
            else gl.add(k);
        }
        List<int[]> dom = domains(clues);
        int[] sizes = new int[9];
        for (int i = 0; i < 9; i++) sizes[i] = dom.get(i).length;
        int[] ord = Alg.countingOrder(sizes, 81);
        rec(0, out, lim, P, R, C, rl, gl, dom, ord);
        return out;
    }

    private boolean feasible(int i, boolean[] R, boolean[] C, List<int[]> dom, int[] ord) {
        int rem = 9 - i;
        if (rem == 0) return true;
        for (int pass = 0; pass < 2; pass++) {
            List<int[]> adj = new ArrayList<>();
            for (int j = i; j < 9; j++) {
                TreeSet<Integer> q = new TreeSet<>();
                for (int p : dom.get(ord[j])) {
                    int r = p / World.N, c = p % World.N;
                    if (!R[r] && !C[c]) q.add(pass == 1 ? c : r);
                }
                adj.add(q.stream().mapToInt(Integer::intValue).toArray());
            }
            int[] m = Alg.match(rem, World.N, adj);
            int matched = 0;
            for (int x : m) if (x >= 0) matched++;
            if (matched < rem) return false;
        }
        return true;
    }

    private void rec(int i, List<int[]> out, int lim, int[] P, boolean[] R, boolean[] C,
                     List<List<Clue>> rl, List<Clue> gl, List<int[]> dom, int[] ord) {
        if (out.size() >= lim) return;
        nodes++;
        if (i == 9) { out.add(P.clone()); return; }
        int sx = ord[i];
        for (int p : dom.get(sx)) {
            int r = p / World.N, c = p % World.N;
            if (R[r] || C[c]) continue;
            P[sx] = p;
            boolean ok = true;
            for (Clue k : rl.get(sx)) if (!k.f(P)) { ok = false; break; }
            if (ok) for (Clue k : gl) if (!k.f(P)) { ok = false; break; }
            if (ok) {
                R[r] = true; C[c] = true;
                if (feasible(i + 1, R, C, dom, ord)) rec(i + 1, out, lim, P, R, C, rl, gl, dom, ord);
                R[r] = false; C[c] = false;
            }
            P[sx] = -1;
            if (out.size() >= lim) return;
        }
    }
}
