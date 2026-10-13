package com.newuniverse.nusmp.client.floor.charybdis;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.client.prop.PropDraw;
import com.newuniverse.nusmp.dungeon.floor.charybdis.MagiculeScaleEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Draws a magicule scale (tools/gen_charybdis.py writes assets/nusmp/geo|textures/entity/magicule_scale*): a small flat glowing plate that points the
 * way it flies and slowly rolls about that line, bright at any depth. A scale that was sent back is drawn in the warm skin, so the player can see which
 * ones are theirs.
 */
public class MagiculeScaleRenderer extends EntityRenderer<MagiculeScaleEntity> {
    private static final GeoSpec NORMAL = GeoSpec.of("magicule", "scale"), BACK = NORMAL.skin("magicule_scale_returned");
    private static final int GLOW_NORMAL = 0xFFFFFFFF, GLOW_BACK = 0xFFFFFFFF;

    public MagiculeScaleRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0f;
    }

    @Override
    public ResourceLocation getTextureLocation(MagiculeScaleEntity e) { return NORMAL.texture(); }

    @Override
    public void render(MagiculeScaleEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        boolean back = e.isReturned();
        float facing = Mth.rotLerp(partial, e.yRotO, e.getYRot()), pitch = Mth.lerp(partial, e.xRotO, e.getXRot());
        float roll = (e.tickCount + partial) * (back ? 30f : 18f);
        pose.pushPose();
        pose.translate(0f, e.getBbHeight() / 2f, 0f);
        pose.mulPose(Axis.YP.rotationDegrees(-facing));
        pose.mulPose(Axis.XP.rotationDegrees(pitch));
        pose.mulPose(Axis.ZP.rotationDegrees(roll));
        GeoDraw.paint(pose, buffers, back ? BACK : NORMAL, null, 0f, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, PropDraw.FULL_BRIGHT, 0xFFFFFFFF, back ? GLOW_BACK : GLOW_NORMAL);
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);
    }
}
