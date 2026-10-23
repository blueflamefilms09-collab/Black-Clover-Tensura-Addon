package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.book.SpiritDiveAnimeGeometry.Shape;
import com.newuniverse.nusmp.book.SpiritDivePieces.Element;
import com.newuniverse.nusmp.book.SpiritDiveReelGeometry;
import com.newuniverse.nusmp.book.SpiritDiveReelRules;
import com.newuniverse.nusmp.book.SpiritDiveReelRules.Pal;
import com.newuniverse.nusmp.book.SpiritDiveReelRules.Spec;
import com.newuniverse.nusmp.client.SafeAdditive;
import com.newuniverse.nusmp.client.prop.PropDraw;
import com.newuniverse.nusmp.client.prop.SealDraw;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

/**
 * Spirit Dive, REEL look: the pieces of the look that are drawn as solid glowing blocks and smooth strips, the way the videos draw them. Every method draws with the textures of
 * {@code tools/gen_spirit_dive_reel_textures.py} and the numbers of {@link SpiritDiveReelRules} / {@link SpiritDiveReelGeometry}; nothing is allocated (statics only; the render thread is the only
 * user).
 * <ul>
 *   <li>the GAUNTLETS: a glowing cube on the forearm (a band round it and a bright glow copy) and a fist cube past the hand, in the arm's own frame so they follow every pose of the arm;</li>
 *   <li>the CROWN: a ring of glowing cubes on the head with a vertical bar and a white light on some of them, the gold ring halo of the V2 design under them, Noelle's thin halo above;</li>
 *   <li>the WINGS: the hex-membrane leaf (Wind) or a fan of thin blades (a comb: Water; flame feathers: Fire; crystal blades: Earth), unfolding with an overshoot;</li>
 *   <li>the GOLD BANDS of the V2 design on the upper arms and thighs;</li>
 *   <li>the WEAPONS of the character tier: Yuno's spirit sword with the magic circle at its hilt, Fuegoleon's flame blade, Noelle's spiky ball of water blades and her crystal spire.</li>
 * </ul>
 */
final class SpiritDiveReelPieces {
    private SpiritDiveReelPieces() {}

    private static final int FB = PropDraw.FULL_BRIGHT;
    private static final Quaternionf Q = new Quaternionf();                                                   // scratch
    private static final float[] BLADE = new float[SpiritDiveReelGeometry.BLADE_ROW];                         // scratch: one blade of a comb
    private static final float[] LEAF = new float[SpiritDiveReelGeometry.LEAF_ROW];                           // scratch: one leaf row, scaled

    private static final ResourceLocation[] CUBE = new ResourceLocation[4], BAR = new ResourceLocation[4], WING = new ResourceLocation[4], WING_GLOW = new ResourceLocation[4],
            CIRCLE = new ResourceLocation[4];
    private static final ResourceLocation RING_GOLD = AuraRender.tex(SpiritDiveReelRules.RING_GOLD), HALO_RING = AuraRender.tex(SpiritDiveReelRules.HALO_RING),
            SPARK = AuraRender.tex(SpiritDiveReelRules.SPARK), GOLD = AuraRender.tex(SpiritDiveReelRules.GOLD), SWORD_WIND = AuraRender.tex(SpiritDiveReelRules.SWORD_WIND),
            SWORD_FIRE = AuraRender.tex(SpiritDiveReelRules.SWORD_FIRE), SPIKE = AuraRender.tex(SpiritDiveReelRules.SPIKE_WATER), SPIRE = AuraRender.tex(SpiritDiveReelRules.SPIRE_WATER),
            SOFT = AuraRender.tex("particle/glow");

    static {
        for (Element e : Element.values()) {
            int i = e.ordinal();
            CUBE[i] = AuraRender.tex(SpiritDiveReelRules.cubeTexture(e));
            BAR[i] = AuraRender.tex(SpiritDiveReelRules.barTexture(e));
            WING[i] = AuraRender.tex(SpiritDiveReelRules.wingTexture(e));
            WING_GLOW[i] = AuraRender.tex(SpiritDiveReelRules.wingGlowTexture(e));
            CIRCLE[i] = AuraRender.tex(SpiritDiveReelRules.circleTexture(e));
        }
    }

    /** The pose of a model part (ModelPart.translateAndRotate without its scale), then the model pixels (1/16 block): what every px number of the tables is in. */
    static void applyPart(PoseStack pose, ModelPart part) {
        pose.translate(part.x / 16f, part.y / 16f, part.z / 16f);
        if (part.xRot != 0f || part.yRot != 0f || part.zRot != 0f) pose.mulPose(Q.rotationZYX(part.zRot, part.yRot, part.xRot));
        pose.scale(1f / 16f, 1f / 16f, 1f / 16f);
    }

    // ------------------------------------------------------------------ boxes and bands (px)
    /** A glowing block: its centre, half extents and pop scale in the current frame. */
    private static void block(PoseStack pose, VertexConsumer vc, float cx, float cy, float cz, float hx, float hy, float hz, int argb) {
        PropDraw.boxUv(pose, vc, cx - hx, cy - hy, cz - hz, cx + hx, cy + hy, cz + hz, 0f, 0f, 1f, 1f, argb, FB);
    }

    /** Four outward quads round a limb: a band (no top and no bottom, so the limb shows through). */
    private static void band(PoseStack pose, VertexConsumer vc, float cx, float y0, float y1, float hx, float hz, int argb) {
        float x0 = cx - hx, x1 = cx + hx, z0 = -hz, z1 = hz;
        PropDraw.quad(pose, vc, x1, y1, z0, x0, y1, z0, x0, y0, z0, x1, y0, z0, 0f, 0f, 1f, 1f, argb, FB);        // -z (front)
        PropDraw.quad(pose, vc, x0, y1, z1, x1, y1, z1, x1, y0, z1, x0, y0, z1, 0f, 0f, 1f, 1f, argb, FB);        // +z (back)
        PropDraw.quad(pose, vc, x0, y1, z0, x0, y1, z1, x0, y0, z1, x0, y0, z0, 0f, 0f, 1f, 1f, argb, FB);        // -x
        PropDraw.quad(pose, vc, x1, y1, z1, x1, y1, z0, x1, y0, z0, x1, y0, z1, 0f, 0f, 1f, 1f, argb, FB);        // +x
    }

    // ------------------------------------------------------------------ the gauntlets
    /**
     * The glowing cube gauntlet and fist cube of one arm: the pose is the arm's frame (px), {@code right} says which arm, {@code g} is the pop ({@link SpiritDiveReelRules#gauntlet}, up to 1.25),
     * {@code level} 1 = the forearm box and the fist cube, 2 = also the band and the glow copy.
     */
    static void gauntlet(AuraContext c, Element el, ModelPart arm, boolean right, boolean slim, float g, int level, int rgb, float lit) {
        if (g <= 0.01f) return;
        PoseStack pose = c.pose();
        pose.pushPose();
        applyPart(pose, arm);
        float cx = (right ? -1f : 1f) * (slim ? 0.5f : 1f);
        float a = Math.min(1f, g * 1.4f);
        pose.translate(cx, 7f, 0f);
        pose.scale(g, g, g);                                                                     // the block pops on with an overshoot
        VertexConsumer body = c.buffers().getBuffer(SafeAdditive.emissive(CUBE[el.ordinal()]));
        int white = SealDraw.argb(0.94f * a, 0xFFFFFF);
        block(pose, body, 0f, 0f, 0f, 2.75f, 3.5f, 2.75f, white);                                 // the forearm: a block from the elbow's lower end to the wrist
        block(pose, body, 0f, 5.1f, 0f, 2.35f, 1.55f, 2.35f, white);                              // the fist cube past the hand
        if (level >= 2) {
            band(pose, body, 0f, -1.2f, 0.2f, 3.05f, 3.05f, SealDraw.argb(0.95f * a, 0xFFFFFF)); // the band round the middle of the forearm (the spiral band of the video)
            VertexConsumer glow = c.buffers().getBuffer(SafeAdditive.additive(CUBE[el.ordinal()]));
            block(pose, glow, 0f, 0f, 0f, 3.35f, 4.0f, 3.35f, SealDraw.argb(Math.min(1f, 0.38f * a * lit), rgb));
        }
        pose.popPose();
    }

    // ------------------------------------------------------------------ the gold bands of the V2 design
    /** A gold band on a limb: the pose is the part's frame (px); {@code arm} true: the upper arm, false: the thigh. */
    static void goldBand(AuraContext c, ModelPart part, boolean arm, boolean right, boolean slim, float a) {
        if (a <= 0.01f) return;
        PoseStack pose = c.pose();
        pose.pushPose();
        applyPart(pose, part);
        VertexConsumer vc = c.buffers().getBuffer(SafeAdditive.emissive(GOLD));
        int col = SealDraw.argb(0.97f * a, 0xFFFFFF);
        if (arm) band(pose, vc, (right ? -1f : 1f) * (slim ? 0.5f : 1f), 0.6f, 1.8f, slim ? 1.85f : 2.35f, slim ? 1.85f : 2.35f, col);
        else band(pose, vc, 0f, 6.6f, 7.8f, 2.35f, 2.35f, col);
        pose.popPose();
    }

    // ------------------------------------------------------------------ the crown
    /**
     * The crown ring on the head (the head frame, px): cubes of the ring, the bars and white lights, the gold ring halo of the V2 design; {@code left} true draws only the left half (the first dive of
     * Wind). {@code u} is the transformation clock, {@code steady} the strength once it is on (the pulse).
     */
    static void crown(AuraContext c, Element el, Spec sp, boolean character, Pal pal, float u, float uLeft, boolean upgrade, boolean leftOnly, float lit) {
        float ign = Math.max(SpiritDiveReelRules.crown(u), upgrade ? SpiritDiveReelRules.crown(uLeft) : 0f);
        if (ign <= 0.01f || sp.crownCubes() <= 0) return;
        PoseStack pose = c.pose();
        pose.pushPose();
        applyPart(pose, c.model().head);
        SealDraw.camera(pose);
        float age = c.age(), edge = (character ? SpiritDiveReelGeometry.CUBE_CHARACTER : SpiritDiveReelGeometry.CUBE) * 0.5f, hgt = (character ? SpiritDiveReelGeometry.CUBE_HEIGHT_CHARACTER : SpiritDiveReelGeometry.CUBE_HEIGHT) * 0.5f;
        float baseY = SpiritDiveReelGeometry.CROWN_Y - hgt - 0.3f;
        int n = Math.min(sp.crownCubes(), SpiritDiveReelGeometry.CROWN.length);
        VertexConsumer body = c.buffers().getBuffer(SafeAdditive.emissive(CUBE[el.ordinal()]));
        for (int i = 0; i < n; i++) {                                                           // the cubes of the ring
            float x = SpiritDiveReelGeometry.CROWN[i][0], z = SpiritDiveReelGeometry.CROWN[i][1];
            if (leftOnly && x < 0f) continue;
            float k = upgrade && x >= 0f ? 1f : SpiritDiveReelRules.crownCube(u, i, n);
            if (k <= 0.01f) continue;
            pose.pushPose();
            pose.translate(x, baseY, z);
            pose.scale(k, k, k);
            block(pose, body, 0f, 0f, 0f, edge, hgt, edge, SealDraw.argb(Math.min(1f, 0.95f * k), 0xFFFFFF));
            pose.popPose();
        }
        VertexConsumer glow = c.buffers().getBuffer(SafeAdditive.additive(CUBE[el.ordinal()]));
        for (int i = 0; i < n; i++) {                                                           // their glow: the same cubes, a little bigger, additive
            float x = SpiritDiveReelGeometry.CROWN[i][0], z = SpiritDiveReelGeometry.CROWN[i][1];
            if (leftOnly && x < 0f) continue;
            float k = upgrade && x >= 0f ? 1f : SpiritDiveReelRules.crownCube(u, i, n);
            if (k <= 0.01f) continue;
            pose.pushPose();
            pose.translate(x, baseY, z);
            pose.scale(k, k, k);
            block(pose, glow, 0f, 0f, 0f, edge * 1.32f, hgt * 1.25f, edge * 1.32f, SealDraw.argb(Math.min(1f, 0.26f * k * lit), pal.glow()));
            pose.popPose();
        }
        // the bars: vertical strips that turn only about the vertical to face the viewer, a white light on the tip of each
        float rx = SealDraw.CAM[0], rz = SealDraw.CAM[2], rl = Mth.sqrt(rx * rx + rz * rz);
        if (rl < 1e-4f) { rx = 1f; rz = 0f; rl = 1f; }
        rx /= rl; rz /= rl;
        int bars = Math.min(sp.crownBars(), SpiritDiveReelGeometry.BARS.length);
        if (bars > 0) {
            VertexConsumer bv = c.buffers().getBuffer(SafeAdditive.additive(BAR[el.ordinal()]));
            VertexConsumer lv = c.buffers().getBuffer(SafeAdditive.additive(SPARK));
            float h = SpiritDiveReelGeometry.BAR_HEIGHT[el.ordinal()] * (character ? 1.2f : 1f), hw = character ? 0.55f : 0.46f;
            for (int b = 0; b < bars; b++) {
                int i = SpiritDiveReelGeometry.BARS[b];
                float x = SpiritDiveReelGeometry.CROWN[i][0], z = SpiritDiveReelGeometry.CROWN[i][1];
                if (leftOnly && x < 0f) continue;
                float k = upgrade && x >= 0f ? 1f : SpiritDiveReelRules.crownCube(u, i, n);
                if (k <= 0.05f) continue;
                float top = baseY - hgt - h * Math.min(k, 1.15f), foot = baseY - hgt + 0.2f;
                float flick = 1f + 0.08f * Mth.sin(age * 0.23f + i * 1.7f);
                PropDraw.quad(pose, bv, x - rx * hw, foot, z - rz * hw, x + rx * hw, foot, z + rz * hw, x + rx * hw, top, z + rz * hw, x - rx * hw, top, z - rz * hw, 0f, 0f, 1f, 1f,
                        SealDraw.argb(Math.min(1f, 0.95f * k * lit), pal.light()), FB);
                SealDraw.glow(pose, lv, x, top + 0.2f, z, (character ? 0.95f : 0.8f) * flick * Math.min(k, 1.1f), age * 0.03f + i, SealDraw.argb(Math.min(1f, 0.95f * k), 0xFFFFFF), FB);
            }
        }
        // the gold ring halo of the V2 design: a flat gold ring round the foot of the crown (and, for the character tier, a second one turning the other way, higher)
        if (sp.goldRings() > 0) {
            VertexConsumer gv = c.buffers().getBuffer(SafeAdditive.emissive(RING_GOLD));
            float ga = SpiritDiveReelRules.smooth(ign * 1.6f - 0.2f);
            ringFlat(pose, gv, 0f, SpiritDiveReelGeometry.CROWN_Y - 0.7f, 0f, 7.3f * (0.7f + 0.3f * ga), age * 0.015f, 0f, SealDraw.argb(0.95f * ga, 0xFFFFFF));
            if (sp.goldRings() > 1) ringFlat(pose, gv, 0f, SpiritDiveReelGeometry.CROWN_Y - 4.6f, 0f, 6.2f * (0.7f + 0.3f * ga), -age * 0.02f, 0.18f, SealDraw.argb(0.8f * ga, 0xFFFFFF));
        }
        pose.popPose();
    }

    /** Noelle's thin halo over the head (head frame): a tilted white ring with a bloom. */
    static void halo(AuraContext c, Pal pal, float u, float lit) {
        float a = SpiritDiveReelRules.smooth((u - 12f) / 8f);
        if (a <= 0.01f) return;
        PoseStack pose = c.pose();
        pose.pushPose();
        applyPart(pose, c.model().head);
        SealDraw.camera(pose);
        float age = c.age(), bob = 0.5f * Mth.sin(age * 0.09f);
        VertexConsumer hv = c.buffers().getBuffer(SafeAdditive.additive(HALO_RING));
        ringFlat(pose, hv, 0f, -18.0f + bob, 0.5f, 6.6f, age * 0.012f, 0.16f, SealDraw.argb(Math.min(1f, 0.95f * a * lit), 0xFFFFFF));
        SealDraw.glow(pose, c.buffers().getBuffer(SafeAdditive.additive(SOFT)), 0f, -16.2f + bob, 0.5f, 7.0f, 0f, SealDraw.argb(0.38f * a * lit, pal.glow()), FB);
        pose.popPose();
    }

    /** A ring sprite lying flat (tilted forward by {@code tilt} rad about x, turned by {@code spin} rad about y), seen from both sides; {@code r} px half size. */
    static void ringFlat(PoseStack pose, VertexConsumer vc, float cx, float cy, float cz, float r, float spin, float tilt, int argb) {
        float cs = Mth.cos(spin), sn = Mth.sin(spin), ct = Mth.cos(tilt), st = Mth.sin(tilt);
        float ux = cs * r, uy = 0f, uz = -sn * r;
        float vx = sn * ct * r, vy = st * r, vz = cs * ct * r;
        SealDraw.plane(pose, vc, cx, cy, cz, ux, uy, uz, vx, vy, vz, false, argb, FB);
        SealDraw.plane(pose, vc, cx, cy, cz, ux, uy, uz, vx, vy, vz, true, argb, FB);
    }

    // ------------------------------------------------------------------ the wings
    /**
     * The wings of one side, in the body frame in px (the pose is scaled to px and {@link SpiritDiveAnimeRibbon#camera} was called): the leaves of Wind (hex-membrane panels) or the fan of blades
     * of the other elements. {@code open} is {@link SpiritDiveReelRules#wingScale}; {@code blades} is how many of the fan's blades are drawn (a half dive draws a short fan).
     */
    static void wings(AuraContext c, Element el, Spec sp, boolean character, Pal pal, float side, float open, int leaves, int blades, int segsLeaf, int segsBlade, float lit, float swing) {
        if (open <= 0.02f) return;
        PoseStack pose = c.pose();
        float age = c.age(), a = SpiritDiveReelRules.smooth(open * 1.8f);
        int i = el.ordinal();
        for (int pass = 0; pass < 2; pass++) {
            VertexConsumer vc = c.buffers().getBuffer(pass == 0 ? SafeAdditive.emissive(WING[i]) : SafeAdditive.additive(WING_GLOW[i]));
            int argb = pass == 0 ? SealDraw.argb(Math.min(1f, (el == Element.WIND ? 0.9f : 0.97f) * a), 0xFFFFFF) : SealDraw.argb(Math.min(1f, 0.6f * a * lit), pass == 1 ? pal.glow() : 0xFFFFFF);
            if (leaves > 0) {
                float k = character ? SpiritDiveReelGeometry.LEAF_CHARACTER : 1f;
                for (int r = 0; r < Math.min(leaves, SpiritDiveReelGeometry.leafRows()); r++) {
                    float[] row = SpiritDiveReelGeometry.leaf(r);
                    SpiritDiveReelGeometry.leafSpine(SpiritDiveAnimeRibbon.SPINE, segsLeaf, row, side, k, open, age + r * 5.3f, swing);
                    SpiritDiveAnimeRibbon.strip(pose, vc, segsLeaf, row[10] * k * (0.35f + 0.65f * Math.min(1f, open)), Shape.LEAF, row[12] + age * 0.04f, 0.42f, 0f, 0.25f, 1f, argb, 0.06f, FB);
                }
            }
            if (blades > 0) {
                float lenMax = SpiritDiveReelGeometry.BLADE_LENGTH[i][character ? 1 : 0];
                float spread = Math.min(1f, open);
                Shape shape = el == Element.FIRE ? Shape.FLAME : el == Element.EARTH ? Shape.SHARD : Shape.BLADE;
                for (int b = 0; b < blades; b++) {
                    SpiritDiveReelGeometry.blade(BLADE, b, blades, side, lenMax * (0.55f + 0.45f * Math.min(open, 1.12f)), spread);
                    SpiritDiveReelGeometry.bladeSpine(SpiritDiveAnimeRibbon.SPINE, segsBlade, BLADE, b, age, swing);
                    SpiritDiveAnimeRibbon.strip(pose, vc, segsBlade, BLADE[10] * (el == Element.FIRE ? 0.85f : 1f), shape, b * 1.3f + age * 0.05f, 0.30f, 0f, 0.12f, 1f, argb, 0.04f, FB);
                }
            }
        }
    }

    // ------------------------------------------------------------------ the weapons (body frame, px; the pose is scaled to px, the camera is read)
    /**
     * A flat sword (Yuno's spirit sword, Fuegoleon's flame blade): a strip that turns its face to the viewer about its own axis, drawn out of the circle at its hilt by {@code k} (0..1). The array
     * is {@link SpiritDiveReelGeometry#SWORD} or {@link SpiritDiveReelGeometry#FLAME_BLADE}.
     */
    static void sword(AuraContext c, Element el, float[] s, Pal pal, float k, float lit, float pulse) {
        if (k <= 0.02f) return;
        PoseStack pose = c.pose();
        float age = c.age();
        float len = s[6] / 0.70f;                                                               // the whole texture: the blade is the upper 70 percent
        float px = s[0], py = s[1] + SpiritDiveReelGeometry.bob(age, el.ordinal()), pz = s[2];
        float ax = s[3], ay = -s[4], az = s[5], al = Mth.sqrt(ax * ax + ay * ay + az * az);
        ax /= al; ay /= al; az /= al;
        float lx = SealDraw.CAM[6], ly = SealDraw.CAM[7], lz = SealDraw.CAM[8];
        float wx = ay * lz - az * ly, wy = az * lx - ax * lz, wz = ax * ly - ay * lx, wl = Mth.sqrt(wx * wx + wy * wy + wz * wz);
        if (wl < 1e-4f) { wx = 1f; wy = 0f; wz = 0f; wl = 1f; }
        float hw = len / 8f * (0.4f + 0.6f * k);                                                // half width: the whole texture is a quarter as wide as it is long
        wx = wx / wl * hw; wy = wy / wl * hw; wz = wz / wl * hw;
        float up = 0.73f * len * k, down = 0.27f * len * k;
        float tx = px + ax * up, ty = py + ay * up, tz = pz + az * up, bx = px - ax * down, by = py - ay * down, bz = pz - az * down;
        ResourceLocation tex = el == Element.FIRE ? SWORD_FIRE : SWORD_WIND;
        for (int pass = 0; pass < 2; pass++) {
            float g = pass == 0 ? 1f : 1.55f;
            VertexConsumer vc = c.buffers().getBuffer(pass == 0 ? SafeAdditive.emissive(tex) : SafeAdditive.additive(tex));
            int argb = pass == 0 ? SealDraw.argb(0.97f * Math.min(1f, k * 1.5f), 0xFFFFFF) : SealDraw.argb(0.42f * k * lit * pulse, pal.glow());
            PropDraw.quad(pose, vc, bx - wx * g, by - wy * g, bz - wz * g, bx + wx * g, by + wy * g, bz + wz * g, tx + wx * g, ty + wy * g, tz + wz * g, tx - wx * g, ty - wy * g, tz - wz * g,
                    0f, 0f, 1f, 1f, argb, FB);
        }
        float r = (el == Element.FIRE ? 7.0f : 9.5f) * (0.5f + 0.5f * k);                       // the magic circle at the hilt turns slowly
        SealDraw.glow(pose, c.buffers().getBuffer(SafeAdditive.additive(CIRCLE[el.ordinal()])), px, py, pz, r, age * (el == Element.FIRE ? -0.03f : 0.04f), SealDraw.argb(Math.min(1f, 0.95f * k * lit), 0xFFFFFF), FB);
    }

    /** Noelle's spiky ball of water blades (a gear-like cluster): two discs of blades turning about different axes round one centre, a bright core. {@code k} 0..1 how far it has formed. */
    static void ball(AuraContext c, Pal pal, float k, float lit) {
        if (k <= 0.02f) return;
        PoseStack pose = c.pose();
        float age = c.age();
        float[] b = SpiritDiveReelGeometry.BALL;
        float cx = b[0], cy = b[1] + SpiritDiveReelGeometry.bob(age, 2.1f), cz = b[2], r0 = b[3] * k, r1 = b[4] * (0.45f + 0.55f * k), hw = b[5];
        float lx = SealDraw.CAM[6], ly = SealDraw.CAM[7], lz = SealDraw.CAM[8];
        VertexConsumer vc = c.buffers().getBuffer(SafeAdditive.emissive(SPIKE));
        int n = SpiritDiveReelRules.SPIKES_PER_DISC;
        for (int d = 0; d < 2; d++) {
            // the disc's axis (unit) and a basis (u, v) of its plane, built without allocation
            float nx = d == 0 ? 0.25f : 1f, ny = d == 0 ? 1f : 0.3f, nz = d == 0 ? 0.35f : -0.2f, nl = Mth.sqrt(nx * nx + ny * ny + nz * nz);
            nx /= nl; ny /= nl; nz /= nl;
            float ux = 0f, uy = nz, uz = -ny;                                                    // n x (1,0,0): a vector in the disc's plane
            float ul = Mth.sqrt(uy * uy + uz * uz);
            if (ul < 1e-3f) { uy = 1f; uz = 0f; ul = 1f; }
            uy /= ul; uz /= ul;
            float vx = ny * uz - nz * uy, vy = nz * ux - nx * uz, vz = nx * uy - ny * ux;
            float spin = age * (d == 0 ? 0.085f : -0.07f) + d * Mth.PI / n;
            for (int i = 0; i < n; i++) {
                float ang = spin + i * Mth.TWO_PI / n, ca = Mth.cos(ang), sa = Mth.sin(ang);
                float dx = ux * ca + vx * sa, dy = uy * ca + vy * sa, dz = uz * ca + vz * sa;
                float ax = cx + dx * r0, ay = cy + dy * r0, az = cz + dz * r0, tx = cx + dx * r1, ty = cy + dy * r1, tz = cz + dz * r1;
                float wx = dy * lz - dz * ly, wy = dz * lx - dx * lz, wz = dx * ly - dy * lx, wl = Mth.sqrt(wx * wx + wy * wy + wz * wz);
                if (wl < 1e-4f) continue;
                wx = wx / wl * hw; wy = wy / wl * hw; wz = wz / wl * hw;
                PropDraw.quad(pose, vc, ax - wx, ay - wy, az - wz, ax + wx, ay + wy, az + wz, tx + wx * 0.25f, ty + wy * 0.25f, tz + wz * 0.25f, tx - wx * 0.25f, ty - wy * 0.25f, tz - wz * 0.25f,
                        0f, 0f, 1f, 1f, SealDraw.argb(0.96f * Math.min(1f, k * 1.5f), 0xFFFFFF), FB);
            }
        }
        SealDraw.glow(pose, c.buffers().getBuffer(SafeAdditive.additive(SOFT)), cx, cy, cz, 5.0f * k, 0f, SealDraw.argb(0.55f * k * lit, pal.glow()), FB);
        SealDraw.glow(pose, c.buffers().getBuffer(SafeAdditive.additive(SPARK)), cx, cy, cz, 2.6f * k, age * 0.05f, SealDraw.argb(0.95f * k, 0xFFFFFF), FB);
    }

    /** Noelle's crystal water spire (three crossed planes, both faces) standing behind her right shoulder, rising and falling slowly. */
    static void spire(AuraContext c, Pal pal, float k, float lit) {
        if (k <= 0.02f) return;
        PoseStack pose = c.pose();
        float age = c.age();
        float[] s = SpiritDiveReelGeometry.SPIRE;
        float bob = SpiritDiveReelGeometry.bob(age, 4.4f), h = s[3] * k, hw = s[4];
        VertexConsumer vc = c.buffers().getBuffer(SafeAdditive.emissive(SPIRE));
        for (int p = 0; p < 3; p++) {
            float ang = p * Mth.PI / 3f + age * 0.01f, ex = Mth.cos(ang) * hw, ez = Mth.sin(ang) * hw;
            float x0 = s[0], y0 = s[1] + bob + 0.25f * s[3], z0 = s[2];
            int argb = SealDraw.argb(0.95f * Math.min(1f, k * 1.4f), 0xFFFFFF);
            SealDraw.plane(pose, vc, x0 - 0.0f, y0 - h * 0.5f, z0, ex, 0f, ez, 0f, h * 0.5f, 0f, false, argb, FB);
            SealDraw.plane(pose, vc, x0 - 0.0f, y0 - h * 0.5f, z0, ex, 0f, ez, 0f, h * 0.5f, 0f, true, argb, FB);
        }
        SealDraw.glow(pose, c.buffers().getBuffer(SafeAdditive.additive(SOFT)), s[0], s[1] + bob + 0.25f * s[3] - h * 0.55f, s[2], 7f * k, 0f, SealDraw.argb(0.3f * k * lit, pal.glow()), FB);
    }
}
