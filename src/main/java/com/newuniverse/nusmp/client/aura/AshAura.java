package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.client.aura.RebuildSetKit.Anchor;
import com.newuniverse.nusmp.client.aura.RebuildSetKit.Piece;
import com.newuniverse.nusmp.client.prop.PropDraw;
import com.newuniverse.nusmp.client.prop.SealDraw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 1.93.0 Ash Magic: the temporary armour (Aura.ASH), the Ash Shroud: burnt charcoal plate cracked and lit by embers, heaps of pale ash and the bare branches
 * of an ash tree, forged in and shattered out like the Djinn Full Equip ({@link EquipAnimation}). Ash had no armour before; the aura shows after Ash Cloud,
 * Revelation of the Cowardly (5 s) and Ash Shroud (9 s). Geo models from tools/gen_ash_rebuild.py:
 * <pre>
 *   chest     BODY  delay 0    cracked charcoal plate over a ribcage of bare branches, an ember core, heaped ash collar  (from the front)
 *   helm      HEAD  delay 2    hood of drifting ash, ember slits, a crown of dead branches like antlers                  (from above)
 *   pauld_r/l ARM   delay 3/4  charred plate under a heap of ash, splintered branch spikes                              (from outside)
 *   vamb_r/l  ARM   delay 5/6  cracked charcoal vambraces, ash cuffs dripping, ember knuckles                           (from the front)
 *   belt      BODY  delay 6    charred lumps and hanging rags of ash                                                    (from below)
 *   back      BODY  delay 8    the ash tree: five branches a side spreading like wings, twigs, embers at the tips       (from behind)
 *   greave_r/l LEG  delay 9/10 charred boots with ash drifts at the soles                                              (from below)
 * </pre>
 * Passes: 1. the pieces, CUTOUT when assembled, TRANSLUCENT while forging / shattering, glow maps (embers in the cracks) ADDITIVE on the chest, helm and back;
 * 2. falling ash flakes, TRANSLUCENT; 3. a ring of ash on the ground, TRANSLUCENT; 4. ember sparks rising, ADDITIVE, full bright, last. About 5800 vertices assembled.
 */
public final class AshAura {
    private AshAura() {}

    private static final String K = "ash";
    private static final Piece[] SET = {
            RebuildSetKit.piece(K, "chest", Anchor.BODY, 0f, 0f, 0f, -14f, true, true),
            RebuildSetKit.piece(K, "helm", Anchor.HEAD, 2f, 0f, -14f, 0f, true, true),
            RebuildSetKit.piece(K, "pauld_r", Anchor.ARM_R, 3f, -14f, -6f, 0f, false),
            RebuildSetKit.piece(K, "pauld_l", Anchor.ARM_L, 4f, 14f, -6f, 0f, false),
            RebuildSetKit.piece(K, "vamb_r", Anchor.ARM_R, 5f, -8f, 2f, -10f, false),
            RebuildSetKit.piece(K, "vamb_l", Anchor.ARM_L, 6f, 8f, 2f, -10f, false),
            RebuildSetKit.piece(K, "belt", Anchor.BODY, 6f, 0f, 12f, -4f, true),
            RebuildSetKit.piece(K, "back", Anchor.BODY, 8f, 0f, -4f, 14f, true, true),
            RebuildSetKit.piece(K, "greave_r", Anchor.LEG_R, 9f, -2f, 12f, -6f, false),
            RebuildSetKit.piece(K, "greave_l", Anchor.LEG_L, 10f, 2f, 12f, -6f, false),
    };

    private static final int EMBER = 0xFF7A38, ASH_LO = 0x6E6C70, ASH_HI = 0xC8C6CA;
    private static ResourceLocation flake, ring, ember;

    /** Called once by AuraRegistry on the client. */
    public static void register() {
        flake = AuraRender.tex("particle/ash_flake");
        ring = AuraRender.tex("particle/ash_ring");
        ember = AuraRender.tex("particle/ash_ember");
        PlayerAuraClient.register(Aura.ASH, AshAura::paint);
    }

    private static float frac(float x) { return x - (float) Math.floor(x); }

    private static void paint(AuraContext c) {
        float f = c.fade();
        if (f <= 0.002f) return;
        float age = c.age(), pulse = 0.7f + 0.3f * Mth.sin(age * 0.19f);
        PoseStack pose = c.pose();
        boolean self1p = RebuildSetKit.firstPersonSelf(c);

        RebuildSetKit.paint(c, SET, false, SealDraw.argb(0.9f * pulse, 0xFFFFFF), 1f);

        SealDraw.camera(pose);
        int light = SealDraw.lift(c.light(), 5);
        VertexConsumer fv = c.buffers().getBuffer(AuraRender.translucent(flake));
        for (int i = 0; i < 12; i++) {                                                   // ash falling off the shroud
            float ph = frac(age * 0.014f + i * 0.0833f), a = i * 2.399f + age * 0.012f;
            float r = 0.5f + 0.2f * Mth.sin(i * 1.7f), y = -0.4f + ph * 1.9f;
            if (self1p && y < 0.1f) continue;
            SealDraw.glow(pose, fv, Mth.cos(a) * r + 0.08f * Mth.sin(age * 0.06f + i), y, Mth.sin(a) * r, 0.045f + 0.03f * Mth.sin(i * 2.7f) * Mth.sin(i * 2.7f), age * 0.04f + i,
                    SealDraw.argb(Mth.sin(ph * Mth.PI) * 0.8f * f, SealDraw.mix(ASH_LO, ASH_HI, ph)), light);
        }
        VertexConsumer rv = c.buffers().getBuffer(AuraRender.translucent(ring));
        pose.pushPose();
        pose.translate(0f, 1.47f, 0f);
        float grow = 0.55f + 0.45f * f;
        SealDraw.flat(pose, rv, 0.95f * grow, age * 0.7f, SealDraw.argb(0.55f * f, ASH_LO), light);
        pose.popPose();
        VertexConsumer ev = c.buffers().getBuffer(AuraRender.additive(ember));
        for (int i = 0; i < 9; i++) {                                                    // embers climbing the body
            float ph = frac(age * 0.03f + i * 0.111f), a = i * 2.9f + age * 0.05f, r = 0.36f + 0.2f * Mth.sin(i * 1.3f), y = 1.4f - ph * 1.9f;
            if (self1p && y < 0.1f) continue;
            SealDraw.glow(pose, ev, Mth.cos(a) * r, y, Mth.sin(a) * r, 0.045f + 0.03f * Mth.sin(ph * Mth.PI), 0f, SealDraw.argb(Mth.sin(ph * Mth.PI) * 0.95f * f, EMBER), PropDraw.FULL_BRIGHT);
        }
    }
}
