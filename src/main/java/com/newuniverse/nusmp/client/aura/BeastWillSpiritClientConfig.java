package com.newuniverse.nusmp.client.aura;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the CLIENT config of the remade Beast Will spirits ({@code config/nusmp-beastwill-spirit-client.toml}): {@code beastwill.classicSpirits} (default false) draws the old animal spirits instead of
 * the remade forms. A look-only setting: nothing on the server depends on it. The value is copied into {@link BeastWillSpiritOptions} when the config loads or reloads.
 */
public final class BeastWillSpiritClientConfig {
    private BeastWillSpiritClientConfig() {}

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue CLASSIC_SPIRITS, FLAT_SPIRITS, CLASSIC_FORMS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("beastwill");
        CLASSIC_SPIRITS = b.comment("true: a Beast Will form is drawn with its old animal spirit (3.57.0 / 3.59.0) instead of the remade form. Looks only.")
                .define("classicSpirits", false);
        FLAT_SPIRITS = b.comment("true: a Beast Will form is drawn with the flat painted sprites of 3.60.0 instead of the 3D cube forms (3.61.0). Looks only.")
                .define("flatSpirits", false);
        CLASSIC_FORMS = b.comment("true: the 3D Beast Will forms are drawn with the models and colours of 3.61.0 / 3.62.0 (the pale, bulky ones) instead of the 3.76.0 look pass. Looks only.")
                .define("classicForms", false);
        b.pop();
        SPEC = b.build();
    }

    /** Registers the file and the reload hook (client only; called from NUSMP in the client block). */
    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, SPEC, "nusmp-beastwill-spirit-client.toml");
        modBus.addListener((ModConfigEvent.Loading e) -> { if (e.getConfig().getSpec() == SPEC) apply(); });
        modBus.addListener((ModConfigEvent.Reloading e) -> { if (e.getConfig().getSpec() == SPEC) apply(); });
    }

    private static void apply() {
        try { BeastWillSpiritOptions.setClassic(CLASSIC_SPIRITS.get()); BeastWillSpiritOptions.setFlat(FLAT_SPIRITS.get()); BeastWillSpiritOptions.setClassicForms(CLASSIC_FORMS.get()); } catch (IllegalStateException unloaded) { BeastWillSpiritOptions.setClassic(false); BeastWillSpiritOptions.setFlat(false); }
    }
}
