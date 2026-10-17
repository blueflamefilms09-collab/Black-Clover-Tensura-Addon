package com.newuniverse.nusmp.arc.spade;

/**
 * Why: several of the Spade bosses' channelled things must be breakable by a burst of damage and not by chip damage: Morris's Re-Structure (24 ticks of
 * re-forming his body) and his Specimen tether (a thread that snaps if he is hurt hard enough while it is held). "Burst" is a share of his maximum health
 * dealt inside a window, which is the one answer a coordinated party has and a lone chipper does not. The watch is armed with a window and a limit,
 * counts damage while the window is open, and trips once the limit is reached; a window that ran out untripped is simply over. Pure, so the rule is tested
 * without a game.
 */
public final class BurstWatch {
    private double damage, limit;
    private long end;
    private boolean armed, tripped;

    /** Arms the watch for {@code ticks} ticks from {@code now}; it trips when {@code limit} damage has been dealt inside the window. */
    public void begin(long now, int ticks, double limit) {
        this.damage = 0;
        this.limit = Math.max(0, limit);
        this.end = now + Math.max(1, ticks);
        this.armed = true;
        this.tripped = false;
    }

    /** Counts a blow of {@code amount} landed at {@code now}. Blows outside the window and before {@link #begin} do nothing. */
    public void add(long now, double amount) {
        if (!armed || tripped || now >= end || !(amount > 0)) return;
        damage += amount;
        if (damage >= limit) tripped = true;
    }

    /** True once the limit was reached inside the window (stays true until the next {@link #begin} or {@link #clear}). */
    public boolean tripped() { return tripped; }

    /** True while the window is open and has not tripped. */
    public boolean open(long now) { return armed && !tripped && now < end; }

    /** Damage counted so far in this window. */
    public double damage() { return damage; }

    public double limit() { return limit; }

    /** Disarms the watch. */
    public void clear() {
        armed = false;
        tripped = false;
        damage = 0;
    }
}
