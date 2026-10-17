package com.newuniverse.nusmp.client.arc;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.arc.ArcBoss;
import com.newuniverse.nusmp.arc.ArcLook;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.client.prop.PropDraw;
import com.newuniverse.nusmp.client.zogratis.ZogratisGlow;
import com.newuniverse.nusmp.client.zogratis.ZogratisPose;
import com.newuniverse.nusmp.client.zogratis.ZogratisRenderer;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Why: thirty bosses must not mean thirty renderers. This ONE class draws every {@link ArcBoss} from the model the art kit wrote for it
 * ({@code assets/nusmp/geo|animations|textures/entity/arc_<model>*}, see tools/gen_arc_kit.py and docs/arc/art_kit.md), using exactly the machinery the Zogratis family
 * already trusts: the pose clock {@link ZogratisPose} (idle, walk and run paced by the ground covered, attack on a swing, cast while winding up and cast_big for an
 * ultimate, hurt, transform, death, and the optional chest_gem_reveal and spawn clips), the glow curve {@link ZogratisGlow} (the glow layer brightens with the power
 * drawn, swells over a cast, starves while exposed) and the clean fade of a dying body. The size is the hitbox over the model's own height ({@link ArcRenderMath#scale}), the
 * stride comes from the {@link ArcLook}, and a boss whose spec has a {@code form2} wears that second model from the middle of its transformation (each half plays half of the
 * transform clip, the way Lucius does). A missing model is tolerated: the geo loader logs it once and nothing is drawn, never an exception.
 */
public class ArcBossRenderer extends ZogratisRenderer<ArcBoss> {
    private final ArcLook look;
    private final GeoSpec first, second;
    private final ArcClips firstClips, secondClips;

    public ArcBossRenderer(EntityRendererProvider.Context ctx, ArcLook look) {
        super(ctx, look.shadow());
        this.look = look;
        String name = ArcModelKey.clean(look.model());
        GeoSpec a = GeoSpec.of(ArcModelKey.KEY, name.isEmpty() ? ArcModelKey.FALLBACK : name);
        GeoSpec b = GeoSpec.of(ArcModelKey.KEY, name.isEmpty() ? ArcModelKey.FALLBACK : ArcModelKey.form2(name));
        this.first = look.glow() ? a : a.noGlow();
        this.second = look.glow() ? b : b.noGlow();
        this.firstClips = new ArcClips(first);
        this.secondClips = new ArcClips(second);
    }

    @Override
    public ResourceLocation getTextureLocation(ArcBoss e) { return first.texture(); }

    /** Wings, capes, halos and weapons reach past the hitbox: cull by the look's bigger box. */
    @Override
    public boolean shouldRender(ArcBoss e, Frustum frustum, double x, double y, double z) { return seen(e, frustum, x, y, z, look.growXz(), look.growUp()); }

    @Override
    public void render(ArcBoss e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        ZogratisPose clock = clock(e);
        ZogratisPose.Frame f = frame(e, partial);
        boolean transforming = e.transforming(), casting = e.state() == ZogratisBoss.STATE_CASTING, newForm = e.form() != ZogratisBoss.FORM_BASE;
        float phase = e.phaseProgress();
        boolean hasSecond = ArcModels.has(second.model());
        boolean useSecond = hasSecond && newForm;
        GeoSpec spec = useSecond ? second : first;
        ArcClips clips = useSecond ? secondClips : firstClips;
        Move move = casting ? e.castingMoveOrNull() : null;
        boolean ultimate = move != null && move.ultimate();
        float size = ArcRenderMath.scale(e.getBbHeight(), look.height());

        f.transforming = transforming;
        f.transformProgress = !hasSecond ? phase : useSecond ? (phase - 0.5f) * 2f : phase * 2f;       // two models each play half of the clip; one plays it all
        f.gemOut = e.exposed();                                       // the optional chest_gem_reveal clip: a model without it simply skips it
        f.cast = casting ? (ultimate ? 2 : 1) : 0;
        f.castProgress = phase;
        f.walkCycle = look.walkCycle() * size;
        f.runCycle = look.runCycle() * size;
        clock.update(f, clips);
        float spawn = ArcRenderMath.spawnSeconds(e.tickCount + partial, clips.length(ZogratisPose.SPAWN), f.dead);
        if (spawn >= 0f && !transforming && !casting) {               // the optional spawn clip, once, when the body first appears
            clock.clip = ZogratisPose.SPAWN;
            clock.seconds = spawn;
        }

        int mode = f.dead ? ZogratisGlow.NORMAL : transforming ? ZogratisGlow.TRANSFORMING : casting ? (ultimate ? ZogratisGlow.ULTIMATE : ZogratisGlow.CASTING)
                : e.exposed() ? ZogratisGlow.EXPOSED : ZogratisGlow.NORMAL;
        float fade = f.dead ? ZogratisGlow.deathFade(f.deathTicks, DEATH_TICKS) : 1f;
        int glow = ZogratisGlow.grey(ZogratisGlow.level(e.power(), f.age, mode, phase) * fade);

        float scale = clock.smoothScale(size, f.age);
        boolean still = clock.speed() < ZogratisPose.WALK_ON;
        float body = Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot), head = Mth.rotLerp(partial, e.yHeadRotO, e.yHeadRot);
        float facing = clock.smoothYaw(f.dead || !(casting || still) ? body : head, f.age);        // it faces its target while it casts or stands, its way while it walks

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-facing));
        pose.scale(scale, scale, scale);
        paint(pose, buffers, spec, clock.clip, clock.seconds, light, bodyRgb(e), fade, glow);
        float flash = transforming ? ZogratisGlow.flash(phase) : 0f;
        if (flash > 0.02f) {                                          // the character's colour burns over the power-up (and over the model swap of a second form)
            int rgb = ZogratisGlow.mix(e.styleColor(), 0xFFFFFF, 0.5f);
            GeoDraw.paint(pose, buffers, spec.noGlow(), clock.clip, clock.seconds, GeoDraw.Space.PROP, GeoDraw.Layer.ADDITIVE, PropDraw.FULL_BRIGHT,
                    ZogratisGlow.withAlpha(rgb, flash * 0.8f), 0);
        }
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);          // the name tag
    }
}
