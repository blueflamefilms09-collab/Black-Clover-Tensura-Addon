package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.book.LightBoneJa;
import com.newuniverse.nusmp.client.SafeAdditive;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.perf.PerfSettings;
import com.newuniverse.nusmp.prop.BoneJaVariant;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Why: how the props of the Bone Magic remake look (the server half is prop.BoneJaProps, the casts are book.BoneJaArts; the spells follow the Black Clover wiki, docs/magic/light_bone_ja.md).
 * Everything is real geometry (PropDraw cones, cylinders, boxes, spheres) with a bone texture, so bones read as bones: ivory, grained, pitted, with knuckles where bones join.
 * <ul>
 *   <li>FANG (Eternal Fangs): a bone fang of three joined segments with knuckles and two barbs, pushed out of the body along its direction, held, and drawn back.</li>
 *   <li>BULLET (Bone Bullets): a sharp spinning bone fragment with fins and a streaming trail.</li>
 *   <li>DAINSLEIF (Demon Sword Dainsleif): a long bone sword (bone grip and guard with spikes, a near-black violet blade) coated in spatial magic (a violet glow, floating cracks) that swings in three arcs.</li>
 *   <li>SLASH (Spatial Rupture): a flying crescent of cut space: a black void crescent with a violet-white edge and cracks, leaving a streak.</li>
 * </ul>
 * Cost rules: a prop never draws more than {@link LightBoneJa#MAX_QUADS_PER_PROP} quads (the preview report pins it); the counts of segments are scaled by the distance from the camera
 * ({@link LightBoneJa#lod}, nothing beyond 128 blocks) and by the video quality; nothing is allocated while painting; flying props are moved here from their age, so the server sends no
 * position packets for them. Pass order: solid parts first, then the glow passes.
 */
public final class BoneJaPropPainter {
    private BoneJaPropPainter() {}

    private static final int FB = PropDraw.FULL_BRIGHT;
    private static final int IVORY = 0xF0E8D0, BONE = 0xE4DCC4, VIOLET = 0xB48CFF, PALE_VIOLET = 0xE6D8FF, DARK = 0x16082A;
    private static ResourceLocation bone, space, glow, streak, flare, crescent, white;

    /** Called once on the client by PropPainterRegistry. */
    public static void register() {
        bone = AuraRender.tex("prop/bone_ja_bone");
        space = AuraRender.tex("prop/bone_ja_space");
        glow = AuraRender.tex("particle/glow");
        streak = AuraRender.tex("particle/light_streak");
        flare = AuraRender.tex("particle/bone_flare");
        crescent = AuraRender.tex("particle/slash_crescent");
        white = ResourceLocation.withDefaultNamespace("textures/misc/white.png");
        PropPainters.register(PropKind.BONE_JA, BoneJaPropPainter::paint);
    }

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

    private static void paint(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, int light) {
        float lod = lod(e);
        if (lod <= 0f) return;
        float q = quality();
        switch (e.param() & 0xFF) {
            case BoneJaVariant.FANG -> fang(e, partial, age, pose, buf, light, lod, q);
            case BoneJaVariant.BULLET -> bullet(e, partial, age, pose, buf, light, lod, q);
            case BoneJaVariant.DAINSLEIF -> sword(e, partial, age, pose, buf, light, lod, q);
            case BoneJaVariant.SLASH -> slash(e, partial, age, pose, buf, lod, q);
            default -> { }
        }
    }

    private static VertexConsumer add(MultiBufferSource buf, ResourceLocation tex) { return buf.getBuffer(SafeAdditive.additive(tex)); }

    private static void sprite(PoseStack pose, MultiBufferSource buf, ResourceLocation tex, float x, float y, float z, float half, float rot, float a, int rgb) {
        if (a <= 0.01f || half <= 0f) return;
        SealDraw.glow(pose, add(buf, tex), x, y, z, half, rot, SealDraw.argb(a, rgb), FB);
    }

    // ================================================================ FANG
    private static void fang(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, int light, float lod, float q) {
        float life = e.life() + partial, max = Math.max(1, e.maxLife());
        float out = Mth.clamp(life / LightBoneJa.FANG_OUT, 0f, 1f), back = Mth.clamp((max - life) / 8f, 0f, 1f);
        float ease = 1f - (1f - out) * (1f - out), len = ((e.param() >> 8) & 0xFF) / 2f * ease * (0.25f + 0.75f * back);
        if (len < 0.1f) return;
        float s = e.scale();
        int sides = LightBoneJa.seg(6, lod, q, 4);
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(e.getXRot()));
        SealDraw.camera(pose);
        VertexConsumer solid = buf.getBuffer(AuraRender.cutout(bone));
        float a = 0.5f * len, b = 0.8f * len, r0 = 0.25f * s;                                    // three bones joined at knuckles: thick, middle, the point
        PropDraw.cylinder(pose, solid, 0f, 0f, 0f, 0f, 0f, a, r0, 0.19f * s, sides, false, SealDraw.argb(1f, BONE), light);
        PropDraw.cylinder(pose, solid, 0f, 0f, a, 0f, 0f, b, 0.19f * s, 0.12f * s, sides, false, SealDraw.argb(1f, IVORY), light);
        PropDraw.cylinder(pose, solid, 0f, 0f, b, 0f, 0f, len, 0.12f * s, 0f, sides, false, SealDraw.argb(1f, IVORY), light);
        if (lod > 0.5f) {
            int rings = LightBoneJa.seg(4, lod, q, 3), segs = LightBoneJa.seg(6, lod, q, 4);
            PropDraw.sphere(pose, solid, 0f, 0f, a, 0.23f * s, rings, segs, SealDraw.argb(1f, BONE), light);                // the knuckles
            PropDraw.sphere(pose, solid, 0f, 0f, b, 0.16f * s, rings, segs, SealDraw.argb(1f, BONE), light);
            for (int side = -1; side <= 1; side += 2)                                           // two barbs, where bones branch
                PropDraw.cylinder(pose, solid, 0f, 0f, 0.32f * len, side * 0.45f * len * 0.3f, side * 0.1f * len, 0.5f * len, 0.1f * s, 0f, 4, false, SealDraw.argb(1f, IVORY), light);
        }
        float burst = 1f - Mth.clamp(life / 4f, 0f, 1f);                                         // the crack of the body as the bone comes out
        sprite(pose, buf, flare, 0f, 0f, 0.3f, 0.9f * s * (0.5f + burst), age * 0.2f, 0.8f * burst, IVORY);
        pose.popPose();
    }

    // ================================================================ BULLET
    private static void bullet(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, int light, float lod, float q) {
        float life = e.life() + partial, k = Mth.clamp(life / 1f, 0f, 1f) * Mth.clamp((e.maxLife() - life) / 1f, 0f, 1f);
        if (k <= 0.01f) return;
        float s = e.scale();
        double go = LightBoneJa.travelled(LightBoneJa.speedOf(e.param()), age, Math.max(1, e.maxLife()));
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(e.getXRot()));
        pose.translate(0f, 0f, (float) go);
        SealDraw.camera(pose);
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(age * 47f));                                       // it spins round its length to bore through armour
        VertexConsumer solid = buf.getBuffer(AuraRender.cutout(bone));
        PropDraw.cylinder(pose, solid, 0f, 0f, 0.05f * s, 0f, 0f, 0.65f * s, 0.1f * s, 0f, LightBoneJa.seg(5, lod, q, 4), false, SealDraw.argb(1f, IVORY), light);
        PropDraw.cylinder(pose, solid, 0f, 0f, -0.3f * s, 0f, 0f, 0.05f * s, 0.07f * s, 0.1f * s, 5, false, SealDraw.argb(1f, BONE), light);
        for (int j = 0; j < 3; j++) {                                                           // three fins that make the spin
            float a = j * Mth.TWO_PI / 3f, c = Mth.cos(a) * 0.12f * s, sn = Mth.sin(a) * 0.12f * s;
            SealDraw.plane(pose, solid, 0f, 0f, -0.18f * s, 0f, 0f, 0.14f * s, c, sn, 0f, false, SealDraw.argb(1f, BONE), light);
            SealDraw.plane(pose, solid, 0f, 0f, -0.18f * s, 0f, 0f, 0.14f * s, c, sn, 0f, true, SealDraw.argb(1f, BONE), light);
        }
        pose.popPose();
        SealDraw.ribbon(pose, add(buf, streak), 0f, 0f, -0.3f * s, 0f, 0f, -2.2f * s, 0.1f * s, 1f, 0f, SealDraw.argb(0.7f * k, IVORY), FB);
        sprite(pose, buf, glow, 0f, 0f, 0.5f * s, 0.2f * s, 0f, 0.5f * k, PALE_VIOLET);
        pose.popPose();
    }

    // ================================================================ DAINSLEIF
    private static void sword(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, int light, float lod, float q) {
        float life = e.life() + partial, max = Math.max(1, e.maxLife());
        float k = Mth.clamp(life / 4f, 0f, 1f) * Mth.clamp((max - life) / 6f, 0f, 1f);
        if (k <= 0.01f) return;
        float s = e.scale() * (0.6f + 0.4f * k);
        float ph = (life - 3f) / LightBoneJa.DAINSLEIF_GAP, sweep = 0f;                         // the three arcs: the blade swings from right to left every 6 ticks
        boolean swinging = life >= 3f && life < 3f + LightBoneJa.DAINSLEIF_GAP * LightBoneJa.DAINSLEIF_SWINGS;
        if (swinging) { float f = ph - (float) Math.floor(ph); sweep = Mth.lerp(f * f * (3f - 2f * f), 62f, -62f); }
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(e.getXRot()));
        pose.translate(0.45f * s, 0f, 0.25f * s);
        pose.mulPose(Axis.YP.rotationDegrees(swinging ? sweep : 18f));                           // sweep about the hand, the rest pose is turned a little outward
        pose.mulPose(Axis.XP.rotationDegrees(swinging ? 8f : -14f));
        SealDraw.camera(pose);
        VertexConsumer solid = buf.getBuffer(AuraRender.cutout(bone));                          // pass 1: the bone parts
        PropDraw.cylinder(pose, solid, 0f, 0f, -0.45f * s, 0f, 0f, 0.3f * s, 0.07f * s, 0.07f * s, 6, true, SealDraw.argb(1f, BONE), light);               // the grip, a long bone
        PropDraw.sphere(pose, solid, 0f, 0f, -0.5f * s, 0.11f * s, 4, 6, SealDraw.argb(1f, IVORY), light);                                               // the pommel knuckle
        PropDraw.box(pose, solid, -0.3f * s, -0.05f * s, 0.3f * s, 0.3f * s, 0.05f * s, 0.4f * s, SealDraw.argb(1f, IVORY), light);                      // the guard
        for (int side = -1; side <= 1; side += 2)
            PropDraw.cylinder(pose, solid, side * 0.3f * s, 0f, 0.35f * s, side * 0.42f * s, 0.05f * s, 0.55f * s, 0.06f * s, 0f, 4, false, SealDraw.argb(1f, IVORY), light);    // the spikes of the guard
        VertexConsumer dark = buf.getBuffer(AuraRender.cutout(space));                          // pass 2: the blade
        pose.pushPose();
        pose.scale(0.34f, 1f, 1f);                                                              // the blade: flat, near black, long
        PropDraw.cylinder(pose, dark, 0f, 0f, 0.4f * s, 0f, 0f, 2.35f * s, 0.2f * s, 0.2f * s, 4, false, SealDraw.argb(1f, 0xFFFFFF), light);
        PropDraw.cylinder(pose, dark, 0f, 0f, 2.35f * s, 0f, 0f, 2.95f * s, 0.2f * s, 0f, 4, false, SealDraw.argb(1f, 0xFFFFFF), light);
        pose.popPose();
        VertexConsumer coat = add(buf, white);                                                  // the coat of spatial magic: a violet glow round the blade
        pose.pushPose();
        pose.scale(0.5f, 1f, 1f);
        PropDraw.cylinder(pose, coat, 0f, 0f, 0.4f * s, 0f, 0f, 2.35f * s, 0.27f * s, 0.27f * s, 4, false, SealDraw.argb(0.3f * k, VIOLET), FB);
        PropDraw.cylinder(pose, coat, 0f, 0f, 2.35f * s, 0f, 0f, 3.1f * s, 0.27f * s, 0f, 4, false, SealDraw.argb(0.3f * k, VIOLET), FB);
        pose.popPose();
        VertexConsumer lines = add(buf, streak);
        SealDraw.ribbon(pose, lines, 0f, 0f, 0.45f * s, 0f, 0f, 3.0f * s, 0.07f * s, 0f, 1f, SealDraw.argb(0.9f * k, PALE_VIOLET), FB);               // the bright edge
        int cracks = LightBoneJa.seg(3, lod, q, 1);
        for (int i = 0; i < cracks; i++) {                                                      // cracks of cut space that float along the blade
            float z = (0.9f + 0.6f * i + ((age * 0.05f + i * 0.31f) % 1f) * 0.5f) * s;
            SealDraw.ribbon(pose, lines, 0.02f * s, 0.34f * s * (i % 2 == 0 ? 1 : -1), z, 0.02f * s, -0.34f * s * (i % 2 == 0 ? 1 : -1), z + 0.12f * s, 0.035f * s, 0f, 1f, SealDraw.argb(0.85f * k, VIOLET), FB);
        }
        sprite(pose, buf, glow, 0f, 0f, 2.9f * s, 0.5f * s, 0f, 0.6f * k, VIOLET);
        pose.popPose();
        if (swinging) {                                                                          // the arc of the swing: a crescent of violet light at the tip
            pose.pushPose();
            SealDraw.camera(pose);
            sprite(pose, buf, crescent, 0f, 0.1f, 1.9f * s, 1.8f * s, Mth.TWO_PI * (ph - (float) Math.floor(ph)), 0.7f * k, VIOLET);
            pose.popPose();
        }
    }

    // ================================================================ SLASH
    private static void slash(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, float lod, float q) {
        float life = e.life() + partial, max = Math.max(1, e.maxLife());
        boolean shortCut = (e.param() & BoneJaVariant.SHORT) != 0;
        float k = Mth.clamp(life / 1.5f, 0f, 1f) * Mth.clamp((max - life) / (shortCut ? 1.5f : 3f), 0f, 1f);
        if (k <= 0.01f) return;
        float s = e.scale() * (shortCut ? 0.9f : 1.5f), roll = (e.seed() & 1023) / 1023f * 80f - 40f + (shortCut ? 20f : 0f);
        double go = LightBoneJa.travelled(LightBoneJa.speedOf(e.param()), age, Math.max(1, e.maxLife()));
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(e.getXRot()));
        pose.translate(0f, 0f, (float) go);
        pose.mulPose(Axis.ZP.rotationDegrees(roll));
        SealDraw.camera(pose);
        VertexConsumer void_ = buf.getBuffer(AuraRender.translucent(crescent));                 // pass 1: the dark of the cut, in the shape of the crescent (two sizes: a thick black edge)
        float w = 1.4f * s, grow = 0.6f + 0.4f * k;
        for (int side = 0; side < 2; side++) {
            float sz = w * grow * (1.02f + 0.07f * side);
            SealDraw.plane(pose, void_, 0f, 0f, 0.02f, sz, 0f, 0f, 0f, sz, 0f, false, SealDraw.argb(0.9f * k, DARK), FB);
            SealDraw.plane(pose, void_, 0f, 0f, 0.02f, sz, 0f, 0f, 0f, sz, 0f, true, SealDraw.argb(0.9f * k, DARK), FB);
        }
        VertexConsumer rim = add(buf, crescent);                                                // pass 2: the edge of the cut
        SealDraw.plane(pose, rim, 0f, 0f, 0f, w * grow, 0f, 0f, 0f, w * grow, 0f, false, SealDraw.argb(0.95f * k, PALE_VIOLET), FB);
        SealDraw.plane(pose, rim, 0f, 0f, 0f, w * grow, 0f, 0f, 0f, w * grow, 0f, true, SealDraw.argb(0.95f * k, PALE_VIOLET), FB);
        SealDraw.plane(pose, rim, 0f, 0f, 0f, 1.15f * w * grow, 0f, 0f, 0f, 1.15f * w * grow, 0f, false, SealDraw.argb(0.5f * k, VIOLET), FB);
        SealDraw.ribbon(pose, add(buf, streak), 0f, 0f, 0f, 0f, 0f, -3.0f * s, 0.26f * s, 1f, 0f, SealDraw.argb(0.6f * k, VIOLET), FB);
        if (!shortCut && lod > 0.5f) {
            VertexConsumer lines = add(buf, streak);
            for (int i = 0; i < 2; i++) {                                                         // two cracks of cut space across the crescent
                float a = (0.4f + 0.5f * i) * Mth.PI;
                SealDraw.ribbon(pose, lines, Mth.cos(a) * 0.2f * s, Mth.sin(a) * 0.2f * s, 0.03f, Mth.cos(a) * 1.3f * s, Mth.sin(a) * 1.3f * s, 0.03f, 0.04f * s, 0f, 1f, SealDraw.argb(0.8f * k, PALE_VIOLET), FB);
            }
        }
        pose.popPose();
    }
}
