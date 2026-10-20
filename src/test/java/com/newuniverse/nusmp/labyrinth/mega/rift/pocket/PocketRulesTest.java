package com.newuniverse.nusmp.labyrinth.mega.rift.pocket;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the numbers of the exclusive pockets that do not need a world: a trap never kills, gravity is what the zone says, no single blow opens a seal, the room nearest the exit falls last
 * in a collapse, an empty tomb is cleared in time and not at once, and the hard gate of the relics lets a roll through only inside the right tomb at the right stage.
 */
class PocketRulesTest {
    @Test
    void aTrapNeverKillsAndNeverExceedsItsCap() {
        for (PocketElement e : PocketElement.values()) {
            assertTrue(e.share() > 0 && e.share() <= PocketRules.TRAP_CAP, e + " share");
            assertFalse(PocketElement.forbiddenEffect(e.effect()), e + " uses a hunger effect");
            assertTrue(e.particle().startsWith("minecraft:") && e.sound().startsWith("minecraft:") && e.damageType().startsWith("minecraft:"));
            assertTrue(e.rune().startsWith("minecraft:"));
        }
        assertEquals(6f, PocketRules.trapDamage(20f, 20f, 0.3), 1e-5f, "30 percent of 20");
        assertEquals(6f, PocketRules.trapDamage(20f, 20f, 0.9), 1e-5f, "capped at 30 percent whatever the share");
        assertEquals(2f, PocketRules.trapDamage(20f, 3f, 0.3), 1e-5f, "never more than leaves half a heart");
        assertEquals(0f, PocketRules.trapDamage(20f, 1f, 0.3), 1e-5f, "a victim at half a heart takes nothing");
        assertEquals(0f, PocketRules.trapDamage(20f, 0.5f, 0.3), 1e-5f);
        assertEquals(0f, PocketRules.trapDamage(0f, 5f, 0.3), 1e-5f);
    }

    @Test
    void aTileIsSteppedOnWithinItsReach() {
        assertTrue(PocketRules.onTile(10.5, 20.5, 10, 20));
        assertTrue(PocketRules.onTile(11.5, 20.5, 10, 20), "one block off the middle");
        assertFalse(PocketRules.onTile(12.5, 20.5, 10, 20), "two blocks off");
        assertFalse(PocketRules.onTile(10.5, 22.6, 10, 20));
    }

    @Test
    void gravityIsWhatTheZoneSays() {
        assertEquals(0.08, PocketRules.gravity(null, 1.0), 1e-12);
        assertEquals(-0.06, PocketRules.gravity(PocketLayout.Kind.INVERTED, 1.0), 1e-12);
        assertEquals(0.02, PocketRules.gravity(PocketLayout.Kind.NEGATED, 1.0), 1e-12);
        assertEquals(0.08 * 0.5, PocketRules.gravity(null, 0.5), 1e-12, "a pocket with half gravity");
        assertEquals(0.02, PocketRules.gravity(PocketLayout.Kind.NEGATED, 1.0), 1e-12);
        assertEquals(0.01, PocketRules.gravity(PocketLayout.Kind.NEGATED, 0.125), 1e-12, "a weightless zone in a pocket of one eighth gravity is lighter still");
        assertEquals(-0.14, PocketRules.gravityModifier(-0.06), 1e-12);
        assertTrue(PocketRules.gravity(PocketLayout.Kind.INVERTED, 1.0) < 0, "the ceiling walker is pulled up");
        assertTrue(PocketRules.SAFE_FALL >= 20, "a drop from the ceiling of a tall corridor must be forgiven");
    }

    @Test
    void noSingleBlowOpensTheSealAndAnUnchargedSwingCountsForNothing() {
        assertEquals(0.0, PocketRules.sealForce(350, 0.5, 2400), 0.0);
        assertEquals(130.0, PocketRules.sealForce(130, 1.0, 2400), 0.0);
        assertEquals(600.0, PocketRules.sealForce(9999, 1.0, 2400), 0.0, "at most a quarter of the seal");
        assertEquals(0.0, PocketRules.sealForce(-5, 1.0, 2400), 0.0);
        int blows = 0;
        double hp = 2400;
        while (hp > 0 && blows < 100) {
            hp -= PocketRules.sealForce(9999, 1.0, 2400);
            blows++;
        }
        assertEquals(4, blows, "even an absurd weapon needs four swings");
        assertEquals(-1, PocketRules.crackStage(2400, 2400));
        assertEquals(0, PocketRules.crackStage(2399, 2400));
        assertEquals(5, PocketRules.crackStage(1200, 2400));
        assertEquals(9, PocketRules.crackStage(0, 2400));
        assertEquals(9, PocketRules.crackStage(-50, 2400));
    }

    @Test
    void aMageUnravelsTheSealOfTheirOwnMagicOnly() {
        List<String> flame = List.of("FLAME", "MAGMA", "EXPLOSION");
        assertTrue(PocketRules.unravels("flame", flame));
        assertTrue(PocketRules.unravels("MAGMA", flame));
        assertFalse(PocketRules.unravels("WATER", flame));
        assertFalse(PocketRules.unravels("", flame));
        assertFalse(PocketRules.unravels(null, flame));
    }

    @Test
    void theDeepestRoomFallsFirstAndTheEntryNeverBeforeTheEnd() {
        int total = 3000;
        int deep = PocketRules.roomFallsAt(8, 8, total), mid = PocketRules.roomFallsAt(4, 8, total), near = PocketRules.roomFallsAt(1, 8, total);
        assertTrue(deep < mid && mid < near, deep + " < " + mid + " < " + near);
        assertEquals((int) Math.round(total * PocketRules.FIRST_FALL), deep);
        assertTrue(near < total, "the room next to the entry falls before the end");
        assertEquals(total, PocketRules.roomFallsAt(0, 8, total), "the entry stands to the end");
        assertTrue(PocketRules.roomWarned(deep - 50, deep));
        assertFalse(PocketRules.roomWarned(deep - 200, deep));
        assertFalse(PocketRules.roomWarned(deep, deep));
        for (long s = 0; s < 40; s++) {
            PocketLayout l = LayoutGen.generate(s + 3, new LayoutGen.Params(7, 20, 0.35, 10, 1, 1, 1, List.of("fire")));
            List<Integer> order = PocketRules.fallOrder(l);
            assertEquals(l.rooms().size() - 1, order.size());
            assertFalse(order.contains(l.entryId()));
            int[] depth = PocketRules.depthsAll(l);
            for (int i = 1; i < order.size(); i++) assertTrue(depth[order.get(i - 1)] >= depth[order.get(i)], "deepest first");
            int deepest = 0;
            for (int dd : depth) deepest = Math.max(deepest, dd);
            assertEquals(deepest, depth[order.get(0)], "the first room to fall is one of the deepest");
        }
    }

    @Test
    void anEmptyTombIsKeptForAWhileAndThenCleared() {
        assertFalse(PocketRules.emptyCollapse(false, 99999, 99999, false), "never with someone inside");
        assertFalse(PocketRules.emptyCollapse(true, 20L * 200, 20L * 100, false), "never in its first grace");
        assertFalse(PocketRules.emptyCollapse(true, 20L * 60, 20L * 400, false), "not before it has been empty long enough");
        assertTrue(PocketRules.emptyCollapse(true, 20L * 130, 20L * 400, false));
        assertTrue(PocketRules.emptyCollapse(true, 0, 0, true), "a collapsing tomb with nobody inside is cleared at once");
    }

    @Test
    void theRelicGateOpensOnlyInsideTheRightTombAtTheRightStage() {
        assertTrue(PocketRules.gate("ember_sanctum", "ember_sanctum", PocketRules.Stage.BOSS, true, false));
        assertFalse(PocketRules.gate("ember_sanctum", "ember_sanctum", PocketRules.Stage.BOSS, false, true), "the boss table needs the boss dead");
        assertTrue(PocketRules.gate("ember_sanctum", "ember_sanctum", PocketRules.Stage.TREASURY, false, true));
        assertFalse(PocketRules.gate("ember_sanctum", "ember_sanctum", PocketRules.Stage.TREASURY, true, false), "the treasury table needs the seal open");
        assertTrue(PocketRules.gate("ember_sanctum", "ember_sanctum", PocketRules.Stage.ANY, false, false));
        assertFalse(PocketRules.gate("frozen_cathedral", "ember_sanctum", PocketRules.Stage.ANY, true, true), "another tomb");
        assertFalse(PocketRules.gate("", "ember_sanctum", PocketRules.Stage.ANY, true, true), "outside every tomb");
        assertFalse(PocketRules.gate(null, "ember_sanctum", PocketRules.Stage.ANY, true, true));
        assertFalse(PocketRules.gate("ember_sanctum", null, PocketRules.Stage.ANY, true, true));
        assertEquals(PocketRules.Stage.BOSS, PocketRules.stageOf("Boss"));
        assertEquals(PocketRules.Stage.ANY, PocketRules.stageOf("whatever"));
        assertEquals(PocketRules.Stage.ANY, PocketRules.stageOf(null));
    }

    @Test
    void monstersFollowTheRoomAndThePartyAndTheMobsFollowTheirHabitat() {
        assertEquals(0, PocketRules.monsterCount(PocketLayout.Kind.ENTRY, 5, 3));
        assertEquals(0, PocketRules.monsterCount(PocketLayout.Kind.TREASURY, 6, 3));
        assertTrue(PocketRules.monsterCount(PocketLayout.Kind.COMBAT, 7, 1) >= 5);
        assertTrue(PocketRules.monsterCount(PocketLayout.Kind.COMBAT, 7, 6) > PocketRules.monsterCount(PocketLayout.Kind.COMBAT, 7, 1));
        assertTrue(PocketRules.monsterCount(PocketLayout.Kind.COMBAT, 7, 6) <= 12);
        assertEquals(3, PocketRules.monsterCount(PocketLayout.Kind.TRAP, 6, 1));
        assertEquals(1, PocketRules.effectiveFloor(0, 0));
        assertEquals(120, PocketRules.effectiveFloor(5, 120));
        assertEquals(300, PocketRules.effectiveFloor(-300, 120));
    }

    @Test
    void theTombARiftLeadsToIsAFunctionOfItsSeedAndTheDepth() {
        List<PocketDef> defs = new java.util.ArrayList<>();
        for (int i = 0; i < 4; i++) defs.add(PocketDataTest.stub("stub" + i, i == 3 ? 100 : 0, i == 0 ? 30 : 10));
        for (long s = 0; s < 200; s++) {
            int a = PocketRules.pick(defs, s, 10), b = PocketRules.pick(defs, s, 10);
            assertEquals(a, b);
            assertTrue(a >= 0 && a <= 2, "the tomb that needs depth 100 is not offered at depth 10");
        }
        int[] seen = new int[4];
        for (long s = 0; s < 4000; s++) seen[PocketRules.pick(defs, s, 500)]++;
        for (int n : seen) assertTrue(n > 200, "every tomb is offered at depth: " + java.util.Arrays.toString(seen));
        assertTrue(seen[0] > seen[1], "the heavier tomb is picked more often: " + java.util.Arrays.toString(seen));
        assertEquals(-1, PocketRules.pick(List.of(), 5, 10));
        assertEquals(-1, PocketRules.pick(defs.subList(3, 4), 5, 10), "none qualifies");
        Random r = new Random(1);
        assertEquals(-1, PocketRules.pickMob(List.of(), false, r));
    }

    @Test
    void theMonstersStandOneTierAboveTheFloorAndTheMegaBossAboveThem() {
        double[] hp = {24.0, 40.0, 66.0, 110.0, 180.0};
        assertEquals(40.0 / 24.0, PocketRules.tierFactor(hp, 1, 1), 1e-9, "a tier-1 body on floor 1 is made a tier-2 monster");
        assertEquals(1.0, PocketRules.tierFactor(hp, 2, 1), 1e-9, "a tier-2 body on floor 1 already is one");
        assertEquals(0.5, PocketRules.tierFactor(hp, 5, 1), 1e-9, "never below half");
        assertEquals(8.0, PocketRules.tierFactor(new double[]{1.0, 100.0}, 1, 1), 1e-9, "never above eight times");
        assertEquals(180.0 / 110.0, PocketRules.tierFactor(hp, 4, 900), 1e-9, "the top tier is the cap");
        assertEquals(1, PocketRules.tierOf(0));
        assertEquals(1, PocketRules.tierOf(200));
        assertEquals(2, PocketRules.tierOf(201));
        assertEquals(5, PocketRules.tierOf(-5000));
        assertEquals(9, PocketRules.bossThreat(1));
        assertEquals(1000, PocketRules.bossThreat(100000));
        assertEquals(9, PocketRules.bossThreat(0), "a floor below 1 counts as 1");
    }

    @Test
    void holdingTheButtonIsWorthLessThanASwingAndNoNumberIsNegative() {
        assertEquals(0.0, PocketRules.sealHold(-3.0), 0.0);
        assertEquals(0.2, PocketRules.sealHold(20.0), 1e-9);
        double swingsPerSecond = 1.0 / 0.6, holdPerSecond = 20 * PocketRules.sealHold(20.0);
        assertTrue(holdPerSecond < swingsPerSecond * 20.0, "a held button is slower than charged swings");
        assertTrue(holdPerSecond > 0.0);
    }
}
