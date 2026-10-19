package com.newuniverse.nusmp.labyrinth.mega.puzzle;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Why: a rule shows its board as small numbers; this is the one table that says which BLOCK each number is, per palette name, as plain text ({@code minecraft:red_concrete},
 * {@code minecraft:lime_glazed_terracotta[facing=east]}), so the pure tests can prove that no rule ever shows a number its palette does not have, and the world layer only has to parse the
 * text. Vanilla blocks only (the Vanilla Block Primacy Rule), no block entities, nothing that falls, burns or ticks on its own. Face 0 of the floor-style palettes is the dungeon's dark tile,
 * which {@link #base} varies by the flavour of the theme so a puzzle laid in a frozen hall does not look like one in a forge. Pure.
 */
public final class Palettes {
    private Palettes() {}

    private static final String[] FACING = {"north", "east", "south", "west"};
    private static final String[] COLOURS = {"red", "blue", "lime", "yellow", "purple", "orange", "cyan", "white"};

    private static final Map<String, String[]> TABLE = new LinkedHashMap<>();

    private static String[] concrete(String... names) {
        String[] out = new String[names.length];
        for (int i = 0; i < names.length; i++) out[i] = "minecraft:" + names[i] + "_concrete";
        return out;
    }

    private static String[] blocks(String... names) {
        String[] out = new String[names.length];
        for (int i = 0; i < names.length; i++) out[i] = "minecraft:" + names[i];
        return out;
    }

    static {
        // colour: 0..7 concrete, 8..15 the same colours as glass (the "lit" look)
        String[] colour = new String[16];
        for (int i = 0; i < 8; i++) {
            colour[i] = "minecraft:" + COLOURS[i] + "_concrete";
            colour[8 + i] = "minecraft:" + COLOURS[i] + "_stained_glass";
        }
        TABLE.put("colour", colour);
        TABLE.put("tile", blocks("blackstone", "sea_lantern", "emerald_block", "redstone_block", "glowstone", "lapis_block", "quartz_block", "amethyst_block"));
        TABLE.put("primary", concrete("black", "red", "blue", "yellow", "purple", "orange", "green"));
        String[] arrow = new String[4];
        for (int i = 0; i < 4; i++) arrow[i] = "minecraft:lime_glazed_terracotta[facing=" + FACING[i] + "]";
        TABLE.put("arrow", arrow);
        TABLE.put("digit", concrete("white", "light_gray", "gray", "black", "brown", "red", "orange", "yellow", "lime", "cyan"));
        TABLE.put("pitch", concrete("white", "light_gray", "light_blue", "blue", "purple", "black"));
        TABLE.put("symbol", blocks("smooth_stone", "gold_block", "lapis_block", "emerald_block", "redstone_block", "diamond_block", "amethyst_block", "copper_block", "iron_block", "coal_block", "quartz_block", "prismarine_bricks", "bone_block"));
        TABLE.put("rune", blocks("gold_block", "lapis_block", "emerald_block", "redstone_block", "amethyst_block", "diamond_block", "smooth_stone"));
        TABLE.put("water", blocks("glass", "light_blue_stained_glass", "yellow_stained_glass", "blue_stained_glass"));
        TABLE.put("beam", new String[] {"minecraft:blackstone", "minecraft:sea_lantern", "minecraft:light_blue_glazed_terracotta[facing=north]", "minecraft:light_blue_glazed_terracotta[facing=east]",
                "minecraft:cyan_glazed_terracotta[facing=north]", "minecraft:cyan_glazed_terracotta[facing=east]", "minecraft:gold_block", "minecraft:redstone_block", "minecraft:emerald_block", "minecraft:polished_blackstone"});
        // pipe: type (0 straight, 1 corner, 2 tee) x orientation (0..3) x wet (0/1): index = type*8 + o*2 + wet; 24 inlet, 25 wall, 26 drain dry, 27 drain flowing
        String[] pipe = new String[28];
        String[] dry = {"white", "orange", "magenta"}, wet = {"light_blue", "cyan", "blue"};
        for (int t = 0; t < 3; t++) for (int o = 0; o < 4; o++) {
            pipe[t * 8 + o * 2] = "minecraft:" + dry[t] + "_glazed_terracotta[facing=" + FACING[o] + "]";
            pipe[t * 8 + o * 2 + 1] = "minecraft:" + wet[t] + "_glazed_terracotta[facing=" + FACING[o] + "]";
        }
        pipe[24] = "minecraft:gold_block";                       // the inlet
        pipe[25] = "minecraft:polished_blackstone";              // the wall round the field
        pipe[26] = "minecraft:redstone_block";                   // the drain, dry
        pipe[27] = "minecraft:emerald_block";                    // the drain, flowing
        TABLE.put("pipe", pipe);
        TABLE.put("maze", blocks("stone_bricks", "blackstone", "sea_lantern", "gold_block", "emerald_block"));
        TABLE.put("lane", concrete("gray", "red", "yellow", "blue", "lime", "orange"));
        TABLE.put("pitjump", new String[] {"minecraft:red_concrete", "minecraft:blue_concrete", "minecraft:yellow_concrete", "minecraft:air", "minecraft:stone_bricks", "minecraft:gray_concrete"});
        TABLE.put("star", blocks("coal_block", "sea_lantern", "glowstone", "diamond_block"));
        TABLE.put("chest", blocks("chest", "cracked_stone_bricks", "gold_block"));
        TABLE.put("token", blocks("coal_block", "bone_block", "gold_block", "lapis_block", "amethyst_block", "redstone_block", "iron_block", "quartz_block", "emerald_block", "diamond_block", "copper_block"));
        TABLE.put("altar", blocks("smooth_quartz", "gold_block", "chiseled_quartz_block"));
        TABLE.put("elem", new String[] {"minecraft:magma_block", "minecraft:blue_ice", "minecraft:packed_mud", "minecraft:white_wool", "minecraft:light_gray_concrete", "minecraft:brown_concrete",
                "minecraft:orange_concrete", "minecraft:yellow_terracotta", "minecraft:gray_concrete", "minecraft:yellow_concrete", "minecraft:gold_block"});
        TABLE.put("charge", new String[] {"minecraft:gray_concrete", "minecraft:light_blue_concrete", "minecraft:yellow_concrete", "minecraft:orange_concrete",
                "minecraft:gray_stained_glass", "minecraft:light_blue_stained_glass", "minecraft:yellow_stained_glass", "minecraft:orange_stained_glass"});
        TABLE.put("keylock", new String[] {"minecraft:red_concrete", "minecraft:blue_concrete", "minecraft:lime_concrete", "minecraft:yellow_concrete", "minecraft:purple_concrete", "minecraft:orange_concrete", "minecraft:cyan_concrete",
                "minecraft:white_concrete", "minecraft:red_stained_glass", "minecraft:blue_stained_glass", "minecraft:lime_stained_glass", "minecraft:yellow_stained_glass", "minecraft:purple_stained_glass", "minecraft:orange_stained_glass",
                "minecraft:cyan_stained_glass", "minecraft:white_stained_glass", "minecraft:obsidian"});
        TABLE.put("sphinx", blocks("chiseled_sandstone", "gold_block"));
        TABLE.put("dial", blocks("sandstone", "black_concrete", "gold_block", "redstone_block"));
        TABLE.put("side", concrete("gray", "blue", "red"));
        TABLE.put("plain", blocks("stone_bricks", "sea_lantern"));
        TABLE.put("spirit", new String[] {"minecraft:blackstone", "minecraft:sea_lantern", "minecraft:glowstone", "minecraft:purple_concrete"});
    }

    /** The names of all palettes. */
    public static java.util.Set<String> names() { return TABLE.keySet(); }

    /** How many faces a palette has (0 for an unknown name). */
    public static int size(String palette) {
        String[] t = TABLE.get(palette);
        return t == null ? 0 : t.length;
    }

    /** The block text of a face, or null when the palette or the face does not exist. */
    public static String block(String palette, int face) {
        String[] t = TABLE.get(palette);
        return t == null || face < 0 || face >= t.length ? null : t[face];
    }

    /**
     * The dark base tile of a theme flavour (the floor look of "off"): the palettes whose face 0 is the dungeon's dark tile ({@code tile}, {@code beam}, {@code maze}, {@code lane},
     * {@code spirit}) use it instead of their own when the theme has a flavour. {@code flavour} is one of the tags of {@link PuzzleWeights}; unknown gives the default.
     */
    public static String base(String flavour) {
        if (flavour == null) return "minecraft:blackstone";
        return switch (flavour) {
            case "grave" -> "minecraft:deepslate_tiles";
            case "fire" -> "minecraft:nether_bricks";
            case "water" -> "minecraft:dark_prismarine";
            case "nature" -> "minecraft:mossy_stone_bricks";
            case "arcane" -> "minecraft:purpur_block";
            case "holy" -> "minecraft:smooth_quartz";
            case "frost" -> "minecraft:packed_ice";
            case "machine" -> "minecraft:polished_andesite";
            case "sky" -> "minecraft:smooth_stone";
            case "blood" -> "minecraft:red_nether_bricks";
            case "shadow" -> "minecraft:polished_blackstone";
            default -> "minecraft:blackstone";
        };
    }

    /** Does this palette use the themed dark base for its face 0? */
    public static boolean usesBase(String palette) {
        return palette.equals("tile") || palette.equals("beam") || palette.equals("maze") || palette.equals("lane") || palette.equals("spirit");
    }
}
