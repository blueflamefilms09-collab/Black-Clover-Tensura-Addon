package com.newuniverse.nusmp.skilltree.line;

import com.newuniverse.nusmp.skilltree.ft.kit.FtClock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;

/**
 * Why: a whirlwind that hits five times, a volley, a meteor that lands after a second and a leap that slams when it comes down are not one instant, and the server must run their later parts
 * without a thread or an entity. The Skill Tree already has exactly one clock for this ({@code FtClock}, a bounded queue drained on the server tick, cleared when the server stops), so the line
 * engine uses it instead of starting a second one, and adds the one rule every delayed part of a player's art needs: before it runs, the caster must still be in the same level, alive and
 * connected ({@code FtClock.present}); a player who died, logged out or changed dimension simply ends the art. A part that throws is isolated by the clock. The helpers hold the caster and level
 * only for as long as the part is waiting, and the clock's queue is bounded, so a spammed art can never grow the server's memory.
 */
public final class LineTimers {
    private LineTimers() {}

    /** Runs {@code task} after {@code delay} ticks if the caster is still there. */
    public static void later(ServerPlayer p, int delay, Runnable task) {
        ServerLevel level = p.serverLevel();
        FtClock.later(level, Math.max(0, delay), () -> {
            if (FtClock.present(p, level)) task.run();
        });
    }

    /** Runs {@code step(i)} for i = 0..count-1, the first at once, then one every {@code interval} ticks, while the caster is still there. */
    public static void repeat(ServerPlayer p, int count, int interval, IntConsumer step) {
        int n = Math.max(1, Math.min(count, 40));
        int every = Math.max(1, interval);
        for (int i = 0; i < n; i++) {
            final int index = i;
            if (i == 0) step.accept(0);
            else later(p, i * every, () -> step.accept(index));
        }
    }

    /**
     * Checks {@code ready} first after {@code firstDelay} ticks and then every {@code interval} ticks, up to {@code maxTicks} in all; runs {@code then} as soon as it is true or the time is up
     * (a leap that never lands, a player stuck in a web, must still end). The caster must still be there at each check.
     */
    public static void waitUntil(ServerPlayer p, int firstDelay, int interval, int maxTicks, BooleanSupplier ready, Runnable then) {
        step(p, Math.max(1, firstDelay), Math.max(1, interval), Math.max(firstDelay, maxTicks), firstDelay, ready, then);
    }

    private static void step(ServerPlayer p, int delay, int interval, int maxTicks, int elapsedAtCheck, BooleanSupplier ready, Runnable then) {
        later(p, delay, () -> {
            if (ready.getAsBoolean() || elapsedAtCheck >= maxTicks) {
                then.run();
                return;
            }
            step(p, interval, interval, maxTicks, elapsedAtCheck + interval, ready, then);
        });
    }
}
