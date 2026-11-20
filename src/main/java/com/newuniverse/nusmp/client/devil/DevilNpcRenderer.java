package com.newuniverse.nusmp.client.devil;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.client.bossmesh.BossMeshDraw;
import com.newuniverse.nusmp.client.bossmesh.MeshPose;
import com.newuniverse.nusmp.devil.DevilFormRules;
import com.newuniverse.nusmp.devil.DevilNpc;
import com.newuniverse.nusmp.devil.DevilNpcRules;
import com.newuniverse.nusmp.client.playermodel.DevilExtras;
import com.newuniverse.nusmp.client.playermodel.PlayerAssets;
import com.newuniverse.nusmp.client.playermodel.PlayerLod;
import com.newuniverse.nusmp.client.playermodel.PlayerLook;
import com.newuniverse.nusmp.client.playermodel.PlayerModelOptions;
import com.newuniverse.nusmp.client.playermodel.PlayerModelRenderer;
import com.newuniverse.nusmp.client.prop.PropDraw;
import com.newuniverse.nusmp.devil.WorldDevil;
import com.newuniverse.nusmp.perf.PerfProbe;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Arrays;

/**
 * Why: draws the figure of the devil that stands in front of the caller during the ritual (docs/devil_forms.md): the devil's Blender mesh (entity_meshes/devil_&lt;id&gt;.txt, drawn by {@link BossMeshDraw}) with
 * its idle breathing, the rise out of the circle when it appears (the RISE action, sunk and fading in), the roar of the offer (the ROAR action for the first two seconds of the offer), the merge into the signer
 * (it shrinks, fades and slides into the owner's body) and the dissolve (the DEATH action and a fade). A glow pass in the devil's colour pulses over it. When the devil's mesh file is not there (a devil
 * whose assets are not made yet) it draws the old generic devil: a plain humanoid body with the horns and the wings of {@link DevilModels}.
 *
 * <p>docs/devil_looks.md: while the client option {@code bossLooks.devilPlayerModel} is on (default) every devil draws as its anime player-model look instead: the player model with the devil's painted 256 skin,
 * its glow map, and the cube hair, horns, wings, tail, bracers and glowing gems of its look ({@link DevilExtras}); a slow breath, the arms raised for the roar. Off = exactly the figure described above. Added
 * cost per figure per frame: the look's quads once (at most 400 including hair, the fine layer only within the detail distance, no additive pass beyond the far distance) and one additive skin pass.
 *
 * <p>Canon figure: while the client option {@code bossLooks.devilRitualFigure} is on (default) this class only hands the drawing to {@link DevilNpcGeoRenderer} (the full-size canon devil of the trial, rank
 * scale kept, wings and horns bones of its body, risen out of the circle); everything described above is the earlier figure, kept behind the option. Both player models are now adult-proportioned ({@code young = false}):
 * the game's player model starts as a child, which was the baby-sized body under adult-sized wings the owner saw.
 *
 * <p>Cost per figure per frame (rule 11): the mesh's quads once (at most {@link DevilFormRules#MAX_QUADS}, the low-detail copy beyond 40 blocks, no glow beyond 48), two draw batches; nothing beyond 64 blocks and
 * no frustum test (at most {@code npc.max} figures, default 8, exist at all). One small rotation object per figure per frame (the game's own {@code Axis} helper), nothing else. Not seen in a running game by its author.
 */
public final class DevilNpcRenderer extends EntityRenderer<DevilNpc> {
    private static final PerfProbe.Section SECTION = PerfProbe.section("c.devil.npc");
    private static final double CULL = 64.0;
    private static final ResourceLocation STEVE = ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");
    private static final ResourceLocation HORNS_TEX = ResourceLocation.fromNamespaceAndPath("anime_multiverse", "textures/entity/devil/horns.png");
    private static final ResourceLocation WINGS_TEX = ResourceLocation.fromNamespaceAndPath("anime_multiverse", "textures/entity/devil/wings.png");
    private static final ResourceLocation HORNS2_TEX = ResourceLocation.fromNamespaceAndPath("anime_multiverse", "textures/entity/devil/horns2.png");   // 4.40.0 (devil.classicLook = false)
    private static final ResourceLocation WINGS2_TEX = ResourceLocation.fromNamespaceAndPath("anime_multiverse", "textures/entity/devil/wings2.png");

    /** One mesh drawer per devil, made on first use (the mesh file is read then). */
    private final BossMeshDraw[] meshes = new BossMeshDraw[WorldDevil.count()];
    private final PlayerModel<LivingEntity> body;
    private final PlayerModel<LivingEntity> slimBody;
    private final float[] alphas = new float[DevilExtras.ANCHORS];
    private final DevilModels.Horns horns, hornsSwept;
    private final DevilModels.Wings wings;
    /** The summoning figure as the full-size canon devil (docs/devil_trial_model.md, "Canon looks"); this class keeps the earlier figure behind {@code bossLooks.devilRitualFigure}. */
    private final DevilNpcGeoRenderer figure;

    public DevilNpcRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0f;
        this.figure = new DevilNpcGeoRenderer(ctx);
        this.body = new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false);
        this.slimBody = new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        this.body.young = false;                                                               // the game's player model starts as a child: the earlier figure was baby-sized under adult-sized wings and horns
        this.slimBody.young = false;
        this.horns = new DevilModels.Horns(ctx.bakeLayer(DevilModels.HORNS));
        this.hornsSwept = new DevilModels.Horns(ctx.bakeLayer(DevilModels.HORNS_SWEPT));
        this.wings = new DevilModels.Wings(ctx.bakeLayer(DevilModels.WINGS));
    }

    @Override
    public ResourceLocation getTextureLocation(DevilNpc e) { return PlayerModelOptions.devilRitualFigure() ? figure.getTextureLocation(e) : STEVE; }

    /** The figure may be taller than its box: only the distance culls it. */
    @Override
    public boolean shouldRender(DevilNpc e, Frustum frustum, double x, double y, double z) { return e.distanceToSqr(x, y, z) < CULL * CULL; }

    private BossMeshDraw mesh(int index) {
        BossMeshDraw m = meshes[index];
        if (m == null) meshes[index] = m = new BossMeshDraw(DevilFormRules.meshId(WorldDevil.at(index).id()));
        return m;
    }

    @Override
    public void render(DevilNpc e, float entityYaw, float partial, PoseStack ps, MultiBufferSource buffers, int light) {
        if (PlayerModelOptions.devilRitualFigure()) {                                          // the default: the full-size canon devil, wings and horns attached to its body
            figure.render(e, entityYaw, partial, ps, buffers, light);
            return;
        }
        WorldDevil d = e.devil();
        if (d == null) return;
        long t0 = PerfProbe.begin();
        try {
            int state = e.state();
            float t = e.stateAge() + partial, age = e.life() + partial;
            float alpha = 1f, scale = 1f, sink = 0f, at = 0f;
            int action = MeshPose.NONE;
            float ox = 0f, oy = 0f, oz = 0f;
            switch (state) {
                case DevilNpcRules.RISING -> {
                    at = Mth.clamp(t / DevilNpcRules.RISE_TICKS, 0f, 1f);
                    action = MeshPose.RISE;
                    alpha = Mth.clamp(at * 2f, 0f, 1f);
                    sink = (1f - at) * 0.9f;
                }
                case DevilNpcRules.OFFER -> {
                    if (t < DevilNpcRules.ROAR_TICKS) {
                        action = MeshPose.ROAR;
                        at = t / DevilNpcRules.ROAR_TICKS;
                    }
                }
                case DevilNpcRules.MERGE -> {
                    float p = Mth.clamp(t / DevilNpcRules.MERGE_TICKS, 0f, 1f);
                    alpha = 1f - p;
                    scale = 1f - 0.6f * p;
                    Player owner = e.owner() == null ? null : e.level().getPlayerByUUID(e.owner());
                    if (owner != null) {                                                     // slides into the signer's chest
                        float k = p * p;
                        ox = (float) (Mth.lerp(partial, owner.xo, owner.getX()) - Mth.lerp(partial, e.xo, e.getX())) * k;
                        oy = (float) (Mth.lerp(partial, owner.yo, owner.getY()) + 0.9 - Mth.lerp(partial, e.yo, e.getY())) * k;
                        oz = (float) (Mth.lerp(partial, owner.zo, owner.getZ()) - Mth.lerp(partial, e.zo, e.getZ())) * k;
                    }
                }
                case DevilNpcRules.DISSOLVE -> {
                    at = Mth.clamp(t / DevilNpcRules.DISSOLVE_TICKS, 0f, 1f);
                    action = MeshPose.DEATH;
                    alpha = 1f - at;
                }
                default -> { }
            }
            if (alpha <= 0.01f) return;
            int index = WorldDevil.indexOf(d.id());
            double dist2 = entityRenderDispatcher.distanceToSqr(e);
            float yaw = Mth.rotLerp(partial, e.yRotO, e.getYRot());
            int rgb = d.color() & 0xFFFFFF;
            ps.pushPose();
            ps.translate(ox, oy - sink, oz);
            ps.mulPose(Axis.YP.rotationDegrees(-yaw));
            if (scale != 1f) ps.scale(scale, scale, scale);
            int a255 = Mth.clamp((int) (alpha * 255f + 0.5f), 0, 255);
            PlayerLook look = DevilExtras.lookFor(d.id());
            BossMeshDraw mesh = look != null || index < 0 ? null : mesh(index);
            if (look != null) {
                drawLook(ps, buffers, light, look, a255, rgb, age, action, at, dist2);
            } else if (mesh != null && mesh.available()) {
                mesh.pose.solve(age, 0f, 0f, action, at);
                float pulse = 0.75f + 0.25f * Mth.sin(age * 0.15f);
                int glow = (Mth.clamp((int) (alpha * pulse * 255f), 0, 255) << 24) | rgb;
                mesh.draw(ps, buffers, light, (a255 << 24) | 0xFFFFFF, glow, dist2);
            } else {
                fallback(ps, buffers, light, a255, rgb, age);
            }
            ps.popPose();
        } finally {
            PerfProbe.end(SECTION, t0);
        }
    }

    /** The devil's player-model look (docs/devil_looks.md): the painted skin on the player model, its glow map, then the look's cube parts. Nothing is allocated; the pose is set on the shared model. */
    private void drawLook(PoseStack ps, MultiBufferSource buffers, int light, PlayerLook look, int a255, int rgb, float age, int action, float at, double dist2) {
        PlayerModel<LivingEntity> m = look.slim() ? slimBody : body;
        float roar = action == MeshPose.ROAR ? Mth.sin(at * Mth.PI) : 0f;
        float sway = Mth.sin(age * 0.07f);
        m.head.xRot = -0.3f * roar;
        m.head.yRot = Mth.sin(age * 0.03f) * 0.12f * (1f - roar);
        m.head.zRot = 0f;
        m.body.xRot = 0f;
        m.body.yRot = 0f;
        m.body.zRot = 0f;
        m.rightArm.xRot = -2.2f * roar;
        m.leftArm.xRot = -2.2f * roar;
        m.rightArm.yRot = 0f;
        m.leftArm.yRot = 0f;
        m.rightArm.zRot = 0.07f + 0.03f * sway;
        m.leftArm.zRot = -0.07f - 0.03f * sway;
        m.rightLeg.xRot = 0f;
        m.leftLeg.xRot = 0f;
        m.hat.copyFrom(m.head);
        m.jacket.copyFrom(m.body);
        m.rightSleeve.copyFrom(m.rightArm);
        m.leftSleeve.copyFrom(m.leftArm);
        m.rightPants.copyFrom(m.rightLeg);
        m.leftPants.copyFrom(m.leftLeg);
        int lod = PlayerLod.level(dist2, PlayerModelOptions.detailSqr(), PlayerModelOptions.farSqr());
        boolean outer = PlayerLod.outerLayer(lod);
        m.hat.visible = outer;
        m.jacket.visible = outer;
        m.rightSleeve.visible = outer;
        m.leftSleeve.visible = outer;
        m.rightPants.visible = outer;
        m.leftPants.visible = outer;
        ResourceLocation skin = PlayerAssets.skin(look.skin());
        float alpha = a255 / 255f;
        float pulse = 0.75f + 0.25f * Mth.sin(age * 0.15f);
        float s = PlayerModelRenderer.VANILLA_SCALE * look.scale();
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(DevilNpcRules.PLAYER_MODEL_TURN));                  // 4.40.0: the player model's front is -z, the figure's turn assumes +z (the living renderer's 180 - yaw): without this it faced away
        ps.scale(-1f, -1f, 1f);
        ps.scale(s, s, s);
        ps.translate(0f, -1.501f, 0f);
        RenderType rt = a255 < 250 ? RenderType.entityTranslucent(skin) : RenderType.entityCutoutNoCull(skin);
        m.renderToBuffer(ps, buffers.getBuffer(rt), light, OverlayTexture.NO_OVERLAY, (a255 << 24) | 0xFFFFFF);
        boolean glowOn = PlayerLod.glow(lod);
        if (glowOn && look.glow()) {
            ResourceLocation g = PlayerAssets.glowOf(look.skin());
            if (PlayerAssets.exists(g))
                m.renderToBuffer(ps, buffers.getBuffer(AuraRender.additive(g)), PropDraw.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, (Mth.clamp((int) (alpha * pulse * 255f), 0, 255) << 24) | rgb);
        }
        Arrays.fill(alphas, alpha);
        DevilExtras.draw(ps, buffers, m, look, alphas, true, false, false, 0f, age / 20f, 0f, light, OverlayTexture.NO_OVERLAY, glowOn ? pulse : 0f, rgb, lod);
        ps.popPose();
        m.hat.visible = true;                                                                 // the generic figure shares this model: leave it as it was
        m.jacket.visible = true;
        m.rightSleeve.visible = true;
        m.leftSleeve.visible = true;
        m.rightPants.visible = true;
        m.leftPants.visible = true;
    }

    /** The old generic devil (a plain humanoid with horns and wings) for a devil whose mesh is not there. */
    private void fallback(PoseStack ps, MultiBufferSource buffers, int light, int a255, int rgb, float age) {
        int argb = (a255 << 24) | 0xFFFFFF;
        int tint = (a255 << 24) | rgb;
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(DevilNpcRules.PLAYER_MODEL_TURN));                  // 4.40.0: the same turn for the generic figure (also a player model)
        ps.scale(-1f, -1f, 1f);
        ps.translate(0f, -1.501f, 0f);
        body.renderToBuffer(ps, buffers.getBuffer(RenderType.entityTranslucent(STEVE)), light, OverlayTexture.NO_OVERLAY, tint);
        ps.pushPose();
        body.head.translateAndRotate(ps);
        ps.translate(0f, -0.5f, 0f);
        ps.scale(1.1f, 1.1f, 1.1f);
        ps.translate(0f, 0.5f, 0f);
        boolean classic = DevilClientConfig.classicLook();
        (classic ? horns : hornsSwept).render(ps, buffers.getBuffer(RenderType.entityTranslucent(classic ? HORNS_TEX : HORNS2_TEX)), light, OverlayTexture.NO_OVERLAY, argb);
        ps.popPose();
        ps.pushPose();
        body.body.translateAndRotate(ps);
        wings.pose(1f, Mth.sin(age * 0.07f) * 0.12f, 0.75f);
        wings.render(ps, buffers.getBuffer(RenderType.entityTranslucent(classic ? WINGS_TEX : WINGS2_TEX)), light, OverlayTexture.NO_OVERLAY, argb);
        ps.popPose();
        ps.popPose();
    }
}
