package com.newuniverse.nusmp.celestial;

import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.MagicProps;
import com.newuniverse.nusmp.skill.CelestialArts;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Why: the server behaviour of the two props of Celestial Spirit Magic. {@code CELESTIAL_1} is a spirit: it steps out of its magic circle (14 ticks), casts its move
 * ({@link CelestialArts#cast}), keeps acting for as long as its move lasts ({@link CelestialArts#sustain}), stands a moment and steps back (14 ticks). It follows its
 * owner's life: when the owner dies, logs out or dismisses it, it ends. {@code CELESTIAL_2} is the Gate of the Celestial Spirit World (param 0: only a door of light
 * with its circle, for the ritual) or the Spirit King himself called as a summon (param 1: a few waves of falling stars, each marked first, then the Judgement).
 * The param the painter reads: bits 0-7 the key ordinal, 8-9 the phase (0 stepping out, 1 casting, 2 standing, 3 leaving), 16-23 ticks since the cast.
 */
public final class CelestialProps {
    private CelestialProps() {}

    public static final int ENTER = 14, LEAVE = 14, CAST_CLIP = 24;
    public static final int PHASE_SHIFT = 8, SINCE_SHIFT = 16;
    private static final String MARKS = "cel_marks";

    /** The spirit's size (the prop's scale). */
    public static float scaleOf(CelestialKey key) {
        return switch (key) {
            case TAURUS -> 1.35f;
            case SAGITTARIUS -> 1.2f;
            case CAPRICORN -> 1.1f;
            case NIKORA -> 0.45f;
            case GEMINI -> 0.9f;
            case PISCES -> 0.85f;
            case HOROLOGIUM -> 0.9f;
            case LYRA -> 0.9f;
            default -> 1.0f;
        };
    }

    /** Called once by PropRegistry. */
    public static void init() {
        MagicProps.register(com.newuniverse.nusmp.prop.PropKind.CELESTIAL_1, new MagicProps.Behavior() {
            @Override public void init(MagicPropEntity e, ServerLevel sl) { e.setSize(Math.max(0.5f, 0.9f * e.scale()), 2.1f * e.scale()); }
            @Override public void tick(MagicPropEntity e, ServerLevel sl) { spiritTick(e, sl); }
            @Override public void end(MagicPropEntity e, ServerLevel sl) {
                Vec3 c = e.position().add(0, 1.0 * e.scale(), 0);
                CelestialKey key = CelestialKey.byOrdinal(e.param() & 0xFF);
                VfxSpawn.send(sl, VfxShape.STAR_MAGIC, c, c.add(0, 1.5, 0), 0xFF000000 | (key == null ? 0xFFFFFF : key.rgb), 24, 1.1f);
                sl.sendParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 18, 0.4, 0.8, 0.4, 0.04);
            }
        });
        MagicProps.register(com.newuniverse.nusmp.prop.PropKind.CELESTIAL_2, new MagicProps.Behavior() {
            @Override public void init(MagicPropEntity e, ServerLevel sl) { e.setSize(Math.max(1f, 1.2f * e.scale()), 3.4f * e.scale()); }
            @Override public void tick(MagicPropEntity e, ServerLevel sl) { gateTick(e, sl); }
            @Override public void end(MagicPropEntity e, ServerLevel sl) {
                Vec3 c = e.position().add(0, 1.8 * e.scale(), 0);
                VfxSpawn.send(sl, VfxShape.DIVINE_PILLAR, c, c.add(0, 2, 0), 0xFFFFF3B0, 30, 1.6f);
                sl.playSound(null, e.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.2f, 1.2f);
            }
        });
    }

    // ================================================================ the place a spirit stands
    /** A spot near {@code want} where a 2-block-tall thing fits and has ground under it: the height is found by looking a few blocks up and down from the player's. */
    public static Vec3 standingPlace(ServerLevel sl, Vec3 want, double y0) {
        for (int dy = 0; dy <= 3; dy++) {
            for (int sign : new int[]{1, -1}) {
                double y = y0 + dy * sign;
                AABB body = new AABB(want.x - 0.45, y + 0.05, want.z - 0.45, want.x + 0.45, y + 2.0, want.z + 0.45);
                AABB below = new AABB(want.x - 0.3, y - 0.6, want.z - 0.3, want.x + 0.3, y - 0.05, want.z + 0.3);
                if (sl.noCollision(body) && !sl.noCollision(below)) return new Vec3(want.x, y, want.z);
            }
        }
        return new Vec3(want.x, y0, want.z);
    }

    // ================================================================ a spirit
    private static void spiritTick(MagicPropEntity e, ServerLevel sl) {
        CelestialKey key = CelestialKey.byOrdinal(e.param() & 0xFF);
        if (key == null || !(e.owner() instanceof ServerPlayer p)) { e.expire(); return; }
        int t = e.life(), max = e.maxLife();
        int castAt = ENTER;
        int phase = t < castAt ? 0 : t < castAt + CAST_CLIP ? 1 : t > max - LEAVE ? 3 : 2;
        if (t == castAt) CelestialArts.cast(e, p, key);
        if (t > castAt) CelestialArts.sustain(e, p, key, t - castAt);
        if (phase < 2 && key != CelestialKey.CAPRICORN) face(e, p, key);
        int since = Math.max(0, Math.min(255, t - castAt));
        int sync = (e.param() & 0xFF) | (phase << PHASE_SHIFT) | (since << SINCE_SHIFT);
        if (sync != e.param()) e.setParam(sync);
        e.setDeltaMovement(Vec3.ZERO);
        if (t % 6 == 0) sl.sendParticles(ParticleTypes.END_ROD, e.getX() + (e.getRandom().nextDouble() - 0.5) * e.scale(), e.getY() + e.getRandom().nextDouble() * 2 * e.scale(),
                e.getZ() + (e.getRandom().nextDouble() - 0.5) * e.scale(), 1, 0, 0.05, 0, 0.01);
        if (t == 1) sl.playSound(null, e.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 0.9f);
    }

    /** Turns the spirit toward the nearest foe within its reach (its owner's look otherwise). */
    private static void face(MagicPropEntity e, ServerPlayer p, CelestialKey key) {
        LivingEntity tg = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity x : e.level().getEntitiesOfClass(LivingEntity.class, e.getBoundingBox().inflate(key.reach + 2), v -> v != p && v.isAlive() && !(v instanceof ServerPlayer o && o.isCreative()))) {
            if (x instanceof net.minecraft.world.entity.player.Player pl && pl.isAlliedTo(p)) continue;
            if (!(x instanceof net.minecraft.world.entity.monster.Enemy) && x.getLastHurtMob() != p && !(x instanceof net.minecraft.world.entity.Mob m && m.getTarget() == p)) continue;
            double d = x.distanceToSqr(e);
            if (d < best) { best = d; tg = x; }
        }
        float yaw = tg != null ? (float) (Mth.atan2(-(tg.getX() - e.getX()), tg.getZ() - e.getZ()) * (180.0 / Math.PI)) : p.getYRot();
        e.setYRot(Mth.approachDegrees(e.getYRot(), yaw, 40f));
    }

    // ================================================================ the Gate and the king's summon
    private static void gateTick(MagicPropEntity e, ServerLevel sl) {
        int t = e.life(), max = e.maxLife();
        Vec3 c = e.position().add(0, 1.8 * e.scale(), 0);
        if (t % 4 == 0) sl.sendParticles(ParticleTypes.END_ROD, c.x + (e.getRandom().nextDouble() - 0.5) * 2, c.y + (e.getRandom().nextDouble() - 0.5) * 3 * e.scale(), c.z + (e.getRandom().nextDouble() - 0.5) * 2, 1, 0, 0.02, 0, 0.0);
        if (t == 1) {
            VfxSpawn.send(sl, VfxShape.DIVINE_PILLAR, c, c.add(0, 3, 0), 0xFFFFF3B0, 40, 2.2f);
            sl.playSound(null, e.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 1.4f, 0.8f);
        }
        if ((e.param() & 1) == 0) return;                                                         // param 0: only the door; param 1: the king
        if (!(e.owner() instanceof ServerPlayer p)) { e.expire(); return; }
        double ms = e.getPersistentData().getFloat(CelestialArts.MS);
        if (t == 20) for (LivingEntity a : CelestialArts.alliesNear(p, e.position(), 20)) {
            a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
            a.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 300, 2));
        }
        CompoundTag d = e.getPersistentData();
        if (t >= 40 && t < max - 70 && (t - 40) % 40 == 0) {                                      // telegraph: mark up to six foes with a ring of stars
            List<LivingEntity> foes = CelestialArts.foesNear(p, e.position(), 24);
            long[] marks = new long[Math.min(6, foes.size())];
            for (int i = 0; i < marks.length; i++) {
                LivingEntity f = foes.get(i);
                marks[i] = net.minecraft.core.BlockPos.asLong((int) Math.floor(f.getX()), (int) Math.floor(f.getY()), (int) Math.floor(f.getZ()));
                VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE, f.position(), f.position().add(0, 1, 0), 0xFFFFF3B0, 14, 0.9f);
                VfxSpawn.send(sl, VfxShape.STAR_MAGIC, f.position().add(0, 8, 0), f.position(), 0xFFFFF3B0, 14, 1.0f);
            }
            d.putLongArray(MARKS, marks);
        }
        if (t >= 54 && (t - 54) % 40 == 0 && t < max - 56) {                                      // the stars land
            for (long m : d.getLongArray(MARKS)) {
                net.minecraft.core.BlockPos bp = net.minecraft.core.BlockPos.of(m);
                Vec3 at = Vec3.atBottomCenterOf(bp);
                VfxSpawn.send(sl, VfxShape.FX_SPIRIT_NOVA, at, at.add(0, 1, 0), 0xFFFFF3B0, 16, 1.0f);
                sl.playSound(null, bp, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.6f, 1.6f);
                for (LivingEntity x : CelestialArts.foesNear(p, at, 3.4)) CelestialArts.kingHit(p, x, 14f, ms);
            }
            d.putLongArray(MARKS, new long[0]);
        }
        if (t == max - 60) VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE, e.position(), e.position().add(0, 1, 0), 0xFFFFF3B0, 30, 3.5f);   // the Judgement's telegraph
        if (t == max - 30) {                                                                       // the Judgement
            VfxSpawn.send(sl, VfxShape.FX_SPIRIT_CATACLYSM, e.position(), e.position().add(0, 2, 0), 0xFFFFF3B0, 30, 2.2f);
            sl.playSound(null, e.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.4f, 0.6f);
            for (LivingEntity x : CelestialArts.foesNear(p, e.position(), 14)) CelestialArts.kingHit(p, x, 40f, ms);
        }
    }
}
