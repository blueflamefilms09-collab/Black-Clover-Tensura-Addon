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
import java.util.Set;
import java.util.function.Consumer;

import static com.newuniverse.nusmp.vfx.client.layer.ElementFx.hash;
import static com.newuniverse.nusmp.vfx.client.layer.ElementFx.life;
import static com.newuniverse.nusmp.vfx.client.layer.ElementFx.EARTH_CHUNK;
import static com.newuniverse.nusmp.vfx.client.layer.ElementFx.EARTH_DUST;
import static com.newuniverse.nusmp.vfx.client.layer.ElementFx.WATER_DROP;
import static com.newuniverse.nusmp.vfx.client.layer.ElementFx.WIND_BAND;
import static com.newuniverse.nusmp.vfx.client.layer.ElementFx.WIND_LEAF;
import static com.newuniverse.nusmp.vfx.client.layer.ElementFx.WIND_STREAK;

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
 *
 * <p>NO LAG (CLAUDE.md rule 11): every segment count goes through {@code ctx.seg} (the quality level and the distance thin them), each shape stays under the 400 vertices of one activation
 * (the cloud about 190, the rain about 140, a bolt about 150, the funnel about 310 at full detail), and nothing is allocated per frame: the camera-relative corners live in a few scratch
 * vectors of this layer (the layer is one object drawn by the render thread only), the maths come from the allocation-free forms of {@link StormMath}, and the paths of a bolt are written
 * into buffers kept between frames. A source scan test pins the render methods free of {@code new}.
 */
public class StormLayer extends AbstractVfxLayer {
    private static final ResourceLocation CLOUD = VfxTextures.byName("storm_cloud"), RAIN = VfxTextures.byName("storm_rain"), BOLT = VfxTextures.byName("storm_bolt"),
            HAIL = VfxTextures.byName("storm_hail"), WAVE = VfxTextures.byName("storm_wave"), FUNNEL = VfxTextures.byName("storm_funnel");

    /** Set by the client side (client.storm.StormClientFx): spawns the ground particles (custom particles only) of a running storm. Null in the preview. */
    public static volatile Consumer<VfxInstance> particleHook;

    /** The seeds of storms ended early: a ring of 64 (client thread only; a plain array, so asking costs no boxing). */
    private static final long[] CANCELLED = new long[64];
    private static int cancelledCount, cancelledNext;

    /** Marks the storm of this seed as ended (client thread). */
    public static void cancel(long seed) {
        CANCELLED[cancelledNext] = seed;
        cancelledNext = (cancelledNext + 1) % CANCELLED.length;
        if (cancelledCount < CANCELLED.length) cancelledCount++;
    }

    public static boolean cancelled(long seed) {
        for (int i = 0; i < cancelledCount; i++) if (CANCELLED[i] == seed) return true;
        return false;
    }

    // scratch for the frame being drawn (render thread only): the camera position, six camera-relative points, the centre of the cell and the bolt paths
    private final Vector3f[] v = {new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()};
    private double camX, camY, camZ, cX, cY, cZ;
    private final double[][] path = new double[10][3], branch = new double[4][3];

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
        camX = ctx.camPos.x;
        camY = ctx.camPos.y;
        camZ = ctx.camPos.z;
        switch (inst.shape) {
            case STORM_CLOUD -> cloud(inst, ctx, buf);
            case STORM_FALL -> fall(inst, ctx, buf);
            case STORM_BOLT -> bolt(inst, ctx, buf);
            case STORM_FRONT -> front(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------------------------------------------------------ helpers (no allocation)
    private static boolean has(long seed, int part) { return StormComp.has(StormMath.maskOf(seed), part); }

    /** Scratch point {@code i} set to a world position, camera relative. */
    private Vector3f p(int i, double x, double y, double z) {
        Vector3f s = v[i];
        s.set((float) (x - camX), (float) (y - camY), (float) (z - camZ));
        return s;
    }

    /** The cloud colours: a dark belly and a lighter, bluer top, with a small shade per puff. */
    private static int belly(float h, float a) { return argb(a, 0.17f + 0.07f * h, 0.19f + 0.07f * h, 0.24f + 0.08f * h); }

    private static int top(float h, float a) { return argb(a, 0.30f + 0.12f * h, 0.34f + 0.12f * h, 0.42f + 0.13f * h); }

    private static int argb(float a, float r, float g, float b) {
        return (Mth.clamp((int) (a * 255), 0, 255) << 24) | (Mth.clamp((int) (r * 255), 0, 255) << 16) | (Mth.clamp((int) (g * 255), 0, 255) << 8) | Mth.clamp((int) (b * 255), 0, 255);
    }

    /** The ground centre of the cell at this age into cX, cY, cZ (the same function the server runs). */
    private void centre(VfxInstance inst, double age) {
        Vec3 f = inst.payload.from(), t = inst.payload.to();
        cX = StormMath.lerpAt(f.x, t.x, age, inst.duration);
        cY = StormMath.lerpAt(f.y, t.y, age, inst.duration);
        cZ = StormMath.lerpAt(f.z, t.z, age, inst.duration);
    }

    /** A quad from the scratch points 0..3 (counter-clockwise, 0 and 1 at the foot / start). */
    private void quad(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, float u0, float v0, float u1, float v1, int c0, int c1) {
        buf.quad(tex, blend, v[0], v[1], v[2], v[3], u0, v0, u1, v1, c0, c1);
    }

    /** A camera-facing streak between the scratch points 4 and 5, {@code width} across: the same shape as ElementFx.streak, without its vectors. */
    private void streak(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, float width, int argb) {
        Vector3f a = v[4], b = v[5];
        float dx = b.x - a.x, dy = b.y - a.y, dz = b.z - a.z;
        if (dx * dx + dy * dy + dz * dz < 1e-6f) return;
        float mx = (a.x + b.x) * 0.5f, my = (a.y + b.y) * 0.5f, mz = (a.z + b.z) * 0.5f;
        float sx = dy * -mz - dz * -my, sy = dz * -mx - dx * -mz, sz = dx * -my - dy * -mx;       // dir x (toward the camera)
        float sl = Mth.sqrt(sx * sx + sy * sy + sz * sz);
        if (sl < 1e-4f) return;
        float k = width * 0.5f / sl;
        sx *= k;
        sy *= k;
        sz *= k;
        v[0].set(a.x - sx, a.y - sy, a.z - sz);
        v[1].set(b.x - sx, b.y - sy, b.z - sz);
        v[2].set(b.x + sx, b.y + sy, b.z + sz);
        v[3].set(a.x + sx, a.y + sy, a.z + sz);
        int col = blend.grade(argb, 0.8f);
        quad(buf, tex, blend, 0, 0, 1, 1, col, col);
    }

    /** A ribbon standing on a circle of the ground plane: radius {@code rb} at the bottom edge, {@code rt} at the top edge, {@code hw} above and below {@code y}. */
    private void hoop(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, double x, double y, double z, float rb, float rt, float hw, int seg, float repeats, float uScroll, int argb) {
        int col = blend.grade(argb, 0.6f);
        for (int i = 0; i < seg; i++) {
            float a0 = Mth.TWO_PI * i / seg, a1 = Mth.TWO_PI * (i + 1) / seg;
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            p(0, x + c0 * rb, y - hw, z + s0 * rb);
            p(1, x + c1 * rb, y - hw, z + s1 * rb);
            p(2, x + c1 * rt, y + hw, z + s1 * rt);
            p(3, x + c0 * rt, y + hw, z + s0 * rt);
            quad(buf, tex, blend, uScroll + repeats * i / seg, 0, uScroll + repeats * (i + 1) / seg, 1, col, col);
        }
    }

    /** A flat band on the ground plane between two radii. */
    private void flatRing(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, double x, double y, double z, float rin, float rout, int seg, float repeats, float uScroll, int argb) {
        int col = blend.grade(argb, 0.6f);
        for (int i = 0; i < seg; i++) {
            float a0 = Mth.TWO_PI * i / seg, a1 = Mth.TWO_PI * (i + 1) / seg;
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            p(0, x + c0 * rin, y, z + s0 * rin);
            p(1, x + c1 * rin, y, z + s1 * rin);
            p(2, x + c1 * rout, y, z + s1 * rout);
            p(3, x + c0 * rout, y, z + s0 * rout);
            quad(buf, tex, blend, uScroll + repeats * i / seg, 0, uScroll + repeats * (i + 1) / seg, 1, col, col);
        }
    }

    /** One rain sheet from the cloud to the ground, its texture scrolling down: the scratch points 0..3 are set by the caller (0 and 1 the foot). */
    private void curtain(VfxVertexBuffer buf, float vs, float height, int col) {
        quad(buf, RAIN, VfxBlend.ALPHA, 0, vs, 1, vs + height * 0.32f, col, col);
    }

    // ------------------------------------------------------------------------------------------------------------------ the cloud deck, the funnel, the eye
    private void cloud(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float env = (float) StormMath.envelope(age, inst.duration);
        if (env <= 0.01f) return;
        float radius = (float) StormMath.radiusAt(inst.power, age, inst.duration);
        centre(inst, age);
        long seed = inst.seed;
        float scale = Mth.clamp(radius / 7f, 0.8f, 1.7f);
        double baseY = cY + StormMath.CLOUD_HEIGHT;
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
                float alpha = (layer == 0 ? 0.92f : 0.85f) * env;
                buf.billboard(ctx, CLOUD, VfxBlend.ALPHA, p(4, cX + Mth.cos(ang) * dist, baseY + rise + bob, cZ + Mth.sin(ang) * dist), size,
                        hash(seed, i, 4 + salt) * Mth.TWO_PI + age * 0.004f, layer == 0 ? belly(h, alpha) : top(h, alpha));
            }
        }

        if (funnel) {
            float funR = inst.power * 0.9f;
            float totalH = (float) StormMath.CLOUD_HEIGHT;
            int rings = 4, seg = ctx.seg(10, 6);
            for (int k = 0; k < rings; k++) {
                float f = (k + 0.5f) / rings;
                float rad = Mth.lerp((float) Math.pow(f, 1.3), 0.4f, funR) * env;
                float rt = Mth.lerp((float) Math.pow(f + 0.25f, 1.3), 0.4f, funR) * env;
                float sway = Mth.sin(age * 0.09f + k * 1.1f) * 0.25f * f;
                hoop(buf, FUNNEL, VfxBlend.ALPHA, cX + sway, cY + f * totalH, cZ + sway * 0.5, rad, rt, totalH / rings * 0.62f, seg, 3, age * (0.10f - 0.012f * k), argb(0.88f * env, 0.36f, 0.38f, 0.43f));
            }
            // dust kicked up round its foot
            hoop(buf, EARTH_DUST, VfxBlend.ALPHA, cX, cY + 0.3, cZ, funR * 0.5f, funR * 1.05f, 0.35f, ctx.seg(10, 6), 2, -age * 0.05f, argb(0.7f * env, 0.55f, 0.5f, 0.42f));
            int debris = ctx.seg(6, 3);
            for (int i = 0; i < debris; i++) {
                float ph = hash(seed, i, 51) * 30f, lt = ((age + ph) % 30f) / 30f;
                float ang = hash(seed, i, 52) * Mth.TWO_PI - age * 0.28f;
                float rad = (0.5f + 0.9f * lt) * funR * 0.8f * env;
                buf.billboard(ctx, EARTH_CHUNK, VfxBlend.ALPHA, p(4, cX + Mth.cos(ang) * rad, cY + 0.4 + lt * totalH * 0.8, cZ + Mth.sin(ang) * rad), 0.28f, age * 0.4f + i,
                        argb(Mth.clamp((1 - lt) * 3, 0, 1) * env, 0.32f, 0.28f, 0.24f));
            }
        }
        if (has(seed, StormComp.EYE)) {
            float pulse = 0.65f + 0.35f * Mth.sin(age * 0.12f);
            flatRing(buf, WIND_BAND, VfxBlend.ADD, cX, cY + 0.15, cZ, (float) StormMath.EYE_RADIUS - 0.35f, (float) StormMath.EYE_RADIUS, ctx.seg(16, 8), 2, age * 0.02f, argb(0.45f * env * pulse, 0.62f, 0.8f, 1f));
        }
    }

    // ------------------------------------------------------------------------------------------------------------------ what falls: rain, hail, gust rings
    private void fall(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float env = (float) StormMath.envelope(age, inst.duration);
        if (env <= 0.01f) return;
        long seed = inst.seed;
        float radius = (float) StormMath.radiusAt(inst.power, age, inst.duration);
        centre(inst, age);
        float scale = Mth.clamp(radius / 7f, 0.8f, 1.7f);
        double top = cY + StormMath.CLOUD_HEIGHT - 0.5, ground = cY + 0.1;
        float height = (float) (top - ground);

        if (has(seed, StormComp.RAIN)) {
            boolean heavy = StormMath.spellOf(seed) == 1;
            int m = ctx.seg(heavy ? 17 : 12, 6);
            float wx = 0.55f, wz = 0.22f;                                    // the wind that slants the rain
            for (int i = 0; i < m; i++) {
                float ang = hash(seed, i, 61) * Mth.TWO_PI, dist = Mth.sqrt(hash(seed, i, 62)) * radius * 0.97f;
                double x = cX + Mth.cos(ang) * dist, z = cZ + Mth.sin(ang) * dist;
                float w = 0.95f * scale;
                float vs = -(age * 0.085f) + hash(seed, i, 63) * 4f;
                int col = argb((0.36f + 0.3f * hash(seed, i, 64)) * env, 0.76f, 0.86f, 0.98f);
                // two crossed vertical sheets, the foot swept downwind
                p(3, x - w, top, z);
                p(2, x + w, top, z);
                p(1, x + w + wx, ground, z + wz);
                p(0, x - w + wx, ground, z + wz);
                curtain(buf, vs, height, col);
                p(3, x, top, z - w);
                p(2, x, top, z + w);
                p(1, x + wx, ground, z + w + wz);
                p(0, x + wx, ground, z - w + wz);
                curtain(buf, vs + 0.37f, height, col);
            }
        }

        if (has(seed, StormComp.HAIL)) {
            int per = StormMath.hailPerVolley(inst.power);
            int vLast = (int) Math.floor(age / StormMath.HAIL_PERIOD), vFirst = Math.max(0, (int) Math.ceil((age - StormMath.HAIL_FALL - 3) / (double) StormMath.HAIL_PERIOD));
            Vec3 f = inst.payload.from(), tt = inst.payload.to();
            for (int vol = vFirst; vol <= vLast; vol++) {
                float start = vol * StormMath.HAIL_PERIOD, t = (age - start) / StormMath.HAIL_FALL;
                if (t < 0) continue;
                double landAge = start + StormMath.HAIL_FALL;
                double lx = StormMath.lerpAt(f.x, tt.x, landAge, inst.duration), lz = StormMath.lerpAt(f.z, tt.z, landAge, inst.duration);
                double vr = StormMath.radiusAt(inst.power, start, inst.duration);
                for (int i = 0; i < per; i++) {
                    double x = lx + StormMath.hailStoneX(seed, vol, i, vr), z = lz + StormMath.hailStoneZ(seed, vol, i, vr);
                    if (t < 1f) {
                        double y = ground + 0.4 + (top - ground) * (1 - t);
                        buf.billboard(ctx, HAIL, VfxBlend.ALPHA, p(4, x, y, z), 0.62f, age * 0.5f + i, argb(env, 0.9f, 0.95f, 1f));
                        p(5, x, y, z);
                        p(4, x, y + 2.2, z);
                        streak(buf, WIND_STREAK, VfxBlend.ADD, 0.16f, argb(0.7f * env, 0.8f, 0.9f, 1f));
                    } else if (t < 1.5f) {                                   // the burst where it landed
                        float k = (t - 1f) / 0.5f;
                        VfxBloom.glow(ctx, buf, p(4, x, ground + 0.35, z), 0.8f * (1 - 0.4f * k), 0xFFDDF0FF, (1f - k) * env);
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
                hoop(buf, WIND_BAND, VfxBlend.ALPHA, cX, cY + 1.1, cZ, (float) ring, (float) ring, 0.9f, ctx.seg(16, 8), 4, age * 0.08f, argb(0.5f * fade * env, 0.82f, 0.9f, 1f));
                int s = ctx.seg(7, 3);
                for (int i = 0; i < s; i++) {
                    float a0 = hash(seed, i + k * 7, 71) * Mth.TWO_PI + age * 0.05f;
                    double yy = cY + 0.6 + hash(seed, i, 72) * 1.5;
                    p(4, cX + Mth.cos(a0) * ring, yy, cZ + Mth.sin(a0) * ring);
                    p(5, cX + Mth.cos(a0 + 0.22f) * ring, yy, cZ + Mth.sin(a0 + 0.22f) * ring);
                    streak(buf, WIND_STREAK, VfxBlend.ADD, 0.2f, argb(0.7f * fade * env, 0.85f, 0.93f, 1f));
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------------ one bolt
    private void bolt(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float pr = Mth.clamp(age / inst.duration, 0f, 1f);
        float fade = (1f - pr) * (1f - pr) * 0.5f + (1f - pr) * 0.5f;
        int frame = (int) (age / 2f);
        float flick = frame % 2 == 0 ? 1f : 0.72f;
        Vec3 a = inst.payload.from(), b = inst.payload.to();
        double len = Math.sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y) + (a.z - b.z) * (a.z - b.z));
        int n = Math.min(9, ctx.seg(9, 5));
        StormMath.boltPath(inst.seed, frame, a.x, a.y, a.z, b.x, b.y, b.z, n, Math.min(1.2, 0.35 + len * 0.045), path);
        int halo = argb(0.85f * fade * flick, 0.55f, 0.72f, 1f), core = argb(fade * flick, 1f, 1f, 1f);
        for (int i = 0; i < n; i++) {
            p(4, path[i][0], path[i][1], path[i][2]);
            p(5, path[i + 1][0], path[i + 1][1], path[i + 1][2]);
            streak(buf, BOLT, VfxBlend.ADD, 0.95f, halo);
            p(4, path[i][0], path[i][1], path[i][2]);
            p(5, path[i + 1][0], path[i + 1][1], path[i + 1][2]);
            streak(buf, BOLT, VfxBlend.ADD, 0.26f, core);
        }
        // two branches that leave the main path and die out
        for (int bi = 0; bi < 2; bi++) {
            int at = bi == 0 ? Math.max(1, n / 3) : Math.max(2, n / 2 + 1);
            double sx = path[at][0], sy = path[at][1], sz = path[at][2];
            double ex = sx + (hash(inst.seed, bi, 81) - 0.5) * 6.0, ey = sy - Math.min(len * 0.22, 4.5), ez = sz + (hash(inst.seed, bi, 82) - 0.5) * 6.0;
            StormMath.boltPath(inst.seed + 17L * (bi + 1), frame, sx, sy, sz, ex, ey, ez, 3, 0.45, branch);
            for (int i = 0; i < 3; i++) {
                float k = 1f - i / 3f;
                p(4, branch[i][0], branch[i][1], branch[i][2]);
                p(5, branch[i + 1][0], branch[i + 1][1], branch[i + 1][2]);
                streak(buf, BOLT, VfxBlend.ADD, 0.5f * k + 0.1f, argb(0.7f * fade * flick, 0.55f, 0.72f, 1f));
                p(4, branch[i][0], branch[i][1], branch[i][2]);
                p(5, branch[i + 1][0], branch[i + 1][1], branch[i + 1][2]);
                streak(buf, BOLT, VfxBlend.ADD, 0.12f * k + 0.04f, argb(0.9f * fade * flick, 1f, 1f, 1f));
            }
        }
        VfxBloom.glow(ctx, buf, p(4, b.x, b.y, b.z), 1.1f, 0xFFBFD8FF, fade * 1.2f);                              // where it strikes
        // a flat flash on the ground under the mark
        float fr = 1.5f * 1.5f;
        int fc = argb(0.45f * fade, 0.75f, 0.85f, 1f);
        p(0, b.x - fr, b.y + 0.06, b.z - fr);
        p(1, b.x + fr, b.y + 0.06, b.z - fr);
        p(2, b.x + fr, b.y + 0.06, b.z + fr);
        p(3, b.x - fr, b.y + 0.06, b.z + fr);
        quad(buf, VfxTextures.GLOW, VfxBlend.ADD, 0, 0, 1, 1, fc, fc);
        VfxBloom.glow(ctx, buf, p(4, a.x, a.y, a.z), 2.6f, 0xFFAFCBFF, fade * (age < 3 ? 1.2f : 0.7f));          // and where it leaves the cloud, lit from inside
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
            buf.billboard(ctx, CLOUD, VfxBlend.ALPHA, p(4, hx - dx * back + px * lat, gy + (surge ? 3.4 : 4.4) + h * 1.0, hz - dz * back + pz * lat), 3.0f + 2.4f * h,
                    hash(seed, i, 94) * Mth.TWO_PI, surge ? top(h, 0.8f * fade) : belly(h, 0.88f * fade));
        }
        // rain streaming behind the head
        float rainH = surge ? 3.2f : 4.2f;
        for (int i = 0; i < 4; i++) {
            double back = 2.0 + 2.2 * i + hash(seed, i, 95), lat = (hash(seed, i, 96) - 0.5) * halfW * 1.6;
            double x = hx - dx * back + px * lat, z = hz - dz * back + pz * lat, w = 1.0;
            float vs = -(age * 0.085f) + hash(seed, i, 97) * 4f;
            p(3, x - px * w, gy + rainH, z - pz * w);
            p(2, x + px * w, gy + rainH, z + pz * w);
            p(1, x + px * w + dx * 0.6, gy + 0.1, z + pz * w + dz * 0.6);
            p(0, x - px * w + dx * 0.6, gy + 0.1, z - pz * w + dz * 0.6);
            curtain(buf, vs, rainH, argb(0.45f * fade * (1f - i * 0.15f), 0.76f, 0.86f, 0.98f));
        }

        if (!surge) {
            // the wall of wind: a translucent band standing across the front, rushing streaks along it, leaves tumbling in it
            float us = age * 0.06f;
            for (int pass = 0; pass < 2; pass++) {
                p(0, hx - px * halfW, gy + 0.1, hz - pz * halfW);
                p(1, hx + px * halfW, gy + 0.1, hz + pz * halfW);
                p(2, hx + px * halfW + dx * 1.1, gy + 3.4, hz + pz * halfW + dz * 1.1);
                p(3, hx - px * halfW + dx * 1.1, gy + 3.4, hz - pz * halfW + dz * 1.1);
                if (pass == 0) quad(buf, WIND_BAND, VfxBlend.ALPHA, us, 0, us + 3f, 1, argb(0.62f * fade, 0.85f, 0.93f, 1f), argb(0.28f * fade, 0.85f, 0.93f, 1f));
                else quad(buf, WIND_BAND, VfxBlend.ALPHA, -us * 1.7f + 0.4f, 0, -us * 1.7f + 2.4f, 1, argb(0.4f * fade, 0.95f, 0.98f, 1f), argb(0.15f * fade, 0.95f, 0.98f, 1f));
            }
            int s = ctx.seg(14, 6);
            for (int i = 0; i < s; i++) {
                double lat = (hash(seed, i, 101) - 0.5) * 2 * halfW, yy = gy + 0.3 + hash(seed, i, 102) * 2.4, tail = 1.6 + 3.0 * hash(seed, i, 103);
                p(4, hx - dx * tail + px * lat, yy, hz - dz * tail + pz * lat);
                p(5, hx + dx * 0.7 + px * lat, yy, hz + dz * 0.7 + pz * lat);
                streak(buf, WIND_STREAK, VfxBlend.ADD, 0.2f, argb(0.75f * fade, 0.86f, 0.94f, 1f));
            }
            int l = ctx.seg(6, 3);
            for (int i = 0; i < l; i++) {
                double lat = (hash(seed, i, 104) - 0.5) * 2 * halfW, back = hash(seed, i, 105) * 3.0 + Mth.sin(age * 0.3f + i) * 0.4;
                buf.billboard(ctx, WIND_LEAF, VfxBlend.ALPHA, p(4, hx - dx * back + px * lat, gy + 0.6 + hash(seed, i, 106) * 2.0, hz - dz * back + pz * lat), 0.3f, age * 0.5f + i, argb(0.9f * fade, 0.48f, 0.6f, 0.4f));
            }
        } else {
            // the surge: a wall of water leaning over its foot with a ragged foaming crest, a lower swell behind it, spray thrown ahead
            float us = age * 0.03f;
            p(0, hx - px * halfW, gy + 0.05, hz - pz * halfW);
            p(1, hx + px * halfW, gy + 0.05, hz + pz * halfW);
            p(2, hx + px * halfW + dx * 0.9, gy + 2.0, hz + pz * halfW + dz * 0.9);
            p(3, hx - px * halfW + dx * 0.9, gy + 2.0, hz - pz * halfW + dz * 0.9);
            quad(buf, WAVE, VfxBlend.ALPHA, us, 0, us + 2.2f, 1, argb(0.92f * fade, 0.45f, 0.66f, 0.86f), argb(0.92f * fade, 0.62f, 0.8f, 0.95f));
            double bx = hx - dx * 1.7, bz = hz - dz * 1.7;
            p(0, bx - px * halfW, gy + 0.05, bz - pz * halfW);
            p(1, bx + px * halfW, gy + 0.05, bz + pz * halfW);
            p(2, bx + px * halfW, gy + 1.15, bz + pz * halfW);
            p(3, bx - px * halfW, gy + 1.15, bz - pz * halfW);
            quad(buf, WAVE, VfxBlend.ALPHA, -us, 0, -us + 2.2f, 1, argb(0.7f * fade, 0.38f, 0.58f, 0.78f), argb(0.7f * fade, 0.5f, 0.7f, 0.9f));
            int sp = ctx.seg(9, 4);
            for (int i = 0; i < sp; i++) {
                double lat = (hash(seed, i, 111) - 0.5) * 2 * halfW;
                float ph = ((age * 0.08f + hash(seed, i, 112)) % 1f);
                double y = gy + 1.6 + Math.sin(ph * Math.PI) * 1.3, ahead = ph * 2.4;
                buf.billboard(ctx, WATER_DROP, VfxBlend.ALPHA, p(4, hx + dx * ahead + px * lat, y, hz + dz * ahead + pz * lat), 0.32f, 0f, argb(0.85f * fade * Mth.sin(ph * Mth.PI), 0.8f, 0.9f, 1f));
            }
        }
    }
}
