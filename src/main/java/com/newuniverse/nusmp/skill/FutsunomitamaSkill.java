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
import net.minecraft.world.phys.Vec3;

/** Futsunomitama (Mikoto): a gravity field that crushes and pins enemies. 2-line chant. */
public class FutsunomitamaSkill extends ChantedMagicSkill {
    private static final ResourceLocation ICON = ResourceLocation.withDefaultNamespace("textures/item/heavy_core.png");
    @Override public ResourceLocation getSkillIcon() { return ICON; }
    @Override protected int chantLines() { return 2; }
    @Override protected double totalCost() { return 2000; }
    @Override protected int castCooldown() { return 500; }

    @Override
    protected boolean cast(ManasSkillInstance instance, ServerPlayer player, ServerLevel level, boolean mastered) {
        Vec3 c = DMUtil.lookPoint(player, 32);
        double radius = mastered ? 8 : 6;
        int ticks = mastered ? 160 : 100;
        for (LivingEntity t : DMUtil.around(player, c, radius)) {
            t.hurt(player.damageSources().playerAttack(player), DMUtil.dmg(mastered ? 16 : 10));
            t.setDeltaMovement(0, -2.0, 0);
            t.hurtMarked = true;
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 6));
            t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1));
        }
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y + 0.5, c.z, 300, radius / 2, 0.3, radius / 2, 0.0);
        level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.5F, 0.5F);
        return true;
    }
}
