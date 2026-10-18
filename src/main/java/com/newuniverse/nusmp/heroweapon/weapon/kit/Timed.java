package com.newuniverse.nusmp.heroweapon.weapon.kit;

import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Why: a good technique shows its tell first and lands a moment later (a ring on the ground, then the blow; a blade out, then the same blade back), and the
 * dispatcher only calls {@code cast} once, at the end of the wind-up. The delayed half therefore has to be scheduled by the technique itself, and it must be
 * safe: by the time it runs the wielder may have died, left, changed dimension, lost the bond or taken another weapon. These two wrappers put the tick-driven
 * runner of the older spells ({@link SpellRuntime}, which already catches every exception so a broken step never takes the server tick down) around a fresh
 * context: the delayed code gets a live {@link HeroCtx} of the wielder (current level, form and power), or does not run at all. Server thread only.
 */
public final class Timed {
    private Timed() {}

    /** The delayed half of a technique. */
    @FunctionalInterface
    public interface Task {
        void run(HeroCtx c);
    }

    /** One step of a field: {@code age} counts ticks since the field began (0, interval, 2 * interval ...). */
    @FunctionalInterface
    public interface Step {
        void run(HeroCtx c, int age);
    }

    /** Runs {@code task} after {@code delay} ticks if the wielder is still alive, in the same level and bonded to the same weapon. */
    public static void later(HeroCtx c, int delay, Task task) {
        ServerPlayer p = c.player;
        ServerLevel level = c.level;
        HeroKind kind = c.kind;
        SpellRuntime.later(level, Math.max(0, delay), () -> {
            HeroCtx live = live(p, level, kind);
            if (live != null) task.run(live);
        });
    }

    /**
     * Runs {@code step} every {@code interval} ticks for {@code ticks} ticks (so a field of N pulses lasts {@code (N - 1) * interval}); a pulse is skipped, not
     * queued, when the wielder is gone.
     */
    public static void zone(HeroCtx c, int ticks, int interval, Step step) {
        ServerPlayer p = c.player;
        ServerLevel level = c.level;
        HeroKind kind = c.kind;
        SpellRuntime.zone(level, Math.max(0, ticks), Math.max(1, interval), age -> {
            HeroCtx live = live(p, level, kind);
            if (live != null) step.run(live, age);
        });
    }

    /** A fresh context of {@code p} when it is still the same living wielder of {@code kind} in {@code level}, else null. */
    public static HeroCtx live(ServerPlayer p, ServerLevel level, HeroKind kind) {
        if (p == null || p.isRemoved() || !p.isAlive() || p.level() != level) return null;
        HeroCtx c = HeroCtx.of(p);
        return c != null && c.kind == kind ? c : null;
    }
}
