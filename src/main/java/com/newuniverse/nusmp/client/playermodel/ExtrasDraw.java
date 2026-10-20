package com.newuniverse.nusmp.client.playermodel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.client.geo.GeoAnim;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.prop.PropDraw;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Why: draws the extras of a look (horns, wings, tails, a halo, a cape, floating weapons: GeoLite parts) on a posed {@link KitPlayerModel} the way the aura armour pieces are drawn
 * ({@code client/aura/RebuildSetKit}): the pose goes to the model part the root bone is named for ({@code translateAndRotate}: the piece turns about the part's own pivot), then the pose is
 * moved back by the part's offset so the model file's absolute humanoid coordinates apply (y up, 24 px = the neck line), and the geo model is drawn in {@link GeoDraw.Space#PLAYER}. Each extra is
 * painted solid (or translucent while the character fades), then its glow map additive and full bright, scaled by the frame's glow level. With no entity the caller passes the clock and the
 * state itself, so the Avatar creation screen draws the same parts.
 */
public final class ExtrasDraw {
    private ExtrasDraw() {}

    /** What decides the clip of the extras this frame. */
    public record Motion(boolean dead, boolean transforming, int cast, boolean swinging, float speed, float seconds, float progress) {
        public static final Motion IDLE = new Motion(false, false, 0, false, 0f, 0f, 0f);
    }

    public static void draw(PoseStack pose, MultiBufferSource buffers, KitPlayerModel<?> model, PlayerLook look, Motion m, int light, int overlay, int argb, float glowLevel, int glowRgb) {
        draw(pose, buffers, model, look, m.dead(), m.transforming(), m.cast(), m.swinging(), m.speed(), m.seconds(), m.progress(), false, false, light, overlay, argb, glowLevel, glowRgb);
    }

    /**
     * The same without a {@link Motion} object (the entity renderer draws this every frame and allocates nothing for it). {@code skipArmR} / {@code skipArmL}: the entity really holds an item in that
     * hand, so the extras hung on that arm (the art kit's picture of its weapon) are not drawn: the real item is.
     */
    public static void draw(PoseStack pose, MultiBufferSource buffers, KitPlayerModel<?> model, PlayerLook look, boolean dead, boolean transforming, int cast, boolean swinging, float speed,
                            float clockSeconds, float progress, boolean skipArmR, boolean skipArmL, int light, int overlay, int argb, float glowLevel, int glowRgb) {
        for (PlayerLook.Extra ex : look.extras()) {
            ExtraModels.Ready r = ExtraModels.of(ex.model());
            if (r.empty()) continue;
            String clipName = ExtraClip.pick(r.clips, dead, transforming, cast, swinging, speed);
            GeoAnim.Clip clip = r.anim == null ? null : r.anim.clip(clipName);
            float seconds = clock(clipName, clip, clockSeconds, progress);
            ResourceLocation tex = PlayerAssets.extraTexture(ex.texture(), false);
            boolean translucent = (argb >>> 24) < 250 || (ex.tint() >>> 24) < 250;
            RenderType rt = translucent ? AuraRender.translucent(tex) : AuraRender.cutout(tex);
            int color = mul(argb, ex.tint());
            int glow = glowArgb(ex, glowLevel, glowRgb);
            ResourceLocation glowTex = glow != 0 ? PlayerAssets.extraTexture(ex.texture(), true) : null;
            if (glowTex != null && !PlayerAssets.exists(glowTex)) glow = 0;
            for (var e : r.views.entrySet()) {
                if ((skipArmR && e.getKey() == PlayerLook.Anchor.ARM_R) || (skipArmL && e.getKey() == PlayerLook.Anchor.ARM_L)) continue;
                net.minecraft.client.model.geom.ModelPart part = model.part(e.getKey());
                pose.pushPose();
                part.translateAndRotate(pose);
                pose.translate(-part.x / 16f, -part.y / 16f, -part.z / 16f);
                GeoDraw.draw(pose, buffers.getBuffer(rt), e.getValue(), clip, seconds, GeoDraw.Space.PLAYER, light, overlay, color);
                if (glow != 0)
                    GeoDraw.draw(pose, buffers.getBuffer(AuraRender.additive(glowTex)), e.getValue(), clip, seconds, GeoDraw.Space.PLAYER, PropDraw.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, glow);
                pose.popPose();
            }
        }
    }

    /** The seconds into a clip: a one-shot clip runs on the frame's progress, a looping one on the clock. */
    static float clock(String name, GeoAnim.Clip clip, Motion m) { return clock(name, clip, m.seconds(), m.progress()); }

    static float clock(String name, GeoAnim.Clip clip, float clockSeconds, float progress) {
        if (clip == null) return clockSeconds;
        boolean oneShot = name.equals(ExtraClip.DEATH) || name.equals(ExtraClip.TRANSFORM) || name.equals(ExtraClip.ATTACK) || name.equals(ExtraClip.CAST_BIG) || name.equals(ExtraClip.HURT);
        return oneShot ? Math.max(0f, Math.min(1f, progress)) * clip.length : ExtraClip.time(clockSeconds, clip.length, clip.loop);
    }

    /** The additive glow colour of an extra: its glow flag, the frame's level and colour; 0 = no glow pass. */
    static int glowArgb(PlayerLook.Extra ex, float level, int rgb) {
        if (!ex.glow() || level <= 0.01f) return 0;
        int a = Math.max(0, Math.min(255, (int) (255f * Math.min(1f, level))));
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    /** ARGB times ARGB, channel by channel. */
    static int mul(int a, int b) {
        int aa = (a >>> 24) * (b >>> 24) / 255, r = ((a >> 16) & 255) * ((b >> 16) & 255) / 255, g = ((a >> 8) & 255) * ((b >> 8) & 255) / 255, bl = (a & 255) * (b & 255) / 255;
        return (aa << 24) | (r << 16) | (g << 8) | bl;
    }
}
