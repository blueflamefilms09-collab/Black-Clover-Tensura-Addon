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
 * 1.93.0 Poison Plant Magic: how the props of PropKind.POISON_PLANT_1 look (GeoLite models, tools/gen_poison_plant_rebuild_geo.py; the server side is prop.PoisonPlantProps). Variant = low byte
 * of the param; phase (1 = breathing / snapping) and the ticks since the action began come from the server:
 * <ul>
 *   <li>variant 0, the Miasma Bloom (poison_plant_bloom): clip idle (the bulb swells), open while it breathes (the six petals fling wide); a toxic sigil on the ground, additive; cutout + glow;
 *       translucent gas puffs (particle/poison_plant_mist) drifting up from the bulb.</li>
 *   <li>variant 1, the Trap Maw (poison_plant_maw): clip idle (the jaws gape and shut slowly) or snap from the start of a snap (the jaws close 6 ticks in); the sigil under it; cutout + glow.</li>
 * </ul>
 */
public final class PoisonPlantPropPainter {
    private PoisonPlantPropPainter() {}

    private static final GeoSpec BLOOM = GeoSpec.of("poison_plant", "bloom"), MAW = GeoSpec.of("poison_plant", "maw");
    private static ResourceLocation sigil, mist;

    public static void register() {
        sigil = AuraRender.tex("particle/poison_plant_sigil");
        mist = AuraRender.tex("particle/poison_plant_mist");
        PropPainters.register(PropKind.POISON_PLANT_1, PoisonPlantPropPainter::paint);
    }

    private static void paint(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, int light) {
        float k = TreePropPainter.fade(e, partial, 6f, 10f);
        if (k <= 0.01f) return;
        float s = e.scale(), life = e.life() + partial, pulse = 0.7f + 0.3f * Mth.sin(age * 0.3f);
        int glowArgb = TreePropPainter.argb(k * pulse, 0xFFFFFF);
        if ((e.param() & 0xFF) == 0) {
            TreePropPainter.sigil(pose, buf, sigil, 4.5f * s * k, age * 0.9f, 0xB8FF30, 0.5f * k);
            boolean open = TreePropPainter.phase(e) == 1;
            float rise = Mth.clamp(life / 8f, 0f, 1f), sc = 1.1f * s * k;
            TreePropPainter.model(pose, buf, BLOOM, open ? "open" : "idle", open ? TreePropPainter.since(e, partial) / 20f : age / 20f, GeoDraw.Layer.CUTOUT, light, sc, sc * (0.3f + 0.7f * rise), sc, 0f,
                    0xFFFFFFFF, glowArgb);
            var vc = buf.getBuffer(AuraRender.translucent(mist));
            TreeDraw.camera(pose);
            for (int i = 0; i < 4; i++) {
                float ph = TreeDraw.frac(age * 0.012f + i * 0.25f), a = i * 2.1f + age * 0.02f;
                TreeDraw.glow(pose, vc, Mth.cos(a) * 0.8f * s, (0.8f + 1.6f * ph) * s, Mth.sin(a) * 0.8f * s, (0.7f + 0.8f * ph) * s, age * 0.01f + i,
                        TreeDraw.argb(Mth.sin(ph * Mth.PI) * 0.45f * k, 0xFFFFFF), TreeDraw.lift(light, 8));
            }
        } else {
            TreePropPainter.sigil(pose, buf, sigil, 2.0f * s * k, age * 0.6f, 0xB8FF30, 0.4f * k);
            boolean snap = TreePropPainter.phase(e) == 1;
            float rise = Mth.clamp(life / 6f, 0f, 1f), sc = 1.15f * s * k;
            TreePropPainter.model(pose, buf, MAW, snap ? "snap" : "idle", snap ? TreePropPainter.since(e, partial) / 20f : age / 20f, GeoDraw.Layer.CUTOUT, light, sc, sc * (0.3f + 0.7f * rise), sc, 0f,
                    0xFFFFFFFF, glowArgb);
        }
    }
}
