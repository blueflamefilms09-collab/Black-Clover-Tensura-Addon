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

    // ---------------------------------------------------------------- 3.76.0: what a devil's attribute does in a fight
    /**
     * The combat effect of an attribute, on the blows of a host who has let the devil in. {@code bonus}: extra damage as a share of the blow at 100 % assimilation with a Supreme devil;
     * {@code effectId}: a mob effect put on the target ("" = none; "nusmp:burn" = fire); {@code heal}: share of the blow given back to the host; {@code strip}: the target loses its
     * protective effects. Pure data, read by {@code DevilCombat}.
     */
    public record Effect(double bonus, String effectId, int ticks, int amp, double heal, boolean strip, String blurb) {}

    private static final Effect NO_EFFECT = new Effect(0, "", 0, 0, 0, false, "");

    public static Effect effect(DevilAttribute a) {
        if (a == null) return NO_EFFECT;
        return switch (a) {
            case NONE -> NO_EFFECT;
            case GRAVITY -> new Effect(0.30, "minecraft:slowness", 60, 2, 0, false, "Your blows fall with a devil's weight: the foe is crushed to a crawl.");
            case SPATIAL -> new Effect(0.55, "", 0, 0, 0, false, "Your blows cut through space: a great deal of extra damage.");
            case CURSE -> new Effect(0.25, "minecraft:wither", 80, 1, 0, false, "Your blows carry a curse that eats the foe.");
            case ANTI_MAGIC -> new Effect(0.30, "", 0, 0, 0, true, "Your blows cancel magic: the foe loses its protections.");
            case FLAME -> new Effect(0.35, "nusmp:burn", 120, 0, 0, false, "Your blows burn with hellfire.");
            case WORD -> new Effect(0.20, "minecraft:weakness", 80, 1, 0, false, "Your word binds: the foe is weakened.");
            case TIME -> new Effect(0.20, "minecraft:slowness", 30, 4, 0, false, "Your blows stop the foe's time for a moment.");
            case BEAST -> new Effect(0.50, "", 0, 0, 0, false, "The beast bites with you: a great deal of extra damage.");
            case FORESIGHT -> new Effect(0.35, "minecraft:glowing", 100, 0, 0, false, "You have seen this blow land: it strikes where the foe is open, and marks it.");
            case GHOST -> new Effect(0.15, "minecraft:slowness", 50, 5, 0, false, "Unseen hands seize the foe and hold it.");
            case SOUL -> new Effect(0.25, "", 0, 0, 0.25, false, "The demon feeds: a quarter of each blow comes back to you as health.");
            case ROT -> new Effect(0.20, "minecraft:poison", 100, 1, 0, false, "Your blows rot what they touch.");
            case EARTH -> new Effect(0.35, "minecraft:slowness", 40, 1, 0, false, "Your blows land like falling rock.");
            case WATER -> new Effect(0.25, "minecraft:mining_fatigue", 80, 1, 0, false, "Your blows drown the foe's strength.");
            case LIGHT -> new Effect(0.40, "minecraft:glowing", 100, 0, 0, false, "Your blows burn with light, and the foe is marked.");
            case ETHER -> new Effect(0.55, "", 0, 0, 0, false, "Raw ether bursts from your blows.");
            case CAUSALITY -> new Effect(0.60, "", 0, 0, 0.10, false, "The current of causality carries your blows: the greatest extra damage, and a little life back.");
        };
    }

    /** The assimilation from which the attribute acts. */
    public static final float EFFECT_FROM = 20f;
    /** Ticks between two attribute effects of one host (a sweep or a fast weapon cannot stack them). */
    public static final int EFFECT_GAP = 20;

    /** How strongly the effect acts, 0..1.5: nothing under 20 %, growing with the level, the tier's share, times 1.5 in the union. */
    public static double effectPower(DevilTier t, float level, boolean union) {
        if (level < EFFECT_FROM) return 0.0;
        double tier = switch (t) { case LOW -> 0.4; case MID -> 0.6; case HIGH -> 0.8; case SUPREME -> 1.0; default -> 0.0; };
        return tier * smooth(clampF(level, 0f, 100f) / 100.0) * (union ? 1.5 : 1.0);
    }

    /** The extra damage of one blow: the blow times the attribute's bonus times the power, never more than three times the blow. */
    public static float effectBonus(float blow, Effect e, double power, double scale) {
        if (!(blow > 0f) || e == null || power <= 0) return 0f;
        return (float) Math.min(blow * 3.0, blow * e.bonus() * power * clampD(scale, 0.0, 10.0));
    }

    /** May the effect act again? */
    public static boolean effectReady(long last, long now) { return now - last >= EFFECT_GAP || now < last; }

    /** The Brand of Sacrifice: what a monster's blow becomes for a branded host. */
    public static float branded(float blow) { return blow > 0f ? blow * 1.2f : blow; }

    /** A piece of the body: two hearts of maximum health. */
    public static final double FLESH_HEALTH = -4.0;
}
