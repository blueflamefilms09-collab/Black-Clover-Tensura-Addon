package com.newuniverse.nusmp.skilltree.ft.ice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.ft.kit.RouteFiles;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Why: Ice-Make's numbers and shapes are tested from the data outward, like the Sky route's: the SHIPPED route file is run through the same {@code problems} check the in-game self-test runs (shape, prices, canvas,
 * balance law, fallback table, two-branch Secret Art), the file text is checked against the schema, and the skill ids of the file against the ids the code registers. Then the pure geometry and bookkeeping that
 * make the constructs safe are pinned with the edge cases that matter: the fan of spears, the share-out of the eagles (never more than two on a foe), the oriented wall (a diagonal wall is not a square, and a fast
 * arrow cannot slip through it between two ticks), the saucer's timetable (a foe is cut when the disc reaches it, on the way out and on the way back), the construct ledger (four at most, the fallen forgotten, no
 * life over ten seconds) and the chill (never more than a body can take). One test BREAKS the data on purpose and demands that the check notice.
 */
class IceNumbersTest {
    private static final String FILE = "ice_make.json";

    @Test
    void theShippedRouteObeysTheLawTheShapeAndTheFallbackTable() throws Exception {
        SkillRoute route = RouteFiles.load(FILE);
        assertEquals(List.of(), IceNumbers.problems(route));
        assertEquals(9, route.nodes().size());
        assertEquals(88, route.totalCost(), "the whole route costs 88 SP");
    }

    @Test
    void theFileTextFollowsTheSchema() throws Exception {
        JsonObject o = RouteFiles.json(FILE);
        assertEquals("fairy_tail", o.get("magic").getAsString());
        assertTrue(o.get("color").getAsString().matches("#[0-9A-Fa-f]{6}"), "colour is #RRGGBB");
        assertTrue(o.get("book").getAsString().matches("[a-z0-9_.-]+:[a-z0-9_./-]+"), "the book is a lower-case resource location");
        assertTrue(o.get("class_id").getAsString().matches("[a-z][a-z0-9_]*"), "the class id is lower-case snake");
        assertEquals("Chisel of Silent Winters", o.get("book_name").getAsString());
        assertEquals(2, o.get("loot_weight").getAsInt(), "the owner's brief: Ice-Make weighs 2 in the dungeon drop");
        Set<String> ids = new HashSet<>();
        for (JsonElement el : o.getAsJsonArray("nodes")) {
            JsonObject n = el.getAsJsonObject();
            String id = n.get("id").getAsString();
            assertTrue(id.matches("[a-z][a-z0-9_]*"), id + " is lower-case snake");
            assertTrue(ids.add(id), id + " is unique");
            assertTrue(n.get("name").getAsString().length() > 2, id + " has a name");
            assertTrue(n.get("desc").getAsString().length() > 20, id + " has a description for the tooltip");
            assertTrue(n.has("icon") && n.get("icon").getAsString().startsWith("minecraft:"), id + " has an item icon");
        }
    }

    @Test
    void theSkillIdsOfTheFileAreTheOnesTheCodeRegisters() throws Exception {
        SkillRoute route = RouteFiles.load(FILE);
        Set<String> inFile = new HashSet<>();
        for (SkillNode n : route.nodes()) if (n.grantsSkill()) inFile.add(n.skill());
        Set<String> inCode = new HashSet<>();
        for (String s : List.of(IceRoute.SKILL_LANCE, IceRoute.SKILL_FLOOR, IceRoute.SKILL_SHIELD, IceRoute.SKILL_CANNON, IceRoute.SKILL_SAUCER, IceRoute.SKILL_EAGLE, IceRoute.SKILL_HAMMER, IceRoute.SKILL_EXCALIBUR)) inCode.add("nusmp:" + s);
        assertEquals(inCode, inFile);
        Set<String> passivesInFile = new HashSet<>();
        for (SkillNode n : route.nodes()) passivesInFile.addAll(n.passives());
        assertEquals(Set.of(IceRoute.PASSIVE_FROST_BODY), passivesInFile);
        assertEquals(IceRoute.ID, route.id());
        assertEquals(IceRoute.CLASS_ID, route.classId());
        Set<String> nodes = new HashSet<>();
        for (SkillNode n : route.nodes()) nodes.add(n.id());
        Set<String> branches = new HashSet<>(IceNumbers.STANDING);
        branches.addAll(IceNumbers.MOVING);
        branches.add(IceRoute.LANCE);
        branches.add(IceRoute.EXCALIBUR);
        assertEquals(nodes, branches, "every node is the root, the Secret Art, or in the standing or the moving branch");
    }

    @Test
    void breakingTheDataIsCaught() throws Exception {
        SkillRoute good = RouteFiles.load(FILE);
        SkillNode hammer = good.node(IceRoute.HAMMER);
        Map<String, List<Double>> noTell = new LinkedHashMap<>(hammer.values());
        noTell.remove("windup");
        List<SkillNode> nodes = new ArrayList<>(good.nodes());
        int i = nodes.indexOf(hammer);
        nodes.set(i, new SkillNode(hammer.id(), hammer.name(), hammer.desc(), hammer.parents(), hammer.cost(), hammer.costs(), hammer.maxLevel(), hammer.skill(), hammer.passives(), noTell, hammer.x(), hammer.y(), hammer.icon()));
        SkillRoute broken = new SkillRoute(good.id(), good.magic(), good.name(), good.desc(), good.book(), good.bookName(), good.bookLore(), good.color(), good.icon(), good.classId(), good.duplicateSp(), good.lootWeight(), nodes);
        List<String> problems = IceNumbers.problems(broken);
        assertFalse(problems.isEmpty(), "a hammer of 8 to 14 raw with no wind-up must be refused");
        boolean named = false;
        for (String s : problems) named |= s.contains("hammer hits for more than") && s.contains("no windup");
        assertTrue(named, "the problem names the missing tell: " + problems);
    }

    @Test
    void theLanceFansAroundTheGaze() {
        assertArrayEq(new double[] {0.0}, IceNumbers.lanceAngles(1, 8));
        assertArrayEq(new double[] {-4.0, 4.0}, IceNumbers.lanceAngles(2, 8));
        assertArrayEq(new double[] {-8.0, 0.0, 8.0}, IceNumbers.lanceAngles(3, 8));
        assertArrayEq(new double[] {0.0}, IceNumbers.lanceAngles(0, 8));
        assertArrayEq(new double[] {-1.5, -0.5, 0.5, 1.5}, IceNumbers.lanceAngles(4, 1));
    }

    private static void assertArrayEq(double[] expected, double[] actual) {
        assertEquals(expected.length, actual.length, "length");
        for (int i = 0; i < expected.length; i++) assertEquals(expected[i], actual[i], 1e-9, "index " + i);
    }

    @Test
    void eaglesShareTheFoesAndNoFoeTakesMoreThanTwo() {
        assertEquals(List.of(0, 1, 2, 0, 1), toList(IceNumbers.eagleAssignment(5, 3, 2)), "the foes in turn, the nearest first");
        assertEquals(List.of(0, 0), toList(IceNumbers.eagleAssignment(5, 1, 2)), "one foe: two eagles fly, the rest stay");
        assertEquals(List.of(0, 1), toList(IceNumbers.eagleAssignment(2, 4, 2)), "fewer eagles than foes: the nearest ones");
        assertEquals(List.of(), toList(IceNumbers.eagleAssignment(5, 0, 2)));
        assertEquals(List.of(), toList(IceNumbers.eagleAssignment(0, 3, 2)));
        for (int eagles = 1; eagles <= 8; eagles++) {
            for (int foes = 1; foes <= 6; foes++) {
                int[] plan = IceNumbers.eagleAssignment(eagles, foes, 2);
                int[] per = new int[foes];
                for (int f : plan) per[f]++;
                for (int c : per) assertTrue(c <= 2, eagles + " eagles on " + foes + " foes put " + c + " on one");
            }
        }
    }

    private static List<Integer> toList(int[] a) {
        List<Integer> out = new ArrayList<>();
        for (int x : a) out.add(x);
        return out;
    }

    @Test
    void theWallIsAnOrientedBoxThatTurnsWithTheCaster() {
        // a wall facing +z: 4 wide (2 each side across x), 1.6 deep (0.8 each way along z), 3 tall, standing on y = 10 at (0, 5)
        IceNumbers.Wall south = new IceNumbers.Wall(0, 10, 5, 0, 1, 2.0, 0.8, 3.0);
        assertTrue(south.contains(0, 11, 5));
        assertTrue(south.contains(1.9, 11, 5.7));
        assertFalse(south.contains(2.1, 11, 5), "past the end of the wall");
        assertFalse(south.contains(0, 11, 5.9), "behind it");
        assertFalse(south.contains(0, 13.5, 5), "above it");
        assertTrue(south.contains(0, 9.6, 5), "just under the ground line still counts");
        assertFalse(south.contains(0, 9.0, 5), "well under it does not");
        // the same wall turned to face +x: its width now runs along z
        IceNumbers.Wall east = new IceNumbers.Wall(0, 10, 5, 1, 0, 2.0, 0.8, 3.0);
        assertTrue(east.contains(0.7, 11, 6.9));
        assertFalse(east.contains(1.0, 11, 5), "past its thin side");
        assertFalse(east.contains(0, 11, 7.1), "past its end");
        // a diagonal wall is a thin diagonal strip, not a big square
        double s = Math.sqrt(0.5);
        IceNumbers.Wall diagonal = new IceNumbers.Wall(0, 10, 0, s, s, 2.0, 0.8, 3.0);
        assertTrue(diagonal.contains(0.5, 11, 0.5), "along its facing, inside the depth");
        assertTrue(diagonal.contains(-1.0, 11, 1.0), "across it, inside the width");
        assertFalse(diagonal.contains(1.9, 11, 1.9), "too far along the facing (2.69 > 0.8)");
        assertFalse(diagonal.contains(2.5, 11, -2.5), "too far across (3.5 > 2)");
    }

    @Test
    void aFastArrowCannotSlipThroughTheWallBetweenTwoTicks() {
        IceNumbers.Wall wall = new IceNumbers.Wall(0, 10, 5, 0, 1, 2.0, 0.8, 3.0);
        // an arrow at 3 blocks a tick: it is at z = 3.0 now and at z = 6.0 next tick, stepping clean over the 1.6 deep wall (z 4.2 to 5.8) if only its positions were tested
        assertFalse(wall.contains(0, 11, 3.0));
        assertFalse(wall.contains(0, 11, 6.0));
        assertTrue(wall.crossedBy(0, 11, 3.0, 0, 11, 6.0), "the move itself crosses the wall");
        assertFalse(wall.crossedBy(0, 11, 0.0, 0, 11, 3.0), "a move that stops short of it does not");
        assertFalse(wall.crossedBy(5, 11, 3.0, 5, 11, 6.0), "a move beside the wall does not");
        assertFalse(wall.crossedBy(0, 14, 3.0, 0, 14, 6.0), "a move over the wall does not");
        assertTrue(wall.crossedBy(0, 11, 5.0, 0, 11, 5.0), "a standing point inside the wall counts");
        assertTrue(wall.reach() > Math.hypot(2.0, 0.8), "the search circle holds the whole wall");
    }

    @Test
    void theSaucerCutsAFoeWhenTheDiscArrives() {
        assertEquals(4, IceNumbers.saucerDelay(5.0, 12.0, 1.4, false), "5 blocks at 1.4 a tick: 3.6 rounds up to 4");
        assertEquals(0, IceNumbers.saucerDelay(0.0, 12.0, 1.4, false), "a foe at the hand is cut at once");
        assertEquals(9, IceNumbers.saucerDelay(12.0, 12.0, 1.4, false), "the far end");
        assertEquals(14, IceNumbers.saucerDelay(5.0, 12.0, 1.4, true), "out 12 and back 7 is 19 blocks, 13.6 ticks rounds up to 14");
    }

    @Test
    void theSaucerReturnIsLaterThanTheCutOnTheWayOut() {
        for (double along = 0; along <= 12.0; along += 1.5) {
            int out = IceNumbers.saucerDelay(along, 12.0, 1.4, false), back = IceNumbers.saucerDelay(along, 12.0, 1.4, true);
            assertTrue(back >= out, "the return pass reaches a foe no sooner than the outward one (along " + along + ")");
        }
        assertEquals(IceNumbers.saucerDelay(12.0, 12.0, 1.4, false), IceNumbers.saucerDelay(12.0, 12.0, 1.4, true), "at the far end the two passes meet");
        assertEquals(1, IceNumbers.saucerDelay(0.1, 12.0, 100.0, false), "even a huge speed takes a whole tick for a foe that is not at the hand");
        assertEquals(120, IceNumbers.saucerDelay(12.0, 12.0, 0.0, false), "a speed of zero is lifted to 0.1 blocks a tick, so the delay stays finite");
        assertEquals(0.0, IceNumbers.along(0, 0, 0, 0, 0, 1, 0, 0, -3, 12), 1e-9, "a point behind the start is at 0");
        assertEquals(12.0, IceNumbers.along(0, 0, 0, 0, 0, 1, 0, 0, 40, 12), 1e-9, "a point past the end is at the reach");
        assertEquals(5.0, IceNumbers.along(0, 0, 0, 0, 0, 1, 3, 2, 5, 12), 1e-9, "the sideways offset does not count");
    }

    @Test
    void theLedgerAllowsFourConstructsAndForgetsTheFallen() {
        long[] none = new long[0];
        assertTrue(IceNumbers.hasRoom(none, 100));
        long[] ledger = none;
        for (int i = 0; i < 4; i++) {
            assertTrue(IceNumbers.hasRoom(ledger, 100), "construct " + (i + 1) + " fits");
            ledger = IceNumbers.record(ledger, 100, 80);
        }
        assertEquals(4, IceNumbers.liveCount(ledger, 100));
        assertFalse(IceNumbers.hasRoom(ledger, 100), "the fifth does not");
        assertFalse(IceNumbers.hasRoom(ledger, 179), "still four standing on the last tick of their life");
        assertTrue(IceNumbers.hasRoom(ledger, 180), "they have fallen by tick 180 (100 + 80)");
        long[] later = IceNumbers.record(ledger, 180, 60);
        assertEquals(1, later.length, "the fallen are forgotten when a new one is recorded");
        assertEquals(240, later[0]);
    }

    @Test
    void noConstructLivesLongerThanTenSeconds() {
        assertEquals(200, IceNumbers.clampLife(5000));
        assertEquals(200, IceNumbers.clampLife(200));
        assertEquals(1, IceNumbers.clampLife(0));
        assertEquals(1, IceNumbers.clampLife(-7));
        assertEquals(100 + 200, IceNumbers.record(new long[0], 100, 9999)[0], "a huge life is cut to ten seconds in the ledger too");
    }

    @Test
    void aChillAddsUpButNeverPassesWhatABodyCanTake() {
        assertEquals(60, IceNumbers.chilled(20, 40, 140));
        assertEquals(140, IceNumbers.chilled(120, 100, 140));
        assertEquals(20, IceNumbers.chilled(-5, 20, 140), "a negative count starts from nothing");
        assertEquals(30, IceNumbers.chilled(30, -10, 140), "a negative addition never thaws");
        assertEquals(1, IceNumbers.chilled(0, 5, 0), "a body that needs no ticks to freeze is capped at 1, not 0");
    }
}
