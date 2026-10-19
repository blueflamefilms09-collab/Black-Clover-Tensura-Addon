package com.newuniverse.nusmp.labyrinth.mega.puzzle.rules;

import com.newuniverse.nusmp.labyrinth.mega.puzzle.Act;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Fx;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Layout;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Params;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.PuzzleBase;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Riddles;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the riddle door, the classic "choose an answer" puzzle, done with buttons in chat. A stone sphinx asks riddles from the bank (the data the owner can extend, {@link Riddles}); three
 * answers are shown as clickable lines in a fresh order every time, so the position of the right one cannot be memorised. A right answer counts, a wrong one costs a little and is followed by a
 * DIFFERENT riddle (so guessing does not pay). The number of riddles rises with depth. Clicking the sphinx asks the open question again, so a party that scrolled past it can always have it
 * repeated. Nothing is locked: a party that gives up simply leaves.
 */
public final class RiddleDoor extends PuzzleBase {
    private final int need;
    private List<String> queue;
    private int qi, correct;
    private int[] order;

    public RiddleDoor(Params p) {
        super("riddle_door", p);
        need = 2 + p.lvl() / 2;
    }

    @Override protected Layout makeLayout() { return Layout.of(Layout.Group.row("sphinx", Layout.Role.PILLAR, 1, "sphinx").tall(2)); }

    @Override protected void init() {
        queue = new ArrayList<>(Riddles.CHOICE);
        for (int i = queue.size() - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            String t = queue.get(i);
            queue.set(i, queue.get(j));
            queue.set(j, t);
        }
        qi = 0;
        correct = 0;
        newOrder();
    }

    private void newOrder() {
        order = iota(3);
        shuffle(order, rng);
    }

    private String current() { return queue.get(qi % queue.size()); }

    private Fx ask() {
        String id = current();
        List<String> answers = new ArrayList<>();
        for (int pos = 0; pos < 3; pos++) answers.add(Riddles.answer(id, order[pos]));
        return Fx.ask(Riddles.question(id), correct + 1, answers);
    }

    @Override protected void onBegin(long t) { emit(ask()); }

    @Override public List<Fx> question() { return List.of(ask()); }

    @Override protected void handle(Act a) {
        if (a.kind() == Act.Kind.CLICK) {
            if (a.cell() == 0) emit(ask());
            return;
        }
        if (a.kind() != Act.Kind.CHOOSE || a.arg() < 0 || a.arg() > 2) return;
        if (order[a.arg()] == 0) {
            correct++;
            tone(0, 10 + correct * 3, "bell");
            if (correct >= need) { win(); return; }
            qi++;
            newOrder();
            say(-1, "right", correct, Integer.toString(correct));
            emit(ask());
        } else {
            qi++;
            newOrder();
            fail(a.actor(), 1, "drain", "wrong");
            emit(ask());
        }
    }

    @Override protected int face(int cell) { return correct > 0 ? 1 : 0; }

    @Override protected Act next(long tick) {
        for (int pos = 0; pos < 3; pos++) if (order[pos] == 0) return Act.choose(0, pos, tick);
        return null;
    }
}
