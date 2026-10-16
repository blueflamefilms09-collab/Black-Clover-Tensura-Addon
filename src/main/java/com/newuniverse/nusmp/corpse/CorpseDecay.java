package com.newuniverse.nusmp.corpse;

/**
 * Why: a Soul Corpse is a loan from death, and the loan has to be visible and has to run out. The same few numbers are needed in
 * three places (the server that drains health, the effect amplifier that tells the client how rotten the body looks, and the tests), so
 * they live here without any game type: how strong the dead hit, how far through its life a body is, which of the four looks
 * (stage 0 to 3) that is, and how fast it loses health. Health only ever goes DOWN: there is no regeneration in this class.
 */
public final class CorpseDecay {
    private CorpseDecay() {}

    /** The highest decay stage (0 = freshly raised, 3 = falling apart). */
    public static final int MAX_STAGE = 3;
    /** Seconds between two refreshes of the stage on the effect (the effect amplifier is how the client learns it). */
    public static final int STAGE_REFRESH_TICKS = 100;
    /** Damage multiplier of a reanimated body (30% stronger than the living creature). */
    public static final double DAMAGE_MULT = 1.30;
    /** Share of its maximum health a reanimated body has lost by the moment it crumbles (the clock, not the damage taken, ends it). */
    public static final double LOSS_AT_END = 0.60;

    /** 0 at the moment it was raised, 1 at the end of its life; clamped. */
    public static double fraction(long born, long now, int lifeTicks) {
        if (lifeTicks <= 0) return 1.0;
        double f = (now - born) / (double) lifeTicks;
        return f < 0 ? 0 : Math.min(f, 1.0);
    }

    /** The look of a body at this fraction of its life: 0 below a quarter, 1 below a half, 2 below three quarters, then 3. */
    public static int stage(double fraction) {
        int s = (int) Math.floor(Math.max(0.0, Math.min(fraction, 1.0)) * (MAX_STAGE + 1));
        return Math.min(s, MAX_STAGE);
    }

    /** The stage of a body at game time 'now'. */
    public static int stage(long born, long now, int lifeTicks) { return stage(fraction(born, now, lifeTicks)); }

    /** Health to take off per second so that {@link #LOSS_AT_END} of the maximum is gone when the life ends (never negative). */
    public static double bleedPerSecond(double maxHealth, int lifeTicks) {
        if (maxHealth <= 0 || lifeTicks <= 0) return 0.0;
        return Math.max(0.0, maxHealth * LOSS_AT_END / (lifeTicks / 20.0));
    }

    /** The health after one second of decay: it only goes down and never below a sliver (the clock ends the body, not the bleed). */
    public static double afterBleed(double health, double maxHealth, int lifeTicks) {
        double next = health - bleedPerSecond(maxHealth, lifeTicks);
        return Math.max(Math.min(health, 0.5), next);
    }

    /** The effect amplifier the client reads: the stage, clamped to the legal range. */
    public static int amplifier(int stage) { return Math.max(0, Math.min(stage, MAX_STAGE)); }

    /** The stage a client reads from an effect amplifier (any odd value is clamped). */
    public static int stageOf(int amplifier) { return amplifier(amplifier); }

    /** Overlay opacity of a stage, 0..1: fresh bodies are only faintly marked, the last stage is almost fully covered. */
    public static float overlayAlpha(int stage) {
        return switch (amplifier(stage)) {
            case 0 -> 0.35f;
            case 1 -> 0.55f;
            case 2 -> 0.75f;
            default -> 0.92f;
        };
    }
}
