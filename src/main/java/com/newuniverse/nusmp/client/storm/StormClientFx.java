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

    private static ParticleOptions opts(String id, int rgb, float scale) {
        TintedParticleType type = FxTypes.type(id);
        if (type == null) return null;
        FxCatalog.Def d = FxCatalog.byId(id);
        boolean tintable = d != null && d.tint();
        return new TintedParticleOptions(type, tintable ? rgb : -1, scale, false, tintable);
    }

    private static void emit(ClientLevel level, ParticleOptions o, double x, double y, double z, double vx, double vy, double vz) {
        if (o == null || PerfBudgets.clientParticles(level.getGameTime(), 1) <= 0) return;
        level.addParticle(o, x, y, z, vx, vy, vz);
    }

    private static double[] arr(Vec3 v) { return new double[]{v.x, v.y, v.z}; }

    private static void tick(VfxInstance inst) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null || !FxConfig.clientCustom()) return;
        int age = inst.age();
        float detail = PerfSettings.vfxQuality().detail;
        if (detail <= 0f) return;
        RandomSource rnd = level.random;
        if (inst.shape == VfxShape.STORM_FALL) {
            double[] c = StormMath.center(arr(inst.payload.from()), arr(inst.payload.to()), age, inst.duration);
            if (mc.player.distanceToSqr(c[0], c[1], c[2]) > NEAR * NEAR) return;
            double r = StormMath.radiusAt(inst.power, age, inst.duration);
            int mask = StormMath.maskOf(inst.seed);
            if (StormComp.has(mask, StormComp.RAIN) && age % 2 == 0) {
                int n = Math.round((StormMath.spellOf(inst.seed) == 1 ? 4 : 2) * detail);
                for (int i = 0; i < n; i++) {
                    double a = rnd.nextDouble() * Math.PI * 2, d = Math.sqrt(rnd.nextDouble()) * r;
                    emit(level, opts("droplet", 0xB4D2F5, 0.8f), c[0] + Math.cos(a) * d, c[1] + 0.2, c[2] + Math.sin(a) * d, 0.0, 0.1, 0.0);
                }
            }
            if (StormComp.has(mask, StormComp.HAIL)) {
                int v = StormMath.hailVolleyLanding(age);
                if (v >= 0) {
                    double vr = StormMath.radiusAt(inst.power, v * StormMath.HAIL_PERIOD, inst.duration);
                    int per = StormMath.hailPerVolley(inst.power);
                    for (int i = 0; i < per; i++) {
                        double[] off = StormMath.hailStone(inst.seed, v, i, vr);
                        double x = c[0] + off[0], z = c[2] + off[1];
                        int n = Math.max(1, Math.round(3 * detail));
                        for (int k = 0; k < n; k++) emit(level, opts("shard_ice", 0xDCEBFF, 0.7f), x, c[1] + 0.3, z, (rnd.nextDouble() - 0.5) * 0.2, 0.12 + rnd.nextDouble() * 0.12, (rnd.nextDouble() - 0.5) * 0.2);
                    }
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
            boolean surge = StormComp.has(StormMath.maskOf(inst.seed), StormComp.SURGE);
            double half = inst.power;
            int n = Math.round(2 * detail);
            for (int i = 0; i < n; i++) {
                double lat = (rnd.nextDouble() - 0.5) * 2 * half;
                double x = hx - dz * lat, z = hz + dx * lat;
                if (surge) {
                    if (age % 3 == 0) emit(level, opts("water_splash", 0x9CC4F0, 0.9f), x, f.y + 0.1, z, 0, 0.02, 0);
                    emit(level, opts("droplet", 0xB4D2F5, 0.8f), x, f.y + 1.2, z, dx * 0.15, 0.12, dz * 0.15);
                } else {
                    emit(level, opts("leaf_green", 0x7CC46A, 0.8f), x, f.y + 0.6 + rnd.nextDouble() * 1.6, z, dx * 0.3, 0.05, dz * 0.3);
                }
            }
        }
    }
}
