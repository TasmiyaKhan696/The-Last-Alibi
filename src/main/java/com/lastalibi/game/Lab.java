package com.lastalibi.game;

import com.lastalibi.algo.*;

import java.util.*;
import java.util.stream.Collectors;

/** The "Algorithm Lab": every topic runs live on the current puzzle. */
public final class Lab {
    public static final String[] TOPICS = {
        "Counting sort", "Radix sort", "Bucket sort", "Quickselect and Median-of-Medians", "Randomized quicksort",
        "Binary heap", "Fibonacci heap", "Disjoint set union", "Amortized analysis", "AVL tree", "Red-black tree",
        "Splay tree", "Segment tree and RMQ", "KMP", "Trie and radix tree", "Suffix tree and suffix array",
        "Flow networks: Ford-Fulkerson, Edmonds-Karp, min cut", "Bipartite matching", "Vertex cover approximation",
        "Set cover approximation", "Metric TSP approximation", "Las Vegas and Monte Carlo", "Bloom filter",
        "P, NP, NP-hard, NP-complete"};

    private final Game g;

    public Lab(Game g) { this.g = g; }

    private World w() { return g.world; }
    private Generator.Puzzle puz() { return g.puzzle; }
    private String nm(int i) { return g.shortName(i); }
    private static String arr(int[] a) { return Arrays.stream(a).mapToObj(String::valueOf).collect(Collectors.joining(",")); }

    private int[] sampleTimes() {
        if (g.progress.times.size() >= 8) return g.progress.times.stream().mapToInt(Integer::intValue).toArray();
        long a = 1;
        int[] out = new int[40];
        for (int i = 0; i < 40; i++) { a = a * 16807 % 2147483647L; out[i] = (int) (60 + a % 600); }
        return out;
    }

    private String allText() {
        return puz().clues().stream().map(Clue::text).collect(Collectors.joining("\n")).toLowerCase();
    }

    private List<String> words(String text) {
        List<String> ws = new ArrayList<>();
        for (String x : text.split("[^a-z]+")) if (x.length() > 1) ws.add(x);
        return ws;
    }

    public String run(int topic) {
        List<int[]> dom = g.solver.domains(puz().clues());
        int[] sz = dom.stream().mapToInt(d -> d.length).toArray();
        List<String> o = new ArrayList<>();
        final int N = World.N;
        switch (topic) {
            case 0 -> {
                int[] ord = Alg.countingOrder(sz, 81);
                o.add("USED BY: the solver orders suspects by how few squares they can use (keys 0..81, so counting sort is O(n+k)).");
                o.add("Squares each suspect may use: [" + arr(sz) + "]");
                o.add("Search order: " + Arrays.stream(ord).mapToObj(this::nm).collect(Collectors.joining(" ")));
                o.add("Counting-sorted sizes: [" + arr(Alg.counting(sz, 81)) + "]");
            }
            case 1 -> {
                o.add("USED BY: the leaderboard orders solve times (LSD radix sort, 4 passes of base 256).");
                int[] s = Alg.radix(sampleTimes());
                o.add("First 12 sorted times (s): [" + arr(Arrays.copyOf(s, Math.min(12, s.length))) + "]");
            }
            case 2 -> {
                double[] sc = new double[10];
                for (int i = 0; i < 10; i++) sc[i] = Math.min(.999, Math.log10(Math.max(1, g.analysis.knuth(puz(), 25))) / 6);
                o.add("USED BY: difficulty scores in [0,1) are sorted with bucket sort (expected O(n)).");
                o.add("10 Monte-Carlo difficulty scores, sorted: " + Arrays.stream(Alg.bucket(sc))
                    .mapToObj(x -> String.format("%.2f", x)).collect(Collectors.joining(",", "[", "]")));
            }
            case 3 -> {
                int[] a = sampleTimes();
                int k = a.length >> 1;
                o.add("USED BY: the leaderboard median (par time). Quickselect is expected O(n); median-of-medians is worst-case O(n).");
                o.add("Median solve time: quickselect=" + Alg.quickselect(a, k) + "s, median-of-medians=" + Alg.mom(a, k) + "s");
            }
            case 4 -> {
                int[] s = sampleTimes();
                int[] a = new int[s.length];
                for (int i = 0; i < s.length; i++) a[i] = s[(i * 7) % s.length];
                Alg.rqs(a);
                o.add("A random pivot gives expected O(n log n) time on every input.");
                o.add("Sorted sample: [" + arr(Arrays.copyOf(a, Math.min(10, a.length))) + "]");
            }
            case 5 -> {
                BinaryHeap<int[]> h = new BinaryHeap<>((x, y) -> x[0] - y[0]);
                for (int i = 0; i < sz.length; i++) h.add(new int[]{sz[i], i});
                List<String> s = new ArrayList<>();
                while (h.size() > 0) { int[] v = h.poll(); s.add(nm(v[1]) + "(" + v[0] + ")"); }
                o.add("Suspects polled by fewest squares:");
                o.add(String.join(" ", s));
                o.add("Array doublings copied " + h.copies + " elements in total (amortized O(1) add).");
            }
            case 6 -> {
                FibHeap h = new FibHeap();
                FibHeap.Node[] nd = new FibHeap.Node[sz.length];
                for (int i = 0; i < sz.length; i++) nd[i] = h.insert(sz[i], i);
                for (int i = 0; i < nd.length; i++) if (i % 3 == 0 && nd[i].key > 1) h.decreaseKey(nd[i], nd[i].key / 2);
                List<String> s = new ArrayList<>();
                while (h.n > 0) { FibHeap.Node x = h.extractMin(); s.add(nm(x.val) + "(" + x.key + ")"); }
                o.add("USED BY: the Hint button. Heap key = squares left per suspect; placing a suspect correctly calls decreaseKey.");
                o.add("Demo: halve some keys, then extract in order:");
                o.add(String.join(" ", s));
                o.add("This game so far: decreaseKey calls = " + g.hintEngine.decreaseKeyCalls + ", rebuilds = " + g.hintEngine.rebuilds);
            }
            case 7 -> {
                Analysis.RoomCount r = g.analysis.dsuRooms();
                o.add("USED BY: the board builds its rooms by uniting neighbouring squares of the same room.");
                o.add("Rooms found: " + r.rooms() + " (must be 6). DSU find-steps for 81 squares: " + r.steps() + " (path compression + union by rank).");
            }
            case 8 -> {
                double[] d = Alg.dyn(100000);
                o.add("Dynamic array (inside the binary heap): 100000 adds, " + (long) d[0] + " element copies in total.");
                o.add(String.format("Amortized cost per add = (n + copies)/n = %.2f (< 3, so O(1) amortized by the aggregate method).", d[1]));
                o.add("Same idea: DSU path compression, Fibonacci-heap decreaseKey and splay-tree access are amortized bounds.");
            }
            case 9 -> {
                AVL a = new AVL();
                for (int i = 0; i < 2000; i++) a.insert(i);
                o.add("USED BY: leaderboard rank = AVL.rank(time)+1 in O(log n).");
                o.add(String.format("2000 sorted inserts (worst case for a plain BST): height = %d (limit 1.44*log2 n = %.1f)",
                    a.height(), 1.44 * Math.log(2000) / Math.log(2)));
                o.add("rank(1500)=" + a.rank(1500) + ", select(1500)=" + a.select(1500));
            }
            case 10 -> {
                RBT r = new RBT();
                Random rnd = new Random();
                for (int i = 0; i < 2000; i++) r.put(String.format("%06d", rnd.nextInt(100000)), String.valueOf(i));
                String nt = String.join("\n", g.allNotes());
                o.add("USED BY: your notes are kept sorted by suspect name.");
                o.add(nt.isEmpty() ? "(no notes yet: use  note <suspect no.> <text>)" : nt);
                o.add("Black height of a 2000-key tree: " + r.blackHeight() + " (> 0 means every red-black rule holds).");
            }
            case 11 -> {
                Splay sp = new Splay();
                for (int i = 0; i < 20; i++) sp.put(i * 7 % 20, i);
                sp.get(7);
                o.add("Self-adjusting tree: every access moves the key to the root.");
                o.add("After get(7), root key = " + sp.rootKey() + ", rotations so far = " + sp.rot);
            }
            case 12 -> {
                int[] rows = new int[9];
                for (int r = 0; r < 9; r++) {
                    Set<Integer> cs = new HashSet<>();
                    for (int[] d : dom) for (int c : d) if (c / N == r) cs.add(c);
                    rows[r] = cs.size();
                }
                SegTree st = new SegTree(rows);
                Sparse sp = new Sparse(sz);
                st.update(4, 0);
                o.add("USED BY: the solvable check finds the tightest row with a segment tree.");
                o.add("Squares usable per row: [" + arr(rows) + "]");
                o.add("Segment tree min of rows 0..8 after update(4,0): " + st.min(0, 8) + ", min of rows 0..3: " + st.min(0, 3));
                o.add("Sparse table over the 9 domain sizes, min of suspects 2..6 (O(1) query): " + sp.min(2, 6));
            }
            case 13 -> {
                String tx = allText();
                o.add("USED BY: the clue search box. Prefix function of \"beside\": [" + arr(Alg.kmpPre("beside")) + "]");
                for (String q : new String[]{"beside", "row", "room"}) o.add("\"" + q + "\" occurs at " + Alg.kmp(tx, q));
            }
            case 14 -> {
                List<String> ws = new ArrayList<>();
                for (Cases.Person p : w().cast) ws.add(p.shortName().toLowerCase());
                ws.addAll(words(allText()));
                Trie tr = new Trie();
                RadixTree rd = new RadixTree();
                for (String x : ws) { tr.insert(x); rd.insert(x); }
                String pre = w().cast[0].shortName().toLowerCase().substring(0, 1);
                List<String> hits = tr.prefix(pre);
                o.add("USED BY: autocomplete in the clue search box.");
                o.add("Words starting with \"" + pre + "\": " + hits.subList(0, Math.min(12, hits.size())));
                o.add("Trie nodes: " + tr.nodes + "   radix-tree nodes: " + rd.nodes() + " (compressed edges need far fewer)");
            }
            case 15 -> {
                String all = allText();
                String tx = all.substring(0, Math.min(1200, all.length()));
                SuffixTree st = new SuffixTree(tx);
                SuffixArray sa = new SuffixArray(tx);
                o.add("Text: all clues (" + tx.length() + " chars).");
                o.add("\"beside\" occurrences: suffix tree = " + st.occ("beside") + ", suffix array = " + sa.search("beside").size());
                o.add("Longest repeated phrase (LCP array): \"" + sa.longest().replace('\n', ' ') + "\"");
            }
            case 16 -> {
                int[] e = new int[9];
                Arrays.fill(e, -1);
                Analysis.Feasibility a = g.analysis.feasibility(puz(), e);
                int[] p2 = e.clone();
                p2[0] = puz().sol()[0];
                p2[1] = puz().sol()[1];
                Analysis.Feasibility b = g.analysis.feasibility(puz(), p2);
                Flow net = new Flow(8);
                for (int i = 0; i < 3; i++) net.add(0, 1 + i, 1);
                for (int i = 0; i < 3; i++) { net.add(1 + i, 4, 1); net.add(1 + i, 5, 1); }
                for (int r = 0; r < 3; r++) net.add(4 + r, 7, 1);
                int f = net.ek(0, 7);
                boolean[] sd = net.side(0);
                int su = 0, ro = 0;
                for (int i = 1; i <= 3; i++) if (sd[i]) su++;
                for (int i = 4; i <= 6; i++) if (sd[i]) ro++;
                o.add("USED BY: the \"check\" command. Empty board: Edmonds-Karp flow = " + a.edmondsKarp()
                    + ", Ford-Fulkerson flow = " + a.fordFulkerson() + ", needed = " + a.need());
                o.add("After placing two suspects correctly: " + b.msg());
                o.add("");
                o.add("Hall demo: three suspects that may only use two rows -> max flow = " + f
                    + " < 3, so no valid placement exists. Min-cut source side: " + su + " suspects reach only " + ro + " rows.");
            }
            case 17 -> {
                List<int[]> adj = new ArrayList<>();
                for (int[] d : dom) adj.add(Arrays.stream(d).map(c -> c / N).distinct().sorted().toArray());
                int[] ml = Alg.match(9, 9, adj);
                int cnt = (int) Arrays.stream(ml).filter(x -> x >= 0).count();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < ml.length; i++) sb.append(nm(i)).append("->row ").append(ml[i] + 1).append("  ");
                o.add("USED BY: the solver prunes branches when the remaining suspects cannot get distinct rows.");
                o.add("Suspects -> rows matching size = " + cnt + " of 9");
                o.add("Assignment: " + sb.toString().trim());
            }
            case 18 -> {
                List<int[]> E = new ArrayList<>();
                for (Clue k : puz().clues()) if (k.o >= 0) E.add(new int[]{k.w, k.o});
                Set<Integer> vc = Alg.vcover(E);
                int opt = 9;
                for (int m = 0; m < 512; m++) {
                    final int mm = m;
                    if (E.stream().allMatch(e -> ((mm >> e[0]) & 1) == 1 || ((mm >> e[1]) & 1) == 1))
                        opt = Math.min(opt, Integer.bitCount(m));
                }
                o.add("Clue graph: suspects are vertices, each direction clue is an edge (" + E.size() + " edges).");
                o.add("2-approx cover (suspects worth placing first): " + vc.stream().map(this::nm).collect(Collectors.joining(" ")));
                o.add("Size " + vc.size() + " vs optimal " + opt + " (guaranteed <= " + 2 * opt + ")");
            }
            case 19 -> {
                long a = 1;
                List<int[]> sets = new ArrayList<>();
                for (int i = 0; i < 12; i++) {
                    List<Integer> s = new ArrayList<>();
                    for (int j = 0; j < 30; j++) { a = a * 16807 % 2147483647L; if ((double) a / 2147483647L < .25) s.add(j); }
                    sets.add(s.stream().mapToInt(Integer::intValue).toArray());
                }
                for (int j = 0; j < 30; j++) sets.add(new int[]{j});
                List<Integer> pick = Alg.setcover(30, sets, 1_000_000_000);
                Set<Integer> cov = new HashSet<>();
                for (int i : pick) for (int x : sets.get(i)) cov.add(x);
                o.add("USED BY: the generator picks its first clues with a greedy set cover (universe = wrong (suspect, square) pairs). This puzzle used "
                    + g.generator.setCoverPicks + " set-cover clues.");
                o.add("Demo: 42 random sets over 30 elements -> greedy picked " + pick.size() + " sets covering " + cov.size()
                    + "/30 (ratio guarantee H(n) ~ ln n).");
            }
            case 20 -> {
                List<Integer> sol = new ArrayList<>();
                for (int p : puz().sol()) sol.add(p);
                Analysis.Route pl = g.analysis.routePlan(sol);
                o.add("USED BY: the \"route\" command (MST + preorder walk, Manhattan distance is a metric).");
                o.add("Tour length " + pl.len() + ", optimal " + pl.opt() + ", ratio "
                    + (pl.opt() > 0 ? String.format("%.2f", (double) pl.len() / pl.opt()) : "1.00") + " (guaranteed <= 2).");
            }
            case 21 -> {
                g.solver.nodes = 0;
                List<int[]> all = g.solver.solve(puz().clues(), 1000);
                long actual = g.solver.nodes;
                double est = g.analysis.knuth(puz(), 300);
                o.add("LAS VEGAS: the generator is always correct (unique solution proven by the solver) but needed "
                    + g.generator.attempts + " random attempt(s) for this puzzle.");
                o.add("MONTE CARLO: Knuth's estimator guessed the search tree has about " + Math.round(est)
                    + " nodes; a full traversal visited " + actual + " (" + all.size() + " solution).");
            }
            case 22 -> {
                Bloom b = new Bloom(1 << 16, 4);
                for (int i = 0; i < 500; i++) b.add(9000 + i);
                int fp = 0;
                for (int i = 0; i < 5000; i++) if (b.has(1_000_000 + i)) fp++;
                o.add("USED BY: Surprise case never repeats a seed (a false positive only skips one seed).");
                o.add(String.format("Measured false-positive rate: %.2f%%   theory: %.2f%%   false negatives: 0 by construction.",
                    fp / 50.0, b.fp() * 100));
            }
            case 23 -> {
                long t0 = System.nanoTime();
                boolean ok = puz().clues().stream().allMatch(k -> k.f(puz().sol()));
                Set<Integer> R = new HashSet<>(), C = new HashSet<>();
                for (int c : puz().sol()) { R.add(c / N); C.add(c % N); }
                ok = ok && R.size() == 9 && C.size() == 9;
                double ver = (System.nanoTime() - t0) / 1e6;
                g.solver.nodes = 0;
                long t1 = System.nanoTime();
                g.solver.solve(puz().clues(), 2);
                double sv = (System.nanoTime() - t1) / 1e6;
                o.add("VERIFY a finished placement: " + ok + " in " + String.format("%.3f", ver)
                    + " ms, O(n + clues) = polynomial, so the decision problem is in NP.");
                o.add("SOLVE from scratch: " + g.solver.nodes + " search nodes, " + String.format("%.1f", sv)
                    + " ms; the worst case grows exponentially with grid size.");
                o.add("Completing a Latin square from partial information is NP-complete, and this puzzle generalises that style of constraint satisfaction.");
                o.add("The 9x9 game is tiny so the solver is instant; a formal NP-completeness proof for this exact clue language is not claimed.");
            }
            default -> o.add("Unknown topic");
        }
        return String.join("\n", o);
    }
}
