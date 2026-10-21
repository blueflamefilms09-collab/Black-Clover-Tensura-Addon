package com.newuniverse.nusmp.labyrinth.mega.lockdown;

import com.newuniverse.nusmp.labyrinth.mega.enc.Band;

/**
 * Why: "which rooms lock, and how hard is the wave inside" is a taste the owner will keep tuning, so it is one small value per room in a data file
 * ({@code data/nusmp/mega/lockdown/rooms.json}) instead of a constant in code. {@code lock} says whether a room that matches closes at all (a puzzle room, a
 * trap room and a cavern deliberately do not: their own mechanics or their open shape are the challenge), {@code chance} lets a common kind (the halls) lock
 * for a share of its rooms only (the share is decided once per room from the sector's seed, so a room is either a challenge room or not, always), {@code band}
 * is the size of trouble the wave brings (the same bands as the Dungeon Master's dice), {@code waves} how many waves must be beaten one after the other,
 * {@code minFloor} the shallowest floor at which the room locks (the first floors are the tutorial) and {@code minArea} the smallest footprint, in blocks,
 * that locks (a closet is not an arena). Immutable, pure (the band is a plain enum of the Dungeon Master).
 */
public record LockdownRule(boolean lock, double chance, Band band, int waves, int minFloor, int minArea, String note) {
    public static final int MAX_WAVES = 3;

    public LockdownRule {
        chance = Math.max(0.0, Math.min(1.0, chance));
        if (band == null || band == Band.NOTHING) band = Band.STANDARD;
        waves = Math.max(1, Math.min(MAX_WAVES, waves));
        minFloor = Math.max(1, minFloor);
        minArea = Math.max(0, minArea);
        note = note == null ? "" : note;
    }

    /** The rule of a room nothing in the table mentions: it does not lock. */
    public static final LockdownRule NONE = new LockdownRule(false, 0.0, Band.STANDARD, 1, 1, 0, "not a challenge room");

    /** A copy with another band (the depth raised it). */
    public LockdownRule withBand(Band b) { return new LockdownRule(lock, chance, b, waves, minFloor, minArea, note); }

    /** A copy with another wave count. */
    public LockdownRule withWaves(int w) { return new LockdownRule(lock, chance, band, w, minFloor, minArea, note); }
}
