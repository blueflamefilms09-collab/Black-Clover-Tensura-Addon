package com.newuniverse.nusmp.heroweapon;

/**
 * Why: the three families of weapons of The Rising of the Shield Hero are not only a label. The four Holy (Cardinal) Weapons carry the three restrictions the
 * owner listed (they reject weapons of another class, they are bound to the wielder, and the heroes gain no experience near each other); the Seven Star and
 * Vassal Weapons carry none of the experience penalty. One enum holds the family and what it implies, so the rules code asks the class, never a list of
 * weapons.
 */
public enum HeroClass {
    /** The four Legendary (Cardinal) Weapons: Shield, Sword, Spear, Bow. */
    HOLY("Holy Weapon", true, true),
    /** The Seven Star Weapons of the world of the shield hero: hammer, claw, axe, whip, staff, gauntlet, projectile. */
    SEVEN_STAR("Seven Star Weapon", false, false),
    /** The other Vassal Weapons (carriage, fan, scythe, katana, book, mirror, instrument, harpoon, sewing set, chain). */
    VASSAL("Vassal Weapon", false, false);

    private final String label;
    private final boolean rejectsOthers, repelsExperience;

    HeroClass(String label, boolean rejectsOthers, boolean repelsExperience) {
        this.label = label;
        this.rejectsOthers = rejectsOthers;
        this.repelsExperience = repelsExperience;
    }

    /** The name shown in tooltips and the Weapon Book. */
    public String label() { return label; }

    /** True when a wielder of this family cannot fight with a weapon of another class (they are shocked instead). */
    public boolean rejectsOthers() { return rejectsOthers; }

    /** True when two wielders of this family standing close to each other gain no experience. */
    public boolean repelsExperience() { return repelsExperience; }
}
