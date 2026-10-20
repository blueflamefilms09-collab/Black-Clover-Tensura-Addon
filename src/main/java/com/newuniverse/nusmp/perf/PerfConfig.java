package com.newuniverse.nusmp.perf;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the performance pass (docs/performance_1_99.md) adds guards and levels of detail, and the owner must be able to turn every one of them up or down without a new build.
 * Two files, both registered from {@link PerfBoot}:
 * <ul>
 *   <li>{@code config/nusmp-performance-common.toml} ({@link #COMMON}, read on both sides): {@code vfxQuality} (low / medium / high), {@code maxParticlesPerTick}, {@code maxEffectPacketsPerTick},
 *       {@code entityScanInterval}, {@code relicScanInterval}, {@code packetRefreshTicks}, the aura level of detail ({@code auraLodNear}, {@code auraLodFar}, {@code auraMidCount}) and
 *       {@code probe.enabled}.</li>
 *   <li>{@code config/nusmp-performance-client.toml} ({@link #CLIENT}, this machine only): a {@code vfxQuality} of its own ("auto" = follow the common file), the client's particle allowance and
 *       {@code probe.enabled} for the client sections.</li>
 * </ul>
 * The values are copied into {@link PerfSettings} when a file loads or changes ({@link #apply}); hot code reads {@link PerfSettings}, never this class. The defaults reproduce the behaviour
 * the mod had before the pass: every guard is generous, and 0 switches a guard off where the comment says so.
 */
public final class PerfConfig {
    public static final ModConfigSpec COMMON;
    public static final ModConfigSpec CLIENT;

    private static final ModConfigSpec.ConfigValue<String> VFX_QUALITY;
    private static final ModConfigSpec.IntValue MAX_PARTICLES, MAX_EFFECT_PACKETS, ENTITY_SCAN, RELIC_SCAN, PACKET_REFRESH, AURA_NEAR, AURA_FAR, AURA_MID;
    private static final ModConfigSpec.BooleanValue PROBE;

    private static final ModConfigSpec.ConfigValue<String> CLIENT_QUALITY;
    private static final ModConfigSpec.IntValue CLIENT_PARTICLES;
    private static final ModConfigSpec.BooleanValue CLIENT_PROBE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("vfx");
        VFX_QUALITY = b.comment("How much the big effects may cost a frame. 'high' = the look the mod was designed with (only effects far past the distance anyone can see are dropped); "
                        + "'medium' = fewer effects at once, lower detail from 64 blocks, a cap on the vertices of a frame; 'low' = for weak machines (half detail, 24 effects, 6000 vertices per frame). "
                        + "Applies on the client; the client file can override it.")
                .define("vfxQuality", "high", o -> o instanceof String s && VfxQuality.byName(s, null) != null);
        b.pop();
        b.push("aura");
        AURA_NEAR = b.comment("Distance (blocks) up to which every running aura of a player is painted. Further away only 'auraMidCount' of them are.")
                .defineInRange("auraLodNear", PerfSettings.DEFAULT_AURA_LOD_NEAR, 8, 512);
        AURA_FAR = b.comment("Distance (blocks) beyond which the auras of a player are not painted at all (the player is a few pixels tall there; vanilla stops drawing players at about 64 blocks anyway).")
                .defineInRange("auraLodFar", PerfSettings.DEFAULT_AURA_LOD_FAR, 8, 1024);
        AURA_MID = b.comment("How many auras of one player are painted between 'auraLodNear' and 'auraLodFar' (the most recently started ones).")
                .defineInRange("auraMidCount", PerfSettings.DEFAULT_AURA_MID_COUNT, 0, 16);
        b.pop();
        b.push("budgets");
        MAX_PARTICLES = b.comment("Server particles the budgeted emitters may send per tick in total. 0 = no limit.")
                .defineInRange("maxParticlesPerTick", PerfSettings.DEFAULT_MAX_PARTICLES_PER_TICK, 0, 100000);
        MAX_EFFECT_PACKETS = b.comment("Effect (VFX) packets the server may send per tick in total, over all spells and bosses. The packets over the limit in one tick are dropped (the effect is not shown). 0 = no limit.")
                .defineInRange("maxEffectPacketsPerTick", PerfSettings.DEFAULT_MAX_EFFECT_PACKETS_PER_TICK, 0, 100000);
        b.pop();
        b.push("scans");
        ENTITY_SCAN = b.comment("Ticks between the non-urgent scans of the entities around a player or a block (the ones that were not throttled before the performance pass, and Elsdocia's inventory look). "
                        + "Higher = less server work, slower reaction.")
                .defineInRange("entityScanInterval", PerfSettings.DEFAULT_ENTITY_SCAN_INTERVAL, 1, 100);
        RELIC_SCAN = b.comment("Ticks a look at what a wearer carries (hands, armour, Curios) stays good before it is read again. Higher = less reflection work through Curios, slower reaction to a swapped relic.")
                .defineInRange("relicScanInterval", PerfSettings.DEFAULT_RELIC_SCAN_INTERVAL, 1, 100);
        PACKET_REFRESH = b.comment("Ticks after which an unchanged state that the client mirrors is sent again anyway (a lost or late packet then heals itself). 0 = never repeat; only changes are sent.")
                .defineInRange("packetRefreshTicks", PerfSettings.DEFAULT_PACKET_REFRESH_TICKS, 0, 24000);
        b.pop();
        b.push("probe");
        PROBE = b.comment("Switch the server performance probe on from the start (it times named sections of the biggest handlers and prints the ten most expensive with /multiverse perf report). "
                        + "Off by default; the commands /multiverse perf on and off do the same at run time.")
                .define("enabled", false);
        b.pop();
        COMMON = b.build();

        ModConfigSpec.Builder c = new ModConfigSpec.Builder();
        c.push("vfx");
        CLIENT_QUALITY = c.comment("This machine's effect quality: 'auto' follows nusmp-performance-common.toml, or 'low', 'medium', 'high'.")
                .define("vfxQuality", "auto", o -> o instanceof String s && (s.trim().equalsIgnoreCase("auto") || VfxQuality.byName(s, null) != null));
        c.pop();
        c.push("budgets");
        CLIENT_PARTICLES = c.comment("Ambient particles the budgeted client emitters may spawn per tick in total. 0 = no limit.")
                .defineInRange("clientParticlesPerTick", PerfSettings.DEFAULT_CLIENT_PARTICLES_PER_TICK, 0, 100000);
        c.pop();
        c.push("probe");
        CLIENT_PROBE = c.comment("Switch the client performance probe on from the start (/nusmp perf report prints the ten most expensive client sections). Off by default.")
                .define("enabled", false);
        c.pop();
        CLIENT = c.build();
    }

    private static boolean clientProbeSeen;

    private PerfConfig() {}

    /** Copies both files into {@link PerfSettings} (and the probe switch). Safe to call at any time: a file that is not loaded yet leaves the defaults. */
    public static void apply() {
        try {
            PerfSettings.setCommonQuality(VfxQuality.byName(VFX_QUALITY.get(), VfxQuality.HIGH));
            PerfSettings.setAuraLod(AURA_NEAR.get(), AURA_FAR.get());
            PerfSettings.setAuraMidCount(AURA_MID.get());
            PerfSettings.setMaxParticlesPerTick(MAX_PARTICLES.get());
            PerfSettings.setMaxEffectPacketsPerTick(MAX_EFFECT_PACKETS.get());
            PerfSettings.setEntityScanInterval(ENTITY_SCAN.get());
            PerfSettings.setRelicScanInterval(RELIC_SCAN.get());
            PerfSettings.setPacketRefreshTicks(PACKET_REFRESH.get());
            boolean on = PROBE.get();
            if (on != PerfSettings.probeInConfig()) {                         // only a CHANGE of the file switches the probe: a command that turned it on or off is not undone by the next look
                PerfSettings.setProbeInConfig(on);
                PerfProbe.setEnabled(on);
            }
        } catch (IllegalStateException notLoaded) {
            // the common file is not read yet: the defaults stay
        }
        try {
            String q = CLIENT_QUALITY.get();
            PerfSettings.setClientQuality(q == null || q.trim().equalsIgnoreCase("auto") ? null : VfxQuality.byName(q, null));
            PerfSettings.setClientParticlesPerTick(CLIENT_PARTICLES.get());
            boolean clientOn = CLIENT_PROBE.get();
            if (clientOn != clientProbeSeen) {
                clientProbeSeen = clientOn;
                PerfProbe.setEnabled(clientOn);
            }
        } catch (IllegalStateException notLoaded) {
            // the client file is not read yet (a dedicated server never reads it): the defaults stay
        }
    }
}
