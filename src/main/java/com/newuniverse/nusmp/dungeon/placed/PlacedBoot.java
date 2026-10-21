package com.newuniverse.nusmp.dungeon.placed;

import com.newuniverse.nusmp.dungeon.DungeonGuard;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: the one place that wires the player-placed block exemption into the game (one call from the mod constructor): its server config, its packet, its
 * events, the break rule that lets the Dungeon Guard's break / break-speed / left-click handlers through for a player-placed block, and its self-tests. The
 * two hooks in the Dungeon Guard's block writers (a removal of a marked block is let through, a builder's write over a mark drops it) call
 * {@link PlayerPlaced} directly.
 */
public final class PlacedBoot {
    private PlacedBoot() {}

    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, PlacedConfig.SPEC, "nusmp-dungeon-placed-server.toml");
        modBus.addListener(PlacedNet::registerPayloads);
        PlacedEvents.register(NeoForge.EVENT_BUS);
        DungeonGuard.addBreakRule(PlayerPlaced::breakRule);
        com.newuniverse.nusmp.labyrinth.loot.backpack.PlacedHook.install((lv, pos, by) -> PlayerPlaced.mark(lv, pos, by instanceof net.minecraft.server.level.ServerPlayer sp ? sp : null));
        PlacedSelfTests.register();
        if (FMLEnvironment.dist == Dist.CLIENT) PlacedClientBoot.init(NeoForge.EVENT_BUS);
    }
}
