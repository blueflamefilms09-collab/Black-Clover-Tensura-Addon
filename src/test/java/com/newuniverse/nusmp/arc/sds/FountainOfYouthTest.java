package com.newuniverse.nusmp.arc.sds;

import com.newuniverse.nusmp.entity.zogratis.PowerCurve;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Ban's Fountain of Youth: up to three lives, a window between them, a price for each rise, a counter, and the Gates at the last rise. */
class FountainOfYouthTest {
    @Test
    void threeLivesTwoFallsAndTheThirdIsReal() {
        FountainOfYouth f = new FountainOfYouth(3);
        assertEquals(1, f.life());
        assertTrue(f.canRevive() && !f.finalLife());
        assertTrue(f.fall(100, false), "life 1 ends in a fall");
        assertTrue(f.falling());
        f.rise();
        assertEquals(2, f.life());
        assertEquals(0.60, f.reservoir(), 1e-9);
        assertTrue(f.fall(500, false), "life 2 ends in a fall");
        f.rise();
        assertEquals(3, f.life());
        assertEquals(0.45, f.reservoir(), 1e-9);
        assertTrue(f.finalLife() && !f.canRevive());
        assertFalse(f.fall(900, false), "life 3 ends in a real death");
        assertFalse(f.falling());
    }

    @Test
    void aKillCommandAndTheVoidAlwaysKill() {
        FountainOfYouth f = new FountainOfYouth(3);
        assertFalse(f.fall(0, true));
        assertFalse(f.falling());
        assertEquals(1, f.life());
    }

    @Test
    void onlyOneFallAtATimeAndTheWindowHasALength() {
        FountainOfYouth f = new FountainOfYouth(3);
        assertTrue(f.fall(100, false));
        assertFalse(f.fall(101, false), "already down");
        assertEquals(FountainOfYouth.windowTicks(1), f.windowLeft(100));
        assertEquals(200, f.windowLeft(100));
        assertEquals(150, f.windowLeft(150));
        assertFalse(f.windowOver(299));
        assertTrue(f.windowOver(300));
        assertEquals(0, f.windowLeft(400));
        f.rise();
        assertTrue(f.fall(1000, false));
        assertEquals(160, f.windowLeft(1000), "the second window is shorter");
        assertEquals(0, new FountainOfYouth(3).windowLeft(5), "not down, no window");
    }

    @Test
    void theReservoirsShrinkSoTheFightHasAnEnd() {
        double total = 1.0;
        FountainOfYouth f = new FountainOfYouth(3);
        for (int i = 0; i < 2; i++) {
            assertTrue(f.fall(i * 1000L, false));
            total += f.nextReservoir();
            f.rise();
        }
        assertEquals(2.05, total, 1e-9, "a bar and a bit more than a half and a half again");
        assertTrue(total < 2.5, "no more than about two health bars in all");
    }

    @Test
    void cuttingTheFountainShrinksTheNextLifeToAFifth() {
        FountainOfYouth f = new FountainOfYouth(3);
        assertTrue(f.fall(0, false));
        assertFalse(f.cut());
        assertTrue(f.cutNow());
        assertFalse(f.cutNow(), "only the first cut counts");
        assertEquals(0.60 * FountainOfYouth.CUT_FACTOR, f.nextReservoir(), 1e-9);
        f.rise();
        assertEquals(0.24, f.reservoir(), 1e-9);
        assertFalse(f.cut(), "the next window starts uncut");
        assertTrue(f.fall(500, false));
        assertEquals(0.45, f.nextReservoir(), 1e-9);
        assertFalse(new FountainOfYouth(3).cutNow(), "nothing to cut while he stands");
    }

    @Test
    void whatCutsTheFountain() {
        assertTrue(FountainOfYouth.cutsFountain("player_attack", true), "an anti-magic blow, whatever the damage type");
        assertTrue(FountainOfYouth.cutsFountain("holy_damage", false));
        assertTrue(FountainOfYouth.cutsFountain("soul_scatter", false));
        assertTrue(FountainOfYouth.cutsFountain("severance", false));
        assertTrue(FountainOfYouth.cutsFountain("spiritual_damage", false));
        assertTrue(FountainOfYouth.cutsFountain("Holy_Damage", false), "case does not matter");
        assertFalse(FountainOfYouth.cutsFountain("player_attack", false));
        assertFalse(FountainOfYouth.cutsFountain("indirect_magic", false));
        assertFalse(FountainOfYouth.cutsFountain("fire_elemental", false));
        assertFalse(FountainOfYouth.cutsFountain(null, false));
    }

    @Test
    void fewerLivesAreConfigurable() {
        FountainOfYouth one = new FountainOfYouth(1);
        assertTrue(one.finalLife());
        assertFalse(one.fall(0, false), "with one life he dies like any boss");
        FountainOfYouth two = new FountainOfYouth(2);
        assertTrue(two.fall(0, false));
        two.rise();
        assertTrue(two.finalLife());
        assertFalse(two.fall(10, false));
        assertEquals(3, new FountainOfYouth(99).lives());
        assertEquals(1, new FountainOfYouth(-5).lives());
    }

    @Test
    void thePowerLadderRunsAcrossTheLivesAndTheGatesOpenAtTheLastRise() {
        FountainOfYouth f = new FountainOfYouth(3);
        assertEquals(1.0, f.powerFraction(1.0, false), 1e-9);
        double lastFraction = 2;
        int power = PowerCurve.OPENING;
        boolean gatesBeforeLastRise = false;
        // life 1 then life 2: the fraction only falls, the power only climbs, and no Gates yet
        for (int life = 1; life <= 2; life++) {
            for (double h = 1.0; h >= 0.0; h -= 0.01) {
                double fr = f.powerFraction(h, false);
                assertTrue(fr <= lastFraction + 1e-9, "the virtual bar only falls: life " + life + " h=" + h);
                lastFraction = fr;
                power = PowerCurve.next((float) fr, power);
                if (PowerCurve.gatesOpen(power)) gatesBeforeLastRise = true;
            }
            assertTrue(f.fall(0, false));
            double down = f.powerFraction(0.0, false);
            assertEquals(f.bottom(life), down, 1e-9, "while he lies down the life counts as empty");
            lastFraction = down;
            f.rise();
            double refilling = f.powerFraction(0.01, true);
            assertEquals(f.top(life + 1), refilling, 1e-9, "while the new life refills it counts as full: no jump");
            assertTrue(Math.abs(refilling - down) < 0.04, "and it is where he lay down (give or take the gap before the Gates)");
        }
        assertFalse(gatesBeforeLastRise, "the Gates stay shut for the first two lives");
        assertEquals(80, power, "life 2 ends at 80%");
        int atRise = PowerCurve.next((float) f.powerFraction(1.0, true), power);
        assertEquals(PowerCurve.FULL, atRise, "the Gates open the moment he rises for the last time");
        assertTrue(f.powerFraction(0.0, false) <= 1e-9);
    }

    @Test
    void theBandsTileTheBarWithoutOverlap() {
        FountainOfYouth f = new FountainOfYouth(3);
        assertEquals(1.0, f.top(1), 1e-9);
        assertEquals(f.bottom(1), f.top(2), 1e-9, "life 2 starts where life 1 ends");
        assertTrue(f.bottom(2) > 0.25, "life 2 ends above the Gates line");
        assertTrue(f.top(3) < 0.25, "life 3 begins under it");
        assertEquals(0.0, f.bottom(3), 1e-9);
        assertTrue(f.top(1) > f.bottom(1) && f.top(2) > f.bottom(2) && f.top(3) > f.bottom(3));
        FountainOfYouth two = new FountainOfYouth(2);
        assertEquals(1.0, two.top(1), 1e-9);
        assertEquals(0.27, two.bottom(1), 1e-9);
        assertEquals(0.24, two.top(2), 1e-9);
        FountainOfYouth one = new FountainOfYouth(1);
        assertEquals(1.0, one.top(1), 1e-9);
        assertEquals(0.0, one.bottom(1), 1e-9);
        assertEquals(0.5, one.powerFraction(0.5, false), 1e-9, "with one life the bar is the ordinary one");
    }

    @Test
    void theStateSurvivesASaveAndALoad() {
        FountainOfYouth f = new FountainOfYouth(3);
        f.fall(1000, false);
        f.cutNow();
        FountainOfYouth.Saved s = f.save(1100);
        assertEquals(1, s.life());
        assertTrue(s.falling() && s.cut());
        assertEquals(100, s.windowLeft());
        FountainOfYouth g = new FountainOfYouth(3);
        g.load(s, 5000);
        assertTrue(g.falling() && g.cut());
        assertEquals(100, g.windowLeft(5000));
        assertTrue(g.windowOver(5100));
        g.rise();
        assertEquals(2, g.life());
        assertEquals(0.24, g.reservoir(), 1e-9, "the cut was remembered");

        FountainOfYouth h = new FountainOfYouth(3);
        h.fall(0, false);
        h.rise();
        FountainOfYouth.Saved up = h.save(10);
        assertEquals(2, up.life());
        assertEquals(0.60, up.reservoir(), 1e-9);
        FountainOfYouth k = new FountainOfYouth(3);
        k.load(up, 10);
        assertEquals(2, k.life());
        assertEquals(0.60, k.reservoir(), 1e-9);
        assertFalse(k.falling());
    }

    @Test
    void loadToleratesNonsense() {
        FountainOfYouth f = new FountainOfYouth(3);
        f.load(null, 0);
        assertEquals(1, f.life());
        f.load(new FountainOfYouth.Saved(99, 7.0, true, true, -50), 0);
        assertEquals(3, f.life(), "a life outside 1..3 is clamped");
        assertFalse(f.falling(), "and the last life cannot be lying down");
        assertEquals(1.0, f.reservoir(), 1e-9);
        f.load(new FountainOfYouth.Saved(-3, -1.0, false, false, 0), 0);
        assertEquals(1, f.life());
        assertEquals(0.05, f.reservoir(), 1e-9);
        f.reset();
        assertEquals(1, f.life());
        assertEquals(1.0, f.reservoir(), 1e-9);
    }
}
