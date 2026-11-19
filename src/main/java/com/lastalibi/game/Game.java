package com.lastalibi.game;

import com.lastalibi.algo.*;
import com.lastalibi.game.Cases.Case;

import java.util.*;
import java.util.stream.Collectors;

/** One play session of one generated case: the placement state plus every game action. */
public final class Game {
    public static final int HINT_COST = 25;

    public final int caseIndex, difficulty, levelIndex;
    public final long seed;
    public final World world;
    public final Solver solver;
    public final Generator generator;
    public final Analysis analysis;
    public final Generator.Puzzle puzzle;
    public final Progress progress;

    /** pos[suspect] = square index or -1 when not yet placed. */
    public final int[] pos = new int[9];
    public int selected = -1;
    public int hintsUsed;
    public boolean solved;
    public boolean showRoute;
    public String message = "";
    public List<Integer> hintCells = List.of();

    public final Analysis.HintEngine hintEngine;
    public RBT notes = new RBT();
    public final AVL leaderboard = new AVL();
    private final Trie trie = new Trie();
    private final List<String> clueLines = new ArrayList<>();
    private long startNanos;

    public record WinResult(int stars, int gain, int rank, int totalSolves, int medianSecs, int secs) {}

    /**
     * @param levelIndex case*3+difficulty for a campaign level, or -1 for a "Surprise case".
     */
    public Game(int caseIndex, int difficulty, long seed, int levelIndex, Progress progress) {
        this.caseIndex = caseIndex;
        this.difficulty = difficulty;
        this.seed = seed;
        this.levelIndex = levelIndex;
        this.progress = progress;
        this.world = new World(caseIndex);
        this.solver = new Solver(world);
        this.generator = new Generator(world, solver);
        this.analysis = new Analysis(world, solver);
        this.puzzle = generator.generate(seed, difficulty);
        this.hintEngine = analysis.newHintEngine();
        for (int t : progress.times) leaderboard.insert(t);
        buildSearch();
        reset();
    }

    public Case theCase() { return world.theCase; }
    public String name(int s) { return world.cast[s].name(); }
    public String shortName(int s) { return world.cast[s].shortName(); }
    public int victim() { return puzzle.victim(); }

    public int elapsedSeconds() { return (int) ((System.nanoTime() - startNanos) / 1_000_000_000L); }

    public void reset() {
        Arrays.fill(pos, -1);
        selected = -1;
        hintCells = List.of();
        message = "";
        hintsUsed = 0;
        solved = false;
        showRoute = false;
        hintEngine.reset();
        startNanos = System.nanoTime();
    }

    public List<Clue> cluesOf(int s) {
        return puzzle.clues().stream().filter(k -> k.w == s).collect(Collectors.toList());
    }

    /** The one global line printed above the cards (e.g. "2 empty rooms."). */
    public String globalClue() {
        return puzzle.clues().stream().filter(k -> k.w < 0).findFirst().map(Clue::text).orElse("");
    }

    // ------------------------------------------------------------------ moves
    private int crossBy(int p, int self) {
        for (int j = 0; j < 9; j++)
            if (pos[j] >= 0 && j != self && pos[j] != p
                && (pos[j] / World.N == p / World.N || pos[j] % World.N == p % World.N)) return j;
        return -1;
    }

    /** Places suspect s on square p. Returns an error text, or null on success. */
    public String place(int s, int p) {
        if (solved) return "The case is already closed.";
        if (p < 0 || p >= 81) return "That square is off the plan.";
        if (world.block(p)) return "Nobody can stand there (courtyard or solid furniture).";
        int j = crossBy(p, s);
        if (j >= 0) return world.cast[j].shortName() + " already stands in that row or column. Pick them up first.";
        for (int o = 0; o < 9; o++) if (o != s && pos[o] == p) pos[o] = -1;
        pos[s] = p;
        selected = -1;
        hintCells = List.of();
        message = "";
        solved = true;
        for (int i = 0; i < 9; i++) if (pos[i] != puzzle.sol()[i]) { solved = false; break; }
        return null;
    }

    public void pickUp(int s) {
        if (solved) return;
        pos[s] = -1;
        if (selected == s) selected = -1;
    }

    /** True when suspect i breaks the one-per-row/column rule or one of their own clues. */
    public boolean flagged(int i) {
        if (pos[i] < 0) return false;
        for (int j = 0; j < 9; j++)
            if (j != i && pos[j] >= 0 && (pos[j] / World.N == pos[i] / World.N || pos[j] % World.N == pos[i] % World.N))
                return true;
        for (Clue k : puzzle.clues()) if ((k.w == i || k.o == i) && !k.f(pos)) return true;
        return false;
    }

    // ------------------------------------------------------------------ hint / check / route
    /** Buys a hint (Fibonacci-heap powered). Returns the hint text or an explanation of why not. */
    public String buyHint() {
        if (solved) return "The case is already closed.";
        if (progress.points < HINT_COST) return "A hint costs " + HINT_COST + " points and you have " + progress.points + ".";
        Analysis.Hint h = hintEngine.next(puzzle, pos);
        if (h == null) return "No hint available.";
        progress.points -= HINT_COST;
        progress.save();
        hintsUsed++;
        selected = h.suspect();
        hintCells = h.cells();
        List<String> own = cluesOf(h.suspect()).stream().map(Clue::text).collect(Collectors.toList());
        String clueText = own.isEmpty()
            ? "They have no clue of their own, so use the other suspects."
            : "Their clue: " + String.join(" ", own);
        String tail = h.cells().size() == 1
            ? "Only ONE square fits: " + cellName(h.cells().get(0)) + "."
            : h.cells().size() + " squares fit: " + h.cells().stream().map(this::cellName).collect(Collectors.joining(" ")) + ". Pick the one that also matches the other clues.";
        message = "Hint: " + name(h.suspect()) + " is selected for you. " + clueText + " " + tail;
        return message;
    }

    public String check() {
        message = "Check: " + analysis.feasibility(puzzle, pos).msg();
        return message;
    }

    public String toggleRoute() {
        showRoute = !showRoute;
        if (!showRoute) { message = ""; return "Route hidden."; }
        List<Integer> placed = new ArrayList<>();
        for (int p : pos) if (p >= 0) placed.add(p);
        Analysis.Route r = analysis.routePlan(placed);
        StringBuilder sb = new StringBuilder("Detective route through " + r.order().size() + " placed suspects: tour length " + r.len());
        if (r.opt() > 0) sb.append(String.format(", true optimum %d (ratio %.2f, guaranteed at most 2).", r.opt(), (double) r.len() / r.opt()));
        else sb.append('.');
        if (r.order().size() > 1)
            sb.append("\nVisit order: ").append(r.order().stream().map(this::cellName).collect(Collectors.joining(" -> ")));
        message = sb.toString();
        return message;
    }

    // ------------------------------------------------------------------ search and notes
    private void buildSearch() {
        for (Clue k : puzzle.clues()) {
            String line = k.text();
            clueLines.add(line);
            for (String w : line.toLowerCase().split("[^a-z]+")) if (w.length() > 1) trie.insert(w);
        }
        for (Cases.Person p : world.cast) trie.insert(p.shortName().toLowerCase());
        notes = new RBT();
    }

    /** KMP clue search plus trie autocomplete suggestions. */
    public List<String> search(String query) {
        String q = query.toLowerCase().trim();
        List<String> out = new ArrayList<>();
        if (q.isEmpty()) return out;
        String[] ws = q.split("\\s+");
        List<String> sug = trie.prefix(ws[ws.length - 1]);
        if (!sug.isEmpty()) out.add("Suggestions: " + String.join(", ", sug.subList(0, Math.min(8, sug.size()))));
        for (int i = 0; i < puzzle.clues().size(); i++) {
            Clue k = puzzle.clues().get(i);
            if (!Alg.kmp(clueLines.get(i).toLowerCase(), q).isEmpty()) {
                String who = k.w >= 0 ? shortName(k.w) + ": " : "";
                out.add("  - " + who + clueLines.get(i));
            }
        }
        return out;
    }

    public void addNote(int s, String text) { notes.put(name(s), text); }

    public List<String> allNotes() {
        List<String> out = new ArrayList<>();
        notes.each((k, v) -> { if (v != null && !v.isEmpty()) out.add(k + ": " + v); });
        return out;
    }

    // ------------------------------------------------------------------ winning
    public int medianSolveTime() {
        int[] a = progress.times.stream().mapToInt(Integer::intValue).toArray();
        if (a.length == 0) return 0;
        return Alg.quickselect(a, a.length >> 1);
    }

    public WinResult win() {
        int secs = elapsedSeconds();
        int rank = leaderboard.rank(secs) + 1;
        progress.times.add(secs);
        leaderboard.insert(secs);
        int med = medianSolveTime();
        int stars = hintsUsed > 0 ? 1 : secs < Cases.PAR[difficulty] ? 3 : 2;
        double factor = levelIndex >= 0 && progress.starsFor(levelIndex) == 0 ? 1 : .4;
        int gain = (int) Math.round(new int[]{20, 35, 55}[difficulty] * stars * factor);
        progress.points += gain;
        if (levelIndex >= 0) progress.stars.merge(levelIndex, stars, Math::max);
        progress.save();
        return new WinResult(stars, gain, rank, progress.times.size(), med, secs);
    }

    // ------------------------------------------------------------------ text rendering
    public String cellName(int p) { return "(r" + (p / World.N + 1) + ",c" + (p % World.N + 1) + ")"; }

    private static char symbol(String o, boolean solid) {
        char c = switch (o) {
            case "BED" -> 'b';
            case "TV" -> 'v';
            case "CHAIR" -> 'c';
            case "PLANT" -> 'p';
            case "SHELF" -> 's';
            case "CARPET" -> 'r';
            case "TABLE" -> 't';
            default -> '.';
        };
        return solid ? Character.toUpperCase(c) : c;
    }

    /**
     * ASCII floor plan. Each square is {@code RFS}: room letter, furniture symbol (UPPERCASE = solid,
     * nobody can stand there), then the suspect number, an {@code x} when the row or column is already
     * used, or {@code *} when the square is a hint candidate.
     */
    public String renderBoard() {
        final int N = World.N;
        boolean[] rowUsed = new boolean[N], colUsed = new boolean[N];
        for (int s = 0; s < 9; s++)
            if (pos[s] >= 0) { rowUsed[pos[s] / N] = true; colUsed[pos[s] % N] = true; }
        Set<Integer> highlight = new HashSet<>(hintCells);
        StringBuilder sb = new StringBuilder("      ");
        for (int c = 0; c < N; c++) sb.append(" c").append(c + 1).append(' ');
        sb.append('\n');
        String letters = "KLHGMW";
        for (int r = 0; r < N; r++) {
            sb.append("  r").append(r + 1).append("  ");
            for (int c = 0; c < N; c++) {
                int p = r * N + c;
                int rm = world.room(p);
                if (rm < 0) { sb.append(" ~~~"); continue; }
                String o = world.obj(p);
                char third = ' ';
                int who = -1;
                for (int s = 0; s < 9; s++) if (pos[s] == p) who = s;
                if (who >= 0) third = (char) ('1' + who);
                else if (highlight.contains(p)) third = '*';
                else if (!world.block(p) && (rowUsed[r] || colUsed[c])) third = 'x';
                sb.append(' ').append(letters.charAt(rm)).append(symbol(o, world.isSolid(o))).append(third);
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    public String legend() {
        StringBuilder sb = new StringBuilder("Rooms: ");
        String letters = "KLHGMW";
        for (int i = 0; i < 6; i++) sb.append(letters.charAt(i)).append('=').append(world.rooms[i]).append(i < 5 ? "  " : "");
        sb.append("\nFurniture (UPPERCASE = solid, nobody can stand there): ");
        sb.append("b/B bed, v/V TV, c/C chair, p/P plant, s/S shelf, r/R carpet, t/T table");
        sb.append("\nSquare = room, furniture, suspect no. | x = row or column already used | ~~~ = courtyard");
        sb.append("\nClue words here: ");
        sb.append(Arrays.stream(World.FURNITURE).map(f -> f.toLowerCase() + "=" + world.word(f))
            .collect(Collectors.joining(", ")));
        return sb.toString();
    }

    public String renderSuspects() {
        StringBuilder sb = new StringBuilder();
        for (int s = 0; s < 9; s++) {
            sb.append(String.format("%d. %-26s %s%s%n", s + 1, name(s),
                pos[s] >= 0 ? cellName(pos[s]) : "(not placed)",
                flagged(s) ? "  <-- breaks a rule" : ""));
            if (s == puzzle.victim()) sb.append("     The Victim.\n");
            for (Clue k : cluesOf(s)) sb.append("     - ").append(k.text()).append('\n');
        }
        return sb.toString();
    }
}
