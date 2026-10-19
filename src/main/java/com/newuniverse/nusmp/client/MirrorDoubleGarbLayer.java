package com.newuniverse.nusmp.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.client.aura.SpatialArmourKit;
import com.newuniverse.nusmp.client.aura.SpatialArmourKit.Style;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.entity.MirrorDoubleEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

/**
 * 1.93.0 Mirror Magic rebuild: the Real Double wears the Glass Garb (the same eight GeoLite pieces as the caster's temporary armour, client.aura.MirrorAura): a crown of
 * mirror panes, the oval mirror breastplate, the pauldron panes, shin panes, the fan of floating mirrors behind it and the ring of shards round its head. The pieces step
 * out of the glass over its first twenty ticks (EquipAnimation, through {@link SpatialArmourKit#pieceOn}) and stay while it lives; when the double shatters, the whole
 * body goes with them. Translucent glass, so it is drawn after the double's own sheen.
 */
public final class MirrorDoubleGarbLayer extends RenderLayer<MirrorDoubleEntity, PlayerModel<MirrorDoubleEntity>> {
    private static final GeoSpec HELM = GeoSpec.of("mirror", "helm"), CHEST = GeoSpec.of("mirror", "chest"), ARM_R = GeoSpec.of("mirror", "arm_r"),
            ARM_L = GeoSpec.of("mirror", "arm_l"), LEG_R = GeoSpec.of("mirror", "leg_r"), LEG_L = GeoSpec.of("mirror", "leg_l"),
            BACK = GeoSpec.of("mirror", "back"), HALO = GeoSpec.of("mirror", "halo");
    private static final Style VIOLET = Style.ghost(0.9f, 0xB89CFF, 0.8f), SILVER = Style.ghost(0.9f, 0xE6EEFF, 0.65f);

    public MirrorDoubleGarbLayer(RenderLayerParent<MirrorDoubleEntity, PlayerModel<MirrorDoubleEntity>> parent) { super(parent); }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, MirrorDoubleEntity e, float limbSwing, float limbSwingAmount, float partial, float age,
                       float netHeadYaw, float headPitch) {
        if (e.isInvisible()) return;
        PlayerModel<MirrorDoubleEntity> m = getParentModel();
        float t = e.tickCount + partial;
        SpatialArmourKit.pieceOn(pose, buffers, light, t, age, m.body, CHEST, 0f, 0f, 0f, -14f, VIOLET);
        SpatialArmourKit.pieceOn(pose, buffers, light, t, age, m.head, HELM, 2f, 0f, -12f, -8f, VIOLET);
        SpatialArmourKit.pieceOn(pose, buffers, light, t, age, m.rightArm, ARM_R, 4f, -12f, 0f, -10f, SILVER);
        SpatialArmourKit.pieceOn(pose, buffers, light, t, age, m.leftArm, ARM_L, 5f, 12f, 0f, -10f, SILVER);
        SpatialArmourKit.pieceOn(pose, buffers, light, t, age, m.rightLeg, LEG_R, 7f, -4f, 10f, -8f, SILVER);
        SpatialArmourKit.pieceOn(pose, buffers, light, t, age, m.leftLeg, LEG_L, 8f, 4f, 10f, -8f, SILVER);
        SpatialArmourKit.pieceOn(pose, buffers, light, t, age, m.body, BACK, 10f, 0f, 0f, 14f, VIOLET);
        SpatialArmourKit.pieceOn(pose, buffers, light, t, age, m.head, HALO, 12f, 0f, -14f, 0f, SILVER);
    }
}
