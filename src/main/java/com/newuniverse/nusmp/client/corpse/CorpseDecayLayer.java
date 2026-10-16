package com.newuniverse.nusmp.client.corpse;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.corpse.CorpseDecay;
import com.newuniverse.nusmp.corpse.CorpseEffects;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: a Soul Corpse has to look like one on ANY body (a zombie, a wolf, a player), not only on our own models, and its decay must be
 * readable at a glance. This layer is added to every living-entity renderer; it does nothing unless the entity carries the
 * {@code nusmp:soul_corpse} marker, and then it draws the parent's own model once more with a stitched, patchwork, bone-showing overlay
 * texture, tinted pale green-grey, and (from the second stage on) a faint additive soul-glow pass. The stage (0 to 3) is the effect's
 * amplifier, so the client needs no packet of its own. The overlay textures are patterned, not laid out for one model, so they read as
 * rot on any UV layout (tools/gen_corpse_assets.py draws them; the humanoid 64x64 layout gets extra stitches where a human has joints).
 * One {@link RenderType} per stage is built once and kept; nothing is allocated per frame.
 */
public final class CorpseDecayLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private static final int STAGES = CorpseDecay.MAX_STAGE + 1;
    private static final int TINT = 0xB9C9B8, WISP_TINT = 0x9FD8C8;
    private static final RenderType[] BODY = new RenderType[STAGES];
    private static final RenderType WISP;

    static {
        for (int i = 0; i < STAGES; i++) BODY[i] = RenderType.entityTranslucent(ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/corpse_decay_" + i + ".png"));
        WISP = RenderType.eyes(ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/corpse_soul.png"));
    }

    public CorpseDecayLayer(RenderLayerParent<T, M> parent) { super(parent); }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float limbSwing, float limbSwingAmount, float partial,
                       float age, float headYaw, float headPitch) {
        MobEffectInstance mark = entity.getEffect(CorpseEffects.holder());
        if (mark == null || entity.isInvisible()) return;
        int stage = CorpseDecay.stageOf(mark.getAmplifier());
        int alpha = Mth.clamp(Math.round(CorpseDecay.overlayAlpha(stage) * 255f), 0, 255);
        int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0f);
        VertexConsumer body = buffers.getBuffer(BODY[stage]);
        getParentModel().renderToBuffer(pose, body, light, overlay, (alpha << 24) | TINT);
        if (stage >= 1) {
            float pulse = 0.55f + 0.45f * Mth.sin((entity.tickCount + partial) * 0.12f);
            int glow = Mth.clamp(Math.round((0.18f + 0.1f * stage) * pulse * 255f), 0, 255);
            getParentModel().renderToBuffer(pose, buffers.getBuffer(WISP), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, (glow << 24) | WISP_TINT);
        }
    }
}
