package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.client.prop.PropDraw;
import com.newuniverse.nusmp.client.prop.SealDraw;
import com.newuniverse.nusmp.vfx.WikiStyles;
import com.newuniverse.nusmp.vfx.WikiStyles.Group;
import com.newuniverse.nusmp.vfx.client.VfxTextures;
import com.newuniverse.nusmp.vfx.client.WikiTextures;
import com.newuniverse.nusmp.vfx.client.layer.WikiMagicLayer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 1.27.0: the temporary armour / aura of the 30 wiki magics (Aura.WIKI). The style the spell passes to PlayerAuras.set is the magic's
 * {@link WikiStyles} index, so ONE painter serves all 30: the group picks the armour language, the magic's colour tints it and its
 * place in the group varies the counts. Documentation: docs/wiki_magic_vfx.md.
 * <pre>
 *   MOTES     a soft wind veil over the body, motes orbiting and rising (Snow falls, Smoke billows), two drifting streaks
 *   TRAILS    crescent blades orbiting the torso (Wing: a pair of feather blades on the back that beat)
 *   CIRCLES   a rune circle at the feet, a tilted ring at the waist, a halo behind the back
 *   SHARDS    a faceted shell, crystal spikes on the back, shoulders and forearms
 *   FLUID     a glossy translucent gel shell that wobbles, drops falling from the hands (Mud is brown)
 *   SPIRAL    ribbons winding up the body (a tight cone for the vortex), a faint flowing shell
 *   INFERNAL  a dark cracked shell with glowing fissures, a dark seal behind the back, embers
 * </pre>
 * Budget: at most two extra passes of the player model, a few dozen quads, no allocation (SealDraw / PropDraw take plain floats; every
 * texture comes from the cached {@link WikiTextures} arrays, a missing one is the glow). Layer space as in SealAura: y points DOWN,
 * the feet are at y = 1.5, the back is +z.
 */
public final class WikiAura {
    private WikiAura() {}

    private static final int FB = PropDraw.FULL_BRIGHT;
    private static final float TAU = Mth.TWO_PI;

    /** Called once by AuraRegistry on the client. */
    public static void register() {
        PlayerAuraClient.register(Aura.WIKI, WikiAura::paint);
    }

    private static void paint(AuraContext c) {
        float f = c.fade();
        if (f <= 0.002f) return;
        int style = WikiStyles.valid(c.style()) ? c.style() : 0;
        Group g = WikiStyles.group(style);
        int v = WikiStyles.variant(style), rgb = WikiMagicLayer.glowRgbOf(style);
        SealDraw.camera(c.pose());
        switch (g) {
            case TRAILS -> trails(c, f, style, v, rgb);
            case CIRCLES -> circles(c, f, style, v, rgb);
            case SHARDS -> shards(c, f, v, rgb);
            case FLUID -> fluid(c, f, style, v, rgb);
            case SPIRAL -> spiral(c, f, style, v, rgb);
            case INFERNAL -> infernal(c, f, style, rgb);
            default -> motes(c, f, v, rgb);
        }
    }

    private static float frac(float x) { return x - (float) Math.floor(x); }
    private static int pale(int rgb) { return SealDraw.mix(rgb, 0xFFFFFF, 0.55f); }

    /** A vertical square (in the xy plane at z = cz), both sides, turned by rot radians: halos behind the back. */
    private static void upright(PoseStack pose, VertexConsumer vc, float cx, float cy, float cz, float half, float rot, int argb) {
        float c = Mth.cos(rot) * half, s = Mth.sin(rot) * half;
        SealDraw.plane(pose, vc, cx, cy, cz, c, s, 0f, -s, c, 0f, false, argb, FB);
        SealDraw.plane(pose, vc, cx, cy, cz, c, s, 0f, -s, c, 0f, true, argb, FB);
    }

    // ================================================================== MOTES: a wind veil with motes drifting up (hang, fall or billow)
    private static void motes(AuraContext c, float f, int v, int rgb) {
        float age = c.age(), rise = v == 1 ? 0.45f : v == 2 ? -0.35f : 0f;
        PoseStack pose = c.pose();
        int tintRgb = v == 1 ? SealDraw.mix(rgb, 0x4A4654, 0.4f) : rgb;
        AuraRender.modelBright(c, AuraRender.emissive(WikiTextures.shell(Group.MOTES)), 1.06f + 0.012f * Mth.sin(age * 0.1f), 1.03f, 1.06f,
                SealDraw.argb(0.3f * f, tintRgb));
        VertexConsumer vc = c.buffers().getBuffer(AuraRender.emissive(WikiTextures.bulk(Group.MOTES)));
        for (int i = 0; i < 10; i++) {
            float ph = frac(age * 0.012f * (1f + 0.3f * v) + i * 0.618f), a = age * 0.05f * (i % 2 == 0 ? 1f : -1f) + i * 2.399f;
            float r = 0.55f + 0.2f * Mth.sin(i * 1.7f), h = (rise < 0f ? 1f - ph : ph) * 2.1f, wob = Mth.sin(i * 1.3f);
            SealDraw.glow(pose, vc, Mth.cos(a) * r, 1.5f - h, Mth.sin(a) * r, 0.1f + 0.05f * wob * wob, age * 0.1f + i,
                    SealDraw.argb(Mth.sin(ph * Mth.PI) * 0.85f * f, v == 1 ? tintRgb : pale(rgb)), FB);
        }
        VertexConsumer streak = c.buffers().getBuffer(AuraRender.additive(WikiTextures.bulk(Group.TRAILS)));
        for (int side = -1; side <= 1; side += 2) {                                              // two streaks drifting off the shoulders
            float sway = Mth.sin(age * 0.11f + side) * 0.1f;
            SealDraw.ribbon(pose, streak, side * 0.32f + sway, 0.2f, 0.95f, side * 0.3f, 0.02f, 0.12f, 0.07f, 0f, 1f, SealDraw.argb(0.55f * f, pale(rgb)), FB);
        }
    }

    // ================================================================== TRAILS: crescent blades orbiting the torso
    private static void trails(AuraContext c, float f, int style, int v, int rgb) {
        float age = c.age();
        PoseStack pose = c.pose();
        VertexConsumer vc = c.buffers().getBuffer(AuraRender.additive(WikiTextures.bulk(Group.TRAILS)));
        VertexConsumer acc = c.buffers().getBuffer(AuraRender.additive(WikiTextures.accent(style)));
        int col = SealDraw.argb(0.9f * f, rgb), head = SealDraw.argb(0.95f * f, pale(rgb));
        if (v == 3) {                                                                             // wing: a pair of feather blades beating on the back
            for (int side = -1; side <= 1; side += 2) {
                float beat = Mth.sin(age * 0.14f) * 0.25f;
                for (int k = 0; k < 3; k++) {
                    float spread = 0.55f + 0.32f * k + beat, rise = 0.15f - 0.28f * k + 0.1f * beat;
                    SealDraw.ribbon(pose, vc, side * 0.22f, 0.12f + 0.12f * k, 0.2f, side * (0.22f + 0.85f * Mth.sin(spread)), rise - 0.1f, 0.35f + 0.6f * Mth.cos(spread * 0.5f),
                            0.13f - 0.02f * k, 0f, 1f, col, FB);
                }
                SealDraw.glow(pose, acc, side * 0.3f, 0.1f, 0.22f, 0.12f, age * 0.1f * side, head, FB);
            }
            return;
        }
        int blades = v == 0 ? 3 : 2;
        float sweep = v == 0 ? 0.55f : v == 1 ? 0.7f : 0.85f, dir = v == 2 ? -1f : 1f;
        for (int i = 0; i < blades; i++) {
            float a = age * 0.07f * dir + i * TAU / blades, ca = Mth.cos(a), sa = Mth.sin(a);
            float cx = ca * 0.62f, cz = sa * 0.62f, cy = 0.4f + 0.15f * Mth.sin(age * 0.1f + i * 2f), tilt = (i % 2 == 0 ? 0.4f : -0.4f);
            float tx = -sa * dir, tz = ca * dir;                                                  // the way the blade travels
            float px = cx, py = cy, pz = cz;
            for (int k = 1; k <= 6; k++) {                                                        // a crescent made of six ribbon pieces
                float x = (k / 6f * 2f - 1f) * sweep, bulge = (1f - (x / sweep) * (x / sweep)) * 0.22f;
                float nx = cx + tx * x, nz = cz + tz * x, ny = cy - bulge + tilt * x;
                SealDraw.ribbon(pose, vc, px, py, pz, nx, ny, nz, 0.045f + 0.06f * Mth.sin(k / 6f * Mth.PI), (k - 1) / 6f, k / 6f, col, FB);
                px = nx; py = ny; pz = nz;
            }
            SealDraw.glow(pose, acc, px, py, pz, 0.12f, age * 0.12f + i, head, FB);               // thorn / leaf / feather / hair tuft at the head of the blade
        }
    }

    // ================================================================== CIRCLES: rune circles at the feet, the waist and behind the back
    private static void circles(AuraContext c, float f, int style, int v, int rgb) {
        float age = c.age(), grow = 0.6f + 0.4f * f;
        PoseStack pose = c.pose();
        AuraRender.modelBright(c, AuraRender.additive(WikiTextures.shell(Group.CIRCLES)), 1.04f, 1.04f, 1.04f, SealDraw.argb(0.22f * f, rgb));
        VertexConsumer ring = c.buffers().getBuffer(AuraRender.additive(WikiTextures.bulk(Group.CIRCLES)));
        VertexConsumer glyph = c.buffers().getBuffer(AuraRender.additive(WikiTextures.accent(style)));
        float dir = v % 2 == 0 ? 1f : -1f;
        pose.pushPose();                                                                         // the circle at the feet
        pose.translate(0f, 1.47f, 0f);
        SealDraw.flat(pose, ring, 0.95f * grow, age * 1.4f * dir, SealDraw.argb(0.9f * f, rgb), FB);
        SealDraw.flat(pose, glyph, 0.62f * grow, -age * 2.2f * dir, SealDraw.argb(0.8f * f, pale(rgb)), FB);
        pose.popPose();
        pose.pushPose();                                                                         // the tilted ring at the waist
        pose.translate(0f, 0.4f + 0.04f * Mth.sin(age * 0.09f), 0f);
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(6f * Mth.sin(age * 0.05f + v)));
        SealDraw.flat(pose, ring, 0.7f, -age * 1.8f * dir, SealDraw.argb(0.6f * f, pale(rgb)), FB);
        pose.popPose();
        upright(pose, ring, 0f, 0.3f, 0.42f, 0.78f * grow, age * 0.03f * dir, SealDraw.argb(0.6f * f, rgb));        // the halo behind the back
        upright(pose, glyph, 0f, 0.3f, 0.44f, 0.5f * grow, -age * 0.05f * dir, SealDraw.argb(0.75f * f, pale(rgb)));
    }

    // ================================================================== SHARDS: a faceted shell and crystal spikes
    private static void shards(AuraContext c, float f, int v, int rgb) {
        float age = c.age();
        PoseStack pose = c.pose();
        ResourceLocation tex = WikiTextures.shell(Group.SHARDS);
        AuraRender.modelBright(c, AuraRender.additive(tex), 1.05f, 1.05f, 1.05f, SealDraw.argb(0.3f * f, rgb));
        VertexConsumer solid = c.buffers().getBuffer(AuraRender.cutout(tex));
        int light = SealDraw.lift(c.light(), 9), col = SealDraw.argb(f, SealDraw.mix(rgb, 0xFFFFFF, 0.15f));
        float len = (0.2f + 0.025f * (v % 4)) * (0.5f + 0.5f * f);
        int back = 4 + v % 3;
        pose.pushPose();
        c.model().body.translateAndRotate(pose);
        for (int k = 0; k < back; k++) {                                                          // the spine
            float y = 0.1f + k * (0.6f / back), l = len * (1.2f - 0.15f * k + 0.1f * Mth.sin(age * 0.1f + k));
            spike(pose, solid, 0f, y, 0.19f, 0f, -0.2f, 0.95f, l, 0.045f, col, light);
        }
        pose.popPose();
        for (int side = -1; side <= 1; side += 2) {
            ModelPart arm = side < 0 ? c.model().rightArm : c.model().leftArm;
            pose.pushPose();
            arm.translateAndRotate(pose);
            spike(pose, solid, side * 0.04f, 0.0f, 0f, side * 0.9f, -0.55f, 0f, len * 1.5f, 0.06f, col, light);          // the shoulder crystal
            spike(pose, solid, side * 0.0f, -0.02f, 0f, 0f, -1f, 0.1f, len * 1.1f, 0.05f, col, light);
            for (int k = 0; k < 3; k++) spike(pose, solid, side * 0.075f, 0.2f + k * 0.14f, 0f, side * 1f, 0.15f, 0f, len * 0.55f, 0.03f, col, light);    // the bracer
            pose.popPose();
        }
    }

    /** A four-sided pyramid from (bx,by,bz) along (dx,dy,dz) (any length: it is normalised) for 'len' blocks, base half width r. */
    private static void spike(PoseStack pose, VertexConsumer vc, float bx, float by, float bz, float dx, float dy, float dz, float len, float r, int argb, int light) {
        float dl = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (dl < 1e-5f) return;
        dx /= dl; dy /= dl; dz /= dl;
        float hx = Math.abs(dy) < 0.9f ? 0f : 1f, hy = Math.abs(dy) < 0.9f ? 1f : 0f;
        float ux = dy * 0f - dz * hy, uy = dz * hx - dx * 0f, uz = dx * hy - dy * hx;            // d x h
        float ul = Mth.sqrt(ux * ux + uy * uy + uz * uz);
        if (ul < 1e-5f) return;
        ux = ux / ul * r; uy = uy / ul * r; uz = uz / ul * r;
        float wx = dy * uz - dz * uy, wy = dz * ux - dx * uz, wz = dx * uy - dy * ux;                  // d x u: already r long
        float tx = bx + dx * len, ty = by + dy * len, tz = bz + dz * len;
        float ax = bx - ux - wx, ay = by - uy - wy, az = bz - uz - wz, bx2 = bx + ux - wx, by2 = by + uy - wy, bz2 = bz + uz - wz;
        float cx = bx + ux + wx, cy = by + uy + wy, cz = bz + uz + wz, ex = bx - ux + wx, ey = by - uy + wy, ez = bz - uz + wz;
        PropDraw.quad(pose, vc, ax, ay, az, bx2, by2, bz2, tx, ty, tz, tx, ty, tz, 0f, 0f, 1f, 1f, argb, light);
        PropDraw.quad(pose, vc, bx2, by2, bz2, cx, cy, cz, tx, ty, tz, tx, ty, tz, 0f, 0f, 1f, 1f, argb, light);
        PropDraw.quad(pose, vc, cx, cy, cz, ex, ey, ez, tx, ty, tz, tx, ty, tz, 0f, 0f, 1f, 1f, argb, light);
        PropDraw.quad(pose, vc, ex, ey, ez, ax, ay, az, tx, ty, tz, tx, ty, tz, 0f, 0f, 1f, 1f, argb, light);
    }

    // ================================================================== FLUID: a glossy gel shell and drops falling from the hands
    private static void fluid(AuraContext c, float f, int style, int v, int rgb) {
        float age = c.age();
        PoseStack pose = c.pose();
        int body = v == 1 ? SealDraw.mix(rgb, 0x4A2E1E, 0.45f) : rgb;
        float wob = 0.012f * Mth.sin(age * 0.15f);
        AuraRender.model(c, AuraRender.translucent(WikiTextures.shell(Group.FLUID)), 1.05f + wob, 1.04f - wob, 1.05f + wob, SealDraw.argb(0.5f * f, body));
        AuraRender.modelBright(c, AuraRender.additive(WikiTextures.shell(Group.FLUID)), 1.06f - wob, 1.05f, 1.06f - wob, SealDraw.argb(0.12f * f, pale(rgb)));
        VertexConsumer drop = c.buffers().getBuffer(AuraRender.translucent(WikiTextures.bulk(Group.FLUID)));
        int light = SealDraw.lift(c.light(), 10);
        for (int side = -1; side <= 1; side += 2) {
            ModelPart arm = side < 0 ? c.model().rightArm : c.model().leftArm;
            pose.pushPose();
            arm.translateAndRotate(pose);
            for (int k = 0; k < 3; k++) {
                float ph = frac(age * 0.035f + k * 0.34f + (side > 0 ? 0.17f : 0f)), y = 0.7f + ph * 0.8f, h = 0.045f * (1f - 0.5f * ph);
                int a = SealDraw.argb(Mth.sin(ph * Mth.PI) * 0.85f * f, body);
                SealDraw.plane(pose, drop, 0f, y, 0f, h, 0f, 0f, 0f, h * 1.6f, 0f, false, a, light);
                SealDraw.plane(pose, drop, 0f, y, 0f, 0f, 0f, h, 0f, h * 1.6f, 0f, false, a, light);
            }
            pose.popPose();
        }
        if (v == 2) {                                                                             // the tongue lashes: one ribbon from the mouth
            VertexConsumer vc = c.buffers().getBuffer(AuraRender.additive(WikiTextures.accent(style)));
            float lash = 0.5f + 0.5f * Mth.sin(age * 0.2f);
            SealDraw.ribbon(pose, vc, 0f, -0.2f, -0.12f, 0.1f * Mth.sin(age * 0.3f), -0.12f + 0.1f * lash, -0.55f - 0.25f * lash, 0.09f, 0f, 1f, SealDraw.argb(0.7f * f, rgb), FB);
        }
    }

    // ================================================================== SPIRAL: ribbons winding up the body
    private static void spiral(AuraContext c, float f, int style, int v, int rgb) {
        float age = c.age();
        PoseStack pose = c.pose();
        AuraRender.modelBright(c, AuraRender.additive(WikiTextures.shell(Group.SPIRAL)), 1.045f, 1.045f, 1.045f, SealDraw.argb(0.22f * f, rgb));
        VertexConsumer vc = c.buffers().getBuffer(AuraRender.additive(WikiTextures.bulk(Group.TRAILS)));
        VertexConsumer acc = c.buffers().getBuffer(AuraRender.additive(WikiTextures.accent(style)));
        int strands = v == 2 ? 3 : 2, pieces = 10;
        float radius = v == 2 ? 0.55f : 0.5f;
        for (int s = 0; s < strands; s++) {
            float px = 0f, py = 0f, pz = 0f;
            for (int i = 0; i <= pieces; i++) {
                float t = i / (float) pieces, phi = age * 0.1f * (v == 2 ? 1.4f : 1f) + t * TAU * 1.5f + s * TAU / strands;
                float r = radius * (v == 2 ? 1.15f - 0.65f * t : 0.85f + 0.15f * Mth.sin(t * Mth.PI));
                float x = Mth.cos(phi) * r, y = 1.45f - t * 2.1f, z = Mth.sin(phi) * r;
                if (i > 0) SealDraw.ribbon(pose, vc, px, py, pz, x, y, z, 0.06f, (i - 1) / (float) pieces, i / (float) pieces,
                        SealDraw.argb((0.35f + 0.6f * t) * f, i % 2 == 0 ? pale(rgb) : rgb), FB);
                px = x; py = y; pz = z;
            }
            float ph = frac(age * 0.04f + s * 0.33f), phi = age * 0.1f + ph * TAU * 1.5f + s * TAU / strands, r = radius * 0.9f;
            SealDraw.glow(pose, acc, Mth.cos(phi) * r, 1.45f - ph * 2.1f, Mth.sin(phi) * r, 0.1f, age * 0.12f, SealDraw.argb(0.9f * f, pale(rgb)), FB);       // a note / flake / mote riding the strand
        }
    }

    // ================================================================== INFERNAL: a dark cracked shell, a dark seal behind, embers
    private static void infernal(AuraContext c, float f, int style, int rgb) {
        float age = c.age(), pulse = 0.6f + 0.4f * Mth.sin(age * 0.2f);
        PoseStack pose = c.pose();
        ResourceLocation shell = WikiTextures.shell(Group.INFERNAL);
        AuraRender.model(c, AuraRender.translucent(shell), 1.05f, 1.03f, 1.05f, SealDraw.argb(0.72f * f, SealDraw.mix(0x14060A, rgb, 0.12f)));
        AuraRender.modelBright(c, AuraRender.additive(shell), 1.06f, 1.04f, 1.06f, SealDraw.argb(0.5f * f * pulse, 0xFF3A20));
        VertexConsumer dark = c.buffers().getBuffer(AuraRender.translucent(WikiTextures.bulk(Group.INFERNAL)));
        int light = SealDraw.lift(c.light(), 6);
        pose.pushPose();                                                                         // the dark seal on the ground
        pose.translate(0f, 1.46f, 0f);
        float half = 0.95f * (0.6f + 0.4f * f);
        float cs = Mth.cos(age * 0.015f) * half, sn = Mth.sin(age * 0.015f) * half;
        SealDraw.plane(pose, dark, 0f, 0f, 0f, cs, 0f, sn, -sn, 0f, cs, false, SealDraw.argb(0.85f * f, 0x1A0A10), light);
        SealDraw.plane(pose, dark, 0f, 0f, 0f, cs, 0f, sn, -sn, 0f, cs, true, SealDraw.argb(0.85f * f, 0x1A0A10), light);
        pose.popPose();
        VertexConsumer skull = c.buffers().getBuffer(AuraRender.additive(WikiTextures.accent(style)));
        upright(pose, skull, 0f, 0.3f, 0.42f, 0.55f * (0.6f + 0.4f * f), 0f, SealDraw.argb(0.45f * f * pulse, 0xE86A58));      // the skull sigil behind the back
        VertexConsumer ember = c.buffers().getBuffer(AuraRender.additive(VfxTextures.SPARK));
        for (int i = 0; i < 10; i++) {
            float ph = frac(age * 0.03f + i * 0.37f), a = i * 2.399f + age * 0.03f, r = 0.45f + 0.2f * Mth.sin(i * 1.9f);
            SealDraw.glow(pose, ember, Mth.cos(a) * r, 1.45f - ph * 2.0f, Mth.sin(a) * r, 0.06f, a, SealDraw.argb(Mth.sin(ph * Mth.PI) * 0.9f * f, 0xFF5A32), FB);
        }
    }
}
