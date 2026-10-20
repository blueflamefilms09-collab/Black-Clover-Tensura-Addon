package com.newuniverse.nusmp.labyrinth.mega.decennial;

import com.newuniverse.nusmp.dungeon.DungeonGuard;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import net.neoforged.bus.api.IEventBus;

/**
 * Why: the one entry point of the Decennial feature, the shape every system of the Mega-Dungeon has ({@code MegaSystem} calls {@link #init} on the mod bus and {@link #register} on the game
 * bus, one line each, and the mod class learns nothing else). It registers the packet that tells a client which blocks of a hall it may break, the Dungeon Guard's one explicit exception (the cells
 * of a hall's interactive elements), the floor-generation hook (a listener of the generator, like the ecology's and the Crucible's), and the in-game self-tests. It adds no block, item, entity type
 * or data component: the 200 bosses are Labyrinth Guardians wearing a profile.
 */
public final class DecennialRegistry {
    private DecennialRegistry() {}

    public static void init(IEventBus modBus) {
        modBus.addListener(DecennialNet::registerPayloads);
        DungeonGuard.allowBreakWhere(BreakableCells::breakable);
        MegaDungeon.addListener(new DecennialArenaHook());
        DecennialSelfTests.register();                                                    // the in-game self-tests (/multiverse mega selftest boss.decennial_*)
    }

    public static void register(IEventBus gameBus) {
        DecennialEvents.register(gameBus);
    }
}
