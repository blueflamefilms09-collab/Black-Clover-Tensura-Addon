package com.newuniverse.nusmp.skilltree.line;

import com.newuniverse.nusmp.skilltree.kit.KitClock;

/**
 * Why: the line engine is several small parts (the combat and beat events on the game bus and the shared clock for delayed arts) that must each
 * start exactly once, and the skill book system should have ONE line for all of it. This is that line; {@code SkillBookSystem.init} calls it, so no other file of the mod needs to know the engine exists. It is
 * safe to call twice. The engine has no registry entry of its own: no item, no skill, no attachment (the player's own persistent data holds the cooldowns), which is
 * exactly why a hundred lines of arts do not touch any frozen registry.
 */
public final class LineSystem {
    private LineSystem() {}

    private static boolean done;

    public static synchronized void init() {
        if (done) return;
        done = true;
        KitClock.init();
        LineEvents.register();
        EvolutionSelfTests.register();                              // 3.26.0: evolution.counters, evolution.debt
    }
}
