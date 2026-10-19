package com.newuniverse.nusmp.client.playermodel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: vanilla's cape layer is written for {@code AbstractClientPlayer} (it reads the cloak positions only a player has), so a character that wears the vanilla cape model (a texture of the
 * standard 64x32 cape layout, named by {@link PlayerLook#cape()}) needs its own: the same transform as vanilla's (hung 1/8 behind the body, swung by the speed along and across its facing, lifted
 * by a fall, flicked by the walk bob, thrown out when sneaking) with the cloak position replaced by a lag behind the entity's last-tick position, drawn through the model's own
 * {@code renderCloak}. It is hidden while a chest plate or an elytra is worn, as vanilla hides it. Most characters have no cape texture (their capes are extras of the look); this is for the
 * ones that do.
 */
public class KitCapeLayer<T extends LivingEntity> extends RenderLayer<T, PlayerModel<T>> {
    private final PlayerModelRenderer.Body<T> owner;

    public KitCapeLayer(PlayerModelRenderer.Body<T> owner) {
        super(owner);
        this.owner = owner;
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T e, float limbSwing, float limbSwingAmount, float partial, float age, float netHeadYaw, float headPitch) {
        PlayerLook look = owner.look();
        if (look == null || look.cape().isEmpty() || e.isInvisible() || !e.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) return;
        ResourceLocation tex = PlayerAssets.skin(look.cape());
        pose.pushPose();
        pose.translate(0f, 0f, 0.125f);
        double d0 = (e.xo - e.getX()) * 4.0, d1 = (e.yo - e.getY()) * 4.0, d2 = (e.zo - e.getZ()) * 4.0;
        float body = Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot);
        double sin = Mth.sin(body * Mth.DEG_TO_RAD), cos = -Mth.cos(body * Mth.DEG_TO_RAD);
        float lift = Mth.clamp((float) d1 * 10f, -6f, 32f);
        float along = Math.max(0f, Mth.clamp((float) (d0 * sin + d2 * cos) * 100f, 0f, 150f));
        float across = Mth.clamp((float) (d0 * cos - d2 * sin) * 100f, -20f, 20f);
        float bob = e.walkAnimation.speed(partial);
        lift += Mth.sin(e.walkAnimation.position(partial) * 6f) * 32f * bob;
        if (e.isCrouching()) lift += 25f;
        pose.mulPose(Axis.XP.rotationDegrees(6f + along / 2f + lift));
        pose.mulPose(Axis.ZP.rotationDegrees(across / 2f));
        pose.mulPose(Axis.YP.rotationDegrees(180f - across / 2f));
        getParentModel().renderCloak(pose, buffers.getBuffer(RenderType.entitySolid(tex)), light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
