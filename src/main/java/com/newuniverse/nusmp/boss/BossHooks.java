package com.newuniverse.nusmp.boss;

import net.neoforged.neoforge.common.NeoForge;

/**
 * One call wires the boss overhaul's game-bus listeners: the buff engine (join, leave, tick, server stopping), the attack
 * profile (Barrier Piercing and Soul Eater riders) and the Black Clover stat buff. Call {@link #register()} once from the mod constructor.
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
        NeoForge.EVENT_BUS.addListener(BossStatBuff::onJoin);                         // 3.64.0: the Black Clover bosses x3
        NeoForge.EVENT_BUS.addListener(BossStatBuff::onIncomingDamage);
        BossStatSelfTests.register();                                                 // key boss.stats
    }
}
