package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the planner is the Director's brain, and the brief names its rules: reproducible per seed, weights from tier / road / theme / budget, per-floor and per-room caps, no
 * conflicting mechanics, nothing in a room the engine marked ineligible (the safe rooms). They are pinned here on the shipped library, over many synthetic floors, without a game.
 */
class PlannerTest {
    private static final String[] KINDS = {"hall", "trap", "puzzle", "trial", "treasure", "cavern", "corridor", "junction", "set", "safe", "landing", "stairwell", "arena"};

    private static Planner.FloorInfo floor(int floor, long seed, boolean boss, String theme) {
        List<Planner.RoomInfo> rooms = new ArrayList<>();
        for (int r = 0; r < 26; r++) {
            String k = KINDS[r % KINDS.length];
            rooms.add(new Planner.RoomInfo(r, k, !(k.equals("safe") || k.equals("landing") || k.equals("stairwell") || k.equals("arena"))));
        }
        return new Planner.FloorInfo(floor, floor > 0 ? Polarity.HOLY : Polarity.ABYSSAL, theme, seed, boss, boss ? 12 : -1, rooms);
    }

    private static boolean same(List<Planner.Slot> a, List<Planner.Slot> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            if (a.get(i).roomId() != b.get(i).roomId() || !a.get(i).def().id().equals(b.get(i).def().id()) || a.get(i).seed() != b.get(i).seed()) return false;
        }
        return true;
    }

    @Test
    void thePlanIsAPureFunctionOfTheSeed() {
        for (int i = 0; i < 60; i++) {
            Planner.FloorInfo f = floor(1 + i * 13, 500L + i, i % 4 == 0, "gravity");
            assertTrue(same(Planner.plan(TestLibrary.defs(), TestLibrary.catalog(), f, Planner.Config.defaults()), Planner.plan(TestLibrary.defs(), TestLibrary.catalog(), f, Planner.Config.defaults())), "seed " + i);
        }
    }

    @Test
    void anotherSeedGivesAnotherPlanMostOfTheTime() {
        int differ = 0, n = 80;
        for (int i = 0; i < n; i++) {
            List<Planner.Slot> a = Planner.plan(TestLibrary.defs(), TestLibrary.catalog(), floor(40, 100L + i, false, "gravity"), Planner.Config.defaults());
            List<Planner.Slot> b = Planner.plan(TestLibrary.defs(), TestLibrary.catalog(), floor(40, 9100L + i, false, "gravity"), Planner.Config.defaults());
            if (!same(a, b)) differ++;
        }
        assertTrue(differ >= n * 0.7, "only " + differ + " of " + n);
    }

    @Test
    void capsConflictsAndSafeRoomsAreKept() {
        Planner.Config cfg = new Planner.Config(4, 1, 1.0, EnumSet.allOf(MechCategory.class), Set.of());
        for (int i = 0; i < 200; i++) {
            int fl = (i % 2 == 0 ? 1 : -1) * (1 + i * 5 % 1000);
            boolean boss = i % 5 == 0;
            Planner.FloorInfo f = floor(fl, 77L * i + 3, boss, i % 3 == 0 ? "necropolis" : "flame");
            List<Planner.Slot> plan = Planner.plan(TestLibrary.defs(), TestLibrary.catalog(), f, cfg);
            int normal = 0, bosses = 0;
            int[] perRoom = new int[26];
            for (Planner.Slot s : plan) {
                if (s.def().scope() == Scope.BOSS) bosses++; else normal++;
                if (s.roomId() >= 0) {
                    perRoom[s.roomId()]++;
                    if (s.def().scope() != Scope.BOSS) assertTrue(f.rooms().get(s.roomId()).eligible(), "a safe room got " + s.def().id());
                }
                assertTrue(s.def().fitsTier(Scaling.tierOf(fl)), s.def().id());
                assertTrue(s.def().road().allows(f.road()), s.def().id());
            }
            assertTrue(normal <= 4, "per-floor cap: " + normal);
            assertTrue(bosses <= 2 && (boss || bosses == 0), "boss mechanics: " + bosses);
            for (int r = 0; r < 26; r++) assertTrue(perRoom[r] <= (r == 12 ? 2 : 1), "room " + r + " has " + perRoom[r]);
            assertTrue(Planner.conflicts(plan, TestLibrary.catalog()).isEmpty(), String.valueOf(Planner.conflicts(plan, TestLibrary.catalog())));
        }
    }

    @Test
    void theBudgetCapsTheCostOfAFloor() {
        for (int i = 0; i < 100; i++) {
            int fl = 1 + i * 9;
            List<Planner.Slot> plan = Planner.plan(TestLibrary.defs(), TestLibrary.catalog(), floor(fl, 31L * i, false, "gravity"), Planner.Config.defaults());
            int cost = 0;
            for (Planner.Slot s : plan) cost += s.def().cost();
            assertTrue(cost <= Weights.floorBudget(Scaling.tierOf(fl)), "cost " + cost + " over the budget at floor " + fl);
        }
    }

    @Test
    void aSwitchedOffCategoryAndTheBlacklistAreHonoured() {
        Set<MechCategory> cats = EnumSet.allOf(MechCategory.class);
        cats.remove(MechCategory.FLOOR);
        Set<String> banned = new HashSet<>();
        for (int i = 0; i < 40; i++) {
            Planner.FloorInfo f = floor(10 + i, 5L * i, false, "gravity");
            for (Planner.Slot s : Planner.plan(TestLibrary.defs(), TestLibrary.catalog(), f, new Planner.Config(4, 1, 1.0, cats, banned))) {
                assertFalse(s.def().category() == MechCategory.FLOOR, "a switched-off category was drawn");
                banned.add(s.def().id());
            }
        }
        Planner.FloorInfo f = floor(10, 5L, false, "gravity");
        for (Planner.Slot s : Planner.plan(TestLibrary.defs(), TestLibrary.catalog(), f, new Planner.Config(4, 1, 1.0, cats, banned))) assertFalse(banned.contains(s.def().id()), "a blacklisted mechanic was drawn");
    }

    @Test
    void zeroPerFloorPlansNothing() {
        assertEquals(0, Planner.plan(TestLibrary.defs(), TestLibrary.catalog(), floor(5, 1, true, "gravity"), new Planner.Config(0, 1, 1.0, EnumSet.allOf(MechCategory.class), Set.of())).size());
    }

    @Test
    void aLikedThemeIsDrawnMoreOften() {
        MechanicDef liked = null;
        for (MechanicDef d : TestLibrary.defs()) if (!d.themes().isEmpty()) { liked = d; break; }
        if (liked == null) return;
        String theme = liked.themes().get(0);
        double with = Weights.weight(liked, 5, Polarity.HOLY, theme, 1.0), without = Weights.weight(liked, 5, Polarity.HOLY, "__none__", 1.0);
        if (liked.fitsTier(1) && liked.road().allows(Polarity.HOLY)) assertTrue(with > without, "theme affinity");
    }
}
