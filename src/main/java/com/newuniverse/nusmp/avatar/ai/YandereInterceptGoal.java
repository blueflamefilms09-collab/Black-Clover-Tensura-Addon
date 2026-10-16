package com.newuniverse.nusmp.avatar.ai;

import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.avatar.AvatarPersonality;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * Why: the movement half of "it takes a hit for the creator". A Yandere Avatar steps into the line of an arrow, bolt or charging attacker that is
 * about to reach its creator, so that the damage lands on it (the damage split itself, 70% on the Avatar and never killing it, happens in
 * {@code AvatarAiEvents.intercept}). The step is a short teleport (never into a wall, never farther than 24 blocks) on a cooldown, so it cannot be
 * used to blink around freely.
 */
public final class YandereInterceptGoal extends Goal {
    private static final int COOLDOWN_TICKS = 30;
    private final AvatarEntity avatar;
    private long readyAt;
    private Vec3 spot;

    public YandereInterceptGoal(AvatarEntity avatar) {
        this.avatar = avatar;
        setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        if (avatar.level().isClientSide || avatar.tickCount % 3 != 0 || !avatar.has(AvatarPersonality.YANDERE) || !AvatarGates.loyal(avatar)) return false;
        if (avatar.level().getGameTime() < readyAt) return false;
        ServerPlayer creator = avatar.creatorPlayer();
        if (creator == null || creator.level() != avatar.level() || avatar.distanceToSqr(creator) > 24 * 24) return false;
        spot = null;
        Vec3 c = creator.position().add(0, 0.1, 0);
        for (Projectile p : avatar.level().getEntitiesOfClass(Projectile.class, creator.getBoundingBox().inflate(14), pr -> pr.getOwner() != creator && pr.getOwner() != avatar)) {
            Vec3 v = p.getDeltaMovement();
            if (v.lengthSqr() < 0.04) continue;
            Vec3 to = creator.getEyePosition().subtract(p.position());
            if (to.lengthSqr() > 14 * 14 || v.normalize().dot(to.normalize()) < 0.9) continue;
            spot = c.subtract(v.normalize().multiply(1, 0, 1).normalize().scale(1.5));
            break;
        }
        if (spot == null) {
            for (Mob m : avatar.level().getEntitiesOfClass(Mob.class, creator.getBoundingBox().inflate(6), mob -> mob.getTarget() == creator && mob != avatar)) {
                if (avatar.distanceToSqr(creator) <= 9) break;                                     // already close enough to take the hit
                Vec3 dir = m.position().subtract(creator.position()).multiply(1, 0, 1);
                if (dir.lengthSqr() < 1e-4) continue;
                spot = c.add(dir.normalize().scale(1.2));
                break;
            }
        }
        return spot != null;
    }

    @Override public boolean canContinueToUse() { return false; }

    @Override
    public void start() {
        readyAt = avatar.level().getGameTime() + COOLDOWN_TICKS;
        if (spot == null) return;
        var box = avatar.getBoundingBox().move(spot.subtract(avatar.position()));
        if (avatar.level().noCollision(avatar, box)) avatar.teleportTo(spot.x, spot.y, spot.z);
    }
}
