package com.newuniverse.nusmp.client.floor.goliath;

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
import com.newuniverse.nusmp.dungeon.floor.goliath.GoliathBoss;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Draws the Goliath (tools/gen_goliath.py writes assets/nusmp/geo|animations|textures/entity/goliath_*) from its synched state:
 * <ul>
 *   <li><b>Skin</b>: the whole stone body, and from the middle of the Gates' transformation the cracked skin (same model, every crack burning), with a
 *       burst of amber over the swap.</li>
 *   <li><b>Rise</b>: the model is sunk into the floor by the eased rise value (smoothed between ticks) and plays the rise clip at that time, so the fists
 *       break through first; the floor's blocks hide what is below.</li>
 *   <li><b>Clip</b> ({@link GoliathPose}): the clip of the move being wound up, paced to its wind-up; otherwise the Zogratis pose logic (idle, walk, blow,
 *       flinch, transformation, death). The glow layer pulses with its fury ({@link ZogratisGlow}).</li>
 * </ul>
 */
public class GoliathRenderer extends ZogratisRenderer<GoliathBoss> {
    private static final String KEY = "goliath";
    private static final GeoSpec BODY = GeoSpec.of(KEY, "body"), CRACKED = BODY.skin("goliath_cracked");
    /** The model is authored at its true size: 6.2 blocks to the top of the horns, and 4.4 blocks of ground to a cycle of its walk. */
    private static final float MODEL_HEIGHT = 6.2f, WALK_CYCLE = 4.4f, SHADOW = 2.2f;
    private static final int AMBER = 0xFFAA33;

    private final Map<Entity, GoliathPose> poses = new WeakHashMap<>();
    private final Clips clips = new Clips(BODY);

    public GoliathRenderer(EntityRendererProvider.Context ctx) { super(ctx, SHADOW); }

    @Override
    public ResourceLocation getTextureLocation(GoliathBoss e) { return BODY.texture(); }

    /** The arms reach well past the hitbox: cull by a bigger box. */
    @Override
    public boolean shouldRender(GoliathBoss e, Frustum frustum, double x, double y, double z) { return seen(e, frustum, x, y, z, 3.5, 1.0); }

    @Override
    public void render(GoliathBoss e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        GoliathPose clock = poses.computeIfAbsent(e, k -> new GoliathPose());
        ZogratisPose.Frame f = frame(e, partial);
        boolean transforming = e.transforming(), casting = e.state() == ZogratisBoss.STATE_CASTING, cracked = e.form() != ZogratisBoss.FORM_BASE;
        float phase = e.phaseProgress(), rise = e.rise(partial);
        Move move = casting ? e.castingMoveOrNull() : null;
        boolean ultimate = move != null && move.ultimate();
        float size = e.getBbHeight() / e.getType().getHeight();
        f.transforming = transforming;
        f.transformProgress = phase;
        f.cast = casting ? (ultimate ? 2 : 1) : 0;
        f.castProgress = phase;
        f.walkCycle = WALK_CYCLE * size;
        f.runCycle = WALK_CYCLE * size;
        clock.update(f, GoliathPose.clipOf(move == null ? null : move.id()), rise, clips);

        GeoSpec spec = cracked ? CRACKED : BODY;
        int mode = f.dead ? ZogratisGlow.NORMAL : transforming ? ZogratisGlow.TRANSFORMING : casting ? (ultimate ? ZogratisGlow.ULTIMATE : ZogratisGlow.CASTING)
                : e.exposed() ? ZogratisGlow.EXPOSED : ZogratisGlow.NORMAL;
        float fade = f.dead ? ZogratisGlow.deathFade(f.deathTicks, DEATH_TICKS) : 1f;
        int glow = ZogratisGlow.grey(ZogratisGlow.level(e.power(), f.age, mode, phase) * fade);
        float scale = clock.smoothScale(size, f.age);
        boolean still = clock.speed() < ZogratisPose.WALK_ON;
        float body = Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot), head = Mth.rotLerp(partial, e.yHeadRotO, e.yHeadRot);
        float facing = clock.smoothYaw(f.dead || !(casting || still) ? body : head, f.age);
        float sink = (1f - rise) * (MODEL_HEIGHT * scale + 0.4f);
        this.shadowRadius = rise < 1f ? 0f : SHADOW * scale;

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-facing));
        pose.translate(0f, -sink, 0f);
        pose.scale(scale, scale, scale);
        paint(pose, buffers, spec, clock.clip, clock.seconds, light, bodyRgb(e), fade, glow);
        float flash = transforming ? ZogratisGlow.flash(phase) : 0f;
        if (flash > 0.02f) {                                                   // amber burns over the skin swap at the middle of the transformation
            int rgb = ZogratisGlow.mix(AMBER, 0xFFFFFF, 0.4f);
            GeoDraw.paint(pose, buffers, spec.noGlow(), clock.clip, clock.seconds, GeoDraw.Space.PROP, GeoDraw.Layer.ADDITIVE, PropDraw.FULL_BRIGHT,
                    ZogratisGlow.withAlpha(rgb, flash * 0.8f), 0);
        }
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);                   // the name tag
    }

    /** The lengths of the clips of the model, read from its animation file when asked (so a resource reload is picked up). */
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
}
