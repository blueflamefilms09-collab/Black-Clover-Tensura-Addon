package com.newuniverse.nusmp.client.dragonballs;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.dragonballs.BallSet;
import com.newuniverse.nusmp.dragonballs.ShenronEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Draws Shenron and Super Shenron from the geo model {@code geo/entity/dragonballs_shenron.geo.json} (tools/gen_dragon_balls.py: a chain of about 26 body bones that undulate in the "idle" clip, a head with
 * horns, whiskers and glowing red eyes, four claws and a dorsal fin). Super Shenron uses the same model with the gold skin. The dragon is always drawn full bright (it hangs in a darkened sky) with its glow
 * layer on, and rises into view over the first two seconds. One model draw per dragon, at most two dragons exist at once: about 400 quads each, no allocation per frame.
 */
public class ShenronRenderer extends EntityRenderer<ShenronEntity> {
    private static final GeoSpec EARTH = GeoSpec.of("dragonballs", "shenron");
    private static final GeoSpec SUPER = EARTH.skin("dragonballs_shenron_super");
    private static final float SCALE = 0.95f;

    public ShenronRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0f;
    }

    @Override
    public ResourceLocation getTextureLocation(ShenronEntity e) { return EARTH.texture(); }

    /** A dragon that tall is seen from far away and its box is small: never cull it. */
    @Override
    public boolean shouldRender(ShenronEntity e, Frustum frustum, double x, double y, double z) { return true; }

    @Override
    public void render(ShenronEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        float age = e.tickCount + partial;
        float rise = Mth.clamp(age / 40f, 0f, 1f);
        rise = rise * rise * (3f - 2f * rise);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partial, e.yRotO, e.getYRot())));
        float s = SCALE * (0.35f + 0.65f * rise);
        pose.scale(s, s, s);
        GeoSpec spec = e.set() == BallSet.SUPER ? SUPER : EARTH;
        GeoDraw.paint(pose, buffers, spec, "idle", age / 20f, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, LightTexture.FULL_BRIGHT, 0xFFFFFFFF, 0xFFFFFFFF);
        pose.popPose();
    }
}
