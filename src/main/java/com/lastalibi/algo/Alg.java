package com.lastalibi.algo;

import java.util.*;

/** Static algorithm toolbox used by the game and shown live in the Algorithm Lab. */
public final class Alg {
    private Alg() {}
    private static final Random RND = new Random();

    /** Counting sort that returns the sorted ORDER of indices (stable). */
    public static int[] countingOrder(int[] key, int max) {
        int[] c = new int[max + 2];
        for (int k : key) c[k + 1]++;
        for (int i = 0; i <= max; i++) c[i + 1] += c[i];
        int[] o = new int[key.length];
        for (int i = 0; i < key.length; i++) o[c[key[i]]++] = i;
        return o;
    }

    public static int[] counting(int[] a, int max) {
        int[] c = new int[max + 2];
        for (int x : a) c[x + 1]++;
        for (int i = 0; i <= max; i++) c[i + 1] += c[i];
        int[] o = new int[a.length];
        for (int x : a) o[c[x]++] = x;
        return o;
    }

    /** LSD radix sort, base 256, four passes (non-negative ints). */
    public static int[] radix(int[] a) {
        int[] src = a.clone(), dst = new int[a.length];
        for (int sh = 0; sh < 32; sh += 8) {
            int[] c = new int[257];
            for (int x : src) c[((x >>> sh) & 255) + 1]++;
            for (int i = 0; i < 256; i++) c[i + 1] += c[i];
            for (int x : src) dst[c[(x >>> sh) & 255]++] = x;
            int[] t = src; src = dst; dst = t;
        }
        return src;
    }

    /** Bucket sort for values in [0,1). */
    public static double[] bucket(double[] a) {
        int n = a.length;
        List<List<Double>> b = new ArrayList<>();
        for (int i = 0; i < n; i++) b.add(new ArrayList<>());
        for (double x : a) b.get(Math.min(n - 1, (int) Math.floor(x * n))).add(x);
        double[] out = new double[n];
        int idx = 0;
        for (List<Double> l : b) {
            for (int i = 1; i < l.size(); i++) {
                double k = l.get(i);
                int j = i - 1;
                while (j >= 0 && l.get(j) > k) { l.set(j + 1, l.get(j)); j--; }
                l.set(j + 1, k);
            }
            for (double d : l) out[idx++] = d;
        }
        return out;
    }

    /** Randomized quicksort (in place, recurses on the smaller side). */
    public static int[] rqs(int[] a) { qs(a, 0, a.length - 1); return a; }
    private static void qs(int[] a, int lo, int hi) {
        while (lo < hi) {
            int p = lo + RND.nextInt(hi - lo + 1);
            int i = partition(a, lo, hi, p);
            if (i - lo < hi - i) { qs(a, lo, i - 1); lo = i + 1; }
            else { qs(a, i + 1, hi); hi = i - 1; }
        }
    }
    private static int partition(int[] a, int lo, int hi, int p) {
        int pv = a[p];
        swap(a, p, hi);
        int i = lo;
        for (int j = lo; j < hi; j++) if (a[j] < pv) { swap(a, i, j); i++; }
        swap(a, i, hi);
        return i;
    }
    private static void swap(int[] a, int i, int j) { int t = a[i]; a[i] = a[j]; a[j] = t; }

    /** Expected O(n) selection of the k-th smallest (0-based). */
    public static int quickselect(int[] arr, int k) {
        int[] b = arr.clone();
        int lo = 0, hi = b.length - 1;
        while (lo < hi) {
            int i = partition(b, lo, hi, lo + RND.nextInt(hi - lo + 1));
            if (k == i) return b[i];
            if (k < i) hi = i - 1; else lo = i + 1;
        }
        return b[lo];
    }

    /** Worst-case O(n) selection (median of medians). */
    public static int mom(int[] arr, int k) { return momR(arr.clone(), 0, arr.length - 1, k); }
    private static void insR(int[] a, int lo, int hi) {
        for (int i = lo + 1; i <= hi; i++) {
            int k = a[i], j = i - 1;
            while (j >= lo && a[j] > k) { a[j + 1] = a[j]; j--; }
            a[j + 1] = k;
        }
    }
    private static int momR(int[] a, int lo, int hi, int k) {
        while (lo < hi) {
            int i = partition(a, lo, hi, momPivot(a, lo, hi));
            if (k == i) return a[i];
            if (k < i) hi = i - 1; else lo = i + 1;
        }
        return a[lo];
    }
    private static int momPivot(int[] a, int lo, int hi) {
        int n = hi - lo + 1;
        if (n <= 5) { insR(a, lo, hi); return lo + (n >> 1); }
        int m = 0;
        for (int i = lo; i <= hi; i += 5) {
            int r = Math.min(i + 4, hi);
            insR(a, i, r);
            int md = i + ((r - i) >> 1);
            swap(a, lo + m, md);
            m++;
        }
        int mid = lo + ((m - 1) >> 1);
        momR(a, lo, lo + m - 1, mid);
        return mid;
    }

    /** Dynamic array doubling: returns {total copies, amortized cost per add}. */
    public static double[] dyn(int n) {
        long cap = 1, size = 0, copies = 0;
        for (int i = 0; i < n; i++) {
            if (size == cap) { copies += size; cap *= 2; }
            size++;
        }
        return new double[]{copies, (double) (size + copies) / size};
    }

    /** Maximum bipartite matching (Kuhn). Returns matchLeft[u] = right vertex or -1. */
    public static int[] match(int nL, int nR, List<int[]> adj) {
        int[] mL = new int[nL], mR = new int[nR];
        Arrays.fill(mL, -1);
        Arrays.fill(mR, -1);
        for (int u = 0; u < nL; u++) aug(u, new boolean[nR], adj, mL, mR);
        return mL;
    }
    private static boolean aug(int u, boolean[] seen, List<int[]> adj, int[] mL, int[] mR) {
        for (int v : adj.get(u)) {
            if (seen[v]) continue;
            seen[v] = true;
            if (mR[v] < 0 || aug(mR[v], seen, adj, mL, mR)) { mR[v] = u; mL[u] = v; return true; }
        }
        return false;
    }

    /** 2-approximation vertex cover via maximal matching. */
    public static Set<Integer> vcover(List<int[]> edges) {
        Set<Integer> c = new LinkedHashSet<>();
        for (int[] e : edges) if (!c.contains(e[0]) && !c.contains(e[1])) { c.add(e[0]); c.add(e[1]); }
        return c;
    }

    /** Greedy set cover (ln n approximation). Returns chosen set indices. */
    public static List<Integer> setcover(int U, List<int[]> sets, int lim) {
        Set<Integer> un = new HashSet<>();
        for (int i = 0; i < U; i++) un.add(i);
        List<Integer> ch = new ArrayList<>();
        while (!un.isEmpty() && ch.size() < lim) {
            int best = -1, bc = 0;
            for (int i = 0; i < sets.size(); i++) {
                int c = 0;
                for (int x : sets.get(i)) if (un.contains(x)) c++;
                if (c > bc) { bc = c; best = i; }
            }
            if (best < 0) break;
            ch.add(best);
            for (int x : sets.get(best)) un.remove(x);
        }
        return ch;
    }

    /** Metric TSP 2-approximation: Prim MST + preorder walk. */
    public static List<Integer> tsp(int[][] d) {
        int n = d.length;
        boolean[] in = new boolean[n];
        int[] par = new int[n];
        double[] key = new double[n];
        Arrays.fill(par, -1);
        Arrays.fill(key, Double.POSITIVE_INFINITY);
        List<List<Integer>> ch = new ArrayList<>();
        for (int i = 0; i < n; i++) ch.add(new ArrayList<>());
        key[0] = 0;
        for (int it = 0; it < n; it++) {
            int u = -1;
            for (int v = 0; v < n; v++) if (!in[v] && (u < 0 || key[v] < key[u])) u = v;
            in[u] = true;
            if (par[u] >= 0) ch.get(par[u]).add(u);
            for (int v = 0; v < n; v++) if (!in[v] && d[u][v] < key[v]) { key[v] = d[u][v]; par[v] = u; }
        }
        List<Integer> t = new ArrayList<>();
        preorder(0, ch, t);
        return t;
    }
    private static void preorder(int u, List<List<Integer>> ch, List<Integer> t) {
        t.add(u);
        for (int v : ch.get(u)) preorder(v, ch, t);
    }
    public static int tourLen(int[][] d, List<Integer> t) {
        int s = 0;
        for (int i = 0; i < t.size(); i++) s += d[t.get(i)][t.get((i + 1) % t.size())];
        return s;
    }

    /** Exact optimum tour length by brute force over permutations (use only for n &lt;= 9). */
    public static int bruteTsp(int[][] d) {
        int n = d.length;
        int[] perm = new int[n - 1];
        for (int i = 1; i < n; i++) perm[i - 1] = i;
        int[] best = {Integer.MAX_VALUE};
        permute(perm, 0, d, best);
        return best[0];
    }
    private static void permute(int[] p, int k, int[][] d, int[] best) {
        if (k == p.length) {
            int l = d[0][p[0]];
            for (int i = 0; i + 1 < p.length; i++) l += d[p[i]][p[i + 1]];
            l += d[p[p.length - 1]][0];
            best[0] = Math.min(best[0], l);
            return;
        }
        for (int i = k; i < p.length; i++) {
            swap(p, k, i); permute(p, k + 1, d, best); swap(p, k, i);
        }
    }

    // ---- string algorithms ----
    public static int[] kmpPre(String p) {
        int[] f = new int[p.length()];
        for (int i = 1, k = 0; i < p.length(); i++) {
            while (k > 0 && p.charAt(i) != p.charAt(k)) k = f[k - 1];
            if (p.charAt(i) == p.charAt(k)) k++;
            f[i] = k;
        }
        return f;
    }
    public static List<Integer> kmp(String t, String p) {
        List<Integer> r = new ArrayList<>();
        if (p.isEmpty()) return r;
        int[] f = kmpPre(p);
        for (int i = 0, k = 0; i < t.length(); i++) {
            while (k > 0 && t.charAt(i) != p.charAt(k)) k = f[k - 1];
            if (t.charAt(i) == p.charAt(k)) k++;
            if (k == p.length()) { r.add(i - k + 1); k = f[k - 1]; }
        }
        return r;
    }
}
