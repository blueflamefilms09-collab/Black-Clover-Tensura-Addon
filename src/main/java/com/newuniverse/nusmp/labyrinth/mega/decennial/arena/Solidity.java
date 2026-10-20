package com.newuniverse.nusmp.labyrinth.mega.decennial.arena;

import java.util.List;

/**
 * Why: the plan of a hall is text, but its promises (a party can walk from the door to the altar, nothing leads off the edge, the boss stands on something with air above it) are about what a body
 * can pass through. This one place says, from a block's text alone, whether a block stops a walker (SOLID), lets one through (PASSABLE: air, light, a carpet, a torch, a banner, a vine, a flower, a
 * cobweb is passable for the walk check although it slows), is a liquid (walked through, or swum), or hurts (HAZARD: lava, magma, fire, a campfire, a cactus, a sweet berry bush, powder snow).
 * It reads the id only (properties are ignored), is pure, and is shared by the plan's own checks, the tests and the previews so they all agree.
 */
public final class Solidity {
    private Solidity() {}

    public enum Kind { SOLID, PASSABLE, LIQUID, HAZARD }

    private static final List<String> PASS_SUFFIX = List.of("_carpet", "_torch", "_wall_torch", "_banner", "_wall_banner", "_sign", "_wall_sign", "_hanging_sign", "_button", "_pressure_plate",
            "_head", "_wall_head", "_skull", "_wall_skull", "_candle", "_sapling", "_bush", "_tulip", "_flower", "_vines", "_fungus", "_rail", "_cluster", "_bud", "_dripleaf_stem");
    private static final List<String> PASS_EXACT = List.of("air", "cave_air", "void_air", "light", "vine", "glow_lichen", "cobweb", "short_grass", "tall_grass", "fern", "large_fern", "dandelion", "poppy",
            "blue_orchid", "allium", "azure_bluet", "oxeye_daisy", "cornflower", "lily_of_the_valley", "lilac", "rose_bush", "peony", "sunflower", "torchflower", "pink_petals",
            "snow", "moss_carpet", "hanging_roots", "spore_blossom", "lantern", "soul_lantern", "chain", "end_rod", "lightning_rod", "redstone_wire", "string", "tripwire", "tripwire_hook", "lever", "ladder",
            "scaffolding", "sculk_vein", "weeping_vines", "weeping_vines_plant", "twisting_vines", "twisting_vines_plant", "cave_vines", "cave_vines_plant", "small_dripleaf", "lily_pad", "kelp", "kelp_plant",
            "seagrass", "tall_seagrass", "sugar_cane", "brown_mushroom", "red_mushroom", "nether_sprouts", "crimson_roots", "warped_roots", "flower_pot", "bell", "candle", "turtle_egg", "frogspawn",
            "pointed_dripstone", "amethyst_cluster", "small_amethyst_bud", "medium_amethyst_bud", "large_amethyst_bud", "structure_void", "skeleton_skull", "wither_skeleton_skull", "dragon_head", "creeper_head",
            "zombie_head", "player_head", "piglin_head", "item_frame", "painting", "sculk_sensor", "calibrated_sculk_sensor");
    private static final List<String> LIQUID = List.of("water", "bubble_column");
    private static final List<String> HAZARD = List.of("lava", "magma_block", "fire", "soul_fire", "campfire", "soul_campfire", "cactus", "sweet_berry_bush", "powder_snow", "wither_rose");

    private static String path(String spec) {
        String s = spec;
        int b = s.indexOf('[');
        if (b >= 0) s = s.substring(0, b);
        int c = s.indexOf(':');
        return c >= 0 ? s.substring(c + 1) : s;
    }

    public static Kind kind(String spec) {
        String p = path(spec);
        if (HAZARD.contains(p)) return Kind.HAZARD;
        if (LIQUID.contains(p)) return Kind.LIQUID;
        if (PASS_EXACT.contains(p)) return Kind.PASSABLE;
        for (String suf : PASS_SUFFIX) if (p.endsWith(suf)) return Kind.PASSABLE;
        return Kind.SOLID;
    }

    public static boolean solid(String spec) { return kind(spec) == Kind.SOLID; }

    /** May a body occupy a cell holding this block (passable or a liquid)? A null cell is air. */
    public static boolean open(String specOrNull) {
        if (specOrNull == null) return true;
        Kind k = kind(specOrNull);
        return k == Kind.PASSABLE || k == Kind.LIQUID;
    }

    /** A block the walk check accepts as ground: a solid block (a full or partial one: slabs and stairs count; a liquid or a hazard never does). */
    public static boolean ground(String spec) { return spec != null && kind(spec) == Kind.SOLID; }
}
