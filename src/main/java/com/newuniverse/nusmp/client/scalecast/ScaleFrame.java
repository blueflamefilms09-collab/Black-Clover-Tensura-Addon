package com.newuniverse.nusmp.client.scalecast;

import com.newuniverse.nusmp.perf.PerfBudget;
import com.newuniverse.nusmp.perf.PerfSettings;
import com.newuniverse.nusmp.perf.VfxQuality;
import com.newuniverse.nusmp.scalecast.ScaleLod;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Vector3f;

/**
 * Why: the client half of the Scale's level of detail (the rules are the pure {@link ScaleLod}). Once per frame, at the start of the level render, it reads the camera (position, look direction)
 * and the effect quality, rolls the quad budget of the frame (keyed by a frame counter, not the game tick) and forgets the choices of the last frame. During the frame every Scale asks
 * {@link #level} once; the answer is remembered by entity id, so a second pass over the same prop (a shadow pass of a shader pack) gets the same level and costs the budget once.
 * Render thread only; no allocation per call (the cost table is built once).
 * <p>
 * If the frame event is not seen (a renderer replacement that does not send it) the budget is switched off after half a second and only the distance levels and the behind-the-camera cull
 * remain, so the Scale can never vanish for good.
 */
public final class ScaleFrame {
    private ScaleFrame() {}

    private static final int[] COST = ScaleLod.cost();
    private static final PerfBudget QUADS = new PerfBudget("scale-quads", 4000);
    private static final long STALE_NANOS = 500_000_000L;
    private static boolean registered;
    private static long frame, lastBegin;
    private static double camX, camY, camZ;
    private static float lookX, lookY, lookZ = -1f;
    private static float detail = 1f;
    private static final Int2IntOpenHashMap CHOSEN = new Int2IntOpenHashMap();

    /** Called once from {@code ScaleBalancePainter.register}. */
    public static synchronized void init() {
        if (registered) return;
        registered = true;
        NeoForge.EVENT_BUS.addListener(ScaleFrame::onStage);
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
        QUADS.setCap(ScaleLod.frameQuads(q));
        CHOSEN.clear();
        frame++;
        lastBegin = System.nanoTime();
    }

    public static float detail() { return detail; }

    /** Squared distance from the camera (the last frame's camera): for the readout's reach test. */
    public static double distSq(Entity e) {
        double dx = e.getX() - camX, dy = e.getY() - camY, dz = e.getZ() - camZ;
        return dx * dx + dy * dy + dz * dz;
    }

    /** The level of a Scale prop: distance band, behind-the-camera cull, the frame's quad budget. */
    public static int level(Entity e) {
        boolean live = System.nanoTime() - lastBegin < STALE_NANOS;          // false: no frame event reaches us (see the class comment): distance and the behind-the-camera cull only
        int id = e.getId();
        if (live) {
            int chosen = CHOSEN.getOrDefault(id, -1);
            if (chosen >= 0) return chosen;
        }
        double dx = e.getX() - camX, dy = e.getY() - camY, dz = e.getZ() - camZ;
        double distSq = dx * dx + dy * dy + dz * dz;
        int lod;
        if (ScaleLod.behind(dx * lookX + dy * lookY + dz * lookZ) && distSq > ScaleLod.KEEP_RADIUS * ScaleLod.KEEP_RADIUS) {
            lod = ScaleLod.CULLED;
        } else {
            lod = ScaleLod.level(distSq, detail);
            if (live && lod < ScaleLod.CULLED) lod = ScaleLod.admit(QUADS, frame, lod, distSq, COST);
        }
        if (live) CHOSEN.put(id, lod);
        return lod;
    }
}
