package com.newuniverse.nusmp.client.relics.order;

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
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.relics.order.GodOfOrder;
import com.newuniverse.nusmp.relics.order.OrderPlan.Phase;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Draws the God of Order (tools/gen_god_of_order.py writes assets/nusmp/geo|animations|textures/entity/god_of_order_*) from its synced state, on the
 * pose clock and the glow of the Zogratis renderers:
 * <ul>
 *   <li><b>Model</b>: one body in two forms, {@code base} and {@code gates} (the open ledger: a second turning halo and floating pages), swapped in the
 *       middle of the transformation like the other framework bosses.</li>
 *   <li><b>Clip</b> ({@link ZogratisPose}) for the fight: idle, walk, attack, cast, cast_big, hurt, transform, death. The visit has clips of its own,
 *       chosen from the synced phase: {@code read} while it reads its verdict (arms open, the halo turning fast), {@code beam} while it burns the
 *       duplicate (the staff levelled at the holder), {@code bow} when it leaves. It steps out of its column: it grows to full size in 0.7 s.</li>
 *   <li><b>Light</b> ({@link ZogratisGlow}): the glow swells while it reads and while it beams.</li>
 * </ul>
 */
public class GodOfOrderRenderer extends ZogratisRenderer<GodOfOrder> {
    /** The geo key of the God's assets: {@code GeoSpec.of(KEY, "base")} is god_of_order_base.* */
    public static final String KEY = "god_of_order";

    private static final GeoSpec BASE = GeoSpec.of(KEY, "base"), GATES = GeoSpec.of(KEY, "gates");
    private static final Clips BASE_CLIPS = new Clips(BASE), GATES_CLIPS = new Clips(GATES);
    /** Blocks of ground one cycle of the walk and of the run clip covers at scale 1 (the stride of the animation). */
    private static final float WALK_CYCLE = 1.9f, RUN_CYCLE = 3.2f;
    /** Ticks it takes to step out of the column and reach full size. */
    private static final float APPEAR_TICKS = 14f;

    /** When each body entered its current phase (client age), to pace the one-shot bow. */
    private final Map<Entity, float[]> phaseStart = new WeakHashMap<>();

    public GodOfOrderRenderer(EntityRendererProvider.Context ctx) { super(ctx, 0.7f); }

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

    @Override
    public ResourceLocation getTextureLocation(GodOfOrder e) { return BASE.texture(); }

    /** The halo and the staff reach far past the hitbox: cull by a bigger box. */
    @Override
    public boolean shouldRender(GodOfOrder e, Frustum frustum, double x, double y, double z) { return seen(e, frustum, x, y, z, 2.4, 1.6); }

    @Override
    public void render(GodOfOrder e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        ZogratisPose clock = clock(e);
        ZogratisPose.Frame f = frame(e, partial);
        int form = e.form();
        boolean transforming = e.transforming(), casting = e.state() == ZogratisBoss.STATE_CASTING, gates = form != ZogratisBoss.FORM_BASE;
        float phase = e.phaseProgress();
        GeoSpec spec = gates ? GATES : BASE;
        ZogratisPose.Clips clips = gates ? GATES_CLIPS : BASE_CLIPS;
        Move move = casting ? e.castingMoveOrNull() : null;
        boolean ultimate = move != null && move.ultimate();

        f.transforming = transforming;
        f.transformProgress = gates ? (phase - 0.5f) * 2f : phase * 2f;               // each model plays its half of the transformation
        f.cast = casting ? (ultimate ? 2 : 1) : 0;
        f.castProgress = phase;
        f.walkCycle = WALK_CYCLE;
        f.runCycle = RUN_CYCLE;
        clock.update(f, clips);

        // the visit's own clips, where the fight's pose has nothing better to say
        Phase visit = e.orderPhase();
        float[] since = phaseStart.computeIfAbsent(e, k -> new float[]{-1f, f.age});
        if (since[0] != visit.ordinal()) {
            since[0] = visit.ordinal();
            since[1] = f.age;
        }
        boolean showing = false;
        if (!f.dead && !transforming && !casting) {
            if (visit == Phase.READING && clips.length("read") > 0f) {
                clock.clip = "read";
                clock.seconds = (f.age - since[1]) / 20f;
                showing = true;
            } else if (visit == Phase.BEAM && clips.length("beam") > 0f) {
                clock.clip = "beam";
                clock.seconds = (f.age - since[1]) / 20f;
                showing = true;
            } else if (visit == Phase.BOW && clips.length("bow") > 0f && e.targetId() < 0) {
                clock.clip = "bow";
                clock.seconds = Math.min(clips.length("bow"), (f.age - since[1]) / 20f);
                showing = true;
            }
        }

        int mode = f.dead ? ZogratisGlow.NORMAL : transforming ? ZogratisGlow.TRANSFORMING : casting ? (ultimate ? ZogratisGlow.ULTIMATE : ZogratisGlow.CASTING)
                : showing && visit != Phase.BOW ? ZogratisGlow.CASTING : e.exposed() ? ZogratisGlow.EXPOSED : ZogratisGlow.NORMAL;
        float fade = f.dead ? ZogratisGlow.deathFade(f.deathTicks, DEATH_TICKS) : 1f;
        float level = ZogratisGlow.level(Math.max(e.power(), showing ? 60 : 0), f.age, mode, showing ? 1f : phase) * fade;
        int glow = ZogratisGlow.grey(level);

        float appear = Mth.clamp((e.tickCount + partial) / APPEAR_TICKS, 0.02f, 1f);
        float eased = appear * appear * (3f - 2f * appear);
        float size = clock.smoothScale(1.0f, f.age) * eased;
        boolean still = clock.speed() < ZogratisPose.WALK_ON;
        float body = Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot), head = Mth.rotLerp(partial, e.yHeadRotO, e.yHeadRot);
        float facing = clock.smoothYaw(f.dead || !(casting || still || showing) ? body : head, f.age);   // it faces what it looks at while it casts, stands or reads

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-facing));
        pose.scale(size, size, size);
        paint(pose, buffers, spec, clock.clip, clock.seconds, light, bodyRgb(e), fade, glow);
        float flash = !transforming ? 0f : ZogratisGlow.flash(phase);                   // the gold burns over the model swap, in the middle
        if (flash > 0.02f) {
            int rgb = ZogratisGlow.mix(0xE8B923, 0xFFFFFF, 0.5f);
            GeoDraw.paint(pose, buffers, spec.noGlow(), clock.clip, clock.seconds, GeoDraw.Space.PROP, GeoDraw.Layer.ADDITIVE, PropDraw.FULL_BRIGHT,
                    ZogratisGlow.withAlpha(rgb, flash * 0.85f), 0);
        }
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);                              // the name tag
    }
}
