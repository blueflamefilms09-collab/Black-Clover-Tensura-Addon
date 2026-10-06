package com.newuniverse.nusmp.skill;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/** Arcs Ray (Lefiya): a piercing beam of light that hits everything in a line. 1-line chant. */
public class ArcsRaySkill extends ChantedMagicSkill {
    private static final ResourceLocation ICON = ResourceLocation.withDefaultNamespace("textures/item/spectral_arrow.png");
    @Override public ResourceLocation getSkillIcon() { return ICON; }
    @Override protected int chantLines() { return 1; }
    @Override protected double totalCost() { return 800; }
    @Override protected int castCooldown() { return 100; }

    @Override
    protected boolean cast(ManasSkillInstance instance, ServerPlayer player, ServerLevel level, boolean mastered) {
        Vec3 start = player.getEyePosition();
        Vec3 end = DMUtil.lookPoint(player, 48);
        Vec3 dir = end.subtract(start).normalize();
        Set<LivingEntity> hit = new HashSet<>();
        for (double d = 0; d < start.distanceTo(end); d += 0.5) {
            Vec3 p = start.add(dir.scale(d));
            level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
            hit.addAll(DMUtil.around(player, p, 1.2));
        }
        for (LivingEntity t : hit) t.hurt(player.damageSources().playerAttack(player), DMUtil.dmg(mastered ? 22 : 14));
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 2.0F, 1.5F);
        return true;
    }
}
