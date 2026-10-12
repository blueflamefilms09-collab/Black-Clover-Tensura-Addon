package com.newuniverse.nusmp.entity.zogratis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The Curse of Decay: it grows while the victim stays in the sigil and breaks when they leave it, fight it with anti-magic or are cleansed. */
class CurseBrandTest {
    private static int stepsUntilEnd(CurseBrand b, boolean inside, boolean anti) {
        int n = 0;
        while (!b.ended() && n < 10000) {
            b.step(inside, anti);
            n++;
        }
        return n;
    }

    @Test
    void standingInTheSigilItRunsItsCourseAndBitesHarder() {
        CurseBrand b = new CurseBrand(200, 1);
        double prev = 0;
        int steps = 0;
        while (!b.ended()) {
            CurseBrand.Result r = b.step(true, false);
            steps++;
            if (!r.ended()) {
                assertTrue(r.multiplier() >= prev - 1e-12, "it only grows while inside");
                assertTrue(r.multiplier() <= CurseBrand.maxMultiplier(1) + 1e-12);
                prev = r.multiplier();
            } else {
                assertEquals(CurseBrand.End.EXPIRED, r.reason());
                assertEquals(0, r.multiplier(), 1e-12);
            }
        }
        assertEquals(200 / CurseBrand.STEP, steps, "ten seconds");
        assertEquals(CurseBrand.maxMultiplier(1), prev, 1e-9, "it reached its peak");
        assertEquals(2.0, CurseBrand.maxMultiplier(1), 1e-12);
        assertEquals(2.4, CurseBrand.maxMultiplier(3), 1e-12);
    }

    @Test
    void theMultiplierGrowsAFifthPerSecondInside() {
        CurseBrand b = new CurseBrand(400, 3);
        for (int i = 0; i < 4; i++) b.step(true, false);        // one second
        assertEquals(1.2, b.multiplier(), 1e-9);
        for (int i = 0; i < 4; i++) b.step(true, false);
        assertEquals(1.4, b.multiplier(), 1e-9);
        for (int i = 0; i < 40; i++) b.step(true, false);
        assertEquals(2.4, b.multiplier(), 1e-9, "level 3 peaks at 2.4");
    }

    @Test
    void leavingTheSigilBreaksItWithinTwoSeconds() {
        for (int level = 1; level <= 3; level++) {
            for (int ticks : new int[]{200, 240, 280}) {
                for (int inside : new int[]{0, 1, 12}) {                  // leaving at once, after a moment, after three seconds
                    CurseBrand b = new CurseBrand(ticks, level);
                    for (int i = 0; i < inside; i++) b.step(true, false);
                    int steps = stepsUntilEnd(b, false, false);
                    assertTrue(steps * CurseBrand.STEP <= 2 * 20, "free in two seconds: " + steps * CurseBrand.STEP + " ticks");
                    assertEquals(CurseBrand.End.LEFT_SIGIL, b.reason());
                }
            }
        }
    }

    @Test
    void leavingResetsTheGrowth() {
        CurseBrand b = new CurseBrand(300, 2);
        for (int i = 0; i < 12; i++) b.step(true, false);
        assertTrue(b.multiplier() > 1.5);
        b.step(false, false);
        assertEquals(1.0, b.multiplier(), 1e-9);
        assertEquals(0, b.insideTicks());
        b.step(true, false);
        assertEquals(1.0 + CurseBrand.GROWTH_PER_SECOND * CurseBrand.STEP / 20.0, b.multiplier(), 1e-9, "growth starts again from nothing");
    }

    @Test
    void antiMagicBurnsItFasterAndHalvesTheBite() {
        CurseBrand plain = new CurseBrand(200, 1), anti = new CurseBrand(200, 1);
        int a = stepsUntilEnd(plain, true, false), b = stepsUntilEnd(anti, true, true);
        assertTrue(b * CurseBrand.ANTI_MAGIC_DRAIN <= a + CurseBrand.ANTI_MAGIC_DRAIN, "about a third as long: " + b + " vs " + a);
        assertEquals(CurseBrand.End.ANTI_MAGIC, anti.reason());
        CurseBrand c = new CurseBrand(400, 1), d = new CurseBrand(400, 1);
        for (int i = 0; i < 4; i++) { c.step(true, false); d.step(true, false); }
        CurseBrand.Result rc = c.step(true, false), rd = d.step(true, true);
        assertEquals(rc.multiplier() * 0.5, rd.multiplier(), 1e-9);
        CurseBrand both = new CurseBrand(200, 1);
        assertTrue(stepsUntilEnd(both, false, true) <= 3, "out of the sigil with anti-magic it is gone almost at once");
    }

    @Test
    void aCleanseEndsItAtOnce() {
        CurseBrand b = new CurseBrand(200, 2);
        b.step(true, false);
        b.cleanse();
        assertTrue(b.ended());
        assertEquals(CurseBrand.End.CLEANSED, b.reason());
        CurseBrand.Result r = b.step(true, false);
        assertTrue(r.ended());
        assertEquals(0, r.multiplier(), 1e-12);
        assertEquals(CurseBrand.End.CLEANSED, r.reason(), "the reason stays");
        CurseBrand done = new CurseBrand(5, 1);
        done.step(true, false);
        assertEquals(CurseBrand.End.EXPIRED, done.reason());
        done.cleanse();
        assertEquals(CurseBrand.End.EXPIRED, done.reason(), "an ended brand keeps its reason");
    }

    @Test
    void theDamageOverALifeIsLethalIfYouStayAndSmallIfYouLeave() {
        // damage ticks once a second (every fourth step), as a multiple of the base bite
        CurseBrand stay = new CurseBrand(VanicaMath.curseTicks(20), 1);
        double total = 0;
        int step = 0;
        while (!stay.ended()) {
            CurseBrand.Result r = stay.step(true, false);
            if (++step % 4 == 0 && !r.ended()) total += r.multiplier();
        }
        assertTrue(total >= 14 && total <= 22, "standing still takes about 16 bites' worth over ten seconds: " + total);
        CurseBrand leave = new CurseBrand(VanicaMath.curseTicks(20), 1);
        double small = 0;
        step = 0;
        while (!leave.ended()) {
            boolean inside = step < 4;                              // one second in, then out
            CurseBrand.Result r = leave.step(inside, false);
            if (++step % 4 == 0 && !r.ended()) small += r.multiplier();
        }
        assertTrue(small <= 2.5, "walking out costs two bites at most: " + small);
    }

    @Test
    void levelsAndLivesAreClamped() {
        assertEquals(1, new CurseBrand(100, -3).level());
        assertEquals(3, new CurseBrand(100, 9).level());
        assertEquals(CurseBrand.STEP, new CurseBrand(0, 1).life());
        assertEquals(CurseBrand.maxMultiplier(1), CurseBrand.maxMultiplier(0), 1e-12);
        CurseBrand b = new CurseBrand(0, 1);
        assertTrue(b.step(true, false).ended(), "the shortest brand is one step");
    }
}
