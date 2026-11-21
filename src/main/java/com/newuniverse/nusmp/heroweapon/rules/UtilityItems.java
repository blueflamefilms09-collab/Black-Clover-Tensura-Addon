package com.newuniverse.nusmp.heroweapon.rules;

import java.util.Set;

/**
 * Why (4.89.4): the owner reported that a weapon of the mod in the hand or the pack stops the items that are held and right-clicked (the Hourglass of the Final Hour that carries a
 * player to the Lucius dimension, the Chronicle Sigil, the Calling Gem). The cause found in the code: those items answer {@code UseAnim.BOW} so that the player sees a draw while the
 * channel runs, and the class rejection of the Holy Weapons read that animation as "a bow", so the hourglass was shocked and cancelled, as was every blow struck with it. And a weapon
 * in the main hand that answers the use key (a technique, a guard, a draw) took the click before the off-hand hourglass was tried. This is the whole decision with no game type, so it is
 * a unit test: which item is a hold-and-use utility (the tag {@code #anime_multiverse:hold_use_items}, or a registry name made of a utility word), when a use animation must not count
 * as a weapon, and when a weapon in the main hand hands the click on. One server setting ({@code heroweapon.utilityItemsAlwaysWork}, on by default) switches all of it; off is the old behaviour.
 */
public final class UtilityItems {
    private UtilityItems() {}

    /** The words of a registry path (split at '_') that mark an item of this mod as a utility that is used, not swung. */
    static final Set<String> WORDS = Set.of("hourglass", "sigil", "radar", "book", "grimoire", "codex", "wiki", "map", "seal", "gem", "crystal", "charm", "key", "ball", "contract",
            "compass", "scroll", "ticket", "summons", "core", "shard");

    /** True when the registry path of an item of this mod (namespace {@code anime_multiverse}) contains a utility word as a whole word. */
    public static boolean nameIsUtility(String namespace, String path) {
        if (!"anime_multiverse".equals(namespace) || path == null) return false;
        for (String part : path.split("_")) if (WORDS.contains(part)) return true;
        return false;
    }

    /** The rule of the tag: a stack in {@code #anime_multiverse:hold_use_items} is never rejected or shocked by a weapon rule, while the setting is on. */
    public static boolean exempt(boolean setting, boolean inTag) { return setting && inTag; }

    /**
     * Must the use animation of this item (bow, block, spear) be ignored when classifying it? Yes for a utility that is no weapon by any other fact: it is a utility (tag or name), it
     * declares no weapon class, is not a relic and does not hit hard. A real weapon that only has a utility word in its name keeps its class.
     */
    public static boolean ignoreUseAnimation(boolean setting, boolean utility, boolean declaresWeapon, boolean relic, double attackBonus) {
        return setting && utility && !declaresWeapon && !relic && attackBonus < WeaponClassifier.OTHER_WEAPON_ATTACK;
    }

    /**
     * Does the weapon in the main hand hand the use key on, so vanilla tries the off hand (where the hourglass is)? Only for a weapon, never while sneaking (sneak + use stays the
     * weapon's own gesture), only when the off hand holds a utility that is ready (not on cooldown), and only while the setting is on. A main hand that is itself a utility keeps its click.
     */
    public static boolean mainYields(boolean setting, boolean sneaking, boolean mainIsWeapon, boolean mainIsUtility, boolean offIsUtility, boolean offReady) {
        return setting && !sneaking && mainIsWeapon && !mainIsUtility && offIsUtility && offReady;
    }
}
