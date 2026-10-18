package com.newuniverse.nusmp.heroweapon.weapon.vassalkit;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the Mirror turns hostile shots back and the Sewing Set and the Chain catch them, and all three need the same two answers: which shots in a volume are a danger
 * to the wielder, and how to turn one round properly. The turning uses the game's own projectile deflection (the call a player's blow makes on a fireball), so the
 * owner changes, a fireball's acceleration follows its new direction and the bookkeeping stays right for modded shots too. Harmless throwables (an ender pearl, a
 * snowball, an egg, an experience bottle, a fishing hook) are left alone so a thrown pearl never teleports the wielder. Shots already in the ground are ignored.
 */
public final class Deflect {
    private Deflect() {}

    /** True for a shot that is worth catching: anything but the harmless vanilla throwables, and only while it is really moving. */
    public static boolean dangerous(Projectile pr) {
        if (pr instanceof ThrownEnderpearl || pr instanceof ThrownExperienceBottle || pr instanceof FishingHook || pr instanceof Snowball || pr instanceof ThrownEgg) return false;
        return pr.getDeltaMovement().lengthSqr() > 0.01;
    }

    /** The shots inside {@code box} that did not come from the wielder or an ally and are a danger. */
    public static List<Projectile> hostileShots(ServerPlayer p, AABB box) {
        List<Projectile> out = new ArrayList<>();
        for (Projectile pr : p.serverLevel().getEntitiesOfClass(Projectile.class, box)) {
            if (!dangerous(pr)) continue;
            if (pr.getOwner() == p || pr.getOwner() instanceof LivingEntity o && Foes.isAlly(p, o)) continue;
            out.add(pr);
        }
        return out;
    }

    /**
     * Sends a hostile shot back at its shooter (straight back when the shooter is gone or unknown), a little faster than it came, and makes it the wielder's. The
     * game's deflection call does the owner change and the acceleration reset.
     */
    public static void turnBack(ServerPlayer p, Projectile pr) {
        Vec3 v = pr.getDeltaMovement();
        double speed = Math.max(0.6, v.length() * 1.1);
        Vec3 dir = v.scale(-1);
        if (pr.getOwner() instanceof LivingEntity shooter && shooter.isAlive()) {
            Vec3 toShooter = shooter.getBoundingBox().getCenter().subtract(pr.position());
            if (toShooter.lengthSqr() > 1e-4) dir = toShooter;
        }
        if (dir.lengthSqr() < 1e-6) dir = new Vec3(0, 0, 1);
        pr.setDeltaMovement(dir.normalize().scale(speed));
        pr.deflect(ProjectileDeflection.NONE, p, p, true);
        pr.hasImpulse = true;
        pr.hurtMarked = true;
    }

    /** Sends a shot back along a given unit direction (for a pane that mirrors it off its face instead of aiming at the shooter). */
    public static void turnAlong(ServerPlayer p, Projectile pr, Vec3 dir) {
        double speed = Math.max(0.6, pr.getDeltaMovement().length() * 1.1);
        pr.setDeltaMovement(dir.normalize().scale(speed));
        pr.deflect(ProjectileDeflection.NONE, p, p, true);
        pr.hasImpulse = true;
        pr.hurtMarked = true;
    }
}
