package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

/**
 * Modification Magic (Morris Libardirt), 1.93.0 rebuild: how the constructs look (geo models with glow maps, tools/gen_modification_geo.py).
 * <ul>
 *   <li>MODIFICATION_1, a clawed tendril: modification_tendril (an orange-cracked steel mound, eight tapering segments, a claw of three hooks). The clip "emerge" plays over the first
 *       10 ticks, "strike" when the synced state says so, "idle" (a sway) otherwise, "wither" over the last 9 ticks.</li>
 *   <li>MODIFICATION_2, a test subject: modification_subject (a stitched, edited human with a bone-blade forearm and a clawed hand, exposed ribs, orange eyes). It rises (clip "rise"),
 *       walks by its speed, strikes from the synced state and collapses over its last 20 ticks.</li>
 *   <li>MODIFICATION_3, a dissection needle: modification_needle turned to point along its flight, spinning.</li>
 * </ul>
 * Render types: the models are CUTOUT (solid, alpha tested, first), their glow maps additive and full bright (after, by GeoDraw).
 */
public final class ModificationPropPainter {
    private ModificationPropPainter() {}

    private static final GeoSpec TENDRIL = GeoSpec.of("modification", "tendril"), SUBJECT = GeoSpec.of("modification", "subject"), NEEDLE = GeoSpec.of("modification", "needle");

    public static void register() {
        PropPainters.register(PropKind.MODIFICATION_1, ModificationPropPainter::tendril);
        PropPainters.register(PropKind.MODIFICATION_2, ModificationPropPainter::subject);
        PropPainters.register(PropKind.MODIFICATION_3, ModificationPropPainter::needle);
    }

    /** The synced state word of prop.ModificationProps (copied so the painter compiles without the server class): bits 0-1 the state (1 = strike), bits 2-17 the start time (mod 65536). */
    private static final int ACT = 1;
    private static int state(int param) { return param & 3; }
    private static int since(int param, long gameTime) { return (int) ((gameTime - ((param >> 2) & 0xFFFF)) & 0xFFFF); }

    private static float smooth(float t) { t = Mth.clamp(t, 0f, 1f); return t * t * (3f - 2f * t); }

    private static boolean moving(MagicPropEntity e) {
        double dx = e.getX() - e.xo, dz = e.getZ() - e.zo;
        return dx * dx + dz * dz > 1.0e-4;
    }

    private static void tendril(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        float s = Math.max(0.3f, e.scale()) * 0.8f, maxL = Math.max(1f, e.maxLife());
        int p = e.param();
        long now = e.level().getGameTime();
        String clip;
        float secs;
        if (age > maxL - 9f) { clip = "wither"; secs = (age - (maxL - 9f)) / 20f; }
        else if (age < 10f) { clip = "emerge"; secs = age / 20f; }
        else if (state(p) == ACT && since(p, now) < 22) { clip = "strike"; secs = (since(p, now) + partial) / 20f; }
        else { clip = "idle"; secs = age / 20f + e.seed() % 13; }
        pose.pushPose();
        pose.scale(s, s, s);
        GeoDraw.paint(pose, buffers, TENDRIL, clip, secs, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF, AuraRender.alpha(0xFFFFFFFF, 0.7f + 0.3f * Mth.sin(age * 0.2f)));
        pose.popPose();
    }

    private static void subject(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        float s = Math.max(0.3f, e.scale()) * 0.95f, maxL = Math.max(1f, e.maxLife());
        int p = e.param();
        long now = e.level().getGameTime();
        float crumbleT = age - (maxL - 20f);
        String clip;
        float secs;
        if (crumbleT > 0f) { clip = "crumble"; secs = crumbleT / 20f; }
        else if (age < 19f) { clip = "rise"; secs = age / 20f; }
        else if (state(p) == ACT && since(p, now) < 18) { clip = "strike"; secs = (since(p, now) + partial) / 20f; }
        else if (moving(e)) { clip = "walk"; secs = age / 20f; }
        else { clip = "idle"; secs = age / 20f; }
        float fade = crumbleT > 0f ? 1f - Mth.clamp(crumbleT / 18f, 0f, 1f) : 1f;
        pose.pushPose();
        pose.scale(s, s, s);
        GeoDraw.paint(pose, buffers, SUBJECT, clip, secs, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF, AuraRender.alpha(0xFFFFFFFF, (0.7f + 0.3f * Mth.sin(age * 0.15f)) * fade));
        pose.popPose();
    }

    private static void needle(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        float maxL = Math.max(1f, e.maxLife());
        float k = smooth(age / 2f) * smooth((maxL - age) / 3f);
        if (k <= 0.01f) return;
        double vx = e.getX() - e.xo, vy = e.getY() - e.yo, vz = e.getZ() - e.zo;
        pose.pushPose();
        pose.translate(0f, 0.2f, 0f);
        if (vx * vx + vy * vy + vz * vz > 1.0e-4) pose.mulPose(new Quaternionf().rotationTo(0f, 1f, 0f, (float) vx, (float) vy, (float) vz));
        pose.translate(0f, -0.5f, 0f);
        float s = Math.max(0.3f, e.scale()) * 0.9f * k;
        pose.scale(s, s, s);
        GeoDraw.paint(pose, buffers, NEEDLE, "spin", age / 20f, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF, AuraRender.alpha(0xFFFFFFFF, k));
        pose.popPose();
    }
}
