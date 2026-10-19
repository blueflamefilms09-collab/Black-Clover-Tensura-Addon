package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.prop.AirPropWord;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 1.93.0 Snow Magic: how its summons look (SNOW_1, variant in the low byte of param, see prop.AirPropKit).
 * <ul>
 *   <li>0, the snow golem: the geo model snow_golem (chunky packed snow, fur yoke, ice horns and shoulder spears, an ice plate with a glowing heart, glowing eyes).
 *       It rises out of the ground over 12 ticks, plays idle / strike from the synced state and crumbles over the last 24 ticks of its life.</li>
 *   <li>1, the frost crystal: snow_crystal (a tall ice crystal with six satellites on a ring of drifts, glowing core), the grow clip when it appears, then
 *       idle (the ring turns, the crystal bobs); a pale frost ring pulses on the ground each second.</li>
 * </ul>
 * Render types: models CUTOUT first (TRANSLUCENT while rising / crumbling so they can fade), their glow maps ADDITIVE (GeoDraw); the ground shade and the frost
 * ring are translucent / additive flat quads lifted 2 to 3 cm.
 */
public final class SnowPropPainter {
    private SnowPropPainter() {}

    private static final GeoSpec GOLEM = GeoSpec.of("snow", "golem"), CRYSTAL = GeoSpec.of("snow", "crystal");
    private static ResourceLocation puff, ring;

    /** Called once on the client by PropPainterRegistry. */
    public static void register() {
        puff = AuraRender.tex("particle/snow_puff");
        ring = AuraRender.tex("particle/snow_ring");
        PropPainters.register(PropKind.SNOW_1, SnowPropPainter::paint);
    }

    private static float smooth(float t) { t = Mth.clamp(t, 0f, 1f); return t * t * (3f - 2f * t); }

    private static void paint(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        int p = e.param(), v = AirPropWord.variant(p);
        long now = e.level().getGameTime();
        float s = Math.max(0.3f, e.scale());
        float left = Math.max(1f, e.maxLife()) - age;
        if (v == 0) golem(e, partial, age, left, p, now, s, pose, buffers, light);
        else crystal(e, partial, age, left, p, now, s, pose, buffers, light);
    }

    private static void golem(MagicPropEntity e, float partial, float age, float left, int p, long now, float s, PoseStack pose, MultiBufferSource buffers, int light) {
        float rise = smooth(age / 12f), crumble = left < 24f ? 1f - Mth.clamp(left / 24f, 0f, 1f) : 0f;
        String clip;
        float secs;
        if (crumble > 0f) { clip = "crumble"; secs = crumble * 1.2f; }
        else if (AirPropWord.state(p) == AirPropWord.ACT && AirPropWord.since(p, now) < 18) { clip = "strike"; secs = (AirPropWord.since(p, now) + partial) / 20f; }
        else { clip = "idle"; secs = age / 20f; }
        VertexConsumer sh = buffers.getBuffer(AuraRender.translucent(puff));
        float r = 1.0f * s * (0.4f + 0.6f * rise);
        PropDraw.quad(pose, sh, -r, 0.02f, r, r, 0.02f, r, r, 0.02f, -r, -r, 0.02f, -r, 0f, 0f, 1f, 1f, (Mth.clamp((int) (0.35f * rise * 255f), 0, 255) << 24) | 0x20303C, light);
        pose.pushPose();
        pose.translate(0f, -(1f - rise) * 2.5f * s, 0f);
        pose.scale(s * 0.95f, s * 0.95f, s * 0.95f);
        float alpha = (1f - crumble * 0.7f);
        boolean solid = crumble <= 0f && rise >= 1f;
        GeoDraw.paint(pose, buffers, GOLEM, clip, secs, GeoDraw.Space.PROP, solid ? GeoDraw.Layer.CUTOUT : GeoDraw.Layer.TRANSLUCENT, light,
                solid ? 0xFFFFFFFF : AuraRender.alpha(0xFFFFFFFF, alpha), AuraRender.alpha(0xFFA6E6FF, (0.7f + 0.3f * Mth.sin(age * 0.12f)) * alpha));
        pose.popPose();
    }

    private static void crystal(MagicPropEntity e, float partial, float age, float left, int p, long now, float s, PoseStack pose, MultiBufferSource buffers, int light) {
        boolean grow = age < 12f;
        float fade = Mth.clamp(left / 10f, 0f, 1f);
        float pulse = 1f - Mth.clamp((e.life() % 20 + partial) / 20f, 0f, 1f);
        VertexConsumer rv = buffers.getBuffer(AuraRender.additive(ring));
        float rr = 4.5f * (1f - pulse) * smooth(age / 8f);
        if (rr > 0.2f) {
            pose.pushPose();
            pose.translate(0f, 0.04f, 0f);
            float c = rr, a = pulse * 0.6f * fade;
            PropDraw.quad(pose, rv, -c, 0f, c, c, 0f, c, c, 0f, -c, -c, 0f, -c, 0f, 0f, 1f, 1f, (Mth.clamp((int) (a * 255f), 0, 255) << 24) | 0xA8E0FF, PropDraw.FULL_BRIGHT);
            pose.popPose();
        }
        pose.pushPose();
        pose.scale(s * 0.7f, s * 0.7f, s * 0.7f);
        boolean solid = !grow && fade >= 1f;
        GeoDraw.paint(pose, buffers, CRYSTAL, grow ? "grow" : "idle", grow ? age / 20f : age / 20f, GeoDraw.Space.PROP, solid ? GeoDraw.Layer.CUTOUT : GeoDraw.Layer.TRANSLUCENT, light,
                solid ? 0xFFFFFFFF : AuraRender.alpha(0xFFFFFFFF, fade), AuraRender.alpha(0xFFBCEBFF, (0.7f + 0.3f * Mth.sin(age * 0.15f)) * fade));
        pose.popPose();
    }
}
