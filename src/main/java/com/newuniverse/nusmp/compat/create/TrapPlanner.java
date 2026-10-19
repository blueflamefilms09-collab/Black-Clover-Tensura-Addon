package com.newuniverse.nusmp.compat.create;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;

/**
 * Why: decides, for one sector, WHICH rooms get a Create trap and of which family, from nothing but the sector's own description. Pure and seeded, so the same sector always
 * plans the same traps (a repair or a re-generation re-arms the same ones) and a test can pin every rule: only the room kinds of {@link TrapRules#eligible}; never a Safe
 * Zone, the Landing, a stairwell or a room that opens straight onto one (the arrival is always clean); never two traps in rooms that open onto each other (a trap is
 * followed by a quiet stretch); no more than {@link TrapRules#maxPerSector}; the family from the theme, depth, threat and room kind. Where in the room the trap goes is a
 * question for the world ({@link LaneFinder}); a plan whose room has no choke is simply dropped when it is armed.
 */
public final class TrapPlanner {
    private TrapPlanner() {}

    /** A room as the planner sees it: its id, the folder name of its kind, whether it is a safe room, and the depth (absolute floor number, at least 1). */
    public record RoomInfo(int id, String folder, boolean safe, int depth) {}

    /** One planned trap. */
    public record Plan(int roomId, TrapFamily family, TriggerKind trigger, int core, long seed) {}

    /**
     * The plans of a sector. {@code edges} joins room ids that open onto each other. {@code density} is the server's multiplier (0 = none). {@code allowed} is the set of
     * families the server has switched on.
     */
    public static List<Plan> plan(List<RoomInfo> rooms, List<int[]> edges, String themeId, int threat, long sectorSeed, double density, Set<TrapFamily> allowed) {
        List<Plan> out = new ArrayList<>();
        if (rooms == null || rooms.isEmpty() || density <= 0.0 || allowed == null || allowed.isEmpty()) return out;
        Map<Integer, Set<Integer>> next = new HashMap<>();
        if (edges != null) {
            for (int[] e : edges) {
                if (e == null || e.length < 2) continue;
                next.computeIfAbsent(e[0], k -> new HashSet<>()).add(e[1]);
                next.computeIfAbsent(e[1], k -> new HashSet<>()).add(e[0]);
            }
        }
        Set<Integer> safeIds = new HashSet<>();
        for (RoomInfo r : rooms) if (r.safe()) safeIds.add(r.id());
        Set<Integer> nearSafe = new HashSet<>(safeIds);
        for (int s : safeIds) nearSafe.addAll(next.getOrDefault(s, Set.of()));

        List<RoomInfo> candidates = new ArrayList<>();
        for (RoomInfo r : rooms) if (TrapRules.eligible(r.folder()) && !nearSafe.contains(r.id())) candidates.add(r);
        candidates.sort(Comparator.comparingInt(RoomInfo::id));

        int cap = TrapRules.maxPerSector(rooms.size(), density);
        SplittableRandom rng = new SplittableRandom(sectorSeed ^ 0x43524541544541L);
        // a seeded order, so a sector that has more candidates than room for traps does not always favour the low room numbers
        for (int i = candidates.size() - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            RoomInfo t = candidates.get(i);
            candidates.set(i, candidates.get(j));
            candidates.set(j, t);
        }
        Set<Integer> taken = new HashSet<>();
        for (RoomInfo r : candidates) {
            if (out.size() >= cap) break;
            // draw every number for this room up front, so whether an earlier room was chosen never shifts a later room's numbers
            double roll = rng.nextDouble(), famRoll = rng.nextDouble(), trigRoll = rng.nextDouble(), coreRoll = rng.nextDouble();
            long seed = rng.nextLong();
            if (roll >= TrapRules.chance(r.folder(), r.depth(), density)) continue;
            boolean clash = false;
            for (int n : next.getOrDefault(r.id(), Set.of())) if (taken.contains(n)) { clash = true; break; }
            if (clash) continue;
            TrapFamily f = TrapRules.pickFamily(themeId, r.depth(), threat, r.folder(), famRoll, allowed);
            if (f == null) continue;
            out.add(new Plan(r.id(), f, TrapRules.pickTrigger(trigRoll), TrapRules.pickCore(coreRoll), seed));
            taken.add(r.id());
        }
        out.sort(Comparator.comparingInt(Plan::roomId));
        return out;
    }
}
