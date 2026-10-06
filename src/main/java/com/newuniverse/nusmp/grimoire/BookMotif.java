package com.newuniverse.nusmp.grimoire;

import java.util.Locale;

/**
 * What is laid over a grimoire's covers. Since 0.24 every cover wears a design from the owner's art pack (tools/art/covers,
 * tools/gen_cover_art_textures.py): {@code cover_base_<set>} (the design's background shading, tinted to the magic's colour) under
 * {@code cover_art_<set>} (its ornament in the art's own colours, glowing while the book is held). Two canon specials keep their
 * own procedural overlay: Asta's tattered book and Karna's straps.
 */
public enum BookMotif {
    STRAPS, TATTERED,
    THREE_LEAF, FOUR_LEAF, FIVE_LEAF, SPADE, TRIPLE_SPADE, HEART, TWO_HEART, DIAMOND, BLACK_MAGIC, GOD_TIER;

    /** True for the art-pack cover designs. */
    public boolean isArt() { return this != STRAPS && this != TATTERED; }

    private String lower() { return name().toLowerCase(Locale.ROOT); }

    /** The overlay texture (ornament). */
    public String texture() { return isArt() ? "cover_art_" + lower() : "ornament_" + lower(); }

    /** The tinted background texture under an art design, or null. */
    public String base() { return isArt() ? "cover_base_" + lower() : null; }

    public String displayName() {
        return switch (this) {
            case STRAPS -> "strapped"; case TATTERED -> "tattered";
            case THREE_LEAF -> "gold filigree and shamrock"; case FOUR_LEAF -> "gilded vines"; case FIVE_LEAF -> "royal flourishes";
            case SPADE -> "a crown of spears"; case TRIPLE_SPADE -> "arcane wheels"; case HEART -> "cloud swirls"; case TWO_HEART -> "a sea serpent";
            case DIAMOND -> "stained crystal"; case BLACK_MAGIC -> "a blood-red burst"; case GOD_TIER -> "a golden sunburst";
        };
    }

    /** The art-pack design of a cover. */
    public static BookMotif forCover(String cover) {
        return switch (cover == null ? "" : cover) {
            case "FOUR_LEAF" -> FOUR_LEAF;
            case "FIVE_LEAF" -> FIVE_LEAF;
            case "SPADE", "DOUBLE_SPADE" -> SPADE;
            case "TRIPLE_SPADE" -> TRIPLE_SPADE;
            case "HEART", "CRACKED_HEART" -> HEART;
            case "TWO_HEART" -> TWO_HEART;
            case "DIAMOND", "FIVE_SIDED", "CRACKED_DIAMOND" -> DIAMOND;
            case "BLACK_MAGIC" -> BLACK_MAGIC;
            case "GOD_TIER" -> GOD_TIER;
            default -> THREE_LEAF;
        };
    }
}
