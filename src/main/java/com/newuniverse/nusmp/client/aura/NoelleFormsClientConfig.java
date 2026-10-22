package com.newuniverse.nusmp.client.aura;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the CLIENT config of Noelle's armour forms ({@code config/nusmp-noelle-forms-client.toml}): {@code noelleForms.classicLook} (default false) draws the first Valkyrie Dress (the Water armour
 * of 1.93.0) instead of the remade Valkyrie / Saint Valkyrie Dress. A look-only setting: nothing on the server depends on it. The value is copied into {@link NoelleFormsOptions} when the config
 * loads or reloads.
 */
public final class NoelleFormsClientConfig {
    private NoelleFormsClientConfig() {}

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue CLASSIC_LOOK;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("noelleForms");
        CLASSIC_LOOK = b.comment("true: the Valkyrie Dress is drawn with its first look (the 1.93.0 water armour) instead of the remade Valkyrie / Saint Valkyrie Dress. Looks only.")
                .define("classicLook", false);
        b.pop();
        SPEC = b.build();
    }

    /** Registers the file and the reload hook (client only; called from NUSMP in the client block). */
    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, SPEC, "nusmp-noelle-forms-client.toml");
        modBus.addListener((ModConfigEvent.Loading e) -> { if (e.getConfig().getSpec() == SPEC) apply(); });
        modBus.addListener((ModConfigEvent.Reloading e) -> { if (e.getConfig().getSpec() == SPEC) apply(); });
    }

    private static void apply() {
        try { NoelleFormsOptions.setClassic(CLASSIC_LOOK.get()); } catch (IllegalStateException unloaded) { NoelleFormsOptions.setClassic(false); }
    }
}
