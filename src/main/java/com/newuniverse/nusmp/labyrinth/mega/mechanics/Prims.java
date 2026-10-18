package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

/**
 * Why: the registry of the eighty effect primitives, the bridge between the data ({@code "op": "damage"}) and the code that does it. They are written as small lambdas in four
 * themed classes ({@link PrimsCreature}, {@link PrimsWorld}, {@link PrimsMob}, {@link PrimsEnv}) and collected here once; the in-game self-test walks this map against the catalogue
 * ({@code ops.json}) and demands an executor for every id and an id for every executor, then runs every one of them on a scratch volume. The two builders keep a primitive's
 * definition to one expression: {@link #once} for an instantaneous effect, {@link #timed} for a sustained one with a duration, a per-tick step and an end.
 */
public final class Prims {
    private Prims() {}

    /** A duration that lasts until the mechanic itself ends. */
    public static final int FOREVER_TICKS = Integer.MAX_VALUE / 4;

    private static final Map<String, Prim> MAP = new LinkedHashMap<>();
    private static boolean booted;

    private static synchronized void boot() {
        if (booted) return;
        booted = true;
        PrimsCreature.register(MAP);
        PrimsWorld.register(MAP);
        PrimsMob.register(MAP);
        PrimsEnv.register(MAP);
    }

    /** The executor of a primitive, or null. */
    public static Prim get(String id) {
        if (!booted) boot();
        return MAP.get(id);
    }

    /** All executor ids, in registration order. */
    public static Set<String> ids() {
        if (!booted) boot();
        return Collections.unmodifiableSet(MAP.keySet());
    }

    static Prim once(Consumer<Run> f) {
        return new Prim() {
            @Override public void fire(Run r) { f.accept(r); }
        };
    }

    static Prim timed(ToIntFunction<Run> duration, Consumer<Run> fire, Consumer<Run> tick, Consumer<Run> end) {
        return new Prim() {
            @Override public void fire(Run r) { fire.accept(r); }
            @Override public int duration(Run r) { return duration.applyAsInt(r); }
            @Override public void tick(Run r) { tick.accept(r); }
            @Override public void end(Run r) { end.accept(r); }
        };
    }
}
