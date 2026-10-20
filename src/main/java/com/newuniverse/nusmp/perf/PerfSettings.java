package com.newuniverse.nusmp.perf;

/**
 * Why: the performance knobs (nusmp-performance-common.toml and nusmp-performance-client.toml) are read by hot code several times a frame or a tick. Reading a config value is a lookup behind a
 * "loaded?" check that throws before the file is read; this holder keeps the numbers in plain volatile fields with safe defaults, {@link PerfConfig} copies the file into it when the file loads
 * or is edited, and every hot path reads a field. Pure (no game classes), so the defaults and the clamps are unit-tested. The defaults reproduce the behaviour the mod had before this pass.
 */
public final class PerfSettings {
    private PerfSettings() {}

    // ---------------------------------------------------------------- defaults (the pre-1.99 behaviour)
    public static final int DEFAULT_MAX_PARTICLES_PER_TICK = 2000;
    public static final int DEFAULT_MAX_EFFECT_PACKETS_PER_TICK = 160;
    public static final int DEFAULT_ENTITY_SCAN_INTERVAL = 5;
    public static final int DEFAULT_RELIC_SCAN_INTERVAL = 10;
    public static final int DEFAULT_PACKET_REFRESH_TICKS = 200;
    public static final int DEFAULT_AURA_LOD_NEAR = 48;
    public static final int DEFAULT_AURA_LOD_FAR = 96;
    public static final int DEFAULT_AURA_MID_COUNT = 2;
    public static final int DEFAULT_CLIENT_PARTICLES_PER_TICK = 400;

    private static volatile VfxQuality commonQuality = VfxQuality.HIGH;
    private static volatile VfxQuality clientQuality;                        // null = follow the common file
    private static volatile int maxParticlesPerTick = DEFAULT_MAX_PARTICLES_PER_TICK;
    private static volatile int maxEffectPacketsPerTick = DEFAULT_MAX_EFFECT_PACKETS_PER_TICK;
    private static volatile int entityScanInterval = DEFAULT_ENTITY_SCAN_INTERVAL;
    private static volatile int relicScanInterval = DEFAULT_RELIC_SCAN_INTERVAL;
    private static volatile int packetRefreshTicks = DEFAULT_PACKET_REFRESH_TICKS;
    private static volatile int auraLodNear = DEFAULT_AURA_LOD_NEAR;
    private static volatile int auraLodFar = DEFAULT_AURA_LOD_FAR;
    private static volatile int auraMidCount = DEFAULT_AURA_MID_COUNT;
    private static volatile int clientParticlesPerTick = DEFAULT_CLIENT_PARTICLES_PER_TICK;
    private static volatile boolean probeInConfig;

    // ---------------------------------------------------------------- readers (hot paths)
    /** The quality in force on this machine: the client file's choice if it made one, else the common file's. */
    public static VfxQuality vfxQuality() {
        VfxQuality c = clientQuality;
        return c != null ? c : commonQuality;
    }

    public static VfxQuality commonQuality() { return commonQuality; }

    /** The server particle allowance per tick across the budgeted emitters; 0 = unlimited. */
    public static int maxParticlesPerTick() { return maxParticlesPerTick; }

    /** The effect (VFX) packets the server may send in one tick across all spells and bosses; 0 = unlimited. */
    public static int maxEffectPacketsPerTick() { return maxEffectPacketsPerTick; }

    /** The period (ticks) of the non-urgent entity scans that were not throttled before this pass, and of the ones that now follow the config. */
    public static int entityScanInterval() { return entityScanInterval; }

    /** How long (ticks) a look at what a wearer carries stays good ({@code Relics.ttl}). */
    public static int relicScanInterval() { return relicScanInterval; }

    /** How often (ticks) an unchanged state is sent again anyway; 0 = never repeat. */
    public static int packetRefreshTicks() { return packetRefreshTicks; }

    public static int auraLodNear() { return auraLodNear; }

    public static int auraLodFar() { return auraLodFar; }

    /** How many auras of a wearer between the near and the far distance are still painted. */
    public static int auraMidCount() { return auraMidCount; }

    /** The client's allowance of ambient particles per tick across the budgeted client emitters; 0 = unlimited. */
    public static int clientParticlesPerTick() { return clientParticlesPerTick; }

    public static boolean probeInConfig() { return probeInConfig; }

    // ---------------------------------------------------------------- writers (the config loader, tests)
    public static void setCommonQuality(VfxQuality q) { commonQuality = q == null ? VfxQuality.HIGH : q; }

    /** null = follow the common file. */
    public static void setClientQuality(VfxQuality q) { clientQuality = q; }

    public static void setMaxParticlesPerTick(int v) { maxParticlesPerTick = Math.max(0, v); }

    public static void setMaxEffectPacketsPerTick(int v) { maxEffectPacketsPerTick = Math.max(0, v); }

    public static void setEntityScanInterval(int v) { entityScanInterval = PerfMath.interval(v); }

    public static void setRelicScanInterval(int v) { relicScanInterval = PerfMath.clamp(v, 1, 100); }

    public static void setPacketRefreshTicks(int v) { packetRefreshTicks = v <= 0 ? 0 : PerfMath.clamp(v, 20, 24000); }

    /** The near and far distance (blocks) of the aura level of detail; far is never below near. */
    public static void setAuraLod(int near, int far) {
        int n = PerfMath.clamp(near, 8, 512);
        auraLodNear = n;
        auraLodFar = Math.max(n, PerfMath.clamp(far, 8, 1024));
    }

    public static void setAuraMidCount(int v) { auraMidCount = PerfMath.clamp(v, 0, 16); }

    public static void setClientParticlesPerTick(int v) { clientParticlesPerTick = Math.max(0, v); }

    public static void setProbeInConfig(boolean v) { probeInConfig = v; }

    /** Back to the defaults (tests). */
    public static void resetDefaults() {
        commonQuality = VfxQuality.HIGH;
        clientQuality = null;
        maxParticlesPerTick = DEFAULT_MAX_PARTICLES_PER_TICK;
        maxEffectPacketsPerTick = DEFAULT_MAX_EFFECT_PACKETS_PER_TICK;
        entityScanInterval = DEFAULT_ENTITY_SCAN_INTERVAL;
        relicScanInterval = DEFAULT_RELIC_SCAN_INTERVAL;
        packetRefreshTicks = DEFAULT_PACKET_REFRESH_TICKS;
        auraLodNear = DEFAULT_AURA_LOD_NEAR;
        auraLodFar = DEFAULT_AURA_LOD_FAR;
        auraMidCount = DEFAULT_AURA_MID_COUNT;
        clientParticlesPerTick = DEFAULT_CLIENT_PARTICLES_PER_TICK;
        probeInConfig = false;
    }
}
