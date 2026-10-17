package com.newuniverse.nusmp.labyrinth.mega.safe;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the compat table is the part of the mod-compat processor that decides WHETHER a block is swapped, and its whole point is to do nothing
 * wrong when it cannot do the right thing: the mod is absent, the id is not in the registry, the list is empty, a token has no value. Each of
 * those is a test, plus the order rule (first present candidate wins), the facing flag, the multi-file merge and the token fill from the block
 * being replaced.
 */
class CompatTableTest {
    private static Map<String, List<String>> swaps(Object... kv) {
        Map<String, List<String>> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            @SuppressWarnings("unchecked") List<String> ids = (List<String>) kv[i + 1];
            m.put((String) kv[i], ids);
        }
        return m;
    }

    private static CompatTable fd() {
        return CompatTable.of(List.of(new CompatTable.Source("farmersdelight",
                swaps("stove", List.of("farmersdelight:stove"), "skillet", List.of("farmersdelight:skillet"), "cutting_board", List.of("farmersdelight:cutting_board")),
                Set.of(), "facing")));
    }

    private static CompatTable furniture() {
        return CompatTable.of(List.of(new CompatTable.Source("refurbished_furniture",
                swaps("chair", List.of("refurbished_furniture:chair_{wood}", "refurbished_furniture:chair_oak"),
                        "sofa", List.of("refurbished_furniture:sofa_{color}", "refurbished_furniture:sofa")),
                Set.of("chair", "sofa"), "direction")));
    }

    private static final Predicate<String> ALL_MODS = m -> true;
    private static final Predicate<String> NO_MODS = m -> false;
    private static final Predicate<String> ALL_BLOCKS = b -> true;
    private static final Predicate<String> NO_BLOCKS = b -> false;

    @Test void aLoadedModWithAKnownBlockSwaps() {
        CompatTable.Pick p = fd().pick("stove", ALL_MODS, ALL_BLOCKS, Map.of());
        assertNotNull(p);
        assertEquals("farmersdelight:stove", p.id());
        assertFalse(p.flipFacing());
        assertEquals("facing", p.facingProperty());
    }

    @Test void anAbsentModNeverSwaps() {
        assertNull(fd().pick("stove", NO_MODS, ALL_BLOCKS, Map.of()));
        assertNull(fd().pick("stove", m -> m.equals("create"), ALL_BLOCKS, Map.of()), "only the file's own mod gates its entries");
    }

    @Test void anUnknownBlockIdNeverSwaps() {
        assertNull(fd().pick("stove", ALL_MODS, NO_BLOCKS, Map.of()));
    }

    @Test void anUnknownHintOrNullsNeverSwap() {
        assertNull(fd().pick("hammock", ALL_MODS, ALL_BLOCKS, Map.of()));
        assertNull(fd().pick(null, ALL_MODS, ALL_BLOCKS, Map.of()));
        assertNull(fd().pick("stove", null, ALL_BLOCKS, Map.of()));
        assertNull(fd().pick("stove", ALL_MODS, null, Map.of()));
    }

    @Test void anEmptyTableAndEmptyListsDoNothing() {
        assertTrue(CompatTable.EMPTY.isEmpty());
        assertNull(CompatTable.EMPTY.pick("stove", ALL_MODS, ALL_BLOCKS, Map.of()));
        assertTrue(CompatTable.of(null).isEmpty());
        assertTrue(CompatTable.of(List.of()).isEmpty());
        CompatTable empty = CompatTable.of(List.of(new CompatTable.Source("farmersdelight", swaps("stove", List.of()), Set.of(), null)));
        assertTrue(empty.isEmpty(), "a hint with no candidates is no hint");
        assertNull(empty.pick("stove", ALL_MODS, ALL_BLOCKS, Map.of()));
        CompatTable blank = CompatTable.of(List.of(new CompatTable.Source("farmersdelight", swaps("stove", List.of("", "  ")), Set.of(), null)));
        assertTrue(blank.isEmpty());
    }

    @Test void malformedSourcesAreSkipped() {
        CompatTable t = CompatTable.of(java.util.Arrays.asList(null, new CompatTable.Source(null, swaps("stove", List.of("a:b")), Set.of(), null),
                new CompatTable.Source(" ", swaps("stove", List.of("a:b")), Set.of(), null), new CompatTable.Source("a", null, Set.of(), null)));
        assertTrue(t.isEmpty());
    }

    @Test void theFirstCandidateThatExistsWins() {
        CompatTable t = CompatTable.of(List.of(new CompatTable.Source("refurbished_furniture",
                swaps("table", List.of("refurbished_furniture:table_wrong", "refurbished_furniture:table_oak", "refurbished_furniture:desk_oak")), Set.of(), null)));
        Predicate<String> exists = b -> b.equals("refurbished_furniture:table_oak") || b.equals("refurbished_furniture:desk_oak");
        assertEquals("refurbished_furniture:table_oak", t.pick("table", ALL_MODS, exists, Map.of()).id());
        Predicate<String> onlyDesk = b -> b.equals("refurbished_furniture:desk_oak");
        assertEquals("refurbished_furniture:desk_oak", t.pick("table", ALL_MODS, onlyDesk, Map.of()).id());
    }

    @Test void tokensAreFilledFromTheReplacedBlock() {
        Map<String, String> dark = CompatTable.tokensFor("dark_oak_stairs");
        assertEquals("dark_oak", dark.get("wood"));
        assertEquals("refurbished_furniture:chair_dark_oak", furniture().pick("chair", ALL_MODS, ALL_BLOCKS, dark).id());
        Map<String, String> blue = CompatTable.tokensFor("light_blue_wool");
        assertEquals("light_blue", blue.get("color"));
        assertEquals("refurbished_furniture:sofa_light_blue", furniture().pick("sofa", ALL_MODS, ALL_BLOCKS, blue).id());
    }

    @Test void aMissingTokenFallsThroughToTheNextCandidate() {
        CompatTable.Pick p = furniture().pick("chair", ALL_MODS, ALL_BLOCKS, Map.of());
        assertNotNull(p);
        assertEquals("refurbished_furniture:chair_oak", p.id(), "no {wood} value: the token-free candidate is used");
        assertNull(furniture().pick("sofa", ALL_MODS, b -> b.equals("refurbished_furniture:sofa_red"), Map.of()), "no {color} value: that candidate is skipped");
        assertEquals("refurbished_furniture:sofa", furniture().pick("sofa", ALL_MODS, ALL_BLOCKS, null).id());
    }

    @Test void aCandidateWhoseExpansionIsNotAnIdIsSkipped() {
        assertEquals("refurbished_furniture:chair_oak", furniture().pick("chair", ALL_MODS, ALL_BLOCKS, Map.of("wood", "Not An Id")).id(),
                "the broken expansion is skipped and the oak candidate answers");
        CompatTable only = CompatTable.of(List.of(new CompatTable.Source("m", swaps("chair", List.of("m:chair_{wood}")), Set.of(), null)));
        assertNull(only.pick("chair", ALL_MODS, ALL_BLOCKS, Map.of("wood", "Not An Id")));
        assertNull(only.pick("chair", ALL_MODS, ALL_BLOCKS, Map.of("wood", " ")));
    }

    @Test void theFacingFlagAndPropertyComeFromTheFile() {
        CompatTable.Pick chair = furniture().pick("chair", ALL_MODS, ALL_BLOCKS, Map.of("wood", "oak"));
        assertTrue(chair.flipFacing());
        assertEquals("direction", chair.facingProperty());
        CompatTable.Pick stove = fd().pick("stove", ALL_MODS, ALL_BLOCKS, Map.of());
        assertFalse(stove.flipFacing());
        assertEquals("facing", stove.facingProperty());
    }

    @Test void twoFilesMergeInFileOrder() {
        CompatTable t = CompatTable.of(List.of(
                new CompatTable.Source("modone", swaps("table", List.of("modone:table")), Set.of(), null),
                new CompatTable.Source("modtwo", swaps("table", List.of("modtwo:table")), Set.of(), null)));
        assertEquals(2, t.candidateCount("table"));
        assertEquals("modone:table", t.pick("table", ALL_MODS, ALL_BLOCKS, Map.of()).id());
        assertEquals("modtwo:table", t.pick("table", m -> m.equals("modtwo"), ALL_BLOCKS, Map.of()).id(), "the first file's mod is absent: the second file answers");
    }

    @Test void hintsAndModsAreCaseInsensitive() {
        CompatTable t = CompatTable.of(List.of(new CompatTable.Source("FarmersDelight", swaps("Stove", List.of(" farmersdelight:stove ")), Set.of(), null)));
        assertTrue(t.hasHint("stove"));
        assertTrue(t.hasHint(" STOVE "));
        assertEquals("farmersdelight:stove", t.pick("StOvE", m -> m.equals("farmersdelight"), ALL_BLOCKS, Map.of()).id());
    }

    @Test void woodAndColourGuessesHaveSaneDefaults() {
        assertEquals("oak", CompatTable.woodOf("oak_planks"));
        assertEquals("dark_oak", CompatTable.woodOf("dark_oak_fence_gate"));
        assertEquals("crimson", CompatTable.woodOf("crimson_stairs"));
        assertEquals("birch", CompatTable.woodOf("stripped_birch_log"));
        assertEquals("warped", CompatTable.woodOf("warped_stem"));
        assertEquals("oak", CompatTable.woodOf("campfire"), "not a wooden block: the default");
        assertEquals("oak", CompatTable.woodOf("bamboo_mosaic_stairs"));
        assertEquals("oak", CompatTable.woodOf(null));
        assertEquals("light_gray", CompatTable.colorOf("light_gray_bed"));
        assertEquals("blue", CompatTable.colorOf("blue_wool"));
        assertEquals("red", CompatTable.colorOf("campfire"));
        assertEquals("red", CompatTable.colorOf(null));
    }

    @Test void idsAreValidatedAndExpanded() {
        assertTrue(CompatTable.validId("farmersdelight:stove"));
        assertTrue(CompatTable.validId("refurbished_furniture:kitchen/counter_oak"));
        assertFalse(CompatTable.validId("stove"));
        assertFalse(CompatTable.validId("Farmers:Stove"));
        assertFalse(CompatTable.validId("a:b{wood}"));
        assertFalse(CompatTable.validId(null));
        assertEquals("m:x_oak_y", CompatTable.expand("m:x_{wood}_y", Map.of("wood", "oak")));
        assertEquals("m:plain", CompatTable.expand("m:plain", null));
        assertNull(CompatTable.expand("m:x_{wood}", Map.of()));
        assertNull(CompatTable.expand(null, Map.of()));
    }
}
