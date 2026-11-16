package com.newuniverse.nusmp.saiyan;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: the Saiyan system (docs/saiyan.md) is one self-contained feature, so it has one entry point: the single line the mod constructor calls. It registers the save attachment, the server config
 * {@code nusmp-saiyan-server.toml}, the two packets, the game events, the commands and the CI self-tests {@code saiyan.*}. Safe to call twice. The client half (keys, HUD, hiding Tensura's magicule bar
 * for a Saiyan) is {@code client.saiyan.SaiyanClient}, wired by the client setup; this class names no client class.
 */
public final class SaiyanBoot {
    private SaiyanBoot() {}

    private static boolean done;

    /** The single entry point, called from the mod constructor on both sides. */
    public static synchronized void init(IEventBus modBus, ModContainer container) {
        if (done) return;
        done = true;
        SaiyanStore.ATTACHMENTS.register(modBus);
        SaiyanRaces.RACES.register(modBus);             // the real Tensura race anime_multiverse:saiyan (always registered; used only with phase.race.tensuraRace)
        io.github.manasmods.manascore.race.api.RaceEvents.SET_RACE.register(SaiyanRaceLink::onSetRace);          // a race set through Tensura's own menu / commands sets the flag
        container.registerConfig(ModConfig.Type.SERVER, SaiyanConfig.SPEC, "nusmp-saiyan-server.toml");
        container.registerConfig(ModConfig.Type.SERVER, SaiyanPhaseConfig.SPEC, "nusmp-saiyan-phase2-server.toml");      // Dragon Ball rework (phases 2 to 6, Giant Ki Avatar)
        container.registerConfig(ModConfig.Type.SERVER, SaiyanProdigyConfig.SPEC, "nusmp-saiyan-prodigy-server.toml");      // the Saiyan Prodigy hub and the Saiyan-blooded evolution (docs/saiyan_prodigy.md)
        com.newuniverse.nusmp.skill.ProdigySkills.init();          // registers anime_multiverse:saiyan_prodigy before the registry opens
        modBus.addListener(SaiyanNet::registerPayloads);
        SaiyanEvents.register(NeoForge.EVENT_BUS);
        SaiyanCommands.register(NeoForge.EVENT_BUS);
        SaiyanBlood.register(NeoForge.EVENT_BUS);                 // the deeds of the Saiyan-blooded evolution (Human to Saiyan)
        SaiyanSelfTests.register();
        SaiyanStage2SelfTests.register();
        SaiyanTechSelfTests.register();
        SaiyanTech3SelfTests.register();
        SaiyanLookSelfTests.register();
        SaiyanPhaseSelfTests.register();                // Dragon Ball rework: db.phase2.* and saiyan.t3.*
        SaiyanRaceSelfTests.register();                 // db.phase2.tensura.*: the real Tensura race (inside the db.phase2 smoke prefix)
        SaiyanProdigySelfTests.register();              // prodigy.*: the hub, the skill, the evolution (docs/saiyan_prodigy.md)
        SaiyanKiFx.registerSelfTests();                 // the anime Ki effects (KiLayer): saiyan.kivfx
        com.newuniverse.nusmp.saiyan.armor.SaiyanArmorBoot.init(modBus, container);      // the craftable Saiyan alloy armour set: items, material, nusmp-saiyan-armor-server.toml, saiyan.armor.* (docs/saiyan_armor.md)
    }
}
