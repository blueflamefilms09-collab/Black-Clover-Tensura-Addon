package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.client.particle.fx.FxClientLod;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.RecombAnimeRules;
import com.newuniverse.nusmp.perf.PerfProbe;
import com.newuniverse.nusmp.perf.PerfSettings;
import com.newuniverse.nusmp.prop.RecombAnimeRules.Clip;
import com.newuniverse.nusmp.vfx.client.RecombAnimeFx;
import com.newuniverse.nusmp.vfx.client.VfxShake;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;


/**
 * Recombination Magic, the anime look ({@code visual.recombAnime}, {@link RecombAnimeOptions}): how the constructs of the magic house are drawn when it is on. {@link RecombinationPropPainter} hands its
 * four painters to the ones here; with the option off it draws the older models (the four-legged bull, the oak room, the boards, the mana drill) exactly as before.
 * <ul>
 *   <li>RECOMBINATION_3, the Raging Black Bull: {@code recombanime_golem}, a giant built of houses: a plastered timber-frame belly under a slate cornice, a chest of stone and timber with arched windows, gabled
 *       shoulder roofs (a chimney on one, a tree on the other), a stone head with two glowing yellow-green slit eyes and two towers for horns, arms of stacked house segments ending in brick fists, legs of
 *       thick brick pillars on stone slabs. It rises out of the ground part by part (assemble), stands (idle), walks with its feet planted by the distance it really moved (no sliding), swings its right
 *       fist down in a blow that lands 12 ticks after the ram begins (punch), spreads its arms for a volley (fire), settles with a stomp after the rise and sinks back into the earth (collapse). The eyes and
 *       the lit windows are the glow map. Dust bursts from the ground under every footfall and the rise, the fist's landing gives the orange flash with spark lines, the chimney smokes
 *       ({@link RecombAnimeFx}; custom particles only).</li>
 *   <li>RECOMBINATION_1, a recombined room: {@code recombanime_house}, a small timber house with arched windows, a door, a tiled roof with moss, a chimney and a tree.</li>
 *   <li>RECOMBINATION_2, the ring of boards: {@code recombanime_ring}, loose building pieces (brick, beam, window, shingles, chimney pot, door, stone, tower) circling.</li>
 *   <li>RECOMBINATION_4, the Mana Corkscrew: {@code recombanime_drill}, a spire of stacked brick and tile rings with glowing seams.</li>
 * </ul>
 * Client only, nothing here decides game state: the state word the server syncs in {@code param} is read, never written. The walk state the golem needs (the phase of its stride, the last tick it
 * was looked at) is kept per prop entity id in a small table that forgets a golem that was not drawn for 400 ticks and never holds more than 48.
 */
public final class RecombAnimePainter {
    private RecombAnimePainter() {}

    private static final GeoSpec GOLEM = GeoSpec.of("recombanime", "golem"), GOLEM_LOD = GeoSpec.of("recombanime", "golem_lod"), HOUSE = GeoSpec.of("recombanime", "house"), RING = GeoSpec.of("recombanime", "ring"), DRILL = GeoSpec.of("recombanime", "drill");
    private static ResourceLocation shadow;

    private static ResourceLocation shadowTex() {
        if (shadow == null) shadow = AuraRender.tex("particle/glow");
        return shadow;
    }

    private static float smooth(float t) { t = Mth.clamp(t, 0f, 1f); return t * t * (3f - 2f * t); }

    private static void shade(PoseStack pose, MultiBufferSource buffers, float r, float alpha, int light) {
        VertexConsumer vc = buffers.getBuffer(AuraRender.translucent(shadowTex()));
        int a = Mth.clamp((int) (alpha * 255f), 0, 255) << 24;
        PropDraw.quad(pose, vc, -r, 0.02f, r, r, 0.02f, r, r, 0.02f, -r, -r, 0.02f, -r, 0f, 0f, 1f, 1f, a | 0x101008, light);
    }

    // ------------------------------------------------------------------------------------------------ per prop state (client visuals only)
    private static final class St {
        int tick = Integer.MIN_VALUE;
        double x, z;
        float phase, speed;
        int still = 99;
        long seen;
    }

    /** Keyed by the prop's int id with no boxing (fastutil): looked up once per frame per prop. */
    private static final Int2ObjectOpenHashMap<St> STATES = new Int2ObjectOpenHashMap<>();
    private static final RecombAnimeRules.FrameBudget FULL_BUDGET = new RecombAnimeRules.FrameBudget();
    private static final Quaternionf TURN = new Quaternionf();                          // scratch for the drill (render thread only): nothing is allocated per frame
    private static final PerfProbe.Section PROBE = PerfProbe.section("c.recomb.paint");

    /** Squared distance from the camera to a prop (0 when there is no camera yet): the level of detail and the culling ask it once per frame. */
    private static double camDistSq(MagicPropEntity e) {
        try {
            Camera cam = Minecraft.getInstance().gameRenderer.getMainCamera();
            if (cam == null || !cam.isInitialized()) return 0.0;
            Vec3 c = cam.getPosition();
            double dx = c.x - e.getX(), dy = c.y - e.getY(), dz = c.z - e.getZ();
            return dx * dx + dy * dy + dz * dz;
        } catch (RuntimeException ex) {
            return 0.0;
        }
    }

    private static St state(MagicPropEntity e) {
        St s = STATES.get(e.getId());
        long now = e.level().getGameTime();
        if (s == null) {
            if (STATES.size() >= RecombAnimeRules.STATE_CAP) {
                STATES.values().removeIf(old -> now - old.seen > RecombAnimeRules.STATE_STALE_TICKS);
                if (STATES.size() >= RecombAnimeRules.STATE_CAP) STATES.clear();                        // many golems at once: start the table again rather than grow it
            }
            s = new St();
            s.x = e.getX();
            s.z = e.getZ();
            STATES.put(e.getId(), s);
        }
        s.seen = now;
        return s;
    }

    // ------------------------------------------------------------------------------------------------ the golem
    public static void golem(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        long t0 = PerfProbe.begin();
        try {
            golem0(e, partial, age, pose, buffers, light);
        } finally {
            PerfProbe.end(PROBE, t0);
        }
    }

    private static void golem0(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        Level level = e.level();
        float maxL = Math.max(1f, e.maxLife());
        int p = e.param();
        long now = level.getGameTime();
        boolean big = RecombAnimeRules.ultra(p);
        float grow = big ? smooth((RecombAnimeRules.ultraSince(p, now) + partial) / 16f) : 0f;
        float sc = RecombAnimeRules.renderScale(e.scale(), grow);
        double dsq = camDistSq(e);
        int lod = RecombAnimeRules.lod(dsq, PerfSettings.auraLodNear(), PerfSettings.vfxQuality().detail, sc);
        if (lod == RecombAnimeRules.LOD_FULL && !FULL_BUDGET.take(RecombAnimeRules.frameKey(now, partial), RecombAnimeRules.FULL_GOLEMS_PER_FRAME)) lod = RecombAnimeRules.LOD_LOW;       // at most 3 full golems a frame
        St st = state(e);
        int st3 = RecombAnimeRules.state(p), since = RecombAnimeRules.since(p, now);
        int tick = e.tickCount;
        if (RecombAnimeRules.newTick(st.tick, tick)) {
            int elapsed = st.tick == Integer.MIN_VALUE ? 1 : Math.min(8, tick - st.tick);
            double dist = Math.hypot(e.getX() - st.x, e.getZ() - st.z);
            st.x = e.getX();
            st.z = e.getZ();
            st.speed = (float) Math.min(dist / Math.max(1, elapsed), 1.2);
            st.still = dist > 0.015 ? 0 : st.still + elapsed;
            float prev = st.phase;
            if (st.still < 4) st.phase = RecombAnimeRules.advancePhase(st.phase, Math.min(dist, 4.0), sc);
            st.tick = tick;
            if (dsq <= RecombAnimeRules.EVENT_RANGE_SQ) events(e, level, st, sc, tick, elapsed, st3, since, maxL, prev);       // far away nothing is spawned (the particles would be dropped anyway)
        }
        boolean moving = st.still < 4;
        float phaseNow = st.still < 4 ? RecombAnimeRules.advancePhase(st.phase, st.speed * partial, sc) : st.phase;
        Clip clip = RecombAnimeRules.clip(st3, since, age, maxL, moving);
        float secs = RecombAnimeRules.clipSeconds(clip, since + partial, age, maxL, phaseNow);
        float sinkOut = clip == Clip.COLLAPSE ? 1f - smooth((age - (maxL - RecombAnimeRules.COLLAPSE_TICKS)) / RecombAnimeRules.COLLAPSE_TICKS) : 1f;
        if (lod == RecombAnimeRules.LOD_CULL) return;                                    // too far to see: the state above still advanced, nothing is drawn
        shade(pose, buffers, 3.9f * sc * (0.4f + 0.6f * sinkOut), 0.42f * sinkOut, light);
        float eyes = (0.72f + 0.28f * Mth.sin(age * 0.2f)) * (1f + 0.5f * grow);
        if (clip == Clip.PUNCH || clip == Clip.FIRE_VOLLEY || big) eyes = 1f;
        pose.pushPose();
        pose.scale(sc, sc, sc);
        if (lod == RecombAnimeRules.LOD_FULL) GeoDraw.paint(pose, buffers, GOLEM, clip.id, secs, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF, AuraRender.alpha(0xFFFFFFFF, Math.min(1f, eyes)));
        else GeoDraw.paint(pose, buffers, GOLEM_LOD, clip.id, secs, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF, 0);                    // low poly, no glow pass
        pose.popPose();
    }

    /** The particles of one game tick (called once per tick per golem): dust under the steps and the rise, the flash of the fist, smoke from the chimney. */
    private static void events(MagicPropEntity e, Level level, St st, float sc, int tick, int elapsed, int state, int since, float maxL, float prevPhase) {
        if (!RecombAnimeOptions.particles()) return;
        double x = e.getX(), y = e.getY(), z = e.getZ();
        double yaw = e.getYRot();
        int age = tick;
        if (age < RecombAnimeRules.ASSEMBLE_TICKS) {                                                  // rising out of the ground
            if ((age & 1) == 0) RecombAnimeFx.rubble(level, x, y, z, 2.6 * sc, 6, 1.5f * sc);
        } else if (RecombAnimeRules.crossed(age, elapsed, RecombAnimeRules.ASSEMBLE_TICKS)) {
            RecombAnimeFx.dustRing(level, x, y, z, 2.2 * sc, RecombAnimeRules.slamDust(sc), 1.8f * sc);
            VfxShake.add(new Vec3(x, y, z), 0.25f * sc, 6);
        }
        if (RecombAnimeRules.crossed(age, elapsed, RecombAnimeRules.STOMP_AT_TICK) && state == 0) {      // the settling stomp (left foot)
            double[] f = RecombAnimeRules.footLanding(true, sc), w = RecombAnimeRules.toWorld(yaw, f[0], f[1] * 0.4);
            RecombAnimeFx.dustRing(level, x + w[0], y, z + w[1], 1.2 * sc, RecombAnimeRules.slamDust(sc), 1.6f * sc);
        }
        if (age > maxL - RecombAnimeRules.COLLAPSE_TICKS) {                                            // falling apart
            if ((age & 1) == 0) RecombAnimeFx.rubble(level, x, y, z, 3.0 * sc, 7, 1.6f * sc);
            return;
        }
        if (age < RecombAnimeRules.ASSEMBLE_TICKS) return;
        if (state == RecombAnimeRules.RAM && RecombAnimeRules.crossed(since, elapsed, RecombAnimeRules.PUNCH_IMPACT_TICK)) {      // the fist lands
            double[] f = RecombAnimeRules.fistLanding(sc), w = RecombAnimeRules.toWorld(yaw, f[0], f[1]);
            RecombAnimeFx.fistImpact(level, x + w[0], y + f[2], z + w[1], sc, RecombAnimeRules.slamSparks(sc), RecombAnimeRules.slamDust(sc), RecombAnimeRules.slamChips(sc));
            VfxShake.add(new Vec3(x + w[0], y, z + w[1]), 0.45f * sc, 8);
        }
        if (state == RecombAnimeRules.FIRE && RecombAnimeRules.crossed(since, elapsed, 3)) {
            double[] c = RecombAnimeRules.chimneyTop(sc), w = RecombAnimeRules.toWorld(yaw, c[0], c[2]);
            for (int i = 0; i < 3; i++) RecombAnimeFx.chimneySmoke(level, x + w[0], y + c[1], z + w[1], 1.6f * sc);
        }
        int fall = RecombAnimeRules.footfalls(prevPhase, st.phase);
        if (fall != 0 && st.still < 4) {
            for (int b = 0; b < 2; b++) {
                if ((fall & (1 << b)) == 0) continue;
                double[] f = RecombAnimeRules.footLanding(b == 0, sc), w = RecombAnimeRules.toWorld(yaw, f[0], f[1]);
                RecombAnimeFx.footfall(level, x + w[0], y, z + w[1], sc, RecombAnimeRules.footfallDust(sc));
            }
            VfxShake.add(new Vec3(x, y, z), 0.06f * sc, 3);
        }
        if ((tick + e.seed()) % RecombAnimeRules.chimneyPuffEvery() == 0) {
            double[] c = RecombAnimeRules.chimneyTop(sc), w = RecombAnimeRules.toWorld(yaw, c[0], c[2]);
            RecombAnimeFx.chimneySmoke(level, x + w[0], y + c[1], z + w[1], sc);
        }
    }

    // ------------------------------------------------------------------------------------------------ the house
    public static void house(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        float s = Math.max(0.3f, e.scale()), maxL = Math.max(1f, e.maxLife());
        boolean far = RecombAnimeRules.cullSmall(camDistSq(e), PerfSettings.auraLodFar(), PerfSettings.vfxQuality().detail);
        boolean swap = e.param() == 1;
        String clip = age < 10f ? (swap ? "swap" : "rise") : "idle";
        float sink = smooth((maxL - age) / 8f);
        if (sink <= 0.01f) return;
        St st = state(e);
        if (RecombAnimeRules.newTick(st.tick, e.tickCount)) {
            int elapsed = st.tick == Integer.MIN_VALUE ? 1 : Math.min(8, e.tickCount - st.tick);
            st.tick = e.tickCount;
            if (RecombAnimeOptions.particles()) {
                if (RecombAnimeRules.crossed(e.tickCount, elapsed, 1)) RecombAnimeFx.houseDust(e.level(), e.getX(), e.getY(), e.getZ(), 1.0 * s, 1.0f * s);
                if ((e.tickCount + e.seed()) % 12 == 0) RecombAnimeFx.chimneySmoke(e.level(), e.getX() + 0.45 * s, e.getY() + 2.5 * s, e.getZ(), 0.8f * s);
            }
        }
        if (far) return;
        float k = 1.25f * s;
        shade(pose, buffers, 1.2f * k * sink, 0.3f * sink, light);
        pose.pushPose();
        pose.translate(0f, -(1f - sink) * 1.6f * k, 0f);
        pose.scale(k, k, k);
        GeoDraw.paint(pose, buffers, HOUSE, clip, age / 20f, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF, AuraRender.alpha(0xFFFFFFFF, (0.75f + 0.25f * Mth.sin(age * 0.15f)) * sink));
        pose.popPose();
    }

    // ------------------------------------------------------------------------------------------------ the ring of building pieces
    public static void ring(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        float s = Math.max(0.4f, e.scale()), maxL = Math.max(1f, e.maxLife());
        float k = smooth(age / 6f) * smooth((maxL - age) / 8f);
        if (k <= 0.01f || RecombAnimeRules.cullSmall(camDistSq(e), PerfSettings.auraLodFar(), PerfSettings.vfxQuality().detail)) return;
        pose.pushPose();
        pose.translate(0f, -1.4f * s * 0.65f + 0.2f * Mth.sin(age * 0.2f), 0f);
        float sc = s * 0.65f;
        pose.scale(sc * (0.7f + 0.3f * k), sc * k, sc * (0.7f + 0.3f * k));
        GeoDraw.paint(pose, buffers, RING, "idle", age / 20f * 1.8f, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, AuraRender.alpha(0xFFFFFFFF, 1f), AuraRender.alpha(0xFFFFFFFF, k * 0.8f));
        pose.popPose();
    }

    // ------------------------------------------------------------------------------------------------ the drill
    public static void drill(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        float maxL = Math.max(1f, e.maxLife());
        float k = smooth(age / 3f) * smooth((maxL - age) / 3f);
        if (k <= 0.01f || RecombAnimeRules.cullSmall(camDistSq(e), PerfSettings.auraLodFar(), PerfSettings.vfxQuality().detail)) return;
        double vx = e.getX() - e.xo, vy = e.getY() - e.yo, vz = e.getZ() - e.zo;
        pose.pushPose();
        pose.translate(0f, 0.3f, 0f);
        if (vx * vx + vy * vy + vz * vz > 1.0e-4) pose.mulPose(TURN.rotationTo(0f, 1f, 0f, (float) vx, (float) vy, (float) vz));
        pose.translate(0f, -0.4f, 0f);
        float s = 0.9f * k;
        pose.scale(s, s, s);
        GeoDraw.paint(pose, buffers, DRILL, "idle", age / 20f, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF, AuraRender.alpha(0xFFFFFFFF, k));
        pose.popPose();
        St st = state(e);
        if (RecombAnimeRules.newTick(st.tick, e.tickCount)) {
            st.tick = e.tickCount;
            if (RecombAnimeOptions.particles() && (e.tickCount & 1) == 0) RecombAnimeFx.rubble(e.level(), e.getX(), e.getY() - 0.2, e.getZ(), 0.3, 2, 0.4f);
        }
    }
}
