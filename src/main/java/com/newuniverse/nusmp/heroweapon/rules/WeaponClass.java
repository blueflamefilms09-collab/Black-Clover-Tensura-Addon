package com.newuniverse.nusmp.heroweapon.rules;

/**
 * Why: the first restriction of the Holy Weapons (the owner's text: "reject non-class weapons") needs a closed list of what "a weapon of another class" means,
 * and the classifier, the rejection events, the tests and the lang all have to agree on it. The classes are the ones of the series' rule (a hero may use
 * anything of the class of his own Legendary Weapon, and is shocked by any other): swords, axes, spears, bows, shields and blunt weapons, plus a catch-all for
 * anything else that is plainly a weapon. {@link #NONE} is everything that is not a weapon (food, blocks, tools used as tools, fists), which is never rejected.
 * Pure: no Minecraft types, so it is tested without the game.
 */
public enum WeaponClass {
    NONE, SWORD, AXE, SPEAR, BOW, SHIELD, BLUNT, OTHER_WEAPON;

    /** The lower-case name used in lang keys and logs. */
    public String id() { return name().toLowerCase(java.util.Locale.ROOT); }
}
