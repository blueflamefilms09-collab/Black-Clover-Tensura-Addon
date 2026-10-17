package com.newuniverse.nusmp.arc.early;

/**
 * Why: Mars is a weapon of the Diamond Kingdom: mage stones were set into his body to amplify his magic, and that is the thing about him a party can act on.
 * The fight's long-term objective is those stones. Three are set in him and each intact stone adds a tenth to his damage; at each step of his power one of
 * them surfaces and glows for a while, and the blows he takes while it glows (anti-magic counts double, a stone is a magic tool) are counted towards
 * cracking it. A cracked stone is gone for good (his damage falls by a tenth and he staggers); a stone that was not cracked in time sinks back and keeps
 * its tenth. So a party that focuses him while a stone shows is rewarded with a weaker, shakier Mars, and one that does not is up against all three.
 * This pure class is the ledger; the game glue decides when stones surface (the power steps) and what a crack looks like.
 */
public final class StoneLedger {
    /** What a blow did. */
    public enum Event { NONE, BROKEN }

    /** The share of his damage one intact stone adds. */
    public static final double STEP = 0.10;

    private final int total;
    private int intact;
    private boolean open;
    private long openUntil;
    private double need, dealt;

    public StoneLedger(int stones) {
        this.total = Math.max(1, stones);
        this.intact = this.total;
    }

    /** A new fight: every stone set, none showing. */
    public void reset() {
        intact = total;
        open = false;
        dealt = 0;
        need = 0;
        openUntil = 0;
    }

    /** Puts the count back from saved data (clamped to what exists); nothing is showing. */
    public void restore(int intactStones) {
        intact = Math.max(0, Math.min(total, intactStones));
        open = false;
        dealt = 0;
    }

    public int intact() { return intact; }

    public int broken() { return total - intact; }

    public int total() { return total; }

    public boolean allBroken() { return intact <= 0; }

    /** Surfaces the next intact stone for {@code ticks} ticks; it takes {@code damageNeeded} to crack. False when none is left or one is already showing. */
    public boolean surface(long now, int ticks, double damageNeeded) {
        exposed(now);
        if (intact <= 0 || open) return false;
        open = true;
        openUntil = now + Math.max(1, ticks);
        need = Math.max(1, damageNeeded);
        dealt = 0;
        return true;
    }

    /** Is a stone showing at {@code now}? A window that has run out closes here, and the stone sinks back. */
    public boolean exposed(long now) {
        if (open && now >= openUntil) {
            open = false;
            dealt = 0;
        }
        return open;
    }

    /** How close the showing stone is to cracking, 0 to 1 (0 when none shows). */
    public double progress(long now) { return exposed(now) ? Math.min(1, dealt / need) : 0; }

    /** A blow on Mars. Counts only while a stone shows; anti-magic counts double. */
    public Event hit(long now, double damage, boolean antiMagic) {
        if (damage <= 0 || !exposed(now)) return Event.NONE;
        dealt += antiMagic ? damage * 2 : damage;
        if (dealt < need) return Event.NONE;
        open = false;
        dealt = 0;
        intact--;
        return Event.BROKEN;
    }

    /** The factor on his damage: 1 plus a tenth for every intact stone. */
    public double amplification() { return 1 + STEP * intact; }
}
