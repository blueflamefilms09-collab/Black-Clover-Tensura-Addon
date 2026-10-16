package com.newuniverse.nusmp.butoh;

import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;

/**
 * Why: a dancer's answer to arrows and bolts is to not be where they land, and to send them home. The deflect dash is a short sidelong or
 * forward slip (the same "find a free spot along the line, then move there" approach as Ebb Step, but swept so it cannot pass through a
 * wall) that opens a 12 tick window in which any missile reaching the player is turned back at its shooter, with the player as its new owner.
 * It works outside the dance too; inside the dance the return is faster and tighter ({@link DeflectMath}). It never touches Ebb Step.
 */
public final class ButohDash {
    private ButohDash() {}

    static final String K_UNTIL = "nusmp_butoh_deflect_until", K_TURNS = "nusmp_butoh_turns";
    /** Below this horizontal speed (blocks per tick) the player counts as standing still, and the dash follows the look direction. */
    private static final double MOVING = 0.08;

    /** The page: dash along the way the player is moving (so a strafe dashes sideways), otherwise along the look; false if there is no room. */
    public static boolean cast(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        boolean dancing = ButohDance.dancing(p);
        Vec3 dir = direction(p);
        Vec3 from = p.position();
        Vec3 dest = null;
        for (double d = DeflectMath.dashDistance(dancing); d >= 1.5; d -= 0.5) {
            Vec3 c = from.add(dir.scale(d));
            if (clearPath(p, from, c)) { dest = c; break; }
        }
        if (dest == null) { GrimoireBook.fail(p, "No room to dash."); return false; }
        b.castCircle(p, 0.6f);
        VfxSpawn.send(p.serverLevel(), VfxShape.BUTOH_AFTERIMAGE, from, dest, b.color, 16, 1.0f);
        b.vfx(p, VfxShape.BUTOH_FX1, from.add(0, 1, 0), dest.add(0, 1, 0), 12, dancing ? 1.0f : 0.7f);
        p.teleportTo(dest.x, dest.y, dest.z);
        p.fallDistance = 0;
        p.getPersistentData().putLong(K_UNTIL, p.level().getGameTime() + DeflectMath.WINDOW_TICKS);
        return true;
    }

    /** The horizontal dash direction: the way the server saw the player move last tick, else the look direction. */
    static Vec3 direction(ServerPlayer p) {
        Vec3 m = p.getKnownMovement();
        Vec3 h = new Vec3(m.x, 0, m.z);
        if (h.lengthSqr() > MOVING * MOVING) return h.normalize();
        Vec3 look = p.getViewVector(1f);
        h = new Vec3(look.x, 0, look.z);
        return h.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : h.normalize();
    }

    /** A free spot reached without passing through a block, with ground under it (no dashing off a cliff into the void). */
    private static boolean clearPath(ServerPlayer p, Vec3 from, Vec3 to) {
        AABB start = p.getBoundingBox();
        AABB end = start.move(to.subtract(from));
        if (!p.level().noCollision(p, start.minmax(end))) return false;
        return !p.level().noCollision(p, end.move(0, -3.0, 0)) && !p.level().containsAnyLiquid(end);
    }

    /** True while the dash window is open for this player. */
    public static boolean windowOpen(ServerPlayer p) {
        return DeflectMath.open(p.level().getGameTime(), p.getPersistentData().getLong(K_UNTIL));
    }

    /** ProjectileImpactEvent (server): a missile about to hit a player inside the window is turned around instead. */
    public static void onImpact(ProjectileImpactEvent e) {
        Projectile pr = e.getProjectile();
        if (pr.level().isClientSide()) return;
        if (!(e.getRayTraceResult() instanceof EntityHitResult hit) || !(hit.getEntity() instanceof ServerPlayer p)) return;
        if (!windowOpen(p) || pr.getOwner() == p) return;
        int turns = pr.getPersistentData().getInt(K_TURNS);
        e.setCanceled(true);
        if (!DeflectMath.mayTurn(turns)) { pr.discard(); return; }            // turned back twice already: it falls, it is not sent a third time
        boolean dancing = ButohDance.dancing(p);
        Entity shooter = pr.getOwner();
        double speed = Math.max(0.6, pr.getDeltaMovement().length() * DeflectMath.speedFactor(dancing));
        Vec3 dir;
        if (shooter != null && shooter.isAlive()) dir = shooter.getBoundingBox().getCenter().subtract(pr.position());
        else dir = pr.getDeltaMovement().scale(-1);
        if (dir.lengthSqr() < 1e-6) dir = p.getViewVector(1f).scale(-1);
        dir = dir.normalize();
        pr.getPersistentData().putInt(K_TURNS, turns + 1);
        pr.deflect(ProjectileDeflection.REVERSE, p, p, true);                  // turns it, makes the dancer its owner
        pr.setPos(pr.position().add(dir.scale(0.6)));                           // out of the dancer's body, so it flies free
        pr.shoot(dir.x, dir.y, dir.z, (float) speed, (float) DeflectMath.spreadDegrees(dancing));
        pr.hurtMarked = true;
        VfxSpawn.send(p.serverLevel(), VfxShape.BUTOH_FX3, pr.position(), pr.position().add(dir), 0xFFB8E6FF, 14, 0.5f);
    }
}
