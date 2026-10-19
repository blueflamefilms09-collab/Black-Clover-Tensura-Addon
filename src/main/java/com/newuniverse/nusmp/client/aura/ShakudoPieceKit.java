package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.client.geo.GeoAnim;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoModelData;
import com.newuniverse.nusmp.client.geo.GeoModels;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.client.prop.PropDraw;
import com.newuniverse.nusmp.client.prop.SealDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.93.0 (the Shakudo, Scale, Spike, Nail and Hair rebuild): the piece engine of their temporary armour. The armour is ONE geo model per magic
 * ({@code assets/nusmp/geo/entity/<key>_armor.geo.json}) whose ROOT BONES are the pieces (helm, chest, waist, pauldron_r / _l, vambrace_r / _l,
 * gauntlet_r / _l, greave_r / _l, back ...), authored on the humanoid frame. Each piece is drawn on its own, anchored on the model part it was built
 * for (so it follows crouching, swimming, swinging and the head), and forged in / shattered out by {@link EquipAnimation} exactly like the pieces of the
 * Djinn full equip: staggered by a per-piece delay of 0 to 12 ticks, flying in from its own side while it fades up and grows 85 to 100 percent over 8
 * ticks, and shattering out over the last 18 ticks.
 *
 * <p>Passes, in draw order (explicit render layers, nothing implicit): 1. every piece's skin pass, solid ({@link AuraRender#cutout}) while it is fully
 * on and translucent ({@link AuraRender#translucent}) while it forges in or shatters out, the cubes already inflated over the skin by the generator
 * (0.45 px or more, so nothing is coplanar with the body); 2. every piece's glow map, additive and full bright ({@link AuraRender#additive}), pulsing.
 * Nothing is ever put in an inventory or armour slot. Arms are scaled to 80 percent across for a slim skin, so the same pieces fit both arm widths.
 * The clip "move" is played instead of "idle" while the player walks or runs (cloaks, tails and tendrils stream).
 *
 * <p>Cost: one skin pass and one glow pass of at most about 150 cubes per set; no allocation per frame (the single-root swap reuses a scratch list).
 * This class belongs to the Shakudo files because the five magics share it; each magic's {@code <Name>Aura} only lists its pieces and its accents.
 */
public final class ShakudoPieceKit {
    private ShakudoPieceKit() {}

    public enum Anchor { HEAD, BODY, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG }

    /**
     * One piece of a set.
     *
     * @param bone  the root bone of the geo model that holds the piece
     * @param delay the stagger of the forge-in in ticks (0 to 12)
     * @param dx    where the piece flies in from, in model pixels in the anchor's frame (the Djinn's own numbers are about 2 to 14)
     * @param head  true: not drawn for the local player in first person (it would sit in the camera)
     * @param far   true: a small detail that is left out when the player is more than 24 blocks from the camera
     */
    public record Piece(String bone, Anchor anchor, int delay, float dx, float dy, float dz, boolean head, boolean far) {}

    public static Piece piece(String bone, Anchor anchor, int delay, float dx, float dy, float dz) { return new Piece(bone, anchor, delay, dx, dy, dz, false, false); }

    public static Piece head(String bone, Anchor anchor, int delay, float dx, float dy, float dz) { return new Piece(bone, anchor, delay, dx, dy, dz, true, false); }

    public static Piece detail(String bone, Anchor anchor, int delay, float dx, float dy, float dz) { return new Piece(bone, anchor, delay, dx, dy, dz, false, true); }

    /** A set: the geo model, its pieces, and the glow colour (ARGB without alpha mattering). */
    public record PieceSet(GeoSpec spec, List<Piece> pieces, int glowRgb) {}

    private static final double DETAIL2 = 24.0 * 24.0;
    private static final ArrayList<GeoModelData.Bone> SCRATCH = new ArrayList<>();

    private static ModelPart part(PlayerModel<?> m, Anchor a) {
        return switch (a) {
            case HEAD -> m.head;
            case BODY -> m.body;
            case RIGHT_ARM -> m.rightArm;
            case LEFT_ARM -> m.leftArm;
            case RIGHT_LEG -> m.rightLeg;
            case LEFT_LEG -> m.leftLeg;
        };
    }

    /** The paint call of a set's armour: the skin passes first, then the glow passes. {@code glowBoost} scales the glow (1 = the usual pulse). */
    public static void paint(AuraContext c, PieceSet set, float glowBoost) {
        float f = c.fade();
        Minecraft mc = Minecraft.getInstance();
        boolean self1p = c.player() == mc.player && mc.options.getCameraType().isFirstPerson();
        boolean slim = c.player().getSkin().model() == PlayerSkin.Model.SLIM;
        boolean near = true;
        if (mc.gameRenderer != null && mc.gameRenderer.getMainCamera() != null) {
            near = mc.gameRenderer.getMainCamera().getPosition().distanceToSqr(c.player().getX(), c.player().getY(), c.player().getZ()) < DETAIL2;
        }
        GeoModelData model = GeoModels.model(set.spec().model());
        if (model == null || model.roots.isEmpty()) return;
        GeoAnim anims = GeoModels.animations(set.spec().animations());
        GeoAnim.Clip clip = anims == null ? null : anims.clip(c.limbSwingAmount() > 0.35f ? "move" : "idle");
        float seconds = c.age() / 20f;
        float pulse = (0.75f + 0.25f * (float) Math.sin(c.age() * 0.13f)) * glowBoost;
        boolean hasGlow = set.spec().glow() != null;

        SCRATCH.clear();
        SCRATCH.addAll(model.roots);
        try {
            for (int pass = 0; pass < 2; pass++) {
                if (pass == 1 && !hasGlow) break;
                for (Piece p : set.pieces()) {
                    if ((p.head() && self1p) || (p.far() && !near)) continue;
                    GeoModelData.Bone root = model.bones.get(p.bone());
                    if (root == null) continue;
                    float a = EquipAnimation.assembled(c, p.delay());
                    if (a == 0f) continue;
                    PoseStack pose = c.pose();
                    ModelPart part = part(c.model(), p.anchor());
                    PartPose rest = part.getInitialPose();
                    pose.pushPose();
                    part.translateAndRotate(pose);                                   // at the part's pivot, turned with it
                    if (slim && (p.anchor() == Anchor.RIGHT_ARM || p.anchor() == Anchor.LEFT_ARM)) pose.scale(0.8f, 1f, 1f);
                    float alpha = EquipAnimation.begin(pose, a, p.dx() / 16f, p.dy() / 16f, p.dz() / 16f);
                    pose.translate(-rest.x / 16f, -rest.y / 16f, -rest.z / 16f);     // back to the file frame the piece was built in
                    model.roots.clear();
                    model.roots.add(root);
                    if (pass == 0) {
                        boolean solid = a >= 1f;
                        RenderType rt = solid ? AuraRender.cutout(set.spec().texture()) : AuraRender.translucent(set.spec().texture());
                        int argb = solid ? 0xFFFFFFFF : AuraRender.alpha(0xFFFFFFFF, alpha);
                        VertexConsumer vc = c.buffers().getBuffer(rt);
                        GeoDraw.draw(pose, vc, model, clip, seconds, GeoDraw.Space.PLAYER, c.light(), OverlayTexture.NO_OVERLAY, argb);
                    } else {
                        int argb = AuraRender.alpha(0xFF000000 | set.glowRgb(), Math.min(1f, alpha * f * pulse));
                        VertexConsumer vc = c.buffers().getBuffer(AuraRender.additive(set.spec().glow()));
                        GeoDraw.draw(pose, vc, model, clip, seconds, GeoDraw.Space.PLAYER, PropDraw.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, argb);
                    }
                    pose.popPose();
                }
            }
        } finally {
            model.roots.clear();
            model.roots.addAll(SCRATCH);
        }
    }

    /** Looks a piece list up by anchor for the pieces' standard staggers: head 0..2, chest 2..4, arms 3..8, legs 6..10, back 9..12 (the Djinn's order). */
    public static int delayFor(Anchor a, int k) {
        return switch (a) {
            case HEAD -> Math.min(12, k);
            case BODY -> Math.min(12, 2 + k);
            case RIGHT_ARM, LEFT_ARM -> Math.min(12, 3 + k);
            case RIGHT_LEG, LEFT_LEG -> Math.min(12, 6 + k);
        };
    }

    // ================================================================ accents shared by the five auras (layer space: y down, feet at 1.5, back +z)
    /** A turning sigil on the ground under the feet (additive, full bright). {@code grow} 0..1 scales it in; {@code spin} is degrees per tick. */
    public static void sigil(AuraContext c, ResourceLocation tex, int rgb, float radius, float spin, float strength, float grow) {
        PoseStack pose = c.pose();
        pose.pushPose();
        pose.translate(0f, 1.47f, 0f);
        SealDraw.flat(pose, c.buffers().getBuffer(AuraRender.additive(tex)), radius * (0.6f + 0.4f * grow), c.age() * spin, SealDraw.argb(strength, rgb), PropDraw.FULL_BRIGHT);
        pose.popPose();
    }

    /**
     * {@code n} motes of {@code tex} rising (rise > 0) or falling (rise < 0) round the body, each on its own phase, fading in and out; additive.
     * Call {@code SealDraw.camera(c.pose())} once first. radius = spread round the body in blocks, size = half size of a mote.
     */
    public static void motes(AuraContext c, ResourceLocation tex, int rgb, int n, float rise, float radius, float size, float strength, int seed) {
        PoseStack pose = c.pose();
        VertexConsumer vc = c.buffers().getBuffer(AuraRender.additive(tex));
        float age = c.age();
        for (int i = 0; i < n; i++) {
            float ph = frac(age * 0.014f * Math.abs(rise) + SealDraw.rnd(seed, i, 1));
            float a = SealDraw.rnd(seed, i, 2) * Mth.TWO_PI + age * 0.03f * (i % 2 == 0 ? 1f : -1f);
            float r = radius * (0.55f + 0.45f * SealDraw.rnd(seed, i, 3));
            float h = (rise < 0f ? 1f - ph : ph) * 2.0f;
            float fade = Mth.sin(ph * Mth.PI);
            SealDraw.glow(pose, vc, Mth.cos(a) * r, 1.5f - h, Mth.sin(a) * r, size * (0.7f + 0.6f * SealDraw.rnd(seed, i, 4)), age * 0.08f + i,
                    SealDraw.argb(strength * fade, rgb), PropDraw.FULL_BRIGHT);
        }
    }

    /** A sprite standing upright at (cx, cy, cz) facing the direction {@code yaw} (radians), both sides, half width / half height. */
    public static void upright(PoseStack pose, VertexConsumer vc, float cx, float cy, float cz, float yaw, float halfW, float halfH, int argb) {
        float ux = Mth.cos(yaw) * halfW, uz = Mth.sin(yaw) * halfW;
        SealDraw.plane(pose, vc, cx, cy, cz, ux, 0f, uz, 0f, -halfH, 0f, false, argb, PropDraw.FULL_BRIGHT);
        SealDraw.plane(pose, vc, cx, cy, cz, ux, 0f, uz, 0f, -halfH, 0f, true, argb, PropDraw.FULL_BRIGHT);
    }

    public static float frac(float x) { return x - (float) Math.floor(x); }
}
