package com.newuniverse.nusmp.labyrinth.mega.enc;

import java.util.UUID;

/**
 * Why: a wandering boss is only a game if the players can win by being clever, and the rules of what it can perceive are the rules of that
 * game. It does not know where the party is. It SEES a player who is in its line of sight and inside a range that shrinks when the player
 * crouches (half), stands in the dark (to 0.6), or stands still (to 0.8): a party that crouches in the shadows of a side room is nearly
 * invisible, one that sprints through a lit hall is seen from across it. Anyone closer than a few blocks is noticed whatever they do (nobody
 * hides from something standing next to them), and nobody in a safe room is noticed at all. It HEARS sudden loud things (a block broken, a fight,
 * an explosion): the louder, the farther, up to the hearing range, and it goes to look, but a footstep is not heard from across the sector.
 * These functions are the whole of it, as plain arithmetic on a few numbers, so "hiding works" is a tested property rather than a hope. The
 * spawn chance is here too (it rises with the sector's threat and with the time since the last wanderer). Pure.
 */
public final class WandererRules {
    private WandererRules() {}

    /** PATROL walks the room graph; INVESTIGATE goes to a sound; CHASE follows a player it noticed; SEARCH looks round where it lost one. */
    public enum State { PATROL, INVESTIGATE, CHASE, SEARCH }

    /**
     * The numbers of perception and of the timers (the config supplies the ranges and the lose time, the rest are fixed by {@link #of}).
     * {@code burstThreshold} is the least loudness that turns its head; {@code closeRange} is where hiding stops working.
     */
    public record Tuning(double sightRange, double hearRange, double burstThreshold, double closeRange, double sneakFactor, double darkFactor, double stillFactor,
                         int loseTicks, int investigateTicks, int searchTicks) {}

    public static Tuning of(double sightRange, double hearRange, int loseTicks) {
        return new Tuning(Math.max(4.0, sightRange), Math.max(8.0, hearRange), 1.0, 3.0, 0.5, 0.6, 0.8, Math.max(20, loseTicks), 400, 600);
    }

    /** A player as the wanderer perceives it on one pass. {@code distance} is the straight distance, the flags describe the player. */
    public record Sighting(UUID id, double x, double y, double z, double distance, boolean lineOfSight, boolean sneaking, boolean dark, boolean still, boolean safe) {}

    /** The loudest thing it heard on one pass: where, how far from it, and how loud at the source. */
    public record Heard(double x, double y, double z, double distance, double loudness) {}

    // ---------------------------------------------------------------- sight
    /** How far a player can be seen at: the sight range, times 0.5 for crouching, 0.6 in the dark and 0.8 standing still. */
    public static double effectiveSight(Tuning t, boolean sneaking, boolean dark, boolean still) {
        double s = t.sightRange();
        if (sneaking) s *= t.sneakFactor();
        if (dark) s *= t.darkFactor();
        if (still) s *= t.stillFactor();
        return s;
    }

    /** Does the wanderer notice this player now? Never in a safe room, never without a line of sight, always when close enough to touch. */
    public static boolean notices(Tuning t, Sighting s) {
        if (s.safe() || !s.lineOfSight()) return false;
        if (s.distance() <= t.closeRange()) return true;
        return s.distance() <= effectiveSight(t, s.sneaking(), s.dark(), s.still());
    }

    // ---------------------------------------------------------------- hearing
    /** How far a sound of this loudness carries to a wanderer: the hearing range at loudness 6 (a blast), a quarter of it at 1.5 and below. */
    public static double hearingRadius(Tuning t, double loudness) {
        return t.hearRange() * Math.max(0.25, Math.min(1.0, loudness / 6.0));
    }

    /** Is a sound of this loudness, this far away, heard at all? */
    public static boolean hears(Tuning t, double loudness, double distance) {
        return loudness >= t.burstThreshold() && distance <= hearingRadius(t, loudness);
    }

    /** How attractive a heard sound is: loud and close beats quiet and far. */
    public static double score(double loudness, double distance) { return loudness / (1.0 + distance / 10.0); }

    // ---------------------------------------------------------------- appearing
    /**
     * The chance, at one check, that a new wanderer appears in a sector with players: nothing during the minimum gap since the last one, then the
     * base chance, rising to double over two more gaps, and by eight percent per threat level above one (at most two and a half times), never
     * above one half.
     */
    public static double spawnChance(double base, int threat, long ticksSinceLast, long minGap) {
        if (!(base > 0.0) || ticksSinceLast < minGap) return 0.0;
        double ramp = minGap <= 0 ? 2.0 : Math.min(2.0, 1.0 + (ticksSinceLast - minGap) / (double) (minGap * 2));
        double t = Math.min(2.5, 1.0 + 0.08 * Math.max(0, threat - 1));
        return Math.min(0.5, base * ramp * t);
    }
}
