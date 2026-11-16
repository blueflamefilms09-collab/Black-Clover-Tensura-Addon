package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.beastwill.BeastTailRules;
import com.newuniverse.nusmp.book.NoelleFormRules;
import com.newuniverse.nusmp.client.bossmesh.BossMeshDraw;
import com.newuniverse.nusmp.perf.PerfProbe;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.Map;

/**
 * Why (docs/beastwill_tails.md): draws the Blender-made tails and tentacles of the Beast Will forms ({@code entity_meshes/bwtail_<stem>.txt}, rules in {@link BeastTailRules}) on the form's body, in the place of the
 * cube tail piece. The mesh is one chain of parts per tail or tentacle; this class is the {@link BossMeshDraw.Placer}: it anchors the mesh on the player's body part (so it follows a crouch or a lean), and turns every
 * part about its joint by the gentle sway of {@link BeastTailRules#angles}, each part on top of the one before it. Looks only: nothing here is read by the server.
 *
 * <p>Cost, per player per frame (rule 11): the mesh's quads once (at most {@link BeastTailRules#MAX_QUADS}, body and glow together; the biggest is the Kraken's 12 tentacles) and its glow pass only for a player within
 * the form's standard level; the far copy of the file (Blender's Decimate, about a third to a half of the quads) from 40 blocks; nothing beyond 48 blocks (the aura's own cull) and at most
 * {@link BeastTailRules#MAX_PLAYERS_PER_FRAME} players per frame (the caller counts; any further player keeps the cube tail). Nothing is allocated per frame: the matrices, the quaternion and the angle array
 * are static scratch. A missing or broken file answers {@link #usable} false once, and the cube tail is drawn instead. Render thread only.
 */
public final class BeastWillTails {
    private BeastWillTails() {}

    private static final PerfProbe.Section SECTION = PerfProbe.section("c.beastwill.tails");
    private static final Quaternionf FLIP = new Quaternionf().rotationX((float) Math.PI);       // the layer's frame (y down, front -z) to the mesh's (y up, front +z)
    private static final Quaternionf TURN = new Quaternionf();
    private static final Map<String, BossMeshDraw> MESHES = new HashMap<>();
    private static final Rig RIG = new Rig();

    /** A resource reload reads the files again (called by {@code GeoModels.clear}). */
    public static void reload() { MESHES.clear(); }

    private static BossMeshDraw mesh(String stem) {
        BossMeshDraw m = MESHES.get(stem);
        if (m == null && BeastTailRules.of(stem) != null) MESHES.put(stem, m = new BossMeshDraw(BeastTailRules.meshId(stem)));
        return m;
    }

    /** True when this form has a mesh tail and its file was read (reads it on the first call). */
    public static boolean usable(String stem) {
        BossMeshDraw m = mesh(stem);
        return m != null && m.available();
    }

    /** Draws the tail of {@code stem} on the player of {@code c}; the pose stack is left as it was. {@code glow} 0..1.5 is the form's glow level this frame. */
    public static void paint(AuraContext c, String stem, NoelleFormRules.Lod lod, int rgb, float glow) {
        BeastTailRules.Tail t = BeastTailRules.of(stem);
        BossMeshDraw m = mesh(stem);
        if (t == null || m == null || !m.available() || lod == NoelleFormRules.Lod.NONE) return;
        float a = EquipAnimation.assembled(c, 6f);                                             // forged in and shattered out like the cube tail it replaces
        if (a == 0f) return;
        long t0 = PerfProbe.begin();
        try {
            ModelPart body = c.model().body;
            PartPose rest = body.getInitialPose();
            PoseStack pose = c.pose();
            pose.pushPose();
            pose.translate(body.x / 16f, body.y / 16f, body.z / 16f);                          // the body's own pose (a crouch, a lean): the same chain as NoelleFormRig.draw
            if (body.xRot != 0f || body.yRot != 0f || body.zRot != 0f) pose.mulPose(TURN.rotationZYX(body.zRot, body.yRot, body.xRot));
            pose.translate(-rest.x / 16f, -rest.y / 16f, -rest.z / 16f);
            pose.translate(0f, 24f / 16f, 0f);                                                 // the feet are 24 px below the neck line
            pose.mulPose(FLIP);
            RIG.kind = t.kind();
            RIG.seconds = c.age() / 20f;
            RIG.alpha = EquipAnimation.alpha(a);
            boolean near = lod != NoelleFormRules.Lod.FAR;
            m.drawParts(pose, c.buffers(), c.light(), 0xFFFFFF, near ? rgb & 0xFFFFFF : 0, near ? glow : 0f, NoelleFormRig.dist2(c), RIG);
            pose.popPose();
        } finally {
            PerfProbe.end(SECTION, t0);
        }
    }

    /** Turns every chain part about its joint, on top of the part before it in its chain (parts arrive in file order, segments of one chain in order). Scratch only. */
    private static final class Rig implements BossMeshDraw.Placer {
        int kind;
        float seconds, alpha;
        final Matrix4f[] chainM = new Matrix4f[BeastTailRules.MAX_CHAIN];
        final Matrix3f[] chainN = new Matrix3f[BeastTailRules.MAX_CHAIN];
        final float[] ang = new float[3];

        Rig() {
            for (int i = 0; i < chainM.length; i++) {
                chainM[i] = new Matrix4f();
                chainN[i] = new Matrix3f();
            }
        }

        @Override
        public float place(int role, float scale, float px, float py, float pz, Matrix4f base, Matrix3f baseN, Matrix4f out, Matrix3f outN) {
            if (!BeastTailRules.isRole(role)) {
                out.set(base);
                outN.set(baseN);
                return alpha;
            }
            int chain = BeastTailRules.chainOf(role), seg = BeastTailRules.segOf(role);
            out.set(seg == 0 ? base : chainM[chain]);
            outN.set(seg == 0 ? baseN : chainN[chain]);
            BeastTailRules.angles(kind, chain, seg, seconds, ang);
            TURN.rotationZYX(ang[2], ang[1], ang[0]);                                           // x first, then y, then z, as the Blender previews
            out.translate(px, py, pz).rotate(TURN).translate(-px, -py, -pz);
            outN.rotate(TURN);
            chainM[chain].set(out);
            chainN[chain].set(outN);
            return alpha;
        }
    }
}
