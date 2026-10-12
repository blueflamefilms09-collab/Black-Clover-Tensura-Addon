package com.newuniverse.nusmp.entity.zogratis;

/**
 * Zenon's bone armour as a pool of damage it can take (pure, unit tested): hits are swallowed until the pool runs dry, the blow
 * that empties it passes its remainder on, and a shell that has run out of time or pool is gone. The wearer is not harder to hit,
 * only harder to hurt: the answer is to burst it, or to wait out its 8 seconds.
 */
public final class BoneShell {
    private double pool, max;
    private long until;

    /** Puts the shell on: a pool of 'pool' damage, lasting until game tick 'until'. */
    public void raise(double pool, long until) {
        this.max = Math.max(0, pool);
        this.pool = this.max;
        this.until = until;
    }

    public boolean active(long now) { return pool > 0 && now < until; }

    /**
     * Takes a blow of 'amount' at tick 'now'. Returns the damage that gets through: 0 while the shell holds it all, the remainder
     * when this blow breaks it, the whole blow if there is no shell.
     */
    public double absorb(long now, double amount) {
        if (!active(now) || !(amount > 0)) return Math.max(0, amount);
        if (amount < pool) {
            pool -= amount;
            return 0;
        }
        double left = amount - pool;
        pool = 0;
        return left;
    }

    /** The shell is gone (shattered, expired, or the fight ended). */
    public void drop() {
        pool = 0;
        max = 0;
        until = 0;
    }

    public double fraction() { return max <= 0 ? 0 : Math.max(0, pool / max); }
    public double pool() { return pool; }
    public long until() { return until; }
}
