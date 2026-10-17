package com.newuniverse.nusmp.labyrinth.mega.gear;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

/**
 * Why: the registration entry point of the standard Normal / Special / Rare equipment that monsters of the Mega-Dungeon drop without limit (1.55.0).
 * {@code MegaSystem} already calls {@link #init} and {@link #register}, so the package wires itself and the lead adds no line for it except the config and
 * the command. {@code init} (mod bus, before the registries freeze and before {@code NUItems.ITEMS.register}) registers the one data component
 * {@code nusmp:gear_grade} and the in-game self-tests ({@link GearSelfTests}, keys {@code gear.*}); {@code register} (game bus, {@code addListener}
 * with no annotation scanning, like the rest of the mod) adds the data reload of the drop tables, the drop listener at LOW priority so it runs after the
 * 1.36.0 economy's and the faction layer's listeners (it only ADDS an item and never touches what they decided), the magic resistance at LOW priority so
 * it cuts the final amount after other systems have multiplied it, and the quest hook on pickup. It registers no items and no blocks (standard gear is
 * ordinary vanilla items), so it has no ordering constraint against {@code NUItems}.
 */
public final class GearRegistry {
    private GearRegistry() {}

    public static void init(IEventBus modBus) {
        GearComponents.COMPONENTS.register(modBus);
        GearSelfTests.register();
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener((AddReloadListenerEvent e) -> e.addListener(new GearTablesReload()));
        gameBus.addListener(EventPriority.LOW, GearDrops::onDrops);
        gameBus.addListener(EventPriority.LOW, GearEvents::onIncomingDamage);
        gameBus.addListener(GearEvents::onPickup);
    }

    /** The {@code gear} admin node ({@code give | roll | status}); the lead attaches it under {@code /multiverse mega}. */
    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return GearCommand.command();
    }
}
