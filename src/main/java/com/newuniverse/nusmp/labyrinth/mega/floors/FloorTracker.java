package com.newuniverse.nusmp.labyrinth.mega.floors;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.Room;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.quests.NusmpEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: a floor is something a player WALKS onto, and the Directional Lock, the System overlay's floor quests, the Rukh flow and the achievements all need to
 * hear the moment it happens, from one place. So this is the only class that watches which room each player stands in: every {@code trackEveryTicks}
 * (10) ticks it looks up the floor under every living, non-spectator player in the Mega-Dungeon and, when it differs from the one remembered, announces it
 * through the contract's {@code MegaDungeon.fireFloorEntered} (and {@code fireFloorCleared} for the floor that was left behind for a deeper one of the
 * same road, once per player and floor), updates the player's {@link PlayerDungeonAlignment} through the pure {@link AlignmentRules}, and tells the
 * quest system ({@code mega.floor} the first time a depth is reached, {@code mega.aligned} when the lock closes). The same pass runs the hard guard
 * ({@link HardLock}) once a second. The remembered floor lives in a plain map of UUID to floor, never saved (the floor under a player is a fact of where they
 * stand, rebuilt by the first look after a login) and dropped on logout, on a dimension change, on respawn and when the server stops.
 *
 * <p>The floor is read from the room under the player's feet and NOT from {@code MegaDungeon.floorAt}: that method answers the sector's first floor for a
 * position that is in no room (a door cap alcove, the air above the rooms, a gap between sectors), which would make a player walking through a cap flip down
 * to floor +1 and back and fire two false floor changes. A position in no room keeps the floor the player was on. Going back to Floor 0 is leaving the
 * dimension: it forgets the floor and fires nothing, so a retreat never changes the lock. A locked player who stands on the OTHER road and is about to be sent
 * out by the hard guard ({@link HardLock#shouldCastOut}) is not tracked at all, so a teleport that the lock will undo within a second cannot earn floor events or
 * quest credit on a road that is closed to them. {@link #check} does one player, which is what the tick calls and what the self-tests call directly (a
 * stand-in player is not in the level's player list).
 */
public final class FloorTracker {
    private FloorTracker() {}

    private static final Logger LOG = LogUtils.getLogger();
    /** The hard guard runs this often, in ticks. */
    private static final int GUARD_EVERY = 20;
    private static final Map<UUID, Integer> CURRENT = new HashMap<>();
    private static boolean warned;

    public static void register(IEventBus bus) {
        bus.addListener(FloorTracker::onServerTick);
        bus.addListener(FloorTracker::onLogin);
        bus.addListener(FloorTracker::onLogout);
        bus.addListener(FloorTracker::onRespawn);
        bus.addListener(FloorTracker::onChangedDimension);
        bus.addListener(FloorTracker::onServerStarted);
        bus.addListener(FloorTracker::onServerStopped);
    }

    // ---------------------------------------------------------------- questions
    /** The floor the tracker last saw this player on (0 when they are not in the dungeon or have not been looked at yet). */
    public static int currentFloor(UUID id) { return CURRENT.getOrDefault(id, 0); }

    public static int currentFloor(ServerPlayer p) { return currentFloor(p.getUUID()); }

    /**
     * The signed floor of the room at a position of the Mega-Dungeon, or 0 when the position is in no sector or in no room of one. A room that was never
     * numbered (a world saved by 1.54.0) is numbered now; if that is impossible the sector's first floor stands in.
     */
    public static int floorAt(ServerLevel level, BlockPos pos) {
        Sector s = MegaDungeon.sectorAt(level, pos);
        if (s == null) return 0;
        Room r = s.roomAt(pos);
        if (r == null) return 0;
        if (r.floor == 0) FloorAssigner.ensureBanded(level.getServer(), s);
        return r.floor != 0 ? r.floor : s.firstFloor();
    }

    // ---------------------------------------------------------------- one player
    /** Looks at one player and announces a change of floor. Server thread only. A stand-in player may be passed (it is not in the level's player list). */
    public static void check(ServerPlayer p) {
        if (p == null || p.isSpectator() || !p.isAlive()) return;
        ServerLevel level = p.serverLevel();
        if (!MegaDungeon.in(level)) return;
        if (HardLock.shouldCastOut(p)) return;                                    // locked to the other road and about to be sent out: they are not really on this one, so no floor event and no quest credit
        int floor = floorAt(level, p.blockPosition());
        if (floor == 0) return;                                                   // in no room: keep the floor they were on
        int previous = CURRENT.getOrDefault(p.getUUID(), 0);
        if (floor == previous) return;
        CURRENT.put(p.getUUID(), floor);
        onFloorChanged(p, floor, previous);
    }

    /** The order of events the brief asks for, for one change of floor. */
    private static void onFloorChanged(ServerPlayer p, int floor, int previous) {
        PlayerDungeonAlignment a = AlignmentAttachments.get(p);
        boolean newDeepest = AlignmentRules.isNewDeepest(a, floor);              // both questions are asked BEFORE the state learns of the move
        boolean clears = AlignmentRules.clearsPrevious(a, previous, floor);
        MegaDungeon.fireFloorEntered(p, floor, previous);
        if (newDeepest) com.newuniverse.nusmp.audio.voice.Voice.say(p, com.newuniverse.nusmp.labyrinth.mega.FloorMap.isBossFloor(floor) ? "system_boss_ahead" : "system_floor_enter");   // 1.110.0: spoken line, the first time a depth is reached (a boss floor warns instead)
        boolean changed = AlignmentRules.onFloorEntered(a, floor);
        boolean locked = false;
        if (clears) {
            MegaDungeon.fireFloorCleared(p, previous);
            if (!newDeepest) com.newuniverse.nusmp.audio.voice.Voice.say(p, "system_floor_clear");            // 1.110.0: spoken line (when the move is also a new depth, that depth's line plays instead)
            if (FloorsConfig.get(FloorsConfig.LOCK_ENABLED)) {
                locked = AlignmentRules.onFloorCleared(a, previous, FloorsConfig.get(FloorsConfig.LOCK_AT_FLOOR), p.level().getGameTime());
            }
        }
        if (newDeepest) NusmpEvents.fire(p, "mega.floor", FloorMap.label(floor));
        if (locked) announceLock(p, previous, a.chosen());
        if (changed || locked) AlignmentNet.send(p);
    }

    private static void announceLock(ServerPlayer p, int cleared, Polarity road) {
        if (FloorsText.real(p)) {
            p.sendSystemMessage(FloorsText.text("locked", "Floor %s is behind you. The %s is yours now, and the other road is sealed to you. The way back to Floor 0 stays open.", FloorsText.floor(cleared), FloorsText.road(road))
                    .withStyle(ChatFormatting.DARK_PURPLE));
            p.playNotifySound(SoundEvents.IRON_DOOR_CLOSE, SoundSource.PLAYERS, 0.8f, 0.5f);
            if (LockPolicy.bypasses(p)) p.sendSystemMessage(FloorsText.text("locked.op", "You are an operator, so the lock does not bind you.").withStyle(ChatFormatting.GRAY));
        }
        NusmpEvents.fire(p, "mega.aligned", road == null ? "" : road.id());
    }

    /** Forgets the floor of one player (a self-test's clean-up; the events do the same for real players). */
    public static void forget(UUID id) { CURRENT.remove(id); }

    // ---------------------------------------------------------------- the clock
    private static final com.newuniverse.nusmp.perf.PerfProbe.Section PROBE_S_MEGA_FLOOR_TRACKER = com.newuniverse.nusmp.perf.PerfProbe.section("s.mega.floor_tracker");   // 1.99.0: performance probe (off by default: one flag read)

    private static void onServerTick(ServerTickEvent.Post e) {
        long t0 = com.newuniverse.nusmp.perf.PerfProbe.begin();
        try {
            onServerTick0(e);
        } finally {
            com.newuniverse.nusmp.perf.PerfProbe.end(PROBE_S_MEGA_FLOOR_TRACKER, t0);
        }
    }

    private static void onServerTick0(ServerTickEvent.Post e) {
        try {
            tick(e.getServer());
        } catch (RuntimeException ex) {
            if (!warned) {
                warned = true;
                LOG.warn("[nusmp] The floor tracker failed (further failures are not logged): {}", ex.toString());
            }
        }
    }

    private static void tick(MinecraftServer server) {
        int t = server.getTickCount();
        boolean track = t % Math.max(1, FloorsConfig.get(FloorsConfig.TRACK_EVERY_TICKS)) == 0;
        boolean guard = t % GUARD_EVERY == 0;
        if (!track && !guard) return;
        if (track) sweepLeavers(server);
        ServerLevel mega = MegaDungeon.level(server);
        if (mega == null || mega.players().isEmpty()) return;
        for (ServerPlayer p : new ArrayList<>(mega.players())) {                // a copy: the guard may take a player out of the dimension while we walk the list
            try {
                if (track) check(p);
                if (guard) HardLock.check(p);
            } catch (RuntimeException ex) {
                if (!warned) {
                    warned = true;
                    LOG.warn("[nusmp] The floor tracker failed for {} (further failures are not logged): {}", p.getGameProfile().getName(), ex.toString());
                }
            }
        }
    }

    /** Drops the remembered floor of anyone who is gone or no longer in the dungeon, whatever way they left (the events cover the usual ways; this covers the rest). */
    private static void sweepLeavers(MinecraftServer server) {
        if (CURRENT.isEmpty()) return;
        CURRENT.keySet().removeIf(id -> {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            return p == null || !MegaDungeon.in(p.level());
        });
    }

    // ---------------------------------------------------------------- events
    private static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        CURRENT.remove(p.getUUID());
        AlignmentNet.send(p);
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        UUID id = e.getEntity().getUUID();
        CURRENT.remove(id);
        LockPolicy.forget(id);
        HardLock.forget(id);
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        CURRENT.remove(p.getUUID());
        AlignmentNet.send(p);
    }

    private static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        CURRENT.remove(p.getUUID());                                              // leaving the dungeon (for Floor 0 or anywhere) forgets the floor and fires nothing
        AlignmentNet.send(p);
    }

    /** Numbers the sectors of an old world (saved before floors existed) once, so the first player to arrive reads real floors. Never throws. */
    private static void onServerStarted(ServerStartedEvent e) {
        try {
            int n = FloorAssigner.numberOldSectors(e.getServer());
            if (n > 0) LOG.info("[nusmp] Numbered the floors of {} sector(s) that were saved before floors existed.", n);
        } catch (RuntimeException ex) {
            LOG.warn("[nusmp] The sectors saved before floors existed could not be numbered: {}", ex.toString());
        }
    }

    private static void onServerStopped(ServerStoppedEvent e) {
        CURRENT.clear();
        LockPolicy.clear();
        HardLock.clear();
        warned = false;
    }
}
