package com.newuniverse.nusmp.arc.sds;

/**
 * Why: Drole's Dance is a dance that makes the dancer stronger the longer it goes on ("her bond with the earth intensifies as she dances, and it directly increases her
 * power level"; see docs/arc/diane.md). In a fight that is a stack counter: every beat of the dance that is completed adds a stack, each stack is a little more damage,
 * and the stacks fade one at a time (five seconds each) so the power is a thing she earns again and again, not a permanent buff. A party answers it by interrupting the
 * dance (the stacks of that dance are taken back) or by cutting her down to size (all of them go). This pure counter holds the stacks and their decay.
 */
public final class DanceStacks {
    /** The most stacks, the damage each adds (a fraction), and the ticks a stack lasts before the next fades. */
    public static final int MAX = 5, DECAY_TICKS = 100;
    public static final float PER_STACK = 0.06f;

    private int stacks;
    private long nextDecay;

    /** The stacks she holds at {@code now} (the old ones fade first). */
    public int stacks(long now) {
        while (stacks > 0 && now >= nextDecay) {
            stacks--;
            nextDecay += DECAY_TICKS;
        }
        if (stacks == 0) nextDecay = Long.MAX_VALUE;
        return stacks;
    }

    /** Adds {@code n} stacks (up to the maximum); every addition restarts the fade of the oldest. Returns the stacks now. */
    public int add(long now, int n) {
        int have = stacks(now);
        stacks = Math.min(MAX, have + Math.max(0, n));
        if (n > 0) nextDecay = now + DECAY_TICKS;
        return stacks;
    }

    /** Takes back up to {@code n} stacks (an interrupted dance). Returns the stacks now. */
    public int remove(long now, int n) {
        stacks = Math.max(0, stacks(now) - Math.max(0, n));
        if (stacks == 0) nextDecay = Long.MAX_VALUE;
        return stacks;
    }

    public void clear() {
        stacks = 0;
        nextDecay = Long.MAX_VALUE;
    }

    /** What her damage is multiplied by: one plus {@link #PER_STACK} for every stack. */
    public float multiplier(long now) { return 1f + PER_STACK * stacks(now); }
}
