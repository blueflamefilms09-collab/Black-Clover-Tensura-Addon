package com.newuniverse.nusmp.client.playermodel;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: the extras of a look (horns, wings, tails, a halo, capes, floating weapons) as a render layer: {@link ExtrasDraw} draws each GeoLite part on the model part it is anchored to, in the
 * clip the entity's state asks for ({@link ExtraClip}: idle, walk, run, attack, cast, cast_big, transform, death), with the frame's glow level on the glow maps. A one-shot clip runs on the
 * frame's progress (the swing, the cast, the transformation, the death), a looping one on the entity's clock.
 */
public class ExtrasLayer<T extends LivingEntity> extends RenderLayer<T, PlayerModel<T>> {
    private final PlayerModelRenderer.Body<T> owner;

    public ExtrasLayer(PlayerModelRenderer.Body<T> owner) {
        super(owner);
        this.owner = owner;
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T e, float limbSwing, float limbSwingAmount, float partial, float age, float netHeadYaw, float headPitch) {
        PlayerLook look = owner.look();
        if (look == null || look.extras().isEmpty() || e.isInvisible()) return;
        PlayerBody.Dynamic d = owner.dynamic();
        boolean dead = e.isDeadOrDying();
        float speed = (float) Math.hypot(e.getX() - e.xo, e.getZ() - e.zo);
        float progress = dead ? Math.min(1f, (e.deathTime + partial) / 20f) : d.transforming || d.cast > 0 ? d.progress : e.getAttackAnim(partial);
        float opacity = look.alpha() * d.alpha;
        int argb = opacity < 0.999f ? ((Math.round(opacity * 255f)) << 24) | 0xFFFFFF : 0xFFFFFFFF;
        int rgb = d.glowRgb >= 0 ? d.glowRgb : look.glowRgb();
        float glow = PlayerLod.glow(owner.lod()) ? d.glowLevel : 0f;                  // far away the additive pass of every extra is dropped
        ExtrasDraw.draw(pose, buffers, owner.kit(), look, dead, d.transforming, d.cast, e.swinging, speed, age / 20f + (e.getId() & 15) * 0.37f, progress, light,
                LivingEntityRenderer.getOverlayCoords(e, 0f), argb, glow, rgb);
    }
}
