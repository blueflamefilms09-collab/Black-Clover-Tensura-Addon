package com.newuniverse.nusmp.skilltree.loot;

import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import com.newuniverse.nusmp.skilltree.data.RouteSchema;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: {@link RouteTable} is the glue between the registry and the weight table: the table of the registry version in force, built once per version. The promises that matter with 283 lines:
 * the table has exactly the routes of positive weight and their total; a thousand reads do not rebuild it; a change of the registry (a datapack reload, a client sync, a clear) is seen on the very
 * next read with no invalidation call; and the lines are drawn in proportion to their loot_weight. Routes are made from json through the real schema (not a constructor), so this test does not
 * care when the route record gains fields. The registry is static state: every test clears the client copy at the end.
 */
class RouteTableTest {
    /** zero-padded so that the registry (which keeps routes in id order) and the list index agree */
    private static String pad(int i) { return String.format("%04d", i); }

    private static SkillRoute route(int i, int weight) {
        String json = "{\"name\":\"Line " + i + "\",\"book\":\"nusmp:rt" + pad(i) + "_book\",\"class_id\":\"rt" + pad(i) + "\",\"color\":\"#112233\",\"loot_weight\":" + weight + ",\"nodes\":[{\"id\":\"a\"}]}";
        SkillRoute r = RouteSchema.parse("nusmp:rt" + pad(i), json).route();
        assertNotNull(r, "the test route must parse");
        assertEquals(weight, r.lootWeight());
        return r;
    }

    private static List<SkillRoute> lines(int n) {
        List<SkillRoute> out = new ArrayList<>();
        int[] hand = {3, 2, 2, 2, 2, 2, 1, 2};
        for (int i = 0; i < n; i++) out.add(route(i, i < hand.length ? hand[i] : 1));
        return out;
    }

    @Test
    void theTableHasTheRoutesOfPositiveWeightOnly() {
        RouteRegistry.clear();
        try {
            List<SkillRoute> routes = lines(283);
            routes.set(10, route(10, 0));
            routes.set(11, route(11, 0));
            RouteRegistry.setClient(routes);
            RouteTable.Snap s = RouteTable.current();
            assertEquals(281, s.table().size());
            long total = 0;
            for (SkillRoute r : routes) total += r.lootWeight();
            assertEquals(total, s.table().total());
            assertEquals(283, s.routes().length);
            assertEquals(0, s.table().weightOf(10), "a weight-0 line cannot drop");
            assertNull(s.at(-1));
            assertNull(s.at(283));
        } finally {
            RouteRegistry.clearClient();
        }
    }

    @Test
    void aThousandReadsBuildOnceAndAChangeIsSeenAtOnce() {
        RouteRegistry.clear();
        try {
            RouteRegistry.setClient(lines(283));
            RouteTable.Snap first = RouteTable.current();
            int builds = RouteTable.builds();
            for (int i = 0; i < 1000; i++) assertSame(first, RouteTable.current());
            assertEquals(builds, RouteTable.builds(), "no rebuild while the registry version is the same");
            RouteRegistry.setClient(lines(100));
            RouteTable.Snap second = RouteTable.current();
            assertEquals(100, second.table().size(), "a new registry version is seen on the very next read");
            assertEquals(builds + 1, RouteTable.builds());
            RouteRegistry.clearClient();
            assertTrue(RouteTable.current().table().isEmpty(), "no routes: an empty table, which drops no book");
        } finally {
            RouteRegistry.clearClient();
        }
    }

    @Test
    void linesAreDrawnInProportionToTheirLootWeight() {
        RouteRegistry.clear();
        try {
            List<SkillRoute> routes = lines(283);
            RouteRegistry.setClient(routes);
            RouteTable.Snap s = RouteTable.current();
            Random r = new Random(1982);
            int n = 400_000;
            int[] hits = new int[283];
            for (int i = 0; i < n; i++) hits[s.table().pick(r::nextDouble)]++;
            long total = s.table().total();
            for (int i : new int[] {0, 1, 6, 150}) {
                double p = routes.get(i).lootWeight() / (double) total, sigma = Math.sqrt(n * p * (1 - p));
                assertTrue(Math.abs(hits[i] - n * p) < 5 * sigma, "line " + i + ": " + hits[i] + " of " + n + ", expected " + n * p);
                assertSame(routes.get(i), s.at(i), "table index and registry order agree");
            }
        } finally {
            RouteRegistry.clearClient();
        }
    }
}
