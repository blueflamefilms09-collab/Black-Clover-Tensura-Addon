package com.newuniverse.nusmp.heroweapon.weapon.pierce;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Why: a long spear and a bow that reaches across a field will, sooner or later, have a villager, a tamed wolf, a friend or a creative player in the
 * line, and the owner's rule is plain: never act on creative or spectator players, allies, the wielder's own summons and tamed pets, or the wielder. This is
 * the one place that decides who a technique may strike, so the Spear and the Bow cannot disagree. A foe is a monster, a mob that is fighting the wielder,
 * a boss, or a player the wielder may harm (not creative, not spectating, not on their team); everything else (animals, villagers, pets, armour stands)
 * is spared by the techniques (the plain melee blow is unchanged). The searches are bounded (at most twelve foes), sorted nearest first, and need line of
 * sight where a technique says so.
 */
public final class Foes {
    private Foes() {}

    /** The most foes any search returns (the owner's cap on targets of one technique). */
    public static final int MAX = SkillRow.MAX_TARGETS;

    /** True when the wielder's techniques may strike this entity. */
    public static boolean valid(HeroCtx c, LivingEntity e) {
        ServerPlayer p = c.player;
        if (e == null || e == p || !e.isAlive() || e.isSpectator() || e instanceof ArmorStand) return false;
        if (e.isAlliedTo(p)) return false;
        if (e instanceof Player other) return !other.isCreative() && p.canHarmPlayer(other);
        if (e instanceof OwnableEntity own && p.getUUID().equals(own.getOwnerUUID())) return false;
        if (e instanceof Enemy || e.getType().getCategory() == MobCategory.MONSTER) return true;
        if (e instanceof Mob m && m.getTarget() == p) return true;
        return BalanceLaw.isBoss(e);
    }

    /** The foes within {@code r} of {@code centre}, nearest to the centre first, at most {@code max} (and never more than {@link #MAX}). */
    public static List<LivingEntity> around(HeroCtx c, Vec3 centre, double r, int max) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : c.around(centre, r)) if (valid(c, e)) out.add(e);
        out.sort(Comparator.comparingDouble((LivingEntity e) -> e.distanceToSqr(centre)));
        return trim(out, max);
    }

    /** The foes within {@code width} of the segment a-b, nearest to a first (by where along the segment they stand), at most {@code max}. */
    public static List<LivingEntity> along(HeroCtx c, Vec3 a, Vec3 b, double width, int max) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : c.along(a, b, width)) if (valid(c, e)) out.add(e);
        out.sort(Comparator.comparingDouble((LivingEntity e) -> {
            Vec3 m = e.getBoundingBox().getCenter();
            return Geo.segmentParam(a.x, a.y, a.z, b.x, b.y, b.z, m.x, m.y, m.z);
        }));
        return trim(out, max);
    }

    /** True when no block stands between the point and the centre or the eyes of the entity. */
    public static boolean sees(HeroCtx c, Vec3 from, LivingEntity e) {
        return clear(c, from, e.getBoundingBox().getCenter()) || clear(c, from, e.getEyePosition());
    }

    private static boolean clear(HeroCtx c, Vec3 from, Vec3 to) {
        return c.level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, c.player)).getType() == HitResult.Type.MISS;
    }

    /**
     * The nearest foe that the wielder can see within {@code range} of {@code apex} and within {@code halfAngleDeg} of the direction {@code axis} (a unit
     * vector), or null. Used by the Spear's aim assist and by the techniques that choose a target for the wielder.
     */
    public static LivingEntity nearestInCone(HeroCtx c, Vec3 apex, Vec3 axis, double range, double halfAngleDeg) {
        double min = Math.cos(Math.toRadians(Geo.clamp(halfAngleDeg, 0.0, 180.0)));
        for (LivingEntity e : around(c, apex, range, MAX)) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(apex);
            if (to.lengthSqr() > 1.0E-6 && to.normalize().dot(axis) < min) continue;
            if (sees(c, apex, e)) return e;
        }
        return null;
    }

    /**
     * The foe that is most nearly straight ahead: within {@code range} of {@code apex} and {@code halfAngleDeg} of the direction {@code axis} (a unit vector),
     * not behind a block, and of those the one whose direction is closest to the axis. Null when there is none. The techniques that lock onto "the foe under
     * the crosshair" use it, so a foe slightly off the centre of the screen still counts.
     */
    public static LivingEntity mostAligned(HeroCtx c, Vec3 apex, Vec3 axis, double range, double halfAngleDeg) {
        double min = Math.cos(Math.toRadians(Geo.clamp(halfAngleDeg, 0.0, 180.0)));
        LivingEntity best = null;
        double bestCos = -2.0;
        for (LivingEntity e : around(c, apex, range, MAX)) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(apex);
            if (to.lengthSqr() < 1.0E-6) continue;
            double cos = to.normalize().dot(axis);
            if (cos < min || cos <= bestCos || !sees(c, apex, e)) continue;
            best = e;
            bestCos = cos;
        }
        return best;
    }

    /**
     * The foe closest to the wielder along the line of sight: the first one within {@code width} of the look ray out to {@code range} that is not behind a
     * block. Null when the ray meets nothing.
     */
    public static LivingEntity aimed(HeroCtx c, double range, double width) {
        Vec3 eye = c.eye(), end = Rays.clipEnd(c, eye, eye.add(c.look().scale(range)));
        List<LivingEntity> hits = along(c, eye, end, width, MAX);
        return hits.isEmpty() ? null : hits.get(0);
    }

    /** A hit through the balance law on a foe only; false for anyone else or when the law refused it. */
    public static boolean hit(HeroCtx c, LivingEntity target, float raw) {
        return raw > 0f && valid(c, target) && c.hit(target, raw);
    }

    private static List<LivingEntity> trim(List<LivingEntity> in, int max) {
        int cap = Math.max(0, Math.min(max, MAX));
        return in.size() > cap ? new ArrayList<>(in.subList(0, cap)) : in;
    }
}
