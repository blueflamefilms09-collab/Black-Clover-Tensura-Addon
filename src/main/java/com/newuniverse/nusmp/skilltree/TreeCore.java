package com.newuniverse.nusmp.skilltree;

import com.newuniverse.nusmp.client.skilltree.TreeCoreClient;
import com.newuniverse.nusmp.skilltree.net.TreeNet;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * Why: the core of the Skill Tree is several small parts (the config, the datapack reload listener and registry, the payloads, the game-bus events, the self-tests) that must each be started
 * exactly once and on the right bus, and {@code SkillTreeSystem} should have one line for all of it. This is that line. The order does not matter between them, but the places do: the config is
 * registered on the mod container so the server's values are copied to a connecting client; the payload registration is a mod-bus listener (NeoForge fires {@code RegisterPayloadHandlersEvent}
 * there); the reload listener, the player events and the server events are game-bus listeners ({@link TreeEvents}); and the one client-only listener (forget the server's routes on leaving a
 * world) is started through {@link TreeCoreClient} only when the game really is a client, the way the rest of this mod starts its client code, so a dedicated server never loads a client class.
 * Safe to call twice.
 */
public final class TreeCore {
    private TreeCore() {}

    private static boolean done;

    public static synchronized void init(IEventBus modBus, ModContainer modContainer) {
        if (done) return;
        done = true;
        modContainer.registerConfig(ModConfig.Type.SERVER, TreeConfig.SPEC, "nusmp-skilltree-server.toml");
        modBus.addListener(TreeNet::registerPayloads);
        TreeEvents.register();
        TreeSelfTests.register();                                     // keys tree.core.*, expected by the server smoke test (prefix "tree")
        if (FMLEnvironment.dist == Dist.CLIENT) TreeCoreClient.init(modBus);
    }
}
