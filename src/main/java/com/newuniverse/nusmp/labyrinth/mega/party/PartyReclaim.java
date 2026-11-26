package com.newuniverse.nusmp.labyrinth.mega.party;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.gate.LabyrinthTravel;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PartyReclaim {
    private static final Logger LOG = LogUtils.getLogger();

    private static final int TOUCH_EVERY = 1200;

    private static Set<Integer> purged = new HashSet<>();

    private PartyReclaim() {}

    public static void register(IEventBus gameBus) {
        gameBus.addListener(PartyReclaim::onAboutToStart);
        gameBus.addListener(PartyReclaim::onStarted);
        gameBus.addListener(PartyReclaim::onTick);
        gameBus.addListener(PartyReclaim::onLogin);
    }

    private static void onAboutToStart(ServerAboutToStartEvent e) {
        purged = new HashSet<>();
        try {
            LaneReclaim.Mode mode = PartyConfig.laneCleanup();
            if (mode == LaneReclaim.Mode.OFF) return;
            var root = e.getServer().getWorldPath(LevelResource.ROOT);
            Set<Integer> retired = LanePurge.readRetired(root);
            if (retired.isEmpty()) return;
            purged = new HashSet<>(LanePurge.run(root, mode, retired));
            LOG.info("[anime_multiverse] Dungeon lanes {} were {} before the dungeon loaded (party.laneCleanup = {}).", purged, mode == LaneReclaim.Mode.DELETE ? "deleted" : "archived", mode);
        } catch (RuntimeException ex) {
            LOG.warn("[anime_multiverse] The freed dungeon lanes could not be cleaned up: {}", ex.toString());
        }
    }

    private static void onStarted(ServerStartedEvent e) {
        try {
            MinecraftServer server = e.getServer();
            MegaParties book = MegaParties.get(server);
            if (!purged.isEmpty()) book.clearRetired(purged);
            purged = new HashSet<>();
            if (PartyConfig.laneCleanup() == LaneReclaim.Mode.OFF || !PartyService.instanced()) return;
            long now = server.overworld().getGameTime();
            Set<Integer> held = new HashSet<>();
            for (PartyIndex.Entry entry : book.index().entries()) held.add(entry.index());
            for (int index : LaneReclaim.unstamped(book.used(), held)) book.touch(index, now, 0L);
            long idle = LaneReclaim.idleTicks(PartyConfig.get(PartyConfig.LANE_IDLE_HOURS));
            List<Integer> due = new ArrayList<>(LaneReclaim.idle(book.used(), held, now, idle));
            for (int index : due) PartyService.retire(server, index);
        } catch (RuntimeException ex) {
            LOG.warn("[anime_multiverse] The idle dungeon lanes could not be checked: {}", ex.toString());
        }
    }

    private static void onTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        if (server.getTickCount() % TOUCH_EVERY != 0) return;
        PartyService.touchOnline(server);
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.getServer() == null) return;
        p.getServer().execute(() -> {
            try {
                if (!p.isAlive() || !MegaDungeon.in(p.level()) || !PartyService.instanced()) return;
                Integer lane = PartyService.assigned(p);
                if (lane == null || lane <= 0 || p.isSpectator() || p.hasPermissions(2)) return;
                if (MegaDungeon.sectorAt(p.level(), p.blockPosition()) != null) return;
                if (MegaData.get(p.getServer()).sectorCount(lane) > 0) return;
                LabyrinthTravel.leaveAny(p);
            } catch (RuntimeException ex) {
                LOG.warn("[anime_multiverse] A player of a freed dungeon lane could not be moved out: {}", ex.toString());
            }
        });
    }
}
