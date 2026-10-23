package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.book.SpiritDiveAnimeRules;
import com.newuniverse.nusmp.book.SpiritDivePieces;
import com.newuniverse.nusmp.book.SpiritDivePieces.Element;
import com.newuniverse.nusmp.client.SafeAdditive;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * Spirit Dive, ANIME look: the body-wrapped energy shell. It is the player's own model parts drawn again, each inflated about its own centre by a fixed number of pixels (so the shell hugs every limb
 * with an even thickness, not a scaled copy of the body), in three passes:
 * <ol>
 *   <li>the inner WASH, {@code INFLATE[0]} px, additive, the swirl texture (energy streaks / flame tongues / currents / glowing cracks) scrolling along the limbs: the vanilla {@code energySwirl}
 *       type with the offset quantised to {@link SpiritDiveAnimeRules#SWIRL_STEPS} cached types, so a frame builds no render type;</li>
 *   <li>the RIM hull, {@code INFLATE[1]} px, additive, drawn with the winding of every quad reversed so only the faces turned AWAY from the viewer survive the back-face culling: what shows is the
 *       ring of the hull that sticks out beyond the silhouette, a bright cel outline round the body that the body itself hides everywhere else (depth test);</li>
 *   <li>the SOFT hull, {@code INFLATE[2]} px, the same trick, dimmer: the glow round the outline.</li>
 * </ol>
 * The inflation moves every vertex of a cuboid outward along the cuboid's own three axes ({@code p + d * (sx * ax + sy * ay + sz * az)}, the signs from the vertex's side of the part's centre), so
 * the corners stay joined and the thickness is the same on every face. All passes together are at most 3 x 6 parts x 24 vertices. No allocation: one consumer, one scratch quaternion, one matrix, the
 * render types are cached by (element, step).
 */
final class SpiritDiveAnimeShell {
    private SpiritDiveAnimeShell() {}

    // ------------------------------------------------------------------ the consumer that inflates and flips
    /** A vertex consumer between the model and the buffer: moves each vertex out by {@code d} along the part's axes and, when {@code flip}, reverses the winding of every quad. */
    static final class Inflate implements VertexConsumer {
        private VertexConsumer out;
        private boolean flip;
        private float d, cx, cy, cz, ax, ay, az, bx, by, bz, ex, ey, ez;
        private final float[] x = new float[4], y = new float[4], z = new float[4], u = new float[4], v = new float[4], nx = new float[4], ny = new float[4], nz = new float[4];
        private final int[] col = new int[4], ov = new int[4], lt = new int[4];
        private int n;

        void begin(VertexConsumer to, boolean flipWinding, float inflatePx) {
            out = to; flip = flipWinding; d = inflatePx / 16f; n = 0;
        }

        /** The frame of the part about to be drawn: its centre (world) and its three axes (the columns of its matrix, which carry the scale), from the pose at that part. */
        void frame(Matrix4f m, float lx, float ly, float lz) {
            ax = m.m00(); ay = m.m10(); az = m.m20();
            bx = m.m01(); by = m.m11(); bz = m.m21();
            ex = m.m02(); ey = m.m12(); ez = m.m22();
            cx = m.m00() * lx + m.m01() * ly + m.m02() * lz + m.m30();
            cy = m.m10() * lx + m.m11() * ly + m.m12() * lz + m.m31();
            cz = m.m20() * lx + m.m21() * ly + m.m22() * lz + m.m32();
            n = 0;
        }

        private static float sgn(float f) { return f < 0f ? -1f : 1f; }

        @Override
        public void addVertex(float px, float py, float pz, int color, float uu, float vv, int overlay, int light, float nnx, float nny, float nnz) {
            float rx = px - cx, ry = py - cy, rz = pz - cz;
            float sa = sgn(rx * ax + ry * ay + rz * az), sb = sgn(rx * bx + ry * by + rz * bz), se = sgn(rx * ex + ry * ey + rz * ez);
            px += d * (sa * ax + sb * bx + se * ex);
            py += d * (sa * ay + sb * by + se * ey);
            pz += d * (sa * az + sb * bz + se * ez);
            if (!flip) {
                out.addVertex(px, py, pz, color, uu, vv, overlay, light, nnx, nny, nnz);
                return;
            }
            int i = n++;
            x[i] = px; y[i] = py; z[i] = pz; u[i] = uu; v[i] = vv; nx[i] = nnx; ny[i] = nny; nz[i] = nnz; col[i] = color; ov[i] = overlay; lt[i] = light;
            if (n == 4) {                                                                              // a whole quad: emit it in the reverse order 0, 3, 2, 1
                n = 0;
                emit(0); emit(3); emit(2); emit(1);
            }
        }

        private void emit(int i) { out.addVertex(x[i], y[i], z[i], col[i], u[i], v[i], ov[i], lt[i], nx[i], ny[i], nz[i]); }

        // the chained calls are never used by a model part (it uses the all-in-one call above)
        @Override public VertexConsumer addVertex(float px, float py, float pz) { return this; }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
        @Override public VertexConsumer setUv(float uu, float vv) { return this; }
        @Override public VertexConsumer setUv1(int uu, int vv) { return this; }
        @Override public VertexConsumer setUv2(int uu, int vv) { return this; }
        @Override public VertexConsumer setNormal(float a, float b, float c) { return this; }
    }

    private static final Inflate INFLATE = new Inflate();
    private static final Quaternionf ROT = new Quaternionf();                                           // scratch: the render thread is the only user

    // ------------------------------------------------------------------ the render types
    private static final RenderType[][] SWIRL = new RenderType[4][SpiritDiveAnimeRules.SWIRL_STEPS];
    private static final ResourceLocation[] SWIRL_TEX = new ResourceLocation[4];
    private static final ResourceLocation RIM = AuraRender.tex(SpiritDiveAnimeRules.RIM_TEXTURE), SOFT = AuraRender.tex(SpiritDiveAnimeRules.SOFT_TEXTURE);

    static {
        for (Element e : Element.values()) SWIRL_TEX[e.ordinal()] = AuraRender.tex(SpiritDiveAnimeRules.swirlTexture(e));
    }

    /** The scrolling energy type of an element at a scroll step (made once, then reused: the table has at most 4 x {@link SpiritDiveAnimeRules#SWIRL_STEPS} entries for the life of the client). */
    static RenderType swirl(Element el, int step) {
        RenderType[] row = SWIRL[el.ordinal()];
        RenderType t = row[step];
        if (t == null) {
            t = com.newuniverse.nusmp.client.NURenderTypes.spiritSwirl(SWIRL_TEX[el.ordinal()], 0f, SpiritDiveAnimeRules.swirlV(el, step));
            row[step] = t;
        }
        return t;
    }

    // ------------------------------------------------------------------ the parts
    /** The six body parts of the shell (index into {@link SpiritDiveAnimeRules#PART_BOX}): head, body, right arm, left arm, right leg, left leg; the model's own part and the stage mask bit of each. */
    static final int HEAD = 0, BODY = 1, ARM_R = 2, ARM_L = 3, LEG_R = 4, LEG_L = 5;
    private static final int[] MASK = {0, SpiritDivePieces.SHELL_BODY, SpiritDivePieces.SHELL_ARM_R, SpiritDivePieces.SHELL_ARM_L, SpiritDivePieces.SHELL_LEG_R, SpiritDivePieces.SHELL_LEG_L};
    private static final int[] ORDER = {2, 1, 1, 1, 0, 0};                                              // the order the shell grows in: legs, then body and arms, then the head

    private static ModelPart part(PlayerModel<AbstractClientPlayer> m, int i) {
        return switch (i) { case HEAD -> m.head; case BODY -> m.body; case ARM_R -> m.rightArm; case ARM_L -> m.leftArm; case LEG_R -> m.rightLeg; default -> m.leftLeg; };
    }

    /**
     * Draws one pass over the parts of {@code mask} (the stage's shell parts; the head when {@code head}), each part at the strength {@code partAlpha(progress, order)} (so the shell grows from
     * the feet up) times {@code strength}.
     *
     * @param skipArmR true when the right arm is drawn by someone else (Fuegoleon's flame arm)
     * @param keptMask parts that are on at full strength whatever the progress (the half dive of an upgrade)
     */
    static void pass(AuraContext c, RenderType rt, boolean flip, float inflatePx, int rgb, float strength, float progress, int mask, boolean head, boolean skipArmR, int keptMask,
                     boolean slim) {
        PlayerModel<AbstractClientPlayer> m = c.model();
        PoseStack pose = c.pose();
        VertexConsumer vc = c.buffers().getBuffer(rt);
        for (int i = 0; i < 6; i++) {
            if (i == HEAD ? !head : (mask & MASK[i]) == 0) continue;
            if (i == ARM_R && skipArmR) continue;
            float a = (keptMask & MASK[i]) != 0 && i != HEAD ? 1f : SpiritDiveAnimeRules.partAlpha(progress, ORDER[i]);
            a *= strength;
            if (a <= 0.01f) continue;
            ModelPart p = part(m, i);
            if (!p.visible) continue;
            INFLATE.begin(vc, flip, i == HEAD ? inflatePx + 0.5f : inflatePx);                      // the head also clears the hat layer (grown 0.5)
            float[] box = SpiritDiveAnimeRules.PART_BOX[i];
            float cxl = box[0], cyl = box[1], czl = box[2];
            if (slim && (i == ARM_R || i == ARM_L)) cxl += i == ARM_R ? 0.5f : -0.5f;
            pose.pushPose();                                                                          // the part's own pose (ModelPart.translateAndRotate without its scale): its axes and centre
            pose.translate(p.x / 16f, p.y / 16f, p.z / 16f);
            if (p.xRot != 0f || p.yRot != 0f || p.zRot != 0f) pose.mulPose(ROT.rotationZYX(p.zRot, p.yRot, p.xRot));
            INFLATE.frame(pose.last().pose(), cxl / 16f, cyl / 16f, czl / 16f);
            pose.popPose();
            int argb = AuraRender.alpha(0xFF000000 | rgb, a);
            p.render(pose, INFLATE, 0xF000F0, OverlayTexture.NO_OVERLAY, argb);
        }
    }

    /** Draws the whole shell of one dive: the passes the level of detail allows (3: wash, rim, soft; 2: wash, rim; 1: the rim outline alone). */
    static void draw(AuraContext c, Element el, int passes, int tint, int rim, int glow, float pulse, float progress, int mask, boolean head, boolean skipArmR, int keptMask, float k) {
        boolean slim = WaterAuraRig.slim(c.model());
        if (passes >= 2) pass(c, swirl(el, SpiritDiveAnimeRules.swirlStep(el, c.age())), false, SpiritDiveAnimeRules.INFLATE[0], tint, 0.6f * pulse * k, progress, mask, false, skipArmR, keptMask, slim);
        pass(c, SafeAdditive.additive(RIM), true, SpiritDiveAnimeRules.INFLATE[1], rim, 0.95f * k, progress, mask, head, skipArmR, keptMask, slim);
        if (passes >= 3) pass(c, SafeAdditive.additive(SOFT), true, SpiritDiveAnimeRules.INFLATE[2], glow, 0.4f * pulse * k, progress, mask, head, skipArmR, keptMask, slim);
    }

    /** Draws one part (the hat layer of the head: the hair cap) once, inflated, with a render type: the same machinery for a single part. */
    static void hat(AuraContext c, RenderType rt, float inflatePx, int rgb, float alpha) {
        PlayerModel<AbstractClientPlayer> m = c.model();
        PoseStack pose = c.pose();
        INFLATE.begin(c.buffers().getBuffer(rt), false, inflatePx);
        ModelPart p = m.hat;
        pose.pushPose();
        pose.translate(p.x / 16f, p.y / 16f, p.z / 16f);
        if (p.xRot != 0f || p.yRot != 0f || p.zRot != 0f) pose.mulPose(ROT.rotationZYX(p.zRot, p.yRot, p.xRot));
        INFLATE.frame(pose.last().pose(), 0f, -4f / 16f, 0f);
        pose.popPose();
        boolean was = p.visible;
        p.visible = true;
        p.render(pose, INFLATE, 0xF000F0, OverlayTexture.NO_OVERLAY, AuraRender.alpha(0xFF000000 | rgb, alpha));
        p.visible = was;
    }
}
