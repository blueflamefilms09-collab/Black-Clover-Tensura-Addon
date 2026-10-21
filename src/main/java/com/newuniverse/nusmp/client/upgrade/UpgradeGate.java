package com.newuniverse.nusmp.client.upgrade;

import com.newuniverse.nusmp.particle.fx.FxConfig;
import com.newuniverse.nusmp.particle.fx.ParticleNearCap;
import com.newuniverse.nusmp.perf.PerfBudgets;
import com.newuniverse.nusmp.perf.PerfSettings;
import com.newuniverse.nusmp.perf.UpgradeBudget;
import com.newuniverse.nusmp.perf.VfxQuality;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Vector3f;

/**
 * Why: the one door every upgrade effect of the client goes through before it spawns a particle (the God melee ambient layers, the Genesis weapon motes, trails and act bursts, the worn relic
 * layers, the tiny Djinn, the Elsdocia glitter). 1.110.0: the owner found the effects too many and too heavy. Each emitter keeps its own look (positions, kinds, table rates); what it
 * spawns is decided here, with the pure numbers of {@link UpgradeBudget}:
 * <ol>
 *   <li>{@link #begin}: once per client tick, nothing while the game is paused, no level, the camera is not ready, {@code particles.upgradeDensity} is 0, or the frame rate is under
 *       {@link UpgradeBudget#MIN_FPS} (the frame rate is counted here from the frames the level renders);</li>
 *   <li>{@link #scale}: per holder, the density option x effect quality x frame rate x distance (somebody else's item: nothing beyond 16 blocks) x view direction (nothing behind the camera);</li>
 *   <li>{@link #take}: how many of the wanted particles this holder's item may spawn now: the per-item bucket, the per-player bucket, the total live cap, then the client allowance of the
 *       performance pass ({@link PerfBudgets#clientParticles});</li>
 *   <li>{@link #add}: the spawn itself, skipped when the point is closer than {@link ParticleNearCap#SPAWN_SKIP} blocks to the camera (the first-person view).</li>
 * </ol>
 * Client thread only; holders are keyed by entity id, never a static "current player".
 */
public final class UpgradeGate {
    private UpgradeGate() {}

    private static final UpgradeBudget BUDGET = new UpgradeBudget();
    private static boolean inited;
    private static long windowStart;
    private static int frames, fps;
    private static long ctxTick = Long.MIN_VALUE;
    private static boolean open;
    private static VfxQuality quality = VfxQuality.HIGH;
    private static float density = UpgradeBudget.DEFAULT_DENSITY;
    private static int selfId = -1;
    private static double cx, cy, cz, lx, ly, lz;

    private static synchronized void init() {
        if (inited) return;
        inited = true;
        NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent e) -> {
            if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
            long now = System.nanoTime();
            if (windowStart == 0L) windowStart = now;
            frames++;
            long dt = now - windowStart;
            if (dt >= 1_000_000_000L) {
                fps = (int) Math.round(frames * 1.0e9 / dt);
                frames = 0;
                windowStart = now;
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> clear());
    }

    /** Forgets every bucket (leaving a world, a resource reload). */
    public static void clear() {
        BUDGET.clear();
        ctxTick = Long.MIN_VALUE;
        open = false;
    }

    /** The frame rate measured over the last second (0 until it is known). */
    public static int fps() {
        return fps;
    }

    /** The budget object, for the report and the tests. */
    public static UpgradeBudget budget() {
        return BUDGET;
    }

    /** One client tick of the gate; true while upgrade effects may spawn. Cheap to call from every emitter: the answer is computed once per game tick. */
    public static boolean begin(Minecraft mc, long tick) {
        init();
        if (tick == ctxTick) return open;
        ctxTick = tick;
        open = false;
        if (mc == null || mc.level == null || mc.player == null || mc.isPaused()) return false;
        density = FxConfig.upgradeDensity();
        quality = PerfSettings.vfxQuality();
        if (UpgradeBudget.density(density) <= 0f || UpgradeBudget.fpsFactor(fps) <= 0f) return false;
        Camera cam = mc.gameRenderer.getMainCamera();
        if (cam == null || !cam.isInitialized()) return false;
        Vec3 p = cam.getPosition();
        Vector3f look = cam.getLookVector();
        cx = p.x;
        cy = p.y;
        cz = p.z;
        lx = look.x;
        ly = look.y;
        lz = look.z;
        selfId = mc.player.getId();
        open = true;
        return true;
    }

    /** The scale of one holder this tick (0 = spawn nothing for him). Call after {@link #begin} said true. */
    public static float scale(AbstractClientPlayer p) {
        double dx = p.getX() - cx, dy = p.getEyeY() - cy, dz = p.getZ() - cz, d2 = dx * dx + dy * dy + dz * dz;
        double dist = Math.sqrt(d2);
        double dot = dist > 1.0e-6 ? (dx * lx + dy * ly + dz * lz) / dist : 1.0;
        return UpgradeBudget.scale(density, quality, fps, dist, dot, p.getId() == selfId);
    }

    /** Is this holder the local player? */
    public static boolean isSelf(AbstractClientPlayer p) {
        return p.getId() == selfId;
    }

    /** How many of {@code wanted} particles holder {@code p} may spawn for the item {@code itemKey} now (the buckets, the live cap and the client allowance of the performance pass). */
    public static int take(AbstractClientPlayer p, int itemKey, int wanted) {
        if (!open || wanted <= 0) return 0;
        int n = BUDGET.take(p.getId(), itemKey, ctxTick, wanted, quality, PerfSettings.clientParticlesPerTick());
        return n <= 0 ? 0 : PerfBudgets.clientParticles(ctxTick, n);
    }

    /** The particles of a burst of {@code wanted} (an act signature, a poof): scaled by the density, the quality and the frame rate, capped, under the live cap and the client allowance. */
    public static int burst(Minecraft mc, int wanted) {
        if (mc == null || mc.level == null || !begin(mc, mc.level.getGameTime())) return 0;
        int n = UpgradeBudget.burstCount(wanted, density, quality, fps);
        n = BUDGET.takeBurst(ctxTick, n, quality, PerfSettings.clientParticlesPerTick());
        return n <= 0 ? 0 : PerfBudgets.clientParticles(ctxTick, n);
    }

    /** {@link UpgradeBudget#stochastic}: a rate as a whole number of particles. */
    public static int count(float want, net.minecraft.util.RandomSource rnd) {
        return UpgradeBudget.stochastic(want, rnd.nextFloat());
    }

    /** Spawns the particle unless the point is within {@link ParticleNearCap#SPAWN_SKIP} blocks of the camera (nothing is skipped before the gate is open, so a caller must ask {@link #begin} first). */
    public static void add(ClientLevel level, ParticleOptions o, double x, double y, double z, double vx, double vy, double vz) {
        if (o == null) return;
        double dx = x - cx, dy = y - cy, dz = z - cz;
        if (ParticleNearCap.skipSpawn(dx * dx + dy * dy + dz * dz)) return;
        level.addParticle(o, x, y, z, vx, vy, vz);
    }
}
