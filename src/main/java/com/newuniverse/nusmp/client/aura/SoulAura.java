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
 * 1.93.0 Soul Magic: the Spirit Raiment, the temporary armour after a cast (Aura.SOUL): pale teal soul fire over bone white. A spectral hood with a bone mask, hollow glowing
 * eyes and two flames for horns; a ribcage of bone bars round a soul core; two wings of soul fire that beat; three small souls orbiting the torso; a tail of soul fire;
 * flame-headed pauldrons on bone caps, chain-ring vambraces, spectral-claw gauntlets and greaves that burn away into a flame under the feet. Every piece is a bone of
 * soul_armour.geo.json anchored on the model part it was built for; SongArmourKit draws them with EquipAnimation (the Djinn Full Equip forge-in and shatter-out).
 * Passes: 1. solid pieces (bone, cloth), cutout (translucent only while forging or shattering); 2. ghost pieces (hood, wings, souls, tail, greaves), always translucent;
 * 3. every glow map, additive (RenderType.eyes), full bright; 4. extras, additive: a rune ring on the ground (particle/soul_rune) and soul flames rising off the shoulders
 * (particle/soul_flame), camera-facing. Style is unused.
 */
public final class SoulAura {
    private SoulAura() {}

    private static final int TEAL = 0x72D9CC, PALE = 0xE6FFFA;
    private static final GeoSpec SPEC = GeoSpec.of("soul", "armour");
    private static final Piece[] PIECES = {
            SongArmourKit.ghost("hood", Anchor.HEAD, 0, 0f, -16f, 0f),
            SongArmourKit.piece("ribs", Anchor.BODY, 2, 0f, 0f, -14f),
            SongArmourKit.ghost("wing_r", Anchor.BODY, 6, -10f, -4f, 12f),
            SongArmourKit.ghost("wing_l", Anchor.BODY, 6, 10f, -4f, 12f),
            SongArmourKit.ghost("wisps", Anchor.BODY, 10, 0f, -14f, 0f),
            SongArmourKit.ghost("tail", Anchor.BODY, 8, 0f, 6f, 12f),
            SongArmourKit.piece("pauldron_r", Anchor.ARM_R, 3, -14f, -4f, 0f),
            SongArmourKit.piece("vambrace_r", Anchor.ARM_R, 5, -12f, 2f, -6f),
            SongArmourKit.piece("gauntlet_r", Anchor.ARM_R, 7, -10f, 8f, -6f),
            SongArmourKit.piece("pauldron_l", Anchor.ARM_L, 3, 14f, -4f, 0f),
            SongArmourKit.piece("vambrace_l", Anchor.ARM_L, 5, 12f, 2f, -6f),
            SongArmourKit.piece("gauntlet_l", Anchor.ARM_L, 7, 10f, 8f, -6f),
            SongArmourKit.ghost("greave_r", Anchor.LEG_R, 9, -3f, 14f, -6f),
            SongArmourKit.ghost("greave_l", Anchor.LEG_L, 10, 3f, 14f, -6f)};
    private static final SongArmourKit.Set SET = new SongArmourKit.Set(SPEC, TEAL, PIECES);
    private static ResourceLocation rune, flame, glow;

    /** Called once by AuraRegistry on the client. */
    public static void register() {
        rune = AuraRender.tex("particle/soul_rune");
        flame = AuraRender.tex("particle/soul_flame");
        glow = AuraRender.tex("particle/glow");
        PlayerAuraClient.register(Aura.SOUL, SoulAura::paint);
    }

    private static void paint(AuraContext c) {
        SongArmourKit.paint(c, SET);
        float f = c.fade();
        if (f <= 0.002f) return;
        PoseStack pose = c.pose();
        SealDraw.camera(pose);
        float age = c.age();
        VertexConsumer vc = c.buffers().getBuffer(AuraRender.additive(rune));
        pose.pushPose();                                                                      // a soul rune ring on the ground
        pose.translate(0f, 1.47f, 0f);
        SealDraw.flat(pose, vc, 0.95f * (0.6f + 0.4f * f), age * 1.1f, SealDraw.argb(0.8f * f, TEAL), PropDraw.FULL_BRIGHT);
        SealDraw.flat(pose, vc, 0.6f * (0.6f + 0.4f * f), -age * 1.8f, SealDraw.argb(0.55f * f, PALE), PropDraw.FULL_BRIGHT);
        pose.popPose();
        VertexConsumer fl = c.buffers().getBuffer(AuraRender.additive(flame));
        for (int i = 0; i < 9; i++) {                                                         // soul flames rising and guttering
            float ph = (age * 0.017f + i * 0.111f) % 1f, a = i * 2.399f + age * 0.025f, r = 0.5f + 0.15f * Mth.sin(i * 1.9f);
            float fade = Mth.sin(ph * Mth.PI);
            SealDraw.glow(pose, fl, Mth.cos(a) * r, 1.4f - ph * 2.0f, Mth.sin(a) * r * 0.8f, 0.08f + 0.06f * fade, Mth.sin(age * 0.1f + i) * 0.3f,
                    SealDraw.argb(0.85f * f * fade, i % 3 == 0 ? PALE : TEAL), PropDraw.FULL_BRIGHT);
        }
        VertexConsumer g = c.buffers().getBuffer(AuraRender.additive(glow));
        SealDraw.glow(pose, g, 0f, 0.28f, -0.12f, 0.2f + 0.04f * Mth.sin(age * 0.2f), 0f, SealDraw.argb(0.55f * f, TEAL), PropDraw.FULL_BRIGHT);       // the core glow through the ribs
    }
}
