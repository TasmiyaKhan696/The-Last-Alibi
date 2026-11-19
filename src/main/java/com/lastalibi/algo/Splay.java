package com.lastalibi.algo;

/** Top-down-recursive splay tree (int keys) that counts rotations. */
public class Splay {
    private static final class Node {
        int k, v;
        Node l, r;
        Node(int k, int v) { this.k = k; this.v = v; }
    }

    private Node root;
    public int rot = 0;

    private Node rR(Node h) { Node x = h.l; h.l = x.r; x.r = h; rot++; return x; }
    private Node rL(Node h) { Node x = h.r; h.r = x.l; x.l = h; rot++; return x; }

    private Node sp(Node h, int k) {
        if (h == null) return null;
        if (k < h.k) {
            if (h.l == null) return h;
            if (k < h.l.k) { h.l.l = sp(h.l.l, k); h = rR(h); }
            else if (k > h.l.k) { h.l.r = sp(h.l.r, k); if (h.l.r != null) h.l = rL(h.l); }
            return h.l != null ? rR(h) : h;
        }
        if (k > h.k) {
            if (h.r == null) return h;
            if (k > h.r.k) { h.r.r = sp(h.r.r, k); h = rL(h); }
            else if (k < h.r.k) { h.r.l = sp(h.r.l, k); if (h.r.l != null) h.r = rR(h.r); }
            return h.r != null ? rL(h) : h;
        }
        return h;
    }

    public void put(int k, int v) {
        root = sp(root, k);
        if (root == null) { root = new Node(k, v); return; }
        if (k < root.k) { Node n = new Node(k, v); n.l = root.l; n.r = root; root.l = null; root = n; }
        else if (k > root.k) { Node n = new Node(k, v); n.r = root.r; n.l = root; root.r = null; root = n; }
        else root.v = v;
    }

    public Integer get(int k) {
        root = sp(root, k);
        return root != null && root.k == k ? root.v : null;
    }

    public int rootKey() { return root == null ? -1 : root.k; }
}
