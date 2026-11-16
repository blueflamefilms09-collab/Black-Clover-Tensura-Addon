package com.newuniverse.nusmp.client.devil;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.bossmesh.BossMeshDraw;
import com.newuniverse.nusmp.client.bossmesh.MeshPose;
import com.newuniverse.nusmp.devil.DevilFormRules;
import com.newuniverse.nusmp.devil.DevilNpc;
import com.newuniverse.nusmp.devil.DevilNpcRules;
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

/**
 * Why: draws the figure of the devil that stands in front of the caller during the ritual (docs/devil_forms.md): the devil's Blender mesh (entity_meshes/devil_&lt;id&gt;.txt, drawn by {@link BossMeshDraw}) with
 * its idle breathing, the rise out of the circle when it appears (the RISE action, sunk and fading in), the roar of the offer (the ROAR action for the first two seconds of the offer), the merge into the signer
 * (it shrinks, fades and slides into the owner's body) and the dissolve (the DEATH action and a fade). A glow pass in the devil's colour pulses over it. When the devil's mesh file is not there (a devil
 * whose assets are not made yet) it draws the old generic devil: a plain humanoid body with the horns and the wings of {@link DevilModels}.
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

    /** One mesh drawer per devil, made on first use (the mesh file is read then). */
    private final BossMeshDraw[] meshes = new BossMeshDraw[WorldDevil.count()];
    private final PlayerModel<LivingEntity> body;
    private final DevilModels.Horns horns;
    private final DevilModels.Wings wings;

    public DevilNpcRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0f;
        this.body = new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false);
        this.horns = new DevilModels.Horns(ctx.bakeLayer(DevilModels.HORNS));
        this.wings = new DevilModels.Wings(ctx.bakeLayer(DevilModels.WINGS));
    }

    @Override
    public ResourceLocation getTextureLocation(DevilNpc e) { return STEVE; }

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
            BossMeshDraw mesh = index < 0 ? null : mesh(index);
            if (mesh != null && mesh.available()) {
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

    /** The old generic devil (a plain humanoid with horns and wings) for a devil whose mesh is not there. */
    private void fallback(PoseStack ps, MultiBufferSource buffers, int light, int a255, int rgb, float age) {
        int argb = (a255 << 24) | 0xFFFFFF;
        int tint = (a255 << 24) | rgb;
        ps.pushPose();
        ps.scale(-1f, -1f, 1f);
        ps.translate(0f, -1.501f, 0f);
        body.renderToBuffer(ps, buffers.getBuffer(RenderType.entityTranslucent(STEVE)), light, OverlayTexture.NO_OVERLAY, tint);
        ps.pushPose();
        body.head.translateAndRotate(ps);
        ps.translate(0f, -0.5f, 0f);
        ps.scale(1.1f, 1.1f, 1.1f);
        ps.translate(0f, 0.5f, 0f);
        horns.render(ps, buffers.getBuffer(RenderType.entityTranslucent(HORNS_TEX)), light, OverlayTexture.NO_OVERLAY, argb);
        ps.popPose();
        ps.pushPose();
        body.body.translateAndRotate(ps);
        wings.pose(1f, Mth.sin(age * 0.07f) * 0.12f, 0.75f);
        wings.render(ps, buffers.getBuffer(RenderType.entityTranslucent(WINGS_TEX)), light, OverlayTexture.NO_OVERLAY, argb);
        ps.popPose();
        ps.popPose();
    }
}
