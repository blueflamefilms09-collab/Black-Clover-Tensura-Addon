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
 * 1.93.0 Nail Magic: how this magic's two constructs look (server side: prop.NailProps). NAIL_1, the bolt: geo model nail_bolt flying point first along its flight (yaw and pitch of the prop), spinning, then still once it is driven in, shrinking away at the end; NAIL_2, the hammer: geo model nail_hammer hanging head down above the ground, a seal warning where it will land, falling onto it over 10 ticks, resting there glowing, then shrinking away.
 * Render order: shadow (translucent), the model (cutout), its glow map (additive, full bright), the ground glyph (additive). The one-shot clips start
 * at the tick the synced state began ({@link ShakudoPropState#since}).
 */
public final class NailPropPainter {
    private NailPropPainter() {}

    private static final GeoSpec BOLT = GeoSpec.of("nail", "bolt"), HAMMER = GeoSpec.of("nail", "hammer");
    private static final ResourceLocation SIGIL = AuraRender.tex("particle/nail_sigil");
    private static final int EMBER = 0xFFA050;

    /** Called once on the client by PropPainterRegistry. */
    public static void register() {
        PropPainters.register(PropKind.NAIL_1, NailPropPainter::bolt);
        PropPainters.register(PropKind.NAIL_2, NailPropPainter::hammer);
    }

    private static void bolt(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        float s = Math.max(0.3f, e.scale()) * 0.7f * fade(e, age, 10f);
        if (s <= 0.02f) return;
        boolean stuck = state(e) == ShakudoPropState.DONE;
        pose.pushPose();
        pose.translate(0f, 0.25f * s, 0f);
        pose.mulPose(Axis.XP.rotationDegrees(e.getXRot()));
        model(pose, buffers, BOLT, stuck ? null : "fly", age / 20f, s, 0f, light, EMBER, stuck ? 0.5f : 0.9f);
        pose.popPose();
    }

    private static void hammer(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        float s = Math.max(0.3f, e.scale()) * 0.8f, maxL = Math.max(1f, e.maxLife());
        float p = smooth((age - 3f) / 11f);
        float vanish = fade(e, age, 8f);
        float flash = state(e) == ShakudoPropState.ACT ? Math.max(0f, 1f - since(e, partial) / 16f) : 0f;
        glyph(pose, buffers, SIGIL, 1.9f * s * (0.5f + 0.5f * smooth(age / 6f)), age * 0.12f, AuraRender.alpha(0xFFFFB070, (0.35f + 0.55f * (1f - p) + 0.6f * flash) * vanish));
        shade(pose, buffers, 1.2f * s, 0.3f * p * vanish, light);
        // the hammer hangs head down: turned half a turn about x, its head 3.3 s below the handle end; the handle end falls from 2 heights above to the ground
        pose.pushPose();
        float drop = (1f - p * p) * 2.2f * s;
        pose.translate(0f, 3.3f * s + drop, 0f);
        pose.mulPose(Axis.XP.rotationDegrees(180f));
        model(pose, buffers, HAMMER, "idle", age / 20f, s * vanish, 0f, light, EMBER, 0.6f + 0.5f * flash + 0.2f * Mth.sin(age * 0.2f));
        pose.popPose();
    }
}
