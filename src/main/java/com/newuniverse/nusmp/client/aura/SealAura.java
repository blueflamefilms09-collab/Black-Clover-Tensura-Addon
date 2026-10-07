package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.client.prop.PropDraw;
import com.newuniverse.nusmp.client.prop.SealDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 0.54 Seal Magic: the player render layer (Aura.SEALING).
 * Style 0, a gentle seal-rune halo: a shimmering skin shell (additive, 1.05), a rune circle at the feet, a tilted rune ring round the waist and a thin
 * halo over the head (not drawn for the local player in first person).
 * Style 1, Orbital Bind: three tilted streak rings of different speed round the body, a latitude sphere shell and orbiting sparks, all additive.
 * Layer space: y points down, the middle of the body is at y = 0.5 and the feet at y = 1.5.
 */
public final class SealAura {
    private SealAura() {}

    private static final int FB = PropDraw.FULL_BRIGHT;
    private static final int CYAN = 0x5CE1FF, ICE = 0xD8F8FF, BLUE = 0x3A8CFF;
    private static ResourceLocation ringA, ringB, ringC, halo, sphere, skin, spark;

    /** Called once by AuraRegistry on the client. */
    public static void register() {
        ringA = AuraRender.tex("aura/seal_ring_a");
        ringB = AuraRender.tex("aura/seal_ring_b");
        ringC = AuraRender.tex("aura/seal_ring_c");
        halo = AuraRender.tex("aura/seal_halo");
        sphere = AuraRender.tex("aura/seal_sphere");
        skin = AuraRender.tex("aura/seal_skin");
        spark = AuraRender.tex("prop/seal_flare");
        PlayerAuraClient.register(Aura.SEALING, SealAura::paint);
    }

    private static void paint(AuraContext c) {
        float f = c.fade();
        if (f <= 0.002f) return;
        boolean self1p = c.player() == Minecraft.getInstance().player && Minecraft.getInstance().options.getCameraType().isFirstPerson();
        if (c.style() == 1) orbital(c, f, self1p); else runeHalo(c, f, self1p);
    }

    private static void runeHalo(AuraContext c, float f, boolean self1p) {
        float age = c.age(), breathe = 0.8f + 0.2f * Mth.sin(age * 0.12f);
        PoseStack pose = c.pose();
        // 1. the shimmering skin shell, 5 % bigger, additive
        AuraRender.modelBright(c, AuraRender.additive(skin), 1.05f, 1.05f, 1.05f, SealDraw.argb(0.3f * f * breathe, CYAN));
        // 2. the rune circle at the feet, 3. the waist ring, 4. the halo over the head
        VertexConsumer vc = c.buffers().getBuffer(AuraRender.additive(halo));
        pose.pushPose();
        pose.translate(0f, 1.47f, 0f);
        SealDraw.flat(pose, vc, 0.95f * (0.6f + 0.4f * f), age * 1.4f, SealDraw.argb(0.9f * f, CYAN), FB);
        SealDraw.flat(pose, vc, 0.62f * (0.6f + 0.4f * f), -age * 2.2f, SealDraw.argb(0.7f * f, ICE), FB);
        pose.popPose();
        pose.pushPose();
        pose.translate(0f, 0.35f + 0.05f * Mth.sin(age * 0.09f), 0f);
        pose.mulPose(Axis.XP.rotationDegrees(8f * Mth.sin(age * 0.05f)));
        SealDraw.flat(pose, vc, 0.7f, -age * 1.8f, SealDraw.argb(0.55f * f * breathe, ICE), FB);
        pose.popPose();
        if (!self1p) {
            pose.pushPose();
            pose.translate(0f, -0.72f + 0.03f * Mth.sin(age * 0.1f), 0f);
            SealDraw.flat(pose, vc, 0.34f, age * 3f, SealDraw.argb(0.8f * f, ICE), FB);
            pose.popPose();
        }
    }

    private static void orbital(AuraContext c, float f, boolean self1p) {
        float age = c.age();
        PoseStack pose = c.pose();
        SealDraw.camera(pose);
        float grow = 0.7f + 0.3f * f;
        // 1. the latitude sphere shell, 1.1 radius, additive, slowly turning
        pose.pushPose();
        pose.translate(0f, 0.5f, 0f);
        pose.mulPose(Axis.YP.rotationDegrees(age * 2f));
        PropDraw.ellipsoid(pose, c.buffers().getBuffer(AuraRender.additive(sphere)), 0f, 0f, 0f, 1.0f * grow, 1.15f * grow, 1.0f * grow, 5, 8,
                SealDraw.argb(0.42f * f, BLUE), FB);
        pose.popPose();
        // 2. three streak rings: radius 0.85 / 1.0 / 1.15, tilted differently, spinning at 9 / -13 / 20 degrees per tick
        ring(c, ringA, 0.85f * grow, 0f, 0f, age * 9f, SealDraw.argb(0.95f * f, CYAN));
        ring(c, ringB, 1.0f * grow, 62f, age * 0.7f, -age * 13f, SealDraw.argb(0.95f * f, ICE));
        ring(c, ringC, 1.15f * grow, -58f, 90f + age * 0.5f, age * 20f, SealDraw.argb(0.9f * f, BLUE));
        // 3. a rune ring on the ground
        pose.pushPose();
        pose.translate(0f, 1.47f, 0f);
        SealDraw.flat(pose, c.buffers().getBuffer(AuraRender.additive(halo)), 0.9f * grow, age * 1.6f, SealDraw.argb(0.6f * f, CYAN), FB);
        pose.popPose();
        // 4. sparks riding the rings
        VertexConsumer vc = c.buffers().getBuffer(AuraRender.additive(spark));
        for (int i = 0; i < 8; i++) {
            if (self1p && i < 3) continue;
            float a = age * (0.16f + 0.03f * (i % 3)) * (i % 2 == 0 ? 1f : -1f) + i * 0.8f;
            float r = (0.85f + 0.15f * (i % 3)) * grow, y = 0.5f + Mth.sin(a * 0.7f + i) * 0.55f;
            float rr = r * Mth.sqrt(Math.max(0.05f, 1f - (y - 0.5f) * (y - 0.5f) / (1.2f * 1.2f)));
            SealDraw.glow(pose, vc, Mth.cos(a) * rr, y, Mth.sin(a) * rr, 0.07f + 0.03f * Mth.sin(age * 0.4f + i), age * 0.2f + i, SealDraw.argb(0.95f * f, ICE), FB);
        }
    }

    /** One streak ring: a flat square tilted by tilt about x, turned by yaw about y, spun in its own plane. */
    private static void ring(AuraContext c, ResourceLocation tex, float r, float tilt, float yaw, float spin, int argb) {
        PoseStack pose = c.pose();
        pose.pushPose();
        pose.translate(0f, 0.5f, 0f);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.mulPose(Axis.XP.rotationDegrees(tilt));
        SealDraw.flat(pose, c.buffers().getBuffer(AuraRender.additive(tex)), r, spin, argb, FB);
        pose.popPose();
    }
}
