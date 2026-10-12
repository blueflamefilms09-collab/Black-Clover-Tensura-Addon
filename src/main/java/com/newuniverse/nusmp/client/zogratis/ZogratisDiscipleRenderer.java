package com.newuniverse.nusmp.client.zogratis;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.entity.zogratis.DarkDiscipleEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The Dark Disciple: one hooded model with a bone mask (tools/gen_zogratis_triad.py), tinted by the devil of its master through the
 * texture (Lucifero violet, Megicula crimson, Beelzebub indigo, Astaroth gold; any other devil the neutral grey). It raises its arm
 * and its glow gathers over the telegraph of its ability ({@link DarkDiscipleEntity#castLeft}), and flinches, swings, walks and runs
 * like the bosses.
 */
public class ZogratisDiscipleRenderer extends ZogratisRenderer<DarkDiscipleEntity> {
    private static final GeoSpec NEUTRAL = spec("disciple");
    private static final GeoSpec LUCIFERO = NEUTRAL.skin("zogratis_disciple_lucifero"), MEGICULA = NEUTRAL.skin("zogratis_disciple_megicula"),
            BEELZEBUB = NEUTRAL.skin("zogratis_disciple_beelzebub"), ASTAROTH = NEUTRAL.skin("zogratis_disciple_astaroth");
    private static final GeoClips CLIPS = new GeoClips(NEUTRAL);
    private static final float SCALE = 0.82f, LONGEST_WIND_UP = 26f;

    public ZogratisDiscipleRenderer(EntityRendererProvider.Context ctx) { super(ctx, 0.45f); }

    private static GeoSpec specOf(DarkDiscipleEntity e) {
        return switch (e.devil()) {
            case LUCIFERO -> LUCIFERO;
            case MEGICULA -> MEGICULA;
            case BEELZEBUB -> BEELZEBUB;
            case ASTAROTH -> ASTAROTH;
            default -> NEUTRAL;
        };
    }

    @Override
    public ResourceLocation getTextureLocation(DarkDiscipleEntity e) { return specOf(e).texture(); }

    @Override
    public boolean shouldRender(DarkDiscipleEntity e, Frustum frustum, double x, double y, double z) { return seen(e, frustum, x, y, z, 0.8, 0.8); }

    @Override
    public void render(DarkDiscipleEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        ZogratisPose clock = clock(e);
        ZogratisPose.Frame f = frame(e, partial);
        int wind = e.castLeft();
        float size = SCALE * e.getBbHeight() / e.getType().getHeight();
        f.cast = wind > 0 ? 1 : 0;
        f.castProgress = wind > 0 ? 1f - Math.min(wind, LONGEST_WIND_UP) / LONGEST_WIND_UP : 0f;
        f.walkCycle = 1.4f * size;
        f.runCycle = 2.2f * size;
        clock.update(f, CLIPS);

        float fade = f.dead ? ZogratisGlow.deathFade(f.deathTicks, DEATH_TICKS) : 1f;
        float level = ZogratisGlow.level(70, f.age, wind > 0 ? ZogratisGlow.CASTING : ZogratisGlow.NORMAL, f.castProgress) * fade;
        float body = Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot), head = Mth.rotLerp(partial, e.yHeadRotO, e.yHeadRot);
        float facing = clock.smoothYaw(!f.dead && (wind > 0 || clock.speed() < ZogratisPose.WALK_ON) ? head : body, f.age);

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-facing));
        pose.scale(size, size, size);
        paint(pose, buffers, specOf(e), clock.clip, clock.seconds, light, bodyRgb(e), fade, ZogratisGlow.grey(level));
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);
    }
}
