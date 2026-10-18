package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.client.aura.AirSetKit.Anchor;
import com.newuniverse.nusmp.client.aura.AirSetKit.Piece;
import com.newuniverse.nusmp.client.prop.PropDraw;
import com.newuniverse.nusmp.client.prop.SealDraw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 1.93.0 Wing Magic: the temporary armour (Aura.WING), the Wings: feathered silver and gold with a pair of real articulated wings on the back, forged in and
 * shattered out like the Djinn Full Equip ({@link EquipAnimation}). Geo models from tools/gen_wing_rebuild.py:
 * <pre>
 *   chest     BODY  delay 0    breastplate of overlapping feather scales, a winged emblem, a glowing heart        (from the front)
 *   helm      HEAD  delay 2    a gold circlet with feathered fins at the temples and a crest plume                (from above)
 *   pauld_r/l ARM   delay 3/4  three stacked feather plates edged in gold                                         (from outside)
 *   vamb_r/l  ARM   delay 5/6  vambraces with three feathers sweeping back, gold gauntlets                        (from the front)
 *   belt      BODY  delay 6    gold belt with feather tassets                                                     (from below)
 *   back      BODY  delay 8    the wings (inner arm + outer half, ten flight feathers each, clip idle: the beat    (from behind, far: 18 px)
 *                              with the outer half lagging) and a halo of gold over the head
 *   greave_r/l LEG  delay 9/10 the winged boots                                                                   (from below)
 * </pre>
 * Passes: 1. the pieces, CUTOUT when assembled, TRANSLUCENT while forging / shattering, glow maps ADDITIVE on the back (gold edges) and the chest only;
 * 2. drifting feathers, TRANSLUCENT; 3. a halo plane behind the back and glints, ADDITIVE full bright; 4. a gold ring on the ground, ADDITIVE, last.
 * About 5000 vertices assembled.
 */
public final class WingAura {
    private WingAura() {}

    private static final String K = "wing";
    private static final Piece[] SET = {
            AirSetKit.piece(K, "chest", Anchor.BODY, 0f, 0f, 0f, -14f, true, true),
            AirSetKit.piece(K, "helm", Anchor.HEAD, 2f, 0f, -14f, 0f, true),
            AirSetKit.piece(K, "pauld_r", Anchor.ARM_R, 3f, -14f, -6f, 0f, false),
            AirSetKit.piece(K, "pauld_l", Anchor.ARM_L, 4f, 14f, -6f, 0f, false),
            AirSetKit.piece(K, "vamb_r", Anchor.ARM_R, 5f, -8f, 2f, -10f, false),
            AirSetKit.piece(K, "vamb_l", Anchor.ARM_L, 6f, 8f, 2f, -10f, false),
            AirSetKit.piece(K, "belt", Anchor.BODY, 6f, 0f, 12f, -4f, true),
            AirSetKit.piece(K, "back", Anchor.BODY, 8f, 0f, -6f, 18f, true, true),
            AirSetKit.piece(K, "greave_r", Anchor.LEG_R, 9f, -2f, 12f, -6f, false),
            AirSetKit.piece(K, "greave_l", Anchor.LEG_L, 10f, 2f, 12f, -6f, false),
    };

    private static final int GOLD = 0xFFDC7A, PALE = 0xF4F0FF;
    private static ResourceLocation down, halo, glint;

    /** Called once by AuraRegistry on the client. */
    public static void register() {
        down = AuraRender.tex("particle/wing_down");
        halo = AuraRender.tex("particle/wing_halo");
        glint = AuraRender.tex("particle/wing_glint");
        PlayerAuraClient.register(Aura.WING, WingAura::paint);
    }

    private static float frac(float x) { return x - (float) Math.floor(x); }

    private static void paint(AuraContext c) {
        float f = c.fade();
        if (f <= 0.002f) return;
        float age = c.age(), pulse = 0.75f + 0.25f * Mth.sin(age * 0.2f);
        PoseStack pose = c.pose();
        boolean self1p = AirSetKit.firstPersonSelf(c);

        AirSetKit.paint(c, SET, false, SealDraw.argb(0.85f * pulse, 0xFFFFFF), 1f);

        SealDraw.camera(pose);
        int light = SealDraw.lift(c.light(), 8);
        VertexConsumer dv = c.buffers().getBuffer(AuraRender.translucent(down));
        for (int i = 0; i < 10; i++) {                                                   // feathers coming loose and drifting down
            float ph = frac(age * 0.011f + i * 0.1f), a = i * 2.399f;
            float y = -0.5f + ph * 2.1f;
            if (self1p && y < 0.1f) continue;
            float x = Mth.cos(a) * (0.55f + 0.25f * Mth.sin(i)) + 0.18f * Mth.sin(age * 0.08f + i * 1.7f);
            SealDraw.glow(pose, dv, x, y, Mth.sin(a) * 0.6f + 0.2f, 0.1f, age * 0.03f * (i % 2 == 0 ? 1 : -1) + i, SealDraw.argb(Mth.sin(ph * Mth.PI) * 0.85f * f, PALE), light);
        }
        VertexConsumer hv = c.buffers().getBuffer(AuraRender.additive(halo));
        if (!self1p) {                                                                   // a halo standing behind the head
            pose.pushPose();
            pose.translate(0f, -0.62f, 0.34f);
            float half = 0.5f * (0.6f + 0.4f * f);
            SealDraw.plane(pose, hv, 0f, 0f, 0f, half, 0f, 0f, 0f, half, 0f, false, SealDraw.argb(0.7f * f * pulse, GOLD), PropDraw.FULL_BRIGHT);
            SealDraw.plane(pose, hv, 0f, 0f, 0f, half, 0f, 0f, 0f, half, 0f, true, SealDraw.argb(0.7f * f * pulse, GOLD), PropDraw.FULL_BRIGHT);
            pose.popPose();
        }
        VertexConsumer gv = c.buffers().getBuffer(AuraRender.additive(glint));
        for (int i = 0; i < 8; i++) {
            float tw = Mth.sin(age * 0.19f + i * 2.1f) * 0.5f + 0.5f;
            tw *= tw;
            float a = i * 0.785f + age * 0.012f, r = 0.45f + 0.15f * Mth.sin(i * 3.1f), y = 1.1f - (i % 4) * 0.5f;
            if (self1p && y < 0.1f) continue;
            SealDraw.glow(pose, gv, Mth.cos(a) * r, y, Mth.sin(a) * r + 0.1f, 0.04f + 0.09f * tw, age * 0.02f, SealDraw.argb(0.9f * f * tw, GOLD), PropDraw.FULL_BRIGHT);
        }
        pose.pushPose();
        pose.translate(0f, 1.47f, 0f);
        float grow = 0.55f + 0.45f * f;
        SealDraw.flat(pose, hv, 0.95f * grow, age * 1.1f, SealDraw.argb(0.55f * f, GOLD), PropDraw.FULL_BRIGHT);
        pose.popPose();
    }
}
