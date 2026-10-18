package com.newuniverse.nusmp.heroweapon.weapon.scythe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.weapon.glassworld.FormsRules;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Why: the Scythe's forms file is data nobody can open in a game here, so the rules of docs/heroweapon/design.md section 6 are checked on the file itself (the
 * shared {@link FormsRules}) and the shape the design promises is pinned: five forms in two branches (the Small Scythe toward Scythe 0, the Demon Elephant Scythe
 * toward the Demon Dragon Scythe), the ultimate only on the Demon Dragon Scythe, the blessing-cutting technique only on Scythe 0, and every skill and passive used.
 */
class ScytheFormsTest {
    private static JsonObject file() throws Exception {
        return JsonParser.parseString(Files.readString(Path.of("src/main/resources/data/nusmp/hero/forms/vassal_scythe.json"))).getAsJsonObject();
    }

    private static JsonObject form(JsonObject root, String id) {
        for (JsonElement f : root.getAsJsonArray("forms")) if (f.getAsJsonObject().get("id").getAsString().equals(id)) return f.getAsJsonObject();
        throw new AssertionError("no form " + id);
    }

    @Test
    void theFileObeysTheFormsRules() throws Exception {
        List<String> bad = FormsRules.check(file(), HeroKind.SCYTHE, Set.copyOf(ScytheText.skillIds()), Set.of(ScytheNumbers.SOUL_HARVEST), Set.copyOf(ScytheText.passiveIds()), 3, 5);
        assertTrue(bad.isEmpty(), "forms file problems: " + bad);
    }

    @Test
    void theTreeIsTheTwoBranchesTheDesignPromises() throws Exception {
        JsonObject root = file();
        assertEquals("vassal_scythe", root.get("weapon").getAsString());
        assertEquals(5, root.getAsJsonArray("forms").size());
        assertEquals("base", form(root, "small_scythe").getAsJsonArray("parents").get(0).getAsString());
        assertEquals("base", form(root, "demon_elephant_scythe").getAsJsonArray("parents").get(0).getAsString());
        assertEquals("demon_elephant_scythe", form(root, "demon_dragon_scythe").getAsJsonArray("parents").get(0).getAsString());
        assertEquals("small_scythe", form(root, "scythe_zero").getAsJsonArray("parents").get(0).getAsString());
        assertEquals("Beginner's Scythe", form(root, "base").get("name").getAsString());
    }

    @Test
    void theUltimateAndTheDispelBelongToOneFormEach() throws Exception {
        JsonObject root = file();
        int ultimate = 0, zero = 0;
        for (JsonElement f : root.getAsJsonArray("forms")) {
            JsonObject o = f.getAsJsonObject();
            for (var e : o.getAsJsonObject("skills").entrySet()) {
                if (e.getValue().getAsString().equals(ScytheNumbers.SOUL_HARVEST)) {
                    ultimate++;
                    assertEquals("demon_dragon_scythe", o.get("id").getAsString());
                }
                if (e.getValue().getAsString().equals(ScytheNumbers.SCYTHE_ZERO)) {
                    zero++;
                    assertEquals("scythe_zero", o.get("id").getAsString());
                }
            }
            assertTrue(o.getAsJsonArray("passives").contains(new JsonPrimitive("soul_tithe")), o.get("id").getAsString() + " gathers souls");
        }
        assertEquals(1, ultimate);
        assertEquals(1, zero);
        assertTrue(form(root, "demon_dragon_scythe").getAsJsonArray("passives").contains(new JsonPrimitive("dragon_soul")));
        assertTrue(form(root, "scythe_zero").getAsJsonArray("passives").contains(new JsonPrimitive("null_edge")));
    }

    @Test
    void theHeavyFormTradesSpeedForPowerAndTheSmallOneTheOtherWay() throws Exception {
        JsonObject root = file();
        JsonObject small = form(root, "small_scythe").getAsJsonObject("stats"), heavy = form(root, "demon_elephant_scythe").getAsJsonObject("stats");
        assertTrue(small.get("attack_speed").getAsDouble() > heavy.get("attack_speed").getAsDouble(), "the small scythe swings faster");
        assertTrue(heavy.get("attack_damage").getAsDouble() > small.get("attack_damage").getAsDouble(), "the heavy scythe hits harder");
        double smallReach = small.has("reach") ? small.get("reach").getAsDouble() : 0.0;
        assertTrue(heavy.get("reach").getAsDouble() > smallReach, "the heavy scythe reaches farther");
    }
}
