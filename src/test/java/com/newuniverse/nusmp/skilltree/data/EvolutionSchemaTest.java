package com.newuniverse.nusmp.skilltree.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.skilltree.line.LineCard;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why (3.26.0, the Evolution skills): the route file grew three things (a route's {@code evolution} block, a node's {@code ultimate} flag, a passive's {@code evolved} block) and the owner's rule for the
 * engine is the same as for every line: it accepts exactly what the generator writes and refuses, with a sentence, what the generator would never write, without ever breaking the other lines. The fifth fixture
 * line ({@code tools/lines/engine_fixture.py}) is the real generator's output with all three; this test proves it loads clean and keeps every field through the wire (the route and the card the client holds),
 * that a line without an evolution is unchanged, and that each way an evolution can be wrong (unknown counter, count out of range, missing text, a damage kind where none belongs, no Ultimate or two,
 * an Ultimate that is not an art, an evolved block on the wrong node or outside its ranges) refuses the line with a message that starts with the field.
 */
class EvolutionSchemaTest {
    private static final String ID = "nusmp:fixture_line_4";

    private static JsonObject fixture() throws IOException { return JsonParser.parseString(LineSchemaTest.samples().get(ID).toString()).getAsJsonObject(); }

    private static JsonObject evolution(JsonObject route) { return route.getAsJsonObject("evolution"); }

    private static JsonObject nodeWith(JsonObject route, String field) {
        for (JsonElement e : route.getAsJsonArray("nodes")) if (e.getAsJsonObject().has(field)) return e.getAsJsonObject();
        throw new AssertionError("no node with " + field);
    }

    private static JsonObject firstPassive(JsonObject route) {
        for (JsonElement e : route.getAsJsonArray("nodes")) if ("passive_toggle".equals(e.getAsJsonObject().get("type").getAsString())) return e.getAsJsonObject();
        throw new AssertionError("no passive");
    }

    private static RouteSchema.Parsed parse(JsonObject o) { return RouteSchema.parse(ID, o); }

    private static void refused(JsonObject o, String why) {
        RouteSchema.Parsed p = parse(o);
        assertNull(p.route(), why + ": the line must be refused");
        assertTrue(p.problems().stream().anyMatch(s -> s.contains(why)), "the message names '" + why + "': " + p.problems());
    }

    // ------------------------------------------------------------------ the good line
    @Test
    void theFixtureLineLoadsCleanWithItsEvolutionItsUltimateAndItsEvolvedPassive() throws IOException {
        RouteSchema.Parsed p = parse(fixture());
        assertTrue(p.clean(), String.join(" | ", p.problems()));
        SkillRoute r = p.route();
        assertTrue(r.hasEvolution());
        assertEquals(new RouteEvolution("damage_taken", 2000, "Take 2,000 fire damage.", "Fixture Ultimate Evolution", "fire"), r.evolution());
        assertEquals("fire", r.evolution().damageKindOrAny());
        SkillNode ult = r.ultimateNode();
        assertNotNull(ult);
        assertEquals(SkillNode.NodeType.ACTIVE, ult.nodeType());
        assertEquals("debt_burst", ult.kind());
        long ultimates = r.nodes().stream().filter(SkillNode::ultimate).count();
        assertEquals(1, ultimates);
        List<SkillNode> evolved = r.nodes().stream().filter(SkillNode::hasEvolved).toList();
        assertEquals(1, evolved.size());
        assertEquals("debt_store", evolved.get(0).kind());
        assertEquals(0.6, evolved.get(0).evolved().num("store_pct"), 1e-9);
        assertEquals(0.7, evolved.get(0).evolved().num("max_pct"), 1e-9);
        assertNull(evolved.get(0).evolved().num("hold_seconds"), "a number the block leaves out is not overridden");
    }

    @Test
    void everyOtherFixtureLineHasNoEvolutionAndSaysSo() throws IOException {
        for (var e : LineSchemaTest.samples().entrySet()) {
            if (e.getKey().equals(ID)) continue;
            SkillRoute r = RouteSchema.parse(e.getKey(), e.getValue()).route();
            assertFalse(r.hasEvolution(), e.getKey());
            assertNull(r.ultimateNode(), e.getKey());
            assertTrue(r.nodes().stream().noneMatch(n -> n.ultimate() || n.hasEvolved()), e.getKey());
        }
    }

    @Test
    void theOldShapesOfTheRecordsStillMeanWhatTheyMeant() {
        SkillNode n = new SkillNode("a", "A", "", List.of(), 1, List.of(), 1, "", List.of(), java.util.Map.of(), 0, 0, "");
        assertFalse(n.ultimate());
        assertFalse(n.hasEvolved());
        SkillNode n16 = new SkillNode("a", "A", "", List.of(), 1, List.of(), 1, "", List.of(), java.util.Map.of(), 0, 0, "", "active_ability", "cone_strike", java.util.Map.of());
        assertFalse(n16.ultimate());
        assertEquals(EvolvedParams.NONE, n16.evolved());
        SkillRoute r = new SkillRoute("nusmp:t", "other", "T", "", "nusmp:t_book", "T Book", "", 0x123456, "minecraft:book", "t", 3, 1, List.of(n));
        assertFalse(r.hasEvolution());
        assertNull(r.evolution());
        assertNull(r.ultimateNode());
        assertEquals("", new RouteEvolution(null, 1, null, null, null).requirement(), "a null is an empty word, never a null pointer");
        assertEquals("any", new RouteEvolution("damage_taken", 1, "t", "n", null).damageKindOrAny());
    }

    // ------------------------------------------------------------------ the wire
    @Test
    void theRouteAndTheCardKeepTheEvolutionThroughTheWire() throws IOException {
        SkillRoute r = parse(fixture()).route();
        RouteWireTest.ByteSink sink = new RouteWireTest.ByteSink(true);
        RouteWire.write(sink, r);
        assertEquals(r, RouteWire.read(new RouteWireTest.ByteSource(sink.bytes())));
        assertEquals(RouteWire.size(r), sink.bytes().length, "the size count agrees with the bytes written");

        LineCard card = LineCard.of(r);
        assertEquals(r.evolution(), card.evolution());
        RouteWireTest.ByteSink cs = new RouteWireTest.ByteSink(true);
        LineCard.writeAll(cs, List.of(card));
        List<LineCard> back = LineCard.readAll(new RouteWireTest.ByteSource(cs.bytes()));
        assertEquals(List.of(card), back);
        assertEquals(r.evolution(), back.get(0).toStub().evolution(), "a client's node-less stub of the line knows what the line asks for");
        assertEquals(LineCard.size(List.of(card)), cs.bytes().length);

        SkillRoute plain = RouteSchema.parse("nusmp:fixture_line_0", LineSchemaTest.samples().get("nusmp:fixture_line_0")).route();
        RouteWireTest.ByteSink ps = new RouteWireTest.ByteSink(true);
        LineCard.writeAll(ps, List.of(LineCard.of(plain)));
        assertNull(LineCard.readAll(new RouteWireTest.ByteSource(ps.bytes())).get(0).evolution());
        assertTrue(cs.bytes().length > ps.bytes().length, "a card with an evolution carries more than one without");
    }

    @Test
    void aBrokenEvolutionOnTheWireIsRefusedNotBuilt() {
        RouteWireTest.ByteSink s = new RouteWireTest.ByteSink(false);
        s.varInt(2);                                                       // a flag that is neither 0 nor 1
        assertThrows(RuntimeException.class, () -> RouteWire.readEvolution(new RouteWireTest.ByteSource(s.bytes())));
        RouteWireTest.ByteSink zero = new RouteWireTest.ByteSink(false);
        RouteWire.writeEvolution(zero, new RouteEvolution("kills", 5, "t", "n", ""));
        byte[] good = zero.bytes();
        assertEquals(new RouteEvolution("kills", 5, "t", "n", ""), RouteWire.readEvolution(new RouteWireTest.ByteSource(good)));
        RouteWireTest.ByteSink bad = new RouteWireTest.ByteSink(false);
        bad.varInt(1);
        bad.utf("kills", 64);
        bad.varInt(0);                                                      // a count of 0
        bad.utf("t", 200);
        bad.utf("n", 64);
        bad.utf("", 64);
        assertThrows(RuntimeException.class, () -> RouteWire.readEvolution(new RouteWireTest.ByteSource(bad.bytes())));
        RouteWireTest.ByteSink big = new RouteWireTest.ByteSink(false);
        big.varInt(1);
        big.utf("kills", 64);
        big.varInt(RouteEvolution.MAX_COUNT + 1);
        big.utf("t", 200);
        big.utf("n", 64);
        big.utf("", 64);
        assertThrows(RuntimeException.class, () -> RouteWire.readEvolution(new RouteWireTest.ByteSource(big.bytes())));
        byte[] cut = java.util.Arrays.copyOf(good, good.length - 2);
        assertThrows(RuntimeException.class, () -> RouteWire.readEvolution(new RouteWireTest.ByteSource(cut)), "a packet that ends early");
    }

    // ------------------------------------------------------------------ what is refused
    @Test
    void anUnknownCounterIsRefused() throws IOException {
        JsonObject o = fixture();
        evolution(o).addProperty("requirement", "slay_everything");
        refused(o, "evolution.requirement");
    }

    @Test
    void aCountOutOfRangeOrMissingIsRefused() throws IOException {
        for (int bad : new int[]{0, -4, RouteEvolution.MAX_COUNT + 1}) {
            JsonObject o = fixture();
            evolution(o).addProperty("count", bad);
            refused(o, "evolution.count");
        }
        JsonObject o = fixture();
        evolution(o).remove("count");
        refused(o, "evolution.count");
        JsonObject ok = fixture();
        evolution(ok).addProperty("count", RouteEvolution.MAX_COUNT);
        assertTrue(parse(ok).clean(), "the top of the range is allowed");
    }

    @Test
    void theTextAndTheUltimateNameAreRequired() throws IOException {
        JsonObject a = fixture();
        evolution(a).remove("text");
        refused(a, "evolution.text");
        JsonObject b = fixture();
        evolution(b).addProperty("ultimate_name", "  ");
        refused(b, "evolution.ultimate_name");
        JsonObject c = fixture();
        evolution(c).addProperty("text", "x".repeat(RouteEvolution.LEN_TEXT + 1));
        RouteSchema.Parsed p = parse(c);
        assertTrue(p.route() == null || p.route().evolution().text().length() <= RouteEvolution.LEN_TEXT, "an over-long sentence is cut or refused, never carried past the wire limit");
    }

    @Test
    void aDamageKindOnlyBelongsToDamageTakenAndMustBeOneOfTheCataloguesWords() throws IOException {
        JsonObject a = fixture();
        evolution(a).addProperty("requirement", "kills");
        refused(a, "damage_kind");
        JsonObject b = fixture();
        evolution(b).getAsJsonObject("params").addProperty("damage_kind", "sunburn");
        refused(b, "damage_kind");
        JsonObject c = fixture();
        evolution(c).getAsJsonObject("params").addProperty("shape", "round");
        refused(c, "params.shape");
        for (String kind : new String[]{"fire", "magic", "explosion", "projectile", "fall", "freeze", "lightning", "wither", "any"}) {
            JsonObject ok = fixture();
            evolution(ok).getAsJsonObject("params").addProperty("damage_kind", kind);
            assertTrue(parse(ok).clean(), kind + " is allowed");
        }
        JsonObject none = fixture();
        evolution(none).remove("params");
        RouteSchema.Parsed p = parse(none);
        assertTrue(p.clean(), "no params is allowed: any damage counts");
        assertEquals("any", p.route().evolution().damageKindOrAny());
    }

    @Test
    void anEvolutionNeedsExactlyOneUltimateArt() throws IOException {
        JsonObject none = fixture();
        nodeWith(none, "ultimate").remove("ultimate");
        refused(none, "exactly one node flagged ultimate");
        JsonObject two = fixture();
        int extra = 0;
        for (JsonElement e : two.getAsJsonArray("nodes")) {
            JsonObject n = e.getAsJsonObject();
            if (extra == 0 && "active_ability".equals(n.get("type").getAsString()) && !n.has("ultimate")) {
                n.addProperty("ultimate", true);
                extra++;
            }
        }
        assertEquals(1, extra, "the fixture has a second art to flag");
        refused(two, "exactly one node flagged ultimate");
    }

    @Test
    void anUltimateOnAPassiveOrANonBooleanIsRefused() throws IOException {
        JsonObject a = fixture();
        firstPassive(a).addProperty("ultimate", true);
        RouteSchema.Parsed pa = parse(a);
        assertNull(pa.route(), "a passive cannot be the Ultimate Evolution art");
        assertTrue(pa.problems().stream().anyMatch(s -> s.contains("ultimate")), pa.problems().toString());
        JsonObject b = fixture();
        nodeWith(b, "ultimate").addProperty("ultimate", "yes");
        RouteSchema.Parsed pb = parse(b);
        assertNull(pb.route());
        assertTrue(pb.problems().stream().anyMatch(s -> s.contains("ultimate")), pb.problems().toString());
    }

    @Test
    void anUltimateWithoutAnEvolutionLoadsButIsNeverLockedAndTheSchemaWarns() throws IOException {
        JsonObject o = fixture();
        o.remove("evolution");
        RouteSchema.Parsed p = parse(o);
        assertTrue(p.ok());
        assertFalse(p.route().hasEvolution());
        assertTrue(p.warnings().stream().anyMatch(s -> s.contains("ultimate") && s.contains("no evolution")), p.warnings().toString());
    }

    @Test
    void anEvolutionOnAClassicRouteIsIgnoredWithAWarning() {
        JsonObject o = JsonParser.parseString("""
                { "name": "Old", "book": "nusmp:old_book", "color": "#112233", "class_id": "old",
                  "evolution": { "requirement": "kills", "count": 5, "text": "t", "ultimate_name": "n" },
                  "nodes": [ { "id": "a", "x": 1 } ] }""").getAsJsonObject();
        RouteSchema.Parsed p = RouteSchema.parse("nusmp:old", o);
        assertNotNull(p.route());
        assertFalse(p.route().hasEvolution());
        assertTrue(p.warnings().stream().anyMatch(s -> s.contains("evolution")), p.warnings().toString());
    }

    @Test
    void anEvolvedBlockOnlyBelongsToAPassiveAndStaysInsideTheKindsRanges() throws IOException {
        JsonObject onArt = fixture();
        nodeWith(onArt, "ultimate").add("evolved", JsonParser.parseString("{\"damage\": 20}"));
        RouteSchema.Parsed pa = parse(onArt);
        assertNull(pa.route());
        assertTrue(pa.problems().stream().anyMatch(s -> s.contains("evolved")), pa.problems().toString());

        JsonObject high = fixture();
        nodeWith(high, "evolved").getAsJsonObject("evolved").addProperty("store_pct", 5.0);
        RouteSchema.Parsed ph = parse(high);
        assertNull(ph.route(), "an evolved number outside the range of the kind refuses the line");
        assertTrue(ph.problems().stream().anyMatch(s -> s.contains("evolved.store_pct")), ph.problems().toString());

        JsonObject unknown = fixture();
        nodeWith(unknown, "evolved").getAsJsonObject("evolved").addProperty("mystery", 1.0);
        assertNull(parse(unknown).route(), "a number the kind does not have");

        JsonObject word = fixture();
        nodeWith(word, "evolved").getAsJsonObject("evolved").addProperty("vfx", "NOT_A_SHAPE");
        assertNull(parse(word).route(), "a word outside the kind's list");

        JsonObject notObject = fixture();
        nodeWith(notObject, "evolved").addProperty("evolved", 5);
        assertNull(parse(notObject).route());

        JsonObject nan = fixture();
        nodeWith(nan, "evolved").getAsJsonObject("evolved").addProperty("store_pct", "high");
        assertNull(parse(nan).route(), "a word where a number belongs");

        JsonObject good = fixture();
        nodeWith(good, "evolved").getAsJsonObject("evolved").addProperty("store_pct", 0.8);
        assertTrue(parse(good).clean(), "the top of the range is allowed");
    }

    @Test
    void oneBrokenEvolutionNeverBreaksTheOtherLines() throws IOException {
        java.util.Map<String, JsonElement> files = new java.util.LinkedHashMap<>(LineSchemaTest.samples());
        JsonObject broken = fixture();
        evolution(broken).addProperty("requirement", "nope");
        files.put(ID, broken);
        RouteIndex idx = RouteIndex.build(files, List.of(), RouteIndex.Caps.REGISTRY);
        assertEquals(4, idx.all().size(), "the four lines without a fault load");
        assertTrue(idx.report().stream().anyMatch(e -> !e.loaded() && e.problems().stream().anyMatch(s -> s.contains("evolution.requirement"))));
    }
}
