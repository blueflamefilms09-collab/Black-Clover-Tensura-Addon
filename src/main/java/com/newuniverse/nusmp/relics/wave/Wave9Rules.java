package com.newuniverse.nusmp.relics.wave;

/**
 * Relics of Many Worlds, wave 9 (ten hand-held weapons): the pure rules, with no Minecraft imports, so they can be tested alone. Every number
 * the weapons use lives here: the tabletop's saving throws, dice and "rests" become the cooldowns, caps and chances below.
 * <p>
 * Tabletop becomes Minecraft: a short rest is a few minutes, a long rest a Minecraft day (24000 ticks), a d6 is rolled from the game's own random
 * source (the callers pass the roll in, so the maths stays deterministic here), a Constitution save is a shorter effect on a boss and a
 * full one on everything else.
 */
public final class Wave9Rules {
    private Wave9Rules() {}

    public static final long DAY = 24000L;
    /** A creature with at least this much maximum health counts as a boss: it resists the long effects by half. */
    public static final double BOSS_HEALTH = 100.0;

    // ---------------------------------------------------------------- Hyperborean Crossbow: Flash Freeze
    public static final int FROST_SLOW_TICKS = 60, FROST_SLOW_AMP = 2, FROST_PATCH_TICKS = 1200, FROST_ARMED_TICKS = 600, FROST_PATCH_CAP = 12;
    public static final double FROST_PATCH_RADIUS = 2.5;

    public static boolean isBoss(double maxHealth) { return maxHealth >= BOSS_HEALTH; }

    /** How long a Flash Freeze or an Entangle lasts on a creature of this size: halved on a boss. */
    public static int controlTicks(int base, double victimMaxHealth) { return isBoss(victimMaxHealth) ? Math.max(1, base / 2) : base; }

    // ---------------------------------------------------------------- Null Blade
    public static final int AWAKEN_KILLS = 20, MAX_CHARGES = 3, EYE_RANGE = 5, EYE_COOLDOWN = 200, FROZEN_TICKS = 100, RESTRAIN_TICKS = 50;
    /** Seconds in game ticks during which a restrained attacker that strikes again is frozen. */
    public static final int FREEZE_WINDOW = 100;

    public static boolean awakened(int kills) { return kills >= AWAKEN_KILLS; }

    /**
     * The maximum health the Null Blade takes from a creature on one hit: half a heart per pip of the d6 (0.5 to 3 hearts of health points
     * = 1 to 6 hp), never more than a share of the creature's maximum health in total (30% of a player, 50% of a mob; 8 hp at most on a player).
     *
     * @param already  health points already drained from the creature and not yet given back
     * @param roll     1 to 6
     */
    public static double drain(double already, double victimMaxHealth, boolean victimIsPlayer, int roll) {
        double want = Math.max(1, Math.min(6, roll));
        double cap = victimIsPlayer ? Math.min(8.0, victimMaxHealth * 0.30) : victimMaxHealth * 0.50;
        return Math.max(0.0, Math.min(want, cap - Math.max(0.0, already)));
    }

    /** How long the drained maximum health stays gone: the game's "until a long rest" is two minutes on a player, five on a mob. */
    public static int drainTicks(boolean victimIsPlayer) { return victimIsPlayer ? 2400 : 6000; }

    /** Temporary hit points the wielder gains from a drain: the amount taken, up to a cap that keeps them honest. */
    public static double tempFromDrain(double drained, double current) { return Math.max(0.0, Math.min(10.0, current + Math.max(0.0, drained)) - current); }

    // ---------------------------------------------------------------- Hunger
    public static final int DEVOUR_ARMED_TICKS = 400, STARVE_AFTER = 3600, GNAW_EVERY = 400;
    public static final double GNAW_FLOOR = 4.0;

    /** Temporary hit points a Devouring hit gives: as much as the damage dealt, up to 10 (5 hearts) against a mob and 6 against a player. */
    public static double devourTemp(double damageDealt, boolean victimIsPlayer) { return Math.max(0.0, Math.min(victimIsPlayer ? 6.0 : 10.0, damageDealt)); }

    /**
     * The curse of Hunger: if it has not drawn blood for three minutes it gnaws its wielder, half a heart every 20 seconds, and never below two hearts.
     *
     * @return health points to take now (0 if none)
     */
    public static double gnaw(long now, long lastFed, long lastGnaw, double health) {
        if (now - lastFed < STARVE_AFTER || now - lastGnaw < GNAW_EVERY) return 0.0;
        return health > GNAW_FLOOR + 1.0 ? 1.0 : 0.0;
    }

    // ---------------------------------------------------------------- Caesar's Gladius
    public static final int GLADIUS_COOLDOWN = 5 * (int) DAY, GLADIATOR_LIFE = 6000, ROGUE_LIFE = 3600, GLADIATORS = 3;

    /** The one-in-ten chance that the gladiators remember the Republic. */
    public static boolean rebels(double roll01) { return roll01 < 0.10; }

    /** When they turn: between 30 and 90 seconds after being called. */
    public static int rebelDelay(int roll) { return 600 + Math.floorMod(roll, 1200); }

    // ---------------------------------------------------------------- Axe of the Exile
    public static final double HOME_RADIUS = 160.0;
    public static final int EXILE_EVERY = 100;

    /** True while the position lies inside the forbidden circle round home (same dimension only; the caller checks that). */
    public static boolean inExile(double dx, double dz) { return dx * dx + dz * dz <= HOME_RADIUS * HOME_RADIUS; }

    /** Exhaustion added to the food bar each {@link #EXILE_EVERY} ticks inside the circle: about a doubling of the hunger rate. */
    public static float exileExhaustion() { return 0.8f; }

    // ---------------------------------------------------------------- Twin Swords of Deflection
    public static final int DEFLECT_TICKS = 40;

    /** A critical on a 19 or 20: one chance in ten. */
    public static boolean twinCrit(double roll01) { return roll01 < 0.10; }

    // ---------------------------------------------------------------- Viper Whip
    public static final int MARK_TICKS = 12000;
    public static final double MARK_RANGE = 1000.0;

    /** The compass point of a bearing from (0,0) to (dx,dz) in Minecraft's axes (north is -z, east is +x). */
    public static String compass(double dx, double dz) {
        if (Math.abs(dx) < 0.5 && Math.abs(dz) < 0.5) return "here";
        double deg = Math.toDegrees(Math.atan2(dx, -dz));
        if (deg < 0) deg += 360.0;
        String[] pts = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        return pts[(int) Math.floor((deg + 22.5) / 45.0) % 8];
    }

    // ---------------------------------------------------------------- Thornblight
    public static final int THORN_TICKS = 240, THORN_WINDOW = 200;

    /** The curse's price: one point of damage for a swing that connects, but never the last two hearts. */
    public static boolean thornPrice(double health) { return health > 5.0; }
}
