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
 * 1.93.0 Smoke Magic: the temporary armour (Aura.SMOKE), rebuilt as a real set of smoke pieces that forge in and shatter out like the Djinn Full Equip
 * ({@link EquipAnimation}). Geo models (tools/gen_smoke_rebuild.py), each on the model part it was built for:
 * <pre>
 *   helm      HEAD   delay 2   a hood of billows with a dark brow band, two ember sparks and two plumes curling back   (flies in from above)
 *   chest     BODY   delay 0   cloud collar, banded plate, an ember core on the sternum                                  (from the front)
 *   belt      BODY   delay 6   a sash of puffs with three tassets of falling smoke                                       (from below)
 *   back      BODY   delay 8   the smoke mantle (three tiers of billows to the knees) and two plumes off the shoulder blades (from behind)
 *   pauld_r/l ARM    delay 3/4 heaped billow pauldrons with an ember                                                      (from outside)
 *   vamb_r/l  ARM    delay 5/6 a cuff of puffs round the forearm and a fist of smoke                                      (from the front)
 *   greave_r/l LEG   delay 9/10 smoke boots with a knee billow and a trailing wisp                                       (from below)
 * </pre>
 * Passes in draw order, each with its render type: 1. the pieces: GeoDraw CUTOUT once assembled, TRANSLUCENT while forging in or shattering out (cutout
 * cannot fade), then their glow maps additive and full bright (the embers); 2. soft smoke sprites rising round the body: entityTranslucent, drawn after the
 * solid pieces and before the additive ones; 3. a smoke ring turning on the ground: entityTranslucent, flat, lifted 3 cm; 4. ember sparks rising: additive
 * (RenderType.eyes), full bright, drawn last. Cost: about 250 cubes with their glow passes, 10 puff sprites, 1 ring and 8 sparks.
 */
public final class SmokeAura {
    private SmokeAura() {}

    private static final String K = "smoke";
    private static final Piece[] SET = {
            RebuildSetKit.piece(K, "chest", Anchor.BODY, 0f, 0f, 0f, -14f, true, true),
            RebuildSetKit.piece(K, "helm", Anchor.HEAD, 2f, 0f, -14f, 0f, true, true),
            RebuildSetKit.piece(K, "pauld_r", Anchor.ARM_R, 3f, -14f, -6f, 0f, false),
            RebuildSetKit.piece(K, "pauld_l", Anchor.ARM_L, 4f, 14f, -6f, 0f, false),
            RebuildSetKit.piece(K, "vamb_r", Anchor.ARM_R, 5f, -8f, 2f, -10f, false),
            RebuildSetKit.piece(K, "vamb_l", Anchor.ARM_L, 6f, 8f, 2f, -10f, false),
            RebuildSetKit.piece(K, "belt", Anchor.BODY, 6f, 0f, 12f, -4f, true),
            RebuildSetKit.piece(K, "back", Anchor.BODY, 8f, 0f, -4f, 14f, true),
            RebuildSetKit.piece(K, "greave_r", Anchor.LEG_R, 9f, -2f, 12f, -6f, false),
            RebuildSetKit.piece(K, "greave_l", Anchor.LEG_L, 10f, 2f, 12f, -6f, false),
    };

    private static final int EMBER = 0xFF9A48, SMOKE_LO = 0x8A8098, SMOKE_HI = 0xD2CCE0;
    private static ResourceLocation puff, ring, ember;

    /** Called once by AuraRegistry on the client. */
    public static void register() {
        puff = AuraRender.tex("particle/smoke_puff");
        ring = AuraRender.tex("particle/smoke_ring");
        ember = AuraRender.tex("particle/smoke_ember");
        PlayerAuraClient.register(Aura.SMOKE, SmokeAura::paint);
    }

    private static float frac(float x) { return x - (float) Math.floor(x); }

    private static void paint(AuraContext c) {
        float f = c.fade();
        if (f <= 0.002f) return;
        float age = c.age(), pulse = 0.75f + 0.25f * Mth.sin(age * 0.17f);
        PoseStack pose = c.pose();
        boolean self1p = RebuildSetKit.firstPersonSelf(c);

        // 1. the armour pieces (cutout once assembled, translucent while forging / shattering) and their ember glow maps
        RebuildSetKit.paint(c, SET, false, SealDraw.argb(0.85f * pulse, 0xFFFFFF), 1f);

        SealDraw.camera(pose);
        int light = SealDraw.lift(c.light(), 6);
        // 2. smoke sprites rising round the body: translucent, soft
        VertexConsumer sv = c.buffers().getBuffer(AuraRender.translucent(puff));
        for (int i = 0; i < 9; i++) {
            float ph = frac(age * 0.017f + i * 0.111f), a = i * 2.399f + age * 0.03f * (i % 2 == 0 ? 1f : -1f);
            float r = 0.46f + 0.12f * Mth.sin(i * 1.7f + age * 0.05f), y = 1.42f - ph * 2.15f;
            if (self1p && y < 0.1f) continue;
            float size = 0.16f + 0.2f * ph;
            SealDraw.glow(pose, sv, Mth.cos(a) * r, y, Mth.sin(a) * r * 0.85f, size, age * 0.02f + i,
                    SealDraw.argb(Mth.sin(ph * Mth.PI) * 0.5f * f, SealDraw.mix(SMOKE_LO, SMOKE_HI, ph)), light);
        }
        // 3. a smoke ring turning on the ground
        VertexConsumer rv = c.buffers().getBuffer(AuraRender.translucent(ring));
        pose.pushPose();
        pose.translate(0f, 1.47f, 0f);
        float grow = 0.55f + 0.45f * f;
        SealDraw.flat(pose, rv, 0.95f * grow, age * 0.9f, SealDraw.argb(0.55f * f, SMOKE_LO), light);
        SealDraw.flat(pose, rv, 0.62f * grow, -age * 1.5f, SealDraw.argb(0.4f * f, SMOKE_HI), light);
        pose.popPose();
        // 4. ember sparks rising: additive, full bright, last
        VertexConsumer ev = c.buffers().getBuffer(AuraRender.additive(ember));
        for (int i = 0; i < 8; i++) {
            float ph = frac(age * 0.03f + i * 0.137f), a = i * 2.9f + age * 0.05f, r = 0.34f + 0.2f * Mth.sin(i * 1.3f), y = 1.4f - ph * 1.9f;
            if (self1p && y < 0.1f) continue;
            SealDraw.glow(pose, ev, Mth.cos(a) * r, y, Mth.sin(a) * r, 0.045f + 0.03f * Mth.sin(ph * Mth.PI), 0f,
                    SealDraw.argb(Mth.sin(ph * Mth.PI) * 0.95f * f, EMBER), PropDraw.FULL_BRIGHT);
        }
    }
}
