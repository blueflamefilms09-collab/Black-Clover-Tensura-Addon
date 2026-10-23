package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.book.SpiritDivePieces;
import com.newuniverse.nusmp.book.SpiritDivePieces.Element;
import com.newuniverse.nusmp.book.SpiritDivePieces.Stage;
import com.newuniverse.nusmp.book.SpiritDiveReelGeometry;
import com.newuniverse.nusmp.book.SpiritDiveReelRules;
import com.newuniverse.nusmp.book.SpiritDiveReelRules.Pal;
import com.newuniverse.nusmp.book.SpiritDiveTier;
import com.newuniverse.nusmp.book.SpiritDiveTier.Grade;
import com.newuniverse.nusmp.book.SpiritDiveTier.Lod;
import com.newuniverse.nusmp.book.SpiritDiveWind3Geometry;
import com.newuniverse.nusmp.book.SpiritDiveWind3Mesh;
import com.newuniverse.nusmp.book.SpiritDiveWind3Rules;
import com.newuniverse.nusmp.book.SpiritDiveWind3Rules.Spec;
import com.newuniverse.nusmp.book.SpiritDiveWind3Rules.Ver;
import com.newuniverse.nusmp.book.SpiritDiveWind4Rules;
import com.newuniverse.nusmp.book.SpiritDiveWindGeometry;
import com.newuniverse.nusmp.book.SpiritDiveWindRules;
import com.newuniverse.nusmp.client.SafeAdditive;
import com.newuniverse.nusmp.client.prop.PropDraw;
import com.newuniverse.nusmp.client.prop.SealDraw;
import com.newuniverse.nusmp.perf.PerfProbe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Spirit Dive WIND remake, the three looks with REAL 3D pieces (client option {@code spiritDive.windLook}): <b>A "Anime-true"</b> is the PACK look ({@link SpiritDiveWind}) whose weapons are now real models
 * ({@link #weaponsA}); <b>B "Clip-style"</b> and <b>C "Saint Spirit"</b> are painted here. Called by {@link SpiritDiveReel#paint} for the element Wind. The pieces are the shared meshes of
 * {@link SpiritDiveWind3Mesh} (authored by {@code tools/gen_sdw3_models.py}); nothing is allocated per frame (static scratch arrays, cached textures, shared meshes), no packet, no server work, no state
 * of any player, at most {@link SpiritDiveTier#CHARACTER_PER_FRAME} character tiers a frame ({@link SpiritDiveFrame}), the vertices counted by {@link SpiritDiveWind3Rules#vertices}, timed in the probe
 * section {@code c.spiritdive.wind3}.
 *
 * <ul>
 *   <li><b>B</b>: the body light in mint and teal, a crown ring of glowing cubes and tall cube bars with white lights, cube gauntlets, a voxel leaf wing of stacked glowing panels (a small one on the
 *       other side for Yuno), the real sword with a flat magic circle round the hilt and a second blade standing behind the shoulder, white ribbons, the ground circle, the entry of the REEL look in the
 *       colours of the design.</li>
 *   <li><b>C</b>: the body light in white with a green breath, wings of long spirit feathers fanned from the shoulder blades, a tall crown of 3D arrow bars (the standard tier: the pack's bar crown),
 *       pauldron plates of light, a halo of small spirit arrows circling the body; in a cast a big arrow of light flies out of the hand.</li>
 * </ul>
 */
public final class SpiritDiveWind3 {
    private SpiritDiveWind3() {}

    private static final int FB = PropDraw.FULL_BRIGHT;
    private static final PerfProbe.Section SECTION = PerfProbe.section("c.spiritdive.wind3");                  // timed (/multiverse perf)

    private static final SpiritDiveWind3Mesh SWORD = SpiritDiveWind3Mesh.get("sword"), BOW = SpiritDiveWind3Mesh.get("bow"), ARROW = SpiritDiveWind3Mesh.get("arrow"),
            SPEAR = SpiritDiveWind3Mesh.get("spear"), BLADE = SpiritDiveWind3Mesh.get("blade"), FEATHER = SpiritDiveWind3Mesh.get("feather"), CUBE = SpiritDiveWind3Mesh.get("cube"),
            PANEL = SpiritDiveWind3Mesh.get("panel"), PLATE = SpiritDiveWind3Mesh.get("plate"), CROWN = SpiritDiveWind3Mesh.get("crown");
    private static final ResourceLocation SOFT = AuraRender.tex(SpiritDiveWind3Rules.SOFT), STAR = AuraRender.tex(SpiritDiveWindRules.STAR), CIRCLE = AuraRender.tex(SpiritDiveWindRules.CIRCLE),
            GROUND = AuraRender.tex(SpiritDiveReelRules.GROUND);
    private static final float[] G = new float[6], SPARK_POS = new float[4];                                    // scratch

    // ------------------------------------------------------------------ the entry point
    /** Called by {@link SpiritDiveReel} for the Wind of the remake: one dive of Wind at {@code st} for the tier {@code grade} in the design {@code ver}. */
    static void paint(AuraContext c, Stage st, Grade grade, float pulse, Ver ver) {
        if (ver == Ver.A) { SpiritDiveWind.paint(c, st, grade, pulse, true); return; }
        Lod lod = SpiritDiveTier.lod(SpiritDiveFrame.distSq(c));
        if (lod == Lod.CULLED) return;
        long t0 = PerfProbe.begin();
        try {
            paintLod(c, st, grade, pulse, lod, ver);
        } finally {
            PerfProbe.end(SECTION, t0);
        }
    }

    private static void paintLod(AuraContext c, Stage st, Grade grade, float pulse, Lod lod, Ver ver) {
        Element el = Element.WIND;
        boolean wantsCharacter = SpiritDiveWindRules.character(grade) && SpiritDiveLook.characterEffects() && lod != Lod.FAR;
        boolean character = wantsCharacter && SpiritDiveFrame.admitCharacter();
        Spec sp = SpiritDiveWind3Rules.spec(ver, character, lod);
        SpiritDiveWindRules.Spec pack = SpiritDiveWindRules.spec(character, lod);                              // shell passes, sparkles, ribbons: shared with the pack look
        SpiritDiveReelRules.Spec reel = SpiritDiveReelRules.spec(el, character, lod);
        Pal pal = SpiritDiveWind3Rules.pal(ver, character);
        boolean upgrade = st == Stage.UPGRADE, full = SpiritDivePieces.full(st), half = SpiritDivePieces.halfBody(el, st);
        float u = SpiritDiveReelRules.clock(c.t(), c.left());
        float uk = upgrade ? SpiritDiveReelRules.clock(1e4f, c.left()) : u;                                  // an upgrade keeps the half that is on
        if (Math.max(u, uk) <= 0.05f) return;
        boolean casting = DiveCast.casting(c.player().getId());
        float k = casting ? 1.35f : 1f, lit = pulse * k;
        boolean self1p = SpiritDiveWind.firstPersonSelf(c);
        int mask = SpiritDivePieces.shellParts(el, st), keptMask = upgrade ? SpiritDivePieces.shellParts(el, Stage.HALF) & mask : 0;

        SpiritDiveAnimeShell.draw(c, el, pack.shellPasses(), pal.body(), pal.rim(), pal.glow(), pulse * k, SpiritDiveReelRules.assemble(u), mask, false, false, keptMask, k);
        if (lod != Lod.FAR) {
            if (ver == Ver.B) {
                if (!self1p && SpiritDiveReelRules.crowned(el, st) && sp.cubes() > 0) crownB(c, sp, pal, u, uk, upgrade, half, lit);
                if (sp.gauntlets() > 0) gauntletsB(c, sp, mask, keptMask, u, lit);
            } else if (!self1p && SpiritDiveReelRules.crowned(el, st)) {
                if (sp.crown3d()) crownC(c, pal, u, uk, upgrade, lit);
                else if (pack.bars() > 0) SpiritDiveWind.crown(c, pack, character, pal, u, uk, upgrade, half, lit);
            }
            PoseStack pose = c.pose();
            pose.pushPose();                                                                                // the body frame in px, the camera read in it
            pose.scale(1f / 16f, 1f / 16f, 1f / 16f);
            SpiritDiveAnimeRibbon.camera(pose);
            if (ver == Ver.B) {
                wingsB(c, sp, pal, st, u, uk, half, lit);
                if (full && lod == Lod.FULL) weaponsB(c, sp, pal, SpiritDiveReelRules.weapon(u), lit, pulse);
            } else {
                feathersC(c, sp, character, pal, st, u, uk, half, lit);
                platesC(c, sp, st, u, uk, half);
                if (full && lod == Lod.FULL) halo(c, sp, pal, SpiritDiveReelRules.settle(u), uk, half, casting, lit);
            }
            if (full && pack.ribbons() > 0) SpiritDiveWind.ribbons(c, pack, SpiritDiveReelRules.settle(u));
            if (u < SpiritDiveReelRules.ENTRY_TICKS - 0.5f && reel.ribbons() > 0 && !upgrade) SpiritDiveReel.ribbons(c, reel, el, pal, u);
            pose.popPose();
        }
        extras(c, pack, reel, pal, st, u, uk, full, lit, pulse, lod, ver);
    }

    // ------------------------------------------------------------------ small helpers
    private static float alpha(float k) { return Math.min(1f, 0.97f * Math.min(1f, k * 1.5f)); }

    /** Draws a mesh at a root point turned to a direction (the pose is the body or head frame in px). */
    private static void piece(AuraContext c, SpiritDiveWind3Mesh m, int layers, float ox, float oy, float oz, float yx, float yy, float yz, float hx, float hy, float hz, float sx, float sy, float sz,
                              float a, float glowA, int glowRgb) {
        PoseStack pose = c.pose();
        pose.pushPose();
        SpiritDiveWind3Draw.basis(pose, ox, oy, oz, yx, yy, yz, hx, hy, hz, sx, sy, sz);
        SpiritDiveWind3Draw.mesh(c, m, layers, a, glowA, glowRgb);
        pose.popPose();
    }

    // ------------------------------------------------------------------ A: the weapons (body frame, px)
    /**
     * Version A: Yuno's sword floats at his right hip (blade, fuller, cross-guard and the two crossed rings of the guard turning slowly), the leaf bow rides on his back and is in the left hand with a real
     * arrow of light while a page is cast; the standard tier floats a small leaf blade. {@code k} (0..1) draws them out of their circle on entry.
     */
    static void weaponsA(AuraContext c, Pal pal, boolean character, boolean casting, float k, float lit, float pulse) { weaponsA(c, pal, character, casting, k, lit, pulse, 1f); }

    /** The same with the energy {@code e} of the final A: the crossed rings of the sword's guard are drawn only while the dive is loud (entry, exit, a cast and 2 s after it). */
    static void weaponsA(AuraContext c, Pal pal, boolean character, boolean casting, float k, float lit, float pulse, float e) {
        if (k <= 0.02f) return;
        float age = c.age(), a = alpha(k), ga = 0.55f * k * lit;
        if (!character) {
            float[] s = SpiritDiveWind3Geometry.LEAF_BLADE;
            float bob = SpiritDiveWindGeometry.bob(age, 0.6f) * s[7];
            piece(c, BLADE, SpiritDiveWind3Rules.SOLID_GLOW, s[0], s[1] + bob, s[2], s[3], s[4], s[5], 1f, 0f, 0f, s[6] * 0.55f * (0.6f + 0.4f * k), s[6] * k, s[6] * 0.55f * (0.6f + 0.4f * k), a, ga, pal.glow());
            return;
        }
        sword(c, SpiritDiveWind3Geometry.SWORD, pal, k, a, ga, age, 0.035f, true, e);
        float[] b = casting ? SpiritDiveWind3Geometry.BOW_HAND : SpiritDiveWind3Geometry.BOW_BACK;
        float bob = SpiritDiveWindGeometry.bob(age, 1.9f) * b[7];
        piece(c, BOW, SpiritDiveWind3Rules.ALL, b[0], b[1] + bob, b[2], b[3], b[4], b[5], 0f, 0f, -1f, b[6], b[6] * (0.5f + 0.5f * k), b[6], a, 0.5f * k * lit, pal.glow());
        if (casting) arrow(c, b, pal, k, a, lit, bob);
    }

    private static void arrow(AuraContext c, float[] bow, Pal pal, float k, float a, float lit, float bob) {
        piece(c, ARROW, SpiritDiveWind3Rules.ALL, bow[0], bow[1] + bob, bow[2] - SpiritDiveWind3Geometry.ARROW_AT, 0f, 0f, -1f, 0f, 1f, 0f, 1f, k, 1f, a, 0.6f * k * lit, pal.glow());
    }

    /** The sword at a root and direction of the table {@code g}: the blade and guard solid and glowing, the rings turning about the blade axis; {@code soft} adds a soft glow at the hilt. */
    private static void sword(AuraContext c, float[] g, Pal pal, float k, float a, float ga, float age, float spin, boolean soft) { sword(c, g, pal, k, a, ga, age, spin, soft, 1f); }

    private static void sword(AuraContext c, float[] g, Pal pal, float k, float a, float ga, float age, float spin, boolean soft, float rings) {
        float bob = SpiritDiveWindGeometry.bob(age, 0.6f) * g[7];
        float s = g[6], sy = s * k, sxz = s * (0.5f + 0.5f * k);
        PoseStack pose = c.pose();
        pose.pushPose();
        SpiritDiveWind3Draw.basis(pose, g[0], g[1] + bob, g[2], g[3], g[4], g[5], 1f, 0f, 0f, sxz, sy, sxz);
        SpiritDiveWind3Draw.mesh(c, SWORD, 3, a, ga, pal.glow());                                           // blade, guard, grip, pommel and the glow shell
        if (rings > SpiritDiveWind4Rules.QUIET) {
            pose.mulPose(ROT.rotationY(age * spin));
            SpiritDiveWind3Draw.mesh(c, SWORD, 4, a * rings, 0f, pal.glow());                              // the crossed rings of the guard, turning (fade out when the dive is quiet)
        }
        pose.popPose();
        if (soft) SealDraw.glow(pose, c.buffers().getBuffer(SafeAdditive.additive(SOFT)), g[0], g[1] + bob, g[2], 6.5f * k, 0f, SealDraw.argb(Math.min(1f, 0.5f * k * ga), pal.light()), FB);
    }

    private static final org.joml.Quaternionf ROT = new org.joml.Quaternionf();                                // scratch

    // ------------------------------------------------------------------ B: the crown, the gauntlets (head and arm frame, px)
    private static void crownB(AuraContext c, Spec sp, Pal pal, float u, float uLeft, boolean upgrade, boolean leftOnly, float lit) {
        float ign = Math.max(SpiritDiveReelRules.crown(u), upgrade ? SpiritDiveReelRules.crown(uLeft) : 0f);
        if (ign <= 0.01f) return;
        PoseStack pose = c.pose();
        pose.pushPose();
        SpiritDiveReelPieces.applyPart(pose, c.model().head);
        SealDraw.camera(pose);
        float age = c.age();
        int n = Math.min(sp.cubes(), SpiritDiveWind3Geometry.CROWN_PLACES), lights = 0;
        for (int i = 0; i < n; i++) {
            SpiritDiveWind3Geometry.crownCube(G, i);
            if (leftOnly && G[0] < -1e-3f) continue;
            float k = upgrade && G[0] >= 0f ? 1f : SpiritDiveReelRules.crownCube(u, i, n);
            if (k <= 0.05f) continue;
            float s = Math.min(k, 1.12f);
            pose.pushPose();
            pose.translate(G[0], G[1] - (1f - Math.min(k, 1f)) * 2.5f, G[2]);
            pose.scale(G[3] * s, G[4] * s, G[5] * s);
            SpiritDiveWind3Draw.mesh(c, CUBE, SpiritDiveWind3Rules.SOLID, Math.min(1f, 0.97f * Math.min(k, 1f)), 0f, 0);
            pose.popPose();
            if ((i & 1) == 1 && lights < sp.lights() && k >= 1f) {                                           // a white light on the top of a tall bar
                float tw = 0.5f + 0.5f * Mth.sin(age * 0.17f + i * 1.7f);
                SealDraw.glow(pose, c.buffers().getBuffer(SafeAdditive.additive(STAR)), G[0], G[1] - G[4] - 0.4f, G[2], 1.7f + 0.6f * tw, age * 0.03f + i, SealDraw.argb(Math.min(1f, 0.9f * lit), 0xFFFFFF), FB);
                lights++;
            }
        }
        SealDraw.glow(pose, c.buffers().getBuffer(SafeAdditive.additive(SOFT)), 0f, SpiritDiveWindGeometry.RING_Y - 1.6f, 0f, 9.0f, 0f, SealDraw.argb(Math.min(1f, 0.38f * ign * lit), pal.glow()), FB);
        pose.popPose();
    }

    private static void gauntletsB(AuraContext c, Spec sp, int mask, int keptMask, float u, float lit) {
        boolean slim = WaterAuraRig.slim(c.model());
        int drawn = 0;
        if ((mask & SpiritDivePieces.SHELL_ARM_L) != 0 && drawn < sp.gauntlets()) {
            gauntlet(c, c.model().leftArm, false, slim, (keptMask & SpiritDivePieces.SHELL_ARM_L) != 0 ? 1f : SpiritDiveReelRules.gauntlet(u), sp.detail(), lit);
            drawn++;
        }
        if ((mask & SpiritDivePieces.SHELL_ARM_R) != 0 && drawn < sp.gauntlets())
            gauntlet(c, c.model().rightArm, true, slim, (keptMask & SpiritDivePieces.SHELL_ARM_R) != 0 ? 1f : SpiritDiveReelRules.gauntlet(u), sp.detail(), lit);
    }

    /** One cube gauntlet: a forearm cube, the fist cube and a band round the forearm, all real cubes with a bright frame; the glow shell round the forearm at full detail. */
    private static void gauntlet(AuraContext c, net.minecraft.client.model.geom.ModelPart arm, boolean right, boolean slim, float g, boolean detail, float lit) {
        if (g <= 0.02f) return;
        PoseStack pose = c.pose();
        pose.pushPose();
        SpiritDiveReelPieces.applyPart(pose, arm);
        float cx = (right ? -1f : 1f) * (slim ? 0.5f : 1f), a = Math.min(1f, g * 1.4f) * 0.97f, s = Math.min(1.15f, g);
        float[] q = SpiritDiveWind3Geometry.GAUNTLET;
        part(c, pose, cx, q[0], q[1] * s, q[2] * s, q[3] * s, a, detail ? 0.3f * a * lit : 0f, detail);
        part(c, pose, cx, q[4], q[5] * s, q[6] * s, q[7] * s, a, 0f, false);
        if (detail) part(c, pose, cx, q[8], q[9] * s, q[10] * s, q[11] * s, a, 0f, false);
        pose.popPose();
    }

    private static void part(AuraContext c, PoseStack pose, float cx, float y, float hx, float hy, float hz, float a, float ga, boolean glow) {
        pose.pushPose();
        pose.translate(cx, y, 0f);
        pose.scale(hx, hy, hz);
        SpiritDiveWind3Draw.mesh(c, CUBE, glow ? SpiritDiveWind3Rules.SOLID_GLOW : SpiritDiveWind3Rules.SOLID, a, ga, 0xFFFFFF);
        pose.popPose();
    }

    // ------------------------------------------------------------------ B: the voxel wing and the weapons (body frame, px)
    private static void wingsB(AuraContext c, Spec sp, Pal pal, Stage st, float u, float uk, boolean half, float lit) {
        boolean upgrade = st == Stage.UPGRADE, full = SpiritDivePieces.full(st);
        float open = SpiritDiveReelRules.wingScale(u) * (full ? 1f : 0.75f), openL = upgrade ? SpiritDiveReelRules.wingScale(uk) : open;
        int layers = SpiritDiveWind3Rules.mask(sp);
        if (sp.panels() > 0) voxelWing(c, 1f, SpiritDiveWind3Geometry.PANELS, sp.panels(), SpiritDiveWind3Geometry.WING_ROOT, SpiritDiveWind3Geometry.WING_AXIS, openL, layers, pal, lit);
        if (sp.panelsSmall() > 0 && !half)
            voxelWing(c, -1f, SpiritDiveWind3Geometry.PANELS_SMALL, sp.panelsSmall(), SpiritDiveWind3Geometry.WING_SMALL_ROOT, SpiritDiveWind3Geometry.WING_SMALL_AXIS, open, layers, pal, lit);
    }

    /** A wing of stacked glowing panels along an axis (a stepped leaf: narrow at the root, widest in the middle), unfolding from the root with an overshoot. */
    private static void voxelWing(AuraContext c, float side, float[][] rows, int n, float[] root, float[] axis, float open, int layers, Pal pal, float lit) {
        if (open <= 0.02f) return;
        float age = c.age(), a = Math.min(1f, 0.95f * SpiritDiveReelRules.smooth(open * 1.8f)), sway = 0.04f * Mth.sin(age * 0.07f + (side < 0f ? 2f : 0f));
        float ax = axis[0] * side, ay = axis[1] - sway, az = axis[2];
        for (int i = 0; i < Math.min(n, rows.length); i++) {
            float[] r = rows[i];
            float d = r[0] * open, w = r[1] * Math.min(1f, 0.35f + 0.65f * open), l = r[2] * Math.min(1f, 0.35f + 0.65f * open);
            float ph = 0.55f + 0.45f * Mth.sin(age * 0.11f - i * 0.7f + (side < 0f ? 1.5f : 0f));
            piece(c, PANEL, layers, root[0] * side + ax * d, root[1] + ay * d, root[2] + az * d, ax, ay, az, side, 0f, -0.45f, w, l, 3.2f, a, 0.28f * a * lit * ph, pal.glow());
        }
    }

    /** Version B's weapons: the real sword at the right hip with a flat magic circle round its hilt, the second blade behind the shoulder; the standard tier floats a small leaf blade. */
    private static void weaponsB(AuraContext c, Spec sp, Pal pal, float k, float lit, float pulse) {
        if (k <= 0.02f) return;
        float age = c.age(), a = alpha(k), ga = 0.6f * k * lit;
        if (sp.sword() == 2) {
            float[] s = SpiritDiveWind3Geometry.LEAF_BLADE;
            float bob = SpiritDiveWindGeometry.bob(age, 0.6f) * s[7];
            piece(c, BLADE, SpiritDiveWind3Rules.SOLID_GLOW, s[0], s[1] + bob, s[2], s[3], s[4], s[5], 1f, 0f, 0f, s[6] * 0.55f * (0.6f + 0.4f * k), s[6] * k, s[6] * 0.55f * (0.6f + 0.4f * k), a, ga, pal.glow());
            return;
        }
        if (sp.sword() != 1) return;
        float[] g = SpiritDiveWind3Geometry.SWORD_B;
        sword(c, g, pal, k, a, ga, age, 0.05f, false);
        if (sp.circle()) {                                                                                  // the flat magic circle round the hilt
            float bob = SpiritDiveWindGeometry.bob(age, 0.6f) * g[7];
            VertexConsumer cv = c.buffers().getBuffer(SafeAdditive.additive(CIRCLE));
            SealDraw.glow(c.pose(), cv, g[0], g[1] + bob, g[2], SpiritDiveWind3Geometry.CIRCLE_B * (0.5f + 0.5f * k), age * 0.03f, SealDraw.argb(Math.min(1f, 0.85f * k * lit), 0xFFFFFF), FB);
            SealDraw.glow(c.pose(), c.buffers().getBuffer(SafeAdditive.additive(SOFT)), g[0], g[1] + bob, g[2], 9.0f * k, 0f, SealDraw.argb(Math.min(1f, 0.5f * k * lit), pal.glow()), FB);
        }
        if (sp.spear() > 0) {
            float[] s = SpiritDiveWind3Geometry.SPEAR_B;
            float bob = SpiritDiveWindGeometry.bob(age, 2.3f) * s[7];
            piece(c, SPEAR, SpiritDiveWind3Rules.ALL, s[0], s[1] + bob, s[2], s[3], s[4], s[5], 1f, 0f, 0f, s[6] * (0.5f + 0.5f * k), s[6] * k, s[6] * (0.5f + 0.5f * k), a, 0.55f * k * lit, pal.glow());
        }
    }

    // ------------------------------------------------------------------ C: the crown, the feathers, the plates, the halo
    private static void crownC(AuraContext c, Pal pal, float u, float uLeft, boolean upgrade, float lit) {
        float ign = Math.max(SpiritDiveReelRules.crown(u), upgrade ? SpiritDiveReelRules.crown(uLeft) : 0f);
        if (ign <= 0.01f) return;
        PoseStack pose = c.pose();
        pose.pushPose();
        SpiritDiveReelPieces.applyPart(pose, c.model().head);
        SealDraw.camera(pose);
        float g = SpiritDiveReelRules.smooth(ign * 1.5f), age = c.age(), sy = 1.25f * (0.4f + 0.6f * Math.min(1f, ign * 1.4f));
        piece(c, CROWN, SpiritDiveWind3Rules.SOLID, 0f, SpiritDiveWindGeometry.RING_Y + 0.4f, 0f, 0f, -1f, 0f, 1f, 0f, 0f, 1f, sy, 1f, Math.min(1f, 0.97f * g), 0f, 0);
        SealDraw.glow(pose, c.buffers().getBuffer(SafeAdditive.additive(SOFT)), 0f, SpiritDiveWindGeometry.RING_Y - 3.5f, 0f, 10.5f, age * 0.01f, SealDraw.argb(Math.min(1f, 0.34f * g * lit), pal.glow()), FB);
        pose.popPose();
    }

    private static void feathersC(AuraContext c, Spec sp, boolean character, Pal pal, Stage st, float u, float uk, boolean half, float lit) {
        if (sp.feathers() <= 0) return;
        boolean upgrade = st == Stage.UPGRADE, full = SpiritDivePieces.full(st);
        int layers = SpiritDiveWind3Rules.SOLID;
        float age = c.age(), sw = Mth.clamp(c.limbSwingAmount(), 0f, 1f);
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? 1f : -1f;
            if (half && side < 0f) continue;                                                              // the first dive of Wind: the left wing only
            float clock = upgrade && side > 0f ? uk : u, open = SpiritDiveReelRules.wingScale(clock) * (full ? 1f : 0.75f);
            if (open <= 0.02f) continue;
            float a = Math.min(1f, 0.97f * SpiritDiveReelRules.smooth(open * 1.8f));
            float[] r = SpiritDiveWind3Geometry.FEATHER_ROOT;
            for (int i = 0; i < sp.feathers(); i++) {
                SpiritDiveWind3Geometry.featherDir(G, i, sp.feathers(), side, 0.05f * Mth.sin(age * 0.06f + i * 0.8f + s * 2f) + 0.10f * sw);
                float f = SpiritDiveWind3Geometry.featherScale(i, sp.feathers(), character) * Math.min(1.1f, open);
                piece(c, FEATHER, layers, r[0] * side, r[1], r[2], G[0], G[1], G[2], side, 0f, 0f, f, f, f * 1.5f, a, 0f, 0);
            }
            SealDraw.glow(c.pose(), c.buffers().getBuffer(SafeAdditive.additive(SOFT)), side * 15f, -16f, 12f, (character ? 26f : 20f) * Math.min(1f, open), 0f, SealDraw.argb(Math.min(1f, 0.28f * a * lit), pal.glow()), FB);
        }
    }

    private static void platesC(AuraContext c, Spec sp, Stage st, float u, float uk, boolean half) {
        if (sp.plates() <= 0) return;
        boolean upgrade = st == Stage.UPGRADE;
        for (int s = 0; s < Math.min(2, sp.plates()); s++) {
            float side = s == 0 ? 1f : -1f;
            if (half && side < 0f) continue;
            float g = SpiritDiveReelRules.gauntlet(upgrade && side > 0f ? uk : u);
            if (g <= 0.02f) continue;
            float[] p = SpiritDiveWind3Geometry.PLATE;
            piece(c, PLATE, SpiritDiveWind3Rules.SOLID, p[0] * side, p[1], p[2], p[3] * side, -1f, 0f, 1f, 0f, 0f, p[4] * Math.min(1.1f, g), p[4] * Math.min(1.1f, g), p[4] * Math.min(1.1f, g), Math.min(1f, 0.97f * g), 0f, 0);
        }
    }

    /** The halo of small spirit arrows circling the body (the weapon of this look), and the big arrow of light out of the left hand while a page is cast. */
    private static void halo(AuraContext c, Spec sp, Pal pal, float set, float uk, boolean half, boolean casting, float lit) {
        if (set <= 0.02f) return;
        float age = c.age(), a = Math.min(1f, 0.97f * set);
        int n = sp.orbiters();
        VertexConsumer sv = c.buffers().getBuffer(SafeAdditive.additive(SOFT));
        for (int i = 0; i < n; i++) {
            if (half && (i & 1) == 1) continue;                                                              // the first dive: half of the halo
            SpiritDiveWind3Geometry.orbiter(G, i, n, age);
            float sc = SpiritDiveWind3Geometry.HALO_SCALE * (0.6f + 0.4f * set);
            piece(c, BLADE, SpiritDiveWind3Rules.SOLID, G[0], G[1], G[2], G[3], G[4], G[5], 0f, -1f, 0f, sc * 0.75f, sc, sc * 0.75f, a, 0f, 0);
            SealDraw.glow(c.pose(), sv, G[0] - G[3] * 2.5f, G[1] - G[4] * 2.5f, G[2] - G[5] * 2.5f, 3.2f * set, 0f, SealDraw.argb(Math.min(1f, 0.55f * set * lit), pal.light()), FB);
        }
        if (sp.arrow() && casting) {
            float[] b = SpiritDiveWind3Geometry.BOW_HAND;
            piece(c, ARROW, SpiritDiveWind3Rules.ALL, b[0], b[1], b[2], 0f, 0f, -1f, 0f, 1f, 0f, 1f, 1f, 1f, a, 0.6f * set * lit, pal.glow());
        }
    }

    // ------------------------------------------------------------------ the rest (block frame)
    private static void extras(AuraContext c, SpiritDiveWindRules.Spec sp, SpiritDiveReelRules.Spec reel, Pal pal, Stage st, float u, float uk, boolean full, float lit, float pulse, Lod lod, Ver ver) {
        PoseStack pose = c.pose();
        float age = c.age(), set = SpiritDiveReelRules.settle(Math.max(u, uk));
        SealDraw.camera(pose);
        pose.pushPose();                                                                                    // the ring on the ground, turning slowly
        pose.translate(0f, 1.478f, 0f);
        SealDraw.flat(pose, c.buffers().getBuffer(SafeAdditive.additive(GROUND)), 0.95f * (0.55f + 0.45f * set), age * 0.6f, SealDraw.argb(0.8f * set * pulse, pal.glow()), FB);
        pose.popPose();
        if (lod == Lod.FAR) return;
        if (full && sp.sparks() > 0) SpiritDiveWind.sparks(c, sp, pal, set, age);
        if (lod != Lod.FULL) return;
        SpiritDiveReel.entry(c, reel, Element.WIND, pal, u, st, SpiritDiveWind3Rules.cage(ver));
    }
}
