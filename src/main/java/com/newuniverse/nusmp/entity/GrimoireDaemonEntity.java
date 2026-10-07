package com.newuniverse.nusmp.entity;

import com.newuniverse.nusmp.book.KotodamaWords;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;
import java.util.UUID;

/**
 * 0.52: Zagred's Grimoire Daemons (docs/zagred_boss_gdd.md, section 5.1). One entity, four tiers:
 * <ul>
 *   <li><b>Lesser</b> (ash / earth): a swarmer in tattered robes with a single-leaf grimoire. Orbits its target and fires a
 *       telegraphed ash bolt every 3 s (blindness or slowness). Dies to any hit.</li>
 *   <li><b>Greater</b> (dark / fire): artillery with horns and a three-leaf grimoire emitting glowing text. Stands still,
 *       marks a circle on the ground, then burns everything in it. A blow to the back takes double and closes the book (the
 *       cast is lost).</li>
 *   <li><b>Arch</b> (spatial / forbidden): a multi-winged commander with a five-leaf grimoire. Teleports, opens gravity wells and
 *       casts Tensura spatial spells. While one lives it is Zagred's anchor: his barrier refreshes twice as fast and he takes
 *       25% less. When it falls his outermost barrier layer breaks.</li>
 *   <li><b>Rule stone</b>: one of the five glyph slabs of "Overwrite". Destroying three cuts the sentence.</li>
 * </ul>
 * They belong to one boss (BossId), never fight it or each other, and vanish when it does.
 */
public class GrimoireDaemonEntity extends Monster {
    public static final int LESSER = 0, GREATER = 1, ARCH = 2, STONE = 3;
    private static final EntityDataAccessor<Integer> TIER = SynchedEntityData.defineId(GrimoireDaemonEntity.class, EntityDataSerializers.INT);
    /** Ticks left of the current telegraph (for the model: the book snaps open and flips). */
    private static final EntityDataAccessor<Integer> CAST = SynchedEntityData.defineId(GrimoireDaemonEntity.class, EntityDataSerializers.INT);

    private UUID bossId;
    private long nextAct, nextTensura;
    private Vec3 aim = Vec3.ZERO;
    private int variant;
    private List<ResourceLocation> kit;

    public GrimoireDaemonEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        setNoGravity(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, 0.2).add(Attributes.FOLLOW_RANGE, 48)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8).add(Attributes.ATTACK_DAMAGE, 4);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(TIER, LESSER);
        b.define(CAST, 0);
    }

    public int tier() { return entityData == null ? LESSER : entityData.get(TIER); }
    public int castLeft() { return entityData.get(CAST); }
    public UUID bossId() { return bossId; }

    /** Which flavour (ash or earth for a lesser one, dark or fire for a greater one). */
    public int variant() { return variant; }

    public void initTier(int tier, UUID boss, int variant) {
        entityData.set(TIER, tier);
        bossId = boss;
        this.variant = variant;
        double hp = switch (tier) { case LESSER -> 12; case GREATER -> 70; case ARCH -> 300; default -> 70; };
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
        setHealth((float) hp);
        refreshDimensions();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (TIER.equals(key)) refreshDimensions();
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        float s = switch (tier()) { case GREATER -> 1.2f; case ARCH -> 1.7f; case STONE -> 0.9f; default -> 0.8f; };
        return super.getDefaultDimensions(pose).scale(s);
    }

    public static GrimoireDaemonEntity spawn(ServerLevel sl, ZagredBossEntity boss, int tier, Vec3 at) {
        GrimoireDaemonEntity d = NUEntities.GRIMOIRE_DAEMON.get().create(sl);
        if (d == null) return null;
        d.moveTo(at.x, at.y, at.z, boss.getRandom().nextFloat() * 360f, 0);
        d.initTier(tier, boss.getUUID(), boss.getRandom().nextInt(2));
        sl.addFreshEntity(d);
        VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, at.add(0, 1, 0), at, KotodamaWords.VIOLET, 18, tier == ARCH ? 2.4f : 1.0f);
        return d;
    }

    ZagredBossEntity boss(ServerLevel sl) {
        return bossId != null && sl.getEntity(bossId) instanceof ZagredBossEntity z && z.isAlive() ? z : null;
    }

    @Override
    public boolean isAlliedTo(Entity other) { return other instanceof ZagredBossEntity || other instanceof GrimoireDaemonEntity || super.isAlliedTo(other); }

    @Override public boolean isPushable() { return tier() != STONE && tier() != GREATER; }
    @Override public boolean fireImmune() { return true; }
    @Override protected boolean shouldDespawnInPeaceful() { return false; }

    // ---------------------------------------------------------------- behaviour
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel sl)) return;
        ZagredBossEntity boss = boss(sl);
        if (boss == null) { discard(); return; }
        long t = tickCount;
        int tier = tier();
        LivingEntity tg = boss.getTarget();
        if (tg == null || !tg.isAlive()) tg = sl.getNearestPlayer(getX(), getY(), getZ(), 40, false);
        if (tier != STONE && tg != null) getLookControl().setLookAt(tg, 40, 40);
        int cast = castLeft();
        if (cast > 0) entityData.set(CAST, cast - 1);
        if (tier == STONE || tg == null) { bob(boss.position().y + 1.2); return; }
        switch (tier) {
            case LESSER -> lesser(sl, boss, tg, t, cast);
            case GREATER -> greater(sl, boss, tg, t, cast);
            case ARCH -> arch(sl, boss, tg, t, cast);
            default -> { }
        }
    }

    /** Hover at a height with a gentle bob. */
    private void bob(double y) {
        double want = y + Math.sin(tickCount * 0.1 + getId()) * 0.15;
        setDeltaMovement(getDeltaMovement().multiply(0.8, 0.6, 0.8).add(0, Math.max(-0.08, Math.min(0.08, (want - getY()) * 0.1)), 0));
    }

    private void drift(Vec3 to, double speed) {
        Vec3 d = to.subtract(position());
        if (d.lengthSqr() < 0.04) { setDeltaMovement(getDeltaMovement().scale(0.7)); return; }
        Vec3 v = d.normalize().scale(Math.min(speed, d.length() * 0.2));
        setDeltaMovement(getDeltaMovement().scale(0.8).add(v.scale(0.2)));
        hasImpulse = true;
    }

    // ---- lesser: orbit, telegraph, ash bolt
    private void lesser(ServerLevel sl, ZagredBossEntity boss, LivingEntity tg, long t, int cast) {
        double ang = (getId() * 2.4) + t * 0.012;
        Vec3 want = tg.position().add(Math.cos(ang) * 7, 2.5, Math.sin(ang) * 7);
        drift(want, 0.14);
        if (cast == 1) bolt(sl, boss, tg);
        else if (cast == 0 && t >= nextAct) {
            aim = tg.getEyePosition();
            entityData.set(CAST, 10);
            nextAct = t + 60 + random.nextInt(20);
            sl.playSound(null, blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.HOSTILE, 1f, 0.6f);
        }
        if (cast > 0) line(sl, getEyePosition(), aim, variant == 0 ? new Vector3f(0.45f, 0.45f, 0.47f) : new Vector3f(0.45f, 0.3f, 0.18f));
    }

    private void bolt(ServerLevel sl, ZagredBossEntity boss, LivingEntity tg) {
        VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, getEyePosition(), aim, variant == 0 ? 0xFF8A8A92 : 0xFF8A6A3A, 8, 0.4f);
        for (LivingEntity t : KotodamaWords.foes(boss, aim, 1.5)) {
            t.hurt(damageSources().indirectMagic(this, boss), 4f);
            t.addEffect(new MobEffectInstance(variant == 0 ? MobEffects.BLINDNESS : MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
        }
    }

    // ---- greater: a marked circle, then fire
    private void greater(ServerLevel sl, ZagredBossEntity boss, LivingEntity tg, long t, int cast) {
        bob(boss.position().y + 0.8 + (getId() % 3) * 0.2);
        if (cast > 0) ring(sl, aim, 3.0, cast);
        if (cast == 1) {
            VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, aim.add(0, 0.5, 0), aim, variant == 0 ? 0xFF3A1E5A : 0xFFFF6A20, 14, 2f);
            sl.playSound(null, bp(aim), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1f, 0.8f);
            for (LivingEntity v : KotodamaWords.foes(boss, aim, 3.0)) {
                KotodamaWords.hurt(boss, v, 12f);
                v.igniteForSeconds(4);
            }
        } else if (cast == 0 && t >= nextAct) {
            aim = tg.position();
            entityData.set(CAST, 30);
            nextAct = t + 70 + random.nextInt(30);
        }
    }

    private static net.minecraft.core.BlockPos bp(Vec3 v) { return net.minecraft.core.BlockPos.containing(v); }

    // ---- arch: teleport, gravity wells, Tensura spatial spells
    private void arch(ServerLevel sl, ZagredBossEntity boss, LivingEntity tg, long t, int cast) {
        bob(boss.position().y + 2.2);
        if (t % 100 == 40) {                                              // shuffles to a new spot near Zagred
            double a = random.nextDouble() * Math.PI * 2, r = 6 + random.nextDouble() * 4;
            Vec3 to = boss.position().add(Math.cos(a) * r, 2.2, Math.sin(a) * r);
            if (sl.noCollision(this, getBoundingBox().move(to.subtract(position())))) {
                VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, position().add(0, 1, 0), position().add(0, 1.5, 0), 0xFFB8A0FF, 12, 1.4f);
                teleportTo(to.x, to.y, to.z);
                VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, to.add(0, 1, 0), to.add(0, 1.5, 0), 0xFFB8A0FF, 12, 1.4f);
            }
        }
        if (cast > 0) ring(sl, aim, 6.0, cast);
        if (cast == 1) well(sl, boss);
        else if (cast == 0 && t >= nextAct) {
            aim = tg.position();
            entityData.set(CAST, 40);
            nextAct = t + 160 + random.nextInt(40);
        }
        if (t >= nextTensura && distanceToSqr(tg) < 30 * 30 && boss.phase() >= 3) {
            if (kit == null) {
                kit = TensuraCaster.learn(this, 3, "gravity", "dimension", "spatial", "space", "void");
                TensuraCaster.ensureMana(this, 1_000_000);
            }
            nextTensura = t + 200;
            if (!kit.isEmpty()) TensuraCaster.cast(this, tg, kit, 10);
        }
    }

    /** A gravity well: for 3 s everything near the centre is dragged into it and bruised. */
    private void well(ServerLevel sl, ZagredBossEntity boss) {
        Vec3 c = aim;
        VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, c.add(0, 0.5, 0), c, 0xFF7A5AD0, 20, 2.6f);
        sl.playSound(null, bp(c), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.5f, 0.4f);
        SpellRuntime.zone(sl, 60, 2, age -> {
            for (LivingEntity v : KotodamaWords.foes(boss, c, 6)) {
                Vec3 pull = c.subtract(v.position());
                if (pull.lengthSqr() < 0.25) continue;
                v.setDeltaMovement(v.getDeltaMovement().add(pull.normalize().scale(0.09)));
                v.hurtMarked = true;
                if (age % 20 == 0) v.hurt(damageSources().indirectMagic(this, boss), 3f);
            }
        });
    }

    // ---- telegraph helpers (dust only; no client code)
    private void ring(ServerLevel sl, Vec3 c, double r, int cast) {
        if (cast % 3 != 0) return;
        DustParticleOptions d = new DustParticleOptions(variant == 0 ? new Vector3f(0.55f, 0.2f, 0.9f) : new Vector3f(1f, 0.35f, 0.1f), 1.3f);
        int n = (int) (r * 8);
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            sl.sendParticles(d, c.x + Math.cos(a) * r, c.y + 0.1, c.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
    }

    private void line(ServerLevel sl, Vec3 a, Vec3 b, Vector3f color) {
        DustParticleOptions d = new DustParticleOptions(color, 1.0f);
        Vec3 v = b.subtract(a);
        int n = (int) Math.max(2, v.length() * 1.5);
        for (int i = 1; i < n; i++) {
            Vec3 p = a.add(v.scale(i / (double) n));
            sl.sendParticles(d, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    // ---------------------------------------------------------------- damage
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof ZagredBossEntity || source.getEntity() instanceof GrimoireDaemonEntity) return false;
        if (tier() == GREATER && source.getEntity() instanceof LivingEntity a && !level().isClientSide) {
            Vec3 toAttacker = a.position().subtract(position()).multiply(1, 0, 1);
            Vec3 look = getViewVector(1f).multiply(1, 0, 1);
            if (toAttacker.lengthSqr() > 1e-4 && look.lengthSqr() > 1e-4 && toAttacker.normalize().dot(look.normalize()) < -0.3) {
                amount *= 2f;                                              // the back of the book: double, and the page is lost
                if (castLeft() > 0) { entityData.set(CAST, 0); nextAct = tickCount + 100; }
            }
        }
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!(level() instanceof ServerLevel sl)) return;
        VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, position().add(0, 1, 0), position(), KotodamaWords.ABYSS, 20, tier() == ARCH ? 3f : 1.2f);
        ZagredBossEntity boss = boss(sl);
        if (boss == null) return;
        if (tier() == ARCH) boss.archFell();
        else if (tier() == STONE) boss.stoneBroken();
    }

    // ---------------------------------------------------------------- client flavour
    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide || tickCount % 3 != 0) return;
        var r = getRandom();
        double x = getX() + (r.nextDouble() - 0.5) * 0.8, y = getY() + r.nextDouble() * getBbHeight(), z = getZ() + (r.nextDouble() - 0.5) * 0.8;
        switch (tier()) {
            case LESSER -> level().addParticle(ParticleTypes.ASH, x, y, z, 0, -0.01, 0);
            case GREATER -> level().addParticle(variant == 0 ? ParticleTypes.SMOKE : ParticleTypes.SMALL_FLAME, x, y, z, 0, 0.02, 0);
            case ARCH -> level().addParticle(ParticleTypes.PORTAL, x, y, z, (r.nextDouble() - 0.5), 0.2, (r.nextDouble() - 0.5));
            default -> level().addParticle(ParticleTypes.ENCHANT, x, y, z, 0, 0.1, 0);
        }
    }

    // ---------------------------------------------------------------- save
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (bossId != null) tag.putUUID("Boss", bossId);
        tag.putInt("Tier", tier());
        tag.putInt("Variant", variant);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Boss")) bossId = tag.getUUID("Boss");
        entityData.set(TIER, tag.getInt("Tier"));
        variant = tag.getInt("Variant");
    }

    /** All of one boss's daemons near 'at'. */
    public static List<GrimoireDaemonEntity> of(ServerLevel sl, UUID boss, Vec3 at, double r) {
        return sl.getEntitiesOfClass(GrimoireDaemonEntity.class, new AABB(at, at).inflate(r), d -> boss.equals(d.bossId) && d.isAlive());
    }

    /** True if 'p' is a live player we would target. */
    static boolean targetable(Entity p) { return p instanceof Player pl && !pl.isCreative() && !pl.isSpectator() && pl.isAlive(); }
}
