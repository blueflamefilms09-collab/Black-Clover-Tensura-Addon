package com.newuniverse.nusmp.labyrinth.mega.system;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the client draws the window from what the server tells it, so this is the one place that turns a player into a {@link SystemNet.Sync} and decides when
 * to send it. Every change (a kill, a room, a stat point, a floor) only marks the player dirty; one tick-handler sends at most one packet per player per ten
 * ticks ({@link SyncThrottle}, the brief's number), carrying everything that changed meanwhile, so a busy fight is a trickle and not a flood. The first change
 * after a quiet spell goes out on the very next tick. A packet is built from the player's CURRENT floor (asked of {@link MegaDungeon#floorAt}, with the last
 * known floor standing in while the player crosses the gap between two sectors, so the window does not flicker in a corridor) and from the saved
 * {@link SystemState}. A player without a connection (a fake player of the self-tests) is skipped, and building a packet never sends it, so the self-test can
 * check the packet itself. The throttle table is per server run and cleared when the server stops.
 */
public final class SystemSync {
    private SystemSync() {}

    private static final Logger LOG = LogUtils.getLogger();
    /** Ticks between two syncs of one player at most. */
    public static final int GAP_TICKS = 10;
    private static final Map<UUID, SyncThrottle> THROTTLES = new HashMap<>();

    // ---------------------------------------------------------------- asking
    /** Something about this player changed: a sync is owed (it goes out on the next tick that is at least {@value #GAP_TICKS} ticks after the last). */
    public static void request(ServerPlayer p) {
        if (p == null || !hasConnection(p)) return;
        THROTTLES.computeIfAbsent(p.getUUID(), k -> new SyncThrottle(GAP_TICKS)).markDirty();
    }

    /** The player's window needs rebuilding from scratch (login, respawn, a new dimension): marks them dirty and forgets the throttle's wait so it goes out at once. */
    public static void requestNow(ServerPlayer p) {
        if (p == null || !hasConnection(p)) return;
        THROTTLES.put(p.getUUID(), new SyncThrottle(GAP_TICKS));
        THROTTLES.get(p.getUUID()).markDirty();
        SystemPoints.forgetSignature(p.getUUID());                      // the stat screen's picture goes out again with it
    }

    /** A real player: a connection to send to, and not a fake player of a test or another mod. */
    public static boolean hasConnection(ServerPlayer p) {
        return p != null && !(p instanceof FakePlayer) && p.connection != null;
    }

    // ---------------------------------------------------------------- sending
    /** Server tick: sends the syncs that are due. Cheap when nobody is dirty. */
    public static void tick(MinecraftServer server) {
        if (THROTTLES.isEmpty()) return;
        long now = server.getTickCount();
        Iterator<Map.Entry<UUID, SyncThrottle>> it = THROTTLES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, SyncThrottle> e = it.next();
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p == null) {
                it.remove();
                continue;
            }
            if (!e.getValue().due(now)) continue;
            e.getValue().sent(now);
            send(p, build(p));
            SystemPoints.syncIfChanged(p);                              // the free pool and the effect table, only when they differ from what the player was told
        }
    }

    public static void forget(UUID player) {
        THROTTLES.remove(player);
        SystemPoints.forget(player);
    }

    public static void clear() {
        THROTTLES.clear();
        SystemPoints.clear();
    }

    /** Sends any payload to a real player; never throws (a player who is just leaving has no connection any more). */
    public static void send(ServerPlayer p, CustomPacketPayload payload) {
        if (!hasConnection(p) || payload == null) return;
        try {
            PacketDistributor.sendToPlayer(p, payload);
        } catch (RuntimeException e) {
            LOG.debug("[nusmp] System packet to {} not sent: {}", p.getGameProfile().getName(), e.toString());
        }
    }

    /** Sends one pop-up line to a real player (a lang key and its arguments; see {@link SystemNet.Toast}). */
    public static void toast(ServerPlayer p, ToastKind kind, String key, String... args) {
        send(p, SystemNet.Toast.of(kind, key, args));
    }

    // ---------------------------------------------------------------- building
    /**
     * The window's state for this player right now. Pure reading: sends nothing. In the Mega-Dungeon it is the floor, the road and the quest; in the Hub
     * (Floor 0, the Sanctuary) it is the status window only, floor 0 with no road, no threat and no quest, so the stat points can be read there (floor quests
     * are for the floors); everywhere else the window is hidden. The wire format is unchanged: the Hub is "floor 0, quest none".
     */
    public static SystemNet.Sync build(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        if (SystemConfig.enabled() && Labyrinth.inSanctuary(level)) return hub(p);
        boolean inMega = MegaDungeon.in(level);
        int floor = inMega ? SystemQuests.currentFloor(level, p) : 0;
        if (inMega && floor == 0) floor = SystemQuests.lastFloor(p);
        Polarity polarity = Polarity.ofFloor(floor);
        boolean active = SystemConfig.enabled() && inMega && floor != 0 && SystemQuests.covers(floor);
        if (!active) return SystemNet.Sync.hidden();
        SystemState st = SystemQuests.state(p);
        QuestProgress q = st.quest();
        int questState = SystemNet.Q_NONE, questFloor = 0, progress = 0, target = 0, timeLeft = -1;
        String kind = "", key = "", fallback = "";
        boolean clock = false;
        if (q != null) {
            questState = switch (q.state()) {
                case ACTIVE -> SystemNet.Q_ACTIVE;
                case DONE -> SystemNet.Q_DONE;
                case FAILED -> SystemNet.Q_FAILED;
            };
            QuestTemplate t = SystemData.quests().get(q.id());
            questFloor = q.floor();
            kind = q.kind().id();
            key = t != null ? t.langKey() : SystemText.PREFIX + "quest." + q.id();
            fallback = t != null ? t.englishTitle(q.target()) : q.kind().defaultTitle(q.target());
            progress = q.progress();
            target = q.target();
            timeLeft = q.timeLeft();
            clock = q.isActive() && q.kind().timed() && floor == q.floor();
        }
        List<Integer> stats = new ArrayList<>(Stat.COUNT);
        for (Stat s : Stat.values()) stats.add(st.points(s));
        return new SystemNet.Sync(true, floor, polarity == null ? "" : polarity.id(), FloorMap.threat(floor), QuestFloors.isBossFloor(floor),
                questState, questFloor, kind, key, fallback, progress, target, timeLeft, clock, stats);
    }

    /** The Hub's picture: the status window with the player's stat points and nothing of a floor (floor 0, no road, threat 0, no quest, no clock). */
    private static SystemNet.Sync hub(ServerPlayer p) {
        SystemState st = SystemQuests.state(p);
        List<Integer> stats = new ArrayList<>(Stat.COUNT);
        for (Stat s : Stat.values()) stats.add(st.points(s));
        return new SystemNet.Sync(true, 0, "", 0, false, SystemNet.Q_NONE, 0, "", "", "", 0, 0, -1, false, stats);
    }
}
