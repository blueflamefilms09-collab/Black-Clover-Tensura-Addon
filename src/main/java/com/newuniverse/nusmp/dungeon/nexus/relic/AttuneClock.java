package com.newuniverse.nusmp.dungeon.nexus.relic;

/**
 * The ten second attunement of a relic (docs/living_dungeon.md section 7): hold it in the main hand and sneak, without a break, for
 * {@link #TICKS} ticks. One clock per player. Letting go of sneak, turning to another slot or relic, taking a hit, or moving more than
 * {@link #DRIFT} blocks from where the ten seconds began starts it over (the owner's design: movement or damage resets the counter). Pure: the
 * glue tells it what the player is doing, once a tick, and shows the progress it reports.
 */
public final class AttuneClock {
    public static final int TICKS = 200;
    /** How far, in blocks along the ground, the player may drift from where the attunement began before it starts over: it asks them to stand still. */
    public static final double DRIFT = 0.25;

    public enum Step { IDLE, RUNNING, DONE, RESET }

    /** Why the last tick was a RESET (NONE when it was not one). */
    public enum Why { NONE, STOPPED, HURT, MOVED }

    private int progress, slot = -1, relic = -1;
    private double anchorX, anchorZ;
    private Why why = Why.NONE;

    /** One tick with no sense of where the player is (it never counts as moving). */
    public Step tick(boolean sneaking, int slot, int relic, boolean hurt) { return tick(sneaking, slot, relic, hurt, 0.0, 0.0); }

    /**
     * One tick. {@code relic} is the ordinal of the unattuned relic in the main hand, or -1 when the main hand holds nothing that can be
     * attuned; {@code slot} is the hotbar slot it sits in; {@code x} and {@code z} are where the player stands (the first tick of a run is the
     * place they must keep).
     */
    public Step tick(boolean sneaking, int slot, int relic, boolean hurt, double x, double z) {
        why = Why.NONE;
        if (relic < 0 || !sneaking || hurt) {
            boolean had = progress > 0;
            if (had) why = hurt ? Why.HURT : Why.STOPPED;
            clear();
            return had ? Step.RESET : Step.IDLE;
        }
        if (progress > 0 && (slot != this.slot || relic != this.relic)) progress = 0;          // a different item: from the beginning
        if (progress > 0 && drifted(x, z)) {
            why = Why.MOVED;
            clear();
            return Step.RESET;
        }
        if (progress == 0) {
            anchorX = x;
            anchorZ = z;
        }
        this.slot = slot;
        this.relic = relic;
        progress++;
        if (progress >= TICKS) {
            clear();
            return Step.DONE;
        }
        return Step.RUNNING;
    }

    private boolean drifted(double x, double z) {
        double dx = x - anchorX, dz = z - anchorZ;
        return dx * dx + dz * dz > DRIFT * DRIFT;
    }

    public void clear() {
        progress = 0;
        slot = -1;
        relic = -1;
    }

    public int progress() { return progress; }

    /** Why the last tick was a RESET: NONE if it was not one. */
    public Why why() { return why; }

    public double fraction() { return Math.min(1.0, progress / (double) TICKS); }

    /** Whole seconds still to go, rounded up. */
    public int secondsLeft() { return (TICKS - progress + 19) / 20; }

    /** A text bar of {@code width} cells for the action bar. */
    public static String bar(double fraction, int width) {
        int full = (int) Math.round(Math.max(0.0, Math.min(1.0, fraction)) * width);
        StringBuilder b = new StringBuilder(width);
        for (int i = 0; i < width; i++) b.append(i < full ? '█' : '░');
        return b.toString();
    }
}
