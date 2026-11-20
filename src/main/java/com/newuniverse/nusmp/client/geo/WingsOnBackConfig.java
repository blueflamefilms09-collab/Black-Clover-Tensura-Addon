package com.newuniverse.nusmp.client.geo;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why (docs/wings_on_back.md): {@code visual.wingsOnBack} in {@code config/nusmp-wingsonback-client.toml}, a CLIENT config (it only changes how wings look). On (the default): a wing worn on a player that the
 * files draw sweeping to the front of the body (the devil, Black Clover boss and several armour wings) is drawn on the back, sweeping backward, like the Djinn and Corundum wings ({@link WingFrame}). Off: the files
 * exactly as they were drawn before. The value is mirrored into the pure {@link WingFrame} switch when the file loads or reloads, the way {@code FlatPropsConfig} does; the dedicated server never registers it.
 */
public final class WingsOnBackConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue WINGS_ON_BACK;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("visual");
        WINGS_ON_BACK = b.comment("Draw the wings of the devil forms, the Black Clover bosses and the Dark, Flame, Lightning, Water, Spirit Dive and Valkyrie armours on the player's back, sweeping backward "
                + "(the way the Djinn wings sit). Off = the old drawing, where those wings angle forward past the body (docs/wings_on_back.md).").define("wingsOnBack", true);
        b.pop();
        SPEC = b.build();
    }

    private WingsOnBackConfig() {}

    /** Client side only: registers the file and keeps the pure switch in step with it. */
    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, SPEC, "nusmp-wingsonback-client.toml");
        modBus.addListener((ModConfigEvent.Loading e) -> apply());
        modBus.addListener((ModConfigEvent.Reloading e) -> apply());
    }

    static void apply() {
        try { WingFrame.setOnBack(WINGS_ON_BACK.get()); } catch (IllegalStateException notLoaded) { WingFrame.setOnBack(true); }
    }
}
