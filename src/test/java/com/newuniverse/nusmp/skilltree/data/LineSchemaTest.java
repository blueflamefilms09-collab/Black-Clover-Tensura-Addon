package com.newuniverse.nusmp.skilltree.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.skilltree.line.LineCatalog;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: part A of the Unique Skill Lines job is "the engine reads what the generator writes, and refuses what the generator would never write, without ever breaking the other lines". The fixture
 * lines in {@code src/test/resources/lines_sample} are made by the REAL generator ({@code tools/lines/engine_fixture.py}) and between them use every kind of the catalogue, so this test proves the
 * validator accepts every kind, every enum list and every number range of {@code catalog.json}, that the wire format carries the new fields, that a classic route still means what it meant, and that
 * each way a line can be malformed (unknown kind, wrong enum, number out of range, kind of the wrong type, grid out of range) skips THAT line with a message while the rest load.
 */
class LineSchemaTest {
    private static final Path SAMPLES = Path.of("src/test/resources/lines_sample");

    static Map<String, JsonObject> samples() throws IOException {
        Map<String, JsonObject> out = new LinkedHashMap<>();
        List<Path> files;
        try (var s = Files.list(SAMPLES)) {
            files = s.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList();
        }
        for (Path f : files) {
            String n = f.getFileName().toString();
            out.put("nusmp:" + n.substring(0, n.length() - 5), JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject());
        }
        return out;
    }

    private static JsonObject copy(JsonObject o) { return JsonParser.parseString(o.toString()).getAsJsonObject(); }

    private static JsonObject node(JsonObject route, String kind) {
        for (JsonElement e : route.getAsJsonArray("nodes")) if (kind.equals(e.getAsJsonObject().get("kind").getAsString())) return e.getAsJsonObject();
        throw new AssertionError("no node of kind " + kind);
    }

    @Test
    void theEngineHoldsAByteCopyOfTheGeneratorsCatalogue() throws IOException {
        String tool = Files.readString(Path.of("tools/lines/catalog.json"), StandardCharsets.UTF_8);
        String jar = Files.readString(Path.of("src/main/resources/nusmp_lines/catalog.json"), StandardCharsets.UTF_8);
        assertEquals(tool, jar, "tools/lines/catalog.json changed: copy it to src/main/resources/nusmp_lines/catalog.json (the engine reads the copy)");
        assertTrue(LineCatalog.get().loaded(), "the catalogue resource must load from the classpath");
        assertEquals(26, LineCatalog.get().activeKinds().size());
        assertEquals(25, LineCatalog.get().passiveKinds().size());
        assertTrue(LineCatalog.get().stanceKinds().contains("stance") && LineCatalog.get().attrKinds().contains("attr"));
    }

    @Test
    void everyFixtureLineLoadsCleanAndTogetherTheyUseEveryKind() throws IOException {
        Set<String> kinds = new LinkedHashSet<>();
        for (Map.Entry<String, JsonObject> e : samples().entrySet()) {
            RouteSchema.Parsed p = RouteSchema.parse(e.getKey(), e.getValue());
            assertTrue(p.clean(), e.getKey() + ": " + String.join(" | ", p.problems()));
            SkillRoute r = p.route();
            assertTrue(r.isLine() && r.franchise().equals("tensura"));
            assertTrue(r.guiBackground().startsWith("nusmp:textures/gui/skill_trees/") && r.bookTexture().startsWith("nusmp:item/skill_book_3d/") && !r.backgroundTheme().isEmpty());
            for (SkillNode n : r.nodes()) {
                assertTrue(n.isLineNode() && !n.type().isEmpty(), n.id());
                kinds.add(n.kind());
                if (n.hasToggle()) assertFalse(n.nodeType() == SkillNode.NodeType.ACTIVE);
            }
        }
        assertEquals(new LinkedHashSet<>(LineCatalog.get().allKinds()), kinds, "the fixture lines must cover every kind of the catalogue (run tools/lines/engine_fixture.py)");
    }

    @Test
    void aLineSurvivesTheWireWithItsNewFields() throws IOException {
        for (Map.Entry<String, JsonObject> e : samples().entrySet()) {
            SkillRoute r = RouteSchema.parse(e.getKey(), e.getValue()).route();
            RouteWireTest.ByteSink sink = new RouteWireTest.ByteSink(true);
            RouteWire.write(sink, r);
            SkillRoute back = RouteWire.read(new RouteWireTest.ByteSource(sink.bytes()));
            assertEquals(r, back, e.getKey());
            assertEquals(RouteWire.size(r), sink.bytes().length, "the size count agrees with the bytes written");
        }
    }

    @Test
    void aClassicRouteKeepsTheClassicGridAndHasNoLineFields() {
        JsonObject o = JsonParser.parseString("""
                { "name": "Old", "book": "nusmp:old_book", "color": "#112233", "class_id": "old",
                  "nodes": [ { "id": "a", "x": 13 } ] }""").getAsJsonObject();
        RouteSchema.Parsed p = RouteSchema.parse("nusmp:old", o);
        assertNull(p.route(), "x 13 is still refused for a classic route");
        o.getAsJsonArray("nodes").get(0).getAsJsonObject().addProperty("x", 12);
        SkillRoute r = RouteSchema.parse("nusmp:old", o).route();
        assertNotNull(r);
        assertFalse(r.isLine());
        assertEquals("", r.franchise());
        assertEquals(SkillNode.NodeType.PLAIN, r.nodes().get(0).nodeType());
    }

    @Test
    void aLineMayUseTheWiderGridButNotBeyondIt() throws IOException {
        JsonObject o = copy(samples().get("nusmp:fixture_line_0"));
        JsonObject n = o.getAsJsonArray("nodes").get(0).getAsJsonObject();
        n.addProperty("x", RouteSchema.MAX_LINE_X);
        n.addProperty("y", RouteSchema.MAX_LINE_Y);
        assertTrue(RouteSchema.parse("nusmp:fixture_line_0", o).clean());
        n.addProperty("x", RouteSchema.MAX_LINE_X + 1);
        assertNull(RouteSchema.parse("nusmp:fixture_line_0", o).route());
    }

    @Test
    void typesAreInferredFromTheKindWhenTheFileGivesOnlyAKind() throws IOException {
        JsonObject o = copy(samples().get("nusmp:fixture_line_0"));
        JsonObject n = node(o, "stance");
        n.remove("type");
        SkillRoute r = RouteSchema.parse("nusmp:fixture_line_0", o).route();
        assertNotNull(r);
        assertEquals("active_ability", r.node(n.get("id").getAsString()).type());
    }

    @Test
    void eachWayOfBeingMalformedSkipsTheLineWithAMessage() throws IOException {
        JsonObject good = samples().get("nusmp:fixture_line_0");
        String[][] cases = {{"kind", "not_a_kind"}, {"enum", "vfx=NOT_A_SHAPE"}, {"range", "cooldown=99999"}, {"type", "passive_toggle"}, {"badtype", "wizard"}};
        for (String[] c : cases) {
            JsonObject o = copy(good);
            JsonObject n = node(o, "stance");
            switch (c[0]) {
                case "kind" -> n.addProperty("kind", c[1]);
                case "enum" -> n.getAsJsonObject("params").addProperty("vfx", "NOT_A_SHAPE");
                case "range" -> { JsonArray a = new JsonArray(); a.add(99999); a.add(99999); a.add(99999); n.getAsJsonObject("values").add("cooldown", a); }
                case "type" -> n.addProperty("type", c[1]);
                default -> n.addProperty("type", c[1]);
            }
            RouteSchema.Parsed p = RouteSchema.parse("nusmp:fixture_line_0", o);
            assertNull(p.route(), c[0] + ": the line must be refused");
            assertTrue(!p.problems().isEmpty() && String.join("|", p.problems()).contains(n.get("id").getAsString()), c[0] + ": the message names the node: " + p.problems());
        }
    }

    @Test
    void aMalformedLineNeverBreaksTheOthers() throws IOException {
        Map<String, JsonElement> files = new LinkedHashMap<>(samples());
        JsonObject broken = copy(samples().get("nusmp:fixture_line_1"));
        node(broken, "stance").addProperty("kind", "nope");
        files.put("nusmp:fixture_line_1", broken);
        files.put("nusmp:fixture_line_9", JsonParser.parseString("[1,2,3]"));
        RouteIndex idx = RouteIndex.build(files, List.of("nusmp:fixture_line_unreadable"), RouteIndex.Caps.REGISTRY);
        assertEquals(3, idx.all().size(), "the three good lines load");
        int bad = 0;
        for (RouteIndex.Entry e : idx.report()) if (!e.loaded()) { bad++; assertFalse(e.problems().isEmpty(), e.id() + " says why"); }
        assertEquals(3, bad, "the broken line, the non-object and the unreadable file are reported, not loaded");
    }

    @Test
    void aTooLongDescriptionOfALineIsCutWithAWarningNotARefusal() throws IOException {
        JsonObject o = copy(samples().get("nusmp:fixture_line_2"));
        o.addProperty("desc", "x".repeat(RouteSchema.LEN_ROUTE_DESC + 50));
        o.getAsJsonArray("nodes").get(0).getAsJsonObject().addProperty("desc", "y".repeat(RouteSchema.LEN_NODE_DESC + 5));
        RouteSchema.Parsed p = RouteSchema.parse("nusmp:fixture_line_2", o);
        assertNotNull(p.route());
        assertEquals(RouteSchema.LEN_ROUTE_DESC, p.route().desc().length());
        assertTrue(p.warnings().stream().anyMatch(w -> w.startsWith("desc:")));
        JsonObject classic = JsonParser.parseString("{\"name\":\"C\",\"desc\":\"" + "x".repeat(401) + "\",\"book\":\"nusmp:c_book\",\"color\":\"#112233\",\"class_id\":\"c\",\"nodes\":[{\"id\":\"a\"}]}").getAsJsonObject();
        assertNull(RouteSchema.parse("nusmp:c", classic).route(), "a classic route keeps its strict text limit");
    }

    @Test
    void routesAreCutIntoPayloadsThatRespectBothLimits() throws IOException {
        List<SkillRoute> routes = new ArrayList<>();
        for (int i = 0; i < 30; i++) for (JsonObject o : samples().values()) routes.add(RouteSchema.parse("nusmp:x", o).route());
        int size = RouteWire.size(routes.get(0));
        List<List<SkillRoute>> chunks = RouteWire.chunks(routes, 64, size * 5 + 1);
        int total = 0;
        for (List<SkillRoute> c : chunks) {
            int bytes = 0;
            for (SkillRoute r : c) bytes += RouteWire.size(r);
            assertTrue(c.size() <= 64 && (c.size() == 1 || bytes <= size * 5 + 1), "a chunk obeys the byte limit");
            total += c.size();
        }
        assertEquals(routes.size(), total, "nothing is lost");
        assertTrue(chunks.size() > 1);
        assertEquals(1, RouteWire.chunks(routes.subList(0, 3), 64, 1_000_000).size());
        assertTrue(RouteWire.chunks(List.of(), 64, 1000).isEmpty());
    }
}
