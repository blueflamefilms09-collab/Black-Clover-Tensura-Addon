package com.newuniverse.nusmp.client.relics.djinn.tiny;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;

/**
 * The one entry point of the Tiny Djinn on the client ({@code DjinnClient.init} calls {@link #init}): the config file, the figure's render and tick hooks.
 * The Lesser Summon's chibi figure replaces the old translucent bust while {@code djinn.tinyModel} is true (the bust code is kept and runs again when it is false).
 */
public final class TinyDjinnClient {
    private TinyDjinnClient() {}

    public static void init(IEventBus modBus) {
        try {
            ModContainer container = ModLoadingContext.get().getActiveContainer();
            if (container != null) container.registerConfig(ModConfig.Type.CLIENT, TinyDjinnConfig.SPEC, "nusmp-djinn-tiny-client.toml");
        } catch (RuntimeException | LinkageError e) {
            // the options are a convenience: the defaults hold (figure on, 16 at most, name and bar on)
        }
        TinyDjinnLayer.init();
    }

    /** The network handler of the cast cue (client thread). */
    public static void cue(int ownerId, int foeId) { TinyDjinnLayer.cue(ownerId, foeId); }

    /** True when the figure is drawn instead of the old bust. */
    public static boolean replacesBust() { return TinyDjinnLayer.replacesBust(); }
}
