package com.newuniverse.nusmp.relics;

/**
 * Where a relic is carried to work. MAIN_HAND relics work in a hand; HEAD and CHEST are real armour; NECK, BACK, BELT, HANDS and CHARM are
 * Curios slots; CONSUMABLE ones are used up. Entries are only appended. (Not the Dungeon's {@code nexus.relic.RelicSlot}.)
 */
public enum RelicSlot {
    MAIN_HAND("Hand", null),
    HEAD("Head", null),
    CHEST("Chest", null),
    NECK("Neck", "necklace"),
    BACK("Back", "back"),
    BELT("Belt", "belt"),
    HANDS("Hands", "hands"),
    CHARM("Charm", "charm"),
    CONSUMABLE("Consumable", null);

    private final String label, curios;

    RelicSlot(String label, String curios) {
        this.label = label;
        this.curios = curios;
    }

    public String label() { return label; }

    /** The Curios slot type id (data/curios/tags/item/&lt;id&gt;.json), or null if this slot is not a Curios slot. */
    public String curiosSlot() { return curios; }

    public boolean isCurio() { return curios != null; }

    public boolean isArmor() { return this == HEAD || this == CHEST; }

    /** Held in a hand to work (weapons, staffs, shields). */
    public boolean isHand() { return this == MAIN_HAND; }

    public static RelicSlot byName(String name) {
        if (name == null) return null;
        for (RelicSlot s : values()) if (s.name().equalsIgnoreCase(name.trim())) return s;
        return null;
    }
}
