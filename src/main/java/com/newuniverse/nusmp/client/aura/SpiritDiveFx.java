package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.book.SpiritDivePieces;
import com.newuniverse.nusmp.book.SpiritDivePieces.Element;
import com.newuniverse.nusmp.book.SpiritDivePieces.Stage;
import com.newuniverse.nusmp.book.SpiritDiveTier;
import com.newuniverse.nusmp.book.SpiritDiveTier.Budget;
import com.newuniverse.nusmp.book.SpiritDiveTier.Grade;
import com.newuniverse.nusmp.book.SpiritDiveTier.Lod;
import com.newuniverse.nusmp.client.prop.PropDraw;
import com.newuniverse.nusmp.client.prop.SealDraw;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

/**
 * Spirit Dive remake: the procedural effects of the remade dive, drawn by {@link SpiritDiveAura} after the pieces. All of them are flat or camera-facing sprites with additive blending
 * (the SafeAdditive rules: {@code AuraRender.additive}, no depth write, sprites that are black under their alpha) in the pose of the aura layer (the feet at y = 1.5, y down), and every count is
 * taken from {@link SpiritDiveTier#budget} for the tier and the level of detail, so the worst case of a dive is the budget and nothing else:
 * <ul>
 *   <li><b>ground</b>: the sigil of the element under the feet (character tier: a large turning sigil and a counter-ring; standard: one turning ring);</li>
 *   <li><b>columns</b>: streaks of light rising round the body (the aura: Yuno's green, Fuegoleon's crimson, Noelle's blue);</li>
 *   <li><b>spirals</b>: helices of the element winding up round the body (wind streams, flame ribbons, water ribbons);</li>
 *   <li><b>entry</b> (character tier, the first {@link SpiritDiveTier#ENTRY_TICKS} ticks): a shockwave ring and a second one, a pillar of light and a flash at the chest;</li>
 *   <li><b>rune rings</b> (character tier, full level of detail): two tilted rings of runes orbiting the torso;</li>
 *   <li><b>motes and bits</b>: mana flowing into the body (the passive intake of the dive) and bits of the element rising;</li>
 *   <li><b>strands</b>: hair (Wind), a flame mane trail (Fire) or ribbons from the shoulders (Water), a few segments each;</li>
 *   <li><b>flares</b>: glowing eyes and hands.</li>
 * </ul>
 * No allocation per frame: one scratch quaternion, textures cached in statics (AuraRender.tex answers from RlCache). The half dive of Wind (Yuno's first dive) draws everything on the LEFT half only.
 */
public final class SpiritDiveFx {
    private SpiritDiveFx() {}

    private static final int FB = PropDraw.FULL_BRIGHT;
    private static final ResourceLocation GLOW = AuraRender.tex("particle/glow"),
            COLUMN = AuraRender.tex("aura/sdv_column"), PILLAR = AuraRender.tex("aura/sdv_pillar"), SHOCK = AuraRender.tex("aura/sdv_shock"), RUNES = AuraRender.tex("aura/sdv_runering"),
            WISP = AuraRender.tex("aura/sdv_wisp"),
            SIGIL_WIND = AuraRender.tex("aura/sdv_sigil_wind"), SIGIL_FIRE = AuraRender.tex("aura/sdv_sigil_fire"), SIGIL_WATER = AuraRender.tex("aura/sdv_sigil_water"),
            SIGIL_EARTH = AuraRender.tex("aura/sdv_sigil_earth"),
            RING_WIND = AuraRender.tex("particle/air_ring"), RING_WATER = AuraRender.tex("particle/waterfx_ripple"), RING_FIRE = AuraRender.tex("particle/flamefx_ring"),
            RING_EARTH = AuraRender.tex("particle/earthfam_rock_ring"),
            BIT_FIRE = AuraRender.tex("particle/flamefx_ember"), BIT_WATER = AuraRender.tex("particle/waterfx_drop"), BIT_EARTH = AuraRender.tex("particle/earthfam_rock_dust");

    private static final Quaternionf Q = new Quaternionf();
    private static final float DEG = 57.29578f;

    // ------------------------------------------------------------------ the entry point
    /**
     * Draws the effects of one dive.
     *
     * @param character the character tier (already admitted for this frame)
     * @param f         0..1 how far the dive is on (forging in / shattering out)
     * @param pulse     the slow shimmer of the look (0.7..1.1)
     * @param self1p    this is the local player in first person: nothing is drawn on the head
     * @param flare     0..1 the cast flare (the wearer is casting): the lights leap
     */
    public static void paint(AuraContext c, Element el, Stage st, Grade g, Lod lod, boolean character, float f, float pulse, boolean self1p, float flare) {
        if (f <= 0.01f || lod == Lod.CULLED) return;
        Budget b = SpiritDiveTier.budget(character, lod);
        PoseStack pose = c.pose();
        SealDraw.camera(pose);
        boolean saint = SpiritDivePieces.saint(el, st);
        int glow = SpiritDiveTier.glow(el, g, saint), prim = SpiritDiveTier.primary(el, g), acc = SpiritDiveTier.accent(el, g);
        float lit = pulse * (1f + 0.35f * flare);
        boolean half = SpiritDivePieces.halfBody(el, st);
        ground(c, el, character, b.rings(), f, lit, glow, prim);
        if (character && b.rings() >= 3) backSigil(c, el, f, lit, glow);
        if (b.columns() > 0) columns(c, b.columns(), half, f, lit, glow, acc);
        if (b.spirals() > 0) spirals(c, el, b.spirals(), half, f, lit, prim, acc);
        if (character && b.pillars() > 0) entry(c, el, b, f, glow, acc);
        if (character && SpiritDiveTier.extras(lod)) runeRings(c, f, lit, glow, acc);
        if (b.motes() > 0) motes(c, b.motes(), half, f, glow, acc);
        if (b.bits() > 0) bits(c, el, b.bits(), half, f, glow);
        if (b.strands() > 0 && el != Element.EARTH) strands(c, el, b.strands(), character, half, self1p, f, lit, prim, acc);
        if (b.flares() > 0) flares(c, el, character, b.flares(), half, self1p, f, lit, glow, acc);
    }

    // ------------------------------------------------------------------ helpers
    private static float frac(float x) { return x - (float) Math.floor(x); }

    private static VertexConsumer add(AuraContext c, ResourceLocation tex) { return c.buffers().getBuffer(AuraRender.additive(tex)); }

    private static void applyPart(PoseStack pose, ModelPart part) {
        pose.translate(part.x / 16f, part.y / 16f, part.z / 16f);
        if (part.xRot != 0f || part.yRot != 0f || part.zRot != 0f) pose.mulPose(Q.rotationZYX(part.zRot, part.yRot, part.xRot));
        pose.scale(1f / 16f, 1f / 16f, 1f / 16f);                                                  // model pixels
    }

    private static ResourceLocation sigil(Element el) {
        return switch (el) { case WIND -> SIGIL_WIND; case WATER -> SIGIL_WATER; case FIRE -> SIGIL_FIRE; case EARTH -> SIGIL_EARTH; };
    }

    private static ResourceLocation ring(Element el) {
        return switch (el) { case WIND -> RING_WIND; case WATER -> RING_WATER; case FIRE -> RING_FIRE; case EARTH -> RING_EARTH; };
    }

    // ------------------------------------------------------------------ the ground
    private static void ground(AuraContext c, Element el, boolean character, int rings, float f, float lit, int glow, int acc) {
        if (rings <= 0) return;
        PoseStack pose = c.pose();
        float age = c.age(), grow = 0.55f + 0.45f * f;
        pose.pushPose();                                                                          // layer space: the feet are at y = 1.5
        pose.translate(0f, 1.478f, 0f);
        if (character && rings >= 2) {
            SealDraw.flat(pose, add(c, sigil(el)), 1.95f * grow, age * 0.55f, SealDraw.argb(0.85f * f * lit, glow), FB);
            SealDraw.flat(pose, add(c, ring(el)), 1.3f * grow, -age * 1.3f, SealDraw.argb(0.6f * f * lit, acc), FB);
        } else {
            SealDraw.flat(pose, add(c, ring(el)), 0.95f * grow, age * (el == Element.EARTH ? 0.5f : 0.9f), SealDraw.argb(0.7f * f * lit, glow), FB);
        }
        pose.popPose();
    }

    // ------------------------------------------------------------------ the columns of light
    private static void columns(AuraContext c, int n, boolean half, float f, float lit, int glow, int acc) {
        PoseStack pose = c.pose();
        VertexConsumer vc = add(c, COLUMN);
        float age = c.age();
        for (int i = 0; i < n; i++) {
            float ph = frac(age * 0.016f * (1f + 0.22f * (i % 3)) + i * 0.371f);
            float a = half ? ((i + 0.5f) / n - 0.5f) * Mth.PI : i * Mth.TWO_PI / n + age * 0.018f * (i % 2 == 0 ? 1f : -1f);
            float r = 0.5f + 0.07f * Mth.sin(i * 2.3f + age * 0.05f);
            float hh = 0.62f + 0.3f * Mth.sin(i * 1.7f), y = 1.5f - (ph * 2.5f - 0.1f);
            float hw = 0.36f, ux = -Mth.sin(a) * hw, uz = Mth.cos(a) * hw;
            int col = SealDraw.argb(Mth.sin(ph * Mth.PI) * 0.3f * f * lit, i % 2 == 0 ? glow : acc);
            float cx = Mth.cos(a) * r, cz = Mth.sin(a) * r;
            SealDraw.plane(pose, vc, cx, y, cz, ux, 0f, uz, 0f, -hh, 0f, false, col, FB);
            SealDraw.plane(pose, vc, cx, y, cz, ux, 0f, uz, 0f, -hh, 0f, true, col, FB);
        }
    }

        // ------------------------------------------------------------------ the sigil behind the back (character tier)
    /** A large sigil standing upright behind the back, turning slowly: Yuno's circle of connected crosses (the series puts it behind his back in the full dive), Fuegoleon's sun of flames, Noelle's waves. */
    private static void backSigil(AuraContext c, Element el, float f, float lit, int glow) {
        PoseStack pose = c.pose();
        float spin = c.age() * 0.012f, r = 1.05f * (0.7f + 0.3f * f);
        float cs = Mth.cos(spin) * r, sn = Mth.sin(spin) * r;                                    // the plane's two vectors turn about its centre: the texture turns, the disc stands still
        VertexConsumer vc = add(c, sigil(el));
        int col = SealDraw.argb(0.4f * f * lit, glow);
        float cy = 1.5f - 1.12f, cz = 0.62f;                                                      // behind the back (+z), at the height of the chest
        SealDraw.plane(pose, vc, 0f, cy, cz, cs, -sn, 0f, -sn, -cs, 0f, false, col, FB);
        SealDraw.plane(pose, vc, 0f, cy, cz, cs, -sn, 0f, -sn, -cs, 0f, true, col, FB);
    }

    // ------------------------------------------------------------------ the helices
    /** Streams of the element winding up round the body: a ribbon of {@link SpiritDiveTier#SPIRAL_SEGMENTS} camera-facing quads per helix, the bright end low (flames, water) or high (wind). */
    private static void spirals(AuraContext c, Element el, int n, boolean half, float f, float lit, int prim, int acc) {
        PoseStack pose = c.pose();
        VertexConsumer vc = add(c, WISP);
        float age = c.age();
        float speed = el == Element.WIND ? 0.075f : el == Element.FIRE ? 0.05f : 0.06f;
        boolean headLow = el != Element.WIND;
        int seg = SpiritDiveTier.SPIRAL_SEGMENTS;
        for (int h = 0; h < n; h++) {
            float dir = h % 2 == 0 ? 1f : -1f, phase = h * Mth.TWO_PI / n + age * speed * dir;
            float px = 0f, py = 0f, pz = 0f;
            for (int k = 0; k <= seg; k++) {
                float t = k / (float) seg;
                float a = phase + t * Mth.TWO_PI * 1.15f * dir;
                float r = 0.58f + 0.2f * Mth.sin(t * Mth.PI) + 0.04f * Mth.sin(age * 0.09f + h);
                float x = Mth.cos(a) * r, y = 1.5f - (0.08f + t * 2.05f), z = Mth.sin(a) * r;
                if (half) x = Math.abs(x);
                if (k > 0) {
                    float tPrev = (k - 1f) / seg, tm = (k - 0.5f) / seg, env = Mth.sin(tm * Mth.PI);
                    int col = SealDraw.argb(0.8f * env * f * lit, SealDraw.mix(prim, acc, 0.3f + 0.35f * (1f - tm)));
                    float hw = 0.08f + 0.1f * env;
                    if (headLow) SealDraw.ribbon(pose, vc, x, y, z, px, py, pz, hw, 0.25f, 1f, col, FB);          // the bright end is the low one (flames, water)
                    else SealDraw.ribbon(pose, vc, px, py, pz, x, y, z, hw, 0.25f, 1f, col, FB);               // the bright end is the high one (wind)
                }
                px = x; py = y; pz = z;
            }
        }
    }

    // ------------------------------------------------------------------ the entry: shockwave, second ring, pillar of light, flash
    private static void entry(AuraContext c, Element el, Budget b, float f, int glow, int acc) {
        float t = c.t(), e = SpiritDiveTier.entry(t);
        if (e >= 1f) return;
        PoseStack pose = c.pose();
        float inv = 1f - e, out = 1f - inv * inv * inv;
        pose.pushPose();
        pose.translate(0f, 1.47f, 0f);
        VertexConsumer sh = add(c, SHOCK);
        SealDraw.flat(pose, sh, 0.5f + 4.4f * out, t * 3f, SealDraw.argb((float) Math.pow(inv, 1.3) * 0.95f, glow), FB);
        float e2 = SpiritDiveTier.entry(t - 5f);
        if (e2 > 0f && e2 < 1f) {
            float inv2 = 1f - e2;
            SealDraw.flat(pose, sh, 0.4f + 3.1f * (1f - inv2 * inv2 * inv2), -t * 2f, SealDraw.argb((float) Math.pow(inv2, 1.3) * 0.7f, acc), FB);
        }
        pose.popPose();
        SealDraw.glow(pose, add(c, GLOW), 0f, 1.5f - 1.0f, 0f, 0.35f + 1.2f * inv, c.age() * 0.1f, SealDraw.argb(inv * 0.9f * f, acc), FB);        // the flash at the chest
        VertexConsumer pil = add(c, PILLAR);
        float h = 2.3f * (0.5f + 0.5f * out), age = c.age();
        for (int k = 0; k < b.pillars(); k++) {
            float a = k * Mth.PI / b.pillars() + age * 0.05f, ux = Mth.cos(a) * 0.45f, uz = Mth.sin(a) * 0.45f;
            int col = SealDraw.argb((float) Math.pow(inv, 1.2) * 0.85f * f, k % 2 == 0 ? glow : acc);
            SealDraw.plane(pose, pil, 0f, 1.5f - h, 0f, ux, 0f, uz, 0f, -h, 0f, false, col, FB);
            SealDraw.plane(pose, pil, 0f, 1.5f - h, 0f, ux, 0f, uz, 0f, -h, 0f, true, col, FB);
        }
    }

    // ------------------------------------------------------------------ the rune rings orbiting the torso
    private static void runeRings(AuraContext c, float f, float lit, int glow, int acc) {
        PoseStack pose = c.pose();
        float age = c.age();
        VertexConsumer vc = add(c, RUNES);
        for (int i = 0; i < 2; i++) {
            pose.pushPose();
            pose.translate(0f, 1.5f - (0.92f + 0.22f * i), 0f);
            pose.mulPose(Q.rotationZYX(0.42f * Mth.cos(age * 0.017f + i * 1.9f), 0f, 0.42f * Mth.sin(age * 0.021f + i * 2.4f)));
            SealDraw.flat(pose, vc, 0.95f + 0.28f * i, (i == 0 ? 1f : -1f) * age * (2.2f - 0.7f * i), SealDraw.argb((0.8f - 0.2f * i) * f * lit, i == 0 ? glow : acc), FB);
            pose.popPose();
        }
    }

    // ------------------------------------------------------------------ the mana flowing in, and the bits of the element rising
    private static void motes(AuraContext c, int n, boolean half, float f, int glow, int acc) {
        PoseStack pose = c.pose();
        VertexConsumer vc = add(c, GLOW);
        float age = c.age();
        for (int i = 0; i < n; i++) {
            float ph = frac(age * 0.022f * (1f + 0.25f * (i % 4)) + i * 0.173f), e = ph * ph * (3f - 2f * ph);
            float a = i * 2.399f + age * 0.012f + e * 2.2f, r0 = 1.45f + 0.5f * Mth.sin(i * 1.7f), h0 = 0.25f + 1.8f * frac(i * 0.381f);
            float r = r0 * (1f - e), h = h0 + (1.05f - h0) * e;
            float x = Mth.cos(a) * r, z = Mth.sin(a) * r;
            if (half) x = Math.abs(x);
            SealDraw.glow(pose, vc, x, 1.5f - h, z, 0.075f * (1f - 0.55f * e) + 0.02f, a, SealDraw.argb(Mth.sin(ph * Mth.PI) * 0.85f * f, i % 3 == 0 ? acc : glow), FB);
        }
    }

    private static void bits(AuraContext c, Element el, int n, boolean half, float f, int glow) {
        PoseStack pose = c.pose();
        ResourceLocation tex = switch (el) { case WIND -> GLOW; case WATER -> BIT_WATER; case FIRE -> BIT_FIRE; case EARTH -> BIT_EARTH; };
        VertexConsumer vc = add(c, tex);
        float age = c.age();
        for (int i = 0; i < n; i++) {
            float ph = frac(age * 0.016f * (1f + 0.35f * (i % 3)) + i * 0.37f), a = i * 2.399f + age * 0.035f * (el == Element.WIND ? 2.2f : 1f);
            float r = 0.45f + 0.22f * Mth.sin(i * 1.9f) + 0.1f * ph, h = el == Element.EARTH ? 0.2f + 1.5f * ph : 0.05f + 2.0f * ph;
            float x = Mth.cos(a) * r, z = Mth.sin(a) * r;
            if (half) x = Math.abs(x);
            SealDraw.glow(pose, vc, x, 1.5f - h, z, 0.05f + 0.04f * (1f - ph), a, SealDraw.argb(Mth.sin(ph * Mth.PI) * f * (el == Element.WIND ? 0.7f : 1f), glow), FB);
        }
    }

    // ------------------------------------------------------------------ strands: hair (Wind), a flame mane trail (Fire), ribbons from the shoulders (Water)
    private static void strands(AuraContext c, Element el, int n, boolean character, boolean half, boolean self1p, float f, float lit, int prim, int acc) {
        boolean head = el != Element.WATER;
        if (head && self1p) return;                                                                // the head is not drawn in first person
        PoseStack pose = c.pose();
        ModelPart part = head ? c.model().head : c.model().body;
        pose.pushPose();
        applyPart(pose, part);
        SealDraw.camera(pose);
        VertexConsumer vc = add(c, WISP);
        float age = c.age(), sw = Mth.clamp(c.limbSwingAmount(), 0f, 1f), len = character ? 1.45f : 1f, wid = character ? 1.6f : 1.15f;
        float down = el == Element.FIRE ? -1.3f : head ? 1.0f : 4.4f, back = el == Element.FIRE ? 2.3f : head ? 3.0f : 1.7f;
        for (int s = 0; s < n; s++) {
            float side = n == 1 ? 0f : (s / (float) (n - 1)) * 2f - 1f;                            // -1..1 across the back
            float px, py, pz;
            if (head) { px = side * 3.2f; py = -5.5f + 1.3f * Mth.abs(side); pz = 3.9f; }
            else { px = (s % 2 == 0 ? -4.7f : 4.7f); py = 1.0f + (s / 2) * 1.2f; pz = 2.4f; }
            if (half) px = Math.abs(px);
            float seed = s * 1.713f;
            for (int k = 0; k < SpiritDiveTier.SEGMENTS; k++) {
                float wave = Mth.sin(age * 0.14f + seed + k * 0.8f) * (0.9f + 0.55f * k) * len;
                float nx = px + wave + side * 0.7f * (k + 1) * (head ? 1f : 0.3f), ny = py + down * len * (1f + 0.5f * sw), nz = pz + (back + 1.6f * sw) * len;
                float w = wid * (1f - 0.62f * k / SpiritDiveTier.SEGMENTS);
                float fade = (1f - 0.7f * k / SpiritDiveTier.SEGMENTS);
                int col = SealDraw.argb(0.85f * f * lit * fade, k < 2 ? SealDraw.mix(prim, acc, 0.45f) : prim);
                SealDraw.ribbon(pose, vc, nx, ny, nz, px, py, pz, w, 0.25f, 1f, col, FB);
                px = nx; py = ny; pz = nz;
            }
        }
        pose.popPose();
    }

    // ------------------------------------------------------------------ eyes and hands
    private static void flares(AuraContext c, Element el, boolean character, int n, boolean half, boolean self1p, float f, float lit, int glow, int acc) {
        PoseStack pose = c.pose();
        VertexConsumer vc = add(c, GLOW);
        float age = c.age();
        int left = n;
        if (el != Element.EARTH && !self1p && left >= 2) {                                          // the eyes
            pose.pushPose();
            applyPart(pose, c.model().head);
            SealDraw.camera(pose);
            float sz = (character ? 1.9f : 1.5f) * (0.9f + 0.1f * Mth.sin(age * 0.3f));
            int col = SealDraw.argb(0.95f * f * Math.min(1.2f, lit), acc);
            SealDraw.glow(pose, vc, 2.0f, -4.2f, -4.45f, sz, 0f, col, FB);
            if (!half) SealDraw.glow(pose, vc, -2.0f, -4.2f, -4.45f, sz, 0f, col, FB);
            pose.popPose();
            left -= 2;
        }
        if (character && left >= 2) {                                                               // the hands
            for (int hand = 0; hand < 2; hand++) {
                if (half && hand == 1) continue;                                                    // Yuno's first dive: only the left hand
                ModelPart arm = hand == 0 ? c.model().leftArm : c.model().rightArm;
                pose.pushPose();
                applyPart(pose, arm);
                SealDraw.camera(pose);
                float sz = 2.8f * (0.85f + 0.15f * Mth.sin(age * 0.23f + hand * 2f));
                SealDraw.glow(pose, vc, 0f, 10.8f, 0f, sz * (1f + 0.2f * (lit - 1f)), age * 0.1f, SealDraw.argb(0.8f * f * Math.min(1.2f, lit), glow), FB);
                pose.popPose();
            }
        }
    }
}
