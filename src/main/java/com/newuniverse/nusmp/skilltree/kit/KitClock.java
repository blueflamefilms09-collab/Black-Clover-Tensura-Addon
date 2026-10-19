package com.newuniverse.nusmp.skilltree.kit;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.function.IntConsumer;

/**
 * Why: a line technique that is not instant (a hammer that is shown and then falls, a floor that bites every half second, a tornado that walks, a Secret Art that winds up)
 * needs "run this some ticks from now" on the server, and the repo's older runtimes for that belong to the Black Clover grimoire, which the owner's isolation rule keeps out of the
 * skill tree. So this is the tree's own tiny clock: one queue of {@link Runnable}s (the pure {@link KitTaskQueue}) drained on {@code ServerTickEvent.Post}, the NeoForge game-bus event that
 * fires once per server tick after the world has moved (it is the event the repo's other runtimes use too), keyed by the world's game time so that a technique scheduled at tick T with
 * delay d runs on tick T+d whatever dimension it is in (game time is global in vanilla). It uses no entity, no block and no thread: an effect is a closure that holds the caster and the
 * level and re-checks that the caster is still there when it runs ({@link #present}), so a player who logged out or changed dimension simply ends the effect. The queue is
 * bounded, every task runs inside a catch so one bad effect can never stop the tick, and the queue is cleared when the server stops (a world that is gone must not run its effects in the next
 * one; this is the only static state and it is cleared at the right moment). {@link #drain} is public so the self-tests can play time forward without waiting for real ticks.
 * {@link #init} is idempotent and is called by each route's register class, so no integration line is needed.
 */
public final class KitClock {
    private KitClock() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final KitTaskQueue<Runnable> QUEUE = new KitTaskQueue<>(4096);
    private static boolean wired, warnedFull;

    /** Hooks the clock to the game bus once (safe to call from every route). */
    public static synchronized void init() {
        if (wired) return;
        wired = true;
        NeoForge.EVENT_BUS.addListener(KitClock::onServerTick);
        NeoForge.EVENT_BUS.addListener(KitClock::onServerStopping);
    }

    /** Runs {@code task} {@code delay} ticks from now (0 means this tick's end). False and the task dropped when the queue is full. */
    public static boolean later(ServerLevel level, int delay, Runnable task) {
        boolean added = QUEUE.add(level.getGameTime() + Math.max(0, delay), task);
        if (!added && !warnedFull) {
            warnedFull = true;
            LOG.warn("[nusmp] the technique effect queue is full ({} waiting); new delayed effects are dropped until it drains", QUEUE.size());
        }
        return added;
    }

    /**
     * Runs {@code step(age)} at age 0, then every {@code interval} ticks while the age is below {@code life}. A step that throws ends the zone (and is logged); a caster who is gone
     * is the step's own business (check {@link #present}).
     */
    public static void zone(ServerLevel level, int life, int interval, IntConsumer step) {
        later(level, 0, new Zone(level, Math.max(1, life), Math.max(1, interval), step));
    }

    /** True while {@code caster} can still be the author of a running effect: in the world, alive, and still in the level the effect started in. */
    public static boolean present(ServerPlayer caster, ServerLevel level) {
        return caster != null && !caster.isRemoved() && caster.isAlive() && caster.serverLevel() == level;
    }

    /** Runs every task due at or before {@code now}; returns how many ran. Each task is isolated: a throwing one is logged and the rest still run. */
    public static int drain(long now) {
        int ran = 0;
        for (Runnable r : QUEUE.popDue(now)) {
            ran++;
            try {
                r.run();
            } catch (Throwable t) {
                LOG.warn("[nusmp] a technique effect failed and was dropped", t);
            }
        }
        return ran;
    }

    /** How many effects are waiting (for the self-tests). */
    public static int pending() { return QUEUE.size(); }

    private static void onServerTick(ServerTickEvent.Post e) {
        if (QUEUE.isEmpty()) return;
        drain(e.getServer().overworld().getGameTime());
    }

    private static void onServerStopping(ServerStoppingEvent e) {
        QUEUE.clear();
        warnedFull = false;
    }

    private static final class Zone implements Runnable {
        private final ServerLevel level;
        private final int life, interval;
        private final IntConsumer step;
        private int age;

        Zone(ServerLevel level, int life, int interval, IntConsumer step) {
            this.level = level;
            this.life = life;
            this.interval = interval;
            this.step = step;
        }

        @Override
        public void run() {
            try {
                step.accept(age);
            } catch (Throwable t) {
                LOG.warn("[nusmp] a technique zone step failed and the zone ended", t);
                return;
            }
            age += interval;
            if (age < life) later(level, interval, this);
        }
    }
}
