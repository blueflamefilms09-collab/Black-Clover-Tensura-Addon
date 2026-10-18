package com.newuniverse.nusmp.heroweapon.weapon.fan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.weapon.glassworld.FormsRules;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Why: the Fan's forms file is data nobody can open in a game here, so the rules of docs/heroweapon/design.md section 6 are checked on the file itself (the
 * shared {@link FormsRules}) and the shape the design promises is pinned: five forms in two branches (the White Tiger Fan toward the Demon Dragon Fan, the
 * Keystone Fan toward the Wave Sealing Sword), the ultimate only on the Demon Dragon Fan, every skill and every passive of the weapon used by some form.
 */
class FanFormsTest {
    private static JsonObject file() throws Exception {
        return JsonParser.parseString(Files.readString(Path.of("src/main/resources/data/nusmp/hero/forms/vassal_fan.json"))).getAsJsonObject();
    }

    private static JsonObject form(JsonObject root, String id) {
        for (JsonElement f : root.getAsJsonArray("forms")) if (f.getAsJsonObject().get("id").getAsString().equals(id)) return f.getAsJsonObject();
        throw new AssertionError("no form " + id);
    }

    @Test
    void theFileObeysTheFormsRules() throws Exception {
        List<String> bad = FormsRules.check(file(), HeroKind.FAN, Set.copyOf(FanText.skillIds()), Set.of(FanNumbers.SNOW_MOON_FLOWER), Set.copyOf(FanText.passiveIds()), 3, 5);
        assertTrue(bad.isEmpty(), "forms file problems: " + bad);
    }

    @Test
    void theTreeIsTheTwoBranchesTheDesignPromises() throws Exception {
        JsonObject root = file();
        assertEquals("vassal_fan", root.get("weapon").getAsString());
        assertEquals(5, root.getAsJsonArray("forms").size());
        assertEquals("base", form(root, "white_tiger_fan").getAsJsonArray("parents").get(0).getAsString());
        assertEquals("base", form(root, "keystone_fan").getAsJsonArray("parents").get(0).getAsString());
        assertEquals("white_tiger_fan", form(root, "demon_dragon_fan").getAsJsonArray("parents").get(0).getAsString());
        assertEquals("keystone_fan", form(root, "wave_sealing_sword").getAsJsonArray("parents").get(0).getAsString());
        assertEquals(2, form(root, "demon_dragon_fan").get("tier").getAsInt());
    }

    @Test
    void onlyTheDemonDragonFanHasTheUltimateAndEveryFormHasTheSweep() throws Exception {
        JsonObject root = file();
        int withUltimate = 0;
        for (JsonElement f : root.getAsJsonArray("forms")) {
            JsonObject o = f.getAsJsonObject();
            JsonObject skills = o.getAsJsonObject("skills");
            assertEquals(FanNumbers.FLOWER_WIND, skills.get("use").getAsString(), o.get("id").getAsString() + " opens with the sweep");
            for (var e : skills.entrySet()) {
                if (e.getValue().getAsString().equals(FanNumbers.SNOW_MOON_FLOWER)) {
                    withUltimate++;
                    assertEquals("demon_dragon_fan", o.get("id").getAsString());
                    assertEquals("key3", e.getKey());
                }
            }
        }
        assertEquals(1, withUltimate);
    }

    @Test
    void everyFormKeepsTheCommonPassivesAndTheBranchPassivesBelongToTheirBranch() throws Exception {
        JsonObject root = file();
        for (JsonElement f : root.getAsJsonArray("forms")) {
            JsonArray p = f.getAsJsonObject().getAsJsonArray("passives");
            assertTrue(p.contains(new com.google.gson.JsonPrimitive("featherfall")) && p.contains(new com.google.gson.JsonPrimitive("residue")));
        }
        assertTrue(form(root, "demon_dragon_fan").getAsJsonArray("passives").contains(new com.google.gson.JsonPrimitive("dragon_surge")));
        assertTrue(form(root, "wave_sealing_sword").getAsJsonArray("passives").contains(new com.google.gson.JsonPrimitive("sealing_tide")));
        assertTrue(form(root, "keystone_fan").getAsJsonArray("passives").contains(new com.google.gson.JsonPrimitive("keystone_ward")));
        assertTrue(form(root, "white_tiger_fan").getAsJsonArray("passives").contains(new com.google.gson.JsonPrimitive("tiger_pounce")));
    }
}
