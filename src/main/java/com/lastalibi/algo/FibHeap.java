package com.lastalibi.algo;

import java.util.*;

/** Fibonacci heap with insert, extractMin and decreaseKey (amortized O(1) / O(log n) / O(1)). */
public class FibHeap {
    public static final class Node {
        public int key, val, deg;
        boolean mark;
        Node p, child, l, r;
    }

    public Node min;
    public int n;

    public Node insert(int key, int val) {
        Node x = new Node();
        x.key = key; x.val = val;
        x.l = x.r = x;
        root(x);
        n++;
        return x;
    }

    private void root(Node x) {
        x.p = null;
        if (min == null) { x.l = x.r = x; min = x; }
        else {
            x.r = min.r; x.l = min;
            min.r.l = x; min.r = x;
            if (x.key < min.key) min = x;
        }
    }

    public Node extractMin() {
        Node z = min;
        if (z == null) return null;
        if (z.child != null) {
            List<Node> ch = new ArrayList<>();
            Node t = z.child;
            do { ch.add(t); t = t.r; } while (t != z.child);
            for (Node x : ch) { x.p = null; x.r = z.r; x.l = z; z.r.l = x; z.r = x; }
            z.child = null;
        }
        z.l.r = z.r; z.r.l = z.l;
        if (z.r == z) min = null;
        else { min = z.r; consolidate(); }
        n--;
        return z;
    }

    private void consolidate() {
        List<Node> roots = new ArrayList<>();
        Node t = min;
        do { roots.add(t); t = t.r; } while (t != min);
        List<Node> A = new ArrayList<>();
        for (Node w : roots) {
            Node x = w;
            int d = x.deg;
            for (;;) {
                while (A.size() <= d) A.add(null);
                Node y = A.get(d);
                if (y == null) break;
                if (y.key < x.key) { Node q = x; x = y; y = q; }
                link(y, x);
                A.set(d, null);
                d++;
            }
            A.set(d, x);
        }
        min = null;
        for (Node x : A) if (x != null) root(x);
    }

    private void link(Node y, Node x) {
        y.p = x; y.mark = false;
        if (x.child == null) { x.child = y; y.l = y.r = y; }
        else { y.r = x.child.r; y.l = x.child; x.child.r.l = y; x.child.r = y; }
        x.deg++;
    }

    public void decreaseKey(Node x, int k) {
        x.key = k;
        Node y = x.p;
        if (y != null && x.key < y.key) { cut(x, y); cascade(y); }
        if (x.key < min.key) min = x;
    }

    private void cut(Node x, Node y) {
        if (x.r == x) y.child = null;
        else { x.l.r = x.r; x.r.l = x.l; if (y.child == x) y.child = x.r; }
        y.deg--;
        x.mark = false;
        root(x);
    }

    private void cascade(Node y) {
        Node z = y.p;
        if (z != null) {
            if (!y.mark) y.mark = true;
            else { cut(y, z); cascade(z); }
        }
    }
}
