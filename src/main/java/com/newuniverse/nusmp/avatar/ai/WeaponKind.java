package com.newuniverse.nusmp.avatar.ai;

/**
 * Why: the dynamic weapon AI must choose how an Avatar fights from what it holds, and that decision has to be testable without a game. This enum is the
 * pure vocabulary: {@link AvatarCombat} turns an item stack into one of these, {@link CombatPlanner} turns two of them (main hand, off hand) into a
 * {@link CombatStyle}. Constants are only ever appended.
 */
public enum WeaponKind {
    /** An empty hand (or an item that is no weapon, tool or focus). */
    NONE,
    /** A sword, axe, mace, spear, tool or any other item that hits harder than a fist. */
    MELEE,
    /** A bow (shoots arrows from a distance, draws for a second). */
    BOW,
    /** A crossbow (loads, then shoots at once). */
    CROSSBOW,
    /** A trident (used in melee; thrown tridents are not used). */
    TRIDENT,
    /** A shield: raised against nearby melee enemies. */
    SHIELD,
    /** A grimoire, a charter grimoire, a staff or a wand: the Avatar fights as a mage. */
    FOCUS
}
