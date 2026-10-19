package com.newuniverse.nusmp.labyrinth.mega.puzzle;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the same "every puzzle is solvable by its own hint, every face exists in its palette, every line it says has a text" check that the JUnit sweep makes, but available at run time
 * (the in-game self-test {@code puzzle.catalog} and the admin command {@code puzzle verify} run it on the real jar, with the real lang and weights files loaded). It plays each puzzle on a few
 * boards with the pure rule only; it touches no world. Returns one line per problem; empty means all well.
 */
public final class PuzzleVerify {
    private PuzzleVerify() {}

    public static List<String> run() {
        List<String> out = new ArrayList<>();
        for (String id : PuzzleCatalog.ids()) {
            if (PuzzleText.en(id + ".name") == null || PuzzleText.en(id + ".hint") == null) out.add(id + ": no name or hint text");
            if (PuzzleWeights.get().types.get(id) == null) out.add(id + ": no line in the weights file");
            for (int tier = 1; tier <= 5; tier += 2) for (int party : new int[] {1, 4}) {
                PuzzleRule r = PuzzleCatalog.create(id, new Params(12345L + tier * 31L + party, party, tier));
                if (r == null) { out.add(id + ": cannot be built"); continue; }
                String bad = check(r);
                if (bad != null) out.add(id + " (tier " + tier + ", party " + party + "): " + bad);
            }
        }
        return out;
    }

    private static String check(PuzzleRule r) {
        Layout lay = r.layout();
        for (int c = 0; c < lay.total(); c++) {
            Layout.Group g = lay.groupOf(c);
            if (g == null || Palettes.size(g.palette()) == 0) return "cell " + c + " has no palette";
            for (int l = 0; l < r.height(c); l++) if (r.face(c, l) < 0 || r.face(c, l) >= Palettes.size(g.palette())) return "cell " + c + " shows a face its palette lacks";
        }
        long t = 0;
        r.begin(t);
        for (int i = 0; i < 4000 && !r.solved(); i++) {
            Act s = r.suggest(t);
            if (s == null) { t += 10; r.tick(t); continue; }
            while (t < s.tick()) { t = Math.min(s.tick(), t + 5); r.tick(t); }
            Act a = s.at(t);
            for (Fx f : r.act(a)) {
                if (f.kind() == Fx.Kind.SAY && PuzzleText.resolve(r.id(), f.key()) == null) return "says '" + f.key() + "' with no text";
            }
            if (a.stepsOn()) {
                long until = t + a.hold();
                while (t < until) { t = Math.min(until, t + 5); r.tick(t); }
                r.act(Act.stepOff(a.actor(), a.cell(), t));
            }
        }
        return r.solved() ? null : "not solved by its own hint";
    }
}
