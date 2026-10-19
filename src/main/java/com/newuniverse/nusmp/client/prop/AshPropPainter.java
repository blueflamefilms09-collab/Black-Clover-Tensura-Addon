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
 * 1.93.0 Ash Magic: how its summons look (ASH_1, variant in the low byte of param, see prop.AirPropWord).
 * <ul>
 *   <li>0, the ash soldier: ash_soldier (a figure of charcoal and ash, antlers of dead branches, ember eyes, a cracked glowing core). It rises out of the
 *       ground over 10 ticks, plays idle / strike from the synced state and crumbles over the last 20 ticks of its life.</li>
 *   <li>1 and 2, the ash trap: ash_trap (a mound of ash with splintered branches that spring up). Armed (1) it lies in idle with only the mound and a faint
 *       ember sigil on the ground (ADDITIVE, pulsing); sprung (state 1 of variant 1, or variant 2 from the start) it plays the spring clip from the tick the
 *       state began and the branches stay up until it falls apart.</li>
 * </ul>
 * Render types: models CUTOUT (TRANSLUCENT while rising / crumbling / fading), glow maps ADDITIVE (GeoDraw), the sigil ADDITIVE full bright, the ground shade
 * translucent.
 */
public final class AshPropPainter {
    private AshPropPainter() {}

    private static final GeoSpec SOLDIER = GeoSpec.of("ash", "soldier"), TRAP = GeoSpec.of("ash", "trap");
    private static ResourceLocation cloud, sigil;

    /** Called once on the client by PropPainterRegistry. */
    public static void register() {
        cloud = AuraRender.tex("particle/ash_cloud");
        sigil = AuraRender.tex("particle/ash_sigil");
        PropPainters.register(PropKind.ASH_1, AshPropPainter::paint);
    }

    private static float smooth(float t) { t = Mth.clamp(t, 0f, 1f); return t * t * (3f - 2f * t); }

    private static void paint(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        int p = e.param(), v = AirPropWord.variant(p);
        long now = e.level().getGameTime();
        float s = Math.max(0.3f, e.scale());
        float left = Math.max(1f, e.maxLife()) - age;
        if (v == 0) soldier(e, partial, age, left, p, now, s, pose, buffers, light); else trap(e, partial, age, left, p, now, s, v, pose, buffers, light);
    }

    private static void soldier(MagicPropEntity e, float partial, float age, float left, int p, long now, float s, PoseStack pose, MultiBufferSource buffers, int light) {
        float rise = smooth(age / 10f), crumble = left < 20f ? 1f - Mth.clamp(left / 20f, 0f, 1f) : 0f;
        String clip;
        float secs;
        if (crumble > 0f) { clip = "crumble"; secs = crumble; }
        else if (AirPropWord.state(p) == AirPropWord.ACT && AirPropWord.since(p, now) < 16) { clip = "strike"; secs = (AirPropWord.since(p, now) + partial) / 20f; }
        else { clip = "idle"; secs = age / 20f; }
        VertexConsumer sh = buffers.getBuffer(AuraRender.translucent(cloud));
        float r = 0.85f * s * (0.4f + 0.6f * rise);
        PropDraw.quad(pose, sh, -r, 0.02f, r, r, 0.02f, r, r, 0.02f, -r, -r, 0.02f, -r, 0f, 0f, 1f, 1f, (Mth.clamp((int) (0.35f * rise * 255f), 0, 255) << 24) | 0x1A1A1E, light);
        pose.pushPose();
        pose.translate(0f, -(1f - rise) * 1.6f * s, 0f);
        float sc = s * 0.88f;
        pose.scale(sc, sc, sc);
        float alpha = 1f - 0.6f * crumble;
        boolean solid = crumble <= 0f && rise >= 1f;
        GeoDraw.paint(pose, buffers, SOLDIER, clip, secs, GeoDraw.Space.PROP, solid ? GeoDraw.Layer.CUTOUT : GeoDraw.Layer.TRANSLUCENT, light,
                solid ? 0xFFFFFFFF : AuraRender.alpha(0xFFFFFFFF, alpha), AuraRender.alpha(0xFFFF9A50, (0.7f + 0.3f * Mth.sin(age * 0.18f)) * alpha));
        pose.popPose();
    }

    private static void trap(MagicPropEntity e, float partial, float age, float left, int p, long now, float s, int v, PoseStack pose, MultiBufferSource buffers, int light) {
        boolean sprung = v == 2 || AirPropWord.state(p) == AirPropWord.ACT;
        float since = (AirPropWord.since(p, now) + partial) / 20f;
        float fade = Mth.clamp(left / 8f, 0f, 1f) * Mth.clamp(age / 6f, 0f, 1f);
        VertexConsumer sg = buffers.getBuffer(AuraRender.additive(sigil));
        float pulse = 0.5f + 0.5f * Mth.sin(age * 0.15f), r = 1.5f * s;
        pose.pushPose();
        pose.translate(0f, 0.04f, 0f);
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(age * 1.5f));
        PropDraw.quad(pose, sg, -r, 0f, r, r, 0f, r, r, 0f, -r, -r, 0f, -r, 0f, 0f, 1f, 1f, (Mth.clamp((int) ((sprung ? 0.9f : 0.25f + 0.3f * pulse) * fade * 255f), 0, 255) << 24) | 0xFF7A38, PropDraw.FULL_BRIGHT);
        pose.popPose();
        pose.pushPose();
        float sc = s * 0.62f;
        pose.scale(sc, sc, sc);
        boolean solid = fade >= 0.99f;
        GeoDraw.paint(pose, buffers, TRAP, sprung ? "spring" : "idle", sprung ? Math.min(0.5f, since) : age / 20f, GeoDraw.Space.PROP, solid ? GeoDraw.Layer.CUTOUT : GeoDraw.Layer.TRANSLUCENT, light,
                solid ? 0xFFFFFFFF : AuraRender.alpha(0xFFFFFFFF, fade), AuraRender.alpha(0xFFFF9A50, (sprung ? 1f : 0.5f) * fade));
        pose.popPose();
    }
}
