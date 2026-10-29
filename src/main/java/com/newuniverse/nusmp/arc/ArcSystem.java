package com.newuniverse.nusmp.arc;

import com.newuniverse.nusmp.arc.dialogue.DialogueLoader;
import com.newuniverse.nusmp.arc.story.ArcStory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Why: the characters of the arcs are many small systems (the roster, the entities, the spawn egg, the story, the dialogue, the Charter grimoires, the
 * boss buff rows, the config, the self-tests) that must be started in one fixed order, and the lead should have exactly one line in the mod constructor for
 * all of it. The order matters: the roster must be filled before anything reads it ({@code ArcCharacters.bootstrap()} also loads every boss class, which
 * registers its entity type), entity types and items must be registered before the registry events, and the Charter Grimoire items go onto
 * {@code NUItems.ITEMS}, so this must run before {@code NUItems.ITEMS.register(modEventBus)}. It registers its own config file, so no separate
 * {@code registerConfig} line exists. Safe to call twice.
 */
public final class ArcSystem {
    private ArcSystem() {}

    private static boolean done;

    /** The single entry point, called from the mod constructor on both sides. */
    public static synchronized void init(IEventBus modBus, ModContainer modContainer) {
        if (done) return;
        done = true;
        ArcCharacters.bootstrap();                       // the roster, and every boss class (so its entity type is registered)
        ArcEntities.init(modBus);                        // the boss types, the NPC type, their attributes
        ArcItems.init(modBus);                           // the character component and the spawn egg
        ArcStory.init(modBus);                           // the story attachment, the Charter rows and items, the boss registry and buff rows, the defeat listener
        modContainer.registerConfig(ModConfig.Type.SERVER, ArcConfig.SPEC, "nusmp-arc-server.toml");
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent e) -> e.addListener(new DialogueLoader()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> DialogueLoader.clear());
        com.newuniverse.nusmp.arc.tier.TierSelfTests.register();   // 3.33.0: keys tier.*
        ArcSelfTests.register();                         // keys arc.*, expected by the server smoke test (SMOKE_EXPECT prefix "arc")
    }
}
