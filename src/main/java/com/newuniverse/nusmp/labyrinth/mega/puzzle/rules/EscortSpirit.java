package com.newuniverse.nusmp.labyrinth.mega.puzzle.rules;

import com.newuniverse.nusmp.labyrinth.mega.puzzle.Act;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Layout;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Params;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.PuzzleBase;

import java.util.HashMap;
import java.util.Map;

/**
 * Why: escort a spirit along a path of floor tiles. A pale spirit stands on the first tile; it moves on to the next tile only while somebody stands on that next tile (two seconds, to light
 * the way), and a shade creeps along the path behind it, a tile every second and a half or so (faster with depth and with a bigger party). If the shade reaches the spirit it is frightened
 * back to the start and the shade falls back too (a small cost); anybody who stands on the shade's tile for a second pushes it back two tiles. One delver can finish it alone (the
 * spirit is quicker than the shade at every depth) but a party can split the work. The rule counts only time the actors really stood on tiles, from the step events the world sends.
 */
public final class EscortSpirit extends PuzzleBase {
    private static final int HOLD = 40, START_GAP = 6;
    private final int len, shadeEvery;
    private int s, h, hold, shadeAcc, pushAcc;
    private long lastT;
    private final Map<Integer, Integer> on = new HashMap<>();                          // cell -> number of actors on it

    public EscortSpirit(Params p) {
        super("escort_spirit", p);
        len = 6 + p.lvl();
        shadeEvery = Math.max(34, 74 - 8 * p.lvl() - 3 * (p.party() - 1));
    }

    @Override protected Layout makeLayout() { return Layout.of(Layout.Group.row("path", Layout.Role.PLATE, len, "spirit")); }

    @Override protected void init() {
        s = 0;
        h = -START_GAP;
        hold = shadeAcc = pushAcc = 0;
        lastT = now;
        on.clear();
    }

    private boolean occupied(int cell) { return on.getOrDefault(cell, 0) > 0; }

    @Override protected void handle(Act a) {
        if (a.kind() != Act.Kind.STEP) return;
        if (a.stepsOn()) on.merge(a.cell(), 1, Integer::sum);
        else on.computeIfPresent(a.cell(), (k, v) -> v > 1 ? v - 1 : null);
    }

    @Override protected void onTick(long t) {
        int dt = (int) Math.max(0, Math.min(20, t - lastT));
        lastT = t;
        if (dt == 0) return;
        if (s + 1 < len) {
            if (occupied(s + 1)) {
                hold += dt;
                if (hold >= HOLD) {
                    s++;
                    hold = 0;
                    tone(s, 4 + 2 * s, "bell");
                    spark(s, true);
                    if (s == len - 1) { win(); return; }
                }
            } else hold = Math.max(0, hold - dt);
        }
        shadeAcc += dt;
        if (shadeAcc >= shadeEvery) { shadeAcc -= shadeEvery; h++; }
        if (h >= 0 && h < len && occupied(h)) {
            pushAcc += dt;
            if (pushAcc >= 20) { h = Math.max(-START_GAP, h - 2); pushAcc = 0; tone(Math.max(0, h), 2, "bass"); }
        } else pushAcc = 0;
        if (h >= s) {
            spark(s, false);
            s = 0;
            h = -START_GAP;
            hold = shadeAcc = 0;
            fail(-1, 1, "dark", "caught");
        }
    }

    @Override protected int face(int cell) {
        if (cell == h) return 3;
        if (cell == s) return 2;
        return cell < s ? 1 : 0;
    }

    @Override protected Act next(long tick) { return s + 1 < len ? Act.stepOn(0, s + 1, tick, HOLD + 3) : null; }

    @Override public int nudgeTicks() { return 400; }
}
