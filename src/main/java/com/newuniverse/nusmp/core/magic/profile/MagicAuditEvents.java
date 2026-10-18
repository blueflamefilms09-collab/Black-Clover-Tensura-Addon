package com.newuniverse.nusmp.core.magic.profile;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/**
 * Why: the magic framework's startup audit ({@link MagicAuditRuntime#runAtStartup()}) and the fault ledger reset used to ride on the merge generator's server events. The generator is gone
 * (1.93.0), the audit is not: this is its own two listeners. One line in NUSMP registers them: {@code MagicAuditEvents.register(NeoForge.EVENT_BUS)}.
 */
public final class MagicAuditEvents {
    private MagicAuditEvents() {}

    public static void register(IEventBus gameBus) {
        gameBus.addListener(MagicAuditEvents::onServerStarted);
        gameBus.addListener(MagicAuditEvents::onServerStopping);
        RetiredMagic.register(gameBus);
    }

    /** The startup audit of the magic framework (errors one by one, warnings as a summary). */
    public static void onServerStarted(ServerStartedEvent e) {
        try {
            MagicAuditRuntime.runAtStartup();
        } catch (RuntimeException ex) {
            SafeCast.fault("magic-audit", ex);
        }
    }

    /** Leaving the world: the fault ledger starts clean. */
    public static void onServerStopping(ServerStoppingEvent e) { SafeCast.reset(); }
}
