package com.newuniverse.nusmp.elite.skills;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: Gaia is the one skill with seven parts (cost, zone, war check, bloom ledger, vein shape, pillars), and the parts must agree: a bigger domain costs more, war costs more, a vein is
 * connected and reproducible, the daily cap really caps, a pillar volley is laid out in a line, and a zone knows what is inside it. All Minecraft-free.
 */
class GaiaRulesTest {
    // ---------------------------------------------------------------- GaiaCost
    @Test
    void costsGrowWithSizeAndWarAndAreNeverFree() {
        assertTrue(GaiaCost.raise(20) > GaiaCost.raise(14));
        assertTrue(GaiaCost.radius(true) > GaiaCost.radius(false));
        assertTrue(GaiaCost.durationTicks(true) > GaiaCost.durationTicks(false));
        double calm = GaiaCost.upkeepPerSecond(14, 0, false, false);
        assertTrue(GaiaCost.upkeepPerSecond(14, 3, false, false) > calm);
        assertTrue(GaiaCost.upkeepPerSecond(14, 0, true, false) > calm);
        assertEquals(calm * 1.5, GaiaCost.upkeepPerSecond(14, 0, false, true), 1e-9);
        assertTrue(GaiaCost.upkeepPerSecond(20, 0, false, false) > calm);
        assertTrue(GaiaCost.pillars(5) > GaiaCost.pillars(3));
        assertEquals(1.0, GaiaCost.sane(Double.NaN));
        assertEquals(1.0, GaiaCost.sane(-50));
        assertEquals(1.0e9, GaiaCost.sane(1e20));
        assertEquals(500.0, GaiaCost.sane(500.0));
    }

    // ---------------------------------------------------------------- GaiaZone
    @Test
    void aZoneKnowsWhatIsInsideAndWhenItEnds() {
        GaiaZone.clear();
        UUID owner = UUID.randomUUID();
        GaiaZone z = new GaiaZone(owner, "overworld", 100, 64, 100, 10, 1000);
        assertTrue(z.contains("overworld", 105, 70, 100));
        assertFalse(z.contains("overworld", 111, 64, 100), "outside the radius");
        assertFalse(z.contains("overworld", 100, 80, 100), "above the cylinder");
        assertFalse(z.contains("the_nether", 100, 64, 100), "another dimension");
        assertTrue(z.active(999));
        assertFalse(z.active(1000));
        GaiaZone.put(z);
        assertSame(z, GaiaZone.of(owner));
        assertSame(z, GaiaZone.at("overworld", 100, 64, 100, 500));
        assertNull(GaiaZone.at("overworld", 500, 64, 500, 500));
        assertNull(GaiaZone.at("overworld", 100, 64, 100, 5000), "an expired zone is not found");
        assertNull(GaiaZone.of(owner), "and is dropped when met");
        GaiaZone.put(z);
        assertEquals(1, GaiaZone.count());
        assertSame(z, GaiaZone.remove(owner));
        assertEquals(0, GaiaZone.count());
        assertEquals(2000, z.endingAt(2000).until());
    }

    // ---------------------------------------------------------------- GaiaWarCheck
    @Test
    void theWarCheckClassifiesVisitorsByNation() {
        assertEquals(GaiaWarCheck.Relation.SELF, GaiaWarCheck.classify(true, "a", "b", true));
        assertEquals(GaiaWarCheck.Relation.ALLY, GaiaWarCheck.classify(false, "a", "a", false));
        assertEquals(GaiaWarCheck.Relation.ENEMY, GaiaWarCheck.classify(false, "a", "b", true));
        assertEquals(GaiaWarCheck.Relation.NEUTRAL, GaiaWarCheck.classify(false, "a", "b", false));
        assertEquals(GaiaWarCheck.Relation.NEUTRAL, GaiaWarCheck.classify(false, "", "", true), "no nation, no war");
        assertEquals(GaiaWarCheck.Relation.NEUTRAL, GaiaWarCheck.classify(false, null, "b", true));
        UUID o = UUID.randomUUID(), x = UUID.randomUUID();
        assertEquals(GaiaWarCheck.Relation.NEUTRAL, GaiaWarCheck.relation(o, x, id -> "", (a, b) -> false), "nations not built: everybody is a stranger");
        assertEquals(GaiaWarCheck.Relation.NEUTRAL, GaiaWarCheck.relation(o, x, null, null));
        assertEquals(GaiaWarCheck.Relation.SELF, GaiaWarCheck.relation(o, o, null, null));
        assertEquals(GaiaWarCheck.Relation.ENEMY, GaiaWarCheck.relation(o, x, id -> id.equals(o) ? "red" : "blue", (a, b) -> true));
        assertEquals(GaiaWarCheck.Relation.ALLY, GaiaWarCheck.relation(o, x, id -> "red", (a, b) -> true));
        assertTrue(GaiaWarCheck.warFooting(List.of(GaiaWarCheck.Relation.NEUTRAL, GaiaWarCheck.Relation.ENEMY)));
        assertFalse(GaiaWarCheck.warFooting(List.of(GaiaWarCheck.Relation.ALLY)));
        assertFalse(GaiaWarCheck.warFooting(null));
        assertTrue(GaiaWarCheck.blesses(GaiaWarCheck.Relation.ALLY) && !GaiaWarCheck.blesses(GaiaWarCheck.Relation.NEUTRAL));
        assertTrue(GaiaWarCheck.guardsAgainst(GaiaWarCheck.Relation.NEUTRAL) && !GaiaWarCheck.guardsAgainst(GaiaWarCheck.Relation.SELF));
        assertTrue(GaiaWarCheck.burdens(GaiaWarCheck.Relation.ENEMY) && !GaiaWarCheck.burdens(GaiaWarCheck.Relation.NEUTRAL));
    }

    // ---------------------------------------------------------------- GaiaBloomLedger
    @Test
    void theLedgerEarnsSpendsAndHonoursTheDailyCap() {
        GaiaBloomLedger l = GaiaBloomLedger.fresh(10);
        assertFalse(l.canBloom(10, 3), "no points yet");
        l.earn(1000);
        assertEquals(GaiaBloomLedger.POINT_CAP, l.points(), "capped");
        assertTrue(l.bloom(10, 3));
        assertTrue(l.bloom(10, 3));
        assertTrue(l.bloom(10, 3));
        assertFalse(l.bloom(10, 3), "the daily cap stops the fourth even with points left");
        assertEquals(0, l.remainingToday(10, 3));
        assertEquals(3, l.total());
        assertEquals(GaiaBloomLedger.POINT_CAP - 3 * GaiaBloomLedger.VEIN_COST, l.points());
        assertTrue(l.bloom(11, 3), "a new day resets the count, not the points");
        assertEquals(1, l.veinsToday());
        assertEquals(4, l.total());
        int before = l.points();
        l.earn(-50);
        assertEquals(before, l.points(), "negative earnings are ignored");
        assertFalse(l.bloom(11, 0), "a cap of zero switches blooming off");
        GaiaBloomLedger back = GaiaBloomLedger.fromInts(l.toInts(), 11);
        assertEquals(l.points(), back.points());
        assertEquals(l.veinsToday(), back.veinsToday());
        assertEquals(l.total(), back.total());
        assertEquals(0, GaiaBloomLedger.fromInts(null, 5).points());
        assertEquals(0, GaiaBloomLedger.fromInts(new int[]{1}, 5).points());
    }

    // ---------------------------------------------------------------- VeinBloom
    @Test
    void veinsAreReproducibleConnectedAndTheRareOresNeedMastery() {
        List<int[]> a = VeinBloom.shape(42L, 6), b = VeinBloom.shape(42L, 6);
        assertEquals(6, a.size());
        for (int i = 0; i < a.size(); i++) assertTrue(java.util.Arrays.equals(a.get(i), b.get(i)), "block " + i + " differs between two runs of one seed");
        // connected: every block after the first touches an earlier one
        for (int i = 1; i < a.size(); i++) {
            boolean touches = false;
            for (int j = 0; j < i; j++) {
                int d = Math.abs(a.get(i)[0] - a.get(j)[0]) + Math.abs(a.get(i)[1] - a.get(j)[1]) + Math.abs(a.get(i)[2] - a.get(j)[2]);
                if (d == 1) touches = true;
            }
            assertTrue(touches, "block " + i + " is not connected");
        }
        assertEquals(1, VeinBloom.shape(7L, 1).size());
        assertEquals(9, VeinBloom.shape(7L, 9).size());
        for (double roll = 0.0; roll < 1.0; roll += 0.01) {
            assertTrue(VeinBloom.pick(roll, 0).minLevel == 0, "diamond and gold are not for the unmastered");
            assertTrue(VeinBloom.size(roll, 0) >= 3 && VeinBloom.size(roll, 0) <= 6);
            assertTrue(VeinBloom.size(roll, 1) >= 5 && VeinBloom.size(roll, 1) <= 9);
        }
        boolean sawGold = false, sawDiamond = false;
        for (double roll = 0.0; roll < 1.0; roll += 0.001) {
            VeinBloom.Ore o = VeinBloom.pick(roll, 1);
            sawGold |= o == VeinBloom.Ore.GOLD;
            sawDiamond |= o == VeinBloom.Ore.DIAMOND;
        }
        assertTrue(sawGold && sawDiamond, "the mastered table reaches the rare ores");
        assertEquals(VeinBloom.Ore.COAL, VeinBloom.pick(0.0, 0));
        assertEquals(VeinBloom.Ore.COAL, VeinBloom.pick(-3.0, 0));
        assertNotEquals(null, VeinBloom.pick(5.0, 1));
        assertEquals("minecraft:deepslate_iron_ore", VeinBloom.Ore.IRON.blockFor(true));
        assertEquals("minecraft:iron_ore", VeinBloom.Ore.IRON.blockFor(false));
        assertTrue(VeinBloom.deep(-1) && !VeinBloom.deep(0));
    }

    // ---------------------------------------------------------------- GaiaPillars
    @Test
    void pillarsStandInALineAndRiseOneAfterAnother() {
        double[][] c = GaiaPillars.centres(0, 0, 3, 4, 3);
        assertEquals(3, c.length);
        assertEquals(0.6 * GaiaPillars.distance(0), c[0][0], 1e-9);
        assertEquals(0.8 * GaiaPillars.distance(2), c[2][1], 1e-9);
        assertEquals(0, GaiaPillars.centres(0, 0, 1, 0, 0).length);
        assertEquals(GaiaPillars.distance(0), GaiaPillars.centres(0, 0, 0, 0, 1)[0][1], 1e-9, "no direction: north-south");
        assertEquals(0, GaiaPillars.delay(0));
        assertTrue(GaiaPillars.delay(2) > GaiaPillars.delay(1));
        assertTrue(GaiaPillars.count(true) > GaiaPillars.count(false));
        assertTrue(GaiaPillars.damage(2, 3, false) > GaiaPillars.damage(0, 3, false), "the volley builds");
        assertEquals(GaiaPillars.DAMAGE, GaiaPillars.damage(0, 1, false), 1e-9);
    }
}
