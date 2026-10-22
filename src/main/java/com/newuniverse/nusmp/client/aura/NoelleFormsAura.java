package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.book.NoelleFormRules;
import com.newuniverse.nusmp.book.NoelleFormRules.Form;
import com.newuniverse.nusmp.book.NoelleFormRules.Lod;
import com.newuniverse.nusmp.book.NoelleFormRules.Recipe;
import com.newuniverse.nusmp.client.aura.NoelleFormRig.Piece;
import com.newuniverse.nusmp.client.aura.WaterAuraRig.Anchor;
import com.newuniverse.nusmp.client.prop.PropDraw;
import com.newuniverse.nusmp.client.prop.SealDraw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Why: the look of Noelle Silva's armour forms (docs/magic/noelle_forms.md), drawn through the existing Water armour slot ({@code Aura.WATER}, see {@link WaterAura#register}): the style byte says the
 * form (low four bits: 1 Valkyrie / Saint Valkyrie Dress, 2 Mermaid Form, 3 Sea Dragon God Form, 4 Black Dragon Form) and the tier (bit 16: the character tier, Noelle's grimoire; every other Water mage
 * draws the standard tier), see {@link NoelleFormRules}. Style 0 (Sea Dragon's Scale Mail) and, with the client switch {@code noelleForms.classicLook}, style 1 are the first painter's own and {@link #paint}
 * answers false for them.
 *
 * <pre>
 *   pass                          render type          what
 *   1 skin shell                  translucent          a film of water / scales / ink over the body (the head is left alone: the face stays visible)
 *   2 pieces                      cutout / translucent geo models of the form (tools/gen_noelle_forms_geo.py): armour, wings, tail, horns ..., + the glow maps additive; forged in and shattered out
 *   3 haze                        additive             a faint sheen over the body
 *   4 ground                      additive             the seal turning on the ground, ripples, and for the first 28 ticks a pillar of water rising round the wearer
 *   5 ribbons, rings, drops       additive             winding ribbons (water or ink), halo / gyro rings (flat strips), orbiting feathers / bubbles / embers
 *   6 back seal, eyes             additive             the big seal behind the back (character tier), glowing eyes (dragon forms)
 * </pre>
 * The cost is bounded by {@link NoelleFormRules#recipe}: ribbons, drops and rings per form and level of detail (a distant player gets none of them), at most {@link NoelleFormRules#QUAD_CAP} sprite
 * quads. Nothing is allocated per frame (the scratch arrays below belong to the render thread).
 */
public final class NoelleFormsAura {
    private NoelleFormsAura() {}

    private static final int FB = PropDraw.FULL_BRIGHT;
    private static final ResourceLocation RING = AuraRender.tex("aura/noelle_ring"), RIBBON = AuraRender.tex("aura/noelle_ribbon"), FEATHER = AuraRender.tex("aura/noelle_feather"),
            EMBER = AuraRender.tex("aura/noelle_ember"), EYE = AuraRender.tex("aura/noelle_eye"), BUBBLE = AuraRender.tex("aura/noelle_bubble"), SPARK = AuraRender.tex("aura/noelle_spark"),
            DOT = AuraRender.tex("aura/noelle_dot"), HAZE = AuraRender.tex("aura/noelle_haze"), RIPPLE = AuraRender.tex("particle/waterfx_ripple");

    /** Everything that differs between the forms but the geo pieces: colours, textures. */
    private record Look(NoelleFormRig.Set set, ResourceLocation shell, ResourceLocation sigil, ResourceLocation drop, int glow, int tint, int main, int accent, int hazeRgb, float glowBoost) {}

    // ---------------------------------------------------------------------------------------------------------------- the sets
    private static final NoelleFormRig.Set VAL = new NoelleFormRig.Set("nvl",
            Piece.of("chest", Anchor.BODY, 1, 0, 0, -14),
            Piece.of("skirt", Anchor.BODY, 5, 0, 10, -6).clip("idle", 1f).glass(),
            Piece.of("pauldron_r", Anchor.ARM_R, 2, -14, -4, 0),
            Piece.of("pauldron_l", Anchor.ARM_L, 2, 14, -4, 0),
            Piece.of("vambrace_r", Anchor.ARM_R, 4, -12, 2, -6),
            Piece.of("vambrace_l", Anchor.ARM_L, 4, 12, 2, -6),
            Piece.of("gauntlet_r", Anchor.ARM_R, 5, -10, 8, -6).detail(),
            Piece.of("gauntlet_l", Anchor.ARM_L, 5, 10, 8, -6).detail(),
            Piece.of("greave_r", Anchor.LEG_R, 6, -3, 14, -6),
            Piece.of("greave_l", Anchor.LEG_L, 7, 3, 14, -6),
            Piece.of("helm", Anchor.HEAD, 0, 0, -16, 0).head(),
            Piece.of("wings", Anchor.BODY, 8, 0, -4, 14).clip("idle", 1f).flap().glass(),
            Piece.of("halo", Anchor.HEAD, 10, 0, -16, 0).clip("idle", 1f).glass().detail().head(),
            Piece.of("wings2", Anchor.BODY, 9, 0, 4, 14).clip("idle", 1f).flap().glass().character(),
            Piece.of("aureole", Anchor.BODY, 12, 0, 0, 18).clip("idle", 1f).glass().detail().character(),
            Piece.of("sash", Anchor.BODY, 6, 0, 6, -8).clip("idle", 1f).glass().detail().character(),
            Piece.of("lance", Anchor.BODY, 11, -14, 0, -4).clip("idle", 1f).glass().detail().character());

    private static final NoelleFormRig.Set MER = new NoelleFormRig.Set("nml",
            Piece.of("bust", Anchor.BODY, 1, 0, 0, -14),
            Piece.of("train", Anchor.BODY, 6, 0, 8, 12).clip("idle", 1f).glass(),
            Piece.of("veil", Anchor.BODY, 5, 0, 6, 8).clip("idle", 1f).glass().detail(),
            Piece.of("leg_r", Anchor.LEG_R, 4, -3, 14, -6),
            Piece.of("leg_l", Anchor.LEG_L, 4, 3, 14, -6),
            Piece.of("vambrace_r", Anchor.ARM_R, 3, -12, 2, -6).clip("idle", 1f).glass(),
            Piece.of("vambrace_l", Anchor.ARM_L, 3, 12, 2, -6).clip("idle", 1f).glass(),
            Piece.of("crown", Anchor.HEAD, 0, 0, -16, 0).glass().head(),
            Piece.of("hair", Anchor.HEAD, 2, 0, -8, 6).clip("idle", 1f).glass().head(),
            Piece.of("trident", Anchor.BODY, 8, -14, 0, -4).clip("idle", 1f).glass().detail(),
            Piece.of("dorsal", Anchor.BODY, 9, 0, 0, 14).clip("idle", 1f).glass().character(),
            Piece.of("scarf", Anchor.BODY, 7, 0, -4, 10).clip("idle", 1f).glass().detail().character(),
            Piece.of("coral", Anchor.HEAD, 10, 0, -14, 0).clip("idle", 1f).glass().detail().head().character());
    private static final NoelleFormRig.Set DRG = dragonSet("ndg"), BLK = dragonSet("nbk");

    /** The Sea Dragon God Form and the Black Dragon Form are one silhouette in two palettes (tools/noelle_dragon_kit.py): the same pieces under two model keys. */
    private static NoelleFormRig.Set dragonSet(String key) {
        return new NoelleFormRig.Set(key,
                Piece.of("chest", Anchor.BODY, 1, 0, 0, -14),
                Piece.of("tassets", Anchor.BODY, 5, 0, 10, -6).clip("idle", 1f),
                Piece.of("pauldron_r", Anchor.ARM_R, 2, -14, -4, 0),
                Piece.of("pauldron_l", Anchor.ARM_L, 2, 14, -4, 0),
                Piece.of("vambrace_r", Anchor.ARM_R, 4, -12, 2, -6),
                Piece.of("vambrace_l", Anchor.ARM_L, 4, 12, 2, -6),
                Piece.of("greave_r", Anchor.LEG_R, 6, -3, 14, -6),
                Piece.of("greave_l", Anchor.LEG_L, 7, 3, 14, -6),
                Piece.of("helm", Anchor.HEAD, 0, 0, -16, 0).head(),
                Piece.of("wings", Anchor.BODY, 8, 0, -4, 14).clip("idle", 1f).flap().glass(),
                Piece.of("tail", Anchor.BODY, 9, 0, 8, 14).clip("idle", 1f).glass(),
                Piece.of("cannon_r", Anchor.ARM_R, 11, -10, 6, -6).clip("idle", 1f),
                Piece.of("ridge", Anchor.BODY, 10, 0, -6, 10).clip("idle", 1f).glass().detail().character(),
                Piece.of("horns2", Anchor.HEAD, 10, 0, -14, 0).detail().head().character(),
                Piece.of("wings2", Anchor.BODY, 11, 0, 4, 14).clip("idle", 1f).flap().glass().character(),
                Piece.of("cannon_l", Anchor.ARM_L, 12, 10, 6, -6).clip("idle", 1f).detail().character());
    }

    private static final Look L_VAL = new Look(VAL, AuraRender.tex("aura/noelle_val_shell"), AuraRender.tex("aura/noelle_sigil_val"), FEATHER, 0xBFF4FF, 0xFFFFFF, 0xFFE49A, 0x9CE8FF, 0xBFEFFF, 0.55f);
    private static final Look L_MER = new Look(MER, AuraRender.tex("aura/noelle_mer_shell"), AuraRender.tex("aura/noelle_sigil_mer"), BUBBLE, 0x9CF8FF, 0xFFFFFF, 0x7CF0FF, 0xFFB8E8, 0x9CF4FF, 0.5f);
    private static final Look L_DRG = new Look(DRG, AuraRender.tex("aura/noelle_drg_shell"), AuraRender.tex("aura/noelle_sigil_drg"), SPARK, 0x6CF0FF, 0xFFFFFF, 0x46D8FF, 0xE8FFFF, 0x6CC8FF, 0.5f);
    private static final Look L_BLK = new Look(BLK, AuraRender.tex("aura/noelle_blk_shell"), AuraRender.tex("aura/noelle_sigil_blk"), EMBER, 0x4C8CFF, 0xFFFFFF, 0x2C7CFF, 0xFF3A3A, 0x2C5CFF, 0.6f);

    private static Look look(Form f) {
        return switch (f) {
            case MERMAID -> L_MER;
            case SEA_DRAGON -> L_DRG;
            case BLACK_DRAGON -> L_BLK;
            default -> L_VAL;
        };
    }

    // ---------------------------------------------------------------------------------------------------------------- entry
    /**
     * Draws the form of {@code c} if its style is one of the remade forms. Returns false (and draws nothing) for style 0 and, with the classic switch, for style 1: the caller then runs the first
     * Water painter.
     */
    public static boolean paint(AuraContext c) {
        int style = c.style();
        Form f = NoelleFormRules.form(style);
        if (f == Form.ARMOUR || f == Form.VALKYRIE && NoelleFormsOptions.classic()) return false;
        float fade = c.fade();
        if (fade <= 0.002f) return true;
        boolean tierC = NoelleFormRules.character(style);
        Lod lod = frameBudget(c.player().getId(), NoelleFormRules.lod(NoelleFormRig.dist2(c), tierC));
        if (lod == Lod.NONE) return true;
        Look k = look(f);
        Recipe r = NoelleFormRules.recipe(f, lod);
        float age = c.age(), pulse = 0.82f + 0.18f * Mth.sin(age * 0.17f) + 0.07f * Mth.sin(age * 0.53f);
        float forge = WaterAuraRig.forge(c, 0f);
        if (r.skin()) WaterAuraRig.shell(c, AuraRender.translucent(k.shell), 1.04f, 1.025f, 1.04f, AuraRender.alpha(0xFFFFFFFF, 0.62f * forge), false, true);
        NoelleFormRig.draw(c, k.set, lod, k.glow, 0.78f * pulse + k.glowBoost, k.tint);
        if (r.skin()) {
            float b = 1.08f + 0.012f * Mth.sin(age * 0.13f);
            WaterAuraRig.shell(c, AuraRender.additive(HAZE), b, 1.05f, b, AuraRender.alpha(0xFF000000 | k.hazeRgb, 0.17f * pulse * forge), true, true);
        }
        extras(c, k, f, r, forge, pulse, tierC);
        return true;
    }

    // ---------------------------------------------------------------------------------------------------------------- the per-frame budget of a crowd
    private static final int[] SEEN = new int[16];
    private static int seenN, charDrawn, fullDrawn;

    /**
     * A crowd costs a bounded number of full figures ({@link NoelleFormRules#budgeted}): the players are drawn in one pass per frame, so a player that comes round a second time starts a new
     * frame and the counters restart. The first players drawn get the full figure, the rest fall back one level (then to the distant one). Render thread only; nothing allocated.
     */
    private static Lod frameBudget(int playerId, Lod wanted) {
        for (int i = 0; i < seenN; i++) {
            if (SEEN[i] == playerId) { seenN = 0; charDrawn = 0; fullDrawn = 0; break; }
        }
        if (seenN >= SEEN.length) { seenN = 0; charDrawn = 0; fullDrawn = 0; }
        SEEN[seenN++] = playerId;
        Lod l = NoelleFormRules.budgeted(wanted, charDrawn, fullDrawn);
        if (l == Lod.CHARACTER) { charDrawn++; fullDrawn++; } else if (l == Lod.STANDARD) fullDrawn++;
        return l;
    }

    // ---------------------------------------------------------------------------------------------------------------- the dynamic parts
    private static final float[] PT = new float[3];

    private static void extras(AuraContext c, Look k, Form f, Recipe r, float forge, float pulse, boolean tierC) {
        if (forge <= 0.01f) return;
        PoseStack pose = c.pose();
        SealDraw.camera(pose);
        float age = c.age();
        if (r.sigil()) ground(c, k, f, forge, pulse, tierC);
        if (r.ribbons() > 0) {
            VertexConsumer vc = c.buffers().getBuffer(AuraRender.additive(RIBBON));
            for (int i = 0; i < r.ribbons(); i++) ribbon(pose, vc, k, f, i, r.ribbonSegs(), age, forge, tierC);
        }
        if (r.rings() > 0) {
            VertexConsumer vc = c.buffers().getBuffer(AuraRender.additive(RING));
            for (int i = 0; i < r.rings(); i++) ring(pose, vc, k, f, i, r.ringSegs(), age, forge);
        }
        if (r.drops() > 0) drops(c, k, f, r.drops(), age, forge);
        if (r.backSigil()) backSeal(c, k, age, forge, pulse);
        if (r.eyes()) eyes(c, k, f, forge, pulse);
    }

    private static float frac(float x) { return x - (float) Math.floor(x); }

    /** The seal on the ground (turning, with two counter-turning ripples), and for the first 28 ticks a pillar of water that rises round the wearer. */
    private static void ground(AuraContext c, Look k, Form f, float forge, float pulse, boolean tierC) {
        PoseStack pose = c.pose();
        float age = c.age(), t = c.t();
        float grow = 0.45f + 0.55f * forge, size = (tierC ? 1.45f : 1.1f) * grow;
        pose.pushPose();
        pose.translate(0f, 1.478f, 0f);
        VertexConsumer ripple = c.buffers().getBuffer(AuraRender.additive(RIPPLE));
        for (int i = 0; i < 3; i++) {
            float ph = frac(age * 0.012f + i / 3f);
            SealDraw.flat(pose, ripple, (0.5f + 1.15f * ph) * grow, age * 0.4f, SealDraw.argb((1f - ph) * 0.8f * forge, i == 1 ? k.accent : k.glow), FB);
        }
        VertexConsumer sigil = c.buffers().getBuffer(AuraRender.additive(k.sigil));
        SealDraw.flat(pose, sigil, size, age * 0.7f, SealDraw.argb(0.78f * forge * pulse, k.main), FB);
        pose.translate(0f, -0.004f, 0f);
        SealDraw.flat(pose, sigil, size * 0.62f, -age * 1.1f, SealDraw.argb(0.55f * forge * pulse, k.accent), FB);
        pose.popPose();
        if (t < 28f) {                                                                              // the pillar: six strips of rising water round the feet
            float e = 1f - t / 28f, rise = SealDraw.smooth(t / 12f);
            VertexConsumer vc = c.buffers().getBuffer(AuraRender.additive(RIBBON));
            for (int i = 0; i < 6; i++) {
                float a = i * Mth.TWO_PI / 6f + age * 0.18f, rad = (0.95f - 0.35f * rise) * (0.6f + 0.4f * e) * (tierC ? 1.25f : 1f);
                float x = Mth.cos(a) * rad, z = Mth.sin(a) * rad;
                SealDraw.ribbon(pose, vc, x, 1.5f, z, x * 0.8f, 1.5f - 3.0f * rise, z * 0.8f, 0.16f, 0f, 1f, SealDraw.argb(0.7f * e * e, i % 2 == 0 ? k.glow : k.accent), FB);
            }
        }
    }

    /** One winding ribbon: shoulders / waist / back, per form. The positions are written to {@link #PT}. */
    private static void ribbonPoint(Form f, int idx, float s, float age, boolean tierC) {
        int side = (idx & 1) == 0 ? -1 : 1;
        int lane = idx >> 1;
        float ph = idx * 1.7f, w = age * 0.11f;
        float x, y, z;
        switch (f) {
            case MERMAID -> {                                                                       // from the hips along the tail, in S-curves
                x = side * (0.26f + 0.34f * s) + 0.30f * s * Mth.sin(w * 1.3f + s * 6f + ph);
                y = 0.55f + 1.0f * s + 0.12f * Mth.sin(w + ph);
                z = 0.10f + (0.6f + 0.5f * lane) * s * 0.9f + 0.18f * Mth.cos(w + s * 5f + ph);
            }
            case SEA_DRAGON -> {                                                                    // a serpent of water coiling up the body
                float a = ph + w * 1.2f + s * Mth.TWO_PI * (1.1f + 0.25f * lane), rad = 0.58f + 0.12f * Mth.sin(s * 5f + w);
                x = Mth.cos(a) * rad;
                y = 1.46f - s * 1.55f;                                                              // up to the shoulders: the face stays clear
                z = Mth.sin(a) * rad;
            }
            case BLACK_DRAGON -> {                                                                  // tendrils of ink from the shoulder blades, licking up and back like flame
                x = side * (0.22f + 0.55f * s) + 0.25f * s * Mth.sin(w * 1.6f + s * 7f + ph);
                y = 0.12f - 1.25f * s + 0.30f * s * Mth.sin(w * 1.1f + s * 4f + ph);
                z = 0.18f + 0.95f * s + 0.28f * s * Mth.cos(w * 1.3f + s * 5f + ph) + 0.1f * lane;
            }
            default -> {                                                                            // Valkyrie: ribbons of water from the shoulders, streaming back and out
                float lift = lane == 0 ? 0f : 0.42f;
                x = side * (0.30f + 0.95f * s) + 0.24f * s * Mth.sin(w + s * 6f + ph);
                y = 0.10f + lift * s - 0.28f * Mth.sin(Mth.PI * s * 0.8f) + 0.85f * s * s + 0.26f * s * Mth.sin(w * 0.8f + s * 4.5f + ph);
                z = 0.20f + 1.45f * s * (0.75f + 0.25f * (lane == 0 ? 1f : 0.6f)) + 0.12f * Mth.sin(w + s * 3f + ph);
            }
        }
        PT[0] = x; PT[1] = y; PT[2] = z;
    }

    private static void ribbon(PoseStack pose, VertexConsumer vc, Look k, Form f, int idx, int segs, float age, float forge, boolean tierC) {
        float px = 0f, py = 0f, pz = 0f;
        for (int i = 0; i <= segs; i++) {
            float s = i / (float) segs;
            ribbonPoint(f, idx, s, age, tierC);
            if (i > 0) {
                float hw = (f == Form.SEA_DRAGON ? 0.15f : 0.125f) * (1f - 0.5f * s) * (tierC ? 1.15f : 1f);
                float a = (float) Math.pow(Math.sin(Mth.PI * Math.min(1f, (s - 0.5f / segs) * 1.02f + 0.02f)), 0.6) * 0.9f * forge;
                SealDraw.ribbon(pose, vc, px, py, pz, PT[0], PT[1], PT[2], hw, (i - 1) / (float) segs, s, SealDraw.argb(a, (idx & 1) == 0 ? k.glow : k.accent), FB);
            }
            px = PT[0]; py = PT[1]; pz = PT[2];
        }
    }

    // ring table per form: {centre height (model space, y down), radius, half width, tilt x, tilt z, spin rate, colour 0 = main / 1 = accent / 2 = glow}
    private static final float[][] RINGS_VAL = {{-0.80f, 0.60f, 0.075f, 0f, 0f, 0.5f, 0}, {-0.05f, 1.10f, 0.06f, 1.15f, 0.2f, 0.9f, 1}, {0.10f, 1.30f, 0.06f, -0.3f, 1.2f, -0.7f, 2}};
    private static final float[][] RINGS_MER = {{0.85f, 0.85f, 0.07f, 0.25f, 0.1f, 0.8f, 0}, {0.15f, 1.05f, 0.06f, -0.35f, 0.2f, -0.6f, 1}};
    private static final float[][] RINGS_DRG = {{-0.85f, 0.58f, 0.07f, 0.12f, 0f, 0.7f, 2}, {0.0f, 1.1f, 0.06f, 0.7f, 0.1f, 0.9f, 0}, {0.1f, 1.35f, 0.06f, -0.4f, 1.0f, -0.6f, 1}};
    private static final float[][] RINGS_BLK = {{-0.82f, 0.62f, 0.075f, -0.15f, 0.1f, -0.9f, 1}, {0.05f, 1.2f, 0.065f, 0.9f, 0.3f, 0.7f, 0}};

    private static float[][] ringTable(Form f) {
        return switch (f) {
            case MERMAID -> RINGS_MER;
            case SEA_DRAGON -> RINGS_DRG;
            case BLACK_DRAGON -> RINGS_BLK;
            default -> RINGS_VAL;
        };
    }

    /** A flat ring strip (both sides) of {@code segs} quads, tilted about x then z, turning about its own axis. */
    private static void ring(PoseStack pose, VertexConsumer vc, Look k, Form f, int idx, int segs, float age, float forge) {
        float[][] t = ringTable(f);
        if (idx >= t.length) return;
        float[] p = t[idx];
        float cy = p[0], rad = p[1], hw = p[2], tx = p[3], tz = p[4], spin = age * 0.04f * p[5];
        int rgb = p[6] == 0 ? k.main : p[6] == 1 ? k.accent : k.glow;
        int argb = SealDraw.argb(0.85f * forge, rgb);
        float cxr = Mth.cos(tx), sxr = Mth.sin(tx), czr = Mth.cos(tz), szr = Mth.sin(tz);
        float step = Mth.TWO_PI / segs;
        for (int s = 0; s < segs; s++) {
            float a0 = spin + s * step, a1 = a0 + step;
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            float u0 = s * 6f / segs, u1 = (s + 1) * 6f / segs;
            // inner0, outer0, outer1, inner1 on the ring plane (y = 0), tilted
            float ax = c0 * (rad - hw), az = s0 * (rad - hw), bx = c0 * (rad + hw), bz = s0 * (rad + hw), cx = c1 * (rad + hw), cz = s1 * (rad + hw), dx = c1 * (rad - hw), dz = s1 * (rad - hw);
            // tilt about x: (y, z); then about z: (x, y)
            float ay = -az * sxr, a2 = az * cxr, by = -bz * sxr, b2 = bz * cxr, cy2 = -cz * sxr, c2 = cz * cxr, dy = -dz * sxr, d2 = dz * cxr;
            float Ax = ax * czr - ay * szr, Ay = ax * szr + ay * czr + cy, Bx = bx * czr - by * szr, By = bx * szr + by * czr + cy;
            float Cx = cx * czr - cy2 * szr, Cy = cx * szr + cy2 * czr + cy, Dx = dx * czr - dy * szr, Dy = dx * szr + dy * czr + cy;
            PropDraw.quad(pose, vc, Ax, Ay, a2, Bx, By, b2, Cx, Cy, c2, Dx, Dy, d2, u0, 0f, u1, 1f, argb, FB);
            PropDraw.quad(pose, vc, Dx, Dy, d2, Cx, Cy, c2, Bx, By, b2, Ax, Ay, a2, u1, 0f, u0, 1f, argb, FB);
        }
    }

    /** Feathers / bubbles / embers / sparks rising and orbiting round the body, each on its own phase. */
    private static void drops(AuraContext c, Look k, Form f, int n, float age, float forge) {
        PoseStack pose = c.pose();
        VertexConsumer vc = c.buffers().getBuffer(AuraRender.additive(k.drop));
        boolean fall = f == Form.VALKYRIE;
        for (int i = 0; i < n; i++) {
            float speed = 0.012f * (1f + 0.3f * (i % 3));
            float ph = frac(age * speed + i * 0.381f);
            float a = i * 2.399f + age * (f == Form.SEA_DRAGON ? 0.03f : 0.02f);
            float rad = 0.55f + 0.30f * Mth.sin(i * 1.7f) + (f == Form.BLACK_DRAGON ? 0.18f : 0f);
            float y = fall ? -0.9f + 2.2f * ph : 1.5f - 2.5f * ph;                                  // feathers drift down from above, the others rise from the feet
            float size = (f == Form.VALKYRIE ? 0.10f : 0.07f) + 0.035f * Mth.sin(i * 2.3f);
            int rgb = i % 3 == 0 ? k.accent : k.glow;
            float rot = f == Form.VALKYRIE ? age * 0.05f + i : 0f;
            SealDraw.glow(pose, vc, Mth.cos(a) * rad, y, Mth.sin(a) * rad, size, rot, SealDraw.argb(Mth.sin(ph * Mth.PI) * 0.95f * forge, rgb), FB);
        }
    }

    /** The great seal standing behind the back, turning slowly (character tier). */
    private static void backSeal(AuraContext c, Look k, float age, float forge, float pulse) {
        float back = WaterAuraRig.forge(c, 12f);
        if (back <= 0.01f) return;
        PoseStack pose = c.pose();
        pose.pushPose();
        c.model().body.translateAndRotate(pose);
        VertexConsumer sg = c.buffers().getBuffer(AuraRender.additive(k.sigil));
        float r = 0.95f * (0.6f + 0.4f * back), a = age * 0.01f, ux = Mth.cos(a) * r, uy = Mth.sin(a) * r;
        int col = SealDraw.argb(0.42f * back * pulse, k.glow);
        SealDraw.plane(pose, sg, 0f, 0.20f, 0.78f, ux, uy, 0f, -uy, ux, 0f, false, col, FB);
        SealDraw.plane(pose, sg, 0f, 0.20f, 0.78f, ux, uy, 0f, -uy, ux, 0f, true, col, FB);
        pose.popPose();
    }

    /** Glowing eyes (dragon forms): two almond flares on the face with a faint streak, skipped for the local player in first person. */
    private static void eyes(AuraContext c, Look k, Form f, float forge, float pulse) {
        if (f != Form.SEA_DRAGON && f != Form.BLACK_DRAGON || NoelleFormRig.selfFirstPerson(c)) return;
        float e = WaterAuraRig.forge(c, 2f);
        if (e <= 0.01f) return;
        PoseStack pose = c.pose();
        pose.pushPose();
        c.model().head.translateAndRotate(pose);
        VertexConsumer vc = c.buffers().getBuffer(AuraRender.additive(EYE));
        int col = SealDraw.argb(0.95f * e * pulse, f == Form.BLACK_DRAGON ? 0xFF2A2A : 0x9CF8FF);
        for (int side = -1; side <= 1; side += 2) {
            SealDraw.plane(pose, vc, side * 0.135f, -0.272f, -0.272f, 0.115f, 0f, 0f, 0f, 0.058f, 0f, true, col, FB);
        }
        pose.popPose();
    }
}
