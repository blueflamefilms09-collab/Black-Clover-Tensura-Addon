package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.prop.AirPropKit;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 1.93.0 Smoke Magic: how its summons look (SMOKE_1, variant in the low byte of param, see prop.AirPropKit).
 * <ul>
 *   <li>0, the smoke clone: the geo model smoke_clone (a caster-shaped figure of billows that thins into a plume instead of legs, ember eyes, an ember
 *       core). It thickens out of the ground over 10 ticks, plays idle, the strike clip from the synced state (starting at the tick the state began)
 *       and the vanish clip over the last 14 ticks of its life.</li>
 *   <li>1, the smoke serpent: smoke_serpent, idle while it lives, the strike clip for its lunge, shrinking away over its last 8 ticks.</li>
 *   <li>2, the coiling serpent: three small smoke_serpents circling the held foe at three heights (a coil), turning faster as the hold goes on.</li>
 * </ul>
 * Render types: the models are CUTOUT (solid, alpha tested, first) with their glow map additive and full bright straight after (GeoDraw); while a summon
 * thickens in or vanishes they are drawn TRANSLUCENT so they can fade; the soft ground shade is a translucent quad lifted 2 cm.
 */
public final class SmokePropPainter {
    private SmokePropPainter() {}

    private static final GeoSpec CLONE = GeoSpec.of("smoke", "clone"), SERPENT = GeoSpec.of("smoke", "serpent");
    private static ResourceLocation shadow;

    /** Called once on the client by PropPainterRegistry. */
    public static void register() {
        shadow = AuraRender.tex("particle/smoke_puff");
        PropPainters.register(PropKind.SMOKE_1, SmokePropPainter::paint);
    }

    private static float smooth(float t) { t = Mth.clamp(t, 0f, 1f); return t * t * (3f - 2f * t); }

    private static void shade(PoseStack pose, MultiBufferSource buffers, float r, float alpha, int light) {
        VertexConsumer vc = buffers.getBuffer(AuraRender.translucent(shadow));
        int a = Mth.clamp((int) (alpha * 255f), 0, 255) << 24;
        PropDraw.quad(pose, vc, -r, 0.02f, r, r, 0.02f, r, r, 0.02f, -r, -r, 0.02f, -r, 0f, 0f, 1f, 1f, a | 0x2A2636, light);
    }

    private static void paint(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        int p = e.param(), v = AirPropKit.variant(p);
        long now = e.level().getGameTime();
        float s = Math.max(0.3f, e.scale());
        float maxL = Math.max(1f, e.maxLife());
        float left = maxL - age;
        if (v == 0) clone(e, partial, age, left, p, now, s, pose, buffers, light);
        else if (v == 1) serpent(age, left, p, now, partial, s, pose, buffers, light);
        else coil(e, age, left, s, pose, buffers, light);
    }

    private static void clone(MagicPropEntity e, float partial, float age, float left, int p, long now, float s, PoseStack pose, MultiBufferSource buffers, int light) {
        float rise = smooth(age / 10f), out = left < 14f ? Mth.clamp(left / 14f, 0f, 1f) : 1f;
        String clip;
        float secs;
        if (left < 14f) { clip = "vanish"; secs = (14f - left) / 14f; }
        else if (AirPropKit.state(p) == AirPropKit.ACT && AirPropKit.since(p, now) < 18) { clip = "strike"; secs = (AirPropKit.since(p, now) + partial) / 20f; }
        else { clip = "idle"; secs = age / 20f; }
        float alpha = rise * out;
        shade(pose, buffers, 0.8f * s * alpha, 0.35f * alpha, light);
        pose.pushPose();
        pose.translate(0f, -(1f - rise) * 0.6f * s, 0f);
        pose.scale(s * 0.85f, s * 0.85f, s * 0.85f);
        boolean solid = alpha >= 0.99f;
        GeoDraw.paint(pose, buffers, CLONE, clip, secs, GeoDraw.Space.PROP, solid ? GeoDraw.Layer.CUTOUT : GeoDraw.Layer.TRANSLUCENT, light,
                solid ? 0xFFFFFFFF : AuraRender.alpha(0xFFFFFFFF, alpha), AuraRender.alpha(0xFFFFB070, (0.7f + 0.3f * Mth.sin(age * 0.2f)) * alpha));
        pose.popPose();
    }

    private static void serpent(float age, float left, int p, long now, float partial, float s, PoseStack pose, MultiBufferSource buffers, int light) {
        float out = left < 8f ? Mth.clamp(left / 8f, 0f, 1f) : 1f, in = smooth(age / 4f), a = out * in;
        boolean lunge = age < 14f;
        float secs = lunge ? age / 20f : (age - 14f) / 20f;
        pose.pushPose();
        pose.scale(s * 0.7f * (0.5f + 0.5f * a), s * 0.7f * (0.5f + 0.5f * a), s * 0.7f * (0.5f + 0.5f * a));
        boolean solid = a >= 0.99f;
        GeoDraw.paint(pose, buffers, SERPENT, lunge ? "strike" : "idle", secs, GeoDraw.Space.PROP, solid ? GeoDraw.Layer.CUTOUT : GeoDraw.Layer.TRANSLUCENT, light,
                solid ? 0xFFFFFFFF : AuraRender.alpha(0xFFFFFFFF, a), AuraRender.alpha(0xFFFFB070, a));
        pose.popPose();
    }

    private static void coil(MagicPropEntity e, float age, float left, float s, PoseStack pose, MultiBufferSource buffers, int light) {
        float a = Mth.clamp(Math.min(age / 6f, left / 8f), 0f, 1f);
        float h = Math.max(1f, e.getBbHeight());
        for (int k = 0; k < 3; k++) {
            float ang = age * (4.5f + 1.4f * k) + k * 120f, y = (0.25f + 0.32f * k) * h;
            pose.pushPose();
            pose.translate(0f, y, 0f);
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(ang));
            pose.translate(0f, 0f, 0.55f * Math.max(0.8f, e.getBbWidth()));
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90f));
            float sc = 0.3f * s * (0.6f + 0.4f * a);
            pose.scale(sc, sc, sc);
            GeoDraw.paint(pose, buffers, SERPENT, "idle", age / 20f + k * 0.4f, GeoDraw.Space.PROP, GeoDraw.Layer.TRANSLUCENT, light,
                    AuraRender.alpha(0xFFFFFFFF, 0.9f * a), AuraRender.alpha(0xFFFFB070, a));
            pose.popPose();
        }
    }
}
