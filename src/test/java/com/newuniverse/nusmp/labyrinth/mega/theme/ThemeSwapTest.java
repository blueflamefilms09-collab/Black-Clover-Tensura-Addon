package com.newuniverse.nusmp.labyrinth.mega.theme;

import com.newuniverse.nusmp.labyrinth.mega.theme.ThemeSwap.Entry;
import com.newuniverse.nusmp.labyrinth.mega.theme.ThemeSwap.How;
import com.newuniverse.nusmp.labyrinth.mega.theme.ThemeSwap.Resolved;
import com.newuniverse.nusmp.labyrinth.mega.theme.ThemeSwap.Shape;
import com.newuniverse.nusmp.labyrinth.mega.theme.ThemeSwap.Slot;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The id-derivation rules of the Region Theme palette swap, over a fake registry (a set of ids), so every rule is pinned without a game: the
 * suffix, the singular of bricks and tiles, the stem without {@code _block}, the wood stem and its logs, the stone fallback for a shape a family
 * lacks, the wood that never falls back to stone, the explicit map, the identity theme, the problem list, and that the Java palette is the Python
 * palette.
 */
class ThemeSwapTest {
    private static Predicate<String> registry(String... ids) {
        Set<String> s = new TreeSet<>(List.of(ids));
        return s::contains;
    }

    private static Entry entry(String id) {
        for (Entry e : ThemeSwap.PALETTE) if (e.id().equals(id)) return e;
        throw new AssertionError("not a palette block: " + id);
    }

    private static Map<String, String> families(String... kv) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return m;
    }

    @Test
    void thePlainBlockIsTheFamilyBase() {
        Predicate<String> reg = registry("minecraft:nether_bricks");
        Resolved r = ThemeSwap.resolve(entry("minecraft:deepslate_bricks"), families("brick", "minecraft:nether_bricks"), Map.of(), reg);
        assertEquals("minecraft:nether_bricks", r.target());
        assertEquals(How.DIRECT, r.how());
        assertTrue(r.swaps());
    }

    @Test
    void aShapeIsTheBasePlusItsSuffix() {
        Predicate<String> reg = registry("minecraft:polished_diorite", "minecraft:polished_diorite_stairs", "minecraft:polished_diorite_slab");
        Map<String, String> f = families("tile", "minecraft:polished_diorite");
        Resolved stairs = ThemeSwap.resolve(entry("minecraft:deepslate_tile_stairs"), f, Map.of(), reg);
        assertEquals("minecraft:polished_diorite_stairs", stairs.target());
        assertEquals(How.DIRECT, stairs.how());
        assertEquals("minecraft:polished_diorite_slab", ThemeSwap.resolve(entry("minecraft:deepslate_tile_slab"), f, Map.of(), reg).target());
    }

    @Test
    void bricksAndTilesTakeTheirSingularForShapes() {
        Predicate<String> reg = registry("minecraft:nether_bricks", "minecraft:nether_brick_stairs", "minecraft:nether_brick_slab", "minecraft:nether_brick_wall",
                "minecraft:polished_blackstone_bricks", "minecraft:polished_blackstone_brick_stairs");
        Map<String, String> f = families("brick", "minecraft:nether_bricks", "polished", "minecraft:polished_blackstone_bricks");
        Resolved s = ThemeSwap.resolve(entry("minecraft:deepslate_brick_stairs"), f, Map.of(), reg);
        assertEquals("minecraft:nether_brick_stairs", s.target());
        assertEquals(How.DERIVED, s.how(), "the plural-less id is a derivation, still the theme's own family");
        assertEquals("minecraft:nether_brick_wall", ThemeSwap.resolve(entry("minecraft:deepslate_brick_wall"), f, Map.of(), reg).target());
        assertEquals("minecraft:polished_blackstone_brick_stairs", ThemeSwap.resolve(entry("minecraft:polished_deepslate_stairs"), f, Map.of(), reg).target());
    }

    @Test
    void aBlockSuffixIsDroppedForShapes() {
        Predicate<String> reg = registry("minecraft:purpur_block", "minecraft:purpur_stairs", "minecraft:purpur_slab");
        Map<String, String> f = families("tile", "minecraft:purpur_block");
        assertEquals("minecraft:purpur_stairs", ThemeSwap.resolve(entry("minecraft:deepslate_tile_stairs"), f, Map.of(), reg).target());
        assertEquals("minecraft:purpur_slab", ThemeSwap.resolve(entry("minecraft:deepslate_tile_slab"), f, Map.of(), reg).target());
    }

    @Test
    void aShapeTheFamilyLacksComesFromTheFallbackSlot() {
        Predicate<String> reg = registry("minecraft:end_stone_bricks", "minecraft:end_stone_brick_wall", "minecraft:purpur_block", "minecraft:purpur_stairs");
        Map<String, String> f = families("brick", "minecraft:end_stone_bricks", "tile", "minecraft:purpur_block");
        Resolved wall = ThemeSwap.resolve(entry("minecraft:deepslate_tile_wall"), f, Map.of(), reg);
        assertEquals("minecraft:end_stone_brick_wall", wall.target(), "purpur has no wall: the brick slot lends one");
        assertEquals(How.FALLBACK, wall.how());
        assertEquals("minecraft:purpur_stairs", ThemeSwap.resolve(entry("minecraft:deepslate_tile_stairs"), f, Map.of(), reg).target(), "its own stair wins over any fallback");
    }

    @Test
    void aFamilyWithNoShapesAtAllUsesTheBrickShapes() {
        Predicate<String> reg = registry("minecraft:netherrack", "minecraft:nether_bricks", "minecraft:nether_brick_stairs");
        Map<String, String> f = families("brick", "minecraft:nether_bricks", "cobbled", "minecraft:netherrack");
        assertEquals("minecraft:netherrack", ThemeSwap.resolve(entry("minecraft:cobbled_deepslate"), f, Map.of(), reg).target());
        Resolved st = ThemeSwap.resolve(entry("minecraft:cobbled_deepslate_stairs"), f, Map.of(), reg);
        assertEquals("minecraft:nether_brick_stairs", st.target());
        assertEquals(How.FALLBACK, st.how());
    }

    @Test
    void aMissingSlotBorrowsFromItsFallback() {
        Predicate<String> reg = registry("minecraft:stone_bricks", "minecraft:stone_brick_stairs");
        Map<String, String> f = families("brick", "minecraft:stone_bricks");
        Resolved r = ThemeSwap.resolve(entry("minecraft:chiseled_deepslate"), f, Map.of(), reg);
        assertEquals("minecraft:stone_bricks", r.target());
        assertEquals(How.FALLBACK, r.how());
        assertEquals("minecraft:stone_bricks", ThemeSwap.resolve(entry("minecraft:cracked_deepslate_bricks"), f, Map.of(), reg).target());
    }

    @Test
    void woodUsesTheStemOfThePlanksAndTheLogsOfTheFamily() {
        Predicate<String> reg = registry("minecraft:crimson_planks", "minecraft:crimson_stairs", "minecraft:crimson_slab", "minecraft:crimson_fence",
                "minecraft:crimson_fence_gate", "minecraft:crimson_trapdoor", "minecraft:crimson_door", "minecraft:crimson_stem", "minecraft:stripped_crimson_stem",
                "minecraft:crimson_button", "minecraft:crimson_pressure_plate");
        Map<String, String> f = families("wood", "minecraft:crimson_planks");
        assertEquals("minecraft:crimson_planks", ThemeSwap.resolve(entry("minecraft:dark_oak_planks"), f, Map.of(), reg).target());
        assertEquals("minecraft:crimson_fence", ThemeSwap.resolve(entry("minecraft:dark_oak_fence"), f, Map.of(), reg).target());
        assertEquals("minecraft:crimson_fence_gate", ThemeSwap.resolve(entry("minecraft:dark_oak_fence_gate"), f, Map.of(), reg).target());
        assertEquals("minecraft:crimson_trapdoor", ThemeSwap.resolve(entry("minecraft:dark_oak_trapdoor"), f, Map.of(), reg).target());
        assertEquals("minecraft:crimson_door", ThemeSwap.resolve(entry("minecraft:dark_oak_door"), f, Map.of(), reg).target());
        assertEquals("minecraft:crimson_stem", ThemeSwap.resolve(entry("minecraft:dark_oak_log"), f, Map.of(), reg).target(), "crimson wood is a stem, not a log");
        assertEquals("minecraft:stripped_crimson_stem", ThemeSwap.resolve(entry("minecraft:stripped_dark_oak_log"), f, Map.of(), reg).target());
        assertEquals("minecraft:crimson_button", ThemeSwap.resolve(entry("minecraft:dark_oak_button"), f, Map.of(), reg).target());
        assertEquals("minecraft:crimson_pressure_plate", ThemeSwap.resolve(entry("minecraft:dark_oak_pressure_plate"), f, Map.of(), reg).target());
    }

    @Test
    void bambooLogsAreBlocks() {
        Predicate<String> reg = registry("minecraft:bamboo_planks", "minecraft:bamboo_block", "minecraft:stripped_bamboo_block");
        Map<String, String> f = families("wood", "minecraft:bamboo_planks");
        assertEquals("minecraft:bamboo_block", ThemeSwap.resolve(entry("minecraft:dark_oak_log"), f, Map.of(), reg).target());
        assertEquals("minecraft:stripped_bamboo_block", ThemeSwap.resolve(entry("minecraft:stripped_dark_oak_log"), f, Map.of(), reg).target());
    }

    @Test
    void woodNeverFallsBackToStone() {
        Predicate<String> reg = registry("minecraft:bamboo_mosaic", "minecraft:bamboo_mosaic_stairs", "minecraft:stone_bricks", "minecraft:stone_brick_stairs", "minecraft:stone_brick_wall");
        Map<String, String> f = families("wood", "minecraft:bamboo_mosaic", "brick", "minecraft:stone_bricks");
        assertEquals("minecraft:bamboo_mosaic_stairs", ThemeSwap.resolve(entry("minecraft:dark_oak_stairs"), f, Map.of(), reg).target());
        Resolved fence = ThemeSwap.resolve(entry("minecraft:dark_oak_fence"), f, Map.of(), reg);
        assertEquals(How.NONE, fence.how(), "a wood family without a fence leaves the dark oak fence, it never becomes stone");
        assertNull(fence.target());
        assertFalse(fence.swaps());
    }

    @Test
    void theExplicitMapWinsAndMayNameAnyBlock() {
        Predicate<String> reg = registry("minecraft:nether_bricks", "minecraft:nether_brick_stairs", "minecraft:soul_lantern", "minecraft:lantern", "minecraft:red_nether_brick_wall");
        Map<String, String> f = families("brick", "minecraft:nether_bricks");
        Map<String, String> ex = families("minecraft:deepslate_brick_stairs", "minecraft:red_nether_brick_wall", "minecraft:lantern", "minecraft:soul_lantern");
        Resolved r = ThemeSwap.resolve(entry("minecraft:deepslate_brick_stairs"), f, ex, reg);
        assertEquals("minecraft:red_nether_brick_wall", r.target());
        assertEquals(How.EXPLICIT, r.how());
        Map<String, ThemeSwap.Resolved> table = ThemeSwap.table(f, ex, reg);
        assertEquals("minecraft:soul_lantern", table.get("minecraft:lantern").target(), "an explicit entry that is not a palette block joins the table");
        assertEquals(How.EXPLICIT, table.get("minecraft:lantern").how());
    }

    @Test
    void anExplicitTargetThatDoesNotExistIsIgnored() {
        Predicate<String> reg = registry("minecraft:nether_bricks", "minecraft:nether_brick_stairs");
        Map<String, String> f = families("brick", "minecraft:nether_bricks");
        Map<String, String> ex = families("minecraft:deepslate_brick_stairs", "minecraft:no_such_block");
        assertEquals("minecraft:nether_brick_stairs", ThemeSwap.resolve(entry("minecraft:deepslate_brick_stairs"), f, ex, reg).target(), "the derived rule answers instead");
        assertTrue(ThemeSwap.problems(f, ex, reg).stream().anyMatch(p -> p.contains("no_such_block")), "and the problem list says so");
    }

    @Test
    void theBaselineMapsEveryBlockToItself() {
        Predicate<String> reg = id -> {
            for (Entry e : ThemeSwap.PALETTE) if (e.id().equals(id)) return true;
            return false;
        };
        Map<String, String> f = ThemeDef.gravity().families();
        Map<String, Resolved> t = ThemeSwap.table(f, Map.of(), reg);
        assertEquals(31, t.size());
        for (Map.Entry<String, Resolved> x : t.entrySet()) {
            assertEquals(How.SAME, x.getValue().how(), x.getKey());
            assertEquals(x.getKey(), x.getValue().target());
            assertFalse(x.getValue().swaps());
        }
        assertEquals(List.of(), ThemeSwap.problems(f, Map.of(), reg));
    }

    @Test
    void theProblemListNamesWhatIsWrong() {
        Predicate<String> reg = registry("minecraft:stone_bricks", "minecraft:stone_brick_stairs");
        List<String> p = ThemeSwap.problems(families("brick", "minecraft:stone_bricks", "tile", "minecraft:not_a_block"), Map.of(), reg);
        assertTrue(p.stream().anyMatch(s -> s.contains("'tile'") && s.contains("not_a_block")), p.toString());
        assertTrue(p.stream().anyMatch(s -> s.contains("'wood'") && s.contains("missing")), p.toString());
        assertTrue(p.stream().anyMatch(s -> s.contains("minecraft:dark_oak_fence") && s.contains("no target")), p.toString());
    }

    @Test
    void theSlotKeysAreTheJsonKeys() {
        assertEquals(Slot.CRACKED_TILE, Slot.byKey("cracked_tile"));
        assertEquals(Slot.WOOD, Slot.byKey(" Wood "));
        assertNull(Slot.byKey("marble"));
        assertNull(Slot.byKey(null));
        assertEquals(9, Slot.values().length);
        assertEquals("_fence_gate", Shape.FENCE_GATE.suffix());
    }

    @Test
    void theJavaPaletteIsThePythonPalette() throws IOException {
        Path lib = Path.of("tools/labyrinth/mega_lib.py");
        assertTrue(Files.exists(lib), "mega_lib.py not found: is the working directory the project root?");
        String py = Files.readString(lib, StandardCharsets.UTF_8);
        Map<String, String> consts = new LinkedHashMap<>();
        Matcher m = Pattern.compile("(?m)^([A-Z_]+) = \"(minecraft:[a-z_]+)\"").matcher(py);
        while (m.find()) consts.put(m.group(1), m.group(2));
        int at = py.indexOf("PALETTE = {");
        assertTrue(at > 0, "PALETTE not found in mega_lib.py");
        String literal = py.substring(at + "PALETTE = {".length(), py.indexOf('}', at));
        TreeSet<String> python = new TreeSet<>();
        Matcher n = Pattern.compile("[A-Z_]+").matcher(literal);
        while (n.find()) {
            String id = consts.get(n.group());
            assertNotNull(id, "unknown palette constant " + n.group());
            python.add(id);
        }
        TreeSet<String> java = new TreeSet<>();
        for (Entry e : ThemeSwap.PALETTE) java.add(e.id());
        assertEquals(python, java, "ThemeSwap.PALETTE and mega_lib.PALETTE must list the same 31 blocks");
        assertEquals(31, java.size());
    }

    @Test
    void aThemesLightShiftMovesTheLightBlocksButNeverUnderTheFloor() {
        assertEquals(15, ThemeSwap.shiftLight(12, 3), "brighter, to the top");
        assertEquals(15, ThemeSwap.shiftLight(15, 3), "never above 15");
        assertEquals(8, ThemeSwap.shiftLight(12, -4), "dimmer");
        assertEquals(ThemeSwap.LIGHT_FLOOR, ThemeSwap.shiftLight(12, -8), "never under the floor of 6");
        assertEquals(ThemeSwap.LIGHT_FLOOR, ThemeSwap.shiftLight(10, -8));
        assertEquals(5, ThemeSwap.shiftLight(5, -3), "a block that was already under the floor keeps its own level as the floor");
        assertEquals(5, ThemeSwap.shiftLight(3, 2), "and a shift upwards still works");
        assertEquals(12, ThemeSwap.shiftLight(12, 0), "no shift, no change");
        assertEquals(0, ThemeSwap.shiftLight(0, 3), "a level-0 block gives no light and stays that way");
        for (int level = 1; level <= 15; level++) {
            for (int shift = -8; shift <= 8; shift++) {
                int r = ThemeSwap.shiftLight(level, shift);
                assertTrue(r >= Math.min(level, ThemeSwap.LIGHT_FLOOR) && r <= 15, level + " shifted by " + shift + " gave " + r);
                if (shift >= 0) assertTrue(r >= level, "a brighter theme never dims a light block: " + level + " + " + shift + " = " + r);
                else assertTrue(r <= level, "a dimmer theme never brightens one: " + level + " " + shift + " = " + r);
            }
        }
    }
}
