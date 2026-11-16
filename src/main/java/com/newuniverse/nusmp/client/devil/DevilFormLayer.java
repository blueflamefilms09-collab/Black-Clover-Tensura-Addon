package com.newuniverse.nusmp.client.devil;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.book.NoelleFormRules;
import com.newuniverse.nusmp.client.SafeAdditive;
import com.newuniverse.nusmp.client.aura.EquipAnimation;
import com.newuniverse.nusmp.client.bossmesh.BossMeshDraw;
import com.newuniverse.nusmp.devil.DevilFormRules;
import com.newuniverse.nusmp.devil.WorldDevil;
import com.newuniverse.nusmp.perf.PerfProbe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * Why: the devil's own form on a player (docs/devil_forms.md). When the player's contract is with a devil of another world ({@code DevilNet.Visual.devil}) the parts of that devil's Blender mesh
 * (entity_meshes/devil_&lt;id&gt;.txt, eleven roles) are drawn ON the player's body: head on the head, torso on the body, arm and forearm on the arm they belong to, legs on the legs, wings and tail on the back.
 * Each part is placed by the body part's own pose (so it walks, turns, crouches and swings with the player), the way the Djinn's pieces and Noelle's forms are ({@code NoelleFormRig}). The stages follow the
 * assimilation: the head piece from 20 %, arms / wings / tail from 50 %, the whole form (torso and legs, the glow, the ground ring) at 100 % and in the union. A part forges in and shatters out with the Djinn's
 * own motion ({@link EquipAnimation}). The Weg markings ({@link WegLayer}) stay under it, tinted in the devil's colour. The vanilla body is not hidden: the meshes are made a little larger than the body.
 *
 * <p>Cost per player drawn per frame (rule 11): the mesh's quads once (at most {@link DevilFormRules#MAX_QUADS} counting body and glow; the low-detail copy beyond 40 blocks, no glow there) plus two quads of
 * ground ring, up to three draw batches. Per frame at most {@link NoelleFormRules#MAX_FULL_PER_FRAME} players at the full level of detail and {@link NoelleFormRules#MAX_FAR_PER_FRAME} at the far one ({@link
 * NoelleFormRules#budgeted}), nothing beyond {@link DevilFormRules#FAR_BLOCKS} blocks. Nothing is allocated per frame (matrices and the rotation are fields). Section {@code c.devil.form} of the performance
 * probe times it. Not seen in a running game by its author.
 */
public final class DevilFormLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final PerfProbe.Section SECTION = PerfProbe.section("c.devil.form");
    private static final ResourceLocation[] RING = {tex("glow"), tex("air_ring"), tex("waterfx_ripple"), tex("flamefx_ring"), tex("earthfam_rock_ring")};
    private static final Quaternionf FLIP = new Quaternionf().rotationX((float) Math.PI);       // the layer's frame (y down, front -z) to the mesh's (y up, front +z)

    private static ResourceLocation tex(String name) { return ResourceLocation.fromNamespaceAndPath("anime_multiverse", "textures/particle/" + name + ".png"); }

    /** One mesh drawer per devil, made on first use; a new array each time the layers are added (a resource reload reads the files again). Render thread only. */
    private static BossMeshDraw[] meshes = new BossMeshDraw[WorldDevil.count()];
    private static int full, far;
    private static final Rig RIG = new Rig();

    private static BossMeshDraw mesh(int index) {
        BossMeshDraw m = meshes[index];
        if (m == null) meshes[index] = m = new BossMeshDraw(DevilFormRules.meshId(WorldDevil.at(index).id()));
        return m;
    }

    /** True when the devil at {@code index} has a mesh and the client setting is on: the generic horns and wings are then not drawn ({@link WegLayer}). */
    static boolean usable(int index) { return index >= 0 && index < meshes.length && DevilClientConfig.customForms() && mesh(index).available(); }

    /** Called once a frame (before the world is drawn): the crowd budget starts again. */
    static void newFrame() { full = 0; far = 0; }

    private DevilFormLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) { super(parent); }

    /** Mod bus, client: puts the layer on both player renderers (after the Weg markings). */
    static void add(EntityRenderersEvent.AddLayers e) {
        meshes = new BossMeshDraw[WorldDevil.count()];
        for (PlayerSkin.Model skin : e.getSkins()) {
            if (e.getSkin(skin) instanceof PlayerRenderer r) r.addLayer(new DevilFormLayer(r));
        }
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer p, float limbSwing, float limbSwingAmount,
                       float partial, float age, float netHeadYaw, float headPitch) {
        DevilClient.Vis v = DevilClient.vis(p.getId());
        if (v == null || v.devil < 0 || p.isInvisible() || p.isSpectator() || !DevilClientConfig.weg() || !usable(v.devil)) return;
        Minecraft mc = Minecraft.getInstance();
        boolean self = p == mc.player;
        double d2 = mc.getEntityRenderDispatcher().distanceToSqr(p);
        NoelleFormRules.Lod lod = self ? NoelleFormRules.Lod.STANDARD : NoelleFormRules.budgeted(DevilFormRules.wanted(d2), 0, full, far);
        if (lod == NoelleFormRules.Lod.NONE) return;
        long t0 = PerfProbe.begin();
        try {
            float now = p.level().getGameTime() + partial;
            boolean air = !p.onGround() && !p.isInWater() && !p.onClimbable();
            v.step(now, air);
            if (!RIG.forge(v, now)) return;                                                    // nothing is on and nothing is going out: no draw, no budget used
            if (lod == NoelleFormRules.Lod.STANDARD) full++;
            else far++;
            WorldDevil d = WorldDevil.at(v.devil);
            int rgb = d.color() & 0xFFFFFF;
            float glowK = lod == NoelleFormRules.Lod.STANDARD ? DevilFormRules.glow(v.level, v.union, age) : 0f;
            RIG.model = getParentModel();
            RIG.flap = air ? Mth.sin(age * 0.38f) * 0.45f : Mth.sin(age * 0.07f) * 0.06f;
            RIG.sway = Mth.sin(age * 0.05f) * 0.12f;
            pose.pushPose();
            pose.translate(0f, 1.501f, 0f);
            pose.mulPose(FLIP);
            mesh(v.devil).drawParts(pose, buffers, light, 0xFFFFFF, rgb, glowK, d2, RIG);
            if (lod == NoelleFormRules.Lod.STANDARD && RIG.asm[DevilFormRules.TORSO] >= 1f) ring(pose, buffers, d, age, glowK);
            pose.popPose();
        } finally {
            PerfProbe.end(SECTION, t0);
        }
    }

    /** The attribute's ground ring: a flat additive quad at the feet turning slowly, in the devil's colour (the whole form only). */
    private static void ring(PoseStack pose, MultiBufferSource buffers, WorldDevil d, float age, float glowK) {
        pose.pushPose();
        RIG.q.rotationY(age * 0.02f);
        pose.mulPose(RIG.q);
        VertexConsumer vc = buffers.getBuffer(SafeAdditive.additive(RING[DevilFormRules.ringOf(d.attribute())]));
        PoseStack.Pose pp = pose.last();
        int c = d.color();
        int a = Mth.clamp((int) (0.55f * glowK * 255f), 0, 255), r = c >> 16 & 255, g = c >> 8 & 255, b = c & 255;
        float s = 1.4f, y = 0.03f;
        corner(vc, pp, -s, y, -s, 0f, 0f, r, g, b, a);
        corner(vc, pp, s, y, -s, 1f, 0f, r, g, b, a);
        corner(vc, pp, s, y, s, 1f, 1f, r, g, b, a);
        corner(vc, pp, -s, y, s, 0f, 1f, r, g, b, a);
        corner(vc, pp, -s, y, s, 0f, 1f, r, g, b, a);                                           // the same square the other way round: seen from below too
        corner(vc, pp, s, y, s, 1f, 1f, r, g, b, a);
        corner(vc, pp, s, y, -s, 1f, 0f, r, g, b, a);
        corner(vc, pp, -s, y, -s, 0f, 0f, r, g, b, a);
        pose.popPose();
    }

    private static void corner(VertexConsumer vc, PoseStack.Pose pp, float x, float y, float z, float u, float v, int r, int g, int b, int a) {
        vc.addVertex(pp, x, y, z).setColor(r, g, b, a).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(pp, 0f, 1f, 0f);
    }

    // ================================================================================================ the rig
    /**
     * Places the mesh's parts on the player's body parts and keeps the forge-in / shatter-out state of each role (in the {@link DevilClient.Vis} of the player drawn). One instance, used by the render thread
     * for one player at a time, so its fields are scratch space and nothing is allocated per frame.
     */
    private static final class Rig implements BossMeshDraw.Placer {
        PlayerModel<AbstractClientPlayer> model;
        float flap, sway;
        final float[] asm = new float[DevilFormRules.COUNT];
        final Quaternionf q = new Quaternionf();

        /** Works out how far each role is assembled ({@link EquipAnimation}'s convention); false when no role is on or going out. */
        boolean forge(DevilClient.Vis v, float now) {
            boolean any = false;
            for (int r = 0; r < DevilFormRules.COUNT; r++) {
                boolean w = DevilFormRules.wants(r, v.level, v.union);
                float d = DevilFormRules.delay(r);
                if (!v.formInit) {                                                              // first seen: already worn, no forging in
                    v.was[r] = w;
                    v.onAt[r] = now - 100f;
                    v.offAt[r] = -1f;
                } else if (w != v.was[r]) {
                    v.was[r] = w;
                    if (w) v.onAt[r] = now;
                    else v.offAt[r] = now;
                }
                float a;
                if (w) a = Mth.clamp((now - v.onAt[r] - d) / EquipAnimation.FORGE_TICKS, 0f, 1f);
                else if (v.offAt[r] < 0f) a = 0f;
                else {
                    float s = (now - v.offAt[r] - d * 0.5f) / EquipAnimation.SHATTER_TICKS;
                    a = s >= 1f ? 0f : s > 0f ? -Mth.clamp(s, 0.001f, 1f) : 1f;
                }
                asm[r] = a;
                if (a != 0f) any = true;
            }
            v.formInit = true;
            return any;
        }

        private ModelPart part(int anchor) {
            return switch (anchor) {
                case DevilFormRules.A_HEAD -> model.head;
                case DevilFormRules.A_ARM_R -> model.rightArm;
                case DevilFormRules.A_ARM_L -> model.leftArm;
                case DevilFormRules.A_LEG_R -> model.rightLeg;
                case DevilFormRules.A_LEG_L -> model.leftLeg;
                default -> model.body;
            };
        }

        @Override
        public float place(int role, float scale, float px, float py, float pz, Matrix4f base, Matrix3f baseN, Matrix4f out, Matrix3f outN) {
            out.set(base);
            outN.set(baseN);
            float a = asm[role];
            if (a == 0f) return 0f;
            ModelPart mp = part(DevilFormRules.anchorOf(role));
            PartPose rest = mp.getInitialPose();
            float k = 1f / (16f * scale);                                                       // layer pixels to model units
            float off = EquipAnimation.offset(a);
            if (off != 0f) out.translate(DevilFormRules.fly(role, 0) * off * k, DevilFormRules.fly(role, 1) * off * k, DevilFormRules.fly(role, 2) * off * k);
            out.translate((mp.x - rest.x) * k, -(mp.y - rest.y) * k, -(mp.z - rest.z) * k);    // where the body part has moved to (mesh frame: y up, front +z)
            if (mp.xRot != 0f || mp.yRot != 0f || mp.zRot != 0f) {                              // its turn, about its rest joint, mirrored into the mesh's frame
                float cx = rest.x * k, cy = (24f - rest.y) * k, cz = -rest.z * k;
                q.rotationZYX(-mp.zRot, -mp.yRot, mp.xRot);
                out.translate(cx, cy, cz).rotate(q).translate(-cx, -cy, -cz);
                outN.rotate(q);
            }
            if (role == DevilFormRules.WING_L || role == DevilFormRules.WING_R) {
                q.rotationZ(role == DevilFormRules.WING_L ? flap : -flap);                      // the beat, about the wing's own root
                out.translate(px, py, pz).rotate(q).translate(-px, -py, -pz);
                outN.rotate(q);
            } else if (role == DevilFormRules.TAIL) {
                q.rotationY(sway);
                out.translate(px, py, pz).rotate(q).translate(-px, -py, -pz);
                outN.rotate(q);
            }
            float sc = EquipAnimation.scale(a);
            if (sc != 1f) out.translate(px, py, pz).scale(sc).translate(-px, -py, -pz);
            return EquipAnimation.alpha(a);
        }
    }
}
