package com.newuniverse.nusmp.vfx.client;

import com.newuniverse.nusmp.vfx.UltimateVfxFallback;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 3.29.0: {@code visual.uniqueUltimates} in {@code config/nusmp-ultimates-client.toml} (a CLIENT config: it only changes how things look). On (the default): each of the ten Evolution Ultimates of 3.28.0 plays its own
 * signature effect. Off: they play the generic shape they used before 3.29.0. The value is mirrored into the pure {@link UltimateVfxFallback} switch when the file loads or reloads, the same way
 * {@link HerculesModelsConfig} does; the dedicated server never registers this config.
 */
public final class UniqueUltimatesConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue UNIQUE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("visual");
        UNIQUE = b.comment("Play the unique signature effect of each Evolution Ultimate (Iron Shadow Dragon Mode, Satan Soul: Hell's Core, All Crush, Great Flood, Monarch of Shadows, Chairman's Last Stand, Devouring Shadow, Final Dose, "
                        + "Disaster of Floods, Elf Ring: Thousand Elf). Off = the generic effects they used before 3.29.0.")
                .define("uniqueUltimates", true);
        b.pop();
        SPEC = b.build();
    }

    private UniqueUltimatesConfig() {}

    /** Client side only: registers the file and keeps the pure switch in step with it. */
    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, SPEC, "nusmp-ultimates-client.toml");
        modBus.addListener((ModConfigEvent.Loading e) -> apply());
        modBus.addListener((ModConfigEvent.Reloading e) -> apply());
    }

    static void apply() {
        try { UltimateVfxFallback.setUniqueEnabled(UNIQUE.get()); } catch (IllegalStateException notLoaded) { UltimateVfxFallback.setUniqueEnabled(true); }
    }
}
