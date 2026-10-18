package com.newuniverse.nusmp.heroweapon;

import java.util.EnumMap;
import java.util.Map;

/**
 * Why: the 21 hero weapons are one item class, so what makes a Whip long and light and a Hammer short and heavy is a table, and the table must be readable
 * and testable without starting the game. Each row is the weapon's melee numbers as the player sees them in the tooltip: the TOTAL attack damage, the TOTAL
 * attacks per second, and the extra reach in blocks beyond the player's own 3. {@code HeroWeaponItem} turns a row into the item's attribute modifiers
 * (the player's base is 1 damage and 4 attacks per second, so the modifiers are the totals minus those); the forms system adds its stat bonuses on top of
 * them. The numbers follow the owner's list (Shield 2, Sword 7, Spear 7 with +1.5 reach, Bow 2, Hammer 9 slow, Claw 5 fast, Axe 9, Whip 4 with +2 reach,
 * Staff 3, Gauntlet 6 fast, Projectile 3, Carriage 2, Fan 5, Scythe 8 with +1 reach, Katana 7 fast, Book 2, Mirror 2, Instrument 2, Harpoon 6 with +1 reach,
 * Sewing Set 2, Chain 6 with +2 reach); the speeds are chosen so that no weapon out-damages a netherite sword (12.8 per second at full charge) by more than
 * a fifth, because the real power of a hero weapon is its techniques, which go through the balance law, not its blows.
 */
public final class HeroBaseStats {
    private HeroBaseStats() {}

    /** The player's own attack damage and attack speed (vanilla), which the item modifiers are measured from. */
    public static final double PLAYER_BASE_DAMAGE = 1.0, PLAYER_BASE_SPEED = 4.0;

    /** One weapon's melee numbers: total damage per blow, total attacks per second, extra entity reach in blocks. */
    public record Row(double damage, double speed, double reach) {
        /** The attack-damage modifier the item carries (total minus the player's base). */
        public double damageModifier() { return damage - PLAYER_BASE_DAMAGE; }

        /** The attack-speed modifier the item carries (total minus the player's base; negative: slower than a fist). */
        public double speedModifier() { return speed - PLAYER_BASE_SPEED; }

        /** Damage per second at full charge. */
        public double dps() { return damage * speed; }
    }

    private static final Map<HeroKind, Row> TABLE = new EnumMap<>(HeroKind.class);

    private static void row(HeroKind k, double damage, double speed, double reach) { TABLE.put(k, new Row(damage, speed, reach)); }

    static {
        // the four Holy Weapons
        row(HeroKind.SHIELD, 2.0, 1.6, 0.0);          // a bash, not a blade: the Shield wins by what it takes, not by what it deals
        row(HeroKind.SWORD, 7.0, 1.8, 0.0);           // "high-speed physical damage"
        row(HeroKind.SPEAR, 7.0, 1.5, 1.5);           // "long-reach piercing attacks"
        row(HeroKind.BOW, 2.0, 1.2, 0.0);             // the shot is the weapon; the blow is a rap with the limb
        // the Seven Star Weapons
        row(HeroKind.HAMMER, 9.0, 0.9, 0.0);          // slow and heavy
        row(HeroKind.CLAW, 5.0, 2.4, 0.0);            // fast
        row(HeroKind.AXE, 9.0, 1.0, 0.0);
        row(HeroKind.WHIP, 4.0, 1.7, 2.0);            // light, long
        row(HeroKind.STAFF, 3.0, 1.4, 0.0);
        row(HeroKind.GAUNTLET, 6.0, 2.2, 0.0);        // fast
        row(HeroKind.PROJECTILE, 3.0, 1.6, 0.0);
        // the other Vassal Weapons
        row(HeroKind.CARRIAGE, 2.0, 1.2, 0.0);
        row(HeroKind.FAN, 5.0, 1.9, 0.0);
        row(HeroKind.SCYTHE, 8.0, 1.2, 1.0);
        row(HeroKind.KATANA, 7.0, 2.0, 0.0);          // fast
        row(HeroKind.BOOK, 2.0, 1.4, 0.0);
        row(HeroKind.MIRROR, 2.0, 1.4, 0.0);
        row(HeroKind.INSTRUMENT, 2.0, 1.4, 0.0);
        row(HeroKind.HARPOON, 6.0, 1.3, 1.0);
        row(HeroKind.SEWING_SET, 2.0, 1.6, 0.0);
        row(HeroKind.CHAIN, 6.0, 1.5, 2.0);
    }

    /** The row of a weapon (every weapon of the enum has one). */
    public static Row of(HeroKind kind) {
        Row r = TABLE.get(kind);
        return r != null ? r : new Row(2.0, 1.6, 0.0);
    }

    /** True when a weapon has a row of its own (the test uses it to catch a weapon appended to the enum without one). */
    public static boolean has(HeroKind kind) { return TABLE.containsKey(kind); }
}
