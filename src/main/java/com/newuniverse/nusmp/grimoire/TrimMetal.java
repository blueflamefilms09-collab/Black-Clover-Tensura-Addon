package com.newuniverse.nusmp.grimoire;

/** Render layer 1: corner clasps and spine bindings. {@link #color} is the default tint; the style picks texture and geometry. APPEND ONLY. */
public enum TrimMetal {
    GOLD("Gold", Style.PLAIN, 0xE0B030),
    SILVER("Silver", Style.PLAIN, 0xC8CCD4),
    BRONZE("Bronze", Style.PLAIN, 0xB07840),
    IRON("Iron", Style.HEAVY, 0x7A8190),
    OBSIDIAN("Obsidian", Style.HEAVY, 0x4A3F66),
    RUNED("Runed", Style.RUNED, 0x7A8CC0),
    /** Canon grimoires carry no corner caps or spine bands: just leather. (Appended last: ordinals are stored.) */
    NONE("None", Style.NONE, 0xC9A84A);

    /** The geometry + texture family; part of the render key (the colour alone is not). */
    public enum Style {
        PLAIN(GrimoireSprite.TRIM_PLAIN, 2.6f), HEAVY(GrimoireSprite.TRIM_HEAVY, 3.1f), RUNED(GrimoireSprite.TRIM_RUNED, 2.8f),
        NONE(GrimoireSprite.TRIM_PLAIN, 0f);
        public final GrimoireSprite sprite;
        public final float bracket;
        Style(GrimoireSprite sprite, float bracket) { this.sprite = sprite; this.bracket = bracket; }
    }

    public final String displayName;
    public final Style style;
    public final int color;

    TrimMetal(String displayName, Style style, int color) { this.displayName = displayName; this.style = style; this.color = color; }

    private static final TrimMetal[] VALUES = values();
    public static TrimMetal byOrdinal(int i) { return VALUES[Math.floorMod(i, VALUES.length)]; }
}
