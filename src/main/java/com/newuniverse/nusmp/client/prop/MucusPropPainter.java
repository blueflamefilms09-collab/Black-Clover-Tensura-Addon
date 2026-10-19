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
 * 1.93.0 Mucus Magic: how the props of PropKind.MUCUS_1 look (GeoLite models, tools/gen_mucus_rebuild_geo.py; the server side is prop.MucusProps). Everything is gel: the models are drawn on
 * the alpha-blended layer (entityTranslucent), the glow map additive on top. Variant = low byte of the param; phase (1 = absorbing) comes from the server:
 * <ul>
 *   <li>variant 0, the Mucus Construct (mucus_construct): clip rise for its first 20 ticks, then idle (it wobbles), absorb while something is inside it; a membrane sigil under it, additive; bubbles
 *       (particle/mucus_bubble) rise round it, translucent.</li>
 *   <li>variant 1, the Mucus Tendril (mucus_tendril): clip strike from tick 2 of its 20; a gel ripple on the ground; model translucent + glow.</li>
 * </ul>
 */
public final class MucusPropPainter {
    private MucusPropPainter() {}

    private static final GeoSpec CONSTRUCT = GeoSpec.of("mucus", "construct"), TENDRIL = GeoSpec.of("mucus", "tendril");
    private static ResourceLocation sigil, ring, bubble;

    public static void register() {
        sigil = AuraRender.tex("particle/mucus_sigil");
        ring = AuraRender.tex("particle/mucus_ring");
        bubble = AuraRender.tex("particle/mucus_bubble");
        PropPainters.register(PropKind.MUCUS_1, MucusPropPainter::paint);
    }

    private static void paint(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, int light) {
        float k = TreePropPainter.fade(e, partial, 6f, 10f);
        if (k <= 0.01f) return;
        float s = e.scale(), life = e.life() + partial, pulse = 0.75f + 0.25f * Mth.sin(age * 0.2f);
        int glow = TreePropPainter.argb(k * pulse * 0.8f, 0xFFFFFF), body = TreePropPainter.argb(0.9f * k, 0xFFFFFF);
        if ((e.param() & 0xFF) == 0) {
            TreePropPainter.sigil(pose, buf, sigil, 2.3f * s * k, age * 0.4f, 0x72D6B4, 0.55f * k);
            boolean absorb = TreePropPainter.phase(e) == 1;
            String clip = absorb ? "absorb" : life < 20f ? "rise" : "idle";
            float clock = absorb ? (age % 20f) / 20f : life < 20f ? life / 20f : age / 20f;
            float sc = 1.1f * s * (0.6f + 0.4f * k);
            TreePropPainter.model(pose, buf, CONSTRUCT, clip, clock, GeoDraw.Layer.TRANSLUCENT, TreeDraw.lift(light, 6), sc, sc, sc, 0f, body, glow);
            var vc = buf.getBuffer(AuraRender.translucent(bubble));
            TreeDraw.camera(pose);
            for (int i = 0; i < 5; i++) {
                float ph = TreeDraw.frac(age * 0.014f + i * 0.2f), a = i * 2.4f + age * 0.03f;
                TreeDraw.glow(pose, vc, Mth.cos(a) * 1.1f * s, (0.3f + 1.8f * ph) * s, Mth.sin(a) * 1.1f * s, 0.16f * s * (0.7f + 0.6f * ph), 0f, TreeDraw.argb(Mth.sin(ph * Mth.PI) * 0.85f * k, 0xFFFFFF),
                        TreeDraw.lift(light, 10));
            }
        } else {
            TreePropPainter.sigil(pose, buf, ring, 1.4f * s * k, 0f, 0x72D6B4, 0.55f * k);
            float rise = Mth.clamp(life / 5f, 0f, 1f), sc = 1.2f * s * k;
            TreePropPainter.model(pose, buf, TENDRIL, life < 2f ? "idle" : "strike", life < 2f ? age / 20f : (life - 2f) / 20f, GeoDraw.Layer.TRANSLUCENT, TreeDraw.lift(light, 6), sc,
                    sc * (0.3f + 0.7f * rise), sc, 0f, body, glow);
        }
    }
}
