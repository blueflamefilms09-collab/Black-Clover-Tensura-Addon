package com.newuniverse.nusmp.labyrinth.mega.puzzle;

import com.newuniverse.nusmp.labyrinth.mega.mechanics.Scaling;
import com.newuniverse.nusmp.labyrinth.mega.mechanics.Seeds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

/**
 * Why: this is "the room generator chooses among the old and the new puzzles by theme and depth": a pure, seeded function from a floor (its rooms, theme, road and depth) and the weights file to
 * the list of rooms that get one of the NEW puzzles and which. The same sector seed always gives the same plan, so it needs no saved state and the tests can replay it. For each room that could
 * hold a puzzle (big enough, of a kind the file gives a chance, not already busy with a library mechanic that seals or sets a puzzle, not a Spire sigil room whose own puzzle is built into its
 * blocks) it rolls the kind's chance, then draws among the new puzzles AND a "legacy" entry whose weight is the share the old puzzles keep; drawing the legacy entry leaves the room alone, so
 * the old content (the library's puzzle mechanics) is exactly as frequent as the file says. Puzzles are drawn by {@link PuzzleWeights#weightOf}: tier, road, theme flavour, room kind and a
 * decay for puzzles already on the floor so a floor does not repeat itself. A room whose template carries a PUZZLE marker always gets a puzzle (the marker may even name it). Pure.
 */
public final class PuzzlePlanner {
    private PuzzlePlanner() {}

    /**
     * One room as the planner sees it. {@code kind} is the folder name of its room kind, {@code template} its template id, {@code span} the smaller side of its box in blocks,
     * {@code eligible} false for safe rooms, arenas and the like, {@code busy} true when the mechanics library planned a sealing or puzzle mechanic there, {@code marker} true when the template
     * carries a PUZZLE marker and {@code forced} the puzzle id that marker names (empty for any).
     */
    public record RoomInfo(int id, String kind, String template, int span, boolean eligible, boolean busy, boolean marker, String forced) {}

    public record FloorInfo(int floor, boolean abyssal, String theme, long sectorSeed, boolean bossFloor, List<RoomInfo> rooms) {}

    /** A planned puzzle: the room, the puzzle id and the seed for its board. */
    public record Slot(int roomId, String type, long seed) {}

    public static List<Slot> plan(FloorInfo f, PuzzleWeights w) {
        List<Slot> out = new ArrayList<>();
        if (!w.enabled || w.maxPerFloor <= 0) return out;
        int tier = Scaling.tierOf(f.floor());
        long fs = Seeds.mix(Seeds.floorSeed(f.sectorSeed(), f.floor()), 0x50555A5AL);
        SplittableRandom rng = Seeds.rng(fs);
        List<RoomInfo> pool = new ArrayList<>();
        for (RoomInfo r : f.rooms()) if (r.eligible() && !r.busy()) pool.add(r);
        pool.sort(java.util.Comparator.comparingInt(RoomInfo::id));
        for (int i = pool.size() - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            RoomInfo t = pool.get(i);
            pool.set(i, pool.get(j));
            pool.set(j, t);
        }
        Map<String, Integer> sameUsed = new HashMap<>(), familyUsed = new HashMap<>();
        int cap = Math.max(0, w.maxPerFloor - (f.bossFloor() ? 1 : 0));
        int slotNo = 0;
        for (RoomInfo r : pool) {
            if (out.size() >= cap && !r.marker()) break;
            boolean apparatus = r.kind().equals("puzzle") && !r.marker() && !w.isOpenRoom(r.template());
            if (apparatus) continue;                                                      // a Spire sigil room: its own puzzle is built into its blocks
            if (r.span() < w.minSpan && !r.marker()) continue;
            double chance = r.marker() ? 1.0 : w.roomChance.getOrDefault(r.kind(), 0.0);
            if (chance <= 0) continue;
            double roll = rng.nextDouble();
            long seed = Seeds.slotSeed(fs, r.id(), slotNo++);
            if (roll >= chance) continue;
            String type = r.forced().isEmpty() || !PuzzleCatalog.has(r.forced()) ? draw(w, rng, tier, f, r, sameUsed, familyUsed) : r.forced();
            if (type == null) continue;                                                   // the old puzzles won the draw: leave the room to them
            out.add(new Slot(r.id(), type, seed));
            sameUsed.merge(type, 1, Integer::sum);
            familyUsed.merge(PuzzleCatalog.family(type), 1, Integer::sum);
        }
        return out;
    }

    /** One weighted draw among the new puzzles and the legacy entry; null when the legacy entry wins or nothing fits. */
    static String draw(PuzzleWeights w, SplittableRandom rng, int tier, FloorInfo f, RoomInfo r, Map<String, Integer> sameUsed, Map<String, Integer> familyUsed) {
        List<String> ids = new ArrayList<>();
        List<Double> ws = new ArrayList<>();
        double total = 0;
        for (String id : PuzzleCatalog.ids()) {
            double x = w.weightOf(w.types.get(id), tier, f.abyssal(), f.theme(), r.kind(), sameUsed.getOrDefault(id, 0), familyUsed.getOrDefault(PuzzleCatalog.family(id), 0));
            if (x <= 0) continue;
            ids.add(id);
            ws.add(x);
            total += x;
        }
        if (total <= 0) return null;
        double legacy = r.marker() ? 0.0 : Math.max(0.0, w.legacy.getOrDefault(r.kind(), 0.0));
        double pick = rng.nextDouble() * (total + legacy);
        if (pick >= total) return null;
        for (int i = 0; i < ids.size(); i++) {
            pick -= ws.get(i);
            if (pick < 0) return ids.get(i);
        }
        return ids.get(ids.size() - 1);
    }
}
