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
 * 1.93.0 Spike Magic: how this magic's two constructs look (server side: prop.SpikeProps). SPIKE_1, the pillar: geo model spike_pillar; a cracked glowing seal warns on the ground until the tick of the strike, then the spikes thrust up (rise), and they sink back in the last 10 ticks; SPIKE_2, the urchin: geo model spike_urchin (idle spin, roll while it travels), glowing hot.
 * Render order: shadow (translucent), the model (cutout), its glow map (additive, full bright), the ground glyph (additive). The one-shot clips start
 * at the tick the synced state began ({@link ShakudoPropState#since}).
 */
public final class SpikePropPainter {
    private SpikePropPainter() {}

    private static final GeoSpec PILLAR = GeoSpec.of("spike", "pillar"), URCHIN = GeoSpec.of("spike", "urchin");
    private static final ResourceLocation CRACK = AuraRender.tex("particle/spike_crack"), SIGIL = AuraRender.tex("particle/spike_sigil");
    private static final int ICE = 0xCFEFFF;

    /** Called once on the client by PropPainterRegistry. */
    public static void register() {
        PropPainters.register(PropKind.SPIKE_1, SpikePropPainter::pillar);
        PropPainters.register(PropKind.SPIKE_2, SpikePropPainter::urchin);
    }

    private static void pillar(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        float s = Math.max(0.3f, e.scale()) * 0.85f, maxL = Math.max(1f, e.maxLife());
        boolean struck = state(e) != ShakudoPropState.IDLE;
        float warn = struck ? 0f : smooth(age / 8f);
        glyph(pose, buffers, CRACK, 1.3f * s * (0.5f + 0.5f * warn), 0.7f, AuraRender.alpha(0xFFBFE4FF, (struck ? 0.35f : 0.35f + 0.55f * warn) * fade(e, age, 10f)));
        if (!struck) {
            glyph(pose, buffers, SIGIL, 0.9f * s, age * 0.2f, AuraRender.alpha(0xFFBFE4FF, 0.5f * warn));
            return;
        }
        float sinceT = since(e, partial);
        float out = age > maxL - 10f ? (age - (maxL - 10f)) / 20f : -1f;
        shade(pose, buffers, 1.0f * s, 0.3f, light);
        if (out >= 0f) model(pose, buffers, PILLAR, "retract", out, s, 0f, light, ICE, 0.7f);
        else model(pose, buffers, PILLAR, sinceT < 10f ? "rise" : "idle", sinceT < 10f ? sinceT / 20f : age / 20f, s, 0f, light, ICE, 0.65f + 0.3f * Mth.sin(age * 0.2f));
    }

    private static void urchin(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        float s = Math.max(0.3f, e.scale()) * 0.85f * grow(age, 6f);
        if (s <= 0.02f) return;
        shade(pose, buffers, 0.8f * s, 0.3f, light);
        model(pose, buffers, URCHIN, moving(e) ? "roll" : "idle", age / 20f, s, 0.05f, light, ICE, 0.7f + 0.3f * Mth.sin(age * 0.3f));
    }
}
