package com.newuniverse.nusmp.arc.early;

/**
 * Why: Heath Grice's Crystal Ice Shield turned aside Magna's fireballs in the anime and was cut through by Asta's sword, which is the whole lesson of the
 * spell: it is a wall against things thrown at him, not against a blade at his throat. This pure class is the rule. While it stands, damage from range (a
 * spell, an arrow, a blow from further than {@value #NEAR} blocks) is swallowed by a pool of ice until the pool is gone; a blow struck from close by passes
 * through and only cracks the shield (half of its damage comes off the pool); anti-magic shatters it at once. So the shield never makes the fight
 * longer for a party that closes in, and it never makes a caster useless either: the pool is finite (a share of his health) and it runs out.
 * The game glue classifies each blow ({@link #classify}) and applies {@link #absorb}.
 */
public final class FrostShield {
    /** What kind of blow it was. */
    public enum Blow { MELEE, RANGED, ANTI_MAGIC }

    /** The damage that got through the shield, and whether this blow broke it. */
    public record Result(double through, boolean broke) {}

    /** A blow from nearer than this, struck by the attacker's own hand, is a melee blow. */
    public static final double NEAR = 4.5;
    /** A melee blow takes this share of its damage off the pool. */
    public static final double CRACK = 0.5;

    private long until;
    private double pool;

    /** Raises the shield for {@code ticks} ticks with a pool of {@code pool} points. */
    public void raise(long now, int ticks, double pool) {
        this.until = now + Math.max(0, ticks);
        this.pool = Math.max(0, pool);
    }

    /** Is it standing at {@code now}? */
    public boolean up(long now) { return now < until && pool > 0; }

    public double pool() { return Math.max(0, pool); }

    /** Takes it down without a shatter (the time ran out, or the fight ended). */
    public void drop() {
        until = 0;
        pool = 0;
    }

    /**
     * What a blow does to the shield. With no shield standing, everything passes. A ranged blow is swallowed (the part beyond the pool passes, and the shield
     * is gone); a melee blow passes whole and cracks the pool; anti-magic passes whole and shatters it.
     */
    public Result absorb(long now, double amount, Blow kind) {
        if (amount <= 0) return new Result(0, false);
        if (!up(now)) {
            drop();
            return new Result(amount, false);
        }
        switch (kind) {
            case ANTI_MAGIC -> {
                drop();
                return new Result(amount, true);
            }
            case MELEE -> {
                pool -= amount * CRACK;
                if (pool <= 0) {
                    drop();
                    return new Result(amount, true);
                }
                return new Result(amount, false);
            }
            default -> {
                if (pool > amount) {
                    pool -= amount;
                    return new Result(0, false);
                }
                double through = amount - pool;
                drop();
                return new Result(through, true);
            }
        }
    }

    /** Which kind a blow is: anti-magic wins; a blow struck by the attacker's own hand from {@value #NEAR} blocks or nearer is melee; anything else is ranged. */
    public static Blow classify(boolean directBlow, boolean antiMagic, double distance) {
        if (antiMagic) return Blow.ANTI_MAGIC;
        return directBlow && distance <= NEAR ? Blow.MELEE : Blow.RANGED;
    }
}
