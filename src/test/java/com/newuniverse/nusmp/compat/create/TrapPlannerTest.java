package com.newuniverse.nusmp.compat.create;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the plan of a sector is what the dungeon promises its players: a clean arrival, a quiet stretch after every trap, and the same traps after a repair. */
class TrapPlannerTest {
    private static final Set<TrapFamily> ALL = EnumSet.allOf(TrapFamily.class);

    /** A ring of rooms with a Landing at 0, a Safe Zone at 7, and corridors, junctions, halls and a vault between. */
    private static List<TrapPlanner.RoomInfo> rooms(int n, int depth) {
        List<TrapPlanner.RoomInfo> out = new ArrayList<>();
        String[] kinds = {"landing", "corridor", "junction", "corridor", "hall", "corridor", "treasure", "safe", "corridor", "link", "trap", "corridor", "vault", "arena", "corridor", "cavern"};
        for (int i = 0; i < n; i++) {
            String k = kinds[i % kinds.length];
            out.add(new TrapPlanner.RoomInfo(i, k, k.equals("landing") || k.equals("safe") || k.equals("stairwell"), depth));
        }
        return out;
    }

    private static List<int[]> ring(int n) {
        List<int[]> e = new ArrayList<>();
        for (int i = 0; i < n; i++) e.add(new int[] {i, (i + 1) % n});
        return e;
    }

    @Test
    void theSamePlanComesBackForTheSameSeed() {
        List<TrapPlanner.Plan> a = TrapPlanner.plan(rooms(64, 20), ring(64), "storm", 5, 99L, 3.0, ALL);
        List<TrapPlanner.Plan> b = TrapPlanner.plan(rooms(64, 20), ring(64), "storm", 5, 99L, 3.0, ALL);
        assertEquals(a, b);
        assertFalse(a.isEmpty());
        assertFalse(a.equals(TrapPlanner.plan(rooms(64, 20), ring(64), "storm", 5, 100L, 3.0, ALL)));
    }

    @Test
    void neverInOrNextToASafeRoomNorInAnIneligibleOne() {
        for (long seed = 1; seed <= 60; seed++) {
            List<TrapPlanner.RoomInfo> rs = rooms(64, 30);
            Map<Integer, TrapPlanner.RoomInfo> byId = new HashMap<>();
            for (TrapPlanner.RoomInfo r : rs) byId.put(r.id(), r);
            Set<Integer> bad = new HashSet<>();
            for (TrapPlanner.RoomInfo r : rs) if (r.safe()) { bad.add(r.id()); bad.add((r.id() + 1) % 64); bad.add((r.id() + 63) % 64); }
            for (TrapPlanner.Plan p : TrapPlanner.plan(rs, ring(64), "gravity", 3, seed, 4.0, ALL)) {
                assertTrue(TrapRules.eligible(byId.get(p.roomId()).folder()), "room kind " + byId.get(p.roomId()).folder());
                assertFalse(bad.contains(p.roomId()), "room " + p.roomId() + " is a safe room or opens onto one");
            }
        }
    }

    @Test
    void neverTwoTrapsInRoomsThatOpenOntoEachOther() {
        for (long seed = 1; seed <= 60; seed++) {
            Set<Integer> chosen = new HashSet<>();
            for (TrapPlanner.Plan p : TrapPlanner.plan(rooms(64, 30), ring(64), "gravity", 3, seed, 4.0, ALL)) chosen.add(p.roomId());
            for (int id : chosen) {
                assertFalse(chosen.contains((id + 1) % 64), "traps in neighbouring rooms " + id);
                assertFalse(chosen.contains((id + 63) % 64), "traps in neighbouring rooms " + id);
            }
        }
    }

    @Test
    void theCapAndTheSwitchesAreHonoured() {
        for (long seed = 1; seed <= 40; seed++) {
            assertTrue(TrapPlanner.plan(rooms(64, 30), ring(64), "gravity", 3, seed, 1.0, ALL).size() <= TrapRules.maxPerSector(64, 1.0));
            assertTrue(TrapPlanner.plan(rooms(64, 30), ring(64), "gravity", 3, seed, 0.0, ALL).isEmpty());
            assertTrue(TrapPlanner.plan(rooms(64, 30), ring(64), "gravity", 3, seed, 3.0, EnumSet.noneOf(TrapFamily.class)).isEmpty());
            for (TrapPlanner.Plan p : TrapPlanner.plan(rooms(64, 30), ring(64), "gravity", 3, seed, 3.0, EnumSet.of(TrapFamily.SAW_RUN, TrapFamily.POUR)))
                assertTrue(p.family() == TrapFamily.SAW_RUN || p.family() == TrapFamily.POUR, "got " + p.family());
        }
        assertTrue(TrapPlanner.plan(null, null, "x", 1, 1L, 1.0, ALL).isEmpty());
        assertTrue(TrapPlanner.plan(List.of(), List.of(), "x", 1, 1L, 1.0, ALL).isEmpty());
    }

    @Test
    void theFloorGatesTheHeavyFamilies() {
        for (long seed = 1; seed <= 80; seed++) {
            for (TrapPlanner.Plan p : TrapPlanner.plan(rooms(64, 1), ring(64), "gravity", 1, seed, 4.0, ALL)) {
                assertTrue(p.family().minDepth() <= 1, "floor 1 got " + p.family());
                assertTrue(p.core() >= TrapRules.MIN_CORE && p.core() <= TrapRules.MAX_CORE);
            }
        }
    }

    @Test
    void anAverageSectorGetsSomeTrapsButNotAFlood() {
        int total = 0, sectors = 100;
        for (long seed = 1; seed <= sectors; seed++) total += TrapPlanner.plan(rooms(48, 25), ring(48), "gravity", 3, seed, 1.0, ALL).size();
        double avg = total / (double) sectors;
        assertTrue(avg >= 0.8 && avg <= 4.0, "average " + avg);
    }
}
