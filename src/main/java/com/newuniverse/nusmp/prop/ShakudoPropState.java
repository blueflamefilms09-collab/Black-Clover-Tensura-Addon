package com.newuniverse.nusmp.prop;

/**
 * 1.93.0: the synced word ({@code MagicPropEntity.param}) of the constructs of Shakudo, Scale, Spike, Nail and Hair Magic, pure arithmetic (no game classes) so
 * the server behaviours ({@link ShakudoPropKit}), the client painters and the unit test read the same layout:
 * bits 0-1 the state (0 idle or moving, 1 act, 2 act2, 3 done or embedded), bits 2-17 the game time (mod 65536) at which the state began (so a painter plays
 * the one-shot clips from their first frame), bits 18-21 a variant the spell chose at spawn.
 */
public final class ShakudoPropState {
    private ShakudoPropState() {}

    public static final int IDLE = 0, ACT = 1, ACT2 = 2, DONE = 3;

    public static int state(int param) { return param & 3; }

    /** Ticks since the state began (the clock is the game time mod 65536, so it also works across the wrap). */
    public static int since(int param, long gameTime) { return (int) ((gameTime - ((param >> 2) & 0xFFFF)) & 0xFFFF); }

    public static int variant(int param) { return (param >> 18) & 15; }

    public static int pack(int variant, int state, long gameTime) { return ((variant & 15) << 18) | (int) ((gameTime & 0xFFFF) << 2) | (state & 3); }
}
