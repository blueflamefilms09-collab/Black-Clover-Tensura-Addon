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
 * 1.93.0 Snow Magic: the temporary armour (Aura.SNOW), the Snow Mantle: packed snow, fur and ice, forged in and shattered out like the Djinn Full Equip
 * ({@link EquipAnimation}). Geo models from tools/gen_snow_rebuild.py:
 * <pre>
 *   chest     BODY  delay 0    drift-white plate with a six-armed ice snowflake and a glowing core, fur collar       (from the front)
 *   helm      HEAD  delay 2    snow hood with a fur brow band and a crown of five icicles                            (from above)
 *   pauld_r/l ARM   delay 3/4  snow drifts with three icicle spears each                                             (from outside)
 *   vamb_r/l  ARM   delay 5/6  fur cuffs and ice gauntlets with frost thorns                                         (from the front)
 *   belt      BODY  delay 6    fur belt with hanging icicles                                                         (from below)
 *   back      BODY  delay 8    a snowflake halo of ice turning behind the back and a cape of icicles                 (from behind)
 *   greave_r/l LEG  delay 9/10 snow boots with fur cuffs and ice shin spikes                                         (from below)
 * </pre>
 * Passes in draw order: 1. the pieces, CUTOUT when assembled, TRANSLUCENT while forging / shattering, the glow maps (cold blue seams, the core) ADDITIVE on the
 * chest and the back only; 2. falling snowflakes round the body, TRANSLUCENT; 3. a frost ring turning on the ground, TRANSLUCENT, 3 cm up; 4. glints and a
 * second frost ring, ADDITIVE, full bright, last. About 6000 vertices assembled.
 */
public final class SnowAura {
    private SnowAura() {}

    private static final String K = "snow";
    private static final Piece[] SET = {
            AirSetKit.piece(K, "chest", Anchor.BODY, 0f, 0f, 0f, -14f, true, true),
            AirSetKit.piece(K, "helm", Anchor.HEAD, 2f, 0f, -14f, 0f, true),
            AirSetKit.piece(K, "pauld_r", Anchor.ARM_R, 3f, -14f, -6f, 0f, false),
            AirSetKit.piece(K, "pauld_l", Anchor.ARM_L, 4f, 14f, -6f, 0f, false),
            AirSetKit.piece(K, "vamb_r", Anchor.ARM_R, 5f, -8f, 2f, -10f, false),
            AirSetKit.piece(K, "vamb_l", Anchor.ARM_L, 6f, 8f, 2f, -10f, false),
            AirSetKit.piece(K, "belt", Anchor.BODY, 6f, 0f, 12f, -4f, true),
            AirSetKit.piece(K, "back", Anchor.BODY, 8f, 0f, -4f, 14f, true, true),
            AirSetKit.piece(K, "greave_r", Anchor.LEG_R, 9f, -2f, 12f, -6f, false),
            AirSetKit.piece(K, "greave_l", Anchor.LEG_L, 10f, 2f, 12f, -6f, false),
    };

    private static final int FROST = 0x9AD8FF, WHITE = 0xF2FAFF;
    private static ResourceLocation flake, ring, glint;

    /** Called once by AuraRegistry on the client. */
    public static void register() {
        flake = AuraRender.tex("particle/snow_flake");
        ring = AuraRender.tex("particle/snow_ring");
        glint = AuraRender.tex("particle/snow_glint");
        PlayerAuraClient.register(Aura.SNOW, SnowAura::paint);
    }

    private static float frac(float x) { return x - (float) Math.floor(x); }

    private static void paint(AuraContext c) {
        float f = c.fade();
        if (f <= 0.002f) return;
        float age = c.age(), pulse = 0.75f + 0.25f * Mth.sin(age * 0.13f);
        PoseStack pose = c.pose();
        boolean self1p = AirSetKit.firstPersonSelf(c);

        AirSetKit.paint(c, SET, false, SealDraw.argb(0.8f * pulse, 0xFFFFFF), 1f);

        SealDraw.camera(pose);
        int light = SealDraw.lift(c.light(), 8);
        VertexConsumer fv = c.buffers().getBuffer(AuraRender.translucent(flake));
        for (int i = 0; i < 12; i++) {                                                   // snow falling through the aura
            float ph = frac(age * 0.012f + i * 0.0833f), a = i * 2.399f + age * 0.01f;
            float r = 0.5f + 0.25f * Mth.sin(i * 1.9f), y = -0.6f + ph * 2.2f;
            if (self1p && y < 0.1f) continue;
            SealDraw.glow(pose, fv, Mth.cos(a) * r + 0.06f * Mth.sin(age * 0.07f + i), y, Mth.sin(a) * r, 0.05f + 0.035f * Mth.sin(i * 2.3f) * Mth.sin(i * 2.3f), age * 0.03f + i,
                    SealDraw.argb(Mth.sin(ph * Mth.PI) * 0.85f * f, WHITE), light);
        }
        VertexConsumer rv = c.buffers().getBuffer(AuraRender.translucent(ring));
        pose.pushPose();
        pose.translate(0f, 1.47f, 0f);
        float grow = 0.55f + 0.45f * f;
        SealDraw.flat(pose, rv, 0.95f * grow, age * 0.8f, SealDraw.argb(0.6f * f, 0xBFE6FF), light);
        pose.popPose();
        VertexConsumer gv = c.buffers().getBuffer(AuraRender.additive(glint));
        for (int i = 0; i < 8; i++) {                                                    // glints on the ice
            float tw = Mth.sin(age * 0.17f + i * 2.1f) * 0.5f + 0.5f;
            tw *= tw;
            float a = i * 0.785f + age * 0.01f, r = 0.4f + 0.12f * Mth.sin(i * 3.1f), y = 1.2f - (i % 4) * 0.55f;
            if (self1p && y < 0.1f) continue;
            SealDraw.glow(pose, gv, Mth.cos(a) * r, y, Mth.sin(a) * r, 0.04f + 0.1f * tw, age * 0.02f, SealDraw.argb(0.9f * f * tw, FROST), PropDraw.FULL_BRIGHT);
        }
        pose.pushPose();
        pose.translate(0f, 1.46f, 0f);
        SealDraw.flat(pose, gv, 0.62f * grow, -age * 1.4f, SealDraw.argb(0.35f * f * pulse, FROST), PropDraw.FULL_BRIGHT);
        pose.popPose();
    }
}
