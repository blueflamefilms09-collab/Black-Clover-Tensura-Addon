package com.newuniverse.nusmp.heroweapon.curse;

import com.newuniverse.nusmp.heroweapon.Sin;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the client draws the screen tint, the corruption bar and the whispers from the curse's numbers, and those numbers live on the server. This class builds the
 * small {@link CurseSyncPayload} from a wielder's state and sends it to that wielder only, and it keeps the last packet sent to each player so that nothing is sent
 * while nothing changed (apart from a repeat every ten seconds, so a client that missed one recovers). The memory is a cache of the last thing sent, kept on the
 * server thread and cleared when a player leaves and when the server stops. A stand-in player of a self-test has no connection and is skipped.
 */
final class CurseSync {
    private CurseSync() {}

    /** Ticks after which an unchanged packet is sent again anyway. */
    private static final long REPEAT_TICKS = 200L;

    private record Sent(CurseSyncPayload payload, long at) {}

    private static final Map<UUID, Sent> LAST = new HashMap<>();

    /** The packet for this state at this time; {@code active} is the sin of the wielder's ACTIVE form when it is a curse form (the weapon out or not), or null. */
    static CurseSyncPayload build(CurseState s, Sin active, long now, CurseTuning t) {
        boolean offer = CurseMachine.offerOpen(s, now, t);
        int overrunLeft = CurseMachine.inOverrun(s, now) ? (int) Math.max(1L, Math.min(Integer.MAX_VALUE, s.overrunUntil - now)) : 0;
        return new CurseSyncPayload(s.despair, s.corruption, offer ? CurseWire.toWire(s.offered()) : 0, CurseWire.toWire(active), overrunLeft,
                offer ? CurseMachine.offerTicksLeft(s, now, t) : 0, CurseWire.seriesMask(s.series));
    }

    /** Sends the packet when it differs from the last one sent (or {@code force}, or the last one is old). */
    static void push(ServerPlayer p, CurseState s, Sin active, long now, boolean force, CurseTuning t) {
        send(p, build(s, active, now, t), now, force);
    }

    /** Tells the client there is no curse any more (a weapon was bound or released, the player respawned). */
    static void reset(ServerPlayer p, long now) { send(p, CurseSyncPayload.none(), now, true); }

    private static void send(ServerPlayer p, CurseSyncPayload payload, long now, boolean force) {
        if (p == null || p instanceof FakePlayer || p.connection == null) return;
        Sent last = LAST.get(p.getUUID());
        if (!force && last != null && last.payload().equals(payload) && now >= last.at() && now - last.at() < REPEAT_TICKS) return;
        PacketDistributor.sendToPlayer(p, payload);
        LAST.put(p.getUUID(), new Sent(payload, now));
    }

    /** Forgets what was sent to a player who left. */
    static void forget(UUID id) { LAST.remove(id); }

    /** Forgets everything (the server stopped). */
    static void clear() { LAST.clear(); }
}
