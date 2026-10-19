package com.newuniverse.nusmp.labyrinth.mega.puzzle.rules;

import com.newuniverse.nusmp.labyrinth.mega.puzzle.Act;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Layout;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Params;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.PuzzleBase;

/**
 * Why: "Simon says" with sound cues: a ring of coloured levers, each with its own note. The dungeon flashes and plays a melody of levers that grows by one each round; the party repeats it.
 * Input during the show is ignored (no penalty, a hint line), a wrong lever costs a little and replays the same round, and if nobody touches a lever for twenty seconds the round replays by
 * itself. The show is driven by the clock the world passes in, so a lag spike cannot make the tune and the flash disagree. After four mistakes the game starts over from round one.
 */
public final class SimonLevers extends PuzzleBase {
    private static final int GAP = 14, FLASH = 8;
    private static final int[] NOTE = {0, 3, 5, 7, 10, 12};
    private final int n, rounds;
    private int[] seq;
    private int round, e, shown;
    private boolean inputPhase;
    private long showT0, lastInputAt;
    private long[] flashUntil;

    public SimonLevers(Params p) {
        super("simon_levers", p);
        n = 4 + (p.lvl() >= 3 ? 1 : 0);
        rounds = Math.min(8, 3 + p.lvl() + (p.party() >= 3 ? 1 : 0));
    }

    @Override protected Layout makeLayout() { return Layout.of(Layout.Group.ring("levers", Layout.Role.PILLAR, n, "colour")); }

    @Override protected void init() {
        seq = new int[rounds];
        for (int i = 0; i < rounds; i++) {
            int v;
            do { v = rng.nextInt(n); } while (i > 0 && v == seq[i - 1]);
            seq[i] = v;
        }
        flashUntil = new long[n];
        round = 1;
        replay(-1);
    }

    private void replay(long start) {
        e = 0;
        shown = 0;
        inputPhase = false;
        showT0 = start;
    }

    @Override protected void onBegin(long t) { if (showT0 < 0) showT0 = t + 20; }

    @Override protected void onTick(long t) {
        if (!inputPhase) {
            if (showT0 < 0) showT0 = t + 20;
            while (shown < round && showT0 + (long) GAP * shown <= t) {
                int c = seq[shown];
                flashUntil[c] = showT0 + (long) GAP * shown + FLASH;
                tone(c, NOTE[c], "harp");
                shown++;
            }
            if (shown >= round && t >= showT0 + (long) GAP * (round - 1) + FLASH + 4) {
                inputPhase = true;
                lastInputAt = t;
            }
        } else if (t - lastInputAt > 400) {
            replay(t + 10);
            say(-1, "again", 0);
        }
    }

    @Override protected void handle(Act a) {
        if (a.kind() != Act.Kind.CLICK || a.cell() >= n) return;
        int c = a.cell();
        if (!inputPhase) { say(a.actor(), "watch", 0); return; }
        lastInputAt = now;
        flashUntil[c] = now + FLASH;
        tone(c, NOTE[c], "harp");
        if (seq[e] == c) {
            e++;
            if (e == round) {
                if (round == rounds) { win(); return; }
                round++;
                replay(now + 30);
                say(-1, "round", round, Integer.toString(round));
            }
        } else {
            spark(c, false);
            replay(now + 40);
            fail(a.actor(), 1, "dark", "wrong");
        }
    }

    @Override protected int face(int cell) { return now < flashUntil[cell] ? 8 + cell : cell; }

    @Override protected int failLimit() { return 4; }

    @Override protected Act next(long tick) {
        if (!inputPhase) {
            if (showT0 < 0) return null;
            return Act.click(0, seq[e], showT0 + (long) GAP * (round - 1) + FLASH + 6);
        }
        return Act.click(0, seq[e], tick);
    }
}
