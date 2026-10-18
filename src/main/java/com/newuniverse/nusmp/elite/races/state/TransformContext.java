package com.newuniverse.nusmp.elite.races.state;

/**
 * Why: the state machine must be testable without a server, so it reads the world and the player only through this view. The real one ({@code runtime.PlayerTransformCtx}) is built
 * from a ServerPlayer each time it is needed; the tests use a small fake with a settable clock.
 */
public interface TransformContext {
    /** The race tier, 1 (lowest) upward. */
    int tier();

    /** The maximum EP. */
    double ep();

    /** Current hit points over maximum, 0..1. */
    double hpFraction();

    boolean night();

    boolean fullMoon();

    boolean skyVisible();

    int itemCount(String itemId);

    /** Current aura over maximum, 0..1 (the ki pool the states drain); 1 when unknown. */
    double auraFraction();

    /** The game clock in ticks. */
    long now();

    default double epScale() { return 1.0; }
}
