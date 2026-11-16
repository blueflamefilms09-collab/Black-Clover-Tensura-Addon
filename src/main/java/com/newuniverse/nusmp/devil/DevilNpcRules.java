package com.newuniverse.nusmp.devil;

/**
 * Why: the pure numbers and decisions of the devil NPC (docs/devil_forms.md): the figure of the devil that stands in front of the caller while the ritual runs and the offer is open. No Minecraft type, so it is
 * unit tested without a game; {@code DevilNpcs} (server) and {@code DevilNpc} (the entity) read these, the client renderer reads the state numbers and the animation lengths.
 *
 * <p>The figure lives from the start of the ritual to the end of the 30 second offer, and is gone sooner when the caller signs (it merges into them), refuses, times out, dies, logs out, changes dimension or
 * breaks the ritual. A safety lifetime ({@link #lifetimeTicks}, 60 s by default) ends it whatever else happens. At most one per player and a server-wide cap.
 */
public final class DevilNpcRules {
    private DevilNpcRules() {}

    /** The state of the figure (synced): rising out of the circle, waiting, making its offer (the roar), merging into the signer, dissolving. Append only. */
    public static final int RISING = 0, WAITING = 1, OFFER = 2, MERGE = 3, DISSOLVE = 4, STATES = 5;

    /** How long each state's animation runs, in ticks (the client plays them, the server ends the terminal states after them). */
    public static final int RISE_TICKS = 30, ROAR_TICKS = 40, MERGE_TICKS = 20, DISSOLVE_TICKS = 30;

    /** The figure checks its owner and the contract once every this many ticks (not every tick). */
    public static final int CHECK_EVERY = 10;

    public static final int DEFAULT_MAX = 8, MAX_MAX = 64, DEFAULT_LIFE_SECONDS = 60, MIN_LIFE_SECONDS = 10, MAX_LIFE_SECONDS = 180;

    /** How far in front of the caller it stands, and how far from the owner it may be before it vanishes (blocks). */
    public static final double FRONT = 3.0, MAX_DISTANCE = 48.0;

    /** The checks and what they decide. */
    public static final int KEEP = 0, DISSOLVE_NOW = 1, VANISH_NOW = 2;

    public static int clampState(int s) { return s < 0 ? 0 : s >= STATES ? STATES - 1 : s; }

    public static boolean terminal(int state) { return state == MERGE || state == DISSOLVE; }

    public static long lifetimeTicks(int seconds) { return DevilRules.clampInt(seconds, MIN_LIFE_SECONDS, MAX_LIFE_SECONDS) * 20L; }

    public static int clampMax(int max) { return DevilRules.clampInt(max, 0, MAX_MAX); }

    /** Whether a figure may be made for a ritual: the setting is on, the caller has none yet, and the server holds fewer than {@code max}. */
    public static boolean mayStart(boolean enabled, boolean ownerHasOne, int live, int max) { return enabled && !ownerHasOne && max > 0 && live < max; }

    /** How many ticks a terminal state lasts before the figure is removed. */
    public static int endTicks(int state) { return state == MERGE ? MERGE_TICKS : DISSOLVE_TICKS; }

    /**
     * What the figure's own check decides: it vanishes at once when its owner is gone or it is past its lifetime; it dissolves when nothing keeps it (the ritual and the offer are over, and it is not already
     * merging or dissolving); otherwise it stays.
     */
    public static int check(boolean ownerHere, boolean contractLive, int state, long age, long lifeTicks) {
        if (!ownerHere || age >= lifeTicks) return VANISH_NOW;
        if (!contractLive && !terminal(state)) return DISSOLVE_NOW;
        return KEEP;
    }

    /** The tick of the ritual at which line {@code i} (0 to 2) is said; line 3 is said when the offer is made. Always distinct, always inside the ritual. */
    public static int lineTick(int i, int ritualTicks) {
        int t = Math.max(8, ritualTicks);
        return switch (i) {
            case 0 -> Math.max(2, t * 15 / 100);
            case 1 -> Math.max(3, t * 45 / 100);
            default -> Math.max(4, t * 75 / 100);
        };
    }

    public static final int LINES = 4;
}
