package com.newuniverse.nusmp.vfx.client;

import com.newuniverse.nusmp.vfx.WaterBc;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The CLIENT config {@code nusmp-water-bc-client.toml} (it only changes what THIS client draws; the dedicated server never registers it and nothing is sent for it): {@code vfx.element.water}, the look of
 * the Water effects Sea Dragon's Roar, Aqua Javelin / Tide Lance, Cradle, Nest, the impact burst, the splash and the ring. BC (default) is the hybrid of 3D toon cores wrapped in glass ({@code WaterBcLayer});
 * V1 is exactly the code before it. The old {@code WaterSpellLayer} shapes are not affected by the setting. The value is mirrored into {@link WaterBc#setStyle} when the file loads or reloads, so the layer
 * reads a field every frame, not a config.
 */
public final class WaterBcConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.EnumValue<WaterBc.Style> STYLE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("vfx");
        b.push("element");
        STYLE = b.comment("The look of the Water effects Sea Dragon's Roar, Aqua Javelin / Tide Lance, Cradle, Nest, the impact burst, the splash and the ring (the splash and the ring are also used by blood and paint pages): "
                        + "BC (default: 3D toon cores wrapped in glass, with a per-frame budget and a flat-sprite fall-back) or V1 (the look before it, exactly). Only changes what this client draws.")
                .defineEnum("water", WaterBc.DEFAULT_STYLE);
        b.pop();
        b.pop();
        SPEC = b.build();
    }

    private WaterBcConfig() {}

    private static boolean done;

    /** One line of the client block of the mod constructor. Safe to call twice. */
    public static synchronized void init(IEventBus modBus, ModContainer container) {
        if (done) return;
        done = true;
        container.registerConfig(ModConfig.Type.CLIENT, SPEC, "nusmp-water-bc-client.toml");        // a literal, so ConfigFileNamesTest sees the name; equals WaterBc.CONFIG_FILE (WaterBcSourceScanTest)
        modBus.addListener((ModConfigEvent.Loading e) -> apply());
        modBus.addListener((ModConfigEvent.Reloading e) -> apply());
    }

    static void apply() {
        try {
            WaterBc.setStyle(STYLE.get());
        } catch (IllegalStateException notLoaded) {
            WaterBc.setStyle(WaterBc.DEFAULT_STYLE);
        }
    }
}
