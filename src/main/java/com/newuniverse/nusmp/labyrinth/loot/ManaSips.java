package com.newuniverse.nusmp.labyrinth.loot;

import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.server.level.ServerPlayer;

/**
 * Why: an elixir has to give back magicules or aura, and the mod already reads and writes those pools through Tensura's own storage (the way {@code EnergyBridge} spends
 * and drains them), so the elixirs go through the same door: they add a share of the MAXIMUM, never past it, mark the storage dirty so the client sees the new number, and
 * do nothing at all for a player Tensura has no data for. It never touches the food bar. Server side only.
 */
final class ManaSips {
    private ManaSips() {}

    /** Restores {@code manaPercent} of the maximum magicules and {@code auraPercent} of the maximum aura; returns true if anything was restored. */
    static boolean restore(ServerPlayer p, int manaPercent, int auraPercent) {
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex == null) return false;
        boolean changed = false;
        if (manaPercent > 0) {
            double max = EnergyHelper.getMaxMagicule(p);
            double now = ex.getMagicule();
            double to = Math.min(max, now + max * manaPercent / 100.0);
            if (to > now) {
                ex.setMagicule(to);
                changed = true;
            }
        }
        if (auraPercent > 0) {
            double max = EnergyHelper.getMaxAura(p);
            double now = ex.getAura();
            double to = Math.min(max, now + max * auraPercent / 100.0);
            if (to > now) {
                ex.setAura(to);
                changed = true;
            }
        }
        if (changed) ex.markDirty();
        return changed;
    }
}
