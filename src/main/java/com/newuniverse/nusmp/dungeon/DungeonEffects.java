package com.newuniverse.nusmp.dungeon;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
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
    public static final DeferredHolder<MobEffect, MobEffect> MAGICULE_POISONING = EFFECTS.register("magicule_poisoning", () -> new MobEffect(MobEffectCategory.HARMFUL, 0xE0B341)
            .addAttributeModifier(Attributes.MAX_HEALTH, id("magicule_poisoning_health"), -0.10, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    /** Raw monster meat: queasy and weak, hungry on top of it. */
    public static final DeferredHolder<MobEffect, MobEffect> MAGICULE_SICKNESS = EFFECTS.register("magicule_sickness", () -> new MobEffect(MobEffectCategory.HARMFUL, 0x8E5BA8) {
        @Override
        public boolean applyEffectTick(LivingEntity e, int amplifier) {
            if (e instanceof Player p) p.causeFoodExhaustion(0.35f * (amplifier + 1));
            return true;
        }

        @Override public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return duration % 20 == 0; }
    }.addAttributeModifier(Attributes.ATTACK_DAMAGE, id("magicule_sickness_attack"), -0.25, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, path); }
}
