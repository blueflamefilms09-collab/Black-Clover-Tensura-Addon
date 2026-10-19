package com.newuniverse.nusmp.labyrinth.mega.puzzle;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Why: the test player. It plays a rule the way the world does (time passes in ticks, a step is an "on" followed by an "off", a hold keeps the actor on the tile) using only the rule's own
 * public interface: its suggestions are the "perfect player", and {@link #garbage} is the "worst player" (random clicks, steps, item offers and answers on random cells, with time passing).
 * Proving that the perfect player wins from every seed AND from every state the worst player can leave behind is what "solvable, reset safe, no soft lock" means here.
 */
final class PuzzleSim {
    final PuzzleRule r;
    long t = 0;
    final List<Fx> log = new ArrayList<>();
    int steps;

    PuzzleSim(PuzzleRule r) {
        this.r = r;
        log.addAll(r.begin(0));
    }

    void advance(long to) {
        while (t < to) {
            t = Math.min(to, t + 5);
            log.addAll(r.tick(t));
        }
    }

    void play(Act s) {
        advance(Math.max(t, s.tick()));
        Act a = s.at(t);
        steps++;
        log.addAll(r.act(a));
        if (a.kind() == Act.Kind.STEP && a.arg() == 1) {
            if (a.hold() > 0) advance(t + a.hold());
            log.addAll(r.act(Act.stepOff(a.actor(), a.cell(), t)));
        }
    }

    /** Follows the rule's suggestions until it is solved; false when it is still open after {@code maxSteps} actions. */
    boolean solve(int maxSteps) {
        for (int i = 0; i < maxSteps && steps < maxSteps * 4; i++) {
            if (r.solved()) return true;
            Act s = r.suggest(t);
            if (s == null) {
                advance(t + 10);
                continue;
            }
            play(s);
        }
        return r.solved();
    }

    /** {@code n} random actions on random cells (some out of range on purpose), with time passing between them. */
    void garbage(SplittableRandom g, int n) {
        int cells = Math.max(1, r.layout().total());
        for (int i = 0; i < n && !r.solved(); i++) {
            advance(t + g.nextInt(40));
            int cell = g.nextInt(cells + 2) - 1;
            int actor = g.nextInt(3);
            Act a = switch (g.nextInt(7)) {
                case 0 -> Act.stepOn(actor, cell, t, g.nextInt(30));
                case 1 -> Act.stepOff(actor, cell, t);
                case 2 -> Act.sneakClick(actor, cell, t);
                case 3 -> Act.choose(actor, g.nextInt(4), t);
                case 4 -> Act.clickWith(actor, cell, TOKENS[g.nextInt(TOKENS.length)], t);
                default -> Act.click(actor, cell, t);
            };
            steps++;
            log.addAll(r.act(a));
        }
    }

    static final String[] TOKENS = {"coal", "bone", "gold", "lapis", "amethyst", "redstone", "iron", "quartz", "emerald", "diamond", "copper", "fire", "water", "earth", "air", "key0", "key1", "key2", "key3"};
}
