package com.newuniverse.nusmp.labyrinth.mega.poise;

/**
 * Why: the life of one poise bar as a small state machine with no clock of its own (the caller passes the tick), so the rule "super armor holds until the bar breaks, then the
 * bearer staggers, then the bar comes back and cannot be broken again at once" is one tested object. The cycle: {@link #raiseArmor} when a telegraphed attack begins; every
 * {@link #hit} while the armor is up drains the bar and answers {@link Hit#ABSORBED}; the blow that empties it answers {@link Hit#BROKEN}: the armor drops, the bearer is helpless for
 * {@code staggerTicks}, and when the stagger ends the bar is back at {@code recoverFrac} and the armor holds without draining for {@code immuneTicks} (so a party cannot chain a
 * captain into a stun-lock). With no armor up a blow is {@link Hit#OPEN}: it hurts and pushes as in the old game and only starts the regeneration delay. Mutable, one per bearer,
 * server thread only.
 */
public final class PoiseState {
    /** What a blow did to the bearer's stance. */
    public enum Hit { OPEN, ABSORBED, BROKEN }

    private double max, poise;
    private long lastHit = -1_000_000L, armorUntil, staggerUntil, immuneUntil;
    private boolean recoverPending;
    private int breaks;

    public PoiseState(double max) {
        this.max = Math.max(1.0, max);
        this.poise = this.max;
    }

    public double max() { return max; }

    public double poise() { return poise; }

    public double fraction() { return max <= 0.0 ? 0.0 : poise / max; }

    public int breaks() { return breaks; }

    /** Restores a saved bar (a reloaded chunk); timers are not saved, so the bearer comes back standing and armor-less. */
    public void restore(double max, double poise, int breaks) {
        this.max = Math.max(1.0, max);
        this.poise = Math.max(0.0, Math.min(this.max, poise));
        this.breaks = Math.max(0, breaks);
        this.armorUntil = 0L;
        this.staggerUntil = 0L;
        this.immuneUntil = 0L;
        this.recoverPending = false;
    }

    /** A new capacity (health changed); the bar keeps its fill share. */
    public void resize(double newMax) {
        double f = fraction();
        this.max = Math.max(1.0, newMax);
        this.poise = this.max * f;
    }

    public boolean armored(long now) { return now < armorUntil; }

    public boolean staggered(long now) { return now < staggerUntil; }

    public boolean immune(long now) { return now < immuneUntil; }

    /** Super armor for {@code ticks} ticks from now (never while staggered; never shortens a longer armor already up). */
    public void raiseArmor(long now, long ticks) {
        if (staggered(now) || ticks <= 0L) return;
        armorUntil = Math.max(armorUntil, now + ticks);
    }

    /** Drops the armor now (the attack ended or was cancelled). */
    public void dropArmor() { armorUntil = 0L; }

    /** A blow of {@code poiseDamage} lands at tick {@code now}. */
    public Hit hit(long now, double poiseDamage, PoiseTuning t) {
        lastHit = now;
        if (!armored(now)) return Hit.OPEN;
        if (immune(now)) return Hit.ABSORBED;
        poise -= Math.max(0.0, poiseDamage);
        if (poise > 0.0) return Hit.ABSORBED;
        poise = 0.0;
        armorUntil = 0L;
        staggerUntil = now + t.staggerTicks();
        immuneUntil = staggerUntil + t.immuneTicks();
        recoverPending = true;
        breaks++;
        return Hit.BROKEN;
    }

    /** Advances the regeneration by {@code elapsed} ticks (the caller may tick every few ticks). */
    public void tick(long now, int elapsed, PoiseTuning t) {
        if (staggered(now)) return;
        if (recoverPending) {
            poise = Math.max(poise, max * t.recoverFrac());
            recoverPending = false;
            return;
        }
        if (poise < max && now - lastHit >= t.regenDelay()) poise = Math.min(max, poise + PoiseMath.regen(max, elapsed, t));
    }
}
