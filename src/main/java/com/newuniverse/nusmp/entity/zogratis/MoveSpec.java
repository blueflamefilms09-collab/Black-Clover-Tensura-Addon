package com.newuniverse.nusmp.entity.zogratis;

/**
 * What the scheduler needs to know about a move (pure; {@link Move} adds the code that runs). Ids are unique within one boss.
 */
public interface MoveSpec {
    String id();
    String label();
    /** Least power (percent) at which the boss may use it. */
    int minPower();
    /** Ticks of telegraph before it lands (0 = instant). */
    int windUpTicks();
    int cooldownTicks();
    /** The target must be at least this far (blocks) ... */
    double minRange();
    /** ... and at most this far. */
    double maxRange();
    /** Relative chance among the moves that are allowed now (at least 1). */
    int weight();
    /** At most one per power-tuned gap, and never twice in a row. */
    boolean ultimate();
    /** Mana it costs below 60% power (the unbound pool pays nothing). */
    default double manaCost() { return 0; }
    /** A crowd move: preferred against two or more foes, shunned against one. */
    default boolean crowd() { return false; }
}
