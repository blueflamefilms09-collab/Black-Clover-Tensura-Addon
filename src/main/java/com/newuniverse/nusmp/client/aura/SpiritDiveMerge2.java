package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.book.SpiritDiveMerge2Rules;
import com.newuniverse.nusmp.book.SpiritDiveMerge2Rules.Models;
import com.newuniverse.nusmp.book.SpiritDivePieces.Element;
import com.newuniverse.nusmp.book.SpiritDiveReelRules;
import com.newuniverse.nusmp.book.SpiritDiveWind3Mesh;
import com.newuniverse.nusmp.book.SpiritDiveWind3Rules;
import com.newuniverse.nusmp.client.SafeAdditive;
import com.newuniverse.nusmp.client.prop.PropDraw;
import com.newuniverse.nusmp.client.prop.SealDraw;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;

/**
 * Spirit Dive MERGE 2, the client side: the four client options (plain flags the painters read, mirrored from {@code nusmp-spiritdive-client.toml} by {@link DiveClientConfig}) and the drawing of the
 * new small element crowns. The numbers are {@link SpiritDiveMerge2Rules}. Render thread only; no allocation per frame (static scratch objects, meshes resolved once, shared arrays), no packet, no server work.
 * <ul>
 *   <li>{@code spiritDive.mergeModels} SLIM (default) | BOXY: which spirit meshes the merge ghost uses ({@link SpiritDiveMerge});</li>
 *   <li>{@code spiritDive.calmEntry} (default true): the look's own entry flash and ribbons are turned down while the spirit merges ({@link SpiritDiveReel#entry}, {@link SpiritDiveReel#ribbons});</li>
 *   <li>{@code spiritDive.mergeFirstPerson} (default true): the merge is also shown to the wearer in first person ({@link SpiritDiveMergeFirstPerson});</li>
 *   <li>{@code spiritDive.crownDistinct} (default true): the smaller, more distinct crowns ({@link #crown}, {@link #fit}).</li>
 * </ul>
 */
public final class SpiritDiveMerge2 {
    private SpiritDiveMerge2() {}

    private static volatile Models models = SpiritDiveMerge2Rules.DEFAULT_MODELS;
    private static volatile boolean calmEntry = SpiritDiveMerge2Rules.DEFAULT_CALM_ENTRY, firstPerson = SpiritDiveMerge2Rules.DEFAULT_FIRST_PERSON, crown = SpiritDiveMerge2Rules.DEFAULT_CROWN;

    public static Models models() { return models; }

    public static boolean slim() { return models == Models.SLIM; }

    /** The size of the ghost relative to the first merge's: the slim spirits are thinner than the boxy ones, so they are drawn a little larger ({@link SpiritDiveMerge2Rules#SLIM_SIZE_K}); 1 for the boxy ones. */
    static float sizeK(Element e, boolean character) { return models == Models.SLIM ? SpiritDiveMerge2Rules.slimSizeK(e, character) : 1f; }

    public static boolean calmEntry() { return calmEntry; }

    public static boolean firstPerson() { return firstPerson; }

    public static boolean crownDistinct() { return crown; }

    public static void set(Models m, boolean calm, boolean fp, boolean crownOn) {
        models = m == null ? SpiritDiveMerge2Rules.DEFAULT_MODELS : m;
        calmEntry = calm;
        firstPerson = fp;
        crown = crownOn;
    }

    // ------------------------------------------------------------------ the crowns
    private static final ResourceLocation SOFT = AuraRender.tex(SpiritDiveWind3Rules.SOFT);
    private static final int FB = PropDraw.FULL_BRIGHT;
    private static final SpiritDiveWind3Mesh[][][] MESH = new SpiritDiveWind3Mesh[4][2][2];                    // element, character, half: resolved once (the lookup builds a key string)
    private static final Quaternionf HEAD = new Quaternionf();                                                  // scratch

    private static SpiritDiveWind3Mesh mesh(Element e, boolean character, boolean half) {
        boolean ch = character && e != Element.EARTH, hf = half && e == Element.WIND;
        SpiritDiveWind3Mesh m = MESH[e.ordinal()][ch ? 1 : 0][hf ? 1 : 0];
        if (m == null) MESH[e.ordinal()][ch ? 1 : 0][hf ? 1 : 0] = m = SpiritDiveMerge2Rules.crown(e, ch, hf);
        return m;
    }

    /**
     * The scale of the OLD crowns that stay (the ANIME sprite, the PACK sprite crown, the B cubes, the C mesh): about the head's top, so they sit closer to the head and are {@link SpiritDiveMerge2Rules#CROWN_K}
     * as wide and tall. The caller has the pose in the head frame (px); a no-op when the option is off.
     */
    static void fit(PoseStack pose) {
        if (!crown) return;
        float t = SpiritDiveMerge2Rules.HEAD_TOP, k = SpiritDiveMerge2Rules.CROWN_K;
        pose.translate(0f, t, 0f);
        pose.scale(k, k, k);
        pose.translate(0f, -t, 0f);
    }

    /** The scale factor the sprite crown of the ANIME look takes (1 when the option is off). */
    static float crownK() { return crown ? SpiritDiveMerge2Rules.CROWN_K : 1f; }

    /**
     * The new small crown of an element on the head (head frame, px): a mesh of {@link SpiritDiveMerge2Rules#crown} (Wind: arrow bars on a thin ring; Fire: flame tongues; Water: wave crests with drops;
     * Earth: crystal spikes) floating {@link SpiritDiveMerge2Rules#HOVER} px, igniting and rising with the entry, following the head's yaw fully and its pitch by half; no spin (the tall bar stays at the front).
     * {@code leftOnly} (the first dive of Wind) draws the left half. Cost: the mesh's vertices ({@code SpiritDiveMerge2Rules#crownVerts}, at most 400 for Wind and Fire, 620 for Water) plus one soft disc.
     */
    static void crown(AuraContext c, Element el, boolean character, int glowRgb, float u, float uLeft, boolean upgrade, boolean leftOnly, float lit, float e) {
        float ign = Math.max(SpiritDiveReelRules.crown(u), upgrade ? SpiritDiveReelRules.crown(uLeft) : 0f);
        if (ign <= 0.01f) return;
        SpiritDiveWind3Mesh m = mesh(el, character, leftOnly);
        PoseStack pose = c.pose();
        ModelPart head = c.model().head;
        pose.pushPose();
        pose.translate(head.x / 16f, head.y / 16f, head.z / 16f);
        pose.mulPose(HEAD.rotationZYX(head.zRot, head.yRot, head.xRot * SpiritDiveMerge2Rules.PITCH_FOLLOW));
        pose.scale(1f / 16f, 1f / 16f, 1f / 16f);
        SealDraw.camera(pose);
        float age = c.age(), g = SpiritDiveReelRules.smooth(ign * 1.5f), rise = 0.4f + 0.6f * Math.min(1f, ign * 1.4f);
        float y = SpiritDiveMerge2Rules.HOVER + SpiritDiveMerge2Rules.BOB * (float) Math.sin(age * SpiritDiveMerge2Rules.BOB_SPEED);
        float breath = 0.78f + 0.22f * e;
        SealDraw.glow(pose, c.buffers().getBuffer(SafeAdditive.additive(SOFT)), 0f, y - 1.0f, 0f, 6.0f * (0.6f + 0.4f * g), 0f, SealDraw.argb(Math.min(1f, 0.22f * g * lit * breath), glowRgb), FB);
        pose.pushPose();
        SpiritDiveWind3Draw.basis(pose, 0f, y, 0f, 0f, -1f, 0f, 1f, 0f, 0f, 1f, rise, 1f);                    // the mesh's +y is the head's up (y is down in this frame)
        SpiritDiveWind3Draw.mesh(c, m, SpiritDiveWind3Rules.SOLID_GLOW, Math.min(1f, 0.98f * g), 0.5f * g * lit * breath, glowRgb);
        pose.popPose();
        pose.popPose();
    }
}
