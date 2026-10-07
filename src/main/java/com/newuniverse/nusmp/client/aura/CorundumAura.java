package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.client.prop.CorundumDraw;
import com.newuniverse.nusmp.client.prop.PropDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 0.54 Corundum Magic: the player render layer (Aura.CORUNDUM), the gem armour. Layer space: y points down, the neck line is y = 0, the middle of the
 * torso is at y = 0.3, the feet at y = 1.5, the back is +z. Style 0 is the armour; style 1 adds a crystal crown and wings of shards.
 * Passes, in draw order (every pass names its render type; nothing is drawn over the face, the head of the skin texture is empty):
 *   1. the plates: the posed player model drawn again 12 % wider and 2 % taller with textures/aura/corundum_plates.png (a 64x64 skin layout, plates only),
 *      cutout (entityCutoutNoCull); translucent (entityTranslucent) only while the aura fades in or out, because cutout cannot fade;
 *   2. the gem pieces: GeoLite models (cutout, with their glow maps additive straight after each piece), each attached to the posed part it belongs to
 *      (body: chest, pauldrons, back crystals; arms: vambraces; legs: greaves; head: the crown, body: the wings of style 1), so they swing with the limbs;
 *   3. the glow of the plates: the same model again with corundum_plates_glow.png, additive (RenderType.eyes), full bright, 12.5 % wider;
 *   4. glints: camera-facing corundum_star sprites on the gems and rising sparkles, additive (eyes), full bright.
 * For the local player in first person the head pieces and the shoulder crystals are left out (the vanilla layer does not draw the player there anyway).
 * Cost: about 130 cubes with their glow passes and 40 sprites at most.
 */
public final class CorundumAura {
    private CorundumAura() {}

    private static final GeoSpec ARMOUR = GeoSpec.of("corundum", "armour"), ARM_R = GeoSpec.of("corundum", "arm_r"), ARM_L = GeoSpec.of("corundum", "arm_l"),
            LEG_R = GeoSpec.of("corundum", "leg_r"), LEG_L = GeoSpec.of("corundum", "leg_l"), CROWN = GeoSpec.of("corundum", "crown"),
            WINGS = GeoSpec.of("corundum", "wings");
    private static final int FB = PropDraw.FULL_BRIGHT, RUBY = 0xFF6A7A, SAPPHIRE = 0x6AA6FF, WHITE = 0xFFFFFF;
    /** Fixed glints in layer space: x, y, z, size, colour (0 ruby, 1 sapphire), style needed. */
    private static final float[][] GLINTS = {
            {0f, 0.31f, -0.40f, 0.20f, 1f, 0f}, {-0.62f, -0.30f, 0f, 0.15f, 0f, 0f}, {0.62f, -0.30f, 0f, 0.15f, 0f, 0f},
            {0f, -0.22f, 0.30f, 0.14f, 1f, 0f}, {-0.30f, 0.40f, -0.36f, 0.10f, 0f, 0f}, {0.30f, 0.40f, -0.36f, 0.10f, 0f, 0f},
            {0f, -0.60f, -0.30f, 0.13f, 0f, 1f}, {-0.82f, -0.58f, 0.42f, 0.15f, 1f, 1f}, {0.82f, -0.58f, 0.42f, 0.15f, 1f, 1f}};
    private static final int SPARKLES = 8;
    private static ResourceLocation plates, platesGlow, star;

    /** Called once by AuraRegistry on the client. */
    public static void register() {
        plates = AuraRender.tex("aura/corundum_plates");
        platesGlow = AuraRender.tex("aura/corundum_plates_glow");
        star = AuraRender.tex("particle/corundum_star");
        PlayerAuraClient.register(Aura.CORUNDUM, CorundumAura::paint);
    }

    private static void paint(AuraContext c) {
        float f = c.fade();
        if (f <= 0.002f) return;
        boolean self1p = c.player() == Minecraft.getInstance().player && Minecraft.getInstance().options.getCameraType().isFirstPerson();
        boolean crowned = c.style() == 1;
        float age = c.age();
        float pulse = 0.8f + 0.2f * Mth.sin(age * 0.14f);
        boolean solid = f >= 0.98f;
        GeoDraw.Layer layer = solid ? GeoDraw.Layer.CUTOUT : GeoDraw.Layer.TRANSLUCENT;
        int white = CorundumDraw.argb(solid ? 1f : f, WHITE);
        int glow = CorundumDraw.argb(Mth.clamp(f * (0.55f + 0.45f * pulse), 0f, 1f), WHITE);

        // 1. the plates over the skin (cutout; translucent only while fading)
        AuraRender.model(c, solid ? AuraRender.cutout(plates) : AuraRender.translucent(plates), 1.12f, 1.02f, 1.12f, white);

        // 2. the gem pieces, each on the part it belongs to
        if (!self1p) part(c, c.model().body, ARMOUR, layer, white, glow, age);
        part(c, c.model().rightArm, ARM_R, layer, white, 0, age);
        part(c, c.model().leftArm, ARM_L, layer, white, 0, age);
        part(c, c.model().rightLeg, LEG_R, layer, white, 0, age);
        part(c, c.model().leftLeg, LEG_L, layer, white, 0, age);
        if (crowned) {
            if (!self1p) part(c, c.model().head, CROWN, layer, white, glow, age);
            if (!self1p) part(c, c.model().body, WINGS, layer, white, glow, age);
        }

        // 3. the plates' glow (the fire in the gems and the seams): additive, full bright
        AuraRender.modelBright(c, AuraRender.additive(platesGlow), 1.125f, 1.025f, 1.125f, CorundumDraw.argb(0.8f * f * pulse, WHITE));

        // 4. glints on the gems and sparkles rising round the body: additive, camera-facing
        PoseStack pose = c.pose();
        CorundumDraw.camera(pose);
        VertexConsumer vc = c.buffers().getBuffer(AuraRender.additive(star));
        for (int i = 0; i < GLINTS.length; i++) {
            float[] g = GLINTS[i];
            if (g[5] > c.style() || (self1p && g[1] < -0.2f)) continue;
            float tw = Mth.sin(age * 0.17f + i * 1.9f) * 0.5f + 0.5f;
            tw *= tw;
            int col = CorundumDraw.mix(g[4] > 0.5f ? SAPPHIRE : RUBY, WHITE, 0.45f);
            CorundumDraw.glow(pose, vc, g[0], g[1], g[2], g[3] * (0.5f + 0.9f * tw), age * 0.03f + i, CorundumDraw.argb(0.9f * f * (0.35f + 0.65f * tw), col), FB);
        }
        for (int i = 0; i < SPARKLES; i++) {
            float ph = (age * 0.021f + i * 0.137f) % 1f;
            float a = i * 2.399f + age * 0.012f;
            float y = 1.35f - ph * 1.95f;
            if (self1p && y < 0.1f) continue;
            float r = 0.46f + 0.1f * Mth.sin(i * 3.1f);
            float fade = Mth.sin(ph * Mth.PI);
            CorundumDraw.glow(pose, vc, Mth.cos(a) * r, y, Mth.sin(a) * r * 0.8f, 0.05f + 0.05f * fade, age * 0.08f + i,
                    CorundumDraw.argb(0.75f * f * fade, i % 2 == 0 ? RUBY : SAPPHIRE), FB);
        }
    }

    /**
     * Draws a GeoLite piece that was authored on the standing humanoid, attached to a posed model part: the piece turns about the part's own pivot with
     * the swing of the limb (translateAndRotate, then back by the part's offset so the file's absolute coordinates apply).
     */
    private static void part(AuraContext c, ModelPart p, GeoSpec spec, GeoDraw.Layer layer, int argb, int glow, float age) {
        PoseStack pose = c.pose();
        pose.pushPose();
        p.translateAndRotate(pose);
        pose.translate(-p.x / 16f, -p.y / 16f, -p.z / 16f);
        GeoDraw.paint(pose, c.buffers(), spec, "idle", age / 20f, GeoDraw.Space.PLAYER, layer, c.light(), argb, glow);
        pose.popPose();
    }
}
