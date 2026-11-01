package com.newuniverse.nusmp.devil;

/**
 * Why: every number and decision of the Devil Contract system (3.48.0) in one pure class, so it is tested without a world. Nothing here reads the game: the server code passes the numbers in.
 *
 * <ul>
 * <li><b>Assimilation</b> is a level from 0 to 100 percent. While the player has it switched on it climbs {@link #climb} percent a second (a Low devil needs 100 s, a Supreme one 40 s), while it is off it falls
 * {@link #DECAY} a second. Every second it costs {@link #drain} magicules: the cost at 100 % is ten times the cost at 0 %.</li>
 * <li><b>The stage</b> is what the body shows: 1 from 20 % (horns), 2 from 50 % (wings, and flight), 3 at 100 % (the Devil Union).</li>
 * <li><b>The Devil Union</b> lasts exactly {@link #unionTicks} (5 minutes by default), then the level drops to 0 and the union can not begin again for {@link #cooldownTicks} (24 in-game hours = 24 000 ticks by
 * default). Both are absolute game times stored with the player, so logging out does not pause them. While the cooldown runs the level can not pass {@link #CAP_WHILE_COOLING} percent.</li>
 * <li><b>Stats</b> (attack damage, movement speed) grow with a smooth curve from 0 at 0 % to the tier's full bonus at 100 %, 1.5 times that inside the union; both have a hard cap.</li>
 * </ul>
 */
public final class DevilRules {
    private DevilRules() {}

    // ---------------------------------------------------------------- the union and its cooldown
    public static final int DEFAULT_UNION_SECONDS = 300;
    public static final int DEFAULT_COOLDOWN_HOURS = 24;
    /** One in-game hour is 1000 ticks (24 000 ticks is one Minecraft day, 20 real minutes). */
    public static final long TICKS_PER_HOUR = 1000L;
    /** While the cooldown of the union runs the level stops here, so it never reaches 100. */
    public static final float CAP_WHILE_COOLING = 99f;
    /** In the union the drain is this share of the drain at 100 %. */
    public static final double UNION_DRAIN_SHARE = 0.25;
    /** In the union the stat bonuses are multiplied by this. */
    public static final double UNION_STAT_MULT = 1.5;

    public static long unionTicks(int seconds) { return clampInt(seconds, 20, 36000) * 20L; }

    public static long cooldownTicks(int hours) { return clampInt(hours, 0, 240) * TICKS_PER_HOUR; }

    /** The game time the union ends at when it begins now. */
    public static long unionEnd(long now, long unionTicks) { return now + Math.max(1L, unionTicks); }

    /** The game time the union may begin again, when it ended now. */
    public static long readyAfter(long now, long cooldownTicks) { return now + Math.max(0L, cooldownTicks); }

    public static boolean inUnion(long unionUntil) { return unionUntil > 0; }

    public static boolean unionDue(long unionUntil, long now) { return unionUntil > 0 && now >= unionUntil; }

    public static boolean cooling(long readyAt, long now) { return readyAt > now; }

    /** Whole seconds left until {@code at} (0 once it has passed; rounded up). */
    public static int secondsLeft(long at, long now) {
        if (at <= now) return 0;
        return (int) Math.min(Integer.MAX_VALUE, (at - now + 19L) / 20L);
    }

    /** The highest level allowed now: 100 when the union may begin, otherwise just under it. */
    public static float cap(boolean cooling) { return cooling ? CAP_WHILE_COOLING : 100f; }

    // ---------------------------------------------------------------- the climb
    /** Percent a second while the level is switched on. */
    public static float climb(DevilTier t) {
        return switch (t) { case LOW -> 1.0f; case MID -> 1.5f; case HIGH -> 2.0f; case SUPREME -> 2.5f; default -> 0f; };
    }

    /** Percent a second lost while it is switched off. */
    public static final float DECAY = 4.0f;

    /** The level after one second. {@code on} climbs (never past {@code cap}), off falls (never below 0). A level above the cap (the cooldown began) is pulled down to it. */
    public static float step(float level, boolean on, DevilTier t, float cap) {
        float next = on ? level + climb(t) : level - DECAY;
        next = Math.min(next, cap);
        return clampF(next, 0f, 100f);
    }

    /** Magicules a second at this level: the base of the tier at 0 %, ten times that at 100 %, rising with the square of the level. */
    public static double drain(DevilTier t, float level) {
        double x = clampF(level, 0f, 100f) / 100.0;
        return baseDrain(t) * (1.0 + 9.0 * x * x);
    }

    /** Magicules a second inside the union. */
    public static double unionDrain(DevilTier t) { return drain(t, 100f) * UNION_DRAIN_SHARE; }

    public static double baseDrain(DevilTier t) {
        return switch (t) { case LOW -> 10; case MID -> 20; case HIGH -> 35; case SUPREME -> 60; default -> 0; };
    }

    // ---------------------------------------------------------------- what the body shows
    public static final float HORNS_AT = 20f, WINGS_AT = 50f, UNION_AT = 100f;

    /** 0 nothing, 1 horns, 2 wings (and flight), 3 the union. */
    public static int stage(float level) { return level >= UNION_AT ? 3 : level >= WINGS_AT ? 2 : level >= HORNS_AT ? 1 : 0; }

    public static boolean flight(float level) { return level >= WINGS_AT; }

    /** The level in steps of five percent: the network sends a new picture of a player only when this, the tier or the union changes. */
    public static int bucket(float level) { return (int) (clampF(level, 0f, 100f) / 5f); }

    // ---------------------------------------------------------------- stats
    /** The smooth curve of every stat: 0 at 0, 1 at 1, slow at the start and heavy at the end. */
    public static double smooth(double x) {
        double c = clampD(x, 0, 1);
        return c * c * (3 - 2 * c);
    }

    public static double tierStrength(DevilTier t) {
        return switch (t) { case LOW -> 2; case MID -> 4; case HIGH -> 7; case SUPREME -> 12; default -> 0; };
    }

    public static double tierSpeed(DevilTier t) {
        return switch (t) { case LOW -> 0.10; case MID -> 0.20; case HIGH -> 0.30; case SUPREME -> 0.45; default -> 0; };
    }

    /** Hard caps: no setting, tier or union can pass these. */
    public static final double MAX_STRENGTH = 20.0, MAX_SPEED = 0.75;

    /** Attack damage added at this level ({@code scale} is the server setting, {@code union} the 1.5 multiplier). */
    public static double strength(DevilTier t, float level, boolean union, double scale) {
        double v = tierStrength(t) * smooth(level / 100.0) * (union ? UNION_STAT_MULT : 1.0) * clampD(scale, 0, 2);
        return clampD(v, 0, MAX_STRENGTH);
    }

    /** Movement speed added (as a multiplier of the total) at this level. */
    public static double speed(DevilTier t, float level, boolean union, double scale) {
        double v = tierSpeed(t) * smooth(level / 100.0) * (union ? UNION_STAT_MULT : 1.0) * clampD(scale, 0, 2);
        return clampD(v, 0, MAX_SPEED);
    }

    // ---------------------------------------------------------------- the contract
    /** The tier a contract price buys: the harder the price, the stronger the devil. The three old prices: tax Low, seal Mid, two hearts Mid. The new ones: an eye High, half the health Supreme. */
    public static DevilTier tierForPrice(String price) {
        if (price == null) return DevilTier.LOW;
        return switch (price) {
            case "half" -> DevilTier.SUPREME;
            case "eye_left", "eye_right" -> DevilTier.HIGH;
            case "seal", "maxhp" -> DevilTier.MID;
            default -> DevilTier.LOW;
        };
    }

    /** The sacrifice a price adds, or null when it is one of the three old prices (they stay as they were). */
    public static Sacrifice sacrificeForPrice(String price) {
        if (price == null) return null;
        return switch (price) {
            case "half" -> Sacrifice.HALF_HEALTH;
            case "eye_left" -> Sacrifice.LOST_EYE_LEFT;
            case "eye_right" -> Sacrifice.LOST_EYE_RIGHT;
            default -> null;
        };
    }

    /** Half of the total maximum health, as the multiplier of the sacrifice's modifier (ADD_MULTIPLIED_TOTAL). */
    public static final double HALF_HEALTH_MULT = -0.5;

    /** The field of view factor for the lost eyes: 1 with none, narrower for one, narrower still for two, softened by the player's own strength setting (0 to 1). */
    public static double fovFactor(int eyes, double strength) {
        double s = clampD(strength, 0, 1);
        return 1.0 - 0.12 * clampInt(eyes, 0, 2) * s;
    }

    /** Reach lost with an eye (blocks, subtracted from the interaction range) when the server setting is on. */
    public static double eyeReachPenalty(int eyes) { return 0.0 - 0.75 * clampInt(eyes, 0, 2) / 2.0; }   // 0.0 - x: no eye is +0.0, never -0.0

    // ---------------------------------------------------------------- the key
    /** Ticks between two asks from one player that the server answers. */
    public static final int MIN_ASK_GAP = 4;

    public static boolean askAllowed(long lastAsk, long now) { return now - lastAsk >= MIN_ASK_GAP || now < lastAsk; }

    // ---------------------------------------------------------------- helpers
    public static int clampInt(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    public static float clampF(float v, float lo, float hi) { return Float.isNaN(v) ? lo : Math.max(lo, Math.min(hi, v)); }

    public static double clampD(double v, double lo, double hi) { return Double.isNaN(v) ? lo : Math.max(lo, Math.min(hi, v)); }
}
