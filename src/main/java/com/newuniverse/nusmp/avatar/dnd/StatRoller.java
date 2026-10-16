package com.newuniverse.nusmp.avatar.dnd;

import java.util.Arrays;
import java.util.Random;

/**
 * Why: the owner wants the Avatar's six scores rolled the tabletop way (4d6, drop the lowest) and upgraded to 5d6 drop the two lowest for a lucky creator.
 * The roll must happen on the server with the server's own random source so a client can never pick its scores; this class is the pure arithmetic of it
 * (no Minecraft), seeded by the caller, so it is unit tested with fixed seeds.
 */
public final class StatRoller {
    private StatRoller() {}

    /** Rolls {@code dice} six-sided dice and keeps the highest {@code keep} of them. */
    public static int rollOne(Random rng, int dice, int keep) {
        int[] d = new int[dice];
        for (int i = 0; i < dice; i++) d[i] = 1 + rng.nextInt(6);
        Arrays.sort(d);
        int sum = 0;
        for (int i = dice - 1; i >= dice - keep && i >= 0; i--) sum += d[i];
        return sum;
    }

    /** Six scores: 4d6 drop the lowest, or (lucky creator) 5d6 drop the two lowest. Both keep the best three dice. */
    public static int[] roll(Random rng, boolean fiveDice) {
        int[] out = new int[Stat.values().length];
        for (int i = 0; i < out.length; i++) out[i] = fiveDice ? rollOne(rng, 5, 3) : rollOne(rng, 4, 3);
        return out;
    }

    /** True when the creator's total luck is high enough for the 5d6 roll (strictly above the threshold, as the owner wrote "Total Luck > 10"). */
    public static boolean usesFiveDice(double totalLuck, int threshold) { return totalLuck > threshold; }

    public static int total(int[] stats) {
        int s = 0;
        for (int v : stats) s += v;
        return s;
    }
}
