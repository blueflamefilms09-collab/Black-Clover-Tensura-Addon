package com.newuniverse.nusmp.avatar.dnd;

import java.util.Map;

/**
 * Why: the rolled scores have to turn into the numbers the Avatar fights with (health, damage, speed, armour, EP, magicule, aura). Keeping the mapping in
 * one pure class means the creation service, the evolution step of naming and the tests all agree, and a balance change is one edit. A score of 10 is
 * neutral; each point moves a stat by a small fixed step, scaled by the race's multiplier for that score.
 */
public final class AvatarStats {
    private AvatarStats() {}

    /** The derived numbers. EP, magicule and aura are Tensura's own pools. */
    public record Derived(double maxHealth, double attackDamage, double moveSpeed, double armor, double ep, double magicule, double aura) {}

    private static double mult(Map<Stat, Double> m, Stat s) {
        Double v = m == null ? null : m.get(s);
        return v == null || v <= 0 ? 1.0 : v;
    }

    /** The race multiplier applied to a score (never below 1, a score is never reduced to nothing). */
    public static double effective(int score, Map<Stat, Double> multipliers, Stat s) { return Math.max(1.0, score * mult(multipliers, s)); }

    /**
     * @param stats        the six scores in {@link Stat} order
     * @param multipliers  the race's per-score multipliers (may be empty)
     * @param baseEp       the race's base EP
     * @param epBonusMult  1.0 normally; evolution or the naming surge pass a larger value
     */
    public static Derived derive(int[] stats, Map<Stat, Double> multipliers, double baseEp, double epBonusMult) {
        double str = effective(stats[Stat.STR.ordinal()], multipliers, Stat.STR), dex = effective(stats[Stat.DEX.ordinal()], multipliers, Stat.DEX),
                con = effective(stats[Stat.CON.ordinal()], multipliers, Stat.CON), in = effective(stats[Stat.INT.ordinal()], multipliers, Stat.INT),
                wis = effective(stats[Stat.WIS.ordinal()], multipliers, Stat.WIS), cha = effective(stats[Stat.CHA.ordinal()], multipliers, Stat.CHA);
        double health = Math.max(10.0, 20.0 + (con - 10.0) * 2.0);
        double attack = Math.max(1.0, 4.0 + (str - 10.0) * 0.6);
        double speed = Math.max(0.18, 0.26 + (dex - 10.0) * 0.004);
        double armor = Math.max(0.0, (con - 10.0) * 0.5 + (dex - 10.0) * 0.25);
        double ep = Math.max(1.0, baseEp * (0.6 + total(str, dex, con, in, wis, cha) / 72.0) * epBonusMult);
        double magicule = Math.max(10.0, ep * (0.5 + (in + wis - 20.0) * 0.02));
        double aura = Math.max(10.0, ep * (0.5 + (str + con - 20.0) * 0.02 + (cha - 10.0) * 0.005));
        return new Derived(health, attack, speed, armor, ep, magicule, aura);
    }

    private static double total(double... v) {
        double s = 0;
        for (double d : v) s += d;
        return s;
    }
}
