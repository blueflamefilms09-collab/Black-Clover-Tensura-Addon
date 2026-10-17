package com.newuniverse.nusmp.labyrinth.mega.macro;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: {@link MacroBlocks} turns a {@link BlockKind} into a Minecraft block state, but WHICH block that is and with which properties is a table of plain
 * strings, and a wrong string (a block id that does not exist in 1.21.1, a property it does not have, a value it does not allow) is the kind of mistake that
 * shows up only as air, or as a crash, in a world nobody can open from here. Keeping the table here, free of Minecraft types, lets a unit test check every row
 * against the game's own block-state table ({@code tools/labyrinth/vanilla_block_states_1_21_1.json}, from the 1.21.1 data) and lets the documentation print
 * the very same table. It also holds the connection rules of the three railing blocks (wall, fence, chain), because those states depend on the neighbours and
 * the generator places blocks without neighbour updates: a fence would stand as a lone post and a wall without its sides if the state were not computed here.
 * Pure: no Minecraft import.
 *
 * <p>Material choices that are decisions, not accidents (the WHY is in docs/labyrinth/mega_macro.md): the gold trim of the Spire is {@code yellow_concrete}, not
 * gold blocks, because the Hub is buildable and a few thousand gold blocks within reach of a pickaxe would be a gold mine; the frame of the Holy gate (13 blocks,
 * inside the protected sphere round the gate) is real gold; the Abyssal frame is crying obsidian; the only block of this mod in the table is the existing Labyrinth
 * Gate.
 */
public final class KindBlocks {
    private KindBlocks() {}

    /** A block id and the properties to set, each as {@code name=value}. */
    public record Spec(String block, String... props) {}

    /** The id of the one block of this mod the table names. */
    public static final String GATE_ID = "nusmp:labyrinth_gate";

    /** The block (and the fixed properties) a kind is drawn with. {@link BlockKind#GATE} is the mod's own block; rails get their connections from the helpers below. */
    public static Spec of(BlockKind k) {
        switch (k) {
            case AIR: return new Spec("minecraft:air");
            case GRASS: return new Spec("minecraft:grass_block");
            case DIRT: return new Spec("minecraft:dirt");
            case STONE: return new Spec("minecraft:stone");
            case DEEPSLATE: return new Spec("minecraft:deepslate");
            case BEDROCK: return new Spec("minecraft:bedrock");
            case PATH: return new Spec("minecraft:stone_bricks");
            case POST: return new Spec("minecraft:stone_brick_wall", "up=true");
            case LANTERN: return new Spec("minecraft:lantern", "hanging=false");
            case SOUL_LANTERN: return new Spec("minecraft:soul_lantern", "hanging=false");
            case LANTERN_HANGING: return new Spec("minecraft:lantern", "hanging=true");
            case CHAIN_Y: return new Spec("minecraft:chain", "axis=y");
            case QUARTZ_BRICKS: return new Spec("minecraft:quartz_bricks");
            case SMOOTH_QUARTZ: return new Spec("minecraft:smooth_quartz");
            case CALCITE: return new Spec("minecraft:calcite");
            case GOLD_TRIM: return new Spec("minecraft:yellow_concrete");
            case GLASS: return new Spec("minecraft:glass");
            case QUARTZ_PILLAR: return new Spec("minecraft:quartz_pillar", "axis=y");
            case CHISELED_QUARTZ: return new Spec("minecraft:chiseled_quartz_block");
            case QUARTZ_SLAB: return new Spec("minecraft:quartz_slab", "type=bottom");
            case SMOOTH_QUARTZ_SLAB: return new Spec("minecraft:smooth_quartz_slab", "type=bottom");
            case QUARTZ_STAIR_N: return new Spec("minecraft:quartz_stairs", "facing=north", "half=bottom", "shape=straight");
            case QUARTZ_STAIR_E: return new Spec("minecraft:quartz_stairs", "facing=east", "half=bottom", "shape=straight");
            case QUARTZ_STAIR_S: return new Spec("minecraft:quartz_stairs", "facing=south", "half=bottom", "shape=straight");
            case QUARTZ_STAIR_W: return new Spec("minecraft:quartz_stairs", "facing=west", "half=bottom", "shape=straight");
            case RAMP_BLOCK: return new Spec("minecraft:deepslate_bricks");
            case RAMP_SLAB: return new Spec("minecraft:deepslate_brick_slab", "type=bottom");
            case RAIL_LIGHT: return new Spec("minecraft:end_stone_brick_wall", "up=true");
            case RAIL_DARK: return new Spec("minecraft:nether_brick_fence");
            case RAIL_CHAIN: return new Spec("minecraft:chain", "axis=x");
            case BLACKSTONE: return new Spec("minecraft:blackstone");
            case BASALT: return new Spec("minecraft:basalt", "axis=y");
            case COBBLED_DEEPSLATE: return new Spec("minecraft:cobbled_deepslate");
            case DRIPSTONE_BLOCK: return new Spec("minecraft:dripstone_block");
            case SCULK: return new Spec("minecraft:sculk");
            case MOSS: return new Spec("minecraft:moss_block");
            case MOSS_CARPET: return new Spec("minecraft:moss_carpet");
            case GLOW_LICHEN: return new Spec("minecraft:glow_lichen", "down=true");
            case POINTED_DRIPSTONE_UP: return new Spec("minecraft:pointed_dripstone", "vertical_direction=up", "thickness=tip");
            case POINTED_DRIPSTONE_DOWN: return new Spec("minecraft:pointed_dripstone", "vertical_direction=down", "thickness=tip");
            case WATER: return new Spec("minecraft:water", "level=0");
            case GATE_FRAME: return new Spec("minecraft:gold_block");
            case GATE: return new Spec(GATE_ID);
            case ABYSS_FRAME: return new Spec("minecraft:crying_obsidian");
            case PLATFORM_DARK: return new Spec("minecraft:polished_blackstone_bricks");
            case PLATFORM_DARK_TRIM: return new Spec("minecraft:chiseled_polished_blackstone");
            case DEEPSLATE_TILES: return new Spec("minecraft:deepslate_tiles");
            case POLISHED_DEEPSLATE: return new Spec("minecraft:polished_deepslate");
            case NATIVE: return new Spec("minecraft:stone");
            default: return new Spec("minecraft:air");
        }
    }

    // ---- connections of the railing blocks

    /** The kinds a fence, wall or chain of the railings joins to: the other railing blocks and any full block. */
    public static boolean joinsRail(BlockKind neighbour) {
        return neighbour.solid || neighbour.isRail() || neighbour == BlockKind.POST;
    }

    /** A fence's four connection properties from which neighbours it joins ({@code north}, {@code east}, {@code south}, {@code west}). */
    public static List<String> fenceProps(boolean north, boolean east, boolean south, boolean west) {
        List<String> p = new ArrayList<>(4);
        p.add("north=" + north);
        p.add("east=" + east);
        p.add("south=" + south);
        p.add("west=" + west);
        return p;
    }

    /**
     * A wall's properties: each side {@code none} or {@code low}, and {@code up}. The post in the middle is shown unless the wall runs straight through
     * (north and south only, or east and west only) with nothing above it, the way the game itself decides it.
     */
    public static List<String> wallProps(boolean north, boolean east, boolean south, boolean west, boolean blockAbove) {
        List<String> p = new ArrayList<>(5);
        p.add("north=" + (north ? "low" : "none"));
        p.add("east=" + (east ? "low" : "none"));
        p.add("south=" + (south ? "low" : "none"));
        p.add("west=" + (west ? "low" : "none"));
        boolean straight = (north && south && !east && !west) || (east && west && !north && !south);
        p.add("up=" + (!straight || blockAbove));
        return p;
    }

    /** The axis a horizontal chain lies along: x when a rail continues east or west of it, else z. */
    public static String chainAxis(boolean east, boolean west, boolean north, boolean south) {
        if (east || west) return "x";
        if (north || south) return "z";
        return "x";
    }
}
