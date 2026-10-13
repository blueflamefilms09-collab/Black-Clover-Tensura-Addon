package com.newuniverse.nusmp.dungeon.sentient;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.dungeon.DungeonConfig;
import com.newuniverse.nusmp.dungeon.LivingDungeon;
import com.newuniverse.nusmp.dungeon.nexus.collapse.CollapseManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * The Dungeon's rooms (the owner's design: "when a player enters a new room, the walls crack open and birth a strict, capped number of
 * mobs"). A room is a chunk column over a band of 16 blocks of height ({@link RoomKey}). Every half second the Dungeon looks at the room each
 * player stands in; the first time a player walks into a room that nobody has been in, it is rolled once ({@link RoomPlan}: populated or
 * barren, and how many) and written down ({@link RoomSave}); a populated room births its mobs from the walls beside its standing places, 7 to 24
 * blocks from the player and preferably behind them ({@link SpotPick}), one wall birth each ({@link WallBirth}). A room that was populated
 * births again after the config's {@code roomRebirthMinutes} when a player is inside again and none of its mobs is left alive. Bounded: a player
 * draws a birth once in ten seconds at most, and not while 28 (the config's {@code maxBornPerPlayer}) born mobs are alive within 64 blocks.
 */
public final class DungeonRoomManager {
    private DungeonRoomManager() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    /** Ticks between two passes over the players. */
    private static final int PASS = 10;
    private static final double CAP_RADIUS = 64.0, MIN_DIST = 7.0, MAX_DIST = 24.0, SPACING = 3.0;
    /** The crack of a room's births lasts this long, the later ones a few ticks more, so the walls do not all give way at once. */
    private static final int DELAY_BASE = 30, DELAY_SPREAD = 40, DELAY_STAGGER = 4;

    /** What the Dungeon knows about one player's rooms: the room that was dealt with, the one being tried and how often, the time of the last birth, the time not to look again before. */
    private static final class State {
        long handled = Long.MIN_VALUE, trying = Long.MIN_VALUE;
        int tries;
        long lastBirth = Long.MIN_VALUE / 2, nextTry;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();
    private static final Set<String> WARNED = new HashSet<>();

    // ---------------------------------------------------------------- the pass
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        int tick = server.getTickCount();
        if (tick % PASS != 3) return;
        if (!SentientMode.active()) {
            if (!STATES.isEmpty()) STATES.clear();
            return;
        }
        ServerLevel d = server.getLevel(LivingDungeon.DIMENSION);
        if (d == null) return;
        long now = d.getGameTime();
        Set<UUID> present = new HashSet<>();
        for (ServerPlayer p : d.players()) {
            present.add(p.getUUID());
            try {
                visit(d, p, now);
            } catch (RuntimeException | LinkageError failure) {
                warn("looking at the room of a player", failure);
            }
        }
        STATES.keySet().retainAll(present);
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { STATES.remove(e.getEntity().getUUID()); }

    public static void onServerStopping(ServerStoppingEvent e) {
        STATES.clear();
        WARNED.clear();
    }

    /** One look at one player: a new room is rolled, and born into if it is populated. */
    private static void visit(ServerLevel d, ServerPlayer p, long now) {
        if (!Targets.valid(p)) return;
        BlockPos pos = p.blockPosition();
        long room = RoomKey.at(pos.getX(), pos.getY(), pos.getZ());
        State st = STATES.computeIfAbsent(p.getUUID(), u -> new State());
        if (room == st.handled || now < st.nextTry) return;                           // the common case: nothing is asked of the world
        FloorKind kind = FloorLookup.birthKindAt(d, pos);
        RoomGate.Decision go = RoomGate.decide(room, st.handled, now, st.nextTry, kind.hasRoomBirths(), st.lastBirth,
                () -> CollapseManager.suppressesLife(d, new ChunkPos(RoomKey.chunkX(room), RoomKey.chunkZ(room))),
                () -> BornDriver.countNear(d, p.getX(), p.getY(), p.getZ(), CAP_RADIUS) + WallBirth.pendingNear(d, pos, CAP_RADIUS),
                DungeonConfig.get(DungeonConfig.BORN_CAP));
        if (go.verdict() == RoomGate.Verdict.HANDLED) st.handled = room;
        else if (go.verdict() == RoomGate.Verdict.WAIT) st.nextTry = go.nextTry();
        if (go.verdict() != RoomGate.Verdict.ENTER) return;
        if (st.trying != room) {
            st.trying = room;
            st.tries = 0;
        }
        RoomGate.After after = RoomGate.after(enter(d, p, room, kind, now), st.tries, now);
        st.tries = after.tries();
        if (after.handled()) st.handled = room;
        if (after.nextTry() > 0L) st.nextTry = after.nextTry();
        if (after.birthed()) st.lastBirth = now;
    }

    /** The player is in a room that was not dealt with yet: roll it if it is new, birth into it if it is populated (or is due to be again). */
    private static RoomGate.Result enter(ServerLevel d, ServerPlayer p, long room, FloorKind kind, long now) {
        RoomSave save = RoomSave.get(d.getServer());
        RoomBook.Room known = save.lookup(room, now);
        boolean again = false;
        if (known != null) {
            if (known.state != RoomBook.BORN) return RoomGate.Result.DONE;                    // barren: nothing was ever here and nothing will be
            if (!RoomGate.rebirthDue(known.lastBirth, DungeonConfig.get(DungeonConfig.ROOM_REBIRTH_MINUTES), now, () -> BornDriver.countInRoom(d, room))) return RoomGate.Result.DONE;
            again = true;
        }
        Dice dice = d.getRandom()::nextDouble;
        RoomScan.Result scan = RoomScan.scan(d, room, kind, dice);
        if (scan == null) return RoomGate.Result.LATER;                                       // its chunk is not loaded yet
        RoomPlan.Birth plan = RoomPlan.decide(kind, room, d.getSeed(), scan.standable(), DungeonConfig.get(DungeonConfig.ROOM_SCALE));
        if (!plan.populated()) {
            if (!again) save.record(room, RoomBook.BARREN, now);
            return RoomGate.Result.DONE;
        }
        Vec3 look = p.getLookAngle();
        List<SpotPick.Spot> pool = sameVolume(d, scan.walled(), FloorLookup.nexusAt(d, p.blockPosition()) != null);
        List<SpotPick.Spot> picks = SpotPick.pick(pool, p.getX(), p.getY(), p.getZ(), look.x, look.z, plan.count(), MIN_DIST, MAX_DIST, SPACING, dice);
        if (picks.isEmpty()) return RoomGate.Result.NOWHERE;                                  // nowhere to birth from where the player stands; nothing is written down, so another look may find some
        int scheduled = 0;
        for (int i = 0; i < picks.size(); i++) {
            SpotPick.Spot s = picks.get(i);
            int delay = DELAY_BASE + d.getRandom().nextInt(DELAY_SPREAD) + i * DELAY_STAGGER;
            scheduled += WallBirth.birthAt(d, List.of(new BlockPos(s.x(), s.y(), s.z())), delay, factory(kind), marker(room, kind));
        }
        if (scheduled == 0) return RoomGate.Result.LATER;
        save.record(room, RoomBook.BORN, now);
        return RoomGate.Result.BIRTHED;
    }

    /**
     * The spots that lie on the same side of a Nexus wall as the player: a room that overlaps the volume of a Nexus (the Monarch's Vault, the
     * Underworld Gate) never births into it from outside, nor out of it from inside.
     */
    private static List<SpotPick.Spot> sameVolume(ServerLevel d, List<SpotPick.Spot> spots, boolean playerInside) {
        List<SpotPick.Spot> out = new ArrayList<>(spots.size());
        for (SpotPick.Spot s : spots) if ((FloorLookup.nexusAt(d, new BlockPos(s.x(), s.y(), s.z())) != null) == playerInside) out.add(s);
        return out;
    }

    private static WallBirth.Factory factory(FloorKind kind) { return (lvl, spot) -> FloorRoster.create(lvl, kind, lvl.getRandom(), spot); }

    /** What a born mob is marked with: the born tag, the room and the floor it was born on, and a place in the world for good. */
    private static Consumer<Mob> marker(long room, FloorKind kind) {
        return m -> {
            m.addTag(SentientTags.BORN);
            m.getPersistentData().putLong(SentientTags.ROOM, room);
            m.getPersistentData().putInt(SentientTags.FLOOR, kind.ordinal());
            m.setPersistenceRequired();
        };
    }

    // ---------------------------------------------------------------- the admin's way
    /**
     * Births {@code count} mobs from the walls round the player at once, ignoring every cap and the room's roll (the floor's list is the
     * Catacombs' where the floor has none). Returns how many are on their way.
     */
    public static int force(ServerPlayer p, int count) {
        ServerLevel level = p.serverLevel();
        BlockPos pos = p.blockPosition();
        FloorKind kind = FloorKind.at(level, pos);
        if (!kind.hasRoomBirths()) kind = FloorKind.CATACOMBS;
        long room = RoomKey.at(pos.getX(), pos.getY(), pos.getZ());
        return WallBirth.birth(level, pos, Math.max(1, count), 5, 20, DELAY_BASE + 10, factory(kind), marker(room, kind));
    }

    private static void warn(String what, Throwable failure) {
        if (WARNED.add(what + failure.getClass().getName())) LOG.warn("[nusmp] Trouble {} (logged once)", what, failure);
    }

    /** For the admin command: the rooms the Dungeon remembers, the born mobs alive near a point. */
    static List<String> describe(ServerLevel level, ServerPlayer p) {
        RoomSave save = RoomSave.get(level.getServer());
        List<String> out = new ArrayList<>();
        out.add("rooms known: " + save.size() + " (" + save.count(RoomBook.BORN) + " populated, " + save.count(RoomBook.BARREN) + " barren)");
        out.add("born mobs alive within 64 blocks: " + BornDriver.countNear(level, p.getX(), p.getY(), p.getZ(), CAP_RADIUS) + " of " + DungeonConfig.get(DungeonConfig.BORN_CAP)
                + " (" + BornDriver.count() + " looked after in all, " + WallBirth.pendingCount() + " wall births waiting)");
        return out;
    }
}
