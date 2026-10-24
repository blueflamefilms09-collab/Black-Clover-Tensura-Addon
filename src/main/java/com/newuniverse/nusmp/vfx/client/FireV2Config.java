package com.newuniverse.nusmp.vfx.client;

import com.newuniverse.nusmp.vfx.FireV2;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The CLIENT config {@code nusmp-fire-v2-client.toml} (it only changes what THIS client draws; the dedicated server never registers it and nothing is sent for it):
 * <ul>
 *   <li>{@code vfx.element.fire}: the look of the fire effects. V2 (default) is the anime-cel / volumetric-glow look ({@code FireV2Layer}); V1 is exactly the code before it.</li>
 *   <li>{@code vfx.element.fireReducedFlashes}: for photosensitive players. true drops the hard flash stars and radial speed lines to a soft fade and slows the flip-book to at most three changes a second.</li>
 * </ul>
 * The values are mirrored into {@link FireV2#setStyle} / {@link FireV2#setReducedFlashes} when the file loads or reloads, so the layer reads a field every frame, not a config.
 */
public final class FireV2Config {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.EnumValue<FireV2.Style> STYLE;
    private static final ModConfigSpec.BooleanValue REDUCED_FLASHES;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("vfx");
        b.push("element");
        STYLE = b.comment("The look of the Fire effects (Leo Rugiens, Sol Linea, Ignis Columna, the flame explosion, Spiral Flame, Wild Bursting Flame, the Flame cast / zone / burst, the flame trail and explosion): "
                        + "V2 (default: anime-cel flames mixed with soft volumetric glow) or V1 (the look before it, exactly). Only changes what this client draws.")
                .defineEnum("fire", FireV2.DEFAULT_STYLE);
        REDUCED_FLASHES = b.comment("Reduced flashes for the fire effects (photosensitivity): hard flash stars and radial speed lines are replaced by a soft fade, there is no pulsing light and no spinning flash sprite, "
                        + "and the flame flip-book changes at most three times a second. Default false. Only changes what this client draws.")
                .define("fireReducedFlashes", FireV2.DEFAULT_REDUCED_FLASHES);
        b.pop();
        b.pop();
        SPEC = b.build();
    }

    private FireV2Config() {}

    private static boolean done;

    /** One line of the client block of the mod constructor. Safe to call twice. */
    public static synchronized void init(IEventBus modBus, ModContainer container) {
        if (done) return;
        done = true;
        container.registerConfig(ModConfig.Type.CLIENT, SPEC, "nusmp-fire-v2-client.toml");                // a literal, so ConfigFileNamesTest sees the name (it equals FireV2.CONFIG_FILE, pinned by FireV2SourceScanTest)
        modBus.addListener((ModConfigEvent.Loading e) -> apply());
        modBus.addListener((ModConfigEvent.Reloading e) -> apply());
    }

    static void apply() {
        try {
            FireV2.setStyle(STYLE.get());
            FireV2.setReducedFlashes(REDUCED_FLASHES.get());
        } catch (IllegalStateException notLoaded) {
            FireV2.setStyle(FireV2.DEFAULT_STYLE);
            FireV2.setReducedFlashes(FireV2.DEFAULT_REDUCED_FLASHES);
        }
    }
}
