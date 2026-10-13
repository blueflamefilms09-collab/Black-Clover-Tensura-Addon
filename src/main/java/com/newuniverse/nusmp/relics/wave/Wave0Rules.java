package com.newuniverse.nusmp.relics.wave;

import java.util.Locale;

/**
 * Wave 0 of the Relics of Many Worlds (the sixteen items the mod already had, remade onto the relic system): the pure rules, with no
 * Minecraft types, so they are unit-tested. The cards are tabletop; this is where their rolls become Minecraft numbers.
 * <ul>
 *   <li><b>Saving throws</b>: a save is a d20 plus the target's toughness against a DC; the chance and the advantage / disadvantage maths live here.</li>
 *   <li><b>Per-day uses</b>: one Minecraft day (24000 ticks) is a long rest; "once until the next dawn" is one use a day.</li>
 *   <li><b>Charges</b>: Thundercleave's two charges (refilled at dawn, or by a thunderstorm), the Weapon of Resurgence's Lay on Hands pool,
 *       the Bloodbound Katana's Blood Charges and sips.</li>
 *   <li><b>Classes</b>: which damage is elemental, which creatures are constructs, who counts as surprised, how a whirlwind moves and how
 *       many creatures it can hold.</li>
 * </ul>
 */
public final class Wave0Rules {
    private Wave0Rules() {}

    public static final long DAY = 24000L;

    /** A source of dice: {@code next(n)} is a uniform number from 0 to n-1 (the game side backs it with the entity's random source). */
    public interface Dice {
        int next(int bound);
    }

    /** {@code n} dice of {@code sides} sides, summed. */
    public static int roll(Dice d, int n, int sides) {
        int sum = 0;
        for (int i = 0; i < Math.max(0, n); i++) sum += 1 + d.next(Math.max(1, sides));
        return sum;
    }

    public static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

    // ---------------------------------------------------------------- saving throws
    /**
     * A target's bonus to a saving throw: knockback resistance (0 to 1) is worth up to +6, every 40 points of maximum health +1 (to +6), every 2
     * points of armour toughness +1 (to +4), and a boss gets +4.
     */
    public static int saveMod(double knockbackResistance, double maxHealth, double toughness, boolean boss) {
        int m = (int) Math.round(clamp(knockbackResistance, 0, 1) * 6);
        m += (int) Math.min(6, Math.floor(Math.max(0, maxHealth) / 40.0));
        m += (int) Math.min(4, Math.floor(Math.max(0, toughness) / 2.0));
        if (boss) m += 4;
        return m;
    }

    /** The chance that d20 + mod meets the DC (a natural 1 or 20 always counts: never under 5% nor over 95%). */
    public static double saveChance(int dc, int mod) { return clamp((21 + mod - dc) / 20.0, 0.05, 0.95); }

    public static double advantage(double chance) { return 1.0 - (1.0 - chance) * (1.0 - chance); }

    public static double disadvantage(double chance) { return chance * chance; }

    /** One roll against a chance (0 to 1). */
    public static boolean passes(Dice d, double chance) { return d.next(1000) < (int) Math.round(clamp(chance, 0, 1) * 1000); }

    /** How many one-second rounds a condition of {@code ticks} lasts (at least one). */
    public static int rounds(int ticks) { return Math.max(1, (ticks + 19) / 20); }

    // ---------------------------------------------------------------- per-day uses ("until the next dawn")
    public static long day(long dayTime) { return Math.floorDiv(dayTime, DAY); }

    /** A day counter: which day it was last used and how many uses it has had since. */
    public record Uses(long day, int used) {}

    /** The counter after one more use of {@code max} a day, or null when none is left. */
    public static Uses takeUse(Uses cur, long today, int max) {
        int used = cur != null && cur.day() == today ? Math.max(0, cur.used()) : 0;
        if (used >= max) return null;
        return new Uses(today, used + 1);
    }

    public static int usesLeft(Uses cur, long today, int max) {
        int used = cur != null && cur.day() == today ? Math.max(0, cur.used()) : 0;
        return Math.max(0, max - used);
    }

    // ---------------------------------------------------------------- Thundercleave
    public static final int THUNDER_MAX = 2, STORM_REGEN_TICKS = 200;

    /** Charges after the day counter turned over since the last refill: a full pair. */
    public static int thunderAtDawn(int charges, long lastDay, long today) {
        int c = Math.max(0, Math.min(THUNDER_MAX, charges));
        return today != lastDay ? THUNDER_MAX : c;
    }

    /** Held aloft under a thunderstorm: a charge every {@link #STORM_REGEN_TICKS} ticks. */
    public static int thunderStorm(int charges, long ticksSinceLast) {
        int c = Math.max(0, Math.min(THUNDER_MAX, charges));
        return c < THUNDER_MAX && ticksSinceLast >= STORM_REGEN_TICKS ? c + 1 : c;
    }

    // ---------------------------------------------------------------- Weapon of Resurgence: the Lay on Hands pool (same numbers as the item's own pool)
    public static final int LAY_MAX = 40, LAY_REFILL_TICKS = 24, LAY_PER_USE = 10, LAY_CLEANSE = 5;

    /** The pool now: {@code stored} is the saved value (null if the blade has never been used: full), {@code at} when it was saved. */
    public static int layPool(Integer stored, long at, long now) {
        if (stored == null) return LAY_MAX;
        int p = stored;
        if (now > at) p += (int) ((now - at) / LAY_REFILL_TICKS);
        return Math.max(0, Math.min(LAY_MAX, p));
    }

    /** Hit points mended by spending {@code points} of the pool: two a point. */
    public static float layHeal(int points) { return 2f * Math.max(0, points); }

    // ---------------------------------------------------------------- Bloodbound Katana
    public static final int BLOOD_MAX = 10, FLARE_COST = 3, FLARE_PER_HIT = 2, SIP_FLOOR_HP = 4;

    /** The blood a sip costs the wielder, 1d4 hit points. */
    public static int sipCost(Dice d) { return roll(d, 1, 4); }

    /** A sip never takes the wielder under two hearts. */
    public static boolean canSip(float health, int cost) { return health - cost >= SIP_FLOOR_HP; }

    /** How many Bloodflares fit in one hit: three charges each, two at most. */
    public static int flares(int charges) { return Math.min(FLARE_PER_HIT, Math.max(0, charges) / FLARE_COST); }

    /** A full blade is unstable: from this many charges every hit costs the wielder a little. */
    public static boolean overflowing(int charges) { return charges >= BLOOD_MAX; }

    // ---------------------------------------------------------------- classes of damage and creature
    public enum Element { NONE, FIRE, COLD, LIGHTNING }

    /** The element of a damage type path ("on_fire", "freeze", "lightning_bolt", "tensura:ice_elemental"...). */
    public static Element elementOf(String damageTypeId) {
        if (damageTypeId == null) return Element.NONE;
        String s = damageTypeId.toLowerCase(Locale.ROOT);
        int colon = s.indexOf(':');
        if (colon >= 0) s = s.substring(colon + 1);
        if (s.contains("lightning") || s.contains("thunder")) return Element.LIGHTNING;
        if (s.contains("freez") || s.contains("frost") || s.contains("cold") || s.equals("ice") || s.startsWith("ice_") || s.contains("snow")) return Element.COLD;
        if ((s.contains("fire") && !s.equals("fireworks")) || s.contains("lava") || s.contains("burn") || s.contains("flame") || s.contains("hot_floor") || s.contains("heat")) return Element.FIRE;
        return Element.NONE;
    }

    /** The damage type id a spent Absorb Elements charges the next blow with. */
    public static String elementDamageType(Element e) {
        return switch (e) {
            case FIRE -> "minecraft:in_fire";
            case COLD -> "minecraft:freeze";
            case LIGHTNING -> "minecraft:lightning_bolt";
            default -> "minecraft:magic";
        };
    }

    /** The Tensura skill that resists an element (lent while Absorb Elements holds); "" for none. */
    public static String elementResistSkill(Element e) {
        return switch (e) {
            case FIRE -> "tensura:flame_attack_resistance";
            case COLD -> "tensura:cold_resistance";
            case LIGHTNING -> "tensura:electricity_resistance";
            default -> "";
        };
    }

    /** Constructs: golems, colossi, statues and the like: nothing living to bleed, rot or doom. */
    public static boolean isConstructPath(String entityTypePath) {
        if (entityTypePath == null) return false;
        String s = entityTypePath.toLowerCase(Locale.ROOT);
        return s.contains("golem") || s.contains("construct") || s.contains("colossus") || s.contains("statue") || s.contains("gargoyle")
                || s.contains("automaton") || s.contains("puppet") || s.contains("armor_stand") || s.contains("armorsaurus");
    }

    /** A creature is surprised when asleep, struck from behind, or hunting someone else (or no one). */
    public static boolean surprised(boolean asleep, boolean fromBehind, boolean isMob, boolean hasTarget, boolean targetsAttacker) {
        if (asleep || fromBehind) return true;
        return isMob && (!hasTarget || !targetsAttacker);
    }

    // ---------------------------------------------------------------- the Boreal whirlwind
    public static final int WHIRL_CAPACITY = 2;
    public static final double WHIRL_RADIUS = 2.2, WHIRL_SPEED = 0.2, WHIRL_MAX_MOVE = 9.0;
    public static final int WHIRL_TICKS = 240, WHIRL_STEP = 4;

    /** A large creature takes two places in the whirlwind (it holds two medium creatures or one large one). */
    public static int sizeWeight(double bbWidth, double bbHeight) { return bbWidth >= 1.4 || bbHeight >= 2.6 ? 2 : 1; }

    public static boolean fits(int usedWeight, int weight) { return usedWeight + weight <= WHIRL_CAPACITY; }

    /** One step of the whirlwind toward its goal on the flat: {x, z, remaining distance}. */
    public static double[] stepToward(double x, double z, double tx, double tz, double maxStep) {
        double dx = tx - x, dz = tz - z, d = Math.sqrt(dx * dx + dz * dz);
        if (d <= maxStep || d < 1.0e-6) return new double[]{tx, tz, 0};
        return new double[]{x + dx / d * maxStep, z + dz / d * maxStep, d - maxStep};
    }

    /** The goal of a move command: the aimed point, no more than {@link #WHIRL_MAX_MOVE} blocks from where the whirlwind stands. */
    public static double[] clampMove(double x, double z, double tx, double tz) {
        double dx = tx - x, dz = tz - z, d = Math.sqrt(dx * dx + dz * dz);
        if (d <= WHIRL_MAX_MOVE || d < 1.0e-6) return new double[]{tx, tz};
        return new double[]{x + dx / d * WHIRL_MAX_MOVE, z + dz / d * WHIRL_MAX_MOVE};
    }
}
