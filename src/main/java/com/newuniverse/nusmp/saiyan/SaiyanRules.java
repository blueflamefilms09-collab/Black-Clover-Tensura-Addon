package com.newuniverse.nusmp.saiyan;

import java.util.Locale;

/**
 * Why: every number of the Saiyan system that is arithmetic (3.63.0, Stage 1) lives here with no Minecraft type, so the progression, the racial bonuses and the anti-farm rules are unit tests and a
 * value read from a hand-edited config can never leave its bounds (each function clamps). Battle Power is a progression LABEL: it never multiplies damage directly. It only feeds {@link #growth}, a
 * 0..1 share of how far a Saiyan has come (a log scale from 100 to 10,000,000, the span of the six ranks), and each racial bonus is {@code base * (1 + gain * growth)} cut at its own cap. That is the
 * one configurable conversion between Battle Power and real combat strength.
 */
public final class SaiyanRules {
    private SaiyanRules() {}

    public static final double START_BP = 100.0;
    /** Battle Power can not pass this, whatever a command or a save says. */
    public static final double BP_HARD_CAP = 1.0e12;
    /** Decades between the first rank (100) and the last (10,000,000). */
    public static final double LOG_SPAN = 5.0;
    public static final int DAY_TICKS = 24000;

    /** The six ranks of the Battle Power table. */
    public enum Rank {
        LOW_CLASS(100), ELITE(1_000), SUPER_ELITE(10_000), LEGENDARY(100_000), DIVINE(1_000_000), TRANSCENDENT(10_000_000);

        public final double from;

        Rank(double from) { this.from = from; }

        /** The lang key of the rank's name. */
        public String key() { return "nusmp.saiyan.rank." + name().toLowerCase(Locale.ROOT); }

        public static Rank byOrdinal(int i) { return values()[(int) clamp(i, 0, values().length - 1)]; }
    }

    public static Rank rankOf(double bp) {
        Rank r = Rank.LOW_CLASS;
        for (Rank x : Rank.values()) if (bp >= x.from) r = x;
        return r;
    }

    public static double clamp(double v, double lo, double hi) { return Double.isNaN(v) ? lo : Math.max(lo, Math.min(hi, v)); }

    public static double clampBp(double bp) { return clamp(bp, START_BP, BP_HARD_CAP); }

    // ------------------------------------------------------------------ Battle Power -> strength
    /** 0..1: how far this Battle Power has come on the log scale 100 .. 10,000,000. */
    public static double growth(double bp) {
        if (!(bp > START_BP)) return 0.0;
        return clamp(Math.log10(bp / START_BP) / LOG_SPAN, 0.0, 1.0);
    }

    /** A racial bonus at this Battle Power: {@code base * (1 + gain * growth)}, never below 0 and never over {@code cap}. */
    public static double bonus(double base, double gain, double cap, double bp) {
        double b = Math.max(0.0, Double.isNaN(base) ? 0.0 : base);
        return Math.min(Math.max(0.0, cap), b * (1.0 + Math.max(0.0, gain) * growth(bp)));
    }

    /** The racial bonuses as fractions (0.35 = +35%), in the order of the spec's table. */
    public record Table(double strength, double speed, double durability, double attackSpeed, double jump, double stamina, double kiCapacity, double knockback, double combatXp) {
        /** Every bonus of {@code base} grown to this Battle Power and cut at the matching entry of {@code caps}. */
        public Table at(double gain, double bp, Table caps) {
            return new Table(bonus(strength, gain, caps.strength, bp), bonus(speed, gain, caps.speed, bp), bonus(durability, gain, caps.durability, bp),
                    bonus(attackSpeed, gain, caps.attackSpeed, bp), bonus(jump, gain, caps.jump, bp), bonus(stamina, gain, caps.stamina, bp),
                    bonus(kiCapacity, gain, caps.kiCapacity, bp), bonus(knockback, gain, caps.knockback, bp), bonus(combatXp, gain, caps.combatXp, bp));
        }
    }

    /** Outgoing melee damage factor from the strength bonus. */
    public static double damageFactor(double strengthBonus) { return 1.0 + Math.max(0.0, strengthBonus); }

    /** Incoming damage factor from the durability bonus: +30% durability takes 1/1.3 of a blow. */
    public static double takenFactor(double durabilityBonus) { return 1.0 / (1.0 + Math.max(0.0, durabilityBonus)); }

    // ------------------------------------------------------------------ Battle Power gain from a kill (the anti-farm rules)
    /** How hard a creature is, in the same units as Battle Power: its health, attack and armour, plus a share of its Tensura Existence Points. */
    public static double victimPower(double maxHealth, double attack, double armor, double ep, double scale, double epWeight) {
        return Math.max(0.0, scale) * (Math.max(0.0, maxHealth) + 4.0 * Math.max(0.0, attack) + 2.0 * Math.max(0.0, armor)) + Math.max(0.0, epWeight) * Math.max(0.0, ep);
    }

    /**
     * How much of a full gain a victim is worth: nothing under {@code minRatio} of the Saiyan's own Battle Power (weak creatures teach nothing), a smooth rise to a full gain at an equal fight, and a
     * little more (at most 1.5) against a stronger one. This is the main answer to "no farming weak mobs".
     */
    public static double difficultyWeight(double victimPower, double bp, double minRatio) {
        if (!(bp > 0.0) || !(victimPower > 0.0)) return 0.0;
        double lo = clamp(minRatio, 0.0, 0.95), r = victimPower / bp;
        if (r < lo) return 0.0;
        if (r < 1.0) {
            double t = (r - lo) / (1.0 - lo);
            return t * t * (3.0 - 2.0 * t);
        }
        return Math.min(1.5, 1.0 + (r - 1.0) * 0.25);
    }

    /** The same kind of creature killed again and again inside the window is worth less each time: 1 / (1 + kills / limit). */
    public static double repeatFactor(int recentSameKills, int limit) {
        return limit <= 0 ? 1.0 : 1.0 / (1.0 + Math.max(0, recentSameKills) / (double) limit);
    }

    /** What may still be gained today: the larger of a floor and a share of the current Battle Power, minus what was gained already. */
    public static double dailyRoom(double bp, double fraction, double floor, double gainedToday) {
        double cap = Math.max(Math.max(0.0, floor), bp * Math.max(0.0, fraction));
        return Math.max(0.0, cap - Math.max(0.0, gainedToday));
    }

    /** The Battle Power one kill gives: a share of the current value, scaled by the victim, the repeat factor and the race's experience bonus, cut at what is left of today's room. */
    public static double killGain(double bp, double fractionPerKill, double weight, double repeat, double xpBonus, double room) {
        double raw = bp * Math.max(0.0, fractionPerKill) * Math.max(0.0, weight) * Math.max(0.0, repeat) * (1.0 + Math.max(0.0, xpBonus));
        return Math.max(0.0, Math.min(raw, Math.max(0.0, room)));
    }

    public static long dayOf(long gameTime) { return Math.floorDiv(Math.max(0L, gameTime), (long) DAY_TICKS); }

    // ------------------------------------------------------------------ the Ki attacks
    /** The raw damage of a Ki attack before the balance law: base times (1 + gain * growth). */
    public static double kiDamage(double base, double gain, double bp) { return Math.max(0.0, base) * (1.0 + Math.max(0.0, gain) * growth(bp)); }

    /** Stamina after regeneration / spending, kept inside 0..max. */
    public static double stamina(double now, double delta, double max) { return clamp(now + delta, 0.0, Math.max(0.0, max)); }

    /** Seconds of sneaking still that awaken the Ki. */
    public static boolean awakened(int stillSeconds, int needSeconds) { return stillSeconds >= Math.max(1, needSeconds); }
}
