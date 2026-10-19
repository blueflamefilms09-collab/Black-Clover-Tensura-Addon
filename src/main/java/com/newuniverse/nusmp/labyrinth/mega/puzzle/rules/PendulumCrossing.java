package com.newuniverse.nusmp.labyrinth.mega.puzzle.rules;

import com.newuniverse.nusmp.labyrinth.mega.puzzle.Params;

/**
 * Why: pendulum timing. Each lane of the field is swept by a blade that swings from one side to the other and back, at a speed and a starting point of its own; the tile under the blade
 * glows red and the tile it reaches next glows amber. A delver who stays on the blade's tile is cut (a small cost, back to the start row). Because the blade covers one tile of five, at
 * most two of the five tiles are ever dangerous or about to be, so there is always a place to stand and a moment to cross; deeper the blades are quicker and there are more lanes.
 * The blade's place is a pure function of the game tick, the same for everyone.
 */
public final class PendulumCrossing extends LaneRule {
    private int[] step, off;

    public PendulumCrossing(Params p) {
        super("pendulum_crossing", p, 5, 2 + p.lvl() / 2);
    }

    @Override protected void init() {
        step = new int[lanes];
        off = new int[lanes];
        for (int i = 0; i < lanes; i++) {
            step[i] = 12 + rng.nextInt(6) - Math.min(3, p.lvl());
            off[i] = rng.nextInt(2 * (w - 1));
        }
        laneReset();
    }

    private int bladeAt(int lane, long t) {
        int period = 2 * (w - 1);
        int m = (int) Math.floorMod(Math.floorDiv(t, (long) step[lane]) + off[lane], (long) period);
        return m <= w - 1 ? m : period - m;
    }

    @Override protected int laneState(int lane, int col, long t) {
        if (bladeAt(lane, t) == col) return 1;
        return bladeAt(lane, t + step[lane]) == col ? 2 : 0;
    }

    @Override protected int grace() { return 5; }
}
