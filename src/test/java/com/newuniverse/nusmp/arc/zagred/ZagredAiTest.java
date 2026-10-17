package com.newuniverse.nusmp.arc.zagred;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** Why: he answers what the party does (a volley gets Shatter, a flier gets Fall, a crowd gets Banish ...), and every such claim of the docs is a line here, so the weights cannot drift from the text. */
class ZagredAiTest {
    static ZagredAi.Read calm() {
        return new ZagredAi.Read(10, 1, 0, 0, 0, 0, 0.0, true, 2, false, "", 0, 4, false, false, false);
    }

    static ZagredAi.Read with(ZagredAi.Read r, int foes, int close, int fliers, int shots, int casters, double burst) {
        return new ZagredAi.Read(r.distance(), foes, close, fliers, shots, casters, burst, r.controlReady(), r.stage(), r.gates(), r.lastId(), r.pages(), r.pageCap(),
                r.silenced(), r.verdictOpen(), r.ruleOpen());
    }

    static double w(String id, ZagredAi.Read r) { return ZagredAi.weight(ZagredTable.byId(id), r); }

    @Test
    void aFlierIsAnsweredWithFall() {
        double calm = w(ZagredTable.FALL, calm());
        double flier = w(ZagredTable.FALL, with(calm(), 2, 0, 2, 0, 0, 0));
        assertTrue(flier > calm * 3, "fall is far likelier against fliers: " + calm + " -> " + flier);
    }

    @Test
    void aVolleyIsAnsweredWithShatterAndTheMoreArrowsTheLikelier() {
        double none = w(ZagredTable.SHATTER, calm());
        double one = w(ZagredTable.SHATTER, with(calm(), 1, 0, 0, 1, 0, 0));
        double many = w(ZagredTable.SHATTER, with(calm(), 1, 0, 0, 3, 0, 0));
        assertTrue(one > none * 10 && many > one);
        assertTrue(many > w(ZagredTable.CLAW, calm()), "a volley beats a plain claw at range");
    }

    @Test
    void aBurstOfDamageIsAnsweredWithReverse() {
        assertTrue(w(ZagredTable.REVERSE, with(calm(), 1, 0, 0, 0, 0, 0.2)) > w(ZagredTable.REVERSE, calm()) * 10);
    }

    @Test
    void aCrowdIsAnsweredWithBanish() {
        double none = w(ZagredTable.BANISH, calm());
        double three = w(ZagredTable.BANISH, with(calm(), 5, ZagredMath.crowdSize() - 1, 0, 0, 0, 0));
        double crowd = w(ZagredTable.BANISH, with(calm(), 6, ZagredMath.crowdSize(), 0, 0, 0, 0));
        assertTrue(crowd > three && three > none * 10, none + " " + three + " " + crowd);
        assertTrue(crowd > w(ZagredTable.CLAW, with(calm(), 6, ZagredMath.crowdSize(), 0, 0, 0, 0)), "against a crowd banish is his first answer");
    }

    @Test
    void theStrongestCasterIsAnsweredWithSeal() {
        assertTrue(w(ZagredTable.SEAL, with(calm(), 2, 0, 0, 0, 1, 0)) > w(ZagredTable.SEAL, calm()) * 5);
    }

    @Test
    void aRingOfRangedFightersIsAnsweredWithHalt() {
        assertTrue(w(ZagredTable.HALT, with(calm(), 3, 2, 0, 0, 0, 0)) > w(ZagredTable.HALT, calm()));
    }

    @Test
    void controlWordsWaitForTheControlLaw() {
        ZagredAi.Read r = calm();
        ZagredAi.Read locked = new ZagredAi.Read(4, 3, 2, 0, 0, 1, 0, false, 2, false, "", 0, 4, false, false, false);
        assertEquals(0.0, w(ZagredTable.HALT, locked), 1e-12);
        assertEquals(0.0, w(ZagredTable.SEAL, locked), 1e-12);
        assertTrue(w(ZagredTable.TRIDENT, locked) > 0, "the other Words are not affected");
        assertTrue(w(ZagredTable.CLAW, locked) > 0);
    }

    @Test
    void aSilencedHeCannotSpeakButCanStillFight() {
        ZagredAi.Read r = calm();
        ZagredAi.Read quiet = new ZagredAi.Read(3, 2, 1, 0, 0, 0, 0, true, 2, false, "", 0, 4, true, false, false);
        for (ZagredTable.Spec s : ZagredTable.specs()) {
            if (s.isWord()) assertEquals(0.0, ZagredAi.weight(s, quiet), 1e-12, s.id());
        }
        assertTrue(ZagredAi.weight(ZagredTable.byId(ZagredTable.CLAW), quiet) > 0);
        assertTrue(ZagredAi.weight(ZagredTable.byId(ZagredTable.ARTS), new ZagredAi.Read(10, 1, 0, 0, 0, 0, 0, true, 2, false, "", 0, 4, true, false, false)) > 0);
        assertTrue(r.stage() == 2);
    }

    @Test
    void whileAVerdictWaitsNoOtherWordIsSpoken() {
        ZagredAi.Read open = new ZagredAi.Read(4, 3, 1, 0, 0, 0, 0, true, 3, false, "verdict", 0, 4, false, true, false);
        for (ZagredTable.Spec s : ZagredTable.specs()) if (s.isWord()) assertEquals(0.0, ZagredAi.weight(s, open), 1e-12, s.id());
        assertTrue(ZagredAi.weight(ZagredTable.byId(ZagredTable.CLAW), open) > 0);
    }

    @Test
    void whileARuleHoldsHeDoesNotStackAVerdictOrAnotherOverwrite() {
        ZagredAi.Read rule = new ZagredAi.Read(10, 3, 1, 0, 0, 0, 0, true, 3, false, "", 0, 4, false, false, true);
        assertEquals(0.0, w(ZagredTable.VERDICT, rule), 1e-12);
        assertEquals(0.0, w(ZagredTable.OVERWRITE, rule), 1e-12);
        assertTrue(w(ZagredTable.TRIDENT, rule) > 0, "he keeps fighting while the party breaks the stones");
    }

    @Test
    void theSameWordTwiceInARowIsDiscouraged() {
        ZagredAi.Read a = calm();
        ZagredAi.Read b = new ZagredAi.Read(a.distance(), 1, 0, 0, 0, 0, 0, true, 2, false, ZagredTable.TRIDENT, 0, 4, false, false, false);
        assertTrue(w(ZagredTable.TRIDENT, b) < w(ZagredTable.TRIDENT, a) * 0.35);
    }

    @Test
    void pagesAreCalledOnlyUnderTheCapAndMostWhenNoneStand() {
        ZagredAi.Read none = new ZagredAi.Read(10, 2, 0, 0, 0, 0, 0, true, 2, false, "", 0, 4, false, false, false);
        ZagredAi.Read some = new ZagredAi.Read(10, 2, 0, 0, 0, 0, 0, true, 2, false, "", 2, 4, false, false, false);
        ZagredAi.Read full = new ZagredAi.Read(10, 2, 0, 0, 0, 0, 0, true, 2, false, "", 4, 4, false, false, false);
        assertTrue(w(ZagredTable.PAGES, none) > w(ZagredTable.PAGES, some));
        assertEquals(0.0, w(ZagredTable.PAGES, full), 1e-12);
    }

    @Test
    void theDevilLeapsAndTheHumanDoesNot() {
        ZagredAi.Read human = new ZagredAi.Read(8, 1, 0, 0, 0, 0, 0, true, 2, false, "", 0, 4, false, false, false);
        ZagredAi.Read devil = new ZagredAi.Read(8, 1, 0, 0, 0, 0, 0, true, 4, true, "", 0, 4, false, false, false);
        assertTrue(w(ZagredTable.CLAW, devil) > w(ZagredTable.CLAW, human) * 3, "eight blocks away: the devil leaps, the man does not");
    }

    @Test
    void theDrawFollowsTheWeights() {
        assertEquals(-1, ZagredAi.choose(new double[]{0, 0, 0}, 0.5));
        assertEquals(-1, ZagredAi.choose(new double[0], 0.5));
        assertEquals(1, ZagredAi.choose(new double[]{0, 5, 0}, 0.0));
        assertEquals(1, ZagredAi.choose(new double[]{0, 5, 0}, 1.0));
        assertEquals(0, ZagredAi.choose(new double[]{1, 1}, 0.2));
        assertEquals(1, ZagredAi.choose(new double[]{1, 1}, 0.8));
        Random rng = new Random(3);
        int[] hits = new int[3];
        for (int i = 0; i < 6000; i++) hits[ZagredAi.choose(new double[]{1, 2, 3}, rng.nextDouble())]++;
        assertTrue(hits[0] < hits[1] && hits[1] < hits[2], "frequencies follow the weights: " + hits[0] + " " + hits[1] + " " + hits[2]);
        assertTrue(Math.abs(hits[2] / 6000.0 - 0.5) < 0.04);
    }

    @Test
    void everyMoveHasAReasonableWeightInSomeSituation() {
        for (ZagredTable.Spec s : ZagredTable.specs()) {
            double best = 0;
            for (int foes = 1; foes <= 6; foes++)
                for (int shots = 0; shots <= 3; shots += 3)
                    for (int casters = 0; casters <= 1; casters++)
                        for (double distance = 4; distance <= 10; distance += 6) {
                            ZagredAi.Read r = new ZagredAi.Read(distance, foes, Math.min(foes, 5), foes > 2 ? 1 : 0, shots, casters, foes > 3 ? 0.3 : 0, true, 3, false, "", 0, 4, false, false, false);
                            best = Math.max(best, ZagredAi.weight(s, r));
                        }
            assertTrue(best > 0, s.id() + " can be chosen");
        }
    }

    @Test
    void theManNeverSwingsBeyondHisReachAndTheDevilLeaps() {
        double reach = ZagredMath.clawReach(false);
        ZagredAi.Read close = new ZagredAi.Read(reach, 1, 0, 0, 0, 0, 0, true, 2, false, "", 0, 4, false, false, false);
        ZagredAi.Read far = new ZagredAi.Read(reach + 2, 1, 0, 0, 0, 0, 0, true, 2, false, "", 0, 4, false, false, false);
        ZagredAi.Read devilFar = new ZagredAi.Read(8, 1, 0, 0, 0, 0, 0, true, 4, true, "", 0, 4, false, false, false);
        ZagredAi.Read devilBeyond = new ZagredAi.Read(ZagredMath.clawLeap() + 1, 1, 0, 0, 0, 0, 0, true, 4, true, "", 0, 4, false, false, false);
        assertTrue(w(ZagredTable.CLAW, close) > 0, "inside his reach he claws");
        assertEquals(0.0, w(ZagredTable.CLAW, far), 1e-12, "past it the man does not swing at the air");
        assertTrue(w(ZagredTable.CLAW, devilFar) > 0, "the devil leaps to where the lane is drawn");
        assertEquals(0.0, w(ZagredTable.CLAW, devilBeyond), 1e-12, "even he does not leap beyond the leap");
    }
}
