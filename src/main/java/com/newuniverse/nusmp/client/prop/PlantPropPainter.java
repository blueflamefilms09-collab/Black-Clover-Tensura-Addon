package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 1.93.0 Plant Magic: how the props of PropKind.PLANT_1 look (GeoLite models, tools/gen_plant_rebuild_geo.py; the server side is prop.PlantProps). Variant = low byte of the param; phase (1 strike,
 * 2 heal) and the ticks since the action began come from the server:
 * <ul>
 *   <li>variant 0, the Flower Sprite (plant_familiar): the entity position is its centre (the model is lowered by 0.55 blocks); clip idle (hover, wings beat), heal (the petals open, the head
 *       swells) or strike (a spin), turned to face along its orbit; cutout + glow map; a golden glow billboard behind the head, additive.</li>
 *   <li>variant 1, the Healing Bloom (plant_bloom): clip open for its first 14 ticks, then idle (sway, the bloom breathes); a flower sigil on the ground, additive; cutout + glow; during a
 *       heal pulse (phase 2) the golden glow behind the bloom flares.</li>
 * </ul>
 */
public final class PlantPropPainter {
    private PlantPropPainter() {}

    private static final GeoSpec FAMILIAR = GeoSpec.of("plant", "familiar"), BLOOM = GeoSpec.of("plant", "bloom");
    private static ResourceLocation sigil, heart;

    public static void register() {
        sigil = AuraRender.tex("particle/plant_sigil");
        heart = AuraRender.tex("particle/plant_heart");
        PropPainters.register(PropKind.PLANT_1, PlantPropPainter::paint);
    }

    private static void glow(PoseStack pose, MultiBufferSource buf, ResourceLocation tex, float y, float size, float rot, int rgb, float alpha) {
        var vc = buf.getBuffer(AuraRender.additive(tex));
        TreeDraw.camera(pose);
        TreeDraw.glow(pose, vc, 0f, y, 0f, size, rot, TreeDraw.argb(alpha, rgb), TreeDraw.FB);
    }

    private static void paint(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, int light) {
        float k = TreePropPainter.fade(e, partial, 6f, 10f);
        if (k <= 0.01f) return;
        float s = e.scale(), life = e.life() + partial, pulse = 0.75f + 0.25f * Mth.sin(age * 0.25f);
        int glowArgb = TreePropPainter.argb(k * pulse, 0xFFFFFF);
        if ((e.param() & 0xFF) == 0) {
            int ph = TreePropPainter.phase(e);
            String clip = ph == 2 ? "heal" : ph == 1 ? "strike" : "idle";
            float clock = ph == 0 ? age / 20f : TreePropPainter.since(e, partial) / 20f;
            float flare = ph == 2 ? 1f - Math.abs(TreePropPainter.since(e, partial) - 6f) / 8f : 0f;
            glow(pose, buf, heart, 0.1f * s, (0.5f + 0.5f * Math.max(0f, flare)) * s, age * 0.05f, 0xFFE070, 0.5f * k * (0.5f + Math.max(0f, flare)));
            float sc = 0.85f * s * (0.6f + 0.4f * k);
            TreePropPainter.model(pose, buf, FAMILIAR, clip, clock, GeoDraw.Layer.CUTOUT, light, sc, sc, sc, -0.55f * s, 0xFFFFFFFF, glowArgb);
        } else {
            TreePropPainter.sigil(pose, buf, sigil, 2.4f * s * k, age * 0.4f, 0xFFB6D8, 0.45f * k);
            boolean pulse2 = TreePropPainter.phase(e) == 2;
            glow(pose, buf, heart, 1.7f * s, (1.2f + (pulse2 ? 0.8f : 0f)) * s, age * 0.03f, 0xFFE070, 0.4f * k * (pulse2 ? 1.4f : 0.7f));
            float sc = 1.0f * s * k;
            TreePropPainter.model(pose, buf, BLOOM, life < 14f ? "open" : "idle", life < 14f ? life / 20f : age / 20f, GeoDraw.Layer.CUTOUT, light, sc, sc, sc, 0f, 0xFFFFFFFF, glowArgb);
        }
    }
}
