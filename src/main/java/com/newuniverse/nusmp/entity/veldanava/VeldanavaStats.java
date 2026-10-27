package com.newuniverse.nusmp.entity.veldanava;

/**
 * Why (3.30.0): the numbers of the anime-accurate Veldanava, in one place with no game class in it, so the boss, the self-test and the unit test read the same figures.
 * Veldanava is the Creator God of the Tensura world: he hits like a god, moves like a flash step, cannot be burned, poisoned, knocked back or affected by potions,
 * heals "at ultra speed" while he fights (unless he is hit by the counter-element), and has five million health.
 *
 * <p>Vanilla caps the MAX_HEALTH attribute at 1024. {@link HealthCap} lifts that cap for the attribute (so the boss really has 5,000,000 health); if that ever fails,
 * {@link #healthDivisor} turns the missing part into a damage divisor (the precedent of the guardian scaling in the mega-dungeon), so the fight is the same size either way.
 *
 * <p>Every one of these is applied only outside the classic mode ({@link VeldanavaConfig#classic()} restores the 3.11.0 - 3.29.0 Veldanava).
 */
public final class VeldanavaStats {
    private VeldanavaStats() {}

    // ---------------------------------------------------------------- the attributes
    /** Exactly five million: the magicule capacity of the anime Veldanava. */
    public static final double HEALTH = 5_000_000.0;
    /** The most the server may ask for (the attribute cap is lifted to this; float health keeps whole numbers up to 16.7 million exactly and stays usable above). */
    public static final double HEALTH_MAX = 1.0E9;
    /** One blow of this much (a real, true-damage hit of his plain attack) kills an unarmoured player outright and cripples a player in endgame armour. */
    public static final double ATTACK_DAMAGE = 80.0;
    /** A flash step: the vanilla zombie walks at 0.23, a sprinting player at about 0.13 + a quarter; this with the chase speed modifier is about 14 blocks a second. */
    public static final double MOVEMENT_SPEED = 0.55;
    /** Vanilla's own caps: armour 30, toughness 20. */
    public static final double ARMOR = 30.0, ARMOR_TOUGHNESS = 20.0;
    public static final double KNOCKBACK_RESISTANCE = 1.0, FOLLOW_RANGE = 96.0;

    /** The 3.11.0 - 3.29.0 numbers (classic mode). */
    public static final double CLASSIC_HEALTH = 4000.0, CLASSIC_ATTACK = 18.0, CLASSIC_SPEED = 0.36, CLASSIC_ARMOR = 12.0, CLASSIC_TOUGHNESS = 6.0, CLASSIC_FOLLOW = 64.0;

    /** Vanilla's MAX_HEALTH cap before {@link HealthCap} lifts it. */
    public static final double VANILLA_HEALTH_CAP = 1024.0;

    /** The health the boss starts with: the classic figure of the server (default 4000) in classic mode, otherwise the new one (default 5,000,000). */
    public static double health(boolean classic, double classicConfigured, double configured) { return classic ? Math.max(100.0, Math.min(1.0E6, classicConfigured)) : clampHealth(configured); }

    /** The configured health forced into what the server may ask for. */
    public static double clampHealth(double configured) { return Double.isNaN(configured) ? HEALTH : Math.max(100.0, Math.min(HEALTH_MAX, configured)); }

    /**
     * The damage divisor when the attribute could not reach the wanted health: 'wanted' health spread over what the attribute really holds ('actualMax'); 1 when it holds it
     * all. A blow of 'x' then takes x / divisor, a heal of 'x' gives x / divisor, and the health shown to the players is health * divisor.
     */
    public static double healthDivisor(double wanted, double actualMax) {
        if (!(actualMax > 0.0) || !(wanted > actualMax)) return 1.0;
        return wanted / actualMax;
    }

    /** What a blow takes once the divisor is applied. */
    public static float scaledDamage(float amount, double divisor) { return divisor <= 1.0 ? amount : (float) (amount / divisor); }

    /** The health the players are shown ("4,999,200 / 5,000,000"). */
    public static long shown(double health, double divisor) { return Math.max(0L, Math.round(health * Math.max(1.0, divisor))); }

    // ---------------------------------------------------------------- his plain attack
    /** The damage of one plain blow of the two human bodies: the attribute, times the body's and the server's scale. */
    public static float meleeDamage(double attackAttribute, float scale) { return (float) (Math.max(0.0, attackAttribute) * Math.max(0f, scale)); }

    /** Does a blow of this size kill a player with this much health outright? */
    public static boolean oneShots(float damage, double playerHealth) { return damage >= playerHealth; }

    // ---------------------------------------------------------------- Ultraspeed Regeneration
    /** Percent of his maximum health he heals per second while he fights (the server may change it, 0 = off). */
    public static final double REGEN_PERCENT_PER_SECOND = 0.1;
    /** A hit of the counter-element stops the regeneration for this long (ticks). */
    public static final int REGEN_BROKEN_TICKS = 120;
    /** He heals every this many ticks (one small heal, not one per tick). */
    public static final int REGEN_EVERY = 10;

    /** The health one regeneration step gives back: 'percent' of 'max' per second, for 'REGEN_EVERY' ticks. 0 for a bad figure. */
    public static float regenStep(double max, double percentPerSecond) {
        if (!(max > 0.0) || !(percentPerSecond > 0.0)) return 0f;
        return (float) (max * Math.min(100.0, percentPerSecond) / 100.0 * REGEN_EVERY / 20.0);
    }

    /** Is the regeneration working at tick 'now'? He must be fighting, whole, and not broken by a counter hit. */
    public static boolean regenerating(boolean fighting, boolean transforming, long now, long brokenUntil, float health, float max) {
        return fighting && !transforming && now >= brokenUntil && health > 0f && health < max;
    }

    /** The tick the regeneration is broken until after a counter hit at 'now' (never earlier than it already was). */
    public static long breakRegen(long now, long brokenUntil) { return Math.max(brokenUntil, now + REGEN_BROKEN_TICKS); }

    // ---------------------------------------------------------------- the nullifications
    /** The damage types (their path in the registry) he cannot be hurt by: fall, every kind of fire and lava, and the wither. (Poison and potions are status effects: those are refused by canBeAffected.) */
    public static boolean nullifiesDamage(String damageTypePath) {
        return switch (damageTypePath) {
            case "in_fire", "on_fire", "lava", "hot_floor", "fireball", "unattributed_fireball", "campfire", "fall", "wither" -> true;
            default -> false;
        };
    }
}
