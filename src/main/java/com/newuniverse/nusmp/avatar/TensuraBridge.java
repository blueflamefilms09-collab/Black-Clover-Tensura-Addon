package com.newuniverse.nusmp.avatar;

import com.mojang.logging.LogUtils;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;

import java.util.UUID;

/**
 * Why: the Avatar system touches Tensura's existence storage (EP, magicule, aura, naming flags) in several places (creation, the naming surge, evolution) and
 * every touch can fail for a mob that Tensura does not track (a missing storage, a missing attribute on the entity type). One class does the reads and writes
 * defensively, so the rest of the system never needs a try block and a Tensura change breaks one file.
 */
public final class TensuraBridge {
    private TensuraBridge() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static boolean warned;

    /** Sets maximum and current EP, magicule and aura. Any pool that cannot be written is skipped. */
    public static void setEnergy(LivingEntity e, double ep, double magicule, double aura) {
        try { EnergyHelper.setBaseMaxEP(e, ep); } catch (Throwable t) { warn("max EP", t); }
        try { EnergyHelper.setMaxMagicule(e, magicule); } catch (Throwable t) { warn("max magicule", t); }
        try { EnergyHelper.setMaxAura(e, aura); } catch (Throwable t) { warn("max aura", t); }
        try {
            var ex = TensuraStorages.getExistenceFrom(e);
            if (ex != null) {
                ex.setEP(ep);
                ex.setMagicule(magicule);
                ex.setAura(aura);
                ex.markDirty();
            }
        } catch (Throwable t) { warn("existence", t); }
    }

    /** Takes a fraction of the entity's maximum magicule from its current magicule (never below zero). */
    public static void drainMagicule(LivingEntity e, double fractionOfMax) {
        try {
            var ex = TensuraStorages.getExistenceFrom(e);
            if (ex == null) return;
            ex.setMagicule(Math.max(0.0, ex.getMagicule() - EnergyHelper.getMaxMagicule(e) * fractionOfMax));
            ex.markDirty();
        } catch (Throwable t) { warn("magicule drain", t); }
    }

    /** Multiplies maximum and current EP, magicule and aura together (the naming surge on a race that does not evolve). */
    public static void scaleEnergy(LivingEntity e, double factor) {
        try {
            setEnergy(e, EnergyHelper.getBaseMaxEP(e) * factor, EnergyHelper.getMaxMagicule(e) * factor, EnergyHelper.getMaxAura(e) * factor);
        } catch (Throwable t) { warn("energy scale", t); }
    }

    /** The entity's current magicule (0 when unknown). */
    public static double magicule(LivingEntity e) {
        try {
            var ex = TensuraStorages.getExistenceFrom(e);
            return ex == null ? 0.0 : ex.getMagicule();
        } catch (Throwable t) { return 0.0; }
    }

    /** Spends magicule; false (and nothing spent) when there is not enough. A cost of zero or less is free. */
    public static boolean spendMagicule(LivingEntity e, double cost) {
        if (cost <= 0) return true;
        try {
            var ex = TensuraStorages.getExistenceFrom(e);
            if (ex == null || ex.getMagicule() < cost) return false;
            ex.setMagicule(ex.getMagicule() - cost);
            ex.markDirty();
            return true;
        } catch (Throwable t) { return false; }
    }

    /** Gives magicule back (a refund), up to the entity's maximum. */
    public static void refundMagicule(LivingEntity e, double amount) {
        try {
            var ex = TensuraStorages.getExistenceFrom(e);
            if (ex == null) return;
            ex.setMagicule(Math.min(EnergyHelper.getMaxMagicule(e), ex.getMagicule() + amount));
            ex.markDirty();
        } catch (Throwable t) { warn("magicule refund", t); }
    }

    /** The entity's maximum EP, or 0 when Tensura does not know it. */
    public static double maxEp(LivingEntity e) {
        try { return EnergyHelper.getMaxEP(e); } catch (Throwable t) { return 0.0; }
    }

    /** The base maximum EP (what a naming surge multiplies). */
    public static double baseMaxEp(LivingEntity e) {
        try { return EnergyHelper.getBaseMaxEP(e); } catch (Throwable t) { return 0.0; }
    }

    /** Marks the entity as nameable by Tensura's own naming and records who owns it (or clears the owner with null). */
    public static void setNaming(LivingEntity e, String name, UUID owner, boolean nameable) {
        try {
            var ex = TensuraStorages.getExistenceFrom(e);
            if (ex == null) return;
            ex.setNameable(nameable);
            if (name != null) ex.setName(name);
            if (owner != null) ex.setPermanentOwner(owner);
            ex.markDirty();
        } catch (Throwable t) { warn("naming flags", t); }
    }

    /** True when Tensura already considers this entity named. */
    public static boolean tensuraNamed(LivingEntity e) {
        try {
            var ex = TensuraStorages.getExistenceFrom(e);
            return ex != null && ex.getName() != null && !ex.getName().isEmpty();
        } catch (Throwable t) { return false; }
    }

    private static void warn(String what, Throwable t) {
        if (warned) return;
        warned = true;
        LOG.warn("[nusmp] Avatar: could not write Tensura {} ({}); further Tensura failures are silent", what, t.toString());
    }
}
