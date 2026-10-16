package com.newuniverse.nusmp.arena;

/**
 * Why: the arena of Lucius (1.46.0) must hold one fight at a time, never start one by accident, never let a half-dead boss wait for the
 * players to come back, and never let the same boss be farmed in a loop. Those are the rules of a small state machine, and a state machine
 * with a clock is exactly the kind of code that is wrong in a corner nobody tries in the game, so it is kept pure here (no Minecraft types,
 * unit tested) and the {@link ArenaDirector} only feeds it facts (how many players, how many on the dais, is the boss alive) and acts on the answers.
 * <ul>
 *   <li><b>READY</b>: nobody has started the fight. The first player in the arena starts a 10-second countdown ("The Final Hour begins");
 *       a player who stands on the dais ring for 5 seconds starts it sooner. When the countdown ends the boss is spawned and the state is FIGHTING.
 *       If everyone leaves during the countdown it starts again from zero.</li>
 *   <li><b>FIGHTING</b>: one boss is alive. If no player is left in the arena for {@value #EMPTY_GRACE_TICKS} ticks, or the boss is gone without having been
 *       defeated, the fight is ABANDONED: the director removes the boss and the arena rests for {@value #RESET_TICKS} ticks (60 s) before it is READY
 *       again, so nobody can leave, heal him up and come back to a half-health boss (the stages refill him anyway) or farm a loop.</li>
 *   <li><b>COOLDOWN</b>: after a defeat the arena rests for the configured minutes (20 by default); no boss spawns, the sigil is open. After an
 *       abandoned fight it rests 60 s. When the clock runs out the state is READY again.</li>
 * </ul>
 * Times are game ticks of the arena's level. The state saves as four numbers ({@link #save}/{@link #restore}), garbage is made safe.
 */
public final class ArenaState {
    public enum Phase { READY, FIGHTING, COOLDOWN }

    /** What {@link #tickReady} tells the director. */
    public enum Start { NOTHING, COUNTDOWN_BEGAN, SPAWN }

    /** What {@link #tickFighting} tells the director. */
    public enum Watch { CONTINUE, ABANDONED }

    public static final int DAIS_TICKS = 100, COUNTDOWN_TICKS = 200, EMPTY_GRACE_TICKS = 40, RESET_TICKS = 1200;

    private Phase phase = Phase.READY;
    private long cooldownUntil;
    private boolean defeated;
    private int arrivalTicks, daisTicks, emptyTicks;

    public Phase phase() { return phase; }

    public boolean defeated() { return defeated; }

    public long cooldownUntil() { return cooldownUntil; }

    /** Ticks left of the countdown to the spawn (0 when it has not begun). */
    public int countdownLeft() { return arrivalTicks <= 0 ? 0 : Math.max(0, COUNTDOWN_TICKS - arrivalTicks); }

    /** Is a boss meant to be alive now? */
    public boolean fighting() { return phase == Phase.FIGHTING; }

    /** Do the return sigils work? Always, but the director asks this to word the message: the sigil is "dormant" while the boss fights. */
    public boolean sigilOpen() { return phase != Phase.FIGHTING; }

    /**
     * One look while READY, 'step' ticks after the last one. 'present' is the number of players in the arena, 'onDais' the number of them
     * on the dais ring. Returns COUNTDOWN_BEGAN once (when the first player arrives), SPAWN when the boss is due (the state is then FIGHTING).
     */
    public Start tickReady(int step, int present, int onDais) {
        if (phase != Phase.READY) return Start.NOTHING;
        if (present <= 0) {
            arrivalTicks = daisTicks = 0;
            return Start.NOTHING;
        }
        boolean first = arrivalTicks == 0;
        arrivalTicks += Math.max(1, step);
        daisTicks = onDais > 0 ? daisTicks + Math.max(1, step) : 0;
        if (arrivalTicks >= COUNTDOWN_TICKS || daisTicks >= DAIS_TICKS) {
            phase = Phase.FIGHTING;
            emptyTicks = 0;
            arrivalTicks = daisTicks = 0;
            defeated = false;
            return Start.SPAWN;
        }
        return first ? Start.COUNTDOWN_BEGAN : Start.NOTHING;
    }

    /** One look while FIGHTING: the number of players in the arena and whether the boss is alive. */
    public Watch tickFighting(int step, int present, boolean bossAlive) {
        if (phase != Phase.FIGHTING) return Watch.CONTINUE;
        emptyTicks = present <= 0 ? emptyTicks + Math.max(1, step) : 0;
        if (emptyTicks >= EMPTY_GRACE_TICKS || !bossAlive) return Watch.ABANDONED;
        return Watch.CONTINUE;
    }

    /** The boss fell for good: the arena rests for 'cooldownTicks'. */
    public void defeat(long now, long cooldownTicks) {
        phase = Phase.COOLDOWN;
        defeated = true;
        cooldownUntil = now + Math.max(0L, cooldownTicks);
        arrivalTicks = daisTicks = emptyTicks = 0;
    }

    /** The fight was abandoned or the boss vanished: the arena rests for 60 seconds. */
    public void abandon(long now) {
        phase = Phase.COOLDOWN;
        defeated = false;
        cooldownUntil = now + RESET_TICKS;
        arrivalTicks = daisTicks = emptyTicks = 0;
    }

    /** One look while COOLDOWN: true when the rest is over and the state is READY again. */
    public boolean tickCooldown(long now) {
        if (phase != Phase.COOLDOWN || now < cooldownUntil) return false;
        phase = Phase.READY;
        defeated = false;
        return true;
    }

    /** Seconds of rest left (0 when none). */
    public long cooldownSecondsLeft(long now) { return phase == Phase.COOLDOWN ? Math.max(0L, (cooldownUntil - now + 19) / 20) : 0L; }

    /** An operator's reset: back to READY with nothing running. */
    public void reset() {
        phase = Phase.READY;
        defeated = false;
        cooldownUntil = 0;
        arrivalTicks = daisTicks = emptyTicks = 0;
    }

    // ---------------------------------------------------------------- save
    /** {phase, cooldownUntil, defeated 0/1}. The countdowns are not saved: a restart simply begins them again. */
    public long[] save() { return new long[]{phase.ordinal(), cooldownUntil, defeated ? 1 : 0}; }

    public void restore(long[] saved) {
        reset();
        if (saved == null || saved.length < 3) return;
        int p = (int) Math.max(0, Math.min(Phase.values().length - 1, saved[0]));
        phase = Phase.values()[p];
        cooldownUntil = Math.max(0, saved[1]);
        defeated = saved[2] == 1;
        if (phase == Phase.COOLDOWN && cooldownUntil == 0) phase = Phase.READY;
    }
}
