package com.newuniverse.nusmp.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 0.52: Charmy's cotton sheep, drawn like the anime's: tall, upright, fluffy, with a pale long face, round pale eyes and curled
 * horns (cooks wear a chef's hat and a blue neckerchief). Three jobs, all summoned by Cotton Magic and never hurt:
 * <ul>
 *   <li>{@link #STRIKER}: climbs out of a cloud, leaps in an arc at a foe and, on landing, runs the spell's effect (Sleeping Sheep Strike).</li>
 *   <li>{@link #COOK}: stands on the kitchen cloud, flips pans and ladles and steams (Sheep Cook).</li>
 *   <li>{@link #BINDER}: leaps at a foe and hugs it in cotton while it is held (Sheep Bondage).</li>
 * </ul>
 */
public class CottonSheepEntity extends PathfinderMob {
    public static final int STRIKER = 0, COOK = 1, BINDER = 2;
    /** 0 idle, 1 leaping, 2 hugging, 3 cooking. */
    public static final int IDLE = 0, LEAP = 1, HUG = 2, COOKING = 3;
    private static final EntityDataAccessor<Integer> ROLE = SynchedEntityData.defineId(CottonSheepEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ANIM = SynchedEntityData.defineId(CottonSheepEntity.class, EntityDataSerializers.INT);

    private int life, maxLife = 100, flightAt = 8, flightLen = 14;
    private Vec3 from = Vec3.ZERO, to = Vec3.ZERO;
    private Runnable arrive;
    private boolean arrived;
    private float facing;

    public CottonSheepEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setInvulnerable(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 10).add(Attributes.MOVEMENT_SPEED, 0.2);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(ROLE, STRIKER);
        b.define(ANIM, IDLE);
    }

    public int role() { return entityData == null ? STRIKER : entityData.get(ROLE); }
    public int anim() { return entityData == null ? IDLE : entityData.get(ANIM); }

    private static CottonSheepEntity make(ServerLevel sl, int role, Vec3 at, float yaw) {
        CottonSheepEntity s = NUEntities.COTTON_SHEEP.get().create(sl);
        if (s == null) return null;
        s.entityData.set(ROLE, role);
        s.moveTo(at.x, at.y, at.z, yaw, 0);
        s.facing = yaw;
        return s;
    }

    private static float yawTo(Vec3 a, Vec3 b) { return (float) (Mth.atan2(b.z - a.z, b.x - a.x) * Mth.RAD_TO_DEG) - 90f; }

    /** Sleeping Sheep Strike: a sheep climbs out of the cloud at 'from', leaps to 'to' and runs 'onArrive' there. */
    public static CottonSheepEntity strike(ServerLevel sl, Vec3 from, Vec3 to, Runnable onArrive) {
        CottonSheepEntity s = make(sl, STRIKER, from, yawTo(from, to));
        if (s == null) return null;
        s.from = from; s.to = to; s.arrive = onArrive; s.maxLife = 60;
        sl.addFreshEntity(s);
        return s;
    }

    /** Sheep Bondage: a sheep leaps at the foe and hugs it for 'hold' ticks. */
    public static CottonSheepEntity bind(ServerLevel sl, Vec3 from, Vec3 to, int hold, Runnable onArrive) {
        CottonSheepEntity s = make(sl, BINDER, from, yawTo(from, to));
        if (s == null) return null;
        s.from = from; s.to = to; s.arrive = onArrive; s.maxLife = 8 + 14 + hold;
        sl.addFreshEntity(s);
        return s;
    }

    /** Sheep Cook: 'count' chefs stand in a ring round 'center' (the top of the kitchen cloud), facing outward. */
    public static void cooks(ServerLevel sl, Vec3 center, int count, double radius, int lifeTicks) {
        for (int k = 0; k < count; k++) {
            double a = Math.PI * 2 * k / count + 0.4;
            Vec3 at = center.add(Math.cos(a) * radius, 0, Math.sin(a) * radius);
            CottonSheepEntity s = make(sl, COOK, at, (float) (a * Mth.RAD_TO_DEG) + 90f);
            if (s == null) continue;
            s.maxLife = lifeTicks;
            s.entityData.set(ANIM, COOKING);
            sl.addFreshEntity(s);
        }
    }

    // ---------------------------------------------------------------- behaviour
    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) {
            if (tickCount % 4 == 0 && anim() == COOKING) level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY() + 2.2, getZ(), 0, 0.04, 0);
            return;
        }
        life++;
        if (role() != COOK) fly(sl);
        else { setYRot(facing); yBodyRot = facing; yHeadRot = facing; }
        if (life >= maxLife) {
            sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1, getZ(), 14, 0.5, 0.7, 0.5, 0.04);
            sl.sendParticles(ParticleTypes.POOF, getX(), getY() + 1, getZ(), 6, 0.4, 0.6, 0.4, 0.02);
            discard();
        }
    }

    /** Strikers and binders: rise out of the cloud, then an arc to the target, then (binders) hold on. */
    private void fly(ServerLevel sl) {
        if (arrived) {
            setPos(to.x, to.y, to.z);
            return;
        }
        if (life < flightAt) {
            double up = 0.9 * Mth.sin((float) (life / (double) flightAt * Math.PI / 2));
            setPos(from.x, from.y + up, from.z);
            entityData.set(ANIM, IDLE);
            return;
        }
        double t = Math.min(1, (life - flightAt) / (double) flightLen), e = t * t * (3 - 2 * t);
        Vec3 start = from.add(0, 0.9, 0), p = start.lerp(to, e).add(0, Math.sin(t * Math.PI) * 2.2, 0);
        float yaw = yawTo(start, to);
        setPos(p.x, p.y, p.z);
        setYRot(yaw);
        yBodyRot = yaw;
        yHeadRot = yaw;
        entityData.set(ANIM, LEAP);
        if (t >= 1) {
            arrived = true;
            if (role() == STRIKER) life = Math.max(life, maxLife - 10);                                     // lands, then pops
            else entityData.set(ANIM, HUG);
            sl.playSound(null, blockPosition(), SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 1.5f, 0.8f);
            sl.sendParticles(ParticleTypes.CLOUD, to.x, to.y + 0.6, to.z, 12, 0.5, 0.4, 0.5, 0.05);
            if (arrive != null) arrive.run();
        }
    }

    // ---------------------------------------------------------------- never hurt, never saved
    @Override
    public boolean hurt(DamageSource source, float amount) { return source.is(DamageTypes.GENERIC_KILL) && super.hurt(source, amount); }

    @Override public boolean isPushable() { return false; }
    @Override public boolean shouldBeSaved() { return false; }
    @Override protected void pushEntities() { }
    @Override public boolean fireImmune() { return true; }
    @Override public void addAdditionalSaveData(CompoundTag tag) { }
    @Override public void readAdditionalSaveData(CompoundTag tag) { }
}
