package com.newuniverse.nusmp.labyrinth.mega.rift;

import com.newuniverse.nusmp.dungeon.DungeonGuardRules;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the owner played the hidden realms and found one room, or no switch (1.101.0, docs/labyrinth/pocket_fix.md). This test is the harness that builds the realm's plan on paper, block by block, for both
 * kinds and many seeds, and walks it: there are five rooms, the arrival reaches the Antechamber, the Hall and the lever chamber on foot but NOT the Vault while the bars stand, the Vault once they are lifted,
 * the lever stands in a room that is not behind the bars, on a wall, with a lit lantern and a sign beside it, the doorways are air over wall (the very edit the Dungeon Guard refuses to everybody but the
 * mod's own builders: the rule is tested here too), and the lever's timing rules never lock anybody in.
 */
class RealmPlanTest {
    @Test
    void aRealmIsFiveRoomsNotOne() {
        for (boolean boss : new boolean[]{true, false}) {
            RealmPlan p = RealmPlan.of(boss, 1234L);
            assertEquals(5, p.rooms().size());
            for (String id : new String[]{RealmPlan.ANTECHAMBER, RealmPlan.CORRIDOR, RealmPlan.HALL, RealmPlan.VAULT, RealmPlan.LEVER_ROOM}) assertNotNull(p.room(id), id);
            assertEquals(4, p.doors().size(), "antechamber-corridor, corridor-hall and two walls to the lever chamber");
        }
    }

    @Test
    void theArrivalWalksToEveryRoomButTheBarredVault() {
        for (long seed = 0; seed < 40; seed++) {
            for (boolean boss : new boolean[]{true, false}) {
                RealmPlan p = RealmPlan.of(boss, seed * 7919L + 3);
                Set<String> shut = p.reachable(false), open = p.reachable(true);
                assertTrue(shut.containsAll(Set.of(RealmPlan.ANTECHAMBER, RealmPlan.CORRIDOR, RealmPlan.HALL, RealmPlan.LEVER_ROOM)), "before the bars lift: " + shut);
                assertFalse(shut.contains(RealmPlan.VAULT), "the vault is behind the bars");
                assertTrue(open.contains(RealmPlan.VAULT) && open.size() == 5, "after the bars lift: " + open);
            }
        }
    }

    @Test
    void whenTheGuardRefusesTheCarvingTheRealmIsOneClosedRoom() {
        // the defect of 1.73.0 to 1.100.0, reproduced on paper: every doorway is air over wall, the guard refuses it, and the player is shut in the Antechamber
        for (boolean boss : new boolean[]{true, false}) {
            RealmPlan p = RealmPlan.of(boss, 8L);
            assertEquals(Set.of(RealmPlan.ANTECHAMBER), p.reachable(false, true), "ONLY ONE ROOM");
            assertEquals(Set.of(RealmPlan.ANTECHAMBER), p.reachable(true, true), "and the bars would not even matter");
            assertEquals(5, p.reachable(true, false).size(), "built inside the guard's internal span it is five rooms");
        }
    }

    @Test
    void theLeverIsReachableAndNotBehindTheDoorItOpens() {
        RealmPlan p = RealmPlan.of(true, 99L);
        RealmPlan.Feature lever = p.first(RealmPlan.Type.LEVER);
        assertNotNull(lever);
        RealmPlan.Room r = p.roomAt(lever.x() + 0.5, lever.z() + 0.5);
        assertNotNull(r);
        assertEquals(RealmPlan.LEVER_ROOM, r.id());
        assertTrue(p.reachable(false).contains(r.id()), "the lever's room is reachable with the bars shut: no soft lock");
        assertFalse(RealmPlan.VAULT.equals(r.id()));
        // it hangs on a wall: the block behind it (the way it faces, reversed) is wall
        java.util.Map<Long, RealmPlan.Mat> v = p.voxels();
        int dz = lever.tag().equals("S") ? -1 : lever.tag().equals("N") ? 1 : 0, dx = lever.tag().equals("E") ? -1 : lever.tag().equals("W") ? 1 : 0;
        assertEquals(RealmPlan.Mat.WALL, v.get(RealmPlan.key(lever.x() + dx, lever.y(), lever.z() + dz)), "the lever is on a wall");
        assertEquals(RealmPlan.Mat.AIR, v.get(RealmPlan.key(lever.x(), lever.y(), lever.z())), "and in an air cell");
        // lit: a sea lantern in the wall each side and a lantern on the floor each side, a sign over it
        long lights = p.ops().stream().filter(f -> f.mat() == RealmPlan.Mat.LIGHT && f.z0() == -20 && f.y0() == 1).count();
        assertEquals(2, lights, "two sea lanterns flank the lever");
        assertEquals(2, p.all(RealmPlan.Type.LANTERN).size());
        assertTrue(p.all(RealmPlan.Type.SIGN).stream().anyMatch(s -> s.tag().endsWith(":lever") && s.x() == lever.x() && s.z() == lever.z()), "a sign over the lever");
        assertTrue(p.all(RealmPlan.Type.SIGN).stream().anyMatch(s -> s.tag().endsWith(":vault")), "a sign over the bars says where the lever is");
        assertTrue(p.all(RealmPlan.Type.SIGN).stream().anyMatch(s -> s.tag().endsWith(":chamber")), "a sign at the chamber's door");
    }

    @Test
    void theBarsAreNineAndTheVaultHoldsThreeChestsAndASecondExit() {
        RealmPlan p = RealmPlan.of(false, 5L);
        assertEquals(9, p.bars().size());
        assertEquals(3, p.all(RealmPlan.Type.CHEST).size());
        RealmPlan.Feature exit = p.first(RealmPlan.Type.EXIT_VAULT), first = p.first(RealmPlan.Type.EXIT);
        assertNotNull(exit);
        assertEquals(RealmPlan.VAULT, p.roomAt(exit.x() + 0.5, exit.z() + 0.5).id());
        assertEquals(RealmPlan.ANTECHAMBER, p.roomAt(first.x() + 0.5, first.z() + 0.5).id());
        for (RealmPlan.Feature c : p.all(RealmPlan.Type.CHEST)) assertEquals(RealmPlan.VAULT, p.roomAt(c.x() + 0.5, c.z() + 0.5).id());
    }

    @Test
    void thePuzzleHasFourPlatesInTheHallAndTheBossLairHasASpawn() {
        RealmPlan puzzle = RealmPlan.of(false, 77L), boss = RealmPlan.of(true, 77L);
        assertEquals(4, puzzle.all(RealmPlan.Type.PLATE).size());
        assertEquals(4, puzzle.all(RealmPlan.Type.ORDER).size());
        assertNull(puzzle.first(RealmPlan.Type.BOSS_SPAWN));
        assertEquals(6, puzzle.all(RealmPlan.Type.LANTERN).size(), "two beside the lever, one beside each plate");
        assertTrue(puzzle.all(RealmPlan.Type.SIGN).stream().anyMatch(s -> s.tag().endsWith(":plates")), "a sign says what the plates are");
        java.util.Map<Long, RealmPlan.Mat> v = puzzle.voxels();
        for (RealmPlan.Feature f : puzzle.all(RealmPlan.Type.LANTERN)) {
            assertEquals(RealmPlan.Mat.AIR, v.get(RealmPlan.key(f.x(), f.y(), f.z())), "a lantern stands in air");
            assertEquals(RealmPlan.Mat.FLOOR, v.get(RealmPlan.key(f.x(), f.y() - 1, f.z())), "on the floor");
        }
        assertEquals(0, boss.all(RealmPlan.Type.PLATE).size());
        assertEquals(RealmPlan.HALL, boss.roomAt(boss.first(RealmPlan.Type.BOSS_SPAWN).x() + 0.5, boss.first(RealmPlan.Type.BOSS_SPAWN).z() + 0.5).id());
        for (RealmPlan.Feature f : puzzle.all(RealmPlan.Type.PLATE)) assertEquals(RealmPlan.HALL, puzzle.roomAt(f.x() + 0.5, f.z() + 0.5).id());
    }

    private static void assertNull(Object o) { org.junit.jupiter.api.Assertions.assertNull(o); }

    @Test
    void theDoorwaysAreAirOverWallAndTheGuardRefusesExactThatToAnyoneButTheBuilder() {
        RealmPlan p = RealmPlan.of(true, 1L);
        java.util.Map<Long, RealmPlan.Mat> shell = new java.util.HashMap<>();
        for (int i = 0; i < p.shellOps(); i++) {
            RealmPlan.Fill f = p.ops().get(i);
            for (int y = f.y0(); y <= f.y1(); y++) for (int z = f.z0(); z <= f.z1(); z++) for (int x = f.x0(); x <= f.x1(); x++) shell.put(RealmPlan.key(x, y, z), f.mat());
        }
        int carved = 0;
        for (RealmPlan.Door d : p.doors())
            for (int y = d.y0(); y <= d.y1(); y++) for (int z = d.z0(); z <= d.z1(); z++) for (int x = d.x0(); x <= d.x1(); x++) {
                RealmPlan.Mat was = shell.get(RealmPlan.key(x, y, z));
                assertTrue(was == RealmPlan.Mat.WALL || was == RealmPlan.Mat.CEIL || was == RealmPlan.Mat.AIR, "a doorway cell was " + was);
                if (was == RealmPlan.Mat.WALL) {
                    carved++;
                    // the bug of 1.99.0 and before: a change from a held block to air is refused outside the builder's span ...
                    assertTrue(DungeonGuardRules.refusesChange(0, false, false, false, true, false, false));
                    // ... and goes through inside it
                    assertFalse(DungeonGuardRules.refusesChange(1, false, false, false, true, false, false));
                }
            }
        assertTrue(carved >= 4 * 3 * 3, "the doorways cut real wall: " + carved + " blocks");
    }

    @Test
    void theLeverNeverLocksAnyoneIn() {
        long unlock = 20 * 120L, auto = 20 * 420L;
        assertEquals(PocketSwitchRules.State.LOCKED, PocketSwitchRules.state(false, false, 100, unlock));
        assertEquals(PocketSwitchRules.State.READY, PocketSwitchRules.state(false, true, 100, unlock), "the boss is dead: it works at once");
        assertEquals(PocketSwitchRules.State.READY, PocketSwitchRules.state(false, false, unlock, unlock), "after the delay it works whatever the boss does");
        assertEquals(PocketSwitchRules.State.SPENT, PocketSwitchRules.state(true, true, 0, unlock));
        assertFalse(PocketSwitchRules.autoOpens(false, auto - 1, unlock, auto, true));
        assertTrue(PocketSwitchRules.autoOpens(false, auto, unlock, auto, true), "by itself after N minutes");
        assertFalse(PocketSwitchRules.autoOpens(false, auto, unlock, auto, false), "an empty realm is left to its own clock");
        assertFalse(PocketSwitchRules.autoOpens(true, auto * 2, unlock, auto, true));
        assertTrue(PocketSwitchRules.autoAt(unlock, 10) >= unlock + PocketSwitchRules.AUTO_GAP_TICKS, "the door never opens before the lever works");
        assertEquals(0, PocketSwitchRules.secondsToUnlock(true, 0, unlock));
        assertEquals(120, PocketSwitchRules.secondsToUnlock(false, 0, unlock));
        assertTrue(PocketSwitchRules.fits(20L * 60 * 12, unlock, auto), "inside the default 12 minute pocket");
    }

    @Test
    void thePlanStaysInsideItsCellAndUnderTheBuildHeight() {
        RealmPlan p = RealmPlan.of(true, 4L);
        int[] e = p.extent();
        assertTrue(e[0] >= -26 && e[2] <= 22 && e[1] >= -24 && e[3] <= 14, "inside PocketBuilder.bounds: " + java.util.Arrays.toString(e));
        assertTrue(p.maxY() <= 16);
    }
}
