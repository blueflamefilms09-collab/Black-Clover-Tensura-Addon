package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.entity.veldanava.skills.SolomonRules;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * Solomon, Lord of Sorcery (Veldanava): the sorcerer king, drawn as A SORCERER'S NIGHT. Deep indigo and violet, antique gold, parchment white, and a small prismatic accent for the six elements (flame orange, tide blue,
 * gale green, stone amber, storm yellow, frost cyan; the pillars also use light white and dark violet-black). Motifs: grimoires with turning pages, hexagrams and Solomon's seal rings, magic circles of nested runic rings,
 * chains of linked circles, runes, drifting parchment pages, a honeycomb mana shell and tall pillars of elemental light. It shares nothing with the pearl-white / rose-gold Jibril set of {@link JibrilLayer} (feathers, wings,
 * ribbons), nothing with the steel Sandalphon set of {@code JudgmentLayer} (clock rings, black cracks), is not the cyan maze of {@link TrialLayer} and never touches the Time Magic layer. Written from scratch for this mod
 * (textures: {@code tools/gen_solomon_vfx_textures.py} -> {@code textures/particle/solomon_*.png}, colour baked in, the pillar / beam / flare / mote are neutral light the layer tints per element). The colour of the
 * packet is ignored (Elemental Sovereignty's spark is the one exception: it takes the tint of the projectile it drinks).
 * <ul>
 *   <li>SOLOMON_COPY: Grand Spell Copy. Sent at the caster: from = its centre, to = the centre of whoever cast the copied spell (the same as from when cast from the Grimoire), power = 1, 30 ticks. A scanning ring and three glyph
 *       lines read the spell at {@code to}, streams of runes run from there to the caster and write themselves onto a floating page, which turns into a closed Grimoire and sinks into the caster; a ring closes on the floor.</li>
 *   <li>SOLOMON_CONVERGENCE: Arcane Convergence. Sent at the caster: from = its centre, power = radius (30 in game, the picture stops at 24 blocks), 30 ticks. Two wide rings close in on the floor, light streams of the six
 *       elements, runes and motes run in from the whole radius and sink into the caster, a magic circle wakes under it; a final flash, a hexagram and two mana rings.</li>
 *   <li>SOLOMON_REALM: Realm of Magic. Sent following the caster, re-sent every 80 ticks while held: from = its centre, power = radius (40 in game), 100 ticks (fades in 12, out 12). The nearest rune veils and a rim ring
 *       mark the edge, a floor seal of a magic circle and a hexagram turns under the caster, pages and runes float, rune lines rise and a crown of circles with six element sigils turns over its head.</li>
 *   <li>SOLOMON_CHAIN: Spell Chain. Sent at the caster: from = its centre, to = a point 6-12 blocks ahead, power = 1, 24 ticks. Three magic circles of three different elements open one behind the other along the line and are
 *       linked by a chain of light; they fire in rapid order, each with a flash, a shock ring and a lance of light.</li>
 *   <li>SOLOMON_ERASURE: Mana Zone Erasure. Sent at the caster: from = its centre, power = radius (40 in game, the picture stops at 36 blocks), 30 ticks. A hexagram seal and a circle on the floor, a void that flashes white, a
 *       rune wave that sweeps out and un-writes the glyphs it passes (they lift off and dissolve), a dome of three rings and a fall of rune dust.</li>
 *   <li>SOLOMON_MARK: the mark of the 72 Pillars. Sent following the marked target: from = its centre, power = size (~1.5), 40 ticks. A hexagram seal closes on the ground and a ring of six small element circles spins round it.</li>
 *   <li>SOLOMON_PILLAR: The 72 Pillars on one target. Sent at the ground point under the target: from = that point, power = spread radius (~6), 120 ticks, the seed names how many pillars this target gets and where and when
 *       each lands (the pure {@link SolomonRules}). A pillar shows a telegraph on the ground, falls as a streak of light, lands with a burst and a column of elemental light and leaves a crater scar that fades; at most six
 *       falling and six landing pillars are drawn at once, scars take what is left of the budget.</li>
 *   <li>SOLOMON_SKIN: Mana Skin saved a life. Sent at the wearer: from = its centre, power = size (~1.5), 16 ticks. A violet-gold honeycomb shell snaps on, cracks with light and drinks the blow as motes.</li>
 *   <li>SOLOMON_ABSORB: Elemental Sovereignty took a projectile. Sent at the projectile: from = its position, to = the wearer's chest, power = 1, 14 ticks. A small tinted spark is pulled through a thin curved line into the
 *       wearer and a ring closes on the chest.</li>
 * </ul>
 * <b>Budgets (rule 11).</b> The engine hard-caps every effect at 100 quads (400 vertices) per activation; REALM asks for 50 quads (200 vertices) because it is re-sent every 80 ticks for a 100-tick life and two
 * instances overlap for 20 ticks. Counts below are quads at full detail ({@code ctx.seg} thins them by distance and by the particle setting; minimums in the code); a glow counts as 2 quads, a ring or plane of any
 * radius is one quad. Worst case quads per effect per frame (measured in the preview at every tick, see tools/vfx_preview/scenes.d/solomon.json and docs/vfx_remake/solomon_fx_calls.md):
 * COPY 32, CONVERGENCE 71, REALM 39 (two overlap for 20 ticks: 78), CHAIN 37, ERASURE 50, MARK 30, PILLAR 34 for the usual 18 pillars on one target and 98 for 72 pillars on one target (the buffer stops adding at
 * 100 quads, the layer checks {@code hasBudget} before every pillar part), SKIN 23, ABSORB 16. Each is one activation; there is no per-frame allocation and every loop count is a constant or a {@code ctx.seg} of at most 16.
 * Decorative extras (motes, drifting pages, rune cells) fall away with the camera distance (the manager already drops an effect that is out of range and thins the rest by its distance level of detail), the Realm only
 * draws the five rune veils nearest to the camera, the Pillar draws at most six falling and six landing pillars and keeps scars only while the budget allows. No effect keeps anything between frames, so nothing has to be
 * cleaned up on logout, death or dimension change; the server sends one packet per effect (VfxSpawn) and nothing here runs on the server.
 */
public class SolomonLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    public static final ResourceLocation RING = t("solomon_ring");
    public static final ResourceLocation HEXAGRAM = t("solomon_hexagram");
    public static final ResourceLocation CIRCLE = t("solomon_circle");
    public static final ResourceLocation PAGE = t("solomon_page");
    public static final ResourceLocation RUNES = t("solomon_runes");
    public static final ResourceLocation PILLAR = t("solomon_pillar");
    public static final ResourceLocation RUNE_WAVE = t("solomon_rune_wave");
    public static final ResourceLocation CHAIN = t("solomon_chain");
    public static final ResourceLocation BOOK = t("solomon_book");
    public static final ResourceLocation BEAM = t("solomon_beam");
    public static final ResourceLocation FLARE = t("solomon_flare");
    public static final ResourceLocation MOTE = t("solomon_mote");
    public static final ResourceLocation SHELL = t("solomon_shell");
    public static final ResourceLocation CRACK = t("solomon_crack");
    public static final ResourceLocation SIGILS = t("solomon_sigils");
    public static final ResourceLocation CRATER = t("solomon_crater");
    public static final ResourceLocation VEIL = t("solomon_veil");

    private static final int WHITE = 0xFFFFFFFF;
    private static final int GOLD = 0xFFFFE29A;
    private static final int GOLD_D = 0xFFE2AC46;
    private static final int PARCH = 0xFFFCF0D4;
    private static final int LILAC = 0xFFD6C0FF;
    private static final int VIOLET = 0xFF8A58EC;
    private static final int INDIGO = 0xFF4A34AA;
    private static final int VOID = 0xFF0A0516;
    /** The element tints: flame, tide, gale, stone, storm, frost (the six of the owner's list), then light and dark (the pillars). */
    private static final int[] ELEM = {0xFFFF8A38, 0xFF3C96FF, 0xFF6EEB8C, 0xFFE0A03C, 0xFFFFEB46, 0xFF82EBFF, 0xFFFFFAEB, 0xFF8A4ADC};

    /** The ticks a pillar falls (the telegraph) and how long its column stands and its crater scar lasts after landing. */
    static final int FALL = SolomonRules.PILLAR_DELAY_TICKS;
    static final int COLUMN_LIFE = 12, SCAR_LIFE = 56;
    /** How tall a pillar of light is drawn (blocks). */
    static final float PILLAR_HEIGHT = 48f;
    /** The most the Convergence and the Erasure draw their rings out to (blocks): further is beyond the draw distance. */
    static final float CONVERGENCE_VISUAL = 24f, ERASURE_VISUAL = 36f;
    /** The pillars drawn falling and landing at once (thinned by the particle setting). */
    static final int PILLARS_FALLING = 6, PILLARS_LANDING = 6;

    /** Scratch corners of the quad being drawn (the layer draws on the render thread only, one effect at a time). */
    private final Vector3f q0 = new Vector3f(), q1 = new Vector3f(), q2 = new Vector3f(), q3 = new Vector3f();

    @Override
    public Set<VfxShape> shapes() {
        return EnumSet.of(VfxShape.SOLOMON_COPY, VfxShape.SOLOMON_CONVERGENCE, VfxShape.SOLOMON_REALM, VfxShape.SOLOMON_CHAIN, VfxShape.SOLOMON_ERASURE, VfxShape.SOLOMON_MARK, VfxShape.SOLOMON_PILLAR, VfxShape.SOLOMON_SKIN, VfxShape.SOLOMON_ABSORB);
    }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case SOLOMON_COPY -> 30;
            case SOLOMON_CONVERGENCE -> 30;
            case SOLOMON_CHAIN -> 24;
            case SOLOMON_ERASURE -> 30;
            case SOLOMON_MARK -> 40;
            case SOLOMON_PILLAR -> 120;
            case SOLOMON_SKIN -> 16;
            case SOLOMON_ABSORB -> 14;
            default -> 100;
        };
    }

    @Override public int defaultColor(VfxShape s) { return GOLD_D; }

    /** The long-running, re-sent Realm is drawn with a cap of its own: 50 quads (two overlap for 20 ticks on a re-send: 100 at most); the others use the usual 100 quads. */
    @Override
    public int vertexBudget(VfxShape s) {
        return switch (s) {
            case SOLOMON_REALM -> 200;
            default -> VfxVertexBuffer.MAX_VERTICES_PER_ACTIVATION;
        };
    }

    @Override
    public void onTick(VfxInstance inst) {
        if (inst.shape != VfxShape.SOLOMON_PILLAR) return;
        // camera shake on the first four landings of this target (VfxShake falls off with the distance and keeps the strongest)
        int m = Mth.clamp(SolomonRules.pillarsOfSeed(inst.seed), 1, SolomonRules.PILLARS);
        int n = Math.min(4, m);
        for (int k = 0; k < n; k++) {
            if (inst.age() == SolomonRules.pillarSpawnTick(k, m) + FALL) {
                double[] o = SolomonRules.pillarOffset(inst.seed, k, inst.power);
                VfxShake.add(inst.payload.from().add(o[0], 0, o[1]), 0.9f, 8);
            }
        }
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case SOLOMON_COPY -> copy(inst, ctx, buf);
            case SOLOMON_CONVERGENCE -> convergence(inst, ctx, buf);
            case SOLOMON_REALM -> realm(inst, ctx, buf);
            case SOLOMON_CHAIN -> chain(inst, ctx, buf);
            case SOLOMON_ERASURE -> erasure(inst, ctx, buf);
            case SOLOMON_MARK -> mark(inst, ctx, buf);
            case SOLOMON_PILLAR -> pillars(inst, ctx, buf);
            case SOLOMON_SKIN -> skin(inst, ctx, buf);
            case SOLOMON_ABSORB -> absorb(inst, ctx, buf);
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

    private static int mix(int c0, int c1, float t) { return VfxVertexBuffer.lerpColor(c0, c1, Mth.clamp(t, 0f, 1f)); }

    private static float horizontal(Vector3f v) { return Mth.sqrt(v.x * v.x + v.z * v.z); }

    private static int elem(int i) { return ELEM[Math.floorMod(i, ELEM.length)]; }

    private static float bez(float s, float c, float e, float u) {
        float v = 1f - u;
        return v * v * s + 2f * u * v * c + u * u * e;
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
    private void ribbon(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, float ax, float ay, float az, float bx, float by, float bz,
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
        quad(buf, tex, blend,
                ax - sx * hwA, ay - sy * hwA, az - sz * hwA, ax + sx * hwA, ay + sy * hwA, az + sz * hwA,
                bx + sx * hwB, by + sy * hwB, bz + sz * hwB, bx - sx * hwB, by - sy * hwB, bz - sz * hwB,
                0, vB, 1, vA, cA, cB);
    }

    private void ribbon(VfxVertexBuffer buf, ResourceLocation tex, float ax, float ay, float az, float bx, float by, float bz, float hwA, float hwB, float vA, float vB, int cA, int cB) {
        ribbon(buf, tex, VfxBlend.ADD, ax, ay, az, bx, by, bz, hwA, hwB, vA, vB, cA, cB);
    }

    /** A flat strip lying on the ground (height y) from (ax, az) to (bx, bz). */
    private void groundStrip(VfxVertexBuffer buf, ResourceLocation tex, float ax, float y, float az, float bx, float bz, float hw, float vA, float vB, int cA, int cB) {
        if (((cA >>> 24) | (cB >>> 24)) < 3) return;
        float dx = bx - ax, dz = bz - az;
        float l = Mth.sqrt(dx * dx + dz * dz);
        if (l < 1e-4f) return;
        float nx = -dz / l * hw, nz = dx / l * hw;
        quad(buf, tex, VfxBlend.ADD, ax - nx, y, az - nz, ax + nx, y, az + nz, bx + nx, y, bz + nz, bx - nx, y, bz - nz, 0, vB, 1, vA, cA, cB);
    }

    private void sprite(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, float x, float y, float z, float size, float rot, int argb) {
        if (size <= 0.001f || (argb >>> 24) < 3) return;
        q0.set(x, y, z);
        buf.billboard(ctx, tex, VfxBlend.ADD, q0, size, rot, argb);
    }

    /** One cell {@code idx} of a {@code cols x rows} atlas as a camera-facing sprite (a rune, an element sigil). */
    private void cell(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, int cols, int rows, int idx, float x, float y, float z, float size, float rot, int argb) {
        if (size <= 0.001f || (argb >>> 24) < 3) return;
        float c = Mth.cos(rot) * size * 0.5f, s = Mth.sin(rot) * size * 0.5f;
        Vector3f r = ctx.camRight, u = ctx.camUp;
        float rxx = r.x * c + u.x * s, rxy = r.y * c + u.y * s, rxz = r.z * c + u.z * s;
        float ryx = u.x * c - r.x * s, ryy = u.y * c - r.y * s, ryz = u.z * c - r.z * s;
        float u0 = (idx % cols) / (float) cols, v0 = (idx / cols) / (float) rows;
        quad(buf, tex, VfxBlend.ADD,
                x - rxx - ryx, y - rxy - ryy, z - rxz - ryz,
                x + rxx - ryx, y + rxy - ryy, z + rxz - ryz,
                x + rxx + ryx, y + rxy + ryy, z + rxz + ryz,
                x - rxx + ryx, y - rxy + ryy, z - rxz + ryz,
                u0, v0, u0 + 1f / cols, v0 + 1f / rows, argb, argb);
    }

    private void rune(VfxRenderContext ctx, VfxVertexBuffer buf, int idx, float x, float y, float z, float size, float rot, int argb) { cell(ctx, buf, RUNES, 4, 2, idx & 7, x, y, z, size, rot, argb); }

    private void sigil(VfxRenderContext ctx, VfxVertexBuffer buf, int idx, float x, float y, float z, float size, float rot, int argb) { cell(ctx, buf, SIGILS, 4, 2, idx & 7, x, y, z, size, rot, argb); }

    /** One cell of the sigil atlas lying on a pose plane (the telegraph of a pillar, the circles of a chain). */
    private void sigilPlane(VfxVertexBuffer buf, VfxPose pose, int idx, float hs, int argb) {
        if (hs <= 0.02f || (argb >>> 24) < 3) return;
        int i = Math.floorMod(idx, 8);
        float u0 = (i % 4) * 0.25f, v0 = (i / 4) * 0.5f;
        Vector3f o = pose.origin(), r = pose.right(), u = pose.up();
        quad(buf, SIGILS, VfxBlend.ADD,
                o.x + (-r.x - u.x) * hs, o.y + (-r.y - u.y) * hs, o.z + (-r.z - u.z) * hs,
                o.x + (r.x - u.x) * hs, o.y + (r.y - u.y) * hs, o.z + (r.z - u.z) * hs,
                o.x + (r.x + u.x) * hs, o.y + (r.y + u.y) * hs, o.z + (r.z + u.z) * hs,
                o.x + (-r.x + u.x) * hs, o.y + (-r.y + u.y) * hs, o.z + (-r.z + u.z) * hs,
                u0, v0, u0 + 0.25f, v0 + 0.5f, argb, argb);
    }

    /** A camera-facing sheet of half size hw x hh, rotated by rot, showing the top {@code reveal} of the texture (the page writes itself from the top down). */
    private void sheet(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, float x, float y, float z, float hw, float hh, float reveal, float rot, int argb) {
        if (hw <= 0.003f || hh <= 0.003f || reveal <= 0.02f || (argb >>> 24) < 3) return;
        float c = Mth.cos(rot), s = Mth.sin(rot);
        Vector3f r = ctx.camRight, u = ctx.camUp;
        float rx = (r.x * c + u.x * s) * hw, ry = (r.y * c + u.y * s) * hw, rz = (r.z * c + u.z * s) * hw;
        float ux = (u.x * c - r.x * s) * hh, uy = (u.y * c - r.y * s) * hh, uz = (u.z * c - r.z * s) * hh;
        float k = 2f * reveal;                                         // the bottom edge sits k * up below the top edge
        float tx = x + ux, ty = y + uy, tz = z + uz;
        quad(buf, tex, VfxBlend.ADD,
                tx - rx - ux * k, ty - ry - uy * k, tz - rz - uz * k,
                tx + rx - ux * k, ty + ry - uy * k, tz + rz - uz * k,
                tx + rx, ty + ry, tz + rz,
                tx - rx, ty - ry, tz - rz,
                0, 0, 1, reveal, argb, argb);
    }

    private void flare(VfxRenderContext ctx, VfxVertexBuffer buf, float x, float y, float z, float size, float rot, int argb) { sprite(ctx, buf, FLARE, x, y, z, size, rot, argb); }

    private void mote(VfxRenderContext ctx, VfxVertexBuffer buf, float x, float y, float z, float size, int argb) { sprite(ctx, buf, MOTE, x, y, z, size, 0f, argb); }

    private void glow(VfxRenderContext ctx, VfxVertexBuffer buf, float x, float y, float z, float size, int argb, float intensity) {
        if (intensity <= 0.01f) return;
        q0.set(x, y, z);
        VfxBloom.glow(ctx, buf, q0, size, argb, intensity);
    }

    /** {@code n} motes that drift from height yFrom to yTo round (cx, cz), each on its own phase, swaying and fading in and out. */
    private void drift(VfxRenderContext ctx, VfxVertexBuffer buf, int n, float cx, float cz, float r0, float r1, float yFrom, float yTo,
                       float age, float period, long seed, int salt, float size, int argbA, int argbB, float alpha) {
        if (alpha <= 0.01f) return;
        for (int i = 0; i < n; i++) {
            float u = Mth.frac(age / period + h(seed, i, salt));
            float an = h(seed, i, salt + 1) * Mth.TWO_PI + u * 1.2f;
            float rr = Mth.lerp(h(seed, i, salt + 2), r0, r1) * (0.7f + 0.3f * u);
            float sway = Mth.sin(u * 7f + i * 1.7f) * 0.2f;
            float sz = size * (0.6f + 0.8f * h(seed, i, salt + 3));
            mote(ctx, buf, cx + Mth.cos(an) * rr + sway, Mth.lerp(u, yFrom, yTo), cz + Mth.sin(an) * rr + sway * 0.7f, sz, a((i % 3 == 0) ? argbA : argbB, Mth.sin(u * Mth.PI) * alpha));
        }
    }

    private static VfxPose ground(float x, float y, float z) { return VfxPose.ground(new Vector3f(x, y, z)); }

    /** The ring texture whose bright circle lies at {@code radius}. */
    private static void ringAt(VfxVertexBuffer buf, VfxPose pose, float radius, int argb) {
        if (radius <= 0.02f || (argb >>> 24) < 3) return;
        buf.plane(RING, VfxBlend.ADD, pose, radius / 0.965f, argb);
    }

    /** The seal whose outer ring lies at {@code radius}. */
    private static void hexAt(VfxVertexBuffer buf, VfxPose pose, float radius, int argb) {
        if (radius <= 0.02f || (argb >>> 24) < 3) return;
        buf.plane(HEXAGRAM, VfxBlend.ADD, pose, radius / 0.985f, argb);
    }

    private static void circleAt(VfxVertexBuffer buf, VfxPose pose, float radius, int argb) {
        if (radius <= 0.02f || (argb >>> 24) < 3) return;
        buf.plane(CIRCLE, VfxBlend.ADD, pose, radius / 0.985f, argb);
    }

    /** The rune wave whose bright front lies at {@code radius}. */
    private static void waveAt(VfxVertexBuffer buf, VfxPose pose, float radius, int argb) {
        if (radius <= 0.02f || (argb >>> 24) < 3) return;
        buf.plane(RUNE_WAVE, VfxBlend.ADD, pose, radius / 0.955f, argb);
    }

    /** A pose standing at (x, y, z) that faces the camera (the camera is at the origin). */
    private static VfxPose faceCamera(float x, float y, float z) {
        return VfxPose.facing(new Vector3f(x, y, z), new Vector3f(-x, -y, -z));
    }

    /** A pose at (x, y, z) that lies nearly flat but is tipped towards the camera by k (0 = flat): a ring over a head reads as a ring, not as an edge-on line. */
    private static VfxPose tilted(float x, float y, float z, float k) {
        float d = Mth.sqrt(x * x + z * z);
        float nx = 0f, nz = 0f;
        if (d > 1e-3f) {
            nx = -x / d * k;
            nz = -z / d * k;
        }
        return VfxPose.facing(new Vector3f(x, y, z), new Vector3f(nx, 1f, nz));
    }

    /** A pose standing at (x, y, z) that faces along (dx, dy, dz) tipped towards the camera by k: the circles of a chain (a gate seen from the side is not an edge-on line). */
    private static VfxPose gate(float x, float y, float z, float dx, float dy, float dz, float k) {
        float l = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (l < 1e-4f) return VfxPose.ground(new Vector3f(x, y, z));
        float cl = Mth.sqrt(x * x + y * y + z * z);
        float nx = dx / l, ny = dy / l, nz = dz / l;
        if (cl > 1e-3f) {
            nx += -x / cl * k;
            ny += -y / cl * k;
            nz += -z / cl * k;
        }
        return VfxPose.facing(new Vector3f(x, y, z), new Vector3f(nx, ny, nz));
    }

    // ================================================================== SOLOMON_COPY
    private void copy(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float fade = life(inst, age, 2, 3);
        if (fade <= 0.01f) return;
        long seed = inst.seed;
        Vector3f c = ctx.rel(inst.from(ctx));
        Vector3f s = ctx.rel(inst.to(ctx));
        float ex = s.x - c.x, ey = s.y - c.y, ez = s.z - c.z;
        boolean self = ex * ex + ey * ey + ez * ez < 2.25f;                 // cast from the Grimoire: the spell is read where the caster stands
        float near = 1f - smooth(40f, 90f, horizontal(c));
        float gy = c.y - 0.86f;
        float hd = horizontal(c);
        float px = c.x - (hd > 1e-3f ? c.x / hd * 1.0f : 0f), pz = c.z - (hd > 1e-3f ? c.z / hd * 1.0f : 0f);   // the page floats beside the caster, on the camera's side
        float py = c.y + 0.35f;

        // 1. the read: at the source a ring scans the caster up and down, a seal lies under it and three glyph lines sweep across
        float sc = Mth.clamp(age / 12f, 0, 1);
        float scanA = (1f - smooth(10f, 14f, age)) * fade;
        if (scanA > 0.01f) {
            float sy0 = s.y - 0.9f;
            float scanY = sy0 + 1.9f * (0.5f - 0.5f * Mth.cos(sc * Mth.TWO_PI));
            float sr = self ? 1.4f : 1.15f;
            ringAt(buf, ground(s.x, scanY, s.z), sr, a(GOLD, 0.95f * scanA));
            VfxBloom.planeGlow(buf, ground(s.x, scanY, s.z), sr, VIOLET, 0.55f * scanA);
            hexAt(buf, ground(s.x, sy0 + 0.04f, s.z).spin(age * 0.18f), sr * 1.45f * (0.6f + 0.4f * VfxAnim.easeOutCubic(sc)), a(WHITE, 0.9f * scanA));
            for (int i = 0; i < 3; i++) {
                float ph = Mth.frac(sc * 1.4f + i / 3f);
                float yy = sy0 + 0.1f + 1.8f * ph;
                float hw = 0.95f;
                float rx = ctx.camRight.x * hw, rz = ctx.camRight.z * hw;
                ribbon(buf, BEAM, s.x - rx, yy, s.z - rz, s.x + rx, yy, s.z + rz, 0.05f, 0.05f, 0f, 1.2f, a(LILAC, 0.9f * scanA * Mth.sin(ph * Mth.PI)), a(GOLD, 0.9f * scanA * Mth.sin(ph * Mth.PI)));
            }
            flare(ctx, buf, s.x, sy0 + 0.9f, s.z, 1.6f * (1f - sc * 0.4f), age * 0.1f, a(LILAC, 0.8f * scanA));
        }

        // 2. the streams: runes run from the source along a bent line to the page beside the caster, each carrying a rune
        int ns = ctx.seg(4, 2);
        for (int i = 0; i < ns; i++) {
            float hh = h(seed, i, 3), h2 = h(seed, i, 4);
            float sx, sy, sz;
            if (self) {
                float an = Mth.TWO_PI * (i + hh * 0.4f) / ns + age * 0.04f;
                sx = c.x + Mth.cos(an) * 2.5f;
                sz = c.z + Mth.sin(an) * 2.5f;
                sy = c.y + (hh - 0.2f) * 1.6f;
            } else {
                sx = s.x + (hh - 0.5f) * 0.7f;
                sy = s.y + (h2 - 0.5f) * 1.2f;
                sz = s.z + (h2 - 0.5f) * 0.7f;
            }
            float head = (age - 5f - i * 1.4f) / 12f;
            if (head <= 0f) continue;
            float cx = (sx + px) * 0.5f + (hh - 0.5f) * 3f, cy = (sy + py) * 0.5f + 1.4f + h2, cz = (sz + pz) * 0.5f + (h2 - 0.5f) * 3f;
            float tail = head - 0.34f;
            if (tail >= 1f) continue;
            int cA = (i & 1) == 0 ? GOLD : LILAC;
            float prevX = 0, prevY = 0, prevZ = 0, prevA = 0;
            for (int k = 0; k <= 3; k++) {
                float u = Mth.clamp(Mth.lerp(k / 3f, tail, head), 0f, 1f);
                float x = bez(sx, cx, px, u), y = bez(sy, cy, py, u), z = bez(sz, cz, pz, u);
                float al = (k / 3f) * (1f - smooth(0.92f, 1.15f, head)) * fade;
                if (k > 0) ribbon(buf, BEAM, prevX, prevY, prevZ, x, y, z, 0.06f, 0.09f, -age * 0.1f + k, -age * 0.1f + k + 0.5f, a(cA, 0.85f * prevA), a(cA, 0.9f * al));
                prevX = x;
                prevY = y;
                prevZ = z;
                prevA = al;
            }
            float u = Mth.clamp(head, 0f, 1f);
            rune(ctx, buf, i * 3 + 1, bez(sx, cx, px, u), bez(sy, cy, py, u), bez(sz, cz, pz, u), 0.6f, age * 0.12f + i, a(WHITE, 0.95f * (1f - smooth(0.9f, 1.15f, head)) * fade));
        }

        // 3. the page: it unfolds beside the caster, writes itself from the top down, turns into the cover of the Grimoire and sinks into the caster
        float pin = VfxAnim.easeOutBack(Mth.clamp((age - 7f) / 6f, 0, 1));
        float wr = Mth.clamp((age - 11f) / 9f, 0, 1);
        float cl = smooth(20f, 25f, age);
        float sink = smooth(25f, 30f, age);
        float pa = fade * (1f - sink);
        float hh = 0.95f * pin * (1f - 0.55f * sink);
        float bx = Mth.lerp(sink, px, c.x), by = Mth.lerp(sink, py, c.y + 0.1f), bz = Mth.lerp(sink, pz, c.z);
        if (pin > 0.02f) {
            glow(ctx, buf, bx, by, bz, 1.8f * pin, VIOLET, 0.4f * pa);
            float bob = 0.04f * Mth.sin(age * 0.3f);
            sheet(ctx, buf, PAGE, bx, by + bob, bz, hh * (1f - cl), hh, wr, Mth.sin(age * 0.2f) * 0.08f, a(WHITE, 0.62f * pa));
            if (cl > 0.02f) sheet(ctx, buf, BOOK, bx, by + bob, bz, hh * cl, hh, 1f, 0f, a(WHITE, 0.95f * pa));
        }

        // 4. the close: a ring shuts on the floor and a flare winks out where the book went in; gold motes rise
        float ct = smooth(16f, 30f, age);
        if (ct > 0.01f && ct < 1f) ringAt(buf, ground(c.x, gy + 0.03f, c.z), 2.6f - 1.9f * ct, a(GOLD, (0.4f + 0.6f * ct) * (1f - ct * 0.4f) * fade));
        float fl = smooth(24f, 27f, age) * (1f - smooth(27f, 30f, age));
        if (fl > 0.01f) {
            flare(ctx, buf, c.x, c.y + 0.1f, c.z, 2.8f * fl, age * 0.05f, a(WHITE, fl));
            glow(ctx, buf, c.x, c.y + 0.1f, c.z, 1.8f * fl, VIOLET, 0.8f * fl);
        }
        if (near > 0.01f) drift(ctx, buf, ctx.seg(6, 2), c.x, c.z, 0.4f, 1.6f, gy + 0.2f, gy + 2.6f, age, 22f, seed, 41, 0.3f, GOLD, LILAC, 0.9f * fade * near * smooth(10f, 18f, age));
    }

    // ================================================================== SOLOMON_CONVERGENCE
    private void convergence(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float fade = life(inst, age, 2, 3);
        if (fade <= 0.01f) return;
        long seed = inst.seed;
        float Rv = Mth.clamp(inst.power, 6f, CONVERGENCE_VISUAL);               // the picture stops at 24 blocks, the real reach is the server's
        Vector3f c = ctx.rel(inst.from(ctx));
        float near = 1f - smooth(40f, 90f, horizontal(c));
        float gy = c.y - 0.86f;
        VfxPose floor = ground(c.x, gy + 0.04f, c.z);
        float run = Mth.clamp(age / 22f, 0, 1);
        float close = VfxAnim.easeInOutSine(run);

        // 1. the floor: two wide rings close in, a magic circle wakes under the caster as the light arrives
        float R = Mth.lerp(close, Rv, 1.0f);
        float rA = (1f - smooth(18f, 23f, age)) * fade;
        ringAt(buf, floor, R, a(GOLD, 0.9f * rA));
        ringAt(buf, floor.lift(0.01f), Mth.lerp(VfxAnim.easeInOutSine(Mth.clamp((age - 3f) / 20f, 0, 1)), Rv * 0.78f, 1.0f), a(LILAC, 0.75f * rA));
        VfxBloom.planeGlow(buf, floor, R * 0.8f, VIOLET, 0.30f * rA);
        float wake = smooth(2f, 18f, age);
        circleAt(buf, floor.lift(0.02f).spin(-age * 0.07f), 3.2f * (0.5f + 0.5f * wake), a(WHITE, 0.85f * wake * fade * (1f - 0.4f * smooth(23f, 30f, age))));

        // 2. the streams: light of the six elements runs in from the whole radius and sinks into the caster; runes and motes ride them
        int ns = ctx.seg(14, 6);
        for (int i = 0; i < ns; i++) {
            float an = Mth.TWO_PI * (i + h(seed, i, 1) * 0.6f) / ns;
            float rr = Rv * (0.45f + 0.55f * h(seed, i, 2));
            float sx = c.x + Mth.cos(an) * rr, sz = c.z + Mth.sin(an) * rr, sy = gy + 0.2f + 2.6f * h(seed, i, 3);
            float p = (age - 1f - h(seed, i, 4) * 9f) / 16f;
            if (p <= 0f || p >= 1.25f) continue;
            float curl = (h(seed, i, 5) - 0.5f) * 1.6f;
            int ec = elem(i % 6);
            float pt = Mth.clamp(p - 0.32f, 0f, 1f), ph = Mth.clamp(p, 0f, 1f);
            float kt = (float) Math.pow(pt, 1.7), kh = (float) Math.pow(ph, 1.7);
            float lat = Mth.sin(kh * Mth.PI) * curl * 2.2f, latT = Mth.sin(kt * Mth.PI) * curl * 2.2f;
            float tx = -Mth.sin(an), tz = Mth.cos(an);
            float hx = Mth.lerp(kh, sx, c.x) + tx * lat, hy = Mth.lerp(kh, sy, c.y), hz = Mth.lerp(kh, sz, c.z) + tz * lat;
            float tx2 = Mth.lerp(kt, sx, c.x) + tx * latT, ty2 = Mth.lerp(kt, sy, c.y), tz2 = Mth.lerp(kt, sz, c.z) + tz * latT;
            float al = (1f - smooth(1.0f, 1.25f, p)) * fade;
            ribbon(buf, BEAM, tx2, ty2, tz2, hx, hy, hz, 0.04f, 0.3f, -age * 0.12f, -age * 0.12f + 1f, a(ec, 0f), a(mix(ec, WHITE, 0.3f), 0.95f * al));
            if (near > 0.01f && ph > 0.02f) { glow(ctx, buf, hx, hy, hz, 0.6f, ec, 0.7f * al); mote(ctx, buf, hx, hy, hz, 0.6f, a(mix(ec, WHITE, 0.55f), al)); }
            if ((i & 1) == 0 && near > 0.01f && ph > 0.02f) rune(ctx, buf, i, hx, hy + 0.05f, hz, 0.85f, age * 0.1f + i, a(mix(ec, WHITE, 0.4f), 0.85f * al));
        }

        // 3. the release: a flash, a hexagram on the floor and two mana rings that open out
        float fl = smooth(19f, 22f, age) * (1f - smooth(22f, 30f, age));
        if (fl > 0.01f) {
            flare(ctx, buf, c.x, c.y, c.z, 8.5f * (0.5f + 0.5f * fl), age * 0.04f, a(WHITE, fl));
            glow(ctx, buf, c.x, c.y, c.z, 4f * (0.4f + 0.6f * fl), VIOLET, fl);
            hexAt(buf, floor.lift(0.03f).spin(age * 0.12f), 3.6f * (0.7f + 0.3f * fl), a(GOLD, 0.95f * fl));
        }
        float rt = Mth.clamp((age - 21f) / 9f, 0, 1);
        if (rt > 0f) {
            ringAt(buf, floor.lift(0.05f), 1.0f + 5.5f * VfxAnim.easeOutCubic(rt), a(GOLD, (1f - rt) * 0.9f));
            ringAt(buf, floor.lift(0.055f), 0.8f + 3.6f * VfxAnim.easeOutCubic(Mth.clamp((age - 22.5f) / 7.5f, 0, 1)), a(LILAC, (1f - rt) * 0.8f));
        }
    }

    // ================================================================== SOLOMON_REALM
    private void realm(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float fade = life(inst, age, 12, 12);
        if (fade <= 0.01f) return;
        long seed = inst.seed;
        float Rv = Mth.clamp(inst.power, 8f, 40f);
        Vector3f c = ctx.rel(inst.from(ctx));
        float near = 1f - smooth(40f, 90f, horizontal(c));
        float gy = c.y - 0.86f;
        VfxPose floor = ground(c.x, gy + 0.05f, c.z);
        float pulse = 0.88f + 0.12f * Mth.sin(age * 0.15f);

        // 1. the floor seal: a magic circle and a hexagram turning against each other, a glow under them (3 quads)
        circleAt(buf, floor.spin(age * 0.012f), 7.5f, a(WHITE, 0.78f * fade * pulse));
        hexAt(buf, floor.lift(0.008f).spin(-age * 0.025f), 5.0f, a(WHITE, 0.62f * fade * pulse));
        VfxBloom.planeGlow(buf, floor, 5.5f, VIOLET, 0.4f * fade);

        // 2. the edge: a rim ring and the five rune veils nearest to the camera (of 24 round the edge)
        ringAt(buf, floor.lift(0.01f), Rv, a(GOLD, 0.5f * fade));
        final int N = 24;
        int taken = 0;
        int nv = ctx.seg(5, 2);
        Vector3f fw = ctx.camUp, rt = ctx.camRight;                           // forward = up x right
        float fx = fw.y * rt.z - fw.z * rt.y, fy = fw.z * rt.x - fw.x * rt.z, fz = fw.x * rt.y - fw.y * rt.x;
        for (int pick = 0; pick < nv; pick++) {
            float best = 0.15f;                                                // the veils in front of the camera, the most centred first
            int bi = -1;
            for (int i = 0; i < N; i++) {
                if ((taken & (1 << i)) != 0) continue;
                float an = Mth.TWO_PI * i / N;
                float x = c.x + Mth.cos(an) * Rv, z = c.z + Mth.sin(an) * Rv;
                float d = Mth.sqrt(x * x + z * z) + 1e-3f;
                if (d > 110f) continue;
                float cosA = (x * fx + z * fz) / d;
                if (cosA > best) {
                    best = cosA;
                    bi = i;
                }
            }
            if (bi < 0) break;
            taken |= 1 << bi;
            float an = Mth.TWO_PI * bi / N;
            float x = c.x + Mth.cos(an) * Rv, z = c.z + Mth.sin(an) * Rv;
            float flick = 0.85f + 0.15f * Mth.sin(age * 0.13f + bi * 1.7f);
            ribbon(buf, VEIL, x, gy + 11f, z, x, gy, z, 3.4f, 3.4f, 0f, 1f, a(WHITE, 0.0f), a(WHITE, 0.85f * fade * flick));
        }

        // 3. the crown: three circles turn over the head, six element sigils orbit them
        float cy = c.y + 1.7f;
        buf.plane(CIRCLE, VfxBlend.ADD, tilted(c.x, cy, c.z, 0.8f).spin(age * 0.05f), 1.15f / 0.985f, a(WHITE, 0.9f * fade));
        buf.plane(RING, VfxBlend.ADD, tilted(c.x, cy + 0.05f, c.z, 0.8f).spin(-age * 0.08f), 0.85f / 0.965f, a(GOLD, 0.9f * fade));
        buf.plane(HEXAGRAM, VfxBlend.ADD, tilted(c.x, cy + 0.1f, c.z, 0.8f).spin(age * 0.11f), 0.55f / 0.985f, a(WHITE, 0.9f * fade));
        glow(ctx, buf, c.x, cy, c.z, 1.3f, VIOLET, 0.6f * fade);
        for (int i = 0; i < 6; i++) {
            float an = Mth.TWO_PI * i / 6 + age * 0.04f;
            sigil(ctx, buf, i, c.x + Mth.cos(an) * 1.35f, cy + 0.08f * Mth.sin(age * 0.12f + i) + Mth.sin(an) * 0.35f, c.z + Mth.sin(an) * 1.35f * 0.9f, 0.4f, 0f, a(WHITE, 0.95f * fade));
        }

        // 4. pages and runes drift up through the realm, rune lines rise from the seal
        if (near > 0.01f) {
            int np = ctx.seg(3, 2);
            for (int i = 0; i < np; i++) {
                float u = Mth.frac(age / 70f + h(seed, i, 11));
                float an = h(seed, i, 12) * Mth.TWO_PI + u * 1.1f;
                float rr = 3.0f + 6.0f * h(seed, i, 13);
                float x = c.x + Mth.cos(an) * rr, z = c.z + Mth.sin(an) * rr, y = gy + 0.8f + u * 4.5f + 0.15f * Mth.sin(age * 0.1f + i);
                float al = Mth.sin(u * Mth.PI) * fade * near;
                float rot = Mth.sin(age * 0.05f + i * 2f) * 0.45f;
                glow(ctx, buf, x, y, z, 0.7f, GOLD_D, 0.25f * al);
                sheet(ctx, buf, PAGE, x, y, z, 0.42f, 0.42f, 1f, rot, a(WHITE, 0.9f * al));
                rune(ctx, buf, i * 2 + 3, x + Mth.cos(an + 1.2f) * 1.6f, y + 0.8f + Mth.sin(u * 5f) * 0.3f, z + Mth.sin(an + 1.2f) * 1.6f, 0.4f, rot * 2f + age * 0.05f, a(i % 2 == 0 ? GOLD : LILAC, 0.9f * al));
            }
            int nb = ctx.seg(3, 2);
            for (int i = 0; i < nb; i++) {
                float an = Mth.TWO_PI * (i + 0.5f) / nb + age * 0.02f;
                float x = c.x + Mth.cos(an) * 6.5f, z = c.z + Mth.sin(an) * 6.5f;
                float ph = Mth.frac(age / 40f + i * 0.25f);
                ribbon(buf, BEAM, x, gy + 0.1f, z, x, gy + 0.1f + 6f * (0.4f + 0.6f * ph), z, 0.07f, 0.04f, -age * 0.05f, -age * 0.05f + 2f, a(elem(i), 0.75f * fade * near), a(LILAC, 0f));
            }
            drift(ctx, buf, ctx.seg(4, 2), c.x, c.z, 1.5f, 8f, gy + 0.2f, gy + 6f, age, 50f, seed, 21, 0.3f, GOLD, LILAC, 0.9f * fade * near);
        }
    }

    // ================================================================== SOLOMON_CHAIN
    private void chain(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float fade = life(inst, age, 1, 5);
        if (fade <= 0.01f) return;
        long seed = inst.seed;
        Vector3f c = ctx.rel(inst.from(ctx));
        Vector3f e = ctx.rel(inst.to(ctx));
        float dx = e.x - c.x, dy = e.y - c.y, dz = e.z - c.z;
        float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.5f) return;
        float nx = dx / len, ny = dy / len, nz = dz / len;
        float near = 1f - smooth(40f, 90f, horizontal(c));
        int e0 = (int) (h(seed, 0, 1) * 6f);
        float pxPrev = c.x + nx * 1.2f, pyPrev = c.y + 0.1f, pzPrev = c.z + nz * 1.2f;
        float prevOpen = 1f;
        for (int i = 0; i < 3; i++) {
            float f = len * (0.4f + 0.3f * i);                               // 40 %, 70 % and 100 % of the way
            float x = c.x + nx * f, y = c.y + ny * f, z = c.z + nz * f;
            int ec = elem(e0 + i * 2);
            float openAt = 1f + i * 3f, fireAt = 11f + i * 3.5f;
            float open = VfxAnim.easeOutBack(Mth.clamp((age - openAt) / 5f, 0, 1));
            if (open <= 0.02f) break;
            float R = (1.5f + 0.7f * i) * open;
            float since = age - fireAt;
            float charge = 1f - smooth(0f, 3f, since < 0 ? -1f : since);   // 1 until it fires
            VfxPose g = gate(x, y, z, nx, ny, nz, 0.9f);
            int tint = mix(WHITE, ec, 0.38f);
            buf.plane(CIRCLE, VfxBlend.ADD, g.spin(age * (i % 2 == 0 ? 0.07f : -0.07f)), R / 0.985f, a(tint, 0.9f * fade));
            buf.plane(RING, VfxBlend.ADD, g.lift(0.01f), R * 1.04f / 0.965f, a(ec, 0.95f * fade));
            VfxBloom.planeGlow(buf, g, R * 0.9f, ec, 0.45f * fade);
            sigil(ctx, buf, e0 + i * 2, x, y, z, 1.2f * open, 0f, a(WHITE, 0.95f * fade));
            // the chain of light from the previous circle (or the caster) to this one, reaching out as it opens
            float reach = VfxAnim.easeOutCubic(Mth.clamp((age - openAt - 1f) / 4f, 0, 1));
            if (reach > 0.02f) {
                float bx = pxPrev + (x - pxPrev) * reach, by = pyPrev + (y - pyPrev) * reach, bz = pzPrev + (z - pzPrev) * reach;
                float seg = Mth.sqrt((bx - pxPrev) * (bx - pxPrev) + (by - pyPrev) * (by - pyPrev) + (bz - pzPrev) * (bz - pzPrev));
                float shimmer = 0.75f + 0.25f * Mth.sin(age * 0.8f + i * 2f);
                ribbon(buf, BEAM, pxPrev, pyPrev, pzPrev, bx, by, bz, 0.22f, 0.22f, -age * 0.15f, -age * 0.15f + seg * 0.5f, a(ec, 0.55f * fade * prevOpen), a(ec, 0.55f * fade));
                ribbon(buf, CHAIN, pxPrev, pyPrev, pzPrev, bx, by, bz, 0.15f, 0.15f, age * 0.03f, age * 0.03f + seg * 0.9f, a(WHITE, 0.95f * shimmer * fade), a(WHITE, 0.95f * shimmer * fade));
            }
            // the shot: a flash, a shock ring and a lance of light through the circle
            if (since > -0.5f && since < 9f) {
                float k = Mth.clamp(since / 9f, 0, 1);
                float f1 = 1f - Mth.clamp(since / 5f, 0, 1);
                if (f1 > 0.01f) {
                    flare(ctx, buf, x, y, z, 5.0f * (0.4f + 0.6f * f1), age * 0.1f, a(mix(WHITE, ec, 0.3f), f1 * fade));
                    glow(ctx, buf, x, y, z, 2.4f * f1, ec, f1 * 0.9f * fade);
                }
                buf.plane(RING, VfxBlend.ADD, g.lift(0.04f), (R + 2.6f * VfxAnim.easeOutCubic(k)) / 0.965f, a(ec, (1f - k) * 0.9f * fade));
                float lanceLen = 6f * VfxAnim.easeOutCubic(Mth.clamp(since / 4f, 0, 1));
                ribbon(buf, BEAM, x, y, z, x + nx * lanceLen, y + ny * lanceLen, z + nz * lanceLen, 0.5f * f1 + 0.05f, 0.14f, -age * 0.4f, -age * 0.4f + 2f, a(WHITE, 0.9f * f1 * fade), a(ec, 0.0f));
                ribbon(buf, BEAM, x, y, z, x + nx * lanceLen * 0.75f, y + ny * lanceLen * 0.75f, z + nz * lanceLen * 0.75f, 0.9f * f1 + 0.05f, 0.25f, -age * 0.3f, -age * 0.3f + 1.5f, a(ec, 0.55f * f1 * fade), a(ec, 0f));
            }
            pxPrev = x;
            pyPrev = y;
            pzPrev = z;
            prevOpen = charge;
        }
        if (near > 0.01f) drift(ctx, buf, ctx.seg(6, 2), c.x + nx * len * 0.5f, c.z + nz * len * 0.5f, 0.3f, 2.5f, c.y - 0.6f, c.y + 2.2f, age, 18f, seed, 31, 0.3f, GOLD, LILAC, 0.9f * fade * near);
    }

    // ================================================================== SOLOMON_ERASURE
    private void erasure(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float fade = life(inst, age, 2, 3);
        if (fade <= 0.01f) return;
        long seed = inst.seed;
        float Rv = Mth.clamp(inst.power, 8f, ERASURE_VISUAL);                   // the picture stops at 36 blocks, the real reach is the server's
        Vector3f c = ctx.rel(inst.from(ctx));
        float near = 1f - smooth(40f, 90f, horizontal(c));
        float gy = c.y - 0.86f;
        VfxPose floor = ground(c.x, gy + 0.04f, c.z);
        final float T0 = 8f;                                                      // the tick the void opens and the wave starts
        float sweep = Mth.clamp((age - T0) / 18f, 0, 1);
        float waveR = Rv * VfxAnim.easeOutCubic(sweep);
        float since = Math.max(0f, age - T0);

        // 1. the seal: a hexagram grows on the floor with a circle turning against it; both are written out by the wave
        float seal = VfxAnim.easeOutCubic(Mth.clamp(age / 8f, 0, 1));
        float sealA = (1f - smooth(T0 + 4f, T0 + 16f, age)) * fade;
        if (sealA > 0.01f) {
            hexAt(buf, floor.spin(age * 0.1f), 4.6f * (0.35f + 0.65f * seal), a(WHITE, 0.95f * sealA));
            circleAt(buf, floor.lift(0.01f).spin(-age * 0.06f), 6.8f * (0.35f + 0.65f * seal), a(WHITE, 0.55f * sealA));
            VfxBloom.planeGlow(buf, floor, 4.6f, VIOLET, 0.5f * sealA);
        }

        // 2. the void and its white flash at the caster
        float vd = smooth(T0 - 3f, T0, age) * (1f - smooth(T0, T0 + 3f, age));
        if (vd > 0.01f) {
            q0.set(c.x, c.y, c.z);
            buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ALPHA, q0, 5.2f * (0.6f + 0.4f * vd), 0f, a(VOID, 0.85f * vd));
            flare(ctx, buf, c.x, c.y, c.z, 2.2f, age * 0.2f, a(LILAC, 0.8f * vd));
        }
        float fl = smooth(T0, T0 + 1f, age) * (1f - smooth(T0 + 1f, T0 + 9f, age));
        if (fl > 0.01f) {
            flare(ctx, buf, c.x, c.y, c.z, 12f * (0.45f + 0.55f * fl), age * 0.03f, a(WHITE, fl));
            glow(ctx, buf, c.x, c.y, c.z, 5f * (0.4f + 0.6f * fl), mix(VIOLET, WHITE, 0.6f), fl);
            VfxBloom.planeGlow(buf, floor, 7f * (0.5f + 0.5f * fl), WHITE, 0.8f * fl);
            ribbon(buf, PILLAR, c.x, gy + 22f, c.z, c.x, gy, c.z, 1.1f, 1.8f, -age * 0.2f, -age * 0.2f + 2f, a(WHITE, 0f), a(LILAC, 0.85f * fl));
        }

        // 3. the wave: a rune wave sweeps out, a second white front trails it, a floor glow follows, the dome of three rings rises with it
        if (sweep > 0f) {
            float wa = (1f - smooth(0.75f, 1.0f, sweep)) * fade;
            waveAt(buf, floor.lift(0.03f).spin(age * 0.02f), waveR, a(mix(LILAC, WHITE, 0.3f), 0.95f * wa));
            waveAt(buf, floor.lift(0.035f).spin(-age * 0.03f), waveR * 0.84f, a(WHITE, 0.6f * wa));
            VfxBloom.planeGlow(buf, floor, waveR * 0.7f, VIOLET, 0.32f * wa);
            for (int j = 0; j < 3; j++) {
                float el = j * 0.46f + 0.05f;
                float rj = waveR * Mth.cos(el);
                float yj = waveR * Mth.sin(el) * 0.7f;
                ringAt(buf, ground(c.x, gy + 0.1f + yj, c.z), rj, a(j == 1 ? GOLD : LILAC, (0.85f - 0.2f * j) * wa));
            }
        }

        // 4. the un-writing: glyphs stand on the floor ahead of the wave, as it passes they lift off, turn and dissolve; rune dust falls
        int nr = ctx.seg(14, 6);
        for (int i = 0; i < nr; i++) {
            float rho = 0.12f + 0.84f * h(seed, i, 1);
            float an = Mth.TWO_PI * (i + h(seed, i, 2) * 0.7f) / nr;
            float rr = Rv * rho;
            float tPass = T0 + 18f * (1f - (float) Math.cbrt(1f - rho));
            float x = c.x + Mth.cos(an) * rr, z = c.z + Mth.sin(an) * rr;
            float u = (age - tPass) / 9f;
            if (u >= 1f) continue;
            float wr = Mth.clamp(age / 6f, 0, 1);
            if (u < 0f) {
                rune(ctx, buf, i, x, gy + 0.25f, z, 0.9f, an, a(i % 2 == 0 ? GOLD : LILAC, 0.9f * wr * fade));
            } else {
                float e = VfxAnim.easeOutCubic(u);
                rune(ctx, buf, i, x, gy + 0.25f + 2.2f * e, z, 0.9f * (1f - 0.5f * u), an + u * 2.2f, a(mix(GOLD, WHITE, u), (1f - u) * 0.95f * fade));
                if (near > 0.01f) {
                    mote(ctx, buf, x + 0.2f, gy + 0.4f + 2.8f * e, z, 0.3f, a(LILAC, (1f - u) * 0.8f * fade * near));
                }
            }
        }
        if (near > 0.01f && age > T0) {
            drift(ctx, buf, ctx.seg(10, 4), c.x, c.z, 1f, Math.max(2f, waveR), gy + 4f, gy + 0.2f, since, 16f, seed, 51, 0.32f, LILAC, GOLD, 0.8f * fade * near * (1f - smooth(0.7f, 1f, sweep)));
        }
    }

    // ================================================================== SOLOMON_MARK
    private void mark(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float fade = life(inst, age, 4, 8);
        if (fade <= 0.01f) return;
        long seed = inst.seed;
        float S = Mth.clamp(inst.power, 0.6f, 4f);
        float hs = Mth.clamp(S / 1.5f, 0.6f, 2.2f);
        Vector3f c = ctx.rel(inst.from(ctx));
        float near = 1f - smooth(40f, 90f, horizontal(c));
        float gy = c.y - 0.9f * hs;
        float snap = 1f - Mth.clamp(age / 9f, 0, 1);                         // 1 -> 0: the seal closes in from wide
        boolean locked = age >= 9f;
        float since = Math.max(0f, age - 9f);
        float pulse = 0.85f + 0.15f * Mth.sin(age * 0.25f);
        VfxPose floor = ground(c.x, gy + 0.05f, c.z);

        // 1. the hexagram seal closes on the ground and burns; a rim ring and a glow
        float R = S * 1.35f * (1f + 1.1f * snap);
        hexAt(buf, floor.spin(age * 0.05f), R, a(WHITE, 0.95f * fade * pulse));
        ringAt(buf, floor.lift(0.01f), R * 1.02f, a(GOLD, 0.8f * fade));
        VfxBloom.planeGlow(buf, floor, S * 1.1f, VIOLET, 0.55f * fade);
        if (locked && since < 10f) {
            float k = since / 10f;
            ringAt(buf, floor.lift(0.03f), S * (1.3f + 1.8f * VfxAnim.easeOutCubic(k)), a(GOLD, (1f - k) * 0.9f * fade));
            flare(ctx, buf, c.x, gy + 0.3f, c.z, 3.0f * hs * (1f - k), age * 0.1f, a(WHITE, (1f - k) * fade));
        }

        // 2. six small circles, one per element, spin round the body on a thin ring
        float oy = c.y + 0.1f * hs;
        float orR = S * 0.95f;
        float grow = VfxAnim.easeOutBack(Mth.clamp(age / 7f, 0, 1));
        ringAt(buf, ground(c.x, oy, c.z), orR * grow, a(LILAC, 0.45f * fade));
        for (int i = 0; i < 6; i++) {
            float an = Mth.TWO_PI * i / 6 + age * 0.09f;
            float x = c.x + Mth.cos(an) * orR * grow, z = c.z + Mth.sin(an) * orR * grow;
            float y = oy + 0.05f * Mth.sin(age * 0.2f + i * 1.3f);
            glow(ctx, buf, x, y, z, 0.55f * hs, elem(i), 0.35f * fade * grow);
            sigil(ctx, buf, i, x, y, z, 0.52f * hs * grow, 0f, a(WHITE, 0.98f * fade));
        }
        if (near > 0.01f) drift(ctx, buf, ctx.seg(6, 2), c.x, c.z, 0.4f * S, 1.1f * S, gy + 0.1f, gy + 2.4f * hs, age, 24f, seed, 61, 0.26f, GOLD, LILAC, 0.9f * fade * near);
    }

    // ================================================================== SOLOMON_PILLAR
    private void pillars(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        long seed = inst.seed;
        int m = Mth.clamp(SolomonRules.pillarsOfSeed(seed), 1, SolomonRules.PILLARS);
        float spread = Mth.clamp(inst.power, 1f, 12f);
        Vector3f g = ctx.rel(inst.from(ctx));
        float near = 1f - smooth(60f, 120f, horizontal(g));
        int capFall = ctx.seg(PILLARS_FALLING, 3), capLand = ctx.seg(PILLARS_LANDING, 3);

        // 1. the telegraphs and the falling streaks, the newest first (what is drawn is what the budget allows)
        int drawn = 0;
        for (int k = m - 1; k >= 0 && drawn < capFall; k--) {
            float t = age - SolomonRules.pillarSpawnTick(k, m);
            if (t < 0f || t >= FALL) continue;
            if (!buf.hasBudget(16 * 4)) break;
            drawn++;
            pillarFall(inst, ctx, buf, g, k, t, spread, near);
        }
        // 2. the landings: burst, shock ring and the column that stands a moment
        drawn = 0;
        for (int k = m - 1; k >= 0 && drawn < capLand; k--) {
            float t = age - SolomonRules.pillarSpawnTick(k, m) - FALL;
            if (t < 0f || t >= COLUMN_LIFE) continue;
            if (!buf.hasBudget(8 * 4)) break;
            drawn++;
            pillarLand(inst, ctx, buf, g, k, t, spread, near);
        }
        // 3. the scars of the older ones, while the budget lasts (the nearest ones first by being the newest)
        if (near > 0.01f) {
            int cap = ctx.seg(16, 6);
            int n = 0;
            for (int k = m - 1; k >= 0 && n < cap; k--) {
                float t = age - SolomonRules.pillarSpawnTick(k, m) - FALL;
                if (t < 0f || t >= SCAR_LIFE) continue;
                if (!buf.hasBudget(4 * 2)) break;
                n++;
                double[] o = SolomonRules.pillarOffset(seed, k, spread);
                float al = (t < 2f ? t / 2f : 1f - smooth(10f, SCAR_LIFE, t)) * 0.9f * near;
                float rot = h(seed, k, 5) * Mth.TWO_PI;
                buf.plane(CRATER, VfxBlend.ALPHA, ground(g.x + (float) o[0], g.y + 0.04f + 0.002f * (k % 8), g.z + (float) o[1]).spin(rot), 2.4f, a(WHITE, al));
            }
        }
    }

    /** Pillar k before it lands (t = 0..12 ticks since it started): the telegraph on the ground and a streak of light falling. */
    private void pillarFall(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f g, int k, float t, float spread, float near) {
        long seed = inst.seed;
        double[] o = SolomonRules.pillarOffset(seed, k, spread);
        int el = SolomonRules.pillarElement(seed, k);
        int ec = elem(el);
        float x = g.x + (float) o[0], z = g.z + (float) o[1], y = g.y;
        float u = t / FALL;
        float rise = smooth(0f, 3f, t);
        float r = (float) SolomonRules.PILLAR_IMPACT_RADIUS;
        VfxPose p = ground(x, y + 0.05f + 0.002f * (k % 8), z);
        // the telegraph: the ring of the impact closes on it, the sigil of the element burns inside
        ringAt(buf, p, r * (1f + 0.55f * (1f - u)), a(el == 7 ? VIOLET : mix(ec, WHITE, 0.25f), 0.85f * rise));
        sigilPlane(buf, p.spin(u * 3f), el, r * 0.62f, a(WHITE, (0.5f + 0.5f * u) * rise));
        VfxBloom.planeGlow(buf, p, r * 0.8f, el == 7 ? VIOLET : ec, 0.55f * u * rise);
        // the streak: it falls from the top of the sky and gets there at the end of the telegraph
        float fall = VfxAnim.easeInCubic(Mth.clamp(u, 0, 1));
        float top = y + PILLAR_HEIGHT;
        float hy = top - (top - y) * fall;
        float ty = Math.min(top, hy + 16f);
        float w = 0.28f + 0.16f * u;
        if (el == 7) ribbon(buf, PILLAR, VfxBlend.ALPHA, x, ty, z, x, hy, z, w * 0.9f, w * 0.9f, -t * 0.2f, -t * 0.2f + 1f, a(VOID, 0f), a(VOID, 0.9f));
        ribbon(buf, PILLAR, x, ty, z, x, hy, z, w * 1.8f, w * 1.8f, -t * 0.2f, -t * 0.2f + 1f, a(ec, 0f), a(ec, 0.55f));
        ribbon(buf, PILLAR, x, ty, z, x, hy, z, w * 0.7f, w * 0.7f, -t * 0.3f, -t * 0.3f + 1f, a(WHITE, 0f), a(mix(ec, WHITE, el == 7 ? 0.3f : 0.7f), 0.9f));
        if (u > 0.55f) glow(ctx, buf, x, hy, z, 1.2f + 1.4f * u, ec, 0.7f);
    }

    /** Pillar k after it landed (t = 0..12 ticks since the landing): a burst, a shock ring and the column of light that stands a moment and thins out. */
    private void pillarLand(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f g, int k, float t, float spread, float near) {
        long seed = inst.seed;
        double[] o = SolomonRules.pillarOffset(seed, k, spread);
        int el = SolomonRules.pillarElement(seed, k);
        int ec = elem(el);
        float x = g.x + (float) o[0], z = g.z + (float) o[1], y = g.y;
        float u = t / COLUMN_LIFE;
        float r = (float) SolomonRules.PILLAR_IMPACT_RADIUS;
        float top = y + PILLAR_HEIGHT;
        float f = 1f - Mth.clamp(t / 5f, 0, 1);
        float wcol = Mth.lerp(smooth(0f, 3f, t), 1.5f, 1.05f) * (1f - 0.5f * smooth(5f, COLUMN_LIFE, t));
        float ca = 1f - smooth(3f, COLUMN_LIFE, t);
        VfxPose p = ground(x, y + 0.06f + 0.002f * (k % 8), z);
        if (el == 7) ribbon(buf, PILLAR, VfxBlend.ALPHA, x, top, z, x, y, z, wcol * 0.8f, wcol * 0.8f, -t * 0.2f, -t * 0.2f + 3f, a(VOID, 0.5f * ca), a(VOID, 0.95f * ca));
        ribbon(buf, PILLAR, x, top, z, x, y, z, wcol * 1.7f, wcol * 1.7f, -t * 0.1f, -t * 0.1f + 3f, a(ec, 0.3f * ca), a(ec, 0.6f * ca));
        ribbon(buf, PILLAR, x, top, z, x, y, z, wcol * 0.62f, wcol * 0.62f, -t * 0.25f, -t * 0.25f + 3f, a(WHITE, 0.6f * ca), a(mix(ec, WHITE, el == 7 ? 0.35f : 0.75f), 0.95f * ca));
        ringAt(buf, p.lift(0.01f), r * (0.6f + 0.9f * VfxAnim.easeOutCubic(Mth.clamp(t / 8f, 0, 1))), a(mix(ec, WHITE, 0.3f), (1f - Mth.clamp(t / 8f, 0, 1)) * 0.95f));
        if (f > 0.01f) {
            flare(ctx, buf, x, y + 0.8f, z, 6.5f * (0.4f + 0.6f * f), t * 0.4f, a(mix(WHITE, ec, 0.25f), f));
            glow(ctx, buf, x, y + 0.6f, z, 3f * f, ec, 0.9f * f);
        }
        buf.plane(CRATER, VfxBlend.ALPHA, p.spin(h(seed, k, 5) * Mth.TWO_PI), 2.4f, a(WHITE, 0.9f * smooth(0f, 1.5f, t) * near));
    }

    // ================================================================== SOLOMON_SKIN
    private void skin(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float fade = life(inst, age, 1, 3);
        if (fade <= 0.01f) return;
        long seed = inst.seed;
        float S = Mth.clamp(inst.power, 0.6f, 4f);
        float hs = Mth.clamp(S / 1.5f, 0.6f, 2.2f);
        Vector3f c = ctx.rel(inst.from(ctx));
        float near = 1f - smooth(40f, 90f, horizontal(c));
        float gy = c.y - 0.9f * hs;
        float snap = 1f - VfxAnim.easeOutCubic(Mth.clamp(age / 3f, 0, 1));
        float sc = 1f + 0.4f * snap;
        float shellA = (1f - smooth(10f, 15f, age)) * fade * smooth(0f, 1.5f, age);
        float hit = smooth(2f, 3f, age) * (1f - smooth(3f, 7f, age));        // the flash of the blow

        // 1. the shell snaps on over the body, cracks, and drinks the blow
        float hw = 0.62f * hs * sc;
        float top = c.y + 1.15f * hs * sc, bot = c.y - 1.05f * hs * sc;
        ribbon(buf, SHELL, c.x, bot, c.z, c.x, top, c.z, hw, hw, 1f, 0f, a(WHITE, 0.95f * shellA), a(WHITE, 0.95f * shellA));
        glow(ctx, buf, c.x, c.y, c.z, 1.6f * hs, VIOLET, 0.55f * shellA);
        float crack = smooth(3f, 6f, age) * (1f - smooth(10f, 15f, age)) * fade;
        if (crack > 0.01f) {
            ribbon(buf, CRACK, c.x, bot, c.z, c.x, top, c.z, hw, hw, 1f, 0f, a(WHITE, 0.95f * crack), a(WHITE, 0.95f * crack));
            for (int i = 0; i < 3; i++) {
                float ox = (h(seed, i, 1) - 0.5f) * 0.7f * hs, oy = (h(seed, i, 2) - 0.45f) * 1.5f * hs;
                flare(ctx, buf, c.x + ctx.camRight.x * ox, c.y + oy, c.z + ctx.camRight.z * ox, 1.3f * hs * crack * (0.6f + 0.4f * Mth.sin(age * 0.9f + i)), age * 0.2f + i, a(GOLD, 0.9f * crack));
            }
        }
        if (hit > 0.01f) {
            flare(ctx, buf, c.x, c.y + 0.1f * hs, c.z, 4f * hs * (0.5f + 0.5f * hit), age * 0.1f, a(WHITE, hit));
            glow(ctx, buf, c.x, c.y, c.z, 2.2f * hs * hit, GOLD, hit);
        }

        // 2. the floor: a ring snaps shut under the feet
        float rt = Mth.clamp(age / 9f, 0, 1);
        ringAt(buf, ground(c.x, gy + 0.04f, c.z), S * (0.5f + 1.3f * VfxAnim.easeOutCubic(rt)), a(GOLD, (1f - rt) * 0.9f * fade));
        VfxBloom.planeGlow(buf, ground(c.x, gy + 0.03f, c.z), S * 0.9f, VIOLET, 0.5f * (1f - rt) * fade);

        // 3. the blow is drunk as motes: they leave the shell and stream into the chest
        int nm = ctx.seg(10, 4);
        for (int i = 0; i < nm; i++) {
            float p = (age - 5f - h(seed, i, 3) * 3f) / 7f;
            if (p <= 0f || p >= 1f) continue;
            float an = h(seed, i, 4) * Mth.TWO_PI;
            float sx = Mth.cos(an) * 0.62f * hs, sy = (h(seed, i, 5) - 0.45f) * 1.9f * hs;
            float k = p * p;
            float lx = sx * (1f - k), ly = sy * (1f - k);
            mote(ctx, buf, c.x + ctx.camRight.x * lx + ctx.camUp.x * 0f, c.y + ly + 0.1f * hs, c.z + ctx.camRight.z * lx, 0.3f * hs * (1f - 0.4f * p), a(i % 2 == 0 ? GOLD : LILAC, Mth.sin(p * Mth.PI) * fade));
        }
        if (near > 0.01f) drift(ctx, buf, ctx.seg(4, 2), c.x, c.z, 0.3f, 0.8f * hs, c.y - 0.5f * hs, c.y + 1.4f * hs, age, 12f, seed, 71, 0.22f, GOLD, LILAC, 0.8f * fade * near);
    }

    // ================================================================== SOLOMON_ABSORB
    private void absorb(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float fade = life(inst, age, 1, 3);
        if (fade <= 0.01f) return;
        long seed = inst.seed;
        Vector3f p = ctx.rel(inst.from(ctx));
        Vector3f w = ctx.rel(inst.to(ctx));
        // the tint of the spark: the packet colour when it carries one (the element of the projectile), else one of the six elements from the seed
        int ec = (inst.color & 0xFFFFFF) == (GOLD_D & 0xFFFFFF) ? elem((int) (h(seed, 0, 1) * 6f)) : (inst.color | 0xFF000000);
        int sp = mix(ec, WHITE, 0.3f);
        float dx = w.x - p.x, dy = w.y - p.y, dz = w.z - p.z;
        float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        float near = 1f - smooth(40f, 90f, horizontal(w));
        // the thin curved line bends sideways and up, the spark runs along it
        float side = (h(seed, 0, 2) - 0.5f) * 2f;
        float cx = (p.x + w.x) * 0.5f + dz / Math.max(len, 0.5f) * side * Math.min(1.4f, len * 0.35f);
        float cy = (p.y + w.y) * 0.5f + 0.4f + Math.min(1.2f, len * 0.2f);
        float cz = (p.z + w.z) * 0.5f - dx / Math.max(len, 0.5f) * side * Math.min(1.4f, len * 0.35f);
        float q = VfxAnim.easeInOutSine(Mth.clamp(age / 9f, 0, 1));
        float tail = Math.max(0f, q - 0.55f);
        float line = 1f - smooth(9f, 14f, age);
        float prevX = 0, prevY = 0, prevZ = 0;
        for (int k = 0; k <= 5; k++) {
            float u = Mth.lerp(k / 5f, tail, q);
            float x = bez(p.x, cx, w.x, u), y = bez(p.y, cy, w.y, u), z = bez(p.z, cz, w.z, u);
            if (k > 0) {
                float f0 = (k - 1) / 5f, f1 = k / 5f;
                ribbon(buf, BEAM, prevX, prevY, prevZ, x, y, z, 0.015f + 0.07f * f0, 0.015f + 0.07f * f1, -age * 0.2f + k, -age * 0.2f + k + 0.5f, a(sp, 0.9f * f0 * line * fade), a(sp, 0.95f * f1 * line * fade));
            }
            prevX = x;
            prevY = y;
            prevZ = z;
        }
        float hx = bez(p.x, cx, w.x, q), hy = bez(p.y, cy, w.y, q), hz = bez(p.z, cz, w.z, q);
        float sa = (1f - smooth(8.5f, 10f, age)) * fade;
        flare(ctx, buf, hx, hy, hz, 0.75f * (1f - 0.35f * q), age * 0.3f, a(mix(sp, WHITE, 0.4f), sa));
        glow(ctx, buf, hx, hy, hz, 0.5f, ec, 0.8f * sa);
        // the ring that closes on the chest as the spark arrives
        float ar = Mth.clamp((age - 7f) / 7f, 0, 1);
        if (ar > 0f) {
            VfxPose rp = faceCamera(w.x, w.y, w.z);
            ringAt(buf, rp, 1.0f - 0.8f * VfxAnim.easeOutCubic(ar), a(mix(GOLD, ec, 0.4f), (1f - ar * ar) * 0.95f * fade));
            hexAt(buf, rp.lift(0.01f).spin(age * 0.3f), 0.55f * (1f - ar * 0.4f), a(WHITE, (1f - ar) * 0.9f * fade));
            float fl = (1f - smooth(9f, 13f, age)) * smooth(7.5f, 9f, age);
            flare(ctx, buf, w.x, w.y, w.z, 1.9f * fl, age * 0.2f, a(WHITE, fl * fade));
            glow(ctx, buf, w.x, w.y, w.z, 1.1f * fl, ec, 0.9f * fl * fade);
        }
        if (near > 0.01f) {
            int nm = ctx.seg(4, 2);
            for (int i = 0; i < nm; i++) {
                float u = Mth.clamp((age - 8f - i * 0.4f) / 5f, 0, 1);
                if (u <= 0f || u >= 1f) continue;
                float an = Mth.TWO_PI * i / nm + age * 0.2f;
                float r = 0.9f * (1f - u);
                mote(ctx, buf, w.x + ctx.camRight.x * Mth.cos(an) * r + ctx.camUp.x * Mth.sin(an) * r, w.y + ctx.camRight.y * Mth.cos(an) * r + ctx.camUp.y * Mth.sin(an) * r, w.z + ctx.camRight.z * Mth.cos(an) * r + ctx.camUp.z * Mth.sin(an) * r, 0.22f, a(sp, Mth.sin(u * Mth.PI) * fade));
            }
        }
    }
}
