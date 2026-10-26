package com.newuniverse.nusmp.vfx.client;

import com.newuniverse.nusmp.vfx.client.layer.HerculesModels;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 3.20.0: {@code visual.herculesModels} in {@code config/nusmp-hercules-client.toml} (a CLIENT config: it only changes how things look). On (the default): the Hercules ultimate draws its real 3D models (the Titan's hand
 * and the fist, the hurled boulder, the rock chunks, the laurel crown and ring). Off: exactly what 3.19.0 drew (the flat sprites). The value is mirrored into the pure {@link HerculesModels} switch when the file
 * loads or reloads, the same way {@link FlatPropsConfig} does; the dedicated server never registers this config.
 */
public final class HerculesModelsConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue MODELS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("visual");
        MODELS = b.comment("Draw the 3D models of the Hercules ultimate (the Titan's hand, the fist, the boulder and rock chunks, the laurel crown and ring). Off = the flat sprites of the older versions.")
                .define("herculesModels", true);
        b.pop();
        SPEC = b.build();
    }

    private HerculesModelsConfig() {}

    /** Client side only: registers the file and keeps the pure switch in step with it. */
    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, SPEC, "nusmp-hercules-client.toml");
        modBus.addListener((ModConfigEvent.Loading e) -> apply());
        modBus.addListener((ModConfigEvent.Reloading e) -> apply());
    }

    static void apply() {
        try { HerculesModels.setEnabled(MODELS.get()); } catch (IllegalStateException notLoaded) { HerculesModels.setEnabled(true); }
    }
}
