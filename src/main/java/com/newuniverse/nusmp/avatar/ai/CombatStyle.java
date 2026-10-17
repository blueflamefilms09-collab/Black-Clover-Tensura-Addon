package com.newuniverse.nusmp.avatar.ai;

/**
 * Why: an Avatar fights in one of four ways and its goals only run when their way is the current one, so two attack goals can never fight over the legs
 * of the same Avatar. {@link CombatPlanner#choose} decides from the held items; the goals ({@code AvatarMeleeGoal}, {@code AvatarRangedGoal},
 * {@code AvatarMageGoal}) test it on every check. Constants are only ever appended.
 */
public enum CombatStyle {
    /** Close in and hit (sword, axe, trident, fists); a held shield is raised by the shield goal. */
    MELEE,
    /** Keep a distance and shoot arrows with a bow. */
    BOW,
    /** Keep a distance, load and shoot with a crossbow. */
    CROSSBOW,
    /** Keep a distance and cast: the cast goal does the casting, the mage goal does the standing and strafing. */
    MAGE
}
