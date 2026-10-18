package com.newuniverse.nusmp.heroweapon.weapon.katana;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
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
 * Why: the Katana's forms file is data nobody can open in a game here, so the rules of docs/heroweapon/design.md section 6 are checked on the file itself (the
 * shared {@link FormsRules}) and the shape the design promises is pinned: five forms with a diamond at the top (the Sakura Stone Katana needs BOTH the Siderite
 * Katana and the Soul Eater Katana), the ultimate only on the top form, the riposte only where a parry is, the phantom blade where the White Tiger line is.
 */
class KatanaFormsTest {
    private static JsonObject file() throws Exception {
        return JsonParser.parseString(Files.readString(Path.of("src/main/resources/data/nusmp/hero/forms/vassal_katana.json"))).getAsJsonObject();
    }

    private static JsonObject form(JsonObject root, String id) {
        for (JsonElement f : root.getAsJsonArray("forms")) if (f.getAsJsonObject().get("id").getAsString().equals(id)) return f.getAsJsonObject();
        throw new AssertionError("no form " + id);
    }

    private static boolean lists(JsonObject form, String skill) {
        for (var e : form.getAsJsonObject("skills").entrySet()) if (e.getValue().getAsString().equals(skill)) return true;
        return false;
    }

    @Test
    void theFileObeysTheFormsRules() throws Exception {
        List<String> bad = FormsRules.check(file(), HeroKind.KATANA, Set.copyOf(KatanaText.skillIds()), Set.of(KatanaNumbers.SAKURA_DANCE), Set.copyOf(KatanaText.passiveIds()), 3, 5);
        assertTrue(bad.isEmpty(), "forms file problems: " + bad);
    }

    @Test
    void theTreeHasADiamondAtTheTop() throws Exception {
        JsonObject root = file();
        assertEquals("vassal_katana", root.get("weapon").getAsString());
        assertEquals(5, root.getAsJsonArray("forms").size());
        JsonArray top = form(root, "sakura_stone_katana").getAsJsonArray("parents");
        assertEquals(2, top.size());
        assertTrue(top.contains(new JsonPrimitive("siderite_katana")) && top.contains(new JsonPrimitive("soul_eater_katana")));
        assertEquals("white_tiger_katana", form(root, "siderite_katana").getAsJsonArray("parents").get(0).getAsString());
        assertEquals(3, form(root, "sakura_stone_katana").get("tier").getAsInt());
    }

    @Test
    void theUltimateIsOnTheTopFormAndTheSoulSliceOnTheSoulEater() throws Exception {
        JsonObject root = file();
        for (JsonElement f : root.getAsJsonArray("forms")) {
            JsonObject o = f.getAsJsonObject();
            String id = o.get("id").getAsString();
            assertEquals(id.equals("sakura_stone_katana"), lists(o, KatanaNumbers.SAKURA_DANCE), id + " and the ultimate");
            assertEquals(id.equals("soul_eater_katana"), lists(o, KatanaNumbers.SOUL_SLICE), id + " and the soul slice");
            assertTrue(o.getAsJsonArray("passives").contains(new JsonPrimitive("iai_tempo")), id + " has the tempo");
            assertEquals(lists(o, KatanaNumbers.COUNTER_STANCE) && (id.equals("soul_eater_katana") || id.equals("sakura_stone_katana")),
                    o.getAsJsonArray("passives").contains(new JsonPrimitive("riposte")), id + ": the riposte goes with the forms built around the parry");
        }
        assertTrue(lists(form(root, "white_tiger_katana"), KatanaNumbers.DOUBLE_SWORD));
        assertTrue(lists(form(root, "siderite_katana"), KatanaNumbers.STARDUST_BLADE));
        assertTrue(lists(form(root, "base"), KatanaNumbers.COUNTER_STANCE) && lists(form(root, "base"), KatanaNumbers.RELEASE_EVADE));
    }
}
