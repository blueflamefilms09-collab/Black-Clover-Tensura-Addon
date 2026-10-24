package com.newuniverse.nusmp.vfx.client;

import com.newuniverse.nusmp.vfx.WindMc;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The CLIENT config {@code nusmp-wind-mc-client.toml} (it only changes what THIS client draws; the dedicated server never registers it and nothing is sent for it): {@code vfx.element.wind}, the look of the
 * Wind effects Crescent Kamaitachi, Slicing Wind Emperor, Gale / Gust Lane and the Wind Ring. MC_C (default) is the Minecraft-style "pixel-anime" look ({@code WindMcLayer}); V1 is exactly the code before
 * it. The Wind Tornado and Spirit of Zephyr are not affected by the setting. The value is mirrored into {@link WindMc#setStyle} when the file loads or reloads, so the layer reads a field every frame, not a config.
 */
public final class WindMcConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.EnumValue<WindMc.Style> STYLE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("vfx");
        b.push("element");
        STYLE = b.comment("The look of the Wind effects Crescent Kamaitachi (also the melee slash of other magics), Slicing Wind Emperor, Gale / Gust Lane and the Wind Ring: MC_C (default: Minecraft-style pixel art, "
                        + "hard 32 px sprites, two-frame flip-book, motion on twos) or V1 (the look before it, exactly). The Wind Tornado and Spirit of Zephyr are never changed by this setting. Only changes what this client draws.")
                .defineEnum("wind", WindMc.DEFAULT_STYLE);
        b.pop();
        b.pop();
        SPEC = b.build();
    }

    private WindMcConfig() {}

    private static boolean done;

    /** One line of the client block of the mod constructor. Safe to call twice. */
    public static synchronized void init(IEventBus modBus, ModContainer container) {
        if (done) return;
        done = true;
        container.registerConfig(ModConfig.Type.CLIENT, SPEC, WindMc.CONFIG_FILE);
        modBus.addListener((ModConfigEvent.Loading e) -> apply());
        modBus.addListener((ModConfigEvent.Reloading e) -> apply());
    }

    static void apply() {
        try {
            WindMc.setStyle(STYLE.get());
        } catch (IllegalStateException notLoaded) {
            WindMc.setStyle(WindMc.DEFAULT_STYLE);
        }
    }
}
