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
 * 1.93.0 Slash Magic: the temporary armour (Aura.SLASH), the Ripper's set after Jack the Ripper's mana blades: gunmetal-black slat plates scarred with green
 * cuts, a serrated mouth guard and blade fins on the cowl, spiked pauldrons, a long green-edged blade along each forearm, the Death Scythe slung across the
 * back with torn cape tatters and six blade shards orbiting, and a saw-ring of blades turning behind the head. Eight GeoLite pieces (tools/gen_slash_geo.py)
 * on the model parts they were built for, put on and taken off with {@link EquipAnimation} through {@link SpatialArmourKit}: the pieces cut IN from their own
 * side (chest from the front, helm from above, arms from the sides, the scythe and the saw-ring from behind), chest first and the saw-ring last (delays 0 to
 * 12), and shatter out over the last 18 ticks. Style 0 is the plain set; style 1 (a heavy page: the Death Scythe, Lunatic Slash) adds the sweep rings.
 * Passes (layer space: y points DOWN, the feet at y = 1.5):
 *   1. the pieces (cutout, translucent only while a piece forges or shatters) and their additive glow maps;
 *   2. the serrated ground ring turning under the feet (and a second counter-turning one for style 1), additive, full bright;
 *   3. flashes of cut marks in the air round the body and glints along the blades (additive, camera-facing, full bright).
 * For the local player in first person the head pieces and the saw-ring are left out.
 */
public final class SlashAura {
    private SlashAura() {}

    private static final GeoSpec HELM = GeoSpec.of("slash", "helm"), CHEST = GeoSpec.of("slash", "chest"), ARM_R = GeoSpec.of("slash", "arm_r"),
            ARM_L = GeoSpec.of("slash", "arm_l"), LEG_R = GeoSpec.of("slash", "leg_r"), LEG_L = GeoSpec.of("slash", "leg_l"),
            BACK = GeoSpec.of("slash", "back"), HALO = GeoSpec.of("slash", "halo");
    private static final int GREEN = 0x48FF7A, PALE = 0xD2FFE0, FB = SpatialArmourKit.FB;
    private static ResourceLocation ring, mark, glint;

    /** Called once by AuraRegistry on the client. */
    public static void register() {
        ring = AuraRender.tex("particle/slash_rip_ring");
        mark = AuraRender.tex("particle/slash_rip_mark");
        glint = AuraRender.tex("particle/slash_rip_glint");
        PlayerAuraClient.register(Aura.SLASH, SlashAura::paint);
    }

    private static void paint(AuraContext c) {
        float age = c.age(), pulse = 0.8f + 0.2f * Mth.sin(age * 0.16f);
        boolean self1p = SpatialArmourKit.self1p(c);
        PlayerModel<AbstractClientPlayer> m = c.model();
        Style steel = Style.solid(GREEN, 0.85f * pulse);
        Style dark = Style.solid(PALE, 0.5f);

        // 1. the pieces cut in from their own side
        SpatialArmourKit.piece(c, m.body, CHEST, 0f, 0f, 0f, -14f, steel);
        if (!self1p) SpatialArmourKit.piece(c, m.head, HELM, 2f, 0f, -16f, 0f, steel);
        SpatialArmourKit.piece(c, m.rightArm, ARM_R, 4f, -14f, -2f, -8f, steel);
        SpatialArmourKit.piece(c, m.leftArm, ARM_L, 5f, 14f, -2f, -8f, steel);
        SpatialArmourKit.piece(c, m.rightLeg, LEG_R, 7f, -6f, 12f, -6f, dark);
        SpatialArmourKit.piece(c, m.leftLeg, LEG_L, 8f, 6f, 12f, -6f, dark);
        SpatialArmourKit.piece(c, m.body, BACK, 10f, 0f, -6f, 14f, steel);
        if (!self1p) SpatialArmourKit.piece(c, m.head, HALO, 12f, 0f, 0f, 14f, dark);

        float on = Math.min(EquipAnimation.alpha(EquipAnimation.assembled(c, 0f)), 1f);
        if (on <= 0.01f) return;
        PoseStack pose = c.pose();

        // 2. the serrated ground rings
        pose.pushPose();
        pose.translate(0f, 1.47f, 0f);
        VertexConsumer rv = c.buffers().getBuffer(AuraRender.additive(ring));
        SpatialArmourKit.flat(pose, rv, 0.95f * (0.7f + 0.3f * on), age * 2.2f, SpatialArmourKit.argb(0.85f * on, GREEN), FB);
        if (c.style() == 1) {
            pose.translate(0f, -0.01f, 0f);
            SpatialArmourKit.flat(pose, rv, 1.45f * (0.6f + 0.4f * on), -age * 1.3f, SpatialArmourKit.argb(0.7f * on, PALE), FB);
        }
        pose.popPose();

        // 3. flashes of cut marks in the air round the body, glints along the blades
        SpatialArmourKit.camera(pose);
        VertexConsumer mv = c.buffers().getBuffer(AuraRender.additive(mark));
        int n = c.style() == 1 ? 7 : 4;
        for (int i = 0; i < n; i++) {
            float ph = (age * 0.11f + i * 0.37f) % 1f;
            if (ph > 0.35f) continue;
            float k = ph / 0.35f, a = i * 2.399f + (int) (age * 0.11f + i * 0.37f) * 1.7f;
            float r = 0.55f + 0.12f * Mth.sin(a * 3f), y = 0.2f + 0.7f * Mth.sin(a * 1.7f + i);
            if (self1p && y < -0.15f) continue;
            SpatialArmourKit.glow(pose, mv, Mth.cos(a) * r, y, Mth.sin(a) * r, 0.34f * (0.5f + 0.5f * k), a * 2f, SpatialArmourKit.argb(0.9f * on * (1f - k), GREEN), FB);
        }
        VertexConsumer gv = c.buffers().getBuffer(AuraRender.additive(glint));
        float tw = 0.5f + 0.5f * Mth.sin(age * 0.23f);
        SpatialArmourKit.glow(pose, gv, -0.66f, 1.35f, 0f, 0.2f * (0.6f + 0.8f * tw), 0f, SpatialArmourKit.argb(0.85f * on, PALE), FB);
        SpatialArmourKit.glow(pose, gv, 0.66f, 1.35f, 0f, 0.2f * (0.6f + 0.8f * (1f - tw)), 0f, SpatialArmourKit.argb(0.85f * on, PALE), FB);
    }
}
