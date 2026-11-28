package com.newuniverse.nusmp.devil;

import com.newuniverse.nusmp.devil.dual.DevilOwnership;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Why (5.7.0, owner request): a devil may ask a player for a contract on its own, 2 percent of the time. Once a minute the server walks its online players (at most the player count, no world scan, no chunk
 * load, no entity); a player whose {@code intervalMinutes} are over, who has no contract, no ritual and no open offer, gets one roll of {@code chancePercent}. A win picks one free devil of the roster that is
 * not Supreme (those only come to a proper ritual) and opens the usual 30 second offer ({@link DevilRituals#callOffer}). The time of the last roll lives in the player's persistent data (by player, kept
 * through logout; no static state). {@code devil.calling.enabled = false} stops it. Worst case: one pass over the players a minute, one roll each every 20 minutes, one chat block per win.
 */
public final class DevilCalling {
    private DevilCalling() {}

    static final String LAST = "anime_multiverse_devil_call_at";

    public static void register(IEventBus bus) { bus.addListener(DevilCalling::onTick); }

    private static void onTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        if (server.getTickCount() % DevilCallingRules.SWEEP_TICKS != 0 || !DevilConfig.callingOn() || !DevilConfig.enabled() || !DevilConfig.worlds()) return;
        long now = server.overworld().getGameTime(), every = DevilCallingRules.intervalTicks(DevilConfig.callingMinutes());
        for (ServerPlayer p : server.getPlayerList().getPlayers()) sweep(p, now, every);
    }

    static void sweep(ServerPlayer p, long now, long every) {
        long last = p.getPersistentData().getLong(LAST);
        if (last <= 0) { p.getPersistentData().putLong(LAST, now); return; }                       // the clock of a new player starts now
        if (!DevilCallingRules.due(last, now, every)) {
            if (last > now) p.getPersistentData().putLong(LAST, now);                              // a world that went back in time must not freeze the roll
            return;
        }
        DevilContract c = DevilStore.peek(p);
        if (!DevilCallingRules.eligible(p.isAlive(), p.isSpectator(), c != null && c.hasContract(), c != null && c.ritual != null, c != null && DevilRituals.offerOpen(c, now))) return;
        p.getPersistentData().putLong(LAST, now);
        if (!DevilCallingRules.wins(DevilConfig.callingPercent(), p.getRandom().nextDouble())) return;
        WorldDevil d = pick(p);
        if (d != null) DevilRituals.callOffer(p, d);
    }

    /** A free, non-Supreme devil at random (two passes over the roster, no allocation), or null. */
    static WorldDevil pick(ServerPlayer p) {
        int n = 0;
        for (int i = 0; i < WorldDevil.count(); i++) if (callable(p, WorldDevil.at(i))) n++;
        int k = DevilCallingRules.pick(n, p.getRandom().nextDouble());
        if (k < 0) return null;
        for (int i = 0; i < WorldDevil.count(); i++) if (callable(p, WorldDevil.at(i)) && k-- == 0) return WorldDevil.at(i);
        return null;
    }

    private static boolean callable(ServerPlayer p, WorldDevil d) {
        return d.tier() != DevilTier.SUPREME && DevilOwnership.verdict(p.getServer(), d.id(), p.getUUID()).mayTake;
    }
}
