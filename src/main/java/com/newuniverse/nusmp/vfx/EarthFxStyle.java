package com.newuniverse.nusmp.vfx;

/**
 * 1.93.0 Why: the six earth-family magics (Sandstone, Rock, Red Ochre, Stone, Earth, Mineral) share ONE effect layer ({@code EarthFamilyLayer}, three shapes
 * EARTHFAM_CAST / ZONE / BURST) and differ by identity: the sprites, the palette, the way the debris moves. The identity travels in the one number every effect
 * already carries, the ARGB colour the spell passes (the magic's own book colour), so no shape had to be added per magic. This pure class is the table
 * (server spells read {@link #color}, the client layer reads {@link #of}); a unit test pins the colours to the books.
 */
public final class EarthFxStyle {
    private EarthFxStyle() {}

    public static final int SANDSTONE = 0, ROCK = 1, RED_OCHRE = 2, STONE = 3, EARTH = 4, MINERAL = 5;
    public static final String[] KEYS = {"sandstone", "rock", "red_ochre", "stone", "earth", "mineral"};
    /** Book colours (RGB): WikiBooks.color for the five wiki magics, EarthBook's for Earth. */
    private static final int[] RGB = {0xD5A76C, 0x777C86, 0xD64C37, 0x8F8C86, 0xB08850, 0x78A8C8};
    /** The light (additive glow) colour of each. */
    private static final int[] GLOW = {0xFFE08C, 0xFF9640, 0xFF7840, 0x96E6FF, 0xD2E65A, 0x78DCFF};
    /** The dark (shadow, soot, stain) colour of each. */
    private static final int[] DARK = {0x7A522A, 0x262A34, 0x62201A, 0x46443F, 0x3A2412, 0x30405A};

    /** The ARGB colour a spell of this magic passes to the effect. */
    public static int color(int style) { return 0xFF000000 | RGB[Math.floorMod(style, RGB.length)]; }

    public static int glow(int style) { return 0xFF000000 | GLOW[Math.floorMod(style, GLOW.length)]; }

    public static int dark(int style) { return 0xFF000000 | DARK[Math.floorMod(style, DARK.length)]; }

    /** The style number each magic hands to {@code PlayerAuras.set(p, Aura.WIKI, ticks, style)}: its WikiStyles index (Earth, which is not a wiki magic, gets 100). */
    private static final int[] ARMOUR = {12, 11, 10, 26, 100, 3};

    public static int armourStyle(int style) { return ARMOUR[Math.floorMod(style, ARMOUR.length)]; }

    public static int style(String enumName) {
        if (enumName == null) return -1;
        return switch (enumName) {
            case "SANDSTONE" -> SANDSTONE;
            case "ROCK" -> ROCK;
            case "RED_OCHRE" -> RED_OCHRE;
            case "STONE" -> STONE;
            case "EARTH" -> EARTH;
            case "MINERAL" -> MINERAL;
            default -> -1;
        };
    }

    /** The style whose book colour is nearest to this ARGB colour (an exact match for every colour a spell passes). */
    public static int of(int argb) {
        int r = (argb >> 16) & 255, g = (argb >> 8) & 255, b = argb & 255, best = 0;
        long bd = Long.MAX_VALUE;
        for (int i = 0; i < RGB.length; i++) {
            int dr = r - ((RGB[i] >> 16) & 255), dg = g - ((RGB[i] >> 8) & 255), db = b - (RGB[i] & 255);
            long d = (long) dr * dr + (long) dg * dg + (long) db * db;
            if (d < bd) { bd = d; best = i; }
        }
        return best;
    }

    /** True when the colour is (about) one of the six book colours: the layer then trusts it, else it draws the palette it finds nearest. */
    public static boolean exact(int argb) {
        int s = of(argb);
        return (argb & 0xFFFFFF) == RGB[s];
    }
}
