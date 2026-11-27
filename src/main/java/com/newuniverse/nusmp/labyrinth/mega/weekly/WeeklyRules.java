package com.newuniverse.nusmp.labyrinth.mega.weekly;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the dungeon's weekly modifiers (replay value): every real-world week the whole server gets the same one to three twists on the creatures of the deeper floors, so the same floor is not
 * played the same way twice. Everything that DECIDES is here and pure: which week it is, how many twists a floor gets (none above the first threshold, one from it, two from the second, three
 * from the third) and which twists a week holds (a fixed shuffle of the list by the week's number, so every player and every server restart agree, and a week never repeats its neighbour's first
 * pick). The game side ({@link WeeklyEvents}) only reads these answers.
 */
public final class WeeklyRules {
    private WeeklyRules() {}

    /** One twist and the attribute change it makes to a creature. {@code percent} is the base strength (25 = +25 percent). Append only, so old weeks keep their meaning. */
    public enum Mod {
        THICK_HIDE("thick_hide", 25), SWIFT_FOES("swift_foes", 15), SAVAGE_BLOWS("savage_blows", 20), ARMOURED("armoured", 30), KEEN_SENSES("keen_senses", 40);

        private final String id;
        private final int percent;

        Mod(String id, int percent) { this.id = id; this.percent = percent; }

        public String id() { return id; }

        public int percent() { return percent; }
    }

    /** The week number of a day count since 1970 (a week starts on a Thursday, the epoch's weekday; only the number matters). */
    public static long weekOf(long epochDay) { return Math.floorDiv(epochDay, 7L); }

    /** How many twists a floor gets: 0 below {@code one}, 1 from it, 2 from {@code two}, 3 from {@code three} (thresholds that are out of order are read as sorted). */
    public static int countFor(int floor, int one, int two, int three) {
        int a = Math.min(one, Math.min(two, three)), c = Math.max(one, Math.max(two, three)), b = one + two + three - a - c;
        if (floor < a) return 0;
        if (floor < b) return 1;
        if (floor < c) return 2;
        return 3;
    }

    /** The first {@code count} twists of a week's fixed shuffle (count is clamped to 0..all). Same week, same answer, on every machine. */
    public static List<Mod> pick(long week, int count) {
        Mod[] all = Mod.values();
        int n = Math.max(0, Math.min(count, all.length));
        List<Mod> pool = new ArrayList<>(List.of(all));
        long state = mix(week ^ 0x5EEDC0FFEEL);
        List<Mod> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            state = mix(state + i);
            out.add(pool.remove((int) Math.floorMod(state, (long) pool.size())));
        }
        return out;
    }

    /** The strength of a twist after the server's scale (100 = as listed), at least 1 percent while the scale is above 0. */
    public static int scaledPercent(Mod m, int scalePercent) {
        if (scalePercent <= 0) return 0;
        return Math.max(1, Math.round(m.percent() * scalePercent / 100f));
    }

    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
