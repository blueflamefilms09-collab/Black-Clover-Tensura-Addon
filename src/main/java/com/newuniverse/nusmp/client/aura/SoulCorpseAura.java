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
 * 1.93.0 Soul Corpse Magic: the Marionette Shell, the temporary armour after a cast (Aura.SOUL_CORPSE): a puppet's body of stitched leather, burlap, bone and rusted
 * iron under slate blue-grey, with strings of pale violet light. A stitched cowl over a bone skull mask with hollow glowing eyes and a nail through the crown; a leather corset
 * with bone ribs and coffin nails; a coffin lid on the back; the puppeteer's control bar floating above the head with the strings that run down to the head and the shoulders; a
 * hanging chain with a hook; slate pauldrons with iron spikes, bandaged vambraces with coffin nails, skeletal-hand gauntlets and bandaged greaves with chain loops. Every piece
 * is a bone of soulcorpse_armour.geo.json anchored on the model part it was built for; SongArmourKit draws them with EquipAnimation (the Djinn Full Equip forge-in and
 * shatter-out). Passes: 1. solid pieces, cutout (translucent only while forging or shattering); 2. every glow map, additive (RenderType.eyes), full bright; 3. extras, additive:
 * a stitched sigil on the ground (particle/scorpse_sigil) and strings of light from the control bar to both hands (particle/scorpse_thread), camera-facing.
 */
public final class SoulCorpseAura {
    private SoulCorpseAura() {}

    private static final int THREAD = 0xA8B4F0, PALE = 0xE8ECFF;
    private static final GeoSpec SPEC = GeoSpec.of("soulcorpse", "armour");
    private static final Piece[] PIECES = {
            SongArmourKit.head("cowl", 0, 0f, -16f, 0f),
            SongArmourKit.piece("corset", Anchor.BODY, 2, 0f, 0f, -14f),
            SongArmourKit.piece("lid", Anchor.BODY, 5, 0f, -4f, 14f),
            SongArmourKit.piece("bar", Anchor.BODY, 11, 0f, -14f, 0f),
            SongArmourKit.piece("chain", Anchor.BODY, 8, 0f, 6f, 12f),
            SongArmourKit.piece("pauldron_r", Anchor.ARM_R, 3, -14f, -4f, 0f),
            SongArmourKit.piece("vambrace_r", Anchor.ARM_R, 5, -12f, 2f, -6f),
            SongArmourKit.piece("gauntlet_r", Anchor.ARM_R, 7, -10f, 8f, -6f),
            SongArmourKit.piece("pauldron_l", Anchor.ARM_L, 3, 14f, -4f, 0f),
            SongArmourKit.piece("vambrace_l", Anchor.ARM_L, 5, 12f, 2f, -6f),
            SongArmourKit.piece("gauntlet_l", Anchor.ARM_L, 7, 10f, 8f, -6f),
            SongArmourKit.piece("greave_r", Anchor.LEG_R, 9, -3f, 14f, -6f),
            SongArmourKit.piece("greave_l", Anchor.LEG_L, 10, 3f, 14f, -6f)};
    private static final SongArmourKit.Set SET = new SongArmourKit.Set(SPEC, THREAD, PIECES);
    private static ResourceLocation sigil, thread, glow;

    /** Called once by AuraRegistry on the client. */
    public static void register() {
        sigil = AuraRender.tex("particle/scorpse_sigil");
        thread = AuraRender.tex("particle/scorpse_thread");
        glow = AuraRender.tex("particle/glow");
        PlayerAuraClient.register(Aura.SOUL_CORPSE, SoulCorpseAura::paint);
    }

    private static void paint(AuraContext c) {
        SongArmourKit.paint(c, SET);
        float f = c.fade();
        if (f <= 0.002f) return;
        PoseStack pose = c.pose();
        SealDraw.camera(pose);
        float age = c.age();
        VertexConsumer vc = c.buffers().getBuffer(AuraRender.additive(sigil));
        pose.pushPose();                                                                      // the stitched sigil on the ground
        pose.translate(0f, 1.47f, 0f);
        SealDraw.flat(pose, vc, 0.95f * (0.6f + 0.4f * f), age * 0.8f, SealDraw.argb(0.8f * f, THREAD), PropDraw.FULL_BRIGHT);
        pose.popPose();
        // strings from the control bar (about 0.6 blocks above the head: y = -0.55 in layer space, which points down) to both wrists, swaying with the arms
        VertexConsumer th = c.buffers().getBuffer(AuraRender.additive(thread));
        float bob = Mth.sin(age * 0.1f) * 0.03f;
        for (int side = -1; side <= 1; side += 2) {
            float wx = side * 0.38f + Mth.sin(age * 0.07f + side) * 0.02f;
            SealDraw.ribbon(pose, th, side * 0.45f, -0.62f + bob, 0f, wx, 0.55f, -0.08f, 0.018f, 0f, 1f, SealDraw.argb(0.65f * f, PALE), PropDraw.FULL_BRIGHT);
        }
        VertexConsumer g = c.buffers().getBuffer(AuraRender.additive(glow));
        for (int i = 0; i < 6; i++) {                                                         // pale motes falling off the strings
            float ph = (age * 0.015f + i * 0.17f) % 1f, a = i * 2.399f;
            SealDraw.glow(pose, g, Mth.cos(a) * 0.45f, -0.6f + ph * 1.9f, Mth.sin(a) * 0.3f, 0.035f, age * 0.1f + i, SealDraw.argb(0.7f * f * Mth.sin(ph * Mth.PI), THREAD), PropDraw.FULL_BRIGHT);
        }
    }
}
