package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * Sandalphon, Lord of Judgment (Veldanava): the executioner angel. Cold white-gold light, pale steel, thin black cracks, ticking clock / stutter motifs and a heavenly cross.
 * Written from scratch for this mod (textures: {@code tools/gen_judgment_vfx_textures.py} -> {@code textures/particle/judgment_*.png}); it shares nothing with any other mod and does not touch the Time Magic layer.
 * <ul>
 *   <li>JUDGMENT_BULLET: from -> to (up to ~64 blocks), power = width, ~12 ticks. A needle of white-gold light crosses the line in 2-3.6 ticks with a shock ring and flare at the muzzle, a hairline black crack
 *       trail that lingers behind it (one jagged ribbon piece per ~4 blocks, at most 16), and a few short stutter-ghost dashes that jump sideways on whole ticks (the frame-skipping look).</li>
 *   <li>JUDGMENT_SIGIL: at from (the marked creature's feet; follows it), power = size, up to 600 ticks. A gold judgment seal on the ground (tally groups, minute ticks, hexagram), a clock ring that TICKS
 *       forward 6 degrees every half second, a small blade-and-scales glyph hovering above with a thin light thread to the ground and a few motes.</li>
 *   <li>JUDGMENT_EDICT: at from, power = radius (~30), ~24 ticks. A huge flat wave sweeps out in ~20 ticks: a dark erased band, the bright wave, a white leading ring, two bands of text-like marks written in the wave,
 *       hairline cracks, and white motes that rise out of the ground behind the front (things erased upward).</li>
 *   <li>JUDGMENT_CLEAVE: from = the caster (the height of the sweep: pass chest height), to = a point ~5 ahead, power = reach, ~10 ticks. A wide, short guillotine crescent (black core, white-gold body, hot rim) sweeps
 *       left to right in a plane tilted downward, two stutter echoes trail it, a blade falls from above onto the middle of the arc and leaves afterimages, sparks fly off the head.</li>
 *   <li>JUDGMENT_FIELD: at from, power = radius (~8), up to 240 ticks. A subdued ground field: a thin golden circle, a faint seal, two clock rings that tick, hairline cracks, thin beams of light standing on the rim, slow motes.</li>
 *   <li>JUDGMENT_CROSS: Final Judgment, at from (the target), power = size (~2), 60-70 ticks. Charge (about 54 % of the duration): a thread of light descends, light streaks converge on the point, a ground clock ring spins down
 *       in stepping ticks and freezes, a halo clock closes round the point. Detonation: a flash, then a pillar of light from the sky to the ground and a crossbar (which always lies across the viewer's view, so every viewer
 *       sees a cross), rays, tip flares, shock rings on the ground and in the air, cracks in the ground, rising motes; then it thins out.</li>
 * </ul>
 * <b>Budgets (rule 11).</b> Every effect draws through the 400-vertex (100-quad) activation cap of the vertex buffer and is far below it; counts are at full detail, {@code ctx.seg} thins them by distance and the particle setting
 * (minimums in brackets). Worst case quads per effect per frame: BULLET 37 (16 crack pieces + 1 glow ribbon + needle 2 + head flare/glow 3 + ghosts 6 + muzzle 5 + impact 4), SIGIL 17, EDICT 81 (glyph bands 30 + 20,
 * motes 22), CLEAVE 78 (measured; the engine hard-caps every activation at 100 quads / 400 vertices anyway), FIELD 40, CROSS 55. No effect allocates more than a few small vectors per frame (the same as the other layers); nothing is kept between frames, so nothing has to be cleaned up on logout,
 * death or dimension change; the server only sends one packet per effect (VfxSpawn) and nothing here runs on the server.
 * <p>
 * <b>3.21.0 with the 3D models on and the effect within 64 blocks</b> (setting {@code visual.judgmentModels}, see {@code JudgmentModels}; the numbers above are for the setting off or far away): measured worst quads per frame over dense age sweeps
 * BULLET 49, CLEAVE 73, FIELD 90, EDICT 71, CROSS 76 (all under the 100-quad cap; every model asks the budget first and is skipped whole when it does not fit, the old flat part then stays).
 */
public class JudgmentLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    public static final ResourceLocation RING = t("judgment_ring");
    public static final ResourceLocation WAVE = t("judgment_wave");
    public static final ResourceLocation SIGIL = t("judgment_sigil");
    public static final ResourceLocation CLOCK = t("judgment_clock");
    public static final ResourceLocation GLYPHS = t("judgment_glyphs");
    public static final ResourceLocation FLARE = t("judgment_flare");
    public static final ResourceLocation MOTE = t("judgment_mote");
    public static final ResourceLocation NEEDLE = t("judgment_needle");
    public static final ResourceLocation BEAM = t("judgment_beam");
    public static final ResourceLocation SHAFT = t("judgment_shaft");
    public static final ResourceLocation CRACK = t("judgment_crack");
    public static final ResourceLocation CRACKS_GROUND = t("judgment_cracks_ground");
    public static final ResourceLocation SLASH = t("judgment_slash");
    public static final ResourceLocation BLADE = t("judgment_blade");
    /** 3.21.0: the material atlas of the 3D models ({@link JudgmentModels}), 4 x 4 cells. */
    public static final ResourceLocation MODEL_ATLAS = t("judgment_model_atlas");

    private static final int WHITE = 0xFFFFFFFF;
    private static final int STEEL = 0xFFAFC3DC;
    private static final int BLACK = 0xFF06050B;
    private static final int WHITE_GOLD = 0xFFFFE3A0;
    /** Glyph band texture is 16:1 (1024 x 64): one repeat covers 16 band widths. */
    private static final float GLYPH_ASPECT = 16f;

    /** 3.21.0: the models are drawn within this distance (blocks) of the camera, further out the flat parts of 3.20.0 stand alone (they cost fewer quads). */
    static final float MODEL_RANGE = 64f;
    /** The tick the law tablet breaks into shards (the wave has left by then). */
    static final float TABLET_SHATTER = 8.5f;

    /** The poser / drawer of the 3D models (one per layer; render thread only). */
    private final JudgmentMeshDraw mesh = new JudgmentMeshDraw();

    /** Scratch corners of the quad being drawn (the layer draws on the render thread only, one effect at a time). */
    private final Vector3f q0 = new Vector3f(), q1 = new Vector3f(), q2 = new Vector3f(), q3 = new Vector3f();

    @Override
    public Set<VfxShape> shapes() {
        return EnumSet.of(VfxShape.JUDGMENT_BULLET, VfxShape.JUDGMENT_SIGIL, VfxShape.JUDGMENT_EDICT, VfxShape.JUDGMENT_CLEAVE, VfxShape.JUDGMENT_FIELD, VfxShape.JUDGMENT_CROSS);
    }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case JUDGMENT_BULLET -> 12;
            case JUDGMENT_SIGIL -> 200;
            case JUDGMENT_EDICT -> 24;
            case JUDGMENT_CLEAVE -> 10;
            case JUDGMENT_FIELD -> 160;
            default -> 66;
        };
    }

    @Override public int defaultColor(VfxShape s) { return WHITE_GOLD; }

    /** The tick the cross detonates on, from the effect's duration (54 % of it, 20..44). */
    static int chargeTicks(int duration) { return Mth.clamp(Math.round(duration * 0.54f), 20, 44); }

    @Override
    public void onSpawn(VfxInstance inst) {
        if (inst.shape == VfxShape.JUDGMENT_EDICT) VfxShake.add(inst.payload.from(), 1.4f, 10);
        if (inst.shape == VfxShape.JUDGMENT_CLEAVE) VfxShake.add(inst.payload.from(), 0.9f, 6);
    }

    @Override
    public void onTick(VfxInstance inst) {
        if (inst.shape == VfxShape.JUDGMENT_CROSS && inst.age() == chargeTicks(inst.duration)) VfxShake.add(inst.payload.from(), 2.6f, 16);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case JUDGMENT_BULLET -> bullet(inst, ctx, buf);
            case JUDGMENT_SIGIL -> sigil(inst, ctx, buf);
            case JUDGMENT_EDICT -> edict(inst, ctx, buf);
            case JUDGMENT_CLEAVE -> cleave(inst, ctx, buf);
            case JUDGMENT_FIELD -> field(inst, ctx, buf);
            case JUDGMENT_CROSS -> cross(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ small maths
    /** 1 while running, ramping 0 -> 1 over the first {@code in} ticks and 1 -> 0 over the last {@code out}. */
    private static float life(VfxInstance inst, float age, float in, float out) {
        float a = in <= 0 ? 1 : Mth.clamp(age / in, 0, 1);
        float b = out <= 0 ? 1 : Mth.clamp((inst.duration - age) / out, 0, 1);
        return Math.min(a, b);
    }

    /** A repeatable hash of (seed, i, k) in 0..1 (no allocation, same on every client). */
    static float h(long seed, int i, int k) {
        long z = seed * 0x9E3779B97F4A7C15L + i * 0xBF58476D1CE4E5B9L + k * 0x94D049BB133111EBL + 0x2545F4914F6CDD1DL;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z ^= z >>> 31;
        return (z >>> 40) / (float) (1 << 24);
    }

    private static float smooth(float e0, float e1, float x) {
        float u = Mth.clamp((x - e0) / (e1 - e0), 0, 1);
        return u * u * (3 - 2 * u);
    }

    /** A clock hand's angle that moves in ticks: {@code stepRad} forward every {@code period} ticks, snapping in the first fifth of each period with a little overshoot. */
    static float tickAngle(float age, float period, float stepRad) {
        float k = age / period;
        float whole = (float) Math.floor(k);
        float snap = VfxAnim.easeOutBack(Mth.clamp((k - whole) * 5f, 0, 1));
        return (whole + snap) * stepRad;
    }

    private static int a(int argb, float alpha) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(alpha, 0f, 1f)); }

    // ------------------------------------------------------------------ primitives
    private void quad(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend,
                      float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz,
                      float u0, float v0, float u1, float v1, int c0, int c1) {
        q0.set(ax, ay, az);
        q1.set(bx, by, bz);
        q2.set(cx, cy, cz);
        q3.set(dx, dy, dz);
        buf.quad(tex, blend, q0, q1, q2, q3, u0, v0, u1, v1, c0, c1);
    }

    /**
     * A ribbon from a to b that turns its face to the camera (the camera is at the origin: positions are camera-relative). Half widths hwA / hwB; the texture runs V = vA at a to V = vB at b
     * (U across); cA / cB are the vertex colours at the two ends (the alpha fades along the ribbon).
     */
    private void ribbon(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, float ax, float ay, float az, float bx, float by, float bz,
                        float hwA, float hwB, float vA, float vB, int cA, int cB) {
        float dx = bx - ax, dy = by - ay, dz = bz - az;
        float mx = (ax + bx) * 0.5f, my = (ay + by) * 0.5f, mz = (az + bz) * 0.5f;
        float ml = Mth.sqrt(mx * mx + my * my + mz * mz);
        float tx = 0, ty = 0, tz = 1;
        if (ml > 1e-4f) {
            tx = -mx / ml;
            ty = -my / ml;
            tz = -mz / ml;
        }
        float sx = dy * tz - dz * ty, sy = dz * tx - dx * tz, sz = dx * ty - dy * tx;
        float sl = Mth.sqrt(sx * sx + sy * sy + sz * sz);
        if (sl < 1e-5f) return;
        sx /= sl;
        sy /= sl;
        sz /= sl;
        quad(buf, tex, blend,
                ax - sx * hwA, ay - sy * hwA, az - sz * hwA, ax + sx * hwA, ay + sy * hwA, az + sz * hwA,
                bx + sx * hwB, by + sy * hwB, bz + sz * hwB, bx - sx * hwB, by - sy * hwB, bz - sz * hwB,
                0, vB, 1, vA, cA, cB);
    }

    /** An upright sprite of half size hw x hh round (cx, cy, cz), spanned by the unit vectors r (right) and u (up); the picture's top is at +u. */
    private void sprite(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, float cx, float cy, float cz, float rx, float ry, float rz,
                        float ux, float uy, float uz, float hw, float hh, int argb) {
        quad(buf, tex, blend,
                cx - rx * hw - ux * hh, cy - ry * hw - uy * hh, cz - rz * hw - uz * hh,
                cx + rx * hw - ux * hh, cy + ry * hw - uy * hh, cz + rz * hw - uz * hh,
                cx + rx * hw + ux * hh, cy + ry * hw + uy * hh, cz + rz * hw + uz * hh,
                cx - rx * hw + ux * hh, cy - ry * hw + uy * hh, cz - rz * hw + uz * hh,
                0, 0, 1, 1, argb, argb);
    }

    private void flare(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f c, float size, float rot, int argb) {
        if (size <= 0.001f || (argb >>> 24) < 3) return;
        buf.billboard(ctx, FLARE, VfxBlend.ADD, c, size, rot, argb);
    }

    private void mote(VfxRenderContext ctx, VfxVertexBuffer buf, float x, float y, float z, float size, int argb) {
        if (size <= 0.001f || (argb >>> 24) < 3) return;
        q0.set(x, y, z);
        buf.billboard(ctx, MOTE, VfxBlend.ADD, q0, size, 0f, argb);
    }

    /** The ground ring (judgment_ring) whose bright circle lies at {@code radius}. */
    private static void ringAt(VfxVertexBuffer buf, VfxPose pose, float radius, int argb) {
        if (radius <= 0.02f || (argb >>> 24) < 3) return;
        buf.plane(RING, VfxBlend.ADD, pose, radius / 0.965f, argb);
    }

    // ------------------------------------------------------------------ 3.21.0: the 3D models
    /** True when the 3D models are drawn here: the setting is on, the particle setting is not minimal and the thing is within {@code range} blocks of the camera (else the 3.20.0 parts stand alone). */
    private static boolean modelsOn(VfxRenderContext ctx, float x, float y, float z, float range) {
        return JudgmentModels.enabled() && ctx.detail >= 0.5f && x * x + y * y + z * z < range * range;
    }

    /** The yaw that turns a model's local +Z towards the camera (the camera is at the origin) from the point (x, z). */
    private static float faceYaw(float x, float z) {
        return (float) Math.atan2(-x, -z);
    }

    /**
     * Lance head: the faceted spear head rides the head of the needle (the tip a little ahead of it), spinning slowly about the line, scaled up with the distance so it still reads far away. At most 22 quads;
     * returns the quads drawn (0 = the flat flare alone).
     */
    private int lanceHead(VfxVertexBuffer buf, float hx, float hy, float hz, float ix, float iy, float iz, float s, float age, float na) {
        float dist = Mth.sqrt(hx * hx + hy * hy + hz * hz);
        float sc = (0.8f + 1.0f * s) * (1f + dist / 60f) * Mth.clamp(0.4f + age / 1.2f, 0.4f, 1f);
        JudgmentModels.Posed p = mesh.posed();
        p.clearPose();
        p.look(ix, iy, iz, 0f, 1f, 0f, age * 0.5f, sc);
        float back = (JudgmentModels.LANCE_TIP - 0.3f) * sc;
        return mesh.draw(buf, MODEL_ATLAS, JudgmentModels.LANCE, hx - ix * back, hy - iy * back, hz - iz * back, a(WHITE, na), 0.15f + 0.5f * na);
    }

    // ================================================================== JUDGMENT_BULLET
    private void bullet(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        float dx = b.x - a.x, dy = b.y - a.y, dz = b.z - a.z;
        float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.5f) return;
        float ix = dx / len, iy = dy / len, iz = dz / len;
        float s = Mth.clamp(inst.power, 0.15f, 2.5f) * (1f + len / 64f);              // a long shot is drawn a little thicker so it still reads from far away
        long seed = inst.seed;
        int col = inst.color, hot = VfxVertexBuffer.whiten(col, 0.8f);
        float dur = inst.duration;

        // where the head and the tail of the needle are: the head crosses the whole line in 2-3.6 ticks, then the tail catches up
        float flyT = Mth.clamp(2f + len / 40f, 2f, 3.6f);
        float hp = Mth.clamp(age / flyT, 0, 1);
        float headD = len * hp;
        float needleLen = Math.min(len, 8f + 6f * s);
        float settle = Mth.clamp((age - flyT) / 3.5f, 0, 1);
        float tailD = hp < 1f ? Math.max(0f, headD - needleLen) : (len - needleLen) + needleLen * settle;
        float na = 1f - settle * settle;

        // a basis across the line: u (sideways), w (the other way)
        float hl = Mth.sqrt(ix * ix + iz * iz);
        float ux = 1, uy = 0, uz = 0;
        if (hl > 0.05f) {
            ux = -iz / hl;
            uz = ix / hl;
        }
        float wx = iy * uz - iz * uy, wy = iz * ux - ix * uz, wz = ix * uy - iy * ux;

        // the hairline black crack trail that lingers: one jagged ribbon piece per ~4 blocks, behind the head only (ALPHA, drawn first)
        float crackFade = 1f - Mth.clamp((age - dur * 0.45f) / (dur * 0.55f), 0, 1);
        if (crackFade > 0.01f) {
            int n = ctx.seg(Mth.clamp((int) Math.ceil(len / 4f), 3, 16), 3);
            float segLen = len / n;
            float jag = Math.min(0.5f, segLen * 0.09f);
            float hwC = 0.3f + 0.1f * s;
            float v0 = h(seed, 99, 0);
            float px = a.x, py = a.y, pz = a.z;
            int dark = a(BLACK, 0.9f * crackFade);
            for (int i = 0; i < n; i++) {
                float d0 = i * segLen;
                if (d0 >= headD) break;
                float d1 = (i + 1) * segLen;
                float qx = a.x + ix * d1, qy = a.y + iy * d1, qz = a.z + iz * d1;
                if (i + 1 < n) {
                    float ja = (h(seed, i + 1, 0) - 0.5f) * 2f * jag, jb = (h(seed, i + 1, 1) - 0.5f) * 2f * jag;
                    qx += ux * ja + wx * jb;
                    qy += uy * ja + wy * jb;
                    qz += uz * ja + wz * jb;
                }
                float vA = v0 + d0 / 5f, vB = v0 + d1 / 5f;
                if (d1 > headD) {
                    float f = (headD - d0) / segLen;
                    qx = px + (qx - px) * f;
                    qy = py + (qy - py) * f;
                    qz = pz + (qz - pz) * f;
                    vB = vA + (vB - vA) * f;
                }
                ribbon(buf, CRACK, VfxBlend.ALPHA, px, py, pz, qx, qy, qz, hwC, hwC, vA, vB, dark, dark);
                px = qx;
                py = qy;
                pz = qz;
            }
            // a faint gold bleed along the crack
            float hx = a.x + ix * headD, hy = a.y + iy * headD, hz = a.z + iz * headD;
            ribbon(buf, BEAM, VfxBlend.ADD, a.x, a.y, a.z, hx, hy, hz, 0.34f + 0.12f * s, 0.34f + 0.12f * s, 0f, 1f, a(col, 0.11f * crackFade), a(col, 0.05f * crackFade));
        }

        // the needle: a wide gold glow, a narrow white core, the flare at its head
        if (na > 0.01f && headD - tailD > 0.05f) {
            float tx = a.x + ix * tailD, ty = a.y + iy * tailD, tz = a.z + iz * tailD;
            float hx = a.x + ix * headD, hy = a.y + iy * headD, hz = a.z + iz * headD;
            ribbon(buf, NEEDLE, VfxBlend.ADD, tx, ty, tz, hx, hy, hz, 0.7f * s + 0.06f, 0.7f * s + 0.06f, 1f, 0f, a(col, 0.6f * na), a(col, 0.9f * na));
            ribbon(buf, NEEDLE, VfxBlend.ADD, tx, ty, tz, hx, hy, hz, 0.26f * s + 0.04f, 0.26f * s + 0.04f, 1f, 0f, a(WHITE, 0.9f * na), a(WHITE, na));
            int lance = 0;
            if (age > 0.1f && modelsOn(ctx, hx, hy, hz, MODEL_RANGE)) lance = lanceHead(buf, hx, hy, hz, ix, iy, iz, s, age, na);        // 3.21.0: the 3D lance head (the flare behind it is dimmed so it shows)
            q0.set(hx, hy, hz);
            flare(ctx, buf, q0, (0.9f + 1.5f * s) * (0.7f + 0.3f * na), age * 0.5f, a(hot, lance > 0 ? 0.55f * na : na));
            VfxBloom.glow(ctx, buf, q0, 0.5f * s + 0.35f, col, 0.7f * na);
        }

        // stutter ghosts: short dashes that appear where the head passed and jump sideways on whole ticks
        int g = ctx.seg(6, 3);
        float frame = (float) Math.floor(age);
        for (int k = 0; k < g; k++) {
            float f = (k + 0.5f) / g;
            float dt = age - flyT * f;
            if (dt < 0f || dt > 3.2f) continue;
            float al = (float) Math.pow(1f - dt / 3.2f, 1.5);
            int fi = (int) frame;
            float so = (h(seed, k, fi) - 0.5f) * (0.5f + 0.9f * s), wo = (h(seed, k, fi + 77) - 0.5f) * (0.4f + 0.7f * s);
            float dist = len * f;
            float cx = a.x + ix * dist + ux * so + wx * wo, cy = a.y + iy * dist + uy * so + wy * wo, cz = a.z + iz * dist + uz * so + wz * wo;
            float dash = 0.7f + 1.1f * h(seed, k, 5);
            ribbon(buf, NEEDLE, VfxBlend.ADD, cx - ix * dash, cy - iy * dash, cz - iz * dash, cx + ix * dash * 0.4f, cy + iy * dash * 0.4f, cz + iz * dash * 0.4f,
                    0.2f * s + 0.07f, 0.2f * s + 0.07f, 1f, 0f, a(hot, 0.5f * al), a(WHITE, 0.95f * al));
        }

        // the muzzle: two shock rings facing along the shot, a flare and a glow
        VfxPose face = VfxPose.facing(new Vector3f(a.x + ix * 0.3f, a.y + iy * 0.3f, a.z + iz * 0.3f), new Vector3f(ix, iy, iz));
        for (int j = 0; j < 2; j++) {
            float tt = (age - j * 1.5f) / 5f;
            if (tt <= 0f || tt >= 1f) continue;
            float r = 0.5f + (2.0f + 1.2f * s) * VfxAnim.easeOutCubic(tt);
            ringAt(buf, face.spin(j * 0.7f), r, a(j == 0 ? WHITE : col, (float) Math.pow(1f - tt, 1.3) * (j == 0 ? 1f : 0.65f)));
        }
        float tm = Mth.clamp(age / 4f, 0, 1);
        if (tm < 1f) {
            q0.set(a.x + ix * 0.2f, a.y + iy * 0.2f, a.z + iz * 0.2f);
            flare(ctx, buf, q0, (1.2f + 2.0f * s) * (1f - 0.4f * tm), 0.4f, a(hot, 1f - tm));
            VfxBloom.glow(ctx, buf, q0, 0.9f * s + 0.5f, col, 0.9f * (1f - tm));
        }

        // the impact: a ring across the line, a flare and a glow where the needle lands
        float ti = (age - flyT) / 5f;
        if (ti > 0f && ti < 1f) {
            VfxPose land = VfxPose.facing(new Vector3f(b), new Vector3f(ix, iy, iz));
            ringAt(buf, land, 0.4f + (1.6f + s) * VfxAnim.easeOutCubic(ti), a(WHITE, (float) Math.pow(1f - ti, 1.2)));
            q0.set(b);
            flare(ctx, buf, q0, (1.0f + 1.8f * s) * (1f - ti * 0.5f), age * 0.3f, a(hot, 1f - ti));
            VfxBloom.glow(ctx, buf, q0, 0.8f * s + 0.4f, col, 0.8f * (1f - ti));
        }
    }

    // ================================================================== JUDGMENT_SIGIL
    private void sigil(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float open = VfxAnim.easeOutBack(Mth.clamp(age / 8f, 0, 1));
        float fade = life(inst, age, 0, 12);
        float power = Math.max(0.3f, inst.power);
        float R = 1.55f * power * open;
        if (R < 0.05f || fade <= 0.01f) return;
        int col = inst.color;
        Vector3f c = ctx.rel(inst.from(ctx)).add(0, 0.05f, 0);
        float pulse = 0.86f + 0.14f * Mth.sin(age * 0.12f);
        VfxPose floor = VfxPose.ground(c);

        VfxBloom.planeGlow(buf, floor, R, col, 0.5f * fade);
        buf.plane(SIGIL, VfxBlend.ADD, floor.spin(age * 0.018f), R, a(col, 0.95f * fade * pulse));
        ringAt(buf, floor, R * 1.0f, a(WHITE, 0.55f * fade));
        // every second the seal gives one soft pulse outwards, on the beat of the clock
        float tp = (age % 20f) / 20f;
        ringAt(buf, floor, R * (1.0f + 0.5f * VfxAnim.easeOutCubic(tp)), a(col, (1f - tp) * 0.35f * fade));
        // the clock ring ticks forward 6 degrees every half second
        buf.plane(CLOCK, VfxBlend.ADD, floor.lift(0.01f).spin(-tickAngle(age, 10f, 0.1047f)), R * 0.5f, a(STEEL, 0.7f * fade));

        // the glyph hovers above the creature: a blade and a balance, turned to the viewer round the vertical axis
        float gy = 1.8f * power + 0.35f + 0.1f * Mth.sin(age * 0.1f);
        float gh = 0.5f * power + 0.35f;
        float vx = -c.x, vz = -c.z, vl = Mth.sqrt(vx * vx + vz * vz);
        float rx = 1f, rz = 0f;
        if (vl > 1e-3f) {
            rx = vz / vl;
            rz = -vx / vl;
        }
        float jolt = 0.06f * (1f - VfxAnim.easeOutCubic(Mth.clamp((age % 40f) / 3f, 0, 1)));      // a small drop on every two seconds: the tick of the clock
        float gcy = c.y + gy - jolt;
        sprite(buf, BLADE, VfxBlend.ADD, c.x, gcy, c.z, rx, 0, rz, 0, 1, 0, gh * 0.375f, gh, a(WHITE, 0.95f * fade * pulse));
        q0.set(c.x, gcy, c.z);
        VfxBloom.glow(ctx, buf, q0, gh * 0.9f, col, 0.55f * fade);
        ribbon(buf, BEAM, VfxBlend.ADD, c.x, gcy - gh, c.z, c.x, c.y + 0.1f, c.z, 0.05f * power + 0.03f, 0.05f * power + 0.03f, age * 0.01f, age * 0.01f + 1.5f, a(col, 0.28f * fade), a(col, 0.05f * fade));

        // tally motes circle the seal and step on the clock; a few more rise from it
        int n = ctx.seg(4, 2);
        float step = tickAngle(age, 6f, 0.5236f);
        for (int i = 0; i < n; i++) {
            float an = Mth.TWO_PI * i / n + step;
            mote(ctx, buf, c.x + Mth.cos(an) * R * 0.86f, c.y + 0.07f, c.z + Mth.sin(an) * R * 0.86f, 0.22f * power + 0.1f, a(WHITE, 0.85f * fade));
        }
        int m = ctx.seg(4, 2);
        for (int i = 0; i < m; i++) {
            float u = Mth.frac(age / 55f + h(inst.seed, i, 3));
            float an = h(inst.seed, i, 4) * Mth.TWO_PI, rr = (0.25f + 0.5f * h(inst.seed, i, 5)) * R;
            mote(ctx, buf, c.x + Mth.cos(an) * rr, c.y + u * gy * 0.9f, c.z + Mth.sin(an) * rr, 0.16f * power + 0.06f, a(col, Mth.sin(u * Mth.PI) * 0.8f * fade));
        }
    }

    /**
     * The law tablet of the Edict: a tall stone tablet with glowing glyph panels rises out of the caster's position in 4.5 ticks, flashes as the wave leaves (ticks 3.5 to 7.5), trembles, and at tick 8.5 breaks into
     * shards that fly out and tumble. At most 14 quads for the tablet and 4 per shard (at most 8 shards); returns the quads drawn.
     */
    private int lawTablet(VfxVertexBuffer buf, VfxRenderContext ctx, Vector3f c, float age, float fade, long seed) {
        float K = 1.7f;
        JudgmentModels.Posed p = mesh.posed();
        p.clearPose();
        float gy = c.y - 0.07f;
        if (age < TABLET_SHATTER) {
            float rise = VfxAnim.easeOutCubic(Mth.clamp(age / 4.5f, 0f, 1f));
            float oy = gy - 3.4f * K * (1f - rise);
            float jit = age > 6.5f ? 0.03f * K * Mth.sin(age * 5f) * (age - 6.5f) / 2f : 0f;
            float flash = Mth.clamp(1f - Math.abs(age - 5.2f) / 2.4f, 0f, 1f);
            float heat = 0.15f + 0.85f * flash + 0.35f * smooth(7.2f, 8.5f, age);
            p.upright(faceYaw(c.x, c.z) + 0.28f, 0f, K);
            return mesh.draw(buf, MODEL_ATLAS, JudgmentModels.TABLET, c.x + jit, oy, c.z, a(WHITE, fade * Mth.clamp(0.35f + age / 2f, 0f, 1f)), Math.min(1f, heat));
        }
        float s = age - TABLET_SHATTER;
        int n = ctx.seg(8, 4);
        int drawn = 0;
        float heat = 0.8f * (1f - smooth(0f, 7f, s));
        for (int i = 0; i < n; i++) {
            float hh = h(seed, i, 61), h2 = h(seed, i, 62), h3 = h(seed, i, 63), h4 = h(seed, i, 64);
            float an = (i + 0.5f) / n * Mth.TWO_PI + (h2 - 0.5f) * 0.7f;
            float spd = (0.2f + 0.2f * h3) * K / 1.7f, up = 0.14f + 0.2f * h4;
            float px = c.x + Mth.cos(an) * (0.2f * K + spd * s), pz = c.z + Mth.sin(an) * (0.2f * K + spd * s);
            float py = Math.max(gy + 0.1f, gy + (0.4f + 2.7f * hh) * K + up * s - 0.0175f * s * s);
            float size = K * (0.45f + 0.5f * hh);
            p.tumble(h(seed, i, 65) - 0.5f, 1f, h(seed, i, 66) - 0.5f, h4 * 6f + s * (0.25f + 0.3f * h3), size, size, size);
            drawn += mesh.draw(buf, MODEL_ATLAS, JudgmentModels.SHARD, px, py, pz, a(WHITE, fade), heat);
        }
        if (s < 3f) {
            q0.set(c.x, gy + 1.6f * K, c.z);
            flare(ctx, buf, q0, 5f * (1f - s / 3f) + 1f, age * 0.3f, a(WHITE, 1f - s / 3f));
        }
        return drawn;
    }

    // ================================================================== JUDGMENT_EDICT
    private void edict(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float p = Mth.clamp(age / 20f, 0, 1);
        float R = inst.power * VfxAnim.easeOutCubic(p);
        float fade = life(inst, age, 0, 7);
        float lead = 1f - 0.45f * p;
        if (fade <= 0.01f) return;
        int col = inst.color;
        long seed = inst.seed;
        Vector3f c = ctx.rel(inst.from(ctx)).add(0, 0.07f, 0);
        VfxPose floor = VfxPose.ground(c);
        boolean models = modelsOn(ctx, c.x, c.y, c.z, MODEL_RANGE);                           // 3.21.0: the law tablet and its shards (the glyph bands and the motes are thinner while they are drawn)

        if (R > 0.3f) {
            // the erased band under the wave, the wave itself, and the bright leading ring
            buf.plane(WAVE, VfxBlend.ALPHA, floor, R * 0.97f / 0.955f, a(BLACK, 0.55f * fade * lead));
            buf.plane(CRACKS_GROUND, VfxBlend.ALPHA, floor.spin(h(seed, 1, 1) * Mth.TWO_PI), R * 0.85f, a(BLACK, 0.55f * fade * Mth.clamp(age / 5f, 0, 1)));
            buf.plane(WAVE, VfxBlend.ADD, floor.spin(age * 0.01f), R / 0.955f, a(col, 0.9f * fade * lead));
            ringAt(buf, floor, R, a(WHITE, 0.95f * fade * lead));

            // marks written in the wave: two bands of text-like glyphs, counter-rotating (white, over the dark band)
            float bw = Mth.clamp(0.08f * inst.power, 0.7f, 2.4f);
            float outer = R * 0.93f, inner = outer - bw;
            if (inner > 0.15f) {
                int segs = ctx.seg(Mth.clamp(Math.round(R * 1.1f), 10, models ? 24 : 30), 8);
                int reps = Math.max(1, Math.round(Mth.TWO_PI * (outer - bw * 0.5f) / (GLYPH_ASPECT * bw)));
                buf.ring(GLYPHS, VfxBlend.ALPHA, floor, inner, outer, segs, reps, age * 0.012f, a(WHITE_GOLD, 0.97f * fade * lead));
            }
            float bw2 = bw * 0.8f, outer2 = R * 0.70f, inner2 = outer2 - bw2;
            if (inner2 > 0.15f) {
                int segs2 = ctx.seg(Mth.clamp(Math.round(R * 0.8f), 8, models ? 14 : 20), 6);
                int reps2 = Math.max(1, Math.round(Mth.TWO_PI * (outer2 - bw2 * 0.5f) / (GLYPH_ASPECT * bw2)));
                buf.ring(GLYPHS, VfxBlend.ALPHA, floor, inner2, outer2, segs2, reps2, -age * 0.015f, a(WHITE, 0.7f * fade * lead));
            }
        }

        int tablet = models ? lawTablet(buf, ctx, c, age, fade, seed) : 0;
        // things erased upward: motes that are born just behind the front and rise out of the ground
        int mN = tablet > 0 ? ctx.seg(10, 5) : ctx.seg(22, 8);
        float msize = 0.5f + 0.02f * inst.power;
        for (int i = 0; i < mN; i++) {
            float born = h(seed, i, 0) * 16f;
            float life = 10f + 6f * h(seed, i, 1);
            float u = (age - born) / life;
            if (u <= 0f || u >= 1f) continue;
            float rs = inst.power * VfxAnim.easeOutCubic(born / 20f) * (0.45f + 0.5f * h(seed, i, 2));
            float an = h(seed, i, 3) * Mth.TWO_PI + u * 0.6f;
            float rise = (age - born) * (0.12f + 0.006f * inst.power) * (1f + u);
            float mx = c.x + Mth.cos(an) * rs, my = c.y + 0.1f + rise, mz = c.z + Mth.sin(an) * rs;
            float al = (float) Math.pow(Mth.sin(u * Mth.PI), 0.8) * fade;
            if ((i & 1) == 0) {
                mote(ctx, buf, mx, my, mz, msize * (1f - 0.5f * u) * (0.8f + 0.5f * h(seed, i, 4)), a(WHITE, al));
            } else {                                                      // a thin dash of light going straight up
                float dash = (1.2f + 2.2f * h(seed, i, 5)) * (0.6f + 0.012f * inst.power);
                ribbon(buf, NEEDLE, VfxBlend.ADD, mx, my - dash, mz, mx, my, mz, 0.1f + 0.004f * inst.power, 0.1f + 0.004f * inst.power, 1f, 0f, a(col, 0f), a(WHITE, 0.9f * al));
            }
        }

        // the flash and the pillar at the middle in the first ticks
        float t0 = Mth.clamp(age / 11f, 0, 1);
        if (t0 < 1f) {
            ribbon(buf, BEAM, VfxBlend.ADD, c.x, c.y, c.z, c.x, c.y + 11f, c.z, 0.9f * (1f - t0) + 0.15f, 0.5f * (1f - t0) + 0.1f, 0f, 1.6f, a(col, 0.8f * (1f - t0)), a(col, 0f));
            q0.set(c.x, c.y + 0.5f, c.z);
            flare(ctx, buf, q0, 6f * (1f - t0) + 1f, age * 0.2f, a(WHITE, 1f - t0));
        }
    }

    /**
     * Guillotine blade of the Cleave: a huge slanted blade (bevel, glowing edge, gold straps) rides the crescent, edge first, from the left end to the right end in 4.5 ticks (the broad face in the plane of the arc,
     * the bevel towards the caster). At most 22 quads; returns the quads drawn (0 = the old falling-blade sprites are drawn instead).
     */
    private int guillotine(VfxVertexBuffer buf, float ox, float oy, float oz, Vector3f e1, Vector3f e2, Vector3f nrm, float R, float age, float fade) {
        float prog = VfxAnim.easeOutCubic(Mth.clamp((age - 0.2f) / 4.4f, 0f, 1f));
        float ang = -1.05f + 2.1f * prog;
        float ca = Mth.cos(ang), sa = Mth.sin(ang);
        float rx = e1.x * ca + e2.x * sa, ry = e1.y * ca + e2.y * sa, rz = e1.z * ca + e2.z * sa;          // outward along the radius
        float tx = -e1.x * sa + e2.x * ca, ty = -e1.y * sa + e2.y * ca, tz = -e1.z * sa + e2.z * ca;       // the way the stroke travels
        float K = 0.42f * R * (0.7f + 0.3f * Mth.clamp(age / 1.2f, 0f, 1f));
        float back = 0.175f * K;
        float px = ox + rx * R - tx * back, py = oy + ry * R - ty * back, pz = oz + rz * R - tz * back;
        JudgmentModels.Posed p = mesh.posed();
        p.clearPose();
        p.axes(rx, ry, rz, -tx, -ty, -tz, nrm.x, nrm.y, nrm.z, K, K, K);
        float heat = 0.15f + 0.5f * (1f - Mth.clamp((age - 2.5f) / 4f, 0f, 1f));
        return mesh.draw(buf, MODEL_ATLAS, JudgmentModels.GUILLOTINE, px, py, pz, a(WHITE, fade * Mth.clamp(age / 0.7f, 0f, 1f)), heat);
    }

    // ================================================================== JUDGMENT_CLEAVE
    private void cleave(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float fade = life(inst, age, 0, 4);
        if (fade <= 0.01f) return;
        Vector3f o = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        float fx = to.x - o.x, fz = to.z - o.z, hl = Mth.sqrt(fx * fx + fz * fz);
        if (hl < 0.1f) {
            fx = 0;
            fz = 1;
            hl = 3f;
        } else {
            fx /= hl;
            fz /= hl;
        }
        float R = Math.max(1.5f, inst.power) * 0.9f;                   // the radius of the crescent
        int col = inst.color, hot = VfxVertexBuffer.whiten(col, 0.8f);
        long seed = inst.seed;

        // the crescent stands in a plane in front of the caster (a smile whose edge is its lower side), pitched back a little and rolled a little so the stroke falls from left to right
        float pitch = 0.45f, roll = 0.32f;
        float sx = -fz, sz = fx;                                       // right of the caster
        float nxv = fx * Mth.cos(pitch), nyv = Mth.sin(pitch), nzv = fz * Mth.cos(pitch);      // the plane's normal
        Vector3f nrm = new Vector3f(nxv, nyv, nzv);
        Vector3f side = new Vector3f(sx, 0, sz);
        Vector3f down = new Vector3f(nrm).cross(side);                 // = down, pushed forward by the pitch
        float cr = Mth.cos(roll), sr = Mth.sin(roll);
        Vector3f e1 = new Vector3f(down).mul(cr).add(side.x * sr, side.y * sr, side.z * sr);
        Vector3f e2 = new Vector3f(side).mul(cr).sub(down.x * sr, down.y * sr, down.z * sr);
        // the lowest point of the crescent, where the stroke lands: 'to', at head height of the caster
        float mx = o.x + fx * hl, my = o.y + 0.9f, mz = o.z + fz * hl;
        VfxPose pose = new VfxPose(new Vector3f(mx - e1.x * R, my - e1.y * R, mz - e1.z * R), e1, e2, nrm);
        float a0 = -1.05f, a1 = 1.05f;
        boolean models = modelsOn(ctx, mx, my, mz, MODEL_RANGE);                                // 3.21.0: the 3D guillotine blade (the arc is drawn a little coarser to leave it room)

        float headP = VfxAnim.easeOutCubic(Mth.clamp(age / 3.5f, 0, 1));
        float tailP = VfxAnim.easeInCubic(Mth.clamp((age - 1.5f) / 6.5f, 0, 1));
        float headA = a0 + (a1 - a0) * headP, tailA = a0 + (a1 - a0) * tailP;
        int segs = models ? ctx.seg(8, 5) : ctx.seg(10, 5);
        float wBody = 0.34f * R;
        float len = headA - tailA;
        if (len > 0.03f) {
            buf.arc(SLASH, VfxBlend.ADD, pose, R + 0.04f * wBody, wBody * 1.3f, tailA, len, segs, a(col, 0.42f * fade));               // a soft glow round the whole blade
            buf.arc(SLASH, VfxBlend.ALPHA, pose, R, wBody * 0.6f, tailA, len, segs, a(BLACK, 0.95f * fade));                               // the black core
            buf.arc(SLASH, VfxBlend.ADD, pose, R + wBody * 0.24f, wBody * 0.52f, tailA, len, segs, a(VfxVertexBuffer.whiten(col, 0.5f), fade));   // the white-gold outer edge of the blade
            buf.arc(SLASH, VfxBlend.ADD, pose, R - wBody * 0.3f, wBody * 0.17f, tailA, len, segs, a(WHITE, 0.85f * fade));                 // the thin bright inner edge
            buf.arc(SLASH, VfxBlend.ADD, pose, R + wBody * 0.5f, wBody * 0.1f, tailA, len, segs, a(WHITE, fade));                         // the hot hair-line on the outer edge
        }
        // two stutter echoes behind it
        int es = models ? ctx.seg(4, 3) : ctx.seg(6, 4);
        for (int k = 1; k <= 2; k++) {
            float ae = age - k * 1.6f;
            if (ae <= 0f) continue;
            float hp2 = VfxAnim.easeOutCubic(Mth.clamp(ae / 3.5f, 0, 1)), tp2 = VfxAnim.easeInCubic(Mth.clamp((ae - 1.5f) / 6.5f, 0, 1));
            float h2 = a0 + (a1 - a0) * hp2, t2 = a0 + (a1 - a0) * tp2;
            if (h2 - t2 > 0.03f) buf.arc(SLASH, VfxBlend.ADD, pose, R * (1f - 0.06f * k) + wBody * 0.24f, wBody * 0.5f, t2, h2 - t2, es, a(hot, (k == 1 ? 0.42f : 0.2f) * fade));
        }

        // the blade falls from above onto the lowest point of the crescent and leaves afterimages
        float bh = 0.46f * R;
        float vx = -mx, vz = -mz, vl = Mth.sqrt(vx * vx + vz * vz);
        float rx = 1f, rz = 0f;
        if (vl > 1e-3f) {
            rx = vz / vl;
            rz = -vx / vl;
        }
        float drop = 6.5f * R / 5f;
        float bladeA = age < 3f ? 1f : 1f - Mth.clamp((age - 3f) / 4f, 0, 1);
        int blade3d = models ? guillotine(buf, pose.origin().x, pose.origin().y, pose.origin().z, e1, e2, nrm, R, age, fade) : 0;
        for (int k = 0; blade3d == 0 && k < 4; k++) {
            float ak = age - k * 0.45f;
            if (ak < 0f) continue;
            float yk = my + drop * (1f - VfxAnim.easeInCubic(Mth.clamp(ak / 3f, 0, 1)));
            float al = (k == 0 ? 0.9f : 0.45f / k) * (k == 0 ? bladeA : 1f - Mth.clamp((age - 3f) / 3f, 0, 1)) * fade;
            if (al <= 0.01f) continue;
            sprite(buf, BLADE, VfxBlend.ADD, mx, yk, mz, rx, 0, rz, 0, 1, 0, bh * 0.1875f, bh * 0.5f, a(k == 0 ? WHITE : hot, al));
            if (k == 0) ribbon(buf, NEEDLE, VfxBlend.ADD, mx, yk + bh * 3.2f, mz, mx, yk + bh * 0.3f, mz, 0.1f * R, 0.1f * R, 1f, 0f, a(col, 0f), a(hot, 0.8f * al));
        }
        // the landing: two rings in the plane of the crescent, a flare, a glow
        float ti = (age - 3f) / 4f;
        if (ti > 0f && ti < 1f) {
            VfxPose land = new VfxPose(new Vector3f(mx, my, mz), e1, e2, nrm);
            ringAt(buf, land, 0.3f + R * 0.55f * VfxAnim.easeOutCubic(ti), a(WHITE, (1f - ti) * fade));
            ringAt(buf, land.spin(0.6f), 0.2f + R * 0.35f * VfxAnim.easeOutCubic(ti), a(col, 0.7f * (1f - ti) * fade));
            q0.set(mx, my, mz);
            flare(ctx, buf, q0, R * 0.9f * (1f - 0.5f * ti), 0.4f, a(hot, (1f - ti) * fade));
            VfxBloom.glow(ctx, buf, q0, R * 0.3f, col, 0.9f * (1f - ti) * fade);
        }
        // sparks off the head of the stroke
        int sp = ctx.seg(6, 2);
        int frame = (int) Math.floor(age);
        float sparkA = (1f - Mth.clamp((age - 3.5f) / 4f, 0, 1)) * fade;
        if (sparkA > 0.01f && age >= 0.2f) {
            for (int i = 0; i < sp; i++) {
                float an = headA - h(seed, i, frame) * 0.5f;
                float out = R + wBody * 0.5f + (h(seed, i, frame + 31) - 0.3f) * 1.2f;
                float ca = Mth.cos(an) * out, sa = Mth.sin(an) * out;
                mote(ctx, buf, pose.origin().x + e1.x * ca + e2.x * sa, pose.origin().y + e1.y * ca + e2.y * sa, pose.origin().z + e1.z * ca + e2.z * sa,
                        0.28f + 0.2f * h(seed, i, 9), a(hot, sparkA));
            }
        }
    }

    /**
     * Scales of judgment over the field: a golden balance (pillar, ring, beam, two pans on chains) hovers above the centre, turned towards the viewer, rises and fades in over 14 ticks, tilts and settles (the right pan,
     * holding a burning heart, ends lower than the left one, which holds a feather), breathes while the field lasts and fades with it. At most 56 quads; returns the quads drawn (0 = the flat field alone).
     */
    private int scalesOfJudgment(VfxVertexBuffer buf, VfxRenderContext ctx, Vector3f c, float age, float fade, float R, int col) {
        float ins = smooth(0f, 14f, age);
        float s = 1.7f * (0.45f + 0.55f * VfxAnim.easeOutBack(Mth.clamp(age / 12f, 0f, 1f))) * Mth.clamp(R / 8f, 0.75f, 1.35f);
        float oy = c.y + 1.6f + 0.15f * Mth.sin(age * 0.07f) - 1.2f * (1f - ins);
        float tilt = 0.5f * (float) Math.exp(-age / 55f) * Mth.cos(age * 0.11f) - 0.09f + 0.015f * Mth.sin(age * 0.05f);
        float sway = 0.05f * Mth.sin(age * 0.09f + 1f) + 0.10f * (float) Math.exp(-age / 40f) * Mth.sin(age * 0.11f);
        JudgmentModels.Posed p = mesh.posed();
        p.clearPose();
        p.roll()[JudgmentModels.S_BEAM] = tilt;
        p.roll()[JudgmentModels.S_PAN_L] = -tilt + sway;
        p.roll()[JudgmentModels.S_PAN_R] = -tilt - sway;
        p.bend()[JudgmentModels.S_PAN_L] = 0.04f * Mth.sin(age * 0.07f + 0.5f);
        p.bend()[JudgmentModels.S_PAN_R] = 0.04f * Mth.sin(age * 0.07f + 2.4f);
        p.upright(faceYaw(c.x, c.z) + 0.4f * Mth.sin(age * 0.019f), 0f, s);
        int n = mesh.draw(buf, MODEL_ATLAS, JudgmentModels.SCALES, c.x, oy, c.z, a(WHITE, 0.97f * fade * ins), 0.22f + 0.12f * Mth.sin(age * 0.1f));
        if (n > 0) {
            q0.set(c.x, oy + JudgmentModels.SCALES_PIVOT_Y * s, c.z);
            VfxBloom.glow(ctx, buf, q0, 1.5f * s, col, 0.3f * fade * ins);
        }
        return n;
    }

    // ================================================================== JUDGMENT_FIELD
    private void field(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float open = VfxAnim.easeOutCubic(Mth.clamp(age / 10f, 0, 1));
        float fade = life(inst, age, 0, 20);
        float R = inst.power * open;
        if (R < 0.2f || fade <= 0.01f) return;
        int col = inst.color;
        long seed = inst.seed;
        float breathe = 0.82f + 0.18f * Mth.sin(age * 0.08f);
        Vector3f c = ctx.rel(inst.from(ctx)).add(0, 0.06f, 0);
        VfxPose floor = VfxPose.ground(c);

        buf.plane(VfxTextures.GLOW, VfxBlend.ADD, floor, R * 1.02f, a(col, 0.10f * fade * breathe));
        buf.plane(SIGIL, VfxBlend.ADD, floor.spin(-age * 0.006f), R, a(STEEL, 0.13f * fade));
        ringAt(buf, floor, R, a(col, 0.85f * fade * breathe));
        ringAt(buf, floor, R * 0.55f, a(WHITE, 0.22f * fade));
        buf.plane(CLOCK, VfxBlend.ADD, floor.spin(tickAngle(age, 20f, 0.1047f)), R * 0.66f, a(STEEL, 0.5f * fade));
        buf.plane(CLOCK, VfxBlend.ADD, floor.spin(-tickAngle(age + 7f, 20f, 0.1047f) + 1f), R * 0.36f, a(STEEL, 0.34f * fade));
        buf.plane(CRACKS_GROUND, VfxBlend.ALPHA, floor.spin(h(seed, 7, 0) * Mth.TWO_PI), R * 0.98f, a(BLACK, 0.8f * fade));
        buf.plane(CRACKS_GROUND, VfxBlend.ALPHA, floor.spin(h(seed, 8, 0) * Mth.TWO_PI + 2.1f), R * 0.62f, a(BLACK, 0.55f * fade));

        // 3.21.0: the scales of judgment hover above the centre (the motes are fewer while they are drawn)
        boolean scales = R > 2f && modelsOn(ctx, c.x, c.y, c.z, MODEL_RANGE) && scalesOfJudgment(buf, ctx, c, age, fade, R, col) > 0;
        // thin beams of light standing on the rim, each flickering slowly on its own
        int nb = ctx.seg(12, 6);
        for (int i = 0; i < nb; i++) {
            float an = Mth.TWO_PI * i / nb + (h(seed, i, 0) - 0.5f) * 0.2f;
            float bx = c.x + Mth.cos(an) * R * 0.99f, bz = c.z + Mth.sin(an) * R * 0.99f;
            float top = (4f + 4f * h(seed, i, 1)) * open;
            float flick = 0.55f + 0.45f * Mth.sin(age * (0.05f + 0.03f * h(seed, i, 2)) + h(seed, i, 3) * Mth.TWO_PI);
            float sc = -age * 0.012f * (0.6f + h(seed, i, 4));
            ribbon(buf, BEAM, VfxBlend.ADD, bx, c.y, bz, bx, c.y + top, bz, 0.34f, 0.22f, sc, sc + top / 6f, a(col, 0.36f * flick * fade), a(col, 0f));
            ribbon(buf, SHAFT, VfxBlend.ADD, bx, c.y, bz, bx, c.y + top * 0.9f, bz, 0.07f, 0.04f, sc, sc + top / 6f, a(WHITE, 0.7f * flick * fade), a(WHITE, 0f));
        }
        int m = scales ? ctx.seg(4, 2) : ctx.seg(8, 3);
        for (int i = 0; i < m; i++) {
            float u = Mth.frac(age / 70f + h(seed, i, 5));
            float an = h(seed, i, 6) * Mth.TWO_PI, rr = (0.45f + 0.55f * h(seed, i, 7)) * R;
            mote(ctx, buf, c.x + Mth.cos(an) * rr, c.y + 0.1f + u * 3.2f, c.z + Mth.sin(an) * rr, 0.22f, a(WHITE, Mth.sin(u * Mth.PI) * 0.65f * fade));
        }
    }

    /**
     * The sword of Final Judgment: a vertical great sword (long fullered blade, winged crossguard, wrapped hilt), point down, comes down out of the sky onto the target while the cross charges (slowly, ghostly at first,
     * trembling when it hangs over the point), plunges in at the boom and stands in the ground while the cross glows, fading with it. The guard stands where the crossbar of light crosses the pillar. At most 36 quads;
     * returns the quads drawn.
     */
    private int swordOfJudgment(VfxVertexBuffer buf, Vector3f g, float S, float d, float p, float age, float fadeAll, float nx, float nz) {
        float K = 1.96f * S;
        float gap, alpha, heat, roll;
        if (d < 0f) {
            gap = 3f * S + 28f * S * (float) Math.pow(1f - p, 2.2);
            gap += 0.1f * S * Mth.sin(age * 0.9f) * p;
            alpha = (0.35f + 0.6f * p) * smooth(0f, 0.16f, p);
            heat = 0.12f + 0.4f * p;
            roll = 0.3f * Mth.sin(age * 0.03f);
        } else {
            float pl = Mth.clamp(d / 2.2f, 0f, 1f);
            gap = 3f * S - 3.5f * S * VfxAnim.easeInCubic(pl);
            if (d > 2.2f) gap += 0.15f * S * (float) Math.exp(-(d - 2.2f) / 3f) * Mth.sin((d - 2.2f) * 2.2f);
            alpha = fadeAll;
            heat = 0.35f + 0.65f * (1f - Mth.clamp(d / 10f, 0f, 1f));
            roll = 0.3f * Mth.sin(age * 0.03f) * (1f - 0.7f * pl);
        }
        JudgmentModels.Posed pz = mesh.posed();
        pz.clearPose();
        pz.look(0f, 1f, 0f, nx, 0f, nz, roll, K);
        return mesh.draw(buf, MODEL_ATLAS, JudgmentModels.SWORD, g.x, g.y + gap, g.z, a(WHITE, alpha), heat);
    }

    // ================================================================== JUDGMENT_CROSS
    private void cross(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float S = Mth.clamp(inst.power, 0.4f, 4f);
        int T = inst.duration;
        int TC = chargeTicks(T);
        float d = age - TC;                                            // < 0 while charging
        float p = Mth.clamp(age / TC, 0, 1);                           // charge progress
        int col = inst.color, hot = VfxVertexBuffer.whiten(col, 0.8f);
        long seed = inst.seed;
        float fadeAll = Mth.clamp((T - age) / 16f, 0, 1);
        if (fadeAll <= 0.01f) return;

        Vector3f g = ctx.rel(inst.from(ctx));                          // the ground point under the target
        float yc = g.y + 9f * S;                                       // the crossing of the cross
        float yTop = yc + 5f * S;
        Vector3f cen = new Vector3f(g.x, yc, g.z);
        VfxPose floor = VfxPose.ground(new Vector3f(g.x, g.y + 0.06f, g.z));
        float tvx = -g.x, tvz = -g.z, tl = Mth.sqrt(tvx * tvx + tvz * tvz);       // from the target towards the camera, on the ground
        float nx = 0, nz = 1;
        if (tl > 1e-3f) {
            nx = tvx / tl;
            nz = tvz / tl;
        }
        float bxd = ctx.camRight.x, bzd = ctx.camRight.z, bl = Mth.sqrt(bxd * bxd + bzd * bzd);       // the crossbar lies across the view
        if (bl > 1e-3f) {
            bxd /= bl;
            bzd /= bl;
        } else {
            bxd = 1;
            bzd = 0;
        }
        VfxPose faceCam = VfxPose.facing(cen, new Vector3f(nx, 0, nz));
        float stutter = 0.8f + 0.2f * h(seed, (int) Math.floor(age), 7);
        boolean models = modelsOn(ctx, g.x, g.y + 6f * S, g.z, MODEL_RANGE);                         // 3.21.0: the 3D sword of judgment (the motes are fewer while it is drawn)

        // ---- the clock rings: they spin down in stepping ticks and freeze at the detonation
        float ang = 16f * VfxAnim.easeOutCubic(p);
        float stepped = (float) Math.floor(ang / 0.2618f) * 0.2618f;
        float click = d >= 0f && d < 3f ? 0.05f * Mth.sin(d * 4f) * (3f - d) / 3f : 0f;
        float ringIn = Mth.clamp(age / 8f, 0, 1);
        buf.plane(CLOCK, VfxBlend.ADD, floor.lift(0.01f).spin(stepped + click), 2.7f * S, a(STEEL, 0.9f * ringIn * fadeAll));
        buf.plane(SIGIL, VfxBlend.ADD, floor.spin(-0.35f * stepped), 4.2f * S, a(col, 0.6f * ringIn * fadeAll));
        if (p > 0.25f) buf.plane(CLOCK, VfxBlend.ADD, faceCam.spin(-stepped * 0.8f), 3.4f * S, a(STEEL, (d < 0f ? 0.75f : 0.5f) * smooth(0.25f, 0.6f, p) * fadeAll));

        if (d < 0f) {
            // ---- charge: a thread of light from the sky, streaks converging, the point glowing up
            float drop = VfxAnim.easeOutCubic(Mth.clamp(age / 14f, 0, 1));
            float thread = 0.035f * S + 0.06f * S * VfxAnim.easeInCubic(p);
            float botY = yTop + (g.y - yTop) * drop;
            ribbon(buf, BEAM, VfxBlend.ADD, g.x, botY, g.z, g.x, yTop + 24f * S, g.z, thread * 2.4f, thread * 2.4f, -age * 0.03f, -age * 0.03f + 4f, a(col, (0.12f + 0.3f * p) * stutter), a(col, 0f));
            ribbon(buf, BEAM, VfxBlend.ADD, g.x, botY, g.z, g.x, yTop + 24f * S, g.z, thread, thread, -age * 0.05f, -age * 0.05f + 4f, a(WHITE, (0.35f + 0.55f * p) * stutter), a(WHITE, 0f));
            int ns = ctx.seg(14, 6);
            for (int i = 0; i < ns; i++) {
                float delay = h(seed, i, 0) * TC * 0.5f;
                float q = Mth.clamp((age - delay) / (TC - delay + 0.01f), 0, 1);
                if (q <= 0f || q >= 1f) continue;
                float ph = h(seed, i, 1) * Mth.TWO_PI, z = h(seed, i, 2) * 2f - 1f, rr = Mth.sqrt(1f - z * z);
                float dist = (10f + 6f * h(seed, i, 3)) * S;
                float k = 1f - VfxAnim.easeInCubic(q);
                float hx = cen.x + Mth.cos(ph) * rr * dist * k, hy = cen.y + z * dist * k, hz = cen.z + Mth.sin(ph) * rr * dist * k;
                float tail = Math.min(dist * 0.5f, 3f * S) * Math.min(1f, q * 3f);
                float tx = hx + Mth.cos(ph) * rr * tail, ty = hy + z * tail, tz = hz + Mth.sin(ph) * rr * tail;
                float al = Math.min(q * 5f, 1f) * (1f - smooth(0.88f, 1f, q));
                ribbon(buf, NEEDLE, VfxBlend.ADD, tx, ty, tz, hx, hy, hz, 0.13f * S, 0.13f * S, 1f, 0f, a(col, 0.5f * al), a(WHITE, al));
            }
            VfxBloom.glow(ctx, buf, cen, (0.5f + 2.5f * p * p) * S, col, (0.3f + 0.7f * p) * stutter);
            flare(ctx, buf, cen, (0.3f + 3.2f * (float) Math.pow(p, 1.5)) * S, age * 0.03f, a(hot, (0.4f + 0.6f * p) * stutter));
            buf.plane(VfxTextures.GLOW, VfxBlend.ADD, floor, 3.5f * S * (0.4f + 0.6f * p), a(col, 0.5f * p));
            if (models) swordOfJudgment(buf, g, S, d, p, age, fadeAll, nx, nz);
            return;
        }

        // ---- detonation
        float open = VfxAnim.easeOutBack(Mth.clamp(d / 3f, 0, 1));
        float barOpen = VfxAnim.easeOutCubic(Mth.clamp(d / 4f, 0, 1));
        float w = (float) Math.pow(fadeAll, 0.6);
        float L = 6.2f * S * barOpen;
        float flash = 1f - Mth.clamp(d / 8f, 0, 1);

        // the sky column, the pillar (halo, core, scrolling shimmer) and two thin steel pillars either side
        float hSky = yTop + 80f - g.y;
        ribbon(buf, BEAM, VfxBlend.ADD, g.x, g.y, g.z, g.x, g.y + hSky, g.z, 1.7f * S * open * w, 1.7f * S * open * w, 0f, 6f, a(col, 0.2f * fadeAll), a(col, 0f));
        float hP = yTop - g.y;
        ribbon(buf, BEAM, VfxBlend.ADD, g.x, g.y, g.z, g.x, yTop, g.z, 1.0f * S * open * w, 0.85f * S * open * w, 0f, 2.2f, a(col, 0.65f * fadeAll), a(col, 0.3f * fadeAll));
        ribbon(buf, SHAFT, VfxBlend.ADD, g.x, g.y, g.z, g.x, yTop, g.z, 0.3f * S * open * w, 0.24f * S * open * w, 0f, 2.2f, a(WHITE, fadeAll), a(WHITE, 0.85f * fadeAll));
        float sc = -age * 0.05f;
        ribbon(buf, BEAM, VfxBlend.ADD, g.x, g.y, g.z, g.x, yTop, g.z, 0.62f * S * open * w, 0.5f * S * open * w, sc, sc + hP / (7f * S), a(hot, 0.5f * fadeAll), a(hot, 0.2f * fadeAll));
        for (int sgn = -1; sgn <= 1; sgn += 2) {
            float ox = bxd * sgn * 2.8f * S * barOpen, oz = bzd * sgn * 2.8f * S * barOpen;
            ribbon(buf, BEAM, VfxBlend.ADD, g.x + ox, g.y, g.z + oz, g.x + ox, g.y + hP * 0.75f, g.z + oz, 0.13f * S * w, 0.1f * S * w, sc, sc + 3f, a(STEEL, 0.5f * fadeAll), a(STEEL, 0f));
        }
        // the crossbar: two halves from the crossing out to the tips, fading towards the tips
        for (int sgn = -1; sgn <= 1; sgn += 2) {
            float ex = cen.x + bxd * sgn * L, ez = cen.z + bzd * sgn * L;
            ribbon(buf, BEAM, VfxBlend.ADD, cen.x, cen.y, cen.z, ex, cen.y, ez, 0.95f * S * w, 0.55f * S * w, 0f, 1f, a(col, 0.7f * fadeAll), a(col, 0.12f * fadeAll));
            ribbon(buf, SHAFT, VfxBlend.ADD, cen.x, cen.y, cen.z, ex, cen.y, ez, 0.4f * S * w, 0.26f * S * w, 0f, 1f, a(WHITE, fadeAll), a(WHITE, 0.5f * fadeAll));
            ribbon(buf, BEAM, VfxBlend.ADD, cen.x, cen.y, cen.z, ex, cen.y, ez, 0.5f * S * w, 0.3f * S * w, sc, sc + L / (5f * S), a(hot, 0.45f * fadeAll), a(hot, 0f));
        }
        // rays round the crossing, jumping on every second tick
        int rays = ctx.seg(8, 4);
        int fr = (int) Math.floor(age / 2f);
        for (int i = 0; i < rays; i++) {
            float th = Mth.TWO_PI * (i + 0.5f) / rays + (h(seed, i, 11) - 0.5f) * 0.25f;
            float len = (3.5f + 5f * h(seed, i + 8, fr)) * S * barOpen;
            float cs = Mth.cos(th), sn = Mth.sin(th);
            float ex = cen.x + (ctx.camRight.x * cs + ctx.camUp.x * sn) * len, ey = cen.y + (ctx.camRight.y * cs + ctx.camUp.y * sn) * len, ez = cen.z + (ctx.camRight.z * cs + ctx.camUp.z * sn) * len;
            ribbon(buf, NEEDLE, VfxBlend.ADD, ex, ey, ez, cen.x, cen.y, cen.z, 0.09f * S, 0.09f * S, 1f, 0f, a(col, 0f), a(WHITE, 0.6f * fadeAll * (0.5f + 0.5f * h(seed, i, fr + 40))));
        }
        // flares at the three tips and at the head of the pillar, the big one at the crossing
        for (int sgn = -1; sgn <= 1; sgn += 2) {
            q0.set(cen.x + bxd * sgn * L, cen.y, cen.z + bzd * sgn * L);
            flare(ctx, buf, q0, 1.1f * S * barOpen * w, age * 0.05f, a(hot, 0.9f * fadeAll));
        }
        q0.set(cen.x, yc + 4.6f * S, cen.z);
        flare(ctx, buf, q0, 1.1f * S * open * w, -age * 0.05f, a(hot, 0.9f * fadeAll));
        float big = (0.6f + 0.4f * (1f - Mth.clamp(d / 30f, 0, 1))) * VfxAnim.easeOutCubic(Mth.clamp(d / 5f, 0, 1));
        flare(ctx, buf, cen, 11f * S * big, age * 0.02f, a(WHITE, 0.95f * fadeAll));
        flare(ctx, buf, cen, 7f * S * big, 0.785f + age * 0.02f, a(col, 0.8f * fadeAll));
        if (flash > 0f) {
            VfxBloom.glow(ctx, buf, cen, 10f * S * (0.4f + 0.6f * flash), WHITE, 1.1f * flash);
            buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, cen, 20f * S * flash, 0f, a(WHITE, 0.32f * flash));
        } else {
            VfxBloom.glow(ctx, buf, cen, 5f * S, col, 0.6f * fadeAll);
        }

        // the halo ring and an air shock ring round the crossing, shock rings along the ground
        ringAt(buf, faceCam, S * (0.5f + 4.9f * VfxAnim.easeOutCubic(Mth.clamp(d / 10f, 0, 1))), a(WHITE, 0.7f * fadeAll));
        float tv = d / 18f;
        if (tv < 1f) ringAt(buf, faceCam.spin(0.4f), 11f * S * VfxAnim.easeOutCubic(tv), a(col, (float) Math.pow(1f - tv, 1.3) * 0.6f));
        for (int k = 0; k < 4; k++) {
            float tt = (d - k * 3.5f) / 16f;
            if (tt <= 0f || tt >= 1f) continue;
            ringAt(buf, floor.spin(k * 0.5f), 14f * S * VfxAnim.easeOutCubic(tt), a(k == 0 ? WHITE : col, (float) Math.pow(1f - tt, 1.2) * 0.9f * (1f - 0.15f * k)));
        }
        float tw = Mth.clamp(d / 14f, 0, 1);
        buf.plane(WAVE, VfxBlend.ADD, floor, 9f * S * VfxAnim.easeOutCubic(tw) / 0.955f, a(col, 0.5f * (1f - Mth.clamp(d / 18f, 0, 1))));
        if (d >= 0.5f) buf.plane(CRACKS_GROUND, VfxBlend.ALPHA, floor.spin(h(seed, 3, 3) * Mth.TWO_PI), 9f * S * VfxAnim.easeOutCubic(Mth.clamp(d / 5f, 0, 1)), a(BLACK, 0.92f * fadeAll));
        VfxBloom.planeGlow(buf, floor, 3f * S, col, 0.5f * fadeAll);
        int sword = models ? swordOfJudgment(buf, g, S, d, p, age, fadeAll, nx, nz) : 0;

        // motes rising along the pillar
        int mN = sword > 0 ? ctx.seg(6, 3) : ctx.seg(16, 6);
        for (int i = 0; i < mN; i++) {
            float u = Mth.frac(d * 0.035f + h(seed, i, 20));
            float an = h(seed, i, 21) * Mth.TWO_PI, rr = (0.3f + 1.9f * h(seed, i, 22)) * S;
            mote(ctx, buf, g.x + Mth.cos(an + u) * rr, g.y + u * 12f * S, g.z + Mth.sin(an + u) * rr, 0.26f * S * (1f - 0.5f * u), a(WHITE, Mth.sin(u * Mth.PI) * 0.9f * fadeAll));
        }
    }
}
