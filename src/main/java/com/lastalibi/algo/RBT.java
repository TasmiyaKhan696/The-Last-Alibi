package com.lastalibi.algo;

import java.util.function.BiConsumer;

/** Left-leaning red-black tree map (String keys) used for the detective's notes. */
public class RBT {
    private static final class Node {
        String k, v;
        Node l, r;
        boolean red;
        Node(String k, String v) { this.k = k; this.v = v; red = true; }
    }

    private Node root;

    private boolean red(Node n) { return n != null && n.red; }
    private Node rotL(Node h) { Node x = h.r; h.r = x.l; x.l = h; x.red = h.red; h.red = true; return x; }
    private Node rotR(Node h) { Node x = h.l; h.l = x.r; x.r = h; x.red = h.red; h.red = true; return x; }
    private void flip(Node h) { h.red = !h.red; h.l.red = !h.l.red; h.r.red = !h.r.red; }

    private Node put1(Node h, String k, String v) {
        if (h == null) return new Node(k, v);
        int c = k.compareTo(h.k);
        if (c < 0) h.l = put1(h.l, k, v);
        else if (c > 0) h.r = put1(h.r, k, v);
        else h.v = v;
        if (red(h.r) && !red(h.l)) h = rotL(h);
        if (red(h.l) && red(h.l.l)) h = rotR(h);
        if (red(h.l) && red(h.r)) flip(h);
        return h;
    }

    public void put(String k, String v) { root = put1(root, k, v); root.red = false; }

    public String get(String k) {
        Node n = root;
        while (n != null) {
            int c = k.compareTo(n.k);
            if (c == 0) return n.v;
            n = c < 0 ? n.l : n.r;
        }
        return null;
    }

    public void each(BiConsumer<String, String> f) { each(f, root); }
    private void each(BiConsumer<String, String> f, Node n) {
        if (n == null) return;
        each(f, n.l);
        f.accept(n.k, n.v);
        each(f, n.r);
    }

    /** Black height, or -1 if a red-black rule is violated. */
    public int blackHeight() { return bh(root); }
    private int bh(Node n) {
        if (n == null) return 1;
        if (red(n.r) || (red(n) && red(n.l))) return -1;
        int a = bh(n.l), b = bh(n.r);
        return a < 0 || b < 0 || a != b ? -1 : a + (red(n) ? 0 : 1);
    }
}
