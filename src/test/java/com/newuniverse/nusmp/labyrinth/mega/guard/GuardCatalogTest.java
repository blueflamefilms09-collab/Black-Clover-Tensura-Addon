package com.newuniverse.nusmp.labyrinth.mega.guard;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the boss chain of a sector (the Guardian's hall, the antechamber behind its Gatekeeper, the Way Onward that ends in a rubble seal) is built by a script
 * and checked there, but the generator and the Java code depend on promises of these templates that no compiler sees: the arena must have one entry and
 * one chain exit that turns into the sector Gatekeeper door, its markers must stand where the altar and the door tests look for them, the antechamber must
 * offer both roads on its two flanks, and every Way Onward must end in a sealed socket with the expansion marker directly above it, facing the way the
 * next sector will attach. A mistake of one block here makes a sector that cannot grow or cannot be finished, and it would first show in a player's world.
 * This test reads the REAL template catalog (the {@code catalog.json} that {@code tools/labyrinth/mega_catalog.py} exports from the shipped {@code .nbt}
 * files, never hand-made shapes) and asserts those promises for exactly the thirteen templates of this builder. It also pins the file-name prefixes
 * ({@code straight_}, {@code left_}, {@code right_}) to the way each corridor ends, because the pool generator sorts the Way Onward templates by prefix and the
 * planner picks a pool by the direction it needs. If the catalog holds none of the thirteen (a tree whose catalog was not rebuilt after they were added) the
 * checks pass with nothing to check, so the test never blocks a build that does not ship them; if it holds some but not all, that fails.
 */
class GuardCatalogTest {
    private static final List<String> MINE = List.of("arena/guardian_hall_a", "arena/guardian_hall_b", "antechamber/antechamber_a", "antechamber/antechamber_b",
            "onward/straight_a", "onward/straight_b", "onward/straight_c", "onward/left_a", "onward/left_b", "onward/left_c", "onward/right_a", "onward/right_b",
            "onward/right_c");
    private static final String DOOR = "nusmp:mega_door", CHAIN = "nusmp:mega_chain", VAULT = "nusmp:mega_vault", ONWARD = "nusmp:mega_onward";
    private static final String EMPTY = "minecraft:empty", AIR = "minecraft:air";

    /** Template name (without the namespace) to its catalog entry, for the templates of this builder that the catalog holds. */
    private static Map<String, JsonObject> rooms() throws IOException {
        try (InputStream in = GuardCatalogTest.class.getResourceAsStream("/mega/catalog.json")) {
            assertTrue(in != null, "src/test/resources/mega/catalog.json is missing (python3 tools/labyrinth/mega_catalog.py writes it)");
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject all = root.getAsJsonObject("templates");
            Map<String, JsonObject> out = new LinkedHashMap<>();
            for (String name : MINE) {
                JsonElement e = all.get("nusmp:mega/" + name);
                if (e != null) out.put(name, e.getAsJsonObject());
            }
            return out;
        }
    }

    private static List<JsonObject> markers(JsonObject room, String kind) {
        List<JsonObject> out = new ArrayList<>();
        for (JsonElement e : room.getAsJsonArray("markers")) {
            JsonObject m = e.getAsJsonObject();
            if (kind.equals(m.get("kind").getAsString())) out.add(m);
        }
        return out;
    }

    private static String arg(JsonObject marker, String key) {
        JsonObject args = marker.getAsJsonObject("args");
        return args.has(key) ? args.get(key).getAsString() : null;
    }

    private static List<JsonObject> sockets(JsonObject room, String name) {
        List<JsonObject> out = new ArrayList<>();
        for (JsonElement e : room.getAsJsonArray("sockets")) {
            JsonObject s = e.getAsJsonObject();
            if (name == null || name.equals(s.get("name").getAsString())) out.add(s);
        }
        return out;
    }

    private static int size(JsonObject room, int axis) { return room.getAsJsonArray("size").get(axis).getAsInt(); }

    /** The position of a socket along the face it stands on (x for a north or south face, z for an east or west face). */
    private static int offset(JsonObject socket) {
        String f = socket.get("facing").getAsString();
        return f.equals("north") || f.equals("south") ? socket.get("x").getAsInt() : socket.get("z").getAsInt();
    }

    private static boolean pos(JsonObject m, int x, int y, int z) {
        return m.get("x").getAsInt() == x && m.get("y").getAsInt() == y && m.get("z").getAsInt() == z;
    }

    @Test
    void eitherAllThirteenTemplatesAreCataloguedOrNone() throws IOException {
        int n = rooms().size();
        assertTrue(n == 0 || n == MINE.size(), "the catalog holds " + n + " of the " + MINE.size() + " templates of the guard builder (all or none expected)");
    }

    @Test
    void sizesAreWholeModulesAndFortyEightHigh() throws IOException {
        for (Map.Entry<String, JsonObject> e : rooms().entrySet()) {
            String n = e.getKey();
            JsonObject r = e.getValue();
            assertEquals(48, size(r, 1), n + " height");
            int sx, sz;
            if (n.startsWith("arena/")) { sx = 45; sz = 45; }
            else if (n.startsWith("antechamber/")) { sx = 27; sz = 27; }
            else if (n.startsWith("onward/straight_")) { sx = 9; sz = n.endsWith("_c") ? 18 : 36; }
            else { sx = n.endsWith("_c") ? 18 : 27; sz = sx; }
            assertEquals(sx, size(r, 0), n + " x size");
            assertEquals(sz, size(r, 2), n + " z size");
        }
    }

    @Test
    void theArenaHasOneEntryAndOneChainExitThatBecomesTheGatekeeper() throws IOException {
        for (Map.Entry<String, JsonObject> e : rooms().entrySet()) {
            if (!e.getKey().startsWith("arena/")) continue;
            String n = e.getKey();
            JsonObject r = e.getValue();
            assertEquals(2, sockets(r, null).size(), n + " is a dead end of the chain: an entry and the exit, nothing else");
            List<JsonObject> entry = sockets(r, DOOR), exit = sockets(r, CHAIN);
            assertEquals(1, entry.size(), n + " entry sockets");
            assertEquals(1, exit.size(), n + " chain sockets");
            JsonObject in = entry.get(0), out = exit.get(0);
            assertEquals("south", in.get("facing").getAsString(), n + " entry face");
            assertEquals(22, offset(in), n + " entry offset");
            assertEquals(EMPTY, in.get("pool").getAsString(), n + ": the arena is attached TO a door, so its entry offers no pool");
            assertEquals(AIR, in.get("final").getAsString(), n + " entry final state (an open hole)");
            assertEquals("north", out.get("facing").getAsString(), n + " exit face");
            assertEquals(22, offset(out), n + " exit offset");
            assertEquals("nusmp:mega/antechamber", out.get("pool").getAsString(), n + " exit pool");
            assertEquals(CHAIN, out.get("target").getAsString(), n + " exit target");
            String fin = out.get("final").getAsString();
            assertTrue(fin.startsWith("nusmp:gatekeeper_door[") && fin.contains("kind=sector") && fin.contains("open=false"), n + " exit final state is the sector Gatekeeper door: " + fin);
        }
    }

    @Test
    void theArenaMarkersStandWhereTheAltarAndTheDoorLookForThem() throws IOException {
        for (Map.Entry<String, JsonObject> e : rooms().entrySet()) {
            if (!e.getKey().startsWith("arena/")) continue;
            String n = e.getKey();
            JsonObject r = e.getValue();
            List<JsonObject> spawn = markers(r, "guardian_spawn"), gate = markers(r, "gatekeeper");
            assertEquals(1, spawn.size(), n + " guardian_spawn markers");
            assertEquals(1, gate.size(), n + " gatekeeper markers");
            assertTrue(pos(spawn.get(0), 22, 12, 22), n + ": guardian_spawn is the air cell above the altar, the centre of the hall at row 12");
            assertTrue(pos(gate.get(0), 22, 10, 2), n + ": gatekeeper is the cell in front of the door slab's middle, one inside the wall");
            assertEquals("north", arg(gate.get(0), "facing"), n + " gatekeeper faces the door");
            assertTrue(markers(r, "spawn").isEmpty() && markers(r, "patrol").isEmpty(), n + ": the hall is the Guardian's, it has no spawn or patrol markers");
        }
    }

    @Test
    void theAntechamberOffersBothRoadsOnItsFlanks() throws IOException {
        for (Map.Entry<String, JsonObject> e : rooms().entrySet()) {
            if (!e.getKey().startsWith("antechamber/")) continue;
            String n = e.getKey();
            JsonObject r = e.getValue();
            assertEquals(3, sockets(r, null).size(), n + " sockets (entry, vault road, onward road)");
            List<JsonObject> entry = sockets(r, CHAIN), vault = sockets(r, VAULT), onward = sockets(r, ONWARD);
            assertTrue(entry.size() == 1 && vault.size() == 1 && onward.size() == 1, n + " needs exactly one socket of each of the three chain names");
            assertEquals("south", entry.get(0).get("facing").getAsString(), n + " entry face");
            assertEquals(13, offset(entry.get(0)), n + " entry offset");
            assertEquals(EMPTY, entry.get(0).get("pool").getAsString(), n + " entry pool");
            assertEquals(AIR, entry.get(0).get("final").getAsString(), n + " entry final state");
            String vf = vault.get(0).get("facing").getAsString(), of = onward.get(0).get("facing").getAsString();
            assertTrue((vf.equals("east") && of.equals("west")) || (vf.equals("west") && of.equals("east")), n + ": the two roads leave on opposite flanks (vault " + vf + ", onward " + of + ")");
            assertEquals(13, offset(vault.get(0)), n + " vault offset");
            assertEquals(13, offset(onward.get(0)), n + " onward offset");
            assertEquals("nusmp:mega/vault", vault.get(0).get("pool").getAsString(), n + " vault pool");
            assertEquals("nusmp:mega/onward", onward.get(0).get("pool").getAsString(), n + " onward pool");
            assertEquals(AIR, vault.get(0).get("final").getAsString(), n + " vault final state");
            assertEquals(AIR, onward.get(0).get("final").getAsString(), n + " onward final state");
            assertEquals(1, markers(r, "patrol").size(), n + " patrol markers");
            assertTrue(markers(r, "spawn").isEmpty(), n + " is a calm lobby: no spawn markers");
        }
    }

    @Test
    void everyWayOnwardEndsInASealWithItsMarkerAndItsNamePromisesTheDirection() throws IOException {
        Map<String, String> endFace = Map.of("straight", "north", "left", "west", "right", "east");
        for (Map.Entry<String, JsonObject> e : rooms().entrySet()) {
            if (!e.getKey().startsWith("onward/")) continue;
            String n = e.getKey();
            JsonObject r = e.getValue();
            String kind = n.substring("onward/".length(), n.indexOf('_'));
            assertEquals(2, sockets(r, null).size(), n + " sockets (the entry and the sealed end)");
            List<JsonObject> entry = sockets(r, ONWARD), end = sockets(r, DOOR);
            assertTrue(entry.size() == 1 && end.size() == 1, n + " needs one onward entry and one sealed end");
            JsonObject in = entry.get(0), seal = end.get(0);
            assertEquals("south", in.get("facing").getAsString(), n + " entry face");
            assertEquals(EMPTY, in.get("pool").getAsString(), n + " entry pool");
            assertEquals(AIR, in.get("final").getAsString(), n + " entry final state");
            assertEquals(endFace.get(kind), seal.get("facing").getAsString(), n + ": a " + kind + " corridor ends on the " + endFace.get(kind) + " face");
            assertEquals(EMPTY, seal.get("pool").getAsString(), n + " seal pool: a sealed end is neither filled nor capped");
            assertEquals("minecraft:cobbled_deepslate", seal.get("final").getAsString(), n + " seal final state");
            assertEquals(4, offset(seal), n + " seal offset");
            List<JsonObject> exp = markers(r, "expansion_socket");
            assertEquals(1, exp.size(), n + " expansion_socket markers");
            JsonObject m = exp.get(0);
            assertTrue(pos(m, seal.get("x").getAsInt(), seal.get("y").getAsInt() + 1, seal.get("z").getAsInt()), n + ": the expansion marker stands directly above the seal's jigsaw cell");
            assertEquals(seal.get("facing").getAsString(), arg(m, "facing"), n + ": the marker faces the way the next sector attaches");
            assertTrue("3".equals(arg(m, "w")) && "4".equals(arg(m, "h")) && "2".equals(arg(m, "d")), n + ": the seal is 3 wide, 4 high and 2 deep");
            assertEquals(1, markers(r, "patrol").size(), n + " patrol markers");
            assertTrue(markers(r, "spawn").isEmpty(), n + ": a corridor of passage has no spawn markers");
        }
    }

    @Test
    void theLeftAndRightCorridorsEnterOnTheSideThatLetsThemTurn() throws IOException {
        for (Map.Entry<String, JsonObject> e : rooms().entrySet()) {
            String n = e.getKey();
            if (!n.startsWith("onward/left_") && !n.startsWith("onward/right_")) continue;
            JsonObject in = sockets(e.getValue(), ONWARD).get(0);
            int width = size(e.getValue(), 0);
            int wantOffset = n.startsWith("onward/left_") ? width - 5 : 4;                 // a left turn needs the entry on the east side of the footprint (22 of 27, 13 of 18)
            assertEquals(wantOffset, offset(in), n + " entry offset");
        }
    }

    @Test
    void everyMarkerIsInsideTheTemplateAndNeverSharesACell() throws IOException {
        for (Map.Entry<String, JsonObject> e : rooms().entrySet()) {
            JsonObject r = e.getValue();
            Set<String> cells = new HashSet<>();
            for (JsonElement me : r.getAsJsonArray("markers")) {
                JsonObject m = me.getAsJsonObject();
                int x = m.get("x").getAsInt(), y = m.get("y").getAsInt(), z = m.get("z").getAsInt();
                String where = e.getKey() + " " + m.get("kind").getAsString() + " at " + x + "," + y + "," + z;
                assertTrue(x >= 0 && x < size(r, 0) && z >= 0 && z < size(r, 2), where + " is outside the footprint");
                assertTrue(y > 8 && y < 40, where + " is not above the floor row");
                assertTrue(cells.add(x + "," + y + "," + z), where + " shares its cell with another marker");
            }
        }
    }
}
