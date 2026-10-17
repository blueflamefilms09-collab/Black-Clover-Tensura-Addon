package com.newuniverse.nusmp.labyrinth.mega.flow;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

/**
 * Why: the registration entry point of the Rukh flow of the Holy floors and the decennial Metal Vessel (1.55.0). {@code MegaSystem} calls {@link #init}
 * and {@link #register} (the lead adds no other line), so the package wires itself. {@code init} registers the package's in-game self-tests
 * ({@link FlowSelfTests}, keys {@code flow.*}) with the shared registry the CI server smoke test runs; it registers no blocks, items or DeferredRegisters,
 * so it has no ordering constraint against {@code NUItems.ITEMS}. {@code register} puts the handlers on the game bus with {@code addListener} (no
 * annotation scanning, like the rest of the mod): the data reload of the classification table; the damage event that multiplies a matching cast, at HIGH
 * priority so the target's own mitigation and the mod's caps see the boosted number; the drops event that hands out the Metal Vessel; the 40-tick ambient
 * Rukh; the logout and stop events that empty the memory of casts; and the listener on ManasCore's skill activations. The hook that reports grimoire casts is
 * not a registration: {@code QuestHooks.onCast} calls {@link FlowCasts#onCast} (one line, in the lead's integration notes).
 */
public final class FlowRegistry {
    private FlowRegistry() {}

    public static void init(IEventBus modBus) {
        FlowSelfTests.register();
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener((AddReloadListenerEvent e) -> e.addListener(new AlignmentReload()));
        gameBus.addListener(EventPriority.HIGH, FlowEvents::onIncoming);
        gameBus.addListener(FlowEvents::onLogout);
        gameBus.addListener(FlowEvents::onStopping);
        gameBus.addListener(FlowAir::onServerTick);
        gameBus.addListener(DecennialReward::onDrops);
        FlowCasts.registerSkillHook();
    }

    /** The {@code flow} admin node ({@code status | classify | vessel list | vessel give}); the lead attaches it under {@code /multiverse mega}. */
    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return FlowCommand.command();
    }
}
