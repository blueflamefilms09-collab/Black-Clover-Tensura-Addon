package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 1.93.0 nature-magic rebuild: the shared kit of the six temporary armours (Tree, Vine, Plant, Poison Plant, Mud, Mucus). One armour is a SET OF PIECES (helm, crown, chest, pauldrons, bracers,
 * greaves, back, aux), each a GeoLite model authored on the standing humanoid and anchored on the posed model part it was built for (so it follows every limb of the walk and the swing). Every
 * piece forges in and shatters out exactly like the Djinn Full Equip through {@link EquipAnimation}: a per-piece stagger of 0 to 12 ticks, a fly-in from the piece's own side while it fades up
 * and grows 85 to 100 percent over 8 ticks, and a shatter over the last 18 ticks of the aura. Nothing here touches an inventory or an armour slot.
 * <p>Render layers (explicit, in this order): 1. the skin overlay (the posed model drawn again 4 percent wider, cutout, or translucent while it fades and for the gel of Mucus);
 * 2. the pieces, cutout (alpha-tested; translucent only while a piece forges or shatters, because cutout cannot fade, and always for gel) each followed by its own glow map, additive and full
 * bright; 3. the skin glow, additive (RenderType.eyes); the painter adds its own sprites last (additive).
 * <p>Fit: the arm pieces are authored for the wide (4 px) arm and are squeezed to 75 percent across the arm for slim (3 px) arms about the arm's inner edge. Head pieces and the pieces that hang in
 * front of the face are left out for the local player in first person; pieces flagged DETAIL are skipped beyond 24 blocks, the whole set beyond 56.
 */
public final class TreeArmour {
    private TreeArmour() {}

    /** The model part a piece is anchored on. */
    public enum Part {
        HEAD, BODY, R_ARM, L_ARM, R_LEG, L_LEG;

        ModelPart of(PlayerModel<AbstractClientPlayer> m) {
            return switch (this) {
                case HEAD -> m.head;
                case BODY -> m.body;
                case R_ARM -> m.rightArm;
                case L_ARM -> m.leftArm;
                case R_LEG -> m.rightLeg;
                case L_LEG -> m.leftLeg;
            };
        }

        /** -1 right arm, +1 left arm, 0 anything else. */
        float arm() { return this == R_ARM ? -1f : this == L_ARM ? 1f : 0f; }
    }

    public static final int HIDE_1P = 1, DETAIL = 2, GLOWS = 4;

    /**
     * One piece of a set.
     *
     * @param delay the stagger in ticks (0 to 12, see EquipAnimation)
     * @param dx    the fly-in offset in model pixels, in the anchor part's own frame (y points down), the Djinn's numbers are about 2 to 14
     * @param flags HIDE_1P, DETAIL, GLOWS
     */
    public record Piece(GeoSpec spec, Part part, int delay, float dx, float dy, float dz, int flags) {
        public boolean has(int f) { return (flags & f) != 0; }
    }

    public static Piece piece(String key, String name, Part part, int delay, float dx, float dy, float dz, int flags) {
        return new Piece(GeoSpec.of(key, name), part, delay, dx, dy, dz, flags);
    }

    /** How a set is drawn. glowRgb is the glow map's tint; glowK its strength (0..1); translucent forces the alpha-blended layer (gel). */
    public record Style(int glowRgb, float glowK, float pulse, boolean translucent) {}

    public static boolean firstPerson(AuraContext c) {
        Minecraft mc = Minecraft.getInstance();
        return c.player() == mc.player && mc.options.getCameraType().isFirstPerson();
    }

    /** True when the player's arms are the slim (3 px) kind. */
    public static boolean slim(AbstractClientPlayer p) {
        try {
            return p.getSkin().model() == PlayerSkin.Model.SLIM;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Distance from the camera to the player in blocks (0 when unknown). */
    public static double distance(AuraContext c) {
        try {
            var cam = Minecraft.getInstance().gameRenderer.getMainCamera();
            return Math.sqrt(cam.getPosition().distanceToSqr(c.player().getX(), c.player().getY(), c.player().getZ()));
        } catch (RuntimeException e) {
            return 0.0;
        }
    }

    public static int argb(float a, int rgb) { return (Mth.clamp((int) (a * 255f), 0, 255) << 24) | (rgb & 0xFFFFFF); }

    /** Draws the whole set. Returns false when nothing was drawn (the player is invisible or far away). */
    public static boolean paint(AuraContext c, Piece[] set, Style st) {
        if (c.player().isInvisible()) return false;
        double dist = distance(c);
        if (dist > 56.0) return false;
        boolean detail = dist < 24.0, fp = firstPerson(c), slim = slim(c.player());
        float pulse = 1f + st.pulse() * Mth.sin(c.age() * 0.13f);
        for (Piece p : set) {
            if ((fp && p.has(HIDE_1P)) || (!detail && p.has(DETAIL))) continue;
            drawPiece(c, p, st, slim, pulse);
        }
        return true;
    }

    /** One piece: anchored on its part, the Djinn's forge-in / shatter-out through EquipAnimation, drawn on the cutout (or translucent) layer with its glow map. */
    public static void drawPiece(AuraContext c, Piece p, Style st, boolean slim, float pulse) {
        float a = EquipAnimation.assembled(c, p.delay());
        if (a == 0f) return;
        PoseStack pose = c.pose();
        ModelPart mp = p.part().of(c.model());
        pose.pushPose();
        mp.translateAndRotate(pose);
        pose.scale(1f / 16f, 1f / 16f, 1f / 16f);
        float alpha = EquipAnimation.begin(pose, a, p.dx(), p.dy(), p.dz());
        pose.scale(16f, 16f, 16f);
        pose.translate(-mp.x / 16f, -mp.y / 16f, -mp.z / 16f);
        float arm = p.part().arm();
        if (slim && arm != 0f) {
            pose.translate(arm * 4f / 16f, 0f, 0f);
            pose.scale(0.75f, 1f, 1f);
            pose.translate(-arm * 4f / 16f, 0f, 0f);
        }
        boolean solid = alpha >= 0.999f && !st.translucent();
        GeoDraw.Layer layer = solid ? GeoDraw.Layer.CUTOUT : GeoDraw.Layer.TRANSLUCENT;
        int glow = p.has(GLOWS) ? argb(Mth.clamp(alpha * st.glowK() * pulse, 0f, 1f), st.glowRgb()) : 0;
        GeoDraw.paint(pose, c.buffers(), p.spec(), "idle", c.age() / 20f, GeoDraw.Space.PLAYER, layer, c.light(), argb(alpha, 0xFFFFFF), glow);
        pose.popPose();
    }

    /**
     * The skin overlay: the posed player model drawn again over the skin (a 64x64 skin layout, only the painted rectangles opaque, the head left empty so the face is never covered).
     * Solid cutout when fully on, translucent while it forges or shatters (the whole overlay follows the chest piece's delay) and always translucent for gel; the glow is a second additive pass.
     */
    public static void skin(AuraContext c, ResourceLocation skin, ResourceLocation glow, int glowRgb, float glowK, boolean translucent, int delay, float grow) {
        float a = EquipAnimation.assembled(c, delay);
        if (a == 0f) return;
        float alpha = EquipAnimation.alpha(a);
        boolean solid = alpha >= 0.999f && !translucent;
        AuraRender.model(c, solid ? AuraRender.cutout(skin) : AuraRender.translucent(skin), 1f + grow, 1f + grow * 0.3f, 1f + grow, argb(alpha, 0xFFFFFF));
        if (glow != null && glowK > 0.01f)
            AuraRender.modelBright(c, AuraRender.additive(glow), 1f + grow * 1.1f, 1f + grow * 0.4f, 1f + grow * 1.1f, argb(alpha * glowK, glowRgb));
    }
}
