package com.newuniverse.nusmp.skill;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Uchide no Kozuchi (Haruhime): "Level Boost" — temporarily raises an ally's power.
 * Targets the player you look at, or yourself if nobody. 3-line chant, long cooldown.
 */
public class UchideNoKozuchiSkill extends ChantedMagicSkill {
    private static final ResourceLocation ICON = ResourceLocation.withDefaultNamespace("textures/item/golden_apple.png");
    @Override public ResourceLocation getSkillIcon() { return ICON; }
    @Override protected int chantLines() { return 3; }
    @Override protected double totalCost() { return 4000; }
    @Override protected int castCooldown() { return 6000; }

    @Override
    protected boolean cast(ManasSkillInstance instance, ServerPlayer player, ServerLevel level, boolean mastered) {
        ServerPlayer target = SkillUtil.lookTarget(player, 10);
        if (target == null) target = player;
        int ticks = mastered ? 2400 : 1200;
        target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, 1));
        target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 1));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, 1));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, ticks, 1));
        target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ticks, mastered ? 4 : 2));
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, target.getX(), target.getY() + 1, target.getZ(), 80, 0.5, 1, 0.5, 0.3);
        level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 0.8F);
        target.displayClientMessage(Component.literal("LEVEL BOOST! Your power surges.").withStyle(ChatFormatting.GOLD), true);
        return true;
    }
}
