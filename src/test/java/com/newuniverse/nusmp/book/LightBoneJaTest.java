package com.newuniverse.nusmp.book;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the Light and Bone Magic remake after the Black Clover wiki cannot be played by its author, so its decisions are pinned as pure arithmetic: the page tables (old ids kept at their old
 * places, new ones appended, every spell with its Japanese name and its source), the caps of every cast (a cast can never flood a server), the prop budget decision, where an arrow lands and
 * where a fang points, the whip curve the painter draws, and the level of detail of the painters.
 */
class LightBoneJaTest {
    private static final List<String> LIGHT_NAMES = List.of("Light Sword of Judgment", "Bright Judgment Whip", "Heaven-Splitting Flash", "Light Shaft of Divine Punishment", "Arrows of Judgment",
            "Lamp of Avior", "Lamp of Avior Gloria", "Healing Ray of Light", "Absolute Monarch of Divine Brilliance");

    @Test
    void theLightBookHasTheNineSpellsOfTheWikiAndKeepsItsOldPagesFirst() {
        Set<String> english = new HashSet<>();
        for (LightBoneJa.Spell s : LightBoneJa.LIGHT) english.add(s.english());
        for (String n : LIGHT_NAMES) assertTrue(english.contains(n), "the wiki spell " + n + " has no row");
        assertEquals(10, LightBoneJa.LIGHT.size(), "the nine wiki spells and the kept Light Speed");
        assertEquals(LightBoneJa.LIGHT_OLD, LightBoneJa.LIGHT_JA.subList(0, LightBoneJa.LIGHT_OLD.size()), "the old ids keep their places");
        assertEquals(List.of("bright_judgment_whip", "heaven_splitting_flash", "arrows_of_judgment", "avior_lamp", "absolute_monarch"),
                LightBoneJa.LIGHT_JA.subList(LightBoneJa.LIGHT_OLD.size(), LightBoneJa.LIGHT_JA.size()), "the new pages are appended at the end");
    }

    @Test
    void theBoneBookKeepsItsOldPagesFirstAndAppendsTheNewOnes() {
        assertEquals(LightBoneJa.BONE_OLD, LightBoneJa.BONE_JA.subList(0, LightBoneJa.BONE_OLD.size()));
        assertEquals(List.of("demon_sword_dainsleif", "spatial_rupture"), LightBoneJa.BONE_JA.subList(LightBoneJa.BONE_OLD.size(), LightBoneJa.BONE_JA.size()));
        Set<String> english = new HashSet<>();
        for (LightBoneJa.Spell s : LightBoneJa.BONE) english.add(s.english());
        for (String n : List.of("Eternal Fangs", "Bone Bullets", "Demon Sword Dainsleif", "Spatial Rupture")) assertTrue(english.contains(n), "the wiki spell " + n + " has no row");
    }

    @Test
    void everyPageHasExactlyOneRowWithJapaneseNameSourceAndAStatusThatMatchesItsPlace() {
        for (String magic : new String[]{"LIGHT", "BONE"}) {
            Set<String> seen = new HashSet<>();
            List<String> ja = LightBoneJa.ids(magic, true), old = LightBoneJa.ids(magic, false);
            assertEquals(ja.size(), LightBoneJa.spells(magic).size(), magic + ": one row per page");
            for (LightBoneJa.Spell s : LightBoneJa.spells(magic)) {
                assertTrue(seen.add(s.id()), "twice: " + s.id());
                assertTrue(ja.contains(s.id()), s.id() + " is not a page");
                assertFalse(s.english().isBlank() || s.kanji().isBlank() || s.romaji().isBlank() || s.source().isBlank() || s.user().isBlank(), s.id());
                assertTrue(s.kanji().codePoints().anyMatch(c -> c > 0x2E80), s.id() + ": the Japanese name is not Japanese");
                assertEquals(!old.contains(s.id()), s.status() == LightBoneJa.Status.NEW, s.id() + ": NEW exactly for the appended pages");
                assertSame(s, LightBoneJa.byId(magic, s.id()));
                assertTrue(s.title().contains(s.kanji()) && s.title().contains(s.english()));
            }
            for (String id : ja) assertTrue(seen.contains(id), id + " has no row");
            assertEquals(ja.size(), new HashSet<>(ja).size(), "unique ids");
        }
    }

    @Test
    void everyCountIsCappedForEveryModifier() {
        assertTrue(LightBoneJa.capsHold());
        for (int m = -3; m <= 9; m++) {
            assertTrue(LightBoneJa.swordCount(m) >= 4 && LightBoneJa.swordCount(m) <= LightBoneJa.MAX_SWORDS);
            assertTrue(LightBoneJa.arrowCount(m) >= 12 && LightBoneJa.arrowCount(m) <= LightBoneJa.MAX_ARROWS);
            assertTrue(LightBoneJa.fangCount(m) >= 16 && LightBoneJa.fangCount(m) <= LightBoneJa.MAX_FANGS);
            assertTrue(LightBoneJa.bulletCount(m) >= 3 && LightBoneJa.bulletCount(m) <= LightBoneJa.MAX_BULLETS);
            assertTrue(LightBoneJa.slashCount(m) >= 1 && LightBoneJa.slashCount(m) <= LightBoneJa.MAX_SLASHES);
        }
        assertTrue(LightBoneJa.swordCount(4) >= LightBoneJa.swordCount(0), "a stronger caster never fires fewer");
        assertTrue(LightBoneJa.MAX_ARROWS + LightBoneJa.MAX_SWORDS + 4 < LightBoneJa.MAX_PROPS_PER_CASTER, "the busiest cast fits under the per-caster prop cap");
        assertTrue(LightBoneJa.MAX_FANGS < LightBoneJa.MAX_PROPS_PER_CASTER);
    }

    @Test
    void thePropBudgetAdmitsUnderTheCapsEndsTheOldestAtTheCasterCapAndRefusesAFullServer() {
        assertEquals(LightBoneJa.ADMIT, LightBoneJa.admit(0, 0));
        assertEquals(LightBoneJa.ADMIT, LightBoneJa.admit(LightBoneJa.MAX_PROPS_PER_CASTER - 1, 10));
        assertEquals(LightBoneJa.ADMIT_AFTER_ENDING_OLDEST, LightBoneJa.admit(LightBoneJa.MAX_PROPS_PER_CASTER, 10));
        assertEquals(LightBoneJa.REFUSE, LightBoneJa.admit(0, LightBoneJa.MAX_PROPS_GLOBAL), "a newcomer gets no prop on a full server");
        assertEquals(LightBoneJa.ADMIT_AFTER_ENDING_OLDEST, LightBoneJa.admit(3, LightBoneJa.MAX_PROPS_GLOBAL), "a caster on a full server swaps instead of adding");
        for (int caster = 0; caster <= 60; caster++)
            for (int global : new int[]{0, 80, LightBoneJa.MAX_PROPS_GLOBAL - 1, LightBoneJa.MAX_PROPS_GLOBAL, 500}) {
                int d = LightBoneJa.admit(caster, global);
                assertTrue(d == LightBoneJa.REFUSE || d == LightBoneJa.ADMIT || d == LightBoneJa.ADMIT_AFTER_ENDING_OLDEST);
                if (d == LightBoneJa.ADMIT) assertTrue(caster < LightBoneJa.MAX_PROPS_PER_CASTER && global < LightBoneJa.MAX_PROPS_GLOBAL, "an admitted prop never breaks a cap");
            }
    }

    @Test
    void arrowsLandInsideTheAreaAndSpreadOverIt() {
        for (int n : new int[]{12, 14, 20}) {
            Set<Long> cells = new HashSet<>();
            for (int i = 0; i < n; i++) {
                double[] o = LightBoneJa.arrowOffset(i, n, LightBoneJa.ARROW_RADIUS);
                assertTrue(Math.hypot(o[0], o[1]) <= LightBoneJa.ARROW_RADIUS + 1e-9, "inside the disc");
                cells.add(Math.round(o[0] / 2.5) * 1000 + Math.round(o[1] / 2.5));
            }
            assertTrue(cells.size() >= n / 2, "the arrows do not pile on one spot: " + cells.size() + " cells for " + n);
        }
        assertEquals(LightBoneJa.fallTicks(22, 2.4), 10);
        assertTrue(LightBoneJa.fallTicks(1, 100) >= 1);
    }

    @Test
    void fangsLeaveTheBodyInEveryDirectionAboveTheFeetAndInThreeWaves() {
        for (int n : new int[]{16, 20, 24}) {
            double minX = 9, maxX = -9, minZ = 9, maxZ = -9;
            int maxDelay = 0;
            for (int i = 0; i < n; i++) {
                double[] d = LightBoneJa.fangDirection(i, n);
                assertEquals(1.0, Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]), 1e-9, "a unit vector");
                assertTrue(d[1] > 0.0 && d[1] < 0.95, "never into the ground");
                minX = Math.min(minX, d[0]); maxX = Math.max(maxX, d[0]); minZ = Math.min(minZ, d[2]); maxZ = Math.max(maxZ, d[2]);
                maxDelay = Math.max(maxDelay, LightBoneJa.fangDelay(i, n));
            }
            assertTrue(minX < -0.4 && maxX > 0.4 && minZ < -0.4 && maxZ > 0.4, "all round the body");
            assertTrue(maxDelay <= (LightBoneJa.FANG_WAVES - 1) * LightBoneJa.FANG_WAVE_GAP, "within the three waves");
        }
    }

    @Test
    void theWhipCurlsButStaysInsideItsLengthAndAllocatesNothing() {
        double[] p = new double[3];
        LightBoneJa.whipPoint(0.0, 13, 5, p);
        assertEquals(0.0, p[0], 1e-9);
        assertEquals(0.0, p[2], 1e-9);
        double prevZ = -1, maxCurl = 0;
        for (int step = 0; step <= 40; step++) {
            double t = step / 40.0;
            for (double age = 0; age < 30; age += 3.7) {
                LightBoneJa.whipPoint(t, 13, age, p);
                assertTrue(Math.abs(p[0]) <= 0.35 * 13 && Math.abs(p[1]) <= 0.35 * 13, "the whip stays near its axis");
                assertTrue(p[2] >= 0 && p[2] <= 13.0001, "never longer than itself");
                if (t > 0.9) maxCurl = Math.max(maxCurl, Math.abs(p[0]) + Math.abs(p[1]));
            }
            LightBoneJa.whipPoint(t, 13, 0, p);
            assertTrue(p[2] >= prevZ - 1e-9, "the whip runs outward");
            prevZ = p[2];
        }
        assertTrue(maxCurl > 0.3, "the tip curls");
    }

    @Test
    void flyingPropsAreMovedByTheClientFromTheirAgeAndTheSpeedRidesInTheParam() {
        for (double v : new double[]{0.5, 1.9, 2.4, 2.6, 6.0, 8.3}) {
            int bits = LightBoneJa.speedBits(v);
            assertEquals(0, bits & 0xFF, "the variant byte stays free");
            assertEquals(v, LightBoneJa.speedOf(bits | 5), 0.07, "the speed survives the param: " + v);
        }
        assertEquals(12.0, LightBoneJa.travelled(2.0, 6, 10), 1e-9);
        assertEquals(20.0, LightBoneJa.travelled(2.0, 99, 10), 1e-9, "never past the end of its life");
        assertEquals(0.0, LightBoneJa.travelled(2.0, -3, 10), 1e-9);
        assertTrue(LightBoneJa.swordSpeed(2.4f) > LightBoneJa.swordSpeed(0.8f));
    }

    @Test
    void theLevelOfDetailFallsWithTheDistanceAndNothingIsDrawnFarAway() {
        assertEquals(1f, LightBoneJa.lod(0), 0);
        assertEquals(1f, LightBoneJa.lod(23.9), 0);
        assertTrue(LightBoneJa.lod(30) < 1f && LightBoneJa.lod(60) < LightBoneJa.lod(30) && LightBoneJa.lod(100) < LightBoneJa.lod(60));
        assertEquals(0f, LightBoneJa.lod(128), 0);
        assertEquals(0f, LightBoneJa.lod(1000), 0);
        assertEquals(4, LightBoneJa.seg(10, 0.1f, 0.1f, 4), "never below the minimum");
        assertEquals(10, LightBoneJa.seg(10, 1f, 1f, 4));
        assertTrue(LightBoneJa.seg(24, 0.45f, 0.5f, 6) < 24);
        assertTrue(LightBoneJa.MAX_QUADS_PER_PROP >= 1000 && LightBoneJa.MAX_QUADS_PER_PROP <= 3000);
    }

    @Test
    void theNumbersOfTheCastsAreSane() {
        assertTrue(LightBoneJa.flashLength(0.9f) >= 28 && LightBoneJa.flashLength(5f) <= 46);
        assertTrue(LightBoneJa.shaftRadius(0.9f) >= 2.59 && LightBoneJa.shaftRadius(5f) <= 4.21);
        assertTrue(LightBoneJa.gloriaRadius(1f) >= 14);
        assertTrue(LightBoneJa.SHAFT_LIFE >= LightBoneJa.SHAFT_TELEGRAPH + LightBoneJa.SHAFT_PULSES * LightBoneJa.SHAFT_GAP, "the column lives through every pulse");
        assertTrue(LightBoneJa.MONARCH_TICKS % LightBoneJa.MONARCH_BLOW_GAP == 0 && LightBoneJa.MONARCH_BLOW_GAP % 10 == 0, "the blows land on a step of the 10 tick zone");
        assertTrue(LightBoneJa.DAINSLEIF_TICKS >= 3 + LightBoneJa.DAINSLEIF_SWINGS * LightBoneJa.DAINSLEIF_GAP);
        assertTrue(LightBoneJa.WHIP_FAN_DEG.length == LightBoneJa.WHIP_LASHES);
        assertTrue(LightBoneJa.SPACE_BYPASS > LightBoneJa.BULLET_BYPASS, "cut space ignores more armour than a bullet");
        assertTrue(LightBoneJa.MAX_HITS <= 16);
    }
}
