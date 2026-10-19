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
 * 1.93.0 Scale Magic: how this magic's two constructs look (server side: prop.ScaleProps). SCALE_1, the serpent: geo model scale_serpent (idle slither, strike), growing in over 10 ticks and shrinking away at the end; SCALE_2, the shield: geo model scale_shield (idle bob, block), hovering 0.4 blocks off the ground.
 * Render order: shadow (translucent), the model (cutout), its glow map (additive, full bright), the ground glyph (additive). The one-shot clips start
 * at the tick the synced state began ({@link ShakudoPropState#since}).
 */
public final class ScalePropPainter {
    private ScalePropPainter() {}

    private static final GeoSpec SERPENT = GeoSpec.of("scale", "serpent"), SHIELD = GeoSpec.of("scale", "shield");
    private static final ResourceLocation RIPPLE = AuraRender.tex("particle/scale_ripple");
    private static final int TEAL = 0xA8FFE8;

    /** Called once on the client by PropPainterRegistry. */
    public static void register() {
        PropPainters.register(PropKind.SCALE_1, ScalePropPainter::serpent);
        PropPainters.register(PropKind.SCALE_2, ScalePropPainter::shield);
    }

    private static void serpent(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        float s = Math.max(0.3f, e.scale()) * 0.75f * grow(age, 10f) * fade(e, age, 12f);
        if (s <= 0.02f) return;
        boolean strike = state(e) == ShakudoPropState.ACT && since(e, partial) < 18f;
        shade(pose, buffers, 1.2f * s, 0.3f, light);
        glyph(pose, buffers, RIPPLE, (1.0f + 0.6f * ((age * 0.03f) % 1f)) * s, 0f, AuraRender.alpha(0xFF8AF0E0, 0.5f * (1f - (age * 0.03f) % 1f)));
        model(pose, buffers, SERPENT, strike ? "strike" : "idle", strike ? since(e, partial) / 20f : age / 20f * (moving(e) ? 1.9f : 1f), s, 0f, light, TEAL,
                0.55f + 0.3f * Mth.sin(age * 0.15f + e.seed()));
    }

    private static void shield(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        float s = Math.max(0.3f, e.scale()) * 0.8f * grow(age, 8f) * fade(e, age, 10f);
        if (s <= 0.02f) return;
        boolean block = state(e) == ShakudoPropState.ACT && since(e, partial) < 10f;
        float flash = block ? 1f - since(e, partial) / 10f : 0f;
        shade(pose, buffers, 0.9f * s, 0.25f, light);
        model(pose, buffers, SHIELD, block ? "block" : "idle", block ? since(e, partial) / 20f : age / 20f, s, 0.45f * s + 0.04f * Mth.sin(age * 0.1f), light, TEAL,
                0.5f + 0.25f * Mth.sin(age * 0.12f) + 0.7f * flash);
    }
}
