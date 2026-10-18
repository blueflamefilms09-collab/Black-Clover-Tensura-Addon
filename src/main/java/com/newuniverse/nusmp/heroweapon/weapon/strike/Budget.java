package com.newuniverse.nusmp.heroweapon.weapon.strike;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: design.md section 5 and the weapon brief fix what a technique may do before the 1.0 to 2.2 level factor: raw damage per hit 3 to 12 for an ordinary technique, 10 to 20
 * for a heavy one, at most 32 for an ultimate; at most 12 targets; a cooldown of at least 60 ticks for a damage technique, 100 for a utility, 600 for an ultimate; a cost of 1 to
 * 3 percent of the maximum magicules, 5 to 8 percent for an ultimate. This class is that rule as code, so each weapon's numbers table can be checked by a plain unit test (and a
 * negative control proves the check can fail). {@code multiplier} is whatever a weapon multiplies a hit by on top of the table (the Claw's mastery, the Axe's Brawn), so the
 * budget holds at the weapon's best, not only on paper. Pure.
 */
public final class Budget {
    private Budget() {}

    public static final int MAX_TARGETS = 12;
    public static final double COST_MIN = 0.01, COST_MAX = 0.03, ULTIMATE_COST_MIN = 0.05, ULTIMATE_COST_MAX = 0.08;

    /** The least cooldown (ticks, before the level scaling) a technique of this kind may have. */
    public static int cooldownFloor(SkillSpec.Kind kind) {
        return switch (kind) {
            case DAMAGE, HEAVY -> 60;
            case UTILITY -> 100;
            case ULTIMATE -> 600;
        };
    }

    /** The most raw damage one hit of this kind may ever do (before the level factor): a weapon that multiplies a hit clamps it here, so the budget holds whatever it stacks. */
    public static double cap(SkillSpec.Kind kind) {
        return switch (kind) {
            case DAMAGE, UTILITY -> 12.0;
            case HEAVY -> 20.0;
            case ULTIMATE -> 32.0;
        };
    }

    /** Every way the spec breaks the budget when each hit is multiplied by {@code multiplier}; empty when it holds. */
    public static List<String> violations(SkillSpec s, double multiplier) {
        List<String> out = new ArrayList<>();
        double best = s.raw() * multiplier;
        switch (s.kind()) {
            case DAMAGE -> {
                if (s.raw() < 3.0 || best > 12.0) out.add(s.id() + ": an ordinary technique hits for 3 to 12 raw, this one for " + s.raw() + " (" + best + " at its best)");
            }
            case HEAVY -> {
                if (s.raw() < 10.0 || best > 20.0) out.add(s.id() + ": a heavy technique hits for 10 to 20 raw, this one for " + s.raw() + " (" + best + " at its best)");
            }
            case UTILITY -> {
                if (s.raw() != 0.0 && (s.raw() < 3.0 || best > 12.0)) out.add(s.id() + ": a utility does no damage or 3 to 12 raw, this one " + s.raw() + " (" + best + " at its best)");
            }
            case ULTIMATE -> {
                if (s.raw() < 3.0 || best > 32.0) out.add(s.id() + ": an ultimate hits for 3 to 32 raw a hit, this one for " + s.raw() + " (" + best + " at its best)");
            }
        }
        if (s.raw() > 0.0 && s.hits() < 1) out.add(s.id() + ": a damaging technique needs at least one hit");
        if (s.maxTargets() < 1 || s.maxTargets() > MAX_TARGETS) out.add(s.id() + ": it may touch 1 to " + MAX_TARGETS + " foes, this one " + s.maxTargets());
        if (s.cooldown() < cooldownFloor(s.kind())) out.add(s.id() + ": the cooldown of a " + s.kind() + " is at least " + cooldownFloor(s.kind()) + " ticks, this one " + s.cooldown());
        double lo = s.ultimate() ? ULTIMATE_COST_MIN : COST_MIN, hi = s.ultimate() ? ULTIMATE_COST_MAX : COST_MAX;
        if (s.cost() < lo - 1.0E-9 || s.cost() > hi + 1.0E-9) out.add(s.id() + ": the cost of a " + s.kind() + " is " + lo + " to " + hi + " of the magicules, this one " + s.cost());
        return out;
    }

    /** Every violation of a whole table, plus a repeated id; empty when it holds. */
    public static List<String> violations(List<SkillSpec> table, double multiplier) {
        List<String> out = new ArrayList<>();
        List<String> seen = new ArrayList<>();
        for (SkillSpec s : table) {
            if (seen.contains(s.id())) out.add(s.id() + ": the id is used twice");
            seen.add(s.id());
            out.addAll(violations(s, multiplier));
        }
        return out;
    }
}
