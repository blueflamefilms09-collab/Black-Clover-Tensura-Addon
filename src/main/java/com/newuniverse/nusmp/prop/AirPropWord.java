package com.newuniverse.nusmp.prop;

/**
 * Why: the synced {@code param} of the rebuilt summons (Smoke, Snow, Wing, Vortex, Air, Ash) tells the painter which model to draw and which clip to play, and the clip must
 * start at the tick the state began. The packing is pure arithmetic, kept here (no Minecraft classes) so it is unit tested: bits 0-7 the variant, bits 8-9 the state
 * (0 idle, 1 act, 2 act2 / vanish), bits 10-25 the game time (mod 65536) at which the state began.
 */
public final class AirPropWord {
    private AirPropWord() {}

    public static final int IDLE = 0, ACT = 1, ACT2 = 2;

    public static int pack(int variant, int state, long gameTime) { return (variant & 0xFF) | ((state & 3) << 8) | ((int) (gameTime & 0xFFFF) << 10); }

    public static int variant(int param) { return param & 0xFF; }

    public static int state(int param) { return (param >> 8) & 3; }

    /** Ticks since the state began: the clock wraps every 65536 ticks and the answer is never negative. */
    public static int since(int param, long gameTime) { return (int) ((gameTime - ((param >> 10) & 0xFFFF)) & 0xFFFF); }
}
