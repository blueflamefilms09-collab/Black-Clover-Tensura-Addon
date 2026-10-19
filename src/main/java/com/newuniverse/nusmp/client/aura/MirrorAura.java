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
 * 1.93.0 Mirror Magic: the temporary armour (Aura.MIRROR), the Glass Garb after Gauche Adlai's mirrors: silver-white glass in violet-silver frames: a crown of
 * five mirror panes, an oval mirror breastplate with panes standing at the shoulders, a large angled pane on each pauldron and framed panes on the forearms,
 * shin panes and round knee mirrors, a fan of seven floating mirrors behind the back and a ring of shards circling the head. Eight GeoLite pieces
 * (tools/gen_mirror_geo.py) on the model parts they were built for, put on and taken off with {@link EquipAnimation} through {@link SpatialArmourKit}: the glass
 * is a GHOST set (always translucent), and every piece steps IN out of the glass from the front or its own side (chest first, the fan and the ring last, delays 0
 * to 12) and shatters out over the last 18 ticks. Style 0 is the plain set; style 1 (a heavy page) adds the great ground sigil and more prism streaks.
 * Passes (layer space: y points DOWN, the feet at y = 1.5):
 *   1. the glass pieces (translucent) and their additive glow maps (rims, gems, frame edges);
 *   2. the ground ring and, for style 1, the sigil (additive, full bright), and prism streaks fanned from the breast mirror (alpha over additive);
 *   3. glints that travel over the panes (additive, camera-facing, full bright).
 * For the local player in first person the head pieces and the halo are left out. The Real Doubles wear the same pieces (client.MirrorDoubleGarbLayer).
 */
public final class MirrorAura {
    private MirrorAura() {}

    private static final GeoSpec HELM = GeoSpec.of("mirror", "helm"), CHEST = GeoSpec.of("mirror", "chest"), ARM_R = GeoSpec.of("mirror", "arm_r"),
            ARM_L = GeoSpec.of("mirror", "arm_l"), LEG_R = GeoSpec.of("mirror", "leg_r"), LEG_L = GeoSpec.of("mirror", "leg_l"),
            BACK = GeoSpec.of("mirror", "back"), HALO = GeoSpec.of("mirror", "halo");
    private static final int VIOLET = 0xB89CFF, SILVER = 0xE6EEFF, FB = SpatialArmourKit.FB;
    private static ResourceLocation ring, sigil, glint, prism;

    /** Called once by AuraRegistry on the client. */
    public static void register() {
        ring = AuraRender.tex("particle/mirror_reflex_ring");
        sigil = AuraRender.tex("particle/mirror_reflex_sigil");
        glint = AuraRender.tex("particle/mirror_reflex_glint");
        prism = AuraRender.tex("particle/mirror_reflex_prism");
        PlayerAuraClient.register(Aura.MIRROR, MirrorAura::paint);
    }

    /** The glass set's draw style: translucent, glow tinted by the colour. */
    static Style glass(int glowRgb, float k) { return Style.ghost(1f, glowRgb, k); }

    private static void paint(AuraContext c) {
        float age = c.age(), pulse = 0.8f + 0.2f * Mth.sin(age * 0.13f);
        boolean self1p = SpatialArmourKit.self1p(c);
        PlayerModel<AbstractClientPlayer> m = c.model();
        Style violet = glass(VIOLET, 0.85f * pulse), silver = glass(SILVER, 0.7f);

        // 1. the glass pieces step out of the mirror
        SpatialArmourKit.piece(c, m.body, CHEST, 0f, 0f, 0f, -14f, violet);
        if (!self1p) SpatialArmourKit.piece(c, m.head, HELM, 2f, 0f, -12f, -8f, violet);
        SpatialArmourKit.piece(c, m.rightArm, ARM_R, 4f, -12f, 0f, -10f, silver);
        SpatialArmourKit.piece(c, m.leftArm, ARM_L, 5f, 12f, 0f, -10f, silver);
        SpatialArmourKit.piece(c, m.rightLeg, LEG_R, 7f, -4f, 10f, -8f, silver);
        SpatialArmourKit.piece(c, m.leftLeg, LEG_L, 8f, 4f, 10f, -8f, silver);
        SpatialArmourKit.piece(c, m.body, BACK, 10f, 0f, 0f, 14f, violet);
        if (!self1p) SpatialArmourKit.piece(c, m.head, HALO, 12f, 0f, -14f, 0f, silver);

        float on = Math.min(EquipAnimation.alpha(EquipAnimation.assembled(c, 0f)), 1f);
        if (on <= 0.01f) return;
        PoseStack pose = c.pose();

        // 2. the ground ring (and the sigil of a heavy page), prism streaks from the breast mirror
        pose.pushPose();
        pose.translate(0f, 1.47f, 0f);
        SpatialArmourKit.flat(pose, c.buffers().getBuffer(AuraRender.additive(ring)), 0.9f * (0.7f + 0.3f * on), age * 1.2f, SpatialArmourKit.argb(0.85f * on, VIOLET), FB);
        if (c.style() == 1) {
            pose.translate(0f, -0.01f, 0f);
            SpatialArmourKit.flat(pose, c.buffers().getBuffer(AuraRender.additive(sigil)), 1.7f * (0.6f + 0.4f * on), -age * 0.6f, SpatialArmourKit.argb(0.8f * on, SILVER), FB);
        }
        pose.popPose();
        SpatialArmourKit.camera(pose);
        VertexConsumer pv = c.buffers().getBuffer(AuraRender.translucent(prism));
        int rays = c.style() == 1 ? 5 : 3;
        for (int i = 0; i < rays; i++) {
            float ph = (age * 0.05f + i * 0.31f) % 1f, a = (i - (rays - 1) / 2f) * 0.55f + 0.3f * Mth.sin(age * 0.07f + i);
            float len = 0.8f + 0.5f * ph;
            float x0 = 0f, y0 = 0.25f, z0 = -0.36f;
            SpatialArmourKit.streak(pose, pv, x0, y0, z0, x0 + Mth.sin(a) * len, y0 - 0.2f - 0.4f * ph, z0 - Mth.cos(a) * len * 0.6f, 0.07f,
                    SpatialArmourKit.argb(0.6f * on * Mth.sin(ph * Mth.PI), 0xFFFFFF), FB);
        }

        // 3. glints travelling over the panes
        VertexConsumer gv = c.buffers().getBuffer(AuraRender.additive(glint));
        for (int i = 0; i < 5; i++) {
            float ph = (age * 0.04f + i * 0.2f) % 1f, a = i * 2.399f;
            float x = Mth.cos(a) * 0.42f, y = 1.1f - ph * 2.3f, z = Mth.sin(a) * 0.4f;
            if (self1p && y < -0.1f) continue;
            SpatialArmourKit.glow(pose, gv, x, y, z, 0.1f + 0.1f * Mth.sin(ph * Mth.PI), age * 0.05f + i, SpatialArmourKit.argb(0.9f * on * Mth.sin(ph * Mth.PI), SILVER), FB);
        }
    }
}
