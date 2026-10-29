package com.newuniverse.nusmp.fairytail.storm;

import com.newuniverse.nusmp.balance.BalanceLaw;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Why: a storm hurts, moves and looks after creatures, and every one of those questions has the same few answers: who may be hurt (never the caster, an ally, a pet, a summon, a
 * creative or spectating player, an armour stand), who is a friend, where the ground is, and how a hit goes through the balance law. This is that and nothing else; the manager decides
 * WHEN, this decides WHO and HOW MUCH. Every area query is a cylinder and is capped at {@link StormMath#MAX_FOES} creatures, so one storm can never scan or hurt an unbounded
 * crowd. Server side only. The rules mirror the (removed) Fairy Tail skills' target rules.
 */
final class StormWorld {
    private StormWorld() {}

    private static final String OWNER_KEY = "nusmp_owner", RELIC_OWNER_KEY = "nusmp_relic_owner";

    // ------------------------------------------------------------------------------------------------------------------ who
    static boolean isSummon(Entity e) {
        var data = e.getPersistentData();
        return data.hasUUID(OWNER_KEY) || data.hasUUID(RELIC_OWNER_KEY);
    }

    /** Anything a storm of this player may hurt. */
    static boolean isFoe(ServerPlayer p, LivingEntity e) {
        if (e == p || !e.isAlive() || e.isSpectator() || e instanceof ArmorStand || e.isAlliedTo(p)) return false;
        if (e instanceof Player o) return StormConfig.hurtPlayers() && !o.isCreative() && p.canHarmPlayer(o);
        if (isSummon(e)) return false;
        if (e instanceof OwnableEntity own && own.getOwnerUUID() != null) return false;
        if (e instanceof Enemy || e.getType().getCategory() == MobCategory.MONSTER) return true;
        return e instanceof Mob m && m.getTarget() == p;
    }

    /** The caster, the players round them and the caster's own pets and summons: who the calm eye looks after. */
    static boolean isFriend(ServerPlayer p, LivingEntity e) {
        if (!e.isAlive() || e.isSpectator()) return false;
        if (e == p) return true;
        if (e instanceof Player o) return o.isAlliedTo(p) || o.getTeam() != null && o.getTeam() == p.getTeam();
        if (e instanceof OwnableEntity own && p.getUUID().equals(own.getOwnerUUID())) return true;
        var data = e.getPersistentData();
        return data.hasUUID(OWNER_KEY) && p.getUUID().equals(data.getUUID(OWNER_KEY));
    }

    /** Creatures in the cylinder of radius {@code r} round (x, z), from {@code y - 3} to {@code y + height}, nearest to the axis first, at most {@link StormMath#MAX_FOES}. */
    static List<LivingEntity> in(ServerPlayer owner, ServerLevel level, double x, double y, double z, double r, double height, boolean foes) {
        double rad = Mth.clamp(r, 0.5, 40.0);
        AABB box = new AABB(x - rad, y - 3.0, z - rad, x + rad, y + height, z + rad);
        List<LivingEntity> found = level.getEntitiesOfClass(LivingEntity.class, box, e -> {
            double dx = e.getX() - x, dz = e.getZ() - z;
            double reach = rad + e.getBbWidth() * 0.5;
            if (dx * dx + dz * dz > reach * reach) return false;
            return foes ? isFoe(owner, e) : isFriend(owner, e);
        });
        found.sort(Comparator.comparingDouble(e -> (e.getX() - x) * (e.getX() - x) + (e.getZ() - z) * (e.getZ() - z)));
        return found.size() > StormMath.MAX_FOES ? new ArrayList<>(found.subList(0, StormMath.MAX_FOES)) : found;
    }

    /** Foes within {@code r} of a point in all three directions (a bolt's mark, a hail stone's landing). */
    static List<LivingEntity> near(ServerPlayer owner, ServerLevel level, Vec3 at, double r) {
        List<LivingEntity> found = level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(r, r + 1.0, r),
                e -> isFoe(owner, e) && e.getBoundingBox().distanceToSqr(at) <= r * r);
        found.sort(Comparator.comparingDouble(e -> e.distanceToSqr(at)));
        return found.size() > StormMath.MAX_FOES ? new ArrayList<>(found.subList(0, StormMath.MAX_FOES)) : found;
    }

    // ------------------------------------------------------------------------------------------------------------------ ground
    /** Where the aimed point of the caster lands: the first block within {@code range} of the crosshair, else the full-range point. */
    static Vec3 aimPoint(ServerPlayer p, double range) {
        double r = Mth.clamp(range, 1.0, StormMath.MAX_AIM);
        Vec3 start = p.getEyePosition(), end = start.add(p.getViewVector(1.0F).scale(r));
        HitResult hit = p.serverLevel().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
    }

    /** The height of the ground under (x, z), searched down from {@code fromY + 2} for at most 24 blocks; {@code fromY} when there is none. */
    static double groundY(ServerLevel level, double x, double fromY, double z) {
        int bx = Mth.floor(x), bz = Mth.floor(z);
        int top = Mth.floor(fromY) + 2;
        int min = Math.max(level.getMinBuildHeight(), top - 26);
        for (int y = top; y >= min; y--) {
            BlockPos pos = new BlockPos(bx, y, bz);
            BlockState st = level.getBlockState(pos);
            if (!st.getCollisionShape(level, pos).isEmpty()) return y + st.getCollisionShape(level, pos).max(net.minecraft.core.Direction.Axis.Y);
        }
        return fromY;
    }

    // ------------------------------------------------------------------------------------------------------------------ hurting and moving
    /** One storm hit through the balance law (EP scaling, the PvP hit cap), scaled by the server's {@code damageScale}. Returns the health actually taken. */
    static float hit(ServerPlayer owner, LivingEntity target, double raw) {
        double scaled = raw * StormConfig.damageScale();
        if (owner == null || target == null || !target.isAlive() || scaled <= 0 || !isFoe(owner, target)) return 0f;
        float dmg = BalanceLaw.damage(owner, target, (float) scaled, 0.4);
        if (dmg <= 0f) return 0f;
        float before = target.getHealth();
        DamageSource src = owner.damageSources().indirectMagic(owner, owner);
        target.invulnerableTime = 0;          // the storm's own gaps and per-creature caps (StormHits) rule the rate, not the 0.5 s of vanilla invulnerability
        target.hurt(src, dmg);
        return Math.max(0f, before - target.getHealth());
    }

    /** Adds a velocity to a creature, limited by {@link StormMath#limitPush}, and tells its client. */
    static void push(LivingEntity e, double vx, double vy, double vz) {
        double[] v = StormMath.limitPush(vx, vy, vz);
        e.setDeltaMovement(e.getDeltaMovement().add(v[0], v[1], v[2]));
        if (v[1] > 0) e.fallDistance = 0f;
        e.hurtMarked = true;
    }
}
