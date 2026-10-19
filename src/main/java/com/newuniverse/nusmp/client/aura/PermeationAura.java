package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.client.aura.SpatialArmourKit.Style;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 1.93.0 Permeation Magic: the temporary armour (Aura.PERMEATION), the Phase Shroud: the magic of passing through shown as a set made of outlines: three thin rings over
 * the brow (the face stays free), a ribcage lattice, hollow pauldron frames and open rings round the forearms, laddered shins, a cloak of wisps with two lattice wings, and a
 * gyroscope of three rings over the head. Eight GeoLite pieces (tools/gen_permeation_geo.py) on the model parts they were built for, put on and taken off with
 * {@link EquipAnimation} through {@link SpatialArmourKit}: they are GHOSTS (always translucent) and phase IN from inside the body (the chest from behind it, the others
 * from short, inward vectors; chest first, the wings and gyroscope last, delays 0 to 12) and phase out over the last 18 ticks. Style 0 is the plain set; style 1 (a heavy page)
 * adds the great ground ring and more scanner rings.
 * Passes (layer space: y points DOWN, the feet at y = 1.5):
 *   1. the ghost pieces (translucent) and their additive glow maps (every outline and core);
 *   2. the ground ring with a ripple running out of it (additive, full bright);
 *   3. scanner rings rising through the body and bubbles of nothing drifting up (additive, full bright).
 * For the local player in first person the head pieces and the gyroscope are left out.
 */
public final class PermeationAura {
    private PermeationAura() {}

    private static final GeoSpec HELM = GeoSpec.of("permeation", "helm"), CHEST = GeoSpec.of("permeation", "chest"), ARM_R = GeoSpec.of("permeation", "arm_r"),
            ARM_L = GeoSpec.of("permeation", "arm_l"), LEG_R = GeoSpec.of("permeation", "leg_r"), LEG_L = GeoSpec.of("permeation", "leg_l"),
            BACK = GeoSpec.of("permeation", "back"), HALO = GeoSpec.of("permeation", "halo");
    private static final int CYAN = 0x65D7E8, PALE = 0xDFFBFF, FB = SpatialArmourKit.FB;
    private static ResourceLocation ring, ripple, mote;

    /** Called once by AuraRegistry on the client. */
    public static void register() {
        ring = AuraRender.tex("particle/permeation_ring");
        ripple = AuraRender.tex("particle/permeation_ripple");
        mote = AuraRender.tex("particle/permeation_mote");
        PlayerAuraClient.register(Aura.PERMEATION, PermeationAura::paint);
    }

    private static void paint(AuraContext c) {
        float age = c.age(), pulse = 0.8f + 0.2f * Mth.sin(age * 0.15f);
        boolean self1p = SpatialArmourKit.self1p(c);
        PlayerModel<AbstractClientPlayer> m = c.model();
        Style cyan = Style.ghost(0.92f, CYAN, 0.9f * pulse), pale = Style.ghost(0.85f, PALE, 0.7f);

        // 1. the pieces phase in from inside the body
        SpatialArmourKit.piece(c, m.body, CHEST, 0f, 0f, 0f, 8f, cyan);
        if (!self1p) SpatialArmourKit.piece(c, m.head, HELM, 2f, 0f, 2f, 6f, cyan);
        SpatialArmourKit.piece(c, m.rightArm, ARM_R, 4f, 6f, 0f, 2f, pale);
        SpatialArmourKit.piece(c, m.leftArm, ARM_L, 5f, -6f, 0f, 2f, pale);
        SpatialArmourKit.piece(c, m.rightLeg, LEG_R, 7f, 3f, -4f, 3f, pale);
        SpatialArmourKit.piece(c, m.leftLeg, LEG_L, 8f, -3f, -4f, 3f, pale);
        SpatialArmourKit.piece(c, m.body, BACK, 10f, 0f, 0f, -10f, cyan);
        if (!self1p) SpatialArmourKit.piece(c, m.head, HALO, 12f, 0f, 8f, 0f, pale);

        float on = Math.min(EquipAnimation.alpha(EquipAnimation.assembled(c, 0f)), 1f);
        if (on <= 0.01f) return;
        PoseStack pose = c.pose();

        // 2. the ground ring with a ripple running out of it
        pose.pushPose();
        pose.translate(0f, 1.47f, 0f);
        SpatialArmourKit.flat(pose, c.buffers().getBuffer(AuraRender.additive(ring)), (c.style() == 1 ? 1.3f : 0.9f) * (0.7f + 0.3f * on), age * 1.1f, SpatialArmourKit.argb(0.85f * on, CYAN), FB);
        float wave = (age * 0.03f) % 1f;
        pose.translate(0f, -0.01f, 0f);
        SpatialArmourKit.flat(pose, c.buffers().getBuffer(AuraRender.additive(ripple)), 0.4f + 1.4f * wave, 0f, SpatialArmourKit.argb(0.6f * on * (1f - wave), PALE), FB);
        pose.popPose();

        // 3. scanner rings rising through the body, bubbles of nothing drifting up
        VertexConsumer rv = c.buffers().getBuffer(AuraRender.additive(ripple));
        int rings = c.style() == 1 ? 3 : 2;
        for (int i = 0; i < rings; i++) {
            float ph = (age * 0.022f + i / (float) rings) % 1f;
            float y = 1.4f - ph * 2.1f;
            if (self1p && y < -0.1f) continue;
            pose.pushPose();
            pose.translate(0f, y, 0f);
            SpatialArmourKit.flat(pose, rv, 0.62f + 0.1f * Mth.sin(ph * Mth.PI), age * 2f + i * 40f, SpatialArmourKit.argb(0.7f * on * Mth.sin(ph * Mth.PI), CYAN), FB);
            pose.popPose();
        }
        SpatialArmourKit.camera(pose);
        VertexConsumer mv = c.buffers().getBuffer(AuraRender.additive(mote));
        for (int i = 0; i < 7; i++) {
            float ph = (age * 0.02f + i * 0.143f) % 1f, a = i * 2.399f + age * 0.015f;
            float y = 1.3f - ph * 2.0f, r = 0.5f + 0.12f * Mth.sin(i * 2.1f + age * 0.05f);
            if (self1p && y < 0.0f) continue;
            SpatialArmourKit.glow(pose, mv, Mth.cos(a) * r, y, Mth.sin(a) * r, 0.06f + 0.05f * Mth.sin(ph * Mth.PI), 0f, SpatialArmourKit.argb(0.85f * on * Mth.sin(ph * Mth.PI), i % 2 == 0 ? CYAN : PALE), FB);
        }
    }
}
