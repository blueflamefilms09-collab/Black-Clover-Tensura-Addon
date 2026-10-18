package com.newuniverse.nusmp.compat.maps;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** The dimension rules map mods (and the game) rely on, applied to the mod's real dimension JSONs. */
class DimensionAuditTest {
    private static JsonObject json(String s) { return JsonParser.parseString(s).getAsJsonObject(); }

    private static final String GOOD = "{\"has_skylight\":false,\"has_ceiling\":true,\"min_y\":0,\"height\":128,\"logical_height\":128,\"coordinate_scale\":1.0,\"ambient_light\":0.05,\"effects\":\"nusmp:x\"}";

    @Test
    void everyRealDimensionPasses() {
        List<DimensionAudit.Report> all = DimensionAudit.auditAll(getClass().getClassLoader());
        assertEquals(DimensionAudit.NAMES.size(), all.size());
        for (DimensionAudit.Report r : all) assertTrue(r.ok(), r.id() + ": " + r.problems());
    }

    @Test
    void theListMatchesTheFoldersSoANewDimensionCannotBeForgotten() throws IOException {
        for (String folder : List.of("dimension_type", "dimension")) {
            Set<String> files = new TreeSet<>();
            try (Stream<Path> s = Files.list(Paths.get("src/main/resources/data/nusmp/" + folder))) {
                s.forEach(p -> files.add(p.getFileName().toString().replace(".json", "")));
            }
            assertEquals(new TreeSet<>(DimensionAudit.NAMES), files, "DimensionAudit.NAMES and data/nusmp/" + folder + " differ: add the new dimension to the list and docs/compat/maps.md");
        }
    }

    @Test
    void profiles() {
        List<DimensionAudit.Report> all = DimensionAudit.auditAll(getClass().getClassLoader());
        DimensionAudit.Report mega = all.stream().filter(r -> r.id().equals("nusmp:mega_dungeon")).findFirst().orElseThrow();
        DimensionAudit.Report hub = all.stream().filter(r -> r.id().equals("nusmp:sanctuary")).findFirst().orElseThrow();
        assertEquals(DimensionAudit.Profile.ROOFED, mega.profile());
        assertEquals(DimensionAudit.Profile.SURFACE, hub.profile());
        assertEquals(1.0, mega.scale());
        assertEquals(DimensionAudit.Profile.ROOFED_SKYLIT, DimensionAudit.profileOf(true, true));
        assertEquals(DimensionAudit.Profile.OPEN_DARK, DimensionAudit.profileOf(false, false));
    }

    @Test
    void goodTypePasses() {
        assertTrue(DimensionAudit.audit("nusmp:x", json(GOOD), null, Set.of()).ok());
    }

    @Test
    void negativeControlsEachRuleFires() {
        assertFalse(DimensionAudit.audit("nusmp:x", json(GOOD.replace("\"height\":128", "\"height\":120")), null, Set.of()).ok(), "height not a multiple of 16");
        assertFalse(DimensionAudit.audit("nusmp:x", json(GOOD.replace("\"min_y\":0", "\"min_y\":-60")), null, Set.of()).ok(), "min_y not a multiple of 16");
        assertFalse(DimensionAudit.audit("nusmp:x", json(GOOD.replace("\"logical_height\":128", "\"logical_height\":256")), null, Set.of()).ok(), "logical height above height");
        assertFalse(DimensionAudit.audit("nusmp:x", json(GOOD.replace("\"height\":128", "\"height\":4096")), null, Set.of()).ok(), "too tall");
        assertFalse(DimensionAudit.audit("nusmp:x", json(GOOD.replace("\"min_y\":0", "\"min_y\":2000")), null, Set.of()).ok(), "min_y not a multiple of 16 and min_y + height over 2032");
        assertFalse(DimensionAudit.audit("nusmp:x", json(GOOD.replace("\"min_y\":0", "\"min_y\":1920")), null, Set.of()).ok(), "min_y + height = 2048, over 2032");
        assertFalse(DimensionAudit.audit("nusmp:x", json(GOOD.replace("1.0", "0")), null, Set.of()).ok(), "scale zero");
        assertFalse(DimensionAudit.audit("nusmp:x", json(GOOD.replace("\"has_ceiling\":true,", "")), null, Set.of()).ok(), "missing field");
        assertFalse(DimensionAudit.audit("nusmp:x", json(GOOD.replace("nusmp:x", "plain")), null, Set.of()).ok(), "effects not namespaced");
        assertFalse(DimensionAudit.audit("Bad Id", json(GOOD), null, Set.of()).ok(), "bad dimension id");
        assertFalse(DimensionAudit.audit("nusmp:x", null, null, Set.of()).ok(), "no type JSON");
        assertFalse(DimensionAudit.audit("nusmp:x", json(GOOD), json("{\"type\":\"nusmp:nope\"}"), Set.of("nusmp:x")).ok(), "dimension names a missing type");
        assertTrue(DimensionAudit.audit("nusmp:x", json(GOOD), json("{\"type\":\"minecraft:overworld\"}"), Set.of()).ok());
        assertTrue(DimensionAudit.audit("nusmp:x", json(GOOD), json("{\"type\":\"nusmp:x\"}"), Set.of("nusmp:x")).ok());
    }
}
