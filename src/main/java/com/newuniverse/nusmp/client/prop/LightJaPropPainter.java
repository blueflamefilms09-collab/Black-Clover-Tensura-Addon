package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.book.LightBoneJa;
import com.newuniverse.nusmp.client.SafeAdditive;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.perf.PerfSettings;
import com.newuniverse.nusmp.prop.LightJaVariant;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Why: how the props of the Light Magic remake look (the server half is prop.LightJaProps, the casts are book.LightJaArts; the spells follow the Black Clover wiki, docs/magic/light_bone_ja.md).
 * Everything is real geometry (PropDraw: cones, cylinders, boxes, spheres, tori) with glow passes of the custom light sprites, golden-white light with a pale gold rim:
 * <ul>
 *   <li>SWORD: a double-pointed shard of light with four lines of light running through it, a streaming trail and a star at the tip. CROSS: two crossing diamond blades and a flare (the cross-slash).</li>
 *   <li>WHIP: one long curling lash of light (a tube along the whip curve of {@link LightBoneJa#whipPoint}) with glints along it. BEAM: the huge beam of the Heaven-Splitting Flash: three nested layers, bright points along it.</li>
 *   <li>COLUMN: the Light Shaft of Divine Punishment: a ring on the ground and a thin line, then the giant column from the sky with a pool of light and a ring at every pulse.</li>
 *   <li>ARROW: a light arrow falling from the sky. ORB: the Lamp of Avior: a blinding sphere with spikes that gathers, flies and goes off as a starburst with a flash.</li>
 *   <li>HEAL: a sphere of white light with a halo ring and rising motes. MONARCH: the giant of golden light with a yellow rim, raising its fist when it strikes.</li>
 *   <li>GLORIA: the Lamp of Avior Gloria: a manga-style burst of black and white spikes and a jagged flash, growing over the caster and going off.</li>
 * </ul>
 * Cost rules: a prop never draws more than {@link LightBoneJa#MAX_QUADS_PER_PROP} quads (the preview report pins it); the counts of segments and spikes are scaled by the distance from the camera
 * ({@link LightBoneJa#lod}, nothing beyond 128 blocks) and by the video quality; nothing is allocated while painting (static scratch arrays, textures looked up once); flying props are moved here
 * from their age, so the server sends no position packets for them. Pass order: solid parts first, then the glow passes.
 */
public final class LightJaPropPainter {
    private LightJaPropPainter() {}

    private static final int FB = PropDraw.FULL_BRIGHT;
    private static final int WHITE = 0xFFFFF6, PALE = 0xFFF0B8, GOLD = 0xFFD870, DEEP = 0xFFB440;
    private static ResourceLocation glow, star, streak, plate, white;
    private static final float[][] PTS = new float[20][3];
    private static final double[] TMP = new double[3];
    private static final float PHI = 2.3999632f;                       // the golden angle

    /** Called once on the client by PropPainterRegistry. */
    public static void register() {
        glow = AuraRender.tex("particle/glow");
        star = AuraRender.tex("particle/light_star");
        streak = AuraRender.tex("particle/light_streak");
        plate = AuraRender.tex("prop/light_ja_plate");
        white = ResourceLocation.withDefaultNamespace("textures/misc/white.png");
        PropPainters.register(PropKind.LIGHT_JA, LightJaPropPainter::paint);
    }

    // ================================================================ shared
    private static float lod(MagicPropEntity e) {
        try {
            var cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            double dx = cam.x - e.getX(), dy = cam.y - e.getY(), dz = cam.z - e.getZ();
            return LightBoneJa.lod(Math.sqrt(dx * dx + dy * dy + dz * dz));
        } catch (RuntimeException ex) {
            return 1f;
        }
    }

    private static float quality() {
        try {
            return PerfSettings.vfxQuality().detail;
        } catch (RuntimeException ex) {
            return 1f;
        }
    }

    private static float fade(MagicPropEntity e, float partial, float in, float out) {
        float life = e.life() + partial, max = Math.max(1, e.maxLife());
        return SealDraw.smooth(life / in) * Mth.clamp((max - life) / out, 0f, 1f);
    }

    private static void paint(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, int light) {
        float lod = lod(e);
        if (lod <= 0f) return;
        float q = quality();
        switch (e.param() & 0xFF) {
            case LightJaVariant.SWORD -> sword(e, partial, age, pose, buf, lod, q);
            case LightJaVariant.CROSS -> cross(e, partial, age, pose, buf, lod, q);
            case LightJaVariant.WHIP -> whip(e, partial, age, pose, buf, lod, q);
            case LightJaVariant.BEAM -> beam(e, partial, age, pose, buf, lod, q);
            case LightJaVariant.COLUMN -> column(e, partial, age, pose, buf, lod, q);
            case LightJaVariant.ARROW -> arrow(e, partial, age, pose, buf, lod, q);
            case LightJaVariant.ORB -> orb(e, partial, age, pose, buf, lod, q);
            case LightJaVariant.HEAL -> heal(e, partial, age, pose, buf, lod, q);
            case LightJaVariant.MONARCH -> monarch(e, partial, age, pose, buf, light, lod, q);
            case LightJaVariant.GLORIA -> gloria(e, partial, age, pose, buf, lod, q);
            default -> { }
        }
    }

    private static VertexConsumer add(MultiBufferSource buf, ResourceLocation tex) { return buf.getBuffer(SafeAdditive.additive(tex)); }

    /** A glowing sprite that faces the camera (needs {@link SealDraw#camera} in the current space). */
    private static void sprite(PoseStack pose, MultiBufferSource buf, ResourceLocation tex, float x, float y, float z, float half, float rot, float a, int rgb) {
        if (a <= 0.01f || half <= 0f) return;
        SealDraw.glow(pose, add(buf, tex), x, y, z, half, rot, SealDraw.argb(a, rgb), FB);
    }

    /** A double-pointed diamond blade along +z from -back to +front (a four-sided cone pair; flatten it with the pose). */
    private static void shard(PoseStack pose, VertexConsumer vc, float back, float front, float r, int argb, int light) {
        PropDraw.cylinder(pose, vc, 0f, 0f, 0f, 0f, 0f, front, r, 0f, 4, false, argb, light);
        PropDraw.cylinder(pose, vc, 0f, 0f, -back, 0f, 0f, 0f, 0f, r, 4, false, argb, light);
    }

    /** One thin plane through the z axis turned by deg about it (a line of light along the length), drawn on both faces. */
    private static void line(PoseStack pose, VertexConsumer vc, float len, float width, float deg, int argb) {
        float c = Mth.cos(deg * Mth.DEG_TO_RAD) * width, s = Mth.sin(deg * Mth.DEG_TO_RAD) * width;
        SealDraw.plane(pose, vc, 0f, 0f, 0f, 0f, 0f, len, c, s, 0f, false, argb, FB);
        SealDraw.plane(pose, vc, 0f, 0f, 0f, 0f, 0f, len, c, s, 0f, true, argb, FB);
    }

    // ================================================================ SWORD
    private static void sword(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, float lod, float q) {
        float k = fade(e, partial, 1.5f, 1f);
        if (k <= 0.01f) return;
        float s = e.scale();
        double go = LightBoneJa.travelled(LightBoneJa.speedOf(e.param()), age, Math.max(1, e.maxLife()));
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(e.getXRot()));
        pose.translate(0f, 0f, (float) go);
        SealDraw.camera(pose);
        pose.pushPose();
        pose.scale(0.26f * s, 1f * s, 1f * s);                                   // flat: a blade, not a spindle
        shard(pose, buf.getBuffer(AuraRender.cutout(plate)), 0.55f, 1.7f, 0.15f, SealDraw.argb(1f, WHITE), FB);
        pose.popPose();
        VertexConsumer vc = add(buf, streak);                                    // the four lines of light that run through the shard
        int n = LightBoneJa.seg(4, lod, q, 2);
        for (int j = 0; j < n; j++) line(pose, vc, 2.1f * s, 0.2f * s, j * 180f / n, SealDraw.argb(0.9f * k, WHITE));
        SealDraw.ribbon(pose, vc, 0f, 0f, -0.8f * s, 0f, 0f, -3.4f * s, 0.14f * s, 1f, 0f, SealDraw.argb(0.8f * k, GOLD), FB);                  // the streaming trail behind
        sprite(pose, buf, star, 0f, 0f, 1.7f * s, 0.6f * s, age * 0.4f, 0.95f * k, WHITE);
        sprite(pose, buf, glow, 0f, 0f, 0.1f * s, 0.5f * s, 0f, 0.55f * k, GOLD);
        pose.popPose();
    }

    // ================================================================ CROSS
    private static void cross(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, float lod, float q) {
        float life = e.life() + partial, k = fade(e, partial, 1.5f, 5f);
        if (k <= 0.01f) return;
        float grow = SealDraw.smooth(life / 2.5f), s = e.scale() * (0.5f + 0.5f * grow);
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(e.getXRot()));
        SealDraw.camera(pose);
        VertexConsumer solid = buf.getBuffer(AuraRender.cutout(plate));
        for (int d = -1; d <= 1; d += 2) {                                       // two diamond blades crossing in an X, flat across the flight
            pose.pushPose();
            pose.mulPose(Axis.ZP.rotationDegrees(45f * d));
            pose.scale(1f, 1f, 0.12f);
            pose.mulPose(Axis.YP.rotationDegrees(90f));                          // the blade's length (+z of the shard) now lies along the x axis
            shard(pose, solid, 1.7f * s, 1.7f * s, 0.16f * s, SealDraw.argb(1f, WHITE), FB);
            pose.popPose();
        }
        sprite(pose, buf, star, 0f, 0f, 0f, 1.4f * s, age * 0.3f, 0.9f * k, WHITE);
        sprite(pose, buf, glow, 0f, 0f, 0f, 1.1f * s, 0f, 0.6f * k, GOLD);
        pose.popPose();
    }

    // ================================================================ WHIP
    private static void whip(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, float lod, float q) {
        float life = e.life() + partial, max = Math.max(1, e.maxLife());
        float reach = Mth.clamp(life / 2.5f, 0f, 1f) * Mth.clamp((max - life) / 3f, 0f, 1f);                    // the lash flies out, then draws back
        if (reach <= 0.02f) return;
        float len = (float) LightBoneJa.WHIP_REACH * e.scale() * (0.35f + 0.65f * reach), phase = age + ((e.param() >> 8) & 3) * 5f;
        int n = Math.min(PTS.length - 1, LightBoneJa.seg(19, lod, q, 6));
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(e.getXRot()));
        SealDraw.camera(pose);
        for (int i = 0; i <= n; i++) {
            LightBoneJa.whipPoint(i / (double) n, len, phase, TMP);
            PTS[i][0] = (float) TMP[0]; PTS[i][1] = (float) TMP[1]; PTS[i][2] = (float) TMP[2];
        }
        VertexConsumer core = add(buf, white);
        int sides = LightBoneJa.seg(6, lod, q, 3);
        for (int i = 0; i < n; i++) {                                           // the core tube, thick at the hand and thin at the tip, and a wider gold one round it
            float t0 = i / (float) n, t1 = (i + 1) / (float) n;
            PropDraw.cylinder(pose, core, PTS[i][0], PTS[i][1], PTS[i][2], PTS[i + 1][0], PTS[i + 1][1], PTS[i + 1][2], 0.1f * (1f - 0.75f * t0), 0.1f * (1f - 0.75f * t1), sides, false,
                    SealDraw.argb(0.97f, WHITE), FB);
            PropDraw.cylinder(pose, core, PTS[i][0], PTS[i][1], PTS[i][2], PTS[i + 1][0], PTS[i + 1][1], PTS[i + 1][2], 0.22f * (1f - 0.7f * t0), 0.22f * (1f - 0.7f * t1), sides, false,
                    SealDraw.argb(0.28f, GOLD), FB);
        }
        for (int i = 2; i <= n; i += 3) sprite(pose, buf, glow, PTS[i][0], PTS[i][1], PTS[i][2], 0.3f, 0f, 0.7f, PALE);          // glints along the lash
        sprite(pose, buf, star, PTS[n][0], PTS[n][1], PTS[n][2], 0.6f, age * 0.5f, 0.95f * reach, WHITE);
        pose.popPose();
    }

    // ================================================================ BEAM
    private static void beam(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, float lod, float q) {
        float life = e.life() + partial, max = Math.max(1, e.maxLife());
        float k = Mth.clamp(life / 1.5f, 0f, 1f) * Mth.clamp((max - life) / 4f, 0f, 1f);
        if (k <= 0.01f) return;
        float len = ((e.param() >> 8) & 0xFF) / 2f, w = e.scale() * (0.4f + 0.6f * k);
        int segs = LightBoneJa.seg(10, lod, q, 4);
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(e.getXRot()));
        SealDraw.camera(pose);
        VertexConsumer vc = add(buf, white);
        PropDraw.cylinder(pose, vc, 0f, 0f, 0f, 0f, 0f, len, 0.95f * w, 0.95f * w, segs, true, SealDraw.argb(0.16f * k, DEEP), FB);      // the three layers of the beam
        PropDraw.cylinder(pose, vc, 0f, 0f, 0f, 0f, 0f, len, 0.52f * w, 0.52f * w, segs, true, SealDraw.argb(0.34f * k, GOLD), FB);
        PropDraw.cylinder(pose, vc, 0f, 0f, 0f, 0f, 0f, len, 0.24f * w, 0.24f * w, Math.max(4, segs - 2), true, SealDraw.argb(0.95f * k, WHITE), FB);
        VertexConsumer sk = add(buf, streak);
        SealDraw.ribbon(pose, sk, 0f, 0f, 0f, 0f, 0f, len, 1.5f * w, 0f, 1f, SealDraw.argb(0.6f * k, PALE), FB);
        int points = Math.min(14, LightBoneJa.seg(Math.max(2, (int) (len / 3.2f)), lod, q, 2));
        for (int i = 0; i < points; i++) {                                      // the bright points along the beam
            float z = len * (i + 0.5f) / points;
            sprite(pose, buf, star, 0f, 0f, z, (0.95f + 0.25f * Mth.sin(age * 0.8f + i)) * w, age * 0.3f + i, 0.9f * k, WHITE);
        }
        sprite(pose, buf, glow, 0f, 0f, 0f, 1.7f * w, 0f, 0.9f * k, WHITE);
        sprite(pose, buf, star, 0f, 0f, len, 2.2f * w, age * 0.2f, 0.9f * k, WHITE);
        pose.popPose();
    }

    // ================================================================ COLUMN
    private static void column(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, float lod, float q) {
        float life = e.life() + partial, max = Math.max(1, e.maxLife()), r = 2.6f * e.scale();
        int tele = LightBoneJa.SHAFT_TELEGRAPH;
        SealDraw.camera(pose);
        int segs = LightBoneJa.seg(24, lod, q, 8);
        if (life < tele) {                                                      // the ring on the ground that marks the place, and a thin line that grows
            float t = life / tele, a = 0.5f + 0.4f * t;
            PropDraw.cylinder(pose, add(buf, white), 0f, 0f, 0f, 0f, 6f + 34f * t, 0f, 0.1f, 0.06f, 6, false, SealDraw.argb(0.55f * t, WHITE), FB);
            VertexConsumer vc = add(buf, white);
            PropDraw.torus(pose, vc, 0f, 0.06f, 0f, 0f, 1f, 0f, r * (1.25f - 0.25f * t), 0.07f, segs, 4, SealDraw.argb(a, PALE), FB);
            PropDraw.torus(pose, vc, 0f, 0.06f, 0f, 0f, 1f, 0f, r * 0.55f, 0.04f, segs, 4, SealDraw.argb(a * 0.7f, WHITE), FB);
            pose.pushPose();
            pose.translate(0f, 0.06f, 0f);
            SealDraw.flat(pose, add(buf, glow), r * 1.6f, age * 2f, SealDraw.argb(0.45f * t, GOLD), FB);
            pose.popPose();
            return;
        }
        float fall = SealDraw.smooth((life - tele) / 3f), out = Mth.clamp((max - life) / 8f, 0f, 1f), k = fall * out;
        if (k <= 0.01f) return;
        float h = 46f, w = k;
        VertexConsumer vc = add(buf, white);                                                                                                       // pass 1: everything white, written together
        PropDraw.cylinder(pose, vc, 0f, 0f, 0f, 0f, h, 0f, 1.15f * r * w, 1.3f * r * w, segs, false, SealDraw.argb(0.14f * k, DEEP), FB);         // the column: three nested layers
        PropDraw.cylinder(pose, vc, 0f, 0f, 0f, 0f, h, 0f, 0.8f * r * w, 0.9f * r * w, segs, false, SealDraw.argb(0.3f * k, GOLD), FB);
        PropDraw.cylinder(pose, vc, 0f, 0f, 0f, 0f, h, 0f, 0.42f * r * w, 0.5f * r * w, Math.max(6, segs - 6), false, SealDraw.argb(0.9f * k, WHITE), FB);
        for (int p = 0; p < LightBoneJa.SHAFT_PULSES; p++) {                                                                                       // a ring runs out at every pulse
            float since = life - (tele + p * LightBoneJa.SHAFT_GAP);
            if (since < 0f || since > 7f) continue;
            float t = since / 7f;
            PropDraw.torus(pose, vc, 0f, 0.06f, 0f, 0f, 1f, 0f, r * (0.5f + 1.1f * t), 0.12f * (1f - t) + 0.03f, segs, 4, SealDraw.argb(0.85f * (1f - t), WHITE), FB);
        }
        pose.pushPose();                                                                                                                           // pass 2: the glow sprites
        pose.translate(0f, 0.06f, 0f);
        SealDraw.flat(pose, add(buf, glow), r * 2.6f, age * 3f, SealDraw.argb(0.85f * k, WHITE), FB);                                              // the pool of light at the foot
        pose.popPose();
        sprite(pose, buf, star, 0f, 1.2f, 0f, r * 1.5f * k, age * 0.1f, 0.8f * k, WHITE);
    }

    // ================================================================ ARROW
    private static void arrow(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, float lod, float q) {
        float k = fade(e, partial, 1f, 1f);
        if (k <= 0.01f) return;
        float s = e.scale() * 0.8f;
        double fall = Math.min(LightBoneJa.travelled(LightBoneJa.speedOf(e.param()), age, Math.max(1, e.maxLife())), LightBoneJa.ARROW_HEIGHT);
        pose.pushPose();
        pose.translate(0f, (float) -fall, 0f);                                   // the prop stands at the top of the fall: the painter drops it
        pose.mulPose(Axis.XP.rotationDegrees(e.getXRot()));
        SealDraw.camera(pose);
        VertexConsumer solid = buf.getBuffer(AuraRender.cutout(plate));
        PropDraw.cylinder(pose, solid, 0f, 0f, -0.95f * s, 0f, 0f, 0.5f * s, 0.045f * s, 0.045f * s, 5, false, SealDraw.argb(1f, PALE), FB);
        PropDraw.cylinder(pose, solid, 0f, 0f, 0.5f * s, 0f, 0f, 1.0f * s, 0.13f * s, 0f, 4, false, SealDraw.argb(1f, WHITE), FB);
        for (int j = 0; j < 2; j++) {                                            // the fletching: two crossed vanes
            float a = j * 90f * Mth.DEG_TO_RAD, c = Mth.cos(a) * 0.14f * s, sn = Mth.sin(a) * 0.14f * s;
            SealDraw.plane(pose, solid, 0f, 0f, -0.82f * s, 0f, 0f, 0.17f * s, c, sn, 0f, false, SealDraw.argb(1f, GOLD), FB);
            SealDraw.plane(pose, solid, 0f, 0f, -0.82f * s, 0f, 0f, 0.17f * s, c, sn, 0f, true, SealDraw.argb(1f, GOLD), FB);
        }
        VertexConsumer sk = add(buf, streak);
        SealDraw.ribbon(pose, sk, 0f, 0f, -0.9f * s, 0f, 0f, -3.6f * s, 0.12f * s, 1f, 0f, SealDraw.argb(0.8f * k, GOLD), FB);
        sprite(pose, buf, star, 0f, 0f, 1.0f * s, 0.4f * s, age * 0.5f, 0.95f * k, WHITE);
        sprite(pose, buf, glow, 0f, 0f, 0.3f * s, 0.35f * s, 0f, 0.5f * k, GOLD);
        pose.popPose();
    }

    // ================================================================ ORB (Lamp of Avior)
    /** A spike of the starburst: a thin cone from the centre out along the direction of fibonacci point i of n. */
    private static void spike(PoseStack pose, VertexConsumer vc, int i, int n, float len, float r, int argb) {
        float y = 1f - 2f * (i + 0.5f) / n, rad = Mth.sqrt(Math.max(0f, 1f - y * y)), a = i * PHI;
        PropDraw.cylinder(pose, vc, 0f, 0f, 0f, Mth.cos(a) * rad * len, y * len, Mth.sin(a) * rad * len, r, 0f, 4, false, argb, FB);
    }

    private static void orb(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, float lod, float q) {
        int param = e.param();
        float life = e.life() + partial, s = e.scale();
        boolean flying = (param & LightJaVariant.FLIGHT) != 0, burst = (param & LightJaVariant.BURST) != 0;
        pose.pushPose();
        if (flying) {
            pose.mulPose(Axis.XP.rotationDegrees(e.getXRot()));
            pose.translate(0f, 0f, (float) LightBoneJa.travelled(LightBoneJa.speedOf(param), age, Math.max(1, e.maxLife())));
        }
        SealDraw.camera(pose);
        if (burst) {                                                              // the starburst: spikes shoot out, a flash, a shock ring
            float t = Mth.clamp(life / 6f, 0f, 1f), out = Mth.clamp((e.maxLife() - life) / 9f, 0f, 1f), reach = (float) LightBoneJa.LAMP_RADIUS * s * (0.25f + 0.75f * t);
            int n = LightBoneJa.seg(26, lod, q, 8);
            VertexConsumer vc = add(buf, white);                                  // pass 1: spikes and the ring (all white)
            for (int i = 0; i < n; i++) {
                float len = reach * (0.45f + 0.55f * SealDraw.rnd(e.seed(), i, 3));
                spike(pose, vc, i, n, len, 0.24f * s * (1f - 0.3f * t), SealDraw.argb(0.85f * out, (i & 1) == 0 ? WHITE : GOLD));
            }
            PropDraw.torus(pose, vc, 0f, 0f, 0f, 0f, 1f, 0f, reach * 0.8f, 0.14f * s * (1f - t) + 0.03f, LightBoneJa.seg(28, lod, q, 10), 4, SealDraw.argb(0.8f * out, PALE), FB);
            sprite(pose, buf, glow, 0f, 0f, 0f, reach * 0.9f, 0f, 0.8f * out * (1f - 0.5f * t), WHITE);       // pass 2: the flash
            sprite(pose, buf, star, 0f, 0f, 0f, reach * 0.8f, life * 0.1f, 0.95f * out, WHITE);
        } else {
            float grow = flying ? 1f : SealDraw.smooth(life / LightBoneJa.LAMP_CHARGE), r = (0.18f + 0.4f * grow) * s;
            VertexConsumer vc = add(buf, white);
            int n = LightBoneJa.seg(14, lod, q, 6);
            for (int i = 0; i < n; i++) {                                          // the thin spikes of the lamp, flickering
                float flick = 0.75f + 0.25f * Mth.sin(age * 0.9f + i * 1.7f);
                spike(pose, vc, i, n, r * (1.8f + 1.4f * flick), 0.06f * s, SealDraw.argb(0.9f, i % 3 == 0 ? GOLD : WHITE));
            }
            PropDraw.sphere(pose, vc, 0f, 0f, 0f, r, LightBoneJa.seg(8, lod, q, 4), LightBoneJa.seg(12, lod, q, 6), SealDraw.argb(0.95f, WHITE), FB);
            sprite(pose, buf, glow, 0f, 0f, 0f, r * 4.2f, 0f, 0.7f, GOLD);
            sprite(pose, buf, star, 0f, 0f, 0f, r * 3.4f, age * 0.3f, 0.95f, WHITE);
            if (flying) SealDraw.ribbon(pose, add(buf, streak), 0f, 0f, -0.2f * s, 0f, 0f, -3.2f * s, 0.4f * s, 1f, 0f, SealDraw.argb(0.8f, GOLD), FB);
        }
        pose.popPose();
    }

    // ================================================================ HEAL
    private static void heal(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, float lod, float q) {
        float k = fade(e, partial, 5f, 8f);
        if (k <= 0.01f) return;
        float r = 0.85f * (0.7f + 0.3f * k) * (1f + 0.05f * Mth.sin(age * 0.4f));
        SealDraw.camera(pose);
        VertexConsumer vc = add(buf, white);
        PropDraw.sphere(pose, vc, 0f, 0f, 0f, r, LightBoneJa.seg(8, lod, q, 4), LightBoneJa.seg(12, lod, q, 6), SealDraw.argb(0.3f * k, PALE), FB);
        PropDraw.sphere(pose, vc, 0f, 0f, 0f, r * 0.62f, LightBoneJa.seg(6, lod, q, 4), LightBoneJa.seg(10, lod, q, 6), SealDraw.argb(0.6f * k, WHITE), FB);
        int segs = LightBoneJa.seg(32, lod, q, 10);
        pose.pushPose();                                                          // the halo rings, turning
        pose.mulPose(Axis.XP.rotationDegrees(72f));
        pose.mulPose(Axis.ZP.rotationDegrees(age * 3f));
        PropDraw.torus(pose, vc, 0f, 0f, 0f, 0f, 1f, 0f, r * 1.35f, 0.045f, segs, 4, SealDraw.argb(0.9f * k, WHITE), FB);
        PropDraw.torus(pose, vc, 0f, 0.05f, 0f, 0f, 1f, 0f, r * 1.12f, 0.025f, segs, 4, SealDraw.argb(0.7f * k, GOLD), FB);
        pose.popPose();
        sprite(pose, buf, glow, 0f, 0f, 0f, r * 2.6f, 0f, 0.6f * k, WHITE);
        sprite(pose, buf, star, 0f, 0f, 0f, r * 2.2f, age * 0.12f, 0.8f * k, WHITE);
        int motes = LightBoneJa.seg(6, lod, q, 2);
        for (int i = 0; i < motes; i++) {                                         // motes of light rising round the sphere
            float a = age * 0.12f + i * Mth.TWO_PI / motes, up = ((age * 0.03f + i * 0.37f) % 1f);
            sprite(pose, buf, glow, Mth.cos(a) * r * 1.2f, -r + up * 2f * r, Mth.sin(a) * r * 1.2f, 0.12f, 0f, 0.9f * k * (1f - up), WHITE);
        }
    }

    // ================================================================ MONARCH
    /** The whole figure; called twice: solid, then slightly larger as the yellow rim. {@code strike} 0..1 raises the right fist. */
    private static void giant(PoseStack pose, VertexConsumer vc, float grow, float strike, float breath, float sc, int argb, boolean full) {
        float h = grow * sc;
        int sides = full ? 8 : 6;
        PropDraw.cylinder(pose, vc, -0.34f * sc, 0f, 0f, -0.34f * sc, 1.6f * h, 0f, 0.24f * sc, 0.2f * sc, sides, false, argb, FB);                              // legs
        PropDraw.cylinder(pose, vc, 0.34f * sc, 0f, 0f, 0.34f * sc, 1.6f * h, 0f, 0.24f * sc, 0.2f * sc, sides, false, argb, FB);
        PropDraw.cylinder(pose, vc, 0f, 1.35f * h, 0f, 0f, 2.15f * h, 0f, 0.95f * sc, 0.62f * sc, sides, false, argb, FB);                                       // the skirt of light armour, flaring out
        PropDraw.box(pose, vc, -0.78f * sc, 2.1f * h, -0.4f * sc, 0.78f * sc, 3.3f * h + breath, 0.4f * sc, argb, FB);                                          // chest
        PropDraw.cylinder(pose, vc, 0f, 2.1f * h, 0.42f * sc, 0f, 3.2f * h + breath, 0.42f * sc, 0.3f * sc, 0.46f * sc, sides, false, argb, FB);                // the breastplate
        PropDraw.sphere(pose, vc, -1.06f * sc, 3.2f * h + breath, 0f, 0.46f * sc, 5, 8, argb, FB);                                                               // pauldrons
        PropDraw.sphere(pose, vc, 1.06f * sc, 3.2f * h + breath, 0f, 0.46f * sc, 5, 8, argb, FB);
        PropDraw.cylinder(pose, vc, -1.14f * sc, 3.0f * h + breath, 0f, -1.2f * sc, 1.7f * h, 0f, 0.2f * sc, 0.17f * sc, sides, false, argb, FB);                // left arm
        PropDraw.sphere(pose, vc, -1.2f * sc, 1.55f * h, 0f, 0.26f * sc, 4, 6, argb, FB);                                                                        // left fist
        pose.pushPose();                                                                                                                                         // the right arm swings forward when it strikes
        pose.translate(1.14f * sc, 3.0f * h + breath, 0f);
        pose.mulPose(Axis.XP.rotationDegrees(-95f * strike));
        PropDraw.cylinder(pose, vc, 0f, 0f, 0f, 0.06f * sc, -1.35f * h, 0f, 0.2f * sc, 0.17f * sc, sides, false, argb, FB);
        PropDraw.sphere(pose, vc, 0.06f * sc, -1.5f * h, 0f, 0.28f * sc, 4, 6, argb, FB);
        pose.popPose();
        PropDraw.cylinder(pose, vc, 0f, 3.25f * h + breath, 0f, 0f, 3.5f * h + breath, 0f, 0.2f * sc, 0.17f * sc, sides, false, argb, FB);                      // neck
        PropDraw.box(pose, vc, -0.33f * sc, 3.5f * h + breath, -0.33f * sc, 0.33f * sc, 4.18f * h + breath, 0.33f * sc, argb, FB);                              // head
        if (full) {
            PropDraw.cylinder(pose, vc, 0f, 4.18f * h + breath, 0f, 0f, 4.62f * h + breath, 0f, 0.36f * sc, 0.3f * sc, 8, false, argb, FB);                      // the crown: a band and five tall points
            for (int i = -2; i <= 2; i++)
                PropDraw.cylinder(pose, vc, i * 0.16f * sc, 4.55f * h + breath, 0f, i * 0.24f * sc, (5.05f - 0.12f * Math.abs(i)) * h + breath, 0f, 0.07f * sc, 0f, 4, false, argb, FB);
        }
    }

    private static void monarch(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, int light, float lod, float q) {
        float life = e.life() + partial, k = fade(e, partial, 14f, 14f);
        if (k <= 0.01f) return;
        float sc = e.scale() * 1.05f, grow = SealDraw.smooth(life / 14f), breath = 0.05f * Mth.sin(age * 0.12f);
        float strike = (e.param() & LightJaVariant.STRIKE) != 0 ? 1f : 0f;
        SealDraw.camera(pose);
        giant(pose, buf.getBuffer(AuraRender.cutout(plate)), grow, strike, breath, sc, SealDraw.argb(1f, SealDraw.mix(PALE, GOLD, 0.25f)), lod > 0.5f);
        VertexConsumer rim = add(buf, white);                                     // the yellow rim: the same figure a little larger, added
        pose.pushPose();
        pose.translate(0f, -0.04f * sc, 0f);
        pose.scale(1.07f, 1.04f, 1.2f);
        giant(pose, rim, grow, strike, breath, sc, SealDraw.argb(0.3f * k, GOLD), false);
        pose.popPose();
        for (int side = -1; side <= 1; side += 2) {                               // the eyes
            PropDraw.box(pose, rim, (0.13f * side - 0.07f) * sc, 3.72f * grow * sc + breath, 0.33f * sc, (0.13f * side + 0.07f) * sc, 3.8f * grow * sc + breath, 0.39f * sc, SealDraw.argb(k, WHITE), FB);
        }
        PropDraw.torus(pose, rim, 0f, 3.65f * grow * sc + breath, -0.5f * sc, 0f, 0f, 1f, 0.95f * sc, 0.035f * sc, LightBoneJa.seg(32, lod, q, 10), 4, SealDraw.argb(0.85f * k, WHITE), FB);   // the halo behind the head
        if (lod > 0.5f) {                                                         // two fans of light feathers behind the shoulders (additive, streaks)
            VertexConsumer fan = add(buf, streak);
            for (int side = -1; side <= 1; side += 2)
                for (int i = 0; i < 5; i++) {
                    float a = (20f + i * 17f) * Mth.DEG_TO_RAD, len = (2.6f - 0.22f * i) * sc, sway = 0.04f * Mth.sin(age * 0.1f + i);
                    float ux = side * Mth.cos(a) * len * 0.5f, uy = Mth.sin(a) * len * 0.5f, cx = side * 0.5f * sc + ux, cy = 3.0f * sc + uy;
                    SealDraw.plane(pose, fan, cx, cy, -0.55f * sc + sway, -ux, -uy, 0f, -uy * 0.12f, ux * 0.12f, 0f, false, SealDraw.argb(0.55f * k, i % 2 == 0 ? WHITE : GOLD), FB);
                    SealDraw.plane(pose, fan, cx, cy, -0.55f * sc + sway, -ux, -uy, 0f, -uy * 0.12f, ux * 0.12f, 0f, true, SealDraw.argb(0.55f * k, i % 2 == 0 ? WHITE : GOLD), FB);
                }
        }
        sprite(pose, buf, glow, 0f, 2.2f * sc, 0f, 3.4f * sc, 0f, 0.35f * k, GOLD);
        sprite(pose, buf, star, 0f, 3.65f * sc, -0.5f * sc, 1.6f * sc, age * 0.05f, 0.8f * k, WHITE);
    }

    // ================================================================ GLORIA (Lamp of Avior Gloria)
    /** A camera-facing jagged flash: {@code n} triangles round the centre, with spikes of changing length (a manga burst). */
    private static void jagged(PoseStack pose, VertexConsumer vc, int seed, int n, float inner, float outer, float rot, int argb) {
        float[] c = SealDraw.CAM;
        for (int i = 0; i < n; i++) {
            float a0 = rot + i * Mth.TWO_PI / n, a1 = rot + (i + 1) * Mth.TWO_PI / n, am = (a0 + a1) * 0.5f;
            float rs = outer * (0.55f + 0.45f * SealDraw.rnd(seed, i, 9));
            float x0 = Mth.cos(a0) * inner, y0 = Mth.sin(a0) * inner, x1 = Mth.cos(a1) * inner, y1 = Mth.sin(a1) * inner, xm = Mth.cos(am) * rs, ym = Mth.sin(am) * rs;
            PropDraw.quad(pose, vc, c[0] * x0 + c[3] * y0, c[1] * x0 + c[4] * y0, c[2] * x0 + c[5] * y0, c[0] * x1 + c[3] * y1, c[1] * x1 + c[4] * y1, c[2] * x1 + c[5] * y1,
                    c[0] * xm + c[3] * ym, c[1] * xm + c[4] * ym, c[2] * xm + c[5] * ym, c[0] * xm + c[3] * ym, c[1] * xm + c[4] * ym, c[2] * xm + c[5] * ym, 0.5f, 0.5f, 0.5f, 0.5f, argb, FB);
        }
    }

    private static void gloria(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, float lod, float q) {
        float life = e.life() + partial, scale = e.scale();
        int charge = LightBoneJa.GLORIA_CHARGE;
        SealDraw.camera(pose);
        float R = 14f * scale;
        if (life < charge) {                                                      // charging: a small manga flash that swells, black spikes round a white one
            float t = SealDraw.smooth(life / charge), r = (0.6f + 2.4f * t) * scale;
            if (lod > 0.3f) jagged(pose, buf.getBuffer(AuraRender.translucent(white)), e.seed(), 14, r * 0.5f, r * 1.35f, age * 0.02f, SealDraw.argb(0.85f, 0x000000));
            jagged(pose, add(buf, white), e.seed() + 1, 14, r * 0.4f, r * 1.1f, -age * 0.03f, SealDraw.argb(0.95f * t + 0.05f, WHITE));
            sprite(pose, buf, glow, 0f, 0f, 0f, r * 2.2f, 0f, 0.5f * t, WHITE);
            return;
        }
        float t = Mth.clamp((life - charge) / 6f, 0f, 1f), out = Mth.clamp((e.maxLife() - life) / 10f, 0f, 1f);
        float reach = R * (0.25f + 0.75f * t), r = reach * 0.22f;
        int spikes = LightBoneJa.seg(26, lod, q, 8), tri = LightBoneJa.seg(22, lod, q, 8);
        VertexConsumer dark = buf.getBuffer(AuraRender.translucent(white));        // pass 1: everything black (the long black spikes and the jagged black outline)
        for (int i = 1; i < spikes; i += 2) {
            float len = reach * (0.5f + 0.5f * SealDraw.rnd(e.seed(), i, 4));
            spike(pose, dark, i, spikes, len, 0.42f * scale * (1f - 0.4f * t), SealDraw.argb(0.85f * out, 0x000000));
        }
        jagged(pose, dark, e.seed(), tri, r * 0.8f, r * 1.9f, age * 0.02f, SealDraw.argb(0.9f * out, 0x000000));
        VertexConsumer light = add(buf, white);                                    // pass 2: everything white (the long white spikes and the jagged flash over the outline)
        for (int i = 0; i < spikes; i += 2) {
            float len = reach * (0.5f + 0.5f * SealDraw.rnd(e.seed(), i, 4));
            spike(pose, light, i, spikes, len, 0.42f * scale * (1f - 0.4f * t), SealDraw.argb(0.9f * out, WHITE));
        }
        jagged(pose, light, e.seed() + 1, tri, r * 0.7f, r * 1.5f, -age * 0.03f, SealDraw.argb(1f * out, WHITE));
        sprite(pose, buf, glow, 0f, 0f, 0f, reach * 0.95f, 0f, 0.85f * out * (1f - 0.4f * t), WHITE);                       // pass 3: the flare sprites
        sprite(pose, buf, star, 0f, 0f, 0f, reach * 0.7f, age * 0.05f, 0.9f * out, WHITE);
    }
}
