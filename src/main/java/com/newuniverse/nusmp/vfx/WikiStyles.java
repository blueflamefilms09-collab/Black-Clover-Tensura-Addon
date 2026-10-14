package com.newuniverse.nusmp.vfx;

import java.util.Locale;

/**
 * The table that maps each of the 30 wiki magics ({@code MagicType} AIR .. WING, the ones {@code WikiBooks.magicForColor} returns) to
 * a visual group and a per-magic variation (pure Java: it only knows the enum constant names, so it is unit tested).
 *
 * <p>The index of a magic is the numeric style the first WikiMagicLayer used; it stays the per-magic variation, so every magic keeps
 * a look of its own inside its group. The group decides the texture, the blend and the shape language (see docs/wiki_magic_vfx.md):
 * <pre>
 *   MOTES      Air, Snow, Smoke                                   wind-like motes that drift
 *   TRAILS     Hair, Poison Plant, Vine, Wing                     crescent slashing trails
 *   CIRCLES    Memory, Modification, Permeation, Soul, Sound,
 *              Switching, Tree                                    rotating rune rings with tick marks
 *   SHARDS     Mineral, Nail, Red Ochre, Rock, Sandstone, Scale,
 *              Shakudo, Spike, Stone                              crystalline radial spikes
 *   FLUID      Mucus, Mud, Tongue                                 droplets and splash arcs
 *   SPIRAL     Skin, Song, Vortex                                 helices
 *   INFERNAL   Soul Corpse                                        a dark circle with red embers
 * </pre>
 * Sandstone is the one magic the owner's list did not name; it joins the shards (it is a layered stone, and the field effect
 * already sparkled it like one).
 */
public final class WikiStyles {
    private WikiStyles() {}

    public enum Group {
        MOTES("motes", false), TRAILS("trails", true), CIRCLES("circles", true), SHARDS("shards", true),
        FLUID("fluid", false), SPIRAL("spiral", true), INFERNAL("infernal", false);

        /** The key used in texture file names: wiki_grp_&lt;key&gt;, wiki_shell_&lt;key&gt;. */
        public final String key;
        /** True when the group is drawn with additive blending (glowing), false for alpha blending (soft, solid-looking). */
        public final boolean additive;

        Group(String key, boolean additive) { this.key = key; this.additive = additive; }
    }

    /** How many wiki magics there are. */
    public static final int COUNT = 30;

    private record Entry(String name, Group group) {}

    private static Entry e(String name, Group group) { return new Entry(name, group); }

    /** Index = the numeric style (keep this order: it is also the order of the texture files and of the shape variations). */
    private static final Entry[] TABLE = {
            e("AIR", Group.MOTES),            // 0
            e("HAIR", Group.TRAILS),          // 1
            e("MEMORY", Group.CIRCLES),       // 2
            e("MINERAL", Group.SHARDS),       // 3
            e("MODIFICATION", Group.CIRCLES), // 4
            e("MUCUS", Group.FLUID),          // 5
            e("MUD", Group.FLUID),            // 6
            e("NAIL", Group.SHARDS),          // 7
            e("PERMEATION", Group.CIRCLES),   // 8
            e("POISON_PLANT", Group.TRAILS),  // 9
            e("RED_OCHRE", Group.SHARDS),     // 10
            e("ROCK", Group.SHARDS),          // 11
            e("SANDSTONE", Group.SHARDS),     // 12
            e("SCALE", Group.SHARDS),         // 13
            e("SHAKUDO", Group.SHARDS),       // 14
            e("SKIN", Group.SPIRAL),          // 15
            e("SMOKE", Group.MOTES),          // 16
            e("SNOW", Group.MOTES),           // 17
            e("SONG", Group.SPIRAL),          // 18
            e("SOUL_CORPSE", Group.INFERNAL), // 19
            e("SOUL", Group.CIRCLES),         // 20
            e("SOUND", Group.CIRCLES),        // 21
            e("SPIKE", Group.SHARDS),         // 22
            e("SWITCHING", Group.CIRCLES),    // 23
            e("TONGUE", Group.FLUID),         // 24
            e("TREE", Group.CIRCLES),         // 25
            e("STONE", Group.SHARDS),         // 26
            e("VINE", Group.TRAILS),          // 27
            e("VORTEX", Group.SPIRAL),        // 28
            e("WING", Group.TRAILS),          // 29
    };

    private static final int[] VARIANT = new int[COUNT];
    private static final int[] GROUP_SIZE = new int[Group.values().length];

    static {
        for (int i = 0; i < COUNT; i++) {
            Group g = TABLE[i].group();
            VARIANT[i] = GROUP_SIZE[g.ordinal()]++;
        }
    }

    /** The style index of an enum constant name (MagicType#name()), or -1 when it is not one of the 30. */
    public static int styleOf(String enumName) {
        if (enumName == null) return -1;
        for (int i = 0; i < COUNT; i++) if (TABLE[i].name().equals(enumName)) return i;
        return -1;
    }

    /** True for an index of the table. */
    public static boolean valid(int style) { return style >= 0 && style < COUNT; }

    /** The enum constant name of a style, or null when the index is outside the table. */
    public static String name(int style) { return valid(style) ? TABLE[style].name() : null; }

    /** The visual group of a style. An index outside the table reads as Air (the plain motes), as the layer always did for an unknown colour. */
    public static Group group(int style) { return valid(style) ? TABLE[style].group() : Group.MOTES; }

    /** The place of the style among the magics of its group, 0-based, in table order: drives the per-magic shape variation. */
    public static int variant(int style) { return valid(style) ? VARIANT[style] : 0; }

    /** How many magics share this group. */
    public static int groupSize(Group g) { return GROUP_SIZE[g.ordinal()]; }

    /** Minimum brightness (0..1 luma) an additive effect should have to read against the world. */
    public static final float MIN_GLOW_LUMA = 0.42f;

    /**
     * Lifts a dark RGB colour toward white until additive blending shows it (Shakudo's dark bronze would otherwise glow almost
     * invisibly); colours that are already bright are returned unchanged. Alpha is not touched (24-bit RGB in, 24-bit RGB out).
     */
    public static int lift(int rgb) {
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        float luma = (0.299f * r + 0.587f * g + 0.114f * b) / 255f;
        if (luma >= MIN_GLOW_LUMA) return rgb & 0xFFFFFF;
        float t = Math.min(0.6f, (MIN_GLOW_LUMA - luma) * 2.4f);
        r = Math.round(r + (255 - r) * t);
        g = Math.round(g + (255 - g) * t);
        b = Math.round(b + (255 - b) * t);
        return (r << 16) | (g << 8) | b;
    }

    /** Texture file name (no extension) of the per-magic accent sprite: wiki_air, wiki_poison_plant ... */
    public static String texture(int style) { return "wiki_" + (valid(style) ? TABLE[style].name() : "air").toLowerCase(Locale.ROOT); }

    /** Texture file name of the group's bulk sprite: wiki_grp_motes ... */
    public static String groupTexture(Group g) { return "wiki_grp_" + g.key; }

    /** Texture file name of the group's armour shell pattern (64x64 tileable): wiki_shell_motes ... */
    public static String shellTexture(Group g) { return "wiki_shell_" + g.key; }
}
