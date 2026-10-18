package com.newuniverse.nusmp.elite.nations.world;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.elite.nations.core.GuardRules;
import com.newuniverse.nusmp.elite.nations.core.Nation;
import com.newuniverse.nusmp.elite.nations.net.NationNet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Why: the TICK HANDLER of the Nations system ("a tick handler with a throttled server loop"). One listener on the server tick that returns at once except every {@code tickInterval} ticks, and then runs each
 * periodic job only when its own interval is due: war phases and alliance expiry, the siege check, the held-area score, board refills and bounty expiry, interest, reputation decay, guard spawning, the member
 * attribute perks and the screen sync flush. Each job is wrapped, so one failing job never stops the others or the server. With the master switch off it does nothing (and clears the perk modifiers once).
 */
public final class NationTicker {
    private NationTicker() {}

    private static long lastSiege, lastArea, lastPerks, lastGuards;
    private static final Set<UUID> PERKED = new HashSet<>();
    private static boolean wasEnabled;

    public static void reset() {
        lastSiege = lastArea = lastPerks = lastGuards = 0L;
        PERKED.clear();
        wasEnabled = false;
    }

    public static void forget(UUID id) { PERKED.remove(id); }

    public static void onServerTick(ServerTickEvent.Post e) {
        MinecraftServer s = e.getServer();
        if (s.overworld() == null) return;
        long now = s.overworld().getGameTime();
        boolean enabled = NationsConfig.enabled();
        if (!enabled) {
            if (wasEnabled) { clearAllPerks(s); wasEnabled = false; }
            return;
        }
        wasEnabled = true;
        if (now % NationsConfig.get(NationsConfig.TICK_INTERVAL) != 0) return;
        run("wars", () -> { if (NationsConfig.warEnabled()) { WarService.tick(s, now); WarService.expire(s, now); } });
        run("siege", () -> {
            if (NationsConfig.warEnabled() && now - lastSiege >= NationsConfig.seconds(NationsConfig.SIEGE_INTERVAL_SECONDS)) { lastSiege = now; WarService.siegeTick(s); }
        });
        run("area", () -> {
            if (now - lastArea >= NationsConfig.seconds(NationsConfig.AREA_INTERVAL_SECONDS)) { lastArea = now; areaTick(s); }
        });
        run("contracts", () -> {
            NationsData d = NationService.data(s);
            if (now - d.lastBoardRefresh >= NationsConfig.minutes(NationsConfig.BOARD_REFRESH_MINUTES) || d.lastBoardRefresh == 0L) { d.lastBoardRefresh = now; d.setDirty(); ContractService.refreshBoards(s, now); }
            ContractService.expireBounties(s, now);
        });
        run("interest", () -> Treasury.runInterest(s, now));
        run("reputation", () -> ReputationService.decay(s, now));
        run("guards", () -> {
            if (now - lastGuards >= 20L) { lastGuards = now; GuardService.tick(s, now); }
        });
        run("perks", () -> {
            if (now - lastPerks >= NationsConfig.seconds(NationsConfig.PERK_REFRESH_SECONDS)) { lastPerks = now; perkTick(s); }
        });
        run("sync", () -> NationNet.flush(s, now));
    }

    private static void run(String job, Runnable r) {
        try { r.run(); } catch (RuntimeException ex) { LogUtils.getLogger().warn("[nusmp] nation job '{}' failed: {}", job, ex.toString()); }
    }

    /** One held-area point for every nation that has a member inside its own capital area. */
    private static void areaTick(MinecraftServer s) {
        NationsData d = NationService.data(s);
        int radius = NationsConfig.get(NationsConfig.CAPITAL_RADIUS), cap = NationsConfig.get(NationsConfig.AREA_SCORE_CAP);
        for (Nation n : d.nations.values()) {
            if (!n.hasCapital || n.areaScore >= cap) continue;
            if (WarService.present(s, n, n, radius) > 0) {
                n.areaScore++;
                NationService.refresh(s, n);
            }
        }
    }

    private static void perkTick(MinecraftServer s) {
        NationsData d = NationService.data(s);
        boolean on = NationsConfig.get(NationsConfig.PERK_ATTRIBUTES);
        int radius = NationsConfig.get(NationsConfig.CAPITAL_RADIUS);
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            if (p instanceof FakePlayer) continue;
            Nation n = d.nationOf(p.getUUID());
            if (!on || n == null) {
                if (PERKED.remove(p.getUUID())) NationPerks.clear(p);
                continue;
            }
            boolean near = n.hasCapital && p.level().dimension().location().toString().equals(n.capitalDim)
                    && GuardRules.inZone(p.blockPosition().getX(), p.blockPosition().getY(), p.blockPosition().getZ(), n.cx, n.cy, n.cz, radius, 64);
            if (near) { NationPerks.apply(p, n.level, true); PERKED.add(p.getUUID()); }
            else if (PERKED.remove(p.getUUID())) NationPerks.clear(p);
        }
    }

    private static void clearAllPerks(MinecraftServer s) {
        for (UUID id : new HashSet<>(PERKED)) {
            ServerPlayer p = s.getPlayerList().getPlayer(id);
            if (p != null) NationPerks.clear(p);
        }
        PERKED.clear();
    }
}
