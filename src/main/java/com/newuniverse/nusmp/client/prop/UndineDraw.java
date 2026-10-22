package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.client.geo.GeoAnim;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoModelData;
import com.newuniverse.nusmp.client.geo.GeoModels;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.undine.UndineLod;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Why: the drawing of the Undine (Noelle's water Spirit Lord, docs/magic/undine.md), apart from the entity glue ({@link UndinePropPainter}) so the headless preview (tools/render_preview, example
 * UndineProp) draws exactly what the game draws. Everything here is primitive math into a pose and buffers: no entity, no world, no allocation per frame (the passes are drawn one after the other, so
 * no lazy wrapper is needed).
 *
 * <p>The look. The figure is hand-painted in the flat anime cel look of the Black Clover stills, so its colours must not change with the angle it is lit from: the body is drawn through the
 * emissive alpha-blended entity type (full bright, no diffuse shading, soft anti-aliased edges), tinted a little darker in the dark so it is not a lamp. On top of it come the translucent water
 * pass (the flowing outer gown, foam bands, droplets) and a soft additive foam pass ({@code AuraRender.additive}, the SafeAdditive types, never {@code RenderType.eyes} directly). The full form
 * also stands on a ring of waves with two soft ripple rings. The models are authored at the size of the full form (3.1 blocks); the companion is the same model drawn smaller.</p>
 *
 * <p>Costs per call: the quads of {@link UndineLod} for the level of detail asked (body + water + fx + base), nothing else.</p>
 */
public final class UndineDraw {
    private UndineDraw() {}

    private static GeoSpec spec(String id) {
        return new GeoSpec(ResourceLocation.fromNamespaceAndPath("nusmp", "geo/entity/" + id + ".geo.json"), ResourceLocation.fromNamespaceAndPath("nusmp", "animations/entity/undine_body.animation.json"),
                ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/" + id + ".png"), null);
    }

    private static final GeoSpec[] BODY = {spec("undine_body"), spec("undine_body_lod1"), spec("undine_body_lod2")};
    private static final GeoSpec[] WATER = {spec("undine_water"), spec("undine_water_lod1")};
    private static final GeoSpec FX = spec("undine_fx"), BASE = spec("undine_base");
    private static final ResourceLocation RING = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/undine_ring.png");

    /** The size the companion is drawn at (about 1.8 blocks) and the size of the full form (about 3.2 blocks), of the model that is authored at 3.1 blocks. */
    public static final float COMPANION_SCALE = 0.60f, FULL_SCALE = 1.04f;

    /** Draws one pass of a model at rest or in a clip into the buffer of the render type. */
    private static void pass(PoseStack pose, MultiBufferSource buffers, GeoSpec s, net.minecraft.client.renderer.RenderType rt, GeoAnim.Clip clip, float secs, int light, int argb) {
        GeoModelData m = GeoModels.model(s.model());
        GeoDraw.draw(pose, buffers.getBuffer(rt), m, clip, secs, GeoDraw.Space.PROP, light, OverlayTexture.NO_OVERLAY, argb);
    }

    /** The tint of the cel colours for a packed light: 0.62 in the dark up to 1 in full light (the cel look stays flat, it only dims). */
    public static int tint(int light, float alpha) {
        int b = Math.max((light >> 4) & 15, (light >> 20) & 15);
        int g = (int) ((0.62f + 0.38f * b / 15f) * 255f + 0.5f);
        int a = Mth.clamp((int) (alpha * 255f + 0.5f), 0, 255);
        return (a << 24) | (g << 16) | (g << 8) | g;
    }

    /** Two soft ripple rings that spread over the water round her feet, one after the other (additive, on a proper ring texture that fades to nothing at both its edges: never a flat cut-out). */
    private static void ripples(PoseStack pose, MultiBufferSource buffers, float age, float fade) {
        var vc = buffers.getBuffer(AuraRender.additive(RING));
        for (int k = 0; k < 2; k++) {
            float t = (age / 56f + k * 0.5f) % 1f;
            float half = 0.9f + 2.7f * t;
            float a = fade * (1f - t) * SealDraw.smooth(t * 5f) * 0.9f;
            if (a <= 0.01f) continue;
            pose.pushPose();
            pose.translate(0f, 0.05f + 0.02f * k, 0f);
            SealDraw.flat(pose, vc, half, age * (k == 0 ? 1.5f : -1.2f), SealDraw.argb(a, 0xD8F0FF), PropDraw.FULL_BRIGHT);
            pose.popPose();
        }
    }

    /**
     * One Undine in the pose's space (the point under her at the origin, facing +z): {@code full} the big form with the wave base, else the companion. {@code lod} 0..2 from {@link UndineLod};
     * {@code fade} 0..1 is her opacity while she forms and fades; {@code clip} and {@code secs} pick the animation.
     */
    public static void undine(PoseStack pose, MultiBufferSource buffers, boolean full, int lod, String clip, float secs, float age, float fade, int light) {
        if (lod >= UndineLod.CULL || fade <= 0.01f) return;
        float s = (full ? FULL_SCALE : COMPANION_SCALE) * (0.9f + 0.1f * fade);
        if (full && lod <= UndineLod.HALF) ripples(pose, buffers, age, fade);
        GeoAnim a = GeoModels.animations(BODY[0].animations());
        GeoAnim.Clip c = a == null ? null : a.clip(clip);
        pose.pushPose();
        pose.scale(s, s, s);
        if (full && lod <= UndineLod.HALF) pass(pose, buffers, BASE, AuraRender.emissive(BASE.texture()), c, secs, light, tint(light, fade * 0.92f));
        pass(pose, buffers, BODY[lod], AuraRender.emissive(BODY[lod].texture()), c, secs, light, tint(light, fade));
        if (lod < WATER.length) pass(pose, buffers, WATER[lod], AuraRender.emissive(WATER[lod].texture()), c, secs, light, tint(light, fade * 0.7f));
        if (lod <= UndineLod.HALF) pass(pose, buffers, FX, AuraRender.additive(FX.texture()), c, secs, PropDraw.FULL_BRIGHT, SealDraw.argb(fade * (0.7f + 0.3f * Mth.sin(age * 0.2f)), 0xFFFFFF));
        pose.popPose();
    }
}
