package com.newuniverse.nusmp.particle.fx;

import com.newuniverse.nusmp.client.particle.fx.FxClientLod;
import com.newuniverse.nusmp.particle.rukh.RukhParticles;
import com.newuniverse.nusmp.perf.PerfBudgets;
import com.newuniverse.nusmp.perf.PerfSettings;
import com.newuniverse.nusmp.perf.VfxQuality;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Why: the one door every effect uses to spawn the mod's custom particles. A call names a THEME ({@link ThemeKey}: {@code magic.FLAME}, {@code relic.grade.GOD}, {@code boss.zenon}, ...) and what
 * it wants of it (a slot, a tone, or the old vanilla particle it replaces), and the facade picks the theme's particle and colour from {@code data/nusmp/particle_themes.json}, goes through the
 * performance budgets ({@code PerfBudgets}: the server particle allowance per tick, the client one) and the quality level ({@code vfxQuality}: fewer particles on a weak machine, fewer far from
 * the camera), and sends it: on the server as one particle packet, on the client straight into the particle engine. Everything is allocation-free per call (the particle options of a theme are
 * built once and cached on its {@link ThemeKey}). With {@code particles.custom} switched off (server file for the server, client file for the client) the facade spawns the old vanilla
 * particle of the mapping instead, so the old look is one switch away. It never throws: a bad theme falls back to the default theme, a registry that is not bound yet to the vanilla particle.
 *
 * <p>The three ways to ask, in the order the codemod ({@code tools/particles/codemod.py}) produces them: {@code spray(level, theme, FxOld, ...)} replaces a vanilla
 * {@code sendParticles(ParticleTypes.X, ...)} (same counts, spreads and speeds); {@code spray(level, theme, FxTone, ...)} and the {@code burst}/{@code stream}/{@code gather}/{@code aura}
 * family replace the Light/Dark Rukh calls (same caps as the Rukh emitters had); {@code send}, {@code burst(.., count, spread)}, {@code ring}, {@code stream} are the theme-first calls new code
 * should use.</p>
 */
public final class Fx {
    private Fx() {}

    /** The ready-made options of a theme: four slots, light and dark. */
    private static final class Resolved {
        final TintedParticleOptions[] light = new TintedParticleOptions[4];
        final TintedParticleOptions[] dark = new TintedParticleOptions[4];
        /** The replacement of each vanilla particle in this theme, filled on first use (one object per theme and vanilla particle, never per call). */
        final TintedParticleOptions[] old = new TintedParticleOptions[FxOld.values().length];
    }

    // ------------------------------------------------------------------------------------------------- options
    private static Resolved resolved(ThemeKey k) {
        int e = ParticleThemes.epoch();
        Object c = k.cache;
        if (c != null && k.cacheEpoch == e) return (Resolved) c;
        if (!FxTypes.bound()) return null;
        Resolved r = build(k.theme());
        k.cache = r;
        k.cacheEpoch = e;
        return r;
    }

    private static Resolved build(ParticleThemes.Theme t) {
        Resolved r = new Resolved();
        for (FxSlot s : FxSlot.values()) {
            r.light[s.index()] = make(t, s, false);
            r.dark[s.index()] = make(t, s, true);
        }
        return r;
    }

    private static TintedParticleOptions make(ParticleThemes.Theme t, FxSlot s, boolean dark) {
        String id = t.particle(s, dark);
        TintedParticleType type = FxTypes.type(id);
        FxCatalog.Def d = FxCatalog.byId(id);
        if (type == null || d == null) {
            id = dark ? "void_speck" : "mote";
            type = FxTypes.type(id);
            d = FxCatalog.byId(id);
        }
        boolean tintable = d != null && d.tint();
        return new TintedParticleOptions(type, tintable ? t.color(dark) : -1, 1.0f, dark, tintable);
    }

    /** The options that replace a vanilla particle in a theme: its slot's particle (dyed with the entry's colour if it has one), or the entry's fixed particle. Cached per theme. */
    private static TintedParticleOptions oldOptions(ThemeKey k, FxOld old) {
        Resolved r = resolved(k);
        if (r == null) return null;
        TintedParticleOptions o = r.old[old.ordinal()];
        if (o != null) return o;
        if (old.fixed != null) {
            TintedParticleType type = FxTypes.type(old.fixed);
            FxCatalog.Def d = FxCatalog.byId(old.fixed);
            if (type == null || d == null) return null;
            boolean tintable = d.tint();
            int rgb = old.rgb >= 0 ? old.rgb : (old.themed && tintable ? k.theme().color(false) : -1);
            o = new TintedParticleOptions(type, tintable ? rgb : -1, 1.0f, false, tintable);
        } else {
            o = r.light[old.slot.index()].withRgb(old.rgb);
        }
        r.old[old.ordinal()] = o;
        return o;
    }

    /** The particle options of a theme's slot in a tone; null only if the registry is not bound yet. Cached, no allocation. */
    public static ParticleOptions options(ThemeKey k, FxSlot slot, FxTone tone) {
        Resolved r = resolved(k);
        return r == null ? null : (tone == FxTone.DARK ? r.dark : r.light)[slot.index()];
    }

    public static ParticleOptions options(ThemeKey k, FxSlot slot) {
        return options(k, slot, FxTone.LIGHT);
    }

    /** The theme's signature particle in a tone (the replacement of a Light or Dark Rukh used as a value). Never null: falls back to the Rukh type or a vanilla glow. */
    public static ParticleOptions options(ThemeKey k, FxTone tone) {
        ParticleOptions o = options(k, FxSlot.MOTE, tone);
        return o != null ? o : fallback(FxSlot.MOTE, tone);
    }

    /** The replacement of a vanilla particle in a theme: the theme's particle of the vanilla one's slot (or the vanilla one itself if the custom ones are unavailable). */
    public static ParticleOptions options(ThemeKey k, FxOld old) {
        ParticleOptions o = FxConfig.valueCustom() ? oldOptions(k, old) : null;
        return o != null ? o : old.vanilla();
    }

    /** The theme's signature options by theme id; never null (for tables of constants that name their theme as a string). */
    public static ParticleOptions primary(String themeId) {
        return options(ThemeKey.of(themeId), FxTone.LIGHT);
    }

    /** A coloured, sized version of the theme's signature particle (the replacement of a coloured dust option). */
    public static ParticleOptions dust(ThemeKey k, Vector3f color, float size) {
        ParticleOptions o = options(k, FxSlot.MOTE, FxTone.LIGHT);
        if (!(o instanceof TintedParticleOptions t)) return ParticleTypes.GLOW;
        int rgb = (Math.round(clamp01(color.x) * 255) << 16) | (Math.round(clamp01(color.y) * 255) << 8) | Math.round(clamp01(color.z) * 255);
        return t.with(rgb, size);
    }

    /** The tone options came from: DARK for the dark flavour of a theme, else LIGHT. */
    public static FxTone toneOf(ParticleOptions o) {
        return o instanceof TintedParticleOptions t && t.dark() ? FxTone.DARK : FxTone.LIGHT;
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    /** What replaces the custom particle when {@code particles.custom} is off or the registry is not bound: the old vanilla look of the slot (Light/Dark: the Rukh of the old code). */
    public static ParticleOptions fallback(FxSlot slot, FxTone tone) {
        if (tone == FxTone.DARK) {
            return switch (slot) {
                case MOTE -> RukhParticles.dark();
                case ACCENT -> ParticleTypes.WITCH;
                case SPARK -> ParticleTypes.SOUL_FIRE_FLAME;
                case AMBIENT -> ParticleTypes.SQUID_INK;
            };
        }
        return switch (slot) {
            case MOTE -> ParticleTypes.END_ROD;
            case ACCENT -> ParticleTypes.ENCHANT;
            case SPARK -> ParticleTypes.CRIT;
            case AMBIENT -> ParticleTypes.CLOUD;
        };
    }

    private static boolean custom(Level l) {
        return l.isClientSide ? FxConfig.clientCustom() : FxConfig.serverCustom();
    }

    // ------------------------------------------------------------------------------------------------- the core emitters
    /** Server: vanilla sendParticles semantics (count particles around the point, velocity = gaussian * speed; count 0 = one directed particle). Returns the count sent. */
    private static int emitServer(ServerLevel sl, ParticleOptions o, int count, double x, double y, double z, double dx, double dy, double dz, double speed, int hardCap) {
        if (o == null) return 0;
        if (count <= 0) {
            if (PerfBudgets.particles(sl.getGameTime(), 1) <= 0) return 0;
            sl.sendParticles(o, x, y, z, 0, dx, dy, dz, speed);
            return 1;
        }
        VfxQuality q = PerfSettings.commonQuality();
        int n = FxMath.scaledCount(Math.min(count, hardCap), q.detail, 1f, FxMath.perCallCap(q.name()));
        n = PerfBudgets.particles(sl.getGameTime(), n);
        if (n > 0) sl.sendParticles(o, x, y, z, n, dx, dy, dz, speed);
        return n;
    }

    /** Client: {@code count} particles at the point, each with the velocity (dx, dy, dz) * speed (what the Rukh client twin did). Returns the count spawned. */
    private static int emitClient(Level l, ParticleOptions o, int count, double x, double y, double z, double dx, double dy, double dz, double speed, int hardCap) {
        if (o == null) return 0;
        VfxQuality q = PerfSettings.vfxQuality();
        float dd = FxClientLod.distanceDetail(x, y, z, q);
        int n = FxMath.scaledCount(Math.max(1, Math.min(count, hardCap)), q.detail, dd, FxMath.perCallCap(q.name()));
        n = PerfBudgets.clientParticles(l.getGameTime(), n);
        for (int i = 0; i < n; i++) l.addParticle(o, x, y, z, FxGeo.clampSpeed(dx * speed), FxGeo.clampSpeed(dy * speed), FxGeo.clampSpeed(dz * speed));
        return n;
    }

    /** Client twin of a vanilla {@code sendParticles} with spread (used when a burst runs on the client). */
    private static int emitClientSpread(Level l, ParticleOptions o, int count, double x, double y, double z, double sx, double sy, double sz, double speed, int hardCap) {
        if (o == null) return 0;
        VfxQuality q = PerfSettings.vfxQuality();
        float dd = FxClientLod.distanceDetail(x, y, z, q);
        int n = FxMath.scaledCount(Math.max(1, Math.min(count, hardCap)), q.detail, dd, FxMath.perCallCap(q.name()));
        n = PerfBudgets.clientParticles(l.getGameTime(), n);
        RandomSource r = l.getRandom();
        for (int i = 0; i < n; i++) {
            l.addParticle(o, x + r.nextGaussian() * sx, y + r.nextGaussian() * sy, z + r.nextGaussian() * sz, FxGeo.clampSpeed(r.nextGaussian() * speed), FxGeo.clampSpeed(r.nextGaussian() * speed), FxGeo.clampSpeed(r.nextGaussian() * speed));
        }
        return n;
    }

    /** Spawns a particle option on either side with the right semantics; the shared tail of the public sprays. */
    private static int dispatch(Level l, ParticleOptions custom, ParticleOptions off, int count, double x, double y, double z, double dx, double dy, double dz, double speed, int hardCap) {
        if (l == null) return 0;
        ParticleOptions o = custom != null && custom(l) ? custom : off;
        if (l instanceof ServerLevel sl) return emitServer(sl, o, count, x, y, z, dx, dy, dz, speed, hardCap);
        if (!l.isClientSide) return 0;
        return emitClient(l, o, count, x, y, z, dx, dy, dz, speed, hardCap);
    }

    // ------------------------------------------------------------------------------------------------- spray: drop-ins for sendParticles / addParticle
    /** Replacement of {@code level.sendParticles(ParticleTypes.X, ...)}: same count, spread and speed, the theme's particle of the vanilla one's slot. Returns the count sent. */
    public static int spray(Level l, ThemeKey k, FxOld old, double x, double y, double z, int count, double dx, double dy, double dz, double speed) {
        if (l == null) return 0;
        return dispatch(l, tinted(k, old), old.vanilla(), count, x, y, z, dx, dy, dz, speed, 4096);
    }

    private static ParticleOptions tinted(ThemeKey k, FxOld old) {
        return oldOptions(k, old);
    }

    /** The same for a slot of the theme (new code). */
    public static int spray(Level l, ThemeKey k, FxSlot slot, double x, double y, double z, int count, double dx, double dy, double dz, double speed) {
        if (l == null) return 0;
        return dispatch(l, options(k, slot, FxTone.LIGHT), fallback(slot, FxTone.LIGHT), count, x, y, z, dx, dy, dz, speed, 4096);
    }

    /** Replacement of {@code RukhFx.spray / RukhClientFx.spray}: the theme's light or dark particle, with the Rukh emitters' cap of 12 per call. */
    public static int spray(Level l, ThemeKey k, FxTone tone, double x, double y, double z, int count, double dx, double dy, double dz, double speed) {
        if (l == null || tone == null) return 0;
        ParticleOptions c = options(k, FxSlot.MOTE, tone);
        ParticleOptions off = fallback(FxSlot.MOTE, tone);
        if (count <= 0) return dispatch(l, c, off, 0, x, y, z, dx, dy, dz, speed, FxGeo.MAX_BURST);
        return dispatch(l, c, off, Math.max(1, FxGeo.clamp(count, FxGeo.MAX_BURST)), x, y, z, dx, dy, dz, speed, FxGeo.MAX_BURST);
    }

    /** One tinted particle of a theme slot with an explicit colour and size (coloured dust and the like). */
    public static int sprayTinted(Level l, ThemeKey k, FxSlot slot, int rgb, float scale, double x, double y, double z, int count, double dx, double dy, double dz, double speed) {
        if (l == null) return 0;
        ParticleOptions o = options(k, slot, FxTone.LIGHT);
        ParticleOptions c = o instanceof TintedParticleOptions t ? t.with(rgb, scale) : o;
        return dispatch(l, c, fallback(slot, FxTone.LIGHT), count, x, y, z, dx, dy, dz, speed, 4096);
    }

    /** Replacement of {@code level.sendParticles(new DustParticleOptions(color, size), ...)}: the theme's signature particle dyed with that colour (the old dust when custom particles are off). */
    public static int sprayDust(Level l, ThemeKey k, Vector3f color, float size, double x, double y, double z, int count, double dx, double dy, double dz, double speed) {
        if (l == null) return 0;
        ParticleOptions off = new net.minecraft.core.particles.DustParticleOptions(color, size);
        return dispatch(l, dust(k, color, size), off, count, x, y, z, dx, dy, dz, speed, 4096);
    }

    /** Replacement of {@code level.addParticle(new DustParticleOptions(color, size), x, y, z, vx, vy, vz)}. */
    public static void addDust(Level l, ThemeKey k, Vector3f color, float size, double x, double y, double z, double vx, double vy, double vz) {
        if (l == null || !l.isClientSide) return;
        ParticleOptions o = custom(l) ? dust(k, color, size) : new net.minecraft.core.particles.DustParticleOptions(color, size);
        if (FxClientLod.distanceDetail(x, y, z, PerfSettings.vfxQuality()) <= 0f) return;
        if (PerfBudgets.clientParticles(l.getGameTime(), 1) <= 0) return;
        l.addParticle(o, x, y, z, vx, vy, vz);
    }

    /** Replacement of {@code level.addParticle(ParticleTypes.X, x, y, z, vx, vy, vz)} (client levels; on a server level it does nothing, like the original). */
    public static void add(Level l, ThemeKey k, FxOld old, double x, double y, double z, double vx, double vy, double vz) {
        if (l == null || !l.isClientSide) return;
        ParticleOptions o = custom(l) ? tinted(k, old) : old.vanilla();
        if (o == null) o = old.vanilla();
        VfxQuality q = PerfSettings.vfxQuality();
        if (FxClientLod.distanceDetail(x, y, z, q) <= 0f) return;
        if (PerfBudgets.clientParticles(l.getGameTime(), 1) <= 0) return;
        l.addParticle(o, x, y, z, vx, vy, vz);
    }

    /** {@link #add(Level, ThemeKey, FxOld, double, double, double, double, double, double)} with a ready particle option (the replacement of {@code addParticle(rukhType, ...)}). */
    public static void add(Level l, ThemeKey k, FxTone tone, double x, double y, double z, double vx, double vy, double vz) {
        if (l == null || !l.isClientSide || tone == null) return;
        ParticleOptions o = custom(l) ? options(k, FxSlot.MOTE, tone) : fallback(FxSlot.MOTE, tone);
        if (o == null) o = fallback(FxSlot.MOTE, tone);
        VfxQuality q = PerfSettings.vfxQuality();
        if (FxClientLod.distanceDetail(x, y, z, q) <= 0f) return;
        if (PerfBudgets.clientParticles(l.getGameTime(), 1) <= 0) return;
        l.addParticle(o, x, y, z, vx, vy, vz);
    }

    // ------------------------------------------------------------------------------------------------- Rukh-style emitters (theme + tone)
    private static Vec3 centre(Entity e) {
        return new Vec3(e.getX(), e.getY() + e.getBbHeight() * 0.6, e.getZ());
    }

    private static int spread(Level l, ThemeKey k, FxTone tone, int count, double x, double y, double z, double sx, double sy, double sz, double speed, int cap) {
        if (l == null || tone == null) return 0;
        ParticleOptions c = options(k, FxSlot.MOTE, tone);
        ParticleOptions off = fallback(FxSlot.MOTE, tone);
        ParticleOptions o = c != null && custom(l) ? c : off;
        int n = FxGeo.clamp(count, cap);
        if (n <= 0) return 0;
        if (l instanceof ServerLevel sl) return emitServer(sl, o, n, x, y, z, sx, sy, sz, speed, cap);
        if (!l.isClientSide) return 0;
        return emitClientSpread(l, o, n, x, y, z, sx, sy, sz, speed, cap);
    }

    public static void burst(Level l, ThemeKey k, Vec3 pos, FxTone tone, int count) {
        burst(l, k, pos, tone, count, 0.35);
    }

    public static void burst(Level l, ThemeKey k, double x, double y, double z, FxTone tone, int count) {
        burst(l, k, new Vec3(x, y, z), tone, count, 0.35);
    }

    public static void burst(Level l, ThemeKey k, Entity at, FxTone tone, int count) {
        if (at != null) burst(l, k, centre(at), tone, count, Math.max(0.25, at.getBbWidth() * 0.5));
    }

    public static void burst(Level l, ThemeKey k, Vec3 pos, FxTone tone, int count, double spread) {
        if (pos == null) return;
        double s = Math.max(0, spread);
        spread(l, k, tone, count, pos.x, pos.y, pos.z, s, s * 0.8, s, 0.03, FxGeo.MAX_BURST);
    }

    public static void stream(Level l, ThemeKey k, Vec3 from, Vec3 to, FxTone tone, int perTick) {
        if (l == null || from == null || to == null || tone == null) return;
        int n = FxGeo.clamp(perTick, FxGeo.MAX_STREAM);
        if (n <= 0) return;
        double[] v = FxGeo.toward(to.x - from.x, to.y - from.y, to.z - from.z, FxGeo.FLIGHT_TICKS);
        RandomSource r = l.getRandom();
        for (int i = 0; i < n; i++) {
            double j = r.nextDouble() * 0.18;
            spray(l, k, tone, from.x + (to.x - from.x) * j + (r.nextDouble() - 0.5) * 0.3, from.y + (to.y - from.y) * j + (r.nextDouble() - 0.5) * 0.3,
                    from.z + (to.z - from.z) * j + (r.nextDouble() - 0.5) * 0.3, 0, v[0], v[1], v[2], 1.0);
        }
    }

    public static void stream(Level l, ThemeKey k, Entity from, Entity to, FxTone tone, int perTick) {
        if (from != null && to != null) stream(l, k, centre(from), centre(to), tone, perTick);
    }

    public static void gather(Level l, ThemeKey k, LivingEntity target, FxTone tone, int count) {
        if (l == null || target == null || tone == null) return;
        int n = FxGeo.clamp(count, FxGeo.MAX_GATHER);
        if (n <= 0) return;
        Vec3 c = centre(target);
        RandomSource r = l.getRandom();
        for (int i = 0; i < n; i++) {
            double[] ring = FxGeo.ring(r.nextDouble(), FxGeo.slotAngle(i, n, r.nextDouble()));
            double px = c.x + ring[0], py = c.y + (r.nextDouble() - 0.5) * 1.6, pz = c.z + ring[1];
            double[] v = FxGeo.toward(c.x - px, c.y - py, c.z - pz, FxGeo.FLIGHT_TICKS);
            spray(l, k, tone, px, py, pz, 0, v[0], v[1], v[2], 1.0);
        }
    }

    public static void aura(Level l, ThemeKey k, Entity e, FxTone tone) {
        if (l == null || e == null || tone == null) return;
        Vec3 c = centre(e);
        spread(l, k, tone, FxGeo.MAX_AURA, c.x, c.y, c.z, e.getBbWidth() * 0.6, e.getBbHeight() * 0.4, e.getBbWidth() * 0.6, 0.01, FxGeo.MAX_AURA);
    }

    // ------------------------------------------------------------------------------------------------- theme-first calls (new code)
    /** A puff of the theme's particles around a point: the count is shared over its particles (40/30/20/10 percent). Returns the count sent. */
    public static int send(Level l, ThemeKey k, double x, double y, double z, int count, double spread) {
        return send(l, k, FxTone.LIGHT, x, y, z, count, spread, 0.04);
    }

    public static int send(Level l, ThemeKey k, FxTone tone, double x, double y, double z, int count, double spread, double speed) {
        if (l == null || count <= 0) return 0;
        Resolved r = resolved(k);
        int[] share = FxGeo.shares(count, 4);
        int sent = 0;
        for (FxSlot s : FxSlot.values()) {
            int n = share[s.index()];
            if (n <= 0) continue;
            ParticleOptions c = r == null ? null : (tone == FxTone.DARK ? r.dark : r.light)[s.index()];
            sent += dispatchSpread(l, c, fallback(s, tone), n, x, y, z, spread, spread * 0.8, spread, speed);
        }
        return sent;
    }

    private static int dispatchSpread(Level l, ParticleOptions custom, ParticleOptions off, int count, double x, double y, double z, double sx, double sy, double sz, double speed) {
        ParticleOptions o = custom != null && custom(l) ? custom : off;
        if (l instanceof ServerLevel sl) return emitServer(sl, o, count, x, y, z, sx, sy, sz, speed, 4096);
        if (!l.isClientSide) return 0;
        return emitClientSpread(l, o, count, x, y, z, sx, sy, sz, speed, 4096);
    }

    /** A bigger burst of the theme at a point (faster, wider). */
    public static int burst(Level l, ThemeKey k, double x, double y, double z, int count, double spread) {
        return send(l, k, FxTone.LIGHT, x, y, z, count, spread, 0.12);
    }

    /** A flat ring of the theme's signature particle around a centre, drifting outwards. {@code points} is capped by the quality level. */
    public static int ring(Level l, ThemeKey k, double cx, double cy, double cz, double radius, int points) {
        return ring(l, k, FxTone.LIGHT, cx, cy, cz, radius, points);
    }

    public static int ring(Level l, ThemeKey k, FxTone tone, double cx, double cy, double cz, double radius, int points) {
        if (l == null || points <= 0) return 0;
        VfxQuality q = PerfSettings.vfxQuality();
        int n = FxMath.scaledCount(points, q.detail, 1f, 96);
        int sent = 0;
        for (int i = 0; i < n; i++) {
            double a = i * (Math.PI * 2 / n);
            sent += spray(l, k, tone, cx + Math.cos(a) * radius, cy, cz + Math.sin(a) * radius, 0, Math.cos(a) * 0.04, 0.01, Math.sin(a) * 0.04, 1.0) > 0 ? 1 : 0;
        }
        return sent;
    }

    /** Particles from A to B (a beam of motes). */
    public static void stream(Level l, ThemeKey k, Vec3 from, Vec3 to, int perTick) {
        stream(l, k, from, to, FxTone.LIGHT, perTick);
    }
}
