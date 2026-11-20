package com.newuniverse.nusmp.client.weaponpage;

import java.util.List;

/**
 * Why: the Weapon page shows the weapon's own mastery bars and, under "Other masteries", every other mastery track the server really has (the Grimoire, the Beast Wills, the other weapons, the Saiyan form, the
 * mod's skills in training). The Codex page, the hub tab and the Status-screen tab all read the same list, so the rules for it live here as plain arithmetic with no game types and are tested
 * ({@code MasteryTracksTest}): a progress fraction is always inside 0..1, the list the player sees never holds more than {@link #MAX_TRACKS} rows (so a page cannot grow without bound), and the weapon that is
 * selected is not listed twice (its own bars are already shown above). Nothing here reads the game or the network; the client cooks the texts once per sync and calls these functions.
 */
public final class MasteryTracks {
    private MasteryTracks() {}

    /** The most rows "Other masteries" ever holds, and the most skills in training added to it. */
    public static final int MAX_TRACKS = 24, MAX_SKILLS = 4;
    /** The owner of a track that belongs to no weapon (Grimoire, Beast Will, Saiyan form, skills). */
    public static final int GLOBAL = -1;

    /** One row: its label, its value text (level and next-level progress, already translated), its fill 0..1 and the weapon page index it belongs to ({@link #GLOBAL} for none). */
    public record Track(String label, String value, float frac, int owner) {
        public Track {
            frac = clamp(frac);
        }
    }

    public static float clamp(float f) { return Float.isNaN(f) ? 0f : Math.max(0f, Math.min(1f, f)); }

    /** The fill of {@code have} out of {@code max}: 0 when max is not positive. */
    public static float frac(double have, double max) { return max > 0 ? clamp((float) (have / max)) : 0f; }

    /** A whole percent 0..100. */
    public static int pct(float frac) { return Math.round(clamp(frac) * 100f); }

    /** The rows shown while weapon {@code selected} is open: every track that is not that weapon's own, in order, never more than {@link #MAX_TRACKS}. */
    public static Track[] forSelection(List<Track> all, int selected) {
        Track[] out = new Track[Math.min(MAX_TRACKS, all.size())];
        int n = 0;
        for (Track t : all) {
            if (selected >= 0 && t.owner() == selected) continue;
            if (n == out.length) break;
            out[n++] = t;
        }
        return n == out.length ? out : java.util.Arrays.copyOf(out, n);
    }

    /**
     * The indexes of the skills worth showing: those with a mastery maximum that are not full yet, highest fraction first (ties: the lower index), at most {@code n}. {@code have} and {@code max} are parallel
     * arrays (one entry per skill the player has); nothing is allocated per skill beyond the small result.
     */
    public static int[] inTraining(double[] have, int[] max, int n) {
        int[] best = new int[Math.max(0, n)];
        int count = 0;
        for (int i = 0; i < have.length && i < max.length; i++) {
            if (max[i] <= 0 || have[i] >= max[i]) continue;
            float f = frac(have[i], max[i]);
            int at = count;
            while (at > 0 && frac(have[best[at - 1]], max[best[at - 1]]) < f) at--;
            if (at >= best.length) continue;
            int last = Math.min(count, best.length - 1);
            for (int j = last; j > at; j--) best[j] = best[j - 1];
            best[at] = i;
            if (count < best.length) count++;
        }
        return count == best.length ? best : java.util.Arrays.copyOf(best, count);
    }
}
