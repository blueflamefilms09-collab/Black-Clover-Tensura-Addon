package com.newuniverse.nusmp.quests;

import com.newuniverse.nusmp.quests.pack.QuestsCommand;
import com.newuniverse.nusmp.quests.pack.QuestsInstall;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

/**
 * Why: the quest side needs four registrations (the advancement trigger, the config, the install command, the first-run installer). They are
 * gathered here so the mod constructor needs ONE line: {@code QuestsSetup.init(modEventBus, modContainer);}. It must run in the constructor,
 * before registries freeze, because the trigger type is a registry entry.
 */
public final class QuestsSetup {
    private QuestsSetup() {}

    public static void init(IEventBus modEventBus, ModContainer modContainer) {
        NusmpEvents.init(modEventBus);
        modContainer.registerConfig(ModConfig.Type.SERVER, QuestsConfig.SPEC, "nusmp-quests-server.toml");
        NeoForge.EVENT_BUS.addListener(QuestsCommand::register);
        QuestWorldEvents.register();
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, QuestsInstall::onServerAboutToStart);
    }
}
