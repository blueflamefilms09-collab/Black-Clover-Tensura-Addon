package com.newuniverse.nusmp.labyrinth.mega.puzzle;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Why: the registration entry point of the puzzle rooms, the one class {@code MegaSystem} calls (the lead adds a single {@code init} and a single {@code register} line, as for every other
 * module). It registers no block, item or entity: {@code init} adds the in-game self-tests ({@link PuzzleSelfTests}, keys {@code puzzle.*}, which the CI smoke test runs); {@code register} wires
 * the game-bus handlers with {@code addListener} like the rest of the mod: the Director's tick, the right-click hook, the reload listener that reads the weights file from the data packs, the
 * player command the answer buttons run, and the server-stopping cleanup. The lead attaches {@link #command()} under {@code /multiverse mega}.
 */
public final class PuzzleRegistry {
    private PuzzleRegistry() {}

    public static void init(IEventBus modBus) { PuzzleSelfTests.register(); }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(PuzzleDirector::onServerTick);
        gameBus.addListener(PuzzleEvents::onRightBlock);
        gameBus.addListener(PuzzleEvents::onStopping);
        gameBus.addListener((AddReloadListenerEvent e) -> e.addListener(new PuzzleWeightsReload()));
        gameBus.addListener((RegisterCommandsEvent e) -> e.getDispatcher().register(PuzzleCommand.player()));
    }

    /** The {@code puzzle} admin node ({@code list | info <id> | apply <id> here | status | plan | clear | reset | verify}). */
    public static LiteralArgumentBuilder<CommandSourceStack> command() { return PuzzleCommand.admin(); }
}
