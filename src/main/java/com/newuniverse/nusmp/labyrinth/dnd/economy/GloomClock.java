package com.newuniverse.nusmp.labyrinth.dnd.economy;

/**
 * Why: the dungeon's resource clock (D&amp;D's torches and rations) is two small state machines kept free of Minecraft types so the rules are
 * testable without a server: the GLOOM counter (seconds spent in the dark with nothing lit in hand: it climbs one a second in the dark,
 * falls two a second in the light, so one step into a torch-lit room does not wipe the warning but a real rest does) and the HUNGER meter
 * (the extra exhaustion a second of the Labyrinth adds on top of what the player already spent, and the "three shanks or fewer" Slowness
 * line). The player tick handler ({@link DungeonHunger}) feeds it one sample a second and applies the verdict; keeping the numbers here means
 * the balance (how long the dark is safe, how hard hunger bites) lives in one tested file and the config keys only parametrise it.
 * One instance per player, held server-side in a map keyed by UUID and dropped when the player leaves the Labyrinth or logs out.
 */
public final class GloomClock {
    /** What the clock says after a sample. */
    public enum State { SAFE, WARNING, GLOOM }

    /** Food level at or below which (3 shanks) the hunger Slowness applies. */
    public static final int STARVING_FOOD = 6;

    private int dark;
    private float lastExhaustion = Float.NaN;

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

    /**
     * The extra exhaustion to add this second so hunger drains {@code drain} times faster. The player's own exhaustion counter wraps at 4
     * (food drops one point and 4 is subtracted), so a lower reading than last time means one wrap. The first sample only records.
     */
    public float extraExhaustion(float exhaustionNow, double drain) {
        float last = lastExhaustion;
        lastExhaustion = exhaustionNow;
        if (Float.isNaN(last) || drain <= 1.0) return 0f;
        float spent = exhaustionNow - last;
        if (spent < 0f) spent += 4f;
        if (spent <= 0f || spent > 4f) return 0f;
        return (float) (spent * (drain - 1.0));
    }

    /** After the extra was applied, remember the new level so the next sample does not count the extra as the player's own spending. */
    public void settle(float exhaustionAfter) { lastExhaustion = exhaustionAfter; }

    /** Is the player hungry enough (3 shanks or fewer) for the Slowness line. */
    public static boolean starving(int foodLevel) { return foodLevel <= STARVING_FOOD; }

    /** A sample's darkness test from plain numbers: dark light AND no light item in hand. */
    public static boolean isDark(int blockLightAtEyes, int darkLimit, boolean lightInHand) {
        return blockLightAtEyes <= darkLimit && !lightInHand;
    }
}
