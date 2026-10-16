package com.newuniverse.nusmp.client.avatar;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.avatar.appearance.AppearanceData;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.Mth;

/**
 * Why: the owner's brief asks for a layered feature renderer that attaches hair, horns, wings and clothing pieces from the synced {@code AppearanceData}
 * and reacts to the frenzy flag. The body and the flat clothing are the composed skin; this layer adds the 3D parts (GeoLite models, see
 * {@link AvatarParts}) on top of the posed player model, so they move with the head and the body. The frenzy changes the light, not the models: the
 * eyes and the aura are in {@link AvatarEyesLayer} and the renderer.
 */
public class AvatarFeatureLayer extends RenderLayer<AvatarEntity, PlayerModel<AvatarEntity>> {
    public AvatarFeatureLayer(RenderLayerParent<AvatarEntity, PlayerModel<AvatarEntity>> parent) { super(parent); }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AvatarEntity e, float limbSwing, float limbSwingAmount, float partial, float age,
                       float netHeadYaw, float headPitch) {
        if (e.isInvisible()) return;
        AppearanceData look = AvatarLooks.of(e);
        float seconds = age / 20f + (e.getId() & 15) * 0.37f;
        float flap = e.onGround() ? 1f : 2.2f;
        float pulse = 0.5f + 0.5f * Mth.sin(age * 0.11f + e.getId());
        AvatarParts.draw(pose, buffers, light, getParentModel(), look, seconds, flap, e.frenzied() ? 1f : pulse);
    }
}
