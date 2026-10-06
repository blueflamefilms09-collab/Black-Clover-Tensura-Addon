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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** Freya, Goddess of Beauty: Charm. Nearby monsters lose interest in you; players are dazed. */
public class FreyaSkill extends GodSkill {
    private static final ResourceLocation ICON = ResourceLocation.withDefaultNamespace("textures/item/pink_dye.png");
    @Override public ResourceLocation getSkillIcon() { return ICON; }
    @Override protected double cost() { return 1200; }
    @Override protected int cooldownTicks() { return 900; }

    @Override
    protected boolean use(ManasSkillInstance instance, ServerPlayer player, boolean mastered) {
        ServerLevel level = player.serverLevel();
        int ticks = mastered ? 200 : 120;
        for (LivingEntity t : DMUtil.around(player, player.position(), mastered ? 14 : 9)) {
            if (t instanceof Mob mob) mob.setTarget(null);
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 3));
            t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 2));
            if (t instanceof ServerPlayer) t.addEffect(new MobEffectInstance(MobEffects.CONFUSION, ticks, 0));
            level.sendParticles(ParticleTypes.HEART, t.getX(), t.getY() + t.getBbHeight() + 0.3, t.getZ(), 3, 0.3, 0.2, 0.3, 0.0);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 2.0F, 1.2F);
        return true;
    }
}
