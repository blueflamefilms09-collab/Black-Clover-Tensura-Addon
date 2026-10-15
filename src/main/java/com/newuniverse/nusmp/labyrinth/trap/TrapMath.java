package com.newuniverse.nusmp.labyrinth.trap;

/**
 * Why: every number and every yes/no rule of the Labyrinth's traps lives here as plain arithmetic with no Minecraft class in it, so the
 * rules are unit-tested without a level (the pattern of {@code TrialMath} and {@code DoorFlood}) and the blocks stay thin: a block reads
 * the world, asks this class "what now", and applies the answer. Two traps are described. The HIDDEN PITFALL is a three-step machine
 * driven by the block's own scheduled ticks (a step arms a short crack delay, the delay opens the floor, a longer delay closes it again
 * once nothing stands in the hole), with the guard rail that only a survival or adventure PLAYER ever springs it: a creative builder, a
 * spectator or a mob wandering over the floor must never be the one who triggers it (mobs would empty every pit room before the first
 * party arrived). The SNUFFER GLYPH is a distance rule (a sphere, squared so no square root) plus a refresh rule for the Darkness
 * effect that only tops the effect up when it is about to fade, because re-applying a status effect every second restarts its fade-in
 * and makes the screen pulse; and a placement rule that decides whether a block the player is putting down is a light the glyph must refuse.
 */
public final class TrapMath {
    private TrapMath() {}

    // ---------------------------------------------------------------- pitfall
    /** Ticks between the first step on a pitfall tile and the floor giving way (a quarter second: long enough to hear it, short enough to be unfair). */
    public static final int CRACK_TICKS = 5;
    /** Ticks a pitfall stays open before it tries to close (five seconds). */
    public static final int OPEN_TICKS = 100;
    /** Ticks between two attempts to close while something still stands in the hole (one second). */
    public static final int RETRY_TICKS = 20;

    /** What a scheduled tick of a pitfall tile has to do. */
    public enum PitAction { OPEN, CLOSE, WAIT, NONE }

    /**
     * May this entity spring a pitfall? Only a player (a mob that walks over the floor must not), never in creative or spectator mode, and
     * never when the server switched traps off.
     */
    public static boolean springs(boolean isPlayer, boolean creative, boolean spectator, boolean trapsOn) {
        return trapsOn && isPlayer && !creative && !spectator;
    }

    /** Does a step on this tile start the crack delay? Safe tiles are the ordinary floor of the route; open tiles are already gone. */
    public static boolean stepArms(boolean safeTile, boolean open, boolean springs) {
        return springs && !safeTile && !open;
    }

    /**
     * The decision of a scheduled tick: a CLOSED tile that ticks was cracking, so it opens; an OPEN tile that ticks closes if nothing stands
     * in its cell, otherwise waits ({@link #RETRY_TICKS} later). A safe tile never opens and never schedules again (a stale tick on one is NONE).
     */
    public static PitAction onTick(boolean safeTile, boolean open, boolean occupied) {
        if (open) return occupied ? PitAction.WAIT : PitAction.CLOSE;
        return safeTile ? PitAction.NONE : PitAction.OPEN;
    }

    /** The delay to schedule after an action: opening waits {@link #OPEN_TICKS}, waiting retries, closing needs no further tick. */
    public static int nextDelay(PitAction a) {
        switch (a) {
            case OPEN: return OPEN_TICKS;
            case WAIT: return RETRY_TICKS;
            default: return 0;
        }
    }

    // ---------------------------------------------------------------- snuffer glyph
    /** How far a glyph's darkness reaches, in blocks. */
    public static final double DARKNESS_RADIUS = 6.0;
    /** How far a glyph refuses a new light, in blocks. */
    public static final int PLACE_RADIUS = 8;
    /** How far a glyph puts out torches already standing when it wakes, in blocks. */
    public static final int SNUFF_RADIUS = 4;
    /** The Darkness effect the glyph applies: ten seconds. */
    public static final int DARKNESS_TICKS = 200;
    /** The glyph checks once a second. */
    public static final int GLYPH_PERIOD = 20;

    /** Is the offset (dx, dy, dz) inside a sphere of this radius? Squared compare, a block on the boundary counts as inside. */
    public static boolean within(double dx, double dy, double dz, double radius) {
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }

    /**
     * Should the glyph top the Darkness effect up now? When the player has none (remaining 0 or less) or it is about to fade (under five
     * seconds): re-applying a status effect at full length every second restarts its fade and the screen pulses, so a healthy effect is
     * left alone.
     */
    public static boolean needsRefresh(int remainingTicks) { return remainingTicks < DARKNESS_TICKS / 2; }

    /** Is this placed block a light the glyph refuses? Anything that emits light, placed by a survival or adventure player. */
    public static boolean refusesLight(int lightEmission, boolean creative, boolean spectator, boolean trapsOn) {
        return trapsOn && lightEmission > 0 && !creative && !spectator;
    }

    /** Is a block at this squared distance from a glyph inside its no-light zone? */
    public static boolean inNoLightZone(double dx, double dy, double dz) { return within(dx, dy, dz, PLACE_RADIUS); }
}
