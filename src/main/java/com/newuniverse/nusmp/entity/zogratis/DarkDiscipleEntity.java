package com.newuniverse.nusmp.entity.zogratis;

import com.newuniverse.nusmp.blackclover.Devil;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * A Dark Disciple: an elite follower of a Zogratis boss, infused with up to 40% of its devil's power. Its stats come from the
 * owner's power at the moment it was summoned ({@link PowerCurve#discipleShare}, {@link DiscipleStats}), and each devil gives it
 * one telegraphed ability:
 * <ul>
 *   <li><b>Lucifero</b>: a gravity pull, then a slam that crushes everyone near it to the ground.</li>
 *   <li><b>Megicula</b>: a rotting curse; its touch and its pulse apply Weakness.</li>
 *   <li><b>Beelzebub</b>: a blink-step: it marks where it will appear behind its target, vanishes, and strikes.</li>
 *   <li><b>Astaroth</b>: a short slow-then-dash: its target is slowed, then it lunges.</li>
 * </ul>
 * It belongs to one boss, shares its target, dies (without drops) when the boss dies or is gone, and drops nothing but experience.
 * Summon it with {@link #spawnFor}; the boss never has more than its power's disciple cap alive.
 */
public class DarkDiscipleEntity extends Monster {
    private static final EntityDataAccessor<Integer> DEVIL = SynchedEntityData.defineId(DarkDiscipleEntity.class, EntityDataSerializers.INT);
    /** Ticks left of the ability telegraph (for the model: it raises its arm, the glow gathers). */
    private static final EntityDataAccessor<Integer> CAST = SynchedEntityData.defineId(DarkDiscipleEntity.class, EntityDataSerializers.INT);

    private UUID ownerId;
    /** 1.21.0: a disciple of a place (the Underworld Gate), not of a boss: it has no owner to follow and never fades for lack of one. */
    private boolean free;
    private double share = 0.08;
    private long nextAbility = 60;
    private Vec3 aim = Vec3.ZERO;
    private int dashLeft, orphanChecks;

    public DarkDiscipleEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 12;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 60).add(Attributes.ATTACK_DAMAGE, 4).add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.FOLLOW_RANGE, 40).add(Attributes.ARMOR, 2).add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(DEVIL, Devil.LUCIFERO.ordinal());
        b.define(CAST, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // ---------------------------------------------------------------- what it is
    public Devil devil() {
        Devil[] all = Devil.values();
        int i = entityData.get(DEVIL);
        return all[Math.max(0, Math.min(all.length - 1, i))];
    }

    /** The glow colour of its devil (0xRRGGBB). */
    public int glowColor() { return DevilStyle.color(devil()); }

    /** Ticks left of the ability's telegraph (0 when idle). */
    public int castLeft() { return entityData.get(CAST); }

    /** The boss it serves. */
    public UUID ownerId() { return ownerId; }

    /** Its share of the devil's power, 0.08 .. 0.4. */
    public double share() { return share; }

    /**
     * Summons a disciple of 'boss' at 'at': its devil, its share (0.4 x the boss's power fraction) and its stats are set from the
     * boss. Null if the boss already has as many disciples as its power allows.
     */
    public static DarkDiscipleEntity spawnFor(ZogratisBoss boss, ServerLevel level, Vec3 at) {
        if (boss.disciples().size() >= boss.tuning().discipleCap()) return null;
        DarkDiscipleEntity d = ZogratisEntities.DARK_DISCIPLE.get().create(level);
        if (d == null) return null;
        d.moveTo(at.x, at.y, at.z, boss.getRandom().nextFloat() * 360f, 0);
        d.infuse(boss);
        level.addFreshEntity(d);
        Tells.burst(level, at.add(0, 1, 0), DevilStyle.color(boss.devil()), 24);
        Tells.ring(level, at, 1.6, DevilStyle.accent(boss.devil()));
        return d;
    }

    /**
     * 1.21.0: a free-standing Dark Disciple of 'devil' infused with 'fraction' (0.08 .. 0.4) of its power, for the Underworld Gate zone of
     * the Variable Core: no boss serves it, it hunts players itself and stays until something removes it. Never null for a loaded level
     * except when the entity type cannot be created.
     */
    public static DarkDiscipleEntity spawnFree(ServerLevel level, Vec3 at, Devil devil, float fraction) {
        DarkDiscipleEntity d = ZogratisEntities.DARK_DISCIPLE.get().create(level);
        if (d == null) return null;
        d.moveTo(at.x, at.y, at.z, level.getRandom().nextFloat() * 360f, 0);
        d.free = true;
        d.share = Math.max(0.08, Math.min(DiscipleStats.MAX_SHARE, fraction));
        d.entityData.set(DEVIL, devil.ordinal());
        d.setBase(Attributes.MAX_HEALTH, DiscipleStats.health(d.share));
        d.setBase(Attributes.ATTACK_DAMAGE, DiscipleStats.attack(d.share));
        d.setBase(Attributes.MOVEMENT_SPEED, DiscipleStats.speed(d.share));
        d.setBase(Attributes.ARMOR, DiscipleStats.armor(d.share));
        d.setHealth(d.getMaxHealth());
        d.nextAbility = 40 + d.getRandom().nextInt(40);
        level.addFreshEntity(d);
        Tells.burst(level, at.add(0, 1, 0), DevilStyle.color(devil), 24);
        Tells.ring(level, at, 1.6, DevilStyle.accent(devil));
        return d;
    }

    /** Is this a disciple of a place rather than of a boss? */
    public boolean isFree() { return free; }

    /**
     * The Sentient Dungeon's wall births: the same free-standing Dark Disciple as {@link #spawnFree}, made and placed but NOT added to the
     * level and without the entrance effects, so that a cracking wall can bring it in when it gives way. Null only if the entity type
     * cannot be created.
     */
    public static DarkDiscipleEntity makeFree(ServerLevel level, Vec3 at, Devil devil, float fraction) {
        DarkDiscipleEntity d = ZogratisEntities.DARK_DISCIPLE.get().create(level);
        if (d == null) return null;
        d.moveTo(at.x, at.y, at.z, level.getRandom().nextFloat() * 360f, 0);
        d.free = true;
        d.share = Math.max(0.08, Math.min(DiscipleStats.MAX_SHARE, fraction));
        d.entityData.set(DEVIL, devil.ordinal());
        d.setBase(Attributes.MAX_HEALTH, DiscipleStats.health(d.share));
        d.setBase(Attributes.ATTACK_DAMAGE, DiscipleStats.attack(d.share));
        d.setBase(Attributes.MOVEMENT_SPEED, DiscipleStats.speed(d.share));
        d.setBase(Attributes.ARMOR, DiscipleStats.armor(d.share));
        d.setHealth(d.getMaxHealth());
        d.nextAbility = 40 + d.getRandom().nextInt(40);
        return d;
    }

    private void infuse(ZogratisBoss boss) {
        ownerId = boss.getUUID();
        share = PowerCurve.discipleShare(boss.power());
        entityData.set(DEVIL, boss.devil().ordinal());
        setBase(Attributes.MAX_HEALTH, DiscipleStats.health(share));
        setBase(Attributes.ATTACK_DAMAGE, DiscipleStats.attack(share));
        setBase(Attributes.MOVEMENT_SPEED, DiscipleStats.speed(share));
        setBase(Attributes.ARMOR, DiscipleStats.armor(share));
        setHealth(getMaxHealth());
        nextAbility = 40 + getRandom().nextInt(40);
    }

    private void setBase(Holder<Attribute> attribute, double value) {
        AttributeInstance a = getAttribute(attribute);
        if (a != null) a.setBaseValue(value);
    }

    private ZogratisBoss owner(ServerLevel sl) {
        return ownerId != null && sl.getEntity(ownerId) instanceof ZogratisBoss b && b.isAlive() ? b : null;
    }

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
        long t = tickCount;
        ZogratisBoss boss = t % 10 == 0 ? owner(sl) : null;
        if (t % 10 == 0 && !free) {
            if (boss == null) {                                           // the owner fell or is gone (a few checks of grace: its chunk may still be loading)
                if (++orphanChecks >= 4) {
                    Tells.burst(sl, position().add(0, 1, 0), glowColor(), 16);
                    discard();
                    return;
                }
            } else orphanChecks = 0;
            if (boss == null) return;
            LivingEntity bt = boss.getTarget();
            if ((getTarget() == null || !getTarget().isAlive()) && bt != null && bt.isAlive()) setTarget(bt);
        }
        LivingEntity tg = getTarget();
        if (dashLeft > 0) { dash(sl, tg); return; }
        if (tg == null || !tg.isAlive()) { if (castLeft() > 0) entityData.set(CAST, 0); return; }
        int cast = castLeft();
        if (cast > 0) {
            entityData.set(CAST, cast - 1);
            telegraph(sl, tg, cast);
            if (cast == 1) release(sl, tg);
        } else if (t >= nextAbility && distanceToSqr(tg) < range() * range()) {
            begin(sl, tg, t);
        }
    }

    private double range() { return devil() == Devil.LUCIFERO ? 12 : devil() == Devil.MEGICULA ? 4 : devil() == Devil.BEELZEBUB ? 14 : 12; }

    private int windUp() {
        return switch (devil()) { case LUCIFERO -> 26; case MEGICULA -> 20; case BEELZEBUB -> 16; default -> 22; };
    }

    private void begin(ServerLevel sl, LivingEntity tg, long t) {
        nextAbility = t + DiscipleStats.abilityCooldown(share) + getRandom().nextInt(30);
        aim = tg.position();
        entityData.set(CAST, windUp());
        getNavigation().stop();
        if (devil() == Devil.ASTAROTH) tg.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, windUp() + 10, 1));   // the "slow"
        sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 1f, 0.7f);
    }

    private void telegraph(ServerLevel sl, LivingEntity tg, int cast) {
        getLookControl().setLookAt(tg, 40f, 40f);
        if (cast % 3 != 0) return;
        int rgb = glowColor();
        switch (devil()) {
            case LUCIFERO -> {                                            // the pull draws the target in during the last 10 ticks, the ring marks the slam
                Tells.ring(sl, position(), 3.6, rgb);
                if (cast <= 10 && distanceToSqr(tg) < 12 * 12) {
                    Vec3 pull = position().subtract(tg.position()).multiply(1, 0, 1);
                    if (pull.lengthSqr() > 4) {
                        tg.setDeltaMovement(tg.getDeltaMovement().add(pull.normalize().scale(0.14)));
                        tg.hurtMarked = true;
                    }
                }
            }
            case MEGICULA -> Tells.ring(sl, position(), 3.0, rgb);
            case BEELZEBUB -> {
                Vec3 to = blinkSpot(tg);
                Tells.column(sl, to, 2.0, rgb);
                Tells.ring(sl, to, 0.9, DevilStyle.accent(devil()));
            }
            default -> {
                Tells.ring(sl, tg.position(), 1.4, rgb);
                Tells.line(sl, position().add(0, 1, 0), tg.position().add(0, 1, 0), rgb);
            }
        }
    }

    /** Where a blink-step lands: just behind the target, as seen from the disciple. */
    private Vec3 blinkSpot(LivingEntity tg) {
        Vec3 dir = tg.position().subtract(position()).multiply(1, 0, 1);
        if (dir.lengthSqr() < 1e-4) dir = new Vec3(1, 0, 0);
        return tg.position().add(dir.normalize().scale(1.3));
    }

    private float hitBase() { return (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * (float) DiscipleStats.abilityDamage(share); }

    private boolean hurtTarget(LivingEntity victim, float base, boolean magic) {
        return ZogratisDamage.hit(this, victim, base, 1.0, 1.0, DiscipleStats.fill(share), magic);
    }

    private void release(ServerLevel sl, LivingEntity tg) {
        int rgb = glowColor();
        switch (devil()) {
            case LUCIFERO -> {                                            // the slam: everyone close is crushed down
                Tells.burst(sl, position().add(0, 0.3, 0), rgb, 40);
                sl.playSound(null, blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.8f, 1.6f);
                for (LivingEntity v : sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(3.6), e -> e.isAlive() && isFoe(e))) {
                    if (distanceToSqr(v) > 3.6 * 3.6) continue;
                    if (hurtTarget(v, hitBase(), true)) {
                        v.setDeltaMovement(v.getDeltaMovement().add(0, -0.7, 0));
                        v.hurtMarked = true;
                        v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 1));
                    }
                }
            }
            case MEGICULA -> {                                            // the curse pulse
                Tells.burst(sl, position().add(0, 1, 0), rgb, 40);
                for (LivingEntity v : sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(3.0), e -> e.isAlive() && isFoe(e))) {
                    if (distanceToSqr(v) > 3.0 * 3.0) continue;
                    if (hurtTarget(v, hitBase() * 0.6f, true)) v.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 0));
                }
            }
            case BEELZEBUB -> {                                           // the blink-step strike
                Vec3 to = blinkSpot(tg);
                Tells.burst(sl, position().add(0, 1, 0), rgb, 20);
                if (sl.noCollision(this, getBoundingBox().move(to.subtract(position())))) {
                    teleportTo(to.x, to.y, to.z);
                    sl.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1f, 1.2f);
                    swing(InteractionHand.MAIN_HAND);
                    if (distanceToSqr(tg) < 4.5 * 4.5) hurtTarget(tg, hitBase(), false);
                }
            }
            default -> {                                                  // the dash: a few fast ticks toward where the target stood
                aim = tg.position();
                dashLeft = 7;
                sl.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1f, 0.5f);
            }
        }
    }

    /** Players in survival and the summons of players: never the animals and villagers standing about. */
    private boolean isFoe(LivingEntity e) {
        if (e == this || !e.isAlive() || ZogratisEntities.isFamily(e)) return false;
        if (e instanceof Player p) return !p.isCreative() && !p.isSpectator();
        return e instanceof OwnableEntity o && o.getOwnerUUID() != null;
    }

    private void dash(ServerLevel sl, LivingEntity tg) {
        dashLeft--;
        Vec3 d = aim.subtract(position()).multiply(1, 0, 1);
        if (d.lengthSqr() > 0.25) setDeltaMovement(d.normalize().scale(0.95).add(0, getDeltaMovement().y, 0));
        hasImpulse = true;
        Tells.burst(sl, position().add(0, 1, 0), glowColor(), 3);
        if (tg != null && tg.isAlive() && getBoundingBox().inflate(0.5).intersects(tg.getBoundingBox())) {
            swing(InteractionHand.MAIN_HAND);
            hurtTarget(tg, hitBase(), false);
            dashLeft = 0;
        }
    }

    // ---------------------------------------------------------------- melee and damage
    @Override
    public boolean doHurtTarget(Entity target) {
        if (!(target instanceof LivingEntity victim) || ZogratisEntities.isFamily(victim)) return false;
        boolean hit = hurtTarget(victim, (float) getAttributeValue(Attributes.ATTACK_DAMAGE), false);
        if (hit && devil() == Devil.MEGICULA) victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));   // the rotting touch
        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() != null && ZogratisEntities.isFamily(source.getEntity())) return false;
        return super.hurt(source, amount);
    }

    // ---------------------------------------------------------------- client flavour
    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide || tickCount % 3 != 0) return;
        int rgb = glowColor();
        var r = getRandom();
        var dust = new DustParticleOptions(new Vector3f(DevilStyle.red(rgb), DevilStyle.green(rgb), DevilStyle.blue(rgb)), castLeft() > 0 ? 1.6f : 1.0f);
        level().addParticle(dust, getX() + (r.nextDouble() - 0.5) * 0.8, getY() + r.nextDouble() * getBbHeight(), getZ() + (r.nextDouble() - 0.5) * 0.8, 0, 0.02, 0);
    }

    // ---------------------------------------------------------------- save
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerId != null) tag.putUUID("Owner", ownerId);
        tag.putDouble("Share", share);
        tag.putInt("Devil", entityData.get(DEVIL));
        if (free) tag.putBoolean("Free", true);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) ownerId = tag.getUUID("Owner");
        if (tag.contains("Share")) share = tag.getDouble("Share");
        if (tag.contains("Devil")) entityData.set(DEVIL, tag.getInt("Devil"));
        free = tag.getBoolean("Free");
    }
}
