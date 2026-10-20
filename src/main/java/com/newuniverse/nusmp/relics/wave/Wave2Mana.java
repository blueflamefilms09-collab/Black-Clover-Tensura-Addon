package com.newuniverse.nusmp.relics.wave;

import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.server.level.ServerPlayer;

/**
 * Wave 2's reads and writes of Tensura's magicules (the same storage calls the grimoire spells and the relic engine use). A player without
 * Tensura data simply has none: nothing is given and nothing can be taken.
 */
final class Wave2Mana {
    private Wave2Mana() {}

    /** Gives back a fraction of the player's maximum magicules (never above the maximum). */
    static void give(ServerPlayer p, double fraction) {
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex == null || fraction <= 0) return;
        double max = EnergyHelper.getMaxMagicule(p);
        ex.setMagicule(Math.min(max, ex.getMagicule() + max * fraction));
        ex.markDirty();
    }

    /** True when the player has at least this fraction of their maximum magicules (nothing is taken). */
    static boolean has(ServerPlayer p, double fraction) {
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex == null) return false;
        double max = EnergyHelper.getMaxMagicule(p);
        return max > 0 && ex.getMagicule() >= max * Math.max(0.0, fraction);
    }

    /** Takes a fraction of the player's maximum magicules if they have that much; false (and nothing taken) otherwise. */
    static boolean take(ServerPlayer p, double fraction) {
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex == null || fraction <= 0) return false;
        double max = EnergyHelper.getMaxMagicule(p);
        if (max <= 0 || ex.getMagicule() < max * fraction) return false;
        ex.setMagicule(ex.getMagicule() - max * fraction);
        ex.markDirty();
        return true;
    }
}
