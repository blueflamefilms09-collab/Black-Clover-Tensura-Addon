package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.client.geo.GeoAnim;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoModelData;
import com.newuniverse.nusmp.client.geo.GeoModels;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.client.prop.PropDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the temporary armour of Song, Sound, Soul, Soul Corpse and Memory Magic is a real SET OF PIECES (helm, chest, pauldrons, vambraces, gauntlets, greaves and the
 * magic's own pieces) that forges in and shatters out exactly like the Djinn Full Equip. The five painters only differ in their data, so the drawing lives here once:
 * every {@link Piece} names a bone of the set's GeoLite model (authored on the standing humanoid, one bone per piece, every piece bone a root of its own so it can be drawn
 * alone), the model part it is anchored on, its stagger (0 to 12 ticks) and the direction it flies in from. {@link EquipAnimation} gives the forge, the stagger and the shatter;
 * this class applies them to the part's pose (the Djinn's own order: part pose, pixels, offset and scale, then the model's absolute frame back).
 * <p>
 * Passes, always in this order (the explicit render layers of the owner's engine): 1. SOLID pieces, cutout (entityCutoutNoCull) while fully on and translucent only while forging in
 * or shattering out, because cutout cannot fade; 2. GHOST pieces (spirits, film, glass), always translucent; 3. every piece's glow map, additive (RenderType.eyes), full bright,
 * so a glow is never covered by a solid drawn after it. Pieces on the arms are narrowed to 78 percent about the inner edge of the arm when the skin has slim arms (the arm part
 * has a 3 px box instead of 4), so the set fits both. Head pieces are left out for the local player in first person. A bone the model does not have is skipped (no exception).
 * No allocation per frame beyond the pose stack: the model's roots are swapped for the one piece and put back in a finally block (the render thread is single).
 */
public final class SongArmourKit {
    private SongArmourKit() {}

    public enum Anchor { HEAD, BODY, ARM_R, ARM_L, LEG_R, LEG_L }

    /** How a piece is drawn: SOLID = cutout, GHOST = translucent always. */
    public enum Look { SOLID, GHOST }

    /**
     * One piece. dx, dy, dz: the direction it flies in from in model pixels of the anchor part (the Djinn's own numbers are about 2 to 14); delay: stagger in ticks (0 to 12);
     * firstPersonHidden: left out for the local player in first person (anything near the eyes); clip: the animation clip that moves the piece's bone (null = rest pose);
     * glowK: how strongly its glow map shines (1 = normal).
     */
    public record Piece(String bone, Anchor anchor, int delay, float dx, float dy, float dz, Look look, boolean firstPersonHidden, String clip, float glowK) {}

    /** A whole set: its model and its glow colour (RGB). */
    public record Set(GeoSpec spec, int glowRgb, Piece[] pieces) {}

    public static Piece piece(String bone, Anchor a, int delay, float dx, float dy, float dz) { return new Piece(bone, a, delay, dx, dy, dz, Look.SOLID, false, "idle", 1f); }
    public static Piece head(String bone, int delay, float dx, float dy, float dz) { return new Piece(bone, Anchor.HEAD, delay, dx, dy, dz, Look.SOLID, true, "idle", 1f); }
    public static Piece ghost(String bone, Anchor a, int delay, float dx, float dy, float dz) { return new Piece(bone, a, delay, dx, dy, dz, Look.GHOST, false, "idle", 1f); }
    /** The same piece with an animation clip on its bone and its own glow strength. */
    public static Piece with(Piece p, String clip, float glowK) { return new Piece(p.bone(), p.anchor(), p.delay(), p.dx(), p.dy(), p.dz(), p.look(), p.firstPersonHidden(), clip, glowK); }

    private static final RandomSource PROBE = RandomSource.create(1L);
    private static final int FB = PropDraw.FULL_BRIGHT;

    /** The part a piece is anchored on. */
    public static ModelPart part(AuraContext c, Anchor a) {
        return switch (a) {
            case HEAD -> c.model().head;
            case BODY -> c.model().body;
            case ARM_R -> c.model().rightArm;
            case ARM_L -> c.model().leftArm;
            case LEG_R -> c.model().rightLeg;
            case LEG_L -> c.model().leftLeg;
        };
    }

    /** True when the skin has slim (3 px) arms: the arm part's box is one pixel narrower. */
    public static boolean slim(AuraContext c) {
        try {
            ModelPart.Cube cube = c.model().rightArm.getRandomCube(PROBE);
            return cube != null && cube.maxX - cube.minX < 3.5f;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Draws a whole set: solid pieces, ghost pieces, then every glow. */
    public static void paint(AuraContext c, Set set) {
        GeoModelData model = GeoModels.model(set.spec().model());
        if (model == null || model.roots.isEmpty()) return;
        GeoAnim anims = GeoModels.animations(set.spec().animations());
        boolean first = c.player() == Minecraft.getInstance().player && Minecraft.getInstance().options.getCameraType().isFirstPerson();
        boolean slim = slim(c);
        float seconds = c.age() / 20f;
        List<GeoModelData.Bone> keep = new ArrayList<>(model.roots);
        try {
            for (int pass = 0; pass < 3; pass++) {
                for (Piece p : set.pieces()) {
                    if (first && p.firstPersonHidden()) continue;
                    if (pass == 0 && p.look() != Look.SOLID) continue;
                    if (pass == 1 && p.look() != Look.GHOST) continue;
                    draw(c, set, model, anims, p, pass, slim, seconds);
                }
            }
        } finally {
            model.roots.clear();
            model.roots.addAll(keep);
        }
    }

    private static void draw(AuraContext c, Set set, GeoModelData model, GeoAnim anims, Piece p, int pass, boolean slim, float seconds) {
        GeoModelData.Bone bone = model.bones.get(p.bone());
        if (bone == null) return;
        float a = EquipAnimation.assembled(c, p.delay());
        if (a == 0f) return;
        PoseStack pose = c.pose();
        ModelPart part = part(c, p.anchor());
        pose.pushPose();
        part.translateAndRotate(pose);
        pose.scale(1f / 16f, 1f / 16f, 1f / 16f);
        float alpha = EquipAnimation.begin(pose, a, p.dx(), p.dy(), p.dz());
        pose.translate(-part.x, -part.y, -part.z);                                                 // back to the model's absolute frame, in pixels
        if (slim && (p.anchor() == Anchor.ARM_R || p.anchor() == Anchor.ARM_L)) {
            float inner = p.anchor() == Anchor.ARM_R ? -4f : 4f;
            pose.translate(inner, 0f, 0f);
            pose.scale(0.78f, 1f, 1f);
            pose.translate(-inner, 0f, 0f);
        }
        pose.scale(16f, 16f, 16f);
        model.roots.clear();
        model.roots.add(bone);
        GeoAnim.Clip clip = p.clip() == null || anims == null ? null : anims.clip(p.clip());
        if (pass == 2) {
            float pulse = 0.78f + 0.22f * Mth.sin(c.age() * 0.13f + p.delay());
            int g = AuraRender.alpha(0xFF000000 | set.glowRgb(), Mth.clamp(alpha * pulse * p.glowK() * 0.6f, 0f, 1f));
            if ((g >>> 24) > 2 && set.spec().glow() != null) {
                VertexConsumer vc = c.buffers().getBuffer(AuraRender.additive(set.spec().glow()));
                GeoDraw.draw(pose, vc, model, clip, seconds, GeoDraw.Space.PLAYER, FB, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, g);
            }
        } else {
            ResourceLocation tex = set.spec().texture();
            boolean solid = p.look() == Look.SOLID && alpha >= 0.985f;
            VertexConsumer vc = c.buffers().getBuffer(solid ? AuraRender.cutout(tex) : AuraRender.translucent(tex));
            int argb = AuraRender.alpha(0xFFFFFFFF, alpha);
            GeoDraw.draw(pose, vc, model, clip, seconds, GeoDraw.Space.PLAYER, c.light(), net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, argb);
        }
        pose.popPose();
    }
}
