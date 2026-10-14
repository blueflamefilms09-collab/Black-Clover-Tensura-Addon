package com.newuniverse.nusmp.entity;

/**
 * 1.35.0: Zagred's phases as pure rules (tested). The four acts follow the health bar (100-75, 75-50, 50-25, 25-0) until the last one is
 * entered; then the bar is refilled (PhaseRefill, once per fight) and the last act holds for the whole second bar, whatever the health,
 * so a refilled bar does not send him back to act I.
 */
public final class ZagredPhases {
    private ZagredPhases() {}

    /** The act for a health share (0 to 1), once `secondBar` is true the last act whatever the health. */
    public static int phaseOf(float frac, boolean secondBar) {
        if (secondBar) return 4;
        return frac > 0.75f ? 1 : frac > 0.5f ? 2 : frac > 0.25f ? 3 : 4;
    }

    /** Does entering `newPhase` start the second bar: the last act, not yet drawn. */
    public static boolean startsSecondBar(int newPhase, boolean secondBar) { return newPhase == 4 && !secondBar; }

    /** Letters of a Redact sentence: 5, +2 from act III, +2 more in act IV (1.35.0: was +1). */
    public static int redactLetters(int phase) { return 5 + (phase >= 3 ? 2 : 0) + (phase == 4 ? 2 : 0); }

    /** The share of a word's cooldown that is kept, by act (1.35.0: act IV 0.7 -> 0.6). */
    public static float cooldownScale(int phase) { return phase <= 2 ? 1f : phase == 3 ? 0.85f : 0.6f; }

    /** Letters of the echo sentence of the second bar: one fewer than the first. */
    public static int echoLetters(int phase) { return Math.max(2, redactLetters(phase) - 1); }
}
