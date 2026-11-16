package com.newuniverse.nusmp.saiyan.armor;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;

/**
 * Why: the craftable Saiyan alloy set (docs/saiyan_armor.md) is one self-contained feature, so it has one entry point: the single line {@code SaiyanBoot.init} calls. It registers the armour material, the
 * ingot and the four pieces, the server config {@code nusmp-saiyan-armor-server.toml} and the CI self-tests {@code saiyan.armor.*}. The recipes are data; the worn look is the worn-model library's;
 * the creative menu line is in {@code NUCreativeTab}; the set bonus is one line in {@code SaiyanManager.second}. Safe to call twice.
 */
public final class SaiyanArmorBoot {
    private SaiyanArmorBoot() {}

    private static boolean done;

    public static synchronized void init(IEventBus modBus, ModContainer container) {
        if (done) return;
        done = true;
        SaiyanArmorItems.MATERIALS.register(modBus);
        SaiyanArmorItems.REGISTER.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, SaiyanArmorConfig.SPEC, "nusmp-saiyan-armor-server.toml");
        SaiyanArmorSelfTests.register();
    }
}
