package com.lastalibi.algo;

import java.util.*;

/** Compressed trie (radix tree): edges carry whole substrings, so it needs far fewer nodes than a trie. */
public class RadixTree {
    private static final class Edge {
        String s;
        Node to;
        Edge(String s, Node to) { this.s = s; this.to = to; }
    }
    private static final class Node {
        final Map<Character, Edge> next = new HashMap<>();
        boolean end;
    }

    private final Node root = new Node();

    public void insert(String w) {
        Node n = root;
        int i = 0;
        for (;;) {
            if (i == w.length()) { n.end = true; return; }
            Edge e = n.next.get(w.charAt(i));
            if (e == null) {
                Node leaf = new Node();
                leaf.end = true;
                n.next.put(w.charAt(i), new Edge(w.substring(i), leaf));
                return;
            }
            String s = e.s;
            int j = 0;
            while (j < s.length() && i + j < w.length() && s.charAt(j) == w.charAt(i + j)) j++;
            if (j == s.length()) { n = e.to; i += j; continue; }
            Node mid = new Node();
            mid.next.put(s.charAt(j), new Edge(s.substring(j), e.to));
            e.s = s.substring(0, j);
            e.to = mid;
            if (i + j == w.length()) { mid.end = true; return; }
            Node leaf = new Node();
            leaf.end = true;
            mid.next.put(w.charAt(i + j), new Edge(w.substring(i + j), leaf));
            return;
        }
    }

    private Node locate(String p) {
        Node n = root;
        int i = 0;
        while (i < p.length()) {
            Edge e = n.next.get(p.charAt(i));
            if (e == null) return null;
            String s = e.s;
            int j = 0;
            while (j < s.length() && i + j < p.length() && s.charAt(j) == p.charAt(i + j)) j++;
            if (i + j < p.length() && j < s.length()) return null;
            if (i + j == p.length()) return e.to;
            i += j;
            n = e.to;
        }
        return n;
    }

    private int ends(Node n) {
        int c = n.end ? 1 : 0;
        for (Edge e : n.next.values()) c += ends(e.to);
        return c;
    }

    /** Number of stored words that start with p. */
    public int count(String p) {
        Node n = locate(p);
        return n == null ? 0 : ends(n);
    }

    public int nodes() { return nodes(root); }
    private int nodes(Node n) {
        int c = 1;
        for (Edge e : n.next.values()) c += nodes(e.to);
        return c;
    }
}
