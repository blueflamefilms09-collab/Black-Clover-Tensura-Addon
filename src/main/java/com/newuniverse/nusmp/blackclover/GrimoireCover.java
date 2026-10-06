package com.newuniverse.nusmp.blackclover;

/**
 * Every cover state. The ORDER here must match the item model overrides (variant index).
 * tier: 3 = common, 4 = rare, 5 = forbidden (devil). damage/cost multiply every spell.
 */
public enum GrimoireCover {
    THREE_LEAF(Kingdom.CLOVER, 3, 1.00, 1.00, "Three leaves: Faith, Hope and Love."),
    FOUR_LEAF(Kingdom.CLOVER, 4, 1.05, 0.95, "The fourth leaf is Good Luck."),
    FIVE_LEAF(Kingdom.CLOVER, 5, 1.10, 0.95, "Within the fifth leaf resides the Devil."),
    SPADE(Kingdom.SPADE, 3, 1.08, 1.00, "The Spade suit: built for war."),
    DOUBLE_SPADE(Kingdom.SPADE, 4, 1.08, 1.00, "This grimoire refuses to explain itself."),
    TRIPLE_SPADE(Kingdom.SPADE, 5, 1.12, 1.00, "Corrupted. Something answers from inside the third spade."),
    HEART(Kingdom.HEART, 3, 1.00, 0.95, "The Heart suit: Mana Method flows easily through you."),
    TWO_HEART(Kingdom.HEART, 4, 1.00, 0.92, "Two joined hearts. You are not alone."),
    CRACKED_HEART(Kingdom.HEART, 3, 1.10, 1.05, "The cover split when they were lost. Grief writes louder than love."),
    DIAMOND(Kingdom.DIAMOND, 3, 1.00, 1.00, "The Diamond suit: every spell is an experiment."),
    FIVE_SIDED(Kingdom.DIAMOND, 4, 1.03, 0.97, "The fifth side is Good Fortune."),
    CRACKED_DIAMOND(Kingdom.DIAMOND, 3, 0.97, 1.03, "Misfortune has split the cover. Luck runs the other way now.");

    public final Kingdom kingdom;
    public final int tier;
    public final double damage, cost;
    public final String lore;

    GrimoireCover(Kingdom kingdom, int tier, double damage, double cost, String lore) {
        this.kingdom = kingdom; this.tier = tier; this.damage = damage; this.cost = cost; this.lore = lore;
    }

    public boolean isRare() { return this == FOUR_LEAF || this == FIVE_SIDED; }   // luck covers (+5% page chance)
    public boolean isForbidden() { return tier >= 5; }
    public boolean isCracked() { return this == CRACKED_HEART || this == CRACKED_DIAMOND; }

    public String displayName() {
        return switch (this) {
            case THREE_LEAF -> "Three-Leaf Grimoire"; case FOUR_LEAF -> "Four-Leaf Grimoire"; case FIVE_LEAF -> "Five-Leaf Grimoire";
            case SPADE -> "Spade Grimoire"; case DOUBLE_SPADE -> "Double-Spade Grimoire"; case TRIPLE_SPADE -> "Triple-Spade Grimoire";
            case HEART -> "Heart Grimoire"; case TWO_HEART -> "Two-Heart Grimoire"; case CRACKED_HEART -> "Cracked-Heart Grimoire";
            case DIAMOND -> "Diamond Grimoire"; case FIVE_SIDED -> "Five-Sided Diamond Grimoire"; case CRACKED_DIAMOND -> "Cracked-Diamond Grimoire";
        };
    }

    /** Despair evolution (four-leaf -> five-leaf, double spade -> triple spade), or null. */
    public GrimoireCover despair() {
        return this == FOUR_LEAF ? FIVE_LEAF : this == DOUBLE_SPADE ? TRIPLE_SPADE : null;
    }

    public static GrimoireCover byName(String name, int fallbackLeaves) {
        try { return valueOf(name); } catch (Exception e) {
            return fallbackLeaves >= 5 ? FIVE_LEAF : fallbackLeaves == 4 ? FOUR_LEAF : THREE_LEAF;
        }
    }
}
