package com.newuniverse.nusmp.labyrinth.mega.gear;

import java.util.Locale;

/**
 * Why: a piece of standard gear is one of nine things a hostile monster could plausibly carry, and almost every rule of the module depends on WHICH:
 * a sword and an axe gain an attack-damage bonus, the four armour pieces an armour bonus, bows, crossbows and shields get neither (their strength is
 * durability and enchantments, because an attack-damage modifier on a bow does nothing and a shield has no attribute at all); armour counts for the
 * magic resistance only when WORN in its own slot while a weapon or shield counts only when HELD (otherwise a chestplate carried in the hand would
 * resist magic); weapons and shields carry Tensura engravings, armour does not (the owner's rule is about weapons). Keeping that table in one pure
 * enum means the roller, the factory, the resistance sum and the tests all read the same facts. The ids are the ones of the JSON tables and the
 * command. Entries are only ever appended (a saved component stores the id, never the ordinal); the brief allows a trident or a mace later.
 */
public enum GearSlot {
    SWORD("sword", Kind.MELEE, -1),
    AXE("axe", Kind.MELEE, -1),
    BOW("bow", Kind.RANGED, -1),
    CROSSBOW("crossbow", Kind.RANGED, -1),
    SHIELD("shield", Kind.SHIELD, -1),
    HELMET("helmet", Kind.ARMOUR, 0),
    CHESTPLATE("chestplate", Kind.ARMOUR, 1),
    LEGGINGS("leggings", Kind.ARMOUR, 2),
    BOOTS("boots", Kind.ARMOUR, 3);

    /** The four families of piece. */
    public enum Kind { MELEE, RANGED, SHIELD, ARMOUR }

    private final String id;
    private final Kind kind;
    private final int armourIndex;

    GearSlot(String id, Kind kind, int armourIndex) {
        this.id = id;
        this.kind = kind;
        this.armourIndex = armourIndex;
    }

    public String id() { return id; }

    public Kind kind() { return kind; }

    public boolean isArmour() { return kind == Kind.ARMOUR; }

    /** Is it carried in a hand (a weapon or a shield) rather than worn? */
    public boolean isHeld() { return kind != Kind.ARMOUR; }

    /** Does it get the percentage bonus through an attribute modifier (the attack damage of a sword or an axe, the armour of an armour piece)? */
    public boolean hasPowerStat() { return kind == Kind.MELEE || kind == Kind.ARMOUR; }

    /** Does it carry Tensura engravings (every weapon and the shield, by the owner's rule 6; armour does not)? */
    public boolean engraves() { return kind != Kind.ARMOUR; }

    /** 0 helmet, 1 chestplate, 2 leggings, 3 boots, and -1 for anything held: the position of the equipment slot it belongs in. */
    public int armourIndex() { return armourIndex; }

    /** The slot with this id (case-insensitive, trimmed), or null for null or an unknown id. */
    public static GearSlot fromId(String id) {
        if (id == null) return null;
        String s = id.trim().toLowerCase(Locale.ROOT);
        for (GearSlot g : values()) if (g.id.equals(s)) return g;
        return null;
    }
}
