package com.newuniverse.nusmp.boss;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The global buff engine's numbers: isolation (non-boss gets nothing), idempotence, suspension, the multiplier. */
class BossBuffTableTest {
    @Test
    void aNonBossGetsNoBuffsAtAll() {
        for (String id : new String[]{"zombie", "tensura:charybdis", "god_of_order", "", null}) {
            BossBuffTable.Plan p = BossBuffTable.plan(id, 1.0, false);
            assertTrue(p.isEmpty(), String.valueOf(id));
            assertTrue(BossBuffTable.missing(p, BossBuffTable.Applied.none()).isEmpty());
        }
    }

    @Test
    void applyingAPlanTwiceChangesNothing() {
        for (BossBuffTable.Profile prof : BossBuffTable.all()) {
            BossBuffTable.Plan plan = BossBuffTable.plan(prof.bossId(), 1.0, false);
            assertFalse(plan.isEmpty(), prof.bossId());
            BossBuffTable.Applied once = BossBuffTable.apply(BossBuffTable.missing(plan, BossBuffTable.Applied.none()), BossBuffTable.Applied.none());
            assertTrue(BossBuffTable.missing(plan, once).isEmpty(), prof.bossId() + ": nothing left to do after one application");
            BossBuffTable.Applied twice = BossBuffTable.apply(BossBuffTable.missing(plan, once), once);
            assertEquals(once, twice, prof.bossId() + ": the second application changes nothing");
            assertTrue(BossBuffTable.surplus(plan, once).isEmpty());
        }
    }

    @Test
    void aWrongLevelIsFixedAndAMissingSkillIsAdded() {
        BossBuffTable.Plan plan = BossBuffTable.plan(BossRegistry.LUCIUS, 1.0, false);
        BossBuffTable.Applied weak = new BossBuffTable.Applied(Map.of(BossBuffTable.MAGIC_BARRIER, 0), Set.of(BossBuffTable.STARVED));
        BossBuffTable.Plan todo = BossBuffTable.missing(plan, weak);
        assertEquals(plan.amplifiers().get(BossBuffTable.MAGIC_BARRIER), todo.amplifiers().get(BossBuffTable.MAGIC_BARRIER), "the level is raised");
        assertFalse(todo.skills().contains(BossBuffTable.STARVED), "a skill the boss has is not taught again");
        assertTrue(todo.skills().contains(BossBuffTable.MATHEMATICIAN));
    }

    @Test
    void exposureSuspendsTheBarriersButKeepsTheSkills() {
        for (BossBuffTable.Profile prof : BossBuffTable.all()) {
            BossBuffTable.Plan open = BossBuffTable.plan(prof.bossId(), 1.0, true);
            assertTrue(open.amplifiers().isEmpty(), prof.bossId() + ": no barrier, no regeneration while exposed");
            assertEquals(Set.copyOf(prof.skills()), open.skills());
            BossBuffTable.Applied full = BossBuffTable.apply(BossBuffTable.plan(prof.bossId(), 1.0, false), BossBuffTable.Applied.none());
            assertEquals(full.amplifiers().keySet().size(), BossBuffTable.surplus(open, full).size(), "every effect comes off");
        }
    }

    @Test
    void theMultiplierScalesAndZeroSwitchesTheEngineOff() {
        BossBuffTable.Plan one = BossBuffTable.plan(BossRegistry.LUCIUS, 1.0, false);
        BossBuffTable.Plan two = BossBuffTable.plan(BossRegistry.LUCIUS, 2.0, false);
        assertEquals(one.amplifiers().get(BossBuffTable.MAGIC_BARRIER) * 2 + 1, two.amplifiers().get(BossBuffTable.MAGIC_BARRIER), "level 2 -> level 4");
        assertTrue(BossBuffTable.plan(BossRegistry.LUCIUS, 0.0, false).isEmpty());
        assertTrue(BossBuffTable.plan(BossRegistry.LUCIUS, -3.0, false).isEmpty());
        assertTrue(BossBuffTable.plan(BossRegistry.LUCIUS, Double.NaN, false).isEmpty());
        for (double m : new double[]{0.25, 0.5, 1, 2, 3, 99})
            for (Integer amp : BossBuffTable.plan(BossRegistry.LUCIUS, m, false).amplifiers().values())
                assertTrue(amp >= 0 && amp < BossBuffTable.MAX_LEVEL, "amplifier bound at multiplier " + m);
        assertEquals(1, BossBuffTable.scaledLevel(1, 1.0));
        assertEquals(0, BossBuffTable.scaledLevel(1, 0.5), "a thinned buff drops out");
        assertEquals(BossBuffTable.MAX_LEVEL, BossBuffTable.scaledLevel(4, 3.0));
    }

    @Test
    void tiersGrowWithTheBossAndLuciusHasEverything() {
        BossBuffTable.Profile lucius = BossBuffTable.profile(BossRegistry.LUCIUS);
        assertEquals(4, lucius.tier());
        assertEquals(Set.copyOf(BossBuffTable.EFFECT_IDS), lucius.levels().keySet());
        assertEquals(Set.of(BossBuffTable.STARVED, BossBuffTable.MATHEMATICIAN, BossBuffTable.MORTAL_FEAR), Set.copyOf(lucius.skills()));
        for (BossBuffTable.Profile p : BossBuffTable.all()) {
            assertTrue(p.tier() <= lucius.tier());
            assertTrue(p.auraRadius() <= lucius.auraRadius() && p.auraJamSeconds() <= 3, "the aura only ever jams for a few seconds");
            assertTrue(p.auraChance() > 0 && p.auraChance() < 1);
            assertTrue(p.soulPoints() <= 3 && p.pierceTicks() <= 40, "the engravings are small");
            for (int level : p.levels().values()) assertTrue(level >= 1 && level <= BossBuffTable.MAX_LEVEL);
        }
        assertFalse(BossBuffTable.profile(BossRegistry.GOLIATH).levels().containsKey(BossBuffTable.INSTANT_REGENERATION), "floor bosses do not regenerate on top of the framework");
    }

    @Test
    void instantRegenerationComesOffInTheSecondHalfAndOnlyThatEffect() {
        for (String id : new String[]{BossRegistry.LUCIUS, BossRegistry.DEVIL_AVATAR}) {
            BossBuffTable.Plan full = BossBuffTable.plan(id, 1.0, false, false);
            BossBuffTable.Plan low = BossBuffTable.plan(id, 1.0, false, true);
            assertTrue(full.amplifiers().containsKey(BossBuffTable.INSTANT_REGENERATION), id);
            assertFalse(low.amplifiers().containsKey(BossBuffTable.INSTANT_REGENERATION), id + ": Tensura's regeneration can stop a death");
            assertEquals(full.amplifiers().size() - 1, low.amplifiers().size(), "the barriers and the coat stay");
            assertEquals(full.skills(), low.skills());
        }
        assertEquals(BossBuffTable.plan(BossRegistry.DANTE, 1.0, false), BossBuffTable.plan(BossRegistry.DANTE, 1.0, false, true));
        assertTrue(BossBuffTable.plan("zombie", 1.0, false, true).isEmpty());
    }

    @Test
    void lowHealthHasAHysteresis() {
        assertFalse(BossBuffTable.lowHealth(0.9, false));
        assertTrue(BossBuffTable.lowHealth(0.5, false));
        assertTrue(BossBuffTable.lowHealth(0.55, true), "once off it stays off until 60%");
        assertFalse(BossBuffTable.lowHealth(0.6, true));
        assertTrue(BossBuffTable.lowHealth(Double.NaN, false));
    }
}
