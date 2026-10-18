package com.newuniverse.nusmp.client.remodel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.entity.CottonCloudEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Why: Charmy's cotton cloud is not a living entity (it is a prop of Cotton Magic with four modes), so it cannot use the generic living-mob renderer; this is its own small renderer around the
 * art kit's model (a heap of cotton with a sleepy face at its rim, tools/arc/char_cotton_cloud.py), keeping what the old {@code CottonCloudRenderer} did: a cumulus for the platform and the
 * ride (sized by the entity's own scale, 1 = the 2.6-block cumulus), a cocoon of three stacked heaps that turn round the foe for Sheep Bondage, and a puff that swells and fades where a strike
 * lands; the fade in at the start and out at the end of its life.
 */
public class KitCloudRenderer extends EntityRenderer<CottonCloudEntity> {
    private static final GeoSpec SPEC = GeoSpec.of("arc", "cotton_cloud");

    public KitCloudRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0f;
    }

    @Override
    public ResourceLocation getTextureLocation(CottonCloudEntity e) { return SPEC.texture(); }

    @Override
    public void render(CottonCloudEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        float age = e.tickCount + partial, s = e.cloudScale();
        int mode = e.mode();
        float alpha = 1f;
        if (mode == CottonCloudEntity.BURST) {
            float t = Math.min(1f, (e.life() + partial) / Math.max(1f, e.maxLife()));
            s *= 0.5f + 1.2f * t;
            alpha = 1f - t;
        } else if (e.maxLife() - e.life() < 10) alpha = Math.max(0f, (e.maxLife() - e.life() - partial) / 10f);
        else if (e.life() < 6) alpha = Math.min(1f, (e.life() + partial) / 6f);
        int argb = ((int) (255 * alpha) << 24) | 0xFFFFFF;
        float seconds = age / 20f;
        int heaps = mode == CottonCloudEntity.WRAP ? 3 : 1;
        for (int k = 0; k < heaps; k++) {
            pose.pushPose();
            if (mode == CottonCloudEntity.WRAP) {                                           // a cocoon: three heaps up the body, each turning
                float ws = e.cloudScale() * 0.5f;
                pose.translate(0f, 0.25f + k * 0.62f * e.cloudScale(), 0f);
                pose.mulPose(Axis.YP.rotationDegrees(k * 55f + age * 2f));
                pose.scale(ws, ws, ws);
            } else {
                pose.scale(s, s, s);
            }
            GeoDraw.paint(pose, buffers, SPEC, "idle", seconds + k * 0.7f, GeoDraw.Space.PROP, GeoDraw.Layer.TRANSLUCENT, light, argb, 0);
            pose.popPose();
        }
    }
}
