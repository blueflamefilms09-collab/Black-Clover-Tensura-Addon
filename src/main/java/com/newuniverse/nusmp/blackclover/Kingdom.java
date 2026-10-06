package com.newuniverse.nusmp.blackclover;

/** The four kingdoms. The kingdom that accepts a mage decides the suit on their grimoire. */
public enum Kingdom {
    CLOVER("Clover Kingdom"), SPADE("Spade Kingdom"), HEART("Heart Kingdom"), DIAMOND("Diamond Kingdom");

    public final String displayName;
    Kingdom(String displayName) { this.displayName = displayName; }

    /** Cover for a starting roll tier (3 = common, 4 = rare, 5 = forbidden). */
    public GrimoireCover coverFor(int tier) {
        return switch (this) {
            case CLOVER -> tier >= 5 ? GrimoireCover.FIVE_LEAF : tier == 4 ? GrimoireCover.FOUR_LEAF : GrimoireCover.THREE_LEAF;
            case SPADE -> tier >= 5 ? GrimoireCover.TRIPLE_SPADE : tier == 4 ? GrimoireCover.DOUBLE_SPADE : GrimoireCover.SPADE;
            // Heart's two-heart cover only comes from a true-love bond, never a roll.
            case HEART -> GrimoireCover.HEART;
            case DIAMOND -> tier >= 4 ? GrimoireCover.FIVE_SIDED : GrimoireCover.DIAMOND;
        };
    }
}
