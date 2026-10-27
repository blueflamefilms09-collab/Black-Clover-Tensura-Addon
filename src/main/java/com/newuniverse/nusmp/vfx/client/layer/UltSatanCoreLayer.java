package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * 3.29.0 Mirajane Strauss, Satan Soul "Hell's Core" (ULT_SATAN_CORE): the demon takes full control and the marks laid on the foes are cashed in.
 * Own palette: deep crimson, violet-black, hellfire orange, bone white. Over 20 ticks (the timeline is read from progress, so it scales with the duration):
 * <ul>
 *   <li>0-7 build-up: a horned pentacle burns into the ground and spins, bat-like wings of dark flame (standing planes: five flame fingers with a
 *       scalloped dark membrane between them) unfold on both sides of the caster, a black-red sphere hangs at the chest and CONTRACTS while embers
 *       are pulled into it.</li>
 *   <li>7-12 peak: the core detonates (white flash, black shell), a ring of hellfire runs out over the ground to the full radius with a standing wall
 *       of flame on its rim, clawed arcs of purple lightning rake round it and bone-white claw rakes tear across the middle; the pentacle flares to full size.</li>
 *   <li>12-20 aftermath: the wings burn away upward, the sigil, ring and wall fade, scorched smoke and embers rise.</li>
 * </ul>
 * Cost: at most ~92 quads (368 vertices) in the worst frame; every count is constant, nothing is allocated per frame (scratch vectors are reused),
 * no entity, no packet: one draw call batch per texture. The wings and the wall face the camera round the vertical axis only.
 */
public class UltSatanCoreLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    private static final ResourceLocation PENTACLE = t("devil_circle"), SIGIL = t("demon_fire_sigil"), SCORCH = t("demon_fire_scorch"), RING = t("demon_fire_ring"),
            WALL = t("demon_fire_wall"), ORB = t("darkfx_orb"), CORONA = t("demon_fire_corona"), FLASH = t("demon_fire_flash"), SWIRL = t("demon_fire_swirl"),
            EMBER = t("demon_fire_ember"), SMOKE = t("demon_fire_smoke"), PILLAR = t("bfx_pillar"), SOLID = t("gel_cel_bar"), BOLT = t("arc_bolt"),
            CLAW = t("beast_claw"), SHOCK = t("darkfx_shock"), BONE_CLAW = t("bone_claw");

    private static final int CRIMSON = 0xFFC01034, BLOOD = 0xFF6A0A1E, VIOLET_BLACK = 0xFF1B0B2E, VIOLET = 0xFF8A3CFF, HELL = 0xFFFF6A1A, HOT = 0xFFFFC070, BONE = 0xFFF4EAD6;

    private static final float DET = 7f;                                                           // the detonation tick (of 20)

    // wing finger directions (degrees from the outward horizontal) and lengths, open and folded
    private static final float[] FA_OPEN = {72f, 38f, 8f, -24f, -54f}, FA_FOLD = {98f, 94f, 90f, 86f, 82f}, FL = {3.4f, 4.7f, 5.1f, 4.5f, 3.5f};

    // per-render scratch (the render thread draws one effect at a time)
    private final Vector3f pa = new Vector3f(), pb = new Vector3f(), pc = new Vector3f(), pd = new Vector3f(), pe = new Vector3f();
    private float cx, cy, cz, rx, rz, hx, hz;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.ULT_SATAN_CORE); }
    @Override public int defaultDuration(VfxShape s) { return 20; }
    @Override public int defaultColor(VfxShape s) { return 0xFFC01034; }

    private static float hash(long seed, int i, int k) {
        long x = seed * 0x9E3779B97F4A7C15L + i * 0xBF58476D1CE4E5B9L + k * 0x94D049BB133111EBL;
        x ^= x >>> 30; x *= 0xBF58476D1CE4E5B9L; x ^= x >>> 27; x *= 0x94D049BB133111EBL; x ^= x >>> 31;
        return (x >>> 40) / (float) (1 << 24);
    }

    private static int a(int argb, float alpha) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(alpha, 0f, 1f)); }
    private static float cl(float v) { return Mth.clamp(v, 0f, 1f); }
    private static float sub(float v, float s, float e) { return cl((v - s) / Math.max(1e-4f, e - s)); }

    /** Camera-relative point of the local frame: x along the camera's horizontal right, y up, d away from the camera. */
    private Vector3f w(Vector3f o, float x, float y, float d) { return o.set(cx + rx * x + hx * d, cy + y, cz + rz * x + hz * d); }

    private void quad(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, float u0, float v0, float u1, float v1, int c0, int c1) {
        if (buf.hasBudget(4)) buf.quad(tex, bl, pa, pb, pc, pd, u0, v0, u1, v1, bl.grade(c0, 1f), bl.grade(c1, 1f));
    }

    private static void sprite(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, Vector3f p, float size, float rot, int col) {
        if (buf.hasBudget(4)) buf.billboard(ctx, tex, bl, p, size, rot, col);
    }

    private static void ground(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, Vector3f centre, float lift, float radius, float spin, int col) {
        if (radius > 0.05f && buf.hasBudget(4)) buf.plane(tex, bl, VfxPose.ground(centre).lift(lift).spin(spin), radius, col);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float p = inst.progress(ctx.partialTick);
        float age = p * 20f;                                                                       // 20-tick timeline whatever the duration
        float R = Math.max(2f, inst.power), u = R / 12f;
        long seed = inst.seed;
        Vector3f c = ctx.rel(inst.from(ctx));
        cx = c.x; cy = c.y; cz = c.z;
        float hl = Mth.sqrt(cx * cx + cz * cz);
        if (hl < 1e-3f) { hx = 0f; hz = 1f; } else { hx = cx / hl; hz = cz / hl; }
        rx = -hz; rz = hx;                                                                          // camera horizontal right

        float det = sub(age, DET, 20f);                                                            // 0 at the detonation, 1 at the end
        float dt = Math.max(0f, age - DET);                                                        // ticks since detonation
        float fadeAll = 1f - sub(age, 15f, 20f);
        Vector3f core = new Vector3f(c).add(0f, 1.25f * u + 0.0f, 0f);

        // ---------------------------------------------------------------- ground: scorch, pentacle, counter-turning sigil
        float burn = VfxAnim.easeOutCubic(sub(age, 0f, 4f));
        float sigR = R * (0.38f + 0.12f * sub(age, 0f, DET) + 0.5f * VfxAnim.easeOutCubic(sub(dt, 0f, 4f)));
        float spin = age * 0.09f + 0.0035f * age * age;
        float flick = 0.85f + 0.15f * Mth.sin(age * 1.7f);
        Vector3f g = new Vector3f(c);
        ground(buf, SCORCH, VfxBlend.ALPHA, g, 0.02f, sigR * (1.0f + 0.12f * det) * 1.05f, -spin * 0.25f, a(VIOLET_BLACK, 0.92f * burn * fadeAll));
        ground(buf, PENTACLE, VfxBlend.ALPHA, g, 0.04f, sigR * 1.05f, spin, a(BLOOD, 0.9f * burn * fadeAll));
        ground(buf, PENTACLE, VfxBlend.ADD, g, 0.07f, sigR, spin, a(VfxVertexBuffer.lerpColor(CRIMSON, HELL, 0.6f * sub(age, 4f, DET + 2f)), (0.75f + 0.25f * flick) * burn * fadeAll));
        ground(buf, SIGIL, VfxBlend.ADD, g, 0.09f, sigR * 1.12f, -spin * 0.6f, a(VIOLET, 0.55f * burn * fadeAll));
        if (buf.hasBudget(4)) VfxBloom.planeGlow(buf, VfxPose.ground(g).lift(0.05f), sigR * 0.8f, CRIMSON, 0.55f * burn * fadeAll);

        // ---------------------------------------------------------------- wings of dark flame
        float open = VfxAnim.easeOutBack(sub(age, 0.5f, 7.5f)) * (1f - 0.55f * VfxAnim.easeInCubic(sub(age, 12f, 20f)));
        float wingA = sub(age, 0.3f, 2.5f) * (1f - sub(age, 13f, 20f));
        if (wingA > 0.02f) {
            float flap = Mth.sin(age * 0.55f) * 0.18f * u * Math.min(1f, open);
            float wu = u * (1.4f + 0.25f * VfxAnim.easeOutCubic(sub(age, DET, DET + 3f)) * (1f - sub(age, 12f, 20f)));
            wing(buf, -1f, open, wingA, wu, flap * 1.4f, age);
            wing(buf, 1f, open, wingA, wu, flap * 1.4f, age);
        }

        // ---------------------------------------------------------------- the core: black-red sphere contracts, then detonates
        if (age < DET) {
            float k = VfxAnim.easeInCubic(sub(age, 0f, DET));
            float size = Mth.lerp(k, 4.2f * u, 1.0f * u);
            float in = sub(age, 0f, 1.5f);
            sprite(ctx, buf, ORB, VfxBlend.ALPHA, core, size, 0f, a(0xFF0B0310, 0.96f * in));
            sprite(ctx, buf, CORONA, VfxBlend.ADD, core, size * 1.9f, age * 0.25f, a(HELL, 0.85f * in));
            sprite(ctx, buf, SWIRL, VfxBlend.ADD, core, size * 1.5f, -age * 0.5f, a(CRIMSON, 0.7f * in));
            VfxBloom.glow(ctx, buf, core, size * 0.7f * (1f + 0.6f * k), VfxVertexBuffer.lerpColor(CRIMSON, HOT, k), 0.5f + 0.6f * k);
            for (int i = 0; i < 9; i++) {                                                        // embers dragged into the core
                float ph = (age * 0.1f + hash(seed, i, 1)) % 1f, ang = hash(seed, i, 2) * Mth.TWO_PI + ph * 2.4f, rr = (5.2f * (1f - ph) + 0.5f) * u * (1f - 0.35f * k);
                pe.set(core.x + Mth.cos(ang) * rr, core.y + (hash(seed, i, 3) - 0.5f) * 3f * u * (1f - ph), core.z + Mth.sin(ang) * rr);
                sprite(ctx, buf, EMBER, VfxBlend.ADD, pe, 0.7f * u, 0f, a(i % 3 == 0 ? HOT : HELL, Mth.sin(ph * Mth.PI) * 0.95f * in));
            }
        } else {
            float shell = VfxAnim.easeOutCubic(sub(dt, 0f, 5f));
            float live = 1f - sub(dt, 0f, 5.5f);
            if (live > 0f) {
                sprite(ctx, buf, ORB, VfxBlend.ALPHA, core, (1.0f + 11f * shell) * u, 0f, a(0xFF0B0310, 0.9f * live));
                sprite(ctx, buf, CORONA, VfxBlend.ADD, core, (2f + 13f * shell) * u, age * 0.4f, a(HELL, 0.9f * live));
            }
            float fl = 1f - sub(dt, 0f, 3.5f);
            if (fl > 0f) {
                sprite(ctx, buf, FLASH, VfxBlend.ADD, core, (5f + 9f * (1f - fl)) * u, age * 0.1f, a(BONE, 0.8f * fl));
                VfxBloom.glow(ctx, buf, core, 4.5f * u * (1f + 0.8f * (1f - fl)), HOT, 1.1f * fl);
            }
            float lv = 1f - sub(dt, 2f, 9f);
            if (lv > 0f) sprite(ctx, buf, SWIRL, VfxBlend.ADD, core, 3.5f * u, -age * 0.5f, a(CRIMSON, 0.6f * lv));
        }

        // ---------------------------------------------------------------- the hellfire ring, its wall of flame, the clawed lightning
        if (age >= DET) {
            float ex = VfxAnim.easeOutCubic(sub(dt, 0f, 6.5f));
            float ringFade = 1f - sub(dt, 4f, 13f);
            float rr = R * (0.12f + 0.93f * ex);
            ground(buf, RING, VfxBlend.ADD, g, 0.11f, rr, spin * 0.2f, a(HELL, 0.95f * ringFade));
            ground(buf, SHOCK, VfxBlend.ADD, g, 0.13f, rr * 0.86f, -spin * 0.3f, a(VIOLET, 0.8f * ringFade));
            wall(buf, c, rr, u, dt, ringFade, seed);
            claws(ctx, buf, c, core, R, u, dt, seed);
        }

        // ---------------------------------------------------------------- aftermath: smoke and rising embers
        if (age > DET + 1f) {
            float af = sub(age, DET + 1f, DET + 4f) * (1f - sub(age, 18f, 20f));
            for (int i = 0; i < 4; i++) {
                float ph = sub(age, DET + 1f + 0.5f * i, 20f), ang = hash(seed, i, 11) * Mth.TWO_PI, rr = R * (0.2f + 0.65f * hash(seed, i, 12));
                pe.set(c.x + Mth.cos(ang) * rr, c.y + (0.8f + 4.5f * ph) * u, c.z + Mth.sin(ang) * rr);
                sprite(ctx, buf, SMOKE, VfxBlend.ALPHA, pe, (3f + 2.5f * ph) * u, hash(seed, i, 13) * 6.28f + age * 0.05f, a(0xFF22102E, 0.75f * af * (1f - ph)));
            }
            for (int i = 0; i < 10; i++) {
                float ph = sub(age, DET + 0.4f * i, 20f), ang = hash(seed, i, 21) * Mth.TWO_PI, rr = R * 0.85f * hash(seed, i, 22);
                pe.set(c.x + Mth.cos(ang) * rr, c.y + (0.3f + 7f * ph) * u, c.z + Mth.sin(ang) * rr);
                sprite(ctx, buf, EMBER, VfxBlend.ADD, pe, (0.55f + 0.4f * hash(seed, i, 23)) * u, 0f, a(i % 4 == 0 ? HOT : HELL, Mth.sin(ph * Mth.PI) * 0.95f * af));
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ wings
    /**
     * One bat wing in the camera's standing plane: an arm from the shoulder to the wrist, five fingers fanning from the wrist, a dark membrane in
     * the scallops between the fingertips and down to the body; fingers and arm are hellfire blades, the tips carry bone-white claws.
     */
    private void wing(VfxVertexBuffer buf, float sgn, float open, float al, float u, float flap, float age) {
        float o = Math.max(0f, open);
        float d = 0.7f * u;                                                                         // the wing stands a little behind the caster
        float sx = sgn * 0.35f * u, sy = 1.55f * u;
        float bx = sgn * 0.3f * u, by = 0.65f * u;
        float wx = sgn * Mth.lerp(o, 0.7f, 3.7f) * u, wy = Mth.lerp(o, 3.3f, 5.5f) * u + flap;
        float[] tx = new float[5], ty = new float[5];
        for (int i = 0; i < 5; i++) {
            float ang = Mth.lerp(Math.min(o, 1.15f), FA_FOLD[i], FA_OPEN[i]) * Mth.DEG_TO_RAD;
            float len = FL[i] * u * (0.5f + 0.5f * Math.min(o, 1f)) * (1f + 0.04f * Mth.sin(age * 0.8f + i * 1.7f));
            tx[i] = wx + sgn * Mth.cos(ang) * len; ty[i] = wy + Mth.sin(ang) * len + (i >= 3 ? flap * 1.5f : 0f);
        }
        int mem = a(VfxVertexBuffer.lerpColor(VIOLET_BLACK, BLOOD, 0.45f), 0.88f * al), mem2 = a(VIOLET_BLACK, 0.92f * al);
        float du = 0.5f * u;
        // membrane: trailing sector to the body, then the four scalloped sectors
        w(pa, wx, wy, d); w(pb, tx[4], ty[4], d + 0.1f * u); w(pc, bx, by, d); w(pd, sx, sy, d);
        quad(buf, SOLID, VfxBlend.ALPHA, 0.3f, 0.3f, 0.7f, 0.7f, mem, mem2);
        for (int i = 0; i < 4; i++) {
            float mx = 0.5f * (tx[i] + tx[i + 1]), my = 0.5f * (ty[i] + ty[i + 1]);
            mx += (wx - mx) * 0.3f; my += (wy - my) * 0.3f;
            w(pa, wx, wy, d); w(pb, tx[i], ty[i], d + 0.05f * u); w(pc, mx, my, d + 0.05f * u); w(pd, tx[i + 1], ty[i + 1], d + 0.05f * u);
            quad(buf, SOLID, VfxBlend.ALPHA, 0.3f, 0.3f, 0.7f, 0.7f, mem, mem2);
        }
        int hot = a(HELL, 0.95f * al), cool = a(CRIMSON, 0.7f * al);
        bone(buf, sx, sy, wx, wy, 0.5f * u, 0.3f * u, d + 0.02f * u, hot, cool);                     // the arm
        for (int i = 0; i < 5; i++) bone(buf, wx, wy, tx[i], ty[i], 0.42f * u, 0.07f * u, d + 0.08f * u, hot, cool);
        if (buf.hasBudget(4) && al > 0.1f) {                                                       // bone-white claw tips on the two outer fingers
            for (int i = 0; i < 5; i += 4) {
                float ang = (float) Math.atan2(ty[i] - wy, (tx[i] - wx) * sgn);
                w(pe, tx[i], ty[i], d + 0.1f * u);
                // a small bone claw curling off the fingertip (flat in the wing plane)
                float s = 0.9f * u, ca = Mth.cos(ang), sa = Mth.sin(ang);
                float ux = sgn * ca, uy = sa, nx = -sgn * sa, ny = ca;
                w(pa, tx[i] - nx * s * 0.25f, ty[i] - ny * s * 0.25f, d + 0.1f * u); w(pb, tx[i] + nx * s * 0.25f, ty[i] + ny * s * 0.25f, d + 0.1f * u);
                w(pc, tx[i] + ux * s * 1.1f + nx * s * 0.1f, ty[i] + uy * s * 1.1f + ny * s * 0.1f, d + 0.1f * u); w(pd, tx[i] + ux * s * 1.1f - nx * s * 0.1f, ty[i] + uy * s * 1.1f - ny * s * 0.1f, d + 0.1f * u);
                quad(buf, PILLAR, VfxBlend.ADD, 0f, 0f, 1f, 1f, a(BONE, 0.9f * al), a(BONE, 0.6f * al));
            }
        }
    }

    /** A tapering blade of flame from (x0,y0) to (x1,y1) in the wing plane. */
    private void bone(VfxVertexBuffer buf, float x0, float y0, float x1, float y1, float w0, float w1, float d, int c0, int c1) {
        float dx = x1 - x0, dy = y1 - y0, l = Mth.sqrt(dx * dx + dy * dy);
        if (l < 1e-3f) return;
        float nx = -dy / l, ny = dx / l;
        w(pa, x0 - nx * w0 * 0.5f, y0 - ny * w0 * 0.5f, d); w(pb, x0 + nx * w0 * 0.5f, y0 + ny * w0 * 0.5f, d);
        w(pc, x1 + nx * w1 * 0.5f, y1 + ny * w1 * 0.5f, d); w(pd, x1 - nx * w1 * 0.5f, y1 - ny * w1 * 0.5f, d);
        quad(buf, PILLAR, VfxBlend.ADD, 0f, 0f, 1f, 1f, c0, c1);
    }

    // ------------------------------------------------------------------------------------------------ wall and claws
    /** The standing wall of hellfire on the rim of the ring: nine flame banners on the circle, each a chord facing the middle. */
    private void wall(VfxVertexBuffer buf, Vector3f c, float rr, float u, float dt, float fade, long seed) {
        if (fade <= 0.02f || rr < 0.5f) return;
        float hgt = (3.2f + 4.6f * VfxAnim.easeOutCubic(sub(dt, 0f, 3f))) * u * (1f - 0.6f * sub(dt, 6f, 13f));
        int n = 9;
        float half = rr * Mth.sin(Mth.PI / n) * 1.12f;
        for (int i = 0; i < n; i++) {
            float ang = Mth.TWO_PI * i / n + 0.35f + hash(seed, i, 31) * 0.08f;
            float x = c.x + Mth.cos(ang) * rr, z = c.z + Mth.sin(ang) * rr, ty = -Mth.sin(ang) * half, tz = Mth.cos(ang) * half;
            float hh = hgt * (0.8f + 0.4f * hash(seed, i, 32));
            pa.set(x - ty, c.y + 0.1f, z - tz); pb.set(x + ty, c.y + 0.1f, z + tz); pc.set(x + ty, c.y + hh, z + tz); pd.set(x - ty, c.y + hh, z - tz);
            float flip = (i & 1) == 0 ? 0f : 0.5f;
            quad(buf, WALL, VfxBlend.ADD, flip, 0f, flip + 0.5f, 1f, a(HELL, 0.95f * fade), a(CRIMSON, 0.0f));
        }
    }

    /** Clawed arcs of purple lightning raking round the ring on the ground, three bone-white rakes tearing across the burst. */
    private void claws(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f c, Vector3f core, float R, float u, float dt, long seed) {
        float live = 1f - sub(dt, 3f, 10f);
        if (live <= 0.02f) return;
        float ex = VfxAnim.easeOutCubic(sub(dt, 0f, 5f));
        for (int i = 0; i < 3; i++) {                                                         // raking round the ground
            float rr = R * (0.25f + 0.7f * ex) * (0.55f + 0.17f * i + 0.1f * hash(seed, i, 41));
            float start = hash(seed, i, 42) * Mth.TWO_PI + dt * (i % 2 == 0 ? 0.35f : -0.3f);
            VfxPose pose = VfxPose.ground(c).lift(0.3f + 0.06f * i);
            buf.arc(BOLT, VfxBlend.ADD, pose, rr, (2.8f + 0.6f * i) * u, start, 1.3f + 0.2f * (i % 2), 4, a(i % 2 == 0 ? VIOLET : 0xFFD8A8FF, live));
        }
        for (int i = 0; i < 2; i++) {                                                         // and two rakes up through the burst, in tilted planes
            float yaw = hash(seed, i, 46) * Mth.TWO_PI + i * 2.2f;
            pe.set(Mth.cos(yaw) * 0.55f, 0.8f, Mth.sin(yaw) * 0.55f);
            VfxPose pose = VfxPose.facing(core, pe).spin(dt * 0.5f * (i == 0 ? 1f : -1f));
            buf.arc(BOLT, VfxBlend.ADD, pose, (3.5f + 2.5f * ex + 1.5f * i) * u, 2.6f * u, 0.4f + 2f * i, 1.7f, 4, a(i == 0 ? 0xFFC890FF : VIOLET, live));
        }
        float rk = 1f - sub(dt, 0f, 4.5f);
        if (rk > 0f) {
            for (int i = 0; i < 3; i++) {
                float rot = (i - 1) * 0.75f + 0.5f * hash(seed, i, 43);
                pe.set(core.x + (hash(seed, i, 44) - 0.5f) * 3f * u, core.y + (hash(seed, i, 45) - 0.5f) * 3f * u, core.z);
                sprite(ctx, buf, CLAW, VfxBlend.ADD, pe, (7f + 4f * (1f - rk)) * u, rot, a(i == 1 ? BONE : 0xFFE0B8FF, 0.95f * rk));
            }
        }
    }
}
