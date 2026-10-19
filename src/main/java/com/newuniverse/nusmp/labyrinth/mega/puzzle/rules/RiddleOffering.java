package com.newuniverse.nusmp.labyrinth.mega.puzzle.rules;

import com.newuniverse.nusmp.labyrinth.mega.puzzle.Act;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Fx;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Layout;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Params;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.PuzzleBase;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Riddles;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Tokens;

/**
 * Why: the riddle door answered by an OFFERING instead of a button. Each altar speaks a riddle whose answer is a thing (coal, bone, gold ...); a row of bowls holds blocks of those things and
 * a right click on a bowl hands over one of them. Carry the right one to the altar and click it: a right offering lights the altar, a wrong one is refused with a small cost and the item is
 * given back. A delver who already carries the answer (a gold ingot, a bone) may offer their own. Bowls give out an item whenever asked, so a lost or dropped offering never blocks the
 * puzzle. The bowls always hold every answer plus two decoys.
 */
public final class RiddleOffering extends PuzzleBase {
    private final int altars, bowls;
    private String[] answer, bowl;
    private boolean[] done;
    private String holding;

    public RiddleOffering(Params p) {
        super("riddle_offering", p);
        altars = 2 + p.lvl() / 2;
        bowls = altars + 2;
    }

    @Override protected Layout makeLayout() {
        return Layout.of(Layout.Group.row("altars", Layout.Role.PILLAR, altars, "altar"), Layout.Group.row("bowls", Layout.Role.PILLAR, bowls, "token"));
    }

    @Override protected void init() {
        int[] t = iota(Tokens.ITEMS.size());
        shuffle(t, rng);
        answer = new String[altars];
        String[] pool = new String[bowls];
        for (int i = 0; i < bowls; i++) {
            pool[i] = Tokens.ITEMS.get(t[i]);
            if (i < altars) answer[i] = pool[i];
        }
        int[] o = iota(bowls);
        shuffle(o, rng);
        bowl = new String[bowls];
        for (int i = 0; i < bowls; i++) bowl[i] = pool[o[i]];
        done = new boolean[altars];
        holding = null;
    }

    @Override protected void handle(Act a) {
        if (a.kind() != Act.Kind.CLICK) return;
        int c = a.cell();
        if (c < altars) {
            if (done[c]) return;
            if (a.item() == null) { say(a.actor(), Riddles.itemQuestion(answer[c]), c + 1, Integer.toString(c + 1)); return; }
            if (answer[c].equals(a.item())) {
                done[c] = true;
                holding = null;
                emit(Fx.consume(a.actor(), a.item()));
                spark(c, true);
                tone(c, 8 + 3 * c, "bell");
                boolean all = true;
                for (boolean d : done) all &= d;
                if (all) win();
            } else if (Tokens.ordinary(a.item())) {
                spark(c, false);
                fail(a.actor(), 1, "drain", "refused");
            }
        } else {
            holding = bowl[c - altars];
            emit(Fx.give(a.actor(), c, holding));
            tone(c, 2, "chime");
        }
    }

    @Override protected int face(int cell) {
        if (cell < altars) return done[cell] ? 1 : 0;
        return Tokens.itemIndex(bowl[cell - altars]);
    }

    @Override protected Act next(long tick) {
        for (int i = 0; i < altars; i++) {
            if (done[i]) continue;
            if (answer[i].equals(holding)) return Act.clickWith(0, i, holding, tick);
            for (int j = 0; j < bowls; j++) if (bowl[j].equals(answer[i])) return Act.click(0, altars + j, tick);
        }
        return null;
    }
}
