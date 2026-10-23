package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.book.SpiritDiveReelRules;
import com.newuniverse.nusmp.book.SpiritDiveReelRules.Pal;
import com.newuniverse.nusmp.book.SpiritDiveWind3Mesh;
import com.newuniverse.nusmp.book.SpiritDiveWind3Rules;
import com.newuniverse.nusmp.book.SpiritDiveWind4Rules;
import com.newuniverse.nusmp.client.SafeAdditive;
import com.newuniverse.nusmp.client.prop.PropDraw;
import com.newuniverse.nusmp.client.prop.SealDraw;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;

/**
 * Spirit Dive WIND, final design A (4.x): the pieces the owner's last decisions added to the "Anime-true" look ({@link SpiritDiveWind#paint} with {@code real3d}):
 * <ul>
 *   <li>the CROWN as a real 3D layer ({@link #crown}): the mesh of {@link SpiritDiveWind4Rules} (a thin ring and tall arrow-tip bars, a lower second ring for the character tier, a glow shell) floating a
 *       hand's width above the head, turning slowly, following the head's pitch only half so it floats; drawn at full detail only (the mid distance keeps the cheap sprite crown), hidden in first person
 *       by the caller as before;</li>
 *   <li>the CALM steady state ({@link #energy}): how loud the dive is (1 in the entry, the exit and for 2 s after a cast; 0 in the quiet steady state), read from the clocks the painter already has plus a
 *       tiny per-diver cast clock ({@link SpiritDiveWind4Rules.CastClock}, 16 slots, no allocation).</li>
 * </ul>
 * Render thread only; static scratch objects and meshes, nothing allocated per frame; no packet, no server work.
 */
final class SpiritDiveWind4 {
    private SpiritDiveWind4() {}

    private static final int FB = PropDraw.FULL_BRIGHT;
    private static final ResourceLocation SOFT = AuraRender.tex(SpiritDiveWind3Rules.SOFT);
    /** The four crown meshes, resolved once (the lookup builds a key string, so it must not run per frame). */
    private static final SpiritDiveWind3Mesh CROWN_YUNO = SpiritDiveWind4Rules.crown(true, false), CROWN_STD = SpiritDiveWind4Rules.crown(false, false),
            CROWN_YUNO_HALF = SpiritDiveWind4Rules.crown(true, true), CROWN_STD_HALF = SpiritDiveWind4Rules.crown(false, true);
    private static final Quaternionf ROT = new Quaternionf(), HEAD = new Quaternionf();                       // scratch
    private static final SpiritDiveWind4Rules.CastClock CLOCK = new SpiritDiveWind4Rules.CastClock();

    /** How loud this diver is right now, 1 .. 0 ({@link SpiritDiveWind4Rules#energy}); 1 when the client option {@code spiritDive.calm} is off. */
    static float energy(AuraContext c, boolean casting) {
        if (!SpiritDiveLook.calm()) return 1f;
        return SpiritDiveWind4Rules.energy(c.t(), c.left(), CLOCK.since(c.player().getId(), c.age(), casting));
    }

    private static float smooth(float x) { return SpiritDiveReelRules.smooth(x); }

    /**
     * The 3D crown on the head. {@code leftOnly} (the first dive of Wind) draws the left half mesh and does not turn it; {@code e} is the energy (the glow breathes a little less in the quiet state).
     * {@code ign} is the larger of the crown ignition clocks of the entry (the crown rises and brightens as it ignites).
     */
    static void crown(AuraContext c, boolean character, Pal pal, float u, float uLeft, boolean upgrade, boolean leftOnly, float lit, float e) {
        float ign = Math.max(SpiritDiveReelRules.crown(u), upgrade ? SpiritDiveReelRules.crown(uLeft) : 0f);
        if (ign <= 0.01f) return;
        if (SpiritDiveMerge2.crownDistinct()) {                                                             // merge 2: the smaller, more distinct crown (client option spiritDive.crownDistinct)
            SpiritDiveMerge2.crown(c, com.newuniverse.nusmp.book.SpiritDivePieces.Element.WIND, character, pal.glow(), u, uLeft, upgrade, leftOnly, lit, e);
            return;
        }
        SpiritDiveWind3Mesh m = character ? (leftOnly ? CROWN_YUNO_HALF : CROWN_YUNO) : (leftOnly ? CROWN_STD_HALF : CROWN_STD);
        PoseStack pose = c.pose();
        ModelPart head = c.model().head;
        pose.pushPose();
        pose.translate(head.x / 16f, head.y / 16f, head.z / 16f);                                           // the head's pivot; the crown follows the yaw fully and the pitch only half
        pose.mulPose(HEAD.rotationZYX(head.zRot, head.yRot, head.xRot * SpiritDiveWind4Rules.PITCH_FOLLOW));
        pose.scale(1f / 16f, 1f / 16f, 1f / 16f);
        SealDraw.camera(pose);
        float age = c.age(), g = smooth(ign * 1.5f), rise = 0.4f + 0.6f * Math.min(1f, ign * 1.4f);
        float y = SpiritDiveWind4Rules.HOVER + SpiritDiveWind4Rules.BOB * (float) Math.sin(age * SpiritDiveWind4Rules.BOB_SPEED);
        float breath = 0.78f + 0.22f * e;                                                                   // the quiet state glows a little less
        SealDraw.glow(pose, c.buffers().getBuffer(SafeAdditive.additive(SOFT)), 0f, y + SpiritDiveWind4Rules.HALO_DY, 0f, SpiritDiveWind4Rules.HALO_SIZE * (0.6f + 0.4f * g), 0f,
                SealDraw.argb(Math.min(1f, 0.30f * g * lit * breath), pal.glow()), FB);
        pose.pushPose();
        SpiritDiveWind3Draw.basis(pose, 0f, y, 0f, 0f, -1f, 0f, 1f, 0f, 0f, 1f, rise, 1f);                // the mesh's +y is the head's up (y is down in this frame)
        if (!leftOnly) pose.mulPose(ROT.rotationY(age * SpiritDiveWind4Rules.SPIN));
        SpiritDiveWind3Draw.mesh(c, m, SpiritDiveWind4Rules.crownMask(character), Math.min(1f, 0.97f * g), 0.62f * g * lit * breath, pal.glow());
        pose.popPose();
        pose.popPose();
    }
}
