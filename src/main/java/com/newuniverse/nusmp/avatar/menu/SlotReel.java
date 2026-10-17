package com.newuniverse.nusmp.avatar.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Why: the "Randomize Aptitude" button is a slot machine. The RESULT is the server's (a client must never be able to pick it), but the show of the reel
 * spinning and slowing down onto it is the screen's. This is that show as pure arithmetic: a strip of aptitude ids made of decoys shuffled from a seed, with the
 * server's winner as the LAST cell, scrolled by an eased curve that starts fast and arrives at the winner's cell exactly (not near it) at the final tick. The
 * decoys are chosen from the seed the server sent, so replaying the same roll shows the same reel; no decoy equals its neighbour or the winner right before the
 * end, so the stop always looks like a stop. The screen turns {@link #position} into a vertical or horizontal scroll and plays a tick sound each time
 * {@link #cellsCrossed} is above zero.
 */
public final class SlotReel {
    private final List<String> strip;
    private final int totalTicks;

    private SlotReel(List<String> strip, int totalTicks) {
        this.strip = strip;
        this.totalTicks = totalTicks;
    }

    /**
     * @param winner the id the server rolled (the reel ends on it)
     * @param pool   the ids the decoys are taken from (the winner may be in it; it is not used as a decoy near the end)
     * @param seed   the server's reel seed
     * @param cells  how many cells the strip has (at least 8): the farther the reel travels, the faster it starts
     * @param ticks  how long the whole spin takes (at least 10)
     */
    public static SlotReel build(String winner, List<String> pool, int seed, int cells, int ticks) {
        int n = Math.max(8, cells);
        List<String> decoys = new ArrayList<>();
        if (pool != null) for (String id : pool) if (id != null && !id.equals(winner)) decoys.add(id);
        List<String> strip = new ArrayList<>(n);
        Random rng = new Random(seed * 0x9E3779B97F4A7C15L + 12345L);
        String prev = "";
        for (int i = 0; i < n - 1; i++) {
            if (decoys.isEmpty()) { strip.add(winner); continue; }
            String pick = decoys.get(rng.nextInt(decoys.size()));
            int guard = 0;
            while (pick.equals(prev) && decoys.size() > 1 && guard++ < 8) pick = decoys.get(rng.nextInt(decoys.size()));
            strip.add(pick);
            prev = pick;
        }
        strip.add(winner);
        return new SlotReel(strip, Math.max(10, ticks));
    }

    public int cells() { return strip.size(); }

    /** The winner's cell: always the last. */
    public int winnerIndex() { return strip.size() - 1; }

    public String winner() { return strip.get(strip.size() - 1); }

    /** The id in cell {@code i}; cells before the strip show its first, cells after it show the winner. */
    public String idAt(int i) { return strip.get(Math.max(0, Math.min(strip.size() - 1, i))); }

    public int totalTicks() { return totalTicks; }

    /** Where the reel is after {@code ticks} (fractions allowed): a cell index, 0 at the start and exactly {@link #winnerIndex} at the end, never beyond it. */
    public float position(float ticks) {
        float u = ForgeMotion.clamp01(ticks / totalTicks);
        return winnerIndex() * ForgeMotion.easeOutCubic(u);
    }

    public boolean done(float ticks) { return ticks >= totalTicks; }

    /** How many cell boundaries the reel passed between two moments (for the tick sound); never negative. */
    public int cellsCrossed(float fromTicks, float toTicks) {
        int a = (int) Math.floor(position(fromTicks)), b = (int) Math.floor(position(toTicks));
        return Math.max(0, b - a);
    }
}
