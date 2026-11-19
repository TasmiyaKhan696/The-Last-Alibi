package com.lastalibi.game;

import com.lastalibi.algo.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Algorithmic helpers built on top of a puzzle: the "Is it solvable?" max-flow check, the detective
 * route (TSP approximation), the Fibonacci-heap hint engine, Knuth's tree-size estimator and the
 * DSU room builder.
 */
public final class Analysis {
    private final World world;
    private final Solver solver;
    private final Random rnd = new Random();

    public Analysis(World world, Solver solver) {
        this.world = world;
        this.solver = solver;
    }

    // ------------------------------------------------------------------ rooms via DSU
    public record RoomCount(int rooms, int steps) {}

    public RoomCount dsuRooms() {
        final int N = World.N;
        DSU d = new DSU(81);
        for (int p = 0; p < 81; p++) {
            if (world.room(p) < 0) continue;
            if (p % N < N - 1 && world.room(p + 1) == world.room(p)) d.union(p, p + 1);
            if (p < 72 && world.room(p + N) == world.room(p)) d.union(p, p + N);
        }
        Set<Integer> roots = new HashSet<>();
        for (int p = 0; p < 81; p++) if (world.room(p) >= 0) roots.add(d.find(p));
        return new RoomCount(roots.size(), d.steps);
    }

    // ------------------------------------------------------------------ Knuth estimator (Monte Carlo)
    /** Estimates the number of nodes in the solver's search tree by random probing. */
    public double knuth(Generator.Puzzle puz, int trials) {
        final int N = World.N;
        List<int[]> dom = solver.domains(puz.clues());
        int[] sizes = dom.stream().mapToInt(a -> a.length).toArray();
        int[] ord = Alg.countingOrder(sizes, 81);
        double tot = 0;
        for (int t = 0; t < trials; t++) {
            int[] P = new int[9];
            Arrays.fill(P, -1);
            boolean[] R = new boolean[9], C = new boolean[9];
            double prod = 1, est = 1;
            for (int d = 0; d < 9; d++) {
                int s = ord[d];
                final int ss = s;
                List<Clue> chk = puz.clues().stream()
                    .filter(k -> !k.unary && (k.o < 0 || k.w == ss || k.o == ss)).toList();
                List<Integer> ch = new ArrayList<>();
                for (int c : dom.get(s)) {
                    if (R[c / N] || C[c % N]) continue;
                    P[s] = c;
                    if (chk.stream().allMatch(k -> k.f(P))) ch.add(c);
                }
                P[s] = -1;
                if (ch.isEmpty()) break;
                prod *= ch.size();
                est += prod;
                int pk = ch.get(rnd.nextInt(ch.size()));
                P[s] = pk;
                R[pk / N] = true;
                C[pk % N] = true;
            }
            tot += est;
        }
        return tot / trials;
    }

    // ------------------------------------------------------------------ solvable check (max-flow / min-cut)
    public record Feasibility(boolean ok, String msg, int edmondsKarp, int fordFulkerson, int need) {}

    private record SideResult(int fe, int ff, String msg) {}

    public Feasibility feasibility(Generator.Puzzle puz, int[] P) {
        final int N = World.N;
        List<Integer> k = new ArrayList<>();
        boolean[] R = new boolean[N], C = new boolean[N];
        for (int s = 0; s < 9; s++) {
            if (P[s] >= 0) { R[P[s] / N] = true; C[P[s] % N] = true; }
            else k.add(s);
        }
        if (k.isEmpty()) return new Feasibility(true, "Everyone is placed.", 0, 0, 0);

        List<List<Integer>> allowed = new ArrayList<>();
        int[] tmp = P.clone();
        for (int s : k) {
            List<Integer> L = new ArrayList<>();
            for (int c = 0; c < 81; c++) {
                if (world.block(c) || R[c / N] || C[c % N]) continue;
                tmp[s] = c;
                boolean ok = true;
                for (Clue x : puz.clues()) if (!x.f(tmp)) { ok = false; break; }
                if (ok) L.add(c);
            }
            tmp[s] = -1;
            allowed.add(L);
        }

        SideResult r = side(true, k, allowed);
        SideResult c = side(false, k, allowed);
        int[] rc = new int[N];
        Arrays.fill(rc, Integer.MAX_VALUE);
        for (int r0 = 0; r0 < N; r0++) {
            if (R[r0]) continue;
            Set<Integer> cs = new HashSet<>();
            for (List<Integer> L : allowed) for (int x : L) if (x / N == r0) cs.add(x);
            rc[r0] = cs.size();
        }
        SegTree st = new SegTree(rc);
        int mn = st.min(0, N - 1), tight = -1;
        for (int i = 0; i < N; i++) if (rc[i] == mn) { tight = i; break; }
        boolean ok = r.msg == null && c.msg == null;
        String msg = ok
            ? "Still solvable: " + k.size() + " unplaced suspects can each get their own row and column (flow "
              + r.fe + "/" + k.size() + " rows, " + c.fe + "/" + k.size() + " columns)."
            : Arrays.asList(r.msg, c.msg).stream().filter(Objects::nonNull).collect(Collectors.joining(" "));
        if (mn != Integer.MAX_VALUE) msg += " Tightest row: row " + (tight + 1) + " has only " + mn + " possible square(s).";
        return new Feasibility(ok, msg, Math.min(r.fe, c.fe), Math.min(r.ff, c.ff), k.size());
    }

    private SideResult side(boolean rows, List<Integer> k, List<List<Integer>> allowed) {
        final int N = World.N;
        int m = k.size(), T = m + 10;
        Flow a = new Flow(m + 11), b = new Flow(m + 11);
        for (int i = 0; i < m; i++) {
            a.add(0, 1 + i, 1);
            b.add(0, 1 + i, 1);
            Set<Integer> lines = new TreeSet<>();
            for (int c : allowed.get(i)) lines.add(rows ? c / N : c % N);
            for (int l : lines) { a.add(1 + i, m + 1 + l, 1); b.add(1 + i, m + 1 + l, 1); }
        }
        for (int l = 0; l < N; l++) { a.add(m + 1 + l, T, 1); b.add(m + 1 + l, T, 1); }
        int fe = a.ek(0, T), ff = b.ff(0, T);
        String msg = null;
        if (fe < m) {
            boolean[] src = a.side(0);
            List<String> names = new ArrayList<>();
            List<Integer> ls = new ArrayList<>();
            for (int i = 0; i < m; i++) if (src[1 + i]) names.add(world.cast[k.get(i)].shortName());
            for (int l = 0; l < N; l++) if (src[m + 1 + l]) ls.add(l + 1);
            String what = rows ? "row" : "column";
            String nm = String.join(", ", names);
            msg = ls.isEmpty()
                ? "Impossible: " + nm + " (" + names.size() + " suspects) have no legal " + what
                  + " left, so some earlier placement or clue rules them out."
                : "Impossible (min-cut): " + nm + " (" + names.size() + " suspects) can only use " + ls.size() + " "
                  + what + "(s): " + ls.stream().map(String::valueOf).collect(Collectors.joining(", ")) + ".";
        }
        return new SideResult(fe, ff, msg);
    }

    // ------------------------------------------------------------------ detective route (metric TSP 2-approximation)
    public record Route(List<Integer> order, int len, int opt) {}

    public Route routePlan(List<Integer> cells) {
        int n = cells.size();
        if (n < 2) return new Route(new ArrayList<>(cells), 0, 0);
        int[][] d = new int[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                d[i][j] = Math.abs(cells.get(i) / World.N - cells.get(j) / World.N)
                        + Math.abs(cells.get(i) % World.N - cells.get(j) % World.N);
        List<Integer> t = Alg.tsp(d);
        int len = Alg.tourLen(d, t);
        int opt = n <= 9 ? Alg.bruteTsp(d) : -1;
        List<Integer> order = new ArrayList<>();
        for (int i : t) order.add(cells.get(i));
        return new Route(order, len, opt);
    }

    // ------------------------------------------------------------------ hint engine (Fibonacci heap)
    public record Hint(int suspect, List<Integer> cells) {}

    /** Keeps a Fibonacci heap of "squares left per suspect"; correct placements trigger decreaseKey. */
    public final class HintEngine {
        private FibHeap heap;
        private FibHeap.Node[] node;
        private int[] count;
        private boolean[] placed;
        public int decreaseKeyCalls = 0;
        public int rebuilds = 0;

        public void reset() { heap = null; }

        public Hint next(Generator.Puzzle puz, int[] pos) {
            final int N = World.N;
            int[] base = new int[9];
            for (int i = 0; i < 9; i++) base[i] = pos[i] == puz.sol()[i] ? pos[i] : -1;
            int[] cnt = new int[9];
            boolean[] pl = new boolean[9];
            List<List<Integer>> cand = new ArrayList<>();
            for (int s = 0; s < 9; s++) {
                pl[s] = base[s] >= 0;
                if (pl[s]) { cand.add(null); continue; }
                List<Integer> L = new ArrayList<>();
                for (int p = 0; p < 81; p++) {
                    if (world.block(p)) continue;
                    boolean crossed = false;
                    for (int q : base) if (q >= 0 && (q / N == p / N || q % N == p % N)) { crossed = true; break; }
                    if (crossed) continue;
                    base[s] = p;
                    boolean ok = true;
                    for (Clue k : puz.clues()) if (!k.f(base)) { ok = false; break; }
                    if (ok) L.add(p);
                    base[s] = -1;
                }
                cand.add(L);
                cnt[s] = L.size();
            }
            boolean rebuild = heap == null;
            if (heap != null)
                for (int s = 0; s < 9; s++)
                    if ((placed[s] && !pl[s]) || (!pl[s] && count[s] < cnt[s])) rebuild = true;
            if (rebuild) {
                heap = new FibHeap();
                node = new FibHeap.Node[9];
                for (int s = 0; s < 9; s++) if (!pl[s]) node[s] = heap.insert(cnt[s], s);
                rebuilds++;
            } else {
                for (int s = 0; s < 9; s++)
                    if (!pl[s] && cnt[s] < count[s]) { heap.decreaseKey(node[s], cnt[s]); decreaseKeyCalls++; }
            }
            count = cnt.clone();
            placed = pl;
            while (heap.n > 0) {
                int s = heap.min.val;
                if (pl[s]) { heap.extractMin(); continue; }
                return new Hint(s, cand.get(s));
            }
            return null;
        }
    }

    public HintEngine newHintEngine() { return new HintEngine(); }
}
