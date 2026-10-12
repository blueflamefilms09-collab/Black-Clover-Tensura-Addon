package com.newuniverse.nusmp.entity.zogratis;

/**
 * Megicula's decaying curse on one victim (pure, unit tested): the state machine behind Vanica's Curse of Decay. A brand has a
 * life in ticks, a level, and it is anchored to a sigil on the ground. Every {@link #STEP} ticks the caller reports whether the
 * victim stands inside the sigil and whether they fight with anti-magic.
 * <ul>
 *   <li><b>Inside the sigil</b> the brand lasts and it bites harder the longer they stay: the damage multiplier grows by
 *       {@link #GROWTH_PER_SECOND} per second inside, up to {@link #maxMultiplier} for its level.</li>
 *   <li><b>Leaving the sigil</b> breaks it: its life burns {@link #OUTSIDE_DRAIN} times faster and the growth is lost, so a victim who
 *       walks out is free in a second or two.</li>
 *   <li><b>Anti-magic</b> (a demon sword, the Anti-Magic Lord) burns the life {@link #ANTI_MAGIC_DRAIN} times faster and halves the damage.</li>
 *   <li>A purification (milk, a cleanse that strips effects) is {@link #cleanse}.</li>
 * </ul>
 */
public final class CurseBrand {
    /** Ticks per {@link #step}. */
    public static final int STEP = 5;
    public static final int OUTSIDE_DRAIN = 8, ANTI_MAGIC_DRAIN = 3;
    public static final double GROWTH_PER_SECOND = 0.2;

    /** Why a brand ended. */
    public enum End { NONE, EXPIRED, LEFT_SIGIL, ANTI_MAGIC, CLEANSED }

    /** The outcome of a step: the damage multiplier of this step (0 once ended), whether it is over, and why. */
    public record Result(double multiplier, boolean ended, End reason) {}

    private final int level;
    private int life, insideTicks;
    private End ended = End.NONE;

    public CurseBrand(int ticks, int level) {
        this.life = Math.max(STEP, ticks);
        this.level = Math.max(1, Math.min(3, level));
    }

    public int level() { return level; }
    public int life() { return life; }
    public boolean ended() { return ended != End.NONE; }
    public End reason() { return ended; }
    public int insideTicks() { return insideTicks; }

    /** The most a brand of this level bites for, as a multiple of its first bite: 2.0, 2.2 or 2.4. */
    public static double maxMultiplier(int level) { return 1.8 + 0.2 * Math.max(1, Math.min(3, level)); }

    /** The current multiplier from the time spent inside the sigil. */
    public double multiplier() { return Math.min(maxMultiplier(level), 1 + GROWTH_PER_SECOND * insideTicks / 20.0); }

    /** The victim was cleansed: the brand is over. */
    public void cleanse() {
        if (ended == End.NONE) ended = End.CLEANSED;
    }

    /** Advances {@link #STEP} ticks. */
    public Result step(boolean inside, boolean antiMagic) {
        if (ended != End.NONE) return new Result(0, true, ended);
        int burn = STEP * (inside ? 1 : OUTSIDE_DRAIN) * (antiMagic ? ANTI_MAGIC_DRAIN : 1);
        life -= burn;
        insideTicks = inside ? insideTicks + STEP : 0;
        if (life <= 0) {
            ended = !inside ? End.LEFT_SIGIL : antiMagic ? End.ANTI_MAGIC : End.EXPIRED;
            return new Result(0, true, ended);
        }
        return new Result(antiMagic ? multiplier() * 0.5 : multiplier(), false, End.NONE);
    }
}
