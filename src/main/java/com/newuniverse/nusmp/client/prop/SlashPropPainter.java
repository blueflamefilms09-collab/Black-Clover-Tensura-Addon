package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.client.aura.SpatialArmourKit;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 1.93.0 Slash Magic rebuild: the summon models, GeoLite models (tools/gen_slash_geo.py) on the prop's feet (Space.PROP), solid CUTOUT with the glow map additive.
 * The old procedural drawing ({@code ModeledMagicProps.slash}) stays in its file, unused here.
 * <ul>
 *   <li>SLASH_1 param 0, the projected slash (Slash Wave): the wide jagged crescent of green-edged blade segments, tilted 18 degrees and spinning slowly as it flies
 *       (clip fly), with a streak behind it and a glint at its heart (additive, camera-facing). param 1, the Ripper Dash after-image: the hollow glass humanoid leaning
 *       into the dash, drawn TRANSLUCENT and fading over its life, with a streak along its back.</li>
 *   <li>SLASH_2, the Death Scythe: the long shaft and the vast curved blade sweeping once round the caster over 0.6 s (clip sweep), a serrated sweep ring on the ground
 *       and a cross of light at the tip of the blade. The scale is the reach (radius / 4).</li>
 * </ul>
 */
public final class SlashPropPainter {
    private SlashPropPainter() {}

    private static final GeoSpec CRESCENT = GeoSpec.of("slash", "crescent"), SCYTHE = GeoSpec.of("slash", "scythe"), GHOST = GeoSpec.of("slash", "ghost");
    private static final int GREEN = 0x48FF7A, PALE = 0xD2FFE0, FB = PropDraw.FULL_BRIGHT;
    private static ResourceLocation streak, ring, glint, cross;

    /** Called once on the client by PropPainterRegistry. */
    public static void register() {
        streak = AuraRender.tex("particle/slash_rip_streak");
        ring = AuraRender.tex("particle/slash_rip_ring");
        glint = AuraRender.tex("particle/slash_rip_glint");
        cross = AuraRender.tex("particle/slash_rip_cross");
        PropPainters.register(PropKind.SLASH_1, SlashPropPainter::slash1);
        PropPainters.register(PropKind.SLASH_2, SlashPropPainter::scythe);
    }

    private static void slash1(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        if (e.param() == 1) { ghost(e, age, pose, buffers, light); return; }
        float s = 0.9f * e.scale();
        float on = Mth.clamp(age / 3f, 0f, 1f), out = Mth.clamp((e.maxLife() - age) / 3f, 0f, 1f), k = Math.min(on, out);
        pose.pushPose();
        pose.scale(s * k, s * k, s * k);
        pose.translate(0f, -1f, 0f);                                    // the model's middle (16 px) on the prop's position
        GeoDraw.paint(pose, buffers, CRESCENT, "fly", age / 20f, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF, SpatialArmourKit.argb(0.95f, PALE));
        // a streak behind (along -z of the prop = the way it came from) and a glint at its heart
        SpatialArmourKit.camera(pose);
        VertexConsumer sv = buffers.getBuffer(AuraRender.additive(streak));
        SpatialArmourKit.streak(pose, sv, 0f, 1f, 2.4f, 0f, 1f, -0.2f, 0.45f, SpatialArmourKit.argb(0.55f * k, GREEN), FB);
        VertexConsumer gv = buffers.getBuffer(AuraRender.additive(glint));
        SpatialArmourKit.glow(pose, gv, 0f, 1f, -0.25f, 0.7f * (0.8f + 0.2f * Mth.sin(age * 0.9f)), age * 0.2f, SpatialArmourKit.argb(0.85f * k, PALE), FB);
        pose.popPose();
    }

    private static void ghost(MagicPropEntity e, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        float life = e.lifeFrac();
        float a = Mth.clamp(Math.min(age / 2f, 1f) * (1f - life), 0f, 1f) * 0.75f;
        if (a <= 0.01f) return;
        float s = 0.9f * e.scale();
        pose.pushPose();
        pose.scale(s, s, s);
        GeoDraw.paint(pose, buffers, GHOST, "idle", age / 20f, GeoDraw.Space.PROP, GeoDraw.Layer.TRANSLUCENT, PropDraw.FULL_BRIGHT, SpatialArmourKit.argb(a, 0xFFFFFF), SpatialArmourKit.argb(a, PALE));
        SpatialArmourKit.camera(pose);
        SpatialArmourKit.streak(pose, buffers.getBuffer(AuraRender.additive(streak)), 0f, 1.2f, 2.6f, 0f, 1.2f, 0.4f, 0.5f, SpatialArmourKit.argb(0.5f * a, GREEN), FB);
        pose.popPose();
    }

    private static void scythe(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        float s = Math.max(0.8f, e.scale());
        float on = Mth.clamp(age / 3f, 0f, 1f), out = Mth.clamp((e.maxLife() - age) / 4f, 0f, 1f), k = Math.min(on, out);
        pose.pushPose();
        // the ground ring first (the sweep's reach), then the scythe
        pose.pushPose();
        pose.translate(0f, 0.04f, 0f);
        SpatialArmourKit.flat(pose, buffers.getBuffer(AuraRender.additive(ring)), 1.5f * s * (0.6f + 0.4f * k), age * 9f, SpatialArmourKit.argb(0.7f * k, GREEN), FB);
        pose.popPose();
        pose.translate(0f, -0.5f, 0f);
        pose.scale(s * k, Math.min(s, 1.3f) * k, s * k);
        GeoDraw.paint(pose, buffers, SCYTHE, age < 12f ? "sweep" : "idle", age < 12f ? age / 20f : age / 20f, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF,
                SpatialArmourKit.argb(0.95f, PALE));
        pose.popPose();
    }
}
