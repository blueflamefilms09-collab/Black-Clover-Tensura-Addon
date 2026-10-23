package com.newuniverse.nusmp.client.mode;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.blackclover.ModeArmor;
import com.newuniverse.nusmp.book.ModeArmorWindV2Mesh;
import com.newuniverse.nusmp.book.ModeArmorWindV2Rules;
import com.newuniverse.nusmp.book.ModeArmorWindV2Rules.Entry;
import com.newuniverse.nusmp.book.ModeArmorWindV2Rules.Look;
import com.newuniverse.nusmp.book.ModeArmorWindV2Rules.Part;
import com.newuniverse.nusmp.book.ModeArmorWindV2Rules.Pass;
import com.newuniverse.nusmp.book.ModeArmorWindV2Rules.Piece;
import com.newuniverse.nusmp.book.SpiritDiveReelRules;
import com.newuniverse.nusmp.book.SpiritDiveTier.Lod;
import com.newuniverse.nusmp.client.SafeAdditive;
import com.newuniverse.nusmp.client.aura.SpiritDiveFrame;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The Wind Spirit Dive overlay of YUNO, version 2 ("10x better"): a layered 3D coat, a small crown of arrow blades and two leaf wings, drawn on the player model while the mode
 * {@link ModeArmor.Mode#WIND_SPIRIT_DIVE_YUNO} runs (the dive or Spirit of Zephyr of the mage whose bound grimoire is Yuno's) and the client option {@code modeArmor.windLook} is V2 (the default).
 * Every other Wind user, and the option CLASSIC, keep the 73-quad overlay of {@link ModeArmorLayer}.
 *
 * <p>How it is drawn: the meshes are static arrays built once by {@link ModeArmorWindV2Mesh} (the same arrays the unit tests and the preview read). Per wearer the layer captures one
 * {@link PoseStack.Pose} per body part (body, arms, legs, head, the crown and the two wing hinges), then draws pass by pass (cloth, fur, glow, wings, light pieces, additive effects) so a wearer costs
 * six buffer switches, not one per piece. The coat tails, cords, ribbons and the leaves of the wings sway with a CPU wave (a sine per vertex that has a sway weight); nothing is allocated per
 * frame by this class (static pose slots and scratch fields; the PoseStack's own push allocates as it does for every layer).</p>
 *
 * <p>Cost: {@link ModeArmorWindV2Rules#vertices} per level of detail (at most 3,200 / 1,200 / 150 vertices, nothing beyond 64 blocks); at most {@link ModeArmorWindV2Rules#CHARACTER_PER_FRAME} wearers a
 * frame draw the full level ({@link SpiritDiveFrame#admitModeV2}), the rest draw the mid level. Render thread only; no packet, no server state.</p>
 */
public final class ModeArmorWindV2 {
    private ModeArmorWindV2() {}

    private static volatile Look look = ModeArmorWindV2Rules.DEFAULT_LOOK;

    /** The client option {@code modeArmor.windLook}. */
    public static Look look() { return look; }

    public static void setLook(Look l) { look = l == null ? ModeArmorWindV2Rules.DEFAULT_LOOK : l; }

    /** True when the overlay of {@code mode} is drawn by this class (Yuno's mode with the option on V2). */
    public static boolean active(ModeArmor.Mode mode) { return mode == ModeArmor.Mode.WIND_SPIRIT_DIVE_YUNO && look == Look.V2; }

    /**
     * True while the overlay v2 is what draws the wings and the crown of player {@code entityId}: the Spirit Dive aura of the Yuno tier ({@code client.aura.SpiritDiveWind}) asks this and then leaves out
     * its own leaf wings and 3D crown (they would duplicate the overlay); its sword, bow, wrist circle, merge layer and entry stay. One map read per diver per frame.
     */
    public static boolean replacesAuraPieces(int entityId) {
        if (look != Look.V2) return false;
        var level = net.minecraft.client.Minecraft.getInstance().level;
        return level != null && ModeArmorClient.mode(entityId, level.getGameTime()) == ModeArmor.Mode.WIND_SPIRIT_DIVE_YUNO;
    }

    private static final int FULL = LightTexture.FULL_BRIGHT;
    private static final ResourceLocation COAT = tex("mwv2_coat"), FUR = tex("mwv2_fur"), GLOW = tex("mwv2_glow"), WING = tex("mwv2_wing"), FX = tex("mwv2_fx");

    private static ResourceLocation tex(String n) { return ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/mode/" + n + ".png"); }

    /** One pose per part, captured while the wearer's pose stack stands in the part, drawn pass by pass; cleared after the draw. */
    private static final PoseStack.Pose[] POSE = new PoseStack.Pose[Part.values().length];
    /** The wearer's wave scratch (the piece being drawn): amplitude of the sine wave, of the run flare, the speed and the phase scale. */
    private static float ax, ay, az, bx, by, bz, wave, phaseScale;
    private static boolean swaying, wingsOn, crownOn;

    private static void enter(PoseStack pose, ModelPart part) {
        pose.pushPose();
        part.translateAndRotate(pose);
        pose.scale(1f / 16f, 1f / 16f, 1f / 16f);
    }

    // ================================================================ the draw
    /**
     * Draws one wearer. {@code k} = grow-in x fade-out (0..1), {@code u} = the transformation clock of the Spirit Dive ({@link SpiritDiveReelRules#clock}: 0 at the start .. 22 when the entry is over; the wings
     * unfold and the crown ignites on it exactly as in the aura), {@code swing} = limb swing amount (the coat tails flare as the wearer runs), {@code distSq} = squared distance to the camera.
     */
    public static void draw(PoseStack pose, MultiBufferSource buffers, int light, PlayerModel<AbstractClientPlayer> m, float now, float k, float u, float swing, double distSq) {
        Lod lod = ModeArmorWindV2Rules.lod(distSq);
        if (lod == Lod.CULLED) return;
        lod = ModeArmorWindV2Rules.effective(lod, lod != Lod.FULL || SpiritDiveFrame.admitModeV2());
        float wing = SpiritDiveReelRules.wingScale(u), ignite = SpiritDiveReelRules.crown(u);
        wingsOn = wing > 0.02f;
        crownOn = ignite > 0.01f;
        capture(pose, m, now, k, wing, ignite, lod);
        int alpha = (int) (255 * Mth.clamp(k, 0f, 1f));
        if (alpha <= 2) { clear(); return; }
        Entry[] es = ModeArmorWindV2Rules.entries(lod);
        for (Pass pass : PASSES) {
            VertexConsumer vc = null;
            for (int i = 0; i < es.length; i++) {
                Entry e = es[i];
                if (e.pass() != pass) continue;
                if (vc == null) vc = buffer(buffers, pass);
                drawEntry(vc, e, now, swing, pass == Pass.COAT || pass == Pass.FUR ? light : FULL, pass == Pass.WING_ADD ? alpha * 6 / 10 : alpha, pass);
            }
            if (pass == Pass.FX && ModeArmorWindV2Rules.leaves(lod) > 0) {
                if (vc == null) vc = buffer(buffers, pass);
                leaves(vc, ModeArmorWindV2Rules.leaves(lod), now, alpha);
            }
        }
        clear();
    }

    private static final Pass[] PASSES = Pass.values();

    private static VertexConsumer buffer(MultiBufferSource b, Pass pass) {
        return switch (pass) {
            case COAT -> b.getBuffer(RenderType.entityCutoutNoCull(COAT));
            case FUR -> b.getBuffer(RenderType.entityCutoutNoCull(FUR));
            case GLOW -> b.getBuffer(SafeAdditive.additive(GLOW));
            case WING -> b.getBuffer(RenderType.entityTranslucent(WING));
            case FX -> b.getBuffer(SafeAdditive.emissive(FX));
            case ADD -> b.getBuffer(SafeAdditive.additive(FX));
            case WING_ADD -> b.getBuffer(SafeAdditive.additive(WING));
        };
    }

    private static void clear() { java.util.Arrays.fill(POSE, null); }

    /** Captures the pose of every part the level draws (the wings and the crown get their own motion). */
    private static void capture(PoseStack pose, PlayerModel<AbstractClientPlayer> m, float now, float k, float wing, float ignite, Lod lod) {
        enter(pose, m.body);
        POSE[Part.BODY.ordinal()] = pose.last();
        for (int side = 0; side < 2; side++) {                                                              // the wing hinges: the root of the leaf at the shoulder blade
            float sg = side == 0 ? 1f : -1f;
            pose.pushPose();
            pose.translate(sg * 1.8f, 2.5f, 2.7f);
            float flap = ModeArmorWindV2Rules.FLAP_DEG * Mth.sin(now * ModeArmorWindV2Rules.FLAP_SPEED + (side == 0 ? 0f : 0.35f));
            pose.mulPose(Axis.ZP.rotationDegrees(-sg * (flap + 4f)));
            float g = Math.max(0.01f, wing * (0.3f + 0.7f * k));
            pose.scale(g, g, g);
            POSE[(side == 0 ? Part.WING_L : Part.WING_R).ordinal()] = pose.last();
            pose.popPose();
        }
        pose.popPose();
        if (lod != Lod.FAR) {
            enter(pose, m.rightArm); POSE[Part.ARM_R.ordinal()] = pose.last(); pose.popPose();
            enter(pose, m.leftArm); POSE[Part.ARM_L.ordinal()] = pose.last(); pose.popPose();
            enter(pose, m.rightLeg); POSE[Part.LEG_R.ordinal()] = pose.last(); pose.popPose();
            enter(pose, m.leftLeg); POSE[Part.LEG_L.ordinal()] = pose.last(); pose.popPose();
            enter(pose, m.head); POSE[Part.HEAD.ordinal()] = pose.last(); pose.popPose();
        }
        enter(pose, m.head);                                                                                // the crown floats over the head, bobbing and turning slowly
        pose.translate(0f, ModeArmorWindV2Rules.CROWN_Y - ModeArmorWindV2Rules.CROWN_BOB * Mth.sin(now * ModeArmorWindV2Rules.CROWN_BOB_SPEED), 0f);
        pose.mulPose(Axis.YP.rotationDegrees(now * ModeArmorWindV2Rules.CROWN_SPIN_DEG));
        pose.scale(1f, 0.3f + 0.7f * Math.min(k, SpiritDiveReelRules.smooth(ignite * 1.5f)), 1f);
        POSE[Part.CROWN.ordinal()] = pose.last();
        pose.popPose();
    }

    private static void drawEntry(VertexConsumer vc, Entry e, float now, float swing, int light, int alpha, Pass pass) {
        ModeArmorWindV2Mesh mesh = ModeArmorWindV2Mesh.get(e.piece(), e.detail());
        Part left = ModeArmorWindV2Rules.partOf(e.piece());
        if ((!wingsOn && left == Part.WING_L) || (!crownOn && left == Part.CROWN)) return;
        setSway(e.piece(), now);
        for (int c = 0; c < e.copies(); c++) {
            Part part = c == 0 ? left : right(left);
            PoseStack.Pose p = POSE[part.ordinal()];
            if (p == null) continue;
            emit(vc, p, mesh, c == 0 ? 1f : -1f, swing, alpha, light);
        }
    }

    private static Part right(Part left) {
        return switch (left) { case ARM_L -> Part.ARM_R; case LEG_L -> Part.LEG_R; case WING_L -> Part.WING_R; default -> left; };
    }

    /** The wave of a piece: which way and how far its swaying vertices move (a sine of the time and the vertex phase), and how much the run flares it. Pieces without a wave are rigid. */
    private static void setSway(Piece piece, float now) {
        swaying = true;
        phaseScale = 1f;
        switch (piece) {
            case TAILS_BACK -> { ax = 0f; ay = -0.3f; az = ModeArmorWindV2Rules.TAIL_SWAY; bx = 0f; by = -0.4f; bz = ModeArmorWindV2Rules.TAIL_FLARE; wave = now * ModeArmorWindV2Rules.TAIL_SWAY_SPEED * 8f; }
            case TAILS_FRONT -> { ax = 0f; ay = 0f; az = -0.45f; bx = 0f; by = 0f; bz = 0.5f; wave = now * ModeArmorWindV2Rules.TAIL_SWAY_SPEED * 8f; }
            case CORDS, TASSEL -> { ax = 0.55f; ay = 0f; az = 0.5f; bx = 0f; by = 0f; bz = 1.4f; wave = now * 0.9f; }
            case WING_BIG, WING_SMALL, WING_ORN -> { ax = 0.2f * ModeArmorWindV2Rules.WING_SWAY; ay = 0.12f * ModeArmorWindV2Rules.WING_SWAY; az = ModeArmorWindV2Rules.WING_SWAY; bx = 0f; by = 0f; bz = 0f;
                wave = now * ModeArmorWindV2Rules.WING_SWAY_SPEED * 8f; phaseScale = 2.2f; }
            case RIBBON_CUFF, RIBBON_TAIL -> { ax = 0.9f; ay = 0f; az = 0.7f; bx = 0f; by = 0f; bz = 2.2f; wave = now * 0.7f; phaseScale = 2.4f; }
            default -> swaying = false;
        }
    }

    /** Writes the quads of a mesh at a pose, mirrored in x by {@code mir}; the vertices with a sway weight move along the wave of the piece. */
    private static void emit(VertexConsumer vc, PoseStack.Pose p, ModeArmorWindV2Mesh m, float mir, float swing, int alpha, int light) {
        float[] pos = m.pos, uv = m.uv, nr = m.nrm, sw = m.sw, ph = m.ph;
        boolean sway = swaying;
        for (int j = 0, n = m.verts; j < n; j++) {
            int i = mir < 0f ? (j & ~3) + ((4 - (j & 3)) & 3) : j;                                              // a mirrored copy keeps its front faces by walking each quad backwards
            float x = pos[i * 3], y = pos[i * 3 + 1], z = pos[i * 3 + 2];
            if (sway) {
                float w = sw[i];
                if (w > 0f) {
                    float s = Mth.sin(wave + ph[i] * phaseScale * 2.1f);
                    x += w * (ax * s + bx * swing);
                    y += w * (ay * s + by * swing);
                    z += w * (az * s + bz * swing);
                }
            }
            vc.addVertex(p, x * mir, y, z).setColor(255, 255, 255, alpha).setUv(uv[i * 2], uv[i * 2 + 1]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                    .setNormal(p, nr[i * 3] * mir, nr[i * 3 + 1], nr[i * 3 + 2]);
        }
    }

    // ================================================================ the leaves
    /** A few leaves drifting up and out from the wings (body frame), turning as they go: each one quad, alpha follows a half sine over its life. */
    private static void leaves(VertexConsumer vc, int count, float now, int alpha) {
        PoseStack.Pose p = POSE[Part.BODY.ordinal()];
        if (p == null) return;
        int[] r = ModeArmorWindV2Rules.R_LEAF;
        float u0 = r[0] / (float) ModeArmorWindV2Rules.FX_W, v0 = r[1] / (float) ModeArmorWindV2Rules.FX_H, u1 = r[2] / (float) ModeArmorWindV2Rules.FX_W, v1 = r[3] / (float) ModeArmorWindV2Rules.FX_H;
        for (int i = 0; i < count; i++) {
            float life = now * 0.012f + i * (1f / 6f) + 0.07f * (i % 3);
            float t = life - (float) Math.floor(life);
            float side = (i & 1) == 0 ? 1f : -1f;
            float x = side * (3.5f + 17f * t + 2.5f * Mth.sin(now * 0.07f + i * 1.9f));
            float y = 3f - 26f * t + 1.5f * Mth.sin(now * 0.11f + i);
            float z = 4.5f + 8f * t + 2f * Mth.sin(now * 0.05f + i * 2.3f);
            float a = now * 0.09f + i * 2.1f, cs = Mth.cos(a) * 1.5f, sn = Mth.sin(a) * 1.5f;
            int al = (int) (alpha * 0.9f * Mth.sin(t * Mth.PI));
            if (al <= 3) continue;
            vert(vc, p, x - cs, y + 1.3f * sn, z - sn, u0, v1, al);
            vert(vc, p, x + cs, y - 1.3f * sn, z + sn, u1, v1, al);
            vert(vc, p, x + cs * 0.4f, y - 2.4f, z + sn * 0.4f, u1, v0, al);
            vert(vc, p, x - cs * 0.4f, y - 2.4f, z - sn * 0.4f, u0, v0, al);
        }
    }

    private static void vert(VertexConsumer vc, PoseStack.Pose p, float x, float y, float z, float u, float v, int alpha) {
        vc.addVertex(p, x, y, z).setColor(255, 255, 255, alpha).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL).setNormal(p, 0f, -1f, 0f);
    }
}
