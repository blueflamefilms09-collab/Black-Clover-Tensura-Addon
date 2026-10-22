package com.newuniverse.nusmp.client.storm;

import com.newuniverse.nusmp.fairytail.storm.StormComp;
import com.newuniverse.nusmp.fairytail.storm.StormMath;
import com.newuniverse.nusmp.particle.fx.FxCatalog;
import com.newuniverse.nusmp.particle.fx.FxConfig;
import com.newuniverse.nusmp.particle.fx.FxTypes;
import com.newuniverse.nusmp.particle.fx.TintedParticleOptions;
import com.newuniverse.nusmp.particle.fx.TintedParticleType;
import com.newuniverse.nusmp.perf.PerfBudgets;
import com.newuniverse.nusmp.perf.PerfSettings;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.VfxInstance;
import com.newuniverse.nusmp.vfx.client.layer.StormLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the picture of a storm (cloud, rain, hail, bolts) is drawn by {@link StormLayer} in the VFX buffer, but a storm also splashes: drops that burst on the ground, hail that breaks
 * into ice where it lands, spray along a surge, leaves torn up by a gale front. Those are particles, and in this mod particles are the mod's OWN custom ones only (the {@code particle/fx}
 * library: {@code droplet}, {@code shard_ice}, {@code water_splash}, {@code leaf_green}), never a vanilla particle: with {@code particles.custom} off nothing is spawned here. Everything
 * goes through the shared client budget ({@link PerfBudgets#clientParticles}) and is thinned by the quality level and the distance, and a storm more than 40 blocks from the camera
 * spawns nothing. The positions are the same functions the server uses ({@link StormMath}), so a stone's ice breaks where its damage lands. Client side only; installed once by the VFX manager.
 */
public final class StormClientFx {
    private StormClientFx() {}

    private static final double NEAR = 40.0;

    /** Hooks the particles into the storm layer (idempotent). */
    public static void install() { StormLayer.particleHook = StormClientFx::tick; }

    /** The options of the four particles, built once and kept (a spawn allocates no options). Null until the registry is bound; then built on first use. */
    private static ParticleOptions droplet, shard, splash, leaf;
    private static final int DROPLET = 0, SHARD = 1, SPLASH = 2, LEAF = 3;

    private static ParticleOptions option(int which) {
        switch (which) {
            case DROPLET: if (droplet == null) droplet = opts("droplet", 0xB4D2F5, 0.8f); return droplet;
            case SHARD: if (shard == null) shard = opts("shard_ice", 0xDCEBFF, 0.7f); return shard;
            case SPLASH: if (splash == null) splash = opts("water_splash", 0x9CC4F0, 0.9f); return splash;
            default: if (leaf == null) leaf = opts("leaf_green", 0x7CC46A, 0.8f); return leaf;
        }
    }

    private static ParticleOptions opts(String id, int rgb, float scale) {
        TintedParticleType type = FxTypes.type(id);
        if (type == null) return null;
        FxCatalog.Def d = FxCatalog.byId(id);
        boolean tintable = d != null && d.tint();
        return new TintedParticleOptions(type, tintable ? rgb : -1, scale, false, tintable);
    }

    private static void emit(ClientLevel level, int which, double x, double y, double z, double vx, double vy, double vz) {
        ParticleOptions o = option(which);
        if (o == null || PerfBudgets.clientParticles(level.getGameTime(), 1) <= 0) return;
        level.addParticle(o, x, y, z, vx, vy, vz);
    }

    /** At most this many storms spawn ground particles in one client tick (a sky full of storms is not a particle storm), and each gets at most {@link #PER_STORM} spawns a tick. */
    private static final int EMITTERS_PER_TICK = 2, PER_STORM = 14;
    private static long emitTick = Long.MIN_VALUE;
    private static int emitters;

    private static void tick(VfxInstance inst) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null || !FxConfig.clientCustom()) return;           // custom particles only: with them off nothing is spawned here
        float detail = PerfSettings.vfxQuality().detail;
        if (detail <= 0f) return;
        long now = level.getGameTime();
        if (now != emitTick) {
            emitTick = now;
            emitters = 0;
        }
        int age = inst.age();
        RandomSource rnd = level.random;
        if (inst.shape == VfxShape.STORM_FALL) {
            Vec3 f = inst.payload.from(), t = inst.payload.to();
            double cx = StormMath.lerpAt(f.x, t.x, age, inst.duration), cy = StormMath.lerpAt(f.y, t.y, age, inst.duration), cz = StormMath.lerpAt(f.z, t.z, age, inst.duration);
            if (mc.player.distanceToSqr(cx, cy, cz) > NEAR * NEAR) return;
            int mask = StormMath.maskOf(inst.seed);
            boolean rain = StormComp.has(mask, StormComp.RAIN) && age % 2 == 0, hailLands = StormComp.has(mask, StormComp.HAIL) && StormMath.hailVolleyLanding(age) >= 0;
            if (!rain && !hailLands) return;
            if (emitters >= EMITTERS_PER_TICK) return;
            emitters++;
            double r = StormMath.radiusAt(inst.power, age, inst.duration);
            int budget = PER_STORM;
            if (rain) {
                int n = Math.min(budget, Math.round((StormMath.spellOf(inst.seed) == 1 ? 4 : 2) * detail));
                budget -= n;
                for (int i = 0; i < n; i++) {
                    double a = rnd.nextDouble() * Math.PI * 2, d = Math.sqrt(rnd.nextDouble()) * r;
                    emit(level, DROPLET, cx + Math.cos(a) * d, cy + 0.2, cz + Math.sin(a) * d, 0.0, 0.1, 0.0);
                }
            }
            if (hailLands) {
                int v = StormMath.hailVolleyLanding(age);
                double vr = StormMath.radiusAt(inst.power, v * StormMath.HAIL_PERIOD, inst.duration);
                int per = StormMath.hailPerVolley(inst.power);
                for (int i = 0; i < per && budget > 0; i++) {
                    double x = cx + StormMath.hailStoneX(inst.seed, v, i, vr), z = cz + StormMath.hailStoneZ(inst.seed, v, i, vr);
                    int n = Math.min(budget, Math.max(1, Math.round(2 * detail)));
                    budget -= n;
                    for (int k = 0; k < n; k++) emit(level, SHARD, x, cy + 0.3, z, (rnd.nextDouble() - 0.5) * 0.2, 0.12 + rnd.nextDouble() * 0.12, (rnd.nextDouble() - 0.5) * 0.2);
                }
            }
        } else if (inst.shape == VfxShape.STORM_FRONT) {
            Vec3 f = inst.payload.from(), t = inst.payload.to();
            double dx = t.x - f.x, dz = t.z - f.z, len = Math.sqrt(dx * dx + dz * dz);
            if (len < 0.1) return;
            dx /= len;
            dz /= len;
            double head = StormMath.frontPos(age, inst.duration, len);
            double hx = f.x + dx * head, hz = f.z + dz * head;
            if (mc.player.distanceToSqr(hx, f.y, hz) > NEAR * NEAR) return;
            if (emitters >= EMITTERS_PER_TICK) return;
            emitters++;
            boolean surge = StormComp.has(StormMath.maskOf(inst.seed), StormComp.SURGE);
            double half = inst.power;
            int n = Math.min(PER_STORM, Math.round(2 * detail));
            for (int i = 0; i < n; i++) {
                double lat = (rnd.nextDouble() - 0.5) * 2 * half;
                double x = hx - dz * lat, z = hz + dx * lat;
                if (surge) {
                    if (age % 3 == 0) emit(level, SPLASH, x, f.y + 0.1, z, 0, 0.02, 0);
                    emit(level, DROPLET, x, f.y + 1.2, z, dx * 0.15, 0.12, dz * 0.15);
                } else {
                    emit(level, LEAF, x, f.y + 0.6 + rnd.nextDouble() * 1.6, z, dx * 0.3, 0.05, dz * 0.3);
                }
            }
        }
    }
}
