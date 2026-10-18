package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;

/**
 * 1.93.0 Why: Sandstone, Rock, Red Ochre, Stone, Earth and Mineral magic each get a temporary armour that is a real SET OF PIECES on the player's
 * model, forged in and shattered out exactly like the Djinn full equip (every piece through {@link EquipAnimation}: staggered by its own delay, flying
 * in from its own side, fading up and growing 85 to 100 percent over 8 ticks, shattering over the last 18). This class is the part the six painters
 * share: the standard list of pieces (helm, chest, belt, mantle, pauldrons, vambraces, gauntlets, greaves, the animated piece behind the body and the
 * one round the head), each a geo model of the set's own ({@code <key>_<piece>}, tools/gen_earth_family_armor.py), anchored on the model part it was
 * built for so it turns with the limb (crouching, swimming, swinging). A set differs by its models, its glow colour and its sparks, not by code.
 *
 * <p>Passes per piece, in draw order: solid ({@code AuraRender.cutout}, geo CUTOUT) once the piece is fully forged, alpha blended ({@code translucent}) while it
 * forges in or shatters out (a cutout pass cannot fade), then the model's own {@code _glow} map additive and full bright. Nothing is drawn for the head
 * pieces of the local player in first person. The pieces are inflated by the generator (at least 0.3 px over the skin) so nothing is coplanar.</p>
 *
 * <p>The anchoring maths: a piece is authored in the humanoid's file frame, so it is drawn at {@code part.translateAndRotate} (the part's own pose, the
 * pivot at the origin), then moved by the equip animation in model pixels, then shifted back by the part's rest pivot so the file frame lines up again
 * (the same chain as {@code BronzeAura.piece}, with the Djinn's fly-in added in between so the scale grows about the part's pivot).</p>
 */
public final class EarthArmorKit {
    private EarthArmorKit() {}

    public enum Anchor { HEAD, BODY, RARM, LARM, RLEG, LLEG }

    /** One piece of the standard set: the model suffix, where it hangs, its stagger (ticks, 0 to 12) and the way it flies in from (model pixels, y down). */
    public record Piece(String name, Anchor anchor, int delay, float dx, float dy, float dz, boolean head, boolean detail) {}

    /** The standard list, in draw order. Delays follow the Djinn's: the helm and the shoulders first, the legs and the back last. */
    public static final Piece[] PIECES = {
            new Piece("helm", Anchor.HEAD, 0, 0f, -16f, 0f, true, false),
            new Piece("pauldron_r", Anchor.RARM, 1, -14f, -4f, 0f, false, false),
            new Piece("pauldron_l", Anchor.LARM, 1, 14f, -4f, 0f, false, false),
            new Piece("chest", Anchor.BODY, 2, 0f, 0f, -14f, false, false),
            new Piece("vambrace_r", Anchor.RARM, 3, -12f, 2f, -6f, false, false),
            new Piece("vambrace_l", Anchor.LARM, 3, 12f, 2f, -6f, false, false),
            new Piece("mantle", Anchor.BODY, 4, 0f, -4f, 8f, false, false),
            new Piece("gauntlet_r", Anchor.RARM, 5, -10f, 8f, -6f, false, false),
            new Piece("gauntlet_l", Anchor.LARM, 5, 10f, 8f, -6f, false, false),
            new Piece("belt", Anchor.BODY, 5, 0f, 10f, -8f, false, false),
            new Piece("greave_r", Anchor.RLEG, 6, -3f, 14f, -6f, false, false),
            new Piece("greave_l", Anchor.LLEG, 7, 3f, 14f, -6f, false, false),
            new Piece("back", Anchor.BODY, 8, 0f, -2f, 12f, false, true),
            new Piece("extra", Anchor.HEAD, 9, 0f, -14f, 0f, true, true),
    };

    /** A set: the models of its pieces (same order as {@link #PIECES}) and the tint of the glow passes. */
    public static final class Set {
        public final String key;
        final GeoSpec[] specs = new GeoSpec[PIECES.length];
        public final int glowRgb;

        public Set(String key, int glowRgb) {
            this.key = key;
            this.glowRgb = glowRgb & 0xFFFFFF;
            for (int i = 0; i < PIECES.length; i++) specs[i] = GeoSpec.of(key, PIECES[i].name);
        }
    }

    private static final double FAR2 = 48.0 * 48.0, DETAIL2 = 24.0 * 24.0;

    /** Draws the whole set for this frame. {@code glow} is the pulse of the glow passes, 0..1.5. */
    public static void paint(AuraContext c, Set set, float glow) {
        float f = c.fade();
        if (f <= 0.002f) return;
        Minecraft mc = Minecraft.getInstance();
        boolean self1p = c.player() == mc.player && mc.options.getCameraType().isFirstPerson();
        double d2 = mc.gameRenderer.getMainCamera() == null ? 0.0 : mc.gameRenderer.getMainCamera().getPosition().distanceToSqr(c.player().getX(), c.player().getY(), c.player().getZ());
        if (d2 > FAR2) return;
        boolean detail = d2 < DETAIL2;
        PlayerModel<?> m = c.model();
        for (int i = 0; i < PIECES.length; i++) {
            Piece p = PIECES[i];
            if (self1p && p.head()) continue;
            if (p.detail() && !detail) continue;
            piece(c, set, i, part(m, p.anchor()), glow);
        }
    }

    static ModelPart part(PlayerModel<?> m, Anchor a) {
        return switch (a) {
            case HEAD -> m.head;
            case BODY -> m.body;
            case RARM -> m.rightArm;
            case LARM -> m.leftArm;
            case RLEG -> m.rightLeg;
            case LLEG -> m.leftLeg;
        };
    }

    private static void piece(AuraContext c, Set set, int index, ModelPart part, float glow) {
        Piece p = PIECES[index];
        float a = EquipAnimation.assembled(c, p.delay());
        if (a == 0f) return;
        PoseStack pose = c.pose();
        PartPose rest = part.getInitialPose();
        pose.pushPose();
        part.translateAndRotate(pose);                                  // the part's own pose, its pivot at the origin (blocks)
        pose.scale(1f / 16f, 1f / 16f, 1f / 16f);                       // model pixels, as the Djinn's pieces
        float alpha = EquipAnimation.begin(pose, a, p.dx(), p.dy(), p.dz());
        pose.scale(16f, 16f, 16f);                                      // back to blocks...
        pose.translate(-rest.x / 16f, -rest.y / 16f, -rest.z / 16f);    // ...and to the file frame's origin
        boolean solid = alpha >= 0.999f;
        int argb = solid ? 0xFFFFFFFF : AuraRender.alpha(0xFFFFFFFF, alpha);
        int glowArgb = AuraRender.alpha(0xFF000000 | set.glowRgb, Math.min(1f, glow * alpha));
        GeoDraw.paint(pose, c.buffers(), set.specs[index], "idle", c.age() / 20f, GeoDraw.Space.PLAYER,
                solid ? GeoDraw.Layer.CUTOUT : GeoDraw.Layer.TRANSLUCENT, c.light(), argb, glowArgb);
        pose.popPose();
    }
}
