package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: which file a placed piece is built from is the whole interface between the plan (which never changes) and the rebuilt rooms, so the pure choice is pinned here: the ids of the
 * variants, the table, determinism by seed, and that the first copies of one template in a plan get different variants.
 */
class RoomVariantsTest {
    private static RoomVariants.Table table() {
        return RoomVariants.parse(JsonParser.parseString("{\"variants\":{\"mega/hall/library_a\":4,\"mega/hall/crypt_b\":3,\"mega/corridor/bend_1x1_a\":4,\"mega/bad/zero\":0,\"mega/bad/many\":9},"
                + "\"stand_rooms\":[\"mega/hall/armory_forge_a\"]}"));
    }

    @Test
    void theIdOfAVariantKeepsTheNamespaceAndMovesTheFolder() {
        assertEquals("nusmp:mega_r/hall/library_a_v2", RoomVariants.variantId("nusmp:mega/hall/library_a", 2));
        assertEquals("mega_r/corridor/bend_1x1_a_v4", RoomVariants.variantId("mega/corridor/bend_1x1_a", 4));
        assertEquals("nusmp:hub/plaza", RoomVariants.variantId("nusmp:hub/plaza", 1), "a template outside mega/ has no variant");
        assertTrue(RoomVariants.isVariantId("nusmp:mega_r/hall/library_a_v2"));
        assertFalse(RoomVariants.isVariantId("nusmp:mega/hall/library_a"));
        assertEquals("nusmp:mega/hall/library_a", RoomVariants.shippedOf("nusmp:mega_r/hall/library_a_v2"));
        assertEquals("mega/corridor/bend_1x1_a", RoomVariants.shippedOf("mega_r/corridor/bend_1x1_a_v4"));
        assertEquals("nusmp:mega/hall/library_a", RoomVariants.shippedOf("nusmp:mega/hall/library_a"), "a shipped id comes back unchanged");
        for (int k = 1; k <= RoomVariants.MAX; k++) assertEquals("nusmp:mega/hall/x", RoomVariants.shippedOf(RoomVariants.variantId("nusmp:mega/hall/x", k)));
    }

    @Test
    void theTableReadsCountsAndIgnoresNonsense() {
        RoomVariants.Table t = table();
        assertEquals(4, t.count("nusmp:mega/hall/library_a"));
        assertEquals(4, t.count("mega/hall/library_a"), "the namespace does not matter");
        assertEquals(0, t.count("mega/bad/zero"));
        assertEquals(0, t.count("mega/bad/many"), "more than MAX variants is not a table entry");
        assertEquals(0, t.count("mega/hall/unknown"));
        assertTrue(t.standRoom("nusmp:mega/hall/armory_forge_a"));
        assertFalse(t.standRoom("mega/hall/library_a"));
        assertEquals(3, t.size());
        assertEquals(0, RoomVariants.parse(null).size());
        assertEquals(0, RoomVariants.parse(JsonParser.parseString("[1,2]")).size());
        assertEquals(0, RoomVariants.Table.empty().size());
    }

    @Test
    void theRealTableIsReadFromTheClassPath() {
        RoomVariants.Table t = RoomVariants.table();
        assertTrue(t.size() >= 100, "the shipped table lists the rebuilt rooms: " + t.size() + " (" + t.problem() + ")");
        assertEquals("", t.problem());
    }

    @Test
    void theAssignmentIsDeterministicBySeed() {
        List<String> ids = List.of("nusmp:mega/hall/library_a", "nusmp:mega/corridor/bend_1x1_a", "nusmp:mega/landing/landing_a", "nusmp:mega/hall/library_a", "nusmp:mega/hall/crypt_b");
        String[] a = RoomVariants.assign(ids, 42L, table()), b = RoomVariants.assign(ids, 42L, table());
        assertEquals(java.util.Arrays.asList(a), java.util.Arrays.asList(b));
        assertEquals("nusmp:mega/landing/landing_a", a[2], "a piece with no variants keeps its template");
        boolean differs = false;
        for (long seed = 0; seed < 50 && !differs; seed++) differs = !java.util.Arrays.equals(a, RoomVariants.assign(ids, seed, table()));
        assertTrue(differs, "the seed must matter");
        for (String s : a) assertFalse(s.isEmpty());
    }

    @Test
    void theFirstCopiesOfOneTemplateGetDifferentVariants() {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < 8; i++) ids.add("nusmp:mega/hall/library_a");
        for (long seed = 0; seed < 200; seed++) {
            String[] out = RoomVariants.assign(ids, seed, table());
            Set<String> firstFour = new HashSet<>(List.of(out).subList(0, 4));
            assertEquals(4, firstFour.size(), "seed " + seed + ": four copies of a template with four variants all differ: " + List.of(out));
            for (String s : out) assertTrue(s.matches("nusmp:mega_r/hall/library_a_v[1-4]"), s);
        }
        String[] three = RoomVariants.assign(List.of("mega/hall/crypt_b", "mega/hall/crypt_b", "mega/hall/crypt_b"), 7L, table());
        assertEquals(3, new HashSet<>(List.of(three)).size());
    }

    @Test
    void everyVariantIsUsedAcrossManySeeds() {
        Map<String, Integer> seen = new java.util.TreeMap<>();
        for (long seed = 0; seed < 400; seed++) {
            String[] out = RoomVariants.assign(List.of("mega/hall/library_a"), seed, table());
            seen.merge(out[0], 1, Integer::sum);
        }
        assertEquals(4, seen.size(), "all four variants appear: " + seen);
        for (int n : seen.values()) assertTrue(n > 60 && n < 140, "roughly even: " + seen);
        assertNotEquals(0, seen.size());
    }
}
