package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 1.93.0 Soul Corpse Magic: how the two props look (GeoLite models of tools/gen_soulcorpse_rebuild_geo.py; the behaviours are prop.SoulCorpseRebuildProps).
 * SOUL_CORPSE_1, param low byte 0 or 1 = the marionette (and the decoy): soulcorpse_puppet (cutout, glow additive on top: the strings and the eyes), clip "walk" while it has
 * moved since the last tick, "attack" while param bit 8 is set, "collapse" (played once) from the start of its last 20 ticks (param bit 10), "idle" otherwise; a stitched ring
 * of particle/scorpse_stitch glows under its feet (additive, flat). Param 2 = the thread nail: soulcorpse_nail laid along the flight (pitched by the entity's x rotation) in its
 * "strike" clip with a glow (additive).
 * SOUL_CORPSE_2, the grasping hand: soulcorpse_hand (cutout, glow on top) rising out of the ground over the first 14 ticks and sinking over the last 10, clip "grasp" while
 * param bit 8 is set and "idle" otherwise; a grave marker (particle/scorpse_grave, additive, camera-facing) and a stitch ring on the ground. Pass order: the model first, every
 * additive pass after it. Every texture exists (particle/scorpse_*.png, entity/soulcorpse_*).
 */
public final class SoulCorpseRebuildPropPainter {
    private SoulCorpseRebuildPropPainter() {}

    private static final GeoSpec PUPPET = GeoSpec.of("soulcorpse", "puppet"), HAND = GeoSpec.of("soulcorpse", "hand"), NAIL = GeoSpec.of("soulcorpse", "nail");
    private static final int FB = PropDraw.FULL_BRIGHT, THREAD = 0xA8B4F0, PALE = 0xE8ECFF;
    private static ResourceLocation stitch, grave, glow;

    /** Called once on the client by PropPainterRegistry. */
    public static void register() {
        stitch = AuraRender.tex("particle/scorpse_stitch");
        grave = AuraRender.tex("particle/scorpse_grave");
        glow = AuraRender.tex("particle/glow");
        PropPainters.register(PropKind.SOUL_CORPSE_1, SoulCorpseRebuildPropPainter::one);
        PropPainters.register(PropKind.SOUL_CORPSE_2, SoulCorpseRebuildPropPainter::hand);
    }

    private static float fade(MagicPropEntity e, float partial, float in, float out) {
        float life = e.life() + partial, max = Math.max(1, e.maxLife());
        return SealDraw.smooth(life / in) * Mth.clamp((max - life) / out, 0f, 1f);
    }

    private static void one(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, int light) {
        if ((e.param() & 0xFF) == 2) nail(e, partial, age, pose, buf, light);
        else puppet(e, partial, age, pose, buf, light);
    }

    private static void puppet(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, int light) {
        float k = fade(e, partial, 8f, 1f);
        if (k <= 0.01f) return;
        boolean collapsing = (e.param() & 0x400) != 0, striking = (e.param() & 0x100) != 0;
        boolean moving = Math.abs(e.getX() - e.xo) + Math.abs(e.getZ() - e.zo) > 0.01;
        float since = Math.max(0f, e.life() + partial - (e.maxLife() - 20f)) / 20f;
        String clip = collapsing ? "collapse" : striking ? "attack" : moving ? "walk" : "idle";
        float clock = collapsing ? since : age / 20f;
        float s = e.scale() * 0.9f * (0.6f + 0.4f * k);
        SealDraw.camera(pose);
        pose.pushPose();
        pose.scale(s, s, s);
        GeoDraw.paint(pose, buf, PUPPET, clip, clock, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, SealDraw.argb(1f, 0xFFFFFF), SealDraw.argb(Mth.clamp(k * (0.6f + 0.2f * Mth.sin(age * 0.2f)), 0f, 1f), 0xFFFFFF));
        pose.popPose();
        VertexConsumer vc = buf.getBuffer(AuraRender.additive(stitch));
        pose.pushPose();
        pose.translate(0f, 0.04f, 0f);
        SealDraw.flat(pose, vc, 0.8f * s, age * 0.7f, SealDraw.argb(0.55f * k * (collapsing ? 1f - since : 1f), THREAD), FB);
        pose.popPose();
    }

    private static void nail(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, int light) {
        float k = fade(e, partial, 2f, 2f);
        if (k <= 0.01f) return;
        float s = e.scale() * (0.6f + 0.4f * k);
        SealDraw.camera(pose);
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(e.getXRot()));
        pose.translate(0f, -0.5f * s, 0f);
        pose.scale(s, s, s);
        GeoDraw.paint(pose, buf, NAIL, "strike", age / 20f, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF, SealDraw.argb(0.95f * k, 0xFFFFFF));
        pose.popPose();
        VertexConsumer vc = buf.getBuffer(AuraRender.additive(glow));
        SealDraw.glow(pose, vc, 0f, 0f, -0.2f * s, 0.4f * s, 0f, SealDraw.argb(0.5f * k, THREAD), FB);
        SealDraw.glow(pose, vc, 0f, 0f, 0.5f * s, 0.25f * s, 0f, SealDraw.argb(0.35f * k, PALE), FB);
    }

    private static void hand(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, int light) {
        float life = e.life() + partial, max = Math.max(1, e.maxLife());
        float rise = SealDraw.smooth(life / 14f) * SealDraw.smooth((max - life) / 10f);
        if (rise <= 0.01f) return;
        boolean closing = (e.param() & 0x100) != 0;
        float s = e.scale();
        SealDraw.camera(pose);
        pose.pushPose();
        pose.translate(0f, -1.55f * s * (1f - rise), 0f);
        pose.scale(s, s, s);
        GeoDraw.paint(pose, buf, HAND, closing ? "grasp" : "idle", age / 20f, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF, SealDraw.argb(0.6f * rise, 0xFFFFFF));
        pose.popPose();
        VertexConsumer vc = buf.getBuffer(AuraRender.additive(stitch));
        pose.pushPose();
        pose.translate(0f, 0.04f, 0f);
        SealDraw.flat(pose, vc, (0.9f + (closing ? 0.25f : 0f)) * s, -age * 0.6f, SealDraw.argb(0.6f * rise, THREAD), FB);
        pose.popPose();
        if (com.newuniverse.nusmp.vfx.FlatProps.drawFlat()) {                  // 1.112.0: the flat white grave billboard only comes back with visual.flatProps
            VertexConsumer g = buf.getBuffer(AuraRender.additive(grave));
            SealDraw.glow(pose, g, 0f, 0.15f * s, 0f, 0.5f * s * rise, 0f, SealDraw.argb(0.45f * rise, PALE), FB);
        }
    }
}
