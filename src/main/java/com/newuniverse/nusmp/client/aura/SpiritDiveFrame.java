package com.newuniverse.nusmp.client.aura;

import com.newuniverse.nusmp.book.SpiritDiveTier;
import com.newuniverse.nusmp.perf.PerfBudget;

/**
 * Spirit Dive remake: the per-frame state of the dive painters, keyed by a frame counter (the same pattern as the 3D compass, client.compass.CompassFrame): {@link #begin} is called once per
 * rendered frame by {@code SpiritDiveClient} (the level-render stage event), and
 * <ul>
 *   <li>{@link #admitCharacter} lets at most {@link SpiritDiveTier#CHARACTER_PER_FRAME} dives per frame draw the character tier (the rest draw the standard tier, so a crowd of Yunos costs
 *       three full looks and the rest light ones);</li>
 *   <li>{@link #markFlameArm} / {@link #flameArmDrawn} tell the flaming-arm painter that the dive already drew the molten arm of the Fuegoleon grade for this player in this frame (it then only adds
 *       its own flames, never a second arm).</li>
 * </ul>
 * When no frame event arrives (the preview harness, a mod that swallows the stage) every dive is admitted and the marker is read once, so nothing is ever lost. Render thread only; no allocation.
 */
public final class SpiritDiveFrame {
    private SpiritDiveFrame() {}

    private static final long STALE_NANOS = 500_000_000L;
    private static final PerfBudget CHARACTER = new PerfBudget("spirit-dive-character", SpiritDiveTier.CHARACTER_PER_FRAME);
    private static long frame, lastBegin;
    private static int flameArmEntity = -1;

    /** Starts a frame. */
    public static void begin() {
        frame++;
        lastBegin = System.nanoTime();
        flameArmEntity = -1;
    }

    private static boolean live() { return System.nanoTime() - lastBegin < STALE_NANOS; }

    /** May one more dive draw the character tier in this frame? */
    public static boolean admitCharacter() { return !live() || CHARACTER.tryTake(frame, 1); }

    /** The dive drew the molten arm of this player. */
    public static void markFlameArm(int entityId) { flameArmEntity = entityId; }

    /** Did the dive draw the molten arm of this player in this frame? (Reading it clears it when no frame events arrive.) */
    public static boolean flameArmDrawn(int entityId) {
        boolean drawn = flameArmEntity == entityId;
        if (drawn && !live()) flameArmEntity = -1;
        return drawn;
    }

    /** The squared distance from the camera to the player being painted (0 when the camera cannot be read). */
    public static double distSq(AuraContext c) {
        try {
            var cam = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera();
            return cam.getPosition().distanceToSqr(c.player().getX(), c.player().getY(), c.player().getZ());
        } catch (RuntimeException ex) {
            return 0.0;
        }
    }

    /** The current frame number (for the self-test and the debug line). */
    public static long frame() { return frame; }
}
