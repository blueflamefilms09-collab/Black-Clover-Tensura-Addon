package com.newuniverse.nusmp.labyrinth.mega.party;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 5.0.0: the even loot split and the party-leader rule. */
class PartyLootTest {
    @Test
    void everyStackAddsUpAndDiffersByAtMostOne() {
        for (int n = 1; n <= 8; n++)
            for (int count = 1; count <= 64; count++)
                for (int start = -3; start < 10; start++) {
                    int[] parts = PartyLoot.split(count, n, start);
                    int sum = 0, min = Integer.MAX_VALUE, max = 0;
                    for (int p : parts) { sum += p; min = Math.min(min, p); max = Math.max(max, p); }
                    assertEquals(count, sum, "count " + count + " among " + n);
                    assertTrue(max - min <= 1, "uneven: " + count + "/" + n);
                }
    }

    @Test
    void theRemaindersRotateSoNobodyIsAheadByMoreThanOneItemOverManyKills() {
        int n = 3, cursor = 0;
        int[] total = new int[n];
        for (int kill = 0; kill < 30; kill++) {
            int count = 1 + kill % 2;                                   // stacks of 1 and 2 among three players: always a remainder
            int[] parts = PartyLoot.split(count, n, cursor);
            for (int i = 0; i < n; i++) total[i] += parts[i];
            cursor = PartyLoot.advance(cursor, count, n);
        }
        int min = Math.min(total[0], Math.min(total[1], total[2])), max = Math.max(total[0], Math.max(total[1], total[2]));
        assertTrue(max - min <= 1, java.util.Arrays.toString(total));
    }

    @Test
    void aSingleItemGoesToTheMemberAtTheCursorAndTheCursorMovesOn() {
        assertArrayEquals(new int[]{0, 1, 0}, PartyLoot.split(1, 3, 1));
        assertEquals(2, PartyLoot.advance(1, 1, 3));
        assertEquals(0, PartyLoot.advance(2, 1, 3));
    }

    @Test
    void theLeaderRuleFollowsTheLeaderOnlyWhenThereIsOne() {
        assertEquals(7, PartyLeader.pick(7, -1));
        assertEquals(3, PartyLeader.pick(7, 3));            // a stronger member goes down to the leader's floor
        assertEquals(9, PartyLeader.pick(2, 9));
        assertEquals(0, PartyLeader.pick(5, 0));            // the leader's first Landing counts too
    }

    @Test
    void theRulesAreWiredAndSwitchable() throws Exception {
        String root = "src/main/java/com/newuniverse/nusmp/labyrinth/mega/";
        String cfg = Files.readString(Path.of(root + "party/PartyConfig.java"));
        assertTrue(cfg.contains("\"leaderLevel\"") && cfg.contains("\"shareLoot\"") && cfg.contains("\"shareRadius\""));
        assertTrue(Files.readString(Path.of(root + "travel/MegaTravel.java")).contains("PartyLeader.pick("));
        assertTrue(Files.readString(Path.of(root + "party/PartyRegistry.java")).contains("PartyLoot.register(gameBus)"));
    }
}
