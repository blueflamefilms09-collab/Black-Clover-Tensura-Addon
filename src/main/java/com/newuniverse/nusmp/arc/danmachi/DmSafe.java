package com.newuniverse.nusmp.arc.danmachi;

import com.mojang.logging.LogUtils;

import java.util.function.IntConsumer;

/**
 * Why: the zones and delayed tasks of the DanMachi monsters run inside the server tick, long after the move that made them started, and the boss may be
 * gone, its arena unloaded or a foe removed by then. A step that throws must stop that one effect and say so once in the log, never take the tick (and
 * every other fight on the server) down. Every zone step and every delayed task of this package goes through these two wrappers.
 */
final class DmSafe {
    private DmSafe() {}

    /** A zone step that stops for good after its first failure (logged once with what it was). */
    static IntConsumer step(String what, IntConsumer step) {
        boolean[] dead = {false};
        return age -> {
            if (dead[0]) return;
            try {
                step.accept(age);
            } catch (RuntimeException | LinkageError e) {
                dead[0] = true;
                LogUtils.getLogger().warn("[nusmp] A DanMachi monster's {} stopped: {}", what, e.toString());
            }
        };
    }

    /** A delayed task that logs its failure once instead of throwing into the tick. */
    static Runnable task(String what, Runnable task) {
        IntConsumer guarded = step(what, age -> task.run());
        return () -> guarded.accept(0);
    }
}
