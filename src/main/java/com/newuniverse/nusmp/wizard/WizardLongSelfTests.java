package com.newuniverse.nusmp.wizard;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.util.RandomSource;

import java.util.List;

/**
 * Self-test {@code wizard.longq.sheet} (smoke prefix wizard.longq) of the CI server smoke test (docs/wizard_long_questionnaire.md): inside the booted mod, a seeded long sheet has the right size and the sections in order, the old
 * sheet is still 12 questions without sections, and reading a best-resolve path and a random path gives a grimoire of 3..5 leaves, a summary of five lines and the stored resolve numbers.
 * Pure data and a seeded random source: runs on a dedicated server, touches no client class and no player.
 */
public final class WizardLongSelfTests {
    private WizardLongSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("wizard.longq.sheet", server -> run());
    }

    private static SelfTests.Result run() {
        RandomSource rng = RandomSource.create(20240607L);
        Questionnaire.Sheet old = Questionnaire.draw(rng, false);
        if (old.texts().size() != 12 || !old.cats().isEmpty() || old.longMode()) return SelfTests.Result.fail("the old sheet is not the 12-question sheet");
        Questionnaire.Sheet sheet = Questionnaire.draw(rng, true);
        int expect = 0;
        for (int i = 0; i < Questionnaire.DRAW_LONG.length; i++) expect += Math.min(Questionnaire.DRAW_LONG[i], Questionnaire.longPools()[i].length);
        if (sheet.texts().size() != expect || sheet.cats().size() != expect || !sheet.longMode()) return SelfTests.Result.fail("the long sheet has " + sheet.texts().size() + " questions, expected " + expect);
        int last = -1;
        for (String c : sheet.cats()) {
            int at = SheetSections.ORDER.indexOf(c);
            if (at < last) return SelfTests.Result.fail("the sections are out of order");
            last = at;
        }
        if (SheetSections.parts(sheet.cats()) != 5) return SelfTests.Result.fail("the long sheet should have 5 parts");
        var pool = MagicFlavors.pool();
        int[] best = Questionnaire.bestResolveAnswers(sheet);
        var top = Questionnaire.result(sheet, best, pool, true, 0.5, rng);
        int[] any = new int[best.length];
        for (int i = 0; i < any.length; i++) any[i] = rng.nextInt(sheet.tokens().get(i).size());
        var random = Questionnaire.result(sheet, any, pool, true, 0.5, rng);
        if (top == null || random == null) return SelfTests.Result.fail("a valid answer path was refused");
        if (top.leaves() < 3 || top.leaves() > 5 || random.leaves() < 3 || random.leaves() > 5) return SelfTests.Result.fail("leaves out of 3..5: " + top.leaves() + " / " + random.leaves());
        if (top.detail() == null || top.detail().resolve() != Questionnaire.resolveOf(sheet, best) || top.detail().resolve() > top.detail().resolveMax()) return SelfTests.Result.fail("the resolve of the best path is wrong");
        List<String> lines = Questionnaire.summary(top);
        if (lines.size() != 5) return SelfTests.Result.fail("the summary has " + lines.size() + " lines");
        if (Questionnaire.result(sheet, new int[]{0}, pool, true, 0.5, rng) != null) return SelfTests.Result.fail("a wrong-sized answer list was accepted");
        return SelfTests.Result.ok(expect + " questions, best path " + top.leaves() + " leaves (resolve " + top.detail().resolve() + "/" + top.detail().resolveMax() + "), random path " + random.leaves() + " leaves");
    }
}
