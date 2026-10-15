package com.newuniverse.nusmp.labyrinth.dnd.economy;

import java.util.Set;

/**
 * Why: "a light in hand" has to cover items that carry no light-level property (a held torch is a block item whose block emits light, but a
 * lantern core from this mod or Tensura is a plain item that only lights through its id and behaviour). The decision is therefore split:
 * the handler asks the game first (a {@code BlockItem} whose block emits light counts, see {@link DungeonHunger}) and falls back to this pure
 * id test, which names the known light carriers and the words that mean "lamp" so a mod's new lantern is accepted without a list edit.
 * Pure: takes the item id as a namespace and a path.
 */
public final class LightItems {
    private LightItems() {}

    private static final Set<String> EXACT = Set.of("torch", "soul_torch", "redstone_torch", "lantern", "soul_lantern", "glowstone", "sea_lantern",
            "shroomlight", "campfire", "soul_campfire", "end_rod", "glow_berries", "ki_resonance_lantern", "magicule_lantern", "labyrinth_lantern");
    private static final String[] WORDS = {"lantern", "torch", "lamp", "glow", "luminous", "flame_core", "light_core", "sun_stone", "star_shard"};

    /** True if an item with this id is a light you can carry. */
    public static boolean isLight(String namespace, String path) {
        if (path == null) return false;
        if (EXACT.contains(path)) return true;
        for (String w : WORDS) if (path.contains(w)) return true;
        return false;
    }
}
