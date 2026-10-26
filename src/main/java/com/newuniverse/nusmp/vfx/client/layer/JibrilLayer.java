package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * Jibril, Lord of Rigor (Veldanava): the angel of law and command, drawn as DAWN / LAW OF THE SKY (3.16 look, replacing the 3.15 white-gold one). A pearl-white core, rose-gold and amber warmth, an azure-to-lavender
 * cool shadow and an IRIDESCENT prismatic rim (a soft pink -> gold -> aqua -> violet gradient on edges, halos and feathers, like light through an angel's wing). Motifs: layered feather wings, nested halos wreathed in tiny
 * feathers, flowing ribbons and banners of light, unrolling law scrolls with calligraphic script, an open book under a crown of rays (the emblem), braided ribbons of light with feather-shaped links, a pillar with a spiral
 * braid and falling petals. It shares nothing with the Sandalphon set of {@code JudgmentLayer} (cold white-gold and steel, black cracks, clock rings, hexagrams, needles, a scales-and-sword emblem), is not the cyan maze
 * of {@link TrialLayer} and never touches the Time Magic layer. Written from scratch for this mod (textures: {@code tools/gen_jibril_vfx_textures.py} -> {@code textures/particle/jibril_*.png}, colour baked in, so the
 * vertex colour is only a tint and an alpha). The colour of the packet is ignored.
 * <ul>
 *   <li>JIBRIL_COMMAND: Absolute Command. Sent following the target: from = its centre, power = size (~1.5), 100 ticks. Anticipation: a law glyph and a halo descend wide and spin in; lock at 9 ticks: flash, shock rings,
 *       law banners slam down round the target and braided chains pin the limbs; held: banners, dawn curtains and spiral ribbons stand, law scrolls orbit inside, a nested halo with orbiting feathers and the book emblem
 *       hang over the head, petals fall and motes rise; the last 14 ticks the banners lift away. Fades in over 6 ticks, out over 14.</li>
 *   <li>JIBRIL_JUDGMENT: Judgment of Heaven. Sent at the ground point: from = ground, to = from + (0,1,0), power = pillar radius (~2.5), 40 ticks. Anticipation (0-8): the floor seal draws in, halos fall down the sky and a
 *       thin braided spear of light drops; strike at tick 8 (flash, camera shake): the braided pillar (64 blocks high) stands with two spiral ribbons round it, wings of light open at its foot, rings and a wave run out;
 *       resolve: halos slide down, petals fall from the sky in a staggered rain, sparks, feathers and motes rise; afterglow: the pillar thins and the seal burns down.</li>
 *   <li>JIBRIL_PURGE: Heavenly Purge. Sent at the caster: from = its centre, power = radius (50), 36 ticks. Anticipation: light streams in to the caster and a ring closes; release: flash, a spiral of ribbons and a sunburst
 *       sweep the floor, four holy wave rings and flowing ribbons run out (the picture stops at 40 blocks, a ring far from the camera fades and is left out), a dome of three rings opens, a column of light stands; afterglow:
 *       a halo glows on the floor while petals, feathers and motes fall and rise.</li>
 *   <li>JIBRIL_SHACKLES: Holy Shackles. Sent following the target: from = its centre, to = the caster's chest, power = size (~1.5), 60 ticks. Braided chains of feather links fly from the caster to the target, three rings of
 *       feather links close round it with a flash and shock rings, law scrolls orbit it, then ribbons and sparks stream from the target to the caster while petals fall.</li>
 *   <li>JIBRIL_WORD: Word of Law. Sent following the caster: from = its centre, power = reach (~1.8), 12-16 ticks. A converging ring, then a law scroll unrolls over a law glyph and a seal in front of the caster (in its facing), a
 *       small pair of wings opens, rings and a wave flash out, script ribbons orbit, sparks, petals and feathers burst.</li>
 *   <li>JIBRIL_REFLECT: the parry lands. Sent at the caster: from = the caster, to = the attacker (1-48 blocks), power = 1, 14 ticks. A parry ring, glyph and emblem at the caster, three braided beams with soft ribbons
 *       run to the attacker with rings, feathers and petals travelling along them, a glyph, seal, rings, a wave, rays, sparks and petals burst at the attacker.</li>
 *   <li>JIBRIL_KINGDOM: Kingdom of God. Sent following the caster, re-sent every 80 ticks while held: from = its centre, power = radius (50), 100 ticks. A rose-window seal and a ribbon spiral on the floor, its rim, the
 *       nearest few dawn curtains of the dome edge, nested halos, the emblem and orbiting feathers over the caster, ribbons, petals and motes.</li>
 *   <li>JIBRIL_WINGS: Wings of Revelation. Sent following the player, re-sent every 80 ticks while on: from = its centre, power = span scale (1.0), 100 ticks. Two pairs of layered feather wings of light behind the body
 *       (they open with the first ticks and flap slowly) with a prismatic rim light, a backlight, nested halos with orbiting feathers, streamers drifting from the wing tips, falling feathers and petals.</li>
 * </ul>
 * <b>Budgets (rule 11).</b> The engine hard-caps every effect at 100 quads (400 vertices) per activation; this layer does not draw "5x the quads", it puts more detail into the textures (up to 1024 px) and uses the budget
 * where it earns it. Counts below are quads at full detail ({@code ctx.seg} thins them by distance and by the particle setting; minimums in the code); a glow counts as 2 quads, a ring or plane of any radius is one quad.
 * Worst case quads per effect per frame (measured in the preview, see tools/vfx_preview/scenes.d/jibril.json and docs/vfx_remake/jibril_fx_calls.md):
 * COMMAND 73, JUDGMENT 69, PURGE 60, SHACKLES 70, WORD 59, REFLECT 58, KINGDOM 41 (budget cap 50; two instances overlap for 20 ticks on a re-send: 82), WINGS 45 (budget cap 50; two instances overlap for 20 ticks: 90).
 * Decorative extras (motes, petals, feathers, curtains, streamers, glow passes) fall away with the camera distance (the manager already drops an effect that is out of range and thins the rest by its distance level of
 * detail), the curtains of the Kingdom are only those nearest to the camera (never more than 6), a far wave ring of the Purge is skipped. No effect keeps anything between frames, so nothing has to be cleaned up on
 * logout, death or dimension change; the server sends one packet per effect (VfxSpawn) and nothing here runs on the server.
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
    public static final ResourceLocation SCROLL = t("jibril_scroll");
    public static final ResourceLocation EMBLEM = t("jibril_emblem");
    public static final ResourceLocation PETAL = t("jibril_petal");
    public static final ResourceLocation RIBBON = t("jibril_ribbon");
    public static final ResourceLocation SPIRAL = t("jibril_spiral");
    public static final ResourceLocation PRISM = t("jibril_prism");

    private static final int WHITE = 0xFFFFFFFF;
    private static final int PEARL = 0xFFFFFAF4;
    private static final int PEACH = 0xFFFFD6C0;
    private static final int ROSEGOLD = 0xFFF0A68A;
    private static final int AMBER = 0xFFFFB85C;
    private static final int AZURE = 0xFF84B0FF;
    private static final int LAV = 0xFFB6A0F4;
    /** The prismatic cycle (pink, gold, aqua, violet); {@link #iris} walks it. */
    private static final int[] IRIS_STOPS = {0xFFFF96CD, 0xFFFFD682, 0xFF8AEEE8, 0xFFB296F4};

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

    @Override public int defaultColor(VfxShape s) { return AMBER; }

    /** The long-running, re-sent effects are drawn with a cap of their own: 50 quads each (two overlap for 20 ticks on a re-send: 100 at most); the others use the usual 100 quads. */
    @Override
    public int vertexBudget(VfxShape s) {
        return switch (s) {
            case JIBRIL_KINGDOM -> 200;
            case JIBRIL_WINGS -> 200;
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

    /** The prismatic cycle at phase t (period 1): pink, gold, aqua, violet. Used to drift the hue of glows over time. */
    static int iris(float t) {
        float f = (t - (float) Math.floor(t)) * 4f;
        int i = (int) f;
        return VfxVertexBuffer.lerpColor(IRIS_STOPS[i & 3], IRIS_STOPS[(i + 1) & 3], f - i);
    }

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
        if (((cA >>> 24) | (cB >>> 24)) < 3) return;
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

    /** A flat strip lying on the ground (height y) from (ax, az) to (bx, bz): streamers that run out over the floor. */
    private void groundStrip(VfxVertexBuffer buf, ResourceLocation tex, float ax, float y, float az, float bx, float bz, float hw, float vA, float vB, int cA, int cB) {
        if (((cA >>> 24) | (cB >>> 24)) < 3) return;
        float dx = bx - ax, dz = bz - az;
        float l = Mth.sqrt(dx * dx + dz * dz);
        if (l < 1e-4f) return;
        float nx = -dz / l * hw, nz = dx / l * hw;
        quad(buf, tex, VfxBlend.ADD, ax - nx, y, az - nz, ax + nx, y, az + nz, bx + nx, y, bz + nz, bx - nx, y, bz - nz, 0, vB, 1, vA, cA, cB);
    }

    /** A law scroll panel centred on (cx, cy, cz) with half-width vector r and half-height vector u; {@code reveal} 0..1 unrolls it from the middle outwards. */
    private void scrollPanel(VfxVertexBuffer buf, float cx, float cy, float cz, float rx, float ry, float rz, float ux, float uy, float uz, float reveal, int argb) {
        if (reveal <= 0.03f || (argb >>> 24) < 3) return;
        float u0 = 0.5f - 0.5f * reveal, u1 = 0.5f + 0.5f * reveal;
        float sx = rx * reveal, sy = ry * reveal, sz = rz * reveal;
        quad(buf, SCROLL, VfxBlend.ADD, cx - sx - ux, cy - sy - uy, cz - sz - uz, cx + sx - ux, cy + sy - uy, cz + sz - uz, cx + sx + ux, cy + sy + uy, cz + sz + uz, cx - sx + ux, cy - sy + uy, cz - sz + uz,
                u0, 0f, u1, 1f, argb, argb);
    }

    private void sprite(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, float x, float y, float z, float size, float rot, int argb) {
        if (size <= 0.001f || (argb >>> 24) < 3) return;
        q0.set(x, y, z);
        buf.billboard(ctx, tex, VfxBlend.ADD, q0, size, rot, argb);
    }

    private void flare(VfxRenderContext ctx, VfxVertexBuffer buf, float x, float y, float z, float size, float rot, int argb) { sprite(ctx, buf, FLARE, x, y, z, size, rot, argb); }

    private void mote(VfxRenderContext ctx, VfxVertexBuffer buf, float x, float y, float z, float size, int argb) { sprite(ctx, buf, MOTE, x, y, z, size, 0f, argb); }

    /** A feather (tip up when rot = 0), a camera-facing sprite. */
    private void feather(VfxRenderContext ctx, VfxVertexBuffer buf, float x, float y, float z, float size, float rot, int argb) { sprite(ctx, buf, FEATHER, x, y, z, size, rot, argb); }

    /** A prismatic bloom: a pearl core with a soft rainbow ring, camera-facing. */
    private void prism(VfxRenderContext ctx, VfxVertexBuffer buf, float x, float y, float z, float size, float alpha) { sprite(ctx, buf, PRISM, x, y, z, size, 0f, a(WHITE, alpha)); }

    private void glow(VfxRenderContext ctx, VfxVertexBuffer buf, float x, float y, float z, float size, int argb, float intensity) {
        if (intensity <= 0.01f) return;
        q0.set(x, y, z);
        VfxBloom.glow(ctx, buf, q0, size, argb, intensity);
    }

    /**
     * {@code n} sprites that drift from height yFrom to yTo round (cx, cz), each on its own phase (staggered), swaying, turning and fading in and out: falling petals (yFrom above yTo), rising feathers or motes.
     * The radius of each is drawn from r0..r1 and narrows a little as it goes.
     */
    private void drift(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, int n, float cx, float cz, float r0, float r1, float yFrom, float yTo,
                       float age, float period, long seed, int salt, float size, float spin, int argbA, int argbB, float alpha) {
        if (alpha <= 0.01f) return;
        for (int i = 0; i < n; i++) {
            float u = Mth.frac(age / period + h(seed, i, salt));
            float an = h(seed, i, salt + 1) * Mth.TWO_PI + u * 1.4f;
            float rr = Mth.lerp(h(seed, i, salt + 2), r0, r1) * (0.65f + 0.35f * u);
            float sway = Mth.sin(u * 7f + i * 1.7f) * 0.28f;
            float sz = size * (0.7f + 0.6f * h(seed, i, salt + 3));
            float rot = Mth.sin(u * 6f + i) * 0.9f + spin * age * ((i & 1) == 0 ? 1f : -1f);
            sprite(ctx, buf, tex, cx + Mth.cos(an) * rr + sway, Mth.lerp(u, yFrom, yTo), cz + Mth.sin(an) * rr + sway * 0.7f, sz, rot, a((i % 3 == 0) ? argbA : argbB, Mth.sin(u * Mth.PI) * alpha));
        }
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

    /** The floor seal (rose window) whose outer ring lies at {@code radius}. */
    private static void sealAt(VfxVertexBuffer buf, VfxPose pose, float radius, int argb) {
        if (radius <= 0.02f || (argb >>> 24) < 3) return;
        buf.plane(SEAL, VfxBlend.ADD, pose, radius / 0.985f, argb);
    }

    /** The ribbon spiral, drawn out to {@code radius}. */
    private static void spiralAt(VfxVertexBuffer buf, VfxPose pose, float radius, int argb) {
        if (radius <= 0.02f || (argb >>> 24) < 3) return;
        buf.plane(SPIRAL, VfxBlend.ADD, pose, radius, argb);
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
        float D = inst.duration;
        float fade = life(inst, age, 6, 14);
        if (fade <= 0.01f) return;
        float S = Mth.clamp(inst.power, 0.6f, 4f);
        float hs = Mth.clamp(S / 1.5f, 0.6f, 2.2f);                           // the body scale of the target
        long seed = inst.seed;
        Vector3f c = ctx.rel(inst.from(ctx));
        float near = 1f - smooth(40f, 90f, horizontal(c));
        float fy = c.y - 0.9f * hs, head = c.y + 0.95f * hs;
        float snap = 1f - Mth.clamp(age / LOCK, 0, 1);                       // 1 -> 0: the glyph and the halo close in from wide (anticipation)
        boolean locked = age >= LOCK;
        float since = Math.max(0f, age - LOCK);
        float pulse = 0.85f + 0.15f * Mth.sin(age * 0.2f);
        float flash = locked ? 1f - Mth.clamp(since / 6f, 0, 1) : 0f;        // the flash of the lock
        float out = 1f - Mth.clamp((D - age) / 14f, 0, 1);                   // 0 -> 1 over the last 14 ticks: the banners lift away
        float hue = age * 0.012f;

        // 1. the floor: a rose-window seal turning slowly, the law glyph snapping shut, a ribbon spiral against them, a rim, a glow, the lock's shock rings and sunburst
        VfxPose floor = VfxPose.ground(new Vector3f(c.x, fy + 0.05f, c.z));
        float R = S * 1.25f * (1f + 0.9f * snap);
        sealAt(buf, floor.spin(-age * 0.012f), S * 2.1f * (1f + 0.5f * snap), a(WHITE, 0.55f * fade * pulse));
        glyphAt(buf, floor.lift(0.005f).spin(age * 0.03f), R, a(WHITE, 0.92f * fade * pulse));
        spiralAt(buf, floor.lift(0.012f).spin(-age * 0.07f), S * 1.05f, a(WHITE, (locked ? 0.55f : 0.28f) * fade));
        ringAt(buf, floor.lift(0.02f), R * 0.99f, a(PEARL, 0.7f * fade));
        VfxBloom.planeGlow(buf, floor, S * 1.3f, AMBER, 0.55f * fade);
        float tt = Mth.clamp(since / 12f, 0, 1);
        if (locked && tt < 1f) {
            ringAt(buf, floor.lift(0.03f), S * (1.25f + 1.5f * VfxAnim.easeOutCubic(tt)), a(WHITE, (1f - tt) * 0.8f * fade));
            waveAt(buf, floor.lift(0.04f), S * (1.0f + 2.6f * VfxAnim.easeOutCubic(tt)), a(WHITE, (1f - tt) * 0.75f * fade));
            buf.plane(RAYS, VfxBlend.ADD, floor.lift(0.015f).spin(since * 0.1f), S * 3.2f * (0.5f + 0.5f * VfxAnim.easeOutCubic(tt)), a(WHITE, (1f - tt) * 0.55f * fade));
        }

        // 2. over the head: a nested halo (two rings turning against each other) comes down and locks, small feathers orbit it, a glow, the book emblem on a prismatic bloom
        float haloY = head + (0.35f + 1.8f * snap) * hs;
        float haloR = 0.66f * hs * (1f + 0.7f * snap);
        haloAt(buf, tilted(c.x, haloY, c.z, 0.75f).spin(-age * 0.05f), haloR, a(WHITE, 0.95f * fade));
        haloAt(buf, tilted(c.x, haloY + 0.06f * hs, c.z, 0.75f).spin(age * 0.08f), haloR * 0.70f, a(PEACH, 0.85f * fade));
        glow(ctx, buf, c.x, haloY, c.z, 0.9f * hs, iris(hue), 0.5f * fade);
        float ey = haloY + 0.98f * hs;
        buf.plane(EMBLEM, VfxBlend.ADD, faceCamera(c.x, ey, c.z), 0.52f * hs, a(WHITE, 0.92f * fade * pulse));
        prism(ctx, buf, c.x, ey, c.z, 2.0f * hs, 0.5f * fade);
        int nf = near > 0.01f ? ctx.seg(5, 3) : 0;
        for (int i = 0; i < nf; i++) {
            float an = Mth.TWO_PI * i / nf + age * 0.07f;
            feather(ctx, buf, c.x + Mth.cos(an) * haloR * 1.05f, haloY + 0.04f * hs + 0.05f * Mth.sin(age * 0.2f + i), c.z + Mth.sin(an) * haloR * 1.05f, 0.30f * hs + 0.08f, Mth.sin(age * 0.1f + i) * 0.8f, a(WHITE, 0.9f * fade * near));
        }

        // 3. the prison: law banners slam down from above round the target and stand (they lift away at the end), a ring at their top and foot, dawn curtains between them
        int n = ctx.seg(8, 4);
        float rise = VfxAnim.easeOutCubic(Mth.clamp(age / 7f, 0, 1));
        float hgt = 2.5f * hs;
        float drop = (1f - rise) * 5f * hs - out * 3f * hs;
        float spin = age * 0.01f;
        float ringR = S * 0.95f;
        if (rise > 0.02f) {
            for (int i = 0; i < n; i++) {
                float an = Mth.TWO_PI * i / n + spin;
                float x = c.x + Mth.cos(an) * ringR, z = c.z + Mth.sin(an) * ringR;
                float sw = Mth.sin(age * 0.11f + i * 1.9f) * 0.05f * hs;
                float flick = 0.88f + 0.12f * Mth.sin(age * 0.17f + i * 1.9f);
                ribbon(buf, BARS, x, fy + drop, z, x + sw, fy + drop + hgt, z + sw, 0.26f * hs, 0.26f * hs, 1f, 0f, a(PEACH, 0.72f * fade * flick), a(WHITE, 0.72f * fade * flick));
            }
            ringAt(buf, VfxPose.ground(new Vector3f(c.x, fy + drop + hgt + 0.05f, c.z)), ringR, a(WHITE, 0.55f * fade));
            ringAt(buf, VfxPose.ground(new Vector3f(c.x, fy + 0.12f, c.z)), ringR, a(PEACH, 0.5f * fade));
            int nk = ctx.seg(4, 2);
            for (int i = 0; i < nk; i++) {
                float an = Mth.TWO_PI * (i + 0.5f) / nk + spin * 1.5f;
                float x = c.x + Mth.cos(an) * ringR * 1.04f, z = c.z + Mth.sin(an) * ringR * 1.04f;
                ribbon(buf, CURTAIN, x, fy + drop * 0.6f, z, x, fy + drop * 0.6f + hgt * 1.15f, z, 0.62f * hs, 0.62f * hs, 1f, 0f, a(WHITE, 0.34f * fade * rise), a(WHITE, 0.16f * fade * rise));
            }
        }

        // 4. braided chains pin the limbs: from the top of the banners in to the arms, legs and chest, each sagging once
        int nc = ctx.seg(4, 2);
        float reach = VfxAnim.easeOutCubic(Mth.clamp((age - 3f) / 8f, 0, 1));
        if (reach > 0.02f) {
            for (int k = 0; k < nc; k++) {
                float an = Mth.TWO_PI * (k + 0.5f) / nc + spin + 0.4f;
                float sx = c.x + Mth.cos(an) * ringR, sz = c.z + Mth.sin(an) * ringR, sy = fy + hgt + drop;
                float ty = c.y + hs * (k % 4 == 0 ? 0.55f : k % 4 == 1 ? 0.0f : k % 4 == 2 ? -0.55f : 0.3f);
                float tx = c.x + Mth.cos(an) * 0.28f * hs, tz = c.z + Mth.sin(an) * 0.28f * hs;
                float ex = sx + (tx - sx) * reach, ey2 = sy + (ty - sy) * reach, ez = sz + (tz - sz) * reach;
                float mx = (sx + ex) * 0.5f, mz = (sz + ez) * 0.5f, my = (sy + ey2) * 0.5f - 0.35f * hs * reach;
                float hw = 0.17f * hs;
                float l1 = Mth.sqrt((mx - sx) * (mx - sx) + (my - sy) * (my - sy) + (mz - sz) * (mz - sz));
                float l2 = Mth.sqrt((ex - mx) * (ex - mx) + (ey2 - my) * (ey2 - my) + (ez - mz) * (ez - mz));
                int ca = a(WHITE, 0.8f * fade);
                ribbon(buf, CHAIN, sx, sy, sz, mx, my, mz, hw, hw, -age * 0.02f, -age * 0.02f + l1 / (hw * 8f), ca, ca);
                ribbon(buf, CHAIN, mx, my, mz, ex, ey2, ez, hw, hw, -age * 0.02f + l1 / (hw * 8f), -age * 0.02f + (l1 + l2) / (hw * 8f), ca, ca);
            }
        }

        // 5. law scrolls orbit slowly inside the prison and unroll after the lock; ribbons of light spiral up the outside of the banners
        float scr = smooth(LOCK, LOCK + 8f, age);
        int ns = ctx.seg(3, 2);
        for (int i = 0; i < ns && scr > 0.03f; i++) {
            float an = Mth.TWO_PI * i / ns + age * 0.03f;
            float rr = ringR * 0.78f;
            float hw = 0.55f * hs, hh = 0.275f * hs;
            scrollPanel(buf, c.x + Mth.cos(an) * rr, c.y + hs * (0.15f + 0.45f * (i % 2)) + 0.06f * Mth.sin(age * 0.1f + i), c.z + Mth.sin(an) * rr,
                    -Mth.sin(an) * hw, 0f, Mth.cos(an) * hw, 0f, hh, 0f, scr, a(WHITE, 0.85f * fade));
        }
        int nr = ctx.seg(3, 2);
        for (int k = 0; k < nr && rise > 0.5f; k++) {
            float px = 0, py = 0, pz = 0;
            for (int m = 0; m <= 3; m++) {
                float y = hgt * 1.1f * m / 3f;
                float an = Mth.TWO_PI * k / nr + age * 0.05f + y * 1.2f;
                float x = c.x + Mth.cos(an) * ringR * 1.14f, z = c.z + Mth.sin(an) * ringR * 1.14f, yy = fy + drop * 0.5f + y;
                if (m > 0) ribbon(buf, RIBBON, px, py, pz, x, yy, z, 0.26f * hs, 0.26f * hs, -age * 0.03f + (m - 1) * 0.5f, -age * 0.03f + m * 0.5f, a(WHITE, 0.45f * fade * (m == 1 ? 0.6f : 1f)), a(WHITE, 0.45f * fade * (m == 3 ? 0.4f : 1f)));
                px = x;
                py = yy;
                pz = z;
            }
        }

        // 6. the lock: a flash at the head and the body, a prismatic bloom
        if (flash > 0f && age >= LOCK - 1f) {
            flare(ctx, buf, c.x, haloY, c.z, 4.5f * hs * (0.4f + 0.6f * flash), age * 0.05f, a(WHITE, flash * fade));
            glow(ctx, buf, c.x, c.y, c.z, 2.0f * hs, AMBER, flash * 0.55f * fade);
            prism(ctx, buf, c.x, c.y + 0.3f * hs, c.z, 5.5f * hs * (0.5f + 0.5f * flash), flash * 0.65f * fade);
        }

        // 7. petals fall and motes rise through the prison, all the time
        if (near > 0.01f) {
            drift(ctx, buf, PETAL, ctx.seg(8, 3), c.x, c.z, 0.3f * ringR, 1.3f * ringR, fy + 3.4f * hs, fy + 0.1f, age, 46f, seed, 81, 0.30f * hs + 0.06f, 0.05f, WHITE, WHITE, 0.9f * fade * near);
            drift(ctx, buf, MOTE, ctx.seg(8, 3), c.x, c.z, 0.2f * ringR, 1.0f * ringR, fy + 0.1f, fy + 2.9f * hs, age, 40f, seed, 91, 0.24f * hs + 0.06f, 0f, WHITE, AMBER, 0.9f * fade * near);
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
        float hue = age * 0.015f;

        // 1. the pillar: before the impact only a thin braided spear falls, after it the whole braided column stands (wide at first, then it settles and thins at the end): a haze, a braid that scrolls up,
        //    a counter braid and a hot core, dawn-coloured (rose gold at the foot, lavender at the top)
        float fall = VfxAnim.easeInCubic(Mth.clamp(age / IMPACT, 0, 1));
        float yb = landed ? g.y : top - (top - g.y) * fall;
        float w = landed ? Mth.lerp(smooth(0f, 6f, since), 1.45f, 0.92f) * (1f - 0.55f * smooth(D - 14f, D, age)) : 0.20f + 0.1f * fall;
        float len = top - yb;
        float sc = -age * 0.06f, vl = len / 18f;
        int cTop = a(LAV, 0.5f * fade), cFoot = a(ROSEGOLD, 0.45f * fade);
        ribbon(buf, PILLAR, g.x, top, g.z, g.x, yb, g.z, R * w * 1.45f, R * w * 1.45f, sc * 0.4f, sc * 0.4f + vl, cTop, cFoot);
        ribbon(buf, PILLAR, g.x, top, g.z, g.x, yb, g.z, R * w * 0.85f, R * w * 0.85f, sc, sc + vl, a(AZURE, 0.8f * fade), a(PEACH, 0.9f * fade));
        ribbon(buf, PILLAR, g.x, top, g.z, g.x, yb, g.z, R * w * 0.62f, R * w * 0.62f, sc * 1.3f + 0.37f, sc * 1.3f + 0.37f + vl, a(WHITE, 0.7f * fade), a(WHITE, 0.8f * fade));
        ribbon(buf, PILLAR, g.x, top, g.z, g.x, yb, g.z, R * w * 0.26f, R * w * 0.26f, sc * 1.7f, sc * 1.7f + vl, a(WHITE, 0.9f * fade), a(WHITE, 1.0f * fade));
        if (!landed) {
            flare(ctx, buf, g.x, yb, g.z, 3.2f * k * (0.5f + fall), age * 0.2f, a(WHITE, fade));
            glow(ctx, buf, g.x, yb, g.z, 1.8f * k, iris(hue), fade);
        }
        // four long ribbons of light stream up the pillar's flanks (each ribbon texture meanders, and scrolls upwards), turning slowly round it
        if (landed) {
            float hr = R * w * 1.3f + 0.3f * k;
            for (int s2 = 0; s2 < 4; s2++) {
                float an = s2 * Mth.HALF_PI + age * 0.05f;
                float x = g.x + Mth.cos(an) * hr, z = g.z + Mth.sin(an) * hr;
                float sweep = 0.35f * Mth.sin(age * 0.09f + s2 * 1.7f);
                ribbon(buf, RIBBON, x, g.y + 0.3f, z, x + Mth.cos(an + 1.57f) * sweep, g.y + 30f, z + Mth.sin(an + 1.57f) * sweep, 0.75f * k + 0.2f, 0.4f * k + 0.1f, age * 0.06f, age * 0.06f + 6f, a(WHITE, 0.6f * fade), a(WHITE, 0.0f));
            }
        }

        // 2. the ground: the seal that warns (it draws in during the anticipation, then burns), the glyph on it, a ribbon spiral turning against them, a sunburst, the rim and a glow
        VfxPose floor = VfxPose.ground(new Vector3f(g.x, g.y + 0.06f, g.z));
        float warn = landed ? 1f : smooth(0f, IMPACT, age);
        float gl = (landed ? 0.95f : 0.7f * smooth(0f, 6f, age)) * (landed ? fade : 1f);
        float draw = landed ? 1f : 1.7f - 0.7f * warn;                            // the seal starts wide and draws in
        sealAt(buf, floor.spin(age * 0.02f), R * 3.0f * draw, a(WHITE, 0.6f * gl));
        glyphAt(buf, floor.lift(0.005f).spin(-age * 0.05f), R * 2.1f * (landed ? 1f : draw), a(WHITE, gl));
        spiralAt(buf, floor.lift(0.012f).spin(age * 0.09f), R * 2.4f, a(WHITE, 0.55f * gl));
        ringAt(buf, floor.lift(0.02f), R * 2.1f, a(PEARL, 0.7f * gl));
        buf.plane(RAYS, VfxBlend.ADD, floor.lift(0.015f).spin(age * 0.04f), R * 3.8f, a(WHITE, 0.30f * gl * warn));
        VfxBloom.planeGlow(buf, floor, R * 1.7f, AMBER, (landed ? 0.9f : 0.5f * smooth(0f, 6f, age)) * (landed ? fade : 1f));

        // 3. halos slide down the pillar (five of them, the anticipation sends them down from the sky already)
        if (age > 2f) {
            float ha = landed ? fade : 0.35f;
            for (int i = 0; i < 5; i++) {
                float u = Mth.frac(age / 18f + i / 5f);
                float y = g.y + 0.3f + (1f - u) * 26f;
                haloAt(buf, VfxPose.ground(new Vector3f(g.x, y, g.z)).spin(age * 0.09f * (i % 2 == 0 ? 1f : -1f)), R * (1.4f - 0.3f * u), a(i == 2 ? WHITE : PEACH, Mth.sin(u * Mth.PI) * 0.85f * ha));
            }
        }

        if (landed) {
            // 4. the strike: flash, a prismatic bloom, rings and two waves running out, wings of light that open at the foot
            float f = 1f - Mth.clamp(since / 7f, 0, 1);
            if (f > 0f) {
                flare(ctx, buf, g.x, g.y + 0.7f, g.z, 11f * k * (0.35f + 0.65f * f), age * 0.05f, a(WHITE, f));
                glow(ctx, buf, g.x, g.y + 0.7f, g.z, 5f * k * (0.4f + 0.6f * f), AMBER, f);
                prism(ctx, buf, g.x, g.y + 1.2f, g.z, 14f * k * (0.4f + 0.6f * f), 0.8f * f);
            }
            float tt = Mth.clamp(since / 16f, 0, 1);
            if (tt < 1f) {
                ringAt(buf, floor.lift(0.03f), R * (1f + 4.5f * VfxAnim.easeOutCubic(tt)), a(WHITE, (1f - tt) * 0.85f));
                ringAt(buf, floor.lift(0.035f), R * (0.8f + 3.2f * VfxAnim.easeOutCubic(Mth.clamp(since / 12f, 0, 1))), a(PEACH, (1f - Mth.clamp(since / 12f, 0, 1)) * 0.7f));
                waveAt(buf, floor.lift(0.04f), R * (1.2f + 9f * VfxAnim.easeOutCubic(tt)), a(WHITE, (float) Math.pow(1f - tt, 1.2) * 0.8f));
                waveAt(buf, floor.lift(0.045f), R * (0.9f + 6f * VfxAnim.easeOutCubic(Mth.clamp(since / 13f, 0, 1))), a(AMBER, (float) Math.pow(1f - Mth.clamp(since / 13f, 0, 1), 1.2) * 0.7f));
            }
            float open = VfxAnim.easeOutBack(Mth.clamp(since / 9f, 0, 1));
            float wa = (1f - smooth(10f, 24f, since)) * 0.85f * fade;
            if (wa > 0.02f && open > 0.02f) {
                float phi = 0.30f + 0.55f * open;
                float cp = Mth.cos(phi), sp = Mth.sin(phi);
                for (int sd = -1; sd <= 1; sd += 2) {
                    float dx = ctx.camRight.x * sd * cp, dy = sp, dz = ctx.camRight.z * sd * cp;
                    float dl = Mth.sqrt(dx * dx + dy * dy + dz * dz);
                    dx /= dl;
                    dy /= dl;
                    dz /= dl;
                    float qx = -dx * dy, qy = 1f - dy * dy, qz = -dz * dy;
                    float ql = Mth.sqrt(qx * qx + qy * qy + qz * qz);
                    wingQuad(buf, VfxBlend.ALPHA, g.x, g.y + 1.1f * k, g.z, dx, dy, dz, qx / ql, qy / ql, qz / ql, 4.6f * k * open, 2.3f * k * open, sd > 0, a(WHITE, wa));
                }
            }
            // sparks thrown up and out
            int nsp = near > 0.01f ? ctx.seg(10, 4) : 0;
            for (int i = 0; i < nsp; i++) {
                float q = Mth.clamp((since - h(seed, i, 7) * 3f) / 12f, 0, 1);
                if (q <= 0f || q >= 1f) continue;
                float an = Mth.TWO_PI * (i + h(seed, i, 8) * 0.6f) / nsp;
                float r0 = R * 0.5f + R * (0.5f + 3.2f * VfxAnim.easeOutCubic(q)), r1 = R * 0.35f + R * 2.4f * VfxAnim.easeOutCubic(Mth.clamp(q - 0.2f, 0, 1));
                float lift = 0.2f + (1.5f + 3.5f * h(seed, i, 9)) * q;
                ribbon(buf, BEAM, g.x + Mth.cos(an) * r1, g.y + 0.2f, g.z + Mth.sin(an) * r1, g.x + Mth.cos(an) * r0, g.y + lift, g.z + Mth.sin(an) * r0, 0.04f, 0.18f * k, 0f, 0.5f, a(WHITE, 0f), a(WHITE, (1f - q) * 0.9f));
            }
            // 5. the resolve: a staggered rain of petals from the sky, feathers and motes rising round the base
            if (near > 0.01f) {
                drift(ctx, buf, PETAL, ctx.seg(14, 5), g.x, g.z, R * 0.5f, R * 3.0f, g.y + 22f, g.y + 0.2f, since, 34f, seed, 11, 0.55f * Mth.sqrt(k) + 0.1f, 0.06f, WHITE, WHITE, 0.95f * fade * near);
                drift(ctx, buf, FEATHER, ctx.seg(8, 3), g.x, g.z, R * 0.6f, R * 2.0f, g.y + 0.2f, g.y + 9f * Mth.sqrt(k), since, 26f, seed, 31, 0.8f * Mth.sqrt(k) + 0.1f, 0f, WHITE, WHITE, 0.95f * fade * near);
                drift(ctx, buf, MOTE, ctx.seg(8, 3), g.x, g.z, R * 0.7f, R * 2.2f, g.y + 0.1f, g.y + 9f * Mth.sqrt(k), age, 20f, seed, 21, 0.34f, 0f, WHITE, AMBER, 0.9f * fade * near);
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
        float gy = c.y - 0.86f;
        VfxPose floor = VfxPose.ground(new Vector3f(c.x, gy, c.z));
        float run = D * 0.66f;                                                // the rings are out at two thirds of the time
        float rel = Mth.clamp((age - 5f) / run, 0, 1);                        // 0 during the anticipation, then the release runs 0 -> 1
        float hue = age * 0.02f;

        // 1. anticipation (0-8 ticks): streamers of light run in to the caster along the floor and a ring closes round it
        float gq = Mth.clamp(age / 8f, 0, 1);
        if (gq < 1f) {
            ringAt(buf, floor.lift(0.03f), 1.2f + 6f * (1f - VfxAnim.easeOutCubic(gq)), a(WHITE, (1f - gq * 0.5f) * 0.8f * fade));
            int ng = ctx.seg(6, 3);
            for (int i = 0; i < ng; i++) {
                float an = Mth.TWO_PI * i / ng + age * 0.25f;
                float r1 = 1.2f + 7f * (1f - VfxAnim.easeOutCubic(gq)), r0 = r1 + 2.8f;
                groundStrip(buf, RIBBON, c.x + Mth.cos(an) * r0, gy + 0.05f, c.z + Mth.sin(an) * r0, c.x + Mth.cos(an) * r1, c.z + Mth.sin(an) * r1, 0.28f, 0f, 0.7f, a(WHITE, 0f), a(WHITE, (1f - gq) * 0.9f * fade));
            }
        }

        // 2. the release: a flash with a prismatic bloom at the caster, a glyph and a seal under it, a column of dawn light
        float f0 = 1f - Mth.clamp((age - 4f) / 9f, 0, 1);
        float pre = smooth(0f, 6f, age);
        if (f0 > 0f) {
            flare(ctx, buf, c.x, c.y, c.z, 9f * (0.4f + 0.6f * f0) * pre, age * 0.04f, a(WHITE, f0 * pre));
            glow(ctx, buf, c.x, c.y, c.z, 5f * (0.4f + 0.6f * f0), AMBER, f0 * pre);
            prism(ctx, buf, c.x, c.y, c.z, 14f * (0.4f + 0.6f * f0), 0.75f * f0 * pre);
        }
        glyphAt(buf, floor.lift(0.01f).spin(age * 0.08f), 2.6f * (0.7f + 0.3f * VfxAnim.easeOutCubic(Mth.clamp(age / 6f, 0, 1))), a(WHITE, 0.9f * fade));
        sealAt(buf, floor.lift(0.008f).spin(-age * 0.04f), 5.2f * (0.6f + 0.4f * VfxAnim.easeOutCubic(Mth.clamp(age / 8f, 0, 1))), a(WHITE, 0.55f * fade));
        float col = VfxAnim.easeOutCubic(Mth.clamp((age - 4f) / 6f, 0, 1));
        ribbon(buf, PILLAR, c.x, gy + 16f * col, c.z, c.x, gy, c.z, 1.3f, 1.8f, -age * 0.08f, -age * 0.08f + 1.1f, a(LAV, 0f), a(ROSEGOLD, 0.5f * fade));
        ribbon(buf, PILLAR, c.x, gy + 16f * col, c.z, c.x, gy, c.z, 0.8f, 1.1f, -age * 0.11f + 0.4f, -age * 0.11f + 1.5f, a(WHITE, 0f), a(PEACH, 0.65f * fade));
        ribbon(buf, PILLAR, c.x, gy + 16f * col, c.z, c.x, gy, c.z, 0.28f, 0.5f, -age * 0.15f, -age * 0.15f + 1.1f, a(WHITE, 0f), a(WHITE, 0.9f * fade));

        // 3. the floor sweeps: a spiral of ribbons and a sunburst open out to the first ring
        float r0 = Rv * VfxAnim.easeOutCubic(rel);
        if (r0 > 0.5f) {
            spiralAt(buf, floor.lift(0.012f).spin(age * 0.16f), r0 * 0.98f, a(WHITE, 0.55f * (1f - rel) * fade));
            buf.plane(RAYS, VfxBlend.ADD, floor.lift(0.015f).spin(-age * 0.12f), r0 * 0.96f, a(WHITE, 0.45f * (1f - rel) * fade));
        }
        VfxBloom.planeGlow(buf, floor, 3.5f + Rv * 0.12f, AMBER, 0.7f * (1f - Mth.clamp(age / 22f, 0, 1)));

        // 4. the holy wave rings on the ground (four, staggered; two have a companion ring): a ring far from the camera is only a thin line there, it fades and is left out
        for (int k = 0; k < 4; k++) {
            float tt = Mth.clamp((age - 5f - 2.5f * k) / run, 0, 1);
            if (tt <= 0f || tt >= 1f) continue;
            float r = Rv * VfxAnim.easeOutCubic(tt);
            float away = Math.abs(dist - r);
            float vis = 1f - smooth(30f, 90f, away);
            float al = (float) Math.pow(1f - tt, 0.8) * (1f - 0.16f * k) * (0.12f + 0.88f * vis) * fade;
            if (al < 0.02f) continue;
            waveAt(buf, floor.lift(0.02f * (k + 1)), r, a(k == 1 ? PEACH : WHITE, 0.95f * al));
            if (k < 2) ringAt(buf, floor.lift(0.02f * (k + 1) + 0.01f), r * 0.97f, a(WHITE, 0.6f * al));
        }

        // 5. ribbons of light flow out over the floor in front of the first ring
        int nsr = near > 0.01f ? ctx.seg(8, 4) : 0;
        for (int i = 0; i < nsr; i++) {
            float an = Mth.TWO_PI * (i + 0.3f * h(seed, i, 71)) / nsr;
            float rf = Rv * VfxAnim.easeOutCubic(rel) * (0.55f + 0.45f * h(seed, i, 72));
            float ln = 3f + 5f * h(seed, i, 73);
            if (rel <= 0.01f || rel >= 1f) break;
            groundStrip(buf, RIBBON, c.x + Mth.cos(an) * (rf - ln), gy + 0.08f, c.z + Mth.sin(an) * (rf - ln), c.x + Mth.cos(an) * rf, c.z + Mth.sin(an) * rf, 0.3f + 0.2f * h(seed, i, 74), 0f, 1.2f, a(WHITE, 0f), a(WHITE, (1f - rel) * 0.8f * fade));
        }

        // 6. the dome: three rings open round the caster and thin out
        float domeR = Math.min(Rv * 0.9f, 30f);
        for (int k = 0; k < 3; k++) {
            float tt = Mth.clamp((age - 5f - 3.5f * k) / 18f, 0, 1);
            if (tt <= 0f || tt >= 1f) continue;
            waveAt(buf, faceCamera(c.x, c.y, c.z).spin(k * 0.7f), domeR * VfxAnim.easeOutCubic(tt), a(k == 1 ? AZURE : WHITE, (float) Math.pow(1f - tt, 1.2) * 0.7f * fade));
        }

        // 7. the afterglow: a halo glows on the floor and petals, feathers and motes fall and rise round the caster
        float after = smooth(run * 0.55f, run * 1.05f, age) * fade;
        if (after > 0.02f) {
            haloAt(buf, floor.lift(0.05f).spin(age * 0.05f), 4.5f, a(WHITE, 0.8f * after));
            glow(ctx, buf, c.x, gy + 0.3f, c.z, 2.5f, iris(hue), 0.6f * after);
        }
        if (near > 0.01f) {
            float spread = 0.3f + 0.7f * VfxAnim.easeOutCubic(Mth.clamp(age / 16f, 0, 1));
            drift(ctx, buf, PETAL, ctx.seg(12, 4), c.x, c.z, 1.5f * spread, 11f * spread, c.y + 7f, gy + 0.2f, age, 30f, seed, 31, 0.6f, 0.05f, WHITE, WHITE, 0.95f * fade * near);
            drift(ctx, buf, FEATHER, ctx.seg(8, 3), c.x, c.z, 1.5f * spread, 10f * spread, gy + 0.2f, c.y + 6f, age, 30f, seed, 41, 0.66f, 0f, WHITE, WHITE, 0.9f * fade * near);
            drift(ctx, buf, MOTE, ctx.seg(10, 4), c.x, c.z, 1f * spread, 12f * spread, gy + 0.2f, c.y + 7f, age, 22f, seed, 51, 0.3f, 0f, WHITE, AMBER, 0.85f * fade * near);
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
        float lock = Mth.clamp((age - 8f) / 8f, 0, 1);                        // the rings close from 8 to 16
        float flash = 1f - Mth.clamp((age - 16f) / 6f, 0, 1);
        float hue = age * 0.015f;

        // 1. the floor under the target: a seal, a glyph that closes in, a ribbon spiral and a rim
        VfxPose floor = VfxPose.ground(new Vector3f(c.x, fy + 0.05f, c.z));
        float gs = S * 0.95f * (1f + 0.8f * (1f - VfxAnim.easeOutCubic(Mth.clamp(age / 10f, 0, 1))));
        sealAt(buf, floor.spin(age * 0.015f), S * 1.8f, a(WHITE, 0.5f * fade));
        glyphAt(buf, floor.lift(0.005f).spin(-age * 0.04f), gs, a(WHITE, 0.85f * fade));
        spiralAt(buf, floor.lift(0.012f).spin(age * 0.08f), S * 1.05f, a(WHITE, 0.5f * fade));
        ringAt(buf, floor.lift(0.02f), gs * 0.99f, a(PEARL, 0.6f * fade));

        // 2. the braided chains: three fly from the caster to the target along arcs (four links of ribbon each), then settle to a faint link; feathers shed from them
        if (len > 0.5f) {
            float hl = Mth.sqrt(dx * dx + dz * dz);
            float sx = hl > 1e-3f ? -dz / hl : 1f, sz = hl > 1e-3f ? dx / hl : 0f;
            float head = VfxAnim.easeOutCubic(Mth.clamp(age / 12f, 0, 1));
            float chainA = (1f - 0.65f * smooth(14f, 26f, age)) * fade;
            int nc = ctx.seg(3, 2);
            float hw = 0.26f;
            for (int k = 0; k < nc; k++) {
                float off = (k - (nc - 1) * 0.5f) * 1.2f;
                float lift = k % 2 == 0 ? 0.9f : -0.25f;
                float px = B.x, py = B.y, pz = B.z;
                for (int m = 1; m <= 4; m++) {
                    float f = m / 4f;
                    if ((m - 1) / 4f >= head) break;
                    float fe = Math.min(f, head);
                    float arc = Mth.sin(fe * Mth.PI);
                    float qx = B.x + dx * fe + sx * off * arc, qy = B.y + dy * fe + lift * arc, qz = B.z + dz * fe + sz * off * arc;
                    float vA = (m - 1) / 4f * len / (hw * 8f), vB = fe * len / (hw * 8f);
                    ribbon(buf, CHAIN, px, py, pz, qx, qy, qz, hw, hw, vA - age * 0.05f, vB - age * 0.05f, a(WHITE, chainA), a(WHITE, chainA));
                    px = qx;
                    py = qy;
                    pz = qz;
                }
            }
            if (near > 0.01f) {
                int nfe = ctx.seg(6, 2);
                for (int i = 0; i < nfe; i++) {
                    float u = Mth.frac(age / 28f + h(seed, i, 13));
                    float f = Mth.clamp(0.1f + 0.8f * h(seed, i, 14), 0f, head);
                    float arc = Mth.sin(f * Mth.PI);
                    float off = ((i % 3) - 1) * 1.2f;
                    float fx = B.x + dx * f + sx * off * arc, fz = B.z + dz * f + sz * off * arc, fyy = B.y + dy * f + (i % 2 == 0 ? 0.9f : -0.25f) * arc - u * 1.2f;
                    feather(ctx, buf, fx, fyy, fz, 0.34f, Mth.sin(u * 6f + i) * 0.9f, a(WHITE, Mth.sin(u * Mth.PI) * 0.85f * fade * Mth.clamp(head * 3f, 0, 1)));
                }
            }
        }

        // 3. three rings of feather links wrap round the target and snap shut with a flash, shock rings and a prismatic bloom
        float close = VfxAnim.easeOutBack(lock);
        float ringA = smooth(7f, 11f, age) * fade;
        if (ringA > 0.01f) {
            for (int k = 0; k < 3; k++) {
                float y = c.y + hs * (k - 1) * 0.7f;
                float r = 1.0f * hs * (1f + 1.1f * (1f - close));
                buf.plane(CHAINRING, VfxBlend.ADD, tilted(c.x, y, c.z, 0.55f).spin(age * 0.05f * (k % 2 == 0 ? 1f : -1f)), r / 0.9f, a(WHITE, ringA));
            }
            haloAt(buf, tilted(c.x, c.y + 1.0f * hs, c.z, 0.75f).spin(-age * 0.06f), 0.55f * hs * (1f + 0.8f * (1f - close)), a(WHITE, 0.85f * ringA));
            glow(ctx, buf, c.x, c.y + 1.0f * hs, c.z, 0.7f * hs, iris(hue), 0.45f * ringA);
        }
        if (flash > 0f && age >= 15f) {
            float tq = 1f - flash;
            flare(ctx, buf, c.x, c.y + 0.2f * hs, c.z, 4.2f * hs * (0.4f + 0.6f * flash), age * 0.06f, a(WHITE, flash * fade));
            glow(ctx, buf, c.x, c.y, c.z, 2.2f * hs, AMBER, flash * 0.9f * fade);
            prism(ctx, buf, c.x, c.y + 0.2f * hs, c.z, 5f * hs * (0.5f + 0.5f * flash), 0.7f * flash * fade);
            ringAt(buf, floor.lift(0.03f), S * (0.9f + 1.8f * VfxAnim.easeOutCubic(tq)), a(WHITE, flash * 0.8f * fade));
            waveAt(buf, floor.lift(0.04f), S * (0.7f + 2.6f * VfxAnim.easeOutCubic(tq)), a(PEACH, flash * 0.7f * fade));
        }

        // 4. law scrolls orbit the target and unroll as the rings close
        float scr = smooth(10f, 18f, age) * fade;
        int nsc = ctx.seg(4, 2);
        for (int i = 0; i < nsc && scr > 0.03f; i++) {
            float an = Mth.TWO_PI * i / nsc + age * 0.05f;
            float rr = 1.15f * hs;
            float hw = 0.5f * hs, hh = 0.25f * hs;
            scrollPanel(buf, c.x + Mth.cos(an) * rr, c.y + hs * (0.2f + 0.35f * (i % 2)) + 0.05f * Mth.sin(age * 0.12f + i), c.z + Mth.sin(an) * rr,
                    -Mth.sin(an) * hw, 0f, Mth.cos(an) * hw, 0f, hh, 0f, scr, a(WHITE, 0.8f * scr));
        }

        // 5. the stolen buffs: two ribbons and sparks flow from the target to the caster's chest, a bloom pulses at the caster
        float streamA = smooth(14f, 22f, age) * fade;
        if (streamA > 0.01f && len > 0.5f) {
            ribbon(buf, RIBBON, c.x, c.y, c.z, B.x, B.y, B.z, 0.18f, 0.10f, -age * 0.07f, -age * 0.07f + len / 4f, a(WHITE, 0.55f * streamA), a(WHITE, 0.75f * streamA));
            ribbon(buf, BEAM, c.x, c.y, c.z, B.x, B.y, B.z, 0.10f, 0.05f, -age * 0.12f, -age * 0.12f + len / 6f, a(AMBER, 0.35f * streamA), a(WHITE, 0.5f * streamA));
            int nsp = ctx.seg(10, 4);
            for (int i = 0; i < nsp; i++) {
                float f = Mth.frac(age / 20f + h(seed, i, 51));
                float arc = Mth.sin(f * Mth.PI);
                float off = (h(seed, i, 52) - 0.5f) * 0.9f * (1f - 0.4f * f);
                mote(ctx, buf, c.x - dx * f + off * 0.6f, c.y - dy * f + arc * 0.6f + off * 0.4f, c.z - dz * f + off * 0.6f, 0.26f + 0.1f * arc, a(i % 3 == 0 ? WHITE : AMBER, Mth.sin(f * Mth.PI) * 0.95f * streamA));
            }
            float pulseB = Mth.frac(age / 20f);
            glow(ctx, buf, B.x, B.y, B.z, 0.9f + 0.7f * (1f - pulseB), AMBER, 0.55f * streamA * pulseB);
            flare(ctx, buf, B.x, B.y, B.z, 1.6f * pulseB, age * 0.1f, a(WHITE, 0.8f * streamA * pulseB));
            prism(ctx, buf, B.x, B.y, B.z, 1.8f * (0.5f + 0.5f * pulseB), 0.5f * streamA * pulseB);
        }

        // 6. petals fall and motes rise round the target
        if (near > 0.01f) {
            drift(ctx, buf, PETAL, ctx.seg(8, 3), c.x, c.z, 0.3f * S, 1.2f * S, fy + 2.8f * hs, fy + 0.1f, age, 38f, seed, 61, 0.28f * hs + 0.06f, 0.05f, WHITE, WHITE, 0.9f * fade * near);
            drift(ctx, buf, MOTE, ctx.seg(8, 3), c.x, c.z, 0.4f * S, 0.95f * S, fy + 0.1f, fy + 2.4f * hs, age, 40f, seed, 71, 0.2f * hs + 0.05f, 0f, WHITE, AMBER, 0.8f * fade * near);
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
        float pop = VfxAnim.easeOutBack(Mth.clamp((age - 1f) / 3f, 0, 1));
        float R = 0.95f * sc * pop;
        float pulse = 0.85f + 0.15f * Mth.sin(age * 0.6f);
        float hue = age * 0.05f;

        // 1. anticipation (0-2 ticks): a ring converges on the spot where the law is written
        float tc = Mth.clamp(age / 2.5f, 0, 1);
        if (tc < 1f) ringAt(buf, face.lift(0.02f), sc * (2.6f - 1.8f * VfxAnim.easeOutCubic(tc)), a(WHITE, (0.4f + 0.6f * tc) * 0.9f));

        // 2. the law is struck: a rose-window seal and a glyph turning against it, a nested halo behind, a faint sunburst, the scroll unrolling across the lower half
        if (R > 0.02f) {
            sealAt(buf, face.lift(-0.02f).spin(-age * 0.12f + 1f), R * 1.4f, a(WHITE, 0.6f * fade));
            glyphAt(buf, face.spin(age * 0.16f), R, a(WHITE, 0.8f * fade * pulse));
            glyphAt(buf, face.lift(0.03f).spin(-age * 0.22f), R * 0.7f, a(PEACH, 0.45f * fade));
            haloAt(buf, face.lift(-0.04f).spin(age * 0.1f), R * 1.2f, a(PEACH, 0.7f * fade));
            haloAt(buf, face.lift(-0.05f).spin(-age * 0.14f), R * 1.55f, a(WHITE, 0.4f * fade));
            buf.plane(RAYS, VfxBlend.ADD, face.lift(-0.06f).spin(age * 0.2f), R * 2.4f, a(WHITE, 0.22f * fade));
        }
        float un = smooth(1f, 5f, age);
        if (un > 0.03f) {
            Vector3f rr = face.right(), uu = face.up();
            scrollPanel(buf, ox - uu.x * 0.55f * sc, oy - uu.y * 0.55f * sc, oz - uu.z * 0.55f * sc, rr.x * 1.2f * sc, rr.y * 1.2f * sc, rr.z * 1.2f * sc, uu.x * 0.6f * sc, uu.y * 0.6f * sc, uu.z * 0.6f * sc, un, a(WHITE, 0.95f * fade));
        }
        // a small pair of wings opens on both sides of the glyph
        float open = VfxAnim.easeOutBack(Mth.clamp((age - 1f) / 4f, 0, 1));
        if (open > 0.03f) {
            Vector3f rr = face.right(), uu = face.up();
            float phi = 0.25f + 0.35f * open;
            float cp = Mth.cos(phi), sp = Mth.sin(phi);
            for (int sd = -1; sd <= 1; sd += 2) {
                float dxx = rr.x * sd * cp + uu.x * sp, dyy = rr.y * sd * cp + uu.y * sp, dzz = rr.z * sd * cp + uu.z * sp;
                float qxx = -rr.x * sd * sp + uu.x * cp, qyy = -rr.y * sd * sp + uu.y * cp, qzz = -rr.z * sd * sp + uu.z * cp;
                wingQuad(buf, VfxBlend.ADD, ox + rr.x * sd * R * 0.9f, oy + rr.y * sd * R * 0.9f, oz + rr.z * sd * R * 0.9f, dxx, dyy, dzz, qxx, qyy, qzz, 1.5f * sc * open, 0.75f * sc * open, sd > 0, a(WHITE, 0.45f * fade));
            }
        }

        // 3. the strike: parry rings (three, staggered) and a wave flash out, a flare with a prismatic bloom
        for (int k = 0; k < 3; k++) {
            float tt = Mth.clamp((age - 1.5f - 1.2f * k) / 5f, 0, 1);
            if (tt > 0f && tt < 1f) ringAt(buf, face.lift(0.04f + 0.01f * k), sc * (0.4f + (1.6f + 0.4f * k) * VfxAnim.easeOutCubic(tt)), a(k == 1 ? PEACH : WHITE, (1f - tt) * 0.95f));
        }
        float tw = Mth.clamp((age - 2f) / 7f, 0, 1);
        if (tw > 0f && tw < 1f) waveAt(buf, face.lift(0.05f), sc * (0.5f + 2.2f * VfxAnim.easeOutCubic(tw)), a(WHITE, (1f - tw) * 0.8f));
        float f0 = 1f - Mth.clamp((age - 1.5f) / 5f, 0, 1);
        if (f0 > 0f) {
            flare(ctx, buf, ox, oy, oz, 3.4f * sc * (0.4f + 0.6f * f0), age * 0.2f, a(WHITE, 0.75f * f0));
            glow(ctx, buf, ox, oy, oz, 1.8f * sc * (0.5f + 0.5f * f0), AMBER, 0.45f * f0 * fade);
            prism(ctx, buf, ox, oy, oz, 4.2f * sc * (0.5f + 0.5f * f0), 0.4f * f0);
        }

        // 4. script ribbons orbit the glyph, runes (emblems and motes) ride its edge, sparks fly out in its plane
        int nrb = ctx.seg(6, 3);
        for (int i = 0; i < nrb; i++) {
            float an0 = Mth.TWO_PI * i / nrb + age * 0.28f, an1 = an0 + 0.7f;
            float rr0 = R * 1.12f;
            Vector3f rr = face.right(), uu = face.up();
            float ca0 = Mth.cos(an0), sa0 = Mth.sin(an0), ca1 = Mth.cos(an1), sa1 = Mth.sin(an1);
            ribbon(buf, RIBBON, ox + (rr.x * ca0 + uu.x * sa0) * rr0, oy + (rr.y * ca0 + uu.y * sa0) * rr0, oz + (rr.z * ca0 + uu.z * sa0) * rr0,
                    ox + (rr.x * ca1 + uu.x * sa1) * rr0, oy + (rr.y * ca1 + uu.y * sa1) * rr0, oz + (rr.z * ca1 + uu.z * sa1) * rr0,
                    0.14f * sc, 0.14f * sc, -age * 0.1f, -age * 0.1f + 0.25f, a(WHITE, 0.0f), a(WHITE, 0.85f * fade));
        }
        int nsp = ctx.seg(12, 5);
        for (int i = 0; i < nsp; i++) {
            float q = Mth.clamp((age - h(seed, i, 1) * 2f) / 8f, 0, 1);
            if (q <= 0f || q >= 1f) continue;
            float an = Mth.TWO_PI * (i + h(seed, i, 2) * 0.5f) / nsp;
            float ca = Mth.cos(an), sa = Mth.sin(an);
            float rx = face.right().x * ca + face.up().x * sa, ry = face.right().y * ca + face.up().y * sa, rz = face.right().z * ca + face.up().z * sa;
            float r0 = R * (0.9f + 0.9f * q), r1 = R * (0.8f + 1.9f * q);
            ribbon(buf, BEAM, ox + rx * r1, oy + ry * r1, oz + rz * r1, ox + rx * r0, oy + ry * r0, oz + rz * r0, 0.03f, 0.10f * sc, 0f, 0.5f, a(WHITE, 0f), a(WHITE, (1f - q) * 0.9f * fade));
        }
        int nm = ctx.seg(8, 3);
        for (int i = 0; i < nm; i++) {
            float an = Mth.TWO_PI * i / nm + age * 0.3f;
            float ca = Mth.cos(an), sa = Mth.sin(an);
            float rr = R * 1.08f;
            float px = ox + (face.right().x * ca + face.up().x * sa) * rr, py = oy + (face.right().y * ca + face.up().y * sa) * rr, pz = oz + (face.right().z * ca + face.up().z * sa) * rr;
            if (i % 2 == 0) mote(ctx, buf, px, py, pz, 0.3f * sc, a(WHITE, 0.9f * fade));
            else sprite(ctx, buf, EMBLEM, px, py, pz, 0.34f * sc, 0f, a(WHITE, 0.8f * fade));
        }

        // 5. the afterglow: petals and feathers burst out and fall
        float burst = smooth(2f, 4f, age);
        if (burst > 0.01f) {
            drift(ctx, buf, PETAL, ctx.seg(10, 4), ox, oz, 0.2f * sc, 1.8f * sc, oy + 1.4f * sc, oy - 1.0f * sc, age, 10f, seed, 61, 0.34f * sc, 0.1f, WHITE, WHITE, 0.95f * fade * burst);
            drift(ctx, buf, FEATHER, ctx.seg(6, 3), ox, oz, 0.3f * sc, 1.6f * sc, oy + 1.0f * sc, oy - 0.8f * sc, age, 12f, seed, 71, 0.46f * sc, 0f, WHITE, WHITE, 0.9f * fade * burst);
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
        float nearA = 1f - smooth(40f, 90f, horizontal(A));
        float nearB = 1f - smooth(40f, 90f, horizontal(B));

        // 1. the parry at the caster: two rings, a glyph with the emblem inside it, a halo, a flash with a bloom
        VfxPose face = VfxPose.facing(new Vector3f(A.x + ux * 0.9f, A.y + uy * 0.9f, A.z + uz * 0.9f), new Vector3f(ux, uy, uz));
        float tp = Mth.clamp(age / 6f, 0, 1);
        if (tp < 1f) {
            ringAt(buf, face, 0.5f + 2.4f * VfxAnim.easeOutCubic(tp), a(WHITE, (1f - tp) * 0.95f));
            ringAt(buf, face.lift(0.02f), 0.3f + 1.6f * VfxAnim.easeOutCubic(Mth.clamp(age / 5f, 0, 1)), a(PEACH, (1f - Mth.clamp(age / 5f, 0, 1)) * 0.8f));
        }
        float ga = 1f - smooth(0.3f, 0.85f, age / D);
        glyphAt(buf, face.lift(0.4f).spin(age * 0.2f), 1.15f, a(WHITE, 0.9f * ga));
        buf.plane(EMBLEM, VfxBlend.ADD, face.lift(0.45f), 0.5f, a(WHITE, 0.9f * ga));
        haloAt(buf, face.lift(0.3f).spin(-age * 0.15f), 1.5f, a(PEACH, 0.6f * ga));
        float f0 = 1f - Mth.clamp(age / 5f, 0, 1);
        if (f0 > 0f) {
            flare(ctx, buf, A.x + ux * 0.9f, A.y + uy * 0.9f, A.z + uz * 0.9f, 6f * (0.4f + 0.6f * f0), age * 0.15f, a(WHITE, f0));
            glow(ctx, buf, A.x, A.y, A.z, 3f * (0.5f + 0.5f * f0), AMBER, 0.9f * f0);
            prism(ctx, buf, A.x + ux * 0.9f, A.y + uy * 0.9f, A.z + uz * 0.9f, 6.5f * (0.5f + 0.5f * f0), 0.7f * f0);
        }

        // 2. the beams: the head runs to the attacker in a few ticks, the tail leaves the caster after it; three braided beams with an amber underlay and two soft ribbons of light
        float head = VfxAnim.easeOutCubic(Mth.clamp((age - 1f) / 6f, 0, 1));
        float tail = Mth.clamp((age - 4f) / 8f, 0, 1);
        if (head > 0.01f) {
            float hl = Mth.sqrt(dx * dx + dz * dz);
            float sx = hl > 1e-3f ? -dz / hl : 1f, sz = hl > 1e-3f ? dx / hl : 0f;
            float ax0 = A.x + ux * 0.9f, ay0 = A.y + uy * 0.9f, az0 = A.z + uz * 0.9f;
            float s0 = Math.min(tail, head - 0.02f), s1 = head;
            for (int k = 0; k < 3; k++) {
                float off = k == 0 ? 0f : (k == 1 ? 0.55f : -0.55f);
                float hw = k == 0 ? 0.22f : 0.13f;
                float sxk = ax0 + sx * off, szk = az0 + sz * off, syk = ay0 + (k == 0 ? 0f : 0.12f * off);
                float ex = B.x - sxk, ey = B.y - syk, ez = B.z - szk;
                float vA = -age * 0.5f + s0 * len / 6f, vB = -age * 0.5f + s1 * len / 6f;
                ribbon(buf, BEAM, sxk + ex * s0, syk + ey * s0, szk + ez * s0, sxk + ex * s1, syk + ey * s1, szk + ez * s1, hw, hw * 0.7f, vA, vB, a(k == 0 ? WHITE : PEACH, 0.95f * fade), a(WHITE, fade));
                if (k == 0) {
                    ribbon(buf, BEAM, sxk + ex * s0, syk + ey * s0, szk + ez * s0, sxk + ex * s1, syk + ey * s1, szk + ez * s1, 0.75f, 0.5f, vA * 0.4f, vB * 0.4f, a(AMBER, 0.40f * fade), a(ROSEGOLD, 0.30f * fade));
                    ribbon(buf, RIBBON, sxk + ex * s0 + sx * 0.2f, syk + ey * s0 + 0.2f, szk + ez * s0 + sz * 0.2f, sxk + ex * s1 + sx * 0.2f, syk + ey * s1 + 0.2f, szk + ez * s1 + sz * 0.2f, 0.5f, 0.35f, -age * 0.3f + s0 * len / 5f, -age * 0.3f + s1 * len / 5f, a(WHITE, 0.55f * fade), a(WHITE, 0.55f * fade));
                    ribbon(buf, RIBBON, sxk + ex * s0 - sx * 0.2f, syk + ey * s0 - 0.2f, szk + ez * s0 - sz * 0.2f, sxk + ex * s1 - sx * 0.2f, syk + ey * s1 - 0.2f, szk + ez * s1 - sz * 0.2f, 0.5f, 0.35f, age * 0.3f + s0 * len / 5f, age * 0.3f + s1 * len / 5f, a(AZURE, 0.45f * fade), a(WHITE, 0.45f * fade));
                }
            }
            // three rings travel along the beam
            for (int k = 0; k < 3; k++) {
                float f = head - 0.20f * k;
                if (f <= 0.03f || f >= 0.98f) continue;
                VfxPose p = VfxPose.facing(new Vector3f(A.x + dx * f, A.y + dy * f, A.z + dz * f), new Vector3f(ux, uy, uz));
                ringAt(buf, p, 0.7f + 0.2f * k, a(k == 1 ? PEACH : WHITE, 0.85f * fade));
            }
            // feathers and petals ride the beam and scatter off it
            if (nearA > 0.01f || nearB > 0.01f) {
                int nfe = ctx.seg(10, 4);
                for (int i = 0; i < nfe; i++) {
                    float f = Mth.clamp(h(seed, i, 41) * head, 0.02f, 0.97f);
                    float u = Mth.clamp((age - 1f) / 10f - f * 0.3f, 0f, 1f);
                    float off = (h(seed, i, 42) - 0.5f) * 1.6f * (0.3f + u);
                    feather(ctx, buf, A.x + dx * f + sx * off, A.y + dy * f + (h(seed, i, 43) - 0.5f) * 1.2f * u - u * 0.5f, A.z + dz * f + sz * off, 0.5f, Mth.sin(u * 6f + i) * 0.9f, a(WHITE, (1f - u) * 0.85f * fade));
                }
                int npt = ctx.seg(6, 3);
                for (int i = 0; i < npt; i++) {
                    float f = Mth.clamp(h(seed, i, 45) * head, 0.02f, 0.97f);
                    float u = Mth.clamp((age - 2f) / 10f - f * 0.3f, 0f, 1f);
                    float off = (h(seed, i, 46) - 0.5f) * 2.2f * (0.3f + u);
                    sprite(ctx, buf, PETAL, A.x + dx * f + sx * off, A.y + dy * f + (h(seed, i, 47) - 0.5f) * 1.6f * u - u * 0.9f, A.z + dz * f + sz * off, 0.4f, u * 5f + i, a(WHITE, (1f - u) * 0.9f * fade));
                }
            }
        }

        // 3. the burst at the attacker: a glyph and a seal, three rings, a wave, a sunburst, a flash with a bloom, sparks, motes and petals
        float since = age - 7f;
        if (since >= 0f) {
            float tt = Mth.clamp(since / 7f, 0, 1);
            VfxPose iface = faceCamera(B.x, B.y, B.z);
            glyphAt(buf, iface.spin(age * 0.25f), 1.3f * (0.6f + 0.4f * VfxAnim.easeOutCubic(tt)), a(WHITE, (1f - tt) * 0.95f));
            sealAt(buf, iface.lift(0.01f).spin(-age * 0.2f), 1.9f * (0.6f + 0.4f * VfxAnim.easeOutCubic(tt)), a(PEACH, (1f - tt) * 0.6f));
            ringAt(buf, iface.lift(0.02f), 0.5f + 2.6f * VfxAnim.easeOutCubic(tt), a(WHITE, (1f - tt) * 0.9f));
            ringAt(buf, iface.lift(0.03f), 0.3f + 1.7f * VfxAnim.easeOutCubic(Mth.clamp(since / 5f, 0, 1)), a(PEACH, (1f - Mth.clamp(since / 5f, 0, 1)) * 0.8f));
            ringAt(buf, iface.lift(0.04f), 0.2f + 3.4f * VfxAnim.easeOutCubic(Mth.clamp(since / 8f, 0, 1)), a(AZURE, (1f - Mth.clamp(since / 8f, 0, 1)) * 0.6f));
            waveAt(buf, iface.lift(0.05f), 0.4f + 3.8f * VfxAnim.easeOutCubic(tt), a(WHITE, (1f - tt) * 0.7f));
            buf.plane(RAYS, VfxBlend.ADD, iface.lift(0.06f).spin(age * 0.3f), 2.8f * (0.5f + 0.5f * VfxAnim.easeOutCubic(tt)), a(WHITE, (1f - tt) * 0.6f));
            float f1 = 1f - tt;
            flare(ctx, buf, B.x, B.y, B.z, 6.5f * (0.4f + 0.6f * f1), -age * 0.1f, a(WHITE, f1));
            glow(ctx, buf, B.x, B.y, B.z, 3.2f * (0.5f + 0.5f * f1), AMBER, f1);
            prism(ctx, buf, B.x, B.y, B.z, 8f * (0.4f + 0.6f * f1), 0.75f * f1);
            int ns = ctx.seg(12, 4);
            for (int i = 0; i < ns; i++) {
                float q = Mth.clamp((since - h(seed, i, 3) * 1.5f) / 6f, 0, 1);
                if (q <= 0f || q >= 1f) continue;
                float th = h(seed, i, 4) * Mth.TWO_PI, ph = (h(seed, i, 5) - 0.5f) * Mth.PI;
                float dd = (0.8f + 2.6f * h(seed, i, 6)) * VfxAnim.easeOutCubic(q);
                float px = B.x + Mth.cos(th) * Mth.cos(ph) * dd, py = B.y + Mth.sin(ph) * dd, pz = B.z + Mth.sin(th) * Mth.cos(ph) * dd;
                if ((i & 1) == 0) mote(ctx, buf, px, py, pz, 0.32f, a(i % 4 == 0 ? WHITE : AMBER, (1f - q) * 0.95f));
                else sprite(ctx, buf, PETAL, px, py - q * 0.8f, pz, 0.36f, q * 6f + i, a(WHITE, (1f - q) * 0.95f));
            }
            int nm = nearB > 0.01f ? ctx.seg(8, 3) : 0;
            for (int i = 0; i < nm; i++) {
                float q = Mth.clamp((since - h(seed, i, 15) * 2f) / 7f, 0, 1);
                if (q <= 0f || q >= 1f) continue;
                float th = h(seed, i, 16) * Mth.TWO_PI, dd = (0.6f + 1.8f * h(seed, i, 17)) * VfxAnim.easeOutCubic(q);
                feather(ctx, buf, B.x + Mth.cos(th) * dd, B.y + 0.3f + q * 1.2f - q * q * 1.0f, B.z + Mth.sin(th) * dd, 0.5f, Mth.sin(q * 6f + i) * 0.9f, a(WHITE, (1f - q) * 0.9f));
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
        float hue = age * 0.006f;

        // 1. the floor: the rose-window seal turning very slowly (faint, the sanctuary), a ribbon spiral turning against it, a sunburst, the rim, and a seal and glyph under the caster
        VfxPose floor = VfxPose.ground(new Vector3f(c.x, fy + 0.04f, c.z));
        sealAt(buf, floor.spin(age * 0.0015f), R, a(WHITE, 0.26f * fade));
        spiralAt(buf, floor.lift(0.006f).spin(-age * 0.004f), R * 0.96f, a(WHITE, 0.14f * fade));
        ringAt(buf, floor.lift(0.01f), R, a(WHITE, 0.5f * fade * pulse));
        ringAt(buf, floor.lift(0.012f), R * 0.965f, a(PEACH, 0.26f * fade));
        ringAt(buf, floor.lift(0.014f), R * 0.5f, a(AZURE, 0.12f * fade));
        if (nearC > 0.01f) {
            buf.plane(RAYS, VfxBlend.ADD, floor.lift(0.016f).spin(age * 0.01f), 6f, a(WHITE, 0.35f * fade * nearC));
            glyphAt(buf, floor.lift(0.02f).spin(age * 0.01f), 3.2f, a(WHITE, 0.65f * fade * nearC));
            ringAt(buf, floor.lift(0.03f), 3.1f, a(PEARL, 0.5f * fade * nearC));
        }

        // 2. the dawn curtains of the dome's edge: only the few nearest to the camera, never more than six, each fades with its distance
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
                float da = (0.55f + 0.2f * Mth.sin(age * 0.06f + i * 1.7f)) * fade * (1f - smooth(35f, 100f, cd));
                if (da < 0.02f) continue;
                float hgt = 15f + 4f * Mth.sin(age * 0.035f + i * 2.1f);
                float ax = c.x + Mth.cos(an - arcHalf) * R, az = c.z + Mth.sin(an - arcHalf) * R;
                float bx = c.x + Mth.cos(an + arcHalf) * R, bz = c.z + Mth.sin(an + arcHalf) * R;
                quad(buf, CURTAIN, VfxBlend.ADD, ax, fy, az, bx, fy, bz, bx, fy + hgt, bz, ax, fy + hgt, az, 0f, 0f, 1f, 1f, a(WHITE, da), a(WHITE, da * 0.6f));
            }
        }

        // 3. over the caster: nested halos, the book emblem on a prismatic bloom, small feathers orbiting, three streamers of light winding round, petals falling and motes rising
        if (nearC > 0.01f) {
            float hy = c.y + 1.2f;
            haloAt(buf, tilted(c.x, hy, c.z, 0.75f).spin(age * 0.04f), 0.52f, a(WHITE, 0.9f * fade * nearC));
            haloAt(buf, tilted(c.x, hy + 0.05f, c.z, 0.75f).spin(-age * 0.07f), 0.36f, a(PEACH, 0.8f * fade * nearC));
            glow(ctx, buf, c.x, hy, c.z, 0.9f, iris(hue), 0.45f * fade * nearC);
            buf.plane(EMBLEM, VfxBlend.ADD, faceCamera(c.x, hy + 0.9f, c.z), 0.42f, a(WHITE, 0.85f * fade * nearC * pulse));
            prism(ctx, buf, c.x, hy + 0.9f, c.z, 1.7f, 0.4f * fade * nearC);
            int nf = ctx.seg(4, 2);
            for (int i = 0; i < nf; i++) {
                float an = Mth.TWO_PI * i / nf + age * 0.05f;
                feather(ctx, buf, c.x + Mth.cos(an) * 0.56f, hy + 0.04f, c.z + Mth.sin(an) * 0.56f, 0.28f, Mth.sin(age * 0.1f + i) * 0.7f, a(WHITE, 0.85f * fade * nearC));
            }
            int nrb = ctx.seg(3, 2);
            for (int i = 0; i < nrb; i++) {
                float an0 = Mth.TWO_PI * i / nrb + age * 0.02f, an1 = an0 + 1.2f;
                float y0 = fy + 0.5f + 1.6f * i, y1 = y0 + 1.5f;
                ribbon(buf, RIBBON, c.x + Mth.cos(an0) * 1.8f, y0, c.z + Mth.sin(an0) * 1.8f, c.x + Mth.cos(an1) * 1.8f, y1, c.z + Mth.sin(an1) * 1.8f, 0.2f, 0.2f, -age * 0.02f, -age * 0.02f + 0.5f, a(WHITE, 0.0f), a(WHITE, 0.6f * fade * nearC));
            }
            drift(ctx, buf, PETAL, ctx.seg(8, 3), c.x, c.z, 1.0f, 5f, c.y + 4.5f, fy + 0.1f, age, 92f, seed, 21, 0.3f, 0.03f, WHITE, WHITE, 0.8f * fade * nearC);
            drift(ctx, buf, MOTE, ctx.seg(6, 2), c.x, c.z, 0.8f, 4f, fy + 0.2f, fy + 5.5f, age, 70f, seed, 11, 0.26f, 0f, WHITE, AMBER, 0.8f * fade * nearC);
        }
    }

    // ================================================================== JIBRIL_WINGS
    /**
     * One wing quad: the picture (root at its left edge, 58 % of the way down; the leading edge on top) is laid in the plane of the axis d (root to tip) and q (up the picture). Mirrored for the left wing by swapping U.
     */
    private void wingQuad(VfxVertexBuffer buf, VfxBlend blend, float px, float py, float pz, float dx, float dy, float dz, float qx, float qy, float qz, float L, float Hh, boolean mirror, int argb) {
        if ((argb >>> 24) < 3 || L <= 0.01f) return;
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
        float hue = age * 0.01f;

        // 1. the backlight: a prismatic bloom and a warm glow behind the shoulders, shifting hue slowly
        if (near > 0.01f) {
            prism(ctx, buf, c.x - fx * 0.5f, c.y + 0.5f, c.z - fz * 0.5f, 6.2f * P, 0.32f * fade * near);
            glow(ctx, buf, c.x - fx * 0.3f, c.y + 0.4f, c.z - fz * 0.3f, 1.6f * P, iris(hue), 0.35f * fade * near);
        }

        // 2. two pairs of layered wings: the big upper one raised, the smaller lower one hanging; each wing is one alpha-blended quad (so the layers of feathers read) and gets a prismatic rim light on top (same quad, added)
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
                wingQuad(buf, VfxBlend.ALPHA, px0, py, pz0, dx, dy, dz, qx, qy, qz, L, L * 0.5f, side > 0, a(pair == 0 ? 0xFFF0E2EC : 0xFFE6D6E8, pa));
                wingQuad(buf, VfxBlend.ADD, px0, py + 0.01f, pz0, dx, dy, dz, qx, qy, qz, L, L * 0.5f, side > 0, a(WHITE, pa * (0.07f + 0.04f * Mth.sin(age * 0.09f + side + pair))));
            }
        }

        // 3. over the head: nested halos with small feathers orbiting them, a glow
        float hy = c.y + 1.2f;
        float hr = 0.5f * Math.max(P, 0.8f);
        haloAt(buf, tilted(c.x, hy, c.z, 0.75f).spin(age * 0.05f), hr, a(WHITE, 0.95f * fade));
        haloAt(buf, tilted(c.x, hy + 0.05f, c.z, 0.75f).spin(-age * 0.08f), hr * 0.7f, a(PEACH, 0.8f * fade));
        if (near > 0.01f) {
            glow(ctx, buf, c.x, hy, c.z, 0.8f * Math.max(P, 0.8f), iris(hue + 0.4f), 0.4f * fade * near);
            int nof = ctx.seg(4, 2);
            for (int i = 0; i < nof; i++) {
                float an = Mth.TWO_PI * i / nof + age * 0.06f;
                feather(ctx, buf, c.x + Mth.cos(an) * hr * 1.08f, hy + 0.04f, c.z + Mth.sin(an) * hr * 1.08f, 0.26f * Math.max(P, 0.8f), Mth.sin(age * 0.1f + i) * 0.7f, a(WHITE, 0.85f * fade * near));
            }

            // 4. streamers of light drift back from the tips of the upper wings, loose feathers and petals fall from the wings, a few motes at the tips
            for (int side = -1; side <= 1; side += 2) {
                float tx = c.x + sx * side * 2.7f * P - fx * 0.5f, ty = c.y + 1.4f * P + flap * 2f, tz = c.z + sz * side * 2.7f * P - fz * 0.5f;
                float mx = tx - fx * 0.9f + sx * side * 0.25f, my = ty - 0.25f, mz = tz - fz * 0.9f + sz * side * 0.25f;
                float ex = mx - fx * 0.9f + sx * side * 0.25f, ey = my - 0.35f, ez = mz - fz * 0.9f + sz * side * 0.25f;
                float w = 0.17f * Math.max(P, 0.8f);
                ribbon(buf, RIBBON, tx, ty, tz, mx, my, mz, w, w, -age * 0.03f, -age * 0.03f + 0.4f, a(WHITE, 0.75f * fade * near), a(WHITE, 0.55f * fade * near));
                ribbon(buf, RIBBON, mx, my, mz, ex, ey, ez, w, w * 0.6f, -age * 0.03f + 0.4f, -age * 0.03f + 0.8f, a(WHITE, 0.55f * fade * near), a(WHITE, 0.0f));
            }
            int nf = ctx.seg(8, 3);
            for (int i = 0; i < nf; i++) {
                float u = Mth.frac(age / 38f + h(seed, i, 31));
                float side = (i & 1) == 0 ? 1f : -1f;
                float span = (0.8f + 2.0f * h(seed, i, 32)) * P;
                float bx = c.x + sx * side * span - fx * (0.4f + 1.4f * u) + Mth.sin(u * 7f + i) * 0.25f;
                float bz = c.z + sz * side * span - fz * (0.4f + 1.4f * u) + Mth.cos(u * 6f + i) * 0.25f;
                feather(ctx, buf, bx, c.y + 1.0f - u * 2.6f, bz, 0.42f * Math.max(P, 0.8f), Mth.sin(u * 8f + i * 1.3f) * 0.9f, a(WHITE, Mth.sin(u * Mth.PI) * 0.85f * fade * near));
            }
            drift(ctx, buf, PETAL, ctx.seg(8, 3), c.x - fx * 0.8f, c.z - fz * 0.8f, 0.8f * P, 3.2f * P, c.y + 1.9f * P, c.y - 1.4f, age, 50f, seed, 51, 0.28f * Math.max(P, 0.8f), 0.05f, WHITE, WHITE, 0.85f * fade * near);
            int nm = ctx.seg(6, 2);
            for (int i = 0; i < nm; i++) {
                float u = Mth.frac(age / 30f + h(seed, i, 41));
                float side = (i & 1) == 0 ? 1f : -1f;
                mote(ctx, buf, c.x + sx * side * (2.2f + 1.0f * h(seed, i, 42)) * P - fx * 0.5f, c.y + 1.6f * P - u * 1.4f, c.z + sz * side * (2.2f + 1.0f * h(seed, i, 42)) * P - fz * 0.5f, 0.28f, a(i % 2 == 0 ? WHITE : AMBER, Mth.sin(u * Mth.PI) * 0.8f * fade * near));
            }
        }
    }
}
