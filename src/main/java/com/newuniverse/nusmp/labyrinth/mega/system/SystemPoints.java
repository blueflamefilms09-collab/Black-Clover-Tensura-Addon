package com.newuniverse.nusmp.labyrinth.mega.system;

import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Why: this is the server's door for "the player picks where each stat point goes". A request from the stat screen arrives here ({@link SystemPointsNet}), and
 * the rule is the one the whole feature rests on: the client is never trusted. The stat index is range-checked, the amount is bounded, the System must be
 * switched on, and the move itself is done by the pure {@link PointRules} (which can only shuffle points between the free pool and a stat, within the cap), so a
 * forged packet gets at most what a legal click would. A move that changed something is made real at once on the same path the admin command uses
 * ({@link SystemStats#apply}: the attribute modifiers, transient and gated by the dimension, are brought up to date) and the new pool and stats are synced
 * ({@link SystemSync#request}, which the throttle batches). This class also builds and sends {@link SystemPointsNet.PointsSync}, but only when something in it
 * differs from what that player was last told (a signature per player), so a busy fight does not resend the effect table twenty times a minute. Server thread only.
 */
public final class SystemPoints {
    private SystemPoints() {}

    /** What each player was last told (a hash of the packet); cleared on logout and server stop, and whenever a full resend is wanted. */
    private static final Map<UUID, Integer> SENT = new HashMap<>();

    // ================================================================ the player's requests
    /**
     * Moves {@code amount} points between the free pool and the stat with this index (negative = back to the pool, only when {@code system.allowRespec}).
     * Anything the rules do not allow moves nothing. Returns the signed number of points that went into the stat (0 = nothing happened).
     */
    public static int allocate(ServerPlayer p, int statIndex, int amount) {
        if (p == null || !SystemConfig.enabled()) return 0;
        Stat[] all = Stat.values();
        if (statIndex < 0 || statIndex >= all.length) return refuse(p);
        if (amount == 0 || amount > PointRules.MAX_AMOUNT || amount < -PointRules.MAX_AMOUNT) return refuse(p);
        SystemState s = SystemQuests.state(p);
        int moved = PointRules.allocate(s, all[statIndex], amount, SystemConfig.get(SystemConfig.MAX_STAT_POINTS), SystemConfig.allowRespec());
        if (moved == 0) return refuse(p);
        SystemStats.apply(p, s);
        SystemSync.request(p);
        return moved;
    }

    /** Moves every placed point back into the free pool, when {@code system.allowRespec} is on (else a line on the action bar says it is off). Returns the points moved. */
    public static int reset(ServerPlayer p) {
        if (p == null || !SystemConfig.enabled()) return 0;
        if (!SystemConfig.allowRespec()) {
            p.displayClientMessage(SystemMsg.text(PointsText.MSG_NO_RESPEC), true);
            return refuse(p);
        }
        SystemState s = SystemQuests.state(p);
        int moved = PointRules.reset(s, true);
        if (moved <= 0) return refuse(p);
        SystemStats.apply(p, s);
        SystemSync.request(p);
        p.displayClientMessage(SystemMsg.text(PointsText.MSG_RESET_DONE, moved), true);
        return moved;
    }

    /** A request that moved nothing: the player's screen may be showing stale numbers, so it is told the truth again at the next sync slot. */
    private static int refuse(ServerPlayer p) {
        forgetSignature(p.getUUID());
        SystemSync.request(p);
        return 0;
    }

    // ================================================================ the free pool by admin or code
    /** Adds (or, negative, takes) points of the free pool, syncs, and returns the change made. */
    public static int changeFree(ServerPlayer p, int n) {
        SystemState s = SystemQuests.state(p);
        int changed = s.addFree(n);
        SystemSync.request(p);
        return changed;
    }

    // ================================================================ telling the client
    /** The picture of the stat screen for this player right now. Pure reading: sends nothing. */
    public static SystemPointsNet.PointsSync build(ServerPlayer p) {
        SystemState s = SystemQuests.state(p);
        List<SystemPointsNet.Effect> effects = new ArrayList<>();
        StatTable table = SystemData.stats();
        for (Stat stat : Stat.values()) {
            StatTable.Def def = table.def(stat);
            if (def == null) continue;
            for (StatTable.Effect e : def.effects()) {
                if (effects.size() >= SystemPointsNet.MAX_EFFECTS) break;
                effects.add(new SystemPointsNet.Effect(stat.ordinal(), e.attribute(), e.op().ordinal(), e.perPoint(), e.cap()));
            }
        }
        return new SystemPointsNet.PointsSync(s.free(), SystemConfig.get(SystemConfig.MAX_STAT_POINTS), SystemConfig.allowRespec(), SystemConfig.pointMode().ordinal(), effects);
    }

    /** Sends the stat screen's picture when it differs from what this player was last told (or was never told). Called by the sync tick. */
    static void syncIfChanged(ServerPlayer p) {
        SystemPointsNet.PointsSync packet = build(p);
        int signature = Objects.hash(packet.free(), packet.cap(), packet.allowRespec(), packet.mode(), packet.effects());
        Integer last = SENT.get(p.getUUID());
        if (last != null && last == signature) return;
        SENT.put(p.getUUID(), signature);
        SystemSync.send(p, packet);
    }

    /** The next sync slot resends the whole picture (login, respawn, dimension change, a refused request). */
    static void forgetSignature(UUID player) { SENT.remove(player); }

    /** The player left. */
    static void forget(UUID player) { SENT.remove(player); }

    /** Server stop. */
    static void clear() { SENT.clear(); }
}
