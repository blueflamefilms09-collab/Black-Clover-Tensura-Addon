package com.newuniverse.nusmp.heroweapon.rules;

import com.newuniverse.nusmp.heroweapon.HeroKind;

/**
 * Why: "which class of weapon is this item" is decided from a handful of facts about the item (its Java class, the tags it carries, how it is used, how hard it
 * hits), and "is a Holy wielder shocked by it" from that class and the wielder's own. Keeping both decisions in a class with no Minecraft types means the whole
 * table of the rejection rule is a unit test: the game-side code ({@code HeroRules}) only reads the facts off an {@code ItemStack} and asks here. The order of
 * the rules matters and is the documented one: the explicit allow tag first, then a class the item declares itself (the hero weapons, this mod's own weapon
 * kinds), then how it is used (a shield is raised, a bow is drawn, a trident is thrown), then its Java class and tags (sword, axe, mace), then "a tool is not a
 * weapon", and last the catch-all: an item that adds 4 or more attack damage is a weapon of no particular class.
 */
public final class WeaponClassifier {
    private WeaponClassifier() {}

    /** An attack-damage modifier of at least this makes an otherwise unclassified item a weapon ({@link WeaponClass#OTHER_WEAPON}). */
    public static final double OTHER_WEAPON_ATTACK = 4.0;

    /** Bit flags of {@link Facts#flags}. */
    public static final int ALLOWED = 1, SWORD = 2, AXE = 4, SPEAR = 8, BOW = 16, SHIELD = 32, BLUNT = 64, TOOL = 128;

    /**
     * What the game side could read from a stack: a class the item declares ({@code declared}, null for none), flags for the tag and class tests
     * ({@link #ALLOWED}: the stack is in {@code #nusmp:hero_allowed}; {@link #SWORD} ... {@link #BLUNT}: it is that kind of item by class, tag or use animation;
     * {@link #TOOL}: a pickaxe, shovel or hoe), and {@code attackBonus}, the sum of its attack-damage modifiers in the main hand.
     */
    public record Facts(WeaponClass declared, int flags, double attackBonus) {
        public boolean has(int flag) { return (flags & flag) != 0; }
    }

    /** The class of an item with these facts. */
    public static WeaponClass classify(Facts f) {
        if (f == null || f.has(ALLOWED)) return WeaponClass.NONE;
        if (f.declared() != null) return f.declared();
        if (f.has(SHIELD)) return WeaponClass.SHIELD;
        if (f.has(BOW)) return WeaponClass.BOW;
        if (f.has(SPEAR)) return WeaponClass.SPEAR;
        if (f.has(SWORD)) return WeaponClass.SWORD;
        if (f.has(AXE)) return WeaponClass.AXE;
        if (f.has(BLUNT)) return WeaponClass.BLUNT;
        if (f.has(TOOL)) return WeaponClass.NONE;
        return f.attackBonus() >= OTHER_WEAPON_ATTACK ? WeaponClass.OTHER_WEAPON : WeaponClass.NONE;
    }

    /** The class of weapon a hero weapon belongs to for this rule: a Holy Weapon its own, every Seven Star and Vassal Weapon none (they are always allowed). */
    public static WeaponClass ownClass(HeroKind kind) {
        if (kind == null) return WeaponClass.NONE;
        return switch (kind) {
            case SHIELD -> WeaponClass.SHIELD;
            case SWORD -> WeaponClass.SWORD;
            case SPEAR -> WeaponClass.SPEAR;
            case BOW -> WeaponClass.BOW;
            default -> WeaponClass.NONE;
        };
    }

    /**
     * True when a wielder of {@code wielder} is shocked for fighting with an item of class {@code held}: the wielder's family rejects other classes (the Holy
     * Weapons do; the Seven Star and Vassal Weapons do not), the item is a weapon at all, and it is not of the wielder's own class.
     */
    public static boolean rejects(HeroKind wielder, WeaponClass held) {
        if (wielder == null || !wielder.heroClass().rejectsOthers()) return false;
        if (held == null || held == WeaponClass.NONE) return false;
        return held != ownClass(wielder);
    }

    /** The classes that are "fired" when used (a bow is drawn, a trident or spear is thrown): using one is rejected, not only hitting with one. */
    public static boolean firesWhenUsed(WeaponClass c) { return c == WeaponClass.BOW || c == WeaponClass.SPEAR; }
}
