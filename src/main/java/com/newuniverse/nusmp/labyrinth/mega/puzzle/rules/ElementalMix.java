package com.newuniverse.nusmp.labyrinth.mega.puzzle.rules;

import com.newuniverse.nusmp.labyrinth.mega.puzzle.Act;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Fx;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Layout;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Params;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.PuzzleBase;
import com.newuniverse.nusmp.labyrinth.mega.puzzle.Tokens;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: elemental affinity pedestals. Four source crystals (fire, water, earth, air) each hand out a shard of their element on a right click; two to four pedestals each want a COMPOUND,
 * shown by the colour of the pedestal, and a compound is two elements: steam is fire and water, mud earth and water, lava fire and earth, dust earth and air, storm water and air, lightning
 * fire and air. Offer two shards to a pedestal (in either order; a right click with a shard in hand); the right pair seals the pedestal, a wrong pair backlashes (a cost, both shards are
 * lost). A pedestal clicked with an empty hand says what it wants; clicked while sneaking it is emptied for free, so a half-filled pedestal can always be cleared. Shards are issued whenever
 * asked, so none can be lost for good.
 */
public final class ElementalMix extends PuzzleBase {
    /** The compounds as pairs of element indices (fire 0, water 1, earth 2, air 3): steam, mud, lava, dust, storm, lightning. */
    static final int[][] COMPOUND = {{0, 1}, {2, 1}, {0, 2}, {2, 3}, {1, 3}, {0, 3}};
    private final int pedestals;
    private int[] want;
    private List<Integer>[] have;
    private boolean[] done;
    private String holding;

    public ElementalMix(Params p) {
        super("elemental_mix", p);
        pedestals = 2 + (p.lvl() >= 2 ? 1 : 0) + (p.lvl() >= 4 ? 1 : 0);
    }

    @Override protected Layout makeLayout() {
        return Layout.of(Layout.Group.row("sources", Layout.Role.PILLAR, 4, "elem"), Layout.Group.row("pedestals", Layout.Role.PILLAR, pedestals, "elem"));
    }

    @SuppressWarnings("unchecked")
    @Override protected void init() {
        int[] o = iota(COMPOUND.length);
        shuffle(o, rng);
        want = new int[pedestals];
        System.arraycopy(o, 0, want, 0, pedestals);
        have = new List[pedestals];
        for (int i = 0; i < pedestals; i++) have[i] = new ArrayList<>();
        done = new boolean[pedestals];
        holding = null;
    }

    private boolean subset(int i) {
        List<Integer> need = new ArrayList<>(List.of(COMPOUND[want[i]][0], COMPOUND[want[i]][1]));
        for (int e : have[i]) if (!need.remove((Integer) e)) return false;
        return true;
    }

    @Override protected void handle(Act a) {
        if (a.kind() != Act.Kind.CLICK) return;
        int c = a.cell();
        if (c < 4) {
            holding = Tokens.ELEMENTS.get(c);
            emit(Fx.give(a.actor(), c, holding));
            tone(c, 4 + c * 3, "chime");
            return;
        }
        int i = c - 4;
        if (done[i]) return;
        if (a.item() == null) {
            if (a.sneaking()) have[i].clear();
            else say(a.actor(), "wants", want[i], "$compound." + want[i], String.valueOf(have[i].size()));
            return;
        }
        int e = Tokens.ELEMENTS.indexOf(a.item());
        if (e < 0) return;
        emit(Fx.consume(a.actor(), a.item()));
        holding = null;
        have[i].add(e);
        tone(c, 5 + e * 3, "chime");
        if (have[i].size() < 2) return;
        if (subset(i)) {
            done[i] = true;
            spark(c, true);
            tone(c, 19, "bell");
            boolean all = true;
            for (boolean d : done) all &= d;
            if (all) win();
        } else {
            have[i].clear();
            spark(c, false);
            fail(a.actor(), 1, "hurt", "backlash");
        }
    }

    @Override protected int face(int cell) {
        if (cell < 4) return cell;
        int i = cell - 4;
        return done[i] ? 10 : 4 + want[i];
    }

    @Override protected Act next(long tick) {
        for (int i = 0; i < pedestals; i++) {
            if (done[i]) continue;
            if (!subset(i)) return Act.sneakClick(0, 4 + i, tick);
            List<Integer> need = new ArrayList<>(List.of(COMPOUND[want[i]][0], COMPOUND[want[i]][1]));
            for (int e : have[i]) need.remove((Integer) e);
            int e = need.get(0);
            if (Tokens.ELEMENTS.get(e).equals(holding)) return Act.clickWith(0, 4 + i, holding, tick);
            return Act.click(0, e, tick);
        }
        return null;
    }
}
