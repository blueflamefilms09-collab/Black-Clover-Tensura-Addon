package com.newuniverse.nusmp.entity.veldanava.skills;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Foes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Why: Devastating Throw hurls a creature (a boss too) or a chunk of ground "like a high-speed projectile" without spawning any entity: the throw is one field of the {@code throw} kind that runs every
 * {@link HerculesRules#THROW_PULSE_TICKS} ticks for at most {@link HerculesRules#THROW_MAX_TICKS} ticks. A thrown creature is given the speed again at every pulse (so it flies a straight line whatever it is);
 * a chunk is a point that moves along the line and is stopped by the first block ({@code level.clip}, one ray a pulse). On the way, at most {@link HerculesRules#THROW_MAX_HITS} different creatures (each once) take
 * {@link HerculesRules#THROW_PATH_DAMAGE}; where it ends (a wall, the ground, the longest path, or the thrown creature dying) a blast of {@link HerculesRules#THROW_BLAST_RADIUS} blocks hurts at most
 * {@link HerculesRules#THROW_BLAST_MAX} creatures, the thrown creature takes the landing blow, and {@link HerculesTerrain} may dig a small crater (at most {@link HerculesRules#THROW_CRATER_BLOCKS} blocks).
 * One owner has at most one throw at a time (a new one replaces the old).
 *
 * <p>Worst case: per pulse one box query (radius {@link HerculesRules#THROW_HIT_RADIUS}) and one ray; 9 pulses a throw; at the end one box query, at most 12 blows, at most 60 blocks.
 */
final class HerculesThrow implements JudgmentFields.Behavior {
    static final String KIND = "throw";

    private final UUID thrown;                 // the creature that flies, or null for a chunk of ground
    private final Vec3 dir;
    private Vec3 pos;
    private int flown;
    private final Set<UUID> hit = new HashSet<>();

    HerculesThrow(UUID thrown, Vec3 start, Vec3 dir) {
        this.thrown = thrown;
        this.pos = start;
        this.dir = dir.normalize();
    }

    @Override public int period() { return HerculesRules.THROW_PULSE_TICKS; }

    @Override
    public void pulse(ServerPlayer owner, ServerLevel level, JudgmentFields.Field f) {
        flown += HerculesRules.THROW_PULSE_TICKS;
        if (thrown != null) {
            Entity e = level.getEntity(thrown);
            if (!(e instanceof LivingEntity t) || !t.isAlive()) { land(owner, level, f, null); return; }
            pos = t.position().add(0.0, t.getBbHeight() * 0.5, 0.0);
            sweep(owner, level, t);
            if (!t.isAlive()) { land(owner, level, f, null); return; }
            if (flown >= 4 && (t.horizontalCollision || t.verticalCollision || t.onGround()) || flown >= HerculesRules.THROW_MAX_TICKS) { land(owner, level, f, t); return; }
            t.setDeltaMovement(dir.scale(HerculesRules.THROW_SPEED));
            t.hurtMarked = true;
            t.fallDistance = 0.0f;
        } else {
            Vec3 next = pos.add(dir.scale(HerculesRules.THROW_SPEED * HerculesRules.THROW_PULSE_TICKS));
            BlockHitResult hr = level.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
            if (hr.getType() == HitResult.Type.BLOCK) {
                pos = hr.getLocation();
                sweep(owner, level, null);
                land(owner, level, f, null);
                return;
            }
            pos = next;
            sweep(owner, level, null);
            if (flown >= HerculesRules.THROW_MAX_TICKS) land(owner, level, f, null);
        }
    }

    /** The creatures on the way: each is hurt once, at most {@link HerculesRules#THROW_MAX_HITS} in all. */
    private void sweep(ServerPlayer owner, ServerLevel level, LivingEntity self) {
        if (hit.size() >= HerculesRules.THROW_MAX_HITS) return;
        double r = HerculesRules.THROW_HIT_RADIUS;
        List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, new AABB(pos, pos).inflate(r),
                x -> x != self && x != owner && x.isAlive() && !x.isSpectator() && Foes.isFoe(owner, x) && !hit.contains(x.getUUID()));
        for (LivingEntity x : near) {
            if (hit.size() >= HerculesRules.THROW_MAX_HITS) break;
            hit.add(x.getUUID());
            HerculesDamage.blow(owner, x, HerculesRules.THROW_PATH_DAMAGE, HerculesDamage.mastery(owner));
        }
    }

    /** The end of the flight: the landing blow, the blast, the crater. The field is over. */
    private void land(ServerPlayer owner, ServerLevel level, JudgmentFields.Field f, LivingEntity landed) {
        f.endsAt = 0L;
        Vec3 at = landed != null ? landed.position() : pos;
        double mastery = HerculesDamage.mastery(owner);
        if (landed != null && landed.isAlive()) {
            HerculesDamage.blow(owner, landed, HerculesRules.THROW_IMPACT_DAMAGE, mastery);
            landed.setDeltaMovement(Vec3.ZERO);
            landed.hurtMarked = true;
        }
        double r = HerculesRules.THROW_BLAST_RADIUS, r2 = r * r;
        List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(r),
                x -> x != landed && x != owner && x.isAlive() && !x.isSpectator() && Foes.isFoe(owner, x) && x.position().distanceToSqr(at) <= r2);
        int n = 0;
        for (LivingEntity x : near) {
            if (n++ >= HerculesRules.THROW_BLAST_MAX) break;
            HerculesDamage.blow(owner, x, HerculesRules.THROW_BLAST_DAMAGE, mastery);
            if (x.isAlive()) x.addEffect(new MobEffectInstance(JudgmentEffects.MOVEMENT_INTERFERENCE, BalanceLaw.controlTicks(x, HerculesRules.TREMOR_INTERFERENCE_TICKS), 0, false, true, true));
        }
        BlockPos ground = HerculesTerrain.under(at);
        HerculesTerrain.bowl(level, ground, HerculesRules.CRATER_RADIUS, 3, HerculesRules.THROW_CRATER_BLOCKS);
        HerculesFx.thud(level, at);
    }
}
