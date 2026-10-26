package com.newuniverse.nusmp.vfx.client;

import com.newuniverse.nusmp.vfx.client.layer.IsrafilModels;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 3.21.0: {@code visual.israfilModels} in {@code config/nusmp-israfil-client.toml} (a CLIENT config: it only changes how things look). On (the default): the Israfil ultimate (Lord of Trials) draws its real 3D models (the Grand
 * Trumpet, the weight plate, the maze walls and pillars, the reality shards and the dark core, the coordinate gates). Off: exactly what 3.20.0 drew (the flat sprites). The value is mirrored into the pure {@link IsrafilModels} switch when the file
 * loads or reloads, the same way {@link FlatPropsConfig} does; the dedicated server never registers this config.
 */
public final class IsrafilModelsConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue MODELS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("visual");
        MODELS = b.comment("Draw the 3D models of the Israfil ultimate (the Grand Trumpet, the weight plate, the maze walls and pillars, the reality shards and dark core, the coordinate gates). Off = the flat sprites of the older versions.")
                .define("israfilModels", true);
        b.pop();
        SPEC = b.build();
    }

    private IsrafilModelsConfig() {}

    /** Client side only: registers the file and keeps the pure switch in step with it. */
    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, SPEC, "nusmp-israfil-client.toml");
        modBus.addListener((ModConfigEvent.Loading e) -> apply());
        modBus.addListener((ModConfigEvent.Reloading e) -> apply());
    }

    static void apply() {
        try { IsrafilModels.setEnabled(MODELS.get()); } catch (IllegalStateException notLoaded) { IsrafilModels.setEnabled(true); }
    }
}
