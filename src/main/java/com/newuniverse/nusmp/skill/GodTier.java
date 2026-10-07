package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;

/** Helpers for the reality-warping skills. */
final class GodTier {
    private GodTier() {}

    /** Living entity under the crosshair, or null. */
    static LivingEntity lookLiving(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 end = DMUtil.lookPoint(player, range);
        AABB box = player.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player.serverLevel(), player, start, end, box,
                e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator() && e != player);
        return hit == null ? null : (LivingEntity) hit.getEntity();
    }

    /** Bosses (tagged c:bosses) or anything with very high max health counts as a boss. */
    static boolean isBoss(LivingEntity e) {
        return e.getType().is(Tags.EntityTypes.BOSSES) || e.getMaxHealth() >= NUConfig.BOSS_HEALTH_THRESHOLD.get();
    }

    /** True if absolute effects (instant kill, erase) may fully apply to this target. */
    static boolean fullyAffected(LivingEntity e) {
        if (e instanceof Player) return com.newuniverse.nusmp.NUGameRules.godTierAffectsPlayers(e.level());
        return !isBoss(e);
    }

    /** Kills the target and credits the player for it. */
    static void kill(ServerPlayer player, LivingEntity target) {
        target.setLastHurtByPlayer(player);
        target.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);
        if (target.isAlive()) {
            target.setHealth(0.0F);
            target.die(player.damageSources().playerAttack(player));
        }
    }

    /** Damage equal to a share of the target's max health (used against bosses / players). */
    static void percentDamage(ServerPlayer player, LivingEntity target, double share) {
        target.hurt(player.damageSources().playerAttack(player), (float) (target.getMaxHealth() * share));
    }

    static void line(ServerLevel level, ParticleOptions particle, Vec3 from, Vec3 to) {
        Vec3 step = to.subtract(from).normalize().scale(0.5);
        Vec3 p = from;
        for (int i = 0; i < from.distanceTo(to) * 2; i++) {
            level.sendParticles(particle, p.x, p.y, p.z, 1, 0.03, 0.03, 0.03, 0.0);
            p = p.add(step);
        }
    }
}
