package com.newuniverse.nusmp.labyrinth.dnd.economy;

/**
 * Why: the dungeon's resource clock (D&amp;D's torches) is a small state machine kept free of Minecraft types so the rules are testable without a server: the
 * GLOOM counter (seconds spent in the dark with nothing lit in hand: it climbs one a second in the dark, falls two a second in the light, so one step into a
 * torch-lit room does not wipe the warning but a real rest does). The player tick handler ({@link DungeonHunger}) feeds it one sample a second and applies the
 * verdict; keeping the numbers here means the balance (how long the dark is safe) lives in one tested file and the config keys only parametrise it.
 * One instance per player, held server-side in a map keyed by UUID and dropped when the player leaves the Labyrinth or logs out.
 * <p>1.99.0, THE HUNGER BAN: this class used to carry the second half of the clock, the HUNGER meter (the extra exhaustion a second of the Labyrinth added on
 * top of what the player had already spent, and the "three shanks or fewer" Slowness line). That half is removed for good, on the owner's explicit order: no
 * system of the mod may make the food bar drain faster or force the player to eat more often. The class and its id stay; the darkness half is unchanged.
 */
public final class GloomClock {
    /** What the clock says after a sample. */
    public enum State { SAFE, WARNING, GLOOM }

    private int dark;

    /** Seconds of accumulated darkness. */
    public int darkSeconds() { return dark; }

    /**
     * One sample a second. {@code inDark} means block light at the eyes is at or below the limit AND no light in either hand.
     * WARNING starts at three quarters of the allowed seconds (the player is told before the debuff), GLOOM at the full count.
     */
    public State step(boolean inDark, int allowedSeconds) {
        int limit = Math.max(1, allowedSeconds);
        dark = inDark ? Math.min(limit * 2, dark + 1) : Math.max(0, dark - 2);
        if (dark >= limit) return State.GLOOM;
        return dark * 4 >= limit * 3 ? State.WARNING : State.SAFE;
    }

    /** True for exactly the sample on which the warning first shows (so the actionbar line is said once, not every second). */
    public boolean warningEdge(State now, State before) { return now == State.WARNING && before == State.SAFE; }

    /** A sample's darkness test from plain numbers: dark light AND no light item in hand. */
    public static boolean isDark(int blockLightAtEyes, int darkLimit, boolean lightInHand) {
        return blockLightAtEyes <= darkLimit && !lightInHand;
    }
}
