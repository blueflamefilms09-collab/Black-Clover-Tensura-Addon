package com.newuniverse.nusmp.labyrinth.mega;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Why: since 1.55.0 the next sector is not built the moment a Gatekeeper opens ({@code mega.expandAfterVault}): it waits until the Crucible behind the
 * Guardian is resolved, because the relic taken there decides the next sector's theme. A player who skips the vault must not be stranded at a sealed
 * tunnel, so this sweep (once a second, only when a sector is waiting) starts the pending expansion as soon as a living player stands within
 * {@link #REACH} blocks of the rubble seal of the Way Onward. It reads the sector's own saved landmark ({@code Sector.expansionPos}); nothing is scanned
 * in the world. Server thread only; cheap: it returns at once while no sector is pending.
 */
public final class ExpansionTrigger {
    private ExpansionTrigger() {}

    /** How close (blocks) a player has to come to the seal for the dungeon to start growing. */
    public static final double REACH = 14.0;
    private static final int EVERY = 20;

    public static void onServerTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        if (server.getTickCount() % EVERY != 0) return;
        ServerLevel level = MegaDungeon.level(server);
        if (level == null || level.players().isEmpty()) return;
        for (Sector s : MegaData.get(server).sectors()) {
            if (!s.expansionPending || s.expansionPos == null) continue;
            BlockPos seal = s.expansionPos;
            for (ServerPlayer p : level.players()) {
                if (p.isSpectator() || !p.isAlive()) continue;
                if (p.distanceToSqr(seal.getX() + 0.5, seal.getY() + 0.5, seal.getZ() + 0.5) <= REACH * REACH) {
                    MegaDungeon.requestPending(level, s);
                    break;
                }
            }
        }
    }
}
