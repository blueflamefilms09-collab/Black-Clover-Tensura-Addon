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
 * 1.93.0 Spatial Magic: the temporary armour (Aura.SPATIAL), the Fallen Angel Gate set: pearl-lilac plates with violet void, a little gold and portal
 * rings on the shoulders, hips and knees, the gate wheel turning behind the back and a portal halo over the head. Eight GeoLite pieces
 * (tools/gen_spatial_geo.py), each anchored on the model part it was built for and put on and taken off with {@link EquipAnimation} through
 * {@link SpatialArmourKit}: chest first, then the helm, arms, legs, and last the back wheel and the halo (delays 0 to 12 ticks), shattering out over
 * the last 18 ticks. Style 0 is the plain set; style 1 (a signature page) adds the ground sigil and the rising motes.
 * Passes, in draw order (layer space: y points DOWN, the feet are at y = 1.5):
 *   1. the solid pieces (cutout, translucent only while a piece forges or shatters), each followed by its additive glow map;
 *   2. the ground gate ring under the feet (additive, full bright), a second counter-turning ring and, for style 1, the sigil;
 *   3. glints on the gate discs and motes falling into the ground (additive, camera-facing, full bright).
 * For the local player in first person the head pieces and the halo are left out.
 */
public final class SpatialAura {
    private SpatialAura() {}

    private static final GeoSpec HELM = GeoSpec.of("spatial", "helm"), CHEST = GeoSpec.of("spatial", "chest"), ARM_R = GeoSpec.of("spatial", "arm_r"),
            ARM_L = GeoSpec.of("spatial", "arm_l"), LEG_R = GeoSpec.of("spatial", "leg_r"), LEG_L = GeoSpec.of("spatial", "leg_l"),
            BACK = GeoSpec.of("spatial", "back"), HALO = GeoSpec.of("spatial", "halo");
    private static final int VIOLET = 0xB088FF, PALE = 0xE6DCFF;
    private static ResourceLocation ring, star, mote, sigil;

    /** Called once by AuraRegistry on the client. */
    public static void register() {
        ring = AuraRender.tex("particle/spatial_ring");
        star = AuraRender.tex("particle/spatial_star");
        mote = AuraRender.tex("particle/spatial_mote");
        sigil = AuraRender.tex("particle/spatial_sigil");
        PlayerAuraClient.register(Aura.SPATIAL, SpatialAura::paint);
    }

    private static void paint(AuraContext c) {
        float age = c.age(), pulse = 0.8f + 0.2f * Mth.sin(age * 0.14f);
        boolean self1p = SpatialArmourKit.self1p(c);
        PlayerModel<AbstractClientPlayer> m = c.model();
        Style plate = Style.solid(VIOLET, 0.85f * pulse);
        Style trim = Style.solid(PALE, 0.7f);

        // 1. the pieces: chest, helm, arms, legs, then the wheel and the halo
        SpatialArmourKit.piece(c, m.body, CHEST, 0f, 0f, 0f, -14f, plate);
        if (!self1p) SpatialArmourKit.piece(c, m.head, HELM, 2f, 0f, -16f, 0f, plate);
        SpatialArmourKit.piece(c, m.rightArm, ARM_R, 4f, -12f, -2f, -6f, plate);
        SpatialArmourKit.piece(c, m.leftArm, ARM_L, 5f, 12f, -2f, -6f, plate);
        SpatialArmourKit.piece(c, m.rightLeg, LEG_R, 7f, -3f, 14f, -6f, trim);
        SpatialArmourKit.piece(c, m.leftLeg, LEG_L, 8f, 3f, 14f, -6f, trim);
        SpatialArmourKit.piece(c, m.body, BACK, 10f, 0f, 0f, 14f, plate);
        if (!self1p) SpatialArmourKit.piece(c, m.head, HALO, 12f, 0f, -16f, 0f, trim);

        // 2. the ground gate rings (and the sigil of a signature cast)
        float on = Math.min(EquipAnimation.alpha(EquipAnimation.assembled(c, 0f)), 1f);
        if (on <= 0.01f) return;
        PoseStack pose = c.pose();
        VertexConsumer vc = c.buffers().getBuffer(AuraRender.additive(ring));
        pose.pushPose();
        pose.translate(0f, 1.47f, 0f);
        SpatialArmourKit.flat(pose, vc, 0.9f * (0.7f + 0.3f * on), age * 1.6f, SpatialArmourKit.argb(0.9f * on, VIOLET), SpatialArmourKit.FB);
        SpatialArmourKit.flat(pose, vc, 0.62f * (0.7f + 0.3f * on), -age * 2.4f, SpatialArmourKit.argb(0.7f * on, PALE), SpatialArmourKit.FB);
        if (c.style() == 1) {
            VertexConsumer sv = c.buffers().getBuffer(AuraRender.additive(sigil));
            pose.translate(0f, -0.01f, 0f);
            SpatialArmourKit.flat(pose, sv, 1.6f * (0.6f + 0.4f * on), age * 0.7f, SpatialArmourKit.argb(0.8f * on, VIOLET), SpatialArmourKit.FB);
        }
        pose.popPose();

        // 3. glints on the gate discs and motes falling into the ground
        SpatialArmourKit.camera(pose);
        VertexConsumer gv = c.buffers().getBuffer(AuraRender.additive(star));
        float[][] glints = {{0f, 0.25f, -0.30f, 0.16f}, {-0.64f, -0.30f, 0f, 0.12f}, {0.64f, -0.30f, 0f, 0.12f}, {0f, 0.2f, 0.46f, 0.30f}};
        for (int i = 0; i < glints.length; i++) {
            float tw = Mth.sin(age * 0.17f + i * 1.9f) * 0.5f + 0.5f;
            tw *= tw;
            float[] g = glints[i];
            SpatialArmourKit.glow(pose, gv, g[0], g[1], g[2], g[3] * (0.6f + 0.8f * tw), age * 0.03f + i, SpatialArmourKit.argb(0.9f * on * (0.35f + 0.65f * tw), PALE), SpatialArmourKit.FB);
        }
        VertexConsumer mv = c.buffers().getBuffer(AuraRender.additive(mote));
        int n = c.style() == 1 ? 10 : 6;
        for (int i = 0; i < n; i++) {
            float ph = (age * 0.025f + i * 0.137f) % 1f, a = i * 2.399f + age * 0.02f;
            float y = -0.4f + ph * 1.85f, r = 0.55f * (1f - ph * 0.7f);
            if (self1p && y < 0.1f) continue;
            SpatialArmourKit.glow(pose, mv, Mth.cos(a) * r, y, Mth.sin(a) * r, 0.05f + 0.04f * (1f - ph), age * 0.1f + i,
                    SpatialArmourKit.argb(0.8f * on * Mth.sin(ph * Mth.PI), i % 2 == 0 ? VIOLET : PALE), SpatialArmourKit.FB);
        }
    }
}
