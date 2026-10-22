package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.gnome.GnomeLod;
import com.newuniverse.nusmp.gnome.GnomeRules;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.ThemeKey;
import com.newuniverse.nusmp.perf.PerfSettings;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the entity glue of the Gnome prop (docs/magic/gnome.md). GNOME_1 is the stone COMPANION that walks beside its master (variant 0) and the full-size stone GIANT that stands ahead of them while a big
 * earth page is cast (variant 1). What is drawn is {@link GnomeDraw}'s (real models from tools/gen_gnome_geo.py: hand-painted sheets, three levels of detail each, a glow-only model).
 *
 * <p>Cost, all on the client: the companion is placed from its OWNER's interpolated body each frame (the server only keeps the entity near the owner every few ticks), so nothing lags; the level of detail
 * follows the distance and the effect quality ({@link GnomeLod}), all Gnomes share one quad budget per frame (the viewer's own close companion is never dropped), nothing is drawn past the last range
 * (64 blocks for a companion, 112 for the giant), nothing is allocated per frame (primitive math only, static scratch), the limbs move by bone rotation only (the clips), and the pebbles and dust go through
 * the client particle budget, a few per game tick per Gnome at most.</p>
 */
public final class GnomePropPainter {
    private GnomePropPainter() {}

    private static final GnomeLod.QuadBudget BUDGET = new GnomeLod.QuadBudget();
    private static final int[] LAST_DUST = new int[16];
    private static final double[] TMP = new double[3];                                                  // the anchor of the form being drawn (render thread only)
    private static final ThemeKey EARTH = ThemeKey.of("magic.EARTH");

    /** Called once by PropPainterRegistry. */
    public static void register() {
        PropPainters.register(PropKind.GNOME_1, GnomePropPainter::paint);
    }

    private static void paint(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        int param = e.param(), variant = GnomeRules.variant(param);
        boolean full = variant == GnomeRules.V_FULL;
        if (!full && GnomeRules.hidden(param)) return;
        Minecraft mc = Minecraft.getInstance();
        double ex = Mth.lerp(partial, e.xo, e.getX()), ey = Mth.lerp(partial, e.yo, e.getY()), ez = Mth.lerp(partial, e.zo, e.getZ());
        double ax = ex, ay = ey, az = ez;
        float yaw = Mth.rotLerp(partial, e.yRotO, e.getYRot());
        Entity o = e.target();
        boolean moving = false, mine = false;
        if (!full && o != null) {                                                                  // the companion is placed from the owner's own interpolated body, so it never lags behind them
            double ox = Mth.lerp(partial, o.xo, o.getX()), oy = Mth.lerp(partial, o.yo, o.getY()), oz = Mth.lerp(partial, o.zo, o.getZ());
            float by = o instanceof LivingEntity l ? Mth.rotLerp(partial, l.yBodyRotO, l.yBodyRot) : o.getYRot();
            GnomeRules.companionTo(TMP, ox, oy, oz, by);
            ax = TMP[0]; ay = TMP[1] + 0.04 * Mth.sin(age * 0.11f); az = TMP[2];                   // it hovers a hand above the ground and bobs a little
            yaw = by;
            moving = o.getDeltaMovement().horizontalDistanceSqr() > 0.0004 || (o instanceof LivingEntity ll && ll.walkAnimation.speed() > 0.05f);
            mine = o == mc.player;
        }
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        double dx = ax - cam.x, dy = ay - cam.y, dz = az - cam.z, dsq = dx * dx + dy * dy + dz * dz;
        int form = full ? GnomeLod.FULL : GnomeLod.COMPANION;
        int wanted = GnomeLod.level(form, dsq, PerfSettings.vfxQuality().detail);
        if (wanted == GnomeLod.CULL) return;
        int lod = GnomeLod.fit(form, wanted, BUDGET, GnomeLod.frameKey(e.level().getGameTime(), partial), mine);
        if (lod == GnomeLod.CULL) return;

        long now = e.level().getGameTime();
        int stamp = GnomeRules.stamp(param);
        int since = GnomeRules.sinceStamp(now, stamp);
        int pose0 = GnomeRules.pose(param);
        int clip;
        float secs;
        float fade;
        if (full) {
            int life = e.life();
            clip = GnomeRules.fullClip(life, e.maxLife(), pose0, since);
            int into = clip == GnomeRules.CLIP_RISE ? life : clip == GnomeRules.CLIP_SINK ? life - (e.maxLife() - GnomeRules.FULL_SINK_TICKS)
                    : clip == GnomeRules.CLIP_STOMP && pose0 != GnomeRules.CLIP_STOMP ? life - GnomeRules.STOMP_AT : since;
            secs = GnomeRules.persistent(clip) ? age / 20f : GnomeRules.secs(Math.max(0, into), partial);
            fade = GnomeRules.fullFade(life, e.maxLife(), partial);
            dust(e, clip, into, ex, ey, ez, yaw, lod);
        } else {
            clip = GnomeRules.companionClip(moving, pose0, since);
            boolean posed = clip != GnomeRules.CLIP_IDLE && clip != GnomeRules.CLIP_WALK && !GnomeRules.persistent(clip);       // a one-shot pose plays from the moment of its event
            secs = posed ? GnomeRules.secs(since, partial) : age / 20f;
            fade = Mth.clamp((e.life() + partial) / 8f, 0f, 1f);
            if (lod == GnomeLod.LOD_FULL && dsq < 20 * 20 && posed && GnomeRules.slamNow(clip, since)) pebbles(e, ax, ay + 0.15, az, 5);
            else if (lod == GnomeLod.LOD_FULL && dsq < 16 * 16 && clip == GnomeRules.CLIP_IDLE && e.tickCount % 40 == 0) pebbles(e, ax, ay + 0.3, az, 1);
        }
        int face = GnomeRules.faceNow(GnomeRules.face(param), GnomeRules.persistent(pose0) && clip != pose0 ? GnomeRules.CLIP_IDLE : pose0, since);      // a doze that the walk ended is not sleepy any more
        if (fade <= 0.01f) return;
        pose.pushPose();
        if (!full) {
            pose.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(partial, e.yRotO, e.getYRot())));     // undo the renderer's turn: the offset below is in world axes
            pose.translate(ax - ex, ay - ey, az - ez);
            pose.mulPose(Axis.YP.rotationDegrees(-yaw));
        }
        GnomeDraw.gnome(pose, buffers, form, lod, GnomeDraw.CLIPS[clip], secs, age, fade, light, face);
        pose.popPose();
    }

    /** The dust of the giant: a ring when it breaks the ground, rubble while it rises, a burst when its fists land. Custom particles, once per game tick per giant, through the client budget. */
    private static void dust(MagicPropEntity e, int clip, int into, double x, double y, double z, float yaw, int lod) {
        if (lod > GnomeLod.LOD_HALF) return;
        int slot = e.getId() & 15, key = e.tickCount * 16 + clip;
        if (LAST_DUST[slot] == key) return;
        if (clip == GnomeRules.CLIP_RISE && into < GnomeRules.FULL_SLAM_TICK && e.tickCount % 2 == 0) {
            LAST_DUST[slot] = key;
            Fx.send(e.level(), EARTH, x, y + 0.2, z, 3, 1.6);
        } else if (GnomeRules.slamNow(clip, into)) {
            LAST_DUST[slot] = key;
            double[] h = GnomeRules.fullHand(x, y, z, yaw);
            Fx.send(e.level(), EARTH, h[0], y + 0.15, h[2], 10, 1.8);
            Fx.ring(e.level(), EARTH, x, y + 0.1, z, 3.2, 14);
        } else if (clip == GnomeRules.CLIP_SINK && e.tickCount % 3 == 0) {
            LAST_DUST[slot] = key;
            Fx.send(e.level(), EARTH, x, y + 0.2, z, 2, 1.4);
        }
    }

    /** A few pebbles from the companion's feet (once per tick at most per companion). */
    private static void pebbles(MagicPropEntity e, double x, double y, double z, int n) {
        int slot = e.getId() & 15, key = e.tickCount * 16 + 15;
        if (LAST_DUST[slot] == key) return;
        LAST_DUST[slot] = key;
        Fx.send(e.level(), EARTH, x, y, z, n, 0.35);
    }
}
