package com.newuniverse.nusmp.thread;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: Arresting Dark Weave must hold someone completely still, and the old "Slowness X" root still lets a player shuffle and jump.
 * A real effect can take away movement (-100%), jumping (-100%) and knockback (immune) through attribute modifiers, shows its icon
 * and timer on the victim's screen, is cleared by the same things that clear any effect, and ends on its own. Wire it in NUSMP: call
 * {@link #init()} and {@code EFFECTS.register(modEventBus)}.
 */
public final class ThreadEffects {
    private ThreadEffects() {}

    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, NUSMP.MODID);

    /** Bound in red thread: cannot walk, jump or be knocked about. */
    public static final DeferredHolder<MobEffect, MobEffect> BOUND = EFFECTS.register("thread_bound", () -> new Bound()
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, id("thread_bound_speed"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            .addAttributeModifier(Attributes.JUMP_STRENGTH, id("thread_bound_jump"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            .addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, id("thread_bound_knockback"), 1.0, AttributeModifier.Operation.ADD_VALUE));

    /** Loads this class before the registry event; call it from the NUSMP constructor. */
    public static void init() {}

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, path); }

    /** An effect that also stops what the attribute cannot: sideways drift and flying up (MobEffect's constructor is protected). */
    private static final class Bound extends MobEffect {
        Bound() { super(MobEffectCategory.HARMFUL, 0xFF3355); }

        @Override public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return true; }

        @Override
        public boolean applyEffectTick(LivingEntity e, int amplifier) {
            var v = e.getDeltaMovement();
            e.setDeltaMovement(0.0, Math.min(0.0, v.y), 0.0);
            return true;
        }
    }
}
