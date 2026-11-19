package com.lastalibi.algo;

import java.util.*;

/** Array-backed min-heap that also counts element copies made by capacity doubling (amortized analysis). */
public class BinaryHeap<T> {
    private final ArrayList<T> a = new ArrayList<>();
    private final Comparator<T> cmp;
    public long copies = 0;
    private int cap = 2;

    public BinaryHeap(Comparator<T> cmp) { this.cmp = cmp; }

    public void add(T x) {
        if (a.size() == cap) { copies += cap; cap *= 2; }
        a.add(x);
        int i = a.size() - 1;
        while (i > 0) {
            int p = (i - 1) >> 1;
            if (cmp.compare(a.get(i), a.get(p)) >= 0) break;
            Collections.swap(a, i, p);
            i = p;
        }
    }

    public T poll() {
        T r = a.get(0);
        T l = a.remove(a.size() - 1);
        if (!a.isEmpty()) {
            a.set(0, l);
            int i = 0;
            for (;;) {
                int L = 2 * i + 1, R = L + 1, m = i;
                if (L < a.size() && cmp.compare(a.get(L), a.get(m)) < 0) m = L;
                if (R < a.size() && cmp.compare(a.get(R), a.get(m)) < 0) m = R;
                if (m == i) break;
                Collections.swap(a, i, m);
                i = m;
            }
        }
        return r;
    }

    public int size() { return a.size(); }
}
