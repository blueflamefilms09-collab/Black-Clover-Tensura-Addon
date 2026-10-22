package com.newuniverse.nusmp.perf;

/**
 * Why: the shared per-tick allowances of the mod, one object per kind of work, with their caps read from {@link PerfSettings} (nusmp-performance-common.toml) every time they are asked, so a
 * config edit takes effect at once. Server budgets belong to the server thread, the client budget to the client thread ({@link PerfBudget} is not synchronized); in single-player the two
 * threads never share one object. Pure (no game classes): the callers pass the game time of the level they work in.
 * <ul>
 *   <li>{@link #particles}: server particles sent by the budgeted emitters ({@code maxParticlesPerTick}).</li>
 *   <li>{@link #effectPacket}: one effect (VFX) packet of {@code VfxSpawn} ({@code maxEffectPacketsPerTick}).</li>
 *   <li>{@link #clientParticles}: ambient particles a client may spawn in one tick across the budgeted client emitters ({@code clientParticlesPerTick}).</li>
 * </ul>
 */
public final class PerfBudgets {
    private PerfBudgets() {}

    public static final PerfBudget PARTICLES = new PerfBudget("particles", PerfSettings.DEFAULT_MAX_PARTICLES_PER_TICK);
    public static final PerfBudget EFFECT_PACKETS = new PerfBudget("effect-packets", PerfSettings.DEFAULT_MAX_EFFECT_PACKETS_PER_TICK);
    public static final PerfBudget CLIENT_PARTICLES = new PerfBudget("client-particles", PerfSettings.DEFAULT_CLIENT_PARTICLES_PER_TICK);
    /** 1.112.0 (lag audit): the quads one client frame may spend on 3D compasses, arrow-needles and routes together (the cap follows the effect quality: {@code CompassLod.frameQuads}); keyed by the client frame counter, not the game tick. */
    public static final PerfBudget COMPASS_QUADS = new PerfBudget("compass-quads", 4000);

    /** How many of {@code wanted} server particles may be sent in the tick {@code gameTime} (the count a {@code sendParticles} call should use). */
    public static int particles(long gameTime, int wanted) {
        PARTICLES.setCap(PerfSettings.maxParticlesPerTick());
        return PARTICLES.take(gameTime, wanted);
    }

    /** May one more effect packet go out in tick {@code gameTime}? Counts it when yes. */
    public static boolean effectPacket(long gameTime) {
        EFFECT_PACKETS.setCap(PerfSettings.maxEffectPacketsPerTick());
        return EFFECT_PACKETS.tryTake(gameTime, 1);
    }

    /** How many of {@code wanted} client particles may be spawned in the client tick {@code gameTime}. */
    public static int clientParticles(long gameTime, int wanted) {
        CLIENT_PARTICLES.setCap(PerfSettings.clientParticlesPerTick());
        return CLIENT_PARTICLES.take(gameTime, wanted);
    }

    public static void resetStats() {
        PARTICLES.resetStats();
        EFFECT_PACKETS.resetStats();
        CLIENT_PARTICLES.resetStats();
        COMPASS_QUADS.resetStats();
    }
}
