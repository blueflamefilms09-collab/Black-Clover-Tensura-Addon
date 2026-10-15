package com.newuniverse.nusmp.boss.arc;

/**
 * Why: the owner's rework of Lucius is a three-act fight (the Untouchable Messiah, Triad Supremacy and Soul Corruptor, the Infinite
 * Clones). Which act is running must be a pure function of his health that cannot flicker, fire its "entered" event exactly once, and
 * survive a save and a reload, so every other part of the arc (the hover, the chains, the clones, the core gem) can ask one object
 * instead of re-deriving it from health fractions in ten places. No Minecraft types, so the rules are unit tested.
 * <p>
 * Phases follow health: 1 above 65%, 2 from 65% down to 30%, 3 under 30%. Going down is immediate. Going back up (a scripted restore
 * is the only way health can rise in combat) needs {@link #MARGIN} more than the line, so a restore that lands on the line cannot make
 * the phase flap. {@link #update} moves at most one phase per call, so a hit that crosses two lines still announces both acts, in
 * order. The set of acts already entered is kept as a bit mask, so an act announced once is never announced again, even after a
 * regress and a second descent.
 */
public final class LuciusArc {
    public static final int NONE = 0, MESSIAH = 1, SOUL_CORRUPTOR = 2, CLONES = 3;
    /** Health fraction under which act 2 / act 3 begin. */
    public static final float P2_BELOW = 0.65f, P3_BELOW = 0.30f;
    /** How far above a line the health must climb before the phase goes back. */
    public static final float MARGIN = 0.03f;

    private int phase = NONE;
    private int entered;

    /** The phase a health fraction asks for, ignoring history. */
    public static int raw(float healthFraction) {
        if (healthFraction < P3_BELOW) return CLONES;
        if (healthFraction < P2_BELOW) return SOUL_CORRUPTOR;
        return MESSIAH;
    }

    /** The current phase: {@link #NONE} before the first update. */
    public int phase() { return phase; }

    /** Has this act been announced in this fight already? */
    public boolean entered(int p) { return p >= MESSIAH && p <= CLONES && (entered & (1 << p)) != 0; }

    /**
     * Advances the state machine for this health fraction. Returns the phase that was entered for the first time by this call, or
     * {@link #NONE} if nothing new began (no change, a regress, or an act that was announced before).
     */
    public int update(float healthFraction) {
        if (Float.isNaN(healthFraction)) return NONE;
        float f = Math.max(0f, Math.min(1f, healthFraction));
        int want = raw(f);
        int next = phase;
        if (phase == NONE) {
            next = MESSIAH;                                   // the fight opens in act 1 whatever the health says; the next call climbs on
        } else if (want > phase) {
            next = phase + 1;
        } else if (want < phase && raw(f - MARGIN) < phase) {
            next = Math.max(want, raw(f - MARGIN));
        }
        if (next == phase) return NONE;
        phase = next;
        if (!entered(next)) {
            entered |= 1 << next;
            return next;
        }
        return NONE;
    }

    /** A fresh fight. */
    public void reset() {
        phase = NONE;
        entered = 0;
    }

    /** {phase, entered mask}: what a save writes. */
    public int[] save() { return new int[]{phase, entered}; }

    /** A saved state back (garbage is made safe: phase 0..3, a mask that contains every act up to the phase). */
    public static LuciusArc restore(int phase, int enteredMask) {
        LuciusArc a = new LuciusArc();
        a.phase = Math.max(NONE, Math.min(CLONES, phase));
        int mask = enteredMask & 0b1110;
        for (int p = MESSIAH; p <= a.phase; p++) mask |= 1 << p;
        a.entered = mask;
        return a;
    }

    /** The act's name for the boss bar and the chat. */
    public static String name(int phase) {
        return switch (phase) {
            case MESSIAH -> "The Untouchable Messiah";
            case SOUL_CORRUPTOR -> "Triad Supremacy and Soul Corruptor";
            case CLONES -> "The Infinite Clones";
            default -> "";
        };
    }
}
