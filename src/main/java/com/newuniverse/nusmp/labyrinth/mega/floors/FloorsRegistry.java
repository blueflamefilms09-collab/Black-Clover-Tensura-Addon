package com.newuniverse.nusmp.labyrinth.mega.floors;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.bus.api.IEventBus;

/**
 * Why: the registration entry point of the floors and the Directional Lock of the bidirectional Mega-Dungeon (1.55.0). {@code MegaSystem} calls
 * {@link #init} and {@link #register} (the lead adds no other line for the code), so the package wires itself. {@code init} registers what must exist before
 * the registries freeze or before the first sector is generated: the player attachment {@code nusmp:dungeon_alignment} and the {@code nusmp:alignment_sync}
 * packet on the mod bus, the {@link FloorAssigner} that numbers a sector's rooms when the generator announces it, the {@link LockPolicy} that every door
 * asks through {@code MegaDungeon.mayEnter}, and the module's in-game self-tests ({@link FloorsSelfTests}, keys {@code floors.*}). {@code register} puts
 * the {@link FloorTracker} on the game bus: the tick that watches the floors and runs the hard guard, and the login, logout, respawn, dimension and stop
 * events that keep its memory and the client's picture right. It registers no block and no item, so it has no ordering constraint against
 * {@code NUItems.ITEMS}. The server config {@link FloorsConfig#SPEC} and the {@link #command} node are attached by the lead (see the module's doc).
 */
public final class FloorsRegistry {
    private FloorsRegistry() {}

    public static void init(IEventBus modBus) {
        AlignmentAttachments.ATTACHMENTS.register(modBus);
        modBus.addListener(AlignmentNet::registerPayloads);
        MegaDungeon.addListener(new FloorAssigner());
        MegaDungeon.addFloorListener(new FloorGrowth());                    // 1.58.0: a party entering a floor starts the building of the next one
        MegaDungeon.setAlignmentPolicy(new LockPolicy());
        FloorsSelfTests.register();
    }

    public static void register(IEventBus gameBus) {
        FloorTracker.register(gameBus);
    }

    /** The {@code floors} admin node ({@code here | player | set | lock | unlock | reset | bands | assign}); the lead attaches it under {@code /multiverse mega}. */
    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return FloorsCommand.command();
    }
}
