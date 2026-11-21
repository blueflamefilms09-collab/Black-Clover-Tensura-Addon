package com.newuniverse.nusmp.boss.rules;

import com.mojang.logging.LogUtils;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.world.entity.LivingEntity;

/**
 * 4.96.0: keeps a boss's Tensura magicule and aura pools full, the way {@code boss/LuciusProfile.fillPools} does for Lucifero. A boss that spends Ki or magicule (Goku, Kakashi) ran dry; Tensura
 * then puts a creature with 0 magicule into sleep mode and, while it sleeps, hurts it for its full max health ({@link BossEnvironment#energyStarvation}). A full pool means no sleep mode, no
 * sleep-mode slowdown and no starvation damage. Server only; called about once a second for the few tracked bosses (never a world scan); a creature without Tensura data is left alone.
 */
public final class BossPools {
    private BossPools() {}

    /** Below this share of the maximum the pool is topped up (so a full pool is not rewritten and re-synced every second). */
    public static final double REFILL_BELOW = 0.9;

    /** Pure: does a pool of {@code now} out of {@code max} need a refill? */
    public static boolean needsRefill(double now, double max) { return max > 0.0 && now < max * REFILL_BELOW; }

    /** Tops up the magicule and aura of one boss. Returns true when something was written. */
    public static boolean topUp(LivingEntity boss) {
        try {
            var ex = TensuraStorages.getExistenceFrom(boss);
            if (ex == null) return false;
            double mMax = EnergyHelper.getMaxMagicule(boss), aMax = EnergyHelper.getMaxAura(boss);
            boolean wrote = false;
            if (needsRefill(ex.getMagicule(), mMax)) { ex.setMagicule(mMax); wrote = true; }
            if (needsRefill(ex.getAura(), aMax)) { ex.setAura(aMax); wrote = true; }
            if (wrote) ex.markDirty();
            return wrote;
        } catch (RuntimeException | LinkageError noTensuraData) {
            LogUtils.getLogger().debug("[anime_multiverse] boss pools not topped up: {}", noTensuraData.toString());
            return false;
        }
    }
}
