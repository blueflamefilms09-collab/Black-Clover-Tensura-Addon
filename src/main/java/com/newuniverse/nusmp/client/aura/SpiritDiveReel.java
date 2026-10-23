package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.book.SpiritDiveAnimeGeometry.Shape;
import com.newuniverse.nusmp.book.SpiritDiveAnimeRules;
import com.newuniverse.nusmp.book.SpiritDivePieces;
import com.newuniverse.nusmp.book.SpiritDivePieces.Element;
import com.newuniverse.nusmp.book.SpiritDivePieces.Stage;
import com.newuniverse.nusmp.book.SpiritDiveReelGeometry;
import com.newuniverse.nusmp.book.SpiritDiveReelRules;
import com.newuniverse.nusmp.book.SpiritDiveReelRules.Pal;
import com.newuniverse.nusmp.book.SpiritDiveReelRules.Spec;
import com.newuniverse.nusmp.book.SpiritDiveTier;
import com.newuniverse.nusmp.book.SpiritDiveTier.Grade;
import com.newuniverse.nusmp.book.SpiritDiveTier.Lod;
import com.newuniverse.nusmp.client.SafeAdditive;
import com.newuniverse.nusmp.client.prop.PropDraw;
import com.newuniverse.nusmp.client.prop.SealDraw;
import com.newuniverse.nusmp.perf.PerfProbe;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Spirit Dive, REEL look (the default look since 3.3): the V2 design (a crown of crystal blocks with a gold ring halo, a hex-membrane wing, gold rings on the limbs) executed the way the owner's two
 * showcase videos execute it. The wearer's own skin, clothes and hair stay; over them:
 * <ol>
 *   <li>the BODY: a mosaic of glowing block patches in two or three shades over the limbs (Noelle's whole body, a few patches on the others), switched on from the feet to the head, with a bright
 *       rim and a soft glow round the silhouette ({@link SpiritDiveAnimeShell}'s inflating consumer, the mosaic textures as the skin);</li>
 *   <li>the GAUNTLETS: a glowing cube on each forearm and a fist cube past each hand;</li>
 *   <li>the CROWN: a ring of glowing cubes on the head with vertical bars and white lights, and the gold ring halo; the character tier of Water adds Noelle's thin halo;</li>
 *   <li>the WINGS: the hex-membrane leaf (Wind), a comb of thin feather blades (Water), flame feathers (Fire), crystal blades (Earth), unfolding with an overshoot;</li>
 *   <li>thin white rings orbiting the body, a ring on the ground, drifting fragments and the gold bands of the V2 design on the upper arms and thighs;</li>
 *   <li>the CHARACTER tier: Yuno's huge spirit sword with the magic circle at its hilt, Fuegoleon's flame blade, Noelle's spiky ball of water blades and her crystal spire;</li>
 *   <li>the ENTRY (22 ticks, the exit is the same clock backwards): a white flash at the chest, thin white-gold ribbons spiralling up round the body, a sphere of crossing circles closing round it,
 *       a ring running out over the ground, the body assembling, the crown igniting cube by cube, the wings unfolding, the weapon drawn out of its circle.</li>
 * </ol>
 * Fuegoleon's flame arm is not drawn here: when {@link FuegoleonArmAura#drawsArm} is true the Fire dive leaves his right arm out (no mosaic, no gauntlet). The server entry point, the tier plumbing and
 * the style bits are untouched; this class only draws.
 *
 * <p>Cost: counted by {@link SpiritDiveReelRules#vertices}; level of detail by the distance ({@link SpiritDiveTier#lod}); at most {@link SpiritDiveTier#CHARACTER_PER_FRAME} character tiers a frame
 * ({@link SpiritDiveFrame}). No allocation per frame: statics for the scratch arrays, cached textures and render types.</p>
 */
public final class SpiritDiveReel {
    private SpiritDiveReel() {}

    private static final int FB = PropDraw.FULL_BRIGHT;
    private static final PerfProbe.Section SECTION = PerfProbe.section("c.spiritdive.reel");                   // timed (/multiverse perf)

    private static final ResourceLocation[][] MOSAIC = new ResourceLocation[4][SpiritDiveReelRules.MOSAIC_LEVELS];
    private static final ResourceLocation RIM = AuraRender.tex(SpiritDiveReelRules.SHARED_RIM), SOFT = AuraRender.tex(SpiritDiveReelRules.SHARED_SOFT),
            RIBBON = AuraRender.tex(SpiritDiveReelRules.RIBBON), RING = AuraRender.tex(SpiritDiveReelRules.RING_TEX), GROUND = AuraRender.tex(SpiritDiveReelRules.GROUND),
            FLASH = AuraRender.tex(SpiritDiveReelRules.FLASH), SPARK = AuraRender.tex(SpiritDiveReelRules.SPARK), GLOW = AuraRender.tex("particle/glow");
    private static final int LEGS = SpiritDivePieces.SHELL_LEG_R | SpiritDivePieces.SHELL_LEG_L, ARM_L = SpiritDivePieces.SHELL_ARM_L, ARM_R = SpiritDivePieces.SHELL_ARM_R;      // masks of the shell
    private static final float[] SPARK_POS = new float[4];                                                    // scratch: x, y, z, phase of a spark

    static {
        for (Element e : Element.values())
            for (int k = 1; k <= SpiritDiveReelRules.MOSAIC_LEVELS; k++) MOSAIC[e.ordinal()][k - 1] = AuraRender.tex(SpiritDiveReelRules.mosaicTexture(e, k));
    }

    // ------------------------------------------------------------------ the entry point
    /** Called by {@link SpiritDiveAura} for the REEL look: one dive of {@code el} at {@code st} for the tier {@code grade}. */
    static void paint(AuraContext c, Element el, Stage st, Grade grade, float pulse) {
        Lod lod = SpiritDiveTier.lod(SpiritDiveFrame.distSq(c));
        if (lod == Lod.CULLED) return;
        long t0 = PerfProbe.begin();
        try {
            paintLod(c, el, st, grade, pulse, lod);
        } finally {
            PerfProbe.end(SECTION, t0);
        }
    }

    private static void paintLod(AuraContext c, Element el, Stage st, Grade grade, float pulse, Lod lod) {
        boolean wantsCharacter = SpiritDiveReelRules.character(el, grade) && SpiritDiveLook.characterEffects() && lod != Lod.FAR;
        boolean character = wantsCharacter && SpiritDiveFrame.admitCharacter();
        Grade use = character ? SpiritDiveTier.effective(el, grade) : Grade.STANDARD;
        Spec sp = SpiritDiveReelRules.spec(el, character, lod);
        Pal pal = SpiritDiveReelRules.pal(el, use);
        boolean upgrade = st == Stage.UPGRADE, full = SpiritDivePieces.full(st), half = SpiritDivePieces.halfBody(el, st);
        float u = SpiritDiveReelRules.clock(c.t(), c.left());
        float uk = upgrade ? SpiritDiveReelRules.clock(1e4f, c.left()) : u;                                  // an upgrade keeps the half that is on
        if (Math.max(u, uk) <= 0.05f) return;
        float flare = DiveCast.casting(c.player().getId()) ? 1f : 0f, k = 1f + 0.4f * flare;
        boolean skipArmR = el == Element.FIRE && FuegoleonArmAura.drawsArm(c.player());                    // the flame arm is its own look: no second arm
        boolean self1p = firstPersonSelf(c);
        int mask = SpiritDivePieces.shellParts(el, st), keptMask = upgrade ? SpiritDivePieces.shellParts(el, Stage.HALF) & mask : 0;
        float lit = pulse * k;

        shell(c, el, sp, character, pal, SpiritDiveReelRules.assemble(u), keptMask, mask, skipArmR, k, pulse);
        gauntlets(c, el, sp, pal, u, mask, keptMask, skipArmR, lit);
        if (!self1p && SpiritDiveReelRules.crowned(el, st) && sp.crownCubes() > 0) {
            SpiritDiveReelPieces.crown(c, el, sp, character, pal, u, uk, upgrade, half, lit);
            if (sp.halo() && el == Element.WATER) SpiritDiveReelPieces.halo(c, pal, u, lit);                  // Noelle's thin halo
        }
        PoseStack pose = c.pose();
        pose.pushPose();                                                                                    // the body frame in px, the camera read in it
        pose.scale(1f / 16f, 1f / 16f, 1f / 16f);
        SpiritDiveAnimeRibbon.camera(pose);
        wings(c, el, sp, character, pal, st, u, uk, half, lit);
        if (full && character && sp.weapon()) weapon(c, el, pal, SpiritDiveReelRules.weapon(u), lit, pulse);
        if (full && character && sp.spire() && el == Element.WATER) SpiritDiveReelPieces.spire(c, pal, SpiritDiveReelRules.weapon(u), lit);
        if (u < SpiritDiveReelRules.ENTRY_TICKS - 0.5f && sp.ribbons() > 0 && !upgrade) ribbons(c, sp, el, pal, u);
        pose.popPose();
        extras(c, el, sp, character, pal, st, u, uk, half, full, lit, pulse, lod);
    }

    // ------------------------------------------------------------------ the body
    /**
     * The body: the mosaic of block patches (the player's own parts drawn again, inflated by a fraction of a pixel, textured with the patchwork), switched on from the feet to the head; the bright
     * rim hull and the soft hull round it. Noelle's body is a solid crystal armour, the others wear lighter patches over their clothes.
     */
    private static void shell(AuraContext c, Element el, Spec sp, boolean character, Pal pal, float asm, int keptMask, int mask, boolean skipArmR, float k, float pulse) {
        boolean slim = WaterAuraRig.slim(c.model());
        int legs = LEGS, fresh = mask & ~keptMask;
        float ma = Math.min(1f, SpiritDiveReelRules.mosaicAlpha(el, character) * k);
        if (sp.shellPasses() >= 2) {
            int i = el.ordinal();
            if ((fresh & legs) != 0)
                SpiritDiveAnimeShell.pass(c, SafeAdditive.emissive(MOSAIC[i][SpiritDiveReelRules.mosaicLevel(asm, 0f, 0.6f) - 1]), false, 0.4f, 0xFFFFFF, ma, asm, fresh & legs, false, skipArmR, 0, slim);
            if ((fresh & ~legs) != 0)
                SpiritDiveAnimeShell.pass(c, SafeAdditive.emissive(MOSAIC[i][SpiritDiveReelRules.mosaicLevel(asm, 0.25f, 0.7f) - 1]), false, 0.4f, 0xFFFFFF, ma, asm, fresh & ~legs, false, skipArmR, 0, slim);
            if (keptMask != 0)
                SpiritDiveAnimeShell.pass(c, SafeAdditive.emissive(MOSAIC[i][SpiritDiveReelRules.MOSAIC_LEVELS - 1]), false, 0.4f, 0xFFFFFF, ma, 1f, keptMask, false, skipArmR, keptMask, slim);
        }
        float rimK = (el == Element.WATER ? 0.9f : character ? 0.8f : 0.6f) * k;
        SpiritDiveAnimeShell.pass(c, SafeAdditive.additive(RIM), true, SpiritDiveAnimeRules.INFLATE[1], pal.rim(), rimK, asm, mask, false, skipArmR, keptMask, slim);
        if (sp.shellPasses() >= 3)
            SpiritDiveAnimeShell.pass(c, SafeAdditive.additive(SOFT), true, SpiritDiveAnimeRules.INFLATE[2], pal.glow(), 0.38f * pulse * k, asm, mask, false, skipArmR, keptMask, slim);
    }

    // ------------------------------------------------------------------ gauntlets
    private static void gauntlets(AuraContext c, Element el, Spec sp, Pal pal, float u, int mask, int keptMask, boolean skipArmR, float lit) {
        if (sp.gauntlet() <= 0) return;
        boolean slim = WaterAuraRig.slim(c.model());
        float g = SpiritDiveReelRules.gauntlet(u);
        if ((mask & ARM_L) != 0)
            SpiritDiveReelPieces.gauntlet(c, el, c.model().leftArm, false, slim, (keptMask & ARM_L) != 0 ? 1f : g, sp.gauntlet(), pal.glow(), lit);
        if ((mask & ARM_R) != 0 && !skipArmR)
            SpiritDiveReelPieces.gauntlet(c, el, c.model().rightArm, true, slim, (keptMask & ARM_R) != 0 ? 1f : g, sp.gauntlet(), pal.glow(), lit);
    }

    // ------------------------------------------------------------------ wings
    private static void wings(AuraContext c, Element el, Spec sp, boolean character, Pal pal, Stage st, float u, float uk, boolean half, float lit) {
        if (sp.leaves() <= 0 && sp.blades() <= 0) return;
        boolean full = SpiritDivePieces.full(st), upgrade = st == Stage.UPGRADE;
        float sw = Mth.clamp(c.limbSwingAmount(), 0f, 1f);
        int leaves = sp.leaves() == 0 ? 0 : full ? sp.leaves() : 1;
        int blades = sp.blades() == 0 ? 0 : full ? sp.blades() : SpiritDiveReelRules.halfBlades(sp.blades());
        for (int s = 0; s < 2; s++) {
            float side = s == 0 ? 1f : -1f;
            if (half && side < 0f) continue;                                                              // the first dive of Wind: one wing, on the left
            float clock = upgrade && side > 0f ? uk : u;
            float open = SpiritDiveReelRules.wingScale(clock) * (full ? 1f : 0.75f);
            SpiritDiveReelPieces.wings(c, el, sp, character, pal, side, open, leaves, blades, sp.leafSegs(), sp.bladeSegs(), lit, sw);
        }
    }

    // ------------------------------------------------------------------ weapons (body frame, px)
    private static void weapon(AuraContext c, Element el, Pal pal, float k, float lit, float pulse) {
        switch (el) {
            case WIND -> SpiritDiveReelPieces.sword(c, el, SpiritDiveReelGeometry.SWORD, pal, k, lit, pulse);
            case FIRE -> SpiritDiveReelPieces.sword(c, el, SpiritDiveReelGeometry.FLAME_BLADE, pal, k, lit, pulse);
            case WATER -> SpiritDiveReelPieces.ball(c, pal, k, lit);
            case EARTH -> { }
        }
    }

    // ------------------------------------------------------------------ the entry: ribbons
    /**
     * The spiral ribbons of the entry: one canonical helix spine, turned about the body for each ribbon (the geometry is shared), thin white-gold strips that always face the viewer and rise from
     * the feet over the head. {@code exiting} runs them downward (the clock runs backwards, the ribbons come back down).
     */
    private static void ribbons(AuraContext c, Spec sp, Element el, Pal pal, float u) {
        float a = SpiritDiveReelRules.ribbonAlpha(u);
        if (a <= 0.01f) return;
        PoseStack pose = c.pose();
        float travel = SpiritDiveReelRules.ribbonTravel(u), head = -4f + (SpiritDiveReelGeometry.BODY_TOP + 14f) * travel, tail = 34f + 10f * travel;
        float spin = u * 0.55f;
        int n = sp.ribbons(), segs = Math.min(sp.ribbonSegs(), SpiritDiveReelGeometry.MAX_SEGS);
        for (int pass = 0; pass < 2; pass++) {                                                              // the thin bright core of each ribbon, then a wider soft glow round it
            VertexConsumer vc = c.buffers().getBuffer(SafeAdditive.additive(RIBBON));
            for (int i = 0; i < n; i++) {
                SpiritDiveReelGeometry.helix(SpiritDiveAnimeRibbon.SPINE, segs, head + SpiritDiveReelGeometry.ribbonLift(i), tail, spin + SpiritDiveReelGeometry.ribbonAngle(i, n), SpiritDiveReelGeometry.ribbonRadius(i));
                int col = pass == 1 ? pal.glow() : i % 3 == 2 ? pal.gold() : 0xFFFFFF;
                float w = (0.5f + 0.14f * (i % 2)) * (pass == 0 ? 1f : 3.2f);
                SpiritDiveAnimeRibbon.strip(pose, vc, segs, w, Shape.RIBBON, 0f, 1f, 0f, 0f, 1f, SealDraw.argb(Math.min(1f, (pass == 0 ? 0.95f : 0.28f) * a), col), 0f, FB);
            }
        }
    }

    // ------------------------------------------------------------------ the rest (block frame)
    private static void extras(AuraContext c, Element el, Spec sp, boolean character, Pal pal, Stage st, float u, float uk, boolean half, boolean full, float lit, float pulse, Lod lod) {
        PoseStack pose = c.pose();
        float age = c.age(), set = SpiritDiveReelRules.settle(Math.max(u, uk));
        SealDraw.camera(pose);
        if (sp.ground()) {                                                                                  // the ring on the ground, turning slowly
            pose.pushPose();
            pose.translate(0f, 1.478f, 0f);
            SealDraw.flat(pose, c.buffers().getBuffer(SafeAdditive.additive(GROUND)), 0.95f * (0.55f + 0.45f * set), age * 0.6f, SealDraw.argb(0.8f * set * pulse, pal.glow()), FB);
            pose.popPose();
        }
        if (lod == Lod.FAR) return;
        if (full && sp.bands() > 0) bands(c, set);
        if (full && sp.orbit() > 0) orbit(c, sp, pal, set, age);
        if (full && sp.sparks() > 0) sparks(c, sp, pal, set, age);
        if (lod != Lod.FULL) return;
        entry(c, sp, el, pal, u, st);
    }

    private static void bands(AuraContext c, float set) {
        boolean slim = WaterAuraRig.slim(c.model());
        SpiritDiveReelPieces.goldBand(c, c.model().leftArm, true, false, slim, set);
        SpiritDiveReelPieces.goldBand(c, c.model().rightArm, true, true, slim, set);
        SpiritDiveReelPieces.goldBand(c, c.model().leftLeg, false, false, slim, set);
        SpiritDiveReelPieces.goldBand(c, c.model().rightLeg, false, true, slim, set);
    }

    /** Thin white rings orbiting the body at hip and shoulder height, tilted and turning (the rings of the video). */
    private static void orbit(AuraContext c, Spec sp, Pal pal, float set, float age) {
        PoseStack pose = c.pose();
        VertexConsumer vc = c.buffers().getBuffer(SafeAdditive.additive(RING));
        for (int i = 0; i < Math.min(sp.orbit(), SpiritDiveReelGeometry.ORBIT.length); i++) {
            float[] o = SpiritDiveReelGeometry.ORBIT[i];
            float h = o[0] + 0.03f * Mth.sin(age * 0.07f + o[4]);
            float spin = age * o[3] + o[4];
            float cs = Mth.cos(spin), sn = Mth.sin(spin), ct = Mth.cos(o[2]), st = Mth.sin(o[2]), r = o[1];
            SealDraw.plane(pose, vc, 0f, 1.5f - h, 0f, cs * r, 0f, -sn * r, sn * ct * r, st * r, cs * ct * r, false, SealDraw.argb(0.85f * set, 0xFFFFFF), FB);
            SealDraw.plane(pose, vc, 0f, 1.5f - h, 0f, cs * r, 0f, -sn * r, sn * ct * r, st * r, cs * ct * r, true, SealDraw.argb(0.85f * set, 0xFFFFFF), FB);
        }
    }

    /** Drifting fragments: small bright diamonds rising round the body (the sparks of the videos). */
    private static void sparks(AuraContext c, Spec sp, Pal pal, float set, float age) {
        PoseStack pose = c.pose();
        VertexConsumer vc = c.buffers().getBuffer(SafeAdditive.additive(SPARK));
        for (int i = 0; i < sp.sparks(); i++) {
            SpiritDiveReelGeometry.spark(SPARK_POS, i, age);
            float ph = SPARK_POS[3];
            SealDraw.glow(pose, vc, SPARK_POS[0], 1.5f - SPARK_POS[1], SPARK_POS[2], 0.045f + 0.03f * (1f - ph), age * 0.05f + i, SealDraw.argb(Mth.sin(ph * Mth.PI) * 0.95f * set, i % 3 == 0 ? pal.glow() : 0xFFFFFF), FB);
        }
    }

    /** The sprites that only exist during the entry: the flash at the chest, the ring running out over the ground, the cage of crossing circles. */
    private static void entry(AuraContext c, Spec sp, Element el, Pal pal, float u, Stage st) {
        if (u >= SpiritDiveReelRules.ENTRY_TICKS - 0.5f || st == Stage.UPGRADE) return;
        PoseStack pose = c.pose();
        float age = c.age();
        float fl = Math.max(SpiritDiveReelRules.flash(u), SpiritDiveReelRules.reveal(u));
        if (fl > 0.01f) {                                                                                   // the flash: a star and a soft white bloom at the chest, growing as it fades; a second burst when the final form appears
            float grow = SpiritDiveReelRules.reveal(u) > SpiritDiveReelRules.flash(u) ? 0.5f + 0.5f * SpiritDiveReelRules.reveal(u) : SpiritDiveReelRules.smooth(u / 4.5f);
            SealDraw.glow(pose, c.buffers().getBuffer(SafeAdditive.additive(FLASH)), 0f, 1.5f - 1.05f, 0f, 0.8f + 1.8f * grow, age * 0.04f, SealDraw.argb(Math.min(1f, fl * 1.2f), 0xFFFFFF), FB);
            SealDraw.glow(pose, c.buffers().getBuffer(SafeAdditive.additive(GLOW)), 0f, 1.5f - 1.05f, 0f, 1.1f + 2.0f * grow, 0f, SealDraw.argb(Math.min(1f, fl * 1.0f), 0xE8FFFA), FB);
            SealDraw.glow(pose, c.buffers().getBuffer(SafeAdditive.additive(GLOW)), 0f, 1.5f - 1.05f, 0f, 0.6f + 1.0f * grow, 0f, SealDraw.argb(Math.min(1f, fl * 1.0f), 0xFFFFFF), FB);
        }
        float gp = SpiritDiveReelRules.groundPulse(u);
        if (gp > 0f) {                                                                                      // the ring pulse: a ring running out from the feet
            pose.pushPose();
            pose.translate(0f, 1.47f, 0f);
            SealDraw.flat(pose, c.buffers().getBuffer(SafeAdditive.additive(RING)), 0.45f + 3.0f * gp, u * 2.5f, SealDraw.argb((1f - gp) * 0.95f, 0xFFFFFF), FB);
            pose.popPose();
        }
        float cg = SpiritDiveReelRules.cage(u);
        if (cg > 0.01f && sp.cage() > 0) cage(c, sp, cg, SpiritDiveReelRules.cageScale(u), age);
    }

    /** The sphere of crossing circles round the body: great circles at different tilts, each turning slowly, closing round the wearer as the entry runs. */
    private static void cage(AuraContext c, Spec sp, float a, float scale, float age) {
        PoseStack pose = c.pose();
        VertexConsumer vc = c.buffers().getBuffer(SafeAdditive.additive(RING));
        for (int i = 0; i < Math.min(sp.cage(), SpiritDiveReelGeometry.CAGE.length); i++) {
            float[] g = SpiritDiveReelGeometry.CAGE[i];
            float nx = g[1], ny = g[2], nz = g[3], nl = Mth.sqrt(nx * nx + ny * ny + nz * nz);
            nx /= nl; ny /= nl; nz /= nl;
            float ux = 0f, uy = nz, uz = -ny;                                                                // n x (1,0,0): a vector in the circle's plane
            float ul = Mth.sqrt(uy * uy + uz * uz);
            if (ul < 1e-3f) { uy = 0f; uz = 1f; ul = 1f; }
            uy /= ul; uz /= ul;
            float vx = ny * uz - nz * uy, vy = nz * ux - nx * uz, vz = nx * uy - ny * ux;
            float ph = g[4] + age * g[5] * 0.5f, cs = Mth.cos(ph), sn = Mth.sin(ph), r = 1.15f * scale;
            float ax = (ux * cs + vx * sn) * r, ay = (uy * cs + vy * sn) * r, az = (uz * cs + vz * sn) * r;
            float bx = (-ux * sn + vx * cs) * r, by = (-uy * sn + vy * cs) * r, bz = (-uz * sn + vz * cs) * r;
            int col = SealDraw.argb(0.8f * a, i % 2 == 0 ? 0xFFFFFF : 0xFFF0C0);
            SealDraw.plane(pose, vc, 0f, 1.5f - g[0], 0f, ax, ay, az, bx, by, bz, false, col, FB);
            SealDraw.plane(pose, vc, 0f, 1.5f - g[0], 0f, ax, ay, az, bx, by, bz, true, col, FB);
        }
    }

    private static boolean firstPersonSelf(AuraContext c) {
        try {
            Minecraft mc = Minecraft.getInstance();
            return c.player() == mc.player && mc.options.getCameraType().isFirstPerson();
        } catch (RuntimeException ex) {
            return false;
        }
    }
}
