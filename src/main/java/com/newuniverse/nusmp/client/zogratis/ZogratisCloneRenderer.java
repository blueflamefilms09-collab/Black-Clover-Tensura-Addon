package com.newuniverse.nusmp.client.zogratis;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.entity.zogratis.ZogratisCloneEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * A clone of Lucius: his body of the form he had when it was made (the human model, or the divine one with its wings and halo, a little
 * smaller to fit the clone's hitbox), his grimoire before it, so it is as convincing as the real one. It raises its arm over the
 * telegraph of its spell, flashes gold when the Swap Strike has marked it, fades out over its last two seconds, and its glow is a plain
 * mid-power one.
 */
public class ZogratisCloneRenderer extends ZogratisRenderer<ZogratisCloneEntity> {
    private static final GeoSpec HUMAN = spec("lucius_base"), DIVINE = spec("lucius_divine");
    private static final GeoClips HUMAN_CLIPS = new GeoClips(HUMAN), DIVINE_CLIPS = new GeoClips(DIVINE);
    private static final float TELL_TICKS = 14f;
    private static final int GOLD_FLASH_RGB = 0xFFD24A;
    private static final int POWER = 60;

    public ZogratisCloneRenderer(EntityRendererProvider.Context ctx) { super(ctx, 0.7f); }

    @Override
    public ResourceLocation getTextureLocation(ZogratisCloneEntity e) { return HUMAN.texture(); }

    @Override
    public boolean shouldRender(ZogratisCloneEntity e, Frustum frustum, double x, double y, double z) { return seen(e, frustum, x, y, z, 2.8, 2.4); }

    @Override
    public void render(ZogratisCloneEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        ZogratisPose clock = clock(e);
        ZogratisPose.Frame f = frame(e, partial);
        boolean divine = e.form() == ZogratisBoss.FORM_DIVINE;
        GeoSpec spec = divine ? DIVINE : HUMAN;
        GeoClips clips = divine ? DIVINE_CLIPS : HUMAN_CLIPS;
        int tell = e.castLeft();
        f.cast = tell > 0 ? 1 : 0;
        f.castProgress = tell > 0 ? 1f - Math.min(tell, TELL_TICKS) / TELL_TICKS : 0f;
        float size = divine ? 0.88f : 1.0f;
        f.walkCycle = 1.5f * size;
        f.runCycle = 2.6f * size;
        clock.update(f, clips);

        boolean marked = e.markLeft() > 0;
        float fade = Math.min(e.fade(), f.dead ? ZogratisGlow.deathFade(f.deathTicks, DEATH_TICKS) : 1f);
        float level = ZogratisGlow.level(POWER, f.age, tell > 0 ? ZogratisGlow.CASTING : ZogratisGlow.NORMAL, f.castProgress);
        if (marked) level = Math.max(level, 0.9f);
        level *= fade;
        int rgb = marked ? ZogratisGlow.mix(0xFFFFFF, GOLD_FLASH_RGB, 0.5f + 0.5f * (float) Math.sin(f.age * 0.7f)) : bodyRgb(e);
        float lift = divine ? 0.12f + 0.05f * (float) Math.sin(f.age * 0.12f) : 0f;
        float body = Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot), head = Mth.rotLerp(partial, e.yHeadRotO, e.yHeadRot);
        float facing = clock.smoothYaw(!f.dead && (tell > 0 || clock.speed() < ZogratisPose.WALK_ON) ? head : body, f.age);

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-facing));
        pose.translate(0f, lift, 0f);
        pose.scale(size, size, size);
        paint(pose, buffers, spec, clock.clip, clock.seconds, light, rgb, fade, ZogratisGlow.grey(level));
        LuciusBook.draw(pose, buffers, clock, clips, 0f, light, fade, ZogratisGlow.grey(level), divine);
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);
    }
}
