package com.newuniverse.nusmp.labyrinth.gate;

/**
 * Why: the decisions behind the gates and the void rescue ({@link LabyrinthTravel}, {@link LabyrinthEvents}) as plain numbers and booleans, so
 * they can be tested without the game (the pattern of {@code dungeon/DungeonGuardRules}). Everything that touches a level, a player or NBT
 * stays in the travel class; what is decided from the values read there is decided here. Pure.
 */
public final class GateRules {
    private GateRules() {}

    /** Ticks after a gate use during which no gate takes the player again (long enough to walk out of the block they were set down in). */
    public static final int GATE_COOLDOWN = 80;
    /** Damage of a void rescue. */
    public static final float RESCUE_DAMAGE = 4f;
    /** A second fall within this many ticks means the pad itself is unsafe: the player is taken out instead of put back. */
    public static final int RESCUE_RETRY_TICKS = 200;
    /** The y under which a player of the Labyrinth is falling into nothing (the dimension's min_y is 0; nothing is ever built under y = 40). */
    public static final double VOID_Y = 1.0;
    /** The void check runs every this many player ticks. */
    public static final int RESCUE_EVERY = 10;

    /** When a gate used at {@code now} stops refusing the player. */
    public static long cooldownUntil(long now) { return now + GATE_COOLDOWN; }

    /** Is a gate still refusing this player? Either stamp (ours, the legacy Maw's) in the future holds. */
    public static boolean cooldownHolds(long now, long ownStamp, long legacyStamp) { return now < ownStamp || now < legacyStamp; }

    /** Does the void check run on this tick of the player? */
    public static boolean checkDue(int tickCount) { return tickCount % RESCUE_EVERY == 0; }

    /** Is this player falling into the void and worth a rescue? Spectators fly where they like; the switch can turn the rescue off. */
    public static boolean falling(double y, boolean spectator, boolean rescueEnabled) { return rescueEnabled && !spectator && y < VOID_Y; }

    /** Does the rescue take the player OUT of the Labyrinth instead of back to the pad: no pad known, or a second fall within the retry window. */
    public static boolean rescueTakesOut(boolean hasPad, boolean rescuedBefore, long now, long lastRescue) {
        return !hasPad || (rescuedBefore && now - lastRescue < RESCUE_RETRY_TICKS);
    }

    /** Does the way back go home (bed or spawn) rather than to the stored origin: the origin's level is gone, or the origin is the Labyrinth itself. */
    public static boolean goesHome(boolean originLoaded, boolean originIsLabyrinth) { return !originLoaded || originIsLabyrinth; }
}
