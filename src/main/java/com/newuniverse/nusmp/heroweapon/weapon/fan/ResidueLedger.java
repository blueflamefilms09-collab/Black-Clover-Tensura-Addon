package com.newuniverse.nusmp.heroweapon.weapon.fan;

/**
 * Why: the Vassal Fan of the series grows by absorbing the magic that leaks out of its foes (the wiki's "Ambient Mana Absorption"). In play that is the Fan's
 * own loop: fighting with it gathers up to six stacks of RESIDUE (a stack for a kill, for a sweep that cut two foes or more, for every fourth full swing) and the
 * red petal tornado (Reverse Snow Moon Flower) burns them for damage; at six stacks the Demon Dragon Fan's sweep also reaches and cuts harder. This is the counter
 * and nothing else, no game imports, so its rules (the cap, the swing count, spending) are tested. One ledger per wielder; it lives in memory only and starts
 * empty whenever the weapon is summoned.
 */
public final class ResidueLedger {
    private int stacks, swings;

    /** The stacks held, 0 to {@link FanNumbers#RESIDUE_MAX}. */
    public int stacks() { return stacks; }

    public boolean full() { return stacks >= FanNumbers.RESIDUE_MAX; }

    /** Adds {@code n} stacks (at most up to the cap); true when the count changed. */
    public boolean add(int n) {
        if (n <= 0 || full()) return false;
        stacks = Math.min(FanNumbers.RESIDUE_MAX, stacks + n);
        if (full()) swings = 0;
        return true;
    }

    /** A full swing of the melee attack landed; true when it completed a stack (every {@link FanNumbers#SWINGS_PER_RESIDUE}th one). */
    public boolean swing() {
        if (full()) return false;
        swings++;
        if (swings >= FanNumbers.SWINGS_PER_RESIDUE) {
            swings = 0;
            return add(1);
        }
        return false;
    }

    /** Empties the ledger and returns how many stacks it held. */
    public int spend() {
        int s = stacks;
        stacks = 0;
        swings = 0;
        return s;
    }

    public void reset() {
        stacks = 0;
        swings = 0;
    }
}
