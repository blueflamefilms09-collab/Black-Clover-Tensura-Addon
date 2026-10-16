package com.newuniverse.nusmp.avatar;

import com.newuniverse.nusmp.avatar.outcome.AvatarUniqueSkill;
import com.newuniverse.nusmp.avatar.outcome.Outcome;
import com.newuniverse.nusmp.avatar.outcome.OutcomeMath;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Why: the outcome engine's weights, the skip rules for a named or Unique Avatar, and the generated Unique Skill table are pure rules the owner specified exactly. */
class OutcomeMathTest {
    private static final double EPS = 1e-9;

    @Test
    void zeroLuckIsTheSpecWeights() {
        OutcomeMath.Weights w = OutcomeMath.weights(0);
        assertEquals(0.30, w.betrayal(), EPS);
        assertEquals(0.25, w.soul(), EPS);
        assertEquals(0.45, w.ordinary(), EPS);
    }

    @Test
    void luckMovesChanceFromBetrayalToSoulOnePercentPerPoint() {
        OutcomeMath.Weights w = OutcomeMath.weights(10);
        assertEquals(0.20, w.betrayal(), EPS);
        assertEquals(0.35, w.soul(), EPS);
        assertEquals(0.45, w.ordinary(), EPS);
    }

    @Test
    void theShiftStopsAtThirtyPercentSoBetrayalReachesZero() {
        for (double luck : new double[]{30, 60, 1000}) {
            OutcomeMath.Weights w = OutcomeMath.weights(luck);
            assertEquals(0.0, w.betrayal(), EPS);
            assertEquals(0.55, w.soul(), EPS);
        }
    }

    @Test
    void negativeLuckNeverMakesBetrayalWorse() {
        assertEquals(0.30, OutcomeMath.weights(-25).betrayal(), EPS);
    }

    @Test
    void weightsAlwaysSumToOne() {
        for (double luck = -30; luck <= 80; luck += 2.5) {
            OutcomeMath.Weights w = OutcomeMath.weights(luck);
            assertEquals(1.0, w.betrayal() + w.soul() + w.ordinary(), 1e-9, "luck " + luck);
            assertTrue(w.betrayal() >= 0 && w.soul() >= 0 && w.ordinary() >= 0);
        }
    }

    @Test
    void aTunedConfigStillSumsToOne() {
        OutcomeMath.Weights w = OutcomeMath.weights(5, 0.6, 0.6, 0.05, 0.5);
        assertEquals(1.0, w.betrayal() + w.soul() + w.ordinary(), 1e-9);
        assertTrue(w.ordinary() >= 0);
    }

    @Test
    void pickFollowsTheBands() {
        OutcomeMath.Weights w = OutcomeMath.weights(0);
        assertEquals(Outcome.BETRAYAL, OutcomeMath.pick(w, 0.0));
        assertEquals(Outcome.BETRAYAL, OutcomeMath.pick(w, 0.299));
        assertEquals(Outcome.SOUL_AWAKENING, OutcomeMath.pick(w, 0.30));
        assertEquals(Outcome.SOUL_AWAKENING, OutcomeMath.pick(w, 0.549));
        assertEquals(Outcome.ORDINARY, OutcomeMath.pick(w, 0.55));
        assertEquals(Outcome.ORDINARY, OutcomeMath.pick(w, 0.999));
    }

    @Test
    void simulationMatchesTheWeights() {
        Random r = new Random(99);
        OutcomeMath.Weights w = OutcomeMath.weights(10);
        int n = 100000, b = 0, s = 0;
        for (int i = 0; i < n; i++) {
            Outcome o = OutcomeMath.pick(w, r.nextDouble());
            if (o == Outcome.BETRAYAL) b++;
            if (o == Outcome.SOUL_AWAKENING) s++;
        }
        assertEquals(0.20, b / (double) n, 0.01);
        assertEquals(0.35, s / (double) n, 0.01);
    }

    @Test
    void aNamedAvatarNeverBetraysWhateverTheRoll() {
        for (double r = 0; r < 1; r += 0.01) assertEquals(Outcome.SKIPPED_NAMED, OutcomeMath.decide(true, false, 0, r));
    }

    @Test
    void aUniqueSkillSkipsTheRollAndNamingWinsWhenBothApply() {
        assertEquals(Outcome.SKIPPED_UNIQUE, OutcomeMath.decide(false, true, 0, 0.0));
        assertEquals(Outcome.SKIPPED_NAMED, OutcomeMath.decide(true, true, 0, 0.0));
        assertEquals(Outcome.BETRAYAL, OutcomeMath.decide(false, false, 0, 0.0));
    }

    @Test
    void outcomeNamesRoundTripAndUnknownIsPending() {
        for (Outcome o : Outcome.values()) assertEquals(o, Outcome.byName(o.name()));
        assertEquals(Outcome.PENDING, Outcome.byName("nonsense"));
        assertFalse(Outcome.PENDING.decided());
        assertTrue(Outcome.BETRAYAL.decided());
    }

    @Test
    void generatedUniqueSkillsAreValidAndVaried() {
        Set<String> ids = new HashSet<>();
        Random r = new Random(5);
        for (int i = 0; i < 400; i++) {
            AvatarUniqueSkill.Perk p = AvatarUniqueSkill.generate(r);
            ids.add(p.id());
            assertTrue(p.passiveAmount() >= 0.15 && p.passiveAmount() <= 0.30);
            assertTrue(p.signaturePower() >= 6 && p.signaturePower() <= 12);
            assertTrue(p.cooldownTicks() >= 160 && p.cooldownTicks() <= 320);
            assertNotNull(p.name());
            assertNotNull(AvatarUniqueSkill.restore(p.id(), p.passiveAmount(), p.signaturePower(), p.cooldownTicks()));
        }
        assertEquals(AvatarUniqueSkill.tableSize(), ids.size());
        assertNull(AvatarUniqueSkill.restore("removed_in_a_later_version", 0.2, 8, 200));
    }
}
