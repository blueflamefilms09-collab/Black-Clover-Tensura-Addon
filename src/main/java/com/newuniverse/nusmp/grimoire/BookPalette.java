package com.newuniverse.nusmp.grimoire;

/**
 * Colours of a grimoire by its magic, after the Black Clover grimoire art: the cover leather, the metal of the frame and ornaments,
 * and the glow when the book is in use. Keyed by MagicType name so this stays pure (no game classes) and testable.
 */
public final class BookPalette {
    public static final int GOLD = 0xE2B95A, SILVER = 0xC9D1DD, BRONZE = 0xB37D45, DARK = 0x3A3232;

    private BookPalette() {}

    /** Cover leather, RGB: the hand-made colour of the magic, or a generated one ({@link GrimoireDesignGenerator}) for magics without an entry. */
    public static int cover(String magic) {
        int fixed = fixedCover(magic);
        if (fixed >= 0) return fixed;
        return magic.isEmpty() ? 0x8A6A4A : GrimoireDesignGenerator.forMagic(magic).cover();
    }

    /** True if this magic has a hand-made palette (everything else is generated); a book with no magic keeps the old default look. */
    public static boolean handMade(String magic) { return magic.isEmpty() || fixedCover(magic) >= 0; }

    private static int fixedCover(String magic) {
        return switch (magic) {
            case "FLAME" -> 0x9C2117; case "EXPLOSION" -> 0xC4561C; case "MAGMA" -> 0x8A2A12;
            case "WATER" -> 0x3F78C8; case "ICE" -> 0x8CC6E6; case "MERCURY" -> 0xB8BECC; case "MIST" -> 0xA7B6C8;
            case "WIND" -> 0x2F5A2C; case "STAR" -> 0x26306E; case "STORM" -> 0x3F7F86;
            case "EARTH" -> 0x7A5430; case "PLANT" -> 0x4E8A38; case "SAND" -> 0xC6A866;
            case "LIGHT" -> 0xDDBE58; case "LIGHTNING" -> 0x8FC2D6; case "SWORD" -> 0x7E8796;
            case "DARK" -> 0x4A1650; case "SHADOW" -> 0x34343F; case "POISON" -> 0x6F8E33;
            case "SPATIAL" -> 0x4A3A7E; case "MIRROR" -> 0x6A62C2; case "GRAVITY" -> 0x3D2462;
            case "TIME" -> 0xE2D6B8; case "SEALING" -> 0x2C4FA8;
            case "REINFORCEMENT" -> 0xC09A48; case "BEAST" -> 0xB89466; case "BONE" -> 0x8E8E96; case "BLOOD" -> 0x6E0A14;
            case "CREATION" -> 0xE6E0D0; case "COPY" -> 0x5F6A80; case "ILLUSION" -> 0x8E4AB0; case "DREAM" -> 0xE69CC8;
            case "ANTI_MAGIC" -> 0x1C1918;
            case "STEEL" -> 0x8FA3B4; case "THREAD" -> 0xB28CC4;
            case "TRANSMUTATION" -> 0x5E6E78; case "ASH" -> 0x55524E; case "COTTON" -> 0xF2EEE4; case "RECOMBINATION" -> 0x3A3430;
            case "PAINTING" -> 0x2E5C8A;
            case "WORLD_TREE" -> 0x4A3A22; case "DICE" -> 0x5A2A7A; case "SLASH" -> 0x1E5A3A; case "COMPASS" -> 0x3A6A4A; case "GAME" -> 0x2A2A6A;
            case "KOTODAMA" -> 0x1E1640;
            case "DEMON_BEAST" -> 0x3A0F2E;
            case "BODY" -> 0xA05A3A;
            case "EYE" -> 0x6A2A4A;
            case "EYEBALL" -> 0x5A1A2A;
            case "CURSE" -> 0x1E0A24;
            case "CURSE_WARDING" -> 0x3A2A5A;
            case "DEMON_FIRE" -> 0x1A0A1A;
            case "DEMON_ICE" -> 0x10182A;
            case "DEMON_LIGHT" -> 0x1A1A0A;
            case "DEMON_WATER" -> 0x0A141A;
            case "BARRIER" -> 0x2A6A8A;
            case "KEY" -> 0x8A6A1A;
            case "CHAIN" -> 0x4A4A52;
            case "BUTOH" -> 0x6A1A3A;
            case "BRIAR" -> 0x3A5A2A;
            case "CHERRY_BLOSSOM" -> 0xD87AA0;
            case "FUNGUS" -> 0x6A5A3A;
            case "FOOD" -> 0xC8803A;
            case "CRYSTAL" -> 0x6A9AD0;
            case "CORUNDUM" -> 0xB02A3A;
            case "BRONZE" -> 0x8A5A2A;
            case "COPPER" -> 0xB0602A;
            case "IRON" -> 0x5A5E66;
            case "BLACK_OIL" -> 0x101018;
            case "GEL" -> 0x4AA0A0;
            case "GLASS" -> 0x8AC0D0;
            case "BUBBLE" -> 0x6A9AE0;
            case "ICE_WEDGE" -> 0x6AB0D8;
            case "LEGION" -> 0x6A5A3A;
            default -> -1;
        };
    }

    /** Frame and ornament metal, RGB: gold for most books, silver for cold and metal magics, bronze for earthy ones. */
    public static int trim(String magic) {
        if (!handMade(magic)) return GrimoireDesignGenerator.forMagic(magic).trim();
        return switch (magic) {
            case "WATER", "ICE", "MERCURY", "MIST", "MIRROR", "STEEL", "SWORD", "CREATION", "LIGHTNING", "SEALING", "CURSE_WARDING", "BARRIER", "CHAIN", "CHERRY_BLOSSOM", "CRYSTAL", "CORUNDUM", "IRON", "GEL", "GLASS", "BUBBLE", "ICE_WEDGE" -> SILVER;
            case "EARTH", "SAND", "BEAST", "BONE", "BODY", "BRIAR", "FUNGUS", "BRONZE", "COPPER", "LEGION" -> BRONZE;
            case "ANTI_MAGIC", "DEMON_BEAST", "EYEBALL", "CURSE", "DEMON_FIRE", "DEMON_ICE", "DEMON_LIGHT", "DEMON_WATER", "BLACK_OIL" -> DARK;
            default -> GOLD;
        };
    }

    /** Glow while the book is out and in use, RGB. Anti-Magic is a dark, light-eating red. */
    public static int glow(String magic) {
        if (!handMade(magic)) return GrimoireDesignGenerator.forMagic(magic).glow();
        return switch (magic) {
            case "FLAME" -> 0xFF4A1F; case "EXPLOSION" -> 0xFF8A2A; case "MAGMA" -> 0xFF5A14;
            case "WATER" -> 0x3F8CFF; case "ICE" -> 0x9FE8FF; case "MERCURY" -> 0xC8D0E0; case "MIST" -> 0xD0DCEC;
            case "WIND" -> 0x4DFF9A; case "STAR" -> 0x7A8CFF; case "STORM" -> 0x38E0D0;
            case "EARTH" -> 0xB07A3C; case "PLANT" -> 0x7CE04A; case "SAND" -> 0xE8C878;
            case "LIGHT" -> 0xFFE680; case "LIGHTNING" -> 0x5AE6FF; case "SWORD" -> 0xC8D2E6;
            case "DARK" -> 0xA04CFF; case "SHADOW" -> 0x6A5AA8; case "POISON" -> 0xB4E040;
            case "SPATIAL" -> 0xB070FF; case "MIRROR" -> 0xC0F0FF; case "GRAVITY" -> 0x8A4CD0;
            case "TIME" -> 0xE8C050; case "SEALING" -> 0xFF5A78;
            case "REINFORCEMENT" -> 0xFFC040; case "BEAST" -> 0xFFB060; case "BONE" -> 0xF0EAD0; case "BLOOD" -> 0xE0182C;
            case "CREATION" -> 0xFFFFFF; case "COPY" -> 0xA0B0D0; case "ILLUSION" -> 0xD070FF; case "DREAM" -> 0xFFA8E0;
            case "ANTI_MAGIC" -> 0xB01020;
            case "STEEL" -> 0xB8C4D8; case "THREAD" -> 0xFF4060;
            case "TRANSMUTATION" -> 0x7AF0D8; case "ASH" -> 0xB0AAA2; case "COTTON" -> 0xFFF4FA; case "RECOMBINATION" -> 0xFF9A3C;
            case "PAINTING" -> 0x7AE0FF;
            case "WORLD_TREE" -> 0x3CE08A; case "DICE" -> 0xB070FF; case "SLASH" -> 0x48FF7A; case "COMPASS" -> 0xFFC94A; case "GAME" -> 0xE8C04A;
            case "KOTODAMA" -> 0x8A4CFF;
            case "DEMON_BEAST" -> 0xB02A6A;
            case "BODY" -> 0xFF8A4A;
            case "EYE" -> 0xFF4A9A;
            case "EYEBALL" -> 0xFF3A3A;
            case "CURSE" -> 0x9A2AFF;
            case "CURSE_WARDING" -> 0xC08AFF;
            case "DEMON_FIRE" -> 0x8A2AD0;
            case "DEMON_ICE" -> 0x3A6AFF;
            case "DEMON_LIGHT" -> 0xFF3AD0;
            case "DEMON_WATER" -> 0x20D0A0;
            case "BARRIER" -> 0x6AD0FF;
            case "KEY" -> 0xFFD04A;
            case "CHAIN" -> 0xC0C8E0;
            case "BUTOH" -> 0xFF5A8A;
            case "BRIAR" -> 0x8AFF4A;
            case "CHERRY_BLOSSOM" -> 0xFFB0D0;
            case "FUNGUS" -> 0xC0A04A;
            case "FOOD" -> 0xFFC04A;
            case "CRYSTAL" -> 0xA0E0FF;
            case "CORUNDUM" -> 0xFF6A7A;
            case "BRONZE" -> 0xD09A4A;
            case "COPPER" -> 0xFF9A4A;
            case "IRON" -> 0xB0B8C8;
            case "BLACK_OIL" -> 0x40E0A0;
            case "GEL" -> 0x7AFFD8;
            case "GLASS" -> 0xE0FFFF;
            case "BUBBLE" -> 0xA0D8FF;
            case "ICE_WEDGE" -> 0xC0F0FF;
            case "LEGION" -> 0xD0C080;
            default -> 0xFFFFFF;
        };
    }

    /** Emblem colour by cover (clover gold, five-leaf black, suits in their kingdom's metal). */
    public static int emblem(String cover) {
        return switch (cover) {
            case "FIVE_LEAF", "BLACK_MAGIC" -> 0x141010;
            case "GOD_TIER" -> 0xFFE9A8;
            case "SPADE", "DOUBLE_SPADE" -> 0x9AA2B4;
            case "TRIPLE_SPADE" -> 0x2A2228;
            case "HEART", "TWO_HEART", "CRACKED_HEART" -> 0xF0A2B2;
            case "DIAMOND", "FIVE_SIDED", "CRACKED_DIAMOND" -> 0xA8D8F2;
            default -> 0xF0C860;
        };
    }

    /** The design a book gets when it isn't a named canon book: its cover's art-pack design; an Anti-Magic five-leaf is tattered. */
    public static BookMotif motif(String cover, String magic) {
        if ("ANTI_MAGIC".equals(magic) && "FIVE_LEAF".equals(cover)) return BookMotif.TATTERED;
        return BookMotif.forCover(cover);
    }

    /** Black magic: soot-black leather and blood-red metal. God-tier: ivory leather and bright gold, glowing white-gold. */
    public static final int BLACK_MAGIC_TRIM = 0x9A1C24, GOD_TIER_COVER = 0xF2EAD6, GOD_TIER_TRIM = 0xFFD670, GOD_TIER_GLOW = 0xFFF2B0;

    /** Blends two RGB colours ({@code t} 0 = a, 1 = b). */
    public static int mix(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 255) + ((((b >> 16) & 255) - ((a >> 16) & 255)) * t));
        int g = Math.round(((a >> 8) & 255) + ((((b >> 8) & 255) - ((a >> 8) & 255)) * t));
        int bl = Math.round((a & 255) + (((b & 255) - (a & 255)) * t));
        return (r << 16) | (g << 8) | bl;
    }

    /** Multiplies an RGB colour's brightness. */
    public static int scale(int rgb, float f) {
        int r = Math.min(255, Math.round(((rgb >> 16) & 255) * f)), g = Math.min(255, Math.round(((rgb >> 8) & 255) * f)), b = Math.min(255, Math.round((rgb & 255) * f));
        return (r << 16) | (g << 8) | b;
    }
}
