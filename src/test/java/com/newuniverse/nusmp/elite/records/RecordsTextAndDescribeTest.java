package com.newuniverse.nusmp.elite.records;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.elite.records.rule.Bonuses;
import com.newuniverse.nusmp.elite.records.rule.BuiltinSynergies;
import com.newuniverse.nusmp.elite.records.rule.Catalog;
import com.newuniverse.nusmp.elite.records.rule.Describe;
import com.newuniverse.nusmp.elite.records.rule.Requirement;
import com.newuniverse.nusmp.elite.records.rule.StatNames;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: (1) the strings of {@code RecordsText} (read with a fallback, so a missing language entry never shows a raw key) and {@code tools/elite/records_lang.json} (what the lead merges into en_us.json
 * later) must not drift apart: compared both ways by reading the source text, so no Minecraft class is needed; also that {@code en_us.json} was not touched by this job. (2) the English lines the screen
 * prints for a requirement or a bonus come from {@link Describe}; they are pinned here, and every title and synergy of the shipped data must describe itself without a placeholder or a raw id.
 */
class RecordsTextAndDescribeTest {
    @Test
    void theLanguageFileAndTheCodeAgreeInBothDirections() throws IOException {
        String src = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/elite/records/RecordsText.java"), StandardCharsets.UTF_8);
        Map<String, String> code = new LinkedHashMap<>();
        Matcher m = Pattern.compile("def\\(\"([^\"]+)\",\\s*\"((?:[^\"\\\\]|\\\\.)*)\"\\)").matcher(src);
        while (m.find()) code.put(m.group(1), JsonParser.parseString("\"" + m.group(2) + "\"").getAsString());
        JsonObject file = JsonParser.parseString(Files.readString(Path.of("tools/elite/records_lang.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        assertFalse(code.isEmpty());
        for (Map.Entry<String, String> e : code.entrySet()) {
            assertTrue(file.has(e.getKey()), "records_lang.json lacks " + e.getKey() + " (run tools/elite/gen_records_lang.py)");
            assertEquals(e.getValue(), file.get(e.getKey()).getAsString(), e.getKey());
        }
        for (String k : file.keySet()) assertTrue(code.containsKey(k), "records_lang.json has " + k + ", which the code does not use");
        for (var e : file.entrySet()) assertTrue(e.getValue().isJsonPrimitive(), "the language file must be flat: " + e.getKey());
    }

    @Test
    void everyKeyUsedByTheScreenAndMessagesIsDeclared() throws IOException {
        Map<String, String> declared = new LinkedHashMap<>();
        String text = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/elite/records/RecordsText.java"), StandardCharsets.UTF_8);
        Matcher m = Pattern.compile("def\\(\"([^\"]+)\",").matcher(text);
        while (m.find()) declared.put(m.group(1), "");
        // the dynamic keys: category and rarity names are built from the enum ids
        for (var c : com.newuniverse.nusmp.elite.records.rule.Category.values()) assertTrue(declared.containsKey("gui.nusmp.records.category." + c.id()), c.id());
        for (var r : com.newuniverse.nusmp.elite.records.rule.Rarity.values()) assertTrue(declared.containsKey("gui.nusmp.records.rarity." + r.id()), r.id());
        // en_us.json is not edited by this job
        String en = Files.readString(Path.of("src/main/resources/assets/nusmp/lang/en_us.json"), StandardCharsets.UTF_8);
        assertFalse(en.contains("gui.nusmp.records."), "en_us.json must not be edited by this job");
    }

    @Test
    void bonusLinesReadNaturally() {
        Bonuses b = BuiltinSynergies.b().base("minecraft:generic.movement_speed", 0.03).add("minecraft:generic.max_health", 4.0).add("minecraft:generic.luck", 0.5)
                .effect("minecraft:fire_resistance", 0).effect("minecraft:regeneration", 1).stat(StatNames.XP_BONUS_PERCENT, 5).stat(StatNames.KILL_HEAL, 1.5).build();
        List<String> lines = Describe.bonusLines(b);
        assertEquals(List.of("+3% Movement Speed", "+4 Max Health", "+0.5 Luck", "Fire Resistance I", "Regeneration II", "+5% experience from kills", "Heal 1.5 HP when you kill something"), lines);
        assertEquals("-2% Armor", Describe.attr(new Bonuses.Attr("minecraft:generic.armor", -0.02, Bonuses.Op.ADD_MULTIPLIED_TOTAL)));
        assertEquals("1,234", Describe.num(1234));
        assertEquals("1.5", Describe.num(1.5));
        assertEquals("0.25", Describe.num(0.25));
        assertEquals("Tensura Thing 7", Describe.stat("tensura_thing", 7));
    }

    @Test
    void everyShippedTitleAndSynergyDescribesItselfCleanly() throws IOException {
        List<Catalog.Source> recs = new ArrayList<>(), tits = new ArrayList<>(), syns = new ArrayList<>();
        Path root = Path.of("src/main/resources/data/nusmp/elite");
        for (String dir : new String[]{"records", "titles", "titles/synergies"}) {
            try (var s = Files.list(root.resolve(dir))) {
                for (Path p : (Iterable<Path>) s.filter(x -> x.toString().endsWith(".json")).sorted()::iterator) {
                    Catalog.Source src = new Catalog.Source(p.toString(), Files.readString(p, StandardCharsets.UTF_8));
                    (dir.equals("records") ? recs : dir.equals("titles") ? tits : syns).add(src);
                }
            }
        }
        Catalog cat = Catalog.load(recs, tits, syns).catalog();
        for (var t : cat.titles()) {
            for (Requirement r : t.requirements()) {
                String line = Describe.requirement(r, cat);
                assertFalse(line.isBlank() || line.equals("?") || line.contains("null"), t.id() + ": " + line);
            }
            for (String l : Describe.bonusLines(t.bonuses())) assertFalse(l.contains("null") || l.contains(":"), t.id() + ": " + l);
        }
        for (var s : cat.synergies()) for (String l : Describe.bonusLines(s.bonuses())) assertFalse(l.contains("null") || l.contains(":"), s.id() + ": " + l);
        // a record requirement shows the record's name, not its id
        var soldier = cat.title("soldier");
        assertEquals("Record: First Casualty", Describe.requirement(soldier.requirements().get(0), cat));
    }
}
