package com.newuniverse.nusmp.client.zogratis;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.entity.zogratis.BloodBeastEntity;
import com.newuniverse.nusmp.entity.zogratis.VanicaMath;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Vanica's Blood Beast: a lion-wolf of living blood with a blood-thorn mane and glowing veins (tools/gen_zogratis_triad.py). It rises out
 * of a puddle (the spawn clip, {@link BloodBeastEntity#riseLeft}), crouches over the telegraph of its pounce
 * ({@link BloodBeastEntity#pounceLeft}: the first moment of the attack clip), hangs open-jawed in the air of the leap, bites (attack) and
 * melts into a puddle when it dies. The more of Megicula's power its master had drawn ({@link BloodBeastEntity#fillPercent}), the bigger
 * and brighter it is.
 */
public class ZogratisBeastRenderer extends ZogratisRenderer<BloodBeastEntity> {
    private static final GeoSpec SPEC = spec("blood_beast");
    private static final GeoClips CLIPS = new GeoClips(SPEC);
    private static final float SCALE = 0.75f;
    /** The pounce's crouch lasts 10 ticks (BloodBeastEntity.POUNCE_WIND) and is the first 0.18 s of the attack clip; the lunge pose is at 0.42 s. */
    private static final float CROUCH_TICKS = 10f, CROUCH_SECONDS = 0.18f, LUNGE_SECONDS = 0.42f;
    /** Ticks of the rise from a puddle: a stampede beast's is short. */
    private static final int STAMPEDE_RISE = 12;

    public ZogratisBeastRenderer(EntityRendererProvider.Context ctx) { super(ctx, 0.9f); }

    @Override
    public ResourceLocation getTextureLocation(BloodBeastEntity e) { return SPEC.texture(); }

    @Override
    public boolean shouldRender(BloodBeastEntity e, Frustum frustum, double x, double y, double z) { return seen(e, frustum, x, y, z, 1.2, 0.6); }

    @Override
    public void render(BloodBeastEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        ZogratisPose clock = clock(e);
        ZogratisPose.Frame f = frame(e, partial);
        int fill = Math.max(0, Math.min(100, e.fillPercent()));
        float size = SCALE * (0.92f + 0.16f * fill / 100f) * (e.stampede() ? 0.9f : 1f) * e.getBbHeight() / e.getType().getHeight();
        f.walkCycle = 1.1f * size;
        f.runCycle = 1.9f * size;
        clock.update(f, CLIPS);

        String clip = clock.clip;
        float secs = clock.seconds;
        int rise = e.riseLeft(), crouch = e.pounceLeft();
        if (!f.dead) {
            if (rise > 0) {
                clip = ZogratisPose.SPAWN;
                float total = Math.max(rise, e.stampede() ? STAMPEDE_RISE : VanicaMath.SPAWN_TICKS);
                float left = Math.max(0f, rise - partial);                   // the counters tick down once per tick: the partial tick keeps the clip smooth
                secs = (1f - left / total) * CLIPS.length(ZogratisPose.SPAWN);
            } else if (crouch > 0) {
                clip = ZogratisPose.ATTACK;
                float left = Math.max(0f, Math.min(crouch, CROUCH_TICKS) - partial);
                secs = (1f - left / CROUCH_TICKS) * CROUCH_SECONDS;
            } else if (e.leaping()) {
                clip = ZogratisPose.ATTACK;                                  // in the air of the leap: jaws open, body stretched
                secs = LUNGE_SECONDS;
            }
        }
        float fade = f.dead ? ZogratisGlow.deathFade(f.deathTicks, DEATH_TICKS) : 1f;
        boolean wind = crouch > 0;
        float level = ZogratisGlow.level(Math.max(20, fill), f.age, wind ? ZogratisGlow.CASTING : ZogratisGlow.NORMAL,
                wind ? 1f - Math.min(crouch, CROUCH_TICKS) / CROUCH_TICKS : 0f) * fade;
        float body = Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot), head = Mth.rotLerp(partial, e.yHeadRotO, e.yHeadRot);
        float facing = clock.smoothYaw(wind && !f.dead ? head : body, f.age);

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-facing));
        pose.scale(size, size, size);
        paint(pose, buffers, SPEC, clip, secs, light, bodyRgb(e), fade, ZogratisGlow.grey(level));
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);
    }
}
