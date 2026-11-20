package com.newuniverse.nusmp.heroweapon.weapon.kit;

import com.newuniverse.nusmp.heroweapon.skill.Dur;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;

/**
 * Why: the four weapons put the same few status effects on people (a slow, a weakness, a resistance, a speed) and take one harmful effect off an ally, and every
 * one of those is a vanilla mob effect with a fixed duration and strength; writing the construction of {@code MobEffectInstance} in a dozen places is how a
 * typo in the amplifier slips in. They live here once. None of them is hard control (a root is the strongest slowness, applied only after the shared control
 * gate has been passed by the technique), and none shows ambient particles.
 */
public final class Status {
    private Status() {}

    /**
     * Adds an effect of {@code amp} (0 is level I) for the BASE length {@code ticks}, which goes through the duration scale ({@link Dur}, setting
     * {@code heroweapon.durationScale}) here, once; the icon shows, the particles do not. A length that is already final goes through {@link #addRaw}.
     */
    public static void add(LivingEntity t, Holder<MobEffect> effect, int ticks, int amp) { addRaw(t, effect, Dur.t(ticks), amp); }

    /** The same effect for exactly {@code ticks} (a length the balance law has already decided, or a mechanical hold). Never scaled. */
    public static void addRaw(LivingEntity t, Holder<MobEffect> effect, int ticks, int amp) {
        if (t != null && t.isAlive() && ticks > 0) t.addEffect(new MobEffectInstance(effect, ticks, Math.max(0, amp), false, true));
    }

    /** Slowness of level {@code amp + 1} for a base length (scaled). */
    public static void slow(LivingEntity t, int ticks, int amp) { add(t, MobEffects.MOVEMENT_SLOWDOWN, ticks, amp); }

    /** Slowness of level {@code amp + 1} for exactly {@code ticks} (never scaled). */
    public static void slowRaw(LivingEntity t, int ticks, int amp) { addRaw(t, MobEffects.MOVEMENT_SLOWDOWN, ticks, amp); }

    /** Weakness I for a base length (scaled). */
    public static void weaken(LivingEntity t, int ticks) { add(t, MobEffects.WEAKNESS, ticks, 0); }

    /** Removes the first harmful effect the target carries; true when one was removed. */
    public static boolean cleanse(LivingEntity t) {
        for (MobEffectInstance e : new ArrayList<>(t.getActiveEffects())) {
            if (e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                t.removeEffect(e.getEffect());
                return true;
            }
        }
        return false;
    }
}
