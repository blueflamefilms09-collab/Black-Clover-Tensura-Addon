package com.newuniverse.nusmp.client.relics.worn;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.relics.RelicTextures;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;

/**
 * Zenon's Spatial Ring on the wrist of the model's right arm. Bone tracking: everything hangs from {@code model.rightArm}
 * ({@code ModelPart#translateAndRotate}), whose pose already holds this frame's walk swing, the attack swing's rotation and offset, the raised-arm poses
 * of a bow or a held item, and the crouch offset, so the ring rides the wrist through all of them. The arm box is 4 pixels wide (3 for the slim skin)
 * and 12 long from 2 pixels above the shoulder pivot, so the wrist band sits at 7.4 to 8.8 pixels below the pivot, outside the sleeve layer.
 * <ol>
 *   <li><b>Solid</b>: a silver band with dark rims.</li>
 *   <li><b>Emissive</b>: the rift core (a vertical slit of light on the back of the band) and three rift fragments riding the rings.</li>
 *   <li><b>Additive</b>: two thin rings that turn round the arm's axis at different angles, like a small gyroscope; they flare (grow, spin once or
 *       twice and brighten) for a moment when Rift Step or Twin Gates fires.</li>
 * </ol>
 * Seen from the player's own eyes nothing is drawn (the first-person hand is the item renderer's business).
 */
public final class ZenonSpatialRingRenderer implements WornRenderer {
    private static final int FULL = LightTexture.FULL_BRIGHT;
    private static final float WRIST_Y = 8.1f, SPIN_DEG = 5.0f;

    @Override
    public void render(WornCtx c) {
        if (c.firstPerson()) return;
        RelicDef def = Relics.defOf(c.stack());
        int main = def == null ? 0x1B2257 : def.paletteMain(), trim = def == null ? 0xC9D3EE : def.paletteTrim(), glow = def == null ? 0x8FB0FF : def.paletteGlow();
        var player = c.player();
        long now = player.level().getGameTime();
        WornCues.Cue cue = WornCues.get(player.getId(), WaveBossKinds.WHICH_RING, now);
        int kind = cue == null ? 0 : cue.kind();
        float t = cue == null ? -1f : WornMath.since(now, cue.start(), c.partialTick());
        float expand = WornMath.ringExpand(t, kind), bright = WornMath.ringGlow(t, kind);
        float spin = c.ageInTicks() * SPIN_DEG + WornMath.ringTurn(t, kind);
        boolean slim = c.slim();
        float x0 = slim ? -2f : -3f, x1 = 1f;                                              // the arm box across, in the arm's own frame
        float cx = (x0 + x1) / 2f;
        ModelPart arm = c.model().rightArm;
        PoseStack pose = c.pose();

        VertexConsumer vc = c.buffers().getBuffer(RenderType.entityCutoutNoCull(RelicTextures.WHITE));
        enter(pose, arm);
        band(pose.last(), vc, c.light(), x0, x1, trim, main);
        pose.popPose();

        vc = c.buffers().getBuffer(RenderType.entityTranslucentEmissive(RelicTextures.WHITE));
        enter(pose, arm);
        WornDraw.diamond(pose.last(), vc, FULL, WornDraw.argb(glow, Math.min(1f, 0.6f + 0.2f * bright)), cx, WRIST_Y, -2.95f, 0.45f, 1.15f, 0.3f, 0f);
        pose.popPose();
        fragments(c, arm, vc, cx, spin, expand, bright, glow);

        vc = c.buffers().getBuffer(RenderType.eyes(RelicTextures.WHITE));
        rings(c, arm, vc, cx, spin, expand, bright, glow);
    }

    private static void enter(PoseStack pose, ModelPart arm) {
        pose.pushPose();
        arm.translateAndRotate(pose);
        pose.scale(WornDraw.PX, WornDraw.PX, WornDraw.PX);
    }

    /** The band: four slabs round the arm (outside the 0.25 pixel sleeve layer) with a rim above and below and a dark inlay on the front. */
    private static void band(PoseStack.Pose p, VertexConsumer vc, int light, float x0, float x1, int trim, int main) {
        int silver = WornDraw.argb(trim, 1f), rim = WornDraw.argb(WornDraw.mix(trim, 0x000000, 0.5f), 1f), inlay = WornDraw.argb(main, 1f);
        float y0 = WRIST_Y - 0.7f, y1 = WRIST_Y + 0.7f, o = 0.25f, w = 0.4f;
        WornDraw.box(p, vc, light, silver, x0 - o - w, y0, -2f - o - w, x1 + o + w, y1, -2f - o);          // front
        WornDraw.box(p, vc, light, silver, x0 - o - w, y0, 2f + o, x1 + o + w, y1, 2f + o + w);             // back
        WornDraw.box(p, vc, light, silver, x0 - o - w, y0, -2f - o, x0 - o, y1, 2f + o);                    // outer side
        WornDraw.box(p, vc, light, silver, x1 + o, y0, -2f - o, x1 + o + w, y1, 2f + o);                    // inner side
        float rx0 = x0 - o - w - 0.12f, rx1 = x1 + o + w + 0.12f, rz = 2f + o + w + 0.12f;
        WornDraw.box(p, vc, light, rim, rx0, y0 - 0.2f, -rz, rx1, y0, rz);
        WornDraw.box(p, vc, light, rim, rx0, y1, -rz, rx1, y1 + 0.2f, rz);
        float mid = (x0 + x1) / 2f;
        WornDraw.box(p, vc, light, inlay, mid - 1.0f, y0 + 0.2f, -rz - 0.05f, mid + 1.0f, y1 - 0.2f, -2f - o - w);
    }

    /** Two rings turning about the arm's axis, tilted differently; flared by the beat. */
    private static void rings(WornCtx c, ModelPart arm, VertexConsumer vc, float cx, float spin, float expand, float bright, int glow) {
        PoseStack pose = c.pose();
        float a = Math.min(0.95f, 0.34f * bright);
        for (int k = 0; k < 2; k++) {
            enter(pose, arm);
            pose.translate(cx, WRIST_Y, 0f);
            pose.mulPose(Axis.YP.rotationDegrees(k == 0 ? spin : -spin * 1.35f));
            pose.mulPose(Axis.XP.rotationDegrees(k == 0 ? 24f : -38f));
            float r = (k == 0 ? 4.2f : 3.1f) * expand;
            WornDraw.annulus(pose.last(), vc, FULL, glow, 0f, a, r - 0.55f, r, 20, 0f);
            WornDraw.annulus(pose.last(), vc, FULL, glow, a, 0f, r, r + 0.55f, 20, 0f);
            pose.popPose();
        }
    }

    /** Three small shards riding the first ring. */
    private static void fragments(WornCtx c, ModelPart arm, VertexConsumer vc, float cx, float spin, float expand, float bright, int glow) {
        PoseStack pose = c.pose();
        enter(pose, arm);
        pose.translate(cx, WRIST_Y, 0f);
        pose.mulPose(Axis.YP.rotationDegrees(spin));
        pose.mulPose(Axis.XP.rotationDegrees(24f));
        float r = 4.2f * expand, a = Math.min(0.95f, 0.5f * bright);
        for (int i = 0; i < 3; i++) {
            double ang = Math.toRadians(WornMath.orbit(0f, i, 3, 0f));
            WornDraw.diamond(pose.last(), vc, FULL, WornDraw.argb(glow, a), (float) Math.cos(ang) * r, 0f, (float) Math.sin(ang) * r, 0.35f, 0.9f, 0.35f, i * 40f);
        }
        pose.popPose();
    }
}
