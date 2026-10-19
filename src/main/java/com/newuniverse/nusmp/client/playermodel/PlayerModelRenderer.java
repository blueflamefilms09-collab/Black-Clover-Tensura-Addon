package com.newuniverse.nusmp.client.playermodel;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Why: THE renderer of the player-model kit. Register it for any living entity that is shaped like a person and give it a {@link PlayerBody}: it draws the vanilla player model (wide or slim
 * arms, the 64x64 skin with its jacket, sleeves, pants and hat layers) with every vanilla animation, the vanilla armour, held-item, head-item and elytra layers, then the hair
 * ({@link HairLayer}), the extras ({@link ExtrasLayer}: GeoLite parts anchored on the model), the cape ({@link KitCapeLayer}) and the glow pass ({@link GlowLayer}). Which look the entity wears,
 * how it is faded, tinted and posed, is the body's business; none of it needs a renderer class of its own.
 *
 * <p>A model has one arm width, and an entity type can wear both (the arcs' NPCs are one type), so this class is a thin dispatcher: it owns a wide and a slim {@link Body}, the real
 * renderers, and hands every call to the one the entity's look names. The kit scales the model by 15/16 like the vanilla player renderer, times the look's scale and the frame's size.
 *
 * <p>Public API of the kit (small and stable): {@link PlayerModelRenderer#PlayerModelRenderer(EntityRendererProvider.Context, PlayerBody)}, {@link PlayerBody}, {@link PlayerLook},
 * {@link PoseState}/{@link PosePatterns}, {@link HairStyleData}/{@link HairDraw} for a screen that draws hair itself, {@link PlayerAssets}, and the registration helper
 * {@link PlayerModelRegistry}. Everything else is internal.
 */
public class PlayerModelRenderer<T extends LivingEntity> extends EntityRenderer<T> {
    /** The scale vanilla's player renderer draws the model with. */
    public static final float VANILLA_SCALE = 0.9375f;

    private final PlayerBody<T> body;
    private final Body<T> wide, slim;

    public PlayerModelRenderer(EntityRendererProvider.Context ctx, PlayerBody<T> body) {
        super(ctx);
        this.body = body;
        this.wide = new Body<>(ctx, body, false);
        this.slim = new Body<>(ctx, body, true);
    }

    private Body<T> pick(T e) { return body.look(e).slim() ? slim : wide; }

    @Override
    public ResourceLocation getTextureLocation(T e) { return pick(e).getTextureLocation(e); }

    @Override
    public boolean shouldRender(T e, Frustum frustum, double x, double y, double z) { return pick(e).shouldRender(e, frustum, x, y, z); }

    @Override
    public Vec3 getRenderOffset(T e, float partial) { return pick(e).getRenderOffset(e, partial); }

    @Override
    public void render(T e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) { pick(e).render(e, yaw, partial, pose, buffers, light); }

    /** The renderer that really draws one arm width. */
    public static class Body<T extends LivingEntity> extends LivingEntityRenderer<T, PlayerModel<T>> {
        private final PlayerBody<T> body;
        private final KitPlayerModel<T> kit;
        private final PlayerBody.Dynamic dyn = new PlayerBody.Dynamic();
        private PlayerLook look;

        @SuppressWarnings("unchecked")
        Body(EntityRendererProvider.Context ctx, PlayerBody<T> body, boolean slim) {
            super(ctx, new KitPlayerModel<T>(ctx.bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), slim), 0.45f);
            this.body = body;
            this.kit = (KitPlayerModel<T>) this.model;
            addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(ctx.bakeLayer(slim ? ModelLayers.PLAYER_SLIM_INNER_ARMOR : ModelLayers.PLAYER_INNER_ARMOR)),
                    new HumanoidModel<>(ctx.bakeLayer(slim ? ModelLayers.PLAYER_SLIM_OUTER_ARMOR : ModelLayers.PLAYER_OUTER_ARMOR)), ctx.getModelManager()));
            addLayer(new ItemInHandLayer<>(this, ctx.getItemInHandRenderer()));
            addLayer(new CustomHeadLayer<>(this, ctx.getModelSet(), ctx.getItemInHandRenderer()));
            addLayer(new ElytraLayer<>(this, ctx.getModelSet()));
            addLayer(new HairLayer<>(this));
            addLayer(new ExtrasLayer<>(this));
            addLayer(new KitCapeLayer<>(this));
            addLayer(new KitRelicLayer<>(this));                                                          // the relics the entity wears (Curios, armour relics, a mob's pack)
            addLayer(new GlowLayer<>(this));
            body.layers(this, ctx);
        }

        /** The look of the frame being drawn (valid inside layers). */
        public PlayerLook look() { return look; }

        /** The per-frame state of the frame being drawn. */
        public PlayerBody.Dynamic dynamic() { return dyn; }

        /** The model, with its pose state. */
        public KitPlayerModel<T> kit() { return kit; }

        public PlayerBody<T> playerBody() { return body; }

        @Override
        public ResourceLocation getTextureLocation(T e) {
            ResourceLocation composed = body.skin(e);
            return composed != null ? composed : PlayerAssets.skin(body.look(e).skin());
        }

        @Override
        public boolean shouldRender(T e, Frustum frustum, double x, double y, double z) {
            if (!e.shouldRender(x, y, z)) return false;
            if (e.noCulling) return true;
            float g = body.cullGrow(e, body.look(e));
            AABB b = e.getBoundingBox();
            return frustum.isVisible(new AABB(b.minX - g, b.minY - 0.3, b.minZ - g, b.maxX + g, b.maxY + g, b.maxZ + g));
        }

        @Override
        protected RenderType getRenderType(T e, boolean bodyVisible, boolean translucent, boolean glowing) {
            if (bodyVisible && opacity() < 0.999f) return RenderType.entityTranslucent(getTextureLocation(e));
            return super.getRenderType(e, bodyVisible, translucent, glowing);
        }

        private float opacity() { return (look == null ? 1f : look.alpha()) * dyn.alpha; }

        @Override
        protected void scale(T e, PoseStack pose, float partial) {
            float s = VANILLA_SCALE * (look == null ? 1f : look.scale()) * dyn.size;
            if (dyn.squash != null) pose.scale(s * dyn.squash[0], s * dyn.squash[1], s * dyn.squash[2]);
            else pose.scale(s, s, s);
        }

        @Override
        protected float getFlipDegrees(T e) { return dyn.keepUpright ? 0f : super.getFlipDegrees(e); }

        @Override
        protected boolean shouldShowName(T e) { return body.showName(e, super.shouldShowName(e), this.entityRenderDispatcher.distanceToSqr(e)); }

        @Override
        protected void renderNameTag(T e, Component name, PoseStack pose, MultiBufferSource buffers, int light, float partial) {
            super.renderNameTag(e, body.name(e, name), pose, buffers, light, partial);
        }

        @Override
        public void render(T e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
            this.look = body.look(e);
            if (!body.visible(e)) {
                if (shouldShowName(e)) renderNameTag(e, e.getDisplayName(), pose, buffers, light, partial);
                return;
            }
            dyn.reset();
            body.dynamic(e, partial, dyn);
            PoseState ps = kit.state;
            ps.clear();
            if (look.holdR() != 0f) ps.add(PoseState.ARM_R, look.holdR(), 0f, 0f);
            if (look.holdL() != 0f) ps.add(PoseState.ARM_L, look.holdL(), 0f, 0f);
            body.pose(e, partial, ps);
            ArmPoses.apply(e, kit);
            float opacity = opacity();
            this.shadowRadius = opacity < 0.5f ? 0f : (dyn.shadow > 0f ? dyn.shadow : 0.42f * look.scale() * dyn.size);
            int argb = ((Math.max(0, Math.min(255, Math.round(opacity * 255f)))) << 24) | (dyn.tint & 0xFFFFFF);
            MultiBufferSource source = argb == 0xFFFFFFFF ? buffers : new TintedBuffers(buffers, argb);
            pose.pushPose();
            if (dyn.lift != 0f || ps.lift != 0f) pose.translate(0.0, dyn.lift + ps.lift, 0.0);
            super.render(e, yaw, partial, pose, source, light);
            pose.popPose();
            pose.pushPose();
            body.decorate(e, partial, new PlayerBody.Decoration() {
                @Override public PoseStack pose() { return pose; }
                @Override public MultiBufferSource buffers() { return buffers; }
                @Override public int light() { return light; }
            });
            pose.popPose();
        }

        /** The ARGB the frame multiplies into hair and extras (opacity and tint). */
        public int frameArgb() {
            return ((Math.max(0, Math.min(255, Math.round(opacity() * 255f)))) << 24) | (dyn.tint & 0xFFFFFF);
        }
    }
}
