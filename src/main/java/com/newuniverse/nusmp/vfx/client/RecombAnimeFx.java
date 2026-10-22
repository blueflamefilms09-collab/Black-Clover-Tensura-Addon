package com.newuniverse.nusmp.vfx.client;

import com.newuniverse.nusmp.client.particle.fx.FxClientLod;
import com.newuniverse.nusmp.client.prop.RecombAnimeOptions;
import com.newuniverse.nusmp.particle.fx.FxCatalog;
import com.newuniverse.nusmp.particle.fx.FxConfig;
import com.newuniverse.nusmp.particle.fx.FxMath;
import com.newuniverse.nusmp.particle.fx.FxTypes;
import com.newuniverse.nusmp.particle.fx.TintedParticleOptions;
import com.newuniverse.nusmp.particle.fx.TintedParticleType;
import com.newuniverse.nusmp.perf.PerfBudgets;
import com.newuniverse.nusmp.perf.PerfSettings;
import com.newuniverse.nusmp.perf.VfxQuality;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * Recombination Magic, the anime look: the particles of the house-golem and of the recombined houses, all of them the mod's own custom particles (particle/fx, {@code Fx} catalog ids): a ring of
 * brown-grey dust that bursts up from the ground when a foot comes down, chunks of brick and sand that fly with it, and, when a fist lands, the fiery orange flash of the anime (a comic impact star
 * and a shock ring in orange and yellow, a fan of hot sparks flying out along the ground like spark lines, embers and tongues of fire). Client only (it asks the camera for the distance and the quality
 * level, and spawns into the client level); every spawn goes through the client particle budget ({@code PerfBudgets.clientParticles}), the quality level and the distance, so a crowd of golems cannot
 * flood a machine. Nothing here runs on a server. {@code particles.custom} off, or {@code visual.recombAnimeParticles} off, silences all of it. The decisions (counts, positions) are in the pure
 * {@code prop.RecombAnimeRules}.
 */
public final class RecombAnimeFx {
    private RecombAnimeFx() {}
    public static final int DUST = 0xFFB9A27C, DUST_DARK = 0xFF8C7A5E, FLASH = 0xFFFF8A2A, FLASH_CORE = 0xFFFFE9A0, SPARK_HOT = 0xFFFFC060, SMOKE = 0xFFC9C2B8;

    private static boolean live(Level l) {
        return l != null && l.isClientSide && FxConfig.clientCustom() && RecombAnimeOptions.particles();
    }

    /** The particle of a catalog id, dyed (if its texture takes a dye) and sized; null while the registry is not bound. */
    private static ParticleOptions opt(String id, int rgb, float scale) {
        TintedParticleType type = FxTypes.type(id);
        FxCatalog.Def d = FxCatalog.byId(id);
        if (type == null || d == null) return null;
        return new TintedParticleOptions(type, d.tint() ? (rgb & 0xFFFFFF) : -1, scale, false, d.tint());
    }

    /** How many of {@code wanted} may be spawned at this point: the quality level, the distance to the camera and the client budget decide. */
    private static int allow(Level l, double x, double y, double z, int wanted) {
        if (wanted <= 0) return 0;
        VfxQuality q = PerfSettings.vfxQuality();
        float dd = FxClientLod.distanceDetail(x, y, z, q);
        if (dd <= 0f) return 0;
        int n = FxMath.scaledCount(wanted, q.detail, dd, FxMath.perCallCap(q.name()));
        return PerfBudgets.clientParticles(l.getGameTime(), n);
    }

    private static void add(Level l, ParticleOptions o, double x, double y, double z, double vx, double vy, double vz) {
        if (o != null) l.addParticle(o, x, y, z, FxMath.clampSpeed(vx), FxMath.clampSpeed(vy), FxMath.clampSpeed(vz));
    }

    // ------------------------------------------------------------------------------------------------ dust
    /**
     * A ring of dust bursting up and out from the ground: soft smoke-coloured clouds rolling outward, sand grains and (a few) chips of brick thrown up. {@code radius} (blocks) is where the ring starts,
     * {@code size} the scale of the clouds (1 = about a block).
     */
    public static void dustRing(Level l, double x, double y, double z, double radius, int count, float size) {
        if (!live(l)) return;
        int n = allow(l, x, y, z, count);
        if (n <= 0) return;
        RandomSource r = l.getRandom();
        ParticleOptions cloud = opt("smoke_wisp", DUST, 3.4f * size), dark = opt("soot_puff", DUST_DARK, 2.0f * size);
        ParticleOptions sand = opt("sand_grain", DUST, 1.6f * size), chip = opt("earth_chip", DUST, 1.1f * size);
        for (int i = 0; i < n; i++) {
            double a = (i + r.nextDouble() * 0.7) * (Math.PI * 2 / n);
            double c = Math.cos(a), s = Math.sin(a), sp = 0.06 + r.nextDouble() * 0.1;
            double px = x + c * radius, pz = z + s * radius;
            if (i % 4 == 3) add(l, dark, px, y + 0.25, pz, c * sp * 0.8, 0.05, s * sp * 0.8);
            else add(l, cloud, px, y + 0.2 + r.nextDouble() * 0.3, pz, c * sp, 0.04 + r.nextDouble() * 0.05, s * sp);
            if (i % 2 == 0) add(l, sand, px, y + 0.3, pz, c * sp * 1.6, 0.18 + r.nextDouble() * 0.15, s * sp * 1.6);
            if (i % 5 == 0) add(l, chip, px, y + 0.3, pz, c * sp * 1.2, 0.3 + r.nextDouble() * 0.2, s * sp * 1.2);
        }
    }

    /** A foot coming down: a small ring of dust and a few grains. */
    public static void footfall(Level l, double x, double y, double z, float renderScale, int count) {
        dustRing(l, x, y, z, 0.6 * renderScale, count, 1.5f * renderScale);
    }

    /** A column of rubble and dust while the golem rises out of the ground or falls apart. */
    public static void rubble(Level l, double x, double y, double z, double radius, int count, float size) {
        if (!live(l)) return;
        int n = allow(l, x, y, z, count);
        if (n <= 0) return;
        RandomSource r = l.getRandom();
        ParticleOptions cloud = opt("smoke_wisp", DUST, 3.0f * size), chip = opt("earth_chip", DUST, 1.2f * size), ash = opt("ash_flake", DUST, 1.0f * size);
        for (int i = 0; i < n; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = Math.sqrt(r.nextDouble()) * radius;
            double px = x + Math.cos(a) * d, pz = z + Math.sin(a) * d;
            add(l, cloud, px, y + 0.1, pz, Math.cos(a) * 0.03, 0.07 + r.nextDouble() * 0.05, Math.sin(a) * 0.03);
            if (i % 2 == 0) add(l, chip, px, y + 0.2, pz, (r.nextDouble() - 0.5) * 0.18, 0.28 + r.nextDouble() * 0.25, (r.nextDouble() - 0.5) * 0.18);
            if (i % 3 == 0) add(l, ash, px, y + 0.3, pz, (r.nextDouble() - 0.5) * 0.06, 0.1, (r.nextDouble() - 0.5) * 0.06);
        }
    }

    // ------------------------------------------------------------------------------------------------ the fist lands
    /**
     * The fiery impact flash of the anime: a comic impact star in orange with a white-yellow core, a shock ring, a fan of hot sparks that fly out along the ground (spark lines), embers and tongues
     * of fire, then the dust ring and thrown bricks of {@link #dustRing}. {@code scale} is the render scale of the golem (1 = a block of flash per block of golem).
     */
    public static void fistImpact(Level l, double x, double y, double z, float scale, int sparks, int dust, int chips) {
        if (!live(l)) return;
        double my = y + 0.35 * scale;
        int f = allow(l, x, y, z, 3);
        if (f > 0) {
            add(l, opt("impact_burst", FLASH, 2.6f * scale), x, my, z, 0, 0, 0);
            add(l, opt("impact_burst", FLASH_CORE, 1.4f * scale), x, my + 0.05, z, 0, 0, 0);
            add(l, opt("shock_ring", FLASH, 2.4f * scale), x, y + 0.2, z, 0, 0, 0);
        }
        int n = allow(l, x, y, z, sparks);
        RandomSource r = l.getRandom();
        ParticleOptions spark = opt("spark", -1, 3.0f * scale), ember = opt("ember", -1, 2.4f * scale), tongue = opt("flame_tongue", -1, 2.4f * scale);
        for (int i = 0; i < n; i++) {                                         // spark lines: evenly fanned, nearly flat, fast
            double a = (i + r.nextDouble() * 0.6) * (Math.PI * 2 / Math.max(1, n));
            double sp = 0.42 + r.nextDouble() * 0.3;
            add(l, spark, x, my, z, Math.cos(a) * sp, 0.05 + r.nextDouble() * 0.12, Math.sin(a) * sp);
            if (i % 3 == 0) add(l, ember, x, my, z, Math.cos(a) * sp * 0.45, 0.12 + r.nextDouble() * 0.12, Math.sin(a) * sp * 0.45);
            if (i % 4 == 1) add(l, tongue, x, y + 0.2, z, Math.cos(a) * sp * 0.25, 0.1 + r.nextDouble() * 0.08, Math.sin(a) * sp * 0.25);
        }
        dustRing(l, x, y, z, 0.8 * scale, dust, 1.7f * scale);
        int c = allow(l, x, y, z, chips);
        ParticleOptions chip = opt("earth_chip", DUST, 2.4f * scale);
        for (int i = 0; i < c; i++) {
            double a = r.nextDouble() * Math.PI * 2, sp = 0.1 + r.nextDouble() * 0.2;
            add(l, chip, x, my, z, Math.cos(a) * sp, 0.35 + r.nextDouble() * 0.3, Math.sin(a) * sp);
        }
    }

    // ------------------------------------------------------------------------------------------------ small things
    /** A puff of chimney smoke. */
    public static void chimneySmoke(Level l, double x, double y, double z, float scale) {
        if (!live(l)) return;
        if (allow(l, x, y, z, 1) <= 0) return;
        RandomSource r = l.getRandom();
        add(l, opt("smoke_wisp", SMOKE, 2.2f * scale), x + (r.nextDouble() - 0.5) * 0.2 * scale, y, z + (r.nextDouble() - 0.5) * 0.2 * scale, (r.nextDouble() - 0.5) * 0.01, 0.045, (r.nextDouble() - 0.5) * 0.01);
    }

    /** The dust and a little rubble where a house rises or a zone is laid out (Recombination's zone and impact effects): a ring of dust and bricks round the point. */
    public static void houseDust(Level l, double x, double y, double z, double radius, float size) {
        dustRing(l, x, y, z, radius, Math.min(26, 8 + (int) (radius * 3)), Math.max(0.5f, size));
    }
}
