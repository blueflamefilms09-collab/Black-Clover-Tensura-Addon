package com.newuniverse.nusmp.song;

/**
 * Why: a song must pulse on a steady beat for each singer, and many singers must not all fire in the same tick of a busy server.
 * This pure scheduler answers "is this singer due" and keeps a per-tick budget, so both limits are testable without a world.
 */
public final class PulseScheduler {
    private long budgetTick = Long.MIN_VALUE;
    private int used;

    /** True when at least {@code interval} ticks have passed since the last pulse (a negative last means never). */
    public static boolean due(long now, long lastPulse, int interval) {
        return lastPulse < 0 || now - lastPulse >= interval;
    }

    /** Takes one slot of this tick's budget; false once {@code cap} pulses have run in the tick. */
    public boolean tryTake(long tick, int cap) {
        if (tick != budgetTick) { budgetTick = tick; used = 0; }
        if (used >= cap) return false;
        used++;
        return true;
    }
}
