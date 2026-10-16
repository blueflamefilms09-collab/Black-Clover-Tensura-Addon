package com.newuniverse.nusmp.avatar.naming;

import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Why: bestowing a formal name costs the creator dearly in Tensura, and the owner wants that as a visible debuff, the "NamingSleepPeriod" (registered as
 * {@code nusmp:naming_sleep}). While it lasts the creator's magicule regeneration is cut to a tenth, they move slowly (drowsy), and every second a share of
 * their magicule is drained through Tensura's own storage. The drain is a plain percentage of the maximum, so it scales with the creator; it never kills and
 * never touches an NPC (it only drains what Tensura reports as a pool).
 */
public final class NamingSleepEffect extends MobEffect {
    /** Percent of the maximum magicule drained each second. */
    private static final double DRAIN_FRACTION = 0.02;

    public NamingSleepEffect() {
        super(MobEffectCategory.HARMFUL, 0x4A3A7A);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, ResourceLocation.fromNamespaceAndPath("nusmp", "naming_sleep_speed"), -0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(TensuraAttributes.MAGICULE_REGENERATION_MULTIPLIER, ResourceLocation.fromNamespaceAndPath("nusmp", "naming_sleep_regen"), -0.9, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return duration % 20 == 0; }

    @Override
    public boolean applyEffectTick(LivingEntity e, int amplifier) {
        if (e.level().isClientSide) return true;
        com.newuniverse.nusmp.avatar.TensuraBridge.drainMagicule(e, DRAIN_FRACTION);
        return true;
    }
}
