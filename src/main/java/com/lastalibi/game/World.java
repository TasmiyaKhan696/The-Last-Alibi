package com.lastalibi.game;

import com.lastalibi.game.Cases.Case;
import com.lastalibi.game.Cases.Person;
import com.lastalibi.game.Cases.Variant;

import java.util.*;

/**
 * The 9x9 mansion for one case: which room each square belongs to (or -1 for the
 * courtyard), which furniture sits on it, and the spatial helpers the clues rely on.
 * Squares are indexed p = row * 9 + col.
 */
public final class World {
    public static final int N = 9;

    // Base floor plan (before the per-case rotation/mirroring). K L H G M W = room letters.
    private static final String[] MAP = {
        "KKKKKGGGG", "LLKKKGGGG", "LLHHHHGGG", "LLH###GGG", "LLH###MMM",
        "LLH###MMM", "WWHHHHMMM", "WWWWMMMMM", "WWWWMMMMM"};
    private static final String ROOM_KEYS = "KLHGMW";

    private static final Object[][] ITEMS = {
        {0, 1, "TABLE"}, {0, 3, "SHELF"}, {1, 3, "CHAIR"}, {1, 0, "TV"}, {3, 0, "CHAIR"}, {5, 1, "CHAIR"},
        {4, 0, "CARPET"}, {2, 3, "CARPET"}, {4, 2, "CARPET"}, {6, 2, "CARPET"}, {0, 5, "SHELF"},
        {1, 7, "BED"}, {1, 8, "BED"}, {3, 8, "TV"}, {7, 0, "SHELF"}, {8, 1, "PLANT"}, {8, 2, "CHAIR"},
        {7, 6, "BED"}, {7, 7, "BED"}, {4, 8, "SHELF"}, {5, 6, "CHAIR"}, {8, 8, "PLANT"},
        {6, 8, "TV"}, {6, 4, "PLANT"}};

    /** Furniture kinds in the order the clue generator walks them. */
    public static final String[] FURNITURE = {"BED", "TV", "CHAIR", "PLANT", "SHELF", "CARPET", "TABLE"};
    private static final Set<String> SOLID = Set.of("TV", "PLANT", "SHELF", "TABLE");

    /** Room "categories" used by the "In a bedroom." style clues. */

    private static final Map<Variant, Map<String, String>> WORDS = new EnumMap<>(Variant.class);
    static {
        WORDS.put(Variant.I, Map.of("BED", "a palang", "TV", "a TV", "CHAIR", "a chair", "PLANT", "a tulsi",
            "SHELF", "an almirah", "CARPET", "a dhurrie", "TABLE", "a chowki"));
        WORDS.put(Variant.H, Map.of("BED", "a bed", "TV", "a TV", "CHAIR", "a director's chair", "PLANT", "a palm",
            "SHELF", "a trophy cabinet", "CARPET", "the red carpet", "TABLE", "a coffee table"));
        WORDS.put(Variant.A, Map.of("BED", "a bed", "TV", "a TV", "CHAIR", "an armchair", "PLANT", "a palm",
            "SHELF", "a cabinet", "CARPET", "a carpet", "TABLE", "a table"));
    }

    private static int[] transform(int tf, int r, int c) {
        switch (tf) {
            case 0: return new int[]{r, c};
            case 1: return new int[]{r, 8 - c};
            case 2: return new int[]{c, r};
            default: return new int[]{8 - r, c};
        }
    }

    public final Case theCase;
    public final int caseIndex;
    private final int[] roomOf = new int[81];
    private final String[] obj = new String[81];
    private final Map<String, String> words;
    public final String[] rooms;
    public final Person[] cast;
    /** A representative square for each room (closest to its centre), used for labelling. */
    public final int[] roomCentre = new int[6];

    public World(int caseIndex) {
        this.caseIndex = caseIndex;
        this.theCase = Cases.ALL[caseIndex];
        this.rooms = theCase.rooms();
        this.cast = theCase.cast();
        this.words = WORDS.get(theCase.variant());
        Arrays.fill(roomOf, -1);
        Arrays.fill(obj, "");
        for (int r = 0; r < N; r++)
            for (int c = 0; c < N; c++) {
                int[] t = transform(theCase.transform(), r, c);
                roomOf[t[0] * N + t[1]] = ROOM_KEYS.indexOf(MAP[r].charAt(c));
            }
        for (Object[] it : ITEMS) {
            int[] t = transform(theCase.transform(), (Integer) it[0], (Integer) it[1]);
            obj[t[0] * N + t[1]] = (String) it[2];
        }
        for (int q = 0; q < 6; q++) {
            double sr = 0, sc = 0;
            int n = 0;
            for (int p = 0; p < 81; p++) if (roomOf[p] == q) { sr += p / N; sc += p % N; n++; }
            double best = 1e9;
            for (int p = 0; p < 81; p++)
                if (roomOf[p] == q) {
                    double d = Math.pow(p / N - sr / n, 2) + Math.pow(p % N - sc / n, 2);
                    if (d < best) { best = d; roomCentre[q] = p; }
                }
        }
    }

    public int room(int p) { return roomOf[p]; }
    public String obj(int p) { return obj[p]; }
    public String word(String furniture) { return words.get(furniture); }
    public boolean isSolid(String o) { return SOLID.contains(o); }

    /** Nobody can stand in the courtyard or on solid furniture. */
    public boolean block(int p) { return roomOf[p] < 0 || SOLID.contains(obj[p]); }

    public boolean beside(int p, String o) {
        int[][] d = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] q : d) {
            int r = p / N + q[0], c = p % N + q[1];
            if (r >= 0 && r < N && c >= 0 && c < N) {
                int neighbour = r * N + c;
                if (roomOf[neighbour] == roomOf[p] && obj[neighbour].equals(o)) return true;
            }
        }
        return false;
    }

    public boolean roomHas(int room, String o) {
        for (int q = 0; q < 81; q++) if (roomOf[q] == room && obj[q].equals(o)) return true;
        return false;
    }

    public String pronoun(int w) { return cast[w].female() ? "She" : "He"; }
}
