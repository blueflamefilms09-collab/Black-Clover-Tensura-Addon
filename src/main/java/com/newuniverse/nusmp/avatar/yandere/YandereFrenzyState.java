package com.newuniverse.nusmp.avatar.yandere;

/**
 * Why: the frenzy has three clear moments (burning, exhausted, over) and must always end; as a tiny pure state machine it can be tested without
 * a world. The numbers live in two ints so the handler can keep them in the Avatar's saved data and a reload cannot leave a frenzy stuck on.
 */
public final class YandereFrenzyState {
    public enum Phase { IDLE, FRENZY, EXHAUSTED }
    public enum Change { NONE, ENTERED_EXHAUSTION, ENDED }

    private int frenzyLeft, exhaustLeft;
    private final int exhaustionTicks;

    public YandereFrenzyState(int frenzyLeft, int exhaustLeft, int exhaustionTicks) {
        this.frenzyLeft = Math.max(0, frenzyLeft);
        this.exhaustLeft = Math.max(0, exhaustLeft);
        this.exhaustionTicks = Math.max(0, exhaustionTicks);
    }

    public Phase phase() { return frenzyLeft > 0 ? Phase.FRENZY : exhaustLeft > 0 ? Phase.EXHAUSTED : Phase.IDLE; }

    public int frenzyLeft() { return frenzyLeft; }

    public int exhaustLeft() { return exhaustLeft; }

    /** The creator was hurt: begin the frenzy, or lengthen the running one to at least {@code duration} (it never stacks beyond it). */
    public void trigger(int duration) {
        frenzyLeft = Math.max(frenzyLeft, Math.max(1, duration));
        exhaustLeft = 0;
    }

    /** One tick forward. Returns what changed: the frenzy running out starts the exhaustion (or ends it at once when there is none). */
    public Change tick() {
        if (frenzyLeft > 0) {
            frenzyLeft--;
            if (frenzyLeft == 0) {
                exhaustLeft = exhaustionTicks;
                return exhaustLeft > 0 ? Change.ENTERED_EXHAUSTION : Change.ENDED;
            }
            return Change.NONE;
        }
        if (exhaustLeft > 0) {
            exhaustLeft--;
            return exhaustLeft == 0 ? Change.ENDED : Change.NONE;
        }
        return Change.NONE;
    }
}
