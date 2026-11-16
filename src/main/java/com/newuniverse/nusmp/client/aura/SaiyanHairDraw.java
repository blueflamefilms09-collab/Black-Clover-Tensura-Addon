package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.book.NoelleFormRules;
import com.newuniverse.nusmp.client.bossmesh.BossMeshDraw;
import com.newuniverse.nusmp.perf.PerfProbe;
import com.newuniverse.nusmp.saiyan.SaiyanHair;
import com.newuniverse.nusmp.saiyan.SaiyanLook;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.world.entity.EquipmentSlot;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * Why: the hair of a transformed Saiyan and its 3D tail (docs/saiyan_hair.md), real Blender meshes worn on the player's body. Called by {@link SaiyanAura} from inside the aura layer, so the model is already
 * posed for this frame: the hair is placed on the head part (it turns and nods with the head, crouches with the body) and the tail on the body part, each swaying a little ({@link SaiyanHair#swayX}); the tail's
 * three segments swing one after the other about their own joints. The meshes are drawn with {@link BossMeshDraw#drawParts} like the devil forms; the form picks the shape, its colour is multiplied into the neutral
 * skin and added as a glow ({@link SaiyanHair.Hair}). A missing mesh draws nothing (the old no-hair look) and, for the tail, lets the caller draw the old ribbon.
 *
 * <p>Cost per player per frame (rule 11): the hair mesh once (at most {@link SaiyanHair#MAX_QUADS} quad lines with its glow pass, the far copy about a third of it and no glow beyond 24 blocks) and the tail mesh once
 * (at most {@link SaiyanHair#MAX_TAIL_QUADS}); at most {@link NoelleFormRules#MAX_FULL_PER_FRAME} players at full detail and {@link NoelleFormRules#MAX_FAR_PER_FRAME} at the far one per frame, nothing beyond
 * {@link SaiyanHair#FAR_BLOCKS} blocks, nothing in first person. Nothing is allocated per frame (matrices and the rotation are fields). Section {@code c.saiyan.hair} of the performance probe times it. Render thread only.
 */
final class SaiyanHairDraw {
    private SaiyanHairDraw() {}

    private static final PerfProbe.Section SECTION = PerfProbe.section("c.saiyan.hair");
    private static final Quaternionf FLIP = new Quaternionf().rotationX((float) Math.PI);        // the layer's frame (y down, front -z) to the mesh's (y up, front +z)
    private static final double FULL_D2 = 20.0 * 20.0, FAR_D2 = (BossMeshDraw.LOD_RANGE + 1.0) * (BossMeshDraw.LOD_RANGE + 1.0);   // the squared distance handed to the mesh: inside its full copy and glow / its far copy

    /** One mesh drawer per hair shape and one for the tail, made on first use; reset when the renderers are made again (a resource reload reads the files again). */
    private static BossMeshDraw[] hair = new BossMeshDraw[SaiyanHair.SHAPES.length];
    private static BossMeshDraw tail;
    private static int full, far;
    private static final Rig RIG = new Rig();

    /** Mod bus, client: a resource reload makes the renderers again, so the meshes are read again; the crowd budget starts again every frame. */
    static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.AddLayers e) -> { hair = new BossMeshDraw[SaiyanHair.SHAPES.length]; tail = null; });
        NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent e) -> { if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) { full = 0; far = 0; } });
    }

    private static BossMeshDraw hairMesh(int shape) {
        BossMeshDraw m = hair[shape];
        if (m == null) hair[shape] = m = new BossMeshDraw(SaiyanHair.meshId(SaiyanHair.SHAPES[shape]));
        return m;
    }

    private static BossMeshDraw tailMesh() {
        if (tail == null) tail = new BossMeshDraw(SaiyanHair.TAIL);
        return tail;
    }

    /**
     * Draws the form's hair (setting {@code saiyan.hair}) and tail (setting {@code saiyan.tail3d}, forms with a tail) on the player. {@code d2}: the squared distance to the camera. Returns true when the 3D tail was
     * drawn (the caller then leaves out the old ribbon); false when the setting is off, the player is out of range or in first person, or the mesh is missing (the ribbon is drawn then).
     */
    static boolean draw(AuraContext c, SaiyanLook.Look k, double d2, boolean self1p) {
        SaiyanHair.Hair h = SaiyanHair.of(k.id());
        boolean wantHair = h != null && SaiyanLookClientConfig.hair() && c.player().getItemBySlot(EquipmentSlot.HEAD).isEmpty();      // a helmet covers the hair
        boolean wantTail = k.tail() && SaiyanLookClientConfig.tail3d();
        if (self1p || (!wantHair && !wantTail)) return false;
        NoelleFormRules.Lod lod = NoelleFormRules.budgeted(SaiyanHair.wanted(d2), 0, full, far);
        if (lod == NoelleFormRules.Lod.NONE) return false;
        if (!SaiyanLookClientConfig.fullDetail() && lod == NoelleFormRules.Lod.STANDARD) lod = NoelleFormRules.Lod.FAR;               // the light version everywhere
        float g = SaiyanHair.grow(c.t(), c.left(), SaiyanLookClientConfig.chargeUp());
        if (g <= 0.02f) return false;
        long t0 = PerfProbe.begin();
        try {
            if (lod == NoelleFormRules.Lod.STANDARD) full++;
            else far++;
            double dd = lod == NoelleFormRules.Lod.STANDARD ? FULL_D2 : FAR_D2;
            RIG.model = c.model();
            RIG.age = c.age();
            RIG.run = c.limbSwingAmount();
            RIG.sc = SaiyanHair.scale(g);
            PoseStack pose = c.pose();
            pose.pushPose();
            pose.translate(0f, 1.501f, 0f);
            pose.mulPose(FLIP);
            boolean drew = false;
            if (wantHair) {
                BossMeshDraw m = hairMesh(SaiyanHair.shapeIndex(h.shape()));
                RIG.onBody = false;
                RIG.loose = SaiyanHair.loose(h.shape());
                int light = h.lit() ? LightTexture.pack(Math.max(LightTexture.block(c.light()), 11), LightTexture.sky(c.light())) : c.light();
                m.drawParts(pose, c.buffers(), light, h.tint(), h.glow(), h.glowK() * g, dd, RIG);
            }
            if (wantTail) {
                RIG.onBody = true;
                RIG.loose = 1f;
                drew = tailMesh().drawParts(pose, c.buffers(), c.light(), k.fur(), 0, 0f, dd, RIG);
            }
            pose.popPose();
            return drew;
        } finally {
            PerfProbe.end(SECTION, t0);
        }
    }

    // ================================================================================================ the rig
    /**
     * Places the mesh's parts on the player's body parts: the hair's {@code head} on the head, its {@code tail} (the hanging locks) on the head too, swaying more; the tail's three parts ({@code tail}, {@code wing_l},
     * {@code wing_r}) on the body, each carried by the one before it. One instance for the render thread, one player at a time: its fields are scratch space.
     */
    private static final class Rig implements BossMeshDraw.Placer {
        private static final int ROLE_HEAD = 1, ROLE_WING_L = 8, ROLE_WING_R = 9, ROLE_TAIL = 10;     // the MeshPose roles the mesh files borrow
        PlayerModel<AbstractClientPlayer> model;
        float age, run, sc, loose = 1f;
        boolean onBody;                                                                                     // false: the hair mesh (on the head); true: the tail mesh (on the body)
        final Quaternionf q = new Quaternionf();
        final Matrix4f[] done = {new Matrix4f(), new Matrix4f(), new Matrix4f()};
        final Matrix3f[] doneN = {new Matrix3f(), new Matrix3f(), new Matrix3f()};

        @Override
        public float place(int role, float scale, float px, float py, float pz, Matrix4f base, Matrix3f baseN, Matrix4f out, Matrix3f outN) {
            int seg = role == ROLE_TAIL ? 0 : role == ROLE_WING_L ? 1 : role == ROLE_WING_R ? 2 : -1;            // the tail mesh's segment (0 root, 1 middle, 2 tip)
            if (onBody ? seg < 0 : role != ROLE_HEAD && role != ROLE_TAIL) { out.set(base); outN.set(baseN); return 0f; }       // a role this mesh does not use
            if (onBody && seg > 0) {
                out.set(done[seg - 1]);                                                                       // carried by the segment before it (its sway and the body's pose included)
                outN.set(doneN[seg - 1]);
            } else {
                out.set(base);
                outN.set(baseN);
                ModelPart mp = onBody ? model.body : model.head;
                PartPose rest = mp.getInitialPose();
                float k = 1f / (16f * scale);                                                               // layer pixels to model units
                out.translate((mp.x - rest.x) * k, -(mp.y - rest.y) * k, -(mp.z - rest.z) * k);               // where the body part has moved to (mesh frame: y up, front +z)
                if (mp.xRot != 0f || mp.yRot != 0f || mp.zRot != 0f) {                                       // its turn, about its rest joint, mirrored into the mesh's frame
                    float cx = rest.x * k, cy = (24f - rest.y) * k, cz = -rest.z * k;
                    q.rotationZYX(-mp.zRot, -mp.yRot, mp.xRot);
                    out.translate(cx, cy, cz).rotate(q).translate(-cx, -cy, -cz);
                    outN.rotate(q);
                }
                if (sc != 1f) out.translate(px, py, pz).scale(sc).translate(-px, -py, -pz);                  // the hair grows in at the flash (the tail's root carries the others)
            }
            int slot = onBody ? SaiyanHair.TAIL_ROOT + seg : role == ROLE_HEAD ? SaiyanHair.CROWN : SaiyanHair.LOCKS;
            q.rotationZYX(SaiyanHair.swayZ(slot, age) * loose, 0f, SaiyanHair.swayX(slot, age, run) * loose);
            out.translate(px, py, pz).rotate(q).translate(-px, -py, -pz);
            outN.rotate(q);
            if (onBody) {
                done[seg].set(out);
                doneN[seg].set(outN);
            }
            return 1f;
        }
    }
}
