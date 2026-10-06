package com.newuniverse.nusmp.skill;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

/** Rea Laevateinn (Riveria): pillars of flame erupt all around the caster. 3-line chant. */
public class ReaLaevateinnSkill extends ChantedMagicSkill {
    private static final ResourceLocation ICON = ResourceLocation.withDefaultNamespace("textures/item/fire_charge.png");
    @Override public ResourceLocation getSkillIcon() { return ICON; }
    @Override protected int chantLines() { return 3; }
    @Override protected double totalCost() { return 3500; }
    @Override protected int castCooldown() { return 700; }

    @Override
    protected boolean cast(ManasSkillInstance instance, ServerPlayer player, ServerLevel level, boolean mastered) {
        double radius = mastered ? 14 : 10;
        for (LivingEntity t : DMUtil.around(player, player.position(), radius)) {
            t.hurt(player.damageSources().playerAttack(player), DMUtil.dmg(mastered ? 35 : 24));
            t.igniteForSeconds(8.0F);
            level.sendParticles(ParticleTypes.FLAME, t.getX(), t.getY(), t.getZ(), 60, 0.3, 3.0, 0.3, 0.02);
        }
        level.sendParticles(ParticleTypes.LAVA, player.getX(), player.getY(), player.getZ(), 80, radius / 2, 0.5, radius / 2, 0.0);
        level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 2.0F, 0.5F);
        return true;
    }
}
