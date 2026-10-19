package com.newuniverse.nusmp.labyrinth.mega.enc;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the small pure parts of the fix that are not the dice or the search: the stuck-monster memory (so a crowd of idle monsters behind a wall stops holding the next
 * encounter back), the idle report (the one log line a minute that says why nothing happened), the gates of the report, the shortest way along the doorways (the
 * hunters follow the rooms of a maze instead of walking at a wall), and the rule that a floor's sign, up or down, plays no part in how deep a party is.
 */
class EncFixPureTest {
    // ---------------------------------------------------------------- StuckTracker
    @Test
    void aMonsterThatStaysPutIsIdleForAsLongAsItStaysAndTheCountRestartsWhenItMoves() {
        StuckTracker t = new StuckTracker();
        UUID id = UUID.randomUUID();
        assertEquals(0L, t.update(id, 10, 65, 10, 1000L), "the first sighting");
        assertEquals(100L, t.update(id, 10.5, 65, 10.5, 1100L));
        assertEquals(500L, t.update(id, 11.5, 65, 9.0, 1500L), "shuffling within two and a half blocks is not moving");
        assertEquals(500L, t.idle(id, 11.5, 65, 9.0, 1500L), "idle does not change anything");
        assertEquals(0L, t.update(id, 20, 65, 10, 1600L), "a real move restarts the count");
        assertEquals(40L, t.update(id, 20, 65, 10, 1640L));
        assertEquals(0L, t.idle(id, 40, 65, 10, 1700L), "somewhere else it is not idle");
        t.forget(id);
        assertEquals(0L, t.idle(id, 20, 65, 10, 5000L));
        assertEquals(0, t.size());
    }

    @Test
    void manyMonstersAreTrackedIndependently() {
        StuckTracker t = new StuckTracker();
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            UUID id = UUID.randomUUID();
            ids.add(id);
            t.update(id, i * 10, 65, 0, 0L);
        }
        for (int i = 0; i < 50; i++) assertEquals(i % 2 == 0 ? 900L : 0L, t.update(ids.get(i), i % 2 == 0 ? i * 10 : i * 10 + 30, 65, 0, 900L));
        t.clear();
        assertEquals(0, t.size());
    }

    // ---------------------------------------------------------------- IdleReport
    @Test
    void anIdleMinuteIsOneLineWithTheReasonsAndNotAWordWhenSomethingSpawned() {
        IdleReport r = new IdleReport();
        assertNull(r.line(60), "nobody was in the dungeon");
        r.playersPresent();
        assertTrue(r.line(60).contains("no party was rolled for at all"), r.line(60));
        r.pass("COOLDOWN", "the party is left alone for 40 more seconds", false);
        r.pass("QUIET", "rolled d20 7 + -3 = 4 vs DC 17: quiet", false);
        r.pass("COOLDOWN", "the party is left alone for 35 more seconds", false);
        String line = r.line(60);
        assertTrue(line.contains("3 passes"), line);
        assertTrue(line.contains("COOLDOWN x2") && line.contains("QUIET x1"), line);
        assertTrue(line.contains("35 more seconds"), "the latest detail of each reason");
        r.pass("SPAWNED", "'x' (minor): 2 monsters", true);
        assertNull(r.line(60), "an encounter spawned: not idle");
        r.reset();
        assertEquals(0, r.passes());
        assertNull(r.line(60));
        assertFalse(r.worthReporting());
    }

    @Test
    void aLongDetailIsCut() {
        IdleReport r = new IdleReport();
        r.playersPresent();
        r.pass("NO_PLACE", "x".repeat(900), false);
        assertTrue(r.line(60).length() < 400);
    }

    // ---------------------------------------------------------------- EncGates
    @Test
    void theFirstGateThatBlocksIsTheVerdict() {
        List<EncGates.Gate> gates = List.of(EncGates.pass("switch", "true"), EncGates.warn("place", "only on rung 2"), EncGates.block("cooldown", "40 s"), EncGates.block("crowd", "9 live"));
        assertEquals("cooldown", EncGates.firstBlock(gates).name());
        assertTrue(EncGates.verdict(gates).startsWith("Blocked at \"cooldown\""), EncGates.verdict(gates));
        assertTrue(gates.get(2).line().startsWith("[STOP] cooldown: 40 s"));
        assertTrue(gates.get(0).line().startsWith("[ok]"));
        List<EncGates.Gate> open = List.of(EncGates.pass("switch", "true"), EncGates.info("party", "1 player"));
        assertNull(EncGates.firstBlock(open));
        assertTrue(EncGates.verdict(open).startsWith("Nothing blocks"));
        assertTrue(EncGates.verdict(List.of(EncGates.warn("dice", "slow"))).contains("1 gate(s)"));
    }

    @Test
    void durationsReadAsSecondsAndMinutes() {
        assertEquals("0 s", EncGates.duration(-5));
        assertEquals("12 s", EncGates.duration(240));
        assertEquals("1 min 05 s", EncGates.duration(1300));
        assertEquals("30 min 00 s", EncGates.duration(36000));
    }

    // ---------------------------------------------------------------- PatrolGraph.route and HuntRoute
    private static PatrolGraph chain() {
        // 0 - 1 - 2 - 3 - 4, a branch 2 - 5, a safe room 6 between 0 and 4 (a shortcut of two doorways through a room the hunters may not enter), an island 7
        List<PatrolGraph.Node> nodes = new ArrayList<>();
        for (int i = 0; i < 8; i++) nodes.add(new PatrolGraph.Node(i, i * 27 + 13, 65, 13, i != 6, false));
        List<int[]> edges = List.of(new int[] {0, 1}, new int[] {1, 2}, new int[] {2, 3}, new int[] {3, 4}, new int[] {2, 5}, new int[] {0, 6}, new int[] {6, 4});
        return PatrolGraph.build(nodes, edges);
    }

    @Test
    void theRouteIsTheShortestWayAlongTheDoorwaysAndAvoidsRoomsThatMayNotBeEntered() {
        PatrolGraph g = chain();
        assertEquals(List.of(0, 1, 2, 3, 4), g.route(0, 4));
        assertEquals(List.of(4, 3, 2, 1, 0), g.route(4, 0));
        assertEquals(List.of(5, 2, 3), g.route(5, 3));
        assertEquals(List.of(1, 2, 3), g.route(1, 3));
        assertEquals(List.of(0, 1, 2, 3, 4), g.route(0, 4), "through the safe room 6 it would be two doorways instead of four, and the hunters never enter it");
        assertEquals(List.of(2), g.route(2, 2));
        assertEquals(List.of(), g.route(0, 7), "an island has no way");
        assertEquals(List.of(), g.route(0, 99), "an unknown room has none");
        assertEquals(List.of(6, 0), g.route(6, 0), "a monster standing in a room it may not be in can still leave");
        assertEquals(List.of(6, 4, 3), g.route(6, 3), "and leave by either door");
        assertEquals(List.of(1, 0, 6), g.route(1, 6), "and the goal itself may be one");
    }

    @Test
    void aHunterWalksToTheNextRoomOfTheWayAndOnToTheOneAfterOnceThere() {
        PatrolGraph g = chain();
        PatrolGraph.Node next = HuntRoute.next(g, 0, 4, 13, 13);
        assertEquals(1, next.id(), "the way to the player's room starts at the next room");
        assertEquals(2, HuntRoute.next(g, 0, 4, g.node(1).x() - 1.0, 13).id(), "close to that room's waypoint it heads for the one after");
        assertNull(HuntRoute.next(g, 3, 3, 0, 0), "the same room: straight at the player");
        assertNull(HuntRoute.next(g, 0, 7, 0, 0), "no way known: straight at the player");
        assertNull(HuntRoute.next(g, 0, 99, 0, 0));
        assertNull(HuntRoute.next(null, 0, 1, 0, 0));
        assertEquals(1, HuntRoute.next(g, 0, 1, 13, 13).id(), "the player's room is the next one: its waypoint is the goal");
    }

    @Test
    void everyHunterOfAMazeReachesThePlayersRoomByFollowingTheRoute() {
        // a random sparse graph: following HuntRoute.next from every room always gets to the goal (never loops, never stalls)
        Random rnd = new Random(77);
        for (int round = 0; round < 40; round++) {
            int n = 12 + rnd.nextInt(10);
            List<PatrolGraph.Node> nodes = new ArrayList<>();
            for (int i = 0; i < n; i++) nodes.add(new PatrolGraph.Node(i, i * 20, 65, (i % 5) * 20, true, false));
            List<int[]> edges = new ArrayList<>();
            for (int i = 1; i < n; i++) edges.add(new int[] {rnd.nextInt(i), i});
            for (int k = 0; k < 4; k++) edges.add(new int[] {rnd.nextInt(n), rnd.nextInt(n)});
            PatrolGraph g = PatrolGraph.build(nodes, edges);
            int goal = rnd.nextInt(n);
            for (int start = 0; start < n; start++) {
                int at = start, steps = 0;
                while (at != goal && steps++ < n) {
                    PatrolGraph.Node w = HuntRoute.next(g, at, goal, g.node(at).x(), g.node(at).z());
                    assertNotNull(w, "a connected graph always has a way");
                    at = w.id();
                }
                assertEquals(goal, at, "from " + start + " to " + goal);
            }
        }
    }

    // ---------------------------------------------------------------- floors up and down
    @Test
    void aFloorAboveAndTheSameFloorBelowAreEquallyDeepAndTheDeepestMemberDecides() {
        for (int f = 1; f <= 1200; f += 7) {
            assertEquals(ThreatInput.of(1, f, true), ThreatInput.of(1, -f, true), "floor +" + f + " and -" + f);
            assertEquals(f, ThreatInput.of(1, -f, true));
        }
        assertEquals(-7, ThreatInput.deepest(3, -7, 0));
        assertEquals(7, ThreatInput.deepest(-3, 7, 2));
        assertEquals(7, ThreatInput.deepest(7, -7), "a tie goes to the first");
        assertEquals(-7, ThreatInput.deepest(-7, 7), "whichever that is");
        assertEquals(7, ThreatInput.deepest(0, 7, -7));
        assertEquals(0, ThreatInput.deepest(0, 0));
        assertEquals(1, ThreatInput.of(0, 0, true), "an unknown floor falls back on the sector, never below one");
        assertEquals(4, ThreatInput.of(4, 0, true));
        assertEquals(4, ThreatInput.of(4, -300, false), "with the switch off the sector's threat decides on either road");
        assertEquals(Integer.MAX_VALUE, Math.abs((long) Integer.MIN_VALUE + 1) > 0 ? Integer.MAX_VALUE : 0, "keeps the compiler honest about the long arithmetic below");
        assertEquals(-2_000_000_000, ThreatInput.deepest(1_999_999_999, -2_000_000_000), "no overflow in the comparison");
    }
}
