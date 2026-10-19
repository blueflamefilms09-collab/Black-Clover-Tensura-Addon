package com.newuniverse.nusmp.labyrinth.mega.puzzle.rules;

import com.newuniverse.nusmp.labyrinth.mega.puzzle.Act;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Layout;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Params;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.PuzzleBase;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: a melody to be played back by ear. Five or six note pillars stand in a row, shaded from light (the lowest note) to dark (the highest); a bell, when struck, plays the tune the vault
 * wants (a few notes longer deeper down) and wipes the notes played so far; the stave stone listens to the last notes played and judges them. Notes can be tried freely (a pillar only plays
 * its note), so the puzzle is about hearing, not about fast hands; only a wrong tune at the stave costs a little, and it clears the notes. A pillar's note never changes, so the party can
 * learn the row. The tune never repeats a note twice in a row.
 */
public final class Melody extends PuzzleBase {
    private static final int[] PITCH = {0, 2, 4, 7, 9, 12};
    private static final int GAP = 12;
    private final int n, len;
    private int[] tune;
    private final List<Integer> played = new ArrayList<>();
    private int playIdx;
    private long playT0;

    public Melody(Params p) {
        super("melody_notes", p);
        n = 5 + (p.lvl() >= 3 ? 1 : 0);
        len = Math.min(8, 3 + p.lvl() + (p.party() >= 4 ? 1 : 0));
    }

    @Override protected Layout makeLayout() {
        return Layout.of(Layout.Group.row("notes", Layout.Role.PILLAR, n, "pitch"), Layout.Group.row("bell", Layout.Role.PILLAR, 1, "altar"), Layout.Group.row("stave", Layout.Role.PILLAR, 1, "altar"));
    }

    @Override protected void init() {
        tune = new int[len];
        for (int i = 0; i < len; i++) {
            int v;
            do { v = rng.nextInt(n); } while (i > 0 && v == tune[i - 1]);
            tune[i] = v;
        }
        played.clear();
        playIdx = len;
    }

    @Override protected boolean countsAsMove(Act a) { return false; }

    private void startPlay(long at) {
        playIdx = 0;
        playT0 = at;
    }

    @Override protected void onBegin(long t) { startPlay(t + 10); }

    @Override protected void onTick(long t) {
        while (playIdx < len && playT0 + (long) GAP * playIdx <= t) {
            tone(tune[playIdx], PITCH[tune[playIdx]] + 6, "harp");
            playIdx++;
        }
    }

    @Override protected void handle(Act a) {
        if (a.kind() != Act.Kind.CLICK) return;
        int c = a.cell();
        if (c < n) {
            tone(c, PITCH[c] + 6, "harp");
            played.add(c);
            if (played.size() > len) played.remove(0);
        } else if (c == n) {
            played.clear();
            startPlay(now + 5);
        } else if (c == n + 1) {
            boolean ok = played.size() == len;
            for (int i = 0; ok && i < len; i++) ok = played.get(i) == tune[i];
            if (ok) { tone(c, 19, "bell"); win(); }
            else {
                played.clear();
                fail(a.actor(), 1, "dark", "offkey");
            }
        }
    }

    @Override protected int face(int cell) { return cell < n ? cell : 0; }

    @Override protected Act next(long tick) {
        boolean prefix = played.size() <= len;
        for (int i = 0; prefix && i < played.size(); i++) prefix = played.get(i) == tune[i];
        if (!prefix) return Act.click(0, n, tick);
        if (played.size() < len) return Act.click(0, tune[played.size()], tick);
        return Act.click(0, n + 1, tick);
    }
}
