package com.newuniverse.nusmp.client.zogratis;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.client.prop.PropDraw;
import com.newuniverse.nusmp.entity.zogratis.DevilStyle;
import com.newuniverse.nusmp.entity.zogratis.LuciusBoss;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Draws Dante, Vanica, Zenon and Lucius (one class, one {@link Look} each) from their synced state:
 * <ul>
 *   <li><b>Model</b>: the Triad wear their base model and switch to the hybrid (devil) model as the Gates open (its transformation clip grows
 *       horns, halo, wings and claws from nothing over the whole 3 seconds); Lucius wears his human model, then the divine one (three pairs
 *       of wings, the halo of gears, antlers) from the middle of the transformation, each half playing its own transformation clip.</li>
 *   <li><b>Clip</b> ({@link ZogratisPose}): idle, walk and run by the ground covered, attack on a swing, cast while winding up (cast_big
 *       for an ultimate, paced to the wind-up so the slam is the release), hurt, transform, death; it always faces what it looks at.</li>
 *   <li><b>Light</b> ({@link ZogratisGlow}): the glow layer pulses brighter the more of the devil's power it has drawn, swells over a
 *       cast, starves while it is exposed after an ultimate, and a flash of the devil's colour covers the model swap.</li>
 *   <li><b>Lucius</b> also floats in his divine form, carries the grimoire prop in front of him in both forms, and glows white while
 *       Julius's will holds the body.</li>
 * </ul>
 */
public class ZogratisBossRenderer<T extends ZogratisBoss> extends ZogratisRenderer<T> {
    /** One boss's wardrobe and proportions. */
    public static final class Look {
        final GeoSpec first, second;
        final GeoClips firstClips, secondClips;
        final float scale, walkCycle, runCycle, shadow;
        final boolean followsHitbox, splitTransform, book;
        final double growXz, growUp;

        /**
         * @param scale         the model's size in the world when the hitbox is its normal size
         * @param followsHitbox grow with the hitbox (the Triad's hybrids are bigger bodies); false when the model was made for it
         * @param splitTransform the two models each play half of the transformation (Lucius); false: the second plays all of it
         * @param walkCycle     blocks of ground one cycle of the walk clip covers (and the run clip: runCycle), at scale 1
         */
        Look(String first, String second, float scale, boolean followsHitbox, boolean splitTransform, boolean book, float shadow,
             float walkCycle, float runCycle, double growXz, double growUp) {
            this.first = spec(first);
            this.second = spec(second);
            this.firstClips = new GeoClips(this.first);
            this.secondClips = new GeoClips(this.second);
            this.scale = scale;
            this.followsHitbox = followsHitbox;
            this.splitTransform = splitTransform;
            this.book = book;
            this.shadow = shadow;
            this.walkCycle = walkCycle;
            this.runCycle = runCycle;
            this.growXz = growXz;
            this.growUp = growUp;
        }
    }

    public static final Look DANTE = new Look("dante_base", "dante_hybrid", 0.90f, true, false, false, 0.60f, 1.5f, 2.4f, 1.3, 1.0);
    public static final Look VANICA = new Look("vanica_base", "vanica_hybrid", 0.82f, true, false, false, 0.55f, 1.4f, 2.3f, 1.4, 1.0);
    public static final Look ZENON = new Look("zenon_base", "zenon_hybrid", 0.92f, true, false, false, 0.58f, 1.5f, 2.4f, 1.6, 1.0);
    public static final Look LUCIUS = new Look("lucius_base", "lucius_divine", 1.0f, false, true, true, 0.70f, 1.5f, 2.6f, 2.8, 2.2);

    /** Lucius's Julius tint of the glow: the Wizard King's white-blue. */
    private static final int JULIUS_RGB = 0xCFE6FF;

    private final Look look;

    public ZogratisBossRenderer(EntityRendererProvider.Context ctx, Look look) {
        super(ctx, look.shadow);
        this.look = look;
    }

    @Override
    public ResourceLocation getTextureLocation(T e) { return look.first.texture(); }

    /** Wings, halos, capes and horns reach far past the hitbox: cull by a bigger box. */
    @Override
    public boolean shouldRender(T e, Frustum frustum, double x, double y, double z) { return seen(e, frustum, x, y, z, look.growXz, look.growUp); }

    @Override
    public void render(T e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        ZogratisPose clock = clock(e);
        ZogratisPose.Frame f = frame(e, partial);
        int form = e.form();
        boolean transforming = e.transforming(), casting = e.state() == ZogratisBoss.STATE_CASTING, newForm = form != ZogratisBoss.FORM_BASE;
        float phase = e.phaseProgress();
        boolean second = look.splitTransform ? newForm : newForm || transforming;          // the Triad's hybrid model is worn for the whole transformation
        GeoSpec spec = second ? look.second : look.first;
        GeoClips clips = second ? look.secondClips : look.firstClips;
        Move move = casting ? e.castingMoveOrNull() : null;
        boolean ultimate = move != null && move.ultimate();

        float target = look.scale * (look.followsHitbox ? e.getBbHeight() / e.getType().getHeight() : 1f);
        f.transforming = transforming;
        f.transformProgress = !look.splitTransform ? phase : second ? (phase - 0.5f) * 2f : phase * 2f;
        f.cast = casting ? (ultimate ? 2 : 1) : 0;
        f.castProgress = phase;
        f.walkCycle = look.walkCycle * target;
        f.runCycle = look.runCycle * target;
        clock.update(f, clips);

        int mode = f.dead ? ZogratisGlow.NORMAL : transforming ? ZogratisGlow.TRANSFORMING : casting ? (ultimate ? ZogratisGlow.ULTIMATE : ZogratisGlow.CASTING)
                : e.exposed() ? ZogratisGlow.EXPOSED : ZogratisGlow.NORMAL;
        float fade = f.dead ? ZogratisGlow.deathFade(f.deathTicks, DEATH_TICKS) : 1f;
        float level = ZogratisGlow.level(e.power(), f.age, mode, phase) * fade;
        boolean julius = e instanceof LuciusBoss l && l.julius();
        int glow = julius ? ZogratisGlow.scaled(JULIUS_RGB, level) : ZogratisGlow.grey(level);

        float size = clock.smoothScale(target, f.age);
        float lift = e instanceof LuciusBoss l ? l.hover() * ZogratisGlow.hoverRamp(transforming, phase, form == ZogratisBoss.FORM_DIVINE) : 0f;
        boolean still = clock.speed() < ZogratisPose.WALK_ON;
        float body = Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot), head = Mth.rotLerp(partial, e.yHeadRotO, e.yHeadRot);
        float facing = clock.smoothYaw(f.dead || !(casting || still) ? body : head, f.age);   // it faces its target while it casts or stands, its way while it walks

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-facing));
        pose.translate(0f, lift, 0f);
        pose.scale(size, size, size);
        paint(pose, buffers, spec, clock.clip, clock.seconds, light, bodyRgb(e), fade, glow);
        if (look.book) LuciusBook.draw(pose, buffers, clock, clips, phase, light, fade, glow, form == ZogratisBoss.FORM_DIVINE);
        // the flash is at the model swap: the middle for Lucius; for the Triad the hybrid body is worn from the first frame, so it also bursts there
        float flash = !transforming ? 0f : look.splitTransform ? ZogratisGlow.flash(phase) : Math.max(ZogratisGlow.flash(phase), ZogratisGlow.flash(0.5f + phase));
        if (flash > 0.02f) {                                                  // the devil's colour burns over the model swap
            int rgb = ZogratisGlow.mix(DevilStyle.color(e.devil()), 0xFFFFFF, 0.5f);
            GeoDraw.paint(pose, buffers, spec.noGlow(), clock.clip, clock.seconds, GeoDraw.Space.PROP, GeoDraw.Layer.ADDITIVE, PropDraw.FULL_BRIGHT,
                    ZogratisGlow.withAlpha(rgb, flash * 0.85f), 0);
        }
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);                   // the name tag
    }
}
