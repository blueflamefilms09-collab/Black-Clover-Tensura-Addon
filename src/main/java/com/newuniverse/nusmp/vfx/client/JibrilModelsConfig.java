package com.newuniverse.nusmp.vfx.client;

import com.newuniverse.nusmp.vfx.client.layer.JibrilModels;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 3.21.0: {@code visual.jibrilModels} in {@code config/nusmp-jibril-client.toml} (a CLIENT config: it only changes how things look). On (the default): the Jibril ultimate draws its real 3D models (the Holy Shackles cuffs
 * and chain, the Spear of Heaven, the halo and law plates, the open tome of law, the command crown). Off: exactly what 3.20.0 drew (the flat sprites). The value is mirrored into the pure {@link JibrilModels} switch when the
 * file loads or reloads, the same way {@link FlatPropsConfig} does; the dedicated server never registers this config.
 */
public final class JibrilModelsConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue MODELS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("visual");
        MODELS = b.comment("Draw the 3D models of the Jibril ultimate (the Holy Shackles cuffs and chain, the Spear of Heaven, the halo and law plates, the tome of law, the command crown). Off = the flat sprites of the older versions.")
                .define("jibrilModels", true);
        b.pop();
        SPEC = b.build();
    }

    private JibrilModelsConfig() {}

    /** Client side only: registers the file and keeps the pure switch in step with it. */
    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, SPEC, "nusmp-jibril-client.toml");
        modBus.addListener((ModConfigEvent.Loading e) -> apply());
        modBus.addListener((ModConfigEvent.Reloading e) -> apply());
    }

    static void apply() {
        try { JibrilModels.setEnabled(MODELS.get()); } catch (IllegalStateException notLoaded) { JibrilModels.setEnabled(true); }
    }
}
