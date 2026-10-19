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
 * 1.93.0 Air Magic: the temporary armour (Aura.AIR), the Air Cushion Armour: plates of compressed air, glass clear with white streamlines and bright edges,
 * forged in and shattered out like the Djinn Full Equip ({@link EquipAnimation}). Geo models from tools/gen_air_rebuild.py (the textures carry an alpha channel,
 * so EVERY piece is drawn translucent, assembled or not):
 * <pre>
 *   chest     BODY  delay 0    thin layered plates, four compression cores on the chest                                  (from the front)
 *   helm      HEAD  delay 2    a sleek helm with a tall blade fin and cheek vanes                                        (from above)
 *   pauld_r/l ARM   delay 3/4  three blades fanned over the shoulder                                                     (from outside)
 *   vamb_r/l  ARM   delay 5/6  two long vanes along the forearm and a pressure orb in the fist                           (from the front)
 *   belt      BODY  delay 6    a fine band with five orbs floating round it (turning)                                    (from below)
 *   back      BODY  delay 8    a halo of vanes turning behind the back, five orbs orbiting, two pairs of blade vanes     (from behind)
 *   greave_r/l LEG  delay 9/10 boots with vanes and a cone of rushing air under the sole                                 (from below)
 * </pre>
 * Passes: 1. the pieces, TRANSLUCENT, glow maps ADDITIVE on the chest and the back; 2. air ripples on the ground, ADDITIVE; 3. long streaks racing up the body and glints, ADDITIVE, last.
 * About 2500 vertices assembled.
 */
public final class AirAura {
    private AirAura() {}

    private static final String K = "air";
    private static final Piece[] SET = {
            RebuildSetKit.piece(K, "chest", Anchor.BODY, 0f, 0f, 0f, -14f, true, true),
            RebuildSetKit.piece(K, "helm", Anchor.HEAD, 2f, 0f, -14f, 0f, true),
            RebuildSetKit.piece(K, "pauld_r", Anchor.ARM_R, 3f, -14f, -6f, 0f, false),
            RebuildSetKit.piece(K, "pauld_l", Anchor.ARM_L, 4f, 14f, -6f, 0f, false),
            RebuildSetKit.piece(K, "vamb_r", Anchor.ARM_R, 5f, -8f, 2f, -10f, false),
            RebuildSetKit.piece(K, "vamb_l", Anchor.ARM_L, 6f, 8f, 2f, -10f, false),
            RebuildSetKit.piece(K, "belt", Anchor.BODY, 6f, 0f, 12f, -4f, true),
            RebuildSetKit.piece(K, "back", Anchor.BODY, 8f, 0f, -4f, 14f, true, true),
            RebuildSetKit.piece(K, "greave_r", Anchor.LEG_R, 9f, -2f, 12f, -6f, false),
            RebuildSetKit.piece(K, "greave_l", Anchor.LEG_L, 10f, 2f, 12f, -6f, false),
    };

    private static final int CYAN = 0x9FE8FF, WHITE = 0xF2FFFF;
    private static ResourceLocation ring, streak, glint;

    /** Called once by AuraRegistry on the client. */
    public static void register() {
        ring = AuraRender.tex("particle/air_ring");
        streak = AuraRender.tex("particle/air_streak");
        glint = AuraRender.tex("particle/air_glint");
        PlayerAuraClient.register(Aura.AIR, AirAura::paint);
    }

    private static float frac(float x) { return x - (float) Math.floor(x); }

    private static void paint(AuraContext c) {
        float f = c.fade();
        if (f <= 0.002f) return;
        float age = c.age(), pulse = 0.75f + 0.25f * Mth.sin(age * 0.2f);
        PoseStack pose = c.pose();
        boolean self1p = RebuildSetKit.firstPersonSelf(c);

        RebuildSetKit.paint(c, SET, true, SealDraw.argb(0.8f * pulse, 0xFFFFFF), 0.85f);

        SealDraw.camera(pose);
        VertexConsumer rv = c.buffers().getBuffer(AuraRender.additive(ring));
        pose.pushPose();
        pose.translate(0f, 1.47f, 0f);
        float grow = 0.55f + 0.45f * f;
        for (int k = 0; k < 3; k++) {                                                    // ripples spreading from the feet
            float ph = frac(age * 0.025f + k / 3f);
            SealDraw.flat(pose, rv, (0.3f + 0.8f * ph) * grow, 0f, SealDraw.argb((1f - ph) * 0.6f * f, CYAN), PropDraw.FULL_BRIGHT);
        }
        pose.popPose();
        VertexConsumer sv = c.buffers().getBuffer(AuraRender.additive(streak));
        for (int i = 0; i < 6; i++) {                                                    // long streaks of air racing up the body
            float ph = frac(age * 0.035f + i * 0.167f), a = i * 1.047f + age * 0.02f, r = 0.5f + 0.06f * Mth.sin(i * 2f);
            float y1 = 1.4f - ph * 2.1f;
            if (self1p && y1 < 0.1f) continue;
            float x = Mth.cos(a) * r, z = Mth.sin(a) * r;
            SealDraw.ribbon(pose, sv, x, y1 + 0.5f, z, x, y1, z, 0.035f, 0f, 1f, SealDraw.argb(Mth.sin(ph * Mth.PI) * 0.7f * f, WHITE), PropDraw.FULL_BRIGHT);
        }
        VertexConsumer gv = c.buffers().getBuffer(AuraRender.additive(glint));
        for (int i = 0; i < 6; i++) {
            float tw = Mth.sin(age * 0.2f + i * 2.1f) * 0.5f + 0.5f;
            tw *= tw;
            float a = i * 1.047f + age * 0.015f, r = 0.45f, y = 1.1f - (i % 3) * 0.55f;
            if (self1p && y < 0.1f) continue;
            SealDraw.glow(pose, gv, Mth.cos(a) * r, y, Mth.sin(a) * r, 0.04f + 0.09f * tw, age * 0.02f, SealDraw.argb(0.9f * f * tw, WHITE), PropDraw.FULL_BRIGHT);
        }
    }
}
