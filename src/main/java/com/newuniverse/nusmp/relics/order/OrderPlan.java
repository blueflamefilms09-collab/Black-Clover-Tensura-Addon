package com.newuniverse.nusmp.relics.order;

/**
 * The timeline of one visit of the God of Order (pure: no Minecraft types, unit tested). The Order's director ticks one plan per active case
 * and does what the {@link Beat} says; the entity only shows the {@link Phase}.
 * <pre>
 *   TELEGRAPH  6 s   a golden ring and a column of glyphs mark the spot; the God is not there yet
 *   (APPEAR)         the God steps out of the column, 8 blocks from the holder of the duplicate
 *   READING    5 s   it reads its verdict aloud; it cannot be hurt
 *   BEAM       2 s   a beam leaves its staff and burns the duplicate to gold dust; it cannot be hurt
 *   (STRIP)          the duplicate is taken, the Notice of Order is handed over
 *   BOW        2 s   it bows and leaves                    (or, if somebody struck it:)
 *   COMBAT           it answers the one who struck it, until its Erasure or the time limit
 *   DONE
 * </pre>
 * A blow during READING or BEAM is remembered: the verdict is finished first (the duplicate goes either way), then the God fights. A blow during
 * the bow starts the fight at once. A holder in creative mode never starts a fight ({@link #noCombat}).
 */
public final class OrderPlan {
    public enum Phase { TELEGRAPH, READING, BEAM, BOW, COMBAT, DONE }

    /** What the director must do this tick. */
    public enum Beat { NONE, APPEAR, BEAM_BEGIN, STRIP, LEAVE }

    public static final int TELEGRAPH_TICKS = 120, READ_TICKS = 100, BEAM_TICKS = 40, BOW_TICKS = 40;
    /** The fight ends by itself after this long (the God withdraws): two minutes. */
    public static final int COMBAT_LIMIT_TICKS = 2_400;
    /** No visit lasts longer than this, whatever happens: five minutes. */
    public static final int CASE_LIMIT_TICKS = 6_000;
    /** The God cannot be hurt for this many ticks after it appears (a spawn is never a free hit), on top of READING and BEAM. */
    public static final int GRACE_TICKS = 20;

    private Phase phase = Phase.TELEGRAPH;
    private int inPhase, total;
    private boolean provoked, noCombat, stripped;

    public OrderPlan(boolean noCombat) { this.noCombat = noCombat; }

    public Phase phase() { return phase; }

    public int ticksInPhase() { return inPhase; }

    public int totalTicks() { return total; }

    public boolean provoked() { return provoked; }

    public boolean stripped() { return stripped; }

    public boolean noCombat() { return noCombat; }

    /** Is the God unhurtable right now? */
    public boolean invulnerable() {
        return phase == Phase.READING || phase == Phase.BEAM || (phase == Phase.BOW && !stripped);
    }

    /** 0..1 over the current phase. */
    public float progress() {
        int len = lengthOf(phase);
        return len <= 0 ? 0f : Math.min(1f, inPhase / (float) len);
    }

    public static int lengthOf(Phase p) {
        return switch (p) {
            case TELEGRAPH -> TELEGRAPH_TICKS;
            case READING -> READ_TICKS;
            case BEAM -> BEAM_TICKS;
            case BOW -> BOW_TICKS;
            case COMBAT -> COMBAT_LIMIT_TICKS;
            default -> 0;
        };
    }

    /** One tick of the case. */
    public Beat tick() {
        if (phase == Phase.DONE) return Beat.NONE;
        total++;
        if (total >= CASE_LIMIT_TICKS) {
            phase = Phase.DONE;
            return Beat.LEAVE;
        }
        inPhase++;
        switch (phase) {
            case TELEGRAPH:
                if (inPhase >= TELEGRAPH_TICKS) { enter(Phase.READING); return Beat.APPEAR; }
                break;
            case READING:
                if (inPhase >= READ_TICKS) { enter(Phase.BEAM); return Beat.BEAM_BEGIN; }
                break;
            case BEAM:
                if (inPhase >= BEAM_TICKS) {
                    stripped = true;
                    enter(provoked && !noCombat ? Phase.COMBAT : Phase.BOW);
                    return Beat.STRIP;
                }
                break;
            case BOW:
                if (inPhase >= BOW_TICKS) { phase = Phase.DONE; return Beat.LEAVE; }
                break;
            case COMBAT:
                if (inPhase >= COMBAT_LIMIT_TICKS) { noCombat = true; enter(Phase.BOW); }
                break;
            default:
                break;
        }
        return Beat.NONE;
    }

    /** The holder (or somebody else) struck the God. Ignored before it appeared, after it left, and for a creative holder. */
    public void provoke() {
        if (noCombat) return;
        if (phase == Phase.READING || phase == Phase.BEAM) provoked = true;
        else if (phase == Phase.BOW) { provoked = true; enter(Phase.COMBAT); }
    }

    /** The God's Erasure was cast: the fight is over and it leaves at once. */
    public Beat erased() { return end(); }

    /** The God died or vanished: the case is over. */
    public Beat lost() { return end(); }

    /** The case is cancelled (the holder left, the world closed). */
    public Beat abort() { return end(); }

    private Beat end() {
        if (phase == Phase.DONE) return Beat.NONE;
        phase = Phase.DONE;
        return Beat.LEAVE;
    }

    private void enter(Phase p) {
        phase = p;
        inPhase = 0;
    }
}
