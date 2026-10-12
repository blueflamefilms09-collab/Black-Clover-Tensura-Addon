package com.newuniverse.nusmp.entity.zogratis;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.UUID;

/**
 * A Blood Beast: a four-legged hound of living blood that Vanica shapes during the fight (Blood Magic), hostile to everyone who
 * fights her. It belongs to one boss, shares its target, and is gone when the boss falls, when the fight ends or after its
 * lifetime ({@link VanicaMath#beastLife}); it drops nothing but experience.
 * <ul>
 *   <li><b>Rises</b> out of a puddle first ({@link #riseLeft}, inert and visible for about a second): the telegraph of every summon.</li>
 *   <li><b>Bite</b>: fast, and the wound bleeds (Wither); a bite heals the beast by half of what it took.</li>
 *   <li><b>Pounce</b> (not the stampede): at 4 to 9.5 blocks it crouches for half a second ({@link #pounceLeft}, the tell), then
 *       leaps; whoever it lands on takes a bite and a half.</li>
 *   <li><b>Stampede</b> beasts (the Crimson Crown) are quick and brittle and live ten seconds.</li>
 *   <li>At the Gates the beasts are tougher and bleed harder; their numbers come from {@link VanicaMath}.</li>
 * </ul>
 * Summon with {@link #spawnFor}. The numbers are all from the owner's power when the beast was made.
 */
public class BloodBeastEntity extends Monster {
    private static final EntityDataAccessor<Integer> RISE = SynchedEntityData.defineId(BloodBeastEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> POUNCE = SynchedEntityData.defineId(BloodBeastEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FILL = SynchedEntityData.defineId(BloodBeastEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> STAMPEDE = SynchedEntityData.defineId(BloodBeastEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> LEAP = SynchedEntityData.defineId(BloodBeastEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int POUNCE_WIND = 10, LEAP_TICKS = 14;
    private static final int BLOOD = 0xB0121E;

    private UUID ownerId;
    private int power = PowerCurve.OPENING, life = VanicaMath.beastLife(false), leapLeft;
    private long nextBite, nextPounce;
    private boolean leapHit;

    public BloodBeastEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 3;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 28).add(Attributes.ATTACK_DAMAGE, 4).add(Attributes.MOVEMENT_SPEED, 0.31)
                .add(Attributes.FOLLOW_RANGE, 40).add(Attributes.ARMOR, 2).add(Attributes.KNOCKBACK_RESISTANCE, 0.3).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(RISE, 0);
        b.define(POUNCE, 0);
        b.define(FILL, 0);
        b.define(STAMPEDE, false);
        b.define(LEAP, false);
    }

    // ---------------------------------------------------------------- state for the model
    /** Ticks left of the rise out of the puddle (the spawn clip; the beast does nothing meanwhile). */
    public int riseLeft() { return entityData.get(RISE); }
    /** Ticks left of the crouch before a pounce (the tell). */
    public int pounceLeft() { return entityData.get(POUNCE); }
    /** 0 .. 100: how much of Megicula's power the owner had drawn when it was made (the glow and the build). */
    public int fillPercent() { return entityData.get(FILL); }
    /** A beast of the Crimson Crown's stampede. */
    public boolean stampede() { return entityData.get(STAMPEDE); }
    /** In mid-leap (synced: true on the client too). */
    public boolean leaping() { return entityData.get(LEAP); }

    /** Sets the ticks left of the leap on the server and keeps the synced flag in step. */
    private void setLeap(int ticks) {
        leapLeft = ticks;
        boolean on = ticks > 0;
        if (entityData.get(LEAP) != on) entityData.set(LEAP, on);
    }
    /** The boss it serves. */
    public UUID ownerId() { return ownerId; }
    /** Ticks it has left to live. */
    public int lifeLeft() { return life; }

    // ---------------------------------------------------------------- making one
    /** How many live Blood Beasts this boss has. */
    public static int countOf(ZogratisBoss boss) {
        if (!(boss.level() instanceof ServerLevel sl)) return 0;
        UUID me = boss.getUUID();
        return sl.getEntitiesOfClass(BloodBeastEntity.class, boss.getBoundingBox().inflate(110), b -> b.isAlive() && me.equals(b.ownerId)).size();
    }

    /**
     * Shapes a beast of 'boss' at 'at': its health, bite, speed and armour come from the boss's power. Null if the boss already has
     * {@link VanicaMath#HARD_BEAST_CAP} beasts or there is no room for it.
     */
    public static BloodBeastEntity spawnFor(ZogratisBoss boss, ServerLevel level, Vec3 at, boolean stampede) {
        if (countOf(boss) >= VanicaMath.HARD_BEAST_CAP) return null;
        BloodBeastEntity b = ZogratisEntities.BLOOD_BEAST.get().create(level);
        if (b == null) return null;
        b.moveTo(at.x, at.y, at.z, boss.getRandom().nextFloat() * 360f, 0);
        if (!level.noCollision(b)) {
            b.discard();
            return null;
        }
        b.infuse(boss, stampede);
        level.addFreshEntity(b);
        Tells.burst(level, at.add(0, 0.3, 0), BLOOD, 14);
        level.playSound(null, BlockPos.containing(at), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 1.2f, 0.6f);
        return b;
    }

    private void infuse(ZogratisBoss boss, boolean stampede) {
        ownerId = boss.getUUID();
        power = boss.power();
        entityData.set(STAMPEDE, stampede);
        entityData.set(FILL, (int) Math.round(VanicaMath.fill(power) * 100));
        entityData.set(RISE, stampede ? 12 : VanicaMath.SPAWN_TICKS);
        setBase(Attributes.MAX_HEALTH, VanicaMath.beastHealth(power, stampede));
        setBase(Attributes.ATTACK_DAMAGE, VanicaMath.beastAttack(power));
        setBase(Attributes.MOVEMENT_SPEED, VanicaMath.beastSpeed(power, stampede));
        setBase(Attributes.ARMOR, VanicaMath.beastArmor(power));
        setHealth(getMaxHealth());
        life = VanicaMath.beastLife(stampede);
        nextPounce = 30 + getRandom().nextInt(30);
        nextBite = 0;
    }

    private void setBase(Holder<Attribute> attribute, double value) {
        AttributeInstance a = getAttribute(attribute);
        if (a != null) a.setBaseValue(value);
    }

    private ZogratisBoss owner(ServerLevel sl) {
        return ownerId != null && sl.getEntity(ownerId) instanceof ZogratisBoss b && b.isAlive() ? b : null;
    }

    // ---------------------------------------------------------------- goals
    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new Chase(this));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16f));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** Runs at its target and bites when close; stands still while it rises, crouches or leaps. */
    private static final class Chase extends Goal {
        private final BloodBeastEntity beast;
        private int repath;

        Chase(BloodBeastEntity beast) {
            this.beast = beast;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = beast.getTarget();
            return t != null && t.isAlive() && beast.canAct();
        }

        @Override public boolean canContinueToUse() { return canUse(); }

        @Override
        public void tick() {
            LivingEntity t = beast.getTarget();
            if (t == null) return;
            beast.getLookControl().setLookAt(t, 30f, 30f);
            if (--repath <= 0) {
                repath = 4;
                beast.getNavigation().moveTo(t, 1.25);
            }
            if (beast.tickCount >= beast.nextBite && beast.getBoundingBox().inflate(0.6, 0.3, 0.6).intersects(t.getBoundingBox())) {
                beast.nextBite = beast.tickCount + VanicaMath.biteCooldown(beast.power);
                beast.swing(InteractionHand.MAIN_HAND);
                beast.bite(t, 1.0f);
            }
        }
    }

    boolean canAct() { return riseLeft() <= 0 && pounceLeft() <= 0 && leapLeft <= 0; }

    @Override public boolean isAlliedTo(Entity other) { return ZogratisEntities.isFamily(other) || super.isAlliedTo(other); }
    @Override protected boolean shouldDespawnInPeaceful() { return false; }
    @Override public boolean removeWhenFarAway(double d) { return false; }

    /** Nothing drops but experience. */
    @Override protected boolean shouldDropLoot() { return false; }

    // ---------------------------------------------------------------- behaviour
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel sl)) return;
        int rise = riseLeft();
        if (rise > 0) {                                                      // rising out of the puddle: inert
            entityData.set(RISE, rise - 1);
            getNavigation().stop();
            if (rise % 6 == 0) Tells.burst(sl, position().add(0, 0.2, 0), BLOOD, 4);
            return;
        }
        if (--life <= 0) {
            vanish(sl);
            return;
        }
        if (tickCount % 10 == 0) {
            ZogratisBoss boss = owner(sl);
            if (boss == null || !boss.fightActive()) {                       // the owner fell, or the fight is over
                vanish(sl);
                return;
            }
            LivingEntity bt = boss.getTarget();
            if (bt != null && bt.isAlive() && (getTarget() == null || !getTarget().isAlive())) setTarget(bt);
        }
        LivingEntity t = getTarget();
        if (t == null || !t.isAlive()) {
            if (pounceLeft() > 0) entityData.set(POUNCE, 0);
            if (leapLeft > 0) setLeap(0);
            return;
        }
        int wind = pounceLeft();
        if (wind > 0) {                                                      // the crouch
            getNavigation().stop();
            getLookControl().setLookAt(t, 60f, 60f);
            entityData.set(POUNCE, wind - 1);
            if (wind % 3 == 0) Tells.line(sl, position().add(0, 0.6, 0), t.position().add(0, 0.6, 0), BLOOD);
            if (wind == 1) leap(sl, t);
            return;
        }
        if (leapLeft > 0) {
            leapStep(sl, t);
            return;
        }
        if (!stampede() && tickCount >= nextPounce && onGround()) {
            double d = distanceTo(t);
            if (d >= 4 && d <= 9.5 && hasLineOfSight(t)) {                   // the ray is cast only when the distance already fits
                entityData.set(POUNCE, POUNCE_WIND);
                nextPounce = tickCount + VanicaMath.pounceCooldown(power);
                sl.playSound(null, blockPosition(), SoundEvents.WOLF_GROWL, SoundSource.HOSTILE, 1.0f, 0.7f);
            }
        }
    }

    private void leap(ServerLevel sl, LivingEntity t) {
        Vec3 d = t.position().subtract(position()).multiply(1, 0, 1);
        if (d.lengthSqr() < 1e-4) return;
        Vec3 dir = d.normalize();
        setDeltaMovement(dir.x * 0.95, 0.42, dir.z * 0.95);
        hasImpulse = true;
        setLeap(LEAP_TICKS);
        leapHit = false;
        sl.playSound(null, blockPosition(), SoundEvents.WOLF_HOWL, SoundSource.HOSTILE, 0.8f, 1.5f);
    }

    private void leapStep(ServerLevel sl, LivingEntity t) {
        setLeap(leapLeft - 1);
        Tells.burst(sl, position().add(0, 0.5, 0), BLOOD, 2);
        if (!leapHit && getBoundingBox().inflate(0.5).intersects(t.getBoundingBox())) {
            leapHit = true;
            swing(InteractionHand.MAIN_HAND);
            bite(t, 1.5f);
            setLeap(0);
        } else if (onGround() && leapLeft < LEAP_TICKS - 3) {
            setLeap(0);
        }
    }

    /** One bite of 'factor' times the beast's attack: through the Zogratis damage law, a bleeding wound, and the beast heals by half of what it took. */
    private boolean bite(LivingEntity victim, float factor) {
        if (ZogratisEntities.isFamily(victim)) return false;
        float before = victim.getHealth() + victim.getAbsorptionAmount();
        if (!ZogratisDamage.hit(this, victim, (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * factor, 1.0, 1.0, VanicaMath.fill(power), false)) return false;
        float dealt = Math.max(0f, before - (victim.getHealth() + victim.getAbsorptionAmount()));
        victim.addEffect(new MobEffectInstance(MobEffects.WITHER, VanicaMath.bleedTicks(power), PowerCurve.gatesOpen(power) ? 1 : 0));
        if (dealt > 0f) heal(Math.min(dealt * 0.5f, getMaxHealth() * 0.25f));
        if (level() instanceof ServerLevel sl)
            sl.sendParticles(VanicaMoves.chips(), victim.getX(), victim.getY() + victim.getBbHeight() * 0.6, victim.getZ(), 8, 0.2, 0.2, 0.2, 0.05);
        return true;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return target instanceof LivingEntity victim && bite(victim, 1.0f);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() != null && ZogratisEntities.isFamily(source.getEntity())) return false;
        return super.hurt(source, amount);
    }

    private void vanish(ServerLevel sl) {
        Tells.burst(sl, position().add(0, 0.6, 0), BLOOD, 18);
        discard();
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel sl) {
            Vec3 at = position();
            VfxSpawn.send(sl, VfxShape.BLOOD_FX3, at.add(0, 0.4, 0), at.add(0, 1.2, 0), 0xFFE0182C, 22, 0.8f);
            Tells.burst(sl, at.add(0, 0.5, 0), BLOOD, 20);
        }
    }

    // ---------------------------------------------------------------- sounds
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.WOLF_GROWL; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.WOLF_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.WOLF_DEATH; }

    // ---------------------------------------------------------------- client flavour
    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide || tickCount % 3 != 0) return;
        var r = getRandom();
        var dust = new DustParticleOptions(new Vector3f(0.69f, 0.07f, 0.12f), pounceLeft() > 0 ? 1.5f : 1.0f);
        level().addParticle(dust, getX() + (r.nextDouble() - 0.5) * 1.0, getY() + r.nextDouble() * getBbHeight(), getZ() + (r.nextDouble() - 0.5) * 1.0, 0, -0.02, 0);
    }

    // ---------------------------------------------------------------- save
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerId != null) tag.putUUID("BOwner", ownerId);
        tag.putInt("BPower", power);
        tag.putInt("BLife", life);
        tag.putBoolean("BStampede", stampede());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("BOwner")) ownerId = tag.getUUID("BOwner");
        if (tag.contains("BPower")) power = tag.getInt("BPower");
        if (tag.contains("BLife")) life = tag.getInt("BLife");
        entityData.set(STAMPEDE, tag.getBoolean("BStampede"));
        entityData.set(FILL, (int) Math.round(VanicaMath.fill(power) * 100));
    }
}
