package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.client.aura.WaterAuraRig.Anchor;
import com.newuniverse.nusmp.client.aura.WaterAuraRig.Piece;
import com.newuniverse.nusmp.client.prop.PropDraw;
import com.newuniverse.nusmp.client.prop.SealDraw;
import com.newuniverse.nusmp.perf.PerfProbe;
import com.newuniverse.nusmp.saiyan.SaiyanLook;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Why: the look of a transformed Saiyan, {@code Aura.SAIYAN} (3.69.0). The server keeps the lease alive while the form is on and passes the form code as the style; the
 * look table is {@link SaiyanLook}. What is drawn, in this order:
 * <pre>
 *   pass                           render type          what
 *   1 battle armour                cutout / translucent  the Saiyan armour set (chest, hip guards, pauldrons, vambraces, greaves, scouter), forged on as the form starts and shattered when it ends
 *   2 fur (SS4 forms)              translucent           the body drawn once more with the fur map, head left out (the face stays the player's own)
 *   3 tail (SS4 forms)             cutout                seven ribbons on the body, swaying
 *   4 aura flames, bolts, ring     additive, full bright  flame tongues round the body, lightning, a slow ring on the ground
 *   5 eyes                         additive, full bright  two glowing bars, only when the face turns to the camera
 *   6 the charge-up (first 46 t)   additive + rock chips  cracks, rising rocks, streaks pulled into the chest, flash, column of light, two shockwave rings
 * </pre>
 * Cost per Saiyan (worst case, the Super Saiyan 4 Limit Breaker, in full detail, mid charge-up): under {@link SaiyanLook#MAX_QUADS} quads plus the armour set (9 pieces, drawn through the same rig as the
 * Flame armour); beyond {@link SaiyanLook#NEAR} blocks the light version (no fur, tail, bolts or rock chips, half the flames and streaks); beyond {@link SaiyanLook#FAR} blocks nothing. No allocation in
 * the frame (scratch arrays), no texture lookups (resolved once), measured by {@code PerfProbe} section {@code c.saiyan.aura}.
 */
public final class SaiyanAura {
    private SaiyanAura() {}

    private static final PerfProbe.Section SECTION = PerfProbe.section("c.saiyan.aura");
    /** Model space: the origin is 1.5 blocks above the feet and y points down, so the feet are at y = 1.5. */
    private static final float FEET = 1.5f;
    private static final int FB = PropDraw.FULL_BRIGHT, WHITE = 0xFFFFFF;

    private static ResourceLocation flame, bolt, ring, eye, fur, tail, crack, pillar, streak, chip, spark, burstTex;
    private static final float[] TMP = new float[7];

    /** The armour pieces, in draw order (tools/gen_saiyan_armor_geo.py). The delays and fly-in directions are the Djinn full equip's, like the other armours. */
    private static final WaterAuraRig.Set SET = new WaterAuraRig.Set("saiyan",
            Piece.of("chest", Anchor.BODY, 1, 0, 0, -14),
            Piece.of("skirt", Anchor.BODY, 5, 0, 10, -6).clip("idle", 1f).detail(),
            Piece.of("pauldron_r", Anchor.ARM_R, 2, -14, -4, 0),
            Piece.of("pauldron_l", Anchor.ARM_L, 2, 14, -4, 0),
            Piece.of("vambrace_r", Anchor.ARM_R, 4, -12, 2, -6),
            Piece.of("vambrace_l", Anchor.ARM_L, 4, 12, 2, -6),
            Piece.of("greave_r", Anchor.LEG_R, 6, -3, 14, -6),
            Piece.of("greave_l", Anchor.LEG_L, 7, 3, 14, -6),
            Piece.of("scouter", Anchor.HEAD, 0, 0, -16, 0).clip("idle", 1f).detail().head());

    /** Called once by AuraRegistry on the client. */
    public static void register() {
        flame = AuraRender.tex("particle/saiyan_flame");
        bolt = AuraRender.tex("particle/saiyan_bolt");
        ring = AuraRender.tex("particle/saiyan_ring");
        eye = AuraRender.tex("particle/saiyan_eye");
        fur = AuraRender.tex("particle/saiyan_fur");
        tail = AuraRender.tex("particle/saiyan_tail");
        crack = AuraRender.tex("particle/saiyan_crack");
        pillar = AuraRender.tex("particle/saiyan_pillar");
        streak = AuraRender.tex("particle/saiyan_streak");
        chip = AuraRender.tex("particle/saiyan_chip");
        burstTex = AuraRender.tex("particle/saiyan_burst");
        spark = AuraRender.tex("particle/saiyan_spark");
        PlayerAuraClient.register(Aura.SAIYAN, SaiyanAura::paint);
    }

    private static void paint(AuraContext c) {
        SaiyanLook.Look k = SaiyanLook.ofCode(c.style());
        float f = c.fade();
        if (k == null || f <= 0.002f || !SaiyanLookClientConfig.looks() || c.player().isInvisible()) return;
        double d2 = SpiritDiveFrame.distSq(c);
        if (d2 > SaiyanLook.FAR * SaiyanLook.FAR) return;
        long t0 = PerfProbe.begin();
        try {
            draw(c, k, f, d2 > SaiyanLook.NEAR * SaiyanLook.NEAR);
        } finally {
            PerfProbe.end(SECTION, t0);
        }
    }

    private static void draw(AuraContext c, SaiyanLook.Look k, float f, boolean far) {
        PoseStack pose = c.pose();
        float age = c.age(), t = c.t(), pulse = 0.85f + 0.15f * Mth.sin(age * 0.3f);
        boolean full = !far && SaiyanLookClientConfig.fullDetail();
        boolean self1p = RebuildSetKit.firstPersonSelf(c);
        int glow = k.glow();

        // 1. the battle armour (solid pieces first, their glow maps are tinted with the form's colour)
        if (SaiyanLookClientConfig.armour() && k.armour()) WaterAuraRig.draw(c, SET, glow, 0.65f * pulse + 0.35f);

        // 2 + 3. the fur and the tail of the Super Saiyan 4 forms
        if (full && k.hasFur()) {
            WaterAuraRig.shell(c, AuraRender.translucent(fur), 1.07f, 1.04f, 1.07f, AuraRender.alpha(0xFF000000 | k.fur(), f), false, true);
            if (!self1p) tail(c, k, age, f);
        }

        // 4 + 5 + 6. everything additive, drawn camera-facing in the player's own pose
        SealDraw.camera(pose);
        flames(c, k, t, age, f, pulse, full, far);
        if (full) bolts(pose, c, k, age, f);
        groundRing(pose, c, k, t, age, f, pulse);
        if (k.eyeGlow() && !self1p) eyes(pose, c, k, age, f, pulse);
        if (SaiyanLookClientConfig.chargeUp() && t < SaiyanLook.CRACK_TICKS) chargeUp(pose, c, k, t, age, f, full, far, self1p);
    }

    // ------------------------------------------------------------------ the steady aura
    private static void flames(AuraContext c, SaiyanLook.Look k, float t, float age, float f, float pulse, boolean full, boolean far) {
        PoseStack pose = c.pose();
        int n = Math.min(k.flames(), SaiyanLook.MAX_FLAMES);
        if (far) n /= 2;
        if (n <= 0) return;
        float grow = SaiyanLookClientConfig.chargeUp() ? 0.12f + 0.88f * smooth(SaiyanLook.gather(t)) : 1f;
        // the aura trembles with the effort while the power gathers and for a moment after the burst (0 in the steady state)
        float shk = SaiyanLookClientConfig.chargeUp() ? SaiyanLook.shake(t) * 0.07f : 0f;
        // the body haze: one tall soft column of the form's colour behind the flames, so the aura has a silhouette
        VertexConsumer hz = c.buffers().getBuffer(AuraRender.additive(pillar));
        float hh = (k.auraH() + 0.5f) * 0.5f * grow;
        upright(pose, hz, shk * Mth.sin(age * 2.9f), FEET - hh, shk * Mth.cos(age * 3.3f), 0.85f, hh, SealDraw.argb(0.26f * f * pulse, k.glow()));
        VertexConsumer vc = c.buffers().getBuffer(AuraRender.additive(flame));
        for (int i = 0; i < n; i++) {
            float a = i * Mth.TWO_PI / n + age * 0.012f * (i % 2 == 0 ? 1f : -1f) + SaiyanLook.hash(i) * 0.5f;
            float r = (i % 2 == 0 ? 0.52f : 0.80f) + 0.08f * Mth.sin(age * 0.1f + i * 1.7f);
            float h = SaiyanLook.flameHeight(k, i, age) * grow;
            float hw = (0.26f + 0.07f * SaiyanLook.hash(i + 7)) * (0.8f + 0.2f * grow);
            upright(pose, vc, Mth.cos(a) * r + shk * Mth.sin(age * 2.7f + i * 1.9f), FEET - h * 0.5f - 0.04f, Mth.sin(a) * r * 0.9f + shk * Mth.cos(age * 3.1f + i * 2.3f), hw, h * 0.5f, SealDraw.argb(0.8f * f * pulse, k.glow()));
        }
    }

    private static void bolts(PoseStack pose, AuraContext c, SaiyanLook.Look k, float age, float f) {
        int n = Math.min(k.bolts(), SaiyanLook.MAX_BOLTS);
        if (n <= 0) return;
        VertexConsumer vc = c.buffers().getBuffer(AuraRender.additive(bolt));
        int col = SealDraw.mix(k.glow(), WHITE, 0.5f);
        for (int i = 0; i < n; i++) {
            float ph = frac(age * 0.045f + SaiyanLook.hash(i + 50));
            if (ph > 0.3f) continue;
            float a = SaiyanLook.hash(i + 60) * Mth.TWO_PI + Mth.floor(age * 0.045f + SaiyanLook.hash(i + 50)) * 1.7f;
            float r = 0.5f + 0.5f * SaiyanLook.hash(i + 61), y0 = 0.4f + 1.2f * SaiyanLook.hash(i + 62), len = 0.9f + 0.7f * SaiyanLook.hash(i + 63);
            float rot = (SaiyanLook.hash(i + 64) - 0.5f) * 0.9f, flick = 1f - ph / 0.3f;
            sprite(pose, vc, Mth.cos(a) * r, FEET - y0 - len * 0.5f, Mth.sin(a) * r, 0.2f, len * 0.5f, rot, SealDraw.argb(f * flick, col));
        }
    }

    private static void groundRing(PoseStack pose, AuraContext c, SaiyanLook.Look k, float t, float age, float f, float pulse) {
        float steady = SaiyanLookClientConfig.chargeUp() ? smooth((t - SaiyanLook.GATHER_TICKS) / 20f) : 1f;
        if (steady <= 0.01f) return;
        VertexConsumer vc = c.buffers().getBuffer(AuraRender.additive(ring));
        pose.pushPose();
        pose.translate(0f, FEET - 0.02f, 0f);
        SealDraw.flat(pose, vc, 0.95f + 0.04f * Mth.sin(age * 0.2f), age * 0.8f, SealDraw.argb(0.4f * f * pulse * steady, k.glow()), FB);
        pose.popPose();
    }

    private static void eyes(PoseStack pose, AuraContext c, SaiyanLook.Look k, float age, float f, float pulse) {
        float yaw = c.netHeadYaw() * Mth.DEG_TO_RAD, cs = Mth.cos(yaw), sn = Mth.sin(yaw);
        float[] cam = SealDraw.CAM;
        float face = -(-sn * cam[6] + -cs * cam[8]);                   // > 0 when the face turns to the camera (the look vector points from the camera into the scene)
        if (face <= 0.05f) return;
        VertexConsumer vc = c.buffers().getBuffer(AuraRender.additive(eye));
        float a = Math.min(1f, face * 1.6f) * f * (0.8f + 0.2f * pulse);
        for (int side = -1; side <= 1; side += 2) {
            float lx = 0.1f * side, lz = -0.27f;
            sprite(pose, vc, lx * cs + lz * sn, -0.12f, -lx * sn + lz * cs, 0.085f, 0.045f, 0f, SealDraw.argb(a, k.eyes()));
        }
    }

    // ------------------------------------------------------------------ the tail
    private static void tail(AuraContext c, SaiyanLook.Look k, float age, float f) {
        if (!k.tail()) return;
        PoseStack pose = c.pose();
        pose.pushPose();
        c.model().body.translateAndRotate(pose);
        SealDraw.camera(pose);
        VertexConsumer vc = c.buffers().getBuffer(AuraRender.cutout(tail));
        int col = AuraRender.alpha(0xFF000000 | k.fur(), f);
        int segs = 7;
        float px = 0f, py = 0.68f, pz = 0.14f, len = 0.2f;
        for (int s = 0; s < segs; s++) {
            float th = -0.25f + (s + 1) * 0.20f, sway = Mth.sin(age * 0.11f - s * 0.6f) * 0.05f * (s + 1) * 0.5f;
            float nx = px + sway * 0.4f, ny = py - Mth.sin(th) * len, nz = pz + Mth.cos(th) * len;
            float hw = 0.07f * (1f - s / 9f);
            strip(pose, vc, nx, ny, nz, px, py, pz, hw, 1f - (s + 1) / (float) segs, 1f - s / (float) segs, col, c.light());
            px = nx; py = ny; pz = nz;
        }
        pose.popPose();
    }

    // ------------------------------------------------------------------ the charge-up
    private static void chargeUp(PoseStack pose, AuraContext c, SaiyanLook.Look k, float t, float age, float f, boolean full, boolean far, boolean self1p) {
        float burst = k.burst(), flash = SaiyanLook.flash(t);
        int glow = k.glow(), hot = SealDraw.mix(glow, WHITE, 0.55f);

        // the ray burst behind the Saiyan: a big radial backdrop of white-hot light that swells with the gathering, flares at the burst and fades out softly. Two layers turn against each other, and it
        // sits a little behind the body (along the view) so the body stays in front of it. Never from the owner's own camera (it would fill the screen).
        float halo = SaiyanLook.halo(t);
        if (halo > 0.01f && !self1p) {
            VertexConsumer vc = c.buffers().getBuffer(AuraRender.additive(burstTex));
            float[] cam = SealDraw.CAM;
            float bx = cam[6] * 0.9f, by = FEET - 1.15f + cam[7] * 0.9f, bz = cam[8] * 0.9f, size = SaiyanLook.haloSize(t, burst);
            sprite(pose, vc, bx, by, bz, size, size, age * 0.012f, SealDraw.argb(0.85f * halo * f, SealDraw.mix(glow, WHITE, 0.3f)));
            sprite(pose, vc, bx, by, bz, size * 0.62f, size * 0.62f, 0.5f - age * 0.02f, SealDraw.argb(0.7f * halo * f, SealDraw.mix(WHITE, glow, 0.15f)));
        }

        // the wind rising round the body (half as many streaks in the light version)
        if (SaiyanLook.windAlpha(t) > 0.01f) {
            VertexConsumer vc = c.buffers().getBuffer(AuraRender.additive(streak));
            int col = SealDraw.mix(glow, WHITE, 0.45f);
            for (int i = 0; i < SaiyanLook.MAX_WIND; i += far ? 2 : 1) {
                SaiyanLook.wind(i, t, TMP);
                if (TMP[3] < 0.02f) continue;
                float hy = FEET - TMP[1], len = TMP[4];
                strip(pose, vc, TMP[0], hy + len, TMP[2], TMP[0], hy, TMP[2], 0.07f, 0f, 1f, SealDraw.argb(0.8f * TMP[3] * f, col), FB);
            }
        }

        // the ground cracks, white-hot at the burst, cooling
        float cr = SaiyanLook.crackRadius(t, burst), ca = SaiyanLook.crackAlpha(t);
        if (cr > 0.05f && ca > 0.01f) {
            VertexConsumer vc = c.buffers().getBuffer(AuraRender.additive(crack));
            pose.pushPose();
            pose.translate(0f, FEET - 0.015f, 0f);
            SealDraw.flat(pose, vc, cr, c.player().getId() * 37f, SealDraw.argb(0.9f * ca * f, SealDraw.mix(glow, WHITE, flash)), FB);
            pose.popPose();
        }

        // the rock chips lifting off the ground (full detail only)
        if (full) {
            float chA = SaiyanLook.chipAlpha(t) * f;
            if (chA > 0.01f) {
                VertexConsumer vc = c.buffers().getBuffer(AuraRender.translucent(chip));
                int col = SealDraw.mix(0x8C8478, glow, 0.2f), light = SealDraw.lift(c.light(), 8);
                for (int i = 0; i < SaiyanLook.MAX_CHIPS; i++) {
                    SaiyanLook.chip(i, t, burst, TMP);
                    float sz = TMP[3];
                    spriteLit(pose, vc, Mth.cos(TMP[0]) * TMP[1], FEET - TMP[2] - sz, Mth.sin(TMP[0]) * TMP[1], sz, sz, TMP[4], SealDraw.argb(chA, col), light);
                }
            }
        }

        // the streaks of energy pulled into the chest
        if (t < SaiyanLook.GATHER_TICKS + 12f) {
            VertexConsumer vc = c.buffers().getBuffer(AuraRender.additive(streak));
            int step = far ? 2 : 1;
            int col = SealDraw.mix(glow, WHITE, 0.35f);
            for (int i = 0; i < SaiyanLook.MAX_STREAKS; i += step) {
                SaiyanLook.streak(i, t, TMP);
                if (TMP[3] < 0.02f) continue;
                float x = TMP[0], z = TMP[2], r = Mth.sqrt(x * x + z * z) + 1e-4f, len = 0.9f + 0.5f * SaiyanLook.hash(i + 70);
                float hy = FEET - TMP[1];
                strip(pose, vc, x + x / r * len * 0.8f, hy - len * 0.45f, z + z / r * len * 0.8f, x, hy, z, 0.06f, 0f, 1f, SealDraw.argb(TMP[3] * f, col), FB);
            }
        }

        // the two shockwave rings along the ground
        for (int q = 0; q < 2; q++) {
            float r = SaiyanLook.ringRadius(t, q, burst), a = SaiyanLook.ringAlpha(t, q);
            if (r <= 0.01f || a <= 0.01f) continue;
            VertexConsumer vc = c.buffers().getBuffer(AuraRender.additive(ring));
            pose.pushPose();
            pose.translate(0f, FEET - 0.03f - 0.01f * q, 0f);
            SealDraw.flat(pose, vc, r, age * 2f * (q == 0 ? 1f : -1f), SealDraw.argb(0.85f * a * f, q == 0 ? glow : hot), FB);
            pose.popPose();
        }

        // the flash at the heart of it
        if (flash > 0.01f) {
            VertexConsumer vc = c.buffers().getBuffer(AuraRender.additive(spark));
            float size = (0.45f + 1.8f * flash) * (0.7f + 0.3f * burst);
            int col = SealDraw.mix(glow, WHITE, 0.6f);
            sprite(pose, vc, 0f, FEET - 1.15f, 0f, size, size, age * 0.07f, SealDraw.argb(Math.min(1f, flash) * f, col));
            sprite(pose, vc, 0f, FEET - 1.15f, 0f, size * 0.7f, size * 0.7f, 0.785f - age * 0.05f, SealDraw.argb(Math.min(1f, flash) * f, WHITE));
        }

        // the column of light that shoots up at the burst (never from the owner's own camera: it would fill the screen)
        float pa = SaiyanLook.pillarAlpha(t);
        if (pa > 0.01f && !self1p) {
            VertexConsumer vc = c.buffers().getBuffer(AuraRender.additive(pillar));
            float h = (7f + 9f * burst) * SaiyanLook.pillar(t), w = SaiyanLook.pillarWidth(t, burst);
            upright(pose, vc, 0f, FEET - h * 0.5f, 0f, w, h * 0.5f, SealDraw.argb(0.65f * pa * f, glow));
            upright(pose, vc, 0f, FEET - h * 0.5f, 0f, w * 0.42f, h * 0.5f, SealDraw.argb(0.95f * pa * f, hot));
        }
    }

    // ------------------------------------------------------------------ drawing helpers
    /** A sprite that faces the camera (needs {@link SealDraw#camera} first), with its own half width and half height, turned by rot radians. y is down in this pose space. */
    private static void sprite(PoseStack pose, VertexConsumer vc, float cx, float cy, float cz, float hw, float hh, float rot, int argb) {
        spriteLit(pose, vc, cx, cy, cz, hw, hh, rot, argb, FB);
    }

    private static void spriteLit(PoseStack pose, VertexConsumer vc, float cx, float cy, float cz, float hw, float hh, float rot, int argb, int light) {
        float co = Mth.cos(rot), si = Mth.sin(rot);
        float[] k = SealDraw.CAM;
        SealDraw.plane(pose, vc, cx, cy, cz, (k[0] * co + k[3] * si) * hw, (k[1] * co + k[4] * si) * hw, (k[2] * co + k[5] * si) * hw,
                (-k[0] * si + k[3] * co) * hh, (-k[1] * si + k[4] * co) * hh, (-k[2] * si + k[5] * co) * hh, false, argb, light);
    }

    /** A sprite that stands upright (its width follows the camera sideways, its height is the world's), centred on c; the image's top is the sky. */
    private static void upright(PoseStack pose, VertexConsumer vc, float cx, float cy, float cz, float hw, float hh, int argb) {
        float[] k = SealDraw.CAM;
        float rx = k[0], rz = k[2], l = Mth.sqrt(rx * rx + rz * rz);
        if (l < 1e-4f) { rx = 1f; rz = 0f; l = 1f; }
        rx = rx / l * hw; rz = rz / l * hw;
        SealDraw.plane(pose, vc, cx, cy, cz, rx, 0f, rz, 0f, -hh, 0f, false, argb, FB);
    }

    /**
     * A strip from the tail point to the head point that turns to face the camera. The texture's top (v0) is at the tail end and its bottom (v1) at the head end, so a tall texture runs along the
     * strip; hw is the half width.
     */
    private static void strip(PoseStack pose, VertexConsumer vc, float tx, float ty, float tz, float hx, float hy, float hz, float hw, float v0, float v1, int argb, int light) {
        float[] k = SealDraw.CAM;
        float dx = hx - tx, dy = hy - ty, dz = hz - tz;
        float wx = dy * k[8] - dz * k[7], wy = dz * k[6] - dx * k[8], wz = dx * k[7] - dy * k[6];
        float l = Mth.sqrt(wx * wx + wy * wy + wz * wz);
        if (l < 1e-5f) { wx = k[0]; wy = k[1]; wz = k[2]; l = 1f; }
        wx = wx / l * hw; wy = wy / l * hw; wz = wz / l * hw;
        PropDraw.quad(pose, vc, hx - wx, hy - wy, hz - wz, hx + wx, hy + wy, hz + wz, tx + wx, ty + wy, tz + wz, tx - wx, ty - wy, tz - wz, 0f, v0, 1f, v1, argb, light);
    }

    private static float frac(float x) { return x - (float) Math.floor(x); }

    private static float smooth(float x) { x = Mth.clamp(x, 0f, 1f); return x * x * (3f - 2f * x); }
}
