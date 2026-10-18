package com.newuniverse.nusmp.heroweapon.weapon.vassalkit;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Why: a technique with a wind-up, a pulse or an aura keeps running for ticks after the key press, long after the wielder may have died, logged out, changed
 * dimension, put the weapon away or released it, and a step that throws must never take the server tick down (the owner's server rules). Every delayed task
 * and repeating zone of the five vassal weapons goes through these two wrappers, which (1) rebuild the {@link HeroCtx} at the moment the step runs, so a bond
 * that ended or a weapon that changed makes the effect stop quietly instead of acting for a person who is no longer there, (2) clamp the running time to
 * 20 seconds (the owner's limit for a repeating task), and (3) log a failure once and end that one effect. The scheduler underneath is the mod's own
 * {@link SpellRuntime}.
 */
public final class Safe {
    private Safe() {}

    /** The longest any task of these weapons may keep running: 20 seconds. */
    public static final int MAX_TICKS = 400;

    /** The kinds of failure already logged. Strings only; emptied when the server stops so a second world in one session logs its own failures. */
    private static final Set<String> TOLD = new HashSet<>();

    static {
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> TOLD.clear());
    }

    /** One step of a zone: return false to end the zone early. */
    @FunctionalInterface
    public interface Step {
        boolean run(HeroCtx c, int age);
    }

    /** True while the person is still in the world and playing. */
    public static boolean alive(ServerPlayer p) {
        return p != null && !p.isRemoved() && p.isAlive() && !p.hasDisconnected();
    }

    /** A fresh context for the same person, weapon and level, or null when the person left, the bond ended, the weapon changed or they went elsewhere. */
    public static HeroCtx fresh(HeroCtx c) {
        if (!alive(c.player)) return null;
        HeroCtx f = HeroCtx.of(c.player);
        return f != null && f.kind == c.kind && f.level == c.level ? f : null;
    }

    /** Runs {@code body} after {@code delay} ticks (at most 20 seconds) with a fresh context; nothing happens if the person or the bond is gone by then. */
    public static void later(HeroCtx c, int delay, String what, Consumer<HeroCtx> body) {
        SpellRuntime.later(c.level, Math.max(0, Math.min(MAX_TICKS, delay)), () -> {
            try {
                HeroCtx f = fresh(c);
                if (f != null) body.accept(f);
            } catch (RuntimeException | LinkageError e) {
                told(what, e);
            }
        });
    }

    /**
     * Runs {@code step} every {@code interval} ticks for {@code ticks} ticks (at most 20 seconds) with a fresh context each time. The zone ends when the step returns
     * false, when the person or the bond is gone and, with {@code needSummoned}, when the weapon is no longer in their hand (a song stops when the player stops playing).
     */
    public static void zone(HeroCtx c, int ticks, int interval, String what, boolean needSummoned, Step step) {
        boolean[] over = {false};
        SpellRuntime.zone(c.level, Math.max(1, Math.min(MAX_TICKS, ticks)), Math.max(1, interval), age -> {
            if (over[0]) return;
            try {
                HeroCtx f = fresh(c);
                if (f == null || needSummoned && !f.summoned() || !step.run(f, age)) over[0] = true;
            } catch (RuntimeException | LinkageError e) {
                over[0] = true;
                told(what, e);
            }
        });
    }

    /** Logs a failed effect once per kind of effect, so one broken technique cannot flood the log. */
    private static void told(String what, Throwable e) {
        if (TOLD.size() < 200 && TOLD.add(what)) LogUtils.getLogger().warn("[nusmp] A vassal weapon technique ({}) stopped: {}", what, e.toString());
    }
}
