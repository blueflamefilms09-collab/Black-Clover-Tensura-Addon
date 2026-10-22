package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.fairytail.storm.StormComp;
import com.newuniverse.nusmp.fairytail.storm.StormMath;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

import static com.newuniverse.nusmp.vfx.client.layer.ElementFx.*;

/**
 * Storm Magic of the Stormcaller Staff (Fairy Tail), drawn as WEATHER, not as wind plus a spark. A storm is a cell: a dark rolling cloud deck hangs over it, curtains of rain fall
 * from the deck, hail stones drop in volleys and burst on the ground, forked bolts leave the cloud and strike, rings of gust roll out from the middle, a funnel walks across the ground,
 * a calm eye ring sits where the caster stands. Every position in time is a function of (seed, age) ({@link StormMath}), the same functions the server uses to hurt things, so what is
 * drawn lands where the damage lands without the server sending a single drop. The server sends five tiny shapes:
 * <ul>
 *   <li>STORM_CLOUD: the cloud deck (two layers of billowing puffs, the upper one towering over the middle), the funnel (hoops of dark swirl with debris) and the eye ring.
 *       from = the ground centre at the start, to = at the end of the slow drift, power = radius; the weather parts are in the seed ({@link StormComp}).</li>
 *   <li>STORM_FALL: what falls from the cloud: rain curtains (crossed, scrolling down), hail stones with trails and impact flashes, gust rings.</li>
 *   <li>STORM_BOLT: one forked bolt, from = the cloud, to = the mark: a jittered main path that re-rolls a few times while it flickers, two branches, a flash in the cloud and one on the ground.</li>
 *   <li>STORM_FRONT: a gale front (a wall of wind with streaks and leaves) or a storm surge (a wall of water with a foaming crest) running from = start to = end, power = half width, with a low roll of cloud over its head and rain behind it.</li>
 *   <li>STORM_CANCEL: the storm of that seed ended early (its owner left, died or cast a newer one); the other shapes of that seed stop drawing.</li>
 * </ul>
 * Alpha blend for the cloud, rain, hail, water and funnel; the additive blend only for the bolt, the streaks and the glows (the alpha-weighted additive of the vertex buffer, so a
 * transparent corner can never light up as a square). Nothing here is a vanilla particle; the optional ground particles of the client (custom particles only) are driven by
 * {@link #particleHook}, installed by the client side, so this class stays free of game classes and renders in the headless preview.
 */
public class StormLayer extends AbstractVfxLayer {
    private static final ResourceLocation CLOUD = VfxTextures.byName("storm_cloud"), RAIN = VfxTextures.byName("storm_rain"), BOLT = VfxTextures.byName("storm_bolt"),
            HAIL = VfxTextures.byName("storm_hail"), WAVE = VfxTextures.byName("storm_wave"), FUNNEL = VfxTextures.byName("storm_funnel");

    /** Set by the client side (client.storm.StormClientFx): spawns the ground particles (custom particles only) of a running storm. Null in the preview. */
    public static volatile Consumer<VfxInstance> particleHook;

    private static final Set<Long> CANCELLED = new LinkedHashSet<>();

    /** Marks the storm of this seed as ended (client thread). */
    public static void cancel(long seed) {
        CANCELLED.add(seed);
        while (CANCELLED.size() > 64) CANCELLED.remove(CANCELLED.iterator().next());
    }

    public static boolean cancelled(long seed) { return CANCELLED.contains(seed); }

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.STORM_CLOUD, VfxShape.STORM_FALL, VfxShape.STORM_BOLT, VfxShape.STORM_FRONT, VfxShape.STORM_CANCEL); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case STORM_BOLT -> 10;
            case STORM_FRONT -> 40;
            case STORM_CANCEL -> 2;
            default -> 160;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFFB8D0F0; }

    @Override
    public void onSpawn(VfxInstance inst) {
        if (inst.shape == VfxShape.STORM_CANCEL) cancel(inst.seed);
        if (inst.shape == VfxShape.STORM_BOLT) VfxShake.add(inst.payload.to(), 0.3f, 6);
    }

    @Override
    public void onTick(VfxInstance inst) {
        Consumer<VfxInstance> hook = particleHook;
        if (hook != null && (inst.shape == VfxShape.STORM_FALL || inst.shape == VfxShape.STORM_FRONT) && !cancelled(inst.seed)) hook.accept(inst);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        if (inst.shape == VfxShape.STORM_CANCEL) return;
        if (inst.shape != VfxShape.STORM_BOLT && cancelled(inst.seed)) return;
        switch (inst.shape) {
            case STORM_CLOUD -> cloud(inst, ctx, buf);
            case STORM_FALL -> fall(inst, ctx, buf);
            case STORM_BOLT -> bolt(inst, ctx, buf);
            case STORM_FRONT -> front(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------------------------------------------------------ helpers
    private static double[] arr(Vec3 v) { return new double[]{v.x, v.y, v.z}; }

    /** The ground centre of the cell at this age (the same function the server runs). */
    private static Vec3 centreAt(VfxInstance inst, double age) {
        double[] c = StormMath.center(arr(inst.payload.from()), arr(inst.payload.to()), age, inst.duration);
        return new Vec3(c[0], c[1], c[2]);
    }

    private static boolean has(long seed, int part) { return StormComp.has(StormMath.maskOf(seed), part); }

    /** The cloud colours: a dark belly and a lighter, bluer top, with a small shade per puff. */
    private static int belly(float h, float a) { return argb(a, 0.17f + 0.07f * h, 0.19f + 0.07f * h, 0.24f + 0.08f * h); }

    private static int top(float h, float a) { return argb(a, 0.30f + 0.12f * h, 0.34f + 0.12f * h, 0.42f + 0.13f * h); }

    private static int argb(float a, float r, float g, float b) {
        return (Mth.clamp((int) (a * 255), 0, 255) << 24) | (Mth.clamp((int) (r * 255), 0, 255) << 16) | (Mth.clamp((int) (g * 255), 0, 255) << 8) | Mth.clamp((int) (b * 255), 0, 255);
    }

    private static Vector3f rel(VfxRenderContext ctx, double x, double y, double z) { return ctx.rel(new Vec3(x, y, z)); }

    // ------------------------------------------------------------------------------------------------------------------ the cloud deck, the funnel, the eye
    private void cloud(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float env = (float) StormMath.envelope(age, inst.duration);
        if (env <= 0.01f) return;
        float radius = (float) StormMath.radiusAt(inst.power, age, inst.duration);
        Vec3 c = centreAt(inst, age);
        long seed = inst.seed;
        float scale = Mth.clamp(radius / 7f, 0.8f, 1.7f);
        double baseY = c.y + StormMath.CLOUD_HEIGHT;
        boolean funnel = has(seed, StormComp.FUNNEL);

        // the deck: a dark lower layer rolling one way and a lighter upper layer rolling the other, towering over the middle
        int n = ctx.seg(funnel ? 11 : 20, 8);
        for (int layer = 0; layer < 2; layer++) {
            for (int i = 0; i < n; i++) {
                int salt = layer * 10;
                float ang = hash(seed, i, 1 + salt) * Mth.TWO_PI + age * 0.012f * (layer == 0 ? 1f : -0.7f);
                float dist = Mth.sqrt(hash(seed, i, 2 + salt)) * radius * 1.08f;
                float h = hash(seed, i, 3 + salt);
                float rise = layer == 0 ? -0.5f : 1.0f + 1.9f * (1f - dist / (radius * 1.08f + 0.01f));
                float bob = Mth.sin(age * 0.05f + i * 1.7f) * 0.25f;
                float size = (3.4f + 3.6f * h) * scale * (0.55f + 0.45f * env);
                Vector3f p = rel(ctx, c.x + Mth.cos(ang) * dist, baseY + rise + bob, c.z + Mth.sin(ang) * dist);
                float alpha = (layer == 0 ? 0.92f : 0.85f) * env;
                buf.billboard(ctx, CLOUD, VfxBlend.ALPHA, p, size, hash(seed, i, 4 + salt) * Mth.TWO_PI + age * 0.004f, layer == 0 ? belly(h, alpha) : top(h, alpha));
            }
        }

        if (funnel) {
            float funR = inst.power * 0.9f;
            float totalH = (float) StormMath.CLOUD_HEIGHT;
            int rings = 4, seg = ctx.seg(10, 6);
            for (int k = 0; k < rings; k++) {
                float f = (k + 0.5f) / rings;
                float rad = Mth.lerp((float) Math.pow(f, 1.3), 0.4f, funR) * env;
                float sway = Mth.sin(age * 0.09f + k * 1.1f) * 0.25f * f;
                VfxPose pose = VfxPose.ground(rel(ctx, c.x + sway, c.y + f * totalH, c.z + sway * 0.5));
                float hw = totalH / rings * 0.62f;
                float rt = Mth.lerp((float) Math.pow(f + 0.25f, 1.3), 0.4f, funR) * env;
                hoop(buf, FUNNEL, VfxBlend.ALPHA, pose, rad, rt, hw, seg, 3, age * (0.10f - 0.012f * k), argb(0.88f * env, 0.36f, 0.38f, 0.43f));
            }
            // dust kicked up round its foot
            hoop(buf, EARTH_DUST, VfxBlend.ALPHA, VfxPose.ground(rel(ctx, c.x, c.y + 0.3, c.z)), funR * 0.5f, funR * 1.05f, 0.35f, ctx.seg(10, 6), 2, -age * 0.05f, argb(0.7f * env, 0.55f, 0.5f, 0.42f));
            int debris = ctx.seg(6, 3);
            for (int i = 0; i < debris; i++) {
                float ph = hash(seed, i, 51) * 30f, lt = ((age + ph) % 30f) / 30f;
                float ang = hash(seed, i, 52) * Mth.TWO_PI - age * 0.28f;
                float rad = (0.5f + 0.9f * lt) * funR * 0.8f * env;
                buf.billboard(ctx, EARTH_CHUNK, VfxBlend.ALPHA, rel(ctx, c.x + Mth.cos(ang) * rad, c.y + 0.4 + lt * totalH * 0.8, c.z + Mth.sin(ang) * rad), 0.28f, age * 0.4f + i, argb(Mth.clamp((1 - lt) * 3, 0, 1) * env, 0.32f, 0.28f, 0.24f));
            }
        }
        if (has(seed, StormComp.EYE)) {
            float pulse = 0.65f + 0.35f * Mth.sin(age * 0.12f);
            VfxPose pose = VfxPose.ground(rel(ctx, c.x, c.y + 0.15, c.z)).spin(age * 0.02f);
            buf.ring(WIND_BAND, VfxBlend.ADD, pose, (float) StormMath.EYE_RADIUS - 0.35f, (float) StormMath.EYE_RADIUS, ctx.seg(16, 8), 2, age * 0.02f, argb(0.45f * env * pulse, 0.62f, 0.8f, 1f));
        }
    }

    // ------------------------------------------------------------------------------------------------------------------ what falls: rain, hail, gust rings
    private void fall(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float env = (float) StormMath.envelope(age, inst.duration);
        if (env <= 0.01f) return;
        long seed = inst.seed;
        float radius = (float) StormMath.radiusAt(inst.power, age, inst.duration);
        Vec3 c = centreAt(inst, age);
        float scale = Mth.clamp(radius / 7f, 0.8f, 1.7f);
        double top = c.y + StormMath.CLOUD_HEIGHT - 0.5, ground = c.y + 0.1;
        float height = (float) (top - ground);

        if (has(seed, StormComp.RAIN)) {
            boolean heavy = StormMath.spellOf(seed) == 1;
            int m = ctx.seg(heavy ? 17 : 12, 6);
            float wx = 0.55f, wz = 0.22f;                                    // the wind that slants the rain
            for (int i = 0; i < m; i++) {
                float ang = hash(seed, i, 61) * Mth.TWO_PI, dist = Mth.sqrt(hash(seed, i, 62)) * radius * 0.97f;
                double x = c.x + Mth.cos(ang) * dist, z = c.z + Mth.sin(ang) * dist;
                float w = 0.95f * scale;
                float vs = -(age * 0.085f) + hash(seed, i, 63) * 4f;
                int col = argb((0.36f + 0.3f * hash(seed, i, 64)) * env, 0.76f, 0.86f, 0.98f);
                // two crossed vertical sheets, the foot swept downwind
                curtain(buf, rel(ctx, x - w, top, z), rel(ctx, x + w, top, z), rel(ctx, x + w + wx, ground, z + wz), rel(ctx, x - w + wx, ground, z + wz), vs, height, col);
                curtain(buf, rel(ctx, x, top, z - w), rel(ctx, x, top, z + w), rel(ctx, x + wx, ground, z + w + wz), rel(ctx, x + wx, ground, z - w + wz), vs + 0.37f, height, col);
            }
        }

        if (has(seed, StormComp.HAIL)) {
            int per = StormMath.hailPerVolley(inst.power);
            int vLast = (int) Math.floor(age / StormMath.HAIL_PERIOD), vFirst = Math.max(0, (int) Math.ceil((age - StormMath.HAIL_FALL - 3) / (double) StormMath.HAIL_PERIOD));
            for (int v = vFirst; v <= vLast; v++) {
                float start = v * StormMath.HAIL_PERIOD, t = (age - start) / StormMath.HAIL_FALL;
                if (t < 0) continue;
                double landAge = start + StormMath.HAIL_FALL;
                Vec3 lc = centreAt(inst, landAge);
                double vr = StormMath.radiusAt(inst.power, start, inst.duration);
                for (int i = 0; i < per; i++) {
                    double[] off = StormMath.hailStone(seed, v, i, vr);
                    double x = lc.x + off[0], z = lc.z + off[1];
                    if (t < 1f) {
                        double y = ground + 0.4 + (top - ground) * (1 - t);
                        Vector3f p = rel(ctx, x, y, z);
                        buf.billboard(ctx, HAIL, VfxBlend.ALPHA, p, 0.62f, age * 0.5f + i, argb(env, 0.9f, 0.95f, 1f));
                        streak(buf, ctx, WIND_STREAK, VfxBlend.ADD, rel(ctx, x, y + 2.2, z), p, 0.16f, argb(0.7f * env, 0.8f, 0.9f, 1f));
                    } else if (t < 1.5f) {                                   // the burst where it landed
                        float k = (t - 1f) / 0.5f;
                        VfxBloom.glow(ctx, buf, rel(ctx, x, ground + 0.35, z), 0.8f * (1 - 0.4f * k), 0xFFDDF0FF, (1f - k) * env);
                    }
                }
            }
        }

        if (has(seed, StormComp.GUST)) {
            int cur = (int) (age / StormMath.GUST_PERIOD);
            for (int k = Math.max(0, cur - 1); k <= cur; k++) {
                double ring = StormMath.gustRingRadius(age, k, inst.power);
                if (ring < 0) continue;
                float fade = (float) (1.0 - Math.min(1.0, Math.max(0.0, (ring / inst.power - 0.1) / 0.9))) * 0.8f + 0.2f;
                VfxPose pose = VfxPose.ground(rel(ctx, c.x, c.y + 1.1, c.z));
                hoop(buf, WIND_BAND, VfxBlend.ALPHA, pose, (float) ring, (float) ring, 0.9f, ctx.seg(16, 8), 4, age * 0.08f, argb(0.5f * fade * env, 0.82f, 0.9f, 1f));
                int s = ctx.seg(7, 3);
                for (int i = 0; i < s; i++) {
                    float a0 = hash(seed, i + k * 7, 71) * Mth.TWO_PI + age * 0.05f;
                    Vector3f q0 = rel(ctx, c.x + Mth.cos(a0) * ring, c.y + 0.6 + hash(seed, i, 72) * 1.5, c.z + Mth.sin(a0) * ring);
                    Vector3f q1 = rel(ctx, c.x + Mth.cos(a0 + 0.22f) * ring, c.y + 0.6 + hash(seed, i, 72) * 1.5, c.z + Mth.sin(a0 + 0.22f) * ring);
                    streak(buf, ctx, WIND_STREAK, VfxBlend.ADD, q0, q1, 0.2f, argb(0.7f * fade * env, 0.85f, 0.93f, 1f));
                }
            }
        }
    }

    /** One rain sheet: a quad from the cloud to the ground whose texture scrolls down. */
    private static void curtain(VfxVertexBuffer buf, Vector3f topA, Vector3f topB, Vector3f footB, Vector3f footA, float vs, float height, int col) {
        buf.quad(RAIN, VfxBlend.ALPHA, footA, footB, topB, topA, 0, vs, 1, vs + height * 0.32f, col, col);
    }

    // ------------------------------------------------------------------------------------------------------------------ one bolt
    private void bolt(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float p = Mth.clamp(age / inst.duration, 0f, 1f);
        float fade = (1f - p) * (1f - p) * 0.5f + (1f - p) * 0.5f;
        int frame = (int) (age / 2f);
        float flick = frame % 2 == 0 ? 1f : 0.72f;
        double[] a = arr(inst.payload.from()), b = arr(inst.payload.to());
        double len = Math.sqrt((a[0] - b[0]) * (a[0] - b[0]) + (a[1] - b[1]) * (a[1] - b[1]) + (a[2] - b[2]) * (a[2] - b[2]));
        int n = ctx.seg(9, 5);
        double[][] path = StormMath.boltPath(inst.seed, frame, a, b, n, Math.min(1.2, 0.35 + len * 0.045));
        Vector3f[] pts = new Vector3f[n + 1];
        for (int i = 0; i <= n; i++) pts[i] = rel(ctx, path[i][0], path[i][1], path[i][2]);
        int halo = argb(0.85f * fade * flick, 0.55f, 0.72f, 1f), core = argb(fade * flick, 1f, 1f, 1f);
        for (int i = 0; i < n; i++) {
            streak(buf, ctx, BOLT, VfxBlend.ADD, pts[i], pts[i + 1], 0.95f, halo);
            streak(buf, ctx, BOLT, VfxBlend.ADD, pts[i], pts[i + 1], 0.26f, core);
        }
        // two branches that leave the main path and die out
        int[] at = {Math.max(1, n / 3), Math.max(2, n / 2 + 1)};
        for (int bi = 0; bi < at.length; bi++) {
            double[] s = path[at[bi]];
            double side = (hash(inst.seed, bi, 81) - 0.5) * 6.0, side2 = (hash(inst.seed, bi, 82) - 0.5) * 6.0;
            double[] e = {s[0] + side, s[1] - Math.min(len * 0.22, 4.5), s[2] + side2};
            double[][] br = StormMath.boltPath(inst.seed + 17L * (bi + 1), frame, s, e, 3, 0.45);
            for (int i = 0; i < 3; i++) {
                float k = 1f - i / 3f;
                Vector3f q0 = rel(ctx, br[i][0], br[i][1], br[i][2]), q1 = rel(ctx, br[i + 1][0], br[i + 1][1], br[i + 1][2]);
                streak(buf, ctx, BOLT, VfxBlend.ADD, q0, q1, 0.5f * k + 0.1f, argb(0.7f * fade * flick, 0.55f, 0.72f, 1f));
                streak(buf, ctx, BOLT, VfxBlend.ADD, q0, q1, 0.12f * k + 0.04f, argb(0.9f * fade * flick, 1f, 1f, 1f));
            }
        }
        VfxBloom.glow(ctx, buf, pts[n], 1.1f, 0xFFBFD8FF, fade * 1.2f);                                           // where it strikes
        VfxBloom.planeGlow(buf, VfxPose.ground(rel(ctx, b[0], b[1] + 0.06, b[2])), 1.5f, 0xFFBFD8FF, fade);
        VfxBloom.glow(ctx, buf, pts[0], 2.6f, 0xFFAFCBFF, fade * (age < 3 ? 1.2f : 0.7f));                       // and where it leaves the cloud, lit from inside
    }

    // ------------------------------------------------------------------------------------------------------------------ a gale front, a storm surge
    private void front(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float fade = life(inst, age, 4, 10);
        if (fade <= 0.01f) return;
        long seed = inst.seed;
        boolean surge = has(seed, StormComp.SURGE);
        Vec3 f = inst.payload.from(), t = inst.payload.to();
        double dx = t.x - f.x, dz = t.z - f.z, len = Math.sqrt(dx * dx + dz * dz);
        if (len < 0.1) return;
        dx /= len;
        dz /= len;
        double px = -dz, pz = dx;
        float halfW = inst.power;
        double head = StormMath.frontPos(age, inst.duration, len);
        double hx = f.x + dx * head, hz = f.z + dz * head, gy = f.y;

        // a low roll of cloud over the head of the front
        int n = ctx.seg(10, 5);
        for (int i = 0; i < n; i++) {
            double back = hash(seed, i, 91) * 3.2, lat = (hash(seed, i, 92) - 0.5) * 2 * halfW * 1.15;
            float h = hash(seed, i, 93);
            Vector3f pp = rel(ctx, hx - dx * back + px * lat, gy + (surge ? 3.4 : 4.4) + h * 1.0, hz - dz * back + pz * lat);
            buf.billboard(ctx, CLOUD, VfxBlend.ALPHA, pp, 3.0f + 2.4f * h, hash(seed, i, 94) * Mth.TWO_PI, surge ? top(h, 0.8f * fade) : belly(h, 0.88f * fade));
        }
        // rain streaming behind the head
        for (int i = 0; i < 4; i++) {
            double back = 2.0 + 2.2 * i + hash(seed, i, 95), lat = (hash(seed, i, 96) - 0.5) * halfW * 1.6;
            double x = hx - dx * back + px * lat, z = hz - dz * back + pz * lat, w = 1.0;
            float vs = -(age * 0.085f) + hash(seed, i, 97) * 4f;
            int col = argb(0.45f * fade * (1f - i * 0.15f), 0.76f, 0.86f, 0.98f);
            float height = surge ? 3.2f : 4.2f;
            curtain(buf, rel(ctx, x - px * w, gy + height, z - pz * w), rel(ctx, x + px * w, gy + height, z + pz * w), rel(ctx, x + px * w + dx * 0.6, gy + 0.1, z + pz * w + dz * 0.6), rel(ctx, x - px * w + dx * 0.6, gy + 0.1, z - pz * w + dz * 0.6), vs, height, col);
        }

        if (!surge) {
            // the wall of wind: a translucent band standing across the front, rushing streaks along it, leaves tumbling in it
            Vector3f l0 = rel(ctx, hx - px * halfW, gy + 0.1, hz - pz * halfW), r0 = rel(ctx, hx + px * halfW, gy + 0.1, hz + pz * halfW);
            Vector3f l1 = rel(ctx, hx - px * halfW + dx * 1.1, gy + 3.4, hz - pz * halfW + dz * 1.1), r1 = rel(ctx, hx + px * halfW + dx * 1.1, gy + 3.4, hz + pz * halfW + dz * 1.1);
            float us = age * 0.06f;
            buf.quad(WIND_BAND, VfxBlend.ALPHA, l0, r0, r1, l1, us, 0, us + 3f, 1, argb(0.62f * fade, 0.85f, 0.93f, 1f), argb(0.28f * fade, 0.85f, 0.93f, 1f));
            buf.quad(WIND_BAND, VfxBlend.ALPHA, l0, r0, r1, l1, -us * 1.7f + 0.4f, 0, -us * 1.7f + 2.4f, 1, argb(0.4f * fade, 0.95f, 0.98f, 1f), argb(0.15f * fade, 0.95f, 0.98f, 1f));
            int s = ctx.seg(14, 6);
            for (int i = 0; i < s; i++) {
                double lat = (hash(seed, i, 101) - 0.5) * 2 * halfW, yy = gy + 0.3 + hash(seed, i, 102) * 2.4, tail = 1.6 + 3.0 * hash(seed, i, 103);
                Vector3f q0 = rel(ctx, hx - dx * tail + px * lat, yy, hz - dz * tail + pz * lat), q1 = rel(ctx, hx + dx * 0.7 + px * lat, yy, hz + dz * 0.7 + pz * lat);
                streak(buf, ctx, WIND_STREAK, VfxBlend.ADD, q0, q1, 0.2f, argb(0.75f * fade, 0.86f, 0.94f, 1f));
            }
            int l = ctx.seg(6, 3);
            for (int i = 0; i < l; i++) {
                double lat = (hash(seed, i, 104) - 0.5) * 2 * halfW, back = hash(seed, i, 105) * 3.0 + Mth.sin(age * 0.3f + i) * 0.4;
                buf.billboard(ctx, WIND_LEAF, VfxBlend.ALPHA, rel(ctx, hx - dx * back + px * lat, gy + 0.6 + hash(seed, i, 106) * 2.0, hz - dz * back + pz * lat), 0.3f, age * 0.5f + i, argb(0.9f * fade, 0.48f, 0.6f, 0.4f));
            }
        } else {
            // the surge: a wall of water leaning over its foot with a ragged foaming crest, a lower swell behind it, spray thrown ahead
            float us = age * 0.03f;
            Vector3f l0 = rel(ctx, hx - px * halfW, gy + 0.05, hz - pz * halfW), r0 = rel(ctx, hx + px * halfW, gy + 0.05, hz + pz * halfW);
            Vector3f l1 = rel(ctx, hx - px * halfW + dx * 0.9, gy + 2.0, hz - pz * halfW + dz * 0.9), r1 = rel(ctx, hx + px * halfW + dx * 0.9, gy + 2.0, hz + pz * halfW + dz * 0.9);
            buf.quad(WAVE, VfxBlend.ALPHA, l0, r0, r1, l1, us, 0, us + 2.2f, 1, argb(0.92f * fade, 0.45f, 0.66f, 0.86f), argb(0.92f * fade, 0.62f, 0.8f, 0.95f));
            double bx = hx - dx * 1.7, bz = hz - dz * 1.7;
            Vector3f m0 = rel(ctx, bx - px * halfW, gy + 0.05, bz - pz * halfW), m1 = rel(ctx, bx + px * halfW, gy + 0.05, bz + pz * halfW);
            Vector3f m2 = rel(ctx, bx + px * halfW, gy + 1.15, bz + pz * halfW), m3 = rel(ctx, bx - px * halfW, gy + 1.15, bz - pz * halfW);
            buf.quad(WAVE, VfxBlend.ALPHA, m0, m1, m2, m3, -us, 0, -us + 2.2f, 1, argb(0.7f * fade, 0.38f, 0.58f, 0.78f), argb(0.7f * fade, 0.5f, 0.7f, 0.9f));
            int sp = ctx.seg(9, 4);
            for (int i = 0; i < sp; i++) {
                double lat = (hash(seed, i, 111) - 0.5) * 2 * halfW;
                float ph = ((age * 0.08f + hash(seed, i, 112)) % 1f);
                double y = gy + 1.6 + Math.sin(ph * Math.PI) * 1.3, ahead = ph * 2.4;
                buf.billboard(ctx, WATER_DROP, VfxBlend.ALPHA, rel(ctx, hx + dx * ahead + px * lat, y, hz + dz * ahead + pz * lat), 0.32f, 0f, argb(0.85f * fade * Mth.sin(ph * Mth.PI), 0.8f, 0.9f, 1f));
            }
        }
    }
}
