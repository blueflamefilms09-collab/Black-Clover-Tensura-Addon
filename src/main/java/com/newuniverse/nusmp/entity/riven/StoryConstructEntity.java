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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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
        return super.hurt(source, amount);
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
