package com.newuniverse.nusmp.skilltree.shop;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the shop type is the rule that keeps a future baker from selling netherite, so it is tested as a pure function with fake item lists: what a type allows by id, namespace and tag (and tag
 * children), what a ban beats, how a TAG line is judged without a world, and that the real {@code types.json} loads clean and holds the types the docs promise.
 */
class ShopTypeTest {
    private static final Path TYPES = Path.of("src/main/resources/data/nusmp/black_market/types.json");

    private static ShopType food() {
        return new ShopType("food", false, Set.of("c:foods", "c:crops"), Set.of(), Set.of("minecraft:sugar"), Set.of("minecraft:enchanted_golden_apple"));
    }

    @Test
    void aTypeAllowsByIdNamespaceAndTag() {
        ShopType t = new ShopType("magic", false, Set.of("c:gems"), Set.of("tensura"), Set.of("minecraft:ender_pearl"), Set.of());
        assertTrue(t.allows("minecraft:ender_pearl", List.of()), "by id");
        assertTrue(t.allows("tensura:magic_stone", List.of()), "by namespace");
        assertTrue(t.allows("minecraft:emerald", List.of("c:gems")), "by tag");
        assertFalse(t.allows("minecraft:dirt", List.of("minecraft:dirt_tag")), "nothing matches");
        assertFalse(t.allows("minecraft:dirt", null), "no tags known");
        assertFalse(t.allows(null, List.of("c:gems")));
    }

    @Test
    void aTagAllowsItsChildren() {
        ShopType t = food();
        assertTrue(t.tagAllowed("c:foods"));
        assertTrue(t.tagAllowed("c:foods/cooked_meat"), "children nest with a slash");
        assertTrue(t.allows("minecraft:cooked_beef", List.of("c:foods/cooked_meat")));
        assertFalse(t.tagAllowed("c:foodstuffs"), "a longer name is not a child");
        assertFalse(t.tagAllowed("c:ores"));
        assertFalse(t.tagAllowed(null));
    }

    @Test
    void aBanBeatsEverythingElse() {
        ShopType t = food();
        assertFalse(t.allows("minecraft:enchanted_golden_apple", List.of("c:foods")), "banned even though it carries an allowed tag");
        ShopType general = new ShopType("general", true, Set.of(), Set.of(), Set.of(), Set.of("minecraft:barrier"));
        assertTrue(general.allows("minecraft:diamond", null), "the general store sells anything");
        assertFalse(general.allows("minecraft:barrier", null), "except the banned");
        assertTrue(general.isBanned("minecraft:barrier"));
        assertFalse(general.isBanned("minecraft:diamond"));
    }

    @Test
    void tagLinesAreJudgedWithoutAWorld() {
        ShopType t = food();
        assertEquals(ShopType.Check.ALLOWED, t.check("#c:foods", null));
        assertEquals(ShopType.Check.ALLOWED, t.check("#c:foods/fruit", null));
        assertEquals(ShopType.Check.REJECTED, t.check("#c:ores", null), "a food shop cannot stock an ore tag");
        assertEquals(ShopType.Check.REJECTED, t.check("#minecraft:logs", null));
        assertEquals(ShopType.Check.ALLOWED, new ShopType("g", true, Set.of(), Set.of(), Set.of(), Set.of()).check("#c:ores", null), "the general store takes any tag");
    }

    @Test
    void singleItemsAreJudgedByIdAndByTagsWhenKnown() {
        ShopType t = food();
        assertEquals(ShopType.Check.ALLOWED, t.check("minecraft:sugar", null), "by id");
        assertEquals(ShopType.Check.REJECTED, t.check("minecraft:enchanted_golden_apple", List.of("c:foods")), "banned");
        assertEquals(ShopType.Check.UNKNOWN, t.check("minecraft:bread", null), "tags not known yet: the pool filter decides in the world");
        assertEquals(ShopType.Check.ALLOWED, t.check("minecraft:bread", List.of("c:foods")));
        assertEquals(ShopType.Check.REJECTED, t.check("minecraft:diamond", List.of("c:gems")), "tags known and none is allowed");
        assertEquals(ShopType.Check.REJECTED, t.check("", null));
        assertEquals(ShopType.Check.REJECTED, t.check(null, null));
    }

    @Test
    void aTypesFileLoadsAndTheGeneralStoreIsAlwaysThere() {
        ShopType.Parsed p = ShopType.parse("{\"types\":{\"food\":{\"tags\":[\"c:foods\"],\"banned_ids\":[\"minecraft:cake\"]}}}");
        assertEquals(2, p.types().size(), "food + the built-in general store");
        assertTrue(p.get("food").tagAllowed("c:foods"));
        assertTrue(p.get("food").isBanned("minecraft:cake"));
        assertTrue(p.get("general").all());
        assertTrue(p.problems().stream().anyMatch(s -> s.contains("general")), "the missing general type is reported");
    }

    @Test
    void badTypesAreSkippedWithASentenceAndNeverThrow() {
        String[][] bad = {
                {"{\"types\":{\"Bad Id\":{\"all\":true}}}", "snake_case"},
                {"{\"types\":{\"empty\":{}}}", "sells nothing"},
                {"{\"types\":{\"x\":5}}", "must be an object"},
                {"{\"types\":{\"x\":{\"tags\":[\"not a tag\"]}}}", "not a valid id"},
                {"{\"types\":{\"x\":{\"tags\":\"c:foods\"}}}", "must be an array"},
                {"{\"types\":{\"x\":{\"all\":\"yes\",\"tags\":[\"c:foods\"]}}}", "true or false"},
                {"{\"types\":{\"x\":{\"tags\":[\"c:foods\"],\"colour\":1}}}", "unknown key"},
                {"{}", "\"types\""},
                {"not json {", "not valid JSON"},
        };
        for (String[] b : bad) {
            ShopType.Parsed p = ShopType.parse(b[0]);
            assertTrue(p.problems().stream().anyMatch(s -> s.contains(b[1])), b[0] + " should complain about '" + b[1] + "' but said " + p.problems());
            assertNotNull(p.get("general"), "there is always a general store");
        }
        assertNull(ShopType.parse("{\"types\":{\"empty\":{}}}").get("empty"));
        assertNotNull(ShopType.parse(null).get("general"));
    }

    @Test
    void theShippedTypesFileLoadsCleanWithTheTypesTheDocsPromise() throws IOException {
        ShopType.Parsed p = ShopType.parse(Files.readString(TYPES));
        assertTrue(p.problems().isEmpty(), p.problems().toString());
        for (String id : List.of("general", "food", "ores", "potions", "tools", "armor", "magic", "building", "materials")) assertNotNull(p.get(id), id);
        ShopType general = p.get("general");
        assertTrue(general.all());
        for (String banned : List.of("minecraft:barrier", "minecraft:command_block", "tensura:mithril_ingot", "nusmp:devils_core", "nusmp:genesis_seed", "tensura:hihiirokane_ingot")) {
            assertTrue(general.isBanned(banned), banned + " must never be bought for gold");
        }
        assertFalse(p.get("food").allows("minecraft:netherite_block", List.of("c:storage_blocks")), "the baker does not sell netherite");
        assertTrue(p.get("food").allows("minecraft:bread", List.of("c:foods")));
        assertTrue(p.get("ores").allows("minecraft:iron_ingot", List.of("c:ingots")));
        assertFalse(p.get("ores").allows("minecraft:netherite_ingot", List.of("c:ingots")), "the ore shop does not sell netherite ingots");
        assertTrue(p.get("magic").allows("tensura:anything", List.of()), "the magic dealer takes every Tensura item");
    }
}
