package com.newuniverse.nusmp.heroweapon.weapon.pierce;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.skill.SimpleSkill;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntConsumer;

/**
 * Why: almost every technique of the Spear and Bow does something now and something a few ticks later (a glint, then the thrust; a ring on the ground, then
 * the falling arrows), and the later part runs inside the server tick, long after the wielder may have died, left, changed world or put the weapon away. A
 * step that throws there would stop that one effect at best. These helpers wrap the mod's own timers ({@link SpellRuntime}, which already catches and logs a
 * failing step) so that a delayed step runs only while the wielder is alive, online and in the same world, a run of pulses has exactly the number of pulses
 * the numbers table says, and a skill body that throws is logged once and refused instead of leaving the dispatcher in the middle of a cast.
 */
public final class Tasks {
    private Tasks() {}

    /** The hooks that already told the log they failed: a hook that runs every few ticks must not fill the log (a fixed set of names, never world state). */
    private static final Set<String> TOLD = ConcurrentHashMap.newKeySet();

    /** Logs the first failure of a named hook or step, then stays quiet about it. */
    public static void warnOnce(String what, Throwable e) {
        if (TOLD.add(what)) LogUtils.getLogger().warn("[nusmp] hero weapon {} failed: {}", what, e.toString());
    }

    /** The names of the hooks and techniques whose first failure was logged and begin with {@code prefix}: how a self-test learns that a guarded body threw. */
    public static List<String> failed(String prefix) {
        List<String> out = new ArrayList<>();
        for (String name : TOLD) if (name.startsWith(prefix)) out.add(name);
        Collections.sort(out);
        return out;
    }

    /** True while the wielder is alive, online and still in the world the technique began in. */
    public static boolean live(HeroCtx c) {
        return c.player.isAlive() && !c.player.hasDisconnected() && c.player.serverLevel() == c.level;
    }

    /** Runs {@code run} after {@code delay} ticks, but only if the wielder is still {@link #live}. */
    public static void later(HeroCtx c, int delay, Runnable run) {
        SpellRuntime.later(c.level, Math.max(0, delay), () -> {
            if (live(c)) run.run();
        });
    }

    /**
     * Runs {@code step(0)}, {@code step(1)} ... {@code step(pulses - 1)}, {@code interval} ticks apart, the first one about {@code delay} ticks from now, each
     * only while the wielder is {@link #live}. (The timer of the mod runs a zone from the tick AFTER the one that created it, at ages 0, interval, 2 *
     * interval ... while the zone's clock has not passed its end; the zone is created from inside a delayed task, so it is given one tick more than
     * {@code (pulses - 1) * interval} and has exactly {@code pulses} steps.)
     */
    public static void pulses(HeroCtx c, int delay, int pulses, int interval, IntConsumer step) {
        int n = Math.max(1, pulses), gap = Math.max(1, interval);
        later(c, delay, () -> SpellRuntime.zone(c.level, (n - 1) * gap + 1, gap, age -> {
            if (live(c)) step.accept(age / gap);
        }));
    }

    /** A skill body that logs once and refuses when it throws, so a bug in one technique never reaches the dispatcher. */
    public static SimpleSkill.Body guard(String id, SimpleSkill.Body body) {
        return c -> {
            try {
                return body.cast(c);
            } catch (RuntimeException | LinkageError e) {
                warnOnce("skill " + id, e);
                return false;
            }
        };
    }
}
