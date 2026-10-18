package com.newuniverse.nusmp.heroweapon.weapon.sword;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Why: the Sword strikes fast and wide, so it must be exact about whom it may strike: never a creative or spectator player, a team mate, a tamed pet (the wielder's
 * own or anyone's), a villager or a peaceful animal. {@link HeroCtx#around} already removes the wielder, spectators and the vanilla team mates; this class adds the
 * rest in one place so every technique and every passive asks the same question. A friend is a team mate, a pet of the wielder or of a friend, or a player the wielder
 * is not allowed to harm (the server has PvP off, or friendly fire is off in their team); a foe is a player the wielder may harm, a hostile mob, a boss, or a mob
 * that is hunting the wielder or one of their friends. Everything else is left alone.
 */
final class SwordTargets {
    private SwordTargets() {}

    /** True for the wielder, their team, their pets and the players they may not harm. */
    static boolean isFriend(ServerPlayer w, LivingEntity e) {
        if (e == w) return true;
        if (!e.isAlive() || e.isSpectator() || e instanceof ArmorStand) return false;
        if (e.isAlliedTo(w)) return true;
        if (e instanceof OwnableEntity o) {
            LivingEntity owner = o.getOwner();
            if (owner != null) return owner == w || owner.isAlliedTo(w) || (owner instanceof Player op && !w.canHarmPlayer(op));
        }
        if (e instanceof Player p) return !w.canHarmPlayer(p);
        return false;
    }

    /** True for a living thing a technique of the wielder may hurt. */
    static boolean isFoe(ServerPlayer w, LivingEntity e) {
        if (e == w || !e.isAlive() || e.isSpectator() || e instanceof ArmorStand) return false;
        if (isFriend(w, e)) return false;
        if (e instanceof Player p) return !p.isCreative() && w.canHarmPlayer(p);
        if (e instanceof OwnableEntity o && o.getOwnerUUID() != null) return false;                  // somebody's tamed animal
        if (e instanceof Enemy || BalanceLaw.isBoss(e)) return true;
        return e instanceof Mob m && m.getTarget() != null && isFriend(w, m.getTarget());
    }

    /** The foes among {@code raw}, nearest to the wielder first, at most {@link SwordNumbers#MAX_TARGETS}. */
    static List<LivingEntity> foes(HeroCtx c, List<LivingEntity> raw) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : raw) if (isFoe(c.player, e)) out.add(e);
        out.sort(Comparator.comparingDouble(e -> e.distanceToSqr(c.player)));
        return out.size() > SwordNumbers.MAX_TARGETS ? new ArrayList<>(out.subList(0, SwordNumbers.MAX_TARGETS)) : out;
    }

    /** Foes within {@code r} of {@code centre}. */
    static List<LivingEntity> foesAround(HeroCtx c, Vec3 centre, double r) { return foes(c, c.around(centre, r)); }

    /** Foes in front of the wielder: within {@code range} and {@code halfAngle} degrees of {@code dir}. */
    static List<LivingEntity> foesInCone(HeroCtx c, Vec3 dir, double range, double halfAngle) {
        double min = Math.cos(Math.toRadians(halfAngle));
        Vec3 eye = c.eye();
        List<LivingEntity> in = new ArrayList<>();
        for (LivingEntity e : foesAround(c, c.player.position(), range)) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            if (to.lengthSqr() < 1.0E-6 || to.normalize().dot(dir) >= min) in.add(e);
        }
        return in;
    }

    /** Foes within {@code width} of the segment a to b, nearest to the wielder first. */
    static List<LivingEntity> foesAlong(HeroCtx c, Vec3 a, Vec3 b, double width) { return foes(c, c.along(a, b, width)); }

    /** The point where a look ray of {@code range} ends: the first wall in the way, else the far end. */
    static Vec3 rayEnd(HeroCtx c, double range) {
        Vec3 eye = c.eye(), end = eye.add(c.look().scale(range));
        HitResult wall = c.level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, c.player));
        return wall.getType() == HitResult.Type.MISS ? end : wall.getLocation();
    }

    /** The foe under the crosshair (the nearest along the look ray, stopping at walls), or null. */
    static LivingEntity aimFoe(HeroCtx c, double range) {
        List<LivingEntity> hit = foes(c, c.along(c.eye(), rayEnd(c, range), 1.0));
        return hit.isEmpty() ? null : hit.get(0);
    }

    /** The box of half-size {@code r} about {@code centre}, for a search of projectiles or other entities. */
    static AABB box(Vec3 centre, double r) { return new AABB(centre, centre).inflate(r); }
}
