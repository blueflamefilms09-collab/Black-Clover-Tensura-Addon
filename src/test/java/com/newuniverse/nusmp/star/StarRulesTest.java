package com.newuniverse.nusmp.star;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the Star nodes rules decide caps, who is the Grinberryall heir, what each page consumes, how many allies Conjunction brings and what the tower does with a tied pair. They are pure, so every number the
 * owner asked for is pinned here (6 per player, 8 for the heir, 128 on the server, 40 s; consume rules; ally bring limit; heir gate for the whole Quartile family; dual mode default FUSION).
 */
class StarRulesTest {
    @Test
    void theDefaultsAreTheOwnersNumbersAndEveryClampHolds() {
        assertEquals(6, StarRules.DEFAULT_PER_PLAYER);
        assertEquals(8, StarRules.DEFAULT_HEIR_PER_PLAYER);
        assertEquals(128, StarRules.DEFAULT_SERVER);
        assertEquals(800, StarRules.DEFAULT_LIFE_TICKS, "40 seconds");
        assertTrue(StarRules.DEFAULT_NODES, "star.nodes is on by default");
        assertEquals("nusmp-star-server.toml", StarRules.CONFIG_FILE);
        assertEquals(6, StarRules.perPlayerCap(false, 6, 8));
        assertEquals(8, StarRules.perPlayerCap(true, 6, 8));
        assertEquals(6, StarRules.perPlayerCap(true, 6, 2), "an heir never has fewer than a normal mage");
        assertEquals(8, StarRules.perPlayerCap(false, 50, 50), "the clamp is 8");
        assertEquals(1, StarRules.perPlayerCap(false, -3, 8));
        assertEquals(16, StarRules.serverCap(0));
        assertEquals(512, StarRules.serverCap(100000));
        assertEquals(200, StarRules.lifeTicks(5));
        assertEquals(2400, StarRules.lifeTicks(999999));
        assertEquals(8, StarRules.placeRange(1));
        assertEquals(32, StarRules.placeRange(500));
        assertEquals(10, StarRules.seedCooldown(0));
        assertEquals(80, StarRules.seedCooldown(500));
        assertEquals(0, StarRules.maxAllies(-1));
        assertEquals(5, StarRules.maxAllies(40));
        assertEquals(0.5, StarRules.heirMastery(0.1));
        assertEquals(1.0, StarRules.heirMastery(7));
    }

    @Test
    void theHeirNeedsASpadeStarGrimoireTheMasteryAndTheClearedSpadeRaid() {
        assertTrue(StarRules.isHeir(true, 1.0, 1.0, true));
        assertFalse(StarRules.isHeir(false, 1.0, 1.0, true), "not a Spade grimoire");
        assertFalse(StarRules.isHeir(true, 0.99, 1.0, true), "mastery below the need");
        assertFalse(StarRules.isHeir(true, 1.0, 1.0, false), "the Spade Kingdom Raid is not cleared");
        assertTrue(StarRules.isHeir(true, 0.6, 0.5, true), "a lower server need is honoured");
        assertFalse(StarRules.isHeir(true, 0.4, 0.1, true), "the need never goes below the floor of 0.5");
    }

    @Test
    void theWholeQuartileFamilyQuintileAndBlackNeverlandNeedTheHeirAndTheBasicPagesDoNot() {
        for (String id : List.of("quartile_scutum", "quartile_hasta", "quartile_flagellum", "quintile_atrum_scutum", "quintile_atrum_hasta", "quintile_atrum_flagellum", "black_neverland"))
            assertTrue(StarRules.needsHeir(id), id + " needs the heir");
        for (String id : List.of("star_seed", "star_conjunction", "star_shot", "starfall", "constellation", "comet", "star_shield", "supernova", "twinkle_dart", "never_never_land", "star_beam",
                "star_chain", "orbit_barrage", "stardust_field", "meteor_swarm"))
            assertFalse(StarRules.needsHeir(id), id + " stays open to every Star mage");
        assertFalse(StarRules.HEIR_PAGES.contains("quartile_flagellum"), "the old page is never blocked from opening (only its whip is for the heir)");
        assertEquals(7, StarRules.NEW_PAGES.size());
        assertTrue(StarRules.unverifiedCanon("quintile_atrum_hasta") && !StarRules.unverifiedCanon("quartile_hasta") && !StarRules.unverifiedCanon("black_neverland"));
        assertFalse(StarRules.NEW_PAGES.contains("halation"), "Halation is not canon and is not built");
    }

    @Test
    void conjunctionAndHastaConsumeStarsScutumAndFlagellumDoNot() {
        assertTrue(StarRules.consumesStars("star_conjunction"));
        assertTrue(StarRules.consumesStars("quartile_hasta"));
        assertTrue(StarRules.consumesStars("quintile_atrum_hasta"));
        assertFalse(StarRules.consumesStars("quartile_scutum"));
        assertFalse(StarRules.consumesStars("quartile_flagellum"));
        assertFalse(StarRules.consumesStars("quintile_atrum_scutum"));
        assertFalse(StarRules.consumesStars("quintile_atrum_flagellum"));
        assertFalse(StarRules.consumesStars("star_seed"));
        assertEquals(4, StarRules.starsNeeded("quartile_hasta"));
        assertEquals(5, StarRules.starsNeeded("quintile_atrum_scutum"));
        assertEquals(0, StarRules.starsNeeded("star_seed"));
    }

    @Test
    void conjunctionBringsAtMostTheConfiguredNumberOfNearestAlliesAndNeverMoreThanTheTierAllows() {
        assertEquals(3, StarRules.allyLimit(StarRules.DEFAULT_ALLIES, 6));
        assertEquals(5, StarRules.allyLimit(99, 6), "the clamp is 5");
        assertEquals(1, StarRules.allyLimit(5, 2), "never more than the tier's targets minus the caster");
        assertEquals(0, StarRules.allyLimit(0, 6));
        assertEquals(0, StarRules.allyLimit(3, 1));
        List<String> nearest = new ArrayList<>(List.of("a", "b", "c", "d", "e"));
        assertEquals(List.of("a", "b", "c"), StarRules.firstN(nearest, 3));
        assertEquals(5, StarRules.firstN(nearest, 99).size());
        assertEquals(0, StarRules.firstN(nearest, -2).size());
        assertEquals(3.0, StarRules.ALLY_RADIUS);
    }

    @Test
    void theDualPlanKeepsFusionByDefaultAndSplitsOnlyAStarPairInSecondGrimoireMode() {
        assertEquals(StarRules.DualMode.FUSION, StarRules.parseDual(null));
        assertEquals(StarRules.DualMode.FUSION, StarRules.parseDual("nonsense"));
        assertEquals(StarRules.DualMode.SECOND_GRIMOIRE, StarRules.parseDual(" second_grimoire "));
        StarRules.DualPlan fuse = StarRules.dualPlan(StarRules.DualMode.FUSION, "GLASS", "FLAME", "WATER");
        assertEquals("GLASS", fuse.first());
        assertNull(fuse.extra());
        assertTrue(fuse.fuse());
        StarRules.DualPlan fuseStar = StarRules.dualPlan(StarRules.DualMode.FUSION, "SAND", "WIND", "STAR");
        assertNull(fuseStar.extra(), "FUSION is the old behaviour even for a Star pair");
        assertTrue(fuseStar.fuse());
        StarRules.DualPlan noStar = StarRules.dualPlan(StarRules.DualMode.SECOND_GRIMOIRE, "GLASS", "FLAME", "WATER");
        assertNull(noStar.extra(), "a pair without Star is fused as before");
        assertTrue(noStar.fuse());
        StarRules.DualPlan split = StarRules.dualPlan(StarRules.DualMode.SECOND_GRIMOIRE, "SAND", "WIND", "STAR");
        assertEquals("WIND", split.first());
        assertEquals("STAR", split.extra());
        assertFalse(split.fuse());
        StarRules.DualPlan splitB = StarRules.dualPlan(StarRules.DualMode.SECOND_GRIMOIRE, "SAND", "STAR", "EARTH");
        assertEquals("EARTH", splitB.first());
        StarRules.DualPlan blend = StarRules.dualPlan(StarRules.DualMode.SECOND_GRIMOIRE, "STAR", "FLAME", "LIGHT");
        assertEquals("FLAME", blend.first(), "a Star blend of two other attributes: the first parent and a Star grimoire");
        assertEquals("STAR", blend.extra());
        StarRules.DualPlan untied = StarRules.dualPlan(StarRules.DualMode.SECOND_GRIMOIRE, "STAR", null, null);
        assertNull(untied.extra(), "nothing tied: one grimoire");
        assertFalse(untied.fuse());
    }

    @Test
    void theDamageNumbersStayInsideTheTierClamp() {
        float mid = 12f, zone = 16f;
        assertTrue(StarRules.hastaRaw(mid, 4, 1.0, true) <= mid * 1.5f, "an heir's Hasta fits the 1.5 clamp");
        assertTrue(StarRules.hastaRaw(zone, 5, 1.0, true) <= zone * 1.5f);
        assertTrue(StarRules.hastaRaw(mid, 4, 1.0, true) > StarRules.hastaRaw(mid, 4, 0.0, false));
        assertEquals(StarRules.hastaRaw(mid, 4, 5.0, false), StarRules.hastaRaw(mid, 4, 1.0, false), "mastery counts at most 1");
        assertTrue(StarRules.whipRaw(7f) * StarRules.WHIP_MAX_HITS_PER_TARGET <= 7f * 1.5f, "a full double whip hit fits the clamp");
        assertTrue(StarRules.whipRaw(16f) * 2 <= 16f * 1.5f);
        assertEquals(36f, StarRules.panePool(12f, 4));
        assertTrue(StarRules.panePool(16f, 5) > StarRules.panePool(16f, 4));
        assertEquals(5f, StarRules.paneDamage(10f, true));
        assertEquals(10f, StarRules.paneDamage(10f, false));
    }

    @Test
    void thePropWordsRoundTripAndThePaneYawFacesItsNormal() {
        int node = StarRules.packNode(5, 3.0);
        assertEquals(3.0, StarRules.nodeDrop(node));
        assertEquals(5, node & 15);
        assertEquals(255 / 4.0, StarRules.nodeDrop(StarRules.packNode(0, 9999)));
        int pane = StarRules.packPane(80, 6.0, 4.5);
        assertEquals(80, StarRules.panePercent(pane));
        assertEquals(6.0, StarRules.paneWidth(pane));
        assertEquals(4.5, StarRules.paneHeight(pane));
        assertEquals(100, StarRules.panePercent(StarRules.packPane(500, 1, 1)));
        for (double theta : new double[]{0, 0.7, 1.9, -2.2, 3.0}) {
            double yaw = Math.toRadians(StarRules.entityYawDeg(theta));
            assertEquals(Math.cos(theta), -Math.sin(yaw), 1e-5, "facing x at " + theta);
            assertEquals(Math.sin(theta), Math.cos(yaw), 1e-5, "facing z at " + theta);
        }
    }
}
