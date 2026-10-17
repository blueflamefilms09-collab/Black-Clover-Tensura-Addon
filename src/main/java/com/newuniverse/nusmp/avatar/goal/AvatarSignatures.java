package com.newuniverse.nusmp.avatar.goal;

import com.newuniverse.nusmp.avatar.AvatarChatterEngine;
import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.avatar.ChatEvent;
import com.newuniverse.nusmp.avatar.outcome.AvatarUniqueSkill;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the three signature moves a generated Unique Skill can carry ({@link AvatarUniqueSkill.Signature}). Each hurts through {@link BalanceLaw#damage}
 * (EP scaling, the player hit cap, the mob multiplier) and only touches what {@link AvatarTargeting#canHarm} allows; the visuals are the mod's own effect shapes
 * ({@link VfxSpawn}), not vanilla particles.
 */
public final class AvatarSignatures {
    private AvatarSignatures() {}

    public static void perform(AvatarEntity a, LivingEntity target, AvatarUniqueSkill.Perk perk) {
        if (!(a.level() instanceof ServerLevel sl)) return;
        AvatarChatterEngine.say(a, ChatEvent.SKILL_CAST, perk.name());
        switch (perk.signature()) {
            case SHOCKWAVE -> {
                VfxSpawn.send(sl, VfxShape.WIND_RING, a.position().add(0, 0.2, 0), a.position().add(0, 1.2, 0), 0xFFE8E0C8, 16, 1.6f);
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
                VfxSpawn.send(sl, VfxShape.WIND_SLASH, a.position().add(0, 1.0, 0), target.position().add(0, target.getBbHeight() * 0.5, 0), 0xFFFFFFFF, 10, 1.0f);
                if (AvatarTargeting.canHarm(a, target)) hit(a, target, perk.signaturePower() * 1.2);
            }
            case SOUL_FLAME -> {
                if (!AvatarTargeting.canHarm(a, target)) return;
                VfxSpawn.send(sl, VfxShape.FIRE_BURST, target.position().add(0, 0.2, 0), target.position().add(0, 1.2, 0), 0xFF4FD8FF, 18, 1.0f);
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
