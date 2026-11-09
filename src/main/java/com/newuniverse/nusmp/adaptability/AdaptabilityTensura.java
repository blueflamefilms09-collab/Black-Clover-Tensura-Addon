package com.newuniverse.nusmp.adaptability;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;

import java.lang.reflect.Method;

/**
 * Why: Adaptability needs three small things from Tensura that this mod does not use anywhere else: whether a blow is Severance, whether it is a plain physical attack, and a way to clear the
 * Severance that has built up on a body. They are looked up by name ONCE, on first use, and called through the cached {@link Method}; if Tensura's names ever change, the lookup logs once and the
 * Severance part of the skill switches itself off (the damage learning and everything else keep working). No Tensura class is linked at compile time, so a Tensura update cannot break the build.
 */
final class AdaptabilityTensura {
    private static final Logger LOG = LogUtils.getLogger();
    private AdaptabilityTensura() {}

    private static boolean looked;
    private static Method severance, physical, effectOf, getAmount, setAmount, setRemoveTime, markDirty;

    private static synchronized void look() {
        if (looked) return;
        looked = true;
        try {
            Class<?> helper = Class.forName("io.github.manasmods.tensura.damage.TensuraDamageHelper");
            severance = helper.getMethod("isSeveranceDamage", DamageSource.class, LivingEntity.class, boolean.class);
            physical = helper.getMethod("isPhysicalAttack", DamageSource.class);
            Class<?> storages = Class.forName("io.github.manasmods.tensura.storage.TensuraStorages");
            effectOf = storages.getMethod("getEffectFrom", LivingEntity.class);
            Class<?> effect = Class.forName("io.github.manasmods.tensura.storage.effect.IEffect");
            getAmount = effect.getMethod("getSeveranceAmount");
            setAmount = effect.getMethod("setSeveranceAmount", float.class);
            setRemoveTime = effect.getMethod("setSeveranceRemoveTime", int.class);
            markDirty = effect.getMethod("markDirty");
        } catch (ReflectiveOperationException | LinkageError e) {
            severance = null;
            LOG.warn("[nusmp] Adaptability: Tensura's Severance helpers were not found ({}); the Severance part of the skill is off", e.toString());
        }
    }

    static boolean available() { look(); return severance != null; }

    /** Tensura's own test: {@code build} true = does this blow build Severance up on the victim; false = is this blow Severance damage. False when Tensura cannot be asked. */
    static boolean isSeverance(DamageSource source, LivingEntity victim, boolean build) {
        if (!available()) return false;
        try { return (boolean) severance.invoke(null, source, victim, build); } catch (ReflectiveOperationException | RuntimeException e) { return false; }
    }

    static boolean isPhysical(DamageSource source) {
        if (!available()) return false;
        try { return (boolean) physical.invoke(null, source); } catch (ReflectiveOperationException | RuntimeException e) { return false; }
    }

    /** Clears the Severance built up on the player (the amount and its timer). */
    static void clearSeverance(ServerPlayer p) {
        if (!available()) return;
        try {
            Object effect = effectOf.invoke(null, p);
            if (effect == null || (float) getAmount.invoke(effect) <= 0f) return;
            setAmount.invoke(effect, 0f);
            setRemoveTime.invoke(effect, 0);
            markDirty.invoke(effect);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // the next hit tries again
        }
    }
}
