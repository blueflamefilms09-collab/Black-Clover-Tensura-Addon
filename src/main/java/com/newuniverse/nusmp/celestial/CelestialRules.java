package com.newuniverse.nusmp.celestial;

import java.util.List;

/**
 * Why: the decisions of Celestial Spirit Magic (does a summon go through, what does it cost, how strong is the spirit, which Star Dresses are open, what drops where,
 * when does the set of twelve count as complete) are numbers and comparisons. They are written here with no game class in them, so a unit test can check them and the
 * game code (the skill, the items, the loot, the Gate) only asks. Everything is keyed on the ordinals of {@link CelestialKey}, so the save format is a bit mask.
 */
public final class CelestialRules {
    private CelestialRules() {}

    /** The least a summon ever costs (magicules), so a small pool still pays something. */
    public static final double COST_FLOOR = 40.0;
    /** The ritual of a contract: ticks the player holds the key, and the share of the maximum magicules it takes. */
    public static final int BIND_TICKS = 60;
    public static final double BIND_COST_FRAC = 0.03;
    /** The highest bond level and the bond points that reach each level. */
    public static final int MAX_BOND = 5;
    private static final int[] BOND_AT = {0, 3, 8, 16, 30, 50};
    /** Star Dress: how long it lasts, what it costs, how long before the next one. */
    public static final int DRESS_TICKS = 1200, DRESS_COOLDOWN = 2400;
    public static final int DRESS_COST_PERMILLE = 150;
    /** Spirits that may stand at once: 1, 2 from half mastery, 3 from nine tenths. */
    public static final double SECOND_SPIRIT_AT = 0.5, THIRD_SPIRIT_AT = 0.9;
    /** A boss takes this share of a spirit's damage (the balance law's boss cap, in the same spirit as BalanceLaw.isBoss users). */
    public static final float BOSS_DAMAGE_FACTOR = 0.6f;

    // ================================================================ collection
    /** The mask with this key added. */
    public static int collect(int mask, CelestialKey key) { return mask | key.bit(); }

    public static boolean has(int mask, CelestialKey key) { return (mask & key.bit()) != 0; }

    /** True when all twelve Gold Zodiac keys are in the mask. */
    public static boolean completes(int mask) { return (mask & CelestialKey.GOLD_MASK) == CelestialKey.GOLD_MASK; }

    /** How many of the twelve gold keys the mask holds. */
    public static int goldCount(int mask) { return Integer.bitCount(mask & CelestialKey.GOLD_MASK); }

    /** The gold keys the mask lacks, in order. */
    public static List<CelestialKey> missingGold(int mask) {
        java.util.ArrayList<CelestialKey> out = new java.util.ArrayList<>();
        for (CelestialKey k : CelestialKey.gold()) if (!has(mask, k)) out.add(k);
        return List.copyOf(out);
    }

    // ================================================================ bond
    public static int bondLevel(int xp) {
        int lvl = 0;
        for (int i = 1; i < BOND_AT.length; i++) if (xp >= BOND_AT[i]) lvl = i;
        return lvl;
    }

    /** Bond points needed for the next level, or -1 at the top. */
    public static int bondNext(int xp) {
        int lvl = bondLevel(xp);
        return lvl >= MAX_BOND ? -1 : BOND_AT[lvl + 1];
    }

    // ================================================================ the contract (bind ritual)
    public enum Bind { OK, NOT_A_SPIRIT_KEY, ALREADY_BOUND, SOMEONE_ELSES, NOT_ENOUGH_MAGICULES }

    /**
     * Whether the contract may be sealed: the key must call a spirit; a key already bound to this player needs nothing more; a key whose item names another owner is refused; the
     * magicules must cover the ritual.
     */
    public static Bind canBind(CelestialKey key, boolean alreadyBound, String itemOwner, String player, double magicules, double maxMagicules) {
        if (key == null || !key.summons()) return Bind.NOT_A_SPIRIT_KEY;
        if (itemOwner != null && !itemOwner.isEmpty() && !itemOwner.equals(player)) return Bind.SOMEONE_ELSES;
        if (alreadyBound) return Bind.ALREADY_BOUND;
        if (magicules < bindCost(maxMagicules)) return Bind.NOT_ENOUGH_MAGICULES;
        return Bind.OK;
    }

    public static double bindCost(double maxMagicules) { return Math.max(COST_FLOOR, maxMagicules * BIND_COST_FRAC); }

    // ================================================================ the summon
    public enum Refusal { NONE, NOT_A_SPIRIT_KEY, NO_KEY, NOT_BOUND, SOMEONE_ELSES, COOLDOWN, NOT_ENOUGH_MAGICULES, TOO_MANY_SPIRITS }

    /** The first reason a summon is refused, or NONE. */
    public static Refusal check(CelestialKey key, boolean keyHeld, boolean bound, String itemOwner, String player, int cooldownLeft, double magicules, double cost,
                                int spiritsOut, int spiritCap) {
        if (key == null || !key.summons()) return Refusal.NOT_A_SPIRIT_KEY;
        if (!keyHeld) return Refusal.NO_KEY;
        if (itemOwner != null && !itemOwner.isEmpty() && !itemOwner.equals(player)) return Refusal.SOMEONE_ELSES;
        if (!bound) return Refusal.NOT_BOUND;
        if (cooldownLeft > 0) return Refusal.COOLDOWN;
        if (spiritsOut >= spiritCap) return Refusal.TOO_MANY_SPIRITS;
        if (magicules < cost) return Refusal.NOT_ENOUGH_MAGICULES;
        return Refusal.NONE;
    }

    public static int spiritCap(double masteryFrac) { return masteryFrac >= THIRD_SPIRIT_AT ? 3 : masteryFrac >= SECOND_SPIRIT_AT ? 2 : 1; }

    /** Magicules one summon costs: a share of the maximum, three percent less per bond level, never under the floor. */
    public static double cost(CelestialKey key, double maxMagicules, int bond) {
        return Math.max(COST_FLOOR, maxMagicules * key.costPermille / 1000.0 * (1.0 - 0.03 * clampBond(bond)));
    }

    /** Ticks before the key may be used again: four percent shorter per bond level. */
    public static int cooldown(CelestialKey key, int bond) { return (int) Math.round(key.cooldown * (1.0 - 0.04 * clampBond(bond))); }

    /** The spirit's damage in HP: its base power times the caster's EP scale (1..12), five percent per bond level, and the mastery curve (0.85..1.15). */
    public static float power(CelestialKey key, double epScale, int bond, double masteryFrac) {
        double m = Math.max(0, Math.min(1, masteryFrac));
        return (float) (key.power * epScale * (1.0 + 0.05 * clampBond(bond)) * (0.85 + 0.3 * m));
    }

    /** A boss takes less: damage is cut by {@link #BOSS_DAMAGE_FACTOR}. */
    public static float vsBoss(float damage, boolean boss) { return boss ? damage * BOSS_DAMAGE_FACTOR : damage; }

    /**
     * Ticks of hard control (chains, sleep, root, stagger) on a target: the spirit's own duration, cut by its boss factor on a boss and capped at one second there;
     * the balance law's caps (BalanceLaw.controlTicks) are applied on top by the caller.
     */
    public static int controlTicks(CelestialKey key, int base, boolean boss) {
        if (!boss) return base;
        return Math.min(20, Math.round(base * key.bossFactor));
    }

    private static int clampBond(int b) { return Math.max(0, Math.min(MAX_BOND, b)); }

    // ================================================================ Star Dress
    /**
     * A Star Dress opens when the key is gold and bound, the bond with its spirit is at least 2, and the mastery of the skill reaches 20% plus 4% for every place
     * of the key in the zodiac (Aquarius 20% ... Libra 64%): the dresses open one after another as the skill grows.
     */
    public static boolean starDressUnlocked(CelestialKey key, boolean bound, int bond, double masteryFrac) {
        if (key == null || !key.hasStarDress() || !bound) return false;
        return bond >= 2 && masteryFrac >= dressMastery(key) - 1e-9;
    }

    public static double dressMastery(CelestialKey key) { return 0.20 + 0.04 * key.ordinal(); }

    public static double dressCost(CelestialKey key, double maxMagicules) { return Math.max(COST_FLOOR * 2, maxMagicules * DRESS_COST_PERMILLE / 1000.0); }

    // ================================================================ drops
    public enum Source { VAULT_CHEST, CHEST, COURT_MOB, BOSS_MOB, MOB }

    /** Gold keys do not appear above the first two floors. */
    public static final int GOLD_FROM_FLOOR = 3;

    /** The chance that a roll of this source drops a key at all (0..1), before luck: 6% vault, 1.2% chest, 1% court mob, 5% boss, 0.15% any other mob. */
    public static double dropChance(Source source, int floor, int luck) {
        double base = switch (source) {
            case VAULT_CHEST -> 0.06;
            case CHEST -> 0.012;
            case COURT_MOB -> 0.010;
            case BOSS_MOB -> 0.05;
            case MOB -> 0.0015;
        };
        if (floor < 1) return 0.0;
        double floorBoost = 1.0 + Math.min(floor, 60) * 0.01;
        return Math.min(0.5, base * floorBoost * (1.0 + 0.1 * Math.max(0, Math.min(10, luck))));
    }

    /**
     * Which key a successful drop gives, for {@code roll} in 0..1: gold keys have weight 1 and silver 2.5, a key the player already has counts a quarter
     * (so a collection fills up without ever being closed to a duplicate), gold is closed on the first floors; the gate key never drops.
     */
    public static CelestialKey pickKey(double roll, int ownedMask, int floor) {
        CelestialKey[] all = CelestialKey.values();
        double[] w = new double[all.length];
        double sum = 0;
        for (CelestialKey k : all) {
            if (!k.summons()) continue;
            if (k.isGold() && floor < GOLD_FROM_FLOOR) continue;
            double weight = k.isGold() ? 1.0 : 2.5;
            if (has(ownedMask, k)) weight *= 0.25;
            w[k.ordinal()] = weight;
            sum += weight;
        }
        if (sum <= 0) return null;
        double t = Math.max(0, Math.min(0.999999999, roll)) * sum, acc = 0;
        for (CelestialKey k : all) {
            acc += w[k.ordinal()];
            if (w[k.ordinal()] > 0 && t < acc) return k;
        }
        return null;
    }

    /** True for a theme whose mobs and chests count as the court of the spirits ("celestial_court" encounter table). */
    public static boolean isCourt(String themeId) { return "celestial_court".equals(themeId); }
}
