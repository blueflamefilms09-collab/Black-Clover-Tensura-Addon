package com.newuniverse.nusmp.avatar.goal;

import com.newuniverse.nusmp.avatar.AvatarChatterEngine;
import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.avatar.ChatEvent;
import com.newuniverse.nusmp.avatar.outcome.AvatarUniqueSkill;
import com.newuniverse.nusmp.balance.BalanceLaw;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the three signature moves a generated Unique Skill can carry ({@link AvatarUniqueSkill.Signature}). Each hurts through {@link BalanceLaw#damage}
 * (EP scaling, the player hit cap, the mob multiplier) and only touches what {@link AvatarTargeting#canHarm} allows; the visuals are vanilla particles,
 * so no new effect shape has to be registered.
 */
public final class AvatarSignatures {
    private AvatarSignatures() {}

    public static void perform(AvatarEntity a, LivingEntity target, AvatarUniqueSkill.Perk perk) {
        if (!(a.level() instanceof ServerLevel sl)) return;
        AvatarChatterEngine.say(a, ChatEvent.SKILL_CAST, perk.name());
        switch (perk.signature()) {
            case SHOCKWAVE -> {
                sl.sendParticles(ParticleTypes.EXPLOSION, a.getX(), a.getY() + 0.3, a.getZ(), 6, 1.5, 0.1, 1.5, 0.0);
                for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, a.getBoundingBox().inflate(4.0), e -> AvatarTargeting.canHarm(a, e))) {
                    hit(a, e, perk.signaturePower());
                    Vec3 push = e.position().subtract(a.position()).multiply(1, 0, 1).normalize().scale(0.9);
                    e.push(push.x, 0.35, push.z);
                    e.hurtMarked = true;
                }
            }
            case DASH_STRIKE -> {
                Vec3 dir = target.position().subtract(a.position()).normalize();
                a.setDeltaMovement(dir.x * 1.4, 0.25, dir.z * 1.4);
                a.hurtMarked = true;
                sl.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY(0.5), target.getZ(), 1, 0, 0, 0, 0);
                if (AvatarTargeting.canHarm(a, target)) hit(a, target, perk.signaturePower() * 1.2);
            }
            case SOUL_FLAME -> {
                if (!AvatarTargeting.canHarm(a, target)) return;
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, target.getX(), target.getY(0.5), target.getZ(), 24, 0.4, 0.6, 0.4, 0.02);
                target.igniteForSeconds(4.0f);
                hit(a, target, perk.signaturePower());
            }
        }
    }

    private static void hit(AvatarEntity a, LivingEntity e, double raw) {
        float dmg = BalanceLaw.damage(a, e, (float) raw, 0.5);
        e.hurt(a.damageSources().mobAttack(a), dmg);
    }
}
