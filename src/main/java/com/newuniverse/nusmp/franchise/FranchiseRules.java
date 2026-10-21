package com.newuniverse.nusmp.franchise;

/**
 * The numbers and decisions of the franchise engravings, with no Minecraft types, so they are unit-tested ({@code FranchiseRulesTest}). {@link FranchiseEvents} gathers the
 * facts of a hit and applies what this class says.
 * <p>
 * Balance: the damage of a weapon is already set by the grade tables ({@code relics.GradeDamage}), so an engraving only ever adds a SHARE of the hit that was going to land
 * (never a flat number, never a share of the target's maximum health). All attack bonuses of one hit together are capped at {@link #ATTACK_CAP} (15%) and are halved when the
 * target is a player; all damage reductions together at {@link #GUARD_CAP} (20%); a heal is at most {@link #HEAL_CAP} per proc; mana returns are capped per cast, per pulse and
 * per minute. A stronger item carries a higher engraving level: +25% per level on shares, chances and heals (never on a cooldown and never past a cap).
 * No rule here touches the food bar.
 */
public final class FranchiseRules {
    private FranchiseRules() {}

    /** All franchise attack bonuses on one hit together, as a share of the hit. */
    public static final double ATTACK_CAP = 0.15;
    /** Against another player the bonus counts for half. */
    public static final double PVP_FACTOR = 0.5;
    /** All franchise damage reductions on one hit together. */
    public static final double GUARD_CAP = 0.20;
    /** The most a single proc may heal. */
    public static final float HEAL_CAP = 2.0f;
    /** The most magicule the engravings return per minute, as a share of the maximum. */
    public static final double REFUND_PER_MINUTE_CAP = 0.05;
    /** The most magicule one trickle pulse (every 5 seconds) returns, as a share of the maximum. */
    public static final double TRICKLE_CAP = 0.006;
    /** A chance never goes above this, whatever the level. */
    public static final double CHANCE_CAP = 0.25;
    /** The highest regeneration time an ally-aura may refresh, in ticks. */
    public static final int REGEN_DURATION_CAP = 100;
    /** A foe with at least this much maximum health counts as a big one. */
    public static final double BIG_HEALTH = 60.0;
    public static final int MAX_LEVEL = 3;

    // ---------------------------------------------------------------- level
    /** 1.0, 1.25, 1.5 for the levels 1 to 3 (anything else is clamped). */
    public static double levelMult(int level) { return 1.0 + 0.25 * (Math.max(1, Math.min(MAX_LEVEL, level)) - 1); }

    /** A relic's engraving level by its grade ordinal 0..6 (normal..genesis): 1 up to rare, 2 for unique and legend, 3 from god. */
    public static int levelForGrade(int gradeOrdinal) { return gradeOrdinal >= 5 ? 3 : gradeOrdinal >= 3 ? 2 : 1; }

    public static double scaled(double base, int level) { return base * levelMult(level); }

    public static double chance(double base, int level) { return Math.max(0.0, Math.min(CHANCE_CAP, base * levelMult(level))); }

    // ---------------------------------------------------------------- attack
    /** The sum of bonuses after the caps: negative counts as zero, a player target halves it, then {@code cap} (itself clamped to 0..0.30) is the ceiling. */
    public static double cappedAttack(double sum, boolean targetIsPlayer, double cap) {
        double s = Math.max(0.0, sum);
        if (targetIsPlayer) s *= PVP_FACTOR;
        return Math.min(s, Math.max(0.0, Math.min(0.30, cap)));
    }

    public static float applyAttack(float amount, double sum, boolean targetIsPlayer, double cap) {
        if (amount <= 0f) return amount;
        return (float) (amount * (1.0 + cappedAttack(sum, targetIsPlayer, cap)));
    }

    /** What the hit's facts say about a bonus condition (see {@link #condMet}). */
    public record Facts(boolean day, double targetFrac, double selfFrac, boolean targetUndead, double targetArmour, double targetMaxHealth,
                        boolean sprinting, boolean falling, boolean sneaking, boolean wet) {}

    /** Whether the condition of a hit_bonus holds. An unknown condition never holds. */
    public static boolean condMet(String cond, Facts f) {
        if (cond == null) return false;
        return switch (cond) {
            case "always" -> true;
            case "target_big" -> f.targetMaxHealth() >= BIG_HEALTH;
            case "target_wounded" -> f.targetFrac() <= 0.5;
            case "target_armoured" -> f.targetArmour() >= 10.0;
            case "self_wounded" -> f.selfFrac() <= 0.5;
            case "day" -> f.day();
            case "night" -> !f.day();
            case "undead" -> f.targetUndead();
            case "sprinting" -> f.sprinting();
            case "airborne" -> f.falling();
            case "sneaking" -> f.sneaking();
            case "wet" -> f.wet();
            default -> false;
        };
    }

    // ---------------------------------------------------------------- defence
    public static double cappedGuard(double sum) { return Math.max(0.0, Math.min(GUARD_CAP, sum)); }

    public static float applyGuard(float amount, double sum) {
        if (amount <= 0f) return amount;
        return (float) (amount * (1.0 - cappedGuard(sum)));
    }

    /** Whether a ward of this tag covers a hit with these properties. */
    public static boolean wardApplies(String tag, boolean magic, boolean projectile, boolean fall, boolean melee, boolean explosion, boolean fire) {
        if (tag == null) return false;
        return switch (tag) {
            case "magic" -> magic;
            case "projectile" -> projectile;
            case "fall" -> fall;
            case "melee" -> melee;
            case "explosion" -> explosion;
            case "fire" -> fire;
            default -> false;
        };
    }

    // ---------------------------------------------------------------- timers
    public static boolean ready(long now, long readyAt) { return now >= readyAt; }

    public static boolean roll(double roll01, double chance) { return roll01 < chance; }

    /** Which of {@code n} equal outcomes a roll between 0 and 1 picks (0 .. n-1). */
    public static int pick(double roll01, int n) { return n <= 1 ? 0 : Math.min(n - 1, (int) Math.floor(Math.max(0.0, roll01) * n)); }

    // ---------------------------------------------------------------- stacks, charges, combos
    public static double stackBonus(int stacks, int max, double per) { return Math.max(0, Math.min(stacks, max)) * per; }

    /** The kill stacks that are left: all of them until {@code decay} ticks after the last kill, then none. */
    public static int stacksLeft(int stacks, long lastKill, long now, int decay) { return now - lastKill > decay ? 0 : stacks; }

    public static int addStack(int stacks, int max) { return Math.min(Math.max(1, max), stacks + 1); }

    /** The combo count a new hit makes: it starts over at 1 when the last hit is older than the window. */
    public static int comboNext(int combo, long lastHit, long now, int window, int max) { return now - lastHit > window ? 1 : Math.min(Math.max(1, max), combo + 1); }

    /** Whether a hit fires the charge (it is the {@code hits}th since the last release). */
    public static boolean chargeFires(int charge, int hits) { return charge + 1 >= Math.max(1, hits); }

    public static int chargeNext(int charge, int hits) { return chargeFires(charge, hits) ? 0 : charge + 1; }

    /** A charge that has not been fed for this long is dropped. */
    public static final int CHARGE_IDLE_TICKS = 200;

    public static boolean chargeStale(long lastHit, long now) { return now - lastHit > CHARGE_IDLE_TICKS; }

    // ---------------------------------------------------------------- price, wind, steal
    /** A blood price is paid only while the bearer keeps at least {@code minHp} after paying. */
    public static boolean priceOk(float health, double cost, double minHp) { return cost > 0 && health - cost >= minHp; }

    /** A second wind fires when a hit left the bearer alive but at or under the threshold share of health. */
    public static boolean windFires(float healthAfter, float maxHealth, double threshold) { return healthAfter > 0f && maxHealth > 0f && healthAfter <= maxHealth * threshold; }

    public static float healCap(double heal, int level) { return (float) Math.min(HEAL_CAP, Math.max(0.0, heal * levelMult(level))); }

    public static float steal(double dealt, double pct, double cap, int level) {
        return (float) Math.max(0.0, Math.min(Math.min(HEAL_CAP, cap), dealt * pct * levelMult(level)));
    }

    // ---------------------------------------------------------------- spells and mana
    public static boolean spellWindow(long now, long lastCast, int window) { return lastCast >= 0 && now - lastCast <= window; }

    /** The magicule a cast returns: the share of the maximum, but never past the per-minute budget. */
    public static double refund(double maxMagicule, double pct, int level, double refundedThisMinute) {
        double want = Math.max(0.0, maxMagicule) * Math.max(0.0, pct) * levelMult(level);
        double room = Math.max(0.0, maxMagicule * REFUND_PER_MINUTE_CAP - refundedThisMinute);
        return Math.min(want, room);
    }

    public static double trickle(double maxMagicule, double pct, int level) {
        return Math.max(0.0, maxMagicule) * Math.min(TRICKLE_CAP, Math.max(0.0, pct) * levelMult(level));
    }

    /** Effects an engraving may never apply: nothing that touches the food bar (the owner's strict hunger ban). */
    public static boolean effectAllowed(String id) {
        if (id == null) return false;
        String p = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        return !p.equals("hunger") && !p.equals("saturation") && !p.contains("food");
    }

    public static int regenDuration(int dur) { return Math.max(20, Math.min(REGEN_DURATION_CAP, dur)); }
}
