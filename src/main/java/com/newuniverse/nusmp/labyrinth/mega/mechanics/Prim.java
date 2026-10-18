package com.newuniverse.nusmp.labyrinth.mega.mechanics;

/**
 * Why: one effect primitive of the library (the ~80 composable verbs: apply an effect, damage, knock back, teleport, spawn a band, place or remove blocks, toggle a light, flip
 * gravity, raise water, lock doors, give a reward, change the weather ...). A primitive is a tiny object with a {@link #fire} that acts NOW on the run's targets and, for the
 * sustained ones (a wind that blows for six seconds, a platform that travels), a {@link #duration} in ticks, a {@link #tick} called every tick until it is over and an
 * {@link #end} called when it is. Blocks a primitive changes go through the run's journal, which the engine restores when the run ends or the mechanic is cleared, so a primitive
 * never has to remember to undo anything. Primitives must not throw for bad arguments (the engine catches, logs once and drops the effect, but a primitive that clamps and goes
 * on is better).
 */
public interface Prim {
    /** Acts now. */
    void fire(Run r);

    /** Ticks the run lasts after {@link #fire}; 0 = it was instantaneous and the run is over (its journal, if any, is kept until the mechanic ends). */
    default int duration(Run r) { return 0; }

    /** Called every tick while the run is alive (after {@link #fire}). */
    default void tick(Run r) {}

    /** Called once when the run ends: the duration passed, the mechanic ended or was cleared. Blocks are restored by the engine after this. */
    default void end(Run r) {}
}
