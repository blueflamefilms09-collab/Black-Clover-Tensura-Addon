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
 * 1.93.0 Vine Magic: how the props of PropKind.VINE_1 look (GeoLite models, tools/gen_vine_rebuild_geo.py; the server side is prop.VineProps). Variant = low byte of the param; phase (bits 8-9) and
 * the ticks since the action began (bits 10-15) come from the server:
 * <ul>
 *   <li>variant 0, the Vine Serpent (vine_serpent): clip rise for its first 20 ticks, then idle (slithering), strike from the start of a bite (the bite lands 6 ticks in); a thorn-ring sigil under it,
 *       additive; the model cutout with its glow map additive on top.</li>
 *   <li>variant 1, the Vine Whip (vine_whip): grows out of the ground, clip lash from tick 3 of its 14, cutout + glow.</li>
 *   <li>variant 2, the Thicket Wall (vine_wall): a solid wall: the model stretched 1.5 times along the wall, idle (sway), cutout + glow, a sigil strip on the ground under it.</li>
 * </ul>
 */
public final class VinePropPainter {
    private VinePropPainter() {}

    private static final GeoSpec SERPENT = GeoSpec.of("vine", "serpent"), WHIP = GeoSpec.of("vine", "whip"), WALL = GeoSpec.of("vine", "wall");
    private static ResourceLocation sigil;

    public static void register() {
        sigil = AuraRender.tex("particle/vine_sigil");
        PropPainters.register(PropKind.VINE_1, VinePropPainter::paint);
    }

    private static void paint(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, int light) {
        float k = TreePropPainter.fade(e, partial, 6f, 10f);
        if (k <= 0.01f) return;
        float s = e.scale(), life = e.life() + partial, pulse = 0.75f + 0.25f * Mth.sin(age * 0.2f);
        int glow = TreePropPainter.argb(k * pulse, 0xFFFFFF);
        switch (e.param() & 0xFF) {
            case 0 -> {
                TreePropPainter.sigil(pose, buf, sigil, 1.8f * s * k, age * 1.1f, 0x9CE84A, 0.5f * k);
                String clip = TreePropPainter.phase(e) == 1 ? "strike" : life < 20f ? "rise" : "idle";
                float clock = TreePropPainter.phase(e) == 1 ? TreePropPainter.since(e, partial) / 20f : life < 20f ? life / 20f : age / 20f;
                float sc = 0.5f * s * (0.6f + 0.4f * k);
                TreePropPainter.model(pose, buf, SERPENT, clip, clock, GeoDraw.Layer.CUTOUT, light, sc, sc, sc, 0f, 0xFFFFFFFF, glow);
            }
            case 1 -> {
                float rise = Mth.clamp(life / 4f, 0f, 1f), sc = 0.6f * s * k;
                TreePropPainter.model(pose, buf, WHIP, life < 3f ? "idle" : "lash", life < 3f ? age / 20f : (life - 3f) / 20f, GeoDraw.Layer.CUTOUT, light, sc, sc * (0.3f + 0.7f * rise), sc, 0f, 0xFFFFFFFF, glow);
            }
            default -> {
                TreePropPainter.sigil(pose, buf, sigil, 1.9f * s * k, age * 0.6f, 0x9CE84A, 0.35f * k);
                float rise = Mth.clamp(life / 8f, 0f, 1f), sc = 1.0f * s * k;
                TreePropPainter.model(pose, buf, WALL, "idle", age / 20f, GeoDraw.Layer.CUTOUT, light, sc * 1.5f, sc * 1.05f * (0.2f + 0.8f * rise), sc * 1.2f, 0f, 0xFFFFFFFF, glow);
            }
        }
    }
}
