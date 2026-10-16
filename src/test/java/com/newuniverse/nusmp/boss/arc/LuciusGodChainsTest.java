package com.newuniverse.nusmp.boss.arc;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** 1.45.0: the god-tier chains: fairness, which act they belong to, and that the old mode never sees them. */
class LuciusGodChainsTest {
    private static final List<String> GOD = List.of(LuciusChains.STASIS_SNARE, LuciusChains.SOUL_RAY_CHAIN, LuciusChains.BLOOD_TIDE_CHAIN,
            LuciusChains.STARVATION_PULSE_CHAIN, LuciusChains.SEVERANCE_CHAIN, LuciusChains.RELIC_EMP_CHAIN, LuciusChains.CHECKMATE_COUNTER,
            LuciusChains.HIT_COUNTER);

    @Test
    void everyGodChainExistsAndIsFair() {
        for (String id : GOD) {
            LuciusChains.Chain c = LuciusChains.byId(id);
            assertNotNull(c, id);
            assertTrue(LuciusChains.valid(c), id + " must pass the fairness rule");
            assertTrue(LuciusChains.godOnly(id), id);
        }
    }

    @Test
    void theHarmfulGodStepsGiveAtLeastTwelveTicksOfWarning() {
        for (String id : GOD)
            for (LuciusChains.Step st : LuciusChains.byId(id).steps())
                if (st.hurts()) assertTrue(st.telegraph() >= LuciusChains.MIN_HURT_TELEGRAPH, id + " " + st.kind());
    }

    @Test
    void theOwnersWarningTimesHold() {
        assertEquals(24, LuciusChains.byId(LuciusChains.RELIC_EMP_CHAIN).steps().get(0).telegraph(), "the relic pulse is telegraphed 24 ticks");
        assertEquals(28, LuciusChains.byId(LuciusChains.SEVERANCE_CHAIN).steps().get(0).telegraph(), "the severance binding is telegraphed 28 ticks");
        LuciusChains.Chain counter = LuciusChains.byId(LuciusChains.CHECKMATE_COUNTER);
        assertEquals(List.of(LuciusChains.Kind.SPATIAL_BLINK, LuciusChains.Kind.COUNTER_STRIKE), counter.steps().stream().map(LuciusChains.Step::kind).toList());
        assertTrue(counter.steps().get(1).telegraph() >= 12, "the unblockable strike has a tell of at least 12 ticks");
    }

    @Test
    void theStagesCarryTheirOwnArsenal() {
        assertTrue(LuciusChains.byId(LuciusChains.STASIS_SNARE).usable(1) && LuciusChains.byId(LuciusChains.SOUL_RAY_CHAIN).usable(1), "stage 1: time and soul");
        assertFalse(LuciusChains.byId(LuciusChains.SOUL_RAY_CHAIN).usable(2));
        assertTrue(LuciusChains.byId(LuciusChains.BLOOD_TIDE_CHAIN).usable(2), "stage 2: blood");
        assertTrue(LuciusChains.byId(LuciusChains.RELIC_EMP_CHAIN).usable(3), "stage 3: space");
        assertFalse(LuciusChains.byId(LuciusChains.RELIC_EMP_CHAIN).usable(1) || LuciusChains.byId(LuciusChains.RELIC_EMP_CHAIN).usable(2));
    }

    @Test
    void actOneNeverMeleesAndActTwoNeverClonesStillHolds() {
        for (LuciusChains.Chain c : LuciusChains.all()) {
            if (c.usable(1)) for (LuciusChains.Step s : c.steps()) assertTrue(s.kind() != LuciusChains.Kind.SOUL_TOUCH && s.kind() != LuciusChains.Kind.COUNTER_STRIKE || c.weight() == 0, c.id());
            if (c.usable(2)) for (LuciusChains.Step s : c.steps()) assertTrue(s.kind() != LuciusChains.Kind.CLONE_WAVE, c.id());
        }
    }

    @Test
    void theOldModeNeverDrawsAGodChain() {
        Random r = new Random(3);
        Map<String, Long> ready = new HashMap<>();
        for (int phase = 1; phase <= 3; phase++)
            for (int i = 0; i < 500; i++) assertFalse(LuciusChains.godOnly(LuciusChains.pick(phase, 0, ready, r, 5).orElseThrow().id()));
    }

    @Test
    void theGodModeDrawsThemAndNeverTheAnswers() {
        Random r = new Random(11);
        Set<String> seen = new HashSet<>();
        for (int phase = 1; phase <= 3; phase++)
            for (int i = 0; i < 3000; i++) seen.add(LuciusChains.pick(phase, 0, new HashMap<>(), r, 5, true).orElseThrow().id());
        for (String id : List.of(LuciusChains.STASIS_SNARE, LuciusChains.SOUL_RAY_CHAIN, LuciusChains.BLOOD_TIDE_CHAIN, LuciusChains.STARVATION_PULSE_CHAIN,
                LuciusChains.SEVERANCE_CHAIN, LuciusChains.RELIC_EMP_CHAIN)) assertTrue(seen.contains(id), id + " is never drawn");
        assertFalse(seen.contains(LuciusChains.CHECKMATE_COUNTER), "weight 0: started by the glue only");
        assertFalse(seen.contains(LuciusChains.HIT_COUNTER));
    }

    @Test
    void theAnswerChainsRunFromBeginToDone() {
        LuciusChains.Runner run = new LuciusChains.Runner();
        assertTrue(run.start(LuciusChains.byId(LuciusChains.CHECKMATE_COUNTER), 0));
        int fires = 0, ticks = 0;
        while (run.active() && ticks < 500) {
            if (run.tick().type() == LuciusChains.EventType.FIRE) fires++;
            ticks++;
        }
        assertEquals(2, fires);
        assertTrue(run.start(LuciusChains.byId(LuciusChains.HIT_COUNTER), ticks), "no cooldown on the reply to a clamped hit");
    }

    @Test
    void theSavedNamesOfTheNewKindsAreStable() {
        assertEquals("RELIC_EMP", LuciusChains.Kind.RELIC_EMP.name());
        assertEquals("COUNTER_STRIKE", LuciusChains.Kind.COUNTER_STRIKE.name());
        assertEquals(LuciusChains.Kind.RETREAT.ordinal() + 1, LuciusChains.Kind.SOUL_RAY.ordinal(), "appended after the old ones");
    }
}
