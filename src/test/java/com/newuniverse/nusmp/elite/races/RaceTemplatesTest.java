package com.newuniverse.nusmp.elite.races;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.elite.races.data.RaceTemplate;
import com.newuniverse.nusmp.elite.races.data.RaceTemplateParser;
import com.newuniverse.nusmp.elite.races.data.RaceTemplates;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: a datapack can replace a race file at any {@code /reload}, so the registry that holds the templates must survive a bad file (the race keeps its bundled version and the
 * reason is reported), must take a good one at once, and the line check must catch a broken chain (an evolution into a race that is not there, a skipped tier, a cycle) before
 * a player is stuck on it.
 */
class RaceTemplatesTest {
    private static JsonObject file(String id) {
        try (var in = RaceTemplates.class.getResourceAsStream(RaceTemplates.DIR + id + ".json")) {
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void aBadReplacementKeepsTheBundledRaceAndAGoodOneTakesEffect() {
        assertTrue(RaceTemplates.loadBundled().isEmpty());
        RaceTemplate original = RaceTemplates.get("lesser_saiyan");
        assertNotNull(original);

        Map<String, JsonObject> pack = new LinkedHashMap<>();
        JsonObject broken = file("lesser_saiyan");
        broken.addProperty("tier", 99);
        pack.put("lesser_saiyan", broken);
        List<String> bad = RaceTemplates.replace(pack);
        assertEquals(1, bad.size());
        assertTrue(bad.get(0).startsWith("lesser_saiyan"), bad.get(0));
        assertSame(original, RaceTemplates.get("lesser_saiyan"), "the bundled race stays when the pack's file is bad");
        assertEquals(List.of("lesser_saiyan: lesser_saiyan: 'tier' = 99.0 is outside 1.0..5.0"), RaceTemplates.problems());

        JsonObject tuned = file("lesser_saiyan");
        tuned.getAsJsonObject("transformations").getAsJsonObject("boosted").addProperty("duration_seconds", 999);
        pack.put("lesser_saiyan", tuned);
        assertTrue(RaceTemplates.replace(pack).isEmpty());
        assertTrue(original != RaceTemplates.get("lesser_saiyan"), "a good file replaces the bundled one");
        assertEquals(999 * 20, RaceTemplates.get("lesser_saiyan").transformations().values().stream()
                .filter(s -> s.state().id.equals("boosted")).findFirst().orElseThrow().durationTicks());
        assertEquals(5, RaceTemplates.all().size(), "the other bundled races are still there");
        assertTrue(RaceTemplates.loadBundled().isEmpty());     // leave the registry as it was found
    }

    @Test
    void theLineCheckCatchesBrokenChains() {
        assertTrue(RaceTemplates.loadBundled().isEmpty());
        Map<String, RaceTemplate> good = RaceTemplates.all();
        assertEquals(List.of(), RaceTemplates.checkLines(good));

        // an evolution into a race that does not exist
        JsonObject ghost = file("divine_saiyan");
        ghost.addProperty("evolves_to", "nobody");
        Map<String, RaceTemplate> a = new LinkedHashMap<>(good);
        a.put("divine_saiyan", RaceTemplateParser.parse("divine_saiyan", ghost));
        assertTrue(RaceTemplates.checkLines(a).stream().anyMatch(s -> s.contains("not a template")), String.valueOf(RaceTemplates.checkLines(a)));

        // a skipped tier (lesser straight to high)
        JsonObject skip = file("lesser_saiyan");
        skip.addProperty("evolves_to", "high_saiyan");
        Map<String, RaceTemplate> b = new LinkedHashMap<>(good);
        b.put("lesser_saiyan", RaceTemplateParser.parse("lesser_saiyan", skip));
        assertTrue(RaceTemplates.checkLines(b).stream().anyMatch(s -> s.contains("tiers must go up by one")));

        // a cycle (divine back to lesser) also breaks the tier rule, and the chain never ends
        JsonObject loop = file("divine_saiyan");
        loop.addProperty("evolves_to", "lesser_saiyan");
        Map<String, RaceTemplate> c = new LinkedHashMap<>(good);
        c.put("divine_saiyan", RaceTemplateParser.parse("divine_saiyan", loop));
        assertTrue(RaceTemplates.checkLines(c).stream().anyMatch(s -> s.contains("does not end")));

        // another line is refused
        JsonObject cross = file("high_saiyan");
        cross.addProperty("evolves_to", "primordial");
        Map<String, RaceTemplate> d = new LinkedHashMap<>(good);
        d.put("high_saiyan", RaceTemplateParser.parse("high_saiyan", cross));
        assertTrue(RaceTemplates.checkLines(d).stream().anyMatch(s -> s.contains("another line")));
    }
}
