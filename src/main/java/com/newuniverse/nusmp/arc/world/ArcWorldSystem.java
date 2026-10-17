package com.newuniverse.nusmp.arc.world;

import com.newuniverse.nusmp.arc.arena.ArcArenaDirector;
import com.newuniverse.nusmp.arc.arena.ArcArenaEvents;
import com.newuniverse.nusmp.arc.arena.ChronicleLectern;
import com.newuniverse.nusmp.arc.hub.ArcHubNpcs;
import com.newuniverse.nusmp.arc.hub.ChronicleBuilder;
import com.newuniverse.nusmp.arc.hub.HubSiteGuard;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: the places where the characters of the arcs appear (the dungeon's floor bosses, the story arenas and the Chronicle, the friends in the Hub, the saga
 * rewards) are several small systems that must be started in one fixed order, and the lead should have exactly one line in the mod constructor for all of it,
 * as for {@code ArcSystem}. The order matters in two places: the roster of the seven older bosses must exist before anything reads the roster (the lead's
 * {@code ArcCharacters.bootstrap()} registers it first; the call here is the safety net, and registering the same record twice is harmless), and the Chronicle Sigil
 * goes onto {@code NUItems.ITEMS}, so this must run before {@code NUItems.ITEMS.register(modEventBus)}. It registers its own config file, so no separate
 * {@code registerConfig} line exists, and it must run after {@code ArcSystem.init} so that its death listener comes after the core's (the story credit is in the
 * story before the saga reward is settled). Safe to call twice.
 */
public final class ArcWorldSystem {
    private ArcWorldSystem() {}

    private static boolean done;

    /** The single entry point, called from the mod constructor on both sides, after {@code ArcSystem.init} and before {@code NUItems.ITEMS.register}. */
    public static synchronized void init(IEventBus modBus, ModContainer modContainer) {
        if (done) return;
        done = true;
        ExistingBosses.registerAll();                    // the seven older bosses as characters (a no-op when ArcCharacters.bootstrap() already did it)
        ArcWorldItems.init();                            // the Chronicle Sigil, onto NUItems.ITEMS
        modContainer.registerConfig(ModConfig.Type.SERVER, ArcWorldConfig.SPEC, "nusmp-arc-world-server.toml");
        IEventBus game = NeoForge.EVENT_BUS;
        ArcWorldCredit.register(game);                   // the story credit of the older bosses and the saga reward of every boss
        ArcFloorBosses.register(game);                   // the Guardian's contract for a boss that stands in its place, the leash, the wanderers' arena
        ArcArenaDirector.register(game);                 // the referee of every story arena
        ArcArenaEvents.register(game);                   // no placing, no spawns, the way-home lodestone
        ChronicleLectern.register(game);                 // the lectern's click
        ChronicleBuilder.register(game);                 // the Promenade and the Chronicle in the Hub
        HubSiteGuard.register(game);                     // their protection
        ArcHubNpcs.register(game);                       // the friends on the posts
        ArcWorldSelfTests.register();                    // keys arcworld.*, expected by the server smoke test
    }
}
