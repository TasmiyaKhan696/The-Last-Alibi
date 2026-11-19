package com.lastalibi.algo;

/** Naive suffix tree: every suffix inserted into a radix tree (O(n^2) build, fine for short texts). */
public class SuffixTree {
    private final RadixTree r = new RadixTree();

    public SuffixTree(String t) {
        for (int i = 0; i < t.length(); i++) r.insert(t.substring(i));
    }

    public int occ(String p) { return p.isEmpty() ? 0 : r.count(p); }
}
