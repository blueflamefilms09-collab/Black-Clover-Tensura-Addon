package com.newuniverse.nusmp.client.playermodel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.client.geo.GeoAnim;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.prop.PropDraw;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Why: draws the extras of a devil look (docs/devil_looks.md: cube hair, horns, wings, tails, bracers, mantles, glowing gems; GeoLite parts anchored on the body parts) on a plain vanilla
 * {@link PlayerModel}: the devil NPC of the ritual is not a living entity and a devil's form is worn on the player's own renderer, so neither can use {@link ExtrasLayer}. It is the loop of
 * {@link ExtrasDraw} with two differences: the model is any {@code PlayerModel} (the anchors are looked up on it), and every anchor has its own opacity, so a worn form forges in by stage (head,
 * then arms, wings and tail, then the legs, as {@code DevilFormRules} says) and the hair layers can be left out when the player keeps their own hair. Same cost rules as the kit: a model named
 * {@code ..._fine} is skipped beyond the detail distance, the additive glow pass only runs for the model with the glow flag and only while the frame's glow level is above zero, one map get per
 * model, no allocation of its own (an iterator over the model's small anchor map, like {@link ExtrasDraw}). Client only; the setting that switches it is {@link PlayerModelOptions#devilPlayerModel()}.
 */
public final class DevilExtras {
    private DevilExtras() {}

    /** The number of anchors: the length of the per-anchor opacity array ({@link PlayerLook.Anchor} ordinal order: head, body, arm_r, arm_l, leg_r, leg_l). */
    public static final int ANCHORS = PlayerLook.Anchor.values().length;

    /** The look a devil wears while the client option is on and the look exists, else null (the old figure is drawn). A map get, a synchronized table get, no allocation. */
    public static PlayerLook lookFor(String devilId) {
        String id = DevilLooks.of(devilId);
        return id != null && PlayerModelOptions.devilPlayerModel() ? PlayerAssets.lookOrNull(id) : null;
    }

    /** Is this a model of the hair layers (left out of a worn form: the player keeps their hair)? */
    public static boolean isHair(String model) { return model.endsWith("_hair") || model.endsWith("_hair_fine"); }

    private static ModelPart part(PlayerModel<?> m, PlayerLook.Anchor a) {
        return switch (a) {
            case HEAD -> m.head;
            case BODY -> m.body;
            case ARM_R -> m.rightArm;
            case ARM_L -> m.leftArm;
            case LEG_R -> m.rightLeg;
            case LEG_L -> m.leftLeg;
        };
    }

    /**
     * Draws the extras of {@code look} on the posed {@code model}. {@code alpha[anchor.ordinal()]} is the opacity of what is anchored there (0 = not drawn at all); {@code withHair} false skips the hair
     * layers; {@code lod} is the frame's {@link PlayerLod} level; {@code glowLevel} 0 draws no additive pass; {@code glowRgb} the colour of that pass; {@code clockSeconds} drives the looping clips,
     * {@code progress} the one-shot ones. The pose stack must be in the living-renderer frame (scaled, y down), exactly where {@link ExtrasLayer} draws.
     */
    public static void draw(PoseStack pose, MultiBufferSource buffers, PlayerModel<?> model, PlayerLook look, float[] alpha, boolean withHair, boolean dead, boolean swinging, float speed,
                            float clockSeconds, float progress, int light, int overlay, float glowLevel, int glowRgb, int lod) {
        for (PlayerLook.Extra ex : look.extras()) {
            if (lod != PlayerLod.FULL && ex.model().endsWith(ExtrasDraw.FINE_SUFFIX)) continue;
            if (!withHair && isHair(ex.model())) continue;
            ExtraModels.Ready r = ExtraModels.of(ex.model());
            if (r.empty()) continue;
            String clipName = ExtraClip.pick(r.clips, dead, false, 0, swinging, speed);
            GeoAnim.Clip clip = r.anim == null ? null : r.anim.clip(clipName);
            float seconds = ExtrasDraw.clock(clipName, clip, clockSeconds, progress);
            ResourceLocation tex = PlayerAssets.extraTexture(ex.texture(), false);
            int glow = ExtrasDraw.glowArgb(ex, glowLevel, glowRgb);
            ResourceLocation glowTex = glow != 0 ? PlayerAssets.extraTexture(ex.texture(), true) : null;
            if (glowTex != null && !PlayerAssets.exists(glowTex)) glow = 0;
            for (var e : r.views.entrySet()) {
                float a = alpha[e.getKey().ordinal()];
                if (a <= 0.01f) continue;
                int argb = a >= 0.98f ? 0xFFFFFFFF : (Math.max(0, Math.min(255, Math.round(a * 255f))) << 24) | 0xFFFFFF;
                boolean translucent = (argb >>> 24) < 250 || (ex.tint() >>> 24) < 250;
                RenderType rt = translucent ? AuraRender.translucent(tex) : AuraRender.cutout(tex);
                ModelPart part = part(model, e.getKey());
                pose.pushPose();
                part.translateAndRotate(pose);
                pose.translate(-part.x / 16f, -part.y / 16f, -part.z / 16f);
                GeoDraw.draw(pose, buffers.getBuffer(rt), e.getValue(), clip, seconds, GeoDraw.Space.PLAYER, light, overlay, ExtrasDraw.mul(argb, ex.tint()));
                if (glow != 0) {
                    int ga = ((glow >>> 24) * Math.max(0, Math.min(255, Math.round(a * 255f)))) / 255;
                    GeoDraw.draw(pose, buffers.getBuffer(AuraRender.additive(glowTex)), e.getValue(), clip, seconds, GeoDraw.Space.PLAYER, PropDraw.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, (ga << 24) | (glow & 0xFFFFFF));
                }
                pose.popPose();
            }
        }
    }
}
