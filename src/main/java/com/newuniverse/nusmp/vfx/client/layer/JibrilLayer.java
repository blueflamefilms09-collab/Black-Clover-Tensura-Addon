package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * Jibril, Lord of Rigor (Veldanava): the angel of law and command. White and warm gold light, pale sky-blue shadow, small rose-white accents; halos, law glyphs (angular rune strokes and a scales
 * star), chains of light, feathers, wings of light, a pillar from the sky. Written from scratch for this mod (textures: {@code tools/gen_jibril_vfx_textures.py} -> {@code textures/particle/jibril_*.png});
 * it shares nothing with any other mod, looks nothing like the clock of the Time Magic layer (which it never touches) and is not the cyan maze of {@link TrialLayer}. The colour of the packet is ignored: the
 * palette is the layer's own.
 * <ul>
 *   <li>JIBRIL_COMMAND: Absolute Command. Sent following the target: from = its centre, power = size (~1.5), 100 ticks. A law glyph on the floor and a halo lock down over the target (they start wide and snap shut at
 *       9 ticks), a ring of rune bars stands round it like a prison, chains of light pin the limbs, a halt glyph hangs over its head. Fades in over 6 ticks, out over 14.</li>
 *   <li>JIBRIL_JUDGMENT: Judgment of Heaven. Sent at the ground point: from = ground, to = from + (0,1,0), power = pillar radius (~2.5), 40 ticks. A glyph warns on the ground, a thin spear of light falls and a pillar of white gold
 *       light (64 blocks high) stands on it at tick 8 (flash, camera shake), halos slide down it, rings and a wave run out, feathers and sparks rise.</li>
 *   <li>JIBRIL_PURGE: Heavenly Purge. Sent at the caster: from = its centre, power = radius (50), 36 ticks. Three holy wave rings run out on the ground (the picture stops at 40 blocks, the ring far from the camera fades and is
 *       left out), a sunburst sweeps the floor, a dome ring opens, a column of light stands at the caster, motes and feathers rise.</li>
 *   <li>JIBRIL_SHACKLES: Holy Shackles. Sent following the target: from = its centre, to = the caster's chest, power = size (~1.5), 60 ticks. Chains fly from the caster to the target, three chain rings close round it with a flash,
 *       then gold sparks stream from the target to the caster.</li>
 *   <li>JIBRIL_WORD: Word of Law. Sent following the caster: from = its centre, power = reach (~1.8), 12-16 ticks. A law glyph and a rune seal turn in front of the caster (in its facing), a thin parry ring flashes out, sparks fly.</li>
 *   <li>JIBRIL_REFLECT: the parry lands. Sent at the caster: from = the caster, to = the attacker (1-48 blocks), power = 1, 14 ticks. A parry ring and glyph at the caster, three beams of reflected light run to the attacker with rings
 *       travelling along them, a glyph, a ring and sparks burst at the attacker.</li>
 *   <li>JIBRIL_KINGDOM: Kingdom of God. Sent following the caster, re-sent every 80 ticks while held: from = its centre, power = radius (50), 100 ticks. CHEAP: a faint floor seal and its rim, a seal and ring under the caster, the
 *       nearest few light curtains of the dome edge, a halo, a few motes and feathers.</li>
 *   <li>JIBRIL_WINGS: Wings of Revelation. Sent following the player, re-sent every 80 ticks while on: from = its centre, power = span scale (1.0), 100 ticks. Two pairs of feathered wings of light behind the body (they open with
 *       the first ticks and flap slowly), a halo over the head, a light trail of feathers. CHEAP.</li>
 * </ul>
 * <b>Budgets (rule 11).</b> No effect draws more than the 100 quads (400 vertices) of the vertex buffer's activation cap, KINGDOM and WINGS ask for far less through {@link #vertexBudget}. Counts below are quads at full detail
 * ({@code ctx.seg} thins them by distance and by the particle setting; minimums in the code); a glow counts as 2 quads. Worst case quads per effect per frame (measured in the preview, see tools/vfx_preview/scenes.d/jibril.json):
 * COMMAND 35, JUDGMENT 44, PURGE 29, SHACKLES 35, WORD 17, REFLECT 23, KINGDOM 24 (budget cap 30; two instances overlap for 20 ticks on a re-send: 48), WINGS 19 (budget cap 32; two instances overlap for 20 ticks: 38).
 * Decorative extras (motes, feathers, curtains, glow passes) fall away with the camera distance (the manager already drops an effect that is out of range and thins the rest by its distance level of detail), the curtains of the
 * Kingdom are only those nearest to the camera (never more than 6), a far wave ring of the Purge is skipped. No effect keeps anything between frames, so nothing has to be cleaned up on logout, death or dimension change; the
 * server sends one packet per effect (VfxSpawn) and nothing here runs on the server. A ring or plane of any radius is one quad.
 */
public class JibrilLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    public static final ResourceLocation RING = t("jibril_ring");
    public static final ResourceLocation GLYPH = t("jibril_glyph");
    public static final ResourceLocation SEAL = t("jibril_seal");
    public static final ResourceLocation HALO = t("jibril_halo");
    public static final ResourceLocation WAVE = t("jibril_wave");
    public static final ResourceLocation RAYS = t("jibril_rays");
    public static final ResourceLocation PILLAR = t("jibril_pillar");
    public static final ResourceLocation BARS = t("jibril_bars");
    public static final ResourceLocation CURTAIN = t("jibril_curtain");
    public static final ResourceLocation BEAM = t("jibril_beam");
    public static final ResourceLocation CHAIN = t("jibril_chain");
    public static final ResourceLocation CHAINRING = t("jibril_chainring");
    public static final ResourceLocation FEATHER = t("jibril_feather");
    public static final ResourceLocation WING = t("jibril_wing");
    public static final ResourceLocation FLARE = t("jibril_flare");
    public static final ResourceLocation MOTE = t("jibril_mote");

    private static final int WHITE = 0xFFFFFFFF;
    private static final int IVORY = 0xFFFFF6DC;
    private static final int GOLD = 0xFFFFD87A;
    private static final int GOLD_DK = 0xFFE0A93A;
    private static final int SKY = 0xFFBDE9FF;
    private static final int ROSE = 0xFFFFD6E0;

    /** The tick the pillar of Judgment lands (flash and shake). */
    static final float IMPACT = 8f;
    /** The tick the glyph and halo of Command snap shut. */
    static final float LOCK = 9f;
    /** How tall the pillar of Judgment is drawn (blocks). */
    static final float PILLAR_HEIGHT = 64f;
    /** The most the Purge draws its rings out to (blocks): further is beyond the draw distance. */
    static final float PURGE_VISUAL = 40f;

    /** Scratch corners of the quad being drawn (the layer draws on the render thread only, one effect at a time). */
    private final Vector3f q0 = new Vector3f(), q1 = new Vector3f(), q2 = new Vector3f(), q3 = new Vector3f();

    @Override
    public Set<VfxShape> shapes() {
        return EnumSet.of(VfxShape.JIBRIL_COMMAND, VfxShape.JIBRIL_JUDGMENT, VfxShape.JIBRIL_PURGE, VfxShape.JIBRIL_SHACKLES, VfxShape.JIBRIL_WORD, VfxShape.JIBRIL_REFLECT, VfxShape.JIBRIL_KINGDOM, VfxShape.JIBRIL_WINGS);
    }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case JIBRIL_COMMAND -> 100;
            case JIBRIL_JUDGMENT -> 40;
            case JIBRIL_PURGE -> 36;
            case JIBRIL_SHACKLES -> 60;
            case JIBRIL_WORD -> 14;
            case JIBRIL_REFLECT -> 14;
            default -> 100;
        };
    }

    @Override public int defaultColor(VfxShape s) { return GOLD; }

    /** The long-running, re-sent effects are drawn with a small cap of their own: 30 quads (Kingdom) and 32 quads (Wings); the others use the usual 100. */
    @Override
    public int vertexBudget(VfxShape s) {
        return switch (s) {
            case JIBRIL_KINGDOM -> 120;
            case JIBRIL_WINGS -> 128;
            default -> VfxVertexBuffer.MAX_VERTICES_PER_ACTIVATION;
        };
    }

    @Override
    public void onTick(VfxInstance inst) {
        if (inst.shape == VfxShape.JIBRIL_JUDGMENT && inst.age() == (int) IMPACT) VfxShake.add(inst.payload.from(), 1.8f * Mth.clamp(inst.power / 2.5f, 0.8f, 1.6f), 14);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case JIBRIL_COMMAND -> command(inst, ctx, buf);
            case JIBRIL_JUDGMENT -> judgment(inst, ctx, buf);
            case JIBRIL_PURGE -> purge(inst, ctx, buf);
            case JIBRIL_SHACKLES -> shackles(inst, ctx, buf);
            case JIBRIL_WORD -> word(inst, ctx, buf);
            case JIBRIL_REFLECT -> reflect(inst, ctx, buf);
            case JIBRIL_KINGDOM -> kingdom(inst, ctx, buf);
            case JIBRIL_WINGS -> wings(inst, ctx, buf);
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

    private static int a(int argb, float alpha) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(alpha, 0f, 1f)); }

    private static float horizontal(Vector3f v) { return Mth.sqrt(v.x * v.x + v.z * v.z); }

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
    private void ribbon(VfxVertexBuffer buf, ResourceLocation tex, float ax, float ay, float az, float bx, float by, float bz,
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
        quad(buf, tex, VfxBlend.ADD,
                ax - sx * hwA, ay - sy * hwA, az - sz * hwA, ax + sx * hwA, ay + sy * hwA, az + sz * hwA,
                bx + sx * hwB, by + sy * hwB, bz + sz * hwB, bx - sx * hwB, by - sy * hwB, bz - sz * hwB,
                0, vB, 1, vA, cA, cB);
    }

    private void flare(VfxRenderContext ctx, VfxVertexBuffer buf, float x, float y, float z, float size, float rot, int argb) {
        if (size <= 0.001f || (argb >>> 24) < 3) return;
        q0.set(x, y, z);
        buf.billboard(ctx, FLARE, VfxBlend.ADD, q0, size, rot, argb);
    }

    private void mote(VfxRenderContext ctx, VfxVertexBuffer buf, float x, float y, float z, float size, int argb) {
        if (size <= 0.001f || (argb >>> 24) < 3) return;
        q0.set(x, y, z);
        buf.billboard(ctx, MOTE, VfxBlend.ADD, q0, size, 0f, argb);
    }

    /** A feather (tip up when rot = 0), a camera-facing sprite. */
    private void feather(VfxRenderContext ctx, VfxVertexBuffer buf, float x, float y, float z, float size, float rot, int argb) {
        if (size <= 0.001f || (argb >>> 24) < 3) return;
        q0.set(x, y, z);
        buf.billboard(ctx, FEATHER, VfxBlend.ADD, q0, size, rot, argb);
    }

    private void glow(VfxRenderContext ctx, VfxVertexBuffer buf, float x, float y, float z, float size, int argb, float intensity) {
        if (intensity <= 0.01f) return;
        q0.set(x, y, z);
        VfxBloom.glow(ctx, buf, q0, size, argb, intensity);
    }

    /** The ring texture whose bright circle lies at {@code radius}. */
    private static void ringAt(VfxVertexBuffer buf, VfxPose pose, float radius, int argb) {
        if (radius <= 0.02f || (argb >>> 24) < 3) return;
        buf.plane(RING, VfxBlend.ADD, pose, radius / 0.965f, argb);
    }

    /** The halo texture whose bright ring lies at {@code radius}. */
    private static void haloAt(VfxVertexBuffer buf, VfxPose pose, float radius, int argb) {
        if (radius <= 0.02f || (argb >>> 24) < 3) return;
        buf.plane(HALO, VfxBlend.ADD, pose, radius / 0.915f, argb);
    }

    /** The holy wave texture whose bright front lies at {@code radius}. */
    private static void waveAt(VfxVertexBuffer buf, VfxPose pose, float radius, int argb) {
        if (radius <= 0.02f || (argb >>> 24) < 3) return;
        buf.plane(WAVE, VfxBlend.ADD, pose, radius / 0.955f, argb);
    }

    /** The glyph disc whose outer ring lies at {@code radius}. */
    private static void glyphAt(VfxVertexBuffer buf, VfxPose pose, float radius, int argb) {
        if (radius <= 0.02f || (argb >>> 24) < 3) return;
        buf.plane(GLYPH, VfxBlend.ADD, pose, radius / 0.985f, argb);
    }

    /** A pose standing at (x, y, z) that faces the camera (the camera is at the origin). */
    private static VfxPose faceCamera(float x, float y, float z) {
        return VfxPose.facing(new Vector3f(x, y, z), new Vector3f(-x, -y, -z));
    }

    /** A pose at (x, y, z) that lies nearly flat but is tipped towards the camera by k (0 = flat): a halo or ring over a head reads as a ring, not as an edge-on line. */
    private static VfxPose tilted(float x, float y, float z, float k) {
        float d = Mth.sqrt(x * x + z * z);
        float nx = 0f, nz = 0f;
        if (d > 1e-3f) {
            nx = -x / d * k;
            nz = -z / d * k;
        }
        return VfxPose.facing(new Vector3f(x, y, z), new Vector3f(nx, 1f, nz));
    }

    // ================================================================== JIBRIL_COMMAND
    private void command(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float fade = life(inst, age, 6, 14);
        if (fade <= 0.01f) return;
        float S = Mth.clamp(inst.power, 0.6f, 4f);
        float hs = Mth.clamp(S / 1.5f, 0.6f, 2.2f);                           // the body scale of the target
        long seed = inst.seed;
        Vector3f c = ctx.rel(inst.from(ctx));
        float near = 1f - smooth(40f, 90f, horizontal(c));
        float fy = c.y - 0.9f * hs, head = c.y + 0.95f * hs;
        float snap = 1f - Mth.clamp(age / LOCK, 0, 1);                       // 1 -> 0: the glyph and the halo close in from wide
        float pulse = 0.85f + 0.15f * Mth.sin(age * 0.2f);
        float flash = 1f - Mth.clamp((age - LOCK) / 6f, 0, 1);               // the flash of the lock

        // the floor: a law glyph with its rim and a glow, and a ring that runs out when it locks
        float R = S * 1.25f * (1f + 0.9f * snap);
        VfxPose floor = VfxPose.ground(new Vector3f(c.x, fy + 0.05f, c.z));
        glyphAt(buf, floor.spin(age * 0.03f), R, a(GOLD, 0.85f * fade * pulse));
        ringAt(buf, floor.lift(0.01f), R * 0.99f, a(WHITE, 0.7f * fade));
        VfxBloom.planeGlow(buf, floor, S * 1.3f, GOLD_DK, 0.6f * fade);
        float tt = Mth.clamp((age - LOCK) / 12f, 0, 1);
        if (age >= LOCK && tt < 1f) ringAt(buf, floor.lift(0.02f), S * (1.25f + 1.3f * VfxAnim.easeOutCubic(tt)), a(WHITE, (1f - tt) * 0.8f * fade));

        // the halo comes down over the head and locks, with a halt glyph hanging over it
        float haloY = head + (0.35f + 1.8f * snap) * hs;
        haloAt(buf, tilted(c.x, haloY, c.z, 0.75f).spin(-age * 0.05f), 0.62f * hs * (1f + 0.7f * snap), a(GOLD, 0.95f * fade));
        glow(ctx, buf, c.x, haloY, c.z, 0.9f * hs, GOLD, 0.55f * fade);
        glyphAt(buf, faceCamera(c.x, haloY + 0.95f * hs, c.z).spin(age * 0.02f), 0.55f * hs, a(WHITE, 0.85f * fade * pulse));

        // the prison: a ring of rune bars that slam down round the target and stand
        int n = ctx.seg(8, 4);
        float rise = VfxAnim.easeOutCubic(Mth.clamp(age / 7f, 0, 1));
        float hgt = 2.4f * hs * rise;
        float spin = age * 0.01f;
        float ringR = S * 0.95f;
        if (hgt > 0.05f) {
            for (int i = 0; i < n; i++) {
                float an = Mth.TWO_PI * i / n + spin;
                float x = c.x + Mth.cos(an) * ringR, z = c.z + Mth.sin(an) * ringR;
                float flick = 0.88f + 0.12f * Mth.sin(age * 0.17f + i * 1.9f);
                ribbon(buf, BARS, x, fy, z, x, fy + hgt, z, 0.20f * hs, 0.20f * hs, -age * 0.012f, -age * 0.012f + hgt / 2.1f, a(GOLD, 0.9f * fade * flick), a(WHITE, 0.95f * fade * flick));
            }
            ringAt(buf, VfxPose.ground(new Vector3f(c.x, fy + hgt, c.z)), ringR, a(WHITE, 0.55f * fade));
            ringAt(buf, VfxPose.ground(new Vector3f(c.x, fy + 0.12f, c.z)), ringR, a(GOLD, 0.45f * fade));
        }

        // chains of light pin the limbs: from the top of the bars in to the arms, legs and chest
        int nc = ctx.seg(4, 2);
        float reach = VfxAnim.easeOutCubic(Mth.clamp((age - 3f) / 8f, 0, 1));
        if (reach > 0.02f) {
            for (int k = 0; k < nc; k++) {
                float an = Mth.TWO_PI * (k + 0.5f) / nc + spin + 0.4f;
                float sx = c.x + Mth.cos(an) * ringR, sz = c.z + Mth.sin(an) * ringR, sy = fy + hgt;
                float ty = c.y + hs * (k % 4 == 0 ? 0.55f : k % 4 == 1 ? 0.0f : k % 4 == 2 ? -0.55f : 0.3f);
                float tx = c.x + Mth.cos(an) * 0.28f * hs, tz = c.z + Mth.sin(an) * 0.28f * hs;
                float ex = sx + (tx - sx) * reach, ey = sy + (ty - sy) * reach, ez = sz + (tz - sz) * reach;
                float len = Mth.sqrt((ex - sx) * (ex - sx) + (ey - sy) * (ey - sy) + (ez - sz) * (ez - sz));
                float hw = 0.11f * hs;
                ribbon(buf, CHAIN, sx, sy, sz, ex, ey, ez, hw, hw, -age * 0.02f, -age * 0.02f + len / (hw * 8f), a(WHITE, 0.95f * fade), a(WHITE, 0.95f * fade));
            }
        }

        // the lock: a flash at the head and the body, motes rising through the prison
        if (flash > 0f && age >= LOCK - 1f) {
            flare(ctx, buf, c.x, haloY, c.z, 4.5f * hs * (0.4f + 0.6f * flash), age * 0.05f, a(WHITE, flash * fade));
            glow(ctx, buf, c.x, c.y, c.z, 2.2f * hs, GOLD, flash * 0.8f * fade);
        }
        int nm = near > 0.01f ? ctx.seg(8, 3) : 0;
        for (int i = 0; i < nm; i++) {
            float u = Mth.frac(age / 44f + h(seed, i, 81));
            float an = h(seed, i, 82) * Mth.TWO_PI + u * 1.5f, rr = (0.3f + 0.65f * h(seed, i, 83)) * ringR;
            mote(ctx, buf, c.x + Mth.cos(an) * rr, fy + 0.1f + u * 2.8f * hs, c.z + Mth.sin(an) * rr, 0.22f * hs + 0.06f, a(i % 3 == 0 ? WHITE : GOLD, Mth.sin(u * Mth.PI) * 0.85f * fade * near));
        }
    }

    // ================================================================== JIBRIL_JUDGMENT
    private void judgment(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float D = inst.duration;
        float R = Mth.clamp(inst.power, 0.8f, 8f);
        float k = R / 2.5f;                                                    // the scale of the whole picture
        long seed = inst.seed;
        boolean landed = age >= IMPACT;
        float fade = landed ? Mth.clamp((D - age) / 12f, 0, 1) : Mth.clamp(age / 3f, 0, 1);
        if (fade <= 0.01f) return;
        Vector3f g = ctx.rel(inst.from(ctx));
        float near = 1f - smooth(60f, 120f, horizontal(g));
        float top = g.y + PILLAR_HEIGHT;
        float since = Math.max(0f, age - IMPACT);

        // the pillar: before the impact only a thin spear of light falls (its head runs from the sky to the ground), after it the whole column stands, wide at first, then it settles and thins at the end
        float fall = VfxAnim.easeInCubic(Mth.clamp(age / IMPACT, 0, 1));
        float yb = landed ? g.y : top - (top - g.y) * fall;
        float w = landed ? Mth.lerp(smooth(0f, 6f, since), 1.45f, 0.92f) * (1f - 0.55f * smooth(D - 14f, D, age)) : 0.20f + 0.1f * fall;
        float len = top - yb;
        float sc = -age * 0.07f, vl = len / 14f;
        ribbon(buf, PILLAR, g.x, top, g.z, g.x, yb, g.z, R * w * 1.25f, R * w * 1.25f, sc * 0.6f, sc * 0.6f + vl, a(GOLD, 0.30f * fade), a(GOLD, 0.36f * fade));
        ribbon(buf, PILLAR, g.x, top, g.z, g.x, yb, g.z, R * w * 0.66f, R * w * 0.66f, sc, sc + vl, a(IVORY, 0.65f * fade), a(WHITE, 0.80f * fade));
        ribbon(buf, PILLAR, g.x, top, g.z, g.x, yb, g.z, R * w * 0.28f, R * w * 0.28f, sc * 1.4f, sc * 1.4f + vl, a(WHITE, 0.9f * fade), a(WHITE, 1.0f * fade));
        if (!landed) {
            flare(ctx, buf, g.x, yb, g.z, 3.2f * k * (0.5f + fall), age * 0.2f, a(WHITE, fade));
            glow(ctx, buf, g.x, yb, g.z, 1.8f * k, GOLD, fade);
        }

        // the ground: the glyph that warns and then burns under the pillar, with its rim and a glow
        VfxPose floor = VfxPose.ground(new Vector3f(g.x, g.y + 0.06f, g.z));
        float gl = (landed ? 0.95f : 0.7f * smooth(0f, 6f, age)) * (landed ? fade : 1f);
        glyphAt(buf, floor.spin(age * 0.05f), R * 2.1f, a(GOLD, gl));
        ringAt(buf, floor.lift(0.01f), R * 2.1f, a(WHITE, 0.7f * gl));
        VfxBloom.planeGlow(buf, floor, R * 1.7f, GOLD, (landed ? 0.9f : 0.5f * smooth(0f, 6f, age)) * (landed ? fade : 1f));

        // halos slide down the pillar
        if (age > 2f) {
            float ha = landed ? fade : 0.35f;
            for (int i = 0; i < 3; i++) {
                float u = Mth.frac(age / 15f + i / 3f);
                float y = g.y + 0.3f + (1f - u) * 24f;
                haloAt(buf, VfxPose.ground(new Vector3f(g.x, y, g.z)).spin(age * 0.09f * (i % 2 == 0 ? 1f : -1f)), R * (1.35f - 0.25f * u), a(i == 1 ? WHITE : GOLD, Mth.sin(u * Mth.PI) * 0.85f * ha));
            }
        }

        if (landed) {
            // the impact: flash, rings and a wave running out, sparks thrown up and out
            float f = 1f - Mth.clamp(since / 7f, 0, 1);
            if (f > 0f) {
                flare(ctx, buf, g.x, g.y + 0.7f, g.z, 11f * k * (0.35f + 0.65f * f), age * 0.05f, a(WHITE, f));
                glow(ctx, buf, g.x, g.y + 0.7f, g.z, 5f * k * (0.4f + 0.6f * f), GOLD, f);
            }
            float tt = Mth.clamp(since / 16f, 0, 1);
            if (tt < 1f) {
                ringAt(buf, floor.lift(0.03f), R * (1f + 4.5f * VfxAnim.easeOutCubic(tt)), a(WHITE, (1f - tt) * 0.85f));
                waveAt(buf, floor.lift(0.04f), R * (1.2f + 9f * VfxAnim.easeOutCubic(tt)), a(GOLD, (float) Math.pow(1f - tt, 1.2) * 0.8f));
            }
            int ns = near > 0.01f ? ctx.seg(8, 4) : 0;
            for (int i = 0; i < ns; i++) {
                float q = Mth.clamp((since - h(seed, i, 7) * 3f) / 12f, 0, 1);
                if (q <= 0f || q >= 1f) continue;
                float an = Mth.TWO_PI * (i + h(seed, i, 8) * 0.6f) / ns;
                float r0 = R * 0.5f + R * (0.5f + 3.2f * VfxAnim.easeOutCubic(q)), r1 = R * 0.35f + R * 2.4f * VfxAnim.easeOutCubic(Mth.clamp(q - 0.2f, 0, 1));
                float lift = 0.2f + (1.5f + 3.5f * h(seed, i, 9)) * q;
                ribbon(buf, BEAM, g.x + Mth.cos(an) * r1, g.y + 0.2f, g.z + Mth.sin(an) * r1, g.x + Mth.cos(an) * r0, g.y + lift, g.z + Mth.sin(an) * r0, 0.04f, 0.18f * k, 0f, 0.6f, a(GOLD, 0f), a(WHITE, (1f - q) * 0.9f));
            }
            // feathers and motes rise round the base
            int nf = near > 0.01f ? ctx.seg(10, 4) : 0;
            for (int i = 0; i < nf; i++) {
                float q = (since - h(seed, i, 11) * 8f) / (18f + 10f * h(seed, i, 12));
                if (q <= 0f || q >= 1f) continue;
                float an = h(seed, i, 13) * Mth.TWO_PI + q * 0.8f, rr = R * (0.5f + 1.4f * h(seed, i, 14)) * (0.5f + q);
                float y = g.y + 0.2f + q * (3f + 6f * h(seed, i, 15));
                feather(ctx, buf, g.x + Mth.cos(an) * rr, y, g.z + Mth.sin(an) * rr, 0.55f + 0.3f * h(seed, i, 16), Mth.sin(q * 6f + i) * 0.7f, a(i % 4 == 0 ? ROSE : IVORY, Math.min(q * 6f, 1f) * (1f - smooth(0.6f, 1f, q)) * 0.95f * near));
            }
            int nm = near > 0.01f ? ctx.seg(8, 3) : 0;
            for (int i = 0; i < nm; i++) {
                float u = Mth.frac(age / 20f + h(seed, i, 21));
                float an = h(seed, i, 22) * Mth.TWO_PI + u * 2f, rr = R * (0.7f + 1.5f * h(seed, i, 23));
                mote(ctx, buf, g.x + Mth.cos(an) * rr, g.y + 0.1f + u * 9f * Mth.sqrt(k), g.z + Mth.sin(an) * rr, 0.3f, a(i % 2 == 0 ? WHITE : GOLD, Mth.sin(u * Mth.PI) * 0.85f * fade * near));
            }
        }
    }

    // ================================================================== JIBRIL_PURGE
    private void purge(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float D = inst.duration;
        float fade = life(inst, age, 2, 10);
        if (fade <= 0.01f) return;
        float Rv = Mth.clamp(inst.power, 6f, PURGE_VISUAL);                  // the picture stops at 40 blocks, the real radius is the server's
        long seed = inst.seed;
        Vector3f c = ctx.rel(inst.from(ctx));
        float dist = horizontal(c);
        float near = 1f - smooth(40f, 90f, dist);
        VfxPose floor = VfxPose.ground(new Vector3f(c.x, c.y - 0.86f, c.z));
        float run = D * 0.66f;                                                // the rings are out at two thirds of the time

        // the flash at the caster and a glyph under it
        float f0 = 1f - Mth.clamp(age / 9f, 0, 1);
        if (f0 > 0f) {
            flare(ctx, buf, c.x, c.y, c.z, 9f * (0.4f + 0.6f * f0), age * 0.04f, a(WHITE, f0));
            glow(ctx, buf, c.x, c.y, c.z, 5f * (0.4f + 0.6f * f0), GOLD, f0);
        }
        glyphAt(buf, floor.lift(0.01f).spin(age * 0.08f), 2.6f * (0.7f + 0.3f * VfxAnim.easeOutCubic(Mth.clamp(age / 6f, 0, 1))), a(GOLD, 0.85f * fade));

        // the column of light at the caster
        float col = VfxAnim.easeOutCubic(Mth.clamp(age / 6f, 0, 1));
        ribbon(buf, PILLAR, c.x, c.y - 0.86f + 16f * col, c.z, c.x, c.y - 0.86f, c.z, 1.1f, 1.6f, -age * 0.09f, -age * 0.09f + 1.1f, a(WHITE, 0f), a(GOLD, 0.5f * fade));
        ribbon(buf, PILLAR, c.x, c.y - 0.86f + 16f * col, c.z, c.x, c.y - 0.86f, c.z, 0.3f, 0.5f, -age * 0.14f, -age * 0.14f + 1.1f, a(WHITE, 0f), a(WHITE, 0.9f * fade));

        // the sunburst that sweeps the floor, as far as the first ring
        float t0 = Mth.clamp(age / run, 0, 1);
        float r0 = Rv * VfxAnim.easeOutCubic(t0);
        if (r0 > 0.5f) buf.plane(RAYS, VfxBlend.ADD, floor.lift(0.015f).spin(age * 0.14f), r0 * 0.96f, a(GOLD, 0.40f * (1f - t0) * fade));
        VfxBloom.planeGlow(buf, floor, 3.5f + Rv * 0.12f, GOLD_DK, 0.7f * (1f - Mth.clamp(age / 22f, 0, 1)));

        // the holy wave rings on the ground: a ring far from the camera is only a thin line there, it fades and is left out
        for (int k = 0; k < 3; k++) {
            float tt = Mth.clamp((age - 3f * k) / run, 0, 1);
            if (tt <= 0f || tt >= 1f) continue;
            float r = Rv * VfxAnim.easeOutCubic(tt);
            float away = Math.abs(dist - r);
            float vis = 1f - smooth(30f, 90f, away);
            float al = (float) Math.pow(1f - tt, 0.8) * (1f - 0.22f * k) * (0.12f + 0.88f * vis) * fade;
            if (al < 0.02f) continue;
            waveAt(buf, floor.lift(0.02f * (k + 1)), r, a(k == 1 ? WHITE : GOLD, 0.95f * al));
        }
        // the dome: two rings that open round the caster and thin out
        float domeR = Math.min(Rv * 0.9f, 30f);
        for (int k = 0; k < 2; k++) {
            float tt = Mth.clamp((age - 1f - 4f * k) / 18f, 0, 1);
            if (tt <= 0f || tt >= 1f) continue;
            waveAt(buf, faceCamera(c.x, c.y, c.z).spin(k * 0.7f), domeR * VfxAnim.easeOutCubic(tt), a(k == 0 ? WHITE : SKY, (float) Math.pow(1f - tt, 1.2) * 0.7f * fade));
        }

        // feathers and motes rise round the caster
        int nf = near > 0.01f ? ctx.seg(6, 3) : 0;
        for (int i = 0; i < nf; i++) {
            float q = Mth.frac(age / 30f + h(seed, i, 31));
            float an = h(seed, i, 32) * Mth.TWO_PI + q, rr = (1.5f + 9f * h(seed, i, 33)) * (0.3f + 0.7f * VfxAnim.easeOutCubic(Mth.clamp(age / 18f, 0, 1)));
            feather(ctx, buf, c.x + Mth.cos(an) * rr, c.y - 0.8f + q * 6f, c.z + Mth.sin(an) * rr, 0.6f, Mth.sin(q * 7f + i) * 0.6f, a(i % 3 == 0 ? ROSE : IVORY, Mth.sin(q * Mth.PI) * 0.9f * fade * near));
        }
        int nm = near > 0.01f ? ctx.seg(10, 4) : 0;
        for (int i = 0; i < nm; i++) {
            float u = Mth.frac(age / 22f + h(seed, i, 41));
            float an = h(seed, i, 42) * Mth.TWO_PI, rr = (1f + 11f * h(seed, i, 43)) * (0.3f + 0.7f * VfxAnim.easeOutCubic(Mth.clamp(age / 16f, 0, 1)));
            mote(ctx, buf, c.x + Mth.cos(an) * rr, c.y - 0.8f + u * 7f, c.z + Mth.sin(an) * rr, 0.3f, a(i % 2 == 0 ? WHITE : GOLD, Mth.sin(u * Mth.PI) * 0.85f * fade * near));
        }
    }

    // ================================================================== JIBRIL_SHACKLES
    private void shackles(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float fade = life(inst, age, 3, 12);
        if (fade <= 0.01f) return;
        float S = Mth.clamp(inst.power, 0.5f, 4f);
        float hs = Mth.clamp(S / 1.5f, 0.6f, 2.2f);
        long seed = inst.seed;
        Vector3f c = ctx.rel(inst.from(ctx)), B = ctx.rel(inst.to(ctx));
        float dx = c.x - B.x, dy = c.y - B.y, dz = c.z - B.z;                // from the caster to the target
        float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        float near = 1f - smooth(40f, 90f, horizontal(c));
        float fy = c.y - 0.9f * hs;
        float lock = Mth.clamp((age - 8f) / 8f, 0, 1);                        // the chain rings close from 8 to 16
        float flash = 1f - Mth.clamp((age - 16f) / 6f, 0, 1);

        // the floor under the target: a glyph and its rim
        VfxPose floor = VfxPose.ground(new Vector3f(c.x, fy + 0.05f, c.z));
        float gs = S * 0.95f * (1f + 0.8f * (1f - VfxAnim.easeOutCubic(Mth.clamp(age / 10f, 0, 1))));
        glyphAt(buf, floor.spin(-age * 0.04f), gs, a(GOLD, 0.8f * fade));
        ringAt(buf, floor.lift(0.01f), gs * 0.99f, a(WHITE, 0.6f * fade));

        // the chains: three fly from the caster to the target along arcs, then fade down to a faint link
        if (len > 0.5f) {
            float hl = Mth.sqrt(dx * dx + dz * dz);
            float sx = hl > 1e-3f ? -dz / hl : 1f, sz = hl > 1e-3f ? dx / hl : 0f;
            float head = VfxAnim.easeOutCubic(Mth.clamp(age / 12f, 0, 1));
            float chainA = (1f - 0.65f * smooth(14f, 26f, age)) * fade;
            int nc = ctx.seg(3, 2);
            float hw = 0.2f;
            for (int k = 0; k < nc; k++) {
                float off = (k - (nc - 1) * 0.5f) * 1.2f;
                float lift = k % 2 == 0 ? 0.9f : -0.25f;
                float px = B.x, py = B.y, pz = B.z;
                for (int m = 1; m <= 3; m++) {
                    float f = m / 3f;
                    if ((m - 1) / 3f >= head) break;
                    float fe = Math.min(f, head);
                    float arc = Mth.sin(fe * Mth.PI);
                    float qx = B.x + dx * fe + sx * off * arc, qy = B.y + dy * fe + lift * arc, qz = B.z + dz * fe + sz * off * arc;
                    float vA = (m - 1) / 3f * len / (hw * 8f), vB = fe * len / (hw * 8f);
                    ribbon(buf, CHAIN, px, py, pz, qx, qy, qz, hw, hw, vA - age * 0.05f, vB - age * 0.05f, a(WHITE, chainA), a(WHITE, chainA));
                    px = qx;
                    py = qy;
                    pz = qz;
                }
            }
        }

        // three chain rings wrap round the target and snap shut with a flash
        float close = VfxAnim.easeOutBack(lock);
        float ringA = smooth(7f, 11f, age) * fade;
        if (ringA > 0.01f) {
            for (int k = 0; k < 3; k++) {
                float y = c.y + hs * (k - 1) * 0.7f;
                float r = 1.0f * hs * (1f + 1.1f * (1f - close));
                buf.plane(CHAINRING, VfxBlend.ADD, tilted(c.x, y, c.z, 0.55f).spin(age * 0.05f * (k % 2 == 0 ? 1f : -1f)), r / 0.9f, a(WHITE, ringA));
            }
        }
        if (flash > 0f && age >= 15f) {
            flare(ctx, buf, c.x, c.y + 0.2f * hs, c.z, 4.2f * hs * (0.4f + 0.6f * flash), age * 0.06f, a(WHITE, flash * fade));
            glow(ctx, buf, c.x, c.y, c.z, 2.2f * hs, GOLD, flash * 0.9f * fade);
        }

        // the stolen buffs: gold sparks and a thin stream flow from the target to the caster's chest
        float streamA = smooth(14f, 22f, age) * fade;
        if (streamA > 0.01f && len > 0.5f) {
            ribbon(buf, BEAM, c.x, c.y, c.z, B.x, B.y, B.z, 0.10f, 0.05f, -age * 0.12f, -age * 0.12f + len / 6f, a(GOLD, 0.30f * streamA), a(WHITE, 0.45f * streamA));
            int ns = ctx.seg(8, 3);
            for (int i = 0; i < ns; i++) {
                float f = Mth.frac(age / 20f + h(seed, i, 51));
                float arc = Mth.sin(f * Mth.PI);
                float off = (h(seed, i, 52) - 0.5f) * 0.9f * (1f - 0.4f * f);
                mote(ctx, buf, c.x - dx * f + off * 0.6f, c.y - dy * f + arc * 0.6f + off * 0.4f, c.z - dz * f + off * 0.6f, 0.26f + 0.1f * arc, a(i % 3 == 0 ? WHITE : GOLD, Mth.sin(f * Mth.PI) * 0.95f * streamA));
            }
            float pulseB = Mth.frac(age / 20f);
            glow(ctx, buf, B.x, B.y, B.z, 0.9f + 0.7f * (1f - pulseB), GOLD, 0.55f * streamA * pulseB);
            flare(ctx, buf, B.x, B.y, B.z, 1.6f * pulseB, age * 0.1f, a(WHITE, 0.8f * streamA * pulseB));
        }
        int nm = near > 0.01f ? ctx.seg(6, 2) : 0;
        for (int i = 0; i < nm; i++) {
            float u = Mth.frac(age / 40f + h(seed, i, 61));
            float an = h(seed, i, 62) * Mth.TWO_PI + u * 1.6f, rr = (0.4f + 0.6f * h(seed, i, 63)) * S * 0.9f;
            mote(ctx, buf, c.x + Mth.cos(an) * rr, fy + 0.1f + u * 2.4f * hs, c.z + Mth.sin(an) * rr, 0.2f * hs + 0.05f, a(i % 2 == 0 ? WHITE : GOLD, Mth.sin(u * Mth.PI) * 0.8f * fade * near));
        }
    }

    // ================================================================== JIBRIL_WORD
    private void word(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float fade = life(inst, age, 1.5f, 5f);
        if (fade <= 0.01f) return;
        float reach = Mth.clamp(inst.power, 0.8f, 4f);
        float sc = Mth.clamp(reach / 1.8f, 0.6f, 2f);
        long seed = inst.seed;
        Vector3f c = ctx.rel(inst.from(ctx));
        // the way the caster faces: a followed living entity gives its body yaw, otherwise the glyph turns to the camera
        float yaw = inst.followYaw(ctx);
        float fx, fz;
        if (Float.isNaN(yaw)) {
            float hl = horizontal(c);
            fx = hl > 1e-3f ? -c.x / hl : 0f;
            fz = hl > 1e-3f ? -c.z / hl : 1f;
        } else {
            fx = -Mth.sin(yaw * Mth.DEG_TO_RAD);
            fz = Mth.cos(yaw * Mth.DEG_TO_RAD);
        }
        float ox = c.x + fx * reach, oy = c.y + 0.1f, oz = c.z + fz * reach;
        VfxPose face = VfxPose.facing(new Vector3f(ox, oy, oz), new Vector3f(fx, 0f, fz));
        float pop = VfxAnim.easeOutBack(Mth.clamp(age / 3f, 0, 1));
        float R = 0.95f * sc * pop;
        float pulse = 0.85f + 0.15f * Mth.sin(age * 0.6f);

        glyphAt(buf, face.spin(age * 0.16f), R, a(GOLD, 0.95f * fade * pulse));
        buf.plane(SEAL, VfxBlend.ADD, face.lift(0.02f).spin(-age * 0.12f + 1f), R * 1.28f / 0.985f, a(WHITE, 0.55f * fade));
        // the parry edge: a thin bright ring that flashes outwards in the first ticks
        float tt = Mth.clamp(age / 6f, 0, 1);
        if (tt < 1f) {
            ringAt(buf, face.lift(0.04f), sc * (0.5f + 1.6f * VfxAnim.easeOutCubic(tt)), a(WHITE, (1f - tt) * 0.95f));
            ringAt(buf, face.lift(0.05f), sc * (0.3f + 1.0f * VfxAnim.easeOutCubic(Mth.clamp(age / 4f, 0, 1))), a(GOLD, (1f - Mth.clamp(age / 4f, 0, 1)) * 0.8f));
        }
        float f0 = 1f - Mth.clamp(age / 5f, 0, 1);
        if (f0 > 0f) {
            flare(ctx, buf, ox, oy, oz, 4.5f * sc * (0.4f + 0.6f * f0), age * 0.2f, a(WHITE, f0));
            glow(ctx, buf, ox, oy, oz, 2.2f * sc * (0.5f + 0.5f * f0), GOLD, 0.9f * f0 * fade);
        }
        // sparks fly out in the plane of the glyph, runes (motes) orbit its edge
        int nsp = ctx.seg(6, 3);
        for (int i = 0; i < nsp; i++) {
            float q = Mth.clamp((age - h(seed, i, 1) * 2f) / 8f, 0, 1);
            if (q <= 0f || q >= 1f) continue;
            float an = Mth.TWO_PI * (i + h(seed, i, 2) * 0.5f) / nsp;
            float ca = Mth.cos(an), sa = Mth.sin(an);
            float rx = face.right().x * ca + face.up().x * sa, ry = face.right().y * ca + face.up().y * sa, rz = face.right().z * ca + face.up().z * sa;
            float r0 = R * (0.9f + 0.9f * q), r1 = R * (0.8f + 1.9f * q);
            ribbon(buf, BEAM, ox + rx * r1, oy + ry * r1, oz + rz * r1, ox + rx * r0, oy + ry * r0, oz + rz * r0, 0.03f, 0.10f * sc, 0f, 0.5f, a(GOLD, 0f), a(WHITE, (1f - q) * 0.9f * fade));
        }
        int nm = ctx.seg(4, 2);
        for (int i = 0; i < nm; i++) {
            float an = Mth.TWO_PI * i / nm + age * 0.3f;
            float ca = Mth.cos(an), sa = Mth.sin(an);
            float rr = R * 1.08f;
            mote(ctx, buf, ox + (face.right().x * ca + face.up().x * sa) * rr, oy + (face.right().y * ca + face.up().y * sa) * rr, oz + (face.right().z * ca + face.up().z * sa) * rr, 0.3f * sc, a(i % 2 == 0 ? WHITE : GOLD, 0.9f * fade));
        }
    }

    // ================================================================== JIBRIL_REFLECT
    private void reflect(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float D = inst.duration;
        float fade = Mth.clamp((D - age) / 5f, 0, 1);
        if (fade <= 0.01f) return;
        long seed = inst.seed;
        Vector3f A = ctx.rel(inst.from(ctx)), B = ctx.rel(inst.to(ctx));
        float dx = B.x - A.x, dy = B.y - A.y, dz = B.z - A.z;
        float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.5f) return;
        float ux = dx / len, uy = dy / len, uz = dz / len;

        // the parry at the caster: a ring and a glyph that face the attacker, a flash
        VfxPose face = VfxPose.facing(new Vector3f(A.x + ux * 0.9f, A.y + uy * 0.9f, A.z + uz * 0.9f), new Vector3f(ux, uy, uz));
        float tp = Mth.clamp(age / 6f, 0, 1);
        if (tp < 1f) ringAt(buf, face, 0.5f + 2.4f * VfxAnim.easeOutCubic(tp), a(WHITE, (1f - tp) * 0.95f));
        glyphAt(buf, face.lift(0.4f).spin(age * 0.2f), 1.15f, a(GOLD, 0.9f * (1f - smooth(0.3f, 0.85f, age / D))));
        float f0 = 1f - Mth.clamp(age / 5f, 0, 1);
        if (f0 > 0f) {
            flare(ctx, buf, A.x + ux * 0.9f, A.y + uy * 0.9f, A.z + uz * 0.9f, 6f * (0.4f + 0.6f * f0), age * 0.15f, a(WHITE, f0));
            glow(ctx, buf, A.x, A.y, A.z, 3f * (0.5f + 0.5f * f0), GOLD, 0.9f * f0);
        }

        // the beams: the head runs to the attacker in a few ticks, the tail leaves the caster after it
        float head = VfxAnim.easeOutCubic(Mth.clamp((age - 1f) / 6f, 0, 1));
        float tail = Mth.clamp((age - 4f) / 8f, 0, 1);
        if (head > 0.01f) {
            float hl = Mth.sqrt(dx * dx + dz * dz);
            float sx = hl > 1e-3f ? -dz / hl : 1f, sz = hl > 1e-3f ? dx / hl : 0f;
            float ax0 = A.x + ux * 0.9f, ay0 = A.y + uy * 0.9f, az0 = A.z + uz * 0.9f;
            for (int k = 0; k < 3; k++) {
                float off = k == 0 ? 0f : (k == 1 ? 0.55f : -0.55f);
                float hw = k == 0 ? 0.30f : 0.15f;
                float sxk = ax0 + sx * off, szk = az0 + sz * off, syk = ay0 + (k == 0 ? 0f : 0.12f * off);
                float ex = B.x - sxk, ey = B.y - syk, ez = B.z - szk;
                float s0 = Math.min(tail, head - 0.02f), s1 = head;
                float vA = -age * 0.5f + s0 * len / 6f, vB = -age * 0.5f + s1 * len / 6f;
                ribbon(buf, BEAM, sxk + ex * s0, syk + ey * s0, szk + ez * s0, sxk + ex * s1, syk + ey * s1, szk + ez * s1, hw, hw * 0.7f, vA, vB, a(k == 0 ? WHITE : GOLD, 0.95f * fade), a(k == 0 ? WHITE : IVORY, fade));
                if (k == 0) ribbon(buf, BEAM, sxk + ex * s0, syk + ey * s0, szk + ez * s0, sxk + ex * s1, syk + ey * s1, szk + ez * s1, 0.75f, 0.5f, vA * 0.4f, vB * 0.4f, a(GOLD, 0.40f * fade), a(GOLD_DK, 0.30f * fade));
            }
            // two rings travel along the beam
            for (int k = 0; k < 2; k++) {
                float f = head - 0.22f * k;
                if (f <= 0.03f || f >= 0.98f) continue;
                VfxPose p = VfxPose.facing(new Vector3f(A.x + dx * f, A.y + dy * f, A.z + dz * f), new Vector3f(ux, uy, uz));
                ringAt(buf, p, 0.7f + 0.2f * k, a(k == 0 ? WHITE : GOLD, 0.85f * fade));
            }
        }

        // the burst at the attacker: a glyph, a ring, a flash and sparks
        float since = age - 7f;
        if (since >= 0f) {
            float tt = Mth.clamp(since / 7f, 0, 1);
            VfxPose iface = faceCamera(B.x, B.y, B.z);
            glyphAt(buf, iface.spin(age * 0.25f), 1.3f * (0.6f + 0.4f * VfxAnim.easeOutCubic(tt)), a(GOLD, (1f - tt) * 0.95f));
            ringAt(buf, iface.lift(0.02f), 0.5f + 2.6f * VfxAnim.easeOutCubic(tt), a(WHITE, (1f - tt) * 0.9f));
            float f1 = 1f - tt;
            flare(ctx, buf, B.x, B.y, B.z, 6.5f * (0.4f + 0.6f * f1), -age * 0.1f, a(WHITE, f1));
            glow(ctx, buf, B.x, B.y, B.z, 3.2f * (0.5f + 0.5f * f1), GOLD, f1);
            int ns = ctx.seg(8, 3);
            for (int i = 0; i < ns; i++) {
                float q = Mth.clamp((since - h(seed, i, 3) * 1.5f) / 6f, 0, 1);
                if (q <= 0f || q >= 1f) continue;
                float th = h(seed, i, 4) * Mth.TWO_PI, ph = (h(seed, i, 5) - 0.5f) * Mth.PI;
                float dd = (0.8f + 2.6f * h(seed, i, 6)) * VfxAnim.easeOutCubic(q);
                mote(ctx, buf, B.x + Mth.cos(th) * Mth.cos(ph) * dd, B.y + Mth.sin(ph) * dd, B.z + Mth.sin(th) * Mth.cos(ph) * dd, 0.32f, a(i % 2 == 0 ? WHITE : GOLD, (1f - q) * 0.95f));
            }
        }
    }

    // ================================================================== JIBRIL_KINGDOM
    private void kingdom(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float fade = life(inst, age, 12, 12);
        if (fade <= 0.01f) return;
        float R = Mth.clamp(inst.power, 6f, 80f);
        long seed = inst.seed;
        Vector3f c = ctx.rel(inst.from(ctx));
        float dist = horizontal(c);
        float fy = c.y - 0.88f;
        float pulse = 0.85f + 0.15f * Mth.sin(age * 0.07f);
        float nearC = 1f - smooth(40f, 90f, dist);                             // the caster and its little things are only drawn near the camera

        // the floor seal and its rim: two quads of any radius, faint (the sanctuary), turning very slowly
        VfxPose floor = VfxPose.ground(new Vector3f(c.x, fy + 0.04f, c.z));
        buf.plane(SEAL, VfxBlend.ADD, floor.spin(age * 0.0015f), R / 0.985f, a(GOLD, 0.20f * fade));
        ringAt(buf, floor.lift(0.01f), R, a(WHITE, 0.45f * fade * pulse));
        ringAt(buf, floor.lift(0.012f), R * 0.965f, a(GOLD, 0.22f * fade));
        if (nearC > 0.01f) {
            glyphAt(buf, floor.lift(0.02f).spin(age * 0.01f), 3.2f, a(GOLD, 0.6f * fade * nearC));
            ringAt(buf, floor.lift(0.03f), 3.1f, a(WHITE, 0.5f * fade * nearC));
        }

        // the light curtains of the dome's edge: only the few nearest to the camera, never more than six, each fades with its distance
        float edge = Math.abs(dist - R);
        if (edge < 80f) {
            final int N = 24;
            float step = Mth.TWO_PI / N;
            // the curtain the camera looks at: where its horizontal look ray (right x up = back, so forward = up x right) meets the dome's edge; straight up or down: the nearest point to the camera
            float lx = ctx.camUp.y * ctx.camRight.z - ctx.camUp.z * ctx.camRight.y, lz = ctx.camUp.x * ctx.camRight.y - ctx.camUp.y * ctx.camRight.x;
            float ll = Mth.sqrt(lx * lx + lz * lz);
            float camX = -c.x, camZ = -c.z;                                     // the camera, seen from the centre of the dome
            float hitX = camX, hitZ = camZ;
            if (ll > 0.2f) {
                lx /= ll;
                lz /= ll;
                float b = camX * lx + camZ * lz;
                float disc = b * b - (camX * camX + camZ * camZ - R * R);
                if (disc >= 0f) {
                    float tHit = -b + Mth.sqrt(disc);                          // the far root: from inside the dome it is the edge in front
                    if (dist > R) tHit = -b - Mth.sqrt(disc);                  // from outside: the near edge
                    if (tHit > 0f) {
                        hitX = camX + lx * tHit;
                        hitZ = camZ + lz * tHit;
                    }
                }
            }
            float camAn = (float) Math.atan2(hitZ, hitX);
            int i0 = Math.round(camAn / step);
            int K = ctx.seg(6, 2);
            float arcHalf = step * 0.5f * 0.92f;
            float spin = age * 0.0006f;
            for (int k = 0; k < K; k++) {
                int i = i0 + (((k + 1) >> 1) * ((k & 1) == 1 ? 1 : -1));
                float an = step * i + spin;
                float mx = c.x + Mth.cos(an) * R, mz = c.z + Mth.sin(an) * R;
                float cd = Mth.sqrt(mx * mx + mz * mz);
                float da = (0.5f + 0.2f * Mth.sin(age * 0.06f + i * 1.7f)) * fade * (1f - smooth(35f, 100f, cd));
                if (da < 0.02f) continue;
                float hgt = 15f + 4f * Mth.sin(age * 0.035f + i * 2.1f);
                float ax = c.x + Mth.cos(an - arcHalf) * R, az = c.z + Mth.sin(an - arcHalf) * R;
                float bx = c.x + Mth.cos(an + arcHalf) * R, bz = c.z + Mth.sin(an + arcHalf) * R;
                quad(buf, CURTAIN, VfxBlend.ADD, ax, fy, az, bx, fy, bz, bx, fy + hgt, bz, ax, fy + hgt, az, 0f, 0f, 1f, 1f, a(GOLD, da), a(WHITE, da * 0.35f));
            }
        }

        // the caster: a halo over the head, a few motes rising and feathers drifting down
        if (nearC > 0.01f) {
            float hy = c.y + 1.2f;
            haloAt(buf, tilted(c.x, hy, c.z, 0.75f).spin(age * 0.04f), 0.5f, a(GOLD, 0.9f * fade * nearC));
            glow(ctx, buf, c.x, hy, c.z, 0.9f, GOLD, 0.45f * fade * nearC);
            int nm = ctx.seg(6, 2);
            for (int i = 0; i < nm; i++) {
                float u = Mth.frac(age / 70f + h(seed, i, 11));
                float an = h(seed, i, 12) * Mth.TWO_PI + u * 1.2f, rr = (0.8f + 3.2f * h(seed, i, 13));
                mote(ctx, buf, c.x + Mth.cos(an) * rr, fy + 0.2f + u * 5.5f, c.z + Mth.sin(an) * rr, 0.26f, a(i % 3 == 0 ? WHITE : GOLD, Mth.sin(u * Mth.PI) * 0.8f * fade * nearC));
            }
            int nf = ctx.seg(4, 2);
            for (int i = 0; i < nf; i++) {
                float u = Mth.frac(age / 90f + h(seed, i, 21));
                float an = h(seed, i, 22) * Mth.TWO_PI + u * 0.9f, rr = 1.2f + 3.5f * h(seed, i, 23);
                feather(ctx, buf, c.x + Mth.cos(an) * rr, c.y + 3.5f - u * 4.3f, c.z + Mth.sin(an) * rr, 0.5f, Mth.sin(u * 9f + i) * 0.8f, a(IVORY, Mth.sin(u * Mth.PI) * 0.7f * fade * nearC));
            }
        }
    }

    // ================================================================== JIBRIL_WINGS
    /**
     * One wing quad: the picture (root at its left edge, 58 % of the way down; the leading edge on top) is laid in the plane of the axis d (root to tip) and q (up the picture). Mirrored for the left wing by swapping U.
     */
    private void wingQuad(VfxVertexBuffer buf, VfxBlend blend, float px, float py, float pz, float dx, float dy, float dz, float qx, float qy, float qz, float L, float Hh, boolean mirror, int argb) {
        float rootUp = 0.58f * Hh, rootDown = 0.42f * Hh;
        float blx = px - qx * rootDown, bly = py - qy * rootDown, blz = pz - qz * rootDown;
        float tlx = px + qx * rootUp, tly = py + qy * rootUp, tlz = pz + qz * rootUp;
        float u0 = mirror ? 1f : 0f, u1 = mirror ? 0f : 1f;
        quad(buf, WING, blend,
                blx, bly, blz, blx + dx * L, bly + dy * L, blz + dz * L,
                tlx + dx * L, tly + dy * L, tlz + dz * L, tlx, tly, tlz,
                u0, 0f, u1, 1f, argb, argb);
    }

    private void wings(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float fade = life(inst, age, 8, 12);
        if (fade <= 0.01f) return;
        float P = Mth.clamp(inst.power, 0.4f, 2.5f);
        long seed = inst.seed;
        Vector3f c = ctx.rel(inst.from(ctx));
        float dist = horizontal(c);
        float near = 1f - smooth(40f, 90f, dist);
        // the way the body faces (the wings are behind it); a followed living entity gives its body yaw, otherwise the wings point back along -z
        float yaw = inst.followYaw(ctx);
        if (Float.isNaN(yaw)) yaw = 0f;
        float fx = -Mth.sin(yaw * Mth.DEG_TO_RAD), fz = Mth.cos(yaw * Mth.DEG_TO_RAD);
        float sx = -fz, sz = fx;                                              // to the player's left or right (the wings are mirrored, so which does not matter)
        float open = VfxAnim.easeOutBack(Mth.clamp(age / 10f, 0, 1));
        float flap = Mth.sin(age * 0.15f) * 0.10f + Mth.sin(age * 0.37f) * 0.025f;
        float al = 0.92f * fade;

        // two pairs: the big upper one raised, the smaller lower one hanging; each wing is one quad (alpha blended, so the layers of feathers read), a soft gold glow sits behind the back
        for (int pair = 0; pair < 2; pair++) {
            if (pair == 1 && dist > 110f) break;
            float L = (pair == 0 ? 3.0f : 2.0f) * P;
            float phi = Mth.lerp(open, 1.35f, pair == 0 ? 0.50f + flap : -0.12f - flap * 0.8f);
            float cp = Mth.cos(phi), sp = Mth.sin(phi);
            float py = c.y + (pair == 0 ? 0.32f : 0.10f), back = pair == 0 ? 0.20f : 0.28f;
            float px0 = c.x - fx * back, pz0 = c.z - fz * back;
            for (int side = -1; side <= 1; side += 2) {
                float dx = sx * side * cp - fx * 0.22f, dy = sp, dz = sz * side * cp - fz * 0.22f;
                float dl = Mth.sqrt(dx * dx + dy * dy + dz * dz);
                dx /= dl;
                dy /= dl;
                dz /= dl;
                // q: up the picture = the vertical, made perpendicular to d
                float dq = dy;
                float qx = -dx * dq, qy = 1f - dy * dq, qz = -dz * dq;
                float ql = Mth.sqrt(qx * qx + qy * qy + qz * qz);
                qx /= ql;
                qy /= ql;
                qz /= ql;
                float pa = pair == 0 ? al : al * 0.72f;
                wingQuad(buf, VfxBlend.ALPHA, px0, py, pz0, dx, dy, dz, qx, qy, qz, L, L * 0.5f, side > 0, a(WHITE, pa));
            }
        }
        // the halo over the head and a soft glow at the back
        float hy = c.y + 1.2f;
        haloAt(buf, tilted(c.x, hy, c.z, 0.75f).spin(age * 0.05f), 0.5f * Math.max(P, 0.8f), a(GOLD, 0.95f * fade));
        if (near > 0.01f) {
            glow(ctx, buf, c.x - fx * 0.3f, c.y + 0.4f, c.z - fz * 0.3f, 1.6f * P, GOLD, 0.35f * fade * near);
            // loose feathers fall from the wings and drift back, a few sparkles at the tips
            int nf = ctx.seg(6, 3);
            for (int i = 0; i < nf; i++) {
                float u = Mth.frac(age / 38f + h(seed, i, 31));
                float side = (i & 1) == 0 ? 1f : -1f;
                float span = (0.8f + 2.0f * h(seed, i, 32)) * P;
                float bx = c.x + sx * side * span - fx * (0.4f + 1.4f * u) + Mth.sin(u * 7f + i) * 0.25f;
                float bz = c.z + sz * side * span - fz * (0.4f + 1.4f * u) + Mth.cos(u * 6f + i) * 0.25f;
                feather(ctx, buf, bx, c.y + 1.0f - u * 2.6f, bz, 0.42f * Math.max(P, 0.8f), Mth.sin(u * 8f + i * 1.3f) * 0.9f, a(i % 4 == 0 ? ROSE : IVORY, Mth.sin(u * Mth.PI) * 0.85f * fade * near));
            }
            int nm = ctx.seg(4, 2);
            for (int i = 0; i < nm; i++) {
                float u = Mth.frac(age / 30f + h(seed, i, 41));
                float side = (i & 1) == 0 ? 1f : -1f;
                mote(ctx, buf, c.x + sx * side * (2.2f + 1.0f * h(seed, i, 42)) * P - fx * 0.5f, c.y + 1.6f * P - u * 1.4f, c.z + sz * side * (2.2f + 1.0f * h(seed, i, 42)) * P - fz * 0.5f, 0.28f, a(i % 2 == 0 ? WHITE : GOLD, Mth.sin(u * Mth.PI) * 0.8f * fade * near));
            }
        }
    }
}
