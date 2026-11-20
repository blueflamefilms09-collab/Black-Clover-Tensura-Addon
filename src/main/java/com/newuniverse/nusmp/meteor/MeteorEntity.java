package com.newuniverse.nusmp.meteor;

import com.newuniverse.nusmp.perf.PerfBudgets;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * One falling meteor (docs/meteor_fall.md). It is a plain {@link Entity}: no AI, no collision, no gravity, never saved. Its whole flight is
 * {@link MeteorRules#position(double[], double[], int, int)} of the synced start, the synced centre of the impact, the synced fall length and its own tick counter, so the server and every
 * client move it by the same formula and the server sends it no movement packets after the one that spawns it. On the server, the tick after the last tick of the fall calls
 * {@link MeteorFall#land}; a meteor that is still there {@link MeteorRules#GRACE} ticks later is discarded. On a client it also makes a few trail particles through the client budget.
 * The caller's landing code and the caster are server-only fields (a client never sees them).
 *
 * <p>Cost: one formula and one {@code setPos} a tick, no world access (it flies through blocks), no packets.
 */
public class MeteorEntity extends Entity {
    private static final EntityDataAccessor<Vector3f> START = SynchedEntityData.defineId(MeteorEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Vector3f> CENTRE = SynchedEntityData.defineId(MeteorEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> FALL = SynchedEntityData.defineId(MeteorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STYLE = SynchedEntityData.defineId(MeteorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(MeteorEntity.class, EntityDataSerializers.FLOAT);

    // server only
    private MeteorSpec spec;
    private UUID caster;
    private Vec3 impact = Vec3.ZERO;

    public MeteorEntity(EntityType<? extends MeteorEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setInvulnerable(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(START, new Vector3f());
        b.define(CENTRE, new Vector3f());
        b.define(FALL, 24);
        b.define(STYLE, 0);
        b.define(SIZE, 1.0f);
    }

    /** Server: sets the meteor up (already clamped spec) and puts it at its start. {@code impact} is the point on the ground, {@code start} the centre of the rock at the first tick. */
    void setup(MeteorSpec s, UUID casterId, Vec3 impactPoint, Vec3 startCentre) {
        this.spec = s;
        this.caster = casterId;
        this.impact = impactPoint;
        Vec3 centre = impactPoint.add(0, 0.55 * s.size(), 0);
        entityData.set(START, new Vector3f((float) startCentre.x, (float) startCentre.y, (float) startCentre.z));
        entityData.set(CENTRE, new Vector3f((float) centre.x, (float) centre.y, (float) centre.z));
        entityData.set(FALL, s.fallTicks());
        entityData.set(STYLE, s.style().ordinal());
        entityData.set(SIZE, s.size());
        placeAt(startCentre);
        this.xo = this.xOld = getX();
        this.yo = this.yOld = getY();
        this.zo = this.zOld = getZ();
    }

    MeteorSpec spec() { return spec; }

    UUID casterId() { return caster; }

    Vec3 impact() { return impact; }

    public MeteorStyle style() { return MeteorStyle.of(entityData.get(STYLE)); }

    public float size() { return entityData.get(SIZE); }

    public int fallTicks() { return entityData.get(FALL); }

    /** Puts the rock's centre at {@code c} (the entity's feet are half its height below the centre). */
    private void placeAt(Vec3 c) { setPos(c.x, c.y - getBbHeight() * 0.5, c.z); }

    @Override
    public void tick() {
        // Level#tickNonPassenger already did setOldPosAndRot and tickCount++ ; baseTick (fire, water, portals ...) is not needed by a rock in the sky.
        int total = fallTicks();
        Vector3f a = entityData.get(START), b = entityData.get(CENTRE);
        double[] p = MeteorRules.position(new double[]{a.x, a.y, a.z}, new double[]{b.x, b.y, b.z}, tickCount, total);
        placeAt(new Vec3(p[0], p[1], p[2]));
        if (level().isClientSide) {
            trail();
            return;
        }
        if (spec == null) {                                              // never saved, so this is a stray (a plugin or a command made one): it has nothing to do
            discard();
        } else if (tickCount >= total) {
            MeteorFall.land(this);
        } else if (MeteorRules.expired(tickCount, total)) {
            discard();
        }
    }

    /** A few particles behind the rock: none beyond {@link MeteorRules#TRAIL_DISTANCE} of every player this client knows, and never more than the client particle budget allows. */
    private void trail() {
        int per = MeteorClientConfig.particlesPerTick();
        if (per <= 0 || level().getNearestPlayer(getX(), getY(), getZ(), MeteorRules.TRAIL_DISTANCE, false) == null) return;
        int n = PerfBudgets.clientParticles(level().getGameTime(), per);
        if (n <= 0) return;
        double dx = getX() - xo, dy = getY() - yo, dz = getZ() - zo, len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0E-4) return;
        dx /= len;
        dy /= len;
        dz /= len;
        MeteorStyle st = style();
        ParticleOptions[] kinds = particles(st);
        float size = size();
        double cy = getY() + getBbHeight() * 0.5;
        for (int i = 0; i < n; i++) {
            double back = (0.3 + random.nextDouble() * 2.6) * size, spread = 0.35 * size;
            level().addParticle(kinds[i & 1], getX() - dx * back + (random.nextDouble() - 0.5) * spread, cy - dy * back + (random.nextDouble() - 0.5) * spread,
                    getZ() - dz * back + (random.nextDouble() - 0.5) * spread, -dx * 0.05, -dy * 0.05, -dz * 0.05);
        }
    }

    private static ParticleOptions[] particles(MeteorStyle s) {
        return switch (s) {
            case FIRE -> new ParticleOptions[]{ParticleTypes.FLAME, ParticleTypes.LARGE_SMOKE};
            case ICE -> new ParticleOptions[]{ParticleTypes.SNOWFLAKE, ParticleTypes.CLOUD};
            case DARK -> new ParticleOptions[]{ParticleTypes.SOUL_FIRE_FLAME, ParticleTypes.SMOKE};
            case KI -> new ParticleOptions[]{ParticleTypes.ELECTRIC_SPARK, ParticleTypes.END_ROD};
            case HOLY -> new ParticleOptions[]{ParticleTypes.END_ROD, ParticleTypes.GLOW};
            case STAR -> new ParticleOptions[]{ParticleTypes.END_ROD, ParticleTypes.FIREWORK};
        };
    }

    // ---------------------------------------------------------------- the entity shell
    /** Never saved (the type says so too); a meteor in the air when the world closes is gone. */
    @Override public boolean shouldBeSaved() { return false; }

    @Override protected void readAdditionalSaveData(CompoundTag tag) { }

    @Override protected void addAdditionalSaveData(CompoundTag tag) { }

    @Override public boolean isPickable() { return false; }

    @Override public boolean isAttackable() { return false; }

    @Override public boolean isNoGravity() { return true; }

    /** The tail reaches several blocks behind the rock, so the culling box is wide. */
    @Override
    public AABB getBoundingBoxForCulling() { return getBoundingBox().inflate(2.0 + 5.0 * size()); }

    /** Beyond {@link MeteorRules#DRAW_DISTANCE} blocks a meteor is not drawn at all. */
    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSq) { return distanceSq < MeteorRules.DRAW_DISTANCE * MeteorRules.DRAW_DISTANCE; }

    /** The ground level the server stores for the landing (used by the self-test and the landing). */
    ServerLevel serverLevel() { return level() instanceof ServerLevel s ? s : null; }
}
