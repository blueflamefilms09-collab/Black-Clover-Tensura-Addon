package com.newuniverse.nusmp.grimoire;

import com.newuniverse.nusmp.blackclover.GrimoireCover;
import com.newuniverse.nusmp.blackclover.Kingdom;

/**
 * The crest on a grimoire's front cover (render layer 2, tintindex 2).
 * The first 12 entries are the covers the mod already had (see {@link GrimoireCover}); the rest are new crest
 * variants. APPEND ONLY: the ordinal is stored on the network and the generator radix depends on the count.
 * <p>
 * count = leaves / hearts / spades / sides shown on the crest. tier: 3 common, 4 rare, 5 forbidden - it also decides how
 * much aura the book gets. kingdom is null for the special outliers (they belong to no kingdom).
 */
public enum InsigniaType {
    THREE_LEAF("Three-Leaf", Kingdom.CLOVER, 3, 3, 0xD8B24C),
    FOUR_LEAF("Four-Leaf", Kingdom.CLOVER, 4, 4, 0xF2D676),
    FIVE_LEAF("Five-Leaf", Kingdom.CLOVER, 5, 5, 0x302A2E),
    HEART("Heart", Kingdom.HEART, 1, 3, 0xE85C8A),
    TWO_HEART("Two-Heart", Kingdom.HEART, 2, 4, 0xF07AA0),
    TIERED_HEART("Tiered-Heart", Kingdom.HEART, 3, 4, 0xFF8FB0),
    CRACKED_HEART("Cracked-Heart", Kingdom.HEART, 1, 3, 0x9A3550),
    SPADE("Spade", Kingdom.SPADE, 1, 3, 0xE4E6F0),
    DOUBLE_SPADE("Double-Spade", Kingdom.SPADE, 2, 4, 0xC8CCDC),
    TRIPLE_SPADE("Triple-Spade", Kingdom.SPADE, 3, 5, 0x3C3C46),
    DIAMOND("Diamond", Kingdom.DIAMOND, 1, 3, 0x5AC8F0),
    DUAL_DIAMOND("Dual-Diamond", Kingdom.DIAMOND, 2, 4, 0x7ADCFF),
    FIVE_SIDED("Five-Sided Diamond", Kingdom.DIAMOND, 5, 4, 0xA8E8FF),
    CRACKED_DIAMOND("Cracked-Diamond", Kingdom.DIAMOND, 1, 3, 0x3A7898),
    BOUNDLESS("Boundless", null, 0, 5, 0xF0D060),
    FORBIDDEN_RUNES("Forbidden Runes", null, 5, 5, 0xB01030);

    public final String displayName;
    public final Kingdom kingdom;
    public final int count;
    public final int tier;
    public final int color;

    InsigniaType(String displayName, Kingdom kingdom, int count, int tier, int color) {
        this.displayName = displayName; this.kingdom = kingdom; this.count = count; this.tier = tier; this.color = color;
    }

    private static final InsigniaType[] VALUES = values();
    public static InsigniaType byOrdinal(int i) { return VALUES[Math.floorMod(i, VALUES.length)]; }

    /** The crest for a cover the mod already uses. Every {@link GrimoireCover} has a one-to-one crest. */
    public static InsigniaType of(GrimoireCover cover) {
        return switch (cover) {
            case THREE_LEAF -> THREE_LEAF; case FOUR_LEAF -> FOUR_LEAF; case FIVE_LEAF -> FIVE_LEAF;
            case SPADE -> SPADE; case DOUBLE_SPADE -> DOUBLE_SPADE; case TRIPLE_SPADE -> TRIPLE_SPADE;
            case HEART -> HEART; case TWO_HEART -> TWO_HEART; case CRACKED_HEART -> CRACKED_HEART;
            case DIAMOND -> DIAMOND; case FIVE_SIDED -> FIVE_SIDED; case CRACKED_DIAMOND -> CRACKED_DIAMOND;
        };
    }

    /**
     * The real cover whose gameplay (spell power, cost, rarity) a book with this crest uses. The four crests that exist only as
     * looks borrow their nearest cover: tiered heart = two-heart, dual diamond = five-sided, boundless = four-leaf (rare, not
     * forbidden), forbidden runes = triple spade.
     */
    public GrimoireCover toCover() {
        return switch (this) {
            case THREE_LEAF -> GrimoireCover.THREE_LEAF; case FOUR_LEAF -> GrimoireCover.FOUR_LEAF; case FIVE_LEAF -> GrimoireCover.FIVE_LEAF;
            case HEART -> GrimoireCover.HEART; case TWO_HEART, TIERED_HEART -> GrimoireCover.TWO_HEART; case CRACKED_HEART -> GrimoireCover.CRACKED_HEART;
            case SPADE -> GrimoireCover.SPADE; case DOUBLE_SPADE -> GrimoireCover.DOUBLE_SPADE; case TRIPLE_SPADE, FORBIDDEN_RUNES -> GrimoireCover.TRIPLE_SPADE;
            case DIAMOND -> GrimoireCover.DIAMOND; case DUAL_DIAMOND, FIVE_SIDED -> GrimoireCover.FIVE_SIDED; case CRACKED_DIAMOND -> GrimoireCover.CRACKED_DIAMOND;
            case BOUNDLESS -> GrimoireCover.FOUR_LEAF;
        };
    }

    public boolean isCracked() { return this == CRACKED_HEART || this == CRACKED_DIAMOND; }
    public boolean isSpecial() { return kingdom == null; }
}
