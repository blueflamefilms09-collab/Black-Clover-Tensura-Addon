package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.compass.CompassFrame;
import com.newuniverse.nusmp.client.compass.CompassRender;
import com.newuniverse.nusmp.compass3d.CompassLod;
import com.newuniverse.nusmp.compass3d.CompassRules;
import com.newuniverse.nusmp.compass3d.CompassVisualConfig;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Why: how the 3D Compass Magic looks (docs/magic/compass_3d.md; the server side is compass3d.CompassKit, the geometry compass3d.CompassMesh, previewed by tools/compass/preview_compass3d.py).
 * <ul>
 *   <li>COMPASS_1: the caster's ONE compass: a thick olive-bronze rim, a green dial, a raised cream and gold 8 point rose, a gold needle with a ring at its tail, a glass dome, a spiky yellow aura
 *       behind and a glint running round the rim. It pops in, bobs and sways gently, and fades out. The needle is a spring: it swings to the charted foe (or to where the caster looks) with a little
 *       overshoot. Guard mode (Willful Compass) adds a turning field ring round it, chart mode (Another Atlas) a brighter aura and a slow outer ring.</li>
 *   <li>COMPASS_2 variant 0: an arrow-needle (gold shaft, four fins, a ring at the tail) riding the page's bolt, rolling about its axis, with a glow trail; it grows out of the compass over the first ticks.
 *       Variant 1: a seal disc, a small compass lying flat, flying out of the big one to a foe's feet and then turning there with a seal ring card. Variant 2: a small turn disc facing the camera, its
 *       needle swinging at the shooter whose shot was bent back. Variant 3: a route of marching 3D dashes and arrow heads from the compass to the charted foe.</li>
 * </ul>
 * Everything is skipped when {@code visual.compass3d} is off (the old flat effects show instead). Pass order: solid model passes first, then the glass, then the additive passes.
 * <p>
 * 1.112.0 (lag audit, docs/perf/lag_audit_1.md): every prop asks {@link CompassFrame} for a level of detail first (by distance from the camera, culled when wholly behind it, charged to a
 * per-frame quad budget), and the per-frame vectors of the old code are plain doubles now.
 */
public final class CompassPropPainter {
    private CompassPropPainter() {}

    /** The client-side spring of one compass's needle (angle and angular speed in degrees); weak so a removed prop frees it. */
    private static final class Needle { float angle, vel, last = Float.NaN; boolean init; }
    private static final Map<Entity, Needle> NEEDLES = new WeakHashMap<>();

    /** Called once on the client by PropPainterRegistry. */
    public static void register() {
        CompassFrame.init();
        PropPainters.register(PropKind.COMPASS_1, CompassPropPainter::main);
        PropPainters.register(PropKind.COMPASS_2, CompassPropPainter::spell);
    }

    // ================================================================ the caster's compass
    /** 1.112.0 (lag audit): the cost of painting the compasses and what comes out of them, one section (off by default: a flag read; {@code /nusmp perf on}). */
    private static final com.newuniverse.nusmp.perf.PerfProbe.Section PROBE_PAINT = com.newuniverse.nusmp.perf.PerfProbe.section("c.compass.paint");

    private static void main(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, int light) {
        long t0 = com.newuniverse.nusmp.perf.PerfProbe.begin();
        try {
            main0(e, partial, age, pose, buf, light);
        } finally {
            com.newuniverse.nusmp.perf.PerfProbe.end(PROBE_PAINT, t0);
        }
    }

    private static void main0(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, int light) {
        if (!CompassVisualConfig.compass3d()) return;
        int word = e.param();
        float k = CompassRules.opacity(age, word, partial);
        if (k <= 0.01f) return;
        int lod = CompassFrame.main(e);
        if (lod >= CompassLod.CULLED) return;                                                  // too far, behind the camera, or the frame's quad budget is spent on nearer ones
        float pop = CompassRules.popIn(age, CompassRules.FADE_IN);
        float s = e.scale() * Math.max(0.05f, pop) * (0.8f + 0.2f * k);
        float bob = 0.07f * Mth.sin(age * 0.09f), sway = 11f * Mth.sin(age * 0.031f), roll = 2.5f * Mth.sin(age * 0.05f + 1f);
        int mode = CompassRules.mode(word);
        pose.pushPose();
        pose.translate(0f, bob, 0f);
        pose.mulPose(Axis.YP.rotationDegrees(sway));
        pose.mulPose(Axis.ZP.rotationDegrees(roll));
        // the needle: toward the charted foe, else toward where the caster looks, a spring with a little overshoot
        float yaw = Mth.rotLerp(partial, e.yRotO, e.getYRot());
        float angle = lod >= CompassLod.LOW ? 0f : spring(e, needleGoal(e, word, partial, bob, yaw - sway, age), age);        // the far compass has no needle to swing
        pose.scale(s, s, s);
        SealDraw.camera(pose);
        float guard = mode == CompassRules.MODE_GUARD ? 1f : 0f, chart = mode == CompassRules.MODE_CHART ? 1f : 0f, attack = mode == CompassRules.MODE_ATTACK ? 1f : 0f;
        float glow = 1f + 0.35f * attack + 0.25f * chart + 0.3f * guard + 0.12f * Mth.sin(age * 0.2f);
        CompassRender.compass(pose, buf, light, angle, k, glow, true, true, lod);
        if (CompassVisualConfig.aura()) {
            CompassRender.aura(pose, buf, age, k, 1f + 0.35f * attack + 0.5f * chart + 0.25f * guard);
            if (lod == CompassLod.FULL) CompassRender.glint(pose, buf, age, k);
        }
        if (guard > 0f) {                                                                      // Willful Compass: a turning field ring round the compass
            pose.pushPose();
            pose.translate(0f, 0f, 0.02f);
            CompassRender.ring(pose, buf, 1.55f, age * 0.03f, 0.55f * k * (0.8f + 0.2f * Mth.sin(age * 0.3f)), CompassRender.GOLD);
            CompassRender.ring(pose, buf, 1.2f, -age * 0.045f, 0.3f * k, CompassRender.PALE);
            pose.popPose();
        } else if (chart > 0f) {                                                               // Another Atlas: a slow outer chart ring
            pose.pushPose();
            pose.translate(0f, 0f, -0.05f);
            CompassRender.ring(pose, buf, 1.45f, age * 0.02f, 0.4f * k, CompassRender.GOLD);
            pose.popPose();
        }
        pose.popPose();
    }

    /** The needle's goal angle (degrees about the model's z axis): at the target if there is one, else along the caster's look; with a faint idle wander. */
    private static float needleGoal(MagicPropEntity e, int word, float partial, float bob, float worldYaw, float age) {
        double dx, dy, dz;
        Entity t = e.target();
        if (t != null) {                                                                       // plain doubles: the positions were two Vec3 per compass per frame
            dx = Mth.lerp(partial, t.xo, t.getX()) - Mth.lerp(partial, e.xo, e.getX());
            dy = Mth.lerp(partial, t.yo, t.getY()) + t.getBbHeight() * 0.5 - Mth.lerp(partial, e.yo, e.getY());
            dz = Mth.lerp(partial, t.zo, t.getZ()) - Mth.lerp(partial, e.zo, e.getZ());
        } else {
            double yw = Math.toRadians(CompassRules.aimYaw(word)), pt = Math.toRadians(CompassRules.aimPitch(word));
            dx = -Math.sin(yw) * Math.cos(pt); dy = -Math.sin(pt); dz = Math.cos(yw) * Math.cos(pt);
        }
        float goal = CompassRules.needleAngle(dx, dy, dz, worldYaw);
        return goal + (t == null ? 7f * Mth.sin(age * 0.07f) : 1.2f * Mth.sin(age * 0.11f));
    }

    /** A damped spring toward 'goal' (degrees): stiff enough to settle in about a second, soft enough to overshoot once. */
    private static float spring(Entity key, float goal, float age) {
        Needle n = NEEDLES.computeIfAbsent(key, x -> new Needle());
        if (!n.init) { n.init = true; n.angle = goal + 160f; n.vel = 0f; n.last = age; }      // it first swings round from behind, the way a compass settles after being lifted
        float dt = Mth.clamp(age - n.last, 0f, 2.5f);
        n.last = age;
        float d = ((goal - n.angle) % 360f + 540f) % 360f - 180f;
        n.vel += (0.34f * d - 0.26f * n.vel) * dt;
        n.angle += n.vel * dt;
        return n.angle;
    }

    // ================================================================ what comes out of it
    private static void spell(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, int light) {
        long t0 = com.newuniverse.nusmp.perf.PerfProbe.begin();
        try {
            spell0(e, partial, age, pose, buf, light);
        } finally {
            com.newuniverse.nusmp.perf.PerfProbe.end(PROBE_PAINT, t0);
        }
    }

    private static void spell0(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, int light) {
        if (!CompassVisualConfig.compass3d()) return;
        switch (CompassRules.variant(e.param())) {
            case CompassRules.V_NEEDLE -> arrow(e, partial, age, pose, buf, light);
            case CompassRules.V_SEAL -> seal(e, partial, age, pose, buf, light);
            case CompassRules.V_TURN -> turn(e, partial, age, pose, buf, light);
            case CompassRules.V_ROUTE -> route(e, partial, age, pose, buf, light);
            default -> { }
        }
    }

    private static float fadeOut(MagicPropEntity e, float partial, float ticks) {
        return Mth.clamp((e.maxLife() - (e.life() + partial)) / ticks, 0f, 1f);
    }

    /** The arrow-needle: the model flies along the bolt, rolling, growing out of the compass in the first ticks, with a streak of light behind. */
    private static void arrow(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, int light) {
        float k = fadeOut(e, partial, 2f) * Mth.clamp(age / 2f, 0f, 1f);
        if (k <= 0.01f) return;
        int lod = CompassFrame.arrow(e);
        if (lod >= CompassLod.CULLED) return;
        float grow = CompassRules.popIn(age, 5f);
        float s = 0.5f * e.scale() * Math.max(0.1f, grow);
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partial, e.xRotO, e.getXRot())));
        SealDraw.camera(pose);
        CompassRender.trail(pose, buf, 0f, 0f, -2.6f * s, 0f, 0f, 0.1f * s, 0.2f * s, 0.55f * k, CompassRender.GOLD);
        CompassRender.softGlow(pose, buf, 0f, 0f, 0.7f * s, 0.55f * s, 0.55f * k, CompassRender.GOLD);
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(age * 16f));
        pose.scale(s, s, s);
        CompassRender.arrow(pose, buf, light, k, 1f, lod);
        pose.popPose();
        pose.popPose();
    }

    /** The seal disc: flies out tilted, lands flat, turns with its needle spinning, a seal ring card on the ground; shrinks away at the end. */
    private static void seal(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, int light) {
        float life = e.life() + partial;
        float in = CompassRules.popIn(life, 6f), out = fadeOut(e, partial, 8f);
        float k = Mth.clamp(life / 3f, 0f, 1f) * out;
        if (k <= 0.01f) return;
        int lod = CompassFrame.small(e);
        if (lod >= CompassLod.CULLED) return;
        float landed = Mth.clamp((life - CompassRules.SEAL_FLIGHT + 2f) / 4f, 0f, 1f);
        float s = 0.5f * e.scale() * Math.max(0.1f, in) * (0.7f + 0.3f * out);
        pose.pushPose();
        pose.translate(0f, 0.04f, 0f);
        SealDraw.camera(pose);
        // the ring card on the ground (once it has landed)
        if (landed > 0f) {
            pose.pushPose();
            pose.mulPose(Axis.XP.rotationDegrees(-90f));
            CompassRender.ring(pose, buf, 1.15f * (0.7f + 0.3f * landed), -age * 0.05f, 0.75f * k * landed, CompassRender.GOLD);
            CompassRender.ring(pose, buf, 0.8f, age * 0.08f, 0.35f * k * landed, CompassRender.PALE);
            pose.popPose();
        }
        pose.translate(0f, 0.27f * s, 0f);                                              // the disc's underside (model z = -0.26) rests on the ground
        float tilt = (1f - landed) * 55f;                                                // in flight it leans, tumbling a little
        pose.mulPose(Axis.XP.rotationDegrees(-90f + tilt));
        pose.mulPose(Axis.ZP.rotationDegrees(age * 5f + CompassRules.index(e.param()) * 40f));
        pose.scale(s, s, s);
        CompassRender.compass(pose, buf, light, age * 11f, k, 1.3f, true, false, lod);
        pose.popPose();
    }

    /** The turn disc: a small compass facing the camera where a shot was bent back; its needle swings at the shooter, a ring of light pulses out of it. */
    private static void turn(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, int light) {
        float life = e.life() + partial;
        float in = CompassRules.popIn(life, 4f), out = fadeOut(e, partial, 6f);
        float k = Mth.clamp(life / 2f, 0f, 1f) * out;
        if (k <= 0.01f) return;
        int lod = CompassFrame.small(e);
        if (lod >= CompassLod.CULLED) return;
        float s = 0.42f * e.scale() * 2f * Math.max(0.1f, in);
        pose.pushPose();
        pose.translate(0f, 0.0f, 0f);
        SealDraw.camera(pose);
        float theta = (float) Math.atan2(-SealDraw.CAM[6], -SealDraw.CAM[8]);          // the face turns to the camera about the vertical axis
        pose.mulPose(Axis.YP.rotation(theta));
        Entity t = e.target();
        float want = 0f;
        if (t != null) {
            double dx = Mth.lerp(partial, t.xo, t.getX()) - Mth.lerp(partial, e.xo, e.getX());
            double dy = Mth.lerp(partial, t.yo, t.getY()) + t.getBbHeight() * 0.5 - Mth.lerp(partial, e.yo, e.getY());
            double dz = Mth.lerp(partial, t.zo, t.getZ()) - Mth.lerp(partial, e.zo, e.getZ());
            want = CompassRules.needleAngle(dx, dy, dz, (float) -Math.toDegrees(theta));
        }
        float angle = CompassRules.needleSwing(want + 150f, want, life);
        pose.scale(s, s, s);
        SealDraw.camera(pose);
        CompassRender.compass(pose, buf, light, angle, k, 1.4f, true, true, lod);
        float p = Mth.clamp(life / 12f, 0f, 1f);
        CompassRender.ring(pose, buf, 1.1f + 1.3f * p, life * 0.2f, 0.6f * k * (1f - p), CompassRender.GOLD);
        CompassRender.softGlow(pose, buf, 0f, 0f, 0.1f, 1.6f, 0.4f * k * (1f - 0.5f * p), CompassRender.GOLD);
        pose.popPose();
    }

    /** The route: marching dashes and arrow heads (real 3D pieces) from the compass toward the charted foe, with a faint line of light under them. */
    private static void route(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, int light) {
        Entity t = e.target();
        if (t == null) return;
        float life = e.life() + partial;
        float k = Mth.clamp(life / 8f, 0f, 1f) * fadeOut(e, partial, 12f);
        if (k <= 0.01f) return;
        double fx = Mth.lerp(partial, e.xo, e.getX()), fy = Mth.lerp(partial, e.yo, e.getY()), fz = Mth.lerp(partial, e.zo, e.getZ());
        double dx = Mth.lerp(partial, t.xo, t.getX()) - fx, dy = Mth.lerp(partial, t.yo, t.getY()) + t.getBbHeight() * 0.5 - fy, dz = Mth.lerp(partial, t.zo, t.getZ()) - fz;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.6) return;
        double ux = dx / len, uy = dy / len, uz = dz / len;
        double span = len - 1.4;                                                          // it leaves the compass's edge (0.9 out) and stops short of the foe (0.5 before it)
        if (span < 0.6) return;
        int n = CompassRules.dashes(span);
        int lod = CompassFrame.route(e, n);
        if (lod >= CompassLod.CULLED) return;
        float yaw = (float) Math.toDegrees(Math.atan2(ux, uz)), pitch = (float) -Math.toDegrees(Math.asin(Mth.clamp((float) uy, -1f, 1f)));
        SealDraw.camera(pose);
        double ax = ux * 0.9, ay = uy * 0.9, az = uz * 0.9;                                 // where the line starts, from the compass
        CompassRender.trail(pose, buf, (float) ax, (float) ay, (float) az, (float) (ax + ux * span), (float) (ay + uy * span), (float) (az + uz * span), 0.05f, 0.22f * k, CompassRender.GOLD);
        int shown = CompassLod.routeDashes(n, lod);
        int step = shown >= n || shown <= 0 ? 1 : Math.max(1, n / shown);                    // a lower level draws every second dash
        boolean twin = lod == CompassLod.FULL;
        for (int i = 0, drawn = 0; i < n && drawn < shown; i += step, drawn++) {
            float f0 = CompassRules.dashStart(i, n, life), f1 = CompassRules.dashEnd(i, n, life);
            float edge = Math.min(Mth.clamp(f0 * 6f, 0f, 1f), Mth.clamp((1f - f1) * 6f + 0.3f, 0f, 1f));      // dashes fade in at the compass and out near the foe
            double seg = span * (f1 - f0);
            if (seg < 0.05) continue;
            double off = 0.9 + span * f0;
            pose.pushPose();
            pose.translate(ux * off, uy * off, uz * off);
            pose.mulPose(Axis.YP.rotationDegrees(yaw));
            pose.mulPose(Axis.XP.rotationDegrees(pitch));
            pose.pushPose();
            pose.scale(0.07f, 0.07f, (float) seg);
            CompassRender.routePiece(pose, buf, false, k * edge, twin);
            pose.popPose();
            pose.translate(0f, 0f, (float) seg);
            pose.scale(0.17f, 0.17f, 0.26f);
            CompassRender.routePiece(pose, buf, true, k * edge, twin);
            pose.popPose();
        }
    }
}
