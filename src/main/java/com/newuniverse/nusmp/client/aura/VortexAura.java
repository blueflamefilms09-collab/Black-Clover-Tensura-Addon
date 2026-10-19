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
 * 1.93.0 Vortex Magic: the temporary armour (Aura.VORTEX), the Whirling Mantle: teal steel and rings of wind that never stop turning, forged in and shattered
 * out like the Djinn Full Equip ({@link EquipAnimation}). Geo models from tools/gen_vortex_rebuild.py:
 * <pre>
 *   chest     BODY  delay 0    plate with a spiral emblem of concentric rings and a glowing eye, wind-wrapped shoulders  (from the front)
 *   helm      HEAD  delay 2    brow band, a cone of turning rings as a crest, two swept swirl fins                      (from above)
 *   pauld_r/l ARM   delay 3/4  spiral shells of stacked turning plates                                                   (from outside)
 *   vamb_r/l  ARM   delay 5/6  two tilted wind rings turning round the forearm, cyclone gauntlets                       (from the front)
 *   belt      BODY  delay 6    three tilted rings whirling round the waist                                              (from below)
 *   back      BODY  delay 8    the mantle: three bands of wind turning round the body at three speeds, a tail of rings   (from behind)
 *   greave_r/l LEG  delay 9/10 spiral boots with a turning ring at the ankle and swept vanes                           (from below)
 * </pre>
 * Passes: 1. the pieces, CUTOUT when assembled, TRANSLUCENT while forging / shattering, glow map ADDITIVE on the chest only; 2. dust
 * spiralling up the body, TRANSLUCENT; 3. a spiral turning on the ground, ADDITIVE, 3 cm up; 4. wind streaks winding up the body, ADDITIVE, last. About 7000
 * vertices assembled.
 */
public final class VortexAura {
    private VortexAura() {}

    private static final String K = "vortex";
    private static final Piece[] SET = {
            RebuildSetKit.piece(K, "chest", Anchor.BODY, 0f, 0f, 0f, -14f, true, true),
            RebuildSetKit.piece(K, "helm", Anchor.HEAD, 2f, 0f, -14f, 0f, true),
            RebuildSetKit.piece(K, "pauld_r", Anchor.ARM_R, 3f, -14f, -6f, 0f, false),
            RebuildSetKit.piece(K, "pauld_l", Anchor.ARM_L, 4f, 14f, -6f, 0f, false),
            RebuildSetKit.piece(K, "vamb_r", Anchor.ARM_R, 5f, -8f, 2f, -10f, false),
            RebuildSetKit.piece(K, "vamb_l", Anchor.ARM_L, 6f, 8f, 2f, -10f, false),
            RebuildSetKit.piece(K, "belt", Anchor.BODY, 6f, 0f, 12f, -4f, true),
            RebuildSetKit.piece(K, "back", Anchor.BODY, 8f, 0f, -4f, 14f, true),
            RebuildSetKit.piece(K, "greave_r", Anchor.LEG_R, 9f, -2f, 12f, -6f, false),
            RebuildSetKit.piece(K, "greave_l", Anchor.LEG_L, 10f, 2f, 12f, -6f, false),
    };

    private static final int TEAL = 0x59B8C9, PALE = 0xCFFAFF;
    private static ResourceLocation dust, spiral, streak;

    /** Called once by AuraRegistry on the client. */
    public static void register() {
        dust = AuraRender.tex("particle/vortex_dust");
        spiral = AuraRender.tex("particle/vortex_spiral");
        streak = AuraRender.tex("particle/vortex_streak");
        PlayerAuraClient.register(Aura.VORTEX, VortexAura::paint);
    }

    private static float frac(float x) { return x - (float) Math.floor(x); }

    private static void paint(AuraContext c) {
        float f = c.fade();
        if (f <= 0.002f) return;
        float age = c.age(), pulse = 0.75f + 0.25f * Mth.sin(age * 0.2f);
        PoseStack pose = c.pose();
        boolean self1p = RebuildSetKit.firstPersonSelf(c);

        RebuildSetKit.paint(c, SET, false, SealDraw.argb(0.85f * pulse, 0xFFFFFF), 1f);

        SealDraw.camera(pose);
        int light = SealDraw.lift(c.light(), 8);
        VertexConsumer dv = c.buffers().getBuffer(AuraRender.translucent(dust));
        for (int i = 0; i < 9; i++) {                                                    // dust whirled up the body
            float ph = frac(age * 0.02f + i * 0.111f), a = i * 2.399f + ph * 7f + age * 0.06f;
            float r = 0.62f * (1f - 0.45f * ph) + 0.05f * Mth.sin(i), y = 1.45f - ph * 2.1f;
            if (self1p && y < 0.1f) continue;
            SealDraw.glow(pose, dv, Mth.cos(a) * r, y, Mth.sin(a) * r, 0.08f + 0.1f * ph, age * 0.05f + i, SealDraw.argb(Mth.sin(ph * Mth.PI) * 0.4f * f, SealDraw.mix(TEAL, PALE, ph)), light);
        }
        VertexConsumer sv = c.buffers().getBuffer(AuraRender.additive(spiral));
        pose.pushPose();
        pose.translate(0f, 1.47f, 0f);
        float grow = 0.55f + 0.45f * f;
        SealDraw.flat(pose, sv, 1.0f * grow, age * 2.6f, SealDraw.argb(0.7f * f, TEAL), PropDraw.FULL_BRIGHT);
        SealDraw.flat(pose, sv, 0.6f * grow, -age * 4.0f, SealDraw.argb(0.5f * f * pulse, PALE), PropDraw.FULL_BRIGHT);
        pose.popPose();
        VertexConsumer wv = c.buffers().getBuffer(AuraRender.additive(streak));
        for (int s = 0; s < 3; s++) {                                                    // three wind streaks winding up the body
            float px = 0, py = 0, pz = 0;
            for (int i = 0; i <= 6; i++) {
                float t = i / 6f, phi = age * 0.12f + t * 6.0f + s * 2.094f, r = 0.55f + 0.1f * Mth.sin(t * 3f);
                float x = Mth.cos(phi) * r, y = 1.4f - t * 1.9f, z = Mth.sin(phi) * r;
                if (i > 0 && !(self1p && y < 0.1f)) SealDraw.ribbon(pose, wv, px, py, pz, x, y, z, 0.05f, (i - 1) / 6f, i / 6f, SealDraw.argb((0.2f + 0.7f * t) * f, SealDraw.mix(TEAL, PALE, t)), PropDraw.FULL_BRIGHT);
                px = x; py = y; pz = z;
            }
        }
    }
}
