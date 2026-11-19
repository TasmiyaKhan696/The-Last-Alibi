package com.lastalibi.game;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Player progress, stored in {@code ~/.the-last-alibi.properties}
 * (the console version's replacement for the browser's localStorage).
 */
public final class Progress {
    public static final int START_POINTS = 50;

    public int points = START_POINTS;
    /** Best star rating per level index (case * 3 + difficulty). */
    public final Map<Integer, Integer> stars = new TreeMap<>();
    /** Every solve time in seconds, used by the AVL leaderboard. */
    public final List<Integer> times = new ArrayList<>();

    private final Path file;

    public Progress() { this(Paths.get(System.getProperty("user.home", "."), ".the-last-alibi.properties")); }

    public Progress(Path file) {
        this.file = file;
        load();
    }

    private void load() {
        if (!Files.isRegularFile(file)) return;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            p.load(in);
            points = Integer.parseInt(p.getProperty("points", String.valueOf(START_POINTS)));
            for (String k : p.stringPropertyNames())
                if (k.startsWith("stars.")) stars.put(Integer.parseInt(k.substring(6)), Integer.parseInt(p.getProperty(k)));
            String t = p.getProperty("times", "");
            if (!t.isBlank()) for (String s : t.split(",")) times.add(Integer.parseInt(s.trim()));
        } catch (IOException | NumberFormatException e) {
            // A damaged save file just means we start fresh.
        }
    }

    public void save() {
        Properties p = new Properties();
        p.setProperty("points", String.valueOf(points));
        stars.forEach((k, v) -> p.setProperty("stars." + k, String.valueOf(v)));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times.size(); i++) sb.append(i > 0 ? "," : "").append(times.get(i));
        p.setProperty("times", sb.toString());
        try (OutputStream out = Files.newOutputStream(file)) {
            p.store(out, "The Last Alibi progress");
        } catch (IOException e) {
            // Saving is best effort.
        }
    }

    public int starsFor(int level) { return stars.getOrDefault(level, 0); }

    /** Easy is always open; Medium and Hard open once the previous level of that case has been cleared. */
    public boolean unlocked(int caseIndex, int difficulty) {
        return difficulty == 0 || starsFor(caseIndex * 3 + difficulty - 1) > 0;
    }
}
