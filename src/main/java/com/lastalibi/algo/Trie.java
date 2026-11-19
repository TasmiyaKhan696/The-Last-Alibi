package com.lastalibi.algo;

import java.util.*;

/** Plain trie with sorted prefix enumeration (clue-search autocomplete). */
public class Trie {
    private static final class Node {
        final TreeMap<Character, Node> c = new TreeMap<>();
        boolean end;
    }

    private final Node root = new Node();
    public int nodes = 1;

    public void insert(String w) {
        Node n = root;
        for (char ch : w.toCharArray()) {
            Node nx = n.c.get(ch);
            if (nx == null) { nx = new Node(); n.c.put(ch, nx); nodes++; }
            n = nx;
        }
        n.end = true;
    }

    public List<String> prefix(String p) {
        Node n = root;
        for (char ch : p.toCharArray()) {
            n = n.c.get(ch);
            if (n == null) return List.of();
        }
        List<String> out = new ArrayList<>();
        go(n, p, out);
        return out;
    }

    private void go(Node n, String s, List<String> out) {
        if (n.end) out.add(s);
        for (Map.Entry<Character, Node> e : n.c.entrySet()) go(e.getValue(), s + e.getKey(), out);
    }
}
