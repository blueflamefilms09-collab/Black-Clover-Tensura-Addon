package com.newuniverse.nusmp.heroweapon.weapon.staff;

/**
 * Why: "accelerated magic casting" is the Vassal Staff's signature in the series, and in play it has to be something the wielder feels: casting spell after
 * spell without a pause keeps a rhythm that shortens the next cooldown a little, and Fenrir Rod quickens every cooldown for a while. The bookkeeping (when was
 * the last cast, how long is the chain, until when does the haste last) is plain arithmetic on the game clock, kept here without any game class so a test can
 * pin it: a chain continues only inside the window, it saturates, a melee beat can extend a chain but never start one, and the haste ends exactly when it
 * should. One instance per wielder; server thread only.
 */
public final class CastRhythm {
    private int chain;
    private long lastCast = -1_000_000L;
    private long hasteUntil;

    /** A staff spell was cast at {@code now}: the chain goes on when the last cast was inside the window, else a new chain of one begins. */
    public void cast(long now) {
        chain = chain > 0 && now - lastCast <= StaffNumbers.RHYTHM_WINDOW ? Math.min(chain + 1, StaffNumbers.RHYTHM_MAX + 1) : 1;
        lastCast = now;
    }

    /** A full melee swing: extends a chain that is still running (it never starts one), so a mage can keep the rhythm with a staff blow. */
    public void beat(long now) {
        if (chain > 0 && now - lastCast <= StaffNumbers.RHYTHM_WINDOW) {
            chain = Math.min(chain + 1, StaffNumbers.RHYTHM_MAX + 1);
            lastCast = now;
        }
    }

    /** Fenrir Rod: the haste lasts until the game time {@code untilTick} (a later call can only extend it). */
    public void haste(long untilTick) { hasteUntil = Math.max(hasteUntil, untilTick); }

    public boolean hasted(long now) { return now < hasteUntil; }

    /** How many EARLIER casts of the running chain still count (0 to {@link StaffNumbers#RHYTHM_MAX}); 0 when the window has passed. */
    public int stacks(long now) {
        if (chain <= 0 || now - lastCast > StaffNumbers.RHYTHM_WINDOW) return 0;
        return Math.min(StaffNumbers.RHYTHM_MAX, chain - 1);
    }

    /** The share of the nominal cooldown the next staff spell waits. */
    public double factor(long now, boolean quickChant) { return StaffNumbers.cooldownFactor(hasted(now), stacks(now), quickChant); }

    public void clear() {
        chain = 0;
        lastCast = -1_000_000L;
        hasteUntil = 0L;
    }
}
