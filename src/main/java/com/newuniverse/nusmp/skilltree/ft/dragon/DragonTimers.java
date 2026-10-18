package com.newuniverse.nusmp.skilltree.ft.dragon;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

/**
 * Why: the game has no "do this in 7 ticks" for a spell, and the Dragon Slayer techniques need it (the inhale before a roar, the flight of a fireball, the pulses of the Secret
 * Art, the bolts of Thunder Palace). A server-tick listener on the NeoForge game bus, draining a bounded {@link DelayQueue}, is the smallest tool that does it on the server
 * thread without a second thread or a block entity, and it keeps the whole technique server-side: the client only ever sees the effect packets. The clock is the SERVER tick
 * count, not a level's game time, so a task due in 7 ticks is due in 7 ticks whatever dimension the caster stands in. Tasks never throw out of the tick (a failing one is
 * logged and dropped, so one bad technique cannot stop the others or the server), the queue is bounded, and it is emptied when the server stops so that no task keeps a
 * level or a player of a finished world alive. Both Dragon Slayer routes call {@link #init} from their register class; it is safe to call twice.
 */
public final class DragonTimers {
    private DragonTimers() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final DelayQueue<Runnable> QUEUE = new DelayQueue<>(2048);
    private static boolean registered;

    /** Starts listening to the server tick and the server stop. Idempotent. */
    public static synchronized void init() {
        if (registered) return;
        registered = true;
        NeoForge.EVENT_BUS.addListener(DragonTimers::onServerTick);
        NeoForge.EVENT_BUS.addListener(DragonTimers::onServerStopping);
    }

    /**
     * Runs {@code task} on the server thread {@code delayTicks} server ticks from now (0 means the next tick). False when the queue is full: the caller treats the technique as
     * having fizzled and does not retry.
     */
    public static boolean later(ServerLevel level, int delayTicks, Runnable task) {
        return QUEUE.add(level.getServer().getTickCount() + Math.max(0, delayTicks), task);
    }

    /** How many tasks wait (the self-test uses it to prove that a delayed technique really queued its follow-up). */
    public static int pending() { return QUEUE.size(); }

    /** Runs what is due, once per server tick, after everything else of the tick has moved (so a follow-up sees where the foes ended up). Public like the mod's other handlers. */
    public static void onServerTick(ServerTickEvent.Post event) {
        for (Runnable r : QUEUE.drainDue(event.getServer().getTickCount())) {
            try {
                r.run();
            } catch (Throwable t) {
                LOG.warn("[nusmp] a Dragon Slayer follow-up failed and was dropped: {}", t.toString());
            }
        }
    }

    /** The server is stopping: nothing queued may outlive the world it was queued in. */
    public static void onServerStopping(ServerStoppingEvent event) {
        QUEUE.clear();
        DragonSkill.clearFailures();
    }
}
