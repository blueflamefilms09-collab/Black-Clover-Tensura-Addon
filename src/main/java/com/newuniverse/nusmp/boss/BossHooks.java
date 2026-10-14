package com.newuniverse.nusmp.boss;

import net.neoforged.neoforge.common.NeoForge;

/**
 * One call wires the boss overhaul's game-bus listeners: the buff engine (join, leave, tick, server stopping) and the attack
 * profile (Barrier Piercing and Soul Eater riders). Call {@link #register()} once from the mod constructor.
 */
public final class BossHooks {
    private BossHooks() {}

    private static boolean done;

    public static void register() {
        if (done) return;
        done = true;
        NeoForge.EVENT_BUS.addListener(BossBuffEngine::onJoin);
        NeoForge.EVENT_BUS.addListener(BossBuffEngine::onLeave);
        NeoForge.EVENT_BUS.addListener(BossBuffEngine::onServerTick);
        NeoForge.EVENT_BUS.addListener(BossBuffEngine::onServerStopping);
        NeoForge.EVENT_BUS.addListener(BossEngravings::onIncomingDamage);
    }
}
