package com.newuniverse.nusmp.heroweapon.weapon.sword;

import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.IntConsumer;

/**
 * Why: the Sword remembers its wielder's run of blows and marks between one call and the next ({@link SwordState}), and its techniques act after a wind-up or over
 * several pulses, so they need a memory and two scheduling helpers that every technique uses. This class is that memory and those helpers. Rules it keeps: server
 * thread only; all state is dropped when the wielder leaves or the server stops (a cache, not a save); a delayed effect is skipped if the wielder died, left, changed
 * level or no longer holds the Sword, and a repeating task ends by itself with the wielder; an exception in a task is caught and logged once by the spell runtime,
 * never thrown into the tick.
 */
final class SwordRuntime {
    private SwordRuntime() {}

    private static final Map<UUID, SwordState> STATES = new HashMap<>();
    private static boolean registered;

    /** Hooks the events (once; the logic's constructor calls it on both sides of the game). */
    static synchronized void register(IEventBus game) {
        if (registered) return;
        registered = true;
        game.addListener(SwordRuntime::onServerStopped);
        game.addListener(SwordRuntime::onLogout);
    }

    /** The state of a wielder, made on first use. */
    static SwordState state(ServerPlayer p) { return STATES.computeIfAbsent(p.getUUID(), k -> new SwordState()); }

    /** The state of a wielder if there is one (no allocation). */
    static SwordState peek(UUID id) { return STATES.get(id); }

    static void forget(UUID id) { STATES.remove(id); }

    static void clearAll() { STATES.clear(); }

    static void onServerStopped(ServerStoppedEvent e) { clearAll(); }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { forget(e.getEntity().getUUID()); }

    /** True while the wielder is still here, in the level the technique began in, and still bound to the Sword: a delayed effect checks it before it acts. */
    static boolean live(HeroCtx c) {
        ServerPlayer p = c.player;
        return !p.isRemoved() && p.isAlive() && p.serverLevel() == c.level && HeroBonds.kindOf(p) == HeroKind.SWORD;
    }

    /** Runs {@code effect} after {@code ticks} (at once for 0), unless the wielder is gone by then. */
    static void after(HeroCtx c, int ticks, Runnable effect) {
        if (ticks <= 0) {
            effect.run();
            return;
        }
        SpellRuntime.later(c.level, ticks, () -> {
            if (live(c)) effect.run();
        });
    }

    /**
     * A repeating task: {@code step} gets the number of ticks since the task began (0, interval, 2 interval ...) and runs while the wielder is here, for at most
     * {@code ticks} ticks and never longer than the 20 seconds of the law.
     */
    static void every(HeroCtx c, int ticks, int interval, IntConsumer step) {
        SpellRuntime.zone(c.level, Math.min(ticks, SwordNumbers.MAX_TASK_TICKS), Math.max(1, interval), age -> {
            if (live(c)) step.accept(age);
        });
    }
}
