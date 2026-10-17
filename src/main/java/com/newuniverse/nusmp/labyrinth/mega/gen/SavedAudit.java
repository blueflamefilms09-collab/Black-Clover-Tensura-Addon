package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.MarkerKind;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: this is the audit of what the game has SAVED, as opposed to {@link SectorAudit}, which judges a plan before it is placed. Every system of the
 * Mega-Dungeon reads the saved {@code Sector}: the Guardian looks for its spawn cell, the Gatekeeper door for its marker, the Crucible for its pedestals, the
 * expander for the Way Onward, the director for the room graph. A sector that lacks one of those, or whose landmarks lie in the wrong rooms, would fail in
 * game in a way that is hard to trace, so {@code /multiverse mega audit <id>} (and the CI smoke test that runs it) checks the promises the other systems rely
 * on: the boss chain exists once each, the rooms stay inside the region and do not overlap, the markers sit in the rooms they belong to, the pedestals
 * match the relic count, the Way Onward's seal is inside an onward corridor, and every room can be walked to from the Landing. The sentences never contain a
 * semicolon, because the command joins them with one.
 *
 * <p>1.58.0, the floor model: a floor has two stairwells instead of a Landing (the arrival one holds the arrival cell and the exit that leads back, the forward
 * one the exit that leads on and the cell where a party stands when it comes back), an ordinary floor has no boss chain and no seal, a boss floor has both, and every
 * room must be walkable from the arrival stairwell.
 */
public final class SavedAudit {
    private SavedAudit() {}

    /** At most this many sentences of the same complaint are listed (a broken sector would otherwise print hundreds of overlaps). */
    private static final int PER_KIND = 4;

    /**
     * The failures of a saved sector; empty means it passes.
     *
     * @param knownIds       the ids of all saved sectors (to check {@code nextId}); null skips that check
     * @param requireMarkers whether the Sector Recipe demands the boss-chain markers (then a missing one is a failure)
     */
    public static List<String> audit(SectorView v, Set<Integer> knownIds, boolean requireMarkers) {
        List<String> bad = new ArrayList<>();
        if ("GENERATING".equals(v.state())) {
            bad.add("the sector is still being generated");
            return bad;
        }
        if ("FAILED".equals(v.state())) {
            bad.add("the sector failed to generate");
            return bad;
        }
        if (v.rooms().isEmpty()) {
            bad.add("the sector has no rooms");
            return bad;
        }
        rooms(v, bad);
        chain(v, bad, requireMarkers);
        graph(v, bad);
        if (v.nextId() >= 0 && knownIds != null && !knownIds.contains(v.nextId())) bad.add("the sector leads to sector " + v.nextId() + " which does not exist");
        return bad;
    }

    // ------------------------------------------------------------------------------------------------ rooms

    private static void rooms(SectorView v, List<String> bad) {
        Set<Integer> seen = new HashSet<>();
        int shown = 0;
        for (SectorFill.RoomOut r : v.rooms()) {
            if (!seen.add(r.id()) && shown++ < PER_KIND) bad.add("room id " + r.id() + " is used twice");
        }
        shown = 0;
        for (SectorFill.RoomOut r : v.rooms()) {
            if (r.kind() == RoomKind.LINK) continue;                              // the tunnel into the Landing lies between regions
            if (!r.box().inside(v.region(), 0) && shown++ < PER_KIND) bad.add("room " + r.id() + " (" + r.template() + ") leaves the region");
        }
        shown = 0;
        List<SectorFill.RoomOut> rs = v.rooms();
        for (int i = 0; i < rs.size(); i++) {
            for (int j = i + 1; j < rs.size(); j++) {
                if (rs.get(i).box().overlaps(rs.get(j).box()) && shown++ < PER_KIND) bad.add("rooms " + rs.get(i).id() + " and " + rs.get(j).id() + " overlap");
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ the boss chain

    private static List<SectorFill.RoomOut> of(SectorView v, RoomKind kind) {
        List<SectorFill.RoomOut> out = new ArrayList<>();
        for (SectorFill.RoomOut r : v.rooms()) if (r.kind() == kind) out.add(r);
        return out;
    }

    private static boolean inAny(List<SectorFill.RoomOut> rooms, Vec cell, int margin) {
        for (SectorFill.RoomOut r : rooms) if (r.box().inflateXZ(margin).contains(cell)) return true;
        return false;
    }

    private static void chain(SectorView v, List<String> bad, boolean requireMarkers) {
        final boolean floor = v.floorModel();
        final boolean boss = !floor || v.bossFloor();
        List<SectorFill.RoomOut> landing = of(v, floor ? RoomKind.STAIRWELL : RoomKind.LANDING), arena = of(v, RoomKind.ARENA), ante = of(v, RoomKind.ANTECHAMBER), vault = of(v, RoomKind.VAULT),
                onward = of(v, RoomKind.ONWARD);
        if (floor) {
            if (landing.size() != 2) bad.add("expected exactly two stairwells (arrival and forward) but the sector has " + landing.size());
        } else once(bad, "Landing", landing.size());
        if (boss) {
            once(bad, "arena", arena.size());
            once(bad, "antechamber", ante.size());
            once(bad, "vault", vault.size());
            if (onward.isEmpty()) bad.add("the sector has no Way Onward corridor");
        } else if (!arena.isEmpty() || !ante.isEmpty() || !vault.isEmpty() || !onward.isEmpty()) {
            bad.add("an ordinary floor has a boss chain (" + arena.size() + " arena, " + ante.size() + " antechamber, " + vault.size() + " vault, " + onward.size() + " Way Onward)");
        }

        Map<MarkerKind, Integer> count = new HashMap<>();
        for (SectorView.MarkerView m : v.markers()) count.merge(m.kind(), 1, Integer::sum);
        if (requireMarkers) {
            if (count.getOrDefault(MarkerKind.ARRIVAL, 0) < 1) bad.add("no arrival marker was saved");
            if (boss) {
                for (MarkerKind k : new MarkerKind[] {MarkerKind.GATEKEEPER, MarkerKind.VAULT_CENTER, MarkerKind.EXPANSION_SOCKET}) {
                    if (count.getOrDefault(k, 0) < 1) bad.add("no " + k.id() + " marker was saved");
                }
                int g = count.getOrDefault(MarkerKind.GUARDIAN_SPAWN, 0);
                if (g != 1) bad.add(g == 0 ? "no guardian_spawn marker was saved" : g + " guardian_spawn markers were saved (exactly one is needed)");
            }
            if (floor && count.getOrDefault(MarkerKind.STAIR_EXIT, 0) != 2) bad.add(count.getOrDefault(MarkerKind.STAIR_EXIT, 0) + " stair_exit markers were saved (the two stairwells need one each)");
        }

        if (v.arrival() == null) bad.add("the sector has no arrival cell");
        else if (!landing.isEmpty() && !inAny(landing, v.arrival(), 0)) bad.add("the arrival cell " + v.arrival() + " is not inside the " + (floor ? "arrival stairwell" : "Landing"));
        if (floor) stairs(v, landing, bad);
        if (!boss) return;

        if (v.guardianSpawn() == null) {
            if (requireMarkers) bad.add("the sector has no guardian spawn cell");
        } else if (!arena.isEmpty() && !inAny(arena, v.guardianSpawn(), 0)) bad.add("the guardian spawn " + v.guardianSpawn() + " is not inside the arena");
        if (v.gatekeeper() == null) {
            if (requireMarkers) bad.add("the sector has no gatekeeper cell");
        } else {
            List<SectorFill.RoomOut> both = new ArrayList<>(arena);
            both.addAll(ante);
            if (!both.isEmpty() && !inAny(both, v.gatekeeper(), 1)) bad.add("the gatekeeper cell " + v.gatekeeper() + " is not at the arena or antechamber");
        }
        if (v.vaultCenter() == null) {
            if (requireMarkers) bad.add("the sector has no vault centre");
        } else if (!vault.isEmpty() && !inAny(vault, v.vaultCenter(), 0)) bad.add("the vault centre " + v.vaultCenter() + " is not inside the vault");

        if (v.pedestals().size() != v.relicCount()) bad.add("the sector offers " + v.relicCount() + " relics but " + v.pedestals().size() + " pedestals were saved");
        int outside = 0;
        for (Vec p : v.pedestals()) if (!vault.isEmpty() && !inAny(vault, p, 0)) outside++;
        if (outside > 0) bad.add(outside + " pedestals are not inside the vault");

        if (v.seal() == null || v.sealFacing() == null) {
            bad.add("the sector has no Way Onward seal");
        } else if (!onward.isEmpty() && !inAny(onward, v.seal(), 0)) {
            bad.add("the Way Onward seal " + v.seal() + " is not inside an onward corridor");
        }
    }

    /**
     * The saved stairs of a floor: the exit that leads back is inside the arrival stairwell (the one that holds the arrival cell), the exit that leads on and the
     * cell where a returning party stands are inside the other one, and the two exits are not the same cell.
     */
    private static void stairs(SectorView v, List<SectorFill.RoomOut> stairwells, List<String> bad) {
        if (v.stairBack() == null) bad.add("the sector has no stair that leads back");
        if (v.stairForward() == null) bad.add("the sector has no stair that leads on");
        if (v.stairForwardPad() == null) bad.add("the sector has no cell for a party that comes back down to it");
        if (stairwells.size() != 2 || v.arrival() == null) return;
        SectorFill.RoomOut arrivalRoom = inAny(List.of(stairwells.get(0)), v.arrival(), 0) ? stairwells.get(0) : stairwells.get(1);
        SectorFill.RoomOut forwardRoom = arrivalRoom == stairwells.get(0) ? stairwells.get(1) : stairwells.get(0);
        if (v.stairBack() != null && !inAny(List.of(arrivalRoom), v.stairBack(), 0)) bad.add("the stair that leads back " + v.stairBack() + " is not inside the arrival stairwell");
        if (v.stairForward() != null && !inAny(List.of(forwardRoom), v.stairForward(), 0)) bad.add("the stair that leads on " + v.stairForward() + " is not inside the forward stairwell");
        if (v.stairForwardPad() != null && !inAny(List.of(forwardRoom), v.stairForwardPad(), 0)) bad.add("the cell for a returning party " + v.stairForwardPad() + " is not inside the forward stairwell");
        if (v.stairBack() != null && v.stairBack().equals(v.stairForward())) bad.add("both stairs end in the same cell");
    }

    private static void once(List<String> bad, String what, int n) {
        if (n != 1) bad.add("expected exactly one " + what + " room but the sector has " + n);
    }

    // ------------------------------------------------------------------------------------------------ the room graph

    private static void graph(SectorView v, List<String> bad) {
        Map<Integer, List<Integer>> adj = new HashMap<>();
        for (SectorFill.RoomOut r : v.rooms()) adj.put(r.id(), new ArrayList<>());
        int shown = 0;
        for (int[] e : v.edges()) {
            List<Integer> a = adj.get(e[0]), b = adj.get(e[1]);
            if (a == null || b == null) {
                if (shown++ < PER_KIND) bad.add("a doorway joins rooms " + e[0] + " and " + e[1] + " but one of them does not exist");
                continue;
            }
            a.add(e[1]);
            b.add(e[0]);
        }
        List<SectorFill.RoomOut> landing = of(v, RoomKind.LANDING);
        if (v.floorModel()) {                                                      // the start is the stairwell that holds the arrival cell
            landing = new ArrayList<>();
            for (SectorFill.RoomOut r : of(v, RoomKind.STAIRWELL)) if (v.arrival() != null && r.box().contains(v.arrival())) landing.add(r);
        }
        if (landing.size() != 1) return;                                           // already reported; there is no start to walk from
        Set<Integer> reached = new HashSet<>();
        ArrayDeque<Integer> todo = new ArrayDeque<>();
        todo.add(landing.get(0).id());
        reached.add(landing.get(0).id());
        while (!todo.isEmpty()) {
            for (int n : adj.get(todo.poll())) if (reached.add(n)) todo.add(n);
        }
        List<Integer> lost = new ArrayList<>();
        for (SectorFill.RoomOut r : v.rooms()) if (!reached.contains(r.id())) lost.add(r.id());
        if (!lost.isEmpty()) bad.add(lost.size() + " rooms cannot be reached from the Landing (first: room " + lost.get(0) + ")");
    }
}
