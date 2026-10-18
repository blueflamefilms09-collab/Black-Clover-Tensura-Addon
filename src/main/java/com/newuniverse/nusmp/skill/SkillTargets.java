package com.newuniverse.nusmp.skill;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Why: the old skills hit "everything living within N blocks": the caster's own wolves, other players' pets and summons, villagers and
 * allies all took the damage. This class is the one answer to "who may this skill hurt, heal or buff": a foe is a hostile creature (or one
 * already hunting the caster, or a player the caster may harm), never the caster, an ally, a pet, anybody's summon, a creative or
 * spectating player or an armour stand; allies are players and the caster's own creatures. Results are capped (24 foes) so no cast can scan
 * or hurt an unbounded crowd, and nothing here throws.
 */
final class SkillTargets {
    private SkillTargets() {}

    static final int MAX_FOES = 24;
    private static final double MAX_RADIUS = 48.0;

    /** True if the entity belongs to a summon system (this skill's cards or a relic's allies): nobody's AoE may hit them. */
    static boolean isSummon(Entity e) {
        var data = e.getPersistentData();
        return data.hasUUID(SummonerSkill.OWNER_KEY) || data.hasUUID("nusmp_relic_owner");
    }

    /** Anything this player may hurt by an area effect. */
    static boolean isFoe(ServerPlayer p, LivingEntity e) {
        if (e == p || !e.isAlive() || e.isSpectator() || e instanceof ArmorStand || e.isAlliedTo(p)) return false;
        if (e instanceof Player o) return !o.isCreative() && p.canHarmPlayer(o);
        if (isSummon(e)) return false;
        if (e instanceof OwnableEntity own && own.getOwnerUUID() != null) return false;   // a tamed animal is nobody's target
        if (e instanceof Enemy || e.getType().getCategory() == MobCategory.MONSTER) return true;
        return e instanceof Mob m && m.getTarget() == p;
    }

    /** Anything this player may hurt by a deliberate aimed single-target skill (also non-hostile creatures the player points at). */
    static boolean isAimable(ServerPlayer p, LivingEntity e) {
        if (e == p || !e.isAlive() || e.isSpectator() || e instanceof ArmorStand || e.isAlliedTo(p)) return false;
        if (e instanceof Player o) return !o.isCreative() && p.canHarmPlayer(o);
        if (isSummon(e)) return false;
        return !(e instanceof OwnableEntity own && own.getOwnerUUID() != null);
    }

    /** Foes within a radius of a point, nearest first, at most {@link #MAX_FOES}. */
    static List<LivingEntity> foes(ServerPlayer p, Vec3 center, double radius) {
        if (p == null || center == null || !p.isAlive()) return List.of();
        double r = Mth.clamp(radius, 0.5, MAX_RADIUS);
        List<LivingEntity> found = p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(r),
                e -> isFoe(p, e) && e.distanceToSqr(center) <= r * r);
        found.sort(Comparator.comparingDouble(e -> e.distanceToSqr(center)));
        return found.size() > MAX_FOES ? new ArrayList<>(found.subList(0, MAX_FOES)) : found;
    }

    /** Foes in a cone in front of the player: within {@code range} and with cos(angle) at least {@code minDot} to the look direction. */
    static List<LivingEntity> foesInCone(ServerPlayer p, double range, double minDot) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getViewVector(1.0F);
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : foes(p, p.position(), range)) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            double len = to.length();
            if (len > range + 1.0) continue;
            if (len < 0.01 || to.scale(1.0 / len).dot(look) >= minDot) out.add(e);
        }
        return out;
    }

    /** Foes within {@code width} of the line segment from-to (a beam, a charge). */
    static List<LivingEntity> foesAlong(ServerPlayer p, Vec3 from, Vec3 to, double width) {
        if (p == null || from == null || to == null || !p.isAlive()) return List.of();
        double w = Mth.clamp(width, 0.3, 6.0);
        AABB box = new AABB(from, to).inflate(w + 0.5);
        Vec3 seg = to.subtract(from);
        double len2 = Math.max(1.0e-6, seg.lengthSqr());
        List<LivingEntity> found = p.serverLevel().getEntitiesOfClass(LivingEntity.class, box, e -> {
            if (!isFoe(p, e)) return false;
            Vec3 c = e.getBoundingBox().getCenter();
            double t = Mth.clamp(c.subtract(from).dot(seg) / len2, 0.0, 1.0);
            Vec3 nearest = from.add(seg.scale(t));
            return nearest.distanceTo(c) <= w + e.getBbWidth() * 0.5;
        });
        found.sort(Comparator.comparingDouble(e -> e.distanceToSqr(from)));
        return found.size() > MAX_FOES ? new ArrayList<>(found.subList(0, MAX_FOES)) : found;
    }

    /** Players (alive, not spectating) around a point, the caster included. */
    static List<ServerPlayer> playersAround(ServerPlayer p, Vec3 center, double radius) {
        if (p == null || center == null) return List.of();
        double r = Mth.clamp(radius, 0.5, MAX_RADIUS);
        return p.serverLevel().getEntitiesOfClass(ServerPlayer.class, new AABB(center, center).inflate(r),
                o -> o.isAlive() && !o.isSpectator() && o.distanceToSqr(center) <= r * r);
    }

    /** True if the creature is on this player's side: the player, or a tamed animal / summon the player owns. */
    static boolean isOwnedBy(Entity e, UUID owner) {
        if (e instanceof OwnableEntity own && owner.equals(own.getOwnerUUID())) return true;
        var data = e.getPersistentData();
        return data.hasUUID(SummonerSkill.OWNER_KEY) && owner.equals(data.getUUID(SummonerSkill.OWNER_KEY));
    }

    /** Allies to heal or buff: every player around, plus the caster's own tamed animals and summons (at most 24 entries). */
    static List<LivingEntity> alliesAround(ServerPlayer p, Vec3 center, double radius) {
        if (p == null || center == null) return List.of();
        double r = Mth.clamp(radius, 0.5, MAX_RADIUS);
        UUID me = p.getUUID();
        List<LivingEntity> found = p.serverLevel().getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(r),
                e -> e.isAlive() && !e.isSpectator() && e.distanceToSqr(center) <= r * r
                        && ((e instanceof Player) || isOwnedBy(e, me)));
        found.sort(Comparator.comparingDouble(e -> e.distanceToSqr(center)));
        return found.size() > MAX_FOES ? new ArrayList<>(found.subList(0, MAX_FOES)) : found;
    }

    /** The living thing under the crosshair that this player may hurt, or null. */
    static LivingEntity lookFoe(ServerPlayer p, double range) {
        LivingEntity e = lookLiving(p, range);
        return e != null && isAimable(p, e) ? e : null;
    }

    /** The ally (a player, or one of the caster's creatures) under the crosshair, or null. */
    static LivingEntity lookAlly(ServerPlayer p, double range) {
        LivingEntity e = lookLiving(p, range);
        if (e == null) return null;
        return e instanceof Player || isOwnedBy(e, p.getUUID()) ? e : null;
    }

    /** The first living thing the crosshair passes through (blocks stop it), whoever it is. */
    static LivingEntity lookLiving(ServerPlayer p, double range) {
        Vec3 start = p.getEyePosition();
        Vec3 end = DMUtil.lookPoint(p, range);
        AABB box = p.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(p.serverLevel(), p, start, end, box,
                e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator() && e != p);
        return hit == null ? null : (LivingEntity) hit.getEntity();
    }
}
