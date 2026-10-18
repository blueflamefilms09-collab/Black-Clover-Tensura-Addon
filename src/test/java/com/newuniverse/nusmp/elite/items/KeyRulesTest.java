package com.newuniverse.nusmp.elite.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.elite.items.crate.CrateTier;
import com.newuniverse.nusmp.elite.items.crate.KeyRules;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Why: the key loot modifier's numbers are data and the decision is pure, so both are pinned here: the chance is the file's value times the scale, kept in 0..1 (0 switches a tier off); each tier rolls
 * on its own; a table with no entry pays nothing; and the shipped file lists only known tiers, chances in 0..1 and each tier getting rarer as it gets better in the tables that give all three.
 */
class KeyRulesTest {
    @Test
    void chanceIsScaledAndClamped() {
        Map<String, Double> m = Map.of("common", 0.4, "rare", 0.8);
        assertEquals(0.4, KeyRules.chance(m, "common", 1.0), 1e-9);
        assertEquals(0.8, KeyRules.chance(m, "common", 2.0), 1e-9);
        assertEquals(1.0, KeyRules.chance(m, "rare", 2.0), 1e-9);
        assertEquals(0.0, KeyRules.chance(m, "elite", 5.0), 1e-9);
        assertEquals(0.0, KeyRules.chance(m, "common", 0.0), 1e-9);
        assertEquals(0.0, KeyRules.chance(null, "common", 1.0), 1e-9);
        assertEquals(0.0, KeyRules.chance(m, "common", Double.NaN), 1e-9);
    }

    @Test
    void eachTierRollsOnItsOwn() {
        Map<String, Double> m = Map.of("common", 1.0, "rare", 0.5, "elite", 0.1);
        assertEquals(List.of(CrateTier.COMMON, CrateTier.RARE, CrateTier.ELITE), KeyRules.roll(m, 1.0, () -> 0.0));
        assertEquals(List.of(CrateTier.COMMON), KeyRules.roll(m, 1.0, () -> 0.99));
        assertEquals(List.of(), KeyRules.roll(Map.of(), 1.0, () -> 0.0));
        Random r = new Random(1);
        int hits = 0;
        for (int i = 0; i < 20000; i++) if (KeyRules.roll(Map.of("elite", 0.1), 1.0, r::nextDouble).size() == 1) hits++;
        assertTrue(hits > 1700 && hits < 2300, "an elite chance of 0.1 should hit about 2000 of 20000, got " + hits);
    }

    @Test
    void shippedFileIsWellFormed() throws Exception {
        try (InputStream in = KeyRulesTest.class.getResourceAsStream("/data/nusmp/loot_modifiers/elite_crate_keys.json")) {
            assertNotNull(in);
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals("nusmp:elite_crate_keys", root.get("type").getAsString());
            assertTrue(root.has("conditions"));
            JsonObject tables = root.getAsJsonObject("tables");
            assertTrue(tables.size() >= 6);
            for (String table : tables.keySet()) {
                assertTrue(table.contains(":"), table);
                JsonObject t = tables.getAsJsonObject(table);
                for (String tier : t.keySet()) {
                    assertNotNull(CrateTier.byId(tier), table + " names an unknown tier " + tier);
                    double c = t.get(tier).getAsDouble();
                    assertTrue(c > 0 && c <= 1, table + "." + tier + " = " + c);
                }
                if (t.has("common") && t.has("rare")) assertTrue(t.get("rare").getAsDouble() <= t.get("common").getAsDouble(), table + ": rare keys must not be commoner than common keys");
                if (t.has("rare") && t.has("elite")) assertTrue(t.get("elite").getAsDouble() <= t.get("rare").getAsDouble(), table + ": elite keys must not be commoner than rare keys");
            }
        }
    }

    @Test
    void registeredModifierIsListed() throws Exception {
        try (InputStream in = KeyRulesTest.class.getResourceAsStream("/data/neoforge/loot_modifiers/global_loot_modifiers.json")) {
            assertNotNull(in);
            String s = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(s.contains("\"nusmp:elite_crate_keys\""), "global_loot_modifiers.json must list the key modifier");
            assertTrue(s.contains("\"nusmp:rift_books\""), "the older modifiers must still be listed");
        }
    }
}
