package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.travel.MegaTravel;
import com.newuniverse.nusmp.labyrinth.mega.travel.StairSelfTests;
import com.newuniverse.nusmp.labyrinth.mega.travel.StairTravel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

/**
 * Why: the registration entry point of the generator (1.54.0), so {@code MegaSystem} wires the whole Mega-Dungeon core with the two calls it already makes.
 * {@code init} runs while the mod is being constructed: it only registers things that need no server (the {@link ExpansionQueue} as the one
 * {@code Expander} the facade talks to, the {@code ChainTravel} the Hub's gates call, and the self-tests); all real state is created lazily on the first server. {@code register} receives the game bus
 * and hooks the five moments the core cares about: every server tick (the expansion job places a few pieces within its time budget, and the players waiting
 * for sector 0 are released), the server start (sectors that were being generated when the server stopped are generated again from their saved seed), the
 * server stop (the queue, the waiting list and the template caches are dropped, so nothing outlives a server), a data pack reload (the pools and the Sector
 * Recipe are read again), and every player tick plus logout (the deepest Landing a player reached is remembered, a player who leaves stops waiting). Every
 * handler catches everything: a bug in the generator must never take the server tick down with it. The handlers are static methods registered by method
 * reference, the pattern the rest of the mod uses.
 */
public final class MegaGenRegistry {
    private static final Logger LOG = LogUtils.getLogger();

    private MegaGenRegistry() {}

    public static void init(IEventBus modBus) {
        MegaDungeon.setExpander(ExpansionQueue.get());
        MegaDungeon.setChainTravel(MegaTravel::enterChain);          // 1.55.0: the Hub's gates take a player up or down a road through the facade
        CoreSelfTests.register();
        StairSelfTests.register();                                    // 1.58.0: stairs.layout and stairs.travel
        CyclicSettings.register();                                    // 1.83.0: generation.cyclic and its knobs (nusmp-labyrinth-mega-gen-server.toml)
        CyclicSelfTests.register();                                   // 1.83.0: gen.cyclic
        ClearanceSettings.register();                                 // 1.107.0: gen.clearance7 (nusmp-labyrinth-mega-clearance-server.toml)
        ClearanceSelfTests.register();                                // 1.107.0: dungeon.clearance7
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(MegaGenRegistry::onServerTick);
        gameBus.addListener(MegaGenRegistry::onServerStarted);
        gameBus.addListener(MegaGenRegistry::onServerStopped);
        gameBus.addListener(MegaGenRegistry::onAddReloadListeners);
        gameBus.addListener(MegaGenRegistry::onPlayerTick);
        gameBus.addListener(MegaGenRegistry::onLogout);
        StairTravel.register(gameBus);                                // 1.58.0: walking onto the pad at the end of a staircase takes the player to the linked floor
    }

    private static final com.newuniverse.nusmp.perf.PerfProbe.Section PROBE_S_MEGA_GEN = com.newuniverse.nusmp.perf.PerfProbe.section("s.mega.gen");   // 1.99.0: performance probe (off by default: one flag read)

    private static void onServerTick(ServerTickEvent.Post e) {
        long t0 = com.newuniverse.nusmp.perf.PerfProbe.begin();
        try {
            onServerTick0(e);
        } finally {
            com.newuniverse.nusmp.perf.PerfProbe.end(PROBE_S_MEGA_GEN, t0);
        }
    }

    private static void onServerTick0(ServerTickEvent.Post e) {
        try {
            ExpansionQueue.get().tick(e.getServer());
            MegaTravel.tick(e.getServer());
        } catch (Throwable t) {
            LOG.error("[nusmp] The Mega-Dungeon tick failed: {}", t.toString(), t);
        }
    }

    private static void onServerStarted(ServerStartedEvent e) {
        try {
            ExpansionQueue.get().onServerStarted(e.getServer());
        } catch (Throwable t) {
            LOG.error("[nusmp] The Mega-Dungeon could not repair unfinished sectors: {}", t.toString(), t);
        }
    }

    private static void onServerStopped(ServerStoppedEvent e) {
        try {
            ExpansionQueue.get().onServerStopped();
            MegaTravel.onServerStopped();
        } catch (Throwable t) {
            LOG.error("[nusmp] The Mega-Dungeon did not shut down cleanly: {}", t.toString(), t);
        }
    }

    private static void onAddReloadListeners(AddReloadListenerEvent e) {
        e.addListener(VanillaPools.get());
    }

    private static void onPlayerTick(PlayerTickEvent.Post e) {
        if (e.getEntity() instanceof ServerPlayer p) MegaTravel.rememberLanding(p);
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        MegaTravel.forget(e.getEntity().getUUID());
    }
}
