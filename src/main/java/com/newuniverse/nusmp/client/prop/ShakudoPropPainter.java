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
 * 1.93.0 Shakudo Magic: how this magic's two constructs look (server side: prop.ShakudoProps). SHAKUDO_1, the golem: geo model shakudo_golem (idle / walk / strike / crumble), rising out of the ground over 12 ticks; SHAKUDO_2, the bulwark: geo model shakudo_bulwark (rise / idle), its seal flashes when it stops a projectile and it sinks in the last 10 ticks.
 * Render order: shadow (translucent), the model (cutout), its glow map (additive, full bright), the ground glyph (additive). The one-shot clips start
 * at the tick the synced state began ({@link ShakudoPropState#since}).
 */
public final class ShakudoPropPainter {
    private ShakudoPropPainter() {}

    private static final GeoSpec GOLEM = GeoSpec.of("shakudo", "golem"), BULWARK = GeoSpec.of("shakudo", "bulwark");
    private static final ResourceLocation SIGIL = AuraRender.tex("particle/shakudo_sigil");
    private static final int GOLD = 0xFFD070;

    /** Called once on the client by PropPainterRegistry. */
    public static void register() {
        PropPainters.register(PropKind.SHAKUDO_1, ShakudoPropPainter::golem);
        PropPainters.register(PropKind.SHAKUDO_2, ShakudoPropPainter::bulwark);
    }

    private static void golem(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        float s = Math.max(0.3f, e.scale()) * 0.8f, maxL = Math.max(1f, e.maxLife());
        float rise = grow(age, 12f), crumbleT = age - (maxL - 26f);
        String clip;
        float secs;
        if (crumbleT > 0f) { clip = "crumble"; secs = crumbleT / 20f; }
        else if (state(e) == ShakudoPropState.ACT && since(e, partial) < 24f) { clip = "strike"; secs = since(e, partial) / 20f; }
        else if (moving(e)) { clip = "walk"; secs = age / 20f; }
        else { clip = "idle"; secs = age / 20f; }
        float end = crumbleT > 0f ? 1f - Mth.clamp(crumbleT / 24f, 0f, 1f) : 1f;
        shade(pose, buffers, 1.4f * s * (0.4f + 0.6f * rise), 0.35f * rise * end, light);
        glyph(pose, buffers, SIGIL, 1.5f * s * rise, age * 0.02f, AuraRender.alpha(0xFFE6B450, 0.55f * rise * end));
        model(pose, buffers, GOLEM, clip, secs, s, -(1f - rise) * 2.6f * s, light, GOLD, (0.55f + 0.35f * Mth.sin(age * 0.12f)) * end);
    }

    private static void bulwark(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        float s = Math.max(0.3f, e.scale()), sink = fade(e, age, 10f);
        boolean up = age < 12f;
        float flash = state(e) == ShakudoPropState.ACT ? Math.max(0f, 1f - since(e, partial) / 14f) : 0f;
        shade(pose, buffers, 1.3f * s, 0.35f * sink, light);
        glyph(pose, buffers, SIGIL, 1.5f * s, -age * 0.015f, AuraRender.alpha(0xFFE6B450, 0.5f * sink * (0.7f + 0.3f * flash)));
        model(pose, buffers, BULWARK, up ? "rise" : "idle", age / 20f, s, -(1f - sink) * 3f * s, light, GOLD, (0.5f + 0.3f * Mth.sin(age * 0.1f) + 0.6f * flash) * sink);
    }
}
