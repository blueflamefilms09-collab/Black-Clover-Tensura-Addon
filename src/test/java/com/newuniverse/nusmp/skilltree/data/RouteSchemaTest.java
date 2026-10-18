package com.newuniverse.nusmp.skilltree.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the schema is the only thing standing between a hand-written JSON file and the players' trees, so it is proved here against the owner's own example (it must load clean, with the
 * defaults of the design) and against a long list of deliberately bad files, each of which has to produce the right readable sentence AND the right scope of damage: a bad route is refused
 * whole, a bad node is skipped together with exactly the nodes that need it and nothing else. Every refusal test starts from the good file and changes one thing, so a test cannot pass for the
 * wrong reason (the unchanged base is asserted clean first, which is the negative control of the whole class).
 */
class RouteSchemaTest {
    static final String ID = "nusmp:fire_dragon_slayer";

    /** The owner's example (design.md section 2), completed to four nodes: a root, a branch, a second branch and a Secret Art that needs two. */
    static final String GOOD = """
            {
              "magic": "fairy_tail",
              "name": "Fire Dragon Slayer",
              "desc": "The lost magic that teaches a mage to eat flame and to breathe it back.",
              "book": "nusmp:fire_dragon_slayer_book",
              "book_name": "Fang of Smoldering Insanity",
              "book_lore": "A dusty tome sought by every mage who was told his class was not enough.",
              "color": "#E8541C",
              "icon": "minecraft:blaze_powder",
              "class_id": "fire_dragon_slayer",
              "duplicate_sp": 3,
              "loot_weight": 1,
              "nodes": [
                { "id": "iron_fist", "name": "Fire Dragon's Iron Fist", "desc": "A burning punch.", "parents": [], "cost": 1, "max_level": 5,
                  "skill": "nusmp:ft_fire_dragons_iron_fist", "passives": [], "values": { "damage": [6, 7, 8, 9, 11], "burn_seconds": [3, 3, 4, 4, 5] }, "x": 0, "y": 4, "icon": "minecraft:blaze_rod" },
                { "id": "roar", "name": "Fire Dragon's Roar", "parents": [ { "node": "iron_fist", "level": 3 } ], "cost": 2, "costs": [2, 3, 4], "max_level": 3,
                  "skill": "nusmp:ft_fire_dragons_roar", "values": { "damage": [10, 13, 16], "range": [9, 11, 13] }, "x": 2, "y": 3 },
                { "id": "appetite", "name": "Flame Appetite", "parents": ["iron_fist"], "cost": 2, "max_level": 3, "passives": ["eat_fire"], "x": 2, "y": 5 },
                { "id": "crimson_lotus", "name": "Crimson Lotus: Exploding Flame Blade", "parents": [ { "node": "roar", "level": 2 }, "appetite" ], "cost": 6, "max_level": 1,
                  "skill": "nusmp:ft_crimson_lotus", "values": { "damage": 24 }, "x": 4, "y": 4 }
              ]
            }
            """;

    private static JsonObject base() { return JsonParser.parseString(GOOD).getAsJsonObject(); }

    private static JsonObject node(JsonObject route, String id) {
        for (JsonElement e : route.getAsJsonArray("nodes")) if (e.getAsJsonObject().get("id").getAsString().equals(id)) return e.getAsJsonObject();
        throw new AssertionError("no node " + id + " in the test file");
    }

    private static String all(List<String> lines) { return String.join(" | ", lines); }

    /** The route is refused whole and a problem mentions the fragment. */
    private static void refused(JsonElement json, String fragment) {
        RouteSchema.Parsed p = RouteSchema.parse(ID, json);
        assertNull(p.route(), "the route should have been refused: " + fragment);
        assertFalse(p.ok());
        assertTrue(all(p.problems()).contains(fragment), "expected a problem containing '" + fragment + "' but got: " + all(p.problems()));
    }

    /** The route still loads, the node is gone, and a problem mentions the fragment. */
    private static RouteSchema.Parsed nodeSkipped(JsonObject json, String nodeId, String fragment) {
        RouteSchema.Parsed p = RouteSchema.parse(ID, json);
        assertNotNull(p.route(), "the route should still load: " + all(p.problems()));
        assertNull(p.route().node(nodeId), "node " + nodeId + " should have been skipped");
        assertFalse(p.clean());
        assertTrue(all(p.problems()).contains(fragment), "expected a problem containing '" + fragment + "' but got: " + all(p.problems()));
        return p;
    }

    // ------------------------------------------------------------------ the good file
    @Test
    void theOwnersExampleLoadsCleanWithTheNumbersItSays() {
        RouteSchema.Parsed p = RouteSchema.parse(ID, GOOD);
        assertTrue(p.clean(), "problems: " + all(p.problems()));
        assertTrue(p.warnings().isEmpty(), "warnings: " + all(p.warnings()));
        SkillRoute r = p.route();
        assertEquals(ID, r.id());
        assertEquals("fairy_tail", r.magic());
        assertEquals("Fire Dragon Slayer", r.name());
        assertEquals("nusmp:fire_dragon_slayer_book", r.book());
        assertEquals("Fang of Smoldering Insanity", r.bookName());
        assertEquals(0xE8541C, r.color());
        assertEquals("minecraft:blaze_powder", r.icon());
        assertEquals("fire_dragon_slayer", r.classId());
        assertEquals(3, r.duplicateSp());
        assertEquals(1, r.lootWeight());
        assertEquals(4, r.nodes().size());
        assertEquals("iron_fist", r.roots().get(0).id());

        SkillNode fist = r.node("iron_fist");
        assertEquals(5, fist.maxLevel());
        assertEquals(List.of(6.0, 7.0, 8.0, 9.0, 11.0), fist.values().get("damage"));
        assertEquals("nusmp:ft_fire_dragons_iron_fist", fist.skill());
        assertEquals("minecraft:blaze_rod", fist.icon());
        assertEquals(0, fist.x());
        assertEquals(4, fist.y());

        SkillNode roar = r.node("roar");
        assertEquals(List.of(new SkillNode.Parent("iron_fist", 3)), roar.parents(), "the long form of a parent");
        assertEquals(List.of(2, 3, 4), roar.costs());
        assertEquals(4, roar.costOf(3));

        SkillNode appetite = r.node("appetite");
        assertEquals(List.of(new SkillNode.Parent("iron_fist", 1)), appetite.parents(), "the short form of a parent is level 1");
        assertEquals(List.of("eat_fire"), appetite.passives());

        SkillNode lotus = r.node("crimson_lotus");
        assertEquals(List.of(new SkillNode.Parent("roar", 2), new SkillNode.Parent("appetite", 1)), lotus.parents(), "a Secret Art needs two branches");
        assertEquals(List.of(24.0), lotus.values().get("damage"), "a lone number is a one-entry table");
    }

    @Test
    void whatTheFileLeavesOutGetsTheDefaultsOfTheDesign() {
        RouteSchema.Parsed p = RouteSchema.parse(ID, """
                { "name": "Plain Magic", "book": "nusmp:plain_book", "class_id": "plain", "color": "#00ff7f",
                  "nodes": [ { "id": "first_step" } ] }""");
        assertTrue(p.clean(), all(p.problems()));
        SkillRoute r = p.route();
        assertEquals("other", r.magic());
        assertEquals("", r.desc());
        assertEquals("Plain Magic Skill Book", r.bookName());
        assertEquals("", r.bookLore());
        assertEquals("minecraft:book", r.icon());
        assertEquals(3, r.duplicateSp());
        assertEquals(1, r.lootWeight());
        assertEquals(0x00FF7F, r.color(), "the colour is read in either case");
        SkillNode n = r.node("first_step");
        assertEquals("First Step", n.name());
        assertEquals("", n.desc());
        assertEquals(1, n.cost());
        assertEquals(1, n.maxLevel());
        assertEquals("", n.skill());
        assertEquals("", n.icon());
        assertEquals(0, n.x());
        assertEquals(0, n.y());
        assertTrue(n.parents().isEmpty() && n.costs().isEmpty() && n.passives().isEmpty() && n.values().isEmpty());
        assertFalse(n.grantsSkill());
    }

    @Test
    void zeroIsAllowedWhereTheOwnerAllowsIt() {
        JsonObject o = base();
        o.addProperty("duplicate_sp", 0);
        o.addProperty("loot_weight", 0);
        node(o, "iron_fist").addProperty("x", 12);
        node(o, "iron_fist").addProperty("y", 9);
        RouteSchema.Parsed p = RouteSchema.parse(ID, o);
        assertTrue(p.clean(), all(p.problems()));
        assertEquals(0, p.route().duplicateSp());
        assertEquals(0, p.route().lootWeight());
        assertEquals(12, p.route().node("iron_fist").x());
        assertEquals(9, p.route().node("iron_fist").y());
    }

    @Test
    void everyLimitIsInclusiveAtItsEdge() {
        JsonObject o = base();
        JsonObject fist = node(o, "iron_fist");
        fist.addProperty("cost", 50);
        fist.addProperty("max_level", 10);
        fist.add("values", JsonParser.parseString("{\"damage\": [1,2,3,4,5,6,7,8,9,10]}"));
        fist.addProperty("desc", "d".repeat(300));
        JsonObject roar = node(o, "roar");
        roar.add("parents", JsonParser.parseString("[{\"node\": \"iron_fist\", \"level\": 10}]"));        // exactly the parent's maximum
        roar.add("costs", JsonParser.parseString("[1, 50, 25]"));                                           // as many prices as levels, the edges of the range
        JsonObject appetite = node(o, "appetite");
        appetite.addProperty("x", 0);
        appetite.addProperty("y", 0);
        appetite.add("passives", JsonParser.parseString("[\"a\",\"b\",\"c\",\"d\",\"e\",\"f\",\"g\",\"h\"]"));      // eight passives
        JsonObject lotus = node(o, "crimson_lotus");
        lotus.add("parents", JsonParser.parseString("[{\"node\": \"roar\", \"level\": 3}, \"appetite\", \"iron_fist\", {\"node\": \"iron_fist\", \"level\": 1}]"));
        o.addProperty("name", "n".repeat(64));
        o.addProperty("desc", "d".repeat(400));
        o.addProperty("book_lore", "l".repeat(200));
        RouteSchema.Parsed p = RouteSchema.parse(ID, o);
        // the lotus lists iron_fist twice on purpose above: that one IS refused, which also proves the duplicate check is still on at the edge
        assertTrue(all(p.problems()).contains("listed twice"), all(p.problems()));
        lotus.add("parents", JsonParser.parseString("[{\"node\": \"roar\", \"level\": 3}, \"appetite\", \"iron_fist\"]"));
        p = RouteSchema.parse(ID, o);
        assertTrue(p.clean(), all(p.problems()));
        assertEquals(10, p.route().node("iron_fist").maxLevel());
        assertEquals(50, p.route().node("iron_fist").costOf(10));
        assertEquals(10, p.route().node("roar").parents().get(0).level());
        assertEquals(8, p.route().node("appetite").passives().size());
        assertEquals(3, p.route().node("crimson_lotus").parents().size());

        JsonObject sixteen = base();
        JsonObject tables = new JsonObject();
        for (int i = 0; i < 16; i++) tables.add("t" + i, JsonParser.parseString("[1]"));
        node(sixteen, "roar").add("values", tables);
        assertTrue(RouteSchema.parse(ID, sixteen).clean(), "sixteen tables is allowed");
        JsonObject sixParents = JsonParser.parseString("""
                { "name": "Six", "book": "nusmp:six_book", "class_id": "six", "color": "#010203",
                  "nodes": [ { "id": "r1", "x": 0 }, { "id": "r2", "x": 1 }, { "id": "r3", "x": 2 }, { "id": "r4", "x": 3 }, { "id": "r5", "x": 4 }, { "id": "r6", "x": 5 },
                             { "id": "top", "parents": ["r1", "r2", "r3", "r4", "r5", "r6"], "x": 6 } ] }""").getAsJsonObject();
        RouteSchema.Parsed six = RouteSchema.parse(ID, sixParents);
        assertTrue(six.clean(), all(six.problems()));
        assertEquals(6, six.route().node("top").parents().size(), "six parents is the limit and is allowed");
    }

    // ------------------------------------------------------------------ a route that is wrong is refused whole
    @Test
    void aFileThatIsNotAnObjectOrNotJsonIsRefused() {
        refused(new JsonArray(), "must be a JSON object");
        refused(new JsonPrimitive("hello"), "must be a JSON object");
        RouteSchema.Parsed bad = RouteSchema.parse(ID, "{ \"name\": ");
        assertNull(bad.route());
        assertTrue(all(bad.problems()).contains("not valid JSON"), all(bad.problems()));
        assertNull(RouteSchema.parse(ID, "").route());
    }

    @Test
    void theRouteIdMustBeANamespaceAndALowerCaseSnakeFileName() {
        for (String bad : new String[]{"fire", "nusmp:Fire", "nusmp:a/b", "nusmp:", ":x", "nusmp:1fire", "a:b:c", "NUSMP:fire"}) {
            RouteSchema.Parsed p = RouteSchema.parse(bad, base());
            assertNull(p.route(), bad);
            assertTrue(all(p.problems()).contains("route id"), bad + ": " + all(p.problems()));
        }
        assertTrue(RouteSchema.validRouteId("nusmp:fire_dragon_slayer"));
        assertTrue(RouteSchema.validRouteId("my-pack.v2:ice_make"));
        assertFalse(RouteSchema.validRouteId(null));
    }

    @Test
    void everyRequiredRouteFieldMustBePresentAndValid() {
        for (String field : new String[]{"name", "book", "class_id", "color", "nodes"}) {
            JsonObject o = base();
            o.remove(field);
            refused(o, field + ": is required");
        }
        JsonObject blank = base();
        blank.addProperty("name", "   ");
        refused(blank, "name: must not be blank");
        JsonObject longName = base();
        longName.addProperty("name", "x".repeat(65));
        refused(longName, "name: is 65 characters long");
        JsonObject notText = base();
        notText.addProperty("name", 5);
        refused(notText, "name: must be text");
    }

    @Test
    void theBookMustBeALowerCaseResourceLocation() {
        for (String bad : new String[]{"Nusmp:Fang", "fang", "nusmp:", "nusmp:fang book", "a:b:c", "nusmp:" + "x".repeat(70)}) {
            JsonObject o = base();
            o.addProperty("book", bad);
            refused(o, "book: ");
        }
    }

    @Test
    void theClassIdMustBeLowerCaseSnake() {
        for (String bad : new String[]{"Fire Dragon", "1fire", "fire-dragon", "FIRE", "_x"}) {
            JsonObject o = base();
            o.addProperty("class_id", bad);
            refused(o, "class_id: ");
        }
        JsonObject empty = base();
        empty.addProperty("class_id", "");
        refused(empty, "class_id: ");
    }

    @Test
    void theColourMustBeAHashAndSixHexDigits() {
        for (String bad : new String[]{"red", "#E8541", "E8541C", "#GGGGGG", "#E8541CFF", ""}) {
            JsonObject o = base();
            o.addProperty("color", bad);
            refused(o, "color: ");
        }
        JsonObject number = base();
        number.addProperty("color", 0xE8541C);
        refused(number, "color: ");
        assertEquals(0xE8541C, RouteSchema.parseColor("#e8541c"));
        assertEquals(-1, RouteSchema.parseColor(null));
        assertEquals("#00FF7F", RouteSchema.colorString(0x00FF7F));
    }

    @Test
    void theNumbersOfTheRouteHaveTheirRanges() {
        for (Object[] c : new Object[][]{{"duplicate_sp", 101}, {"duplicate_sp", -1}, {"duplicate_sp", 1.5}, {"loot_weight", 1001}, {"loot_weight", -3}}) {
            JsonObject o = base();
            o.add((String) c[0], new JsonPrimitive((Number) c[1]));
            refused(o, c[0] + ": must be a whole number");
        }
        JsonObject text = base();
        text.addProperty("loot_weight", "2");
        refused(text, "loot_weight: must be a whole number");
    }

    @Test
    void theNodesArrayMustExistHaveBetweenOneAndFortyNodesAndAtLeastOneRoot() {
        JsonObject notArray = base();
        notArray.addProperty("nodes", "none");
        refused(notArray, "nodes: must be an array");
        JsonObject empty = base();
        empty.add("nodes", new JsonArray());
        refused(empty, "at least one node");
        JsonObject many = base();
        JsonArray fortyOne = new JsonArray();
        for (int i = 0; i < 41; i++) {
            JsonObject n = new JsonObject();
            n.addProperty("id", "n" + i);
            fortyOne.add(n);
        }
        many.add("nodes", fortyOne);
        refused(many, "41 nodes, the limit is 40");
        JsonArray forty = new JsonArray();
        for (int i = 0; i < 40; i++) {
            JsonObject n = new JsonObject();
            n.addProperty("id", "n" + i);
            n.addProperty("x", i % 13);
            n.addProperty("y", i / 13);
            forty.add(n);
        }
        JsonObject exactly = base();
        exactly.add("nodes", forty);
        assertTrue(RouteSchema.parse(ID, exactly).clean(), "exactly forty nodes is allowed");
        JsonObject onlyChild = base();
        JsonArray one = new JsonArray();
        one.add(node(onlyChild, "roar"));
        onlyChild.add("nodes", one);
        refused(onlyChild, "no node could be loaded");
    }

    // ------------------------------------------------------------------ a node that is wrong is skipped, with the nodes that need it
    @Test
    void aNodeIdMustBeLowerCaseSnake() {
        for (String bad : new String[]{"Roar", "1roar", "roar!", ""}) {
            JsonObject o = base();
            node(o, "roar").addProperty("id", bad);
            RouteSchema.Parsed p = RouteSchema.parse(ID, o);
            assertNotNull(p.route(), bad);
            assertTrue(all(p.problems()).contains("\"id\" must be lower-case snake"), all(p.problems()));
            assertNull(p.route().node("roar"), "the renamed node is gone");
            assertNull(p.route().node("crimson_lotus"), "the Secret Art needed roar, which no longer exists under that name");
            assertNotNull(p.route().node("iron_fist"));
        }
    }

    @Test
    void aSecondNodeWithTheSameIdIsSkippedAndTheFirstOneStays() {
        JsonObject dup = base();
        node(dup, "appetite").addProperty("id", "roar");               // appetite becomes a second "roar"
        RouteSchema.Parsed p = RouteSchema.parse(ID, dup);
        assertNotNull(p.route());
        assertTrue(all(p.problems()).contains("already used by an earlier node"), all(p.problems()));
        assertEquals(List.of("iron_fist", "roar"), p.route().nodes().stream().map(SkillNode::id).toList(), "the lotus needed 'appetite', which is now gone as well");
        assertEquals(List.of(2, 3, 4), p.route().node("roar").costs(), "the node that stays is the first roar");
    }

    @Test
    void theNumbersOfANodeHaveTheirRanges() {
        String[][] cases = {{"cost", "0"}, {"cost", "51"}, {"cost", "1.5"}, {"cost", "\"2\""}, {"max_level", "0"}, {"max_level", "11"}, {"x", "13"}, {"x", "-1"}, {"y", "10"}, {"y", "-1"}};
        for (String[] c : cases) {
            JsonObject o = base();
            node(o, "appetite").add(c[0], JsonParser.parseString(c[1]));
            nodeSkipped(o, "appetite", c[0] + ": must be a whole number");
        }
    }

    @Test
    void costsAreNoLongerThanMaxLevelAndEachWithinRange() {
        JsonObject tooMany = base();
        node(tooMany, "roar").add("costs", JsonParser.parseString("[2, 3, 4, 5]"));
        nodeSkipped(tooMany, "roar", "costs: 4 prices for a node with max_level 3");
        for (String bad : new String[]{"[0, 3]", "[2, 99]", "[\"x\"]", "[1.5]"}) {
            JsonObject o = base();
            node(o, "roar").add("costs", JsonParser.parseString(bad));
            nodeSkipped(o, "roar", "costs[");
        }
        JsonObject notArray = base();
        node(notArray, "roar").addProperty("costs", 5);
        nodeSkipped(notArray, "roar", "costs: must be an array");
        JsonObject shorter = base();
        node(shorter, "roar").add("costs", JsonParser.parseString("[2]"));
        RouteSchema.Parsed ok = RouteSchema.parse(ID, shorter);
        assertTrue(ok.clean() || ok.problems().isEmpty(), "a shorter list is fine: its last price repeats");
        assertEquals(2, ok.route().node("roar").costOf(3));
    }

    @Test
    void valuesAreTablesOfFiniteNumbers() {
        JsonObject nan = base();
        node(nan, "roar").getAsJsonObject("values").add("damage", new JsonPrimitive(Double.NaN));
        nodeSkipped(nan, "roar", "values.damage: must be a finite number");
        JsonObject inf = base();
        JsonArray arr = new JsonArray();
        arr.add(1);
        arr.add(Double.POSITIVE_INFINITY);
        node(inf, "roar").getAsJsonObject("values").add("damage", arr);
        nodeSkipped(inf, "roar", "values.damage[1]");
        JsonObject huge = base();
        node(huge, "roar").getAsJsonObject("values").addProperty("damage", 1.0e12);
        nodeSkipped(huge, "roar", "values.damage");
        JsonObject text = base();
        node(text, "roar").getAsJsonObject("values").addProperty("damage", "lots");
        nodeSkipped(text, "roar", "values.damage: must be a number");
        JsonObject empty = base();
        node(empty, "roar").getAsJsonObject("values").add("damage", new JsonArray());
        nodeSkipped(empty, "roar", "values.damage: needs 1 to 10 numbers");
        JsonObject eleven = base();
        node(eleven, "roar").getAsJsonObject("values").add("damage", JsonParser.parseString("[1,2,3,4,5,6,7,8,9,10,11]"));
        nodeSkipped(eleven, "roar", "values.damage: needs 1 to 10 numbers");
        JsonObject name = base();
        node(name, "roar").getAsJsonObject("values").add("Damage", JsonParser.parseString("[1]"));
        nodeSkipped(name, "roar", "values.Damage: the name must be lower-case snake");
        JsonObject notObject = base();
        node(notObject, "roar").add("values", JsonParser.parseString("[1, 2]"));
        nodeSkipped(notObject, "roar", "values: must be an object");
        JsonObject manyTables = base();
        JsonObject v = new JsonObject();
        for (int i = 0; i < 17; i++) v.add("t" + i, JsonParser.parseString("[1]"));
        node(manyTables, "roar").add("values", v);
        nodeSkipped(manyTables, "roar", "values: 17 tables, the limit is 16");
    }

    @Test
    void theSkillMustBeAResourceLocationThatNoOtherNodeGrants() {
        for (String bad : new String[]{"Fire:Roar", "noname", "nusmp:", "nusmp:a b"}) {
            JsonObject o = base();
            node(o, "roar").addProperty("skill", bad);
            nodeSkipped(o, "roar", "skill: ");
        }
        JsonObject same = base();
        node(same, "crimson_lotus").addProperty("skill", "nusmp:ft_fire_dragons_roar");
        nodeSkipped(same, "crimson_lotus", "already granted by node 'roar'");
        JsonObject none = base();
        node(none, "roar").addProperty("skill", "");
        RouteSchema.Parsed ok = RouteSchema.parse(ID, none);
        assertTrue(ok.clean(), "an empty skill means the node grants nothing: " + all(ok.problems()));
        assertFalse(ok.route().node("roar").grantsSkill());
    }

    @Test
    void passivesAreSnakeIds() {
        JsonObject o = base();
        node(o, "appetite").add("passives", JsonParser.parseString("[\"Eat Fire\"]"));
        nodeSkipped(o, "appetite", "passives[0]");
        JsonObject many = base();
        node(many, "appetite").add("passives", JsonParser.parseString("[\"a\",\"b\",\"c\",\"d\",\"e\",\"f\",\"g\",\"h\",\"i\"]"));
        nodeSkipped(many, "appetite", "passives: 9 entries, the limit is 8");
        JsonObject notArray = base();
        node(notArray, "appetite").addProperty("passives", "eat_fire");
        nodeSkipped(notArray, "appetite", "passives: must be an array");
    }

    // ------------------------------------------------------------------ parents, order and cycles
    @Test
    void aParentMustBeANodeOfTheRouteThatComesEarlierAndReachesTheLevelAsked() {
        JsonObject unknown = base();
        node(unknown, "roar").add("parents", JsonParser.parseString("[\"ghost\"]"));
        nodeSkipped(unknown, "roar", "'ghost' is not a node of this route");
        JsonObject self = base();
        node(self, "roar").add("parents", JsonParser.parseString("[\"roar\"]"));
        nodeSkipped(self, "roar", "cannot be its own parent");
        JsonObject tooHigh = base();
        node(tooHigh, "roar").add("parents", JsonParser.parseString("[{\"node\": \"iron_fist\", \"level\": 6}]"));
        nodeSkipped(tooHigh, "roar", "asked for at level 6 but it only goes up to 5");
        JsonObject twice = base();
        node(twice, "roar").add("parents", JsonParser.parseString("[\"iron_fist\", {\"node\": \"iron_fist\", \"level\": 2}]"));
        nodeSkipped(twice, "roar", "listed twice");
        JsonObject badLevel = base();
        node(badLevel, "roar").add("parents", JsonParser.parseString("[{\"node\": \"iron_fist\", \"level\": 0}]"));
        nodeSkipped(badLevel, "roar", "\"level\" must be a whole number from 1 to 10");
        JsonObject notNode = base();
        node(notNode, "roar").add("parents", JsonParser.parseString("[5]"));
        nodeSkipped(notNode, "roar", "must be a node id");
        JsonObject seven = base();
        node(seven, "roar").add("parents", JsonParser.parseString("[\"a\",\"b\",\"c\",\"d\",\"e\",\"f\",\"g\"]"));
        nodeSkipped(seven, "roar", "parents: 7 entries, the limit is 6");
    }

    @Test
    void aParentThatComesLaterIsRefusedAndSoIsAnyCycle() {
        JsonObject later = base();
        JsonArray reordered = new JsonArray();
        reordered.add(node(later, "roar"));            // roar before its parent iron_fist
        reordered.add(node(later, "iron_fist"));
        reordered.add(node(later, "appetite"));
        reordered.add(node(later, "crimson_lotus"));
        later.add("nodes", reordered);
        RouteSchema.Parsed p = RouteSchema.parse(ID, later);
        assertNotNull(p.route());
        assertNull(p.route().node("roar"), "roar needs iron_fist, which comes after it");
        assertNull(p.route().node("crimson_lotus"), "and the lotus, which needs roar, goes with it");
        assertNotNull(p.route().node("iron_fist"));
        assertNotNull(p.route().node("appetite"));
        assertTrue(all(p.problems()).contains("comes later in the file"), all(p.problems()));

        JsonObject cycle = JsonParser.parseString("""
                { "name": "Loop", "book": "nusmp:loop_book", "class_id": "loop", "color": "#112233",
                  "nodes": [ { "id": "root", "x": 0 },
                             { "id": "a", "parents": ["b"], "x": 1 },
                             { "id": "b", "parents": ["a"], "x": 2 } ] }""").getAsJsonObject();
        RouteSchema.Parsed c = RouteSchema.parse(ID, cycle);
        assertNotNull(c.route());
        assertEquals(1, c.route().nodes().size(), "both nodes of the cycle are refused");
        assertEquals("root", c.route().nodes().get(0).id());
        assertTrue(all(c.problems()).contains("rules out cycles") || all(c.problems()).contains("comes later"), all(c.problems()));
    }

    @Test
    void aBadNodeTakesOnlyTheNodesThatNeedItWithIt() {
        JsonObject o = base();
        node(o, "roar").addProperty("cost", 0);                        // roar is bad; the lotus needs roar; appetite does not
        RouteSchema.Parsed p = RouteSchema.parse(ID, o);
        assertNotNull(p.route());
        assertNull(p.route().node("roar"));
        assertNull(p.route().node("crimson_lotus"), "the Secret Art needs roar, so it goes too");
        assertNotNull(p.route().node("iron_fist"), "the root is untouched");
        assertNotNull(p.route().node("appetite"), "the other branch is untouched");
        assertTrue(all(p.problems()).contains("node 'crimson_lotus': parents: needs 'roar', which was skipped"), all(p.problems()));
        assertFalse(p.clean());
        assertTrue(p.ok());
    }

    @Test
    void ifTheOnlyRootIsBadTheRouteIsRefused() {
        JsonObject o = base();
        node(o, "iron_fist").addProperty("cost", 99);
        RouteSchema.Parsed p = RouteSchema.parse(ID, o);
        assertNull(p.route(), "every other node needs the root, so nothing is left");
        assertTrue(all(p.problems()).contains("no node could be loaded"), all(p.problems()));
    }

    @Test
    void aNodeThatIsNotAnObjectIsReportedByPosition() {
        JsonObject o = base();
        o.getAsJsonArray("nodes").add("surprise");
        RouteSchema.Parsed p = RouteSchema.parse(ID, o);
        assertNotNull(p.route());
        assertEquals(4, p.route().nodes().size());
        assertTrue(all(p.problems()).contains("node #5: a node must be a JSON object"), all(p.problems()));
    }

    // ------------------------------------------------------------------ warnings never stop a load
    @Test
    void oddButHarmlessThingsAreWarnings() {
        JsonObject o = base();
        o.addProperty("maxLevel", 4);                                  // a misspelt route field
        node(o, "roar").addProperty("maxLevel", 2);                    // a misspelt node field
        node(o, "appetite").addProperty("x", 2);
        node(o, "appetite").addProperty("y", 3);                       // the same cell as roar
        node(o, "iron_fist").addProperty("cost", 4);                   // cost 4 but... iron_fist has no costs list, so no warning
        node(o, "roar").addProperty("cost", 9);                        // costs[0] is 2
        node(o, "iron_fist").getAsJsonObject("values").add("damage", JsonParser.parseString("[1,2,3,4,5,6]"));      // six entries for max_level 5
        RouteSchema.Parsed p = RouteSchema.parse(ID, o);
        assertTrue(p.clean(), "none of these is a problem: " + all(p.problems()));
        String w = all(p.warnings());
        assertTrue(w.contains("unknown field \"maxLevel\" is ignored (did you mean \"max_level\"?)"), w);
        assertTrue(w.contains("'roar' and 'appetite' are both at x=2, y=3") || w.contains("'appetite' and 'roar'"), w);
        assertTrue(w.contains("\"cost\" is 9 but costs[0] is 2"), w);
        assertTrue(w.contains("values.damage has 6 entries but the node only goes to level 5"), w);
        assertEquals(4, p.route().nodes().size());
    }

    // ------------------------------------------------------------------ the small checks
    @Test
    void theSmallCheckersMeanWhatTheyDocument() {
        assertTrue(RouteSchema.validSnake("fire_dragons_roar"));
        assertTrue(RouteSchema.validSnake("a1"));
        assertFalse(RouteSchema.validSnake("Fire"));
        assertFalse(RouteSchema.validSnake("1a"));
        assertFalse(RouteSchema.validSnake(""));
        assertFalse(RouteSchema.validSnake(null));
        assertTrue(RouteSchema.validResource("minecraft:blaze_rod"));
        assertTrue(RouteSchema.validResource("nusmp:items/skill_book"));
        assertFalse(RouteSchema.validResource("blaze_rod"));
        assertFalse(RouteSchema.validResource("nusmp:"));
        assertFalse(RouteSchema.validResource(":x"));
        assertFalse(RouteSchema.validResource("Nusmp:x"));
        assertEquals("Iron Fist", RouteSchema.titleCase("iron_fist"));
        assertEquals("Roar", RouteSchema.titleCase("roar"));
    }
}
