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
import org.joml.Quaternionf;

/**
 * Recombination Magic (Henry Legolant), 1.93.0 rebuild: how the magic house's constructs look (geo models with glow maps, tools/gen_recombination_geo.py). The old painter drew a box of planks.
 * <ul>
 *   <li>RECOMBINATION_1, a recombined room: recombination_room (stone foundation, oak boards, a tile roof, a door, lit windows, a chimney). The clip "rise" plays over the first 10 ticks,
 *       "swap" instead when the prop is a Room Swap marker (param 1), "idle" after; it sinks into the ground over the last 8 ticks.</li>
 *   <li>RECOMBINATION_2, a ring of boards: the six planks of recombination_halo circling at knee height, turning, fading in and out.</li>
 *   <li>RECOMBINATION_3, THE RAGING BLACK BULL: recombination_bull. It assembles (clip "assemble", 26 ticks), idles, walks by its speed, charges (ram state) and recoils its cannons
 *       (fire state), and collapses over the last 24 ticks. The Ultra Giant Bull (bit 18 of param) grows to 1.6 times over 16 ticks and glows brighter.</li>
 *   <li>RECOMBINATION_4, the drill of Mana Corkscrew: recombination_drill turned to point along its flight, spinning.</li>
 * </ul>
 * Render types: models are CUTOUT (solid, alpha tested, first), glow maps additive and full bright (after, by GeoDraw); the soft ground shadow is a translucent quad 2 cm over the ground.
 */
public final class RecombinationPropPainter {
    private RecombinationPropPainter() {}

    private static final GeoSpec ROOM = GeoSpec.of("recombination", "room"), HALO = GeoSpec.of("recombination", "halo"), BULL = GeoSpec.of("recombination", "bull"), DRILL = GeoSpec.of("recombination", "drill");
    private static ResourceLocation shadow;

    public static void register() {
        shadow = AuraRender.tex("particle/glow");
        PropPainters.register(PropKind.RECOMBINATION_1, RecombinationPropPainter::room);
        PropPainters.register(PropKind.RECOMBINATION_2, RecombinationPropPainter::boards);
        PropPainters.register(PropKind.RECOMBINATION_3, RecombinationPropPainter::bull);
        PropPainters.register(PropKind.RECOMBINATION_4, RecombinationPropPainter::drill);
    }

    /** The synced state word of prop.RecombinationProps (copied so the painter compiles without the server class): bits 0-1 the state, bits 2-17 the start time (mod 65536), bit 18 the Ultra form, bits 19-29 its start time (mod 2048). */
    private static final int RAM = 1, FIRE = 2, ULTRA_BIT = 1 << 18;
    private static int state(int param) { return param & 3; }
    private static int since(int param, long gameTime) { return (int) ((gameTime - ((param >> 2) & 0xFFFF)) & 0xFFFF); }
    private static boolean ultra(int param) { return (param & ULTRA_BIT) != 0; }
    private static int ultraSince(int param, long gameTime) { return ultra(param) ? (int) ((gameTime - ((param >> 19) & 0x7FF)) & 0x7FF) : 0; }

    private static float smooth(float t) { t = Mth.clamp(t, 0f, 1f); return t * t * (3f - 2f * t); }

    private static boolean moving(MagicPropEntity e) {
        double dx = e.getX() - e.xo, dz = e.getZ() - e.zo;
        return dx * dx + dz * dz > 1.0e-4;
    }

    private static void shade(PoseStack pose, MultiBufferSource buffers, float r, float alpha, int light) {
        VertexConsumer vc = buffers.getBuffer(AuraRender.translucent(shadow));
        int a = Mth.clamp((int) (alpha * 255f), 0, 255) << 24;
        PropDraw.quad(pose, vc, -r, 0.02f, r, r, 0.02f, r, r, 0.02f, -r, -r, 0.02f, -r, 0f, 0f, 1f, 1f, a | 0x101008, light);
    }

    private static void room(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        float s = Math.max(0.3f, e.scale()), maxL = Math.max(1f, e.maxLife());
        boolean swap = e.param() == 1;
        String clip;
        float secs;
        if (age < 10f) { clip = swap ? "swap" : "rise"; secs = age / 20f; }
        else { clip = "idle"; secs = age / 20f; }
        float sink = smooth((maxL - age) / 8f);
        if (sink <= 0.01f) return;
        shade(pose, buffers, 0.9f * s * sink, 0.3f * sink, light);
        pose.pushPose();
        pose.translate(0f, -(1f - sink) * 1.2f * s, 0f);
        pose.scale(s, s, s);
        GeoDraw.paint(pose, buffers, ROOM, clip, secs, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF, AuraRender.alpha(0xFFFFFFFF, (0.75f + 0.25f * Mth.sin(age * 0.15f)) * sink));
        pose.popPose();
    }

    private static void boards(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        float s = Math.max(0.4f, e.scale()), maxL = Math.max(1f, e.maxLife());
        float k = smooth(age / 6f) * smooth((maxL - age) / 8f);
        if (k <= 0.01f) return;
        pose.pushPose();
        pose.translate(0f, -1.5f * s + 0.2f * Mth.sin(age * 0.2f), 0f);
        pose.scale(s * (0.7f + 0.3f * k), s * k, s * (0.7f + 0.3f * k));
        GeoDraw.paint(pose, buffers, HALO, "idle", age / 20f * 1.8f, GeoDraw.Space.PROP, GeoDraw.Layer.TRANSLUCENT, light, AuraRender.alpha(0xFFFFFFFF, k), AuraRender.alpha(0xFFFFFFFF, k * 0.8f));
        pose.popPose();
    }

    private static void bull(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        float maxL = Math.max(1f, e.maxLife());
        int p = e.param();
        long now = e.level().getGameTime();
        boolean big = ultra(p);
        float grow = big ? smooth((ultraSince(p, now) + partial) / 16f) : 0f;
        float s = Math.max(0.3f, e.scale()) * 0.95f * (1f + 0.6f * grow);
        float collapseT = age - (maxL - 24f);
        String clip;
        float secs;
        if (collapseT > 0f) { clip = "collapse"; secs = collapseT / 20f; }
        else if (age < 26f) { clip = "assemble"; secs = age / 20f; }
        else if (state(p) == RAM && since(p, now) < 28) { clip = "charge"; secs = (since(p, now) + partial) / 20f; }
        else if (state(p) == FIRE && since(p, now) < 12) { clip = "fire"; secs = (since(p, now) + partial) / 20f; }
        else if (moving(e)) { clip = "walk"; secs = age / 20f; }
        else { clip = "idle"; secs = age / 20f; }
        float fade = collapseT > 0f ? 1f - Mth.clamp(collapseT / 22f, 0f, 1f) : 1f;
        shade(pose, buffers, 1.7f * s * fade, 0.4f * fade, light);
        pose.pushPose();
        pose.scale(s, s, s);
        float pulse = (0.6f + 0.4f * Mth.sin(age * 0.12f)) * (1f + 0.5f * grow);
        GeoDraw.paint(pose, buffers, BULL, clip, secs, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF, AuraRender.alpha(0xFFFFFFFF, Math.min(1f, pulse) * fade));
        pose.popPose();
    }

    private static void drill(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        float maxL = Math.max(1f, e.maxLife());
        float k = smooth(age / 3f) * smooth((maxL - age) / 3f);
        if (k <= 0.01f) return;
        double vx = e.getX() - e.xo, vy = e.getY() - e.yo, vz = e.getZ() - e.zo;
        pose.pushPose();
        pose.translate(0f, 0.3f, 0f);
        if (vx * vx + vy * vy + vz * vz > 1.0e-4) pose.mulPose(new Quaternionf().rotationTo(0f, 1f, 0f, (float) vx, (float) vy, (float) vz));
        pose.translate(0f, -0.4f, 0f);
        float s = 0.9f * k;
        pose.scale(s, s, s);
        GeoDraw.paint(pose, buffers, DRILL, "idle", age / 20f, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF, AuraRender.alpha(0xFFFFFFFF, k));
        pose.popPose();
    }
}
