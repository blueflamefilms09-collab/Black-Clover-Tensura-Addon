package com.newuniverse.nusmp.heroweapon.weapon.vassalkit;

import com.newuniverse.nusmp.multiverse.Squads;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
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
import java.util.Optional;
import java.util.UUID;

/**
 * Why: every technique of the five vassal weapons must answer "who may I strike and who may I help" the same way, and the answer is stricter than the shared
 * {@code HeroCtx.around} (which returns any living thing that is not on the wielder's team, villagers and passive animals included). A foe here is what the Djinn weapons
 * already use: a hostile monster, a creature that is hunting the wielder or an ally, or a player the wielder may harm (never creative or spectator players, never
 * the wielder's own pets and summons, never somebody else's tamed animal). An ally is the wielder, their team, their party, their own pets and summons, and
 * any player they may not harm. Targets are returned nearest first and capped, so a technique can never touch more than the budget allows. Server thread only.
 */
public final class Foes {
    private Foes() {}

    /** The marker the relic summons carry in their persistent data; such a creature belongs to somebody and is not a target. */
    private static final String RELIC_OWNER = "nusmp_relic_owner";

    // ------------------------------------------------------------------ who is who
    public static boolean isFoe(ServerPlayer p, LivingEntity e) {
        if (e == p || !e.isAlive() || e.isSpectator() || e instanceof ArmorStand || e.isAlliedTo(p)) return false;
        if (e instanceof Player o) return !o.isCreative() && p.canHarmPlayer(o);
        if (e instanceof OwnableEntity own && own.getOwnerUUID() != null) return false;          // a tamed or summoned creature belongs to its owner's side
        if (e.getPersistentData().hasUUID(RELIC_OWNER)) return false;
        if (e instanceof Enemy || e.getType().getCategory() == MobCategory.MONSTER) return true;
        if (!(e instanceof Mob m)) return false;
        LivingEntity hunted = m.getTarget();
        return hunted != null && (hunted == p || isAlly(p, hunted));
    }

    public static boolean isAlly(ServerPlayer p, LivingEntity e) {
        if (e == p || !e.isAlive() || e.isSpectator()) return e == p;
        if (p.isAlliedTo(e) || e.isAlliedTo(p)) return true;           // both ways: a summon says "I am allied to my owner" on its own side
        if (e instanceof Player o) return Squads.partyMembers(p).contains(o.getUUID()) || !p.canHarmPlayer(o);
        if (e instanceof OwnableEntity own) {
            UUID owner = own.getOwnerUUID();
            if (owner == null) return false;
            if (owner.equals(p.getUUID())) return true;
            return p.serverLevel().getPlayerByUUID(owner) instanceof Player ownerPlayer && isAlly(p, ownerPlayer);
        }
        return false;
    }

    /**
     * A companion is an ally the wielder can count on without asking: their team, their party, and their own pets and summons (never a stranger who is merely not
     * allowed to be harmed). Techniques that MOVE somebody else (Ariadne's Thread) use this for the people nobody chose by hand.
     */
    public static boolean isCompanion(ServerPlayer p, LivingEntity e) {
        if (e == p || !e.isAlive() || e.isSpectator()) return false;
        if (p.isAlliedTo(e) || e.isAlliedTo(p)) return true;
        if (e instanceof Player o) return Squads.partyMembers(p).contains(o.getUUID());
        return e instanceof OwnableEntity own && p.getUUID().equals(own.getOwnerUUID());
    }

    // ------------------------------------------------------------------ searches
    /** Foes within {@code r} of {@code c}, nearest first, at most {@code max}. */
    public static List<LivingEntity> around(ServerPlayer p, Vec3 c, double r, int max) {
        double rr = r * r;
        List<LivingEntity> found = new ArrayList<>(p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r), e -> e.distanceToSqr(c) <= rr && isFoe(p, e)));
        return nearest(found, c, max);
    }

    /** Allies (the wielder included) within {@code r} of {@code c}, nearest first, at most {@code max}. */
    public static List<LivingEntity> alliesAround(ServerPlayer p, Vec3 c, double r, int max) {
        double rr = r * r;
        List<LivingEntity> found = new ArrayList<>(p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r), e -> e.distanceToSqr(c) <= rr && isAlly(p, e)));
        return nearest(found, c, max);
    }

    /** Foes within {@code width} of the segment a-b, nearest to a first, at most {@code max}. The caller clips b to the first wall (see {@link #wallEnd}). */
    public static List<LivingEntity> along(ServerPlayer p, Vec3 a, Vec3 b, double width, int max) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(a, b).inflate(width + 1.0), e -> isFoe(p, e))) {
            Vec3 c = e.getBoundingBox().getCenter();
            if (Geo.segDist(c.x, c.y, c.z, a.x, a.y, a.z, b.x, b.y, b.z) <= width + e.getBbWidth() / 2.0) out.add(e);
        }
        return nearest(out, a, max);
    }

    /** Foes within {@code range} of the wielder inside a cone of {@code halfAngleDeg} round the look direction, nearest first, at most {@code max}. */
    public static List<LivingEntity> cone(ServerPlayer p, double range, double halfAngleDeg, int max) {
        return coneFrom(p, p.getEyePosition(), p.getViewVector(1f), range, halfAngleDeg, max);
    }

    /** Foes within {@code range} of {@code origin} inside a cone of {@code halfAngleDeg} round the direction {@code look}, nearest first, at most {@code max} (an echo strikes from the image, not from the wielder). */
    public static List<LivingEntity> coneFrom(ServerPlayer p, Vec3 origin, Vec3 look, double range, double halfAngleDeg, int max) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : around(p, origin, range, 64)) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(origin);
            if (Geo.inCone(to.x, to.y, to.z, look.x, look.y, look.z, halfAngleDeg)) out.add(e);
        }
        return nearest(out, origin, max);
    }

    /** The point on the ground under {@code at}, looking down at most {@code maxDrop} blocks; {@code at} itself when there is no ground in reach. */
    public static Vec3 groundBelow(ServerPlayer p, Vec3 at, double maxDrop) {
        HitResult floor = p.level().clip(new ClipContext(at.add(0, 0.5, 0), at.add(0, -maxDrop, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return floor.getType() == HitResult.Type.MISS ? at : floor.getLocation();
    }

    /** The end of the ray from {@code from} to {@code to} where the first solid block stops it (the far end when nothing does). */
    public static Vec3 wallEnd(ServerPlayer p, Vec3 from, Vec3 to) {
        HitResult wall = p.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return wall.getType() == HitResult.Type.MISS ? to : wall.getLocation();
    }

    /** The first foe under the crosshair within {@code range}; a wall in between stops the look. Null when there is none. */
    public static LivingEntity target(ServerPlayer p, double range) {
        return ray(p, p.getEyePosition(), p.getViewVector(1f), range, 0.3);
    }

    /** The first foe on the ray from {@code origin} along the unit direction {@code dir}, {@code width} blocks fat; a wall stops the ray. Null when there is none. */
    public static LivingEntity ray(ServerPlayer p, Vec3 origin, Vec3 dir, double range, double width) {
        Vec3 end = wallEnd(p, origin, origin.add(dir.scale(range)));
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (LivingEntity e : p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(origin, end).inflate(width + 1.0), e -> isFoe(p, e))) {
            double d = entryDistanceSqr(e.getBoundingBox().inflate(width), origin, end);
            if (d < bestD) {
                bestD = d;
                best = e;
            }
        }
        return best;
    }

    /** The first ally (never the wielder) under the crosshair within {@code range}; a wall stops the look. Null when there is none. */
    public static LivingEntity allyTarget(ServerPlayer p, double range) {
        Vec3 origin = p.getEyePosition(), dir = p.getViewVector(1f);
        Vec3 end = wallEnd(p, origin, origin.add(dir.scale(range)));
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (LivingEntity e : p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(origin, end).inflate(1.3), e -> e != p && isAlly(p, e))) {
            double d = entryDistanceSqr(e.getBoundingBox().inflate(0.3), origin, end);
            if (d < bestD) {
                bestD = d;
                best = e;
            }
        }
        return best;
    }

    /** The squared distance from {@code origin} to where the segment origin-end enters the box (0 when it starts inside); infinite when it misses. */
    private static double entryDistanceSqr(AABB box, Vec3 origin, Vec3 end) {
        if (box.contains(origin)) return 0.0;
        Optional<Vec3> hit = box.clip(origin, end);
        return hit.isPresent() ? hit.get().distanceToSqr(origin) : Double.MAX_VALUE;
    }

    /** The living thing with this id in the wielder's level, or null. */
    public static LivingEntity byId(ServerLevel level, UUID id) {
        return id != null && level.getEntity(id) instanceof LivingEntity living ? living : null;
    }

    private static List<LivingEntity> nearest(List<LivingEntity> list, Vec3 c, int max) {
        list.sort(Comparator.comparingDouble(e -> e.distanceToSqr(c)));
        return list.size() > max ? new ArrayList<>(list.subList(0, Math.max(0, max))) : list;
    }
}
