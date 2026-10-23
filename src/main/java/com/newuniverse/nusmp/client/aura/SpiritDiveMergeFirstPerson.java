package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.book.SpiritDiveMerge2Rules;
import com.newuniverse.nusmp.book.SpiritDiveMergeRules;
import com.newuniverse.nusmp.book.SpiritDivePieces;
import com.newuniverse.nusmp.book.SpiritDivePieces.Element;
import com.newuniverse.nusmp.book.SpiritDiveWind3Mesh;
import com.newuniverse.nusmp.client.SafeAdditive;
import com.newuniverse.nusmp.client.prop.PropDraw;
import com.newuniverse.nusmp.client.prop.SealDraw;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Spirit Dive MERGE 2, FIRST PERSON: the wearer sees the merge too. In first person the player model is not drawn, so the third person merge ({@link SpiritDiveMerge}) shows nothing; this draws the same slim
 * (or boxy) spirit in the VIEW's own space: it appears at the upper side of the screen, a little ahead of the eyes, then flows down and in along a curve, shrinking, and passes the bottom edge of the screen
 * on its way into the chest, with a short soft flash at the contact. Everything is clamped by {@link SpiritDiveMerge2Rules}: no more than {@link SpiritDiveMerge2Rules#FP_MAX_COVERAGE} of the screen is ever
 * covered by the spirit or the flash, the spirit's alpha is a share of the third person ghost's and fades as it nears the eye, nothing is blurred, and the faint inner silhouette is NOT drawn.
 *
 * <p>(The event hook is {@link DiveMergeFirstPerson}; this class holds the drawing so the preview harness can run it.) Only the local player, only while his own Spirit Dive runs and the camera is first person, only during the 22 tick entry / exit (clock 0..18.6), only with the client options {@code spiritDive.merge} and
 * {@code spiritDive.mergeFirstPerson}. Cost: one ghost (at most 900 vertices, slim character tier) and 4 vertices of flash, once per frame; no allocation (static scratch), no packet, no server state. Drawn in the
 * level stage {@code AFTER_PARTICLES} (the same stage and frame set-up as the Paladin overlay). Not seen in a running game by its author.</p>
 */
public final class SpiritDiveMergeFirstPerson {
    private SpiritDiveMergeFirstPerson() {}

    private static final ResourceLocation FLASH = AuraRender.tex(SpiritDiveMergeRules.FLASH);
    private static final Quaternionf Q = new Quaternionf();
    private static final Matrix3f M3 = new Matrix3f();
    private static final Vector3f D = new Vector3f(), X = new Vector3f(), Y = new Vector3f(), Z = new Vector3f();
    private static final float[] PATH = new float[3];

    /**
     * Draws the first person merge in the pose's frame, which must be the view's space (x right, y up, -z ahead, blocks): the spirit and the flash. Shared by the game and the preview harness
     * ({@link #paintHarness}). {@code side} is +1 / -1 (the spirit comes in from that side), {@code m} the merge clock.
     */
    static void draw(PoseStack pose, MultiBufferSource buffers, Element el, boolean character, float side, float m, float age, float fovDeg, float aspect) {
        SpiritDiveMerge2Rules.fpPath(m, side, PATH);
        float px = PATH[0] * -1f, py = PATH[1], pz = -PATH[2];                                           // the rules use x to the left and z ahead; the view has x to the right and -z ahead
        float dist = (float) Math.sqrt(px * px + py * py + pz * pz);
        float alpha = SpiritDiveMerge2Rules.fpAlpha(m, dist);
        if (alpha > 0.01f && SpiritDiveMergeRules.flowing(m)) {
            float extent = SpiritDiveMerge2Rules.fpExtent(m, dist, fovDeg, aspect, character), s = SpiritDiveMerge2Rules.fpScale(extent);
            // the spirit's up (the mesh's -y) is the view's up; its front (the mesh's -z) looks along its way into the chest
            D.set(-SpiritDiveMerge2Rules.FP_CHEST_X - px, SpiritDiveMerge2Rules.FP_CHEST_Y - py, -SpiritDiveMerge2Rules.FP_CHEST_Z - pz).normalize();
            Y.set(0f, -1f, 0f);
            Z.set(-D.x, -D.y, -D.z);
            Z.fma(-Z.dot(Y), Y);
            if (Z.lengthSquared() < 1e-6f) Z.set(0f, 0f, 1f);
            Z.normalize();
            Y.cross(Z, X);                                                                              // x = y cross z: a right handed basis (mesh x, y, z in view space)
            M3.set(X.x, X.y, X.z, Y.x, Y.y, Y.z, Z.x, Z.y, Z.z);
            pose.pushPose();
            pose.translate(px, py, pz);
            pose.mulPose(Q.setFromNormalized(M3));
            pose.scale(s, s, s);
            SpiritDiveMerge.draw(pose, buffers, SpiritDiveMerge.ghostMesh(el, character), alpha);
            pose.popPose();
        }
        float fa = SpiritDiveMerge2Rules.fpFlashAlpha(m);
        if (fa > 0.01f) {                                                                                // the soft flash at the contact, at the chest: only its top edge reaches into the view
            float cx = -SpiritDiveMerge2Rules.FP_CHEST_X, cy = SpiritDiveMerge2Rules.FP_CHEST_Y + 0.62f, cz = -SpiritDiveMerge2Rules.FP_CHEST_Z - 0.2f, d = (float) Math.sqrt(cx * cx + cy * cy + cz * cz);
            float r = SpiritDiveMerge2Rules.fpFlashRadius(m, d, fovDeg, aspect);
            int argb = SealDraw.argb(fa, SpiritDiveMergeRules.flashRgb(el));
            var vc = buffers.getBuffer(SafeAdditive.additive(FLASH));
            PropDraw.quad(pose, vc, cx - r, cy - r, cz, cx + r, cy - r, cz, cx + r, cy + r, cz, cx - r, cy + r, cz, 0f, 0f, 1f, 1f, argb, PropDraw.FULL_BRIGHT);
            PropDraw.quad(pose, vc, cx - r, cy + r, cz, cx + r, cy + r, cz, cx + r, cy - r, cz, cx - r, cy - r, cz, 0f, 0f, 1f, 1f, argb, PropDraw.FULL_BRIGHT);
        }
    }

    /** The painter of the preview tool's first person scenes ({@code examples.SdmFp}): the wearer's own view (the eyes just above the neck line, looking ahead) with only the first person merge. Preview only. */
    public static void paintHarness(AuraContext c) {
        int style = c.style();
        Element el = SpiritDivePieces.element(style);
        float m = SpiritDiveMergeRules.clock(c.t(), c.left());
        PoseStack pose = c.pose();
        pose.pushPose();
        pose.translate(0f, -0.12f, -0.3f);                                                               // the eyes: a little above the neck line; the harness camera sits 0.3 block ahead of the face so the head is not in the view
        pose.mulPose(Q.rotationZ((float) Math.PI));                                                      // the view space (x right, y up, z back) in the model's own (x, y down, z)
        draw(pose, c.buffers(), el, SpiritDiveMergeRules.character(el, SpiritDivePieces.grade(style)), SpiritDiveMergeRules.side(c.player().getUUID().hashCode()), m, c.age(), 70f, 16f / 9f);
        pose.popPose();
    }
}
