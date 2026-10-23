package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.book.FuegoleonArmDjinnRules;
import com.newuniverse.nusmp.book.FuegoleonArmDjinnRules.Lod;
import com.newuniverse.nusmp.book.FuegoleonArmDjinnRules.State;
import com.newuniverse.nusmp.book.FuegoleonArmRules;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.client.prop.PropDraw;
import com.newuniverse.nusmp.client.prop.SealDraw;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

/**
 * Fuegoleon's flame arm (3.1), the DJINN look: a sculpted limb of flame forged in piece by piece, as the Djinn Equip is. See docs/magic/fuegoleon_arm_djinn.md.
 *
 * <p><b>Parts.</b> Five models of the {@code fuegoleonarm} key, one per piece ({@code shoulder, upper, elbow, fore, fist}: 21 cubes in all, 24 vertices a cube, two passes), each anchored on the
 * player's right arm like the Djinn armour layer anchors its pieces: the models are authored in whole-body coordinates, so the arm's pivot and rotation are applied and the arm's rest position undone
 * (the same placement as the 3.0.0 arm and the Djinn gauntlet). Their textures are the reference's cel-shaded flame (bands of white-yellow, yellow, orange, red and a dark red outline) and
 * their glow maps light only the hot core. The tongues, sparks, haze and flashes are {@link FuegoleonArmDjinnFx}.</p>
 *
 * <p><b>Forge-in.</b> {@code EquipAnimation.assembled} (the Djinn's own animation, staggered {@link FuegoleonArmDjinnRules#PIECE_DELAY}) drives every piece: it fades up, grows from 85 percent and slides in from
 * the shoulder side, with a flash on its centre, so the flame kindles from the shoulder down to the fist in 20 ticks whenever the Salamander bond starts (the server decides when: the
 * {@code Aura.FUEGOLEON_ARM} lease; the client only picks the look). When the lease runs out unrenewed the pieces shatter out the same way.</p>
 *
 * <p><b>Cost.</b> Worst case per arm at the full level: 21 cubes * 24 * 2 passes = 1008 vertices plus {@link FuegoleonArmDjinnRules#worstQuads()} quads (176 vertices); the Djinn's arm
 * assembly is 1324. At most {@link FuegoleonArmDjinnRules#FULL_PER_FRAME} arms per frame draw at the full level, the rest of a crowd HALF (no glow pass, five tongues of two segments, no sparks);
 * beyond 48 blocks nothing. No allocation per frame; first person is always full.</p>
 */
public final class FuegoleonArmDjinn {
    private FuegoleonArmDjinn() {}

    private static final GeoSpec[] SPECS = {GeoSpec.of(FuegoleonArmDjinnRules.GEO_KEY, "shoulder"), GeoSpec.of(FuegoleonArmDjinnRules.GEO_KEY, "upper"),
            GeoSpec.of(FuegoleonArmDjinnRules.GEO_KEY, "elbow"), GeoSpec.of(FuegoleonArmDjinnRules.GEO_KEY, "fore"), GeoSpec.of(FuegoleonArmDjinnRules.GEO_KEY, "fist")};
    private static final int PIECES = SPECS.length;
    private static final float[] ASM = new float[PIECES];                                     // scratch: how far each piece is forged in this paint
    private static final Quaternionf ROT = new Quaternionf();                                 // scratch: the render thread is the only user
    private static final FuegoleonArmDjinnRules.Cap FULL = new FuegoleonArmDjinnRules.Cap();

    /** The squared distance from the camera to the player being painted (0 when the camera cannot be read). */
    static double distSq(AuraContext c) {
        try {
            var cam = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera();
            return cam.getPosition().distanceToSqr(c.player().getX(), c.player().getY(), c.player().getZ());
        } catch (RuntimeException ex) {
            return 0.0;
        }
    }

    /**
     * Third person: the flame arm of one wearer, called by {@link FuegoleonArmAura} for the DJINN look (the lease and the style are already checked).
     */
    static void paint(AuraContext c, FuegoleonArmRules.View view) {
        int id = c.player().getId();
        boolean casting = DiveCast.casting(id), diving = PlayerAuraClient.has(id, com.newuniverse.nusmp.aura.Aura.SPIRIT_DIVE);
        State st = FuegoleonArmDjinnRules.state(c.style() == FuegoleonArmRules.STYLE_LINKED, casting, diving);
        if (FuegoleonArmDjinnRules.visible(view, st) <= 0f) return;
        Lod lod = FuegoleonArmDjinnRules.lod(distSq(c));
        if (lod == Lod.CULLED) return;
        if (lod == Lod.FULL) {                                                                // the per-frame cap: a crowd draws the rest at HALF (partial tick + game time tell the frame)
            long frame = c.player().level().getGameTime() * 4096L + (long) (c.partial() * 4000f);
            if (!FULL.admit(frame, id)) lod = Lod.HALF;
        }
        ModelPart arm = c.model().rightArm;
        PoseStack pose = c.pose();
        boolean slim = WaterAuraRig.slim(c.model());
        pose.pushPose();
        pose.translate(arm.x / 16f, arm.y / 16f, arm.z / 16f);
        if (arm.xRot != 0f || arm.yRot != 0f || arm.zRot != 0f) pose.mulPose(ROT.rotationZYX(arm.zRot, arm.yRot, arm.xRot));
        limb(pose, c.buffers(), arm.getInitialPose(), c.age(), c.t(), c.left(), c.fade(), st, lod, slim);
        pose.scale(1f / 16f, 1f / 16f, 1f / 16f);
        FuegoleonArmDjinnFx.draw(pose, c.buffers(), c.age(), st, lod, false, ASM);
        pose.popPose();
    }

    /**
     * The five pieces of the limb. {@code pose} is at the arm's pivot with the arm's rotation applied (blocks); the models are authored in whole-body coordinates, so the rest position is undone here. Fills
     * {@link #ASM} (how far each piece is forged in) for the effects that follow.
     *
     * @param t    ticks since the arm was lit (partial tick included)
     * @param left ticks until the lease would run out
     * @param fade 0..1, below 0.97 the limb is drawn blended instead of solid
     */
    static void limb(PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers, PartPose rest, float age, float t, float left, float fade, State st, Lod lod, boolean slim) {
        pose.pushPose();
        if (slim) pose.scale(0.8f, 1f, 1f);
        pose.translate(-rest.x / 16f, -rest.y / 16f, -rest.z / 16f);
        int glow = lod == Lod.FULL ? SealDraw.argb(Mth.clamp(FuegoleonArmDjinnRules.glow(st), 0f, 1f), 0xFFFFFF) : 0;
        for (int i = 0; i < PIECES; i++) {
            float a = EquipAnimation.assembled(t, left, FuegoleonArmDjinnRules.PIECE_DELAY[i]);
            ASM[i] = a;
            if (a == 0f) continue;
            float[] ctr = FuegoleonArmDjinnRules.PIECE_CENTRE[i];                              // grow and slide about the piece's own centre (arm frame -> whole-body blocks)
            float cx = (rest.x + ctr[0]) / 16f, cy = (rest.y + ctr[1]) / 16f, cz = (rest.z + ctr[2]) / 16f;
            pose.pushPose();
            pose.translate(cx, cy, cz);
            float alpha = EquipAnimation.begin(pose, a, 0f, FuegoleonArmDjinnRules.PIECE_FROM_Y[i] / 16f, 0f);
            pose.translate(-cx, -cy, -cz);
            boolean solid = a >= 1f && fade >= 0.97f;
            GeoDraw.paint(pose, buffers, SPECS[i], "idle", age / 20f, GeoDraw.Space.PLAYER, solid ? GeoDraw.Layer.CUTOUT : GeoDraw.Layer.TRANSLUCENT, PropDraw.FULL_BRIGHT,
                    SealDraw.argb(alpha, 0xFFFFFF), glow);
            pose.popPose();
        }
        pose.popPose();
    }

    /** The forge-in state of the pieces after the last {@link #limb} call (for the effects drawn next). */
    static float[] assembled() { return ASM; }

    /** The effects of the arm in first person (the hand's own camera basis), after {@link #limb}. {@code pose} is at the arm pivot, in model pixels. */
    static void fxFirstPerson(PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers, float age, State st) {
        FuegoleonArmDjinnFx.draw(pose, buffers, age, st, Lod.FULL, true, ASM);
    }
}
