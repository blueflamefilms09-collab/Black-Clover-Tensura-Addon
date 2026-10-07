package com.newuniverse.nusmp.entity;

import com.newuniverse.nusmp.book.PaintStudio;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A living illustration (0.44 Painting Magic): something painted with the brush that came off the canvas as a solid, tangible
 * creature of wet paint. Drawn in its paint's colour with a glowing wet outline ({@code client.PaintedConstructRenderer}); it rises
 * from flat to full height as it appears.
 * <ul>
 *   <li>BEAST: a fast painted hound (Painted Menagerie). KNIGHT: an einherjar with a sword (Living Illustration, Master of
 *       Valhalla). GIANT: a hulking painted giant whose blows fling foes (Living Illustration, sneaking).</li>
 *   <li>Durability and strength scale with the painter's {@link PaintStudio#power} (EP, gear, mood). Every blow carries its paint's
 *       element ({@link PaintStudio#applyPaint}).</li>
 *   <li>Paint runs: in water or rain it takes damage. It melts into a splash when it falls or its time runs out.</li>
 * </ul>
 */
public class PaintedConstructEntity extends TamableAnimal {
    public enum Kind { BEAST, KNIGHT, GIANT }

    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(PaintedConstructEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PAINT = SynchedEntityData.defineId(PaintedConstructEntity.class, EntityDataSerializers.INT);

    private int life = 600;
    private float power = 1f;

    public PaintedConstructEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.ATTACK_DAMAGE, 4).add(Attributes.ATTACK_KNOCKBACK, 0)
                .add(Attributes.MOVEMENT_SPEED, 0.32).add(Attributes.FOLLOW_RANGE, 32).add(Attributes.ARMOR, 0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(KIND, Kind.KNIGHT.ordinal());
        b.define(PAINT, 0);
    }

    public Kind kind() { Kind[] v = Kind.values(); return v[Math.floorMod(entityData.get(KIND), v.length)]; }
    public PaintStudio.Paint paint() { return PaintStudio.Paint.of(entityData.get(PAINT)); }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (KIND.equals(key)) refreshDimensions();
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        if (entityData == null) return super.getDefaultDimensions(pose);                        // during construction
        return switch (kind()) {
            case BEAST -> EntityDimensions.scalable(0.9f, 0.9f);
            case KNIGHT -> EntityDimensions.scalable(0.6f, 1.95f);
            case GIANT -> EntityDimensions.scalable(1.2f, 3.9f);
        };
    }

    /** How many illustrations this painter has standing. */
    public static int countOf(ServerPlayer owner) {
        return owner.serverLevel().getEntitiesOfClass(PaintedConstructEntity.class, owner.getBoundingBox().inflate(64), e -> e.isOwnedBy(owner)).size();
    }

    public static PaintedConstructEntity spawn(ServerPlayer owner, Kind kind, Vec3 at, float yaw, PaintStudio.Paint paint, float power, int life) {
        PaintedConstructEntity e = NUEntities.PAINTED_CONSTRUCT.get().create(owner.serverLevel());
        if (e == null) return null;
        e.entityData.set(KIND, kind.ordinal());
        e.entityData.set(PAINT, paint.ordinal());
        e.refreshDimensions();
        e.power = power;
        e.life = life;
        e.tame(owner);
        e.moveTo(at.x, at.y, at.z, yaw, 0);
        e.setYHeadRot(yaw);
        e.setYBodyRot(yaw);
        double hp = switch (kind) { case BEAST -> 12; case KNIGHT -> 20; case GIANT -> 40; } * power;
        double atk = switch (kind) { case BEAST -> 3.5; case KNIGHT -> 5; case GIANT -> 9; } * power;
        double speed = switch (kind) { case BEAST -> 0.4; case KNIGHT -> 0.31; case GIANT -> 0.26; };
        set(e, Attributes.MAX_HEALTH, hp);
        set(e, Attributes.ATTACK_DAMAGE, atk);
        set(e, Attributes.MOVEMENT_SPEED, speed);
        set(e, Attributes.ARMOR, kind == Kind.KNIGHT ? 6 : kind == Kind.GIANT ? 4 : 0);
        set(e, Attributes.ATTACK_KNOCKBACK, kind == Kind.GIANT ? 1.5 : 0);
        e.setHealth(e.getMaxHealth());
        e.setCustomName(net.minecraft.network.chat.Component.literal("Painted " + switch (kind) { case BEAST -> "Beast"; case KNIGHT -> "Einherjar"; case GIANT -> "Giant"; }));
        e.setCustomNameVisible(false);
        owner.serverLevel().addFreshEntity(e);
        return e;
    }

    private static void set(LivingEntity e, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> a, double v) {
        var inst = e.getAttribute(a);
        if (inst != null) inst.setBaseValue(v);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.3, true));
        goalSelector.addGoal(5, new FollowOwnerGoal(this, 1.2, 7f, 3f));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        targetSelector.addGoal(3, new HurtByTargetGoal(this));
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false, m -> m instanceof Enemy));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (--life <= 0 || getOwner() == null || !getOwner().isAlive()) { melt(); return; }
        if (tickCount % 20 == 0 && isInWaterOrRain()) hurt(damageSources().drown(), 1f);     // paint runs in the wet
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity t && getOwner() instanceof ServerPlayer owner) {
            PaintStudio.applyPaint(owner, t, paint(), power * 0.6f);
            if (level() instanceof ServerLevel sl)
                VfxSpawn.send(sl, VfxShape.PAINT_SPLAT, t.position().add(0, t.getBbHeight() * 0.5, 0), t.position().add(0, 2, 0), paint().color, 14, 0.5f);
        }
        return hit;
    }

    /** Melts into a splash of paint and is gone. */
    public void melt() {
        if (isRemoved()) return;
        if (level() instanceof ServerLevel sl) {
            VfxSpawn.send(sl, VfxShape.PAINT_SPLAT, position(), position().add(0, 1, 0), paint().color, 60, kind() == Kind.GIANT ? 2.2f : 1.2f);
            sl.playSound(null, blockPosition(), SoundEvents.SLIME_BLOCK_BREAK, SoundSource.NEUTRAL, 1f, 0.8f);
        }
        discard();
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        melt();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() != null && source.getEntity() == getOwner()) return false;
        return super.hurt(source, amount);
    }

    @Override public boolean isFood(ItemStack stack) { return false; }
    @Override public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) { return null; }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override protected boolean shouldDropLoot() { return false; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("PaintKind", entityData.get(KIND));
        tag.putInt("PaintColour", entityData.get(PAINT));
        tag.putInt("PaintLife", life);
        tag.putFloat("PaintPower", power);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(KIND, tag.getInt("PaintKind"));
        entityData.set(PAINT, tag.getInt("PaintColour"));
        life = tag.contains("PaintLife") ? tag.getInt("PaintLife") : 0;
        power = tag.contains("PaintPower") ? tag.getFloat("PaintPower") : 1f;
    }
}
