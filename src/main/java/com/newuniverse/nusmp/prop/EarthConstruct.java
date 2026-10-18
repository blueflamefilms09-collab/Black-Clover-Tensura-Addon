package com.newuniverse.nusmp.prop;

/**
 * 1.93.0 Why: one prop kind ({@link PropKind#EARTH_CONSTRUCT}) carries nine different constructs of the earth-family magics, so the server behaviour and the client
 * painter must read the same word. The synced {@code param} int packs: bits 0-3 the construct TYPE, bits 4-5 the STATE (0 idle or walking, 1 strike, 2 second
 * action such as block, roar or spit), bits 6-21 the game time (mod 65536) at which the state began, so the painter starts a one-shot clip at its first frame.
 * Pure arithmetic with no game classes (unit tested in EarthConstructTest).
 */
public final class EarthConstruct {
    private EarthConstruct() {}

    public static final int GOLEM = 0, TITAN = 1, SENTINEL = 2, SPHINX = 3, CAGE = 4, TOTEM = 5, SPIRE = 6, BOULDER = 7, LANCE = 8;
    public static final int TYPES = 9;
    public static final int IDLE = 0, ACT = 1, ACT2 = 2;

    /** The magic (EarthFxStyle index) each type belongs to. */
    private static final int[] STYLE = {4, 1, 3, 0, 0, 2, 5, 1, 5};
    private static final String[] NAME = {"golem", "titan", "sentinel", "sphinx", "cage", "totem", "spire", "boulder", "lance"};

    public static int pack(int type, int state, long gameTime) { return (type & 15) | ((state & 3) << 4) | (int) ((gameTime & 0xFFFF) << 6); }

    public static int type(int param) { return param & 15; }

    public static int state(int param) { return (param >> 4) & 3; }

    /** Ticks since the state began (the clock is the game time mod 65536). */
    public static int since(int param, long gameTime) { return (int) ((gameTime - ((param >> 6) & 0xFFFF)) & 0xFFFF); }

    public static int withState(int param, int state, long gameTime) { return pack(type(param), state, gameTime); }

    /** The EarthFxStyle index of a type (which magic's palette its effects use). */
    public static int style(int type) { return STYLE[Math.floorMod(type, TYPES)]; }

    public static String name(int type) { return NAME[Math.floorMod(type, TYPES)]; }

    public static boolean valid(int type) { return type >= 0 && type < TYPES; }
}
