package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.ThemeKey;
import com.newuniverse.nusmp.perf.PerfSettings;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.spiritspace.SpaceMoodRules;
import com.newuniverse.nusmp.spiritspace.SpiritSpaceLod;
import com.newuniverse.nusmp.spiritspace.SpiritSpaceMesh;
import com.newuniverse.nusmp.spiritspace.SpiritSpaceRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * Why: the entity glue of the Spirit of Space's two prop kinds (docs/magic/spirit_space.md). SPIRITSPACE_1 is the spirit: the orb (variant 0) that circles its owner and the powered wisp (variant 1)
 * above and behind; SPIRITSPACE_2 is one ring of a portal (variants 2 and 3). What is drawn is {@link SpiritSpaceDraw}'s.
 *
 * <p>Cost, all on the client: the orb and the wisp are placed from their OWNER's interpolated body each frame (the server only keeps the entity near the owner every few ticks), so nothing lags; the
 * level of detail follows the distance and the effect quality ({@link SpiritSpaceLod}), everything shares one quad budget per frame (the viewer's own close spirit is never degraded), nothing is
 * drawn past the last range (64 blocks for an orb, 112 for a wisp, 64 for a ring), nothing is allocated per frame (primitive math only), and the few star-mote particles go through the client particle
 * budget once per game tick.</p>
 */
public final class SpiritSpacePropPainter {
    private SpiritSpacePropPainter() {}

    private static final SpiritSpaceLod.QuadBudget BUDGET = new SpiritSpaceLod.QuadBudget();
    private static final int[] LAST_MOTE = new int[16];
    private static final double[] TMP = new double[3];                                          // the anchor of the form being drawn (render thread only)
    private static final Quaternionf TURN = new Quaternionf();
    private static final float[] POSE = new float[SpaceMoodRules.P_COUNT];
    private static final ThemeKey SPACE = ThemeKey.of("magic.SPATIAL");

    /** Called once by PropPainterRegistry. */
    public static void register() {
        PropPainters.register(PropKind.SPIRITSPACE_1, SpiritSpacePropPainter::spirit);
        PropPainters.register(PropKind.SPIRITSPACE_2, SpiritSpacePropPainter::portal);
    }

    private static void spirit(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        int param = e.param(), variant = SpiritSpaceRules.variant(param);
        boolean wisp = variant == SpiritSpaceRules.V_WISP;
        if (!wisp && SpiritSpaceRules.hidden(param)) return;
        Minecraft mc = Minecraft.getInstance();
        double ex = Mth.lerp(partial, e.xo, e.getX()), ey = Mth.lerp(partial, e.yo, e.getY()), ez = Mth.lerp(partial, e.zo, e.getZ());
        double ax = ex, ay = ey, az = ez;
        float yaw = Mth.rotLerp(partial, e.yRotO, e.getYRot());
        Entity o = e.target();
        int since = SpiritSpaceRules.sinceStamp(e.level().getGameTime(), SpiritSpaceRules.extra(param));
        SpaceMoodRules.Mood rawMood = SpaceMoodRules.moodOf(param);
        SpaceMoodRules.Mood mood = SpaceMoodRules.effective(rawMood, since);
        SpaceMoodRules.Expression expr = SpaceMoodRules.effective(SpaceMoodRules.expressionOf(param), rawMood, since);
        float sinceF = since == Integer.MAX_VALUE ? 1.0e6f : since + partial;
        if (o != null) {                                                                    // placed from the owner's own interpolated body, so it never lags behind them
            double ox = Mth.lerp(partial, o.xo, o.getX()), oy = Mth.lerp(partial, o.yo, o.getY()), oz = Mth.lerp(partial, o.zo, o.getZ());
            float by = o instanceof LivingEntity l ? Mth.rotLerp(partial, l.yBodyRotO, l.yBodyRot) : o.getYRot();
            if (wisp) {
                SpiritSpaceRules.wispTo(TMP, ox, oy, oz, by);
            } else {
                SpiritSpaceRules.orbitTo(TMP, ox, oy, oz, by, age);
                SpaceMoodRules.pose(POSE, mood, sinceF);                                         // the pose moves the orbit: in toward the face, low and slow when sleepy
                TMP[0] = ox + (TMP[0] - ox) * POSE[SpaceMoodRules.P_RADIUS];
                TMP[2] = oz + (TMP[2] - oz) * POSE[SpaceMoodRules.P_RADIUS];
                TMP[1] += POSE[SpaceMoodRules.P_HEIGHT];
            }
            ax = TMP[0]; ay = TMP[1]; az = TMP[2];
            yaw = by;
        }
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        double dx = ax - cam.x, dy = ay - cam.y, dz = az - cam.z, dsq = dx * dx + dy * dy + dz * dz;
        int wanted = SpiritSpaceLod.level(wisp, dsq, PerfSettings.vfxQuality().detail);
        if (wanted == SpiritSpaceLod.CULL) return;
        boolean own = o != null && o == mc.player;
        int lod = SpiritSpaceLod.fit(wisp, wanted, own, BUDGET, SpiritSpaceLod.frameKey(e.level().getGameTime(), partial));
        if (lod == SpiritSpaceLod.CULL) return;
        float fade = wisp ? Mth.clamp(Math.min((e.life() + partial) / 8f, (e.maxLife() - e.life() - partial) / 10f), 0f, 1f) : Mth.clamp((e.life() + partial) / 10f, 0f, 1f);
        if (fade <= 0.01f) return;
        pose.pushPose();
        pose.mulPose(TURN.rotationY(Mth.rotLerp(partial, e.yRotO, e.getYRot()) * Mth.DEG_TO_RAD));   // undo the renderer's turn: the offset below is in world axes
        pose.translate(ax - ex, ay - ey, az - ez);
        if (wisp) {
            pose.mulPose(TURN.rotationY(-yaw * Mth.DEG_TO_RAD));
            SpiritSpaceDraw.wisp(pose, buffers, lod, age, fade, mood, expr, sinceF);
        } else {
            float az2 = (float) (Math.atan2(cam.x - ax, cam.z - az) / (Math.PI * 2.0)), el = (float) (Math.atan2(cam.y - ay, Math.sqrt((cam.x - ax) * (cam.x - ax) + (cam.z - az) * (cam.z - az))) / (Math.PI * 2.0));
            SpiritSpaceDraw.orb(pose, buffers, lod, age, fade, mood, expr, sinceF, az2, el);
        }
        pose.popPose();
        if (lod <= SpiritSpaceLod.HALF && dsq < 24 * 24) motes(e, ax, ay, az, wisp);
    }

    /** A few star motes from the custom particle library, once per game tick per spirit, through the client particle budget. */
    private static void motes(MagicPropEntity e, double x, double y, double z, boolean wisp) {
        int slot = e.getId() & 15;
        if (LAST_MOTE[slot] == e.tickCount || e.tickCount % 5 != 0) return;
        LAST_MOTE[slot] = e.tickCount;
        Fx.send(e.level(), SPACE, x, y, z, wisp ? 2 : 1, wisp ? 0.9 : 0.35);
    }

    private static void portal(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        Minecraft mc = Minecraft.getInstance();
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        double ex = Mth.lerp(partial, e.xo, e.getX()), ey = Mth.lerp(partial, e.yo, e.getY()), ez = Mth.lerp(partial, e.zo, e.getZ());
        double dx = ex - cam.x, dy = ey - cam.y, dz = ez - cam.z;
        if (dx * dx + dy * dy + dz * dz > SpiritSpaceLod.PORTAL_RANGE * SpiritSpaceLod.PORTAL_RANGE) return;
        if (!BUDGET.take(SpiritSpaceLod.frameKey(e.level().getGameTime(), partial), SpiritSpaceMesh.portalQuads(), SpiritSpaceLod.FRAME_QUADS)) return;
        float life = e.life() + partial;
        float open = Math.min(Mth.clamp(life / 8f, 0f, 1f), Mth.clamp((e.maxLife() - life) / 10f, 0f, 1f));
        SpiritSpaceDraw.portal(pose, buffers, age, open, 1f);
    }
}
