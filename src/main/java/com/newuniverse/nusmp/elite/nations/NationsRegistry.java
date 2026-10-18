package com.newuniverse.nusmp.elite.nations;

import com.newuniverse.nusmp.elite.api.EliteHooks;
import com.newuniverse.nusmp.elite.nations.net.NationNet;
import com.newuniverse.nusmp.elite.nations.world.NationBlocks;
import com.newuniverse.nusmp.elite.nations.world.NationEvents;
import com.newuniverse.nusmp.elite.nations.world.NationSelfTests;
import com.newuniverse.nusmp.elite.nations.world.NationsConfig;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/**
 * Why: the one registry class of the Nations & War system (docs/elite/nations.md), in the shape the EliteTensuraAddon plan asks for: {@code init(IEventBus, ModContainer)} is the only thing {@code EliteSystem}
 * calls. It registers the server config {@code nusmp-elite-nations-server.toml} (master switch inside), the two blocks and their items (own registers), the three payloads, the game-bus handlers and the reload
 * listener, the in-game self-tests ({@code elite.nations.*}) and the client hooks (key, screen) on the client only; and it BINDS the two hooks this subsystem owns in {@code EliteHooks}:
 * {@code nationOf} and {@code atWar}. With the master switch off the hooks answer "" and false.
 */
public final class NationsRegistry {
    private NationsRegistry() {}

    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, NationsConfig.SPEC, "nusmp-elite-nations-server.toml");
        NationBlocks.init(modBus);
        modBus.addListener(NationNet::registerPayloads);
        modBus.addListener(NationsRegistry::onCreativeTab);
        NationEvents.register(NeoForge.EVENT_BUS);
        NationSelfTests.register();
        EliteHooks.nationOf = NationApi::nationOf;
        EliteHooks.atWar = NationApi::isAtWar;
        if (FMLEnvironment.dist == Dist.CLIENT) com.newuniverse.nusmp.client.elite.nations.NationsClient.init(modBus);
    }

    private static void onCreativeTab(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            e.accept(NationBlocks.BANNER_ITEM.get());
            e.accept(NationBlocks.BOARD_ITEM.get());
        }
    }
}
