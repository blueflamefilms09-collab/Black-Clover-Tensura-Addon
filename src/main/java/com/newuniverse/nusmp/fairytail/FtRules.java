package com.newuniverse.nusmp.fairytail;

import com.newuniverse.nusmp.skill.rules.Tier;

/**
 * Why: every decision of the Fairy Tail skills that is only arithmetic (which spells a mastery opens, what a spell costs and recovers in
 * for a tier, the damage bonus of a form and of eaten flame, whether a mastered Unique may evolve) lives here with no Minecraft class, so
 * a unit test can state the rules and the in-game code cannot drift from them.
 */
public final class FtRules {
    private FtRules() {}

    /** Most charge eaten flame or iron can store. */
    public static final int MAX_CHARGE = 100;
    /** Charge lost per second. */
    public static final double CHARGE_DECAY_PER_SECOND = 2.0;
    /** The damage bonus at full charge (0.5 = +50 percent). */
    public static final double CHARGE_BONUS = 0.5;

    /** Is the spell open at this mastery fraction (0..1)? */
    public static boolean unlocked(FtMove move, double masteryFrac) {
        if (move == null) return false;
        return Math.round(Math.max(0.0, Math.min(1.0, masteryFrac)) * 100.0) >= move.unlockPct();
    }

    /** How many spells of the line are open at this mastery. */
    public static int openCount(FtLine line, double masteryFrac) {
        int n = 0;
        for (FtMove mv : line.moves()) if (unlocked(mv, masteryFrac)) n++;
        return n;
    }

    /** The next open mode after {@code mode} (or before it when {@code reverse}); stays put when nothing else is open. */
    public static int nextOpen(FtLine line, double masteryFrac, int mode, boolean reverse) {
        int n = line.modes();
        if (n <= 0) return 0;
        for (int step = 1; step <= n; step++) {
            int m = Math.floorMod(mode + (reverse ? -step : step), n);
            if (unlocked(line.move(m), masteryFrac)) return m;
        }
        return Math.max(0, Math.min(mode, n - 1));
    }

    /** Spell power by tier: a mastered Unique hits harder, and the Ultimate harder again. */
    public static double tierMul(Tier tier) { return tier.pick(1.0, 1.3, 1.85); }

    /** The Aura (or magicule) cost of a spell: a share of the pool, never below the floor; an Ultimate pays a little more for its power. */
    public static double cost(FtMove move, double maxPool, Tier tier) {
        if (move.costFrac() <= 0) return 0;
        return Math.max(FtMove.COST_FLOOR, maxPool * move.costFrac() * tier.pick(1.0, 0.9, 1.1));
    }

    /** Cooldown in ticks: mastery shortens it by up to a quarter, and a form by another tenth; never below 10 ticks for a real cooldown. */
    public static int cooldown(FtMove move, double masteryFrac, boolean inForm) {
        if (move.cooldown() <= 0) return 0;
        double k = 1.0 - 0.25 * Math.max(0.0, Math.min(1.0, masteryFrac)) - (inForm ? 0.1 : 0.0);
        return Math.max(10, (int) Math.round(move.cooldown() * k));
    }

    /** The raw damage handed to the balance law: table x tier x form x charge. */
    public static double rawDamage(FtMove move, Tier tier, double formMul, double charge) {
        return move.damage() * tierMul(tier) * formMul * (1.0 + CHARGE_BONUS * Math.max(0.0, Math.min(MAX_CHARGE, charge)) / MAX_CHARGE);
    }

    /** Charge left after {@code elapsedTicks}. */
    public static double decayedCharge(double charge, long elapsedTicks) {
        if (charge <= 0 || elapsedTicks <= 0) return Math.max(0, charge);
        return Math.max(0.0, charge - CHARGE_DECAY_PER_SECOND * elapsedTicks / 20.0);
    }

    /** Charge gained by eating {@code units} blocks, flames or ingots, capped. */
    public static double eatCharge(double charge, int units) {
        return Math.min(MAX_CHARGE, Math.max(0, charge) + 12.0 * Math.max(0, units));
    }

    /** Share of the maximum Aura an eaten unit gives back. */
    public static double eatRestoreFrac(int units) { return Math.min(0.6, 0.06 * Math.max(0, units)); }

    /** The EP a mastered Unique needs before it may evolve (the pack-wide setting is passed in). */
    public static boolean canEvolve(boolean mastered, double ep, double needEp) {
        return mastered && (needEp <= 0 || ep >= needEp);
    }

    /** Form duration for a tier: a mastered Unique holds a form a fifth longer, an Ultimate two fifths. */
    public static int formTicks(FtMove move, Tier tier) { return (int) Math.round(move.ticks() * tier.pick(1.0, 1.2, 1.4)); }

    /** Armour render style of a form: its index; -1 when the move has none. */
    public static int armourStyle(FtMove move) { return move.form(); }

    /** Share of a hit a form with {@code physicalCut} turns aside, zero for damage the form is immune to is handled by the caller. */
    public static double afterCut(double amount, double physicalCut) { return amount * (1.0 - Math.max(0.0, Math.min(0.95, physicalCut))); }
}
