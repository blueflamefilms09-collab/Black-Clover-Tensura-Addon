package com.newuniverse.nusmp.client.avatar;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.geo.GeoAnim;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoModels;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.client.prop.PropDraw;
import com.newuniverse.nusmp.client.zogratis.ZogratisGlow;
import com.newuniverse.nusmp.client.zogratis.ZogratisPose;
import com.newuniverse.nusmp.client.zogratis.ZogratisRenderer;
import com.newuniverse.nusmp.entity.avatar.DevilAvatarBoss;
import com.newuniverse.nusmp.entity.zogratis.DevilStyle;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Draws the Devil Avatar (tools/gen_devil_avatar.py writes assets/nusmp/geo|animations|textures/entity/devil_avatar_*) from its synced state,
 * on the pose clock and the glow of the Zogratis renderers:
 * <ul>
 *   <li><b>Model</b>: one body in two forms, {@code base} until the Gates open and {@code gates} after (a second halo, longer horns, more cracks),
 *       each playing its half of the transformation clip with the swap in the middle; the hitbox grows 1.15 times with the form and the model
 *       follows it. Two skins of each: the Avatar of Lucifero's violet and the Avatar of Megicula's crimson (the glow layer follows the tint).</li>
 *   <li><b>Clip</b> ({@link ZogratisPose}): idle, walk and run by the ground covered, attack on a swing, cast while winding up (cast_big for
 *       the ultimate), hurt, transform, death.</li>
 *   <li><b>Light</b> ({@link ZogratisGlow}): the glow pulses brighter with the power drawn, swells over a cast, starves while exposed, and a
 *       flash of the devil's colour covers the model swap.</li>
 * </ul>
 */
public class AvatarRenderer extends ZogratisRenderer<DevilAvatarBoss> {
    /** The geo key of the Avatar's assets: {@code GeoSpec.of(KEY, "base")} is devil_avatar_base.* */
    public static final String KEY = "devil_avatar";

    private static final GeoSpec BASE = GeoSpec.of(KEY, "base"), GATES = GeoSpec.of(KEY, "gates");
    private static final GeoSpec BASE_MEGICULA = BASE.skin(KEY + "_base_megicula"), GATES_MEGICULA = GATES.skin(KEY + "_gates_megicula");
    private static final Clips BASE_CLIPS = new Clips(BASE), GATES_CLIPS = new Clips(GATES);
    /** Blocks of ground one cycle of the walk and of the run clip covers at scale 1 (the stride of the animation). */
    private static final float WALK_CYCLE = 2.0f, RUN_CYCLE = 3.4f;

    public AvatarRenderer(EntityRendererProvider.Context ctx) { super(ctx, 0.8f); }

    /** The lengths of the clips of one model, read from its animation file when asked (so a resource reload is picked up). */
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

    private static GeoSpec specOf(DevilAvatarBoss e, boolean gates) {
        return e.megicula() ? (gates ? GATES_MEGICULA : BASE_MEGICULA) : (gates ? GATES : BASE);
    }

    @Override
    public ResourceLocation getTextureLocation(DevilAvatarBoss e) { return specOf(e, false).texture(); }

    /** The halo, the horns and the cloak reach far past the hitbox: cull by a bigger box. */
    @Override
    public boolean shouldRender(DevilAvatarBoss e, Frustum frustum, double x, double y, double z) { return seen(e, frustum, x, y, z, 2.6, 1.8); }

    @Override
    public void render(DevilAvatarBoss e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        ZogratisPose clock = clock(e);
        ZogratisPose.Frame f = frame(e, partial);
        int form = e.form();
        boolean transforming = e.transforming(), casting = e.state() == ZogratisBoss.STATE_CASTING, gates = form != ZogratisBoss.FORM_BASE;
        float phase = e.phaseProgress();
        GeoSpec spec = specOf(e, gates);
        ZogratisPose.Clips clips = gates ? GATES_CLIPS : BASE_CLIPS;
        Move move = casting ? e.castingMoveOrNull() : null;
        boolean ultimate = move != null && move.ultimate();

        float target = e.getBbHeight() / e.getType().getHeight();                       // the hitbox grows with the Gates; the body follows it
        f.transforming = transforming;
        f.transformProgress = gates ? (phase - 0.5f) * 2f : phase * 2f;                  // each model plays its half of the transformation
        f.cast = casting ? (ultimate ? 2 : 1) : 0;
        f.castProgress = phase;
        f.walkCycle = WALK_CYCLE * target;
        f.runCycle = RUN_CYCLE * target;
        clock.update(f, clips);

        int mode = f.dead ? ZogratisGlow.NORMAL : transforming ? ZogratisGlow.TRANSFORMING : casting ? (ultimate ? ZogratisGlow.ULTIMATE : ZogratisGlow.CASTING)
                : e.exposed() ? ZogratisGlow.EXPOSED : ZogratisGlow.NORMAL;
        float fade = f.dead ? ZogratisGlow.deathFade(f.deathTicks, DEATH_TICKS) : 1f;
        float level = ZogratisGlow.level(e.power(), f.age, mode, phase) * fade;
        int glow = ZogratisGlow.grey(level);

        float size = clock.smoothScale(target, f.age);
        boolean still = clock.speed() < ZogratisPose.WALK_ON;
        float body = Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot), head = Mth.rotLerp(partial, e.yHeadRotO, e.yHeadRot);
        float facing = clock.smoothYaw(f.dead || !(casting || still) ? body : head, f.age);   // it faces its target while it casts or stands, its way while it walks

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-facing));
        pose.scale(size, size, size);
        paint(pose, buffers, spec, clock.clip, clock.seconds, light, bodyRgb(e), fade, glow);
        float flash = !transforming ? 0f : ZogratisGlow.flash(phase);                   // the flash is at the model swap, in the middle
        if (flash > 0.02f) {                                                             // the devil's colour burns over the swap
            int rgb = ZogratisGlow.mix(DevilStyle.color(e.devil()), 0xFFFFFF, 0.5f);
            GeoDraw.paint(pose, buffers, spec.noGlow(), clock.clip, clock.seconds, GeoDraw.Space.PROP, GeoDraw.Layer.ADDITIVE, PropDraw.FULL_BRIGHT,
                    ZogratisGlow.withAlpha(rgb, flash * 0.85f), 0);
        }
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);                              // the name tag
    }
}
