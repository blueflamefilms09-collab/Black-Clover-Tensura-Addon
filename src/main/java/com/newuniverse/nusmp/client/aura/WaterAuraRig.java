package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.client.prop.SealDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

/**
 * 1.93.0 (the Water / Dark / Lightning / Flame rebuild): the shared engine of the four temporary armours. An armour is a {@link Set} of {@link Piece}s; each
 * piece is its own GeoLite model (assets/nusmp/geo/entity/&lt;key&gt;_arm_&lt;name&gt;.geo.json, a texture, a {@code _glow} map, optionally an animation clip) authored in
 * WHOLE-BODY coordinates (feet at y = 0, shoulders at y = 24, the right arm at x = -5, see tools/water_aura_lib.py), anchored on the model part it was built for
 * (head, body, an arm, a leg) so it follows every pose, crouch, swing and swim, and forged in / shattered out by {@link EquipAnimation} EXACTLY like the Djinn full
 * equip: staggered by the piece's delay (0 to 12 ticks), flying in from its own side while it fades up and grows from 85 to 100 percent over 8 ticks, and shattering
 * out over the last 18 ticks of the aura.
 *
 * <p>Passes (the explicit render layers of the owner's engine): a piece that is fully on is drawn {@code CUTOUT} (entityCutoutNoCull, opaque with alpha holes) and its glow
 * map additive and full bright; while it forges in or shatters out, and always for a {@code glass} piece (water crystal, shadow, flame), it is drawn
 * {@code TRANSLUCENT} so it can fade. Solid overlays on the skin are the caller's job and go BEFORE the pieces (scaled at least 2 percent up); additive extras go AFTER.
 * Slim arms (Alex): the arm pieces are narrowed to 80 percent about the arm's own pivot. Nothing here is put in an inventory or an armour slot.</p>
 */
public final class WaterAuraRig {
    private WaterAuraRig() {}

    public enum Anchor { HEAD, BODY, ARM_R, ARM_L, LEG_R, LEG_L }

    /** The distance (squared) beyond which a piece flagged {@link Piece#detail()} is skipped, and beyond which the whole armour is skipped. */
    private static final double DETAIL2 = 24.0 * 24.0, FAR2 = 64.0 * 64.0;

    /** One piece of an armour. Build with {@link #of} and the chained flags. */
    public static final class Piece {
        public final String name;
        public final Anchor anchor;
        public final float delay, dx, dy, dz;
        boolean glass, flap, detail, head;
        String clip;
        float speed = 1f, glowK = 1f, phase;

        private Piece(String name, Anchor anchor, float delay, float dx, float dy, float dz) {
            this.name = name; this.anchor = anchor; this.delay = delay; this.dx = dx; this.dy = dy; this.dz = dz;
        }

        /**
         * @param name   the file name part: assets/nusmp/geo/entity/&lt;key&gt;_arm_&lt;name&gt;
         * @param delay  stagger in ticks, 0 to 12
         * @param dx dy dz the direction in model pixels the piece flies in from (vanilla layer space: y down; negative x = the model's right side); the Djinn uses 2 to 14
         */
        public static Piece of(String name, Anchor anchor, float delay, float dx, float dy, float dz) { return new Piece(name, anchor, delay, dx, dy, dz); }

        /** Always translucent (water, shadow, flame sheets). */
        public Piece glass() { this.glass = true; return this; }
        /** Plays the model's animation clip (looped) at this speed. */
        public Piece clip(String clip, float speed) { this.clip = clip; this.speed = speed; return this; }
        /** The clip runs faster while the wearer is in the air (wings). */
        public Piece flap() { this.flap = true; return this; }
        /** Small details: skipped for players farther than 24 blocks. */
        public Piece detail() { this.detail = true; return this; }
        /** Sits on the face: skipped for the local player in first person. */
        public Piece head() { this.head = true; return this; }
        /** Scales the glow of this piece. */
        public Piece glow(float k) { this.glowK = k; return this; }
        /** Offsets the clip clock (seconds) so repeated pieces do not move in step. */
        public Piece phase(float seconds) { this.phase = seconds; return this; }
    }

    /** The pieces of one armour and their model specs (resolved once). */
    public static final class Set {
        public final String key;
        public final Piece[] pieces;
        final GeoSpec[] specs;

        public Set(String key, Piece... pieces) {
            this.key = key;
            this.pieces = pieces;
            this.specs = new GeoSpec[pieces.length];
            for (int i = 0; i < pieces.length; i++) specs[i] = GeoSpec.of(key, "arm_" + pieces[i].name);
        }

        public int size() { return pieces.length; }
    }

    private static ModelPart part(PlayerModel<AbstractClientPlayer> m, Anchor a) {
        return switch (a) {
            case HEAD -> m.head;
            case BODY -> m.body;
            case ARM_R -> m.rightArm;
            case ARM_L -> m.leftArm;
            case LEG_R -> m.rightLeg;
            case LEG_L -> m.leftLeg;
        };
    }

    /** True for the Alex model: the game builds its arm pivots 0.5 px lower (2.5 instead of 2.0). */
    public static boolean slim(PlayerModel<AbstractClientPlayer> m) { return m.rightArm.getInitialPose().y > 2.25f; }

    /** 0..1: the opacity of a piece with this delay right now (forging in 0 to 1, fully on 1, shattering 1 to 0), for extras that should follow the armour's own forge. */
    public static float forge(AuraContext c, float delay) { return EquipAnimation.alpha(EquipAnimation.assembled(c, delay)); }

    /**
     * The body overlay of an armour (the posed player model drawn again with {@code rt}, scaled about the middle of the body), WITHOUT the head and the hat layer when
     * {@code skipHead}: the face stays the player's own (the owner's rule: an overlay never hides the face).
     */
    public static void shell(AuraContext c, net.minecraft.client.renderer.RenderType rt, float sx, float sy, float sz, int argb, boolean bright, boolean skipHead) {
        PlayerModel<AbstractClientPlayer> m = c.model();
        boolean h = m.head.visible, ha = m.hat.visible;
        if (skipHead) { m.head.visible = false; m.hat.visible = false; }
        try {
            if (bright) AuraRender.modelBright(c, rt, sx, sy, sz, argb); else AuraRender.model(c, rt, sx, sy, sz, argb);
        } finally {
            m.head.visible = h; m.hat.visible = ha;
        }
    }

    /** True when the player is farther than the 24 block detail distance from the camera (or when there is no camera: then false). */
    public static boolean far(AuraContext c) { return dist2(c) > DETAIL2; }

    private static double dist2(AuraContext c) {
        try {
            var cam = Minecraft.getInstance().gameRenderer.getMainCamera();
            return cam.getPosition().distanceToSqr(c.player().getX(), c.player().getY(), c.player().getZ());
        } catch (RuntimeException ex) {
            return 0.0;
        }
    }

    private static boolean firstPersonSelf(AuraContext c) {
        try {
            Minecraft mc = Minecraft.getInstance();
            return c.player() == mc.player && mc.options.getCameraType().isFirstPerson();
        } catch (RuntimeException ex) {
            return false;
        }
    }

    /**
     * Draws the whole set. {@code glowRgb} tints every glow map; {@code glowK} (0..1.5) is the armour's glow level this frame (a pulse the caller computes).
     * The caller has already drawn the solid overlays; additive extras come after this call.
     */
    public static void draw(AuraContext c, Set set, int glowRgb, float glowK) { draw(c, set, glowRgb, glowK, 0xFFFFFF); }

    /** The same with a colour multiplied into every piece (0xFFFFFF = the textures as they are; a style such as Black Lightning uses a dark grey). */
    public static void draw(AuraContext c, Set set, int glowRgb, float glowK, int tintRgb) {
        double d2 = dist2(c);
        if (d2 > FAR2) return;
        boolean near = d2 <= DETAIL2, self1p = firstPersonSelf(c);
        boolean slim = slim(c.model());
        boolean air = !c.player().onGround() && !c.player().isInWater();
        PoseStack pose = c.pose();
        int lit = SealDraw.lift(c.light(), 6);
        for (int i = 0; i < set.pieces.length; i++) {
            Piece p = set.pieces[i];
            if ((p.detail && !near) || (p.head && self1p)) continue;
            float a = EquipAnimation.assembled(c, p.delay);
            if (a == 0f) continue;
            ModelPart part = part(c.model(), p.anchor);
            PartPose rest = part.getInitialPose();
            pose.pushPose();
            pose.translate(part.x / 16f, part.y / 16f, part.z / 16f);                 // the part's own pose (the same chain as ModelPart.translateAndRotate, without its scale)
            if (part.xRot != 0f || part.yRot != 0f || part.zRot != 0f) pose.mulPose(new Quaternionf().rotationZYX(part.zRot, part.yRot, part.xRot));
            pose.scale(1f / 16f, 1f / 16f, 1f / 16f);                                  // model pixels: what EquipAnimation.begin's numbers are in
            float alpha = EquipAnimation.begin(pose, a, p.dx, p.dy, p.dz);
            pose.scale(16f, 16f, 16f);
            if (slim && (p.anchor == Anchor.ARM_R || p.anchor == Anchor.ARM_L)) pose.scale(0.8f, 1f, 1f);
            pose.translate(-rest.x / 16f, -rest.y / 16f, -rest.z / 16f);               // the model is authored in whole-body coordinates: undo the part's rest position
            float seconds = c.age() / 20f * p.speed * (p.flap ? (air ? 2.3f : 1f) : 1f) + p.phase;
            int white = ((int) (Mth.clamp(alpha, 0f, 1f) * 255f + 0.5f) << 24) | (tintRgb & 0xFFFFFF);
            int glow = ((int) (Mth.clamp(alpha * glowK * p.glowK, 0f, 1f) * 255f + 0.5f) << 24) | (glowRgb & 0xFFFFFF);
            boolean translucent = p.glass || a < 1f;
            GeoDraw.paint(pose, c.buffers(), set.specs[i], p.clip, seconds, GeoDraw.Space.PLAYER, translucent ? GeoDraw.Layer.TRANSLUCENT : GeoDraw.Layer.CUTOUT,
                    translucent ? lit : c.light(), white, glow);
            pose.popPose();
        }
    }
}
