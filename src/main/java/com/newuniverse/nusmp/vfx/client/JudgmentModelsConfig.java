package com.newuniverse.nusmp.vfx.client;

import com.newuniverse.nusmp.vfx.client.layer.JudgmentModels;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 3.21.0: {@code visual.judgmentModels} in {@code config/nusmp-judgment-client.toml} (a CLIENT config: it only changes how things look). On (the default): Sandalphon, Lord of Judgment, draws its real 3D models (the guillotine
 * blade, the scales of judgment, the law tablet and its shards, the lance head, the sword of Final Judgment). Off: exactly what 3.20.0 drew (the flat sprites). The value is mirrored into the pure {@link JudgmentModels} switch when the file
 * loads or reloads, the same way {@link FlatPropsConfig} does; the dedicated server never registers this config.
 */
public final class JudgmentModelsConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue MODELS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("visual");
        MODELS = b.comment("Draw the 3D models of Sandalphon, Lord of Judgment (the guillotine blade, the scales of judgment, the law tablet and its shards, the lance head, the sword of Final Judgment). Off = the flat sprites of the older versions.")
                .define("judgmentModels", true);
        b.pop();
        SPEC = b.build();
    }

    private JudgmentModelsConfig() {}

    /** Client side only: registers the file and keeps the pure switch in step with it. */
    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, SPEC, "nusmp-judgment-client.toml");
        modBus.addListener((ModConfigEvent.Loading e) -> apply());
        modBus.addListener((ModConfigEvent.Reloading e) -> apply());
    }

    static void apply() {
        try { JudgmentModels.setEnabled(MODELS.get()); } catch (IllegalStateException notLoaded) { JudgmentModels.setEnabled(true); }
    }
}
