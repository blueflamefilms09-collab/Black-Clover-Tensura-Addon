package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.newuniverse.nusmp.dungeon.nexus.zone.HiveTactics.Kind;

import java.util.ArrayList;
import java.util.List;

/**
 * How the swarm of the Tempest Mirage is brought in: a zone cap on the number alive, waves that refill it while players are in the village,
 * which kinds come and where they appear (never within sight of the players: out of reach of them at least). Pure.
 */
public final class HiveWaves {
    private HiveWaves() {}

    /** The most that live in the village at once, how many are made when it is built, the biggest wave, the gap between waves (ticks), the least distance from a player a creature appears at. */
    public static final int CAP = 40, FIRST = 26, WAVE_SIZE = 8, WAVE_PERIOD = 600, SAFE_DIST = 16;
    /** A trap brings this many (at least, at most), whatever the cap says, but never takes the swarm past the cap by more than OVERFLOW. */
    public static final int AMBUSH_MIN = 3, AMBUSH_MAX = 5, OVERFLOW = 6;

    private static final Kind[] KINDS = {Kind.ORC, Kind.LIZARDMAN, Kind.SLIME, Kind.GOBLIN, Kind.HOUND};
    private static final int[] WEIGHTS = {34, 24, 24, 10, 8};

    /** The kind for a roll between 0 (included) and 1 (excluded): orcs, lizardmen and slimes mostly, goblins and hound dogs as flavour. */
    public static Kind kindFor(double roll) {
        int total = 0;
        for (int w : WEIGHTS) total += w;
        double at = Math.max(0.0, Math.min(0.999999, roll)) * total;
        int acc = 0;
        for (int i = 0; i < KINDS.length; i++) {
            acc += WEIGHTS[i];
            if (at < acc) return KINDS[i];
        }
        return KINDS[0];
    }

    /** How many to bring now: nothing with nobody in the village, nothing at the cap or before the wave is due, else a wave (smaller if the cap is near). */
    public static int due(int alive, long now, long lastWave, int playersInside) {
        if (playersInside <= 0 || alive >= CAP) return 0;
        if (lastWave != Long.MIN_VALUE && now >= lastWave && now - lastWave < WAVE_PERIOD) return 0;
        return Math.min(WAVE_SIZE, CAP - alive);
    }

    /** How many a sprung trap brings: more for a bigger party, at least AMBUSH_MIN, at most AMBUSH_MAX, and never past the cap plus the overflow. */
    public static int ambushSize(int alive, int players) {
        int want = Math.max(AMBUSH_MIN, Math.min(AMBUSH_MAX, 2 + players));
        return Math.max(0, Math.min(want, CAP + OVERFLOW - alive));
    }

    /**
     * Which of the spots to use for `count` creatures: those at least SAFE_DIST from every player, in a stable order that depends on the seed
     * (so one place is not always filled first). Fewer are returned if fewer qualify. Spots and players are {x, z}.
     */
    public static List<Integer> pickSpots(double[][] spots, double[][] players, int count, long seed) {
        List<Integer> ok = new ArrayList<>();
        for (int i = 0; i < spots.length; i++) {
            boolean far = true;
            for (double[] p : players) if (Math.hypot(p[0] - spots[i][0], p[1] - spots[i][1]) < SAFE_DIST) far = false;
            if (far) ok.add(i);
        }
        ok.sort((a, b) -> Integer.compare(TempestRng.hash(seed, a, 0, 41), TempestRng.hash(seed, b, 0, 41)));
        return new ArrayList<>(ok.subList(0, Math.min(Math.max(0, count), ok.size())));
    }
}
