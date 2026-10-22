package com.newuniverse.nusmp.client.compass;

import com.newuniverse.nusmp.compass3d.CompassLod;
import com.newuniverse.nusmp.perf.PerfBudgets;
import com.newuniverse.nusmp.perf.PerfSettings;
import com.newuniverse.nusmp.perf.VfxQuality;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Vector3f;

/**
 * Why: the client half of the compass level of detail (the rules are the pure {@link CompassLod}). Once per frame, at the start of the level render, it reads the camera (position, look
 * direction) and the effect quality, rolls the quad budget of the frame ({@link PerfBudgets#COMPASS_QUADS}, keyed by a frame counter, not the game tick) and forgets the choices of the last
 * frame. During the frame every compass, arrow-needle and route asks {@link #level} once; the answer is remembered by entity id, so a second pass over the same prop (a shadow pass of a shader
 * pack, the glass pass) gets the same level and costs the budget once. Render thread only; no allocation per call (the cost tables are built once).
 * <p>
 * If the frame event is not seen (a renderer replacement that does not send it) the budget is switched off after half a second and only the distance levels and the behind-the-camera cull
 * remain, so the compass can never vanish for good.
 */
public final class CompassFrame {
    private CompassFrame() {}

    /** The cost tables of {@link CompassLod#admit}, built once. */
    private static final int[] COST_MAIN = CompassLod.compassCost(true, true), COST_SMALL = CompassLod.compassCost(true, false), COST_ARROW = CompassLod.arrowCost();
    private static final int[][] COST_ROUTE = new int[16][];

    private static final long STALE_NANOS = 500_000_000L;
    private static boolean registered;
    private static long frame, lastBegin;
    private static double camX, camY, camZ;
    private static float lookX, lookY, lookZ = -1f;
    private static float detail = 1f;
    private static final Int2IntOpenHashMap CHOSEN = new Int2IntOpenHashMap();

    /** Called once from {@code CompassPropPainter.register}. */
    public static synchronized void init() {
        if (registered) return;
        registered = true;
        NeoForge.EVENT_BUS.addListener(CompassFrame::onStage);
    }

    private static void onStage(RenderLevelStageEvent e) {
        if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) begin(e.getCamera());
    }

    /** Starts a frame: the camera, the quality, a fresh budget. */
    static void begin(Camera cam) {
        Vec3 pos = cam.getPosition();
        Vector3f look = cam.getLookVector();
        camX = pos.x;
        camY = pos.y;
        camZ = pos.z;
        lookX = look.x;
        lookY = look.y;
        lookZ = look.z;
        VfxQuality q = PerfSettings.vfxQuality();
        detail = q.detail;
        PerfBudgets.COMPASS_QUADS.setCap(CompassLod.frameQuads(q));
        CHOSEN.clear();
        frame++;
        lastBegin = System.nanoTime();
    }

    public static float detail() { return detail; }

    /** The level of a compass prop of the caster (rim, dial, rose, needle, glass). */
    public static int main(Entity e) { return level(e, CompassLod.COMPASS, COST_MAIN, true); }

    /** The level of a small compass (a seal disc: no glass; a turn disc keeps its glass but is small enough for the same table). */
    public static int small(Entity e) { return level(e, CompassLod.COMPASS, COST_SMALL, true); }

    /** The level of an arrow-needle. */
    public static int arrow(Entity e) { return level(e, CompassLod.ARROW, COST_ARROW, true); }

    /** The level of a route of {@code dashes} dashes (the position of the prop is where the route starts; a route is never culled for lying behind the camera: its far end may be in view). */
    public static int route(Entity e, int dashes) {
        int n = Math.max(2, Math.min(15, dashes));
        int[] cost = COST_ROUTE[n];
        if (cost == null) cost = COST_ROUTE[n] = CompassLod.routeCost(n);
        return level(e, CompassLod.ROUTE, cost, false);
    }

    private static int level(Entity e, double[] bands, int[] cost, boolean cullBehind) {
        boolean live = System.nanoTime() - lastBegin < STALE_NANOS;          // false: no frame event reaches us (see the class comment): distance and the behind-the-camera cull only
        int id = e.getId();
        if (live) {
            int chosen = CHOSEN.getOrDefault(id, -1);
            if (chosen >= 0) return chosen;
        }
        double dx = e.getX() - camX, dy = e.getY() - camY, dz = e.getZ() - camZ;
        double distSq = dx * dx + dy * dy + dz * dz;
        int lod;
        if (cullBehind && CompassLod.behind(dx * lookX + dy * lookY + dz * lookZ) && distSq > CompassLod.KEEP_RADIUS * CompassLod.KEEP_RADIUS) {
            lod = CompassLod.CULLED;
        } else {
            lod = CompassLod.level(distSq, detail, bands);
            if (live && lod < CompassLod.CULLED) lod = CompassLod.admit(PerfBudgets.COMPASS_QUADS, frame, lod, distSq, cost);
        }
        if (live) CHOSEN.put(id, lod);
        return lod;
    }
}
