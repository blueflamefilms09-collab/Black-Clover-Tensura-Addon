package com.newuniverse.nusmp.entity.riven;

import com.newuniverse.nusmp.entity.NUEntities;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * A story construct Riven manifests (Story Manifestation): a weapon that fights for him, a bull-crest shield that stands between him
 * and his target, or a pale clone of him. Short lived, small, and blocked by the same rules as anything else: Tensura immunities and
 * anti-magic apply to its blows. Allied with Riven and with each other.
 */
public class StoryConstructEntity extends Monster {
    public static final int WEAPON = 0, SHIELD = 1, CLONE = 2, AVATAR = 3;
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(StoryConstructEntity.class, EntityDataSerializers.INT);
    private UUID owner;
    private int life = 400;
    private int avatarActionAt = -1;
    private int avatarFieldUntil;
    private long truthMirrorAt;

    public StoryConstructEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 24).add(Attributes.ATTACK_DAMAGE, 5).add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.FOLLOW_RANGE, 40).add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(KIND, WEAPON);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public int kind() { return entityData.get(KIND); }
    public boolean isAvatar() { return kind() == AVATAR; }
    public void breakAvatar() {
        if (!isAvatar()) return;
        if (level() instanceof ServerLevel sl)
            VfxSpawn.send(sl, VfxShape.MIRROR_SHATTER, position(), position().add(0, 1, 0), 0xFFD5C8FF, 10, 1.2f);
        discard();
    }

    public static StoryConstructEntity spawn(ServerLevel sl, RivenBossEntity boss, int kind, Vec3 at) {
        StoryConstructEntity e = NUEntities.STORY_CONSTRUCT.get().create(sl);
        if (e == null) return null;
        e.owner = boss.getUUID();
        e.entityData.set(KIND, kind);
        e.moveTo(at.x, at.y, at.z, boss.getYRot(), 0);
        if (kind == SHIELD) { e.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40); e.setHealth(40); e.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(2); }
        if (kind == CLONE) { e.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(7); e.life = 300; }
        sl.addFreshEntity(e);
        VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE_EXPLOSION, at, at.add(0, 1, 0), 0xFF8A6AFF, 20, 0.8f);
        return e;
    }

    public static StoryConstructEntity spawnAvatar(ServerLevel sl, RivenBossEntity boss, String name, Vec3 at) {
        StoryConstructEntity avatar = spawn(sl, boss, AVATAR, at);
        if (avatar == null) return null;
        avatar.life = 240;
        avatar.setCustomName(net.minecraft.network.chat.Component.literal(name));
        avatar.setCustomNameVisible(true);
        avatar.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(8);
        return avatar;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (tickCount % 3 == 0) level().addParticle(ParticleTypes.ENCHANT, getX() + (random.nextDouble() - 0.5), getY() + 0.8 + random.nextDouble() * 0.6, getZ() + (random.nextDouble() - 0.5), 0, 0.05, 0);
            return;
        }
        if (--life <= 0 || ownerBoss() == null) { discard(); return; }
        RivenBossEntity boss = ownerBoss();
        if (getTarget() == null && boss.getTarget() != null && boss.getTarget().isAlive()) setTarget(boss.getTarget());
        if (kind() == SHIELD && boss.getTarget() != null && tickCount % 10 == 0) {          // stands between Riven and his target
            Vec3 mid = boss.position().add(boss.getTarget().position().subtract(boss.position()).normalize().scale(2.2));
            getNavigation().moveTo(mid.x, mid.y, mid.z, 1.3);
        }
        if (isAvatar()) tickAvatar(boss, (ServerLevel) level());
    }

    private void tickAvatar(RivenBossEntity boss, ServerLevel level) {
        String name = getCustomName() == null ? "" : getCustomName().getString();
        if ("Kami Tenchi".equals(name)) {
            for (Player player : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(24))) {
                if (player.isFallFlying()) player.stopFallFlying();
            }
        }
        if ("Lord of Nightmares".equals(name) && tickCount < avatarFieldUntil) {
            for (Player player : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(4))) {
                if (player.isAlive()) MarquisStatus.markEvilEye(player, 2);
            }
        }
        if (avatarActionAt < 0 && tickCount % 50 == 1) {
            avatarActionAt = tickCount + 8;
            VfxSpawn.send(level, VfxShape.THREAD_LINE, position(), position().add(0, 1.5, 0),
                    0xFFD9B8FF, 8, 1.1f);
        }
        if (avatarActionAt < 0 || tickCount < avatarActionAt) return;
        avatarActionAt = -1;
        LivingEntity target = boss.getTarget();
        switch (name) {
            case "Zeus" -> {
                if (target != null && target.isAlive() && distanceToSqr(target) <= 16
                        && getLookAngle().normalize().dot(target.getEyePosition().subtract(getEyePosition()).normalize()) > 0.45) {
                    target.hurt(damageSources().mobAttack(this), 12f);
                    target.knockback(0.7, getX() - target.getX(), getZ() - target.getZ());
                }
                VfxSpawn.send(level, VfxShape.ANTI_MAGIC_SLASH, getEyePosition(), getEyePosition().add(getLookAngle().scale(4)),
                        0xFF85C8FF, 8, 0.9f);
            }
            case "Beerus" -> {
                VfxSpawn.send(level, VfxShape.MAGIC_CIRCLE_EXPLOSION, position(), position().add(0, 0.1, 0),
                        0xFFB37AFF, 10, 1.4f);
                for (Player player : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(3))) {
                    if (player.isAlive()) player.hurt(damageSources().mobAttack(this), 14f);
                }
            }
            case "Ultimate Madoka" -> {
                var harmful = boss.getActiveEffects().stream().filter(effect -> !effect.getEffect().value().isBeneficial()).findFirst();
                harmful.ifPresent(effect -> boss.removeEffect(effect.getEffect()));
                if (target != null) target.getActiveEffects().stream().filter(effect -> effect.getEffect().value().isBeneficial())
                        .findFirst().ifPresent(effect -> target.removeEffect(effect.getEffect()));
                VfxSpawn.send(level, VfxShape.MIRROR_SHATTER, position(), position().add(0, 1, 0), 0xFFFFD8FF, 10, 0.8f);
            }
            case "Anti-Spiral" -> {
                if (target != null && target.isAlive() && distanceToSqr(target) <= 144 && hasLineOfSight(target))
                    target.hurt(damageSources().mobAttack(this), 10f);
                VfxSpawn.send(level, VfxShape.DARK_SLASH_DIMENSION, getEyePosition(), target == null ? getEyePosition() : target.getEyePosition(),
                        0xFF8C6AFF, 12, 1.0f);
            }
            case "Lord of Nightmares" -> {
                avatarFieldUntil = tickCount + 80;
                VfxSpawn.send(level, VfxShape.DREAM_MANIFEST, position(), position().add(0, 0.1, 0), 0xFF482C70, 16, 1.1f);
            }
            case "Arceus" -> {
                String counter = RivenCombat.arceusCounter(target == null ? "" : ThreatScan.of(boss, target).magic);
                int color = switch (counter) {
                    case "water" -> 0xFF55C9E8;
                    case "lightning" -> 0xFFFFFF55;
                    case "flame" -> 0xFFFF7040;
                    case "earth" -> 0xFFBE9B62;
                    case "light" -> 0xFFFFF1A8;
                    case "dark" -> 0xFF8B62D8;
                    default -> 0xFFE2D8FF;
                };
                VfxSpawn.send(level, VfxShape.ALCHEMY_CIRCLE, position(), position().add(0, 0.1, 0),
                        color, 10, 1.0f);
                if (target != null && target.isAlive() && distanceToSqr(target) <= 196 && hasLineOfSight(target)) {
                    Vec3 from = getEyePosition();
                    Vec3 to = target.getBoundingBox().getCenter();
                    VfxSpawn.send(level, VfxShape.LIGHTNING_SPEAR, from, to, color, 8, 0.85f);
                    target.hurt(damageSources().indirectMagic(this, this), 10f);
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
                    var particle = switch (counter) {
                        case "water" -> ParticleTypes.SPLASH;
                        case "lightning" -> ParticleTypes.ELECTRIC_SPARK;
                        case "flame" -> ParticleTypes.FLAME;
                        case "earth" -> ParticleTypes.CRIT;
                        case "light" -> ParticleTypes.END_ROD;
                        case "dark" -> ParticleTypes.SOUL;
                        default -> ParticleTypes.ENCHANT;
                    };
                    level.sendParticles(particle, to.x, to.y, to.z, 12, 0.35, 0.35, 0.35, 0.02);
                }
            }
            case "Grand Zeno" -> {
                if (target instanceof ServerPlayer player && MarquisStatus.evilEyeActive(player)) {
                    Vec3 anchor = boss.position().add(boss.getLookAngle().multiply(1.5, 0, 1.5));
                    MarquisStatus.compress(player, anchor, 60);
                } else {
                    var constructs = level.getEntitiesOfClass(StoryConstructEntity.class, getBoundingBox().inflate(8),
                            construct -> construct.ownedBy(owner) && construct != this);
                    if (!constructs.isEmpty()) constructs.get(0).discard();
                }
                VfxSpawn.send(level, VfxShape.MIRROR_SHATTER, position(), position().add(0, 1, 0), 0xFFFFF1FF, 10, 0.7f);
            }
            default -> { }
        }
    }

    private RivenBossEntity ownerBoss() {
        return owner != null && level() instanceof ServerLevel sl && sl.getEntity(owner) instanceof RivenBossEntity b && b.isAlive() ? b : null;
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return other instanceof RivenBossEntity || other instanceof StoryConstructEntity || super.isAlliedTo(other);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof RivenBossEntity || source.getEntity() instanceof StoryConstructEntity) return false;
        float before = getHealth();
        boolean hurt = super.hurt(source, amount);
        if (hurt && isAvatar() && "Truth".equals(getCustomName() == null ? "" : getCustomName().getString())
                && level() instanceof ServerLevel sl && sl.getGameTime() >= truthMirrorAt
                && source.getEntity() instanceof Player attacker) {
            truthMirrorAt = sl.getGameTime() + 20;
            RivenBossEntity boss = ownerBoss();
            if (boss != null) {
                float taken = Math.max(0, before - getHealth());
                attacker.hurt(damageSources().indirectMagic(this, this), Math.min(taken, RivenCombat.damage(boss.phase(), true, 0)));
                VfxSpawn.send(sl, VfxShape.MIRROR_SHATTER, getBoundingBox().getCenter(), attacker.getBoundingBox().getCenter(),
                        0xFFFFE3C4, 8, 0.8f);
            }
        }
        return hurt;
    }

    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override protected boolean shouldDespawnInPeaceful() { return false; }
    @Override protected boolean isSunBurnTick() { return false; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Kind", kind());
        tag.putInt("Life", life);
        if (owner != null) tag.putUUID("Owner", owner);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(KIND, tag.getInt("Kind"));
        life = tag.contains("Life") ? tag.getInt("Life") : 400;
        if (tag.hasUUID("Owner")) owner = tag.getUUID("Owner");
    }

    /** Needed by the boss to count its constructs. */
    public boolean ownedBy(UUID id) { return id.equals(owner); }
}
