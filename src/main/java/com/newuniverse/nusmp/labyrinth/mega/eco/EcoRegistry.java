package com.newuniverse.nusmp.labyrinth.mega.eco;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Why: the registration entry point of the faction layer of the Mega-Dungeon (1.54.0): territorial wars between monster factions and the faction
 * drops on top of the stone-and-meat economy. {@code MegaSystem} calls {@link #init} and {@link #register} (the lead adds no other line), so the
 * package wires itself: {@code init} registers the one {@link com.newuniverse.nusmp.labyrinth.mega.SectorListener} that gives a freshly generated
 * sector's rooms their native factions, and {@code register} puts the handlers on the game bus with {@code addListener} (no annotation scanning,
 * like the rest of the mod): the data reload of the faction files, the tag-change cache flush, the two entity events of the turf wars, the faction
 * drops at LOW priority so they run after the 1.36.0 economy's own listener, and the cache flush when the server stops. {@code init} also registers
 * the package's in-game self-tests ({@link EcoSelfTests}, keys {@code eco.*}) with the shared registry the CI server smoke test runs. It registers
 * no blocks or items, so it has no ordering constraint against {@code NUItems.ITEMS}.
 */
public final class EcoRegistry {
    private EcoRegistry() {}

    public static void init(IEventBus modBus) {
        MegaDungeon.addListener(new FactionAssigner());
        EcoSelfTests.register();
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener((AddReloadListenerEvent e) -> e.addListener(new FactionReload()));
        gameBus.addListener(FactionRegistry::onTagsUpdated);
        gameBus.addListener(TurfWars::onJoin);
        gameBus.addListener(EventPriority.LOW, TurfWars::onChangeTarget);
        gameBus.addListener(EventPriority.LOW, FactionDrops::onDrops);
        gameBus.addListener((ServerStoppedEvent e) -> FactionRegistry.clearCache());
    }

    /** The {@code faction} admin node ({@code list | of | room | set}); the lead attaches it under {@code /multiverse mega}. */
    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return FactionCommand.command();
    }
}
