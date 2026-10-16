package com.newuniverse.nusmp.client.shadow;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.client.prop.PropPainters;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Why: draws the shadow wolves of Unite Canis ({@code PropKind.SHADOW_PACK_1}) as one GeoLite model (devil_shadow_wolf, tools/gen_devil_assets.py) with
 * the clip of its state: bite (param bits 0-7 hold the bite tick), run (it moved this tick) or idle. Cutout with the glow map additive; translucent
 * while it forms and while it fades. The spec is built once; nothing is allocated per frame beyond vertices.
 */
public final class ShadowPackPainter {
    private ShadowPackPainter() {}

    private static final GeoSpec WOLF = new GeoSpec(rl("geo/devils/devil_shadow_wolf.geo.json"), rl("animations/devils/devil_shadow_wolf.animation.json"),
            rl("textures/entity/devils/devil_shadow_wolf.png"), rl("textures/entity/devils/devil_shadow_wolf_glow.png"));

    private static ResourceLocation rl(String p) { return ResourceLocation.fromNamespaceAndPath("nusmp", p); }

    /** Called once on the client by PropPainterRegistry. */
    public static void register() {
        PropPainters.register(PropKind.SHADOW_PACK_1, ShadowPackPainter::wolf);
    }

    private static void wolf(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, int light) {
        int phase = e.param() & 0xFF, life = e.life(), left = e.maxLife() - life;
        float fade = Mth.clamp(Math.min(life + partial, left - partial) / 8f, 0f, 1f);
        if (fade <= 0.01f) return;
        String clip;
        float t;
        if (phase > 0) { clip = "bite"; t = (phase - 1 + partial) / 20f; }
        else {
            float dx = (float) (e.getX() - e.xo), dz = (float) (e.getZ() - e.zo), sp = Mth.sqrt(dx * dx + dz * dz);
            if (sp > 0.05f) { clip = "run"; t = age / 20f * Mth.clamp(sp / 0.3f, 0.8f, 1.4f); } else { clip = "idle"; t = age / 20f; }
        }
        float s = e.scale() * 1.15f;
        pose.pushPose();
        pose.scale(s, s, s);
        int a = (int) (255 * fade);
        GeoDraw.paint(pose, buf, WOLF, clip, t, GeoDraw.Space.PROP, fade < 0.98f ? GeoDraw.Layer.TRANSLUCENT : GeoDraw.Layer.CUTOUT, light,
                (a << 24) | 0xFFFFFF, (a << 24) | 0xFFFFFF);
        pose.popPose();
    }
}
