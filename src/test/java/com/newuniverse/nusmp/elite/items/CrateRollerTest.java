package com.newuniverse.nusmp.elite.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.elite.items.crate.CrateRoller;
import com.newuniverse.nusmp.elite.items.crate.CrateTable;
import com.newuniverse.nusmp.elite.items.crate.CrateTableParser;
import com.newuniverse.nusmp.elite.items.crate.CrateTier;
import com.newuniverse.nusmp.elite.items.crate.RewardEntry;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

/**
 * Why: the odds of a crate are rules, not feelings. This pins the pure roller (weights, ranges, exclusion of a type, the edge dice 0.0 and 1.0), the parser (a bad entry is dropped and reported, a
 * table with a command key is refused) and the three shipped tables (they parse with no error, every reward type is a known one, every item of the mod they name exists, and a higher tier is worth more).
 */
class CrateRollerTest {
    private static final Predicate<String> KNOWN = Set.of("item", "coins", "skill_book", "sp", "xp")::contains;

    private static CrateTable table() {
        return new CrateTable("t", 2, 3, List.of(
                new RewardEntry("item", 1, "minecraft:a", 1, 1, ""),
                new RewardEntry("coins", 3, "", 10, 20, ""),
                new RewardEntry("xp", 0, "", 5, 5, "")));
    }

    @Test
    void edgeDiceNeverOverrun() {
        CrateTable t = table();
        assertEquals(2, CrateRoller.rollCount(t, () -> 0.0));
        assertEquals(3, CrateRoller.rollCount(t, () -> 1.0));
        assertEquals(2, CrateRoller.rollCount(t, () -> Double.NaN));              // NaN clamps to index 0
    }

    @Test
    void weightsAreHonoured() {
        CrateTable t = table();
        CrateRoller.Roll first = CrateRoller.rollOne(t, () -> 0.0, Set.of());
        assertEquals("item", first.entry().type());                              // pick 0 of total 4 lands in the first entry (weight 1)
        CrateRoller.Roll last = CrateRoller.rollOne(t, () -> 0.999, Set.of());
        assertEquals("coins", last.entry().type());                              // the zero-weight xp entry can never win
        Random r = new Random(7);
        int coins = 0, items = 0;
        for (int i = 0; i < 4000; i++) {
            CrateRoller.Roll x = CrateRoller.rollOne(t, r::nextDouble, Set.of());
            if (x.entry().type().equals("coins")) coins++;
            else if (x.entry().type().equals("item")) items++;
            else throw new AssertionError("a zero weight entry was rolled");
        }
        assertTrue(coins > items * 2, "coins (weight 3) should come up about three times as often as the item (weight 1): " + coins + " vs " + items);
    }

    @Test
    void amountsStayInRange() {
        CrateTable t = table();
        Random r = new Random(11);
        for (int i = 0; i < 2000; i++) {
            CrateRoller.Roll x = CrateRoller.rollOne(t, r::nextDouble, Set.of());
            assertTrue(x.amount() >= x.entry().min() && x.amount() <= x.entry().max(), "amount " + x.amount() + " outside " + x.entry());
        }
    }

    @Test
    void excludedTypesNeverAppearAndEmptyGivesNull() {
        CrateTable t = table();
        Random r = new Random(3);
        for (int i = 0; i < 500; i++) assertFalse(CrateRoller.rollOne(t, r::nextDouble, Set.of("coins")).entry().type().equals("coins"));
        assertNull(CrateRoller.rollOne(t, r::nextDouble, Set.of("coins", "item")));
        assertEquals(0, CrateRoller.roll(new CrateTable("e", 1, 1, List.of()), r::nextDouble, Set.of()).size());
    }

    @Test
    void wholeOpeningRollsWithinTheRange() {
        CrateTable t = table();
        Random r = new Random(5);
        for (int i = 0; i < 300; i++) {
            int n = CrateRoller.roll(t, r::nextDouble, Set.of()).size();
            assertTrue(n >= 2 && n <= 3, "rolls " + n);
        }
    }

    @Test
    void refusedTypesAreRerolledWithoutThatType() {
        CrateTable t = new CrateTable("t", 3, 3, List.of(
                new RewardEntry("coins", 100, "", 5, 5, ""),
                new RewardEntry("item", 1, "minecraft:stone", 2, 2, "")));
        Random r = new Random(9);
        // the economy is off: coins are refused every time, so every slot still pays out, with the item
        List<String> got = CrateRoller.open(t, r::nextDouble, roll -> roll.entry().type().equals("coins") ? null : roll.entry().id() + "x" + roll.amount());
        assertEquals(List.of("minecraft:stonex2", "minecraft:stonex2", "minecraft:stonex2"), got);
        // nothing can be given at all: the opening is empty rather than looping forever
        assertEquals(0, CrateRoller.open(t, r::nextDouble, roll -> null).size());
    }

    @Test
    void parserDropsBadEntriesAndRefusesCommands() {
        String json = "{\"rolls\":[1,2],\"entries\":["
                + "{\"type\":\"item\",\"id\":\"minecraft:stone\",\"weight\":5,\"count\":4},"
                + "{\"type\":\"item\",\"id\":\"minecraft:dirt\",\"weight\":0},"
                + "{\"type\":\"sp\",\"weight\":2,\"min\":3,\"max\":1},"
                + "{\"type\":\"warp\",\"weight\":2},"
                + "{\"type\":\"item\",\"id\":\"minecraft:stone\",\"weight\":2,\"command\":\"op @p\"}]}";
        CrateTableParser.Result res = CrateTableParser.parse("x", json, KNOWN);
        assertNotNull(res.table());
        assertEquals(1, res.table().entries().size());
        assertEquals(4, res.table().entries().get(0).min());
        assertEquals(4, res.errors().size());
        assertTrue(res.errors().stream().anyMatch(e -> e.contains("command-free")), "the command key must be named as refused");
        assertFalse(res.ok());
    }

    @Test
    void parserReportsNotJsonAndNoEntries() {
        assertNull(CrateTableParser.parse("x", "not json{", KNOWN).table());
        assertNull(CrateTableParser.parse("x", "{\"entries\":[]}", KNOWN).table());
        assertNull(CrateTableParser.parse("x", "[1]", KNOWN).table());
        CrateTableParser.Result many = CrateTableParser.parse("x", "{\"rolls\":99,\"entries\":[{\"type\":\"xp\",\"weight\":1}]}", KNOWN);
        assertFalse(many.ok());                                                   // 99 rolls is reported ...
        assertEquals(16, many.table().rollsMax());                                // ... and clamped, the entry is kept
    }

    private static CrateTable shipped(CrateTier tier) throws Exception {
        try (InputStream in = CrateRollerTest.class.getResourceAsStream("/data/nusmp/elite/items/crates/" + tier.id() + ".json")) {
            assertNotNull(in, "missing crate table " + tier.id());
            CrateTableParser.Result res = CrateTableParser.parse(tier.id(), new String(in.readAllBytes(), StandardCharsets.UTF_8), KNOWN);
            assertTrue(res.ok(), "crate table " + tier.id() + ": " + res.errors());
            return res.table();
        }
    }

    @Test
    void shippedTablesParseAndNameRealItems() throws Exception {
        Set<String> ours = EliteIds.allItems();
        Set<String> older = Set.of("magic_stone");                                  // pre-existing items of the mod the tables use
        for (CrateTier tier : CrateTier.values()) {
            CrateTable t = shipped(tier);
            for (RewardEntry e : t.entries()) {
                if (!e.type().equals("item")) continue;
                assertFalse(e.id().isEmpty(), tier + ": an item entry needs an id");
                if (e.id().startsWith("nusmp:")) {
                    String path = e.id().substring("nusmp:".length());
                    assertTrue(ours.contains(path) || older.contains(path), tier + ": unknown nusmp item " + e.id());
                } else {
                    assertTrue(e.id().startsWith("minecraft:"), tier + ": item ids are minecraft: or nusmp: " + e.id());
                }
            }
        }
    }

    @Test
    void higherTiersRollMoreAndKeysChain() throws Exception {
        CrateTable c = shipped(CrateTier.COMMON), r = shipped(CrateTier.RARE), e = shipped(CrateTier.ELITE);
        assertTrue(c.rollsMax() <= r.rollsMax() && r.rollsMax() <= e.rollsMax());
        assertTrue(c.rollsMin() <= r.rollsMin() && r.rollsMin() <= e.rollsMin());
        // an Elite Crate gives more Skill Points than a Rare one on average, and a Rare more than a Common
        assertTrue(meanSp(c) < meanSp(r) && meanSp(r) < meanSp(e), meanSp(c) + " " + meanSp(r) + " " + meanSp(e));
        // the Common table can hand out the next key but never an Elite key (no shortcut to the top)
        assertTrue(c.entries().stream().noneMatch(x -> x.id().equals("nusmp:elite_crate_key")));
    }

    private static double meanSp(CrateTable t) {
        double w = 0, total = t.totalWeight();
        for (RewardEntry x : t.entries()) if (x.type().equals("sp")) w += x.weight() * (x.min() + x.max()) / 2.0;
        return w / total * (t.rollsMin() + t.rollsMax()) / 2.0;
    }
}
