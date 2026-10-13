package com.newuniverse.nusmp.relics.djinn;

import com.newuniverse.nusmp.mana.ManaLaw;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.server.level.ServerPlayer;

/**
 * Magoi, the Djinn's food, is the player's Tensura magicule pool (MP; Aura, AP, is the second pool it bridges to; the Black Clover mana of this
 * mod is the same pool: see mana.ManaLaw). No new meter exists. Every amount here is a share of the maximum magicules. Creative players pay nothing; a player with no Tensura record is treated as having no pool and
 * pays nothing either, the way the other systems of the mod do.
 */
final class Magoi {
    private Magoi() {}

    /** The bar as a share of its maximum (1.0 for creative players and players without a pool). */
    static double fraction(ServerPlayer p) {
        if (p.isCreative()) return 1.0;
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex == null) return 1.0;
        double max = EnergyHelper.getMaxMagicule(p);
        return max <= 0 ? 0.0 : Math.min(1.0, ex.getMagicule() / max);
    }

    /**
     * Spends {@code share} of the maximum magicules the way the Kotodama words do (magicules first, then Aura for what they cannot cover), if the
     * magicules stay above {@code floor} (a share of the maximum) or the Aura makes up the rest. False, and nothing spent, otherwise. A pool run
     * nearly dry brings Black Clover's mana strain ({@link ManaLaw#exhaust}: weakness, slowness, and below half a percent a blackout).
     */
    static boolean pay(ServerPlayer p, double share, double floor) {
        if (p.isCreative() || share <= 0) return true;
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex == null) return true;
        double max = EnergyHelper.getMaxMagicule(p), cost = max * share, keep = max * floor;
        double mp = ex.getMagicule(), ap = ex.getAura();
        double fromMp = Math.min(cost, Math.max(0.0, mp - keep));
        double rest = cost - fromMp;
        if (rest > ap) return false;
        ex.setMagicule(mp - fromMp);
        if (rest > 0) ex.setAura(Math.max(0.0, ap - rest));
        ex.markDirty();
        if (ex.getMagicule() < max * 0.05) ManaLaw.exhaust(p);
        return true;
    }

    /** Gives back a share of the maximum (an Extreme Magic that was cancelled before it struck). */
    static void refund(ServerPlayer p, double share) {
        if (p.isCreative() || share <= 0) return;
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex == null) return;
        double max = EnergyHelper.getMaxMagicule(p);
        ex.setMagicule(Math.min(max, ex.getMagicule() + max * share));
        ex.markDirty();
    }

    /** Keeps the pools inside their maximums (after the Maso's raised maximum is taken off). */
    static void clamp(ServerPlayer p) {
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex == null) return;
        ex.setMagicule(Math.min(ex.getMagicule(), EnergyHelper.getMaxMagicule(p)));
        ex.setAura(Math.min(ex.getAura(), EnergyHelper.getMaxAura(p)));
        ex.markDirty();
    }

    /** Current Magoi and maximum, for the ritual's share arithmetic: {current, max}. */
    static double[] pool(ServerPlayer p) {
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex == null) return new double[]{0, 0};
        return new double[]{ex.getMagicule(), EnergyHelper.getMaxMagicule(p)};
    }

    /** Current Aura and maximum: {current, max}. */
    static double[] aura(ServerPlayer p) {
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex == null) return new double[]{0, 0};
        return new double[]{ex.getAura(), EnergyHelper.getMaxAura(p)};
    }

    /** Takes {@code magicule} and {@code aura} points out of the pools (the ritual's EP price, already computed against the floor). */
    static void take(ServerPlayer p, double magicule, double aura) {
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex == null) return;
        ex.setMagicule(Math.max(0.0, ex.getMagicule() - magicule));
        ex.setAura(Math.max(0.0, ex.getAura() - aura));
        ex.markDirty();
    }
}
