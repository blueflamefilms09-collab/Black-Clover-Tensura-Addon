package com.newuniverse.nusmp.client.corpse;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.geo.GeoAnim;
import com.newuniverse.nusmp.client.geo.GeoModels;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.client.zogratis.ZogratisGlow;
import com.newuniverse.nusmp.client.zogratis.ZogratisPose;
import com.newuniverse.nusmp.client.zogratis.ZogratisRenderer;
import com.newuniverse.nusmp.corpse.CorpseDecay;
import com.newuniverse.nusmp.corpse.CorpseEffects;
import com.newuniverse.nusmp.corpse.NamedCorpseEntity;
import com.newuniverse.nusmp.corpse.NamedCorpses;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Why: Jimmy, Alfredo and Carl are GeoLite models (tools/gen_corpse_assets.py writes assets/nusmp/geo|animations|textures/entity/
 * corpse_*), and their decay look is part of the texture: four skins per puppet (stage 0 to 3, the same stage the marker effect carries
 * for every other Soul Corpse), each with its own glow map (eyes, stitch light). The animation clock, hurt tint and culling box come
 * from the Zogratis renderer base; this class only picks the model, the stage skin and the clip (idle, walk, run, attack, and a
 * throwing clip for Carl while {@link NamedCorpseEntity#castLeft()} counts down).
 */
public class NamedCorpseRenderer extends ZogratisRenderer<NamedCorpseEntity> {
    private static final int STAGES = CorpseDecay.MAX_STAGE + 1;
    private static final GeoSpec[][] SKINS = new GeoSpec[NamedCorpses.values().length][STAGES];
    private static final Clips[] CLIPS = new Clips[NamedCorpses.values().length];
    private static final float THROW_TICKS = NamedCorpseEntity.THROW_TICKS;

    static {
        for (NamedCorpses k : NamedCorpses.values()) {
            GeoSpec base = GeoSpec.of("corpse", k.id.substring("corpse_".length()));
            for (int s = 0; s < STAGES; s++) SKINS[k.ordinal()][s] = s == 0 ? base : base.skin(k.id + "_s" + s);
            CLIPS[k.ordinal()] = new Clips(base);
        }
    }

    public NamedCorpseRenderer(EntityRendererProvider.Context ctx) { super(ctx, 0.5f); }

    /** The clip lengths of one model, read from its animation file when asked (the base class's reader has a protected constructor). */
    private static final class Clips implements ZogratisPose.Clips {
        private final GeoSpec spec;

        Clips(GeoSpec spec) { this.spec = spec; }

        private GeoAnim.Clip find(String clip) {
            GeoAnim a = GeoModels.animations(spec.animations());
            return a == null ? null : a.clip(clip);
        }

        @Override
        public float length(String clip) {
            GeoAnim.Clip c = find(clip);
            return c == null ? 0f : c.length;
        }

        @Override
        public boolean loops(String clip) {
            GeoAnim.Clip c = find(clip);
            return c == null || c.loop;
        }
    }

    private static GeoSpec skin(NamedCorpseEntity e) {
        MobEffectInstance mark = e.getEffect(CorpseEffects.holder());
        return SKINS[e.kind().ordinal()][mark == null ? 0 : CorpseDecay.stageOf(mark.getAmplifier())];
    }

    @Override
    public ResourceLocation getTextureLocation(NamedCorpseEntity e) { return skin(e).texture(); }

    @Override
    public boolean shouldRender(NamedCorpseEntity e, Frustum frustum, double x, double y, double z) { return seen(e, frustum, x, y, z, 0.9, 0.6); }

    @Override
    public void render(NamedCorpseEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        ZogratisPose clock = clock(e);
        ZogratisPose.Frame f = frame(e, partial);
        int cast = e.castLeft();
        f.cast = cast > 0 ? 1 : 0;
        f.castProgress = cast > 0 ? 1f - Math.min(cast, THROW_TICKS) / THROW_TICKS : 0f;
        f.walkCycle = e.kind() == NamedCorpses.ALFREDO ? 2.0f : 1.5f;
        f.runCycle = e.kind() == NamedCorpses.JIMMY ? 2.4f : 2.8f;
        clock.update(f, CLIPS[e.kind().ordinal()]);

        float fade = f.dead ? ZogratisGlow.deathFade(f.deathTicks, DEATH_TICKS) : 1f;
        float glow = (0.65f + 0.25f * Mth.sin(f.age * 0.1f)) * fade;
        float body = Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot), head = Mth.rotLerp(partial, e.yHeadRotO, e.yHeadRot);
        float facing = clock.smoothYaw(!f.dead && (cast > 0 || clock.speed() < ZogratisPose.WALK_ON) ? head : body, f.age);

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-facing));
        paint(pose, buffers, skin(e), clock.clip, clock.seconds, light, bodyRgb(e), fade, ZogratisGlow.grey(glow));
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);
    }
}
