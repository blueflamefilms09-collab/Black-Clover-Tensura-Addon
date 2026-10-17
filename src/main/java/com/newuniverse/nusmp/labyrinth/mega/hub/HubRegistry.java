package com.newuniverse.nusmp.labyrinth.mega.hub;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.bus.api.IEventBus;

/**
 * Why: the registration entry point of Floor 0, the Sanctuary Hub (1.55.0): the safe, buildable plains dimension between the Overworld Gateway and the two
 * roads of the Mega-Dungeon. {@code MegaSystem} calls {@link #init} and {@link #register} (the lead adds no other line), so the package wires itself:
 * {@code init} (mod bus, before {@code NUItems.ITEMS.register}) registers the biome modifier serializer that keeps the Sanctuary's biome peaceful and plain
 * ({@link SanctuaryPeaceModifier}) and the in-game self-tests ({@link HubSelfTests}, keys {@code hub.*}) with the shared registry the CI server smoke test
 * runs; {@code register} (game bus) adds the rule handlers of {@link HubRules}. The package registers no block and no item (the gates are the existing
 * {@code nusmp:labyrinth_gate}), so it has no ordering constraint against {@code NUItems.ITEMS}. The travel entry points are the static methods of
 * {@link HubTravel} that the lead's routing in {@code LabyrinthTravel.enter} calls; the dimension, its type and the biome are data files.
 */
public final class HubRegistry {
    private HubRegistry() {}

    public static void init(IEventBus modBus) {
        SanctuaryPeaceModifier.init(modBus);
        HubSelfTests.register();
    }

    public static void register(IEventBus gameBus) {
        HubRules.register(gameBus);
    }

    /** The {@code hub} admin node ({@code tp [player] | status | plaza | gates}); the lead attaches it under {@code /multiverse mega}. */
    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return HubCommand.command();
    }
}
