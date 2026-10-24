package com.newuniverse.nusmp.star;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: stars sometimes die to Anti-Magic (owner decision, phase 2), and the numbers that decide it must stay inside their bounds on a public server (rules 9 and 11): the chance is clamped to 0..1 whatever a
 * hand-edited server file says, a pane and its end-stars are rolled higher and an heir lower, a star that survived a roll is left alone for a while (so a lingering Nihility zone cannot wear a star down in a
 * second), one dispel breaks at most eight things, and the registry reads only look at stars that are really inside the sphere of the dispel. The hook itself is one call inside the mod's dispel and
 * the work is a pass over the in-memory registries (no entity query); the source scan pins both.
 */
class StarDispelTest {
    private static final double EPS = 1e-9;

    @Test
    void theChanceIsClampedAndTheDefaultIsThirtyFivePercent() {
        assertEquals(0.35, StarDispelRules.DEFAULT_CHANCE, EPS);
        assertEquals(0.0, StarDispelRules.clampChance(-3), EPS);
        assertEquals(1.0, StarDispelRules.clampChance(7), EPS);
        assertEquals(StarDispelRules.DEFAULT_CHANCE, StarDispelRules.clampChance(Double.NaN), EPS);
        assertEquals(0.2, StarDispelRules.clampChance(0.2), EPS);
    }

    @Test
    void aPaneAndItsEndStarsAreRolledAboutSixtyPercentAnHeirHalf() {
        double base = StarDispelRules.DEFAULT_CHANCE;
        assertEquals(0.35, StarDispelRules.chance(base, StarDispelRules.Role.NODE, false), EPS);
        assertEquals(0.60, StarDispelRules.chance(base, StarDispelRules.Role.PANE, false), 1e-6);
        assertEquals(0.175, StarDispelRules.chance(base, StarDispelRules.Role.NODE, true), EPS);
        assertEquals(0.30, StarDispelRules.chance(base, StarDispelRules.Role.PANE, true), 1e-6);
        assertTrue(StarDispelRules.chance(base, StarDispelRules.Role.PANE, false) > StarDispelRules.chance(base, StarDispelRules.Role.NODE, false));
        assertTrue(StarDispelRules.chance(base, StarDispelRules.Role.NODE, true) < StarDispelRules.chance(base, StarDispelRules.Role.NODE, false));
    }

    @Test
    void noSettingCanPushAChanceOutsideZeroToOne() {
        for (double base : new double[]{-5, 0, 0.35, 0.9, 1, 50, Double.NaN, Double.POSITIVE_INFINITY}) {
            for (StarDispelRules.Role role : StarDispelRules.Role.values()) {
                for (boolean heir : new boolean[]{false, true}) {
                    double c = StarDispelRules.chance(base, role, heir);
                    assertTrue(c >= 0.0 && c <= 1.0, "chance " + c + " for base " + base);
                }
            }
        }
        assertEquals(0.0, StarDispelRules.chance(0, StarDispelRules.Role.PANE, false), EPS, "0 means stars never die to Anti-Magic");
        assertEquals(1.0, StarDispelRules.chance(1, StarDispelRules.Role.PANE, false), EPS);
    }

    @Test
    void aRollBreaksOnlyBelowTheChance() {
        assertFalse(StarDispelRules.breaks(0.0, 0.0));
        assertTrue(StarDispelRules.breaks(1.0, 0.999999));
        assertTrue(StarDispelRules.breaks(0.35, 0.34));
        assertFalse(StarDispelRules.breaks(0.35, 0.35));
        assertFalse(StarDispelRules.breaks(0.35, 0.9));
    }

    @Test
    void aThousandRollsLandNearTheChance() {
        Random rnd = new Random(7);
        int broke = 0, n = 20000;
        for (int i = 0; i < n; i++) if (StarDispelRules.breaks(0.35, rnd.nextDouble())) broke++;
        double share = broke / (double) n;
        assertTrue(share > 0.33 && share < 0.37, "share " + share);
    }

    @Test
    void aStarThatSurvivedIsLeftAloneForFortyTicksAndTheTableIsBounded() {
        StarDispelRules.Grace g = new StarDispelRules.Grace();
        UUID a = UUID.randomUUID();
        assertTrue(g.ready(a, 100));
        g.survived(a, 100);
        assertFalse(g.ready(a, 100));
        assertFalse(g.ready(a, 100 + StarDispelRules.GRACE_TICKS - 1));
        assertTrue(g.ready(a, 100 + StarDispelRules.GRACE_TICKS));
        g.forget(a);
        assertTrue(g.ready(a, 101));
        for (int i = 0; i < StarDispelRules.GRACE_CAP * 3; i++) g.survived(UUID.randomUUID(), 5);
        assertTrue(g.size() <= StarDispelRules.GRACE_CAP, "the table is bounded: " + g.size());
        for (int i = 0; i < 10; i++) g.survived(UUID.randomUUID(), 5 + 1000);
        assertTrue(g.size() <= StarDispelRules.GRACE_CAP, "expired entries are dropped when it fills");
        g.clear();
        assertEquals(0, g.size());
    }

    @Test
    void theRegistryReadFindsOnlyLiveStarsInsideTheSphereOfTheSameDimension() {
        StarNodes n = new StarNodes();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        n.add(a, new StarNodes.Node(UUID.randomUUID(), "o", 1, 64, 0, n.nextSeq(), 1000), 6, 128);
        n.add(a, new StarNodes.Node(UUID.randomUUID(), "o", 30, 64, 0, n.nextSeq(), 1000), 6, 128);
        n.add(b, new StarNodes.Node(UUID.randomUUID(), "o", 2, 64, 0, n.nextSeq(), 1000), 6, 128);
        n.add(b, new StarNodes.Node(UUID.randomUUID(), "nether", 2, 64, 0, n.nextSeq(), 1000), 6, 128);
        n.add(b, new StarNodes.Node(UUID.randomUUID(), "o", 3, 64, 0, n.nextSeq(), 5), 6, 128);
        List<StarNodes.Owned> hit = n.near("o", 0, 64, 0, 5, 10, 16);
        assertEquals(2, hit.size(), "two live stars of the overworld within 5 blocks: " + hit);
        assertEquals(2, n.near("o", 0, 64, 0, 5, 10, 16).stream().map(StarNodes.Owned::owner).distinct().count());
        assertEquals(1, n.near("o", 0, 64, 0, 5, 10, 1).size(), "the limit holds");
        assertEquals(3, n.near("o", 0, 64, 0, 40, 10, 16).size());
        assertEquals(0, n.near("o", 0, 64, 0, 5, 10, 0).size());
        assertEquals(0, new StarNodes().near("o", 0, 64, 0, 5, 10, 16).size(), "an empty registry costs nothing");
        assertEquals(5, n.total(), "reading removes nothing");
    }

    @Test
    void thePaneBookFindsPanesNearTheDispelAndTheEndStarsThatHoldThem() {
        StarPaneBook book = new StarPaneBook();
        StarMath.Pane geom = StarMath.pane(List.of(new StarMath.P(0, 2, 0), new StarMath.P(4, 2, 0), new StarMath.P(4, 5, 0), new StarMath.P(0, 5, 0)), new StarMath.P(2, 2, 8), 6, 5);
        UUID owner = UUID.randomUUID(), other = UUID.randomUUID();
        book.add(UUID.randomUUID(), owner, "o", geom, 10f, 100);
        assertEquals(1, book.within("o", 2, 3, 0, 1).size());
        assertEquals(1, book.within("o", 2, 3, 4, 5).size(), "a dispel a few blocks away reaches the pane");
        assertEquals(0, book.within("o", 50, 3, 0, 5).size());
        assertEquals(0, book.within("nether", 2, 3, 0, 5).size());
        assertTrue(book.isEndStar(owner, "o", 0, 2, 0, StarDispelRules.END_STAR_TOLERANCE), "a corner star of the pane");
        assertTrue(book.isEndStar(owner, "o", 4, 5, 0, StarDispelRules.END_STAR_TOLERANCE));
        assertFalse(book.isEndStar(owner, "o", 20, 2, 0, StarDispelRules.END_STAR_TOLERANCE), "a free star far away");
        assertFalse(book.isEndStar(other, "o", 0, 2, 0, StarDispelRules.END_STAR_TOLERANCE), "only the pane's own owner's stars hold it");
        assertFalse(book.isEndStar(owner, "nether", 0, 2, 0, StarDispelRules.END_STAR_TOLERANCE));
    }

    @Test
    void boxDistanceIsZeroInsideAndEuclideanOutside() {
        double[] box = {0, 0, 0, 2, 2, 2};
        assertEquals(0.0, StarDispelRules.boxDistSq(box, 1, 1, 1), EPS);
        assertEquals(9.0, StarDispelRules.boxDistSq(box, 5, 1, 1), EPS);
        assertEquals(3.0, StarDispelRules.boxDistSq(box, 3, 3, 3), EPS);
        assertEquals(1.0, StarDispelRules.boxDistSq(box, 1, -1, 1), EPS);
    }

    @Test
    void aDispelNeverBreaksMoreThanEightThingsOrLooksAtMoreThanSixteenStars() {
        assertEquals(8, StarDispelRules.MAX_BREAKS);
        assertEquals(16, StarDispelRules.MAX_CANDIDATES);
        assertTrue(StarDispelRules.MAX_BREAKS <= 10, "the shatter packets stay inside one cast budget of ten (StarFx.CAST_PACKETS)");
    }

    // ------------------------------------------------------------------------------------------------ the source
    private static final Path JAVA = Path.of("").toAbsolutePath().resolve("src/main/java/com/newuniverse/nusmp");

    private static String code(String rel) throws IOException {
        return Files.readString(JAVA.resolve(rel), StandardCharsets.UTF_8).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\n]*", "");
    }

    @Test
    void theHookIsOneCallInsideTheModsDispelAndNeedsNoTickListenerOrEntityQuery() throws IOException {
        String nul = code("antimagic/Nullification.java");
        assertEquals(1, nul.split("StarDispel\\.onDispel", -1).length - 1, "one hook call");
        int at = nul.indexOf("StarDispel.onDispel"), method = nul.indexOf("public static int shatterBarriers");
        assertTrue(method >= 0 && at > method && at < nul.indexOf("new AABB(c, c)"), "the hook runs at the start of shatterBarriers, before its own entity query");
        String d = code("star/StarDispel.java");
        assertFalse(d.contains("getEntities") || d.contains("getEntitiesOfClass") || d.contains("SubscribeEvent") || d.contains("TickEvent") || d.contains("ServerTickEvent"), "no entity query and no listener");
        assertTrue(d.contains("StarFx.send(budget"), "the shatter goes through StarFx and its budget");
        assertTrue(d.contains("MAX_BREAKS") && d.contains("MAX_CANDIDATES"), "bounded");
        assertTrue(d.contains("friendly(level"), "the caster's and allies' stars are spared");
        assertTrue(d.contains("StarConfig.nodes()"), "star.nodes off is a no-op");
        String cfg = code("star/StarConfig.java");
        assertTrue(cfg.contains("StarDispelRules.CONFIG_KEY") && cfg.contains("DISPEL_CHANCE") && cfg.contains("clampChance"), "the server setting is clamped when read");
        assertEquals("dispelChance", StarDispelRules.CONFIG_KEY);
    }
}
