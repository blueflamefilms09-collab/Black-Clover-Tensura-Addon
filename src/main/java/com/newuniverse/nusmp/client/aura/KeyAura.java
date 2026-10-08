package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.client.prop.KeyDraw;
import com.newuniverse.nusmp.client.prop.PropDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 0.54 Key Magic: the player render layer (Aura.KEY), one style. A keyhole glyph glows on the back and three small great keys orbit the body.
 * Layer space: y points down, the middle of the torso is at y = 0.3, the feet at y = 1.5, the back is +z.
 * Passes, in draw order (nothing is drawn over the face, so there is no head pass to skip; in first person the keys in front of the camera are left out):
 *   1. the three keys: the textured great-key model, cutout (entityCutoutNoCull), scaled down to orbit at 0.82 blocks, tip leading;
 *   2. their glow maps: additive and full bright, drawn by GeoDraw straight after each key;
 *   3. the glyph on the back: one upright additive quad (RenderType.eyes, full bright) 0.2 behind the torso, drawn on both sides, breathing in strength.
 */
public final class KeyAura {
    private KeyAura() {}

    private static final GeoSpec KEY = GeoSpec.of("key", "great_key");
    private static ResourceLocation glyph;

    /** Called once by AuraRegistry on the client. */
    public static void register() {
        glyph = AuraRender.tex("aura/key_aura_glyph");
        PlayerAuraClient.register(Aura.KEY, KeyAura::paint);
    }

    private static void paint(AuraContext c) {
        float f = c.fade();
        if (f <= 0.002f) return;
        boolean self1p = c.player() == Minecraft.getInstance().player && Minecraft.getInstance().options.getCameraType().isFirstPerson();
        float age = c.age();
        PoseStack pose = c.pose();

        // 1 + 2. three small keys orbiting, tip first, each at its own height
        for (int i = 0; i < 3; i++) {
            float a = age * 0.07f + i * 2.0944f;
            float x = Mth.cos(a) * 0.82f, z = Mth.sin(a) * 0.82f;
            if (self1p && z < -0.2f) continue;
            float y = 0.25f + 0.3f * Mth.sin(age * 0.05f + i * 2.1f) + (i - 1) * 0.12f;
            float sc = 0.08f * (0.4f + 0.6f * f);
            pose.pushPose();
            pose.translate(x, y, z);
            pose.mulPose(Axis.YP.rotationDegrees(180f - a * (180f / Mth.PI)));         // the model's tip is its -z: turn it along the orbit
            pose.mulPose(Axis.XP.rotationDegrees(-12f));
            pose.scale(sc, sc, sc);
            pose.translate(0f, -0.75f, 0f);                                              // GeoDraw's player space puts the model's origin on the feet
            GeoDraw.paint(pose, c.buffers(), KEY, "idle", age / 20f + i * 1.3f, GeoDraw.Space.PLAYER, GeoDraw.Layer.CUTOUT, c.light(),
                    0xFFFFFFFF, KeyDraw.argb(0.9f * f, 0xFFE9B0));
            pose.popPose();
        }

        // 3. the keyhole glyph on the back
        float breathe = 0.78f + 0.22f * Mth.sin(age * 0.11f);
        pose.pushPose();
        pose.translate(0f, 0.3f, 0.2f);
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(age * 0.04f) * 3f));
        KeyDraw.upright(pose, c.buffers().getBuffer(AuraRender.additive(glyph)), 0f, 0f, 0f, 0.42f, -0.42f, KeyDraw.argb(0.9f * f * breathe, 0xFFFFFF), PropDraw.FULL_BRIGHT);
        pose.popPose();
    }
}
