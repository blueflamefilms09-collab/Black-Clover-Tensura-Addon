package com.newuniverse.nusmp.vfx.client;

import com.newuniverse.nusmp.vfx.StarV2;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The CLIENT config {@code nusmp-star-v2-client.toml} (it only changes what THIS client draws; the dedicated server never registers it and nothing is sent for it):
 * <ul>
 *   <li>{@code vfx.element.star}: the look of the Star nodes (placed stars, constellation lines, Conjunction, the Scutum pane, the Hasta lance, the whip, the Black Neverland dome). V2 (default) is the
 *       anime-cel / volumetric-glow look; V1 draws them as the old starlight beam and a star as a plain glow.</li>
 *   <li>{@code vfx.element.starReducedFlashes}: for photosensitive players. true drops the hard flash stars, the speed lines and the lens flare to a soft fade and stops the twinkling.</li>
 * </ul>
 * The values are mirrored into {@link StarV2#setStyle} / {@link StarV2#setReducedFlashes} when the file loads or reloads, so the painters read a field every frame, not a config.
 */
public final class StarV2Config {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.EnumValue<StarV2.Style> STYLE;
    private static final ModConfigSpec.BooleanValue REDUCED_FLASHES;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("vfx");
        b.push("element");
        STYLE = b.comment("The look of the Star nodes (the placed stars, constellation lines, Conjunction, the Quartile Scutum pane, the Hasta lance, the whip, Black Neverland): "
                        + "V2 (default: anime-cel stars and lines mixed with soft volumetric glow) or V1 (the old starlight beam; a star is a plain glow). Only changes what this client draws.")
                .defineEnum("star", StarV2.DEFAULT_STYLE);
        REDUCED_FLASHES = b.comment("Reduced flashes for the Star effects (photosensitivity): hard flash stars, speed lines and the lens flare are replaced by a soft fade and the stars stop twinkling. "
                        + "Default false. Only changes what this client draws.")
                .define("starReducedFlashes", StarV2.DEFAULT_REDUCED_FLASHES);
        b.pop();
        b.pop();
        SPEC = b.build();
    }

    private StarV2Config() {}

    private static boolean done;

    /** One line of the client block of the mod constructor. Safe to call twice. */
    public static synchronized void init(IEventBus modBus, ModContainer container) {
        if (done) return;
        done = true;
        container.registerConfig(ModConfig.Type.CLIENT, SPEC, "nusmp-star-v2-client.toml");                // a literal, so ConfigFileNamesTest sees the name (it equals StarV2.CONFIG_FILE, pinned by StarV2SourceScanTest)
        modBus.addListener((ModConfigEvent.Loading e) -> apply());
        modBus.addListener((ModConfigEvent.Reloading e) -> apply());
    }

    static void apply() {
        try {
            StarV2.setStyle(STYLE.get());
            StarV2.setReducedFlashes(REDUCED_FLASHES.get());
        } catch (IllegalStateException notLoaded) {
            StarV2.setStyle(StarV2.DEFAULT_STYLE);
            StarV2.setReducedFlashes(StarV2.DEFAULT_REDUCED_FLASHES);
        }
    }
}
