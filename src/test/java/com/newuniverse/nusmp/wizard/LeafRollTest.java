package com.newuniverse.nusmp.wizard;

import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The leaf odds of the long questionnaire (docs/wizard_long_questionnaire.md): seeded Monte Carlo over real sheets, so a change of a resolve question or a threshold that breaks the bands fails here. */
class LeafRollTest {
    private static final int N = 60000;

    /** {3-leaf, 4-leaf, 5-leaf} shares for random answers (best=false) or the best resolve answer everywhere (best=true). */
    private static double[] bands(long seed, boolean best) {
        RandomSource rng = RandomSource.create(seed);
        int[] c = new int[6];
        for (int i = 0; i < N; i++) {
            Questionnaire.Sheet s = Questionnaire.draw(rng, true);
            int[] a;
            if (best) a = Questionnaire.bestResolveAnswers(s);
            else {
                a = new int[s.texts().size()];
                for (int q = 0; q < a.length; q++) a[q] = rng.nextInt(s.tokens().get(q).size());
            }
            c[LeafRoll.leaves(Questionnaire.resolveOf(s, a), rng.nextInt(LeafRoll.LUCK_SIDES))]++;
        }
        return new double[]{c[3] / (double) N, c[4] / (double) N, c[5] / (double) N};
    }

    @Test
    void randomAnswersGiveAboutSeventyEightEighteenFour() {
        double[] b = bands(7, false);
        System.out.printf("random answers: 3 leaves %.3f, 4 leaves %.3f, 5 leaves %.3f%n", b[0], b[1], b[2]);
        assertTrue(b[0] > 0.74 && b[0] < 0.82, "3 leaves about 78%: " + b[0]);
        assertTrue(b[1] > 0.15 && b[1] < 0.22, "4 leaves about 18%: " + b[1]);
        assertTrue(b[2] > 0.015 && b[2] < 0.06, "5 leaves a few percent: " + b[2]);
    }

    @Test
    void theBestResolveAnswersGiveFourMostlyAndFiveSometimes() {
        double[] b = bands(11, true);
        System.out.printf("best answers: 3 leaves %.3f, 4 leaves %.3f, 5 leaves %.3f%n", b[0], b[1], b[2]);
        assertTrue(b[0] < 0.02, "the best path never ends on 3 leaves (almost): " + b[0]);
        assertTrue(b[1] > 0.5, "4 leaves most of the time: " + b[1]);
        assertTrue(b[2] > 0.2 && b[2] < 0.5, "5 leaves sometimes, never always: " + b[2]);
    }

    @Test
    void theOldRuleIsExactlyAsItWas() {
        for (int resolve = -2; resolve <= 6; resolve++)
            for (int luck = 0; luck <= 2; luck++) {
                int s = resolve + luck;
                assertEquals(s >= 5 ? 5 : s >= 3 ? 4 : 3, LeafRoll.oldLeaves(resolve, luck));
            }
    }

    @Test
    void luckIsOnlyZeroOrOneOnTheLongSheet() {
        assertEquals(2, LeafRoll.LUCK_SIDES);
        // one point of luck moves a result by at most one step
        for (int r = -4; r <= 20; r++) assertTrue(LeafRoll.leaves(r, 1) - LeafRoll.leaves(r, 0) <= 1);
    }
}
