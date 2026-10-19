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
 * 1.93.0 Gravity Magic: the temporary armour (Aura.GRAVITY), the Demon King's regalia after Dante Zogratis's gravity: black-violet iron stacked three layers
 * deep, huge descending pauldrons, a crown of spikes, a singularity on the breast and a tilted accretion ring with three dark orbs turning behind the back.
 * Eight GeoLite pieces (tools/gen_gravity_geo.py), each on the model part it was built for, put on and taken off with {@link EquipAnimation} through
 * {@link SpatialArmourKit}. Every piece falls IN from above (the set is dropped onto the body, weight first: chest, helm, arms, legs, then the ring and the
 * halo; delays 0 to 12) and shatters out over the last 18 ticks. Style 0 is the plain set, style 1 (the Demon King's presence, a heavy page) adds the sigil.
 * Passes (layer space: y points DOWN, the feet are at y = 1.5):
 *   1. the pieces (cutout, translucent only while a piece forges or shatters) and their additive glow maps;
 *   2. a black pool on the ground under the feet (gravity_disc, translucent, drawn before the light);
 *   3. the pressure ring and, for style 1, the sigil on the ground, and the fractures round the feet (additive, full bright);
 *   4. falling pressure streaks round the body and glints on the singularity (additive, camera-facing, full bright).
 * For the local player in first person the head pieces and the halo are left out.
 */
public final class GravityAura {
    private GravityAura() {}

    private static final GeoSpec HELM = GeoSpec.of("gravity", "helm"), CHEST = GeoSpec.of("gravity", "chest"), ARM_R = GeoSpec.of("gravity", "arm_r"),
            ARM_L = GeoSpec.of("gravity", "arm_l"), LEG_R = GeoSpec.of("gravity", "leg_r"), LEG_L = GeoSpec.of("gravity", "leg_l"),
            BACK = GeoSpec.of("gravity", "back"), HALO = GeoSpec.of("gravity", "halo");
    private static final int VIOLET = 0xB45AFF, PALE = 0xE6C8FF, FB = SpatialArmourKit.FB;
    private static ResourceLocation ring, disc, crack, sigil, streak, star;

    /** Called once by AuraRegistry on the client. */
    public static void register() {
        ring = AuraRender.tex("particle/gravity_ring");
        disc = AuraRender.tex("particle/gravity_disc");
        crack = AuraRender.tex("particle/gravity_crack");
        sigil = AuraRender.tex("particle/gravity_sigil");
        streak = AuraRender.tex("particle/gravity_streak");
        star = AuraRender.tex("particle/spatial_star");
        PlayerAuraClient.register(Aura.GRAVITY, GravityAura::paint);
    }

    private static void paint(AuraContext c) {
        float age = c.age(), pulse = 0.75f + 0.25f * Mth.sin(age * 0.11f);
        boolean self1p = SpatialArmourKit.self1p(c);
        PlayerModel<AbstractClientPlayer> m = c.model();
        Style iron = Style.solid(VIOLET, 0.8f * pulse);
        Style dark = Style.solid(PALE, 0.55f);

        // 1. the pieces fall onto the body from above
        SpatialArmourKit.piece(c, m.body, CHEST, 0f, 0f, -16f, -2f, iron);
        if (!self1p) SpatialArmourKit.piece(c, m.head, HELM, 2f, 0f, -18f, 0f, iron);
        SpatialArmourKit.piece(c, m.rightArm, ARM_R, 4f, -6f, -16f, 0f, iron);
        SpatialArmourKit.piece(c, m.leftArm, ARM_L, 5f, 6f, -16f, 0f, iron);
        SpatialArmourKit.piece(c, m.rightLeg, LEG_R, 7f, 0f, -12f, 0f, dark);
        SpatialArmourKit.piece(c, m.leftLeg, LEG_L, 8f, 0f, -12f, 0f, dark);
        SpatialArmourKit.piece(c, m.body, BACK, 10f, 0f, -14f, 10f, iron);
        if (!self1p) SpatialArmourKit.piece(c, m.head, HALO, 12f, 0f, -18f, 0f, dark);

        float on = Math.min(EquipAnimation.alpha(EquipAnimation.assembled(c, 0f)), 1f);
        if (on <= 0.01f) return;
        PoseStack pose = c.pose();

        // 2. a black pool under the feet (alpha blended, before the light)
        pose.pushPose();
        pose.translate(0f, 1.46f, 0f);
        SpatialArmourKit.flat(pose, c.buffers().getBuffer(AuraRender.translucent(disc)), 0.95f * (0.6f + 0.4f * on), 0f, SpatialArmourKit.argb(0.75f * on, 0xFFFFFF), c.light());
        // 3. the pressure ring, the sigil and the fractures (additive)
        pose.translate(0f, -0.012f, 0f);
        VertexConsumer rv = c.buffers().getBuffer(AuraRender.additive(ring));
        float squeeze = 1f - ((age * 0.03f) % 1f) * 0.45f;                                  // the ring crowds inwards
        SpatialArmourKit.flat(pose, rv, 1.05f * squeeze * (0.7f + 0.3f * on), age * 0.8f, SpatialArmourKit.argb(0.8f * on * squeeze, VIOLET), FB);
        SpatialArmourKit.flat(pose, rv, 0.62f, -age * 1.4f, SpatialArmourKit.argb(0.5f * on, PALE), FB);
        pose.translate(0f, -0.004f, 0f);
        SpatialArmourKit.flat(pose, c.buffers().getBuffer(AuraRender.additive(crack)), 1.25f * (0.5f + 0.5f * on), 20f, SpatialArmourKit.argb(0.7f * on * pulse, VIOLET), FB);
        if (c.style() == 1) {
            pose.translate(0f, -0.004f, 0f);
            SpatialArmourKit.flat(pose, c.buffers().getBuffer(AuraRender.additive(sigil)), 1.9f * (0.6f + 0.4f * on), age * 0.5f, SpatialArmourKit.argb(0.8f * on, VIOLET), FB);
        }
        pose.popPose();

        // 4. falling pressure streaks and glints on the singularity
        SpatialArmourKit.camera(pose);
        VertexConsumer sv = c.buffers().getBuffer(AuraRender.additive(streak));
        int n = c.style() == 1 ? 10 : 6;
        for (int i = 0; i < n; i++) {
            float ph = (age * 0.045f + i * 0.173f) % 1f, a = i * 2.399f;
            float rr = 0.6f + 0.1f * Mth.sin(i * 3.1f), x = Mth.cos(a) * rr, z = Mth.sin(a) * rr;
            float top = -0.9f + ph * 2.2f;
            SpatialArmourKit.streak(pose, sv, x, top - 0.5f, z, x, top, z, 0.035f, SpatialArmourKit.argb(0.55f * on * Mth.sin(ph * Mth.PI), i % 2 == 0 ? VIOLET : PALE), FB);
        }
        VertexConsumer gv = c.buffers().getBuffer(AuraRender.additive(star));
        float tw = 0.5f + 0.5f * Mth.sin(age * 0.2f);
        SpatialArmourKit.glow(pose, gv, 0f, 0.15f, -0.34f, 0.12f * (0.7f + 0.6f * tw), age * 0.04f, SpatialArmourKit.argb(0.9f * on, PALE), FB);
        SpatialArmourKit.glow(pose, gv, 0f, 0.1f, 0.55f, 0.16f * (0.7f + 0.6f * tw), -age * 0.03f, SpatialArmourKit.argb(0.8f * on, VIOLET), FB);
    }
}
