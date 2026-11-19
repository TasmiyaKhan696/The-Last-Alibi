package com.lastalibi.algo;

/** AVL tree of ints with subtree sizes: insert, rank and select in O(log n). */
public class AVL {
    private static final class Node {
        int k, h = 1, sz = 1;
        Node l, r;
        Node(int k) { this.k = k; }
    }

    private Node root;

    private int h(Node n) { return n == null ? 0 : n.h; }
    private int sz(Node n) { return n == null ? 0 : n.sz; }
    private void upd(Node n) { n.h = 1 + Math.max(h(n.l), h(n.r)); n.sz = 1 + sz(n.l) + sz(n.r); }
    private Node rotR(Node y) { Node x = y.l; y.l = x.r; x.r = y; upd(y); upd(x); return x; }
    private Node rotL(Node x) { Node y = x.r; x.r = y.l; y.l = x; upd(x); upd(y); return y; }

    private Node ins(Node n, int k) {
        if (n == null) return new Node(k);
        if (k < n.k) n.l = ins(n.l, k); else n.r = ins(n.r, k);
        upd(n);
        int b = h(n.l) - h(n.r);
        if (b > 1) {
            if (h(n.l.l) < h(n.l.r)) n.l = rotL(n.l);
            return rotR(n);
        }
        if (b < -1) {
            if (h(n.r.r) < h(n.r.l)) n.r = rotR(n.r);
            return rotL(n);
        }
        return n;
    }

    public void insert(int k) { root = ins(root, k); }

    /** Number of stored keys strictly smaller than k. */
    public int rank(int k) {
        int r = 0;
        Node n = root;
        while (n != null) {
            if (k <= n.k) n = n.l;
            else { r += 1 + sz(n.l); n = n.r; }
        }
        return r;
    }

    /** i-th smallest key (0-based) or -1. */
    public int select(int i) {
        Node n = root;
        while (n != null) {
            int s = sz(n.l);
            if (i < s) n = n.l;
            else if (i == s) return n.k;
            else { i -= s + 1; n = n.r; }
        }
        return -1;
    }

    public int height() { return h(root); }
    public int size() { return sz(root); }
}
