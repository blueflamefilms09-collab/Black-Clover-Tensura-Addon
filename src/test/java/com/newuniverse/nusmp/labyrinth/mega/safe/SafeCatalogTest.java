package com.newuniverse.nusmp.labyrinth.mega.safe;

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
 * Why: the Landings and Safe Zones are built by a script and checked there (a template that fails the walk-through is never written), but the Java side
 * of resting trusts what the templates promise: a rest anchor is found by its data marker, the generator attaches rooms through their doors, and a
 * Landing must hand the player an arrival cell. This test reads the REAL template catalog (the {@code catalog.json} that {@code tools/labyrinth/mega_catalog.py}
 * exports from the shipped {@code .nbt} files, never hand-made shapes) and asserts the promises the Java code depends on, for exactly the eight rooms this
 * builder ships: the right size, two to four doors on module centres in the ordinary side pool, at least two seats, a hearth and one to three beds, one
 * or two patrol points, an arrival marker in a Landing and nowhere else, no spawn or faction marker, every marker well formed and inside the room, and
 * every hint a word the compat table knows (or the one documented decorative word). If the catalog holds none of these rooms (a stripped test tree) the
 * checks pass with nothing to check, so the test never blocks a build that does not ship them.
 */
class SafeCatalogTest {
    private static final List<String> MINE = List.of("landing/landing_a", "landing/landing_b", "safe/hearth_hall_a", "safe/hearth_hall_b",
            "safe/workshop_a", "safe/workshop_b", "safe/refectory", "safe/chapel");
    private static final Set<String> FORBIDDEN = Set.of("spawn", "faction", "trap", "pedestal", "guardian_spawn", "gatekeeper", "expansion_socket", "vault_center");
    private static final Set<String> FACINGS = Set.of("north", "east", "south", "west");
    private static final Set<String> SEAT_HINTS = Set.of("chair", "sofa");
    /** A kitchen fire the compat stove may replace, or a flame (the chapel's sacred fire) that no compat mod ever replaces. */
    private static final Set<String> HEARTH_HINTS = Set.of("stove", "brazier");
    private static final Set<String> FURNITURE_HINTS = Set.of("table", "counter", "cutting_board", "storage", "skillet", "cooking_pot");
    private static final int[] CENTRES = {4, 13, 22};
    private static final String SIDE_POOL = "nusmp:mega/side";
    private static final String DOOR = "nusmp:mega_door";

    /** Template name (without the namespace) to its catalog entry, for the rooms of this builder that the catalog holds. */
    private static Map<String, JsonObject> rooms() throws IOException {
        try (InputStream in = SafeCatalogTest.class.getResourceAsStream("/mega/catalog.json")) {
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
            if (kind == null || kind.equals(m.get("kind").getAsString())) out.add(m);
        }
        return out;
    }

    private static String arg(JsonObject marker, String key) {
        JsonObject args = marker.getAsJsonObject("args");
        return args.has(key) ? args.get(key).getAsString() : null;
    }

    private static boolean isLanding(String name) {
        return name.startsWith("landing/");
    }

    @Test
    void eitherAllEightRoomsAreCataloguedOrNone() throws IOException {
        int n = rooms().size();
        assertTrue(n == 0 || n == MINE.size(), "the catalog holds " + n + " of the " + MINE.size() + " rooms of the safe builder (all or none expected)");
    }

    @Test
    void sizesAreWholeModulesAndFortyEightHigh() throws IOException {
        for (Map.Entry<String, JsonObject> e : rooms().entrySet()) {
            var size = e.getValue().getAsJsonArray("size");
            int sx = size.get(0).getAsInt(), sy = size.get(1).getAsInt(), sz = size.get(2).getAsInt();
            assertEquals(48, sy, e.getKey() + " height");
            assertTrue(sx % 9 == 0 && sz % 9 == 0, e.getKey() + " is not a whole number of 9-block modules: " + sx + " x " + sz);
            boolean big = isLanding(e.getKey()) || e.getKey().contains("hearth_hall");
            boolean workshop = e.getKey().contains("workshop");
            int expectX = big ? 27 : 18, expectZ = big ? 27 : workshop ? 27 : 18;
            assertEquals(expectX, sx, e.getKey() + " x size");
            assertEquals(expectZ, sz, e.getKey() + " z size");
        }
    }

    @Test
    void doorsAreOrdinarySideSocketsOnModuleCentres() throws IOException {
        for (Map.Entry<String, JsonObject> e : rooms().entrySet()) {
            JsonObject room = e.getValue();
            int sx = room.getAsJsonArray("size").get(0).getAsInt(), sz = room.getAsJsonArray("size").get(2).getAsInt();
            var sockets = room.getAsJsonArray("sockets");
            int doors = sockets.size();
            assertTrue(doors >= 2 && doors <= 4, e.getKey() + " has " + doors + " doors (2 to 4 allowed)");
            if (isLanding(e.getKey())) assertEquals(4, doors, e.getKey() + " is a Landing and needs a door on every side");
            Set<String> seen = new HashSet<>();
            for (JsonElement se : sockets) {
                JsonObject s = se.getAsJsonObject();
                String where = e.getKey() + " door at " + s.get("x") + "," + s.get("z");
                assertEquals(SIDE_POOL, s.get("pool").getAsString(), where + " pool");
                assertEquals(DOOR, s.get("name").getAsString(), where + " name");
                assertEquals(DOOR, s.get("target").getAsString(), where + " target");
                assertEquals("minecraft:air", s.get("final").getAsString(), where + " final state");
                assertEquals(9, s.get("y").getAsInt(), where + " row");
                int x = s.get("x").getAsInt(), z = s.get("z").getAsInt();
                String facing = s.get("facing").getAsString();
                int lateral;
                switch (facing) {
                    case "north" -> { assertEquals(0, z, where + " is not on the north face"); lateral = x; }
                    case "south" -> { assertEquals(sz - 1, z, where + " is not on the south face"); lateral = x; }
                    case "west" -> { assertEquals(0, x, where + " is not on the west face"); lateral = z; }
                    case "east" -> { assertEquals(sx - 1, x, where + " is not on the east face"); lateral = z; }
                    default -> { assertTrue(false, where + " has the facing " + facing); lateral = -1; }
                }
                boolean centre = false;
                for (int c : CENTRES) if (c == lateral) centre = true;
                assertTrue(centre, where + " is not on a module centre (4, 13 or 22)");
                assertTrue(seen.add(facing + ":" + lateral), where + " is a duplicate");
            }
        }
    }

    @Test
    void restAnchorsAndPatrolsFollowTheBrief() throws IOException {
        for (Map.Entry<String, JsonObject> e : rooms().entrySet()) {
            JsonObject room = e.getValue();
            String n = e.getKey();
            assertTrue(markers(room, "seat").size() >= 2, n + " needs at least two seats");
            assertTrue(markers(room, "hearth").size() >= 1, n + " needs a hearth");
            int beds = markers(room, "bed").size();
            assertTrue(beds >= 1 && beds <= 3, n + " has " + beds + " beds (1 to 3)");
            int patrol = markers(room, "patrol").size();
            assertTrue(patrol >= 1 && patrol <= 2, n + " has " + patrol + " patrol markers (1 or 2)");
            for (String kind : FORBIDDEN) assertTrue(markers(room, kind).isEmpty(), n + " must not carry " + kind + " markers");
            assertEquals(isLanding(n) ? 1 : 0, markers(room, "arrival").size(), n + (isLanding(n) ? " needs exactly one arrival marker" : " must not have an arrival marker"));
        }
    }

    @Test
    void everyMarkerIsWellFormedAndUsesAHintTheCompatTableKnows() throws IOException {
        for (Map.Entry<String, JsonObject> e : rooms().entrySet()) {
            String n = e.getKey();
            for (JsonObject m : markers(e.getValue(), null)) {
                String kind = m.get("kind").getAsString();
                String where = n + " " + kind + " at " + m.get("x") + "," + m.get("y") + "," + m.get("z");
                String as = arg(m, "as");
                switch (kind) {
                    case "seat" -> {
                        assertTrue(as != null && SEAT_HINTS.contains(as), where + " has the hint " + as);
                        assertTrue(arg(m, "facing") != null && FACINGS.contains(arg(m, "facing")), where + " has the facing " + arg(m, "facing"));
                    }
                    case "hearth" -> assertTrue(as != null && HEARTH_HINTS.contains(as), where + " has the hint " + as);
                    case "bed" -> assertEquals("bed", as, where + " hint");
                    case "furniture" -> assertTrue(as != null && FURNITURE_HINTS.contains(as), where + " has the hint " + as);
                    case "patrol", "arrival" -> assertTrue(as == null, where + " takes no hint");
                    default -> assertTrue(false, where + " is a marker kind the safe rooms do not use");
                }
                if (as != null && !"brazier".equals(as)) {
                    assertTrue(CompatTable.KNOWN_HINTS.contains(as), where + ": the hint " + as + " is not one of CompatTable.KNOWN_HINTS");
                }
            }
        }
    }

    @Test
    void markersAreInsideTheRoomAndNeverShareACell() throws IOException {
        for (Map.Entry<String, JsonObject> e : rooms().entrySet()) {
            JsonObject room = e.getValue();
            var size = room.getAsJsonArray("size");
            Set<String> cells = new HashSet<>();
            for (JsonObject m : markers(room, null)) {
                int x = m.get("x").getAsInt(), y = m.get("y").getAsInt(), z = m.get("z").getAsInt();
                String where = e.getKey() + " " + m.get("kind").getAsString() + " at " + x + "," + y + "," + z;
                assertTrue(x >= 0 && x < size.get(0).getAsInt() && z >= 0 && z < size.get(2).getAsInt(), where + " is outside the footprint");
                assertTrue(y > 8 && y < 40, where + " is not above the floor row");
                assertTrue(cells.add(x + "," + y + "," + z), where + " shares its cell with another marker");
            }
        }
    }
}
