package com.newuniverse.nusmp.dungeon;

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

/** The three effects of the Living Dungeon (docs/living_dungeon.md): thin air, magicule radiation and raw monster meat. */
public final class DungeonEffects {
    private DungeonEffects() {}

    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, NUSMP.MODID);

    /** Thin air: damage over time, slower movement and mining. */
    public static final DeferredHolder<MobEffect, MobEffect> SUFFOCATION = EFFECTS.register("suffocation", () -> new MobEffect(MobEffectCategory.HARMFUL, 0x6B7C8C) {
        @Override
        public boolean applyEffectTick(LivingEntity e, int amplifier) {
            e.hurt(e.damageSources().drown(), 1.0f + 0.5f * amplifier);
            return true;
        }

        @Override public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return duration % 30 == 0; }
    }.addAttributeModifier(Attributes.MOVEMENT_SPEED, id("suffocation_speed"), -0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            .addAttributeModifier(Attributes.BLOCK_BREAK_SPEED, id("suffocation_mining"), -0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    /** Magicule radiation: maximum health falls 10% per level (five levels). */
    public static final DeferredHolder<MobEffect, MobEffect> MAGICULE_POISONING = EFFECTS.register("magicule_poisoning", () -> new Plain(MobEffectCategory.HARMFUL, 0xE0B341)
            .addAttributeModifier(Attributes.MAX_HEALTH, id("magicule_poisoning_health"), -0.10, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    /**
     * Raw monster meat: queasy and weak. Until 1.98.0 it was "hungry on top of it" (a food exhaustion every second); the hunger ban (1.99.0) took that part out:
     * the effect keeps its id and its attack penalty and no longer touches the food bar.
     */
    public static final DeferredHolder<MobEffect, MobEffect> MAGICULE_SICKNESS = EFFECTS.register("magicule_sickness", () -> new MobEffect(MobEffectCategory.HARMFUL, 0x8E5BA8) {
        @Override public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return false; }
    }.addAttributeModifier(Attributes.ATTACK_DAMAGE, id("magicule_sickness_attack"), -0.25, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    /** An effect with nothing but attribute modifiers (MobEffect's constructor is protected). */
    private static final class Plain extends MobEffect {
        Plain(MobEffectCategory category, int color) { super(category, color); }
    }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, path); }
}
