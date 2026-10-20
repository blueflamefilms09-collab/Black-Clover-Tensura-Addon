package com.newuniverse.nusmp.client.playermodel;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

/**
 * Why: the hair system as a render layer. It reads the look's haircut and colour, moves the entity's {@link HairState} on (its springs, from how the entity moved), and draws the style with
 * {@link HairDraw} after the armour, so a helmet is drawn first and the hair is dropped or shortened under it: any item in the head slot hides the cap and the pieces marked {@code hide} and keeps
 * only what hangs out of the helmet. The sheet is drawn alpha-tested (a translucent type while the character fades). Nothing here depends on the entity type.
 */
public class HairLayer<T extends LivingEntity> extends RenderLayer<T, PlayerModel<T>> {
    private final PlayerModelRenderer.Body<T> owner;

    public HairLayer(PlayerModelRenderer.Body<T> owner) {
        super(owner);
        this.owner = owner;
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T e, float limbSwing, float limbSwingAmount, float partial, float age, float netHeadYaw, float headPitch) {
        PlayerLook look = owner.look();
        if (look == null || !look.hasHair() || e.isInvisible() || !PlayerLod.hair(owner.lod())) return;        // far away the hair (a hundred boxes and its springs) is skipped
        HairStyleData.Style style = PlayerAssets.hair(look.hair());
        if (style == null || style.bald()) return;
        KitPlayerModel<T> kit = owner.kit();
        boolean helmet = !e.getItemBySlot(EquipmentSlot.HEAD).isEmpty();
        HairState state = HairState.of(e);
        state.update(e, style, age, kit.head.xRot, kit.head.zRot, kit.body.xRot);
        boolean fading = look.alpha() * owner.dynamic().alpha < 0.999f;
        var sheet = PlayerAssets.hairSheet(style);
        RenderType rt = fading ? RenderType.entityTranslucent(sheet) : RenderType.entityCutoutNoCull(sheet);
        HairDraw.draw(pose, buffers.getBuffer(rt), style, kit.head, kit.body, state, HairPalette.cached(look.hairRgb()), helmet, light, LivingEntityRenderer.getOverlayCoords(e, 0f), 0xFFFFFFFF);
    }
}
