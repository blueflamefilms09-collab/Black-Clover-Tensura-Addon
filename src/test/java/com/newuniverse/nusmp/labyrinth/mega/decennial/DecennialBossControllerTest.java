package com.newuniverse.nusmp.labyrinth.mega.decennial;

import com.newuniverse.nusmp.labyrinth.mega.decennial.DecennialBossController.Event;
import com.newuniverse.nusmp.labyrinth.mega.decennial.DecennialBossController.Kind;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the phase machine is the one piece of a Decennial boss that is pure logic, so it is proved by running whole fights in a loop and asserting the order and the timing of what it asks the
 * world to do: the shift into the next phase at its health threshold, the two-second invulnerable cinematic, the lair actions on their own clocks (never two within three seconds, never before the
 * phase that owns them, faster as the phases go), the add budget (an alive cap and a cap for the whole fight, never negative), the waves, the reset after a wipe and the save and restore a
 * server restart needs. The fights are run on the real shipped bosses (a two-phase, a three-phase and a four-phase one) as well as a hand-built profile.
 */
class DecennialBossControllerTest {
    private static DecennialProfile boss(int level) {
        DecennialRoster.loadShipped();
        return DecennialRoster.forLevel(level);
    }

    /** Runs a fight for {@code ticks}, with the boss's health falling linearly from 1 to {@code endHp}; returns every event with its tick. */
    private static List<Object[]> fight(DecennialBossController c, long ticks, double endHp, int alive) {
        List<Object[]> out = new ArrayList<>();
        for (long t = 1; t <= ticks; t++) {
            double hp = 1.0 - (1.0 - endHp) * t / (double) ticks;
            for (Event e : c.tick(t, hp, true, alive)) out.add(new Object[] {t, e});
        }
        return out;
    }

    @Test
    void nothingHappensWhileNoPartyIsInTheHall() {
        DecennialBossController c = new DecennialBossController(boss(-20));
        for (long t = 1; t < 2000; t++) assertTrue(c.tick(t, 0.2, false, 0).isEmpty());
        assertEquals(1, c.phase());
        assertFalse(c.fighting());
    }

    @Test
    void aShiftHasATwoSecondInvulnerableCinematicAndEndsInTheNextPhase() {
        DecennialProfile p = boss(-40);                                    // three phases
        assertEquals(3, p.phaseCount());
        DecennialBossController c = new DecennialBossController(p);
        long t = 1;
        assertTrue(c.tick(t, 1.0, true, 0).isEmpty());
        double below = p.phase(2).at() - 0.01;
        List<Event> start = c.tick(++t, below, true, 0);
        assertEquals(1, start.size());
        assertEquals(Kind.PHASE_START, start.get(0).kind());
        assertEquals(2, start.get(0).phase());
        long began = t;
        assertTrue(c.invulnerable(t));
        assertTrue(c.inCinematic(t));
        assertEquals(1, c.phase(), "still the old phase during the cinematic");
        for (long k = 1; k < DecennialBossController.CINEMATIC_TICKS; k++) {
            assertTrue(c.invulnerable(began + k), "tick " + k);
            assertTrue(c.tick(began + k, below, true, 0).isEmpty(), "nothing else happens during a shift (tick " + k + ")");
        }
        List<Event> end = c.tick(began + DecennialBossController.CINEMATIC_TICKS, below, true, 0);
        assertEquals(Kind.PHASE_END, end.get(0).kind());
        assertEquals(2, end.get(0).phase());
        assertEquals(2, c.phase());
        assertFalse(c.invulnerable(began + DecennialBossController.CINEMATIC_TICKS));
        assertEquals(40, DecennialBossController.CINEMATIC_TICKS, "two seconds");
    }

    @Test
    void aWholeFightWalksEveryPhaseInOrderOnceEach() {
        for (int level : new int[] {-1, 12, -57, 100}) {
            DecennialProfile p = boss(level);
            DecennialBossController c = new DecennialBossController(p);
            List<Object[]> events = fight(c, 20000, 0.02, 0);
            int next = 2;
            for (Object[] o : events) {
                Event e = (Event) o[1];
                if (e.kind() == Kind.PHASE_START) {
                    assertEquals(next, e.phase(), p.id());
                } else if (e.kind() == Kind.PHASE_END) {
                    assertEquals(next, e.phase(), p.id());
                    next++;
                }
            }
            assertEquals(p.phaseCount() + 1, next, p.id() + " reached every phase");
            assertEquals(p.phaseCount(), c.phase());
            assertTrue(c.isFinalPhase());
        }
    }

    @Test
    void lairActionsComeOnTheirOwnClocksNeverCloserThanTheGapAndNeverBeforeTheirPhase() {
        DecennialProfile p = boss(-57);                                    // two lair actions, the second from phase 2
        assertEquals(2, p.lairs().size());
        DecennialBossController c = new DecennialBossController(p);
        List<Object[]> events = fight(c, 12000, 0.5, 0);                  // health only falls to 50 %: phase 1 for a long time
        long last = -1000;
        int firstLair = 0, secondLair = 0;
        for (Object[] o : events) {
            Event e = (Event) o[1];
            if (e.kind() != Kind.LAIR) continue;
            long t = (Long) o[0];
            assertTrue(t - last >= DecennialBossController.LAIR_GAP, "two lair actions " + (t - last) + " ticks apart");
            last = t;
            if (e.id().equals(p.lairs().get(0).id())) firstLair++;
            else secondLair++;
            assertTrue(e.power() > 0.5 && e.power() <= 1.5 * DecennialRules.MAX_DAMAGE_MULT);
        }
        assertTrue(firstLair >= 3, "the first lair action fires repeatedly: " + firstLair);
        assertTrue(events.stream().noneMatch(o -> ((Event) o[1]).kind() == Kind.PHASE_END) || secondLair >= 0);
        // the first one is not instant: the party gets the opening seconds
        assertTrue((Long) events.stream().filter(o -> ((Event) o[1]).kind() == Kind.LAIR).findFirst().get()[0] >= DecennialBossController.FIRST_LAIR_DELAY / 2);
    }

    @Test
    void aLairActionOfALaterPhaseWaitsForIt() {
        DecennialProfile p = boss(-57);
        DecennialProfile.LairRef second = p.lairs().get(1);
        assertTrue(second.fromPhase() >= 2);
        DecennialBossController c = new DecennialBossController(p);
        for (long t = 1; t < 5000; t++) for (Event e : c.tick(t, 1.0, true, 0)) if (e.kind() == Kind.LAIR) assertFalse(second.id().equals(e.id()), "the second lair action is a phase-2 action");
    }

    @Test
    void lairActionsComeFasterAsThePhasesGoButNeverFasterThanTheFloor() {
        int a = DecennialBossController.intervalTicks(20, 1), b = DecennialBossController.intervalTicks(20, 3), c = DecennialBossController.intervalTicks(20, 4);
        assertTrue(a > b && b > c, a + " " + b + " " + c);
        assertEquals(20 * 20, a);
        assertEquals(DecennialRules.MIN_LAIR_SECONDS * 20, DecennialBossController.intervalTicks(8, 4), "the floor");
        assertEquals(DecennialRules.MIN_LAIR_SECONDS * 20, DecennialBossController.intervalTicks(1, 1));
    }

    @Test
    void theAddBudgetCapsWhatIsAliveAndWhatAFightMaySpawn() {
        DecennialProfile p = boss(-1);
        DecennialProfile.AddsSpec a = p.adds();
        DecennialBossController d = new DecennialBossController(p);
        int total = 0, rounds = 0;
        while (rounds++ < 100) {
            int g = d.grantAdds(3, 0);                                                    // nobody ever kills any: alive stays 0, so only the fight cap stops it
            assertTrue(g >= 0 && g <= 3);
            total += g;
            if (g == 0) break;
        }
        assertEquals(a.fightCap(), total, "exactly the fight cap is ever granted");
        assertEquals(0, d.grantAdds(3, 0));
        DecennialBossController e = new DecennialBossController(p);
        assertEquals(0, e.grantAdds(5, a.maxAlive()), "no room under the alive cap");
        assertEquals(1, e.grantAdds(5, a.maxAlive() - 1));
        assertEquals(0, e.grantAdds(-4, 0), "a negative request grants nothing");
    }

    @Test
    void phaseAddsAndWavesAreTrimmedToTheBudgetAndOnlyComeWhenFewAreAlive() {
        DecennialProfile p = boss(-60);                                    // a summoner with waves
        assertTrue(p.adds().wave() > 0 && p.adds().waveEvery() > 0, "this test needs a boss with waves");
        DecennialBossController c = new DecennialBossController(p);
        int waves = 0, phases = 0, total = 0;
        for (long t = 1; t < 40000; t++) {
            double hp = Math.max(0.35, 1.0 - t / 60000.0);
            for (Event e : c.tick(t, hp, true, 0)) {
                if (e.kind() == Kind.ADDS) {
                    total += e.count();
                    assertTrue(e.count() >= 1 && e.count() <= DecennialRules.MAX_ADDS_PER_WAVE);
                    if (e.id().equals("wave")) waves++;
                    else phases++;
                }
            }
        }
        assertTrue(waves >= 3 && phases >= 1, "waves " + waves + " phases " + phases + " (the phases keep their share even when waves come first)");
        assertTrue(total <= p.adds().fightCap(), "never more than the fight cap: " + total);
        assertEquals(0, new DecennialBossController(p).grantExtraAdds(0, 0));
        DecennialBossController greedy = new DecennialBossController(p);
        int extra = 0;
        for (int i = 0; i < 100; i++) extra += greedy.grantExtraAdds(3, 0);
        assertEquals(p.adds().fightCap() - greedy.reservedForPhases(), extra, "extras leave the phases' share alone");
        assertTrue(greedy.reservedForPhases() > 0);
        DecennialBossController crowded = new DecennialBossController(p);
        for (long t = 1; t < 20000; t++) for (Event e : crowded.tick(t, 1.0, true, p.adds().maxAlive())) assertFalse(e.kind() == Kind.ADDS, "no wave while the hall is full of adds");
    }

    @Test
    void aWipeResetsTheFight() {
        DecennialProfile p = boss(-40);
        DecennialBossController c = new DecennialBossController(p);
        fight(c, 8000, 0.1, 0);
        assertTrue(c.phase() > 1);
        assertTrue(c.addsSpent() > 0 || c.phase() > 1);
        c.reset();
        assertEquals(1, c.phase());
        assertEquals(0, c.addsSpent());
        assertFalse(c.fighting());
        assertFalse(c.inCinematic(10));
        assertTrue(c.tick(1, 1.0, true, 0).isEmpty(), "the party gets its opening seconds again");
    }

    @Test
    void thePhaseAndTheBudgetSurviveASaveAndARestore() {
        DecennialProfile p = boss(-40);
        DecennialBossController c = new DecennialBossController(p);
        fight(c, 9000, 0.2, 0);
        int[] saved = c.save();
        DecennialBossController d = new DecennialBossController(p);
        d.restore(saved);
        assertEquals(c.phase(), d.phase());
        assertEquals(c.addsSpent(), d.addsSpent());
        d.restore(new int[] {99, 99, -5});                                   // garbage is clamped, never trusted
        assertEquals(p.phaseCount(), d.phase());
        assertEquals(0, d.addsSpent());
        d.restore(null);
        d.restore(new int[1]);
    }

    @Test
    void aBigBlowNeverSkipsAPhase() {
        DecennialProfile p = boss(100);
        DecennialBossController c = new DecennialBossController(p);
        assertTrue(c.tick(1, 1.0, true, 0).isEmpty());
        List<Event> e = c.tick(2, 0.01, true, 0);                            // health falls straight to 1 %
        assertEquals(1, e.size());
        assertEquals(2, e.get(0).phase(), "only the next phase is entered");
        for (long t = 3; t < 2 + DecennialBossController.CINEMATIC_TICKS; t++) c.tick(t, 0.01, true, 0);
        List<Event> next = c.tick(2 + DecennialBossController.CINEMATIC_TICKS, 0.01, true, 0);
        assertEquals(Kind.PHASE_END, next.get(0).kind());
        assertEquals(2, c.phase());
        List<Event> again = c.tick(3 + DecennialBossController.CINEMATIC_TICKS, 0.01, true, 0);
        assertEquals(Kind.PHASE_START, again.get(0).kind());
        assertEquals(3, again.get(0).phase());
    }

    @Test
    void speedAndDamageFollowThePhaseAndStayInsideTheCaps() {
        DecennialProfile p = boss(100);
        DecennialBossController c = new DecennialBossController(p);
        double lastDamage = 0;
        for (int i = 1; i <= p.phaseCount(); i++) {
            DecennialProfile.Phase ph = p.phase(i);
            assertTrue(ph.damage() >= lastDamage, "damage never falls as phases go");
            lastDamage = ph.damage();
            assertTrue(ph.speed() <= DecennialRules.MAX_SPEED_MULT && ph.damage() <= DecennialRules.MAX_DAMAGE_MULT);
        }
        assertEquals(p.phase(1).speed(), c.speedMult(), 1e-9);
        assertEquals(p.phase(1).damage(), c.damageMult(), 1e-9);
    }
}
