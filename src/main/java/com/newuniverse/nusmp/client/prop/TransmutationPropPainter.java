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
 * Transmutation Magic (Grey), 1.93.0 rebuild: how the constructs look (geo models with glow maps, tools/gen_transmutation_geo.py).
 * <ul>
 *   <li>TRANSMUTATION_1, an iron spike: transmutation_spike (a rune disc in the ground, a main spike of five stacked blocks and four leaning ones). The clip "rise" plays over the
 *       first 8 ticks (it overshoots and settles), "fall" over the last 8, "idle" between.</li>
 *   <li>TRANSMUTATION_2, the Transmuted Sentinel: transmutation_sentinel. It rises (clip "rise", 22 ticks), plays idle / walk by its speed, strike from the synced state and crumbles
 *       over its last 26 ticks.</li>
 *   <li>TRANSMUTATION_3, an alchemic blade: transmutation_blade, turned to point along its direction of travel (up while it circles), spinning faster while it lunges.</li>
 * </ul>
 * Render types: the model is CUTOUT (solid, alpha tested, drawn first), its glow map additive and full bright (drawn after it by GeoDraw); the circle discs of the models are cut out of
 * their textures (alpha 0 between the lines) so nothing is coplanar and nothing is translucent.
 */
public final class TransmutationPropPainter {
    private TransmutationPropPainter() {}

    private static final GeoSpec SPIKE = GeoSpec.of("transmutation", "spike"), SENTINEL = GeoSpec.of("transmutation", "sentinel"), BLADE = GeoSpec.of("transmutation", "blade");

    /** Called once on the client by PropPainterRegistry. */
    public static void register() {
        PropPainters.register(PropKind.TRANSMUTATION_1, TransmutationPropPainter::spike);
        PropPainters.register(PropKind.TRANSMUTATION_2, TransmutationPropPainter::sentinel);
        PropPainters.register(PropKind.TRANSMUTATION_3, TransmutationPropPainter::blade);
    }

    /** The synced state word of prop.TransmutationProps (copied, as the painters of the other magics do, so the painter compiles without the server class): bits 0-1 the state, bits 2-17 the game time (mod 65536) it began. */
    private static final int ACT = 1;
    private static int state(int param) { return param & 3; }
    private static int since(int param, long gameTime) { return (int) ((gameTime - ((param >> 2) & 0xFFFF)) & 0xFFFF); }

    private static float smooth(float t) { t = Mth.clamp(t, 0f, 1f); return t * t * (3f - 2f * t); }

    private static boolean moving(MagicPropEntity e) {
        double dx = e.getX() - e.xo, dz = e.getZ() - e.zo;
        return dx * dx + dz * dz > 1.0e-4;
    }

    private static void spike(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        float s = Math.max(0.3f, e.scale()), maxL = Math.max(1f, e.maxLife());
        String clip;
        float secs;
        if (age < 9f) { clip = "rise"; secs = age / 20f; }
        else if (age > maxL - 8f) { clip = "fall"; secs = (age - (maxL - 8f)) / 20f; }
        else { clip = "idle"; secs = age / 20f; }
        pose.pushPose();
        pose.scale(s, s, s);
        GeoDraw.paint(pose, buffers, SPIKE, clip, secs, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF, AuraRender.alpha(0xFFFFFFFF, 0.7f + 0.3f * Mth.sin(age * 0.2f)));
        pose.popPose();
    }

    private static void sentinel(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        float s = Math.max(0.3f, e.scale()) * 0.9f, maxL = Math.max(1f, e.maxLife());
        int p = e.param();
        long now = e.level().getGameTime();
        float crumbleT = age - (maxL - 26f);
        String clip;
        float secs;
        if (crumbleT > 0f) { clip = "crumble"; secs = crumbleT / 20f; }
        else if (age < 22f) { clip = "rise"; secs = age / 20f; }
        else if (state(p) == ACT && since(p, now) < 24) { clip = "strike"; secs = (since(p, now) + partial) / 20f; }
        else if (moving(e)) { clip = "walk"; secs = age / 20f; }
        else { clip = "idle"; secs = age / 20f; }
        float fade = crumbleT > 0f ? 1f - Mth.clamp(crumbleT / 24f, 0f, 1f) : 1f;
        pose.pushPose();
        pose.scale(s, s, s);
        GeoDraw.paint(pose, buffers, SENTINEL, clip, secs, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF, AuraRender.alpha(0xFFFFFFFF, (0.7f + 0.3f * Mth.sin(age * 0.12f)) * fade));
        pose.popPose();
    }

    private static void blade(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        float s = Math.max(0.3f, e.scale()) * 0.8f, maxL = Math.max(1f, e.maxLife());
        float k = smooth(age / 6f) * smooth((maxL - age) / 8f);
        if (k <= 0.01f) return;
        double vx = e.getX() - e.xo, vy = e.getY() - e.yo, vz = e.getZ() - e.zo;
        boolean lunge = state(e.param()) == ACT;
        pose.pushPose();
        pose.translate(0f, 0.35f * s, 0f);
        if (vx * vx + vy * vy + vz * vz > 0.01 * 0.01 * 4)
            pose.mulPose(new Quaternionf().rotationTo(0f, 1f, 0f, (float) vx, (float) vy, (float) vz));
        pose.translate(0f, -0.35f * s, 0f);
        pose.scale(s * k, s * k, s * k);
        GeoDraw.paint(pose, buffers, BLADE, lunge ? "spin" : "idle", age / 20f, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF, AuraRender.alpha(0xFFFFFFFF, k * (lunge ? 1f : 0.75f)));
        pose.popPose();
    }
}
