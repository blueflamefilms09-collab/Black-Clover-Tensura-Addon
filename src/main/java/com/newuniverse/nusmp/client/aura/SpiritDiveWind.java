package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.book.SpiritDiveAnimeGeometry.Shape;
import com.newuniverse.nusmp.book.SpiritDivePieces;
import com.newuniverse.nusmp.book.SpiritDivePieces.Element;
import com.newuniverse.nusmp.book.SpiritDivePieces.Stage;
import com.newuniverse.nusmp.book.SpiritDiveReelGeometry;
import com.newuniverse.nusmp.book.SpiritDiveReelRules;
import com.newuniverse.nusmp.book.SpiritDiveReelRules.Pal;
import com.newuniverse.nusmp.book.SpiritDiveTier;
import com.newuniverse.nusmp.book.SpiritDiveTier.Grade;
import com.newuniverse.nusmp.book.SpiritDiveTier.Lod;
import com.newuniverse.nusmp.book.SpiritDiveWind4Rules;
import com.newuniverse.nusmp.book.SpiritDiveWindGeometry;
import com.newuniverse.nusmp.book.SpiritDiveWindRules;
import com.newuniverse.nusmp.book.SpiritDiveWindRules.Spec;
import com.newuniverse.nusmp.client.SafeAdditive;
import com.newuniverse.nusmp.client.prop.PropDraw;
import com.newuniverse.nusmp.client.prop.SealDraw;
import com.newuniverse.nusmp.perf.PerfProbe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Spirit Dive, the WIND of the PACK look: the Wind dive of the REEL look drawn the way the owner's reference pack shows Yuno's Spirit Dive ({@code Spirit_Dive.png}, {@code Spirit_of_Zephyr.png}, the bow of
 * Boreas, the AMV). Called by {@link SpiritDiveReel#paint} for the element Wind when the client option {@code spiritDive.windLook} is PACK (the default); {@code REEL} gives the Wind of 3.3 back. The wearer's
 * own skin and clothes stay; over them:
 * <ol>
 *   <li>the BODY LIGHT: the pale green-white wash, flowing streaks and bright rim of the body shell ({@link SpiritDiveAnimeShell}, in these colours), switched on from the feet to the head;</li>
 *   <li>the CROWN: a thin ring floating over the head and tall vertical bars with arrow-tip tops standing on it (the character tier: more bars and a second ring);</li>
 *   <li>the WRIST CIRCLE: a ring of connected crosses and branches round the wrist of the casting hand (the left arm; the character tier both);</li>
 *   <li>the LEAVES: ONE big smooth leaf swept up and back from the left shoulder blade with an ornate cross at its tip, a smaller second leaf on the right (the character tier: both sides get both);</li>
 *   <li>the CHARACTER tier: the big circle of connected crosses behind the back, Zephyr's long sword with the crossed circles of the guard (at rest) or Boreas' leaf bow with an arrow of light (while a page is
 *       cast), four-point sparkles, white ribbons of wind looping round the body;</li>
 *   <li>the ENTRY (the same 22 ticks and the same grammar as the REEL look: flash, white ribbons, a ring running out, a cage of circles, the body assembling, the crown igniting, the leaves unfolding), in pale
 *       green and white.</li>
 * </ol>
 * Cost: counted by {@link SpiritDiveWindRules#vertices} and never above the REEL Wind's count; level of detail by the distance ({@link SpiritDiveTier#lod}); at most
 * {@link SpiritDiveTier#CHARACTER_PER_FRAME} character tiers a frame ({@link SpiritDiveFrame}); timed in the probe section {@code c.spiritdive.wind}. No allocation per frame: statics for the scratch
 * arrays, cached textures and render types; no packet, no server work, no state of any player.
 */
public final class SpiritDiveWind {
    private SpiritDiveWind() {}

    private static final int FB = PropDraw.FULL_BRIGHT;
    private static final PerfProbe.Section SECTION = PerfProbe.section("c.spiritdive.wind");                  // timed (/multiverse perf)

    private static final ResourceLocation LEAF = AuraRender.tex(SpiritDiveWindRules.LEAF), LEAF_GLOW = AuraRender.tex(SpiritDiveWindRules.LEAF_GLOW), CROSS = AuraRender.tex(SpiritDiveWindRules.CROSS),
            BAR = AuraRender.tex(SpiritDiveWindRules.BAR), RING = AuraRender.tex(SpiritDiveWindRules.RING), WRIST = AuraRender.tex(SpiritDiveWindRules.WRIST), CIRCLE = AuraRender.tex(SpiritDiveWindRules.CIRCLE),
            SWORD = AuraRender.tex(SpiritDiveWindRules.SWORD), GUARD = AuraRender.tex(SpiritDiveWindRules.GUARD), BOW = AuraRender.tex(SpiritDiveWindRules.BOW), ARROW = AuraRender.tex(SpiritDiveWindRules.ARROW),
            STAR = AuraRender.tex(SpiritDiveWindRules.STAR), GROUND = AuraRender.tex(SpiritDiveReelRules.GROUND), RIBBON = AuraRender.tex(SpiritDiveReelRules.RIBBON);
    private static final float[] SPARK_POS = new float[4];                                                    // scratch: x, y, z, phase of a sparkle

    // ------------------------------------------------------------------ the entry point
    /** Called by {@link SpiritDiveReel} for the PACK Wind: one dive of Wind at {@code st} for the tier {@code grade}. */
    static void paint(AuraContext c, Stage st, Grade grade, float pulse) { paint(c, st, grade, pulse, false); }

    /** The same, with {@code real3d} true for the version A of the Wind remake: the weapons are the real 3D pieces of {@link SpiritDiveWind3} instead of the flat strips. */
    static void paint(AuraContext c, Stage st, Grade grade, float pulse, boolean real3d) {
        Lod lod = SpiritDiveTier.lod(SpiritDiveFrame.distSq(c));
        if (lod == Lod.CULLED) return;
        long t0 = PerfProbe.begin();
        try {
            paintLod(c, st, grade, pulse, lod, real3d);
        } finally {
            PerfProbe.end(SECTION, t0);
        }
    }

    private static void paintLod(AuraContext c, Stage st, Grade grade, float pulse, Lod lod, boolean real3d) {
        Element el = Element.WIND;
        boolean wantsCharacter = SpiritDiveWindRules.character(grade) && SpiritDiveLook.characterEffects() && lod != Lod.FAR;
        boolean character = wantsCharacter && SpiritDiveFrame.admitCharacter();
        Spec sp = SpiritDiveWindRules.spec(character, lod);
        SpiritDiveReelRules.Spec reel = SpiritDiveReelRules.spec(el, character, lod);                         // the entry sprites and the ribbons of the entry count as in the REEL look
        Pal pal = SpiritDiveWindRules.pal(character);
        boolean upgrade = st == Stage.UPGRADE, full = SpiritDivePieces.full(st), half = SpiritDivePieces.halfBody(el, st);
        float u = SpiritDiveReelRules.clock(c.t(), c.left());
        float uk = upgrade ? SpiritDiveReelRules.clock(1e4f, c.left()) : u;                                  // an upgrade keeps the half that is on
        if (Math.max(u, uk) <= 0.05f) return;
        boolean casting = DiveCast.casting(c.player().getId());
        float k = casting ? 1.35f : 1f, lit = pulse * k;
        boolean self1p = firstPersonSelf(c);
        int mask = SpiritDivePieces.shellParts(el, st), keptMask = upgrade ? SpiritDivePieces.shellParts(el, Stage.HALF) & mask : 0;
        boolean overlayWings = real3d && SpiritDiveWindRules.character(grade) && com.newuniverse.nusmp.client.mode.ModeArmorWindV2.replacesAuraPieces(c.player().getId());   // Mode Armour v2 (Yuno): the overlay draws the wings and the crown
        float e = real3d ? SpiritDiveWind4.energy(c, casting) : 1f;                                          // 4.x final A: 1 loud (entry, exit, a cast) .. 0 the quiet steady state
        if (e <= SpiritDiveWind4Rules.QUIET) sp = SpiritDiveWind4Rules.calmSpec(sp);                         // the loud effects have faded out: the quiet set (no ribbons, no circle, a quarter of the sparkles)

        SpiritDiveAnimeShell.draw(c, el, sp.shellPasses(), pal.body(), pal.rim(), pal.glow(), pulse * k, SpiritDiveReelRules.assemble(u), mask, false, false, keptMask, k);
        if (!overlayWings) {                                                                                // Mode Armour v2 (Yuno): the overlay draws the crown, not the aura
            if (!self1p && SpiritDiveReelRules.crowned(el, st) && sp.bars() > 0) {
                if (real3d && lod == Lod.FULL) SpiritDiveWind4.crown(c, character, pal, u, uk, upgrade, half, lit, e);   // the crown is a real 3D layer at full detail
                else crown(c, sp, character, pal, u, uk, upgrade, half, lit);
            }
        }
        if (sp.wrists() > 0) wrists(c, sp, pal, u, uk, mask, keptMask, lit, e);
        PoseStack pose = c.pose();
        pose.pushPose();                                                                                    // the body frame in px, the camera read in it
        pose.scale(1f / 16f, 1f / 16f, 1f / 16f);
        SpiritDiveAnimeRibbon.camera(pose);
        if (!overlayWings) leaves(c, sp, lod, character, pal, st, u, uk, half, lit);
        if (full && sp.backCircle()) backCircle(c, pal, SpiritDiveReelRules.settle(u) * e, lit);
        if (real3d) { if (full && lod == Lod.FULL) SpiritDiveWind3.weaponsA(c, pal, character, casting, SpiritDiveReelRules.weapon(u), lit, pulse, e); }
        else if (full && sp.weapon()) weapon(c, pal, SpiritDiveWindRules.weapon(casting), SpiritDiveReelRules.weapon(u), lit, pulse);
        if (full && sp.ribbons() > 0) ribbons(c, sp, SpiritDiveReelRules.settle(u) * e);
        if (u < SpiritDiveReelRules.ENTRY_TICKS - 0.5f && reel.ribbons() > 0 && !upgrade) SpiritDiveReel.ribbons(c, reel, el, pal, u);
        pose.popPose();
        extras(c, sp, reel, pal, st, u, uk, full, lit, pulse, lod, e);
    }

    // ------------------------------------------------------------------ the crown (head frame, px)
    /** A band of four outward quads (the ring), from {@code xa} to {@code xb} on the front and the back; {@code leftOnly} drops the right side (-x). */
    private static void band(PoseStack pose, VertexConsumer vc, float half, float y0, float y1, boolean leftOnly, int argb) {
        float x0 = leftOnly ? 0f : -half, x1 = half, z0 = -half, z1 = half;
        PropDraw.quad(pose, vc, x1, y1, z0, x0, y1, z0, x0, y0, z0, x1, y0, z0, 0f, 0f, 1f, 1f, argb, FB);        // -z (front)
        PropDraw.quad(pose, vc, x0, y1, z1, x1, y1, z1, x1, y0, z1, x0, y0, z1, 0f, 0f, 1f, 1f, argb, FB);        // +z (back)
        if (!leftOnly) PropDraw.quad(pose, vc, -half, y1, z0, -half, y1, z1, -half, y0, z1, -half, y0, z0, 0f, 0f, 1f, 1f, argb, FB);   // -x
        PropDraw.quad(pose, vc, half, y1, z1, half, y1, z0, half, y0, z0, half, y0, z1, 0f, 0f, 1f, 1f, argb, FB);  // +x
    }

    /**
     * The crown on the head: the thin ring of the pack, the tall bars with arrow-tip tops on it (each turns about the vertical to face the viewer), a soft glow under them. {@code leftOnly} (the first dive
     * of Wind) draws the left half of the ring and the bars with x >= 0.
     */
    static void crown(AuraContext c, Spec sp, boolean character, Pal pal, float u, float uLeft, boolean upgrade, boolean leftOnly, float lit) {
        float ign = Math.max(SpiritDiveReelRules.crown(u), upgrade ? SpiritDiveReelRules.crown(uLeft) : 0f);
        if (ign <= 0.01f) return;
        PoseStack pose = c.pose();
        pose.pushPose();
        SpiritDiveReelPieces.applyPart(pose, c.model().head);
        SealDraw.camera(pose);
        float age = c.age();
        float ga = SpiritDiveReelRules.smooth(ign * 1.5f);
        VertexConsumer rv = c.buffers().getBuffer(SafeAdditive.additive(RING));
        band(pose, rv, SpiritDiveWindGeometry.RING_HALF, SpiritDiveWindGeometry.RING_Y - SpiritDiveWindGeometry.RING_THICK * 0.5f, SpiritDiveWindGeometry.RING_Y + SpiritDiveWindGeometry.RING_THICK * 0.5f, leftOnly,
                SealDraw.argb(Math.min(1f, 0.95f * ga * lit), 0xFFFFFF));
        if (sp.rings() > 1)
            band(pose, rv, SpiritDiveWindGeometry.RING2_HALF, SpiritDiveWindGeometry.RING2_Y - SpiritDiveWindGeometry.RING2_THICK * 0.5f, SpiritDiveWindGeometry.RING2_Y + SpiritDiveWindGeometry.RING2_THICK * 0.5f, leftOnly,
                    SealDraw.argb(Math.min(1f, 0.8f * ga * lit), pal.light()));
        float rx = SealDraw.CAM[0], rz = SealDraw.CAM[2], rl = Mth.sqrt(rx * rx + rz * rz);
        if (rl < 1e-4f) { rx = 1f; rz = 0f; rl = 1f; }
        rx /= rl; rz /= rl;
        int n = Math.min(sp.bars(), SpiritDiveWindGeometry.BARS_MAX);
        VertexConsumer bv = c.buffers().getBuffer(SafeAdditive.additive(BAR));
        float foot = SpiritDiveWindGeometry.RING_Y + SpiritDiveWindGeometry.RING_THICK * 0.5f;
        for (int b = 0; b < n; b++) {
            float x = SpiritDiveWindGeometry.BARS[b][0], z = SpiritDiveWindGeometry.BARS[b][1], h = SpiritDiveWindGeometry.BARS[b][2] * (character ? 1.1f : 1f);
            if (leftOnly && x < -1e-3f) continue;
            float k = upgrade && x >= 0f ? 1f : SpiritDiveReelRules.crownCube(u, b, n);
            if (k <= 0.05f) continue;
            float hw = h * SpiritDiveWindGeometry.BAR_ASPECT * 0.5f, top = foot - h * Math.min(k, 1.12f) * (1f + 0.04f * Mth.sin(age * 0.19f + SpiritDiveWindGeometry.BARS[b][3]));
            PropDraw.quad(pose, bv, x - rx * hw, foot, z - rz * hw, x + rx * hw, foot, z + rz * hw, x + rx * hw, top, z + rz * hw, x - rx * hw, top, z - rz * hw, 0f, 0f, 1f, 1f,
                    SealDraw.argb(Math.min(1f, 0.95f * Math.min(k, 1f) * lit), 0xFFFFFF), FB);
        }
        pose.popPose();
    }

    // ------------------------------------------------------------------ the wrist circles (arm frame, px)
    static void wrists(AuraContext c, Spec sp, Pal pal, float u, float uk, int mask, int keptMask, float lit) { wrists(c, sp, pal, u, uk, mask, keptMask, lit, 1f); }

    /** The same with the energy {@code e} of the final A: below 1 the wrist ring breathes at a low alpha and the facing sprite fades out (the quiet state). */
    static void wrists(AuraContext c, Spec sp, Pal pal, float u, float uk, int mask, int keptMask, float lit, float e) {
        boolean slim = WaterAuraRig.slim(c.model());
        int drawn = 0;
        if ((mask & SpiritDivePieces.SHELL_ARM_L) != 0 && drawn < sp.wrists()) {                                // the left hand is the casting hand of the pack
            wrist(c, c.model().leftArm, false, slim, (keptMask & SpiritDivePieces.SHELL_ARM_L) != 0 ? 1f : SpiritDiveReelRules.gauntlet(u), pal, lit, e);
            drawn++;
        }
        if ((mask & SpiritDivePieces.SHELL_ARM_R) != 0 && drawn < sp.wrists()) wrist(c, c.model().rightArm, true, slim, (keptMask & SpiritDivePieces.SHELL_ARM_R) != 0 ? 1f : SpiritDiveReelRules.gauntlet(u), pal, lit, e);
    }

    /** One wrist circle: the ring of crosses in the plane perpendicular to the arm (seen from both sides) and a copy that always faces the viewer, so the ring reads from every side. */
    private static void wrist(AuraContext c, ModelPart arm, boolean right, boolean slim, float g, Pal pal, float lit, float e) {
        float a = Math.min(1f, g * 1.2f);
        if (e < 1f) a *= SpiritDiveWind4Rules.WRIST_CALM + SpiritDiveWind4Rules.WRIST_PULSE * Mth.sin(c.age() * 0.09f + (right ? 1.7f : 0f)) + (1f - SpiritDiveWind4Rules.WRIST_CALM) * e;   // a low breath when quiet
        if (a <= 0.02f) return;
        PoseStack pose = c.pose();
        pose.pushPose();
        SpiritDiveReelPieces.applyPart(pose, arm);
        SealDraw.camera(pose);
        float cx = (right ? -1f : 1f) * (slim ? 0.5f : 1f), spin = c.age() * (right ? -0.04f : 0.045f), s = Math.min(1.1f, g);
        VertexConsumer vc = c.buffers().getBuffer(SafeAdditive.additive(WRIST));
        SpiritDiveReelPieces.ringFlat(pose, vc, cx, SpiritDiveWindGeometry.WRIST_Y, 0f, SpiritDiveWindGeometry.WRIST_PLANE * s, spin, 0f, SealDraw.argb(Math.min(1f, 0.9f * a * lit), 0xFFFFFF));
        if (e > SpiritDiveWind4Rules.QUIET)
            SealDraw.glow(pose, vc, cx, SpiritDiveWindGeometry.WRIST_Y, 0f, SpiritDiveWindGeometry.WRIST_SPRITE * s, spin * 0.5f, SealDraw.argb(Math.min(1f, 0.7f * a * lit * e), pal.light()), FB);
        pose.popPose();
    }

    // ------------------------------------------------------------------ the leaves (body frame, px)
    /** The big leaf and the small leaf of each side, unfolding with an overshoot; the cross on the big leaf's tip. */
    static void leaves(AuraContext c, Spec sp, Lod lod, boolean character, Pal pal, Stage st, float u, float uk, boolean half, float lit) {
        if (sp.leafSegs() <= 0) return;
        boolean full = SpiritDivePieces.full(st), upgrade = st == Stage.UPGRADE;
        float sw = Mth.clamp(c.limbSwingAmount(), 0f, 1f);
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? 1f : -1f;
            if (half && side < 0f) continue;                                                              // the first dive of Wind: the left side only
            int mask = SpiritDiveWindRules.leafMask(character, lod, side > 0f);
            float clock = upgrade && side > 0f ? uk : u;
            float open = SpiritDiveReelRules.wingScale(clock) * (full ? 1f : 0.75f);
            for (int row = 0; row < SpiritDiveWindGeometry.leafRows(); row++)
                if ((mask >> row & 1) != 0) leaf(c, sp, row, side, character, pal, open, lit, sw);
        }
    }

    private static void leaf(AuraContext c, Spec sp, int row, float side, boolean character, Pal pal, float open, float lit, float swing) {
        if (open <= 0.02f) return;
        PoseStack pose = c.pose();
        float age = c.age(), a = SpiritDiveReelRules.smooth(open * 1.8f), k = character ? SpiritDiveWindGeometry.LEAF_CHARACTER : 1f;
        float[] r = SpiritDiveWindGeometry.leaf(row);
        float t = age + row * 5.3f + (side < 0f ? 7f : 0f);
        int segs = sp.leafSegs();
        for (int pass = 0; pass < 2; pass++) {
            VertexConsumer vc = c.buffers().getBuffer(pass == 0 ? SafeAdditive.emissive(LEAF) : SafeAdditive.additive(LEAF_GLOW));
            int argb = pass == 0 ? SealDraw.argb(Math.min(1f, 0.88f * a), 0xFFFFFF) : SealDraw.argb(Math.min(1f, 0.7f * a * lit), pal.glow());
            SpiritDiveReelGeometry.leafSpine(SpiritDiveAnimeRibbon.SPINE, segs, r, side, k, open, t, swing);
            SpiritDiveAnimeRibbon.strip(pose, vc, segs, r[10] * k * (0.35f + 0.65f * Math.min(1f, open)), Shape.LEAF, r[12] + age * 0.03f, 0.42f, 0f, 0.25f, 1f, argb, 0.06f, FB);
        }
        if (row == SpiritDiveWindGeometry.BIG && sp.crosses() > 0 && (side > 0f || character)) {             // the ornate cross near the tip of the big leaf
            int i = Math.round(SpiritDiveWindGeometry.CROSS_AT * segs) * 3;
            SpiritDiveReelGeometry.leafSpine(SpiritDiveAnimeRibbon.SPINE, segs, r, side, k, open, t, swing);
            float sz = (character ? SpiritDiveWindGeometry.CROSS_SIZE_CHARACTER : SpiritDiveWindGeometry.CROSS_SIZE) * Math.min(1f, open);
            SealDraw.glow(pose, c.buffers().getBuffer(SafeAdditive.additive(CROSS)), SpiritDiveAnimeRibbon.SPINE[i] + side * 1.5f, SpiritDiveAnimeRibbon.SPINE[i + 1] - SpiritDiveWindGeometry.CROSS_LIFT,
                    SpiritDiveAnimeRibbon.SPINE[i + 2], sz, side * 0.45f, SealDraw.argb(Math.min(1f, 0.95f * a * lit), 0xFFFFFF), FB);
        }
    }

    // ------------------------------------------------------------------ the circle behind the back, the weapons, the ribbons (body frame, px)
    /** The big circle of connected crosses behind the back of the character tier: a plane facing back and front, turning slowly about its axis. */
    static void backCircle(AuraContext c, Pal pal, float set, float lit) {
        if (set <= 0.02f) return;
        PoseStack pose = c.pose();
        float[] b = SpiritDiveWindGeometry.BACK;
        float spin = c.age() * b[4], cs = Mth.cos(spin), sn = Mth.sin(spin), r = b[3] * (0.6f + 0.4f * set);
        VertexConsumer vc = c.buffers().getBuffer(SafeAdditive.additive(CIRCLE));
        int argb = SealDraw.argb(Math.min(1f, 0.85f * set * lit), 0xFFFFFF);
        SealDraw.plane(pose, vc, b[0], b[1], b[2], cs * r, sn * r, 0f, -sn * r, cs * r, 0f, false, argb, FB);
        SealDraw.plane(pose, vc, b[0], b[1], b[2], cs * r, sn * r, 0f, -sn * r, cs * r, 0f, true, argb, FB);
    }

    /**
     * A flat strip along an axis that turns its face to the viewer about that axis (a sword, a bow, an arrow), from {@code down} px behind the point {@code (px, py, pz)} to {@code up} px beyond it, drawn
     * twice: the body (alpha blended) and a wider additive glow. The axis need not be a unit vector.
     */
    private static void flat(AuraContext c, ResourceLocation tex, float px, float py, float pz, float ax, float ay, float az, float up, float down, float hw, float glowK, int body, int glow) {
        PoseStack pose = c.pose();
        float al = Mth.sqrt(ax * ax + ay * ay + az * az);
        ax /= al; ay /= al; az /= al;
        float lx = SealDraw.CAM[6], ly = SealDraw.CAM[7], lz = SealDraw.CAM[8];
        float wx = ay * lz - az * ly, wy = az * lx - ax * lz, wz = ax * ly - ay * lx, wl = Mth.sqrt(wx * wx + wy * wy + wz * wz);
        if (wl < 1e-4f) { wx = 1f; wy = 0f; wz = 0f; wl = 1f; }
        wx = wx / wl * hw; wy = wy / wl * hw; wz = wz / wl * hw;
        float tx = px + ax * up, ty = py + ay * up, tz = pz + az * up, bx = px - ax * down, by = py - ay * down, bz = pz - az * down;
        for (int pass = 0; pass < 2; pass++) {
            float g = pass == 0 ? 1f : glowK;
            VertexConsumer vc = c.buffers().getBuffer(pass == 0 ? SafeAdditive.emissive(tex) : SafeAdditive.additive(tex));
            PropDraw.quad(pose, vc, bx - wx * g, by - wy * g, bz - wz * g, bx + wx * g, by + wy * g, bz + wz * g, tx + wx * g, ty + wy * g, tz + wz * g, tx - wx * g, ty - wy * g, tz - wz * g,
                    0f, 0f, 1f, 1f, pass == 0 ? body : glow, FB);
        }
    }

    /** Yuno's spirit weapon, drawn out of its circle by {@code k} (0..1): Zephyr's sword, or Boreas' bow with its arrow of light. */
    private static void weapon(AuraContext c, Pal pal, SpiritDiveWindRules.Weapon w, float k, float lit, float pulse) {
        if (k <= 0.02f) return;
        float age = c.age();
        if (w == SpiritDiveWindRules.Weapon.SWORD) {
            float[] s = SpiritDiveWindGeometry.SWORD;
            float len = s[6] / 0.70f, px = s[0], py = s[1] + SpiritDiveWindGeometry.bob(age, 0.6f), pz = s[2];
            float hw = len / 8f * (0.4f + 0.6f * k);
            flat(c, SWORD, px, py, pz, s[3], -s[4], s[5], 0.70f * len * k, 0.30f * len * k, hw, 1.5f,
                    SealDraw.argb(0.97f * Math.min(1f, k * 1.5f), 0xFFFFFF), SealDraw.argb(0.42f * k * lit * pulse, pal.glow()));
            float r = s[7] * (0.5f + 0.5f * k);                                                             // the crossed circles of the guard turn slowly at the hilt
            SealDraw.glow(c.pose(), c.buffers().getBuffer(SafeAdditive.additive(GUARD)), px, py, pz, r, age * 0.035f, SealDraw.argb(Math.min(1f, 0.95f * k * lit), 0xFFFFFF), FB);
        } else {
            float[] b = SpiritDiveWindGeometry.BOW, a = SpiritDiveWindGeometry.ARROW;
            float py = b[1] + SpiritDiveWindGeometry.bob(age, 1.9f), h = b[6] * (0.5f + 0.5f * k);
            flat(c, BOW, b[0], py, b[2], b[3], -b[4], b[5], h * 0.5f, h * 0.5f, h * 0.25f, 1.35f, SealDraw.argb(0.97f * Math.min(1f, k * 1.5f), 0xFFFFFF), SealDraw.argb(0.4f * k * lit * pulse, pal.glow()));
            float len = a[1] * k;                                                                           // the arrow of light flies out of the bow's centre
            flat(c, ARROW, b[0], py, b[2] + a[0], 0f, 0f, -1f, len, 0f, a[2] * (0.5f + 0.5f * k), 1.6f, SealDraw.argb(0.97f * Math.min(1f, k * 1.5f), 0xFFFFFF), SealDraw.argb(0.55f * k * lit * pulse, pal.glow()));
        }
    }

    /** White ribbons of wind looping slowly round the body (the loops of Spirit of Zephyr): thin strips along a helix, one pass. */
    static void ribbons(AuraContext c, Spec sp, float set) {
        if (set <= 0.02f) return;
        PoseStack pose = c.pose();
        VertexConsumer vc = c.buffers().getBuffer(SafeAdditive.additive(RIBBON));
        int n = sp.ribbons(), segs = Math.min(sp.ribbonSegs(), SpiritDiveReelGeometry.MAX_SEGS);
        for (int i = 0; i < n; i++) {
            SpiritDiveWindGeometry.ribbonSpine(SpiritDiveAnimeRibbon.SPINE, segs, i, n, c.age());
            SpiritDiveAnimeRibbon.strip(pose, vc, segs, 1.7f + 0.4f * (i % 2), Shape.RIBBON, 0f, 1f, 0f, 0f, 1f, SealDraw.argb(0.32f * set, 0xFFFFFF), 0f, FB);
        }
    }

    // ------------------------------------------------------------------ the rest (block frame)
    static void extras(AuraContext c, Spec sp, SpiritDiveReelRules.Spec reel, Pal pal, Stage st, float u, float uk, boolean full, float lit, float pulse, Lod lod) { extras(c, sp, reel, pal, st, u, uk, full, lit, pulse, lod, 1f); }

    /** The same with the energy {@code e} of the final A: the ground ring is dimmed and the extra sparkles fade out when quiet. */
    static void extras(AuraContext c, Spec sp, SpiritDiveReelRules.Spec reel, Pal pal, Stage st, float u, float uk, boolean full, float lit, float pulse, Lod lod, float e) {
        PoseStack pose = c.pose();
        float age = c.age(), set = SpiritDiveReelRules.settle(Math.max(u, uk));
        SealDraw.camera(pose);
        pose.pushPose();                                                                                    // the ring on the ground, turning slowly
        pose.translate(0f, 1.478f, 0f);
        SealDraw.flat(pose, c.buffers().getBuffer(SafeAdditive.additive(GROUND)), 0.95f * (0.55f + 0.45f * set), age * 0.6f, SealDraw.argb(0.8f * set * pulse * (e < 1f ? SpiritDiveWind4Rules.GROUND_CALM + (1f - SpiritDiveWind4Rules.GROUND_CALM) * e : 1f), pal.glow()), FB);
        pose.popPose();
        if (lod == Lod.FAR) return;
        if (full && sp.sparks() > 0) sparks(c, sp, pal, set, age, e <= SpiritDiveWind4Rules.QUIET ? sp.sparks() : e < 1f ? SpiritDiveWind4Rules.calmSparks(sp) : sp.sparks(), e);
        if (lod != Lod.FULL) return;
        SpiritDiveReel.entry(c, reel, Element.WIND, pal, u, st, SpiritDiveWindRules.CAGE_RGB);
    }

    /** Four-point sparkles drifting up round the body (the white stars of Spirit of Zephyr). */
    static void sparks(AuraContext c, Spec sp, Pal pal, float set, float age) { sparks(c, sp, pal, set, age, sp.sparks(), 1f); }

    /** The same keeping the first {@code keep} sparkles at full strength and fading the rest with the energy {@code e} (the quiet state of the final A). */
    static void sparks(AuraContext c, Spec sp, Pal pal, float set, float age, int keep, float e) {
        PoseStack pose = c.pose();
        VertexConsumer vc = c.buffers().getBuffer(SafeAdditive.additive(STAR));
        for (int i = 0; i < sp.sparks(); i++) {
            SpiritDiveReelGeometry.spark(SPARK_POS, i, age);
            float ph = SPARK_POS[3];
            SealDraw.glow(pose, vc, SPARK_POS[0], 1.5f - SPARK_POS[1], SPARK_POS[2], 0.07f + 0.05f * (1f - ph), age * 0.04f + i, SealDraw.argb(Mth.sin(ph * Mth.PI) * 0.95f * set * (i < keep ? 1f : e), i % 3 == 0 ? pal.glow() : 0xFFFFFF), FB);
        }
    }

    static boolean firstPersonSelf(AuraContext c) {
        try {
            Minecraft mc = Minecraft.getInstance();
            return c.player() == mc.player && mc.options.getCameraType().isFirstPerson();
        } catch (RuntimeException ex) {
            return false;
        }
    }
}
