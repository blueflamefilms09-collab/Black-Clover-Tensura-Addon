package com.newuniverse.nusmp.entity;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Real Double (0.42): a mirror double of its owner, stepped out of a summoned mirror. After the Black Clover wiki (the clone is
 * the owner's mirrored reflection and can use the same spells, so together they strike with twice the power) and Tensura's Body
 * Double (a tenth of the owner's magic power buys a clone that takes extra damage and fights until the end).
 *
 * <ul>
 *   <li>Drawn with its owner's skin, mirrored, with a violet glass sheen ({@code client.MirrorDoubleRenderer}).</li>
 *   <li>Health and strength are a share of the owner's: {@link #setup} takes the owner's max health and the EP-scaled power the
 *       spell computed.</li>
 *   <li>Takes 50% more damage than it is dealt (Body Double); shatters into glass when it falls or its time runs out.</li>
 *   <li>Fights beside its owner: follows, defends, attacks what the owner attacks, and every few seconds fires a Reflect Ray.
 *       When the owner casts a spell, every double echoes it with a ray at the owner's target ({@link #echo}).</li>
 * </ul>
 */
public class MirrorDoubleEntity extends TamableAnimal {
    private int life = 1200, rayCooldown = 40;
    private float power = 1f;

    public MirrorDoubleEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.ATTACK_DAMAGE, 4)
                .add(Attributes.MOVEMENT_SPEED, 0.33).add(Attributes.FOLLOW_RANGE, 32).add(Attributes.ARMOR, 2);
    }

    /** Sizes the double from its owner: health = half the owner's max health x power, attack scaled by power, the owner's weapon. */
    public void setup(ServerPlayer owner, float power, int lifeTicks) {
        this.power = power;
        this.life = lifeTicks;
        tame(owner);
        var hp = getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(Math.max(10, owner.getMaxHealth() * 0.5 * power));
        var atk = getAttribute(Attributes.ATTACK_DAMAGE);
        if (atk != null) atk.setBaseValue(3 + 3 * power);
        setHealth(getMaxHealth());
        ItemStack held = owner.getMainHandItem();
        if (!held.isEmpty()) setItemSlot(EquipmentSlot.MAINHAND, held.copyWithCount(1));
        setDropChance(EquipmentSlot.MAINHAND, 0f);
        setCustomName(owner.getName().copy().append(" (Real Double)"));
        setCustomNameVisible(false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.25, true));
        goalSelector.addGoal(5, new FollowOwnerGoal(this, 1.15, 6f, 2.5f));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        targetSelector.addGoal(3, new HurtByTargetGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (--life <= 0 || getOwner() == null || !getOwner().isAlive()) shatter();
    }

    /** Every few seconds: a Reflect Ray at the current target. */
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        LivingEntity t = getTarget();
        if (--rayCooldown > 0 || t == null || !t.isAlive() || distanceToSqr(t) > 18 * 18 || !hasLineOfSight(t)) return;
        rayCooldown = 70;
        ray(t, 5f);
    }

    /** The owner cast a spell: the double casts it with them (a ray at the owner's target). */
    public void echo(@Nullable LivingEntity target) {
        if (target == null || !target.isAlive() || target == getOwner() || distanceToSqr(target) > 24 * 24) return;
        ray(target, 4f);
    }

    private void ray(LivingEntity t, float base) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 from = getEyePosition(), to = t.getBoundingBox().getCenter();
        VfxSpawn.send(sl, VfxShape.MIRROR_RAY, from, to, 0xFFC8B0FF, 14, 0.7f);
        LivingEntity owner = getOwner();
        DamageSource src = owner != null ? damageSources().indirectMagic(this, owner) : damageSources().mobAttack(this);
        t.hurt(src, BalanceLaw.damage(t, base * power, 0.5));
        sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 1.6f);
    }

    /** Breaks into glass and is gone. */
    public void shatter() {
        if (isRemoved()) return;
        if (level() instanceof ServerLevel sl) {
            VfxSpawn.send(sl, VfxShape.MIRROR_SHATTER, position().add(0, 1, 0), position().add(0, 2, 0), 0xFFC8B0FF, 24, 1.2f);
            sl.playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1f, 1.1f);
        }
        discard();
    }

    /** Body Double: a clone takes 50% more damage; its owner cannot hurt it. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() != null && source.getEntity() == getOwner()) return false;
        return super.hurt(source, amount * 1.5f);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        shatter();
    }

    @Override public boolean isFood(ItemStack stack) { return false; }
    @Override public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) { return null; }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override protected boolean shouldDropLoot() { return false; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("DoubleLife", life);
        tag.putFloat("DoublePower", power);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        life = tag.contains("DoubleLife") ? tag.getInt("DoubleLife") : 0;
        power = tag.contains("DoublePower") ? tag.getFloat("DoublePower") : 1f;
    }
}
