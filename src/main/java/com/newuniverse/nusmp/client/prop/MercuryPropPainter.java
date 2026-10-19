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
 * Mercury Magic (Nozel Silva), 1.93.0 rebuild: how the constructs look (geo models with glow maps, tools/gen_mercury_geo.py).
 * <ul>
 *   <li>MERCURY_1, a flying body: param 0 = mercury_blade (a floating double-edged sword with drops running down its fuller), 1 = mercury_spear (a hyper-dense spear with a liquid tail); both
 *       turned to point along their flight, spinning, shrinking in and out over 3 ticks.</li>
 *   <li>MERCURY_2, the Silver Eagle: mercury_eagle forms over the first 12 ticks (clip "form"), beats its wings for the flight ("flap") and glides while it dissolves, shrinking out over
 *       the last 6 ticks.</li>
 *   <li>MERCURY_3, the cage of the Silver Star of Execution: mercury_cage, eight tall bars with arrow heads, a ring on top and four shield plates; the clip "close" plays over the first 16
 *       ticks, "idle" after, "open" over the last 8.</li>
 * </ul>
 * Render types: the models are CUTOUT (solid, alpha tested, first), their glow maps additive and full bright (after, by GeoDraw).
 */
public final class MercuryPropPainter {
    private MercuryPropPainter() {}

    private static final GeoSpec BLADE = GeoSpec.of("mercury", "blade"), SPEAR = GeoSpec.of("mercury", "spear"), EAGLE = GeoSpec.of("mercury", "eagle"), CAGE = GeoSpec.of("mercury", "cage");

    /** Called once on the client by PropPainterRegistry. */
    public static void register() {
        PropPainters.register(PropKind.MERCURY_1, MercuryPropPainter::flyer);
        PropPainters.register(PropKind.MERCURY_2, MercuryPropPainter::eagle);
        PropPainters.register(PropKind.MERCURY_3, MercuryPropPainter::cage);
    }

    /** Copied from book.MercuryRules / prop.MercuryProps so the painter compiles without the server classes. */
    private static final int EAGLE_FLIGHT_TICKS = 26;
    private static final float EAGLE_MODEL = 0.55f;

    private static float smooth(float t) { t = Mth.clamp(t, 0f, 1f); return t * t * (3f - 2f * t); }

    private static void flyer(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        float maxL = Math.max(1f, e.maxLife());
        float k = smooth(age / 3f) * smooth((maxL - age) / 3f);
        if (k <= 0.01f) return;
        boolean spear = e.param() == 1;
        double vx = e.getX() - e.xo, vy = e.getY() - e.yo, vz = e.getZ() - e.zo;
        pose.pushPose();
        pose.translate(0f, 0.25f, 0f);
        if (vx * vx + vy * vy + vz * vz > 1.0e-4) pose.mulPose(new Quaternionf().rotationTo(0f, 1f, 0f, (float) vx, (float) vy, (float) vz));
        pose.translate(0f, spear ? -0.9f : -0.5f, 0f);
        float s = Math.max(0.3f, e.scale()) * (spear ? 1.0f : 0.9f) * k;
        pose.scale(s, s, s);
        GeoDraw.paint(pose, buffers, spear ? SPEAR : BLADE, spear ? "idle" : "spin", age / 20f, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF, AuraRender.alpha(0xFFFFFFFF, k));
        pose.popPose();
    }

    private static void eagle(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        float maxL = Math.max(1f, e.maxLife());
        float k = smooth((maxL - age) / 6f);
        if (k <= 0.01f) return;
        String clip = age < 12f ? "form" : age <= EAGLE_FLIGHT_TICKS + 4 ? "flap" : "glide";
        float secs = age < 12f ? age / 20f : age / 20f;
        float s = Math.max(0.3f, e.scale()) * EAGLE_MODEL * k;
        pose.pushPose();
        pose.scale(s, s, s);
        GeoDraw.paint(pose, buffers, EAGLE, clip, secs, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF, AuraRender.alpha(0xFFFFFFFF, 0.8f + 0.2f * Mth.sin(age * 0.3f)));
        pose.popPose();
    }

    private static void cage(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        float s = Math.max(0.3f, e.scale()), maxL = Math.max(1f, e.maxLife());
        String clip;
        float secs;
        if (age > maxL - 8f) { clip = "open"; secs = (age - (maxL - 8f)) / 20f; }
        else if (age < 16f) { clip = "close"; secs = age / 20f; }
        else { clip = "idle"; secs = age / 20f; }
        pose.pushPose();
        pose.scale(s, s, s);
        GeoDraw.paint(pose, buffers, CAGE, clip, secs, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF, AuraRender.alpha(0xFFFFFFFF, 0.65f + 0.35f * Mth.sin(age * 0.15f)));
        pose.popPose();
    }
}
