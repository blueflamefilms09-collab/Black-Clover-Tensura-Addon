package com.newuniverse.nusmp.lords;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: the Lords of the Mortal Sins (the Black Axe of Sloth, Gunblade of Envy, Spear of Vanity and the holy sword Seifort, and the mastery of Greed's Half-Starved state) are one self-contained feature, so they have one entry
 * point: the single line the mod constructor calls. It registers the eight new form items, the two data components, the attachment of the burden and the mastery, the server config {@code nusmp-lords-server.toml}, the two
 * payloads, the events and the CI self-tests ({@code lords.rules}, {@code lords.world}, {@code lords.burden}). The four Lord skills are registered with the others in {@code skill/NUSkills}. Safe to call twice. The
 * client half is {@code client.lords.LordClient}, called from the client block of the constructor; the weapon renderer lists {@link LordItems#renderables()}.
 */
public final class LordBoot {
    private LordBoot() {}

    private static boolean done;

    public static synchronized void init(IEventBus modBus, ModContainer container) {
        if (done) return;
        done = true;
        LordItems.REGISTER.register(modBus);
        LordComponents.COMPONENTS.register(modBus);
        LordStore.ATTACHMENTS.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, LordConfig.SPEC, "nusmp-lords-server.toml");
        modBus.addListener(LordNet::registerPayloads);
        LordEvents.register(NeoForge.EVENT_BUS);
        LordSelfTests.register();
    }
}
