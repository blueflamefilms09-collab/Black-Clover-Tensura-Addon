package com.newuniverse.nusmp.butoh;

/**
 * Why: the dance state needs one shared list of moves that the server (evasion odds, hitbox size), the client (which body clip to play) and
 * the tests all agree on, with no game classes in it so it can be unit tested. Each move names its clip in cast.animation.json (written by
 * tools/gen_cast_animations.py), its base evasion chance, and how small the dancer's body gets while that move is held (a low crouch or a
 * fold is a smaller target than a spin on tiptoe).
 */
public enum DanceMove {
    //            clip              evasion  scale change
    SWAY         ("dance_sway",         0.40, -0.15),
    SPIN         ("dance_spin",         0.55, -0.18),
    RIPPLE       ("dance_ripple",       0.45, -0.16),
    CROUCH_WAVE  ("dance_crouch_wave",  0.60, -0.24),
    LUNGE_FEINT  ("dance_lunge_feint",  0.50, -0.20),
    FOLD         ("dance_fold",         0.65, -0.25),
    DRIFT        ("dance_drift",        0.35, -0.15),
    RISE         ("dance_rise",         0.42, -0.17);

    public final String clip;
    /** The chance to slip a hit while this move is held, before the cap and the boss penalty (EvasionTable). */
    public final double evasion;
    /** The change to the player's scale attribute while this move is held (between DanceScale.MIN_CHANGE and MAX_CHANGE). */
    public final double scaleChange;

    DanceMove(String clip, double evasion, double scaleChange) {
        this.clip = clip;
        this.evasion = evasion;
        this.scaleChange = scaleChange;
    }

    private static final DanceMove[] ALL = values();

    public static int count() { return ALL.length; }

    public static DanceMove byIndex(int i) { return ALL[Math.floorMod(i, ALL.length)]; }
}
