package com.newuniverse.nusmp.avatar.inventory;

import com.newuniverse.nusmp.avatar.ai.WeaponKind;

/**
 * Why: a looting Avatar "equips armor and weapons if better", and "better" must be one clear rule instead of a guess scattered over goals. This pure class
 * scores armour and weapons from plain numbers (defence, toughness, damage, enchantment points, how worn the item is) that {@link AvatarGear} reads off the item
 * stacks, and decides whether a found piece replaces the worn one. A piece must beat the current one by a margin (so two near-equal pieces do not trade places
 * forever) and must not be nearly broken. The kind of a main-hand weapon is never switched by loot alone: a swordsman keeps its sword when it finds a bow.
 */
public final class GearScore {
    private GearScore() {}

    /** A candidate must be this much better than what is worn (5%). */
    public static final double MARGIN = 1.05;
    /** An item with less than this share of its durability left is never equipped. */
    public static final double MIN_DURABILITY = 0.08;

    /** The numbers of one armour piece. {@code maxDurability} 0 means it cannot wear out. */
    public record Armor(int defense, double toughness, double knockbackResistance, int enchantPoints, int durabilityLeft, int maxDurability) {
        public double wear() { return maxDurability <= 0 ? 1.0 : Math.max(0.0, Math.min(1.0, durabilityLeft / (double) maxDurability)); }
    }

    /** The numbers of one weapon: the damage it adds, whether it is a melee weapon, enchantment points and wear. */
    public record Weapon(double damage, int enchantPoints, int durabilityLeft, int maxDurability) {
        public double wear() { return maxDurability <= 0 ? 1.0 : Math.max(0.0, Math.min(1.0, durabilityLeft / (double) maxDurability)); }
    }

    /** One number for an armour piece; 0 for nothing. A worn piece counts for less. */
    public static double armor(Armor a) {
        if (a == null) return 0.0;
        double raw = a.defense() + a.toughness() * 0.5 + a.knockbackResistance() * 3.0 + a.enchantPoints() * 0.25;
        return raw * (0.5 + 0.5 * a.wear());
    }

    /** True when {@code candidate} should replace {@code current} (null or empty current means "nothing worn"). */
    public static boolean betterArmor(Armor candidate, Armor current) {
        if (candidate == null || candidate.wear() < MIN_DURABILITY) return false;
        double c = armor(candidate);
        if (c <= 0.0) return false;
        double cur = armor(current);
        return cur <= 0.0 || c > cur * MARGIN;
    }

    /** One number for a weapon; 0 for nothing. */
    public static double weapon(Weapon w) {
        if (w == null) return 0.0;
        return (w.damage() + w.enchantPoints() * 0.5) * (0.6 + 0.4 * w.wear());
    }

    /** True when {@code candidate} should replace {@code current} among weapons OF THE SAME KIND. */
    public static boolean betterWeapon(Weapon candidate, Weapon current) {
        if (candidate == null || candidate.wear() < MIN_DURABILITY) return false;
        double c = weapon(candidate);
        if (c <= 0.0) return false;
        double cur = weapon(current);
        return cur <= 0.0 || c > cur * MARGIN;
    }

    /**
     * Should a found weapon of {@code newKind} go to the main hand? An empty hand (or a non-weapon) takes any weapon that is not a bow without ammunition;
     * a hand that already holds a weapon of the same kind swaps only for a better one; a different kind never replaces the held weapon on its own.
     */
    public static boolean equipMainHand(WeaponKind held, double heldScore, WeaponKind found, double foundScore, boolean hasAmmo) {
        if (found == WeaponKind.NONE || found == WeaponKind.SHIELD) return false;
        if ((found == WeaponKind.BOW || found == WeaponKind.CROSSBOW) && !hasAmmo) return false;
        if (held == WeaponKind.NONE || held == WeaponKind.SHIELD) return foundScore > 0.0 || found == WeaponKind.FOCUS;
        if (held != found) return false;
        return foundScore > heldScore * MARGIN;
    }
}
