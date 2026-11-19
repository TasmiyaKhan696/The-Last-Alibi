package com.lastalibi.game;

import com.lastalibi.algo.Alg;

import java.util.*;

/**
 * Builds a fresh case from a seed. It is a Las Vegas algorithm: it retries random culprit layouts
 * until one works, and the result is always correct because the solver proves that exactly one
 * solution remains. Same seed + same case + same level = same puzzle.
 */
public final class Generator {

    /** A generated puzzle: solution, victim index, murderer index and the clue list. */
    public record Puzzle(int[] sol, int victim, int murderer, List<Clue> clues) {}

    /** Mulberry32, bit-for-bit the same sequence as the original web game. */
    public static final class Rng {
        private int a;
        public Rng(int seed) { this.a = seed; }
        public double next() {
            a += 0x6D2B79F5;
            int t = (a ^ (a >>> 15)) * (1 | a);
            t = (t + ((t ^ (t >>> 7)) * (61 | t))) ^ t;
            return ((t ^ (t >>> 14)) & 0xFFFFFFFFL) / 4294967296.0;
        }
    }

    private static final int[] TIME_BUDGET_MS = {2000, 2000, 2000};
    private static final int[] MIN_REL = {0, 0, 0};
    private static final int[] EXTRA_CLUES = {0, 0, 0};

    private final World world;
    private final Solver solver;
    private final Clue.Factory K;
    /** Random layouts tried for the last puzzle (Las Vegas statistic). */
    public int attempts;
    /** How many clues the greedy set cover picked for the last puzzle. */
    public int setCoverPicks;

    public Generator(World world, Solver solver) {
        this.world = world;
        this.solver = solver;
        this.K = new Clue.Factory(world);
    }

    private static <T> void shuffle(List<T> a, Rng R) {
        for (int i = a.size() - 1; i > 0; i--) {
            int j = (int) (R.next() * (i + 1));
            T t = a.get(i); a.set(i, a.get(j)); a.set(j, t);
        }
    }

    private boolean fill(int r, int[] cells, boolean[] used, Rng R) {
        if (r == World.N) return true;
        List<Integer> cols = new ArrayList<>();
        for (int c = 0; c < World.N; c++) if (!used[c] && !world.block(r * World.N + c)) cols.add(c);
        shuffle(cols, R);
        for (int c : cols) {
            used[c] = true;
            cells[r] = r * World.N + c;
            if (fill(r + 1, cells, used, R)) return true;
            used[c] = false;
        }
        return false;
    }

    private static final Comparator<Clue> UNARY_FIRST = Comparator.comparingInt(c -> c.unary ? 0 : 1);
    private static final Comparator<Clue> UNARY_LAST = Comparator.comparingInt(c -> c.unary ? 1 : 0);

    public Puzzle generate(long seed, int difficulty) {
        attempts = 0;
        Rng R = new Rng((int) seed);
        final int N = World.N;
        for (;;) {
            attempts++;
            final boolean simple = difficulty == 0 && attempts <= 25;
            int[] cells = new int[N];
            boolean[] used = new boolean[N];
            if (!fill(0, cells, used, R)) continue;

            List<Integer> order = new ArrayList<>();
            for (int c : cells) order.add(c);
            shuffle(order, R);
            int[] sol = order.stream().mapToInt(Integer::intValue).toArray();

            final int V = 8;
            int vr = world.room(sol[V]), mu = -1, cnt = 0;
            for (int i = 0; i < V; i++) if (world.room(sol[i]) == vr) { cnt++; mu = i; }
            if (cnt != 1) continue; // the victim must share her room with exactly one person

            // BEGINNER MODE: every suspect must be identifiable from their own
            // simple picture clues alone. No Sudoku chains, coordinates or
            // cross-suspect deduction are needed to find an individual square.
            if (difficulty == 0) {
                List<Clue> beginnerClues = new ArrayList<>();
                beginnerClues.add(K.valone(V));
                boolean allDirect = true;
                for (int suspect = 0; suspect < N; suspect++) {
                    int solutionSquare = sol[suspect];
                    List<Clue> candidates = new ArrayList<>();
                    candidates.add(K.room(suspect, world.room(solutionSquare)));
                    String item = world.obj(solutionSquare);
                    if (!item.isEmpty() && !world.isSolid(item)) candidates.add(K.on(suspect, item));
                    for (String furniture : World.FURNITURE) {
                        if (world.beside(solutionSquare, furniture)) candidates.add(K.bes(suspect, furniture));
                        if (world.roomHas(world.room(solutionSquare), furniture)) candidates.add(K.rmo(suspect, furniture));
                    }

                    List<Integer> domain = new ArrayList<>();
                    for (int q = 0; q < N * N; q++) if (!world.block(q)) domain.add(q);
                    List<Clue> chosen = new ArrayList<>();
                    while (domain.size() > 3 && chosen.size() < 3) {
                        Clue best = null;
                        List<Integer> bestDomain = domain;
                        for (Clue candidate : candidates) {
                            if (chosen.contains(candidate)) continue;
                            List<Integer> filtered = new ArrayList<>();
                            int[] trial = new int[N];
                            Arrays.fill(trial, -1);
                            for (int q : domain) {
                                trial[suspect] = q;
                                if (candidate.f(trial)) filtered.add(q);
                            }
                            if (filtered.size() < bestDomain.size()) {
                                best = candidate;
                                bestDomain = filtered;
                            }
                        }
                        if (best == null) break;
                        chosen.add(best);
                        domain = bestDomain;
                    }
                    if (domain.size() > 3 || !domain.contains(solutionSquare)) { allDirect = false; break; }
                    beginnerClues.addAll(chosen);
                }
                if (!allDirect || solver.solve(beginnerClues, 2).size() != 1) continue;
                setCoverPicks = 0;
                return new Puzzle(sol, V, mu, beginnerClues);
            }

            Set<Integer> usedRooms = new HashSet<>();
            for (int q : sol) usedRooms.add(world.room(q));
            Clue valone = K.valone(V), empty = K.empty(world.rooms.length - usedRooms.size());
            List<Clue> must = List.of(valone, empty);
            List<Clue> cl = new ArrayList<>(must);
            List<Clue> rest = new ArrayList<>();
            List<List<Clue>> CANDS = new ArrayList<>();

            for (int s = 0; s < 9; s++) {
                int p = sol[s], r = p / N;
                List<Clue> cand = new ArrayList<>();
                cand.add(K.room(s, world.room(p)));
                String o = world.obj(p);
                if (!o.isEmpty() && !world.isSolid(o)) cand.add(K.on(s, o));
                for (String b : World.FURNITURE) {
                    if (world.beside(p, b)) cand.add(K.bes(s, b));
                    if (world.roomHas(world.room(p), b)) cand.add(K.rmo(s, b));
                }
                boolean alone = true;
                for (int t = 0; t < 9; t++) {
                    if (t == s) continue;
                    if (world.room(sol[t]) == world.room(p)) alone = false;
                    int k = sol[t] / N - r, m = sol[t] % N - sol[s] % N;
                    if (simple) continue;
                    if (k >= 1 && k <= 3) cand.add(K.rel(s, t, k, 'N'));
                    if (k <= -1 && k >= -3) cand.add(K.rel(s, t, -k, 'S'));
                    if (m >= 1 && m <= 3) cand.add(K.rel(s, t, m, 'W'));
                    if (m <= -1 && m >= -3) cand.add(K.rel(s, t, -m, 'E'));
                }
                if (alone && !simple) cand.add(K.alone(s));
                shuffle(cand, R);
                if (difficulty < 2) cand.sort(UNARY_FIRST);
                CANDS.add(cand);
            }

            // Greedy set cover: pick the unary clues that rule out the most wrong (suspect, square) pairs.
            Set<Clue> took = Collections.newSetFromMap(new IdentityHashMap<>());
            {
                List<Clue> un = new ArrayList<>();
                List<int[]> sets = new ArrayList<>();
                for (List<Clue> l : CANDS)
                    for (Clue c : l)
                        if (c.unary) {
                            un.add(c);
                            List<Integer> el = new ArrayList<>();
                            int[] T = new int[9];
                            for (int q = 0; q < 81; q++) {
                                if (world.block(q)) continue;
                                Arrays.fill(T, -1);
                                T[c.w] = q;
                                if (!c.f(T)) el.add(c.w * 81 + q);
                            }
                            sets.add(el.stream().mapToInt(Integer::intValue).toArray());
                        }
                List<Integer> pick = Alg.setcover(729, sets, 12);
                for (int i : pick) { cl.add(un.get(i)); took.add(un.get(i)); }
                setCoverPicks = pick.size();
                for (int s = 0; s < V; s++) {
                    final int ss = s;
                    if (cl.stream().noneMatch(k -> k.w == ss)) {
                        Clue c = CANDS.get(s).stream().filter(k -> !took.contains(k)).findFirst()
                            .orElse(CANDS.get(s).get(0));
                        cl.add(c);
                        took.add(c);
                    }
                }
                for (List<Clue> l : CANDS) for (Clue c : l) if (!took.contains(c)) rest.add(c);
            }

            // Add true clues until the solution is unique.
            shuffle(rest, R);
            if (difficulty < 2) rest.sort(UNARY_FIRST);
            boolean ok = true;
            while (solver.solve(cl, 2).size() > 1) {
                if (rest.isEmpty()) { ok = false; break; }
                cl.add(rest.remove(0));
            }
            if (!ok) continue;

            // Try to drop clues that are not needed (time boxed).
            long t1 = System.currentTimeMillis();
            long budget = TIME_BUDGET_MS[difficulty];
            List<Clue> tryOrder = new ArrayList<>(cl);
            tryOrder.sort(UNARY_LAST);
            for (Clue c : tryOrder) {
                if (System.currentTimeMillis() - t1 > budget) break;
                if (must.contains(c)) continue;
                long same = cl.stream().filter(k -> k.w == c.w).count();
                if (same < 2) continue;
                cl.remove(c);
                if (solver.solve(cl, 2).size() != 1) cl.add(c);
            }

            // Make sure the puzzle has at least 3 direction clues, then add easy-mode extras.
            {
                long nr = cl.stream().filter(k -> k.o >= 0).count();
                for (Clue k : new ArrayList<>(rest)) {
                    if (k.o < 0) continue;
                    if (nr >= MIN_REL[difficulty]) break;
                    cl.add(k);
                    rest.remove(k);
                    nr++;
                }
            }
            for (int i = 0; i < EXTRA_CLUES[difficulty] && !rest.isEmpty(); i++) cl.add(rest.remove(0));

            return new Puzzle(sol, V, mu, cl);
        }
    }
}
