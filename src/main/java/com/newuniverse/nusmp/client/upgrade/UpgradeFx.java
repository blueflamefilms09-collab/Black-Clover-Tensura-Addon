package com.newuniverse.nusmp.client.upgrade;

import com.newuniverse.nusmp.perf.PerfBudgets;
import com.newuniverse.nusmp.relics.upgrade.UpgradeItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the ambient layers and the swing trail of the upgraded weapons, as client particles. Every upgraded weapon has, in its spec, six layers (rising motes, turning rings, sparks along the
 * edge, arcs, a light shaft, a glow on the ground ...) and a swing trail of its own pattern; this class runs them for whoever holds the weapon.
 * <p>
 * Strictly budgeted, all client side: only holders within {@link #RANGE} blocks, at most {@link #MAX_HOLDERS} per tick, density by distance (full to 12 blocks, then thinner; beyond 24 only the
 * ground layer) times the option {@code visual.ambientDensity}, and every spawn is taken from the client particle budget ({@link PerfBudgets#clientParticles}, the same allowance the other ambient
 * emitters share). The particle options are built once per layer; the positions are computed in a few doubles kept between calls (no per-tick allocation but the particles themselves).
 */
public final class UpgradeFx {
    private UpgradeFx() {}

    static final double RANGE = 40.0;
    static final int MAX_HOLDERS = 6;

    private static final Map<UpgradeSpec.Layer, ParticleOptions> OPTIONS = new IdentityHashMap<>();
    private static final Map<UpgradeSpec.Swing, ParticleOptions> SWING_OPTIONS = new IdentityHashMap<>();
    private static final Map<String, ParticleOptions> PLAIN = new HashMap<>();

    // scratch: hand, direction, right, forward of the holder being processed
    private static double hx, hy, hz, dx, dy, dz, rx, rz, fx, fz;

    public static void onClientTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null || mc.isPaused() || !UpgradeConfig.enabled()) return;
        float density = UpgradeConfig.density();
        boolean trails = UpgradeConfig.trails();
        long tick = level.getGameTime();
        int holders = 0;
        List<AbstractClientPlayer> players = level.players();
        for (int i = 0; i < players.size() && holders < MAX_HOLDERS; i++) {
            AbstractClientPlayer p = players.get(i);
            if (p.isSpectator() || p.isInvisible()) continue;
            double dist2 = p.distanceToSqr(mc.player);
            if (dist2 > RANGE * RANGE) continue;
            ItemStack held = p.getMainHandItem();
            String id = BuiltInRegistries.ITEM.getKey(held.getItem()).getPath();
            if (!UpgradeItems.has(id)) {
                held = p.getOffhandItem();
                id = BuiltInRegistries.ITEM.getKey(held.getItem()).getPath();
                if (!UpgradeItems.has(id)) continue;
            }
            UpgradeSpec spec = UpgradeSpecs.get(id);
            if (spec == null) continue;
            holders++;
            float far = dist2 < 144.0 ? 1f : dist2 < 576.0 ? 0.45f : 0.2f;
            frame(p, spec);
            if (density > 0f) {
                for (UpgradeSpec.Layer l : spec.ambient) {
                    if (dist2 >= 576.0 && !"ground".equals(l.type())) continue;
                    layer(level, p, spec, l, tick, density * far);
                }
            }
            if (trails && spec.swing != null && p.swinging && dist2 < 900.0) swing(level, p, spec, tick);
        }
    }

    // ---------------------------------------------------------------- the holder's frame
    /** Computes the hand, the weapon's direction, the right and forward unit vectors of the holder into the scratch fields. */
    private static void frame(AbstractClientPlayer p, UpgradeSpec spec) {
        double yaw = Math.toRadians(p.yBodyRot);
        double s = Math.sin(yaw), c = Math.cos(yaw);
        fx = -s;
        fz = c;
        double side = p.getMainArm() == HumanoidArm.RIGHT ? 1.0 : -1.0;
        rx = -c * side;
        rz = -s * side;
        double crouch = p.isCrouching() ? 0.8 : 1.0;
        hx = p.getX() + rx * 0.34 + fx * 0.3;
        hy = p.getY() + 0.9 * crouch;
        hz = p.getZ() + rz * 0.34 + fz * 0.3;
        double t = Math.toRadians(spec.handTilt);
        dx = fx * Math.sin(t);
        dy = Math.cos(t);
        dz = fz * Math.sin(t);
    }

    // ---------------------------------------------------------------- ambient layers
    private static void layer(ClientLevel level, AbstractClientPlayer p, UpgradeSpec spec, UpgradeSpec.Layer l, long tick, float k) {
        RandomSource rnd = level.random;
        float want = l.rate() / 20f * k;
        int n = (int) want + (rnd.nextFloat() < want - (int) want ? 1 : 0);
        if (n <= 0) return;
        n = PerfBudgets.clientParticles(tick, n);
        if (n <= 0) return;
        ParticleOptions opt = options(l);
        double len = spec.handLength;
        double ax, ay, az;                                                       // the anchor
        switch (l.anchor()) {
            case "feet" -> { ax = p.getX(); ay = p.getY() + 0.06; az = p.getZ(); }
            case "front" -> { ax = p.getX() + fx * 0.7 + rx * 0.1; ay = p.getY() + 1.1; az = p.getZ() + fz * 0.7 + rz * 0.1; }
            case "mid" -> { ax = hx + dx * len * 0.55; ay = hy + dy * len * 0.55; az = hz + dz * len * 0.55; }
            case "blade" -> { ax = hx + dx * len * 0.6; ay = hy + dy * len * 0.6; az = hz + dz * len * 0.6; }
            default -> { ax = hx + dx * len; ay = hy + dy * len; az = hz + dz * len; }
        }
        double r = l.radius();
        switch (l.type()) {
            case "motes", "drip" -> {
                boolean drip = "drip".equals(l.type());
                for (int i = 0; i < n; i++) {
                    double ox = (rnd.nextDouble() - 0.5) * 2 * r, oy = (rnd.nextDouble() - 0.5) * 2 * r, oz = (rnd.nextDouble() - 0.5) * 2 * r;
                    level.addParticle(opt, ax + ox, ay + oy, az + oz, (rnd.nextDouble() - 0.5) * 0.01, drip ? 0.0 : l.rise() + rnd.nextDouble() * 0.01, (rnd.nextDouble() - 0.5) * 0.01);
                }
            }
            case "ring", "orbit" -> {
                boolean orbit = "orbit".equals(l.type());
                double a0 = tick * l.speed() + p.getId() * 0.7;
                double tilt = Math.toRadians(l.tilt());
                double ct = Math.cos(tilt), st = Math.sin(tilt);
                double sgn = l.speed() >= 0 ? 1.0 : -1.0;
                for (int i = 0; i < n; i++) {
                    double a = a0 + i * (6.2831853 / Math.max(2, n)) + (orbit ? 1.9 : 0.0);
                    double rad = orbit ? r * (1.0 + 0.3 * Math.sin(2 * a)) : r;
                    double ca = Math.cos(a), sa = Math.sin(a);
                    // plane spanned by the right vector and (forward tilted up by `tilt`)
                    double ux = rx, uz = rz;
                    double vx = fx * ct, vy = st, vz = fz * ct;
                    double px = ax + rad * (ca * ux + sa * vx), py = ay + rad * (sa * vy), pz = az + rad * (ca * uz + sa * vz);
                    double tx = (-sa * ux + ca * vx) * 0.04 * sgn, ty = (ca * vy) * 0.04 * sgn, tz = (-sa * uz + ca * vz) * 0.04 * sgn;
                    level.addParticle(opt, px, py, pz, tx, ty, tz);
                }
            }
            case "edge" -> {
                for (int i = 0; i < n; i++) {
                    double t = l.from() + rnd.nextDouble() * (l.to() - l.from());
                    double px, py, pz;
                    if ("front".equals(l.anchor())) {
                        px = ax + rx * (rnd.nextDouble() - 0.5) * 0.9;
                        py = ay + (rnd.nextDouble() - 0.5) * 1.2;
                        pz = az + rz * (rnd.nextDouble() - 0.5) * 0.9;
                    } else {
                        px = hx + dx * len * t;
                        py = hy + dy * len * t;
                        pz = hz + dz * len * t;
                    }
                    level.addParticle(opt, px + (rnd.nextDouble() - 0.5) * r, py + (rnd.nextDouble() - 0.5) * r, pz + (rnd.nextDouble() - 0.5) * r,
                            (rnd.nextDouble() - 0.5) * 0.02, 0.005 + rnd.nextDouble() * 0.01, (rnd.nextDouble() - 0.5) * 0.02);
                }
            }
            case "arc" -> {
                for (int i = 0; i < Math.max(1, n); i++) {
                    double x1 = ax + (rnd.nextDouble() - 0.5) * 2 * r, y1 = ay + (rnd.nextDouble() - 0.5) * 2 * r, z1 = az + (rnd.nextDouble() - 0.5) * 2 * r;
                    double x2 = ax + (rnd.nextDouble() - 0.5) * 2 * r, y2 = ay + (rnd.nextDouble() - 0.5) * 2 * r, z2 = az + (rnd.nextDouble() - 0.5) * 2 * r;
                    for (int s = 0; s <= 3; s++) {
                        double t = s / 3.0;
                        level.addParticle(opt, Mth.lerp(t, x1, x2) + (rnd.nextDouble() - 0.5) * 0.06, Mth.lerp(t, y1, y2) + (rnd.nextDouble() - 0.5) * 0.06,
                                Mth.lerp(t, z1, z2) + (rnd.nextDouble() - 0.5) * 0.06, 0.0, 0.0, 0.0);
                    }
                }
            }
            case "shaft" -> {
                for (int i = 0; i < n; i++) level.addParticle(opt, ax + (rnd.nextDouble() - 0.5) * 0.08, ay + rnd.nextDouble() * 0.2, az + (rnd.nextDouble() - 0.5) * 0.08, 0.0, 0.03 + rnd.nextDouble() * 0.02, 0.0);
            }
            case "ground" -> {
                double a0 = tick * l.speed() + p.getId() * 1.3;
                for (int i = 0; i < n; i++) {
                    double a = a0 + i * (6.2831853 / Math.max(2, n)) + rnd.nextDouble() * 0.4;
                    level.addParticle(opt, ax + r * Math.cos(a), ay, az + r * Math.sin(a), 0.0, 0.01 + rnd.nextDouble() * 0.01, 0.0);
                }
            }
            default -> { }
        }
    }

    // ---------------------------------------------------------------- the swing trail
    private static void swing(ClientLevel level, AbstractClientPlayer p, UpgradeSpec spec, long tick) {
        UpgradeSpec.Swing sw = spec.swing;
        int duration = 6;                                                        // the vanilla swing; the trail follows swingTime, so a slower swing simply lays more ticks of it
        float u = Mth.clamp((p.swingTime + 1f) / duration, 0f, 1f);
        int n = PerfBudgets.clientParticles(tick, sw.count());
        if (n <= 0) return;
        RandomSource rnd = level.random;
        ParticleOptions opt = swingOptions(sw);
        double phi = Math.toRadians(Mth.lerp(u, 70f, -70f)), eps = Math.toRadians(Mth.lerp(u, 55f, -25f));
        double reach = sw.reach();
        String pat = sw.pattern();
        // the weapon's direction at this moment of the swing
        double ce = Math.cos(eps), se = Math.sin(eps);
        double sx = fx * ce * Math.cos(phi) + rx * ce * Math.sin(phi), sy = se, sz = fz * ce * Math.cos(phi) + rz * ce * Math.sin(phi);
        double ox = hx - dx * 0.0, oy = hy + 0.2, oz = hz - dz * 0.0;
        for (int i = 0; i < n; i++) {
            double r = reach * (0.5 + 0.5 * rnd.nextDouble());
            double px = ox + sx * r, py = oy + sy * r, pz = oz + sz * r;
            double vx = 0, vy = 0, vz = 0;
            switch (pat) {
                case "thrust" -> {
                    double t = reach * (0.3 + 0.9 * u) * (0.4 + 0.6 * rnd.nextDouble());
                    px = ox + fx * t + (rnd.nextDouble() - 0.5) * 0.12;
                    py = oy + 0.1 + (rnd.nextDouble() - 0.5) * 0.12;
                    pz = oz + fz * t + (rnd.nextDouble() - 0.5) * 0.12;
                }
                case "bolt" -> {
                    px += (rnd.nextDouble() - 0.5) * 0.3;
                    py += (rnd.nextDouble() - 0.5) * 0.3;
                    pz += (rnd.nextDouble() - 0.5) * 0.3;
                }
                case "sigil", "clover", "circuit", "ray" -> {
                    double tx = ox + sx * reach, ty = oy + sy * reach, tz = oz + sz * reach;      // at the tip
                    double a = rnd.nextDouble() * 6.2831853;
                    double rad = 0.15 + 0.4 * u;
                    if ("clover".equals(pat)) rad = 0.5 * Math.abs(Math.cos(2 * a)) + 0.08;
                    if ("circuit".equals(pat)) {
                        double g = 0.14;
                        px = tx + rx * Math.round((Math.cos(a) * 0.5) / g) * g;
                        py = ty + Math.round((Math.sin(a) * 0.5) / g) * g;
                        pz = tz + rz * Math.round((Math.cos(a) * 0.5) / g) * g;
                        px += fx * 0.0;
                    } else if ("ray".equals(pat)) {
                        px = tx;
                        py = ty;
                        pz = tz;
                        vx = Math.cos(a) * 0.06 * rx + Math.sin(a) * 0.06 * fx;
                        vy = Math.sin(a) * 0.05;
                        vz = Math.cos(a) * 0.06 * rz + Math.sin(a) * 0.06 * fz;
                    } else {
                        px = tx + rad * (Math.cos(a) * rx);
                        py = ty + rad * Math.sin(a);
                        pz = tz + rad * (Math.cos(a) * rz);
                    }
                }
                case "claw" -> {
                    double off = (i % 3 - 1) * 0.18;
                    px += rx * off;
                    pz += rz * off;
                    py += 0.0;
                }
                case "wave" -> py += 0.14 * Math.sin(u * 14.0 + i);
                case "flame" -> vy = 0.03;
                case "wind" -> {
                    vx = -sx * 0.02;
                    vz = -sz * 0.02;
                }
                default -> { }
            }
            level.addParticle(opt, px, py, pz, vx, vy, vz);
        }
    }

    // ---------------------------------------------------------------- particle options
    private static ParticleOptions options(UpgradeSpec.Layer l) {
        ParticleOptions o = OPTIONS.get(l);
        if (o == null) {
            o = build(l.particle(), l.rgb(), l.size());
            OPTIONS.put(l, o);
        }
        return o;
    }

    private static ParticleOptions swingOptions(UpgradeSpec.Swing s) {
        ParticleOptions o = SWING_OPTIONS.get(s);
        if (o == null) {
            o = build(s.particle(), s.rgb(), s.size());
            SWING_OPTIONS.put(s, o);
        }
        return o;
    }

    /** A coloured dust, or the named vanilla particle (end_rod when the name is unknown). */
    static ParticleOptions build(String name, int rgb, float size) {
        if ("dust".equals(name)) return new DustParticleOptions(new Vector3f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f), Mth.clamp(size, 0.2f, 2f));
        ParticleOptions known = PLAIN.get(name);
        if (known != null) return known;
        Object t = BuiltInRegistries.PARTICLE_TYPE.get(ResourceLocation.withDefaultNamespace(name));
        known = t instanceof ParticleOptions po ? po : ParticleTypes.END_ROD;
        PLAIN.put(name, known);
        return known;
    }

    public static void clear() {
        OPTIONS.clear();
        SWING_OPTIONS.clear();
        PLAIN.clear();
    }
}
