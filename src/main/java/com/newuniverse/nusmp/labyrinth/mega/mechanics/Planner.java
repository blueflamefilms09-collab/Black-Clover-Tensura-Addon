package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import com.newuniverse.nusmp.labyrinth.mega.Polarity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;

/**
 * Why: the heart of "the dungeon picks mechanics by weights from the floor's tier, road, theme, threat budget and a per-floor seed (reproducible), respects per-floor and per-room
 * caps and never stacks conflicting mechanics". It is a PURE function of (library, floor description, config): the same sector seed and floor always give the same plan, so the
 * plan needs no saved state and can be tested without a game. The algorithm: roll how many floor-wide mechanics (0 to 2, more on deep floors), pick them; shuffle the eligible
 * rooms with the seed and give the first ones a room or zone mechanic until the per-floor cap or the threat budget is spent (a room may take more than one only when the per-room
 * cap allows); on a boss floor add up to two boss mechanics for the arena from a small budget of their own. Every pick is weighted ({@link Weights}), skips the blacklist and the
 * switched-off categories, and skips anything that conflicts ({@link Channels}) with a mechanic that shares its space (floor-wide mechanics share space with everything).
 */
public final class Planner {
    private Planner() {}

    /** One room of the floor as the planner sees it: its id, the folder name of its RoomKind, and whether a mechanic may go there (the engine says no for safe rooms, pockets ...). */
    public record RoomInfo(int id, String kind, boolean eligible) {}

    /** The floor to plan: signed floor, road, theme id, the sector's own seed, whether it has a boss, the arena room (-1 none) and the rooms. */
    public record FloorInfo(int floor, Polarity road, String theme, long sectorSeed, boolean bossFloor, int arenaRoomId, List<RoomInfo> rooms) {}

    /** The config as the planner needs it. */
    public record Config(int perFloor, int perRoom, double budgetMult, Set<MechCategory> categories, Set<String> blacklist) {
        public static Config defaults() { return new Config(4, 1, 1.0, Set.of(MechCategory.values()), Set.of()); }
    }

    /** A planned mechanic: the room it belongs to (-1 = the whole floor), the definition and the seed for its anchor, props and rolls. */
    public record Slot(int roomId, MechanicDef def, long seed) {}

    public static List<Slot> plan(List<MechanicDef> library, OpCatalog cat, FloorInfo f, Config c) {
        List<Slot> out = new ArrayList<>();
        if (library.isEmpty() || (c.perFloor() <= 0)) return out;
        int tier = Scaling.tierOf(f.floor());
        long fs = Seeds.floorSeed(f.sectorSeed(), f.floor());
        SplittableRandom rng = Seeds.rng(fs);
        double mult = Scaling.clampDifficulty(c.budgetMult());
        int budget = (int) Math.round(Weights.floorBudget(tier) * mult);
        int slotNo = 0;

        // 1. the floor-wide mechanics
        int nFloor = rng.nextInt(100) < 70 ? 1 : 0;
        if (nFloor == 1 && c.perFloor() >= 4 && tier >= 3 && rng.nextInt(100) < 25) nFloor = 2;
        nFloor = Math.min(nFloor, c.perFloor());
        for (int i = 0; i < nFloor; i++) {
            MechanicDef d = pick(library, cat, f, c, rng, Set.of(Scope.FLOOR), null, budget, all(out));
            if (d == null) break;
            out.add(new Slot(-1, d, Seeds.slotSeed(fs, -1, slotNo++)));
            budget -= d.cost();
        }

        // 2. rooms and zones
        List<RoomInfo> pool = new ArrayList<>();
        for (RoomInfo r : f.rooms()) if (r.eligible() && r.id() != f.arenaRoomId()) pool.add(r);
        for (int i = pool.size() - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            RoomInfo t = pool.get(i);
            pool.set(i, pool.get(j));
            pool.set(j, t);
        }
        int remaining = c.perFloor() - out.size();
        for (RoomInfo r : pool) {
            if (remaining <= 0 || budget <= 0) break;
            for (int k = 0; k < Math.max(1, c.perRoom()) && remaining > 0; k++) {
                if (k > 0 && rng.nextInt(100) >= 30) break;
                List<MechanicDef> overlap = new ArrayList<>(floorWide(out));
                overlap.addAll(inRoom(out, r.id()));
                MechanicDef d = pick(library, cat, f, c, rng, Set.of(Scope.ROOM, Scope.ZONE), r.kind(), budget, overlap);
                if (d == null) break;
                out.add(new Slot(r.id(), d, Seeds.slotSeed(fs, r.id(), slotNo++)));
                budget -= d.cost();
                remaining--;
            }
        }

        // 3. the boss arena
        if (f.bossFloor() && f.arenaRoomId() >= 0) {
            int bossBudget = (int) Math.round(Weights.bossBudget(tier) * mult);
            int n = 1 + (c.perRoom() >= 2 && tier >= 3 && rng.nextInt(100) < 40 ? 1 : 0);
            for (int i = 0; i < n; i++) {
                List<MechanicDef> overlap = new ArrayList<>(floorWide(out));
                overlap.addAll(inRoom(out, f.arenaRoomId()));
                MechanicDef d = pick(library, cat, f, c, rng, Set.of(Scope.BOSS), "arena", bossBudget, overlap);
                if (d == null) break;
                out.add(new Slot(f.arenaRoomId(), d, Seeds.slotSeed(fs, f.arenaRoomId(), slotNo++)));
                bossBudget -= d.cost();
            }
        }
        return out;
    }

    private static List<MechanicDef> all(List<Slot> s) {
        List<MechanicDef> out = new ArrayList<>();
        for (Slot x : s) out.add(x.def());
        return out;
    }

    private static List<MechanicDef> floorWide(List<Slot> s) {
        List<MechanicDef> out = new ArrayList<>();
        for (Slot x : s) if (x.roomId() < 0) out.add(x.def());
        return out;
    }

    private static List<MechanicDef> inRoom(List<Slot> s, int room) {
        List<MechanicDef> out = new ArrayList<>();
        for (Slot x : s) if (x.roomId() == room) out.add(x.def());
        return out;
    }

    /** One weighted pick among the library entries of the given scopes, or null when nothing fits. */
    static MechanicDef pick(List<MechanicDef> lib, OpCatalog cat, FloorInfo f, Config c, SplittableRandom rng, Set<Scope> scopes, String roomKind, int budget, List<MechanicDef> overlap) {
        List<MechanicDef> cand = new ArrayList<>();
        List<Double> ws = new ArrayList<>();
        double total = 0;
        for (MechanicDef d : lib) {
            if (!scopes.contains(d.scope()) || d.cost() > budget) continue;
            if (!c.categories().contains(d.category()) || c.blacklist().contains(d.id())) continue;
            double w = Weights.weight(d, f.floor(), f.road(), f.theme(), roomKind == null ? 1.0 : Weights.roomBias(roomKind, d.category()));
            if (w <= 0) continue;
            boolean clash = false;
            for (MechanicDef o : overlap) if (Channels.conflict(d, o, cat)) { clash = true; break; }
            if (clash) continue;
            cand.add(d);
            ws.add(w);
            total += w;
        }
        if (cand.isEmpty() || total <= 0) return null;
        double r = rng.nextDouble() * total;
        for (int i = 0; i < cand.size(); i++) {
            r -= ws.get(i);
            if (r < 0) return cand.get(i);
        }
        return cand.get(cand.size() - 1);
    }

    /** The mechanics of a plan that conflict with each other (should always be empty; the self-test and JUnit assert it). Floor-wide ones clash with everything, others only within a room. */
    public static List<String> conflicts(List<Slot> plan, OpCatalog cat) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < plan.size(); i++) {
            for (int j = i + 1; j < plan.size(); j++) {
                Slot a = plan.get(i), b = plan.get(j);
                boolean share = a.roomId() < 0 || b.roomId() < 0 || a.roomId() == b.roomId();
                if (share && Channels.conflict(a.def(), b.def(), cat)) out.add(a.def().id() + " x " + b.def().id());
            }
        }
        return out;
    }
}
