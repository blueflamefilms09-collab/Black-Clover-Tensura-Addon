package com.newuniverse.nusmp.elite.world;

import com.newuniverse.nusmp.elite.api.EliteHooks;
import com.newuniverse.nusmp.elite.world.calamity.CalamityManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: the ONE registry class of the world area ({@code elite/world}), as the plan asks: {@code init(IEventBus, ModContainer)} registers everything the area owns and nothing else, so
 * {@code EliteSystem} needs a single line. Contents: Bosses, World Events and Magicule Mechanics: the chunk magicule storage and sync, the five crystal blocks and the Mother Rock, ore
 * conversion, the touched-mob handler, the calamity system (the Gravebound Colossus, the Lich with its phylacteries, the Gaia Spike, the Void Bolt), the Magicule Gauge and the Calamity Core,
 * the commands, the data reload listener, the in-game self-tests and the {@code EliteHooks.chunkMagicule} hook. The config {@code nusmp-elite-world-server.toml} has a master switch (on by
 * default, conservative rates); with it off nothing here acts (the blocks and entities still exist so a world with them can load).
 */
public final class WorldRegistry {
    private WorldRegistry() {}

    public static void init(IEventBus modBus, ModContainer container) {
        WorldBlocks.init();
        WorldBlocks.BLOCKS.register(modBus);
        WorldItems.ITEMS.register(modBus);
        WorldEntities.TYPES.register(modBus);
        WorldAttachments.ATTACHMENTS.register(modBus);
        modBus.addListener(WorldEntities::attributes);
        modBus.addListener(MagiculeSync::registerPayloads);
        modBus.addListener(WorldEvents::onCreativeTab);
        container.registerConfig(ModConfig.Type.SERVER, WorldConfig.SPEC, "nusmp-elite-world-server.toml");

        IEventBus game = NeoForge.EVENT_BUS;
        WorldData.register(game);
        MagiculeEngine.register(game);
        MobTouchHandler.register(game);
        CalamityManager.register(game);
        WorldEvents.register(game);
        EliteHooks.chunkMagicule = MagiculeApi::effectiveAt;
        WorldSelfTests.register();

        if (FMLEnvironment.dist == Dist.CLIENT) com.newuniverse.nusmp.client.elite.world.WorldClient.init(modBus);
    }
}
