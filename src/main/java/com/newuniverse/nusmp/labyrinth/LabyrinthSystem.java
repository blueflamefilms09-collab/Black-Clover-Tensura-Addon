package com.newuniverse.nusmp.labyrinth;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: one entry point for the whole Labyrinth (1.35.0) so the mod class gains a single line instead of one per package, and so the ORDER of
 * registration is fixed in one place: every registry here puts its block items on {@code NUItems.ITEMS} (the pattern of
 * {@link com.newuniverse.nusmp.dungeon.DungeonBlocks}), which means {@code LabyrinthSystem.init} must run BEFORE
 * {@code NUItems.ITEMS.register(modEventBus)} in {@code NUSMP}, otherwise the items are added to a DeferredRegister that has already been
 * handed to the bus and never reach the registry. The packages are called in dependency order: masonry blocks first (the gate, the trials
 * and the vault name them), then the gate (dimension travel), the trials, the seals, the vault, and last the generation (processors and the
 * palette data map, which read everything above). The game-bus handlers of the gate package are added here too, after the registries, with
 * {@code addListener} like the rest of the mod (no {@code @SubscribeEvent} scanning).
 */
public final class LabyrinthSystem {
    private LabyrinthSystem() {}

    public static void init(IEventBus modBus) {
        com.newuniverse.nusmp.labyrinth.LabyrinthBlocks.init(modBus);                                      // A1: the masonry, glyphs, lanterns, magicule veins
        com.newuniverse.nusmp.labyrinth.gate.GateRegistry.init(modBus);                                    // C: the Labyrinth Gate and Exit Gate
        com.newuniverse.nusmp.labyrinth.trial.TrialRegistry.init(modBus);                                  // E1: Sealed Archway, Magic Barrier, runes and glyphs
        com.newuniverse.nusmp.labyrinth.seal.SealRegistry.init(modBus);                                    // E2: Gatekeeper doors, Peak Altar, Shadow Key, Floor Warden
        com.newuniverse.nusmp.labyrinth.vault.VaultRegistry.init(modBus);                                  // D: Relic Pedestal and the relic loot entry
        com.newuniverse.nusmp.labyrinth.gen.GenRegistry.init(modBus);                                      // B: processors, palette data map, arrival logic
        com.newuniverse.nusmp.labyrinth.trap.TrapRegistry.init(modBus);                                    // 1.36.0 F1: pitfall tile, snuffer glyph, sigil plates, riddle stone
        com.newuniverse.nusmp.labyrinth.trap.TrapRegistry.register(NeoForge.EVENT_BUS);                    // 1.36.0 F1: light-placement guard near a snuffer glyph
        com.newuniverse.nusmp.labyrinth.dnd.economy.EconomyRegistry.register(NeoForge.EVENT_BUS);          // 1.36.0 F2: stone and meat drops, no vanilla XP, hunger and gloom
        com.newuniverse.nusmp.labyrinth.dnd.vital.VitalStrike.register(NeoForge.EVENT_BUS);                // 1.36.0 F3: the HP buffer and the Vital Strike
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.labyrinth.gate.LabyrinthEvents::onPlayerTick);   // C: void rescue
        NeoForge.EVENT_BUS.addListener(com.newuniverse.nusmp.labyrinth.gate.LabyrinthEvents::onDeath);        // C: a death in the Labyrinth forgets the way back
    }
}
