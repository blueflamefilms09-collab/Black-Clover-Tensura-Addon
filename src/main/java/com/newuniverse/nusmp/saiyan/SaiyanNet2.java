package com.newuniverse.nusmp.saiyan;

/**
 * Why: the asks of the Dragon Ball rework (phases 3 to 5 and the third set of techniques) that come after {@link SaiyanNet#OP_ENERGY} on the same one-byte {@code saiyan_action} packet. They live here and not in
 * {@link SaiyanNet} so the older range check there stays exactly as it was; {@link SaiyanNet#onAction} sends a number in this range to {@link SaiyanManager#onActionPhase} and nowhere else. Pure: no game type.
 * The numbers are save-free (an ask is never stored), so a new op is only ever appended.
 */
public final class SaiyanNet2 {
    private SaiyanNet2() {}

    public static final int OP_STRIKE = 22, OP_VANISH = 23, OP_RUSH = 24, OP_COUNTER = 25, OP_BREAKER = 26, OP_INSTINCT = 27, OP_SPHERE = 28, OP_WAVE_START = 29, OP_WAVE_RELEASE = 30,
            OP_REINFORCE = 31, OP_AVATAR = 32, OP_AFTERIMAGE = 33, OP_GRAVITY = 34, OP_BRANCH = 35;
    public static final int FIRST = OP_STRIKE, LAST = OP_BRANCH;

    /** True for an ask of this range (the server still checks the sender, the rate and every rule). */
    public static boolean valid(int op) { return op >= FIRST && op <= LAST; }

    /** The ops a plain key press sends, in the order the "next technique" key cycles through them (the client reads it; the wave and the branch have keys of their own). */
    public static final int[] CYCLE = {OP_STRIKE, OP_VANISH, OP_RUSH, OP_COUNTER, OP_BREAKER, OP_INSTINCT, OP_SPHERE, OP_REINFORCE, OP_AVATAR, OP_AFTERIMAGE, OP_GRAVITY};

    /** The language key of an op's name (the client shows it when the cycle key moves). */
    public static String nameKey(int op) { return "anime_multiverse.saiyan.tech2." + op; }
}
