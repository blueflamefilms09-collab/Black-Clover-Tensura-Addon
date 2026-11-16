package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

import static com.newuniverse.nusmp.vfx.client.layer.SkillAnimeLayer.*;
import static com.newuniverse.nusmp.vfx.client.layer.SkillAnime2Layer.*;

/**
 * ANIME2_SKILL for {@link SkillAnime2Layer}: the signature of each skill wave 3 left out, one style per {@link SkillAnime2Vfx#SKILL_STYLES} entry (power = style + 1; from = the feet unless said; the
 * from -> to distance is the radius, except where said). Each is a handful of pieces from the kit, far under the 100-quad cap (the preview's worst case is in the doc):
 * <ul>
 *   <li>LIARIS: Liaris Freese (Bell's growth): a crimson seal, two helices of light climbing the body, petals, a column of light. HATI: Hati (Bete's wolf): fire, lightning and magic motes streak INTO the body.</li>
 *   <li>HAWK: Hawk Eye (Allen's Bird Card): an eye opens over the head, a scanning ring sweeps to the radius, feathers drift. SPIRIT_CARD: Spirit Card: a card spins over the head, green rings and motes heal outwards.</li>
 *   <li>SUMMON: a summon card flips open on a rune circle and a pillar of light lifts the summons in (the colour tells the card). SHISHOU: the Teacher's analysis rings round the wielder, an eye, data lines; the
 *       from -> to distance is 1 + the mode, one pip per mode.</li>
 *   <li>GATE_ARMOR: golden plates fly in and lock on the body. GATE_VIMANA: from -> to the flight, golden ripple gates trail behind and wings of light spread. GATE_KING: a sunburst, golden gates round the
 *       radius, banners of light. GATE_ELIXIR: a fountain of golden droplets and bubbles with a healing glow.</li>
 *   <li>DEVIL_KICK: Shinra's kick, from = the hip, to = the foot's reach: a flaming crescent sweeps up through the cone. ALMIGHTY: The Almighty (Yhwach): a golden eye opens above, rays of every future, black
 *       smoke. VISION_STAR: The Visionary's meteor from -> to (the radius rides as the style's extra). VISION_VOID: a black sphere of imagination with debris rising. VISION_SELF: Perfect Body: rings climb, a
 *       crown of rays, streaks of light.</li>
 * </ul>
 */
final class SkillAnime2More {
    private SkillAnime2More() {}

    static void draw(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        Vector3f from = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        float R = Math.max(0.8f, from.distance(to));
        switch (SkillAnime2Vfx.skillStyle(inst.power)) {
            case 1 -> hati(inst, ctx, buf, age, from);
            case 2 -> hawk(inst, ctx, buf, age, from, R);
            case 3 -> spiritCard(inst, ctx, buf, age, from, R);
            case 4 -> summon(inst, ctx, buf, age, from, R);
            case 5 -> shishou(inst, ctx, buf, age, from, R);
            case 6 -> armor(inst, ctx, buf, age, from);
            case 7 -> vimana(inst, ctx, buf, age, from, to);
            case 8 -> king(inst, ctx, buf, age, from, R);
            case 9 -> elixir(inst, ctx, buf, age, from);
            case 10 -> kick(inst, ctx, buf, age, from, to);
            case 11 -> almighty(inst, ctx, buf, age, from);
            case 12 -> visionStar(inst, ctx, buf, age, from, to);
            case 13 -> visionVoid(inst, ctx, buf, age, from, R);
            case 14 -> visionSelf(inst, ctx, buf, age, from);
            default -> liaris(inst, ctx, buf, age, from);
        }
    }

    private static Vector3f at(Vector3f c, float x, float y, float z) { return new Vector3f(x, y, z).add(c); }

    private static void groundRing(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f c, float r0, float r1, float spin, int col, float a) {
        buf.ring(RING, VfxBlend.ADD, VfxPose.ground(at(c, 0, 0.06f, 0)), r0, r1, ctx.seg(16, 8), 4, spin, al(col, a));
    }

    // ------------------------------------------------------------------ LIARIS FREESE
    private static void liaris(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age, Vector3f c) {
        int col = inst.color;
        float fade = life(inst, age, 2, 9), open = VfxAnim.easeOutBack(ramp(age, 0, 6f));
        buf.plane(SIGIL, VfxBlend.ALPHA, VfxPose.ground(at(c, 0, 0.06f, 0)).spin(age * 0.05f), 1.3f * open, al(col, 0.85f * fade));
        buf.plane(SIGIL, VfxBlend.ADD, VfxPose.ground(at(c, 0, 0.08f, 0)).spin(-age * 0.07f), 1.0f * open, al(hot(col, 0.6f), 0.6f * fade));
        buf.beam(ctx, STREAK, VfxBlend.ADD, c, at(c, 0, 3.4f * open, 0), 1.2f, 0.5f, 1, 0, al(col, 0.45f * fade), al(hot(col, 0.5f), 0.1f * fade));
        int n = ctx.seg(8, 4);
        for (int i = 0; i < n; i++) {                                                  // two helices of light climb the body: the growth
            float ph = Mth.frac(age * 0.05f + (i / 2) / (float) (n / 2)), ang = ph * 8f + (i % 2) * Mth.PI;
            buf.billboard(ctx, SPARKLE, VfxBlend.ADD, at(c, Mth.cos(ang) * 0.75f, ph * 2.8f, Mth.sin(ang) * 0.75f), 0.4f, ang, al(hot(col, 0.5f), Mth.sin(ph * Mth.PI) * fade));
        }
        for (int i = 0; i < 3; i++) {
            float ph = Mth.frac(age * 0.035f + i / 3f);
            buf.billboard(ctx, PETAL, VfxBlend.ALPHA, at(c, Mth.cos(i * 2.1f + age * 0.1f) * (0.4f + ph), 0.2f + ph * 2.4f, Mth.sin(i * 2.1f + age * 0.1f) * (0.4f + ph)), 0.3f, age * 0.2f + i, al(0xFFFFB0C0, Mth.sin(ph * Mth.PI) * fade));
        }
        if (age < 8f) flash(ctx, buf, at(c, 0, 1f, 0), 1.6f, age * 0.3f, col, 1 - age / 8f);
    }

    // ------------------------------------------------------------------ HATI
    private static void hati(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age, Vector3f c) {
        int col = inst.color;
        float fade = life(inst, age, 0, 6);
        Vector3f chest = at(c, 0, 1.1f, 0);
        RandomSource r = inst.random();
        int n = ctx.seg(8, 4);
        for (int i = 0; i < n; i++) {                                                  // fire and lightning streak INTO the body
            float t0 = r.nextFloat() * 5f, u = ramp(age, t0, 7f);
            Vector3f d = randomDir(r);
            if (u <= 0 || u >= 1) continue;
            Vector3f p0 = new Vector3f(d).mul(2.6f).add(chest), head = lerp(p0, chest, VfxAnim.easeInCubic(u)), tail = lerp(p0, chest, Math.max(0, VfxAnim.easeInCubic(u) - 0.3f));
            if (i % 2 == 0) buf.beam(ctx, STREAK, VfxBlend.ADD, tail, head, 0.05f, 0.35f, 1, 0, al(col, 0.2f * fade), al(hot(col, 0.5f), 0.9f * fade));
            else zigzag(ctx, buf, head, lerp(head, chest, 0.35f), 3, 0.2f, 0.06f, inst.seed + i * 31L + (long) (age * 0.5f), 0xFFFFF04D, fade * 0.9f);
        }
        buf.ring(RING, VfxBlend.ADD, camPose(chest), 2.4f * (1 - VfxAnim.easeOutCubic(ramp(age, 0, 8f))) + 0.3f, 2.6f * (1 - VfxAnim.easeOutCubic(ramp(age, 0, 8f))) + 0.5f, ctx.seg(14, 7), 3, 0, al(col, 0.6f * fade));
        buf.billboard(ctx, GLOW, VfxBlend.ADD, chest, 2.2f, 0, al(col, 0.55f * ramp(age, 4f, 5f) * fade));
        if (age >= 7f) flash(ctx, buf, chest, 1.5f, age * 0.3f, col, (1 - ramp(age, 7f, 6f)) * fade);
    }

    // ------------------------------------------------------------------ HAWK EYE
    private static void hawk(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age, Vector3f c, float R) {
        int col = inst.color;
        float fade = life(inst, age, 2, 8), open = VfxAnim.easeOutBack(ramp(age, 0, 6f));
        Vector3f head = at(c, 0, 2.6f, 0);
        buf.billboard(ctx, GLOW, VfxBlend.ADD, head, 1.9f * open, 0, al(col, 0.45f * fade));
        buf.billboard(ctx, EYE, VfxBlend.ALPHA, head, 1.5f * open, 0, al(INK, 0.6f * fade));
        buf.billboard(ctx, EYE, VfxBlend.ADD, head, 1.4f * open, 0, al(hot(col, 0.5f), fade));
        float rr = Math.min(R, 24f) * VfxAnim.easeOutCubic(ramp(age, 0, inst.duration * 0.8f));
        groundRing(ctx, buf, c, rr * 0.96f, rr, 0, col, 0.8f * fade * (1 - ramp(age, inst.duration * 0.5f, inst.duration * 0.5f)));
        int n = ctx.seg(5, 3);
        for (int i = 0; i < n; i++) {                                                  // feathers drift down from the eye
            float ph = Mth.frac(age * 0.04f + i / (float) n);
            buf.billboard(ctx, FEATHER, VfxBlend.ALPHA, at(head, Mth.cos(i * 2.4f) * (0.5f + ph), -ph * 2.2f, Mth.sin(i * 2.4f) * (0.5f + ph)), 0.7f, ph * 3f + i, al(hot(col, 0.4f), Mth.sin(ph * Mth.PI) * fade));
        }
    }

    // ------------------------------------------------------------------ SPIRIT CARD
    private static void spiritCard(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age, Vector3f c, float R) {
        int col = inst.color;
        float fade = life(inst, age, 2, 8), pop = VfxAnim.easeOutBack(ramp(age, 0, 6f));
        Vector3f top = at(c, 0, 2.5f, 0);
        buf.billboard(ctx, GLOW, VfxBlend.ADD, top, 1.6f * pop, 0, al(col, 0.4f * fade));
        buf.billboard(ctx, CARD, VfxBlend.ALPHA, top, 1.0f * pop, Mth.sin(age * 0.15f) * 0.25f, al(col, 0.95f * fade));
        buf.billboard(ctx, CARD, VfxBlend.ADD, top, 1.05f * pop, Mth.sin(age * 0.15f) * 0.25f, al(hot(col, 0.6f), 0.7f * fade));
        float rr = Math.min(R, 16f) * VfxAnim.easeOutCubic(ramp(age, 0, 12f));
        groundRing(ctx, buf, c, rr * 0.9f, rr, age * 0.02f, col, 0.7f * fade * (1 - ramp(age, 8f, 12f)));
        RandomSource r = inst.random();
        int n = ctx.seg(8, 4);
        for (int i = 0; i < n; i++) {                                                  // healing motes rise
            float ph = Mth.frac(age * 0.04f + r.nextFloat()), ang = r.nextFloat() * Mth.TWO_PI, rad = Math.min(R, 6f) * Mth.sqrt(r.nextFloat());
            Vector3f p = at(c, Mth.cos(ang) * rad, ph * 2.4f, Mth.sin(ang) * rad);
            if (i % 2 == 0) buf.billboard(ctx, LEAF, VfxBlend.ALPHA, p, 0.4f, ang + ph * 3f, al(col, Mth.sin(ph * Mth.PI) * fade));
            else buf.billboard(ctx, SPARKLE, VfxBlend.ADD, p, 0.4f, age * 0.2f + i, al(hot(col, 0.5f), Mth.sin(ph * Mth.PI) * fade));
        }
    }

    // ------------------------------------------------------------------ SUMMON CARDS
    private static void summon(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age, Vector3f c, float R) {
        int col = inst.color;
        float fade = life(inst, age, 2, 9), open = VfxAnim.easeOutBack(ramp(age, 0, 6f)), s = Mth.clamp(R * 0.4f, 0.9f, 2.4f);
        buf.plane(RUNE, VfxBlend.ALPHA, VfxPose.ground(at(c, 0, 0.06f, 0)).spin(age * 0.05f), 1.5f * s * open, al(col, 0.9f * fade));
        buf.plane(RUNE, VfxBlend.ADD, VfxPose.ground(at(c, 0, 0.08f, 0)).spin(-age * 0.08f), 1.1f * s * open, al(hot(col, 0.5f), 0.6f * fade));
        float flip = ramp(age, 2f, 10f);                                                // the card rises and flips, then bursts into light
        if (flip < 1f) {
            Vector3f card = at(c, 0, 0.6f + 1.4f * VfxAnim.easeOutCubic(flip), 0);
            buf.billboard(ctx, CARD, VfxBlend.ALPHA, card, 0.9f * Math.abs(Mth.cos(flip * 9f)) + 0.15f, 0, al(col, fade));
            buf.billboard(ctx, GLOW, VfxBlend.ADD, card, 1.5f, 0, al(col, 0.4f * fade));
        } else {
            flash(ctx, buf, at(c, 0, 1.6f, 0), 1.6f * s, age * 0.2f, col, 1 - ramp(age, 12f, 6f));
        }
        buf.beam(ctx, STREAK, VfxBlend.ADD, at(c, 0, 0, 0), at(c, 0, 4.5f * s * ramp(age, 4f, 6f), 0), 1.3f * s, 0.6f * s, 1, 0, al(col, 0.45f * fade), al(hot(col, 0.5f), 0.1f * fade));
        int n = ctx.seg(8, 4);
        for (int i = 0; i < n; i++) {
            float ph = Mth.frac(age * 0.05f + i / (float) n), ang = i * 2.4f;
            buf.billboard(ctx, SPARKLE, VfxBlend.ADD, at(c, Mth.cos(ang) * s * (1.2f - ph * 0.8f), ph * 3f, Mth.sin(ang) * s * (1.2f - ph * 0.8f)), 0.4f, age * 0.3f + i, al(hot(col, 0.5f), Mth.sin(ph * Mth.PI) * fade));
        }
    }

    // ------------------------------------------------------------------ SHISHOU
    private static void shishou(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age, Vector3f c, float R) {
        int col = inst.color, mode = Mth.clamp(Math.round(R) - 1, 0, 6);
        float fade = life(inst, age, 2, 8), open = VfxAnim.easeOutBack(ramp(age, 0, 6f));
        Vector3f mid = at(c, 0, 1.2f, 0);
        VfxPose p = camPose(mid);
        buf.plane(RUNE, VfxBlend.ALPHA, p.spin(age * 0.06f), 1.5f * open, al(col, 0.8f * fade));
        buf.plane(RUNE, VfxBlend.ADD, p.spin(-age * 0.1f), 1.1f * open, al(hot(col, 0.5f), 0.7f * fade));
        buf.billboard(ctx, EYE, VfxBlend.ADD, mid, 0.9f * open, 0, al(hot(col, 0.7f), fade));
        for (int i = 0; i <= mode; i++) {                                              // one pip per mode round the outer ring
            float ang = Mth.TWO_PI * i / 7f + age * 0.1f;
            buf.billboard(ctx, SPARKLE, VfxBlend.ADD, p.point(Mth.cos(ang) * 1.5f, Mth.sin(ang) * 1.5f), 0.4f, ang, al(WHITE, fade));
        }
        int n = ctx.seg(6, 3);
        for (int i = 0; i < n; i++) {                                                  // lines of data rise beside the wielder
            float ph = Mth.frac(age * 0.07f + i / (float) n), x = (i - (n - 1) * 0.5f) * 0.5f;
            Vector3f a = at(c, x, ph * 1.6f, 0.2f);
            buf.beam(ctx, STITCH, VfxBlend.ADD, a, at(a, 0, 0.5f, 0), 0.1f, 0.1f, 1, 0, al(col, 0.8f * fade * Mth.sin(ph * Mth.PI)), al(col, 0.8f * fade * Mth.sin(ph * Mth.PI)));
        }
        buf.billboard(ctx, GLOW, VfxBlend.ADD, mid, 3f, 0, al(col, 0.2f * fade));
    }

    // ------------------------------------------------------------------ GOLDEN ARMOUR
    private static void armor(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age, Vector3f c) {
        int col = inst.color;
        float fade = life(inst, age, 0, 8);
        float[][] spots = {{0, 1.4f, 0.3f}, {0.35f, 1.6f, 0}, {-0.35f, 1.6f, 0}, {0, 0.9f, 0.3f}, {0.4f, 1.0f, 0}, {-0.4f, 1.0f, 0}, {0.15f, 0.4f, 0.1f}, {-0.15f, 0.4f, 0.1f}, {0, 2.0f, 0}};
        int n = Math.min(spots.length, ctx.seg(9, 5));
        RandomSource r = inst.random();
        for (int i = 0; i < n; i++) {                                                  // golden plates fly in and lock on the body one after another
            float t0 = i * 0.9f, u = ramp(age, t0, 6f);
            if (u <= 0) continue;
            Vector3f lock = at(c, spots[i][0], spots[i][1], spots[i][2]), start = at(lock, (r.nextFloat() - 0.5f) * 4f, 1.5f + r.nextFloat() * 2f, (r.nextFloat() - 0.5f) * 4f);
            Vector3f p = lerp(start, lock, VfxAnim.easeOutBack(u));
            buf.billboard(ctx, FACET, VfxBlend.ALPHA, p, 0.55f - 0.15f * u, (1 - u) * 3f + i, al(col, fade));
            if (u >= 1f && age - t0 < 9f) flash(ctx, buf, lock, 0.55f, i, col, 1 - (age - t0 - 6f) / 3f);
        }
        buf.billboard(ctx, GLOW, VfxBlend.ADD, at(c, 0, 1.1f, 0), 3.4f, 0, al(col, 0.3f * ramp(age, 4f, 8f) * fade));
        groundRing(ctx, buf, c, 0.9f, 1.15f, age * 0.04f, col, 0.7f * fade);
    }

    // ------------------------------------------------------------------ VIMANA
    private static void vimana(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age, Vector3f c, Vector3f to) {
        int col = inst.color;
        float fade = life(inst, age, 0, 8);
        Vector3f dir = dirOr(c, to, new Vector3f(0, 0, 1)), side = sideOf(dir), under = at(c, 0, 0.2f, 0);
        for (int i = 0; i < 3; i++) {                                                  // golden gates trail behind the flight
            Vector3f g = new Vector3f(dir).mul(-1.4f * (i + 1)).add(under).add(0, 0.8f, 0);
            portal(buf, g, dir, 1.1f - 0.2f * i, VfxAnim.easeOutBack(ramp(age, i * 1.5f, 5f)), age * 0.05f + i, col, fade * (1 - 0.25f * i));
        }
        for (int s = -1; s <= 1; s += 2) {                                             // wings of light spread from the chariot
            for (int k = 0; k < 3; k++) {
                Vector3f root = at(under, 0, 0.5f, 0), tip = new Vector3f(side).mul(s * (1.4f + 0.5f * k)).add(new Vector3f(dir).mul(-0.9f - 0.4f * k)).add(0, 0.5f + 0.15f * k, 0).add(under);
                buf.beam(ctx, FEATHER, VfxBlend.ALPHA, root, lerp(root, tip, VfxAnim.easeOutCubic(ramp(age, 0, 6f))), 0.2f, 0.6f, 1, 0, al(hot(col, 0.3f), 0.9f * fade), al(col, 0.9f * fade));
            }
        }
        buf.beam(ctx, STREAK, VfxBlend.ADD, under, new Vector3f(dir).mul(-5f).add(under), 0.9f, 0.1f, 1, 0, al(col, 0.55f * fade), al(col, 0.05f));
        buf.billboard(ctx, GLOW, VfxBlend.ADD, at(under, 0, 0.5f, 0), 2.4f, 0, al(col, 0.45f * fade));
        if (age < 6f) flash(ctx, buf, under, 1.8f, age * 0.3f, col, 1 - age / 6f);
    }

    // ------------------------------------------------------------------ KING'S AURA
    private static void king(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age, Vector3f c, float R) {
        int col = inst.color;
        float fade = life(inst, age, 2, 9), open = VfxAnim.easeOutBack(ramp(age, 0, 6f)), rr = Math.min(R, 14f);
        Vector3f behind = at(c, 0, 2.3f, 0);
        buf.billboard(ctx, GLOW, VfxBlend.ADD, behind, 4.2f * open, 0, al(col, 0.4f * fade));
        buf.billboard(ctx, RAYS, VfxBlend.ADD, behind, 5f * open, age * 0.012f, al(hot(col, 0.4f), 0.8f * fade));
        buf.plane(HALO, VfxBlend.ADD, VfxPose.ground(at(c, 0, 2.5f, 0)), 0.6f * open, al(hot(col, 0.5f), fade));
        float spread = VfxAnim.easeOutCubic(ramp(age, 0, 10f));
        groundRing(ctx, buf, c, rr * spread * 0.9f, rr * spread, age * 0.02f, col, 0.75f * fade * (1 - 0.5f * spread));
        int m = ctx.seg(5, 3);
        for (int i = 0; i < m; i++) {                                                  // golden gates stand round the radius, banners of light rise between them
            float ang = Mth.TWO_PI * i / m + 0.3f;
            Vector3f g = at(c, Mth.cos(ang) * rr * 0.75f * spread, 1.3f, Mth.sin(ang) * rr * 0.75f * spread);
            portal(buf, g, new Vector3f(c).sub(g), 0.9f, VfxAnim.easeOutBack(ramp(age, 2f + i * 0.8f, 5f)), age * 0.04f + i, col, fade);
            buf.beam(ctx, STREAK, VfxBlend.ADD, at(g, 0, -1.3f, 0), at(g, 0, 4f, 0), 0.5f, 0.2f, 1, 0, al(col, 0.3f * fade), al(hot(col, 0.5f), 0.05f * fade));
        }
    }

    // ------------------------------------------------------------------ ELIXIR
    private static void elixir(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age, Vector3f c) {
        int col = inst.color;
        float fade = life(inst, age, 2, 9);
        buf.billboard(ctx, GLOW, VfxBlend.ADD, at(c, 0, 1.1f, 0), 3f * VfxAnim.easeOutBack(ramp(age, 0, 6f)), 0, al(col, 0.4f * fade));
        groundRing(ctx, buf, c, 0.8f, 1.1f + 1.2f * VfxAnim.easeOutCubic(ramp(age, 0, 10f)), age * 0.02f, col, 0.65f * fade * (1 - ramp(age, 6f, 12f)));
        RandomSource r = inst.random();
        int n = ctx.seg(10, 5);
        for (int i = 0; i < n; i++) {                                                  // a fountain of golden droplets rises and falls back, bubbles drift up
            float ph = Mth.frac(age * 0.05f + r.nextFloat()), ang = r.nextFloat() * Mth.TWO_PI, rad = 0.3f + 0.7f * Mth.sin(ph * Mth.PI) * r.nextFloat();
            Vector3f p = at(c, Mth.cos(ang) * rad, 2.6f * Mth.sin(ph * Mth.PI), Mth.sin(ang) * rad);
            if (i % 3 == 0) buf.billboard(ctx, BUBBLE, VfxBlend.ALPHA, p, 0.35f, 0, al(hot(col, 0.5f), 0.8f * Mth.sin(ph * Mth.PI) * fade));
            else buf.billboard(ctx, DROPLET, VfxBlend.ALPHA, p, 0.38f, (ph - 0.5f) * 1.5f, al(col, Mth.sin(ph * Mth.PI) * fade));
        }
        buf.beam(ctx, STREAK, VfxBlend.ADD, c, at(c, 0, 3.2f, 0), 0.9f, 0.4f, 1, 0, al(col, 0.35f * fade), al(hot(col, 0.5f), 0.05f * fade));
        for (int i = 0; i < 3; i++) buf.billboard(ctx, SPARKLE, VfxBlend.ADD, at(c, (i - 1) * 0.5f, 1.2f + 0.5f * i, 0.2f), 0.4f, age * 0.3f + i, al(WHITE, fade));
    }

    // ------------------------------------------------------------------ DEVIL'S KICK
    private static void kick(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age, Vector3f hip, Vector3f foot) {
        int col = inst.color;
        float fade = life(inst, age, 0, 6), sweep = VfxAnim.easeOutCubic(ramp(age, 0, 5f));
        Vector3f dir = dirOr(hip, foot, new Vector3f(0, 0, 1)), side = sideOf(dir);
        float reach = Math.max(1.5f, hip.distance(foot));
        VfxPose p = VfxPose.facing(lerp(hip, foot, 0.55f), side).spin(-1.3f + 2.2f * sweep);
        buf.plane(CRESCENT, VfxBlend.ALPHA, p, reach * 0.55f, al(INK, 0.45f * fade));
        buf.plane(CRESCENT, VfxBlend.ADD, p, reach * 0.6f, al(col, 0.85f * fade));
        buf.plane(CRESCENT, VfxBlend.ADD, p, reach * 0.5f, al(hot(col, 0.8f), 0.9f * fade));
        int n = ctx.seg(6, 3);
        for (int i = 0; i < n; i++) {                                                  // ink-edged flames lick along the sweep
            float ph = (i + 0.5f) / n, h = -0.3f + 1.7f * ph;
            Vector3f f0 = new Vector3f(dir).mul(reach * (0.35f + 0.5f * Mth.sin(ph * Mth.PI * 0.9f))).add(hip).add(0, h - 1.0f, 0);
            float f = ramp(age, ph * 3f, 3f) * fade;
            upright(buf, FLAME, VfxBlend.ALPHA, f0, 0.6f, 1.0f * f, al(INK, 0.5f * f));
            upright(buf, FLAME, VfxBlend.ADD, f0, 0.5f, 0.9f * f, al(col, 0.9f * f));
        }
        if (age >= 3f) flash(ctx, buf, foot, 1.5f, age * 0.3f, col, 1 - ramp(age, 3f, 7f));
    }

    // ------------------------------------------------------------------ THE ALMIGHTY
    private static void almighty(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age, Vector3f c) {
        int col = inst.color;
        float fade = life(inst, age, 3, 10), open = VfxAnim.easeOutBack(ramp(age, 0, 10f));
        Vector3f sky = at(c, 0, 5.2f, 0);
        buf.billboard(ctx, GLOW, VfxBlend.ADD, sky, 6f * open, 0, al(col, 0.35f * fade));
        buf.billboard(ctx, EYE, VfxBlend.ALPHA, sky, 4.2f * open, 0, al(INK, 0.85f * fade));
        buf.billboard(ctx, EYE, VfxBlend.ADD, sky, 4f * open, 0, al(hot(col, 0.5f), fade));
        int n = ctx.seg(10, 5);
        for (int i = 0; i < n; i++) {                                                  // a ray for every future, one after another, fan down from the eye
            float f = ramp(age, 4f + i * 0.5f, 3f) * fade;
            if (f <= 0.01f) continue;
            float ang = Mth.TWO_PI * i / n, rad = 2.6f;
            Vector3f g = at(c, Mth.cos(ang) * rad, 0.1f, Mth.sin(ang) * rad);
            buf.beam(ctx, STREAK, VfxBlend.ADD, sky, g, 0.1f, 0.4f, 1, 0, al(col, 0.35f * f), al(hot(col, 0.5f), 0.7f * f));
        }
        for (int i = 0; i < 4; i++) {                                                  // black smoke coils round the body
            float ph = Mth.frac(age * 0.04f + i / 4f), ang = ph * 6f + i * 1.6f;
            buf.billboard(ctx, MIST, VfxBlend.ALPHA, at(c, Mth.cos(ang) * 0.9f, 0.2f + ph * 2.2f, Mth.sin(ang) * 0.9f), 1.2f, ang, al(INK, 0.8f * Mth.sin(ph * Mth.PI) * fade));
        }
        buf.plane(RUNE, VfxBlend.ADD, VfxPose.ground(at(c, 0, 0.06f, 0)).spin(age * 0.03f), 2.6f * open, al(col, 0.6f * fade));
    }

    // ------------------------------------------------------------------ THE VISIONARY
    private static void visionStar(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age, Vector3f sky, Vector3f ground) {
        int col = inst.color;
        float fade = life(inst, age, 0, 8), fall = VfxAnim.easeInCubic(ramp(age, 0, 12f)), R = Mth.clamp(SkillAnime2Vfx.skillExtra(inst.power), 3f, 20f);
        Vector3f head = lerp(sky, ground, fall), tail = lerp(sky, ground, Math.max(0, fall - 0.3f));
        if (fall < 1f) {
            buf.beam(ctx, STREAK, VfxBlend.ADD, tail, head, 1.5f, 3.0f, 1, 0, al(col, 0.2f * fade), al(col, 0.7f * fade));
            buf.beam(ctx, STREAK, VfxBlend.ADD, tail, head, 0.5f, 1.4f, 1, 0, al(hot(col, 0.7f), 0.3f * fade), al(WHITE, 0.95f * fade));
            buf.billboard(ctx, GLOW, VfxBlend.ADD, head, 5f, 0, al(col, 0.6f * fade));
            buf.billboard(ctx, SPARKLE, VfxBlend.ADD, head, 4f, age * 0.3f, al(WHITE, fade));
        } else {
            float land = age - 12f, open = VfxAnim.easeOutCubic(ramp(land, 0, 8f));
            flash(ctx, buf, at(ground, 0, 1f, 0), 6f * (1 - land / 14f), land * 0.2f, col, Math.max(0, 1 - land / 10f));
            buf.ring(RING, VfxBlend.ADD, VfxPose.ground(at(ground, 0, 0.08f, 0)), R * open * 0.85f, R * open, ctx.seg(20, 10), 5, 0, al(col, 0.8f * fade * (1 - open * 0.6f)));
            int n = ctx.seg(8, 4);
            for (int i = 0; i < n; i++) {
                float ang = Mth.TWO_PI * i / n, rad = R * 0.7f * open;
                buf.billboard(ctx, SPARKLE, VfxBlend.ADD, at(ground, Mth.cos(ang) * rad, 0.4f + 2f * Mth.sin(open * Mth.PI), Mth.sin(ang) * rad), 0.7f, ang, al(hot(col, 0.6f), (1 - open * 0.5f) * fade));
            }
        }
    }

    private static void visionVoid(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age, Vector3f c, float R) {
        int col = inst.color;
        float fade = life(inst, age, 4, 12), open = VfxAnim.easeOutBack(ramp(age, 0, 10f)), rr = Mth.clamp(R, 2f, 14f);
        Vector3f core = at(c, 0, 2.2f, 0);
        buf.billboard(ctx, CORE, VfxBlend.ALPHA, core, rr * 0.5f * open, 0, al(INK, 0.95f * fade));
        buf.billboard(ctx, GLOW, VfxBlend.ADD, core, rr * 0.9f * open, 0, al(col, 0.3f * fade));
        buf.plane(RUNE, VfxBlend.ALPHA, VfxPose.ground(at(c, 0, 0.06f, 0)).spin(age * 0.03f), rr * open, al(INK, 0.6f * fade));
        buf.plane(RUNE, VfxBlend.ADD, VfxPose.ground(at(c, 0, 0.08f, 0)).spin(-age * 0.05f), rr * 0.9f * open, al(col, 0.6f * fade));
        for (int k = 0; k < 3; k++) {                                                  // rings fall into the void
            float ph = Mth.frac(age * 0.04f + k / 3f);
            buf.ring(RING, VfxBlend.ADD, camPose(core), rr * (1 - ph) * 0.8f + 0.2f, rr * (1 - ph) * 0.8f + 0.4f, ctx.seg(14, 7), 3, 0, al(col, ph * 0.6f * fade));
        }
        RandomSource r = inst.random();
        int n = ctx.seg(8, 4);
        for (int i = 0; i < n; i++) {                                                  // levitating debris
            float ph = Mth.frac(age * 0.035f + r.nextFloat()), ang = r.nextFloat() * Mth.TWO_PI, rad = rr * 0.8f * Mth.sqrt(r.nextFloat());
            Vector3f p = at(c, Mth.cos(ang + ph) * rad, 0.3f + ph * 3.5f, Mth.sin(ang + ph) * rad);
            if (i % 2 == 0) buf.billboard(ctx, ROCK, VfxBlend.ALPHA, p, 0.4f, ang + age * 0.1f, al(0xFF3A2A4A, fade));
            else buf.billboard(ctx, FACET, VfxBlend.ALPHA, p, 0.4f, ang + age * 0.1f, al(col, 0.8f * fade));
        }
        if (age % 6f < 2f) zigzag(ctx, buf, core, at(core, (r.nextFloat() - 0.5f) * rr, (r.nextFloat() - 0.5f) * 2f, (r.nextFloat() - 0.5f) * rr), 4, 0.4f, 0.08f, inst.seed + (long) (age / 6f), col, fade);
    }

    private static void visionSelf(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, float age, Vector3f c) {
        int col = inst.color;
        float fade = life(inst, age, 2, 9), open = VfxAnim.easeOutBack(ramp(age, 0, 6f));
        for (int k = 0; k < 3; k++) {                                                  // three rings climb the body
            float ph = ramp(age, k * 2.5f, 10f);
            if (ph > 0 && ph < 1) buf.ring(RING, VfxBlend.ADD, VfxPose.ground(at(c, 0, 0.1f + 2.2f * VfxAnim.easeOutCubic(ph), 0)), 0.55f + 0.5f * ph, 0.75f + 0.6f * ph, ctx.seg(14, 7), 3, age * 0.04f, al(col, (1 - ph) * fade));
        }
        buf.billboard(ctx, GLOW, VfxBlend.ADD, at(c, 0, 1.1f, 0), 3.6f * open, 0, al(col, 0.3f * fade));
        buf.billboard(ctx, RAYS, VfxBlend.ADD, at(c, 0, 2.2f, 0), 2.6f * open, age * 0.02f, al(hot(col, 0.5f), 0.7f * fade));
        int n = ctx.seg(8, 4);
        for (int i = 0; i < n; i++) {                                                  // streaks of light climb round the body
            float ph = Mth.frac(age * 0.06f + i / (float) n), ang = Mth.TWO_PI * i / n;
            Vector3f a = at(c, Mth.cos(ang) * 0.7f, ph * 2.2f, Mth.sin(ang) * 0.7f);
            buf.beam(ctx, STREAK, VfxBlend.ADD, a, at(a, 0, 0.7f, 0), 0.12f, 0.06f, 1, 0, al(col, 0.8f * fade * Mth.sin(ph * Mth.PI)), al(hot(col, 0.6f), 0.8f * fade * Mth.sin(ph * Mth.PI)));
        }
    }
}
