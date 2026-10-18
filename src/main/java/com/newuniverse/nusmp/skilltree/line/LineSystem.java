package com.newuniverse.nusmp.skilltree.line;

import com.newuniverse.nusmp.skilltree.ft.kit.FtClock;
import net.neoforged.bus.api.IEventBus;

/**
 * Why: the line engine is several small parts (the payloads on the mod bus, the combat and beat events on the game bus, the shared clock for delayed arts, the in-game self-tests) that must each
 * start exactly once, and the Skill Tree core should have ONE line for all of it. This is that line; {@code TreeCore.init} calls it, so no other file of the mod needs to know the engine exists. It is
 * safe to call twice. The engine has no registry entry of its own: no item, no skill, no attachment (the player's data is the existing skill-tree record plus the player's own persistent data), which is
 * exactly why 275 lines do not touch any frozen registry.
 */
public final class LineSystem {
    private LineSystem() {}

    private static boolean done;

    public static synchronized void init(IEventBus modBus) {
        if (done) return;
        done = true;
        modBus.addListener(LineNet::registerPayloads);
        FtClock.init();
        LineEvents.register();
        LineSelfTests.register();
    }
}
