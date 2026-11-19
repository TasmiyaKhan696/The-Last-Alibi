package com.lastalibi;

import com.lastalibi.algo.Bloom;
import com.lastalibi.game.*;
import com.lastalibi.game.Cases.Theme;

import java.util.*;

/**
 * THE LAST ALIBI - Algorithms edition (console port).
 *
 * A murder-mystery grid logic puzzle: put nine suspects on a 9x9 mansion plan, one per row and column,
 * using the clues on their cards. The murderer is the one alone with the victim.
 *
 * Run:  mvn -q compile exec:java      or      java -jar target/the-last-alibi-1.0.0.jar
 * Options:  --lab [topic]   run the Algorithm Lab without playing
 *           --case C --level L [--seed S]   jump straight into a case (C = 1..6, L = 1..3)
 */
public final class Main {
    private static final Scanner IN = new Scanner(System.in);
    private static final Progress PROGRESS = new Progress();
    private static final Bloom SEEN = new Bloom(1 << 16, 4);
    private static final Random RND = new Random();

    private static final String HOW_TO_PLAY = String.join("\n",
        "HOW TO PLAY",
        " 1. Pick a world, then a case and a level.",
        " 2. Place a suspect with:  place <suspect no.> <row> <col>   (short form: p 3 5 2)",
        "    Pick one up again with: up <suspect no.>",
        " 3. Everyone stood in a DIFFERENT ROW AND COLUMN. Squares marked x are already crossed out.",
        " 4. Read the clues on each suspect: furniture (beside a table, on a carpet), rooms (in the room",
        "    with a TV) and directions (2 rows up from Max). The MURDERER is the one alone with the victim.",
        " 5. Nobody can stand in the courtyard (~~~) or on solid furniture (UPPERCASE symbols).",
        " 6. Solving a case earns points (more stars = more points). A hint costs " + Game.HINT_COST
            + " points and caps the level at 1 star.",
        " 7. Clear Easy to unlock Medium, then Hard.",
        " Type 'help' during a case for the full command list.");

    private static final String ABOUT = String.join("\n",
        "ABOUT THE LAST ALIBI",
        "A murder-mystery grid logic puzzle built as an Advanced Algorithms mini project. A backtracking",
        "constraint solver checks every puzzle has exactly one solution, and a generator builds a fresh case",
        "from a random culprit layout by adding only true clues.",
        "Three worlds, six cases and eighteen levels.",
        "Under the hood: counting sort and bipartite matching in the solver, set cover and a Bloom filter in",
        "the generator, a Fibonacci heap for hints, max-flow and min-cut for the solvable check, a TSP",
        "approximation for the detective route, and an AVL tree, radix sort and quickselect for the",
        "leaderboard. Open the Algorithm Lab to see each one run live.");

    private static final String HELP = String.join("\n",
        "COMMANDS",
        "  place <n> <row> <col>   (p)   put suspect n on a square, e.g.  p 3 5 2",
        "  up <n>                  (u)   pick suspect n up again",
        "  board                   (b)   redraw the floor plan",
        "  suspects                (l)   show every suspect card with its clues",
        "  brief                         read the case file again",
        "  legend                        explain the board symbols",
        "  hint                    (h)   buy a hint (" + Game.HINT_COST + " points, caps the level at 1 star)",
        "  check                   (c)   is the board still solvable? (max-flow / min-cut)",
        "  route                   (r)   detective route through placed suspects (TSP approximation)",
        "  search <text>           (s)   search the clues (KMP) with autocomplete (trie)",
        "  note <n> <text>               save a note about suspect n (red-black tree)",
        "  notes                         list your notes, sorted by suspect name",
        "  reset                         clear the board and the timer",
        "  lab                           open the Algorithm Lab",
        "  time                          show the clock",
        "  back                    (q)   leave the case");

    private Main() {}

    // ------------------------------------------------------------------ input helpers
    private static String ask(String prompt) {
        System.out.print(prompt);
        System.out.flush();
        return IN.hasNextLine() ? IN.nextLine().trim() : null;
    }

    private static int askInt(String prompt, int lo, int hi) {
        for (;;) {
            String s = ask(prompt);
            if (s == null) return -1;
            try {
                int v = Integer.parseInt(s);
                if (v >= lo && v <= hi) return v;
            } catch (NumberFormatException ignored) { /* ask again */ }
            System.out.println("Please type a number from " + lo + " to " + hi + ".");
        }
    }

    private static String fmt(int s) { return (s / 60) + ":" + String.format("%02d", s % 60); }

    private static String starString(int st) { return "*".repeat(st) + "-".repeat(3 - st); }

    // ------------------------------------------------------------------ entry point
    public static void main(String[] args) {
        if (args.length == 0 || args[0].equals("--gui")) { GuiLauncher.launch(); return; }
        if (args[0].equals("--console")) {
            args = Arrays.copyOfRange(args, 1, args.length);
            if (args.length == 0) { home(); return; }
        }
        if (args.length > 0 && args[0].equals("--lab")) { runLabCli(args); return; }
        Integer c = null, l = null;
        Long seed = null;
        for (int i = 0; i + 1 < args.length; i++) {
            try {
                switch (args[i]) {
                    case "--case" -> c = Integer.parseInt(args[i + 1]) - 1;
                    case "--level" -> l = Integer.parseInt(args[i + 1]) - 1;
                    case "--seed" -> seed = Long.parseLong(args[i + 1]);
                    default -> { }
                }
            } catch (NumberFormatException e) {
                System.out.println("Ignoring bad value for " + args[i] + ".");
            }
        }
        if (c != null && c >= 0 && c < 6) {
            int d = l != null && l >= 0 && l < 3 ? l : 0;
            int n = c * 3 + d;
            playLoop(c, d, seed != null ? seed : seedFor(n), seed != null ? -1 : n);
            return;
        }
        home();
    }

    /** Same seed formula as the original web game: level n = case*3 + difficulty. */
    private static long seedFor(int n) { return 11L + (n / 3) * 37L + (n % 3) * 5L; }

    private static long freshSeed() {
        int t = 0, x;
        do { x = RND.nextInt(1_000_000); } while (SEEN.has(x) && ++t < 50);
        return x;
    }

    // ------------------------------------------------------------------ menus
    private static void banner() {
        System.out.println();
        System.out.println("==============================================");
        System.out.println("   THE LAST ALIBI  -  Algorithms edition");
        System.out.println("   WHO DIS?  Three worlds. One game.");
        System.out.println("==============================================");
        System.out.println("   Points: " + PROGRESS.points);
    }

    private static void home() {
        for (;;) {
            banner();
            System.out.println(" 1) Choose your world   (Hollywood / Jaipur / Arabian Nights)");
            System.out.println(" 2) Surprise case");
            System.out.println(" 3) Algorithm Lab");
            System.out.println(" 4) How to play");
            System.out.println(" 5) About");
            System.out.println(" 0) Quit");
            int ch = askInt("> ", 0, 5);
            switch (ch) {
                case 1 -> chooseWorld();
                case 2 -> {
                    int ci = RND.nextInt(6), d = RND.nextInt(3);
                    playLoop(ci, d, freshSeed(), -1);
                }
                case 3 -> labMenu(null);
                case 4 -> System.out.println("\n" + HOW_TO_PLAY);
                case 5 -> System.out.println("\n" + ABOUT);
                default -> { PROGRESS.save(); System.out.println("Case closed. Goodbye, detective."); return; }
            }
        }
    }

    private static void chooseWorld() {
        for (;;) {
            System.out.println("\nCHOOSE YOUR WORLD");
            for (int t = 0; t < Cases.THEMES.length; t++) {
                Theme th = Cases.THEMES[t];
                System.out.println(" " + (t + 1) + ") " + th.name() + "  -  " + th.tagline());
            }
            System.out.println(" 0) Back");
            int t = askInt("> ", 0, Cases.THEMES.length);
            if (t <= 0) return;
            chooseCase(Cases.THEMES[t - 1]);
        }
    }

    private static void chooseCase(Theme th) {
        for (;;) {
            System.out.println("\n" + th.name() + "  -  " + th.tagline());
            int[] cs = th.cases();
            for (int i = 0; i < cs.length; i++) {
                Cases.Case k = Cases.ALL[cs[i]];
                StringBuilder lv = new StringBuilder();
                for (int d = 0; d < 3; d++) {
                    boolean open = PROGRESS.unlocked(cs[i], d);
                    lv.append(Cases.LEVEL_NAMES[d]).append(' ').append(open ? starString(PROGRESS.starsFor(cs[i] * 3 + d)) : "[locked]");
                    if (d < 2) lv.append("   ");
                }
                System.out.println(" " + (i + 1) + ") " + k.title() + "  (" + k.location() + ")\n      " + lv);
            }
            System.out.println(" 0) Back");
            int i = askInt("> ", 0, cs.length);
            if (i <= 0) return;
            int ci = cs[i - 1];
            System.out.println("Level:  1) Easy   2) Medium   3) Hard   0) Back");
            int d = askInt("> ", 0, 3);
            if (d <= 0) continue;
            d--;
            if (!PROGRESS.unlocked(ci, d)) {
                System.out.println("That level is locked. Clear " + Cases.LEVEL_NAMES[d - 1] + " first.");
                continue;
            }
            int n = ci * 3 + d;
            playLoop(ci, d, seedFor(n), n);
        }
    }

    // ------------------------------------------------------------------ labs
    private static void labMenu(Game current) {
        Game g = current != null ? current : new Game(0, 1, 7, -1, PROGRESS);
        Lab lab = new Lab(g);
        for (;;) {
            System.out.println("\nALGORITHM LAB  (each topic runs live on the current puzzle)");
            for (int i = 0; i < Lab.TOPICS.length; i++) System.out.printf("%3d) %s%n", i + 1, Lab.TOPICS[i]);
            System.out.println("  0) Back");
            int t = askInt("> ", 0, Lab.TOPICS.length);
            if (t <= 0) return;
            System.out.println("\n--- " + Lab.TOPICS[t - 1] + " ---");
            System.out.println(lab.run(t - 1));
        }
    }

    private static void runLabCli(String[] args) {
        Game g = new Game(0, 1, 7, -1, PROGRESS);
        Lab lab = new Lab(g);
        if (args.length > 1) {
            try {
                int t = Integer.parseInt(args[1]);
                if (t < 1 || t > Lab.TOPICS.length) throw new NumberFormatException();
                System.out.println("--- " + Lab.TOPICS[t - 1] + " ---\n" + lab.run(t - 1));
            } catch (NumberFormatException e) {
                System.out.println("Topic must be a number from 1 to " + Lab.TOPICS.length + ".");
            }
            return;
        }
        for (int i = 0; i < Lab.TOPICS.length; i++)
            System.out.println("--- " + (i + 1) + ". " + Lab.TOPICS[i] + " ---\n" + lab.run(i) + "\n");
    }

    // ------------------------------------------------------------------ playing a case
    private static void playLoop(int ci, int d, long seed, int levelIndex) {
        int curCase = ci, curDiff = d, curLevel = levelIndex;
        long curSeed = seed;
        for (;;) {
            SEEN.add((int) curSeed);
            System.out.println("\nSetting the scene...");
            Game g = new Game(curCase, curDiff, curSeed, curLevel, PROGRESS);
            Cases.Case k = g.theCase();
            System.out.println("\n=== " + k.title() + "  -  " + Cases.LEVEL_NAMES[curDiff] + " ===");
            brief(g);
            show(g);
            boolean won = session(g);
            if (!won) return;
            Game.WinResult w = g.win();
            printWin(g, w);
            if (curLevel >= 0 && curLevel < 17) {
                String a = ask("\nNext case? (y/n) > ");
                if (a != null && a.toLowerCase().startsWith("y")) {
                    int n = curLevel + 1;
                    curLevel = n;
                    curCase = n / 3;
                    curDiff = n % 3;
                    curSeed = seedFor(n);
                    continue;
                }
            }
            return;
        }
    }

    private static void brief(Game g) {
        Cases.Case k = g.theCase();
        System.out.println("\nCASE FILE  -  " + k.location());
        System.out.println(k.story());
        System.out.println(g.globalClue() + " One suspect per row and column.");
        System.out.println("The murderer is the one alone with " + g.name(g.victim()) + ".");
    }

    private static void show(Game g) {
        System.out.println();
        System.out.print(g.renderBoard());
        System.out.println(g.legend());
        System.out.println();
        System.out.print(g.renderSuspects());
        System.out.println("Points: " + PROGRESS.points + "   Time: " + fmt(g.elapsedSeconds()) + "   Hints: " + g.hintsUsed
            + "   (type 'help' for commands)");
    }

    private static void status(Game g) {
        System.out.println();
        System.out.print(g.renderBoard());
        System.out.println("Points: " + PROGRESS.points + "   Time: " + fmt(g.elapsedSeconds()) + "   Hints: " + g.hintsUsed);
    }

    private static int parseSuspect(String s) {
        try {
            int v = Integer.parseInt(s);
            if (v >= 1 && v <= 9) return v - 1;
        } catch (NumberFormatException ignored) { /* fall through */ }
        return -1;
    }

    /** Runs the command loop. Returns true when the case was solved, false when the player left. */
    private static boolean session(Game g) {
        for (;;) {
            if (g.solved) return true;
            String line = ask("\ncase> ");
            if (line == null) return false;
            if (line.isEmpty()) continue;
            String[] parts = line.split("\\s+", 3);
            String cmd = parts[0].toLowerCase();
            switch (cmd) {
                case "place", "p" -> {
                    if (parts.length < 3) { System.out.println("Usage: place <suspect 1-9> <row 1-9> <col 1-9>"); break; }
                    String[] rc = parts[2].trim().split("\\s+");
                    int s = parseSuspect(parts[1]);
                    int r = -1, c = -1;
                    try {
                        r = Integer.parseInt(rc[0]) - 1;
                        c = Integer.parseInt(rc[rc.length > 1 ? 1 : 0]) - 1;
                    } catch (NumberFormatException ignored) { /* handled below */ }
                    if (s < 0 || rc.length < 2 || r < 0 || r > 8 || c < 0 || c > 8) {
                        System.out.println("Usage: place <suspect 1-9> <row 1-9> <col 1-9>");
                        break;
                    }
                    String err = g.place(s, r * World.N + c);
                    if (err != null) System.out.println("X " + err);
                    else System.out.println(g.name(s) + " now stands at " + g.cellName(r * World.N + c) + ".");
                    status(g);
                }
                case "up", "u" -> {
                    int s = parts.length > 1 ? parseSuspect(parts[1]) : -1;
                    if (s < 0) { System.out.println("Usage: up <suspect 1-9>"); break; }
                    g.pickUp(s);
                    status(g);
                }
                case "board", "b" -> status(g);
                case "suspects", "list", "l" -> System.out.print(g.renderSuspects());
                case "brief" -> brief(g);
                case "legend" -> System.out.println(g.legend());
                case "hint", "h" -> { System.out.println(g.buyHint()); status(g); }
                case "check", "c" -> System.out.println(g.check());
                case "route", "r" -> System.out.println(g.toggleRoute());
                case "search", "s" -> {
                    if (parts.length < 2) { System.out.println("Usage: search <text>   e.g. search bed"); break; }
                    String q = line.substring(line.indexOf(' ') + 1);
                    List<String> hits = g.search(q);
                    if (hits.isEmpty()) System.out.println("No clue mentions that.");
                    else hits.forEach(System.out::println);
                }
                case "note" -> {
                    int s = parts.length > 1 ? parseSuspect(parts[1]) : -1;
                    if (s < 0 || parts.length < 3) { System.out.println("Usage: note <suspect 1-9> <text>"); break; }
                    g.addNote(s, parts[2]);
                    System.out.println("Noted for " + g.name(s) + ".");
                }
                case "notes" -> {
                    List<String> n = g.allNotes();
                    if (n.isEmpty()) System.out.println("(no notes yet)");
                    else n.forEach(System.out::println);
                }
                case "reset" -> { g.reset(); status(g); }
                case "lab" -> labMenu(g);
                case "time" -> System.out.println("Time " + fmt(g.elapsedSeconds()));
                case "help", "?" -> System.out.println(HELP);
                case "back", "q", "quit", "exit" -> { return false; }
                default -> System.out.println("Unknown command. Type 'help' for the list.");
            }
        }
    }

    private static void printWin(Game g, Game.WinResult w) {
        System.out.println();
        System.out.print(g.renderBoard());
        System.out.println("\n*** CASE SOLVED! ***");
        System.out.println(g.name(g.puzzle.murderer()) + " was the murderer, alone with " + g.name(g.victim()) + ".");
        System.out.println("Rating: " + starString(w.stars()) + "  (" + w.stars() + "/3)");
        System.out.println("Time " + fmt(w.secs()) + "  -  Hints " + g.hintsUsed);
        System.out.println("Leaderboard: rank " + w.rank() + " of " + w.totalSolves() + "  -  par (median) " + fmt(w.medianSecs()));
        System.out.println("+" + w.gain() + " points (total " + PROGRESS.points + ")");
    }
}
