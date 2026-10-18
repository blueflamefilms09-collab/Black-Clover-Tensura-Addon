package com.newuniverse.nusmp.elite.nations.world;

import com.newuniverse.nusmp.elite.nations.core.Nation;
import com.newuniverse.nusmp.elite.nations.event.ReputationChangedEvent;
import com.newuniverse.nusmp.elite.nations.rep.ReputationTier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Why: reputation per player per nation ("reputation tiers ... and effects"): the only place that changes a value, so the event, the tier-change notice and the dirty marks are never skipped, plus the
 * slow decay toward neutral. Effects of a tier (shop price, contract tier cap, guard hostility) are read from {@link ReputationTier} by whoever needs them. The key may be a nation id or any other faction
 * id a later system uses. Server thread only.
 */
public final class ReputationService {
    private ReputationService() {}

    public static int get(MinecraftServer s, UUID player, String nationId) { return NationService.data(s).reputation.get(player, nationId); }

    public static ReputationTier tier(MinecraftServer s, UUID player, String nationId) { return ReputationTier.of(get(s, player, nationId)); }

    /** Adds (or subtracts) reputation. Returns the new value. */
    public static int add(MinecraftServer s, UUID player, String nationId, int delta, String reason) {
        if (s == null || player == null || nationId == null || nationId.isEmpty() || delta == 0) return get(s, player, nationId);
        NationsData d = NationService.data(s);
        int old = d.reputation.get(player, nationId);
        int now = d.reputation.add(player, nationId, delta);
        if (now == old) return now;
        d.setDirty();
        Treasury.post(new ReputationChangedEvent(player, nationId, old, now, reason == null ? "" : reason));
        ReputationTier before = ReputationTier.of(old), after = ReputationTier.of(now);
        if (before != after) {
            ServerPlayer p = s.getPlayerList().getPlayer(player);
            Nation n = d.nations.get(nationId);
            if (p != null) Msg.tell(p, "nusmp.nation.m.rep_tier", n == null ? nationId : n.name, Msg.of("nusmp.nation.tier." + after.id()));
        }
        return now;
    }

    public static void set(MinecraftServer s, UUID player, String nationId, int value) {
        NationsData d = NationService.data(s);
        int old = d.reputation.get(player, nationId);
        int now = d.reputation.set(player, nationId, value);
        if (now != old) {
            d.setDirty();
            Treasury.post(new ReputationChangedEvent(player, nationId, old, now, "set"));
        }
    }

    /** One decay step if the interval has passed. */
    public static void decay(MinecraftServer s, long now) {
        int amount = NationsConfig.get(NationsConfig.REP_DECAY_AMOUNT);
        if (amount <= 0) return;
        NationsData d = NationService.data(s);
        if (now - d.lastRepDecay < NationsConfig.minutes(NationsConfig.REP_DECAY_INTERVAL_MINUTES)) return;
        d.lastRepDecay = now;
        if (d.reputation.decayAll(amount) > 0) d.setDirty();
    }
}
