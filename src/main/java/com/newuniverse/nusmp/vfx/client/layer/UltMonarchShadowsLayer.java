package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * 3.29.0 Ultimate "Monarch of Shadows" (Ashborn, the Shadow Monarch): the king takes his throne and cashes in every grudge as a blast of dark light.
 * Power = radius (12-13), 20 ticks, drawn at the caster's feet. Palette: abyss black, royal violet, electric purple-blue edge light, a few white sparks.
 * <ul>
 *   <li>Ticks 0-6 (build-up): a dark pool and the Monarch's sigil spread over the ground, two sigils counter-turning; ranks of upright shadow soldiers
 *       (flat dark figures with a violet eye each) rise out of the floor in two rings (12 outside, 6 inside, every other one a taller knight); over the
 *       caster's head a crown of seven dark spikes forms, tall and short in turn, with a violet edge light and a halo.</li>
 *   <li>Ticks 6-12 (peak): a pillar of dark light with a white core punches up through the crown, a white-violet flash, two shock rings run out over the
 *       ground to the full radius, cracks and eight forked purple lightning bolts crawl along the ground, four more arc round the pillar, the soldiers' eyes
 *       flare and white sparks fly out.</li>
 *   <li>Ticks 12-20 (aftermath): the pillar thins and goes, the soldiers sink and come apart into black motes that drift up, the crown and the sigils fade.</li>
 * </ul>
 * Everything is a constant count: at most 380 vertices of 400, no allocation per frame beyond four reused vectors, deterministic from seed and age.
 */
public class UltMonarchShadowsLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    private static final ResourceLocation UMBRA = t("bfx_umbra"), SIGIL_A = t("curse_sigil"), SIGIL_B = t("judgment_sigil"),
            SHOCK = t("darkfx_shock"), SPIKY_RING = t("curse_ring"), CRACK = t("bfx_ground_crack"), SPIKE = t("bone_spike"), HALO = t("bfx_halo"),
            CORONA = t("darkfx_corona"), FLARE = t("darkfx_flare"), PILLAR = t("solomon_pillar"), CORE = t("bfx_pillar"), BOLT = t("lightningfx_bolt"),
            MOTE = t("darkfx_mote"), GLOW = VfxTextures.GLOW;

    private static final int ABYSS = 0xFF06020E, ABYSS_RIM = 0xFF1A0B33, VIOLET = 0xFF7A2BE8, EDGE = 0xFF7E86FF, WHITE = 0xFFF2EAFF;

    private static final int OUTER = 10, INNER = 4, SPIKES = 7, GROUND_BOLTS = 8, SKY_BOLTS = 3, SPARKS = 4, MOTES = 8;

    private final Vector3f p0 = new Vector3f(), p1 = new Vector3f(), p2 = new Vector3f(), p3 = new Vector3f(), tmp = new Vector3f();

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.ULT_MONARCH_SHADOWS); }
    @Override public int defaultDuration(VfxShape s) { return 20; }
    @Override public int defaultColor(VfxShape s) { return 0xFF7A2BE8; }

    @Override
    public void onSpawn(VfxInstance inst) {
        VfxShake.add(inst.payload.from(), 0.55f, 12);                 // mild: the hit at tick 6 only, falls off with distance
    }

    private static float hash(long seed, int i, int k) {
        long x = seed * 0x9E3779B97F4A7C15L + i * 0xBF58476D1CE4E5B9L + k * 0x94D049BB133111EBL;
        x ^= x >>> 30; x *= 0xBF58476D1CE4E5B9L; x ^= x >>> 27; x *= 0x94D049BB133111EBL; x ^= x >>> 31;
        return (x >>> 40) / (float) (1 << 24);
    }

    private static int a(int argb, float alpha) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(alpha, 0f, 1f)); }
    private static float cl(float v) { return Mth.clamp(v, 0f, 1f); }
    private static float sub(float v, float s, float e) { return cl((v - s) / Math.max(1e-4f, e - s)); }
    private static float out(float v) { return VfxAnim.easeOutCubic(cl(v)); }

    /** Camera-facing flat figure standing on 'base' (turned round the vertical axis only): width w, height h, the lower 'show' part of the texture. */
    private void stand(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, float bx, float by, float bz, float w, float h, float show, int col) {
        if (show <= 0.01f || !buf.hasBudget(4)) return;
        float rx = ctx.camRight.x, rz = ctx.camRight.z, l = Mth.sqrt(rx * rx + rz * rz);
        if (l < 1e-4f) { rx = 1f; rz = 0f; l = 1f; }
        rx = rx / l * w * 0.5f; rz = rz / l * w * 0.5f;
        float hh = h * show;
        p0.set(bx - rx, by, bz - rz); p1.set(bx + rx, by, bz + rz); p2.set(bx + rx, by + hh, bz + rz); p3.set(bx - rx, by + hh, bz - rz);
        int c = bl.grade(col, 1f);
        buf.quad(tex, bl, p0, p1, p2, p3, 0f, 1f - show, 1f, 1f, c, c);
    }

    /** A flat strip on the ground (or any plane) from (ax,ay,az) to (bx,by,bz), 'w' wide, texture V running along it. */
    private void strip(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, float ax, float ay, float az, float bx, float by, float bz, float w, float sx, float sy, float sz, int col) {
        if (!buf.hasBudget(4)) return;
        float h = w * 0.5f;
        p0.set(ax - sx * h, ay - sy * h, az - sz * h); p1.set(ax + sx * h, ay + sy * h, az + sz * h);
        p2.set(bx + sx * h, by + sy * h, bz + sz * h); p3.set(bx - sx * h, by - sy * h, bz - sz * h);
        int c = bl.grade(col, 1f);
        buf.quad(tex, bl, p0, p1, p2, p3, 0f, 1f, 1f, 0f, c, c);
    }

    private void flat(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, Vector3f c, float r, float spin, int col) {
        if (r > 0.05f && buf.hasBudget(4)) buf.plane(tex, bl, VfxPose.ground(c).spin(spin), r, col);
    }

    private void bill(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, float x, float y, float z, float size, float rot, int col) {
        if (size > 0.02f && buf.hasBudget(4)) { tmp.set(x, y, z); buf.billboard(ctx, tex, bl, tmp, size, rot, col); }
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float R = Math.max(2f, inst.power);
        long seed = inst.seed;
        Vector3f c = ctx.rel(inst.from(ctx));
        float gy = c.y + 0.03f;
        Vector3f gc = new Vector3f(c.x, gy, c.z);

        float open = out(age / 5f);                                                   // the ground spreads
        float ground = open * (1f - sub(age, 15f, 20f));                              // and goes
        float flash = sub(age, 5.2f, 6.2f) * (1f - sub(age, 6.2f, 10f));              // the hit
        float live = 1f - sub(age, 13f, 20f);                                         // the crown and the pillar leave
        float charge = sub(age, 0f, 6f);

        // ---------------------------------------------------------------- the ground: dark pool, violet light under it, the sigils
        if (ground > 0.01f) {
            flat(buf, UMBRA, VfxBlend.ALPHA, gc, R * 1.12f * open, 0f, a(ABYSS, 0.8f * ground));
            flat(buf, GLOW, VfxBlend.ADD, gc, R * 1.15f * open, 0f, a(VIOLET, (0.30f + 0.35f * flash + 0.1f * charge) * ground));
            flat(buf, SIGIL_A, VfxBlend.ADD, new Vector3f(c.x, gy + 0.01f, c.z), R * 0.98f * open, age * 0.035f, a(VIOLET, (0.55f + 0.4f * flash) * ground));
            flat(buf, SIGIL_B, VfxBlend.ADD, new Vector3f(c.x, gy + 0.02f, c.z), R * 0.62f * open, -age * 0.06f, a(EDGE, (0.5f + 0.45f * flash) * ground));
            flat(buf, SPIKY_RING, VfxBlend.ADD, new Vector3f(c.x, gy + 0.015f, c.z), R * 1.02f * open, 0f, a(EDGE, 0.55f * ground));
        }

        // ---------------------------------------------------------------- the ranks of shadow soldiers (flat dark figures, a violet eye each)
        int total = OUTER + INNER;
        for (int i = 0; i < total; i++) {
            boolean outer = i < OUTER;
            int j = outer ? i : i - OUTER;
            int n = outer ? OUTER : INNER;
            float ang = Mth.TWO_PI * j / n + (outer ? 0.12f : 0.5f) + (hash(seed, i, 1) - 0.5f) * 0.18f;
            float rad = R * (outer ? 0.84f : 0.5f) * (0.96f + 0.08f * hash(seed, i, 2));
            float start = (outer ? 0.4f : 1.4f) + 2.4f * hash(seed, i, 3);
            float rise = out(sub(age, start, start + 3.2f));
            float d0 = 12f + 5f * hash(seed, i, 4);
            float dis = sub(age, d0, d0 + 3f);
            float show = rise * (1f - 0.55f * dis);
            float h = (outer && (i & 1) == 0 ? 4.6f : 3.5f) * (0.95f + 0.1f * hash(seed, i, 5));
            float sx = c.x + Mth.cos(ang) * rad, sz = c.z + Mth.sin(ang) * rad;
            float alpha = 0.97f * (1f - dis);
            if (alpha <= 0.02f) continue;
            stand(ctx, buf, UMBRA, VfxBlend.ALPHA, sx, gy - 0.1f, sz, h * 0.6f, h * 0.8f * show, 1f, a(ABYSS, alpha));          // the cloaked body: a stretched soft shadow
            stand(ctx, buf, SPIKE, VfxBlend.ALPHA, sx, gy + h * 0.66f * show, sz, h * 0.24f, h * 0.4f * show, 1f, a(ABYSS, alpha));  // the pointed hood
            float eye = rise * (0.45f + 0.55f * Math.max(flash, 0.35f * charge)) * (1f - dis);
            if (eye > 0.05f) bill(ctx, buf, FLARE, VfxBlend.ADD, sx, gy + h * show * 0.74f, sz, 0.9f + 1.2f * flash, 0f, a(0xFFD8C8FF, eye));
            if (outer && eye > 0.05f) flat(buf, GLOW, VfxBlend.ADD, new Vector3f(sx, gy + 0.04f, sz), 1.5f + 0.8f * flash, 0f, a(VIOLET, 0.8f * eye));
        }

        // ---------------------------------------------------------------- the crown of dark spikes over the caster
        float cy = c.y + 2.9f;
        if (live > 0.02f) {
            float formed = sub(age, 2f, 7f);
            bill(ctx, buf, CORONA, VfxBlend.ADD, c.x, cy + 1.0f, c.z, (5f + 4f * flash) * formed, age * 0.02f, a(VIOLET, (0.45f + 0.4f * flash) * live));
            flat(buf, HALO, VfxBlend.ADD, new Vector3f(c.x, cy - 0.05f, c.z), (2.4f + 0.4f * flash) * formed, age * 0.08f, a(EDGE, 0.7f * live * formed));
            for (int k = 0; k < SPIKES; k++) {
                float st = 2.2f + 0.45f * k;
                float g = VfxAnim.easeOutBack(sub(age, st, st + 3f));
                if (g <= 0.02f) continue;
                float phi = Mth.TWO_PI * k / SPIKES + 0.3f;
                float bx = c.x + Mth.cos(phi) * 1.5f, bz = c.z + Mth.sin(phi) * 1.5f;
                float sh = (k % 2 == 0 ? 4.2f : 2.7f) * Math.min(1.1f, g), by = cy + (1f - cl(g)) * 1.6f;
                stand(ctx, buf, SPIKE, VfxBlend.ALPHA, bx, by, bz, 0.95f, sh, 1f, a(ABYSS_RIM, 0.98f * live));
                stand(ctx, buf, SPIKE, VfxBlend.ADD, bx, by, bz, 1.08f, sh * 1.03f, 1f, a(EDGE, (0.16f + 0.4f * flash) * live));
            }
            if (flash > 0.02f) VfxBloom.glow(ctx, buf, tmp.set(c.x, cy + 1.2f, c.z), 2.2f, VIOLET, flash * live);
        }

        // ---------------------------------------------------------------- the pillar of dark light, the flash, the shock rings
        float pw = sub(age, 5.8f, 7f) * (1f - sub(age, 10.5f, 16f));
        if (pw > 0.02f) {
            float wob = 1f + 0.08f * Mth.sin(age * 2.3f);
            stand(ctx, buf, PILLAR, VfxBlend.ADD, c.x, gy, c.z, 3.4f * pw * wob, 17f, 1f, a(VIOLET, 0.9f * pw));
            stand(ctx, buf, CORE, VfxBlend.ADD, c.x, gy, c.z, 1.25f * pw, 17f, 1f, a(WHITE, 0.85f * pw));
        }
        if (flash > 0.02f) bill(ctx, buf, FLARE, VfxBlend.ADD, c.x, gy + 0.9f, c.z, 7f * flash + 2f, 0f, a(WHITE, flash));
        for (int k = 0; k < 2; k++) {
            float rp = sub(age, 6f + 0.9f * k, 13.5f + 0.9f * k);
            if (rp <= 0f || rp >= 1f) continue;
            float rr = R * (k == 0 ? 1.12f : 0.95f) * out(rp);
            flat(buf, SHOCK, VfxBlend.ADD, new Vector3f(c.x, gy + 0.03f + 0.01f * k, c.z), rr * 1.08f, k * 0.5f, a(k == 0 ? EDGE : VIOLET, (1f - rp) * (k == 0 ? 0.95f : 0.8f)));
        }
        float cr = sub(age, 6f, 8f) * (1f - sub(age, 11f, 18f));
        flat(buf, CRACK, VfxBlend.ADD, new Vector3f(c.x, gy + 0.02f, c.z), R * 0.9f * out(sub(age, 6f, 9f)), 0.4f, a(VIOLET, 0.85f * cr));

        // ---------------------------------------------------------------- purple lightning: forked bolts along the ground and round the pillar
        float lb = sub(age, 6f, 8.5f) * (1f - sub(age, 11f, 15f));
        if (lb > 0.02f) {
            int fl = (int) (age * 0.9f);                                                          // the bolts jump between a few poses
            for (int k = 0; k < GROUND_BOLTS; k++) {
                float ang = Mth.TWO_PI * k / GROUND_BOLTS + (hash(seed, k + fl * 7, 6) - 0.5f) * 0.45f;
                float len = R * (0.55f + 0.45f * hash(seed, k, 7)) * out(sub(age, 6f, 9.5f));
                float ca = Mth.cos(ang), sa = Mth.sin(ang);
                strip(buf, BOLT, VfxBlend.ADD, c.x + ca * 1.2f, gy + 0.06f, c.z + sa * 1.2f, c.x + ca * len, gy + 0.06f, c.z + sa * len, 1.7f, -sa, 0f, ca,
                        a(k % 2 == 0 ? EDGE : 0xFFB070FF, lb));
            }
            for (int k = 0; k < SKY_BOLTS; k++) {
                float ang = Mth.TWO_PI * k / SKY_BOLTS + 0.8f + hash(seed, k + fl * 5, 8);
                float ca = Mth.cos(ang), sa = Mth.sin(ang);
                float rx = ctx.camRight.x, rz = ctx.camRight.z, l = Mth.sqrt(rx * rx + rz * rz);
                if (l < 1e-4f) { rx = 1f; rz = 0f; l = 1f; }
                rx /= l; rz /= l;
                strip(buf, BOLT, VfxBlend.ADD, c.x + ca * 1.3f, gy + 0.2f, c.z + sa * 1.3f, c.x + ca * 3.2f, gy + 9f, c.z + sa * 3.2f, 1.5f, rx, 0f, rz, a(WHITE, 0.9f * lb));
            }
        }

        // ---------------------------------------------------------------- white sparks thrown out, black motes of the dissolving soldiers
        float sp = sub(age, 6f, 14f);
        if (sp > 0f && sp < 1f) {
            for (int k = 0; k < SPARKS; k++) {
                float ang = Mth.TWO_PI * (k + hash(seed, k, 9)) / SPARKS, r = R * (0.2f + 0.7f * out(sp)) * (0.6f + 0.5f * hash(seed, k, 10));
                bill(ctx, buf, FLARE, VfxBlend.ADD, c.x + Mth.cos(ang) * r, gy + 0.5f + 2.2f * sp * hash(seed, k, 11) + 0.6f * Mth.sin(sp * 6f + k), c.z + Mth.sin(ang) * r,
                        0.55f * (1f - sp), 0f, a(WHITE, 0.95f * (1f - sp)));
            }
        }
        for (int k = 0; k < MOTES; k++) {
            int s = k < 4 ? k * 2 + 1 : OUTER + (k - 4);
            float d0 = 12f + 5f * hash(seed, s, 4);
            float dd = sub(age, d0, d0 + 7f);
            if (dd <= 0f || dd >= 1f) continue;
            boolean outer = s < OUTER;
            int j = outer ? s : s - OUTER, n = outer ? OUTER : INNER;
            float ang = Mth.TWO_PI * j / n + (outer ? 0.12f : 0.5f) + (hash(seed, s, 1) - 0.5f) * 0.18f;
            float rad = R * (outer ? 0.84f : 0.5f) * (0.96f + 0.08f * hash(seed, s, 2));
            float mx = c.x + Mth.cos(ang) * rad + (hash(seed, k, 12) - 0.5f) * 1.2f * dd, mz = c.z + Mth.sin(ang) * rad + (hash(seed, k, 13) - 0.5f) * 1.2f * dd;
            bill(ctx, buf, MOTE, VfxBlend.ALPHA, mx, gy + 1.2f + 3.6f * dd, mz, 0.9f * (1f - 0.5f * dd), hash(seed, k, 14) * 6.28f + age * 0.1f, a(0xFFC8B0FF, 0.9f * (1f - dd)));
        }
    }
}
