package com.newuniverse.nusmp.dungeon.nexus.relic;

/**
 * The ten second attunement of a relic (docs/living_dungeon.md section 7): hold it in the main hand and sneak, without a break, for
 * {@link #TICKS} ticks. One clock per player. Letting go of sneak, turning to another slot or relic, or taking a hit starts it over. Pure:
 * the glue tells it what the player is doing, once a tick, and shows the progress it reports.
 */
public final class AttuneClock {
    public static final int TICKS = 200;

    public enum Step { IDLE, RUNNING, DONE, RESET }

    private int progress, slot = -1, relic = -1;

    /**
     * One tick. {@code relic} is the ordinal of the unattuned relic in the main hand, or -1 when the main hand holds nothing that can be
     * attuned; {@code slot} is the hotbar slot it sits in.
     */
    public Step tick(boolean sneaking, int slot, int relic, boolean hurt) {
        if (relic < 0 || !sneaking || hurt) {
            boolean had = progress > 0;
            clear();
            return had ? Step.RESET : Step.IDLE;
        }
        if (progress > 0 && (slot != this.slot || relic != this.relic)) progress = 0;          // a different item: from the beginning
        this.slot = slot;
        this.relic = relic;
        progress++;
        if (progress >= TICKS) {
            clear();
            return Step.DONE;
        }
        return Step.RUNNING;
    }

    public void clear() {
        progress = 0;
        slot = -1;
        relic = -1;
    }

    public int progress() { return progress; }

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
