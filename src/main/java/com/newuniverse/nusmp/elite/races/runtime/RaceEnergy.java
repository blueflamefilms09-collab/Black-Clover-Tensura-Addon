package com.newuniverse.nusmp.elite.races.runtime;

import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: a Saiyan's ki is Tensura's AURA (the pool that physical, body-based skills spend, as magicule is the pool of magic), so the states drain it and read it. The existing
 * {@code avatar.TensuraBridge} has no aura drain, and every Tensura call here can fail for an entity Tensura does not track, so these few reads and writes sit in one defensive
 * place: a failure answers "full" (never blocks a transformation) and writes nothing.
 */
public final class RaceEnergy {
    private RaceEnergy() {}

    /** Current aura over maximum aura, 0..1; 1 when unknown. */
    public static double auraFraction(LivingEntity e) {
        try {
            var ex = TensuraStorages.getExistenceFrom(e);
            double max = EnergyHelper.getMaxAura(e);
            if (ex == null || max <= 0) return 1.0;
            return Math.max(0.0, Math.min(1.0, ex.getAura() / max));
        } catch (Throwable t) {
            return 1.0;
        }
    }

    /** Takes a share (0..1) of the MAXIMUM aura from the current aura, never below zero. */
    public static void drainAura(LivingEntity e, double shareOfMax) {
        if (shareOfMax <= 0) return;
        try {
            var ex = TensuraStorages.getExistenceFrom(e);
            double max = EnergyHelper.getMaxAura(e);
            if (ex == null || max <= 0) return;
            ex.setAura(Math.max(0.0, ex.getAura() - max * shareOfMax));
            ex.markDirty();
        } catch (Throwable ignored) {
            // an entity Tensura does not track: nothing to drain
        }
    }

    /** Sets the current aura to a share of the maximum (self-tests). */
    public static void setAuraFraction(LivingEntity e, double fraction) {
        try {
            var ex = TensuraStorages.getExistenceFrom(e);
            double max = EnergyHelper.getMaxAura(e);
            if (ex == null || max <= 0) return;
            ex.setAura(max * Math.max(0.0, Math.min(1.0, fraction)));
            ex.markDirty();
        } catch (Throwable ignored) {
            // see above
        }
    }
}
