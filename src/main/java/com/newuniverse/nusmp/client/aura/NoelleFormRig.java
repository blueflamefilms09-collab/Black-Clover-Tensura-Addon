package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.book.NoelleFormRules;
import com.newuniverse.nusmp.client.aura.WaterAuraRig.Anchor;
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
 * Why: the piece engine of Noelle Silva's forms (docs/magic/noelle_forms.md). It is {@link WaterAuraRig}'s anchoring, slim-arm narrowing and forge-in / shatter-out ({@link EquipAnimation}),
 * with the one thing the forms need that the shared rig (also used by the Dark, Lightning and Flame armour) does not have: a LEVEL OF DETAIL decided by {@link NoelleFormRules#lod}. A piece is
 * flagged {@link Piece#detail()} (small ornaments: skipped beyond 24 blocks) and/or {@link Piece#character()} (the extra pieces of the character tier: Noelle's grimoire only, and only within
 * 32 blocks); nothing is drawn beyond 64 blocks. The shared rig is not edited. Nothing is allocated per frame apart from the rotation of a moving part.
 */
public final class NoelleFormRig {
    private NoelleFormRig() {}

    /** One piece of a form. Build with {@link #of} and the chained flags. */
    public static final class Piece {
        public final String name;
        public final Anchor anchor;
        public final float delay, dx, dy, dz;
        boolean glass, flap, detail, head, character;
        String clip;
        float speed = 1f, glowK = 1f, phase;

        private Piece(String name, Anchor anchor, float delay, float dx, float dy, float dz) {
            this.name = name; this.anchor = anchor; this.delay = delay; this.dx = dx; this.dy = dy; this.dz = dz;
        }

        /** @param name assets/nusmp/geo/entity/&lt;key&gt;_arm_&lt;name&gt;; delay 0..12 ticks; dx dy dz the direction (model pixels, y down) the piece flies in from. */
        public static Piece of(String name, Anchor anchor, float delay, float dx, float dy, float dz) { return new Piece(name, anchor, delay, dx, dy, dz); }

        public Piece glass() { this.glass = true; return this; }
        public Piece clip(String clip, float speed) { this.clip = clip; this.speed = speed; return this; }
        public Piece flap() { this.flap = true; return this; }
        /** Small details: skipped for players farther than 24 blocks. */
        public Piece detail() { this.detail = true; return this; }
        /** Sits on the face: skipped for the local player in first person. */
        public Piece head() { this.head = true; return this; }
        /** Only the character tier (Noelle's grimoire) draws it, and only within 32 blocks. */
        public Piece character() { this.character = true; return this; }
        public Piece glow(float k) { this.glowK = k; return this; }
        public Piece phase(float seconds) { this.phase = seconds; return this; }
        public boolean isCharacter() { return character; }
        public boolean isDetail() { return detail; }
    }

    /** The pieces of one form and their model specs (resolved once). */
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

    /** The squared distance from the camera to the player (0 when there is no camera: the previews, a fake client). */
    public static double dist2(AuraContext c) {
        try {
            var cam = Minecraft.getInstance().gameRenderer.getMainCamera();
            return cam.getPosition().distanceToSqr(c.player().getX(), c.player().getY(), c.player().getZ());
        } catch (RuntimeException ex) {
            return 0.0;
        }
    }

    /** True for the local player seen from their own eyes (the face pieces and the eyes are not drawn then). */
    public static boolean selfFirstPerson(AuraContext c) { return firstPersonSelf(c); }

    private static boolean firstPersonSelf(AuraContext c) {
        try {
            Minecraft mc = Minecraft.getInstance();
            return c.player() == mc.player && mc.options.getCameraType().isFirstPerson();
        } catch (RuntimeException ex) {
            return false;
        }
    }

    /**
     * Draws the set at {@code lod}. {@code glowRgb} tints every glow map; {@code glowK} (0..1.5) is the glow level this frame; {@code tintRgb} is multiplied into every piece. The caller has
     * drawn the solid skin overlay already; the additive extras come after this call.
     */
    public static void draw(AuraContext c, Set set, NoelleFormRules.Lod lod, int glowRgb, float glowK, int tintRgb) {
        if (lod == NoelleFormRules.Lod.NONE) return;
        boolean near = lod != NoelleFormRules.Lod.FAR, ch = lod == NoelleFormRules.Lod.CHARACTER, self1p = firstPersonSelf(c);
        boolean slim = WaterAuraRig.slim(c.model());
        boolean air = !c.player().onGround() && !c.player().isInWater();
        PoseStack pose = c.pose();
        int lit = SealDraw.lift(c.light(), 6);
        for (int i = 0; i < set.pieces.length; i++) {
            Piece p = set.pieces[i];
            if ((p.detail && !near) || (p.character && !ch) || (p.head && self1p)) continue;
            float a = EquipAnimation.assembled(c, p.delay);
            if (a == 0f) continue;
            ModelPart part = part(c.model(), p.anchor);
            PartPose rest = part.getInitialPose();
            pose.pushPose();
            pose.translate(part.x / 16f, part.y / 16f, part.z / 16f);                 // the part's own pose (the same chain as ModelPart.translateAndRotate, without its scale)
            if (part.xRot != 0f || part.yRot != 0f || part.zRot != 0f) pose.mulPose(new Quaternionf().rotationZYX(part.zRot, part.yRot, part.xRot));
            pose.scale(1f / 16f, 1f / 16f, 1f / 16f);
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
