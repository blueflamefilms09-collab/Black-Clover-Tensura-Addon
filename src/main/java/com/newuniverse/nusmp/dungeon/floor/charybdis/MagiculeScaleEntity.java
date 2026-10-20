package com.newuniverse.nusmp.dungeon.floor.charybdis;

import com.newuniverse.nusmp.entity.zogratis.VolleyBudget;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.item.MagebaneShieldArts;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxOld;
import com.newuniverse.nusmp.particle.fx.FxThemes;

/**
 * One magicule scale of the Charybdis Spawn: a small flat glowing shard that flies slowly through the water ({@link CharybdisTable#SCALE_SPEED}
 * blocks a tick, in a straight line, no drag) so it can be read and answered. It is a real projectile (the other parry spells of the mod see it
 * too) and it is meant to be turned, not tanked ({@link ParryRules}): a player who raises a shield or stands in the Magebane Shield's parry
 * window when it arrives sends it back at the leviathan at half again its speed, where it wounds for a share of the leviathan's health, and a
 * player who strikes the scale itself sends it back too. Anything else is an ordinary hit: bounded by the boss's damage law and by what one
 * volley may take from one victim ({@link VolleyBudget}), with Slowness on top. A scale that misses fades after six seconds, one that has been
 * sent back never hurts a player, and nothing here changes a block or survives a save.
 */
public class MagiculeScaleEntity extends AbstractHurtingProjectile {
    private static final EntityDataAccessor<Boolean> RETURNED = SynchedEntityData.defineId(MagiculeScaleEntity.class, EntityDataSerializers.BOOLEAN);
    private static final DustParticleOptions GLINT = new DustParticleOptions(new Vector3f(0.25f, 0.95f, 1.0f), 0.9f);
    private static final DustParticleOptions GLINT_BACK = new DustParticleOptions(new Vector3f(1.0f, 0.86f, 0.45f), 0.9f);

    private int life;
    private UUID bossId;
    private VolleyBudget budget;
    private ParryRules.Pool pool;
    private int slowTicks = 60, slowLevel;
    /** The victims whose share of the volley was spent when this scale reached them: it flies on through them. */
    private final Set<Integer> passed = new HashSet<>();

    public MagiculeScaleEntity(EntityType<? extends MagiculeScaleEntity> type, Level level) {
        super(type, level);
        this.accelerationPower = 0.0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(RETURNED, false);
    }

    /** Has it been sent back (by a shield, a parry or a blow)? Synced, so the renderer can show it. */
    public boolean isReturned() { return entityData.get(RETURNED); }

    /**
     * Fires one scale of a volley from the boss. 'dir' is its direction (any length but zero); 'budget' and 'pool' are shared by the whole
     * volley. Null if the scale could not be made.
     */
    public static MagiculeScaleEntity launch(ServerLevel sl, ZogratisBoss boss, Vec3 from, Vec3 dir, VolleyBudget budget, ParryRules.Pool pool) {
        MagiculeScaleEntity s = CharybdisEntities.MAGICULE_SCALE.get().create(sl);
        if (s == null) return null;
        Vec3 d = dir.lengthSqr() < 1e-6 ? boss.getViewVector(1f) : dir.normalize();
        s.setPos(from.x, from.y, from.z);
        s.setOwner(boss);
        s.bossId = boss.getUUID();
        s.budget = budget;
        s.pool = pool;
        s.slowTicks = CharybdisTable.slowTicks(boss.power());
        s.slowLevel = CharybdisTable.slowLevel(boss.power());
        s.setDeltaMovement(d.scale(CharybdisTable.SCALE_SPEED));
        s.setYRot(Swim.yawOf(d.x, d.z));
        s.setXRot(Swim.pitchOf(d.x, d.y, d.z));
        s.yRotO = s.getYRot();
        s.xRotO = s.getXRot();
        sl.addFreshEntity(s);
        return s;
    }

    // ---------------------------------------------------------------- flight
    @Override protected float getInertia() { return 1.0f; }

    @Override protected float getLiquidInertia() { return 1.0f; }

    @Override protected boolean shouldBurn() { return false; }

    @Override protected ParticleOptions getTrailParticle() { return isReturned() ? GLINT_BACK : GLINT; }

    /** A player may strike it (like a fireball), a scale already sent back may not be struck again. */
    @Override public boolean isPickable() { return !isRemoved() && !isReturned(); }

    @Override public float getPickRadius() { return 0.7f; }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || isRemoved()) return;
        life++;
        if (!isReturned() && getOwner() instanceof Player) entityData.set(RETURNED, true);   // another parry (the Magebane Shield's) turned it: it is the player's now
        if (life > CharybdisTable.SCALE_LIFE) {
            if (level() instanceof ServerLevel sl) Fx.spray(sl, FxThemes.BOSS_CHARYBDIS, FxOld.END_ROD, getX(), getY(), getZ(), 4, 0.1, 0.1, 0.1, 0.02);
            discard();
        }
    }

    // ---------------------------------------------------------------- who it can touch
    /** A scale that is still the boss's touches the boss's foes; one that was sent back touches only the boss that fired it. Never another scale. */
    @Override
    protected boolean canHitEntity(Entity target) {
        if (target instanceof MagiculeScaleEntity || target == getOwner() || passed.contains(target.getId())) return false;
        if (!super.canHitEntity(target)) return false;
        if (isReturned()) return bossId != null && bossId.equals(target.getUUID());
        return getOwner() instanceof ZogratisBoss boss && boss.isFoe(target);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (!(level() instanceof ServerLevel sl) || isRemoved()) return;
        Entity target = hit.getEntity();
        if (isReturned()) {
            woundBoss(sl, target);
            return;
        }
        if (!(target instanceof LivingEntity victim) || !(getOwner() instanceof ZogratisBoss boss) || !boss.isAlive()) {
            discard();
            return;
        }
        ParryRules.Deflect how = deflectOf(victim, hit.getLocation());
        if (how != ParryRules.Deflect.NONE) {
            turnBack(sl, victim, how);
            return;
        }
        if (budget != null && !budget.tryHit(victim.getId())) {          // this volley has had its share of this one: the scale flies on
            passed.add(victim.getId());
            return;
        }
        if (boss.strike(victim, CharybdisTable.SCALE, true))
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowTicks, slowLevel));
        Fx.spray(sl, FxThemes.BOSS_CHARYBDIS, FxOld.GLOW, getX(), getY(), getZ(), 8, 0.2, 0.2, 0.2, 0.05);
        sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.HOSTILE, 1.0f, 1.4f);
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        if (!(level() instanceof ServerLevel sl) || isRemoved()) return;
        Fx.spray(sl, FxThemes.BOSS_CHARYBDIS, FxOld.GLOW, getX(), getY(), getZ(), 4, 0.1, 0.1, 0.1, 0.03);
        discard();
    }

    // ---------------------------------------------------------------- the answers
    /** What a victim's guard does to a scale that reached them at 'at': a shield or the parry window, and only for what comes from the front. */
    private ParryRules.Deflect deflectOf(LivingEntity victim, Vec3 at) {
        boolean blocking = victim.isBlocking();
        boolean parry = victim instanceof Player p && MagebaneShieldArts.parryOpen(p);
        if (!blocking && !parry) return ParryRules.Deflect.NONE;
        Vec3 look = victim.getViewVector(1f), to = at.subtract(victim.position());
        return ParryRules.onImpact(blocking, parry, ParryRules.facing(look.x, 0, look.z, to.x, 0, to.z));        // like a shield: flat, front half
    }

    /** A blow of a player's at the scale itself (a sword, an arrow, a spell that hurts it) sends it back. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isRemoved() || !(level() instanceof ServerLevel sl)) return false;
        if (!(source.getEntity() instanceof Player p) || !p.isAlive() || !ParryRules.strikeDeflects(true, isReturned())) return false;
        turnBack(sl, p, ParryRules.Deflect.NONE);
        return true;
    }

    /** Sends the scale back at the boss that fired it, owned by whoever turned it, faster; it was a threat and is now a weapon. */
    private void turnBack(ServerLevel sl, LivingEntity by, ParryRules.Deflect how) {
        Entity boss = bossId == null ? null : sl.getEntity(bossId);
        if (boss == null || !boss.isAlive()) {
            discard();
            return;
        }
        Vec3 to = boss.getBoundingBox().getCenter().subtract(position());
        if (to.lengthSqr() < 1e-4) to = by.getViewVector(1f);
        setOwner(by);
        entityData.set(RETURNED, true);
        Vec3 v = to.normalize().scale(ParryRules.returnSpeed(getDeltaMovement().length()));
        setDeltaMovement(v);
        setYRot(Swim.yawOf(v.x, v.z));
        setXRot(Swim.pitchOf(v.x, v.y, v.z));
        hurtMarked = true;
        passed.clear();
        life = Math.min(life, CharybdisTable.SCALE_LIFE - 60);          // at least three more seconds to reach it
        Fx.spray(sl, FxThemes.BOSS_CHARYBDIS, FxOld.CRIT, getX(), getY(), getZ(), 10, 0.2, 0.2, 0.2, 0.25);
        sl.playSound(null, blockPosition(), how == ParryRules.Deflect.PARRY ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.2f, how == ParryRules.Deflect.PARRY ? 1.6f : 1.3f);
    }

    /** A scale that was sent back reaches the leviathan: a real wound, from the volley's pool. */
    private void woundBoss(ServerLevel sl, Entity target) {
        if (!(target instanceof LivingEntity boss) || !boss.isAlive() || !boss.getUUID().equals(bossId)) {
            discard();
            return;
        }
        double want = ParryRules.bonus(boss.getMaxHealth());
        double got = pool == null ? want : pool.take(want);
        if (got > 0.01) {
            boss.invulnerableTime = 0;                                   // every scale of a volley counts, not just the first of a tick
            boss.hurt(sl.damageSources().indirectMagic(this, getOwner()), (float) got);
        }
        Fx.spray(sl, FxThemes.BOSS_CHARYBDIS, FxOld.FLASH, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
        Fx.spray(sl, FxThemes.BOSS_CHARYBDIS, FxOld.GLOW, getX(), getY(), getZ(), 14, 0.5, 0.5, 0.5, 0.1);
        sl.playSound(null, blockPosition(), got > 0.01 ? SoundEvents.TRIDENT_HIT : SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.HOSTILE, 1.4f, 1.0f);
        discard();
    }
}
