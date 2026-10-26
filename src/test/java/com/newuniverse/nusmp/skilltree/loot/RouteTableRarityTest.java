package com.newuniverse.nusmp.skilltree.loot;

import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import com.newuniverse.nusmp.skilltree.data.RouteSchema;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.rarity.BlockedFranchises;
import com.newuniverse.nusmp.skilltree.rarity.BookFacts;
import com.newuniverse.nusmp.skilltree.rarity.BookRarity;
import com.newuniverse.nusmp.skilltree.unique.LineIndex;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: 3.26.0 hangs two things on the cached weight table: one table PER RARITY over the same routes (a tier that drew "rare" must pick a rare line, still weighted by loot_weight) and the franchise
 * BLACKLIST (a blocked line has weight 0 in every table). Both must cost nothing per drop, so the table is built once per registry version AND per setting text, and a thousand drops do not rebuild it.
 * This runs the REAL shipped lines (their franchises and rarities come from the shipped book table) as routes built from json: the per-rarity tables hold only lines of their rarity with their own weights
 * and together weigh what the main table weighs; with the default blacklist set no table can ever pick a Tensura, Black Clover or Magi line; changing the setting rebuilds once and putting it back restores
 * the weights; and a route the book table does not know (a data pack's line) is common and never blocked.
 */
class RouteTableRarityTest {
    private final Supplier<String> before = BlockedFranchises.source();

    @AfterEach
    void putBack() {
        BlockedFranchises.setSource(before);
        RouteRegistry.clearClient();
        RouteTable.clear();
    }

    private static SkillRoute route(String id, int weight) {
        String json = "{\"name\":\"" + id + "\",\"book\":\"nusmp:" + id + "_book\",\"class_id\":\"" + id + "\",\"color\":\"#112233\",\"loot_weight\":" + weight + ",\"nodes\":[{\"id\":\"a\"}]}";
        SkillRoute r = RouteSchema.parse("nusmp:" + id, json).route();
        assertNotNull(r);
        return r;
    }

    /** every shipped line as a route, weight 2 for all (3 for epic ones, to see that the weights survive) */
    private static List<SkillRoute> shipped() {
        List<SkillRoute> out = new ArrayList<>();
        for (LineIndex.Line l : LineIndex.shipped()) out.add(route(l.bare(), l.rarity() == BookRarity.EPIC ? 3 : 2));
        return out;
    }

    @Test
    void theRarityTablesPartitionTheWeightTableAndKeepTheWeights() {
        RouteRegistry.clear();
        List<SkillRoute> routes = shipped();
        assertTrue(routes.size() >= 200);
        RouteRegistry.setClient(routes);
        BlockedFranchises.setSource(null);
        RouteTable.Snap s = RouteTable.current();
        long sum = 0;
        for (BookRarity r : BookRarity.values()) {
            WeightedTable t = s.rarityTable(r);
            sum += t.total();
            for (int i = 0; i < s.routes().length; i++) {
                int w = t.weightOf(i);
                if (w > 0) {
                    assertEquals(r, BookFacts.rarityOf(s.routes()[i].id()), s.routes()[i].id() + " is in the wrong table");
                    assertEquals(s.table().weightOf(i), w, "the weight is the line's own");
                }
            }
            assertFalse(t.isEmpty(), "the shipped table has " + r.id() + " lines");
        }
        assertEquals(s.table().total(), sum);
        assertTrue(s.rarityTable(null) == s.rarityTable(BookRarity.COMMON), "null reads as common");
    }

    @Test
    void aPickFromARarityTableIsALineOfThatRarity() {
        RouteRegistry.clear();
        RouteRegistry.setClient(shipped());
        BlockedFranchises.setSource(null);
        RouteTable.Snap s = RouteTable.current();
        Random random = new Random(9);
        for (BookRarity r : BookRarity.values()) {
            for (int i = 0; i < 2000; i++) {
                SkillRoute pick = s.at(s.rarityTable(r).pick(random::nextDouble));
                assertNotNull(pick);
                assertEquals(r, BookFacts.rarityOf(pick.id()));
            }
        }
    }

    @Test
    void theDefaultBlacklistZeroesEveryBlockedLineInEveryTable() {
        RouteRegistry.clear();
        RouteRegistry.setClient(shipped());
        BlockedFranchises.setSource(() -> BlockedFranchises.DEFAULT);
        RouteTable.Snap s = RouteTable.current();
        int blocked = 0;
        for (int i = 0; i < s.routes().length; i++) {
            boolean off = BookFacts.isBlocked(s.routes()[i].id(), BlockedFranchises.DEFAULT);
            if (off) blocked++;
            if (off) {
                assertEquals(0, s.table().weightOf(i), s.routes()[i].id());
                for (BookRarity r : BookRarity.values()) assertEquals(0, s.rarityTable(r).weightOf(i), s.routes()[i].id() + " in the " + r.id() + " table");
            }
        }
        assertTrue(blocked >= 75, "Tensura, Black Clover and Magi are about 75 lines: " + blocked);
        Random random = new Random(4);
        for (int i = 0; i < 20_000; i++) {
            SkillRoute pick = s.at(s.table().pick(random::nextDouble));
            assertFalse(BookFacts.isBlocked(pick.id(), BlockedFranchises.DEFAULT), pick.id());
        }
    }

    @Test
    void theTableIsRebuiltOnceForANewSettingAndNotPerDraw() {
        RouteRegistry.clear();
        RouteRegistry.setClient(shipped());
        String[] setting = {""};
        BlockedFranchises.setSource(() -> setting[0]);
        RouteTable.Snap open = RouteTable.current();
        int builds = RouteTable.builds();
        for (int i = 0; i < 1000; i++) assertSame(open, RouteTable.current());
        assertEquals(builds, RouteTable.builds(), "no rebuild while neither the registry nor the setting changed");
        setting[0] = BlockedFranchises.DEFAULT;
        RouteTable.Snap shut = RouteTable.current();
        assertNotSame(open, shut);
        assertEquals(builds + 1, RouteTable.builds(), "one rebuild for the new setting");
        for (int i = 0; i < 1000; i++) assertSame(shut, RouteTable.current());
        assertEquals(builds + 1, RouteTable.builds());
        assertTrue(shut.table().total() < open.table().total());
        setting[0] = "";
        RouteTable.Snap again = RouteTable.current();
        assertEquals(open.table().total(), again.table().total(), "putting the setting back restores the weights");
        assertEquals(open.table().size(), again.table().size());
    }

    @Test
    void aSettingOfJunkOrNothingBlocksNothing() {
        RouteRegistry.clear();
        RouteRegistry.setClient(shipped());
        BlockedFranchises.setSource(() -> ",,, ;; ../x <b>");
        RouteTable.Snap junk = RouteTable.current();
        BlockedFranchises.setSource(() -> "");
        assertEquals(RouteTable.current().table().total(), junk.table().total());
    }

    @Test
    void aRouteTheBookTableDoesNotKnowIsCommonAndNeverBlocked() {
        RouteRegistry.clear();
        RouteRegistry.setClient(List.of(route("zz_datapack_line", 5)));
        BlockedFranchises.setSource(() -> BlockedFranchises.DEFAULT);
        RouteTable.Snap s = RouteTable.current();
        assertEquals(5, s.table().total());
        assertEquals(5, s.rarityTable(BookRarity.COMMON).total());
        assertTrue(s.rarityTable(BookRarity.EPIC).isEmpty());
        assertTrue(s.rarityTable(BookRarity.MYTHIC).isEmpty());
        assertEquals(BookRarity.COMMON, BookFacts.rarityOf("zz_datapack_line"));
        assertFalse(BookFacts.isBlocked("zz_datapack_line"));
        assertEquals("", BookFacts.franchiseOf("zz_datapack_line"));
        assertNull(BookFacts.line(null));
    }

    @Test
    void theOldTwoTableFormStillCompilesAndHasNoRarityTables() {
        RouteTable.Snap old = new RouteTable.Snap(new SkillRoute[0], WeightedTable.EMPTY);
        for (BookRarity r : BookRarity.values()) assertTrue(old.rarityTable(r).isEmpty());
        assertTrue(RouteTable.Snap.EMPTY.rarityTable(BookRarity.EPIC).isEmpty());
    }
}
