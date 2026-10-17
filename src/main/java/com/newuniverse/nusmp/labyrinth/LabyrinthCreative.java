package com.newuniverse.nusmp.labyrinth;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Why: the Labyrinth's blocks and items are registered by six independent registries (masonry, gate, trial, seal, vault, generation), each with
 * its own class. The creative tab wants one line, so this class lists the ids and looks them up in the item registry at display time: an id
 * that is not registered (a registry that failed, a renamed block in a future version) is skipped instead of crashing the tab, and the tab
 * never has to import six classes. The order is the order a player meets them: the gate, the masonry, the trial pieces, the seals, the vault.
 */
public final class LabyrinthCreative {
    private LabyrinthCreative() {}

    /** Registry ids (namespace nusmp) in display order. Only appended, never reordered. */
    public static final List<String> IDS = List.of(
            "labyrinth_gate", "labyrinth_exit",
            "labyrinth_bricks", "labyrinth_tiles", "labyrinth_pillar", "labyrinth_glyph", "labyrinth_lantern", "magicule_vein",
            "sealed_archway", "magic_barrier", "ward_rune", "spawn_glyph",
            "gatekeeper_door", "peak_altar", "shadow_key", "floor_warden_spawn_egg",
            "relic_pedestal",
            "pitfall_tile", "snuffer_glyph", "sigil_plate", "riddle_stone",
            "relic_forge",   // 1.36.0 traps and puzzles
            "shadow_core");   // 1.55.0: the Solo Leveling System's soul-extraction item

    /** Adds every registered Labyrinth item to the tab, in order; unknown ids are skipped. */
    public static void addTo(CreativeModeTab.Output output) {
        for (String id : IDS) {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("nusmp", id));
            if (item != null && item != Items.AIR) output.accept(item);
        }
    }
}
