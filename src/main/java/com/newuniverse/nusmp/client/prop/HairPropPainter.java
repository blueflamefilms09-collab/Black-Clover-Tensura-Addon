package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.prop.ShakudoPropState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import static com.newuniverse.nusmp.client.prop.ShakudoPropDraw.*;

/**
 * 1.93.0 Hair Magic: how this magic's two constructs look (server side: prop.HairProps). HAIR_1, the tendril: geo model hair_tendril (rise, idle sway, lash, then curled round its target in grab), shrinking away at the end; HAIR_2, the cocoon: geo model hair_cocoon round the owner (idle pulse, open in the last 16 ticks).
 * Render order: shadow (translucent), the model (cutout), its glow map (additive, full bright), the ground glyph (additive). The one-shot clips start
 * at the tick the synced state began ({@link ShakudoPropState#since}).
 */
public final class HairPropPainter {
    private HairPropPainter() {}

    private static final GeoSpec TENDRIL = GeoSpec.of("hair", "tendril"), COCOON = GeoSpec.of("hair", "cocoon");
    private static final ResourceLocation SIGIL = AuraRender.tex("particle/hair_sigil");
    private static final int PINK = 0xFFB0D0;

    /** Called once on the client by PropPainterRegistry. */
    public static void register() {
        PropPainters.register(PropKind.HAIR_1, HairPropPainter::tendril);
        PropPainters.register(PropKind.HAIR_2, HairPropPainter::cocoon);
    }

    private static void tendril(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        float s = Math.max(0.3f, e.scale()) * 0.8f * fade(e, age, 10f);
        if (s <= 0.02f) return;
        String clip;
        float secs;
        if (age < 10f) { clip = "rise"; secs = age / 20f; }
        else if (state(e) == ShakudoPropState.ACT) {
            float t = since(e, partial);
            if (t < 14f) { clip = "lash"; secs = t / 20f; } else { clip = "grab"; secs = (t - 14f) / 20f; }
        } else { clip = "idle"; secs = age / 20f; }
        shade(pose, buffers, 0.9f * s, 0.3f, light);
        glyph(pose, buffers, SIGIL, 1.1f * s, age * 0.03f, AuraRender.alpha(0xFFFF8EB6, 0.45f * grow(age, 8f)));
        model(pose, buffers, TENDRIL, clip, secs, s, 0f, light, PINK, 0.6f + 0.3f * Mth.sin(age * 0.14f));
    }

    private static void cocoon(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        float maxL = Math.max(1f, e.maxLife());
        float open = age - (maxL - 16f);
        float s = Math.max(0.3f, e.scale()) * 0.85f * (0.3f + 0.7f * grow(age, 10f));
        shade(pose, buffers, 1.0f * s, 0.25f, light);
        boolean opening = open > 0f;
        model(pose, buffers, COCOON, opening ? "open" : "idle", opening ? open / 20f : age / 20f, s, 0f, light, PINK, 0.55f + 0.3f * Mth.sin(age * 0.1f) + (opening ? 0.4f : 0f));
    }
}
