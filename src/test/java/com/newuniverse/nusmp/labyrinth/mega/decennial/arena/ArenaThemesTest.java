package com.newuniverse.nusmp.labyrinth.mega.decennial.arena;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.arc.arena.BlockSpec;
import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.BossArenaStructure.Effect;
import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.BossArenaStructure.Element;
import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.BossArenaStructure.Kind;
import com.newuniverse.nusmp.labyrinth.mega.decennial.arena.BossArenaStructure.Placed;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: a hall is a hardcoded build that nobody can see running, so every promise it makes is a property of its PLAN and is checked by walking the plan: the footprint holds every write, the boundary wall
 * is closed everywhere except at the two doorways the floor's template keeps, the ceiling is closed, there is a floor under every interior column, every interactive element is inside the room and has the
 * cells the plan says it has, a walk from the entry reaches the dais and (once the locked gate is open) the Gatekeeper and most of the floor, and the clearing round the altar holds only the dais. They
 * are checked for every hall at four sizes (the smallest, the floor's own 45, the hall's default and the largest), because a hall is built at 45 in a floor of the Mega-Dungeon and at its own size when an
 * admin builds it. Every block of every plan is looked up in the table of the game's real blocks and states, so a typo fails here and not on the first arrival of a party.
 */
class ArenaThemesTest {
    private static final Path VANILLA = Path.of("tools/labyrinth/vanilla_block_states_1_21_1.json");
    private static final Path VANILLA_NAMES = Path.of("tools/labyrinth/vanilla_blocks_1_21_1.txt");

    @Test
    void thereAreAtLeastTwentyFourDistinctHallsAndTheyAreTheRightSize() {
        List<String> ids = ArenaThemes.ids();
        assertTrue(ids.size() >= 24, "halls: " + ids.size());
        assertEquals(36, ids.size());
        assertEquals(ids.size(), new HashSet<>(ids).size());
        for (String id : ids) {
            ArenaThemes.Def d = ArenaThemes.def(id);
            assertTrue(id.matches("[a-z][a-z0-9_]{2,31}"), id);
            assertTrue(d.size() >= 41 && d.size() <= 61 && (d.size() & 1) == 1, id + " size " + d.size());
            assertTrue(List.of("holy", "evil", "any").contains(d.side()), id);
            assertTrue(d.title().length() > 6, id);
        }
        for (String need : List.of("graveyard", "ruined_throne_room", "cathedral", "sunken_temple", "volcanic_forge", "frozen_keep", "hanging_gardens", "bone_cathedral", "blood_arena", "mirror_hall",
                "clockwork_vault", "sky_bridge", "abyss_rift_ring", "witch_hollow", "forbidden_library", "colosseum", "dragon_hoard_cavern", "void_observatory", "hell_pit", "celestial_court"))
            assertTrue(ArenaThemes.exists(need), need);
        for (int k = 1; k <= 7; k++) assertTrue(ArenaThemes.exists("qliphoth_gate_" + k));
        assertTrue(ArenaThemes.exists("qliphoth_root_throne"));
        assertNull(ArenaThemes.build("no_such_hall", 45));
        assertFalse(ArenaThemes.exists(null));
        assertEquals(45, ArenaThemes.normalSize(44));
        assertEquals(41, ArenaThemes.normalSize(3));
        assertEquals(61, ArenaThemes.normalSize(999));
    }

    @Test
    void everyHallIsSoundAtEverySize() {
        for (String id : ArenaThemes.ids()) {
            for (int size : new int[] {41, 45, ArenaThemes.defaultSize(id), 61}) {
                BossArenaStructure a = ArenaThemes.build(id, size);
                assertEquals(size, a.size, id);
                List<String> problems = ArenaChecks.problems(a);
                assertTrue(problems.isEmpty(), id + "@" + size + ": " + problems);
            }
        }
    }

    @Test
    void everyBlockOfEveryPlanExistsInTheGamesOwnTable() throws IOException {
        JsonObject vanilla = JsonParser.parseString(Files.readString(VANILLA, StandardCharsets.UTF_8)).getAsJsonObject();
        Set<String> names = new HashSet<>(List.of(Files.readString(VANILLA_NAMES, StandardCharsets.UTF_8).split("\\s+")));
        Set<String> checked = new HashSet<>();
        for (String id : ArenaThemes.ids()) {
            BossArenaStructure a = ArenaThemes.build(id, ArenaThemes.defaultSize(id));
            for (String text : a.palette()) check(text, vanilla, names, checked, id);
            for (Placed p : a.extras()) check(p.spec(), vanilla, names, checked, id + " extras");
            for (Placed p : a.sealCells()) check(p.spec(), vanilla, names, checked, id + " seal");
            for (Element e : a.elements()) for (Placed p : e.rubble) check(p.spec(), vanilla, names, checked, id + " rubble");
        }
        assertTrue(checked.size() > 200, "specs checked: " + checked.size());
    }

    private static void check(String text, JsonObject vanilla, Set<String> names, Set<String> checked, String where) {
        if (text.isEmpty() || !checked.add(text)) return;
        BlockSpec spec = BlockSpec.parse(text);
        if (spec.namespace().equals("nusmp")) {
            assertTrue(spec.path().equals("gatekeeper_door") || spec.path().equals("guardian_altar"), where + ": unexpected mod block " + text);
            return;
        }
        assertEquals("minecraft", spec.namespace(), where + ": " + text);
        assertTrue(names.contains(spec.path()) || vanilla.has(spec.path()), where + ": no such block " + spec.path());
        if (spec.properties().isEmpty()) return;
        assertTrue(vanilla.has(spec.path()), where + ": " + spec.path() + " has no properties in the table, but the plan sets " + spec.properties());
        JsonObject props = vanilla.getAsJsonObject(spec.path());
        for (var prop : spec.properties().entrySet()) {
            assertTrue(props.has(prop.getKey()), where + ": " + spec.path() + " has no property " + prop.getKey());
            JsonArray values = props.getAsJsonArray(prop.getKey());
            boolean found = false;
            for (JsonElement v : values) found |= v.getAsString().equals(prop.getValue());
            assertTrue(found, where + ": " + spec.path() + "[" + prop.getKey() + "] has no value " + prop.getValue());
        }
    }

    @Test
    void plansAreDeterministic() {
        for (String id : ArenaThemes.ids()) {
            ArenaThemes.Def d = ArenaThemes.def(id);
            BossArenaStructure a = d.builder().apply(45), b = d.builder().apply(45);
            assertTrue(java.util.Arrays.equals(a.raw(), b.raw()), id + " cells");
            assertEquals(a.palette(), b.palette(), id);
            assertEquals(a.elements().size(), b.elements().size(), id);
            assertEquals(a.zones().size(), b.zones().size(), id);
        }
    }

    @Test
    void anElementIsAnExplicitSetOfBreakableCellsAndNothingHardInTheRest() {
        Set<String> unbreakable = Set.of("minecraft:obsidian", "minecraft:crying_obsidian", "minecraft:bedrock", "minecraft:barrier", "minecraft:reinforced_deepslate", "minecraft:command_block",
                "minecraft:end_portal_frame", "minecraft:respawn_anchor", "minecraft:ender_chest", "minecraft:netherite_block");
        int elements = 0;
        for (String id : ArenaThemes.ids()) {
            BossArenaStructure a = ArenaThemes.build(id, ArenaThemes.defaultSize(id));
            assertFalse(a.elements().isEmpty(), id);
            for (Element e : a.elements()) {
                elements++;
                if (e.locked) {
                    assertEquals(Kind.GATE, e.kind, id);
                    continue;
                }
                for (Placed c : e.cells) {
                    String base = c.spec().contains("[") ? c.spec().substring(0, c.spec().indexOf('[')) : c.spec();
                    assertFalse(unbreakable.contains(base), id + " element " + e.id + " uses a block that is too hard to be breakable: " + base);
                }
                if (e.effect == Effect.ADDS) assertTrue(e.adds >= 1 && e.adds <= 3, id);
                assertTrue(e.bossShare >= 0 && e.bossShare <= 0.05, id);
            }
        }
        assertTrue(elements > 400, "elements across all halls: " + elements);
    }

    @Test
    void theGraveyardHasBreakableTombstonesThatSpawnAdds() {
        BossArenaStructure a = ArenaThemes.build("graveyard", 55);
        List<Element> stones = a.elements(Kind.TOMBSTONE);
        assertTrue(stones.size() >= 20, "tombstones: " + stones.size());
        for (Element e : stones) {
            assertEquals(Effect.ADDS, e.effect);
            assertTrue(e.adds >= 1);
            assertEquals(2, e.cells.size(), "a tombstone is two blocks");
        }
        assertTrue(a.elements(Kind.COFFIN).size() >= 2, "the mausoleum's coffins");
        assertTrue(a.zones("grave").size() >= 20);
        assertTrue(a.spots().size() >= 20);
        assertNotNull(a.elementAt(stones.get(0).x(), stones.get(0).y(), stones.get(0).z()));
        assertNull(a.elementAt(0, 0, 0));
    }

    @Test
    void theThroneRoomHasDestructiblePillarsThatFallTowardsTheMiddle() {
        for (int size : new int[] {41, 45, 51, 61}) {
            BossArenaStructure a = ArenaThemes.build("ruined_throne_room", size);
            List<Element> pillars = a.elements(Kind.PILLAR);
            assertTrue(pillars.size() >= (size >= 45 ? 12 : 8), "pillars at " + size + ": " + pillars.size());
            for (Element e : pillars) {
                assertEquals(Effect.TOPPLE, e.effect);
                assertEquals(4, e.strip.length);
                assertTrue(e.strip[0] <= e.strip[2] && e.strip[1] <= e.strip[3]);
                assertFalse(e.rubble.isEmpty(), "a fallen pillar leaves rubble");
                for (Placed r : e.rubble) {
                    assertTrue(a.inside(r.x(), r.y(), r.z()));
                    assertFalse(a.solidAt(r.x(), r.y(), r.z()), "rubble lies where the plan has nothing solid");
                }
                assertTrue(e.cells.size() >= 30, "a pillar is a real column: " + e.cells.size());
            }
        }
        assertTrue(ArenaThemes.build("ruined_throne_room", 45).elements(Kind.CHANDELIER).size() >= 4);
    }

    @Test
    void theGateChambersGrowDeeperAndDarkerWithEveryGate() {
        int lastNodes = 0, lastLight = 99, lastRoots = 0;
        for (int k = 1; k <= 7; k++) {
            BossArenaStructure a = ArenaThemes.build("qliphoth_gate_" + k, 57);
            int nodes = a.elements(Kind.ROOT_NODE).size();
            assertTrue(nodes >= lastNodes && nodes >= 3, "gate " + k + " nodes " + nodes);
            lastNodes = nodes;
            List<Element> gates = a.elements(Kind.GATE);
            assertEquals(1, gates.size(), "one great gate in gate chamber " + k);
            assertTrue(gates.get(0).locked, "the great gate cannot be broken by a player");
            for (Element n : a.elements(Kind.ROOT_NODE)) {
                assertEquals(Effect.ADDS, n.effect);
                assertTrue(n.bossShare > 0, "a root node is a weak point");
            }
            assertFalse(a.zones("sap").isEmpty(), "black sap pools for the lair action");
            int light = lightLevel(a);
            assertTrue(light <= lastLight, "gate " + k + " light " + light + " after " + lastLight);
            lastLight = light;
            int roots = count(a, "minecraft:mangrove_roots");
            assertTrue(roots >= lastRoots * 0.8, "gate " + k + " roots " + roots);
            lastRoots = roots;
            // seals: the first k slots of the gate's lintel burn
            int lit = 0;
            for (Placed p : gates.get(0).cells) if (p.y() == 10 && p.spec().equals("minecraft:crying_obsidian") && p.z() == 3) lit++;
            assertEquals(k, lit, "gate " + k + " has " + k + " lit seals");
        }
        BossArenaStructure t = ArenaThemes.build("qliphoth_root_throne", 59);
        assertTrue(t.elements(Kind.ROOT_NODE).size() >= 8);
        assertEquals(1, t.elements(Kind.GATE).size());
        assertTrue(lightLevel(t) <= lightLevel(ArenaThemes.build("qliphoth_gate_7", 57)));
    }

    private static int count(BossArenaStructure a, String spec) {
        int n = 0;
        int idx = a.palette().indexOf(spec);
        if (idx < 0) return 0;
        for (short s : a.raw()) if (s == idx) n++;
        return n;
    }

    private static int lightLevel(BossArenaStructure a) {
        for (String s : a.palette()) if (s.startsWith("minecraft:light[level=")) return Integer.parseInt(s.substring("minecraft:light[level=".length(), s.length() - 1));
        return -1;
    }

    @Test
    void theHolyHallsAreBrighterThanTheHallsOfTheDark() {
        assertTrue(lightLevel(ArenaThemes.build("celestial_court", 45)) >= 14);
        assertTrue(lightLevel(ArenaThemes.build("cathedral", 45)) >= 13);
        assertTrue(lightLevel(ArenaThemes.build("witch_hollow", 45)) <= 8);
        assertTrue(lightLevel(ArenaThemes.build("qliphoth_gate_7", 45)) <= 6);
    }

    @Test
    void aStandaloneHallBringsItsOwnGateEntryAltarAndSeal() {
        for (String id : ArenaThemes.ids()) {
            BossArenaStructure a = ArenaThemes.build(id, 45);
            int c = a.c;
            long gate = a.extras().stream().filter(p -> p.spec().startsWith("nusmp:gatekeeper_door[kind=sector")).count();
            assertEquals(24, gate, id + ": a 3 x 4 x 2 Gatekeeper slab");
            assertTrue(a.extras().stream().anyMatch(p -> p.x() == c && p.y() == 3 && p.z() == c && p.spec().equals("nusmp:guardian_altar")), id);
            assertEquals(12, a.sealCells().size(), id);
            for (Placed p : a.sealCells()) {
                assertEquals(a.size - 2, p.z());
                assertTrue(p.y() >= 1 && p.y() <= 4 && Math.abs(p.x() - c) <= 1);
                assertEquals("minecraft:iron_bars", p.spec());
            }
            // the cells the floor's template keeps are never written by the plan
            for (int y = 1; y <= 4; y++) for (int dx = -1; dx <= 1; dx++) {
                assertFalse(a.writes(c + dx, y, 0) || a.writes(c + dx, y, 1) || a.writes(c + dx, y, a.size - 1) || a.writes(c + dx, y, a.size - 2), id + " writes into a doorway");
            }
            for (int y = 1; y <= 3; y++) assertFalse(a.writes(c, y, c), id + " writes into the altar column");
            assertTrue(BossArenaStructure.inDoorway(c, 2, 0, a.size));
            assertTrue(BossArenaStructure.inDoorway(c + 1, 4, a.size - 1, a.size));
            assertFalse(BossArenaStructure.inDoorway(c + 2, 2, 0, a.size));
            assertFalse(BossArenaStructure.inDoorway(c, 5, 0, a.size));
        }
    }

    @Test
    void rotationTurnsTheGateToTheWayTheFloorFaces() {
        int size = 45, c = 22;
        int[] out = new int[2];
        BossArenaStructure.rotate(c, 2, size, 1, out);                         // the gate cell, north of the middle, turned a quarter clockwise: east
        assertEquals(c + 20, out[0]);
        assertEquals(c, out[1]);
        BossArenaStructure.rotate(c, 2, size, 2, out);
        assertEquals(c, out[0]);
        assertEquals(c + 20, out[1]);
        BossArenaStructure.rotate(c, 2, size, 3, out);
        assertEquals(c - 20, out[0]);
        for (int x = 0; x < size; x += 7) for (int z = 0; z < size; z += 5) {
            int[] p = {x, z};
            for (int i = 0; i < 4; i++) {
                BossArenaStructure.rotate(p[0], p[1], size, 1, out);
                p = new int[] {out[0], out[1]};
            }
            assertEquals(x, p[0]);
            assertEquals(z, p[1]);
        }
        BossArenaStructure.rotate(5, 7, size, 4, out);
        assertEquals(5, out[0]);
        assertEquals(7, out[1]);
        BossArenaStructure.rotate(5, 7, size, -1, out);
        int[] back = new int[2];
        BossArenaStructure.rotate(5, 7, size, 3, back);
        assertEquals(back[0], out[0]);
        assertEquals(back[1], out[1]);
    }

    @Test
    void theSolidityTableAgreesWithTheGame() {
        assertEquals(Solidity.Kind.SOLID, Solidity.kind("minecraft:sea_lantern"));
        assertEquals(Solidity.Kind.SOLID, Solidity.kind("minecraft:mangrove_roots"));
        assertEquals(Solidity.Kind.SOLID, Solidity.kind("minecraft:stone_brick_stairs[facing=north,half=bottom]"));
        assertEquals(Solidity.Kind.SOLID, Solidity.kind("minecraft:iron_bars"));
        assertEquals(Solidity.Kind.PASSABLE, Solidity.kind("minecraft:red_carpet"));
        assertEquals(Solidity.Kind.PASSABLE, Solidity.kind("minecraft:soul_lantern[hanging=false]"));
        assertEquals(Solidity.Kind.PASSABLE, Solidity.kind("minecraft:light[level=9]"));
        assertEquals(Solidity.Kind.PASSABLE, Solidity.kind("minecraft:cobweb"));
        assertEquals(Solidity.Kind.PASSABLE, Solidity.kind("minecraft:crimson_roots"));
        assertEquals(Solidity.Kind.LIQUID, Solidity.kind("minecraft:water"));
        assertEquals(Solidity.Kind.HAZARD, Solidity.kind("minecraft:lava"));
        assertEquals(Solidity.Kind.HAZARD, Solidity.kind("minecraft:magma_block"));
        assertTrue(Solidity.open(null));
        assertTrue(Solidity.open("minecraft:air"));
        assertFalse(Solidity.open("minecraft:stone"));
        assertFalse(Solidity.ground("minecraft:lava"));
        assertTrue(Solidity.ground("minecraft:stone"));
    }

    @Test
    void aPlanRefusesWhatItShouldNot() {
        BossArenaStructure a = new BossArenaStructure("probe", 45, 30);
        a.reserve(22, 1, 0);
        assertFalse(a.set(22, 1, 0, "minecraft:stone"), "a reserved cell is kept");
        assertFalse(a.set(-1, 0, 0, "minecraft:stone"));
        assertFalse(a.set(0, 0, 45, "minecraft:stone"));
        assertFalse(a.set(0, 99, 0, "minecraft:stone"));
        assertEquals(3, a.outOfBounds());
        assertTrue(a.set(3, 0, 3, "minecraft:stone"));
        assertEquals("minecraft:stone", a.get(3, 0, 3));
        assertNull(a.get(4, 0, 4));
        boolean threw = false;
        try {
            new BossArenaStructure("probe", 40, 30);
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        assertTrue(threw, "an even size is refused");
        threw = false;
        try {
            new BossArenaStructure("probe", 63, 30);
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        assertTrue(threw, "63 is too big");
        a.beginElement(Kind.CRYSTAL, Effect.SHATTER, 0);
        threw = false;
        try {
            a.beginElement(Kind.CRYSTAL, Effect.SHATTER, 0);
        } catch (IllegalStateException e) {
            threw = true;
        }
        assertTrue(threw, "one element at a time");
        a.endElement();
    }
}
