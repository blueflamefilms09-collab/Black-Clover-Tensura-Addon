package com.newuniverse.nusmp.butoh;

/**
 * Why: from 1.46.0 the Butoh dance is a passive of the skill (on by itself while the player can pay for it), so one small rule set must
 * decide, every few ticks, whether the dance should start, stop or be left alone. Keeping it free of Minecraft types means the hysteresis
 * (stop when the mana cannot pay, come back only above a restart share, so it never flickers) and the cheaper idle upkeep can be unit tested.
 * {@link ButohDance} gathers the facts from the game and applies the answer.
 */
public final class DancePassive {
    private DancePassive() {}

    /** Share of the maximum magicule the dancer must have before a dance that dropped for lack of mana starts again. */
    public static final double DEFAULT_RESTART = 0.15;

    /** Seconds-worth of ticks without moving or fighting before the dancer counts as idle (the cheaper upkeep). */
    public static final int IDLE_WINDOW_TICKS = 20;

    public enum Action { NONE, START, STOP }

    /**
     * What to do now.
     * @param dancing   whether the dance is on right now
     * @param wanted    the player has the skill, the passive is configured on and the dance is not switched to resting
     * @param blocked   the body is out of the dancer's hands (stunned, asleep, riding, swimming, time stopped, ...)
     * @param manaShare current magicule divided by the maximum (0 when unknown)
     * @param restart   the share needed to (re)start
     */
    public static Action decide(boolean dancing, boolean wanted, boolean blocked, double manaShare, double restart) {
        if (!wanted) return dancing ? Action.STOP : Action.NONE;
        if (blocked) return dancing ? Action.STOP : Action.NONE;
        if (dancing) return Action.NONE;                       // already on; running dry is handled by the upkeep, not by the share
        return manaShare >= restart && manaShare > 0 ? Action.START : Action.NONE;
    }

    /** The upkeep multiplier: the idle factor while standing still, 1 while moving or fighting. The factor is clamped to 0..1. */
    public static double drainFactor(boolean idle, double idleFactor) {
        return idle ? Math.max(0, Math.min(1, idleFactor)) : 1.0;
    }

    /** Remembers the last tick the dancer moved or fought, so "idle" can be asked at upkeep time. */
    public static final class Activity {
        private long lastActive = Long.MIN_VALUE / 2;

        /** @param movedSq horizontal distance moved since the last tick, squared; @param fighting swinging, hurt or just hit something */
        public void observe(long tick, double movedSq, boolean fighting) {
            if (fighting || movedSq > 0.0025) lastActive = tick;
        }

        public boolean idle(long tick) { return tick - lastActive >= IDLE_WINDOW_TICKS; }

        /** A fresh start counts as activity, so the first second is paid in full. */
        public void touch(long tick) { lastActive = tick; }
    }
}
