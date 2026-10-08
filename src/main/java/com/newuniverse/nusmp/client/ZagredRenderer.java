package com.newuniverse.nusmp.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.entity.NUEntities;
import com.newuniverse.nusmp.entity.ZagredBossEntity;
import com.newuniverse.nusmp.item.NUItems;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * 0.48: Zagred's true form (replaces the 0.47 humanoid look; the entity id "zagred" is unchanged).
 * <ul>
 *   <li>{@link ZagredModel}: a tall black devil in cuboids with spread bat wings, horns, claws and a whip tail; textures from
 *       tools/gen_zagred_true_form.py.</li>
 *   <li>Glow pass (full-bright, additive): the red eyes, horn and hair tips, ribcage cracks, rune lines and wing veins, pulsing
 *       faster each phase.</li>
 *   <li>Aura pass: the model again, slightly larger, through the nusmp:rendertype_zagred_aura core shader: a fresnel rim pulsing
 *       blood red to crimson (0.49). If the shader can't load, an additive eyes pass stands in.</li>
 *   <li>Target reticle: while it fights or telegraphs a word, a turning rune ring (and inward marks when casting) is projected at
 *       its target's feet (state from ZagredStatePayload).</li>
 *   <li>Phase 4: the otherworldly trident in its right claws.</li>
 * </ul>
 */
public class ZagredRenderer extends MobRenderer<ZagredBossEntity, ZagredModel> {
    static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/zagred_true.png");
    static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/zagred_true_glow.png");
    static final ResourceLocation RING = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/particle/koto_ring.png");

    private final ItemInHandRenderer items;

    public ZagredRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new ZagredModel(ctx.bakeLayer(ZagredModel.LAYER)), 0.45f);
        this.items = ctx.getItemInHandRenderer();
        addLayer(new Glow(this));
        addLayer(new Aura(this));
        addLayer(new Trident(this));
    }

    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(NUEntities.ZAGRED.get(), ZagredRenderer::new);
    }

    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(ZagredModel.LAYER, ZagredModel::createLayer);
    }

    @Override
    public ResourceLocation getTextureLocation(ZagredBossEntity e) { return TEX; }

    @Override
    protected void scale(ZagredBossEntity entity, PoseStack pose, float partialTick) {
        pose.scale(0.70f, 0.70f, 0.70f);
    }

    @Override
    public void render(ZagredBossEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        super.render(e, yaw, partial, pose, buffers, light);
        int state = e.clientState();
        if (state < ZagredBossEntity.STATE_COMBAT || e.clientTarget() < 0) return;
        Entity t = e.level().getEntity(e.clientTarget());
        if (t == null || !t.isAlive()) return;
        Vec3 off = t.getPosition(partial).subtract(e.getPosition(partial));
        reticle(pose, buffers.getBuffer(RenderType.eyes(RING)), off, t.getBbWidth(), e.tickCount + partial, state == ZagredBossEntity.STATE_CASTING);
    }

    /** A turning rune ring at the target's feet; casting adds four marks closing in and a brighter, faster ring. */
    static void reticle(PoseStack pose, VertexConsumer vc, Vec3 off, float width, float age, boolean casting) {
        pose.pushPose();
        pose.translate(off.x, off.y + 0.06, off.z);
        float r = Math.max(0.8f, width * 1.3f) * (casting ? 1.0f + 0.15f * Mth.sin(age * 0.6f) : 1f);
        pose.mulPose(Axis.YP.rotation(age * (casting ? 0.12f : 0.04f)));
        int k = casting ? 255 : 150;
        flat(pose.last(), vc, r, (int) (k * 1.0), (int) (k * 0.05), (int) (k * 0.35));
        if (casting) {
            pose.mulPose(Axis.YP.rotation(-age * 0.2f));
            flat(pose.last(), vc, r * 0.55f, 220, 10, 30);
        }
        pose.popPose();
    }

    /** A horizontal textured square of half-size r (both faces), full-bright. */
    static void flat(PoseStack.Pose p, VertexConsumer vc, float r, int red, int green, int blue) {
        float[][] c = {{-r, -r, 0, 0}, {r, -r, 1, 0}, {r, r, 1, 1}, {-r, r, 0, 1}};
        for (int side = 0; side < 2; side++)
            for (int i = 0; i < 4; i++) {
                float[] k = c[side == 0 ? i : 3 - i];
                vc.addVertex(p, k[0], 0, k[1]).setColor(red, green, blue, 255).setUv(k[2], k[3]).setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(0xF000F0).setNormal(p, 0, side == 0 ? 1 : -1, 0);
            }
    }

    /** Eyes, horn and hair tips, ribcage cracks, runes and wing veins: full-bright, pulsing faster each phase. */
    static final class Glow extends RenderLayer<ZagredBossEntity, ZagredModel> {
        Glow(ZagredRenderer parent) { super(parent); }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, ZagredBossEntity e, float limbSwing, float limbSwingAmount,
                           float partial, float age, float netHeadYaw, float headPitch) {
            float pulse = 0.65f + 0.35f * Mth.sin(age * (0.08f + 0.04f * e.phase()));
            int v = (int) (255 * pulse);                                    // eyes() is additive: dim by colour, not alpha
            getParentModel().renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(GLOW)), 0xF000F0, OverlayTexture.NO_OVERLAY, 0xFF000000 | (v << 16) | (v << 8) | v);
        }
    }

    /** The fresnel aura: the model drawn again a little larger through the aura shader. */
    static final class Aura extends RenderLayer<ZagredBossEntity, ZagredModel> {
        Aura(ZagredRenderer parent) { super(parent); }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, ZagredBossEntity e, float limbSwing, float limbSwingAmount,
                           float partial, float age, float netHeadYaw, float headPitch) {
            float k = 0.55f + 0.15f * e.phase() + (e.clientState() == ZagredBossEntity.STATE_CASTING ? 0.3f : 0f);
            int a = (int) (255 * Math.min(1f, k));
            boolean shader = NUShaders.zagredAura() != null;
            int color = shader ? (a << 24) | 0xFFFFFF : 0xFF000000 | ((int) (a * 0.3f) << 16) | ((int) (a * 0.02f) << 8) | (int) (a * 0.04f);
            pose.pushPose();
            pose.scale(1.04f, 1.02f, 1.04f);
            getParentModel().renderToBuffer(pose, buffers.getBuffer(NURenderTypes.zagredAura(TEX)), 0xF000F0, OverlayTexture.NO_OVERLAY, color);
            pose.popPose();
        }
    }

    /** Phase 4: the otherworldly trident in its right claws. */
    final class Trident extends RenderLayer<ZagredBossEntity, ZagredModel> {
        private ItemStack stack;
        Trident(ZagredRenderer parent) { super(parent); }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, ZagredBossEntity e, float limbSwing, float limbSwingAmount,
                           float partial, float age, float netHeadYaw, float headPitch) {
            if (e.phase() < 4) return;
            if (stack == null) stack = new ItemStack(NUItems.OTHERWORLD_TRIDENT.get());
            pose.pushPose();
            getParentModel().translateToHand(HumanoidArm.RIGHT, pose);
            pose.mulPose(Axis.XP.rotationDegrees(-90));
            pose.mulPose(Axis.YP.rotationDegrees(180));
            pose.translate(0f, 0.05f, -0.1f);
            items.renderItem(e, stack, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, false, pose, buffers, light);
            pose.popPose();
        }
    }
}
