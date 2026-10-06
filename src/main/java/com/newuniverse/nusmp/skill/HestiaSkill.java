package com.newuniverse.nusmp.skill;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;

/** Hestia, Goddess of the Hearth: heals and blesses you and every player near you. */
public class HestiaSkill extends GodSkill {
    private static final ResourceLocation ICON = ResourceLocation.withDefaultNamespace("textures/item/campfire.png");
    @Override public ResourceLocation getSkillIcon() { return ICON; }
    @Override protected double cost() { return 1500; }
    @Override protected int cooldownTicks() { return 1200; }

    @Override
    protected boolean use(ManasSkillInstance instance, ServerPlayer player, boolean mastered) {
        ServerLevel level = player.serverLevel();
        double radius = mastered ? 16 : 10;
        for (ServerPlayer ally : level.getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(radius))) {
            ally.heal(ally.getMaxHealth() * (mastered ? 0.6F : 0.35F));
            ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, mastered ? 2 : 1));
            ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 1200, mastered ? 3 : 1));
            level.sendParticles(ParticleTypes.HEART, ally.getX(), ally.getY() + 2, ally.getZ(), 5, 0.4, 0.2, 0.4, 0.0);
        }
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, player.getX(), player.getY(), player.getZ(), 20, radius / 3, 0.2, radius / 3, 0.01);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.4F);
        return true;
    }
}
