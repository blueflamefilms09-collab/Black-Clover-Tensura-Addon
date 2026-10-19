package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.client.aura.SongArmourKit.Anchor;
import com.newuniverse.nusmp.client.aura.SongArmourKit.Piece;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.client.prop.PropDraw;
import com.newuniverse.nusmp.client.prop.SealDraw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 1.93.0 Sound Magic: the Resonance Plate, the temporary armour after a cast (Aura.SOUND): brushed steel and black rubber with cyan light. Helm (headphones with
 * speaker-cone ear cups, a mic boom, two antenna fins), chest (an amp plate: speaker grille, VU meter strip, knobs), three arcs of sound behind the back that pulse
 * outward one after the other, a coiled cable tail with a plug, speaker-stack pauldrons, tuning-fork vambraces, resonator-ring gauntlets and bass-drum greaves with a ring of light
 * at the ankle. Every piece is a bone of sound_armour.geo.json anchored on the model part it was built for; SongArmourKit draws them with EquipAnimation (the Djinn
 * Full Equip forge-in and shatter-out). Passes: 1. solid pieces, cutout (translucent only while forging or shattering); 2. every glow map, additive (RenderType.eyes),
 * full bright; 3. extras, additive: sound rings (particle/sound_wave) running out of the chest and a ring on the ground, camera-facing glints. Style is unused.
 */
public final class SoundAura {
    private SoundAura() {}

    private static final int CYAN = 0x70C8F0, PALE = 0xD8F4FF;
    private static final GeoSpec SPEC = GeoSpec.of("sound", "armour");
    private static final Piece[] PIECES = {
            SongArmourKit.head("helm", 0, 0f, -16f, 0f),
            SongArmourKit.piece("chest", Anchor.BODY, 2, 0f, 0f, -14f),
            SongArmourKit.piece("wave_a", Anchor.BODY, 6, 0f, -4f, 14f),
            SongArmourKit.piece("wave_b", Anchor.BODY, 8, 0f, -4f, 16f),
            SongArmourKit.piece("wave_c", Anchor.BODY, 10, 0f, -4f, 18f),
            SongArmourKit.piece("cable", Anchor.BODY, 9, 0f, 6f, 12f),
            SongArmourKit.piece("pauldron_r", Anchor.ARM_R, 3, -14f, -4f, 0f),
            SongArmourKit.piece("vambrace_r", Anchor.ARM_R, 5, -12f, 2f, -6f),
            SongArmourKit.piece("gauntlet_r", Anchor.ARM_R, 7, -10f, 8f, -6f),
            SongArmourKit.piece("pauldron_l", Anchor.ARM_L, 3, 14f, -4f, 0f),
            SongArmourKit.piece("vambrace_l", Anchor.ARM_L, 5, 12f, 2f, -6f),
            SongArmourKit.piece("gauntlet_l", Anchor.ARM_L, 7, 10f, 8f, -6f),
            SongArmourKit.piece("greave_r", Anchor.LEG_R, 9, -3f, 14f, -6f),
            SongArmourKit.piece("greave_l", Anchor.LEG_L, 10, 3f, 14f, -6f)};
    private static final SongArmourKit.Set SET = new SongArmourKit.Set(SPEC, CYAN, PIECES);
    private static ResourceLocation wave, glow;

    /** Called once by AuraRegistry on the client. */
    public static void register() {
        wave = AuraRender.tex("particle/sound_wave");
        glow = AuraRender.tex("particle/glow");
        PlayerAuraClient.register(Aura.SOUND, SoundAura::paint);
    }

    private static void paint(AuraContext c) {
        SongArmourKit.paint(c, SET);
        float f = c.fade();
        if (f <= 0.002f) return;
        PoseStack pose = c.pose();
        SealDraw.camera(pose);
        float age = c.age();
        VertexConsumer vc = c.buffers().getBuffer(AuraRender.additive(wave));
        pose.pushPose();                                                                      // a sound ring on the ground, breathing outward
        pose.translate(0f, 1.47f, 0f);
        for (int k = 0; k < 2; k++) {
            float ph = ((age * 0.03f) + k * 0.5f) % 1f;
            SealDraw.flat(pose, vc, 0.5f + 0.7f * ph, age * 0.5f * (k == 0 ? 1 : -1), SealDraw.argb(0.7f * f * (1f - ph), k == 0 ? CYAN : PALE), PropDraw.FULL_BRIGHT);
        }
        pose.popPose();
        VertexConsumer g = c.buffers().getBuffer(AuraRender.additive(glow));
        for (int i = 0; i < 8; i++) {                                                         // glints rising off the speaker pauldrons
            float ph = (age * 0.02f + i * 0.125f) % 1f, a = i * 2.399f + age * 0.03f;
            float side = (i % 2 == 0 ? -1f : 1f) * 0.42f, fade = Mth.sin(ph * Mth.PI);
            SealDraw.glow(pose, g, side + Mth.cos(a) * 0.12f, 0.1f - ph * 0.8f, Mth.sin(a) * 0.12f, 0.04f + 0.035f * fade, age * 0.1f + i,
                    SealDraw.argb(0.8f * f * fade, i % 3 == 0 ? PALE : CYAN), PropDraw.FULL_BRIGHT);
        }
    }
}
