package com.newuniverse.nusmp.vfx.client;

import com.newuniverse.nusmp.vfx.RobloxMath;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The CLIENT config {@code nusmp-roblox-vfx-client.toml} (it only changes what THIS client draws; the dedicated server never registers it and nothing is sent for it):
 * <ul>
 *   <li>{@code vfx.robloxStyle}: true (default) draws the ten effects listed in {@link RobloxMath#SHAPES} (Kamehameha beam, ki shockwave and blast, lightning spear, earth spikes, anti-magic slash, tornado,
 *       ice shatter, water burst, the grimoire circle's explosion) with the Roblox-style mesh toolkit; false draws exactly the old layers.</li>
 * </ul>
 * The value is mirrored into {@link RobloxMath#setEnabled} when the file loads or reloads, so the layers read a field every frame, not a config.
 */
public final class RobloxStyleConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue ROBLOX_STYLE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("vfx");
        ROBLOX_STYLE = b.comment("Roblox-style effects: layered meshes with scrolling textures, shock rings and flip-book sprites instead of piles of flat sprites, for ten effects (Kamehameha, ki shockwave / blast, "
                        + "lightning spear, earth spikes, anti-magic slash, tornado, ice shatter, water burst, the grimoire circle explosion). false = the old drawing of all of them. Only changes what this client draws.")
                .define("robloxStyle", true);
        b.pop();
        SPEC = b.build();
    }

    private RobloxStyleConfig() {}

    private static boolean done;

    /** One line of the client block of the mod constructor. Safe to call twice. */
    public static synchronized void init(IEventBus modBus, ModContainer container) {
        if (done) return;
        done = true;
        container.registerConfig(ModConfig.Type.CLIENT, SPEC, "nusmp-roblox-vfx-client.toml");            // a literal, so ConfigFileNamesTest sees the name (it equals RobloxMath.CONFIG_FILE, pinned by RobloxStyleBudgetTest)
        modBus.addListener((ModConfigEvent.Loading e) -> apply());
        modBus.addListener((ModConfigEvent.Reloading e) -> apply());
    }

    static void apply() {
        try {
            RobloxMath.setEnabled(ROBLOX_STYLE.get());
        } catch (IllegalStateException notLoaded) {
            RobloxMath.setEnabled(true);
        }
    }
}
