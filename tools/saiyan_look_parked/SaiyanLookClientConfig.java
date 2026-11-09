package com.newuniverse.nusmp.client.aura;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the CLIENT config of the Saiyan form looks ({@code config/nusmp-saiyan-look-client.toml}, 3.66.0): {@code saiyan.looks} (default true) draws the hair, eyes, aura and fur of a transformed
 * Saiyan; false restores the old look (only the server's motes). {@code saiyan.fullDetail} (default true) turns off the heavy parts (fur, tail, lightning) for slow computers. Looks only: nothing on
 * the server depends on either. The values are copied into volatile fields when the config loads or reloads.
 */
public final class SaiyanLookClientConfig {
    private SaiyanLookClientConfig() {}

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue LOOKS, FULL_DETAIL;
    private static volatile boolean looks = true, full = true;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("saiyan");
        LOOKS = b.comment("true: a transformed Saiyan is drawn with its form's hair, glowing eyes, flame aura and power-up ring. false: only the plain particles of 3.65.0. Looks only.")
                .define("looks", true);
        FULL_DETAIL = b.comment("true: nearby Saiyans also get fur, a tail and lightning in the forms that have them. false: the light version everywhere (about a third of the quads). Looks only.")
                .define("fullDetail", true);
        b.pop();
        SPEC = b.build();
    }

    public static boolean looks() { return looks; }
    public static boolean fullDetail() { return full; }

    /** Registers the file and the reload hook (client only; called from NUSMP in the client block). */
    public static void init(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, SPEC, "nusmp-saiyan-look-client.toml");
        modBus.addListener((ModConfigEvent.Loading e) -> { if (e.getConfig().getSpec() == SPEC) apply(); });
        modBus.addListener((ModConfigEvent.Reloading e) -> { if (e.getConfig().getSpec() == SPEC) apply(); });
    }

    private static void apply() {
        try { looks = LOOKS.get(); full = FULL_DETAIL.get(); } catch (IllegalStateException unloaded) { looks = true; full = true; }
    }
}
