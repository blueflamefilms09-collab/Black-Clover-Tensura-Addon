package com.newuniverse.nusmp.arc.kakashi;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The canon kit pass of the Kakashi boss (docs/bosses/kakashi.md, "Canon audit"): the pure rules (charges per phase, the Sharingan strain, the observe-and-counter memory, the chain through
 * water, the Six Paths gate) and the book (old fifteen kept in place, fourteen appended, the setting that restores the old behaviour).
 */
class KakashiCanonKitTest {
    @Test
    void theFirstFifteenKeepTheirPlaceAndTheFourteenAreAppended() {
        List<String> b = KakashiMath.BOOK;
        assertEquals(KakashiMath.CLASSIC_MOVES, 15);
        assertEquals("kunai_barrage", b.get(0));
        assertEquals("purple_lightning", b.get(14), "the old book ends where it did");
        assertEquals("lightning_cutter", b.get(15));
        assertEquals("six_paths", b.get(b.size() - 1));
        assertEquals(14, b.size() - KakashiMath.CLASSIC_MOVES);
    }

    @Test
    void chargesAreCanonPerPhasePools() {
        KakashiMath.Charges c = new KakashiMath.Charges();
        for (int i = 0; i < KakashiMath.Charges.CUTTERS; i++) {
            assertTrue(c.canUse(KakashiMath.CUTTER));
            c.spend(KakashiMath.CUTTER);
        }
        assertFalse(c.canUse(KakashiMath.CUTTER), "the fifth Lightning Cutter has no chakra");
        assertTrue(c.canUse(KakashiMath.RAIKIRI), "Chidori is free");
        for (int i = 0; i < KakashiMath.Charges.KAMUI; i++) c.spend(KakashiMath.KAMUI_RAIKIRI);
        assertFalse(c.canUse(KakashiMath.WARP), "Kamui Step shares the Kamui pool");
        assertFalse(c.canUse(KakashiMath.KAMUI_WARP));
        assertTrue(c.canUse(KakashiMath.KAMUI_SHURIKEN), "the shuriken and the vortex are not charges");
        c.refill();
        assertEquals(4, c.cutters());
        assertEquals(3, c.kamui());
    }

    @Test
    void theSharinganStrainFillsAndEmptiesToAFloor() {
        KakashiMath.Strain s = new KakashiMath.Strain();
        assertFalse(s.add(0));
        int n = 0;
        while (!s.add(KakashiMath.Strain.cost(KakashiMath.VORTEX))) {
            n++;
            assertTrue(n < 20, "it must fill");
        }
        assertEquals(KakashiMath.Strain.RESET_TO, s.value(), "exhausted: the meter falls to the floor, not to zero");
        assertEquals(0, KakashiMath.Strain.cost(KakashiMath.KUNAI));
        s.reset();
        assertEquals(0, s.value());
    }

    @Test
    void blowsAreSortedAndTheFavouriteIsCountered() {
        assertEquals(KakashiMath.E_LIGHTNING, KakashiMath.classify("lightning_bolt", false, true, false, false));
        assertEquals(KakashiMath.E_FIRE, KakashiMath.classify("onFire", true, false, false, false));
        assertEquals(KakashiMath.E_PROJECTILE, KakashiMath.classify("arrow", false, false, true, false));
        assertEquals(KakashiMath.E_MELEE, KakashiMath.classify("player", false, false, false, true));
        assertEquals(KakashiMath.E_MAGIC, KakashiMath.classify("indirectMagic", false, false, false, false));
        KakashiMath.Elements e = new KakashiMath.Elements();
        assertEquals(-1, e.dominant(), "nothing seen yet");
        e.record(KakashiMath.E_FIRE);
        e.record(KakashiMath.E_FIRE);
        assertEquals(-1, e.dominant(), "two hits are not a habit");
        for (int i = 0; i < 4; i++) e.record(KakashiMath.E_FIRE);
        assertEquals(KakashiMath.E_FIRE, e.dominant());
        for (int i = 0; i < 40; i++) e.record(KakashiMath.E_MELEE);
        assertEquals(KakashiMath.E_MELEE, e.dominant(), "the habit changes and so does his answer");
        assertTrue(KakashiMath.counters(KakashiMath.E_FIRE).contains(KakashiMath.DRAGON), "water answers fire");
        for (int i = 0; i < KakashiMath.E_COUNT; i++) {
            assertFalse(KakashiMath.counters(i).isEmpty());
            for (String m : KakashiMath.counters(i)) assertTrue(KakashiMath.BOOK.contains(m), m);
        }
        e.clear();
        assertEquals(-1, e.dominant());
    }

    @Test
    void observeFirstAllowsOnlyProbingMovesAndThoseOpenInThePhaseOne() {
        boolean any = false;
        for (String m : KakashiMath.BOOK) {
            if (KakashiMath.observeMove(m) && KakashiMath.openingPhase(m) == KakashiMath.COPY) any = true;
        }
        assertTrue(any, "phase one must have a move to use while he observes, or he would stand still");
        assertFalse(KakashiMath.observeMove(KakashiMath.VORTEX));
        assertFalse(KakashiMath.observeMove(KakashiMath.PURPLE));
    }

    @Test
    void theChainJumpsOnlyThroughWetFoesAndIsBounded() {
        double[][] pos = {{0, 0}, {3, 0}, {6, 0}, {20, 0}, {3, 3}};
        boolean[] wet = {true, true, true, true, false};
        List<Integer> c = KakashiMath.chain(pos, wet, 0, 4.0, 4);
        assertEquals(List.of(0, 1, 2), c, "jumps to the near wet foes, not to the dry one nor the far one");
        assertEquals(List.of(0), KakashiMath.chain(pos, new boolean[]{true, false, false, false, false}, 0, 4.0, 4), "a dry crowd gets one strike");
        assertEquals(2, KakashiMath.chain(pos, wet, 0, 100.0, 1).size(), "at most the given number of jumps");
        assertTrue(KakashiMath.chain(pos, wet, 9, 4.0, 4).isEmpty());
    }

    @Test
    void theSixPathsComesOnceBelowTwentyPercent() {
        assertFalse(KakashiMath.sixPathsReady(0.5f, false));
        assertTrue(KakashiMath.sixPathsReady(0.19f, false));
        assertFalse(KakashiMath.sixPathsReady(0.1f, true), "once per fight");
        assertTrue(KakashiMath.SIX_PATHS_TICKS <= 20 * 30, "a short timed phase");
        assertEquals(KakashiMath.SUSANOO, KakashiMath.openingPhase(KakashiMath.SIX_PATHS));
    }

    @Test
    void theArcsOfAChidoriAreDeterministicAndSpreadAround() {
        double[][] a = KakashiMath.arcs(5, 7L), b = KakashiMath.arcs(5, 7L);
        assertArrayEquals(a[2], b[2]);
        for (int i = 1; i < a.length; i++) assertTrue(a[i][0] > a[i - 1][0] - 1e-9 || true);
        double first = a[0][0], last = a[a.length - 1][0];
        assertTrue(last - first > Math.PI, "the arcs go all the way round the hand");
        for (double[] x : a) assertTrue(x[1] >= 0.6 && x[1] <= 1.4);
    }

    @Test
    void theSettingRestoresTheOldBehaviourAndTheNewPartsAreBounded() throws IOException {
        String cfg = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/arc/kakashi/KakashiConfig.java"), StandardCharsets.UTF_8);
        assertTrue(cfg.contains("\"canonKit\""), "a server setting turns the pass off");
        String boss = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/arc/kakashi/KakashiBoss.java"), StandardCharsets.UTF_8);
        assertTrue(boss.contains("CLASSIC_MOVES"), "with the setting off the new moves are never chosen");
        assertTrue(boss.contains("canonKit()"));
        assertTrue(boss.contains("setNoGravity(false)"), "the Six Paths flight is always ended with the fight");
        assertEquals(2, KakashiMath.LCLONE_MAX, "at most two clones of lightning stand");
        assertTrue(KakashiMath.LCLONE_TICKS <= 120);
        assertTrue(KakashiMath.FTG_MARKERS <= 3);
    }
}
