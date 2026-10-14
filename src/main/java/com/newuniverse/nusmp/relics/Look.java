package com.newuniverse.nusmp.relics;

/**
 * The silhouette family of a relic: the core draws the runtime worn look (armour layer textures, the small Curios models) from a look plus
 * a palette. Entries are only appended.
 */
public enum Look {
    SWORD, RAPIER, STAFF, WAND, HAMMER, AXE, BOW, SHIELD, TOME, PLATE, ROBE, LEATHER, COAT, SUIT, CLOAK, CAPE, MANTLE, HOOD, CROWN, CIRCLET, HAT,
    GLOVES, GAUNTLETS, BELT, AMULET, PENDANT, LOCKET, CHARM, POTION, ORB,
    BOOTS, SANDALS, GREAVES;

    /** A sensible look for a slot when a def names none. */
    public static Look defaultFor(RelicSlot slot) {
        return switch (slot) {
            case MAIN_HAND -> SWORD;
            case HEAD -> CIRCLET;
            case CHEST -> PLATE;
            case NECK -> AMULET;
            case BACK -> CLOAK;
            case BELT -> BELT;
            case HANDS -> GAUNTLETS;
            case CHARM -> CHARM;
            case FEET -> BOOTS;
            default -> POTION;
        };
    }
}
