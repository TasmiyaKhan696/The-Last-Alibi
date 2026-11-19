package com.lastalibi.algo;

import java.util.*;

/** Suffix array with binary-search pattern lookup and LCP-based longest repeated substring (Kasai). */
public class SuffixArray {
    private final String t;
    private final Integer[] sa;

    public SuffixArray(String t) {
        this.t = t;
        sa = new Integer[t.length()];
        for (int i = 0; i < sa.length; i++) sa[i] = i;
        Arrays.sort(sa, (a, b) -> t.substring(a).compareTo(t.substring(b)));
    }

    private int cmp(int i, String p) {
        int s = sa[i];
        String x = t.substring(s, Math.min(t.length(), s + p.length()));
        return Integer.signum(x.compareTo(p));
    }

    public List<Integer> search(String p) {
        int lo = 0, hi = sa.length;
        while (lo < hi) { int m = (lo + hi) >> 1; if (cmp(m, p) < 0) lo = m + 1; else hi = m; }
        int first = lo;
        hi = sa.length;
        while (lo < hi) { int m = (lo + hi) >> 1; if (cmp(m, p) <= 0) lo = m + 1; else hi = m; }
        List<Integer> out = new ArrayList<>(Arrays.asList(sa).subList(first, lo));
        Collections.sort(out);
        return out;
    }

    public String longest() {
        int n = t.length();
        int[] rank = new int[n];
        for (int i = 0; i < n; i++) rank[sa[i]] = i;
        int best = 0, at = 0;
        for (int i = 0, h = 0; i < n; i++) {
            if (rank[i] > 0) {
                int j = sa[rank[i] - 1];
                while (i + h < n && j + h < n && t.charAt(i + h) == t.charAt(j + h)) h++;
                if (h > best) { best = h; at = i; }
                if (h > 0) h--;
            } else h = 0;
        }
        return t.substring(at, at + best);
    }
}
