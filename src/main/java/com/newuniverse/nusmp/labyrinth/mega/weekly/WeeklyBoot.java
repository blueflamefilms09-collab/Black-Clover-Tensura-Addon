package com.newuniverse.nusmp.labyrinth.mega.weekly;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/** Why: the one place that wires the weekly dungeon twists into the game (one call from the mod constructor): the server config, the join handler and the self-tests. */
public final class WeeklyBoot {
    private WeeklyBoot() {}

    public static void init(ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, WeeklyConfig.SPEC, "nusmp-labyrinth-mega-weekly-server.toml");
        WeeklyEvents.register(NeoForge.EVENT_BUS);
        FirstClearListener first = new FirstClearListener();
        com.newuniverse.nusmp.labyrinth.mega.MegaDungeon.addListener(first);
        com.newuniverse.nusmp.labyrinth.mega.MegaDungeon.addFloorListener(first);
        WeeklySelfTests.register();
    }
}
