package com.newuniverse.nusmp.heroweapon.weapon.strike;

/**
 * Why: the owner's balance budget for the hero weapons (design.md section 5) is a handful of numbers per technique: the raw damage of one hit, how many hits, the cooldown,
 * the cost in magicules and how many foes it may touch. If those numbers lived only inside lambdas nobody could check them, and a later edit could quietly break the budget.
 * A skill's numbers are therefore one small immutable record in a pure table (each weapon's {@code <Kind>Numbers}), the skill object is built FROM the record, and a test
 * runs every record through {@link Budget}. Pure (no Minecraft types) so JUnit can run it.
 *
 * @param id         the global skill id, {@code <kind>.<name>}
 * @param kind       which part of the budget applies
 * @param raw        the highest raw damage of ONE hit (before the level factor); 0 for a technique that does no damage
 * @param hits       how many hits one target can take from one cast
 * @param cooldown   ticks, before the level scaling
 * @param cost       share of the maximum magicules (0.02 is two percent)
 * @param maxTargets how many foes one cast may touch
 */
public record SkillSpec(String id, Kind kind, double raw, int hits, int cooldown, double cost, int maxTargets) {
    /** DAMAGE: an ordinary technique (3 to 12 raw), HEAVY: a heavy blow (10 to 20), UTILITY: control, movement or support (little or no damage), ULTIMATE: the strongest of a form. */
    public enum Kind { DAMAGE, HEAVY, UTILITY, ULTIMATE }

    public boolean ultimate() { return kind == Kind.ULTIMATE; }

    /** The short name after the weapon's prefix: {@code hammer.quake_stomp} gives {@code quake_stomp}. */
    public String shortName() {
        int dot = id.indexOf('.');
        return dot < 0 ? id : id.substring(dot + 1);
    }
}
