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
 * Dante's Gravity Crown on the head. Three layers over the helmet the core paints:
 * <ol>
 *   <li><b>Solid</b> (lit): a square gold band with violet inlays and eight spikes, the tall one over the brow with its setting, and the dark cores of
 *       six floating shards.</li>
 *   <li><b>Emissive</b> (RenderType.entityTranslucentEmissive): the gem and the glowing shells of the shards.</li>
 *   <li><b>Additive</b> (RenderType.eyes): the thin lens-ring of the well that tilts above the crown.</li>
 * </ol>
 * Bone tracking: the band and spikes hang from {@code model.head} ({@code ModelPart#translateAndRotate} carries the head's pivot, its yaw and pitch
 * and the crouch offset), so they follow every look and the sneak. The shards and the lens-ring ride the same pivot but give back most of the head's
 * rotation (about 85% of the yaw, 60% of the pitch), so they hang in the air and lag behind a quick turn of the head, then catch up: they tilt with the
 * pitch only a little. The shards turn at {@link #SHARD_DEG} degrees a tick on the wearer's age with the partial tick, which is the interpolation between
 * ticks. When Weight of the Crown fires (a cue from the server) they are pulled into the crown, burn brighter, burst out and settle ({@link WornMath}).
 */
public final class DanteGravityCrownRenderer implements WornRenderer {
    private static final int SHARDS = 6;
    private static final float SHARD_DEG = 2.4f, ORBIT_R = 9.5f, ORBIT_Y = -14.0f, YAW_GIVE = 0.85f, PITCH_GIVE = 0.60f;
    private static final int FULL = LightTexture.FULL_BRIGHT;
    /** Spike positions round the square band (x, z), their heights, and the order they are drawn in. */
    private static final float[][] SPIKES = {{0f, -5.5f, 5.0f}, {5.5f, -5.5f, 4.0f}, {5.5f, 0f, 3.4f}, {5.5f, 5.5f, 3.8f}, {0f, 5.5f, 3.6f}, {-5.5f, 5.5f, 3.8f}, {-5.5f, 0f, 3.4f}, {-5.5f, -5.5f, 4.0f}};

    @Override
    public void render(WornCtx c) {
        if (c.firstPerson()) return;
        RelicDef def = Relics.defOf(c.stack());
        int main = def == null ? 0x2A1747 : def.paletteMain(), trim = def == null ? 0xD8B04A : def.paletteTrim(), glow = def == null ? 0xB58CFF : def.paletteGlow();
        var player = c.player();
        long now = player.level().getGameTime();
        WornCues.Cue cue = WornCues.get(player.getId(), WaveBossKinds.WHICH_CROWN, now);
        float t = cue == null ? -1f : WornMath.since(now, cue.start(), c.partialTick());
        float radius = WornMath.wellRadius(t), bright = WornMath.wellGlow(t), turn = WornMath.wellTurn(t);
        float age = c.ageInTicks();
        ModelPart head = c.model().head;
        PoseStack pose = c.pose();

        VertexConsumer vc = c.buffers().getBuffer(RenderType.entityCutoutNoCull(RelicTextures.WHITE));
        enter(pose, head);
        crown(pose.last(), vc, c.light(), main, trim);
        pose.popPose();
        shards(c, head, vc, age, turn, radius, bright, main, glow, 0);

        vc = c.buffers().getBuffer(RenderType.entityTranslucentEmissive(RelicTextures.WHITE));
        enter(pose, head);
        WornDraw.diamond(pose.last(), vc, FULL, WornDraw.argb(glow, Math.min(1f, 0.55f + 0.2f * bright)), 0f, -8.1f, -5.8f, 0.9f, 1.2f, 0.5f, 0f);
        pose.popPose();
        shards(c, head, vc, age, turn, radius, bright, main, glow, 1);

        vc = c.buffers().getBuffer(RenderType.eyes(RelicTextures.WHITE));
        lens(c, head, vc, age, turn, radius, bright, glow);
    }

    /** Pushes the pose and moves it to the head's pivot, with the head's rotation and the crouch offset, in pixels. */
    private static void enter(PoseStack pose, ModelPart head) {
        pose.pushPose();
        head.translateAndRotate(pose);
        pose.scale(WornDraw.PX, WornDraw.PX, WornDraw.PX);
    }

    /** The same, but giving back most of the head's yaw and pitch, so what hangs from it stays level and lags the head. */
    private static void enterFloating(PoseStack pose, ModelPart head) {
        enter(pose, head);
        pose.mulPose(Axis.XP.rotation(-head.xRot * PITCH_GIVE));
        pose.mulPose(Axis.YP.rotation(-head.yRot * YAW_GIVE));
    }

    // ---------------------------------------------------------------- the band and its spikes (solid)
    private static void crown(PoseStack.Pose p, VertexConsumer vc, int light, int main, int trim) {
        int gold = WornDraw.argb(trim, 1f), dark = WornDraw.argb(WornDraw.mix(trim, 0x000000, 0.45f), 1f), inlay = WornDraw.argb(main, 1f);
        // the band: four slabs round the helmet (the head is +-4, the helmet +-5), upper and lower rims, and the violet inlays
        WornDraw.box(p, vc, light, gold, -5.5f, -9.0f, -5.5f, 5.5f, -7.2f, -5.0f);
        WornDraw.box(p, vc, light, gold, -5.5f, -9.0f, 5.0f, 5.5f, -7.2f, 5.5f);
        WornDraw.box(p, vc, light, gold, -5.5f, -9.0f, -5.0f, -5.0f, -7.2f, 5.0f);
        WornDraw.box(p, vc, light, gold, 5.0f, -9.0f, -5.0f, 5.5f, -7.2f, 5.0f);
        WornDraw.box(p, vc, light, dark, -5.65f, -7.4f, -5.65f, 5.65f, -7.2f, 5.65f);
        WornDraw.box(p, vc, light, dark, -5.65f, -9.2f, -5.65f, 5.65f, -9.0f, 5.65f);
        WornDraw.box(p, vc, light, inlay, -3.6f, -8.6f, -5.62f, 3.6f, -7.7f, -5.5f);
        WornDraw.box(p, vc, light, inlay, -3.6f, -8.6f, 5.5f, 3.6f, -7.7f, 5.62f);
        WornDraw.box(p, vc, light, inlay, -5.62f, -8.6f, -3.6f, -5.5f, -7.7f, 3.6f);
        WornDraw.box(p, vc, light, inlay, 5.5f, -8.6f, -3.6f, 5.62f, -7.7f, 3.6f);
        // the spikes, leaning a little outward
        for (float[] s : SPIKES) {
            float len = (float) Math.sqrt(s[0] * s[0] + s[1] * s[1]);
            float lean = len < 1e-3f ? 0f : 0.7f / len;
            WornDraw.pyramid(p, vc, light, gold, s[0], -9.2f, s[1], 1.25f, 1.25f, s[0] + s[0] * lean, -9.2f - s[2], s[1] + s[1] * lean);
        }
        // the setting of the brow gem
        WornDraw.box(p, vc, light, gold, -1.4f, -9.0f, -5.8f, 1.4f, -7.2f, -5.5f);
        WornDraw.box(p, vc, light, dark, -1.1f, -8.7f, -5.9f, 1.1f, -7.5f, -5.8f);
    }

    // ---------------------------------------------------------------- the floating shards (pass 0: dark cores, pass 1: glowing shells)
    private static void shards(WornCtx c, ModelPart head, VertexConsumer vc, float age, float turn, float radius, float bright, int main, int glow, int pass) {
        PoseStack pose = c.pose();
        for (int i = 0; i < SHARDS; i++) {
            float ang = WornMath.orbit(age, i, SHARDS, SHARD_DEG) + turn;
            float r = ORBIT_R * radius;
            float y = ORBIT_Y + WornMath.bob(age, i, 0.9f) + (1f - Math.min(1f, radius)) * 3.0f;
            enterFloating(pose, head);
            pose.translate((float) Math.cos(Math.toRadians(ang)) * r, y, (float) Math.sin(Math.toRadians(ang)) * r);
            pose.mulPose(Axis.YP.rotationDegrees(-ang * 1.5f));
            pose.mulPose(Axis.ZP.rotationDegrees(14f + 8f * (float) Math.sin(age * 0.05 + i)));
            if (pass == 0) {
                WornDraw.diamond(pose.last(), vc, c.light(), WornDraw.argb(WornDraw.mix(main, glow, 0.30f), 1f), 0f, 0f, 0f, 0.8f, 2.4f, 0.8f, 0f);
            } else {
                float a = Math.min(0.95f, 0.38f * bright);
                WornDraw.diamond(pose.last(), vc, FULL, WornDraw.argb(glow, a), 0f, 0f, 0f, 1.25f, 3.3f, 1.25f, 0f);
            }
            pose.popPose();
        }
    }

    // ---------------------------------------------------------------- the lens ring (additive)
    private static void lens(WornCtx c, ModelPart head, VertexConsumer vc, float age, float turn, float radius, float bright, int glow) {
        PoseStack pose = c.pose();
        enterFloating(pose, head);
        pose.translate(0f, ORBIT_Y - 2.4f, 0f);
        pose.mulPose(Axis.XP.rotationDegrees(7f * (float) Math.sin(age * 0.04)));
        pose.mulPose(Axis.YP.rotationDegrees(age * 1.1f + turn));
        float r = ORBIT_R * (0.82f + 0.18f * radius);
        float a = Math.min(0.9f, 0.30f * bright);
        WornDraw.annulus(pose.last(), vc, FULL, glow, 0f, a, r - 1.1f, r, 28, 0f);
        WornDraw.annulus(pose.last(), vc, FULL, glow, a * 0.8f, 0f, r, r + 1.1f, 28, 0f);
        pose.popPose();
    }
}
