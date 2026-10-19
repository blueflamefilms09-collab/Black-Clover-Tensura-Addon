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
 * 1.93.0 Mud Magic: how the props of PropKind.MUD_1 look (GeoLite models, tools/gen_mud_rebuild_geo.py; the server side is prop.MudProps). Variant = low byte of the param; phase (1 = slam /
 * erupting) and the ticks since the action began come from the server:
 * <ul>
 *   <li>variant 0, the Mud Golem (mud_golem): clip rise for its first 22 ticks, then idle, slam from the start of a slam (the fists land 16 ticks in); a cracked-earth sigil under it, additive; the
 *       model cutout with its glow map (the amber cracks and eyes) additive on top.</li>
 *   <li>variant 1, the Mud Spout (mud_spout): clip idle until tick 8, then erupt (the column shoots up); a mud ripple on the ground under it; cutout + glow.</li>
 * </ul>
 */
public final class MudPropPainter {
    private MudPropPainter() {}

    private static final GeoSpec GOLEM = GeoSpec.of("mud", "golem"), SPOUT = GeoSpec.of("mud", "spout");
    private static ResourceLocation sigil, ripple;

    public static void register() {
        sigil = AuraRender.tex("particle/mud_sigil");
        ripple = AuraRender.tex("particle/mud_ripple");
        PropPainters.register(PropKind.MUD_1, MudPropPainter::paint);
    }

    private static void paint(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, int light) {
        float k = TreePropPainter.fade(e, partial, 6f, 10f);
        if (k <= 0.01f) return;
        float s = e.scale(), life = e.life() + partial, pulse = 0.75f + 0.25f * Mth.sin(age * 0.2f);
        int glow = TreePropPainter.argb(k * pulse, 0xFFFFFF);
        if ((e.param() & 0xFF) == 0) {
            TreePropPainter.sigil(pose, buf, sigil, 2.6f * s * k, age * 0.3f, 0xFF9A3C, 0.6f * k);
            String clip = TreePropPainter.phase(e) == 1 ? "slam" : life < 22f ? "rise" : "idle";
            float clock = TreePropPainter.phase(e) == 1 ? TreePropPainter.since(e, partial) / 20f : life < 22f ? life / 20f : age / 20f;
            float sc = 0.95f * s * (0.6f + 0.4f * k);
            TreePropPainter.model(pose, buf, GOLEM, clip, clock, GeoDraw.Layer.CUTOUT, light, sc, sc, sc, 0f, 0xFFFFFFFF, glow);
        } else {
            float ph = Mth.clamp((life - 8f) / 12f, 0f, 1f);
            TreePropPainter.sigil(pose, buf, ripple, (0.8f + 1.6f * ph) * s, 0f, 0xD2A070, 0.7f * k * (1f - ph * 0.6f));
            boolean up = life >= 8f;
            float sc = 1.25f * s * k;
            TreePropPainter.model(pose, buf, SPOUT, up ? "erupt" : "idle", up ? (life - 8f) / 20f : age / 20f, GeoDraw.Layer.CUTOUT, light, sc, sc * (up ? 1f : 0.15f), sc, 0f, 0xFFFFFFFF, glow);
        }
    }
}
