package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 0.53 Bronze Magic: how this magic's props look.
 * <ul>
 *   <li>BRONZE_1, the guardian statue: the geo model bronze_statue (cast bronze, verdigris on the edges, a warm glowing medallion, shield and
 *       short sword). It rises out of the ground over 12 ticks, plays idle / walk by its speed, strike and block from the synced state (the clip
 *       starts at the tick the state began) and crumbles over the last 26 ticks of its life.</li>
 *   <li>BRONZE_2, a poison lizard: the geo model bronze_lizard with idle / run / bite / spit clips, popping in and shrinking away at the end.</li>
 * </ul>
 * Render types: the model is CUTOUT (solid, alpha tested, drawn first), its glow map is additive and full bright (drawn after it by GeoDraw),
 * and the soft ground shadow is a translucent quad lifted 2 cm over the ground so nothing is coplanar.
 */
public final class BronzePropPainter {
    private BronzePropPainter() {}

    private static final GeoSpec STATUE = GeoSpec.of("bronze", "statue");
    private static final GeoSpec LIZARD = GeoSpec.of("bronze", "lizard");
    private static ResourceLocation shadow;

    /** Called once on the client by PropPainterRegistry. */
    public static void register() {
        shadow = AuraRender.tex("particle/glow");
        PropPainters.register(PropKind.BRONZE_1, BronzePropPainter::statue);
        PropPainters.register(PropKind.BRONZE_2, BronzePropPainter::lizard);
    }

    /** The synced state word of prop.BronzeProps: bits 0-1 the state, bits 2-17 the game time (mod 65536) at which it began. */
    private static final int ACT = 1, ACT2 = 2;
    private static int state(int param) { return param & 3; }
    private static int since(int param, long gameTime) { return (int) ((gameTime - ((param >> 2) & 0xFFFF)) & 0xFFFF); }

    private static float smooth(float t) { t = Mth.clamp(t, 0f, 1f); return t * t * (3f - 2f * t); }

    /** A soft dark disc on the ground. */
    private static void shade(PoseStack pose, MultiBufferSource buffers, float r, float alpha, int light) {
        VertexConsumer vc = buffers.getBuffer(AuraRender.translucent(shadow));
        int a = Mth.clamp((int) (alpha * 255f), 0, 255) << 24;
        PropDraw.quad(pose, vc, -r, 0.02f, r, r, 0.02f, r, r, 0.02f, -r, -r, 0.02f, -r, 0f, 0f, 1f, 1f, a | 0x101008, light);
    }

    private static boolean moving(MagicPropEntity e) {
        double dx = e.getX() - e.xo, dz = e.getZ() - e.zo;
        return dx * dx + dz * dz > 1.0e-4;
    }

    // ================================================================================================ BRONZE_1
    private static void statue(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        float s = Math.max(0.3f, e.scale()) * 0.82f;
        float maxL = Math.max(1f, e.maxLife());
        int p = e.param();
        long now = e.level().getGameTime();
        float rise = smooth(age / 12f);
        float crumbleT = age - (maxL - 26f);
        String clip;
        float secs;
        if (crumbleT > 0f) { clip = "crumble"; secs = crumbleT / 20f; }
        else if (state(p) == ACT && since(p, now) < 24) { clip = "strike"; secs = (since(p, now) + partial) / 20f; }
        else if (state(p) == ACT2 && since(p, now) < 18) { clip = "block"; secs = (since(p, now) + partial) / 20f; }
        else if (moving(e)) { clip = "walk"; secs = age / 20f; }
        else { clip = "idle"; secs = age / 20f; }
        shade(pose, buffers, 0.75f * s * (0.4f + 0.6f * rise), 0.35f * rise * (crumbleT > 0f ? 1f - Mth.clamp(crumbleT / 24f, 0f, 1f) : 1f), light);
        pose.pushPose();
        pose.translate(0f, -(1f - rise) * 2.3f * s, 0f);
        pose.scale(s, s, s);
        float pulse = 0.65f + 0.35f * Mth.sin(age * 0.12f);
        int glow = AuraRender.alpha(0xFFFFC070, pulse * (crumbleT > 0f ? 1f - Mth.clamp(crumbleT / 24f, 0f, 1f) : 1f));
        GeoDraw.paint(pose, buffers, STATUE, clip, secs, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF, glow);
        pose.popPose();
    }

    // ================================================================================================ BRONZE_2
    private static void lizard(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        float maxL = Math.max(1f, e.maxLife());
        float s = Math.max(0.3f, e.scale()) * 0.62f * smooth(age / 6f) * smooth((maxL - age) / 8f);
        if (s <= 0.01f) return;
        int p = e.param();
        long now = e.level().getGameTime();
        String clip;
        float secs;
        if (state(p) == ACT && since(p, now) < 14) { clip = "bite"; secs = (since(p, now) + partial) / 20f; }
        else if (state(p) == ACT2 && since(p, now) < 20) { clip = "spit"; secs = (since(p, now) + partial) / 20f; }
        else if (moving(e)) { clip = "run"; secs = age / 20f; }
        else { clip = "idle"; secs = age / 20f; }
        shade(pose, buffers, 0.9f * s, 0.3f, light);
        pose.pushPose();
        pose.scale(s, s, s);
        int glow = AuraRender.alpha(0xFFFFD080, 0.6f + 0.4f * Mth.sin(age * 0.15f + e.seed()));
        GeoDraw.paint(pose, buffers, LIZARD, clip, secs, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF, glow);
        pose.popPose();
    }
}
