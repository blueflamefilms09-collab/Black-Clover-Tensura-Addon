package com.newuniverse.nusmp.entity.veldanava.skills;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: Temporal Debt is a count of stacks that must live on the creature, show on its screen, run out by itself and be cleared by death, and it is kept as the amplifier of the
 * {@link JudgmentEffects#TEMPORAL_DEBT} effect: stack n is amplifier n - 1. This is the one place that adds to it, reads it and detonates it, so the cap and the lifetime are in one spot.
 * A stack lives 30 seconds from the last time one was added.
 */
public final class TemporalDebt {
    private TemporalDebt() {}

    public static final int LIFETIME_TICKS = 600;

    /** The stacks a creature carries (0 when none). */
    public static int get(LivingEntity e) {
        try {
            MobEffectInstance i = e.getEffect(JudgmentEffects.TEMPORAL_DEBT);
            return i == null ? 0 : i.getAmplifier() + 1;
        } catch (RuntimeException notReady) {
            return 0;
        }
    }

    /** Adds stacks (never past the config cap) and renews the lifetime. Returns the stacks now carried. Server side. */
    public static int add(LivingEntity e, int stacks) {
        if (e.level().isClientSide || stacks <= 0 || !e.isAlive()) return get(e);
        int now = Math.min(Math.max(1, JudgmentConfig.debtMax()), get(e) + stacks);
        e.addEffect(new MobEffectInstance(JudgmentEffects.TEMPORAL_DEBT, LIFETIME_TICKS, now - 1, false, true, true));
        return now;
    }

    /** Clears the debt and returns the stacks it was. */
    public static int clear(LivingEntity e) {
        int was = get(e);
        if (was > 0) e.removeEffect(JudgmentEffects.TEMPORAL_DEBT);
        return was;
    }

    /**
     * Detonates the debt of a creature: removes it and returns the damage it is worth (stacks x the config per-stack damage x {@code multiplier}, at least {@code floor}, at most {@code cap}).
     * The caller deals the damage.
     */
    public static double detonate(LivingEntity e, double multiplier, double floor, double cap) {
        int stacks = clear(e);
        return JudgmentRules.detonation(stacks, JudgmentConfig.debtDamage(), multiplier, floor, cap);
    }
}
