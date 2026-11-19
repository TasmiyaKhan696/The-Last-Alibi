package com.lastalibi.game;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A single clue. {@code check} is evaluated on a (possibly partial) placement array
 * P[suspect] = square or -1 and returns false only when the placement already contradicts the clue.
 */
public final class Clue {
    /** Suspect the clue is about (-1 for global clues). */
    public final int w;
    /** Second suspect for relational clues, -1 otherwise. */
    public final int o;
    /** Unary clue: depends only on the position of suspect w. */
    public final boolean unary;
    private final Predicate<int[]> check;
    private final Supplier<String> text;

    Clue(int w, int o, boolean unary, Predicate<int[]> check, Supplier<String> text) {
        this.w = w; this.o = o; this.unary = unary; this.check = check; this.text = text;
    }

    public boolean f(int[] placement) { return check.test(placement); }
    public String text() { return text.get(); }
    @Override public String toString() { return text(); }

    /** Factory for every clue kind used by the game. */
    public static final class Factory {
        private final World world;

        public Factory(World world) { this.world = world; }

        private int countFurniture(String kind) {
            int count = 0;
            for (int p = 0; p < World.N * World.N; p++)
                if (world.obj(p).equals(kind)) count++;
            return count;
        }

        private String definiteFurniture(String kind) {
            String noun = world.word(kind).replaceFirst("^(a|an|the) ", "");
            return "the only " + noun;
        }

        public Clue room(int w, int r) {
            return new Clue(w, -1, true, P -> P[w] < 0 || world.room(P[w]) == r,
                () -> world.pronoun(w) + " was in the " + world.rooms[r] + ".");
        }

        /** Beginner clue: gives the suspect's row directly. */
        public Clue row(int w, int r) {
            return new Clue(w, -1, true, P -> P[w] < 0 || P[w] / World.N == r,
                () -> world.cast[w].shortName() + " was in row " + (r + 1) + ".");
        }

        /** Beginner clue: gives the suspect's column directly. */
        public Clue column(int w, int c) {
            return new Clue(w, -1, true, P -> P[w] < 0 || P[w] % World.N == c,
                () -> world.cast[w].shortName() + " was in column " + (c + 1) + ".");
        }

        public Clue on(int w, String b) {
            return new Clue(w, -1, true, P -> P[w] < 0 || world.obj(P[w]).equals(b), () -> {
                boolean unique = countFurniture(b) == 1;
                if (unique && b.equals("CARPET"))
                    return world.pronoun(w) + " was the only person on " + world.word(b).replaceFirst("^(a|an|the) ", "the ") + ".";
                String verb = b.equals("BED") ? "lying on" : b.equals("CHAIR") ? "sitting in" : "standing on";
                return world.pronoun(w) + " was " + verb + " " + (unique ? definiteFurniture(b) : world.word(b)) + ".";
            });
        }

        public Clue rmo(int w, String b) {
            return new Clue(w, -1, true, P -> P[w] < 0 || world.roomHas(world.room(P[w]), b),
                () -> world.pronoun(w) + " was in the room with " + (countFurniture(b) == 1 ? definiteFurniture(b) : world.word(b)) + ".");
        }

        public Clue bes(int w, String b) {
            return new Clue(w, -1, true, P -> P[w] < 0 || world.beside(P[w], b),
                () -> world.pronoun(w) + " was beside " + (countFurniture(b) == 1 ? definiteFurniture(b) : world.word(b)) + " in the same room.");
        }

        public Clue alone(int w) {
            return new Clue(w, -1, false, P -> {
                if (P[w] < 0) return true;
                for (int j = 0; j < P.length; j++)
                    if (j != w && P[j] >= 0 && world.room(P[j]) == world.room(P[w])) return false;
                return true;
            }, () -> world.pronoun(w) + " was alone in " + (world.cast[w].female() ? "her" : "his") + " room.");
        }

        /** w is k rows/columns in direction d (N,S,E,W) of suspect o. */
        public Clue rel(int w, int o, int k, char d) {
            Predicate<int[]> f = P -> {
                if (P[w] < 0 || P[o] < 0) return true;
                int rw = P[w] / World.N, ro = P[o] / World.N, cw = P[w] % World.N, co = P[o] % World.N;
                switch (d) {
                    case 'N': return rw == ro - k;
                    case 'S': return rw == ro + k;
                    case 'W': return cw == co - k;
                    default:  return cw == co + k;
                }
            };
            return new Clue(w, o, false, f, () -> {
                String dir = switch (d) { case 'N' -> "up from"; case 'S' -> "down from"; case 'E' -> "right of"; default -> "left of"; };
                String unit = (d == 'N' || d == 'S') ? "row" : "column";
                return world.pronoun(w) + " was " + k + " " + unit + (k > 1 ? "s" : "") + " " + dir + " " + world.cast[o].shortName() + ".";
            });
        }

        /** The victim is alone with the murderer (exactly two people share her room once everyone is placed). */
        public Clue valone(int w) {
            return new Clue(w, -1, false, P -> {
                if (P[w] < 0) return true;
                int n = 0;
                boolean anyUnplaced = false;
                for (int q : P) {
                    if (q < 0) anyUnplaced = true;
                    else if (world.room(q) == world.room(P[w])) n++;
                }
                return n <= 2 && (anyUnplaced || n == 2);
            }, () -> world.pronoun(w) + " was alone with the murderer.");
        }

        /** Exactly k rooms hold nobody. */
        public Clue empty(int k) {
            return new Clue(-1, -1, false, P -> {
                for (int q : P) if (q < 0) return true;
                Set<Integer> used = new HashSet<>();
                for (int q : P) used.add(world.room(q));
                return world.rooms.length - used.size() == k;
            }, () -> k == 0 ? "No room was left empty." : k == 1 ? "Exactly 1 room was left empty." : "Exactly " + k + " rooms were left empty.");
        }
    }
}
