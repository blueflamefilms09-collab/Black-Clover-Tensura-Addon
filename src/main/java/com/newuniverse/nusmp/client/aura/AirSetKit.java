package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.resources.PlayerSkin;

/**
 * Why: the 1.93.0 rebuild gives Smoke, Snow, Wing, Vortex, Air and Ash Magic a real SET of temporary armour pieces (helm, chest, belt, back piece, two
 * pauldrons, two vambraces, two greaves ...), each its own GeoLite model with a glow map, anchored on the model part it was built for (so it follows
 * crouching, swimming and swinging), and every set must FORGE IN and SHATTER OUT exactly like the Djinn Full Equip ({@link EquipAnimation}). This kit
 * is that shared plumbing: a list of {@link Piece}s per magic, one {@code paint} call.
 *
 * <p>How a piece is drawn (the same order as the Djinn layer's {@code part}): the pose goes to the anchor part (translateAndRotate: the piece turns about
 * the part's own pivot), the pose is scaled to model pixels, {@link EquipAnimation#begin} applies the fly-in / shatter offset and size, the pose goes
 * back to blocks and to the part's own offset so the model file's absolute humanoid coordinates apply, and the model is painted: solid (cutout) when it
 * is fully assembled and the magic wants a solid look, translucent while it forges in or shatters out (cutout cannot fade) or always for the magics whose
 * armour is glass or air. The glow map goes on additive and full bright, pulsed by the caller. Arm pieces are authored for wide arms and narrowed about
 * the arm's own centre for slim skins.
 *
 * <p>Pieces that sit on the head or the back are skipped for the local player in first person (the game does not draw that body there anyway and the
 * pieces would hang in front of the camera).
 */
public final class AirSetKit {
    private AirSetKit() {}

    public enum Anchor { HEAD, BODY, ARM_R, ARM_L, LEG_R, LEG_L }

    /**
     * One piece of a set.
     *
     * @param delay  stagger in ticks (0..12), see {@link EquipAnimation}
     * @param dx     the direction the piece flies in from, in model pixels in the anchor part's frame (the Djinn's own numbers are about 2 to 14)
     * @param hide1p skipped for the local player in first person
     * @param glow   the piece's glow map is drawn (an extra additive pass over the whole model: only for pieces with real glowing parts)
     */
    public record Piece(GeoSpec spec, Anchor anchor, float delay, float dx, float dy, float dz, boolean hide1p, boolean glow) {}

    public static Piece piece(String key, String name, Anchor anchor, float delay, float dx, float dy, float dz, boolean hide1p, boolean glow) {
        return new Piece(GeoSpec.of(key, name), anchor, delay, dx, dy, dz, hide1p, glow);
    }

    public static Piece piece(String key, String name, Anchor anchor, float delay, float dx, float dy, float dz, boolean hide1p) {
        return piece(key, name, anchor, delay, dx, dy, dz, hide1p, false);
    }

    public static boolean firstPersonSelf(AuraContext c) {
        Minecraft mc = Minecraft.getInstance();
        return c.player() == mc.player && mc.options.getCameraType().isFirstPerson();
    }

    public static boolean slim(AuraContext c) { return c.player().getSkin().model() == PlayerSkin.Model.SLIM; }

    private static ModelPart part(AuraContext c, Anchor a) {
        return switch (a) {
            case HEAD -> c.model().head;
            case BODY -> c.model().body;
            case ARM_R -> c.model().rightArm;
            case ARM_L -> c.model().leftArm;
            case LEG_R -> c.model().rightLeg;
            case LEG_L -> c.model().leftLeg;
        };
    }

    /**
     * Paints the set.
     *
     * @param translucent  true: every piece is drawn translucent (glass and air armour); false: solid once assembled
     * @param glowArgb     colour and strength of the additive glow pass (alpha 0 = none); the pieces' own fade is multiplied in
     * @return how many pieces were drawn (for the preview's budget check)
     */
    public static int paint(AuraContext c, Piece[] set, boolean translucent, int glowArgb, float baseAlpha) {
        boolean self1p = firstPersonSelf(c), slim = slim(c);
        int drawn = 0;
        float seconds = c.age() / 20f;
        PoseStack pose = c.pose();
        for (Piece p : set) {
            if (self1p && p.hide1p()) continue;
            float a = EquipAnimation.assembled(c, p.delay());
            if (a == 0f) continue;
            ModelPart part = part(c, p.anchor());
            pose.pushPose();
            part.translateAndRotate(pose);
            pose.scale(1f / 16f, 1f / 16f, 1f / 16f);
            float alpha = EquipAnimation.begin(pose, a, p.dx(), p.dy(), p.dz());
            pose.scale(16f, 16f, 16f);
            pose.translate(-part.x / 16f, -part.y / 16f, -part.z / 16f);
            if (slim && (p.anchor() == Anchor.ARM_R || p.anchor() == Anchor.ARM_L)) {
                float side = p.anchor() == Anchor.ARM_R ? -1f : 1f;
                pose.translate(side * 6f / 16f, 0f, 0f);
                pose.scale(0.88f, 1f, 1f);
                pose.translate(-side * 6f / 16f - side * 0.5f / 16f, 0f, 0f);
            }
            boolean solid = !translucent && a >= 1f;
            int argb = solid ? 0xFFFFFFFF : AuraRender.alpha(0xFFFFFFFF, alpha * baseAlpha);
            int glow = (glowArgb >>> 24) == 0 || !p.glow() ? 0 : AuraRender.alpha(glowArgb, alpha);
            GeoDraw.paint(pose, c.buffers(), p.spec(), "idle", seconds, GeoDraw.Space.PLAYER, solid ? GeoDraw.Layer.CUTOUT : GeoDraw.Layer.TRANSLUCENT,
                    c.light(), argb, glow);
            pose.popPose();
            drawn++;
        }
        return drawn;
    }
}
