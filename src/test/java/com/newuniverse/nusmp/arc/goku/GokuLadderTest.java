package com.newuniverse.nusmp.arc.goku;

import com.newuniverse.nusmp.saiyan.SaiyanForms;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The Goku boss's form schedule: canon order, no skipped step, never down, forms read from the owner's chart, monotone strength, and the skins exist. Pure: no game type is started. */
class GokuLadderTest {
    private static final String[] CANON = {"base", "super_saiyan", "super_saiyan_2", "super_saiyan_3", "super_saiyan_god", "super_saiyan_blue", "super_saiyan_blue_kaioken",
            "ultra_instinct_sign", "perfected_ultra_instinct", "true_ultra_instinct"};

    @Test
    void theStepsAreTheCanonOrderAndEveryFormIsInTheChart() {
        assertEquals(CANON.length, GokuLadder.size());
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < CANON.length; i++) {
            GokuLadder.Step s = GokuLadder.step(i);
            assertEquals(CANON[i], s.id(), "step " + i);
            assertTrue(seen.add(s.id()), "duplicate " + s.id());
            if (i == 0) assertNull(s.form(), "Base is the absence of a form");
            else assertNotNull(SaiyanForms.byId(s.id()), s.id() + " is not a form of the Saiyan chart");
            assertFalse(s.line().isBlank(), "every form has a line");
            assertEquals(i, GokuLadder.indexOf(s.id()));
        }
        assertEquals(-1, GokuLadder.indexOf("nope"));
    }

    @Test
    void healthLinesFallAndTimeFractionsRiseThroughTheLadder() {
        for (int i = 2; i < GokuLadder.size(); i++) {
            assertTrue(GokuLadder.step(i).hpBelow() < GokuLadder.step(i - 1).hpBelow(), "hp line of step " + i);
            assertTrue(GokuLadder.step(i).timeFrac() > GokuLadder.step(i - 1).timeFrac(), "time fraction of step " + i);
        }
        assertTrue(GokuLadder.step(GokuLadder.last()).timeFrac() < 1.0, "the last form comes before the enrage time ends");
    }

    @Test
    void healthAndTimeBothAskForAHigherStepAndNeverALowerOne() {
        long enrage = 14_400;
        assertEquals(0, GokuLadder.wanted(1.0, 0, enrage), "a fresh fight is Base");
        assertEquals(0, GokuLadder.wanted(Double.NaN, 0, enrage), "a broken reading is Base");
        int prev = 0;
        for (double hp = 1.0; hp >= 0.0; hp -= 0.01) {
            int w = GokuLadder.wanted(hp, 0, enrage);
            assertTrue(w >= prev, "health: monotone at " + hp);
            prev = w;
        }
        prev = 0;
        for (long t = 0; t <= enrage + 100; t += 20) {
            int w = GokuLadder.wanted(1.0, t, enrage);
            assertTrue(w >= prev, "time: monotone at " + t);
            prev = w;
        }
        assertEquals(GokuLadder.last(), GokuLadder.wanted(1.0, enrage, enrage), "at the enrage time he is in True Ultra Instinct whatever his health");
        assertEquals(GokuLadder.last(), GokuLadder.wanted(0.01, 0, enrage), "nearly dead is the last form");
    }

    @Test
    void nextNeverSkipsAStepNeverGoesDownAndWaitsForTheDwell() {
        long enrage = 14_400;
        assertEquals(0, GokuLadder.next(0, 0.01, enrage, enrage, GokuLadder.MIN_DWELL - 1), "no change before the dwell is over");
        assertEquals(1, GokuLadder.next(0, 0.01, enrage, enrage, GokuLadder.MIN_DWELL), "one step at a time, even from full-down health");
        assertEquals(5, GokuLadder.next(5, 1.0, 0, enrage, 10_000), "healing never sends him back down");
        assertEquals(GokuLadder.last(), GokuLadder.next(GokuLadder.last(), 0.0, enrage, enrage, 10_000), "the last form stays");
        int form = 0;
        int steps = 0;
        while (form < GokuLadder.last() && steps < 100) {
            int n = GokuLadder.next(form, 0.0, enrage, enrage, GokuLadder.MIN_DWELL);
            assertEquals(form + 1, n);
            form = n;
            steps++;
        }
        assertEquals(GokuLadder.last(), form);
        assertEquals(0, GokuLadder.next(-5, 1.0, 0, enrage, 0), "an out-of-range index is clamped");
    }

    @Test
    void strengthRisesWithTheFormAndNeverFalls() {
        double strength = 1.0;
        for (int i = 1; i < GokuLadder.size(); i++) {
            assertTrue(GokuLadder.damageMul(i, strength) >= GokuLadder.damageMul(i - 1, strength), "damage of step " + i);
            assertTrue(GokuLadder.defense(i) >= GokuLadder.defense(i - 1), "defence of step " + i);
            assertTrue(GokuLadder.reduction(i) >= GokuLadder.reduction(i - 1), "reduction of step " + i);
            assertTrue(GokuLadder.speedMul(i, strength) >= 0.5, "speed floor of step " + i);
        }
        assertEquals(1.0, GokuLadder.damageMul(0, 1.0), 1e-9);
        assertEquals(1.0, GokuLadder.speedMul(0, 1.0), 1e-9);
        assertEquals(0.0, GokuLadder.defense(0), 1e-9);
        assertTrue(GokuLadder.damageMul(GokuLadder.last(), 1.0) > GokuLadder.damageMul(1, 1.0) * 2, "the last form is far above Super Saiyan");
        assertTrue(GokuLadder.damageMul(4, 0.6) < GokuLadder.damageMul(4, 1.3), "the difficulty's form strength counts");
        for (int i = 0; i < GokuLadder.size(); i++) assertTrue(GokuLadder.reduction(i) <= GokuLadder.MAX_REDUCTION + 1e-9, "the reduction is capped at step " + i);
    }

    @Test
    void canonPowerCompoundsTheRatiosAndStartsAtOne() {
        assertEquals(1.0, GokuLadder.canonPower(0), 1e-9);
        assertEquals(50.0, GokuLadder.canonPower(1), 1e-9);
        assertEquals(100.0, GokuLadder.canonPower(2), 1e-9);
        for (int i = 1; i < GokuLadder.size(); i++) assertTrue(GokuLadder.canonPower(i) >= GokuLadder.canonPower(i - 1));
    }

    @Test
    void everyStepHasASkinColourAndTheSkinsAreOnDisk() throws Exception {
        for (int i = 0; i < GokuLadder.size(); i++) {
            assertTrue(GokuLadder.SKINS.contains(GokuLadder.skin(i)), "step " + i);
            int rgb = GokuLadder.rgb(i);
            assertEquals(0, rgb & ~0xFFFFFF, "an aura colour is 0xRRGGBB");
        }
        assertEquals(GokuLadder.SKINS.get(0), GokuLadder.skin(0));
        assertEquals(GokuLadder.SKINS.get(GokuLadder.SKINS.size() - 1), GokuLadder.skin(GokuLadder.last()));
        assertEquals(GokuLadder.SKINS.get(0), GokuLadder.skin(-3), "an out-of-range index is clamped");
        Path dir = Path.of("src/main/resources/assets/anime_multiverse/textures/entity");
        for (String skin : GokuLadder.SKINS) {
            for (String file : new String[]{skin + ".png", skin + "_glow.png"}) {
                Path p = dir.resolve(file);
                assertTrue(Files.isRegularFile(p), file + " is missing (python tools/goku_boss/gen_goku_skins.py)");
                assertTrue(Files.size(p) <= 350 * 1024, file + " is bigger than 350 KB");
            }
        }
    }

    @Test
    void theElectricFormsAreTheOnesTheSeriesDrawsWithLightning() {
        assertFalse(GokuLadder.electric(0));
        assertFalse(GokuLadder.electric(1));
        assertTrue(GokuLadder.electric(2));
        assertTrue(GokuLadder.electric(3));
        assertFalse(GokuLadder.electric(4));
        assertTrue(GokuLadder.electric(6));
        assertTrue(GokuLadder.electric(GokuLadder.last()));
    }
}
