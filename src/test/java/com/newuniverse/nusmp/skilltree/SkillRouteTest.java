package com.newuniverse.nusmp.skilltree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillNode.Parent;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Why: the route and node records carry the arithmetic every other part leans on (what a level costs, what a whole route costs, which node grants which skill, how the per-level tables are read), so their
 * edges are pinned here without a server: a flat price, a per-level price list that repeats its last entry, a table read past its end, a node that grants nothing, and the storage key format that the
 * saved tree and the packets share.
 */
class SkillRouteTest {
    private static SkillNode node(String id, List<Parent> parents, int cost, List<Integer> costs, int max, String skill, Map<String, List<Double>> values) {
        return new SkillNode(id, id, "", parents, cost, costs, max, skill, List.of(), values, 0, 0, "");
    }

    private static SkillRoute route() {
        SkillNode fist = node("iron_fist", List.of(), 1, List.of(), 3, "nusmp:test_fist", Map.of("damage", List.of(6.0, 8.0, 10.0)));
        SkillNode roar = node("roar", List.of(new Parent("iron_fist", 2)), 2, List.of(2, 3, 5), 3, "nusmp:test_roar", Map.of());
        SkillNode appetite = node("appetite", List.of(new Parent("iron_fist", 1)), 1, List.of(), 1, "", Map.of());
        return new SkillRoute("nusmp:ember_knight", "black_clover", "Ember Knight", "", "nusmp:ember_knight_book", "Fang of Smoldering Insanity", "", 0xE8541C, "minecraft:blaze_powder",
                "ember_knight", 3, 1, List.of(fist, roar, appetite));
    }

    @Test
    void pricesFlatAndPerLevel() {
        SkillRoute r = route();
        assertEquals(1, r.node("iron_fist").costOf(1));
        assertEquals(1, r.node("iron_fist").costOf(3));
        assertEquals(3, r.node("iron_fist").totalCost(3));
        assertEquals(2, r.node("roar").costOf(1));
        assertEquals(5, r.node("roar").costOf(3));
        assertEquals(5, r.node("roar").costOf(9), "a level past the list repeats the last price");
        assertEquals(10, r.node("roar").totalCost(3));
        assertEquals(3 + 10 + 1, r.totalCost());
    }

    @Test
    void valuesRepeatTheirLastEntryAndFallBack() {
        SkillNode fist = route().node("iron_fist");
        assertEquals(6.0, fist.value("damage", 1, -1), 1e-9);
        assertEquals(10.0, fist.value("damage", 3, -1), 1e-9);
        assertEquals(10.0, fist.value("damage", 7, -1), 1e-9);
        assertEquals(6.0, fist.value("damage", 0, -1), 1e-9, "level 0 reads the first entry");
        assertEquals(42.0, fist.value("range", 1, 42.0), 1e-9);
    }

    @Test
    void lookupsAndKeys() {
        SkillRoute r = route();
        assertNotNull(r.node("roar"));
        assertNull(r.node("nothing"));
        assertEquals("nusmp:test_roar", r.node("roar").skill());
        assertEquals(r.node("roar"), r.bySkill("nusmp:test_roar"));
        assertNull(r.bySkill("nusmp:nothing"));
        assertTrue(!r.node("appetite").grantsSkill());
        assertEquals(1, r.roots().size());
        assertEquals("iron_fist", r.roots().get(0).id());
        assertEquals("nusmp:ember_knight#roar", SkillRoute.key(r.id(), "roar"));
    }
}
