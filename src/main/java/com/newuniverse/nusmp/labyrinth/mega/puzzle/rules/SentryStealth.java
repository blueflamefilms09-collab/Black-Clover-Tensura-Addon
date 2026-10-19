package com.newuniverse.nusmp.labyrinth.mega.puzzle.rules;

import com.newuniverse.nusmp.labyrinth.mega.puzzle.Layout;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Params;

/**
 * Why: stealth past sentries. Between the start row and the goal row lie lanes, each watched by a stone sentry standing at its end that turns to look along the lane for one phase in
 * every four (forty ticks a phase); the phase before, the lane's tiles and its sentry glow amber as a warning. A delver caught standing on a watched lane tile for longer than a moment is
 * sent back to the start row (a small cost). Every lane is looked along for only one phase in four and the amber warning gives a full phase of notice, so the field can always be crossed;
 * the phases are drawn from the seed, so which lane is watched when differs from room to room. More lanes deeper down; more delvers means a shorter grace.
 */
public final class SentryStealth extends LaneRule {
    private static final int PHASE = 40;
    private int[] watched;

    public SentryStealth(Params p) {
        super("sentry_stealth", p, 5, 2 + p.lvl() / 2);
    }

    @Override protected Layout.Group extraGroup() { return Layout.Group.row("sentries", Layout.Role.DISPLAY, lanes, "lane"); }

    @Override protected void init() {
        watched = new int[lanes];
        for (int i = 0; i < lanes; i++) watched[i] = rng.nextInt(4);
        laneReset();
    }

    @Override protected int laneState(int lane, int col, long t) {
        int ph = (int) Math.floorMod(Math.floorDiv(t, (long) PHASE), 4L);
        if (ph == watched[lane]) return 1;
        return (ph + 1) % 4 == watched[lane] ? 2 : 0;
    }

    @Override protected int grace() { return 12 - Math.min(4, p.party() / 2); }

    @Override protected int extraFace(int j) { return laneState(j, 0, now); }
}
