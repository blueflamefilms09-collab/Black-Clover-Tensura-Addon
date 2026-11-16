package com.newuniverse.nusmp.bossdrops;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why (docs/bosses/boss_drops.md): the data table of a boss decides what a kill pays, so the pure half is proved here: a good file reads, every bad file is refused with the reason, a roll respects
 * chance, count, first-kill-only and the stack cap, and a count is cut into at most the piece budget. No Minecraft type.
 */
class BossDropTableTest {
    private static JsonObject json(String s) { return JsonParser.parseString(s).getAsJsonObject(); }

    private static final String GOOD = "{\"boss\":\"meliodas\",\"max_stacks\":3,\"drops\":["
            + "{\"item\":\"anime_multiverse:trophy_meliodas\",\"chance\":1.0,\"min\":1,\"max\":2},"
            + "{\"item\":\"tensura:silver_coin\",\"chance\":0.5,\"min\":5,\"max\":9,\"killer_only\":true},"
            + "{\"item\":\"tensura:gold_coin\",\"chance\":1.0,\"min\":1,\"max\":1,\"first_kill_only\":true}]}";

    @Test
    void aGoodFileReadsWithItsDefaults() {
        BossDropTable.Table t = BossDropTable.parse(json(GOOD));
        assertEquals("meliodas", t.boss());
        assertEquals(3, t.maxStacks());
        assertEquals(3, t.rows().size());
        assertTrue(t.rows().get(2).firstKillOnly() && !t.rows().get(0).firstKillOnly());
        BossDropTable.Table d = BossDropTable.parse(json("{\"boss\":\"ban\",\"drops\":[{\"item\":\"a:b\"}]}"));
        assertEquals(BossDropTable.DEFAULT_MAX_STACKS, d.maxStacks());
        assertEquals(1.0, d.rows().get(0).chance());
        assertEquals(1, d.rows().get(0).min());
        assertEquals(1, d.rows().get(0).max());
    }

    @Test
    void everyBadFileIsRefused() {
        String[] bad = {
                "{\"drops\":[{\"item\":\"a:b\"}]}",                                                         // no boss
                "{\"boss\":\"Bad Id\",\"drops\":[{\"item\":\"a:b\"}]}",                                     // bad boss id
                "{\"boss\":\"x\"}",                                                                         // no drops
                "{\"boss\":\"x\",\"drops\":[]}",                                                            // empty drops
                "{\"boss\":\"x\",\"max_stacks\":0,\"drops\":[{\"item\":\"a:b\"}]}",                         // max_stacks under 1
                "{\"boss\":\"x\",\"max_stacks\":17,\"drops\":[{\"item\":\"a:b\"}]}",                        // max_stacks over the hard cap
                "{\"boss\":\"x\",\"drops\":[{\"item\":\"nonamespace\"}]}",                                  // bad item id
                "{\"boss\":\"x\",\"drops\":[{\"item\":\"A:B\"}]}",                                          // upper case item id
                "{\"boss\":\"x\",\"drops\":[{\"item\":\"a:b\",\"chance\":0}]}",                             // chance zero
                "{\"boss\":\"x\",\"drops\":[{\"item\":\"a:b\",\"chance\":1.5}]}",                           // chance over one
                "{\"boss\":\"x\",\"drops\":[{\"item\":\"a:b\",\"min\":0}]}",                                // min under 1
                "{\"boss\":\"x\",\"drops\":[{\"item\":\"a:b\",\"min\":3,\"max\":2}]}",                      // max under min
                "{\"boss\":\"x\",\"drops\":[{\"item\":\"a:b\",\"max\":65}]}",                               // count over the cap
                "{\"boss\":\"x\",\"drops\":[{\"item\":\"a:b\",\"killer_only\":false}]}",                    // only the killer is ever paid
                "{\"boss\":\"x\",\"drops\":[3]}",                                                           // a row that is not an object
        };
        for (String s : bad) assertThrows(IllegalArgumentException.class, () -> BossDropTable.parse(json(s)), s);
        StringBuilder many = new StringBuilder("{\"boss\":\"x\",\"drops\":[");
        for (int i = 0; i <= BossDropTable.MAX_ROWS; i++) many.append(i == 0 ? "" : ",").append("{\"item\":\"a:b\"}");
        assertThrows(IllegalArgumentException.class, () -> BossDropTable.parse(json(many + "]}")), "more rows than the table cap");
    }

    @Test
    void aRollHonoursChanceCountFirstKillAndTheStackCap() {
        BossDropTable.Table t = BossDropTable.parse(json(GOOD));
        List<BossDropTable.Stack> low = BossDropTable.roll(t, true, 1.0, () -> 0.0);
        assertEquals(3, low.size(), "the lowest dice pays every row, up to the cap of 3");
        assertEquals(1, low.get(0).count());
        assertEquals(5, low.get(1).count());
        List<BossDropTable.Stack> repeat = BossDropTable.roll(t, false, 1.0, () -> 0.0);
        assertEquals(2, repeat.size(), "no first-kill row on a repeat kill");
        List<BossDropTable.Stack> high = BossDropTable.roll(t, false, 1.0, () -> 0.99);
        assertEquals(1, high.size(), "a high dice only pays the chance-1.0 row");
        assertEquals(2, high.get(0).count(), "the top of the count range");
        assertEquals(0, BossDropTable.roll(t, true, 0.0, () -> 0.0).size(), "a chance scale of zero pays nothing");
        assertEquals(3, BossDropTable.roll(t, true, 5.0, () -> 0.99).size(), "a big scale never lifts a chance over one and the cap holds");
        BossDropTable.Table cap = BossDropTable.parse(json("{\"boss\":\"x\",\"max_stacks\":1,\"drops\":[{\"item\":\"a:b\"},{\"item\":\"a:c\"}]}"));
        assertEquals(1, BossDropTable.roll(cap, true, 1.0, () -> 0.0).size());
    }

    @Test
    void countsStayInsideTheirRangeOverManyRolls() {
        BossDropTable.Table t = BossDropTable.parse(json(GOOD));
        Random r = new Random(7);
        for (int i = 0; i < 5000; i++) {
            for (BossDropTable.Stack s : BossDropTable.roll(t, i % 2 == 0, 1.0, r::nextDouble)) {
                assertTrue(s.count() >= 1 && s.count() <= 9, "count " + s.count());
            }
            assertTrue(BossDropTable.roll(t, true, 1.0, r::nextDouble).size() <= t.maxStacks());
        }
    }

    @Test
    void aCountIsCutIntoABoundedNumberOfPieces() {
        assertEquals(List.of(16, 16, 8), BossDropTable.split(40, 16, 32));
        assertEquals(List.of(1, 1), BossDropTable.split(2, 1, 32));
        assertEquals(List.of(), BossDropTable.split(0, 16, 32));
        assertEquals(2, BossDropTable.split(64, 1, 2).size(), "never more pieces than the room");
        assertEquals(List.of(1, 1, 1, 1), BossDropTable.split(5, 0, 4), "a zero stack size is treated as one piece per item, bounded by the room");
    }
}
