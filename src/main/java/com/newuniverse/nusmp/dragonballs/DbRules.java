package com.newuniverse.nusmp.dragonballs;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Why: every number of the Dragon Ball system that a test, the config and the server logic must agree on lives here, pure: the ball count range, the phrase normaliser and its short rolling window,
 * the stone lock maths and the scatter picker. Nothing here knows Minecraft, so {@code DbRulesTest} runs without a game.
 */
public final class DbRules {
    private DbRules() {}

    /** Canon is seven balls; the owner asked for eight, so the setting allows 7 or 8 and every ball item up to 8 stars is registered. */
    public static final int CANON_COUNT = 7, MIN_COUNT = 7, MAX_COUNT = 8;

    public static int clampCount(int n) { return Math.max(MIN_COUNT, Math.min(MAX_COUNT, n)); }

    // ---------------------------------------------------------------- phrases
    /** Lower case, every character that is not a letter or digit becomes a space, runs of spaces collapse: "Takkaradoppo, Pakkoradoppo -- YAIKANAAN!" -> "takkaradoppo pakkoradoppo yaikanaan". */
    public static String norm(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder(Math.min(s.length(), 512));
        boolean gap = true;
        for (int i = 0; i < s.length() && i < 512; i++) {
            char c = s.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                b.append(Character.toLowerCase(c));
                gap = false;
            } else if (!gap) {
                b.append(' ');
                gap = true;
            }
        }
        int n = b.length();
        return n > 0 && b.charAt(n - 1) == ' ' ? b.substring(0, n - 1) : b.toString();
    }

    /** Does the spoken text contain the phrase as whole words (case and punctuation ignored)? */
    public static boolean contains(String spoken, String phrase) {
        String p = norm(phrase);
        if (p.isEmpty()) return false;
        return (" " + norm(spoken) + " ").contains(" " + p + " ");
    }

    public static boolean matchesAny(String spoken, List<? extends String> phrases) {
        if (phrases == null) return false;
        String s = " " + norm(spoken) + " ";
        for (String ph : phrases) {
            String p = norm(ph);
            if (!p.isEmpty() && s.contains(" " + p + " ")) return true;
        }
        return false;
    }

    /** Most messages the rolling window keeps per player (so a long phrase can be said in a few lines). */
    public static final int WINDOW_MAX = 4;

    /** The last few chat lines of ONE player inside a short time window; the words may be spoken over several lines. Pure and bounded (at most {@link #WINDOW_MAX} lines, each cut to 200 chars). */
    public static final class Window {
        private final long[] at = new long[WINDOW_MAX];
        private final String[] text = new String[WINDOW_MAX];
        private int n;

        public void add(long nowMs, String line) {
            if (line == null) return;
            if (line.length() > 200) line = line.substring(0, 200);
            if (n == WINDOW_MAX) {
                System.arraycopy(at, 1, at, 0, WINDOW_MAX - 1);
                System.arraycopy(text, 1, text, 0, WINDOW_MAX - 1);
                n--;
            }
            at[n] = nowMs;
            text[n] = line;
            n++;
        }

        /** The lines no older than windowMs, joined by a space. */
        public String joined(long nowMs, long windowMs) {
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < n; i++) if (nowMs - at[i] <= windowMs) b.append(text[i]).append(' ');
            return b.toString();
        }

        public void clear() { n = 0; }
    }

    // ---------------------------------------------------------------- the stone lock (real time, epoch milliseconds)
    public static final long MAX_LOCK_MINUTES = 10080;

    public static long lockUntil(long nowMs, long minutes) {
        long m = Math.max(0, Math.min(MAX_LOCK_MINUTES, minutes));
        return m == 0 ? 0L : nowMs + m * 60_000L;
    }

    public static boolean locked(long untilMs, long nowMs) { return untilMs > nowMs; }

    public static long remainingMs(long untilMs, long nowMs) { return Math.max(0L, untilMs - nowMs); }

    /** "58m 12s", "1h 03m", "45s". */
    public static String format(long ms) {
        long s = Math.max(0, (ms + 999) / 1000);
        long h = s / 3600, m = (s % 3600) / 60;
        if (h > 0) return h + "h " + (m < 10 ? "0" : "") + m + "m";
        if (m > 0) return m + "m " + (s % 60 < 10 ? "0" : "") + (s % 60) + "s";
        return s + "s";
    }

    // ---------------------------------------------------------------- the scatter picker
    /**
     * Picks {@code count} (x, z) points in the ring minR..maxR round (cx, cz), each at least {@code minSep} from the others. Bounded: at most {@code 60 * count} attempts; the separation is halved
     * every 20 failed tries, so it ALWAYS returns {@code count} points (at the worst a few closer than asked, never fewer).
     */
    public static List<int[]> scatter(Random r, int count, int cx, int cz, int minR, int maxR, int minSep) {
        int lo = Math.max(0, Math.min(minR, maxR)), hi = Math.max(lo + 1, maxR);
        List<int[]> out = new ArrayList<>(count);
        double sep = Math.max(0, minSep);
        int fails = 0;
        for (int tries = 0; out.size() < count && tries < 60 * count; tries++) {
            double ang = r.nextDouble() * Math.PI * 2, rad = Math.sqrt(lo * (double) lo + r.nextDouble() * (hi * (double) hi - lo * (double) lo));
            int x = cx + (int) Math.round(Math.cos(ang) * rad), z = cz + (int) Math.round(Math.sin(ang) * rad);
            boolean ok = true;
            for (int[] p : out) {
                double dx = p[0] - x, dz = p[1] - z;
                if (dx * dx + dz * dz < sep * sep) { ok = false; break; }
            }
            if (ok) { out.add(new int[]{x, z}); fails = 0; }
            else if (++fails >= 20) { sep /= 2; fails = 0; }
        }
        while (out.size() < count) out.add(new int[]{cx + r.nextInt(hi * 2 + 1) - hi, cz + r.nextInt(hi * 2 + 1) - hi});   // unreachable in practice: the halving always frees a spot
        return out;
    }
}
