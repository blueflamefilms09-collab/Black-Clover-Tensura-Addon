package com.newuniverse.nusmp.avatar.aptitude;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Why: the contract record {@link Aptitude} is fixed (id, name, rarity, tax, magic, skills), but the Soul Forge screen also needs how an aptitude LOOKS: the
 * colour of its icon and aura tint, the icon texture and one line of flavour text. Those are data too (the same JSON file), so they are kept in this side
 * table next to {@link AptitudeCatalog}, replaced whole on every reload and read lock-free. Also the home of the rarity colours, so the server tooltip and
 * the client frames agree. Pure: no Minecraft class.
 *
 * @param tint        0xRRGGBB, the colour of the icon glyph, the aura tint and the frame glow
 * @param icon        a resource location of a texture ("nusmp:textures/gui/soulforge/apt/fire.png"); the screen draws a letter in a circle when it is missing
 * @param description one short line shown in the tooltip (may be empty)
 */
public record AptitudeLook(int tint, String icon, String description) {
    /** Frame colours of the five rarities (0xRRGGBB): grey-blue, green, cyan, violet, gold. Indexed by {@link Aptitude.Rarity#ordinal()}. */
    private static final int[] RARITY = {0xB8C4D8, 0x7CF0B0, 0x5CD8FF, 0x9A7CFF, 0xE8C468};

    private static volatile Map<String, AptitudeLook> looks = Map.of();

    public static int rarityColour(Aptitude.Rarity r) {
        int i = r == null ? 0 : r.ordinal();
        return RARITY[Math.min(i, RARITY.length - 1)];
    }

    /** The default icon path of an aptitude id. */
    public static String defaultIcon(String id) { return "nusmp:textures/gui/soulforge/apt/" + id + ".png"; }

    /** The look of this aptitude: the loaded one, or a default made from its rarity (never null). */
    public static AptitudeLook of(Aptitude a) {
        AptitudeLook l = looks.get(a.id());
        return l != null ? l : new AptitudeLook(rarityColour(a.rarity()), defaultIcon(a.id()), "");
    }

    /** Replaces all looks at once (called by the loader on every reload). */
    public static void replaceAll(Map<String, AptitudeLook> next) {
        looks = next == null ? Map.of() : Collections.unmodifiableMap(new HashMap<>(next));
    }

    /** Lower-case snake id check shared with the Avatar entity's own cleaning rule. */
    public static boolean validId(String id) { return id != null && id.toLowerCase(Locale.ROOT).equals(id) && id.matches("[a-z][a-z0-9_]*"); }
}
