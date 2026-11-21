package com.newuniverse.nusmp.grimoire;

import com.newuniverse.nusmp.blackclover.GrimoireCover;

/**
 * Canon leaves (docs/grimoire_canon_leaves.md): what a cover MEANS in Black Clover, one short line per cover. Pure strings (no game class but the enum), so it is tested without a game and the
 * tooltip, the wizard line and the admin command all read the same text. The enum lore is not touched. Facts are from memory (the English wiki was not reachable when this was written).
 * BLACK_MAGIC and GOD_TIER are made up by this mod and say so.
 */
public final class GrimoireMeaning {
    private GrimoireMeaning() {}

    private static final String THREE = "Three leaves: Faith, Hope and Love. The grimoire of almost every mage.";
    private static final String FOUR = "Four leaves: the fourth leaf is Luck, Good Fortune. Rare, a sign of being chosen (as with Yuno).";
    private static final String FIVE = "Five leaves: the Devil's leaf. A forbidden, devil-bound grimoire (as with Asta's anti-magic book).";

    /** True for the covers this mod invented (not in the series). */
    public static boolean modOnly(GrimoireCover c) { return c != null && c.isLegendary(); }

    /** The lang key of the meaning line of a cover (en_us holds the same text as {@link #text}). */
    public static String key(GrimoireCover c) { return "anime_multiverse.grimoire.meaning." + (c == null ? "none" : c.name().toLowerCase(java.util.Locale.ROOT)); }

    /** The meaning line of a cover; never empty (a null cover reads as the three-leaf). */
    public static String text(GrimoireCover c) {
        if (c == null) return THREE;
        return switch (c) {
            case THREE_LEAF -> THREE;
            case FOUR_LEAF -> FOUR;
            case FIVE_LEAF -> FIVE;
            case BLACK_MAGIC -> "Mod-only, not canon: a black grimoire written by the devil Liebe's power, above the five-leaf.";
            case GOD_TIER -> "Mod-only, not canon: a legendary gold grimoire above every leaf, not devil-bound.";
            default -> suit(c);
        };
    }

    /** A suit cover: its kingdom's cover, ranked by the same leaf count as the clover ones. */
    private static String suit(GrimoireCover c) {
        String rank = c.tier >= 5 ? "five-leaf rank, forbidden and devil-bound" : c.tier == 4 ? "four-leaf rank, rare, the luck leaf" : "three-leaf rank, common (Faith, Hope, Love)";
        return c.kingdom.displayName + " suit cover: " + rank + ".";
    }
}
