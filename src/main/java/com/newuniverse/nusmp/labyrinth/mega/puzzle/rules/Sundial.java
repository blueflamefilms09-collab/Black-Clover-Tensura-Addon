package com.newuniverse.nusmp.labyrinth.mega.puzzle.rules;

import com.newuniverse.nusmp.labyrinth.mega.puzzle.Act;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Layout;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Params;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.PuzzleBase;

/**
 * Why: a sundial for a place without a sun. A ring of twelve hour stones is lit by a shadow that creeps round it, one hour every few seconds (faster deeper down); a gold stone marks the
 * hour that must be struck, and a click on the gnomon at the moment the shadow lies on the gold hour is a hit. Enough hits solve it; a click at the wrong moment costs a little and takes a
 * hit away. The shadow is a pure function of the game tick (not of when anybody looked), so every player sees the same hour and the server alone decides.
 */
public final class Sundial extends PuzzleBase {
    private final int step, hitsNeeded;
    private int phase, target, hits;

    public Sundial(Params p) {
        super("sundial", p);
        step = 50 - 4 * p.lvl();
        hitsNeeded = 3 + p.lvl() / 2;
    }

    @Override protected Layout makeLayout() { return Layout.of(Layout.Group.ring("hours", Layout.Role.DISPLAY, 12, "dial"), Layout.Group.row("gnomon", Layout.Role.PILLAR, 1, "altar")); }

    @Override protected void init() {
        phase = rng.nextInt(12);
        target = rng.nextInt(12);
        hits = 0;
    }

    private int shadow(long t) { return (int) Math.floorMod(Math.floorDiv(t, (long) step) + phase, 12L); }

    @Override protected void handle(Act a) {
        if (a.kind() != Act.Kind.CLICK || a.cell() != 12) return;
        if (shadow(a.tick()) == target) {
            hits++;
            tone(target, 6 + hits * 3, "bell");
            spark(target, true);
            if (hits >= hitsNeeded) { win(); return; }
            int nt;
            do { nt = rng.nextInt(12); } while (nt == target);
            target = nt;
        } else {
            hits = Math.max(0, hits - 1);
            spark(shadow(a.tick()), false);
            fail(a.actor(), 1, "slow", "miss");
        }
    }

    @Override protected int face(int cell) {
        if (cell == 12) return 0;
        int s = shadow(now);
        if (cell == s && cell == target) return 3;
        if (cell == s) return 1;
        return cell == target ? 2 : 0;
    }

    @Override protected Act next(long tick) {
        long t = nextTickAt(tick, step, phase, 12, target);
        return Act.click(0, 12, t > tick ? t + 3 : t);
    }
}
