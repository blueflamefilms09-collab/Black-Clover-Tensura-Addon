package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * 3.29.0 Ultimate "Chairman's Last Stand" (Solo Leveling, Chairman Go Gunhee): brilliant gold-white light and thunder, an old warrior's last charge. No shadow, no violet: gold, white,
 * warm amber and a touch of deep blue storm. 'from' = the caster's feet, {@code inst.power} = the burst radius, 20 ticks.
 * <ul>
 *   <li>ticks 0-6, the weight gathers: a guild seal of gold lights up on the ground and tightens, a blue storm rim under it, motes of light are drawn in from the edge, a thin gold
 *       column starts to rise at the feet.</li>
 *   <li>ticks 6-12, the charge lands: a heavy white impact flash, golden lances of light rush out from the feet over the ground (one long main lance, the rest shorter), a thunderclap
 *       ring rolls out to the full radius, the ground cracks into a crater, gold lightning drops on the ring, a pillar of light stands over the feet.</li>
 *   <li>ticks 12-20, what is left: the lances thin and tear off, the ring and wall fade, dust and sparks fall, the cracks and the guild seal stay golden on the ground and fade last.</li>
 * </ul>
 * Everything is a soft billboard, a camera-facing ribbon, a ground plane or no flat slabs. Textures are the shared white ones, tinted here. About 370 vertices of 400.
 */
public class UltChairmanStandLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    private static final ResourceLocation SEAL = t("seal_sigil"), SEAL_RINGS = t("seal_rings"), JUDG_SIGIL = t("judgment_sigil"), HALO = t("bfx_halo"),
            RING = t("lightningfx_ring"), SOUND_SHOCK = t("sound_shock"), CRACK = t("bfx_ground_crack"), CRACK2 = t("hercules_cracks"),
            BEAM = t("judgment_beam"), SOLO_BEAM = t("solomon_beam"), STREAK = t("light_streak"), BOLT = t("lightningfx_bolt"), BOLT2 = t("grand_zone_bolt"),
            FLASH = t("lightningfx_flash"), FLARE = t("hercules_flash"), SPARK = t("lightningfx_spark"), DUST = t("earth_dust"), STAR = t("light_star");

    private static final int GOLD = 0xFFFFC53A, PALE = 0xFFFFEDB0, WHITE = 0xFFFFFFFF, AMBER = 0xFFFF9424, BLUE = 0xFF2F52E0, DEEP = 0xFF101C66, ICE = 0xFF9DB8FF, DUSTC = 0xFFB8935A;

    private static final int LANCES = 8, BOLTS = 7, MOTES = 10, DUSTS = 5;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.ULT_CHAIRMAN_STAND); }
    @Override public int defaultDuration(VfxShape s) { return 20; }
    @Override public int defaultColor(VfxShape s) { return 0xFFFFC53A; }

    @Override
    public void onTick(VfxInstance inst) {
        if (inst.age() == 6) VfxShake.add(inst.payload.from(), 0.9f, 10);                       // one mild thump on the landing, nothing more
    }

    private static float hash(long seed, int i, int k) {
        long x = seed * 0x9E3779B97F4A7C15L + i * 0xBF58476D1CE4E5B9L + k * 0x94D049BB133111EBL;
        x ^= x >>> 30; x *= 0xBF58476D1CE4E5B9L; x ^= x >>> 27; x *= 0x94D049BB133111EBL; x ^= x >>> 31;
        return (x >>> 40) / (float) (1 << 24);
    }

    private static int a(int argb, float alpha) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(alpha, 0f, 1f)); }
    private static float cl(float v) { return Mth.clamp(v, 0f, 1f); }
    private static float sub(float v, float s, float e) { return cl((v - s) / Math.max(1e-4f, e - s)); }
    private static float out(float x) { return VfxAnim.easeOutCubic(cl(x)); }

    private static void sprite(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, float x, float y, float z, float size, float rot, int col) {
        if (buf.hasBudget(4)) buf.billboard(ctx, tex, bl, new Vector3f(x, y, z), size, rot, col);
    }

    private static void ground(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, Vector3f c, float y, float half, float spin, int col) {
        if (half <= 0.01f || !buf.hasBudget(4)) return;
        buf.plane(tex, bl, VfxPose.ground(new Vector3f(c.x, c.y + y, c.z)).spin(spin), half, col);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        final float age = inst.ageTicks(ctx.partialTick);
        final float R = Mth.clamp(inst.power, 4f, 24f);
        final long seed = inst.seed;
        final Vector3f c = ctx.rel(inst.from(ctx));
        final float cx = c.x, cy = c.y, cz = c.z;
        final float dir0 = hash(seed, 0, 0) * Mth.TWO_PI;

        final float build = sub(age, 0f, 6f), peak = sub(age, 6f, 12f), tail = sub(age, 12f, 20f);
        final float landed = sub(age, 6f, 7.5f);                                                // 0 before the hit, 1 right after
        final float flick = 0.78f + 0.22f * Mth.sin(age * 2.9f + hash(seed, 1, 1) * 6f);        // thunder flicker, same on every client

        // ---- the ground: crater scorch (dark, so the gold has something to read against), blue storm rim, gold floor light
        float scorch = landed * (1f - sub(age, 15f, 20f));
        ground(buf, VfxTextures.GLOW, VfxBlend.ALPHA, c, 0.03f, R * 0.62f * (0.6f + 0.4f * out(sub(age, 6f, 11f))), 0f, a(DEEP, 0.55f * scorch));
        float lightIn = sub(age, 0f, 5f) * (1f - sub(age, 13f, 20f));
        ground(buf, VfxTextures.GLOW, VfxBlend.ADD, c, 0.05f, R * (0.55f + 0.5f * peak), 0f, a(GOLD, 0.42f * lightIn + 0.3f * landed * (1f - peak)));
        ground(buf, HALO, VfxBlend.ADD, c, 0.07f, R * (1.28f - 0.18f * out(build)), age * 0.02f, a(BLUE, 0.5f * sub(age, 0f, 4f) * (1f - sub(age, 11f, 18f))));

        // ---- the guild seal: lights up and tightens in the build-up, turns, stays golden to the end
        float sealA = sub(age, 0f, 4f) * (1f - sub(age, 16f, 20f) * 0.9f);
        float sealR = R * (1.18f - 0.2f * out(build)) * 0.8f;
        ground(buf, SEAL_RINGS, VfxBlend.ADD, c, 0.09f, sealR, age * 0.045f, a(GOLD, 0.8f * sealA));
        ground(buf, SEAL, VfxBlend.ADD, c, 0.1f, sealR * 0.72f, -age * 0.07f, a(PALE, 0.7f * sealA * (0.75f + 0.25f * landed)));
        ground(buf, JUDG_SIGIL, VfxBlend.ADD, c, 0.11f, sealR * 0.38f, age * 0.1f, a(WHITE, 0.55f * sealA * landed));

        // ---- cracks of the crater: split open on the hit, stay
        float crackA = landed * (1f - sub(age, 16f, 20f) * 0.85f);
        float crackR = R * (0.28f + 0.3f * out(sub(age, 6f, 12f)));
        ground(buf, CRACK, VfxBlend.ADD, c, 0.12f, crackR, dir0, a(GOLD, 0.95f * crackA));
        ground(buf, CRACK2, VfxBlend.ADD, c, 0.13f, crackR * 0.9f, dir0 + 1.3f, a(AMBER, 0.85f * crackA));

        // ---- thunderclap rings: the first white-gold and fast, the second warm and a beat late, a jagged one of lightning between
        float r1 = out(sub(age, 5.5f, 12.5f)), r2 = out(sub(age, 7f, 15f)), r3 = out(sub(age, 6.2f, 13.5f));
        ground(buf, SOUND_SHOCK, VfxBlend.ADD, c, 0.16f, R * 1.08f * r1 + 0.1f, 0f, a(PALE, 0.7f * landed * (1f - sub(age, 11f, 16f))));
        ground(buf, HALO, VfxBlend.ADD, c, 0.18f, R * 0.92f * r2 + 0.1f, 0.4f, a(AMBER, 0.85f * landed * (1f - sub(age, 13f, 19f))));
        ground(buf, RING, VfxBlend.ADD, c, 0.2f, R * 1.0f * r3 + 0.1f, age * 0.12f, a(WHITE, 0.6f * landed * (1f - sub(age, 11f, 17f)) * flick));

        // ---- dust thrown up by the ring (alpha, so it sits dark against the light)
        float dustA = landed * (1f - sub(age, 12f, 20f));
        for (int k = 0; k < DUSTS; k++) {
            float ang = dir0 + Mth.TWO_PI * k / DUSTS + hash(seed, k, 30) * 0.8f, d = R * (0.5f + 0.45f * hash(seed, k, 31)) * out(sub(age, 6.5f, 14f));
            float rise = 0.4f + (1.2f + 1.6f * hash(seed, k, 32)) * sub(age, 7f, 20f);
            sprite(ctx, buf, DUST, VfxBlend.ALPHA, cx + Mth.cos(ang) * d, cy + rise, cz + Mth.sin(ang) * d, R * (0.16f + 0.1f * hash(seed, k, 33)), hash(seed, k, 34) * 6f, a(DUSTC, 0.5f * dustA));
        }

        // ---- the lances: golden light running out from the feet in a straight rush, a wide soft body and a white core, a head flare on the point
        for (int k = 0; k < LANCES; k++) {
            float ang = dir0 + (k == 0 ? 0f : Mth.TWO_PI * k / LANCES + (hash(seed, k, 40) - 0.5f) * 0.35f);
            float lenF = k == 0 ? 1.0f : 0.62f + 0.22f * hash(seed, k, 41);
            float startT = 5.5f + (k == 0 ? 0f : 0.6f + 0.5f * hash(seed, k, 42));
            float head = R * lenF * out(sub(age, startT, startT + 5.5f));
            float tl = R * lenF * VfxAnim.easeInCubic(sub(age, startT + 2.5f, startT + 9.5f)) * 0.95f;
            float fade = 1f - sub(age, startT + 5f, startT + 11f);
            if (head <= 0.2f || fade <= 0.02f || head - tl < 0.2f) continue;
            float dx = Mth.cos(ang), dz = Mth.sin(ang), h = 0.55f + (k == 0 ? 0.25f : 0f) + 0.2f * Mth.sin(age * 0.5f + k);
            Vector3f p0 = new Vector3f(cx + dx * tl, cy + h, cz + dz * tl), p1 = new Vector3f(cx + dx * head, cy + h, cz + dz * head);
            float wd = k == 0 ? 1.7f : 1.0f;
            buf.beam(ctx, SOLO_BEAM, VfxBlend.ADD, p0, p1, wd * 0.5f, wd * 1.7f, 2, 0f, a(AMBER, 0.55f * fade), a(GOLD, 0.9f * fade));
            buf.beam(ctx, BEAM, VfxBlend.ADD, p0, p1, wd * 0.18f, wd * 0.5f, 2, age * 0.04f, a(WHITE, 0.4f * fade), a(WHITE, 0.95f * fade));
            sprite(ctx, buf, FLARE, VfxBlend.ADD, p1.x, p1.y, p1.z, wd * 1.4f * (0.6f + 0.4f * fade), age * 0.2f + k, a(PALE, 0.9f * fade));
        }

        // ---- gold thunder drops on the ring: blue-white halo of the bolt behind, gold core in front, a spark where it lands
        for (int k = 0; k < BOLTS; k++) {
            float start = 6f + 3.2f * hash(seed, k, 50);
            float life = sub(age, start, start + 0.5f) * (1f - sub(age, start + 2f, start + 4.5f));
            if (life <= 0.02f) continue;
            float ang = dir0 + 0.4f + Mth.TWO_PI * k / BOLTS + (hash(seed, k, 51) - 0.5f) * 0.5f, d = R * (0.3f + 0.65f * hash(seed, k, 52));
            float fl = (((int) (age * 2.2f) + k) & 1) == 0 ? 1f : 0.7f;
            float bx = cx + Mth.cos(ang) * d, bz = cz + Mth.sin(ang) * d, hgt = 8.5f + 3f * hash(seed, k, 53);
            sprite(ctx, buf, k % 2 == 0 ? BOLT : BOLT2, VfxBlend.ADD, bx, cy + hgt * 0.5f, bz, hgt * 1.02f, 0f, a(BLUE, 0.55f * life * fl));
            sprite(ctx, buf, k % 2 == 0 ? BOLT : BOLT2, VfxBlend.ADD, bx, cy + hgt * 0.5f, bz, hgt * 0.84f, 0f, a(k % 3 == 0 ? WHITE : GOLD, 0.95f * life * fl));
            sprite(ctx, buf, FLASH, VfxBlend.ADD, bx, cy + 0.4f, bz, 2.6f * life, age, a(PALE, 0.9f * life));
        }

        // ---- the pillar of light over the feet: thin at first, swells on the hit, goes out upward
        float pillarH = (2f + 14f * out(sub(age, 2f, 9f))) * (1f - 0.5f * sub(age, 12f, 20f));
        float pillarA = sub(age, 1.5f, 5f) * (1f - sub(age, 11f, 18f));
        float pw = (0.8f + 2.8f * landed) * (1f - 0.5f * sub(age, 12f, 18f));
        if (pillarA > 0.02f) {
            Vector3f b0 = new Vector3f(cx, cy + 0.05f, cz), b1 = new Vector3f(cx, cy + pillarH, cz);
            buf.beam(ctx, SOLO_BEAM, VfxBlend.ADD, b0, b1, pw * 1.5f, pw * 0.7f, 2, age * 0.05f, a(GOLD, 0.65f * pillarA), a(AMBER, 0.0f));
            buf.beam(ctx, BEAM, VfxBlend.ADD, b0, b1, pw * 0.55f, pw * 0.25f, 2, -age * 0.07f, a(WHITE, 0.95f * pillarA), a(PALE, 0.1f * pillarA));
        }

        // ---- the heavy hit: a white flash at the feet with a star of rays, then a lingering warm glow
        float hit = (1f - sub(age, 6f, 10f)) * sub(age, 5f, 6.3f);
        if (hit > 0.02f) {
            VfxBloom.glow(ctx, buf, new Vector3f(cx, cy + 0.9f, cz), R * 0.34f * (0.8f + 0.5f * (1f - hit)), PALE, hit);
            sprite(ctx, buf, FLASH, VfxBlend.ADD, cx, cy + 0.9f, cz, R * (0.5f + 0.5f * (1f - hit)), dir0 + age * 0.08f, a(WHITE, 0.95f * hit));
            sprite(ctx, buf, STAR, VfxBlend.ADD, cx, cy + 0.9f, cz, R * 0.95f * (0.6f + 0.4f * (1f - hit)), -dir0, a(GOLD, 0.8f * hit));
        }
        float ember = landed * (1f - sub(age, 9f, 17f));
        sprite(ctx, buf, VfxTextures.GLOW, VfxBlend.ADD, cx, cy + 1.2f, cz, R * 0.55f, 0f, a(AMBER, 0.5f * ember));

        // ---- motes of light: drawn in to the feet while the weight gathers, then flung out and up and falling away as sparks
        for (int k = 0; k < MOTES; k++) {
            float ang = hash(seed, k, 60) * Mth.TWO_PI + age * 0.12f * (k % 2 == 0 ? 1 : -1);
            float x, y, z, al, sz;
            if (age < 6f) {
                float d = R * (0.95f - 0.8f * VfxAnim.easeInCubic(build)) * (0.7f + 0.3f * hash(seed, k, 61));
                x = cx + Mth.cos(ang) * d; z = cz + Mth.sin(ang) * d; y = cy + 0.3f + 1.6f * build * hash(seed, k, 62);
                al = 0.9f * sub(age, 0f, 1.5f); sz = 0.45f;
            } else {
                float f = sub(age, 6f, 20f), d = R * (0.1f + 0.7f * hash(seed, k, 63)) * out(f * 1.4f);
                x = cx + Mth.cos(ang) * d; z = cz + Mth.sin(ang) * d;
                y = cy + 0.4f + (2f + 5f * hash(seed, k, 64)) * out(f * 1.6f) - 3f * f * f * hash(seed, k, 65);
                al = 0.95f * (1f - sub(age, 13f, 20f)); sz = 0.5f * (1f - 0.4f * f);
            }
            sprite(ctx, buf, SPARK, VfxBlend.ADD, x, y, z, sz + 0.2f * hash(seed, k, 66), 0f, a(k % 3 == 0 ? WHITE : GOLD, al));
        }
    }
}
