package com.newuniverse.nusmp.entity.zogratis;

/**
 * Lucius's precognition (pure): Astaroth lets him read the next blow before it lands and step out of its way. He holds a few
 * <b>charges</b>; a blow from an attacker he can read is dodged (and costs a charge), and the boss then punishes the attacker. The
 * party's answers are built into the rules:
 * <ul>
 *   <li><b>Out of sequence</b>: a blow from a <i>different</i> attacker within {@value #SEQUENCE_WINDOW} ticks of a dodge was not part
 *       of the future he read, so it lands and costs him nothing. Two fighters striking together beat a charge.</li>
 *   <li><b>Anti-magic</b> is outside the flow of time he reads: it always lands, and it burns one of his charges.</li>
 *   <li><b>No attacker</b> (poison, fire, a field a player left behind) has no future to read: it always lands.</li>
 *   <li><b>Spent</b>: with no charge left the blow lands; charges come back one at a time ({@link #rechargeTicks}).</li>
 *   <li><b>Suspended</b>: Julius's will, the Gates and a rewind switch it off for a while.</li>
 * </ul>
 */
public final class Precognition {
    public enum Verdict {
        /** Precognition is switched off: the blow lands. */
        OFF,
        /** The blow was read and dodged (a charge was spent). */
        DODGED,
        /** No charge left: the blow lands. */
        EMPTY,
        /** The blow has no attacker to read: it lands. */
        NO_ATTACKER,
        /** A second attacker inside the sequence window: it lands. */
        OUT_OF_SEQUENCE,
        /** An anti-magic blow: it lands and burns a charge. */
        ANTI_MAGIC
    }

    /** Ticks after a dodge in which a different attacker's blow is out of sequence. */
    public static final int SEQUENCE_WINDOW = 12;
    public static final int MAX_CHARGES = 3;

    private int charges;
    private long nextCharge, lastDodge = Long.MIN_VALUE / 2, offUntil;
    private int lastAttacker = -1;

    /** Most charges he can hold: 1 under 40% power, 2 under 80%, 3 from there. */
    public static int maxCharges(int power) { return power >= 80 ? 3 : power >= 40 ? 2 : 1; }

    /** Ticks for one charge to come back: slower at the opening, quicker as he draws more power. */
    public static int rechargeTicks(int power) { return power >= 100 ? 120 : power >= 80 ? 150 : power >= 60 ? 190 : power >= 40 ? 230 : 280; }

    /** A fresh fight: full charges for this power, switched on. */
    public void reset(int power, long now) {
        charges = maxCharges(power);
        nextCharge = now + rechargeTicks(power);
        lastDodge = Long.MIN_VALUE / 2;
        lastAttacker = -1;
        offUntil = 0;
    }

    public int charges() { return charges; }

    /** Switches it off until 'until' (a game tick); a later call can only extend that. */
    public void suspend(long until) { offUntil = Math.max(offUntil, until); }

    public boolean off(long now) { return now < offUntil; }

    /** Ticks until the next charge, or 0 if he is full. */
    public int ticksToCharge(long now, int power) { return charges >= maxCharges(power) ? 0 : (int) Math.max(0, nextCharge - now); }

    /** Call once a tick (or once a second with the real 'now'): charges return one at a time, and a new power's cap applies. */
    public void tick(long now, int power) {
        int max = maxCharges(power);
        if (charges > max) charges = max;
        if (charges >= max) { nextCharge = now + rechargeTicks(power); return; }
        if (off(now)) { nextCharge = Math.max(nextCharge, offUntil + rechargeTicks(power)); return; }
        if (now >= nextCharge) {
            charges++;
            nextCharge = now + rechargeTicks(power);
        }
    }

    /** A power step fills him up (drawing more of Astaroth's power also brings the charges back). */
    public void refill(int power, long now) {
        charges = maxCharges(power);
        nextCharge = now + rechargeTicks(power);
    }

    /** Gives back one charge now (Chrono Acceleration), never past the cap. */
    public void grant(int power) { charges = Math.min(maxCharges(power), charges + 1); }

    /**
     * A blow is about to land. 'attackerId' is the attacker's entity id or a negative number if there is none; 'antiMagic' says it is an
     * anti-magic blow. Returns what happens to it; {@link Verdict#DODGED} is the only one that stops it.
     */
    public Verdict onHit(long now, int attackerId, boolean antiMagic, int power) {
        if (off(now)) return Verdict.OFF;
        if (attackerId < 0) return Verdict.NO_ATTACKER;
        if (antiMagic) {
            if (charges > 0) { charges--; nextCharge = now + rechargeTicks(power); }
            return Verdict.ANTI_MAGIC;
        }
        if (charges <= 0) return Verdict.EMPTY;
        if (lastAttacker >= 0 && lastAttacker != attackerId && now - lastDodge <= SEQUENCE_WINDOW) return Verdict.OUT_OF_SEQUENCE;
        charges--;
        nextCharge = now + rechargeTicks(power);                                   // every dodge restarts the recharge
        lastDodge = now;
        lastAttacker = attackerId;
        return Verdict.DODGED;
    }
}
