package com.newuniverse.nusmp.elite.hub;

import com.newuniverse.nusmp.elite.api.EliteHooks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

/**
 * Why: the one entry point of the Hub economy (1.83.0, docs/elite/hub.md): the Tensura-coin wallet and its {@code EliteHooks.wallet} hook, the coin drops (global loot modifier for mobs and chests,
 * a death listener for the decennial Guardian), the two Hub NPCs, their menus and packets, the quest data attachment and the data reload of the quests and the shop, and the Dungeon Map. The
 * mod class gains no line: {@code EliteSystem.init} calls {@link #init} from its {@code hub} section. The Hub has its own item register, so the order against {@code NUItems.ITEMS.register}
 * does not matter, but everything must be registered before the registries freeze, which the call from the mod constructor guarantees. It registers the server config
 * {@code nusmp-elite-hub-server.toml}, every deferred register, the payloads, the game-bus listeners (with {@code addListener}, no annotation scanning, like the rest of the mod), the dungeon's floor
 * callbacks, the in-game self-tests ({@code elite.hub.*}) and, on a client only, the client half by name. Every system of the Hub is switched by the one master switch {@code hub.enabled} and does
 * nothing when it is off; the wallet hook is set once and follows the switch and the currency at each call.
 */
public final class HubRegistry {
    private HubRegistry() {}

    private static boolean done;

    public static synchronized void init(IEventBus modBus, ModContainer container) {
        if (done) return;
        done = true;
        container.registerConfig(ModConfig.Type.SERVER, HubConfig.SPEC, "nusmp-elite-hub-server.toml");

        HubAttachments.ATTACHMENTS.register(modBus);
        HubNpcs.init(modBus);
        HubItems.init(modBus);
        HubMenus.init(modBus);
        CoinLootModifier.register(modBus);
        modBus.addListener(HubNet::register);

        EliteHooks.wallet = Currency.HOOK;

        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent e) -> e.addListener(new HubReload()));
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, CoinBossDrops::onDrops);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, CoinMobDrops::onDrops);
        QuestEvents.register(NeoForge.EVENT_BUS);
        QuestEvents.registerFloorListener();
        HubNpcSpawner.register(NeoForge.EVENT_BUS);

        HubSelfTests.register();

        if (FMLEnvironment.dist == Dist.CLIENT) com.newuniverse.nusmp.client.elite.hub.HubClient.init(modBus);
    }
}
