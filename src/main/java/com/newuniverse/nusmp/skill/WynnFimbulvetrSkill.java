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

/** Wynn Fimbulvetr (Riveria): a blizzard that freezes everything at the target area. 3-line chant. */
public class WynnFimbulvetrSkill extends ChantedMagicSkill {
    private static final ResourceLocation ICON = ResourceLocation.withDefaultNamespace("textures/item/snowball.png");
    @Override public ResourceLocation getSkillIcon() { return ICON; }
    @Override protected int chantLines() { return 3; }
    @Override protected double totalCost() { return 3000; }
    @Override protected int castCooldown() { return 600; }

    @Override
    protected boolean cast(ManasSkillInstance instance, ServerPlayer player, ServerLevel level, boolean mastered) {
        Vec3 c = DMUtil.lookPoint(player, 40);
        double radius = mastered ? 10 : 7;
        for (LivingEntity t : DMUtil.around(player, c, radius)) {
            t.hurt(player.damageSources().playerAttack(player), DMUtil.dmg(mastered ? 30 : 20));
            t.setTicksFrozen(t.getTicksRequiredToFreeze() + 200);
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 140, 4));
        }
        level.sendParticles(ParticleTypes.SNOWFLAKE, c.x, c.y + 1, c.z, 400, radius / 2, 2, radius / 2, 0.05);
        level.playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 2.0F, 0.5F);
        return true;
    }
}
