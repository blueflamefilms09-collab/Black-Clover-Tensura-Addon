package com.newuniverse.nusmp.labyrinth.mega.eco;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Why: "who fights whom" and "who is at home where" are the two questions every part of the faction system asks (the turf-war goal, the
 * target-change guard, the room assignment, the command), and they must have ONE answer built from the data files in ONE place. The graph
 * takes the loaded {@link FactionDef}s and settles the relations once: rivalry is made symmetric (if the goblins list the ogres, the ogres
 * hunt goblins too, unless the goblin file says {@code oneWay}), alliance is always symmetric, and an alliance beats a rivalry when a pair
 * is listed as both, so "the cult and the demons it summons" can never fight by a data slip. Ids are kept in sorted order so a seeded pick
 * walks the factions in the same order on every machine whatever order the files were read in. A reference to a faction that does not exist is
 * dropped and reported through {@link #problems()}, never thrown. Immutable once built. Pure (no Minecraft types).
 */
public final class FactionGraph {
    public static final FactionGraph EMPTY = new FactionGraph(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyList());

    private final Map<String, FactionDef> defs;
    private final Map<String, Set<String>> hunts;
    private final Map<String, Set<String>> allies;
    private final List<String> problems;

    private FactionGraph(Map<String, FactionDef> defs, Map<String, Set<String>> hunts, Map<String, Set<String>> allies, List<String> problems) {
        this.defs = defs;
        this.hunts = hunts;
        this.allies = allies;
        this.problems = problems;
    }

    /** Builds the graph. A second file with the same id replaces the first (the caller decides which file wins by the order it passes them). */
    public static FactionGraph of(Collection<FactionDef> all) {
        Map<String, FactionDef> defs = new TreeMap<>();
        for (FactionDef d : all) defs.put(d.id(), d);
        List<String> problems = new ArrayList<>();
        Map<String, Set<String>> hunts = new HashMap<>(), allies = new HashMap<>();
        for (String id : defs.keySet()) {
            hunts.put(id, new HashSet<>());
            allies.put(id, new HashSet<>());
        }
        for (FactionDef d : defs.values()) {
            for (String a : d.allies()) {
                if (!defs.containsKey(a)) {
                    problems.add(d.id() + ": ally '" + a + "' is not a faction");
                    continue;
                }
                allies.get(d.id()).add(a);
                allies.get(a).add(d.id());
            }
        }
        for (FactionDef d : defs.values()) {
            for (String r : d.rivals()) {
                if (!defs.containsKey(r)) {
                    problems.add(d.id() + ": rival '" + r + "' is not a faction");
                    continue;
                }
                if (allies.get(d.id()).contains(r)) {
                    problems.add(d.id() + " and " + r + " are both allies and rivals; the alliance wins");
                    continue;
                }
                hunts.get(d.id()).add(r);
                if (!d.oneWay()) hunts.get(r).add(d.id());
            }
        }
        return new FactionGraph(Collections.unmodifiableMap(defs), hunts, allies, Collections.unmodifiableList(problems));
    }

    public int size() { return defs.size(); }

    /** Every faction id, sorted. */
    public List<String> ids() { return new ArrayList<>(defs.keySet()); }

    public boolean knows(String id) { return id != null && defs.containsKey(id); }

    /** The definition, or null for an id that is not a faction. */
    public FactionDef def(String id) { return id == null ? null : defs.get(id); }

    /** What looked wrong in the data (unknown rivals and allies, an alliance that contradicts a rivalry). */
    public List<String> problems() { return problems; }

    /** The factions whose members {@code id}'s members hunt anywhere (never contains an ally or the faction itself). */
    public Set<String> rivalsOf(String id) { return Collections.unmodifiableSet(hunts.getOrDefault(id, Collections.emptySet())); }

    public Set<String> alliesOf(String id) { return Collections.unmodifiableSet(allies.getOrDefault(id, Collections.emptySet())); }

    /** Two different factions that never fight each other by agreement. */
    public boolean allied(String a, String b) {
        return a != null && !a.equals(b) && allies.getOrDefault(a, Collections.emptySet()).contains(b);
    }

    /** The same faction or an ally: a member of {@code a} has no business attacking one of {@code b}. */
    public boolean sameOrAllied(String a, String b) {
        return a != null && (a.equals(b) || allied(a, b));
    }

    /** Do members of {@code a} hunt members of {@code b} wherever they meet? (Directional: see {@code oneWay}.) */
    public boolean hostile(String a, String b) {
        return a != null && !a.equals(b) && hunts.getOrDefault(a, Collections.emptySet()).contains(b);
    }

    /** How much faction {@code id} likes a region of this theme: the file's weight, 1 when the file does not mention the theme, 0 for an unknown faction. */
    public double themeWeight(String id, String themeId) {
        FactionDef d = def(id);
        return d == null ? 0.0 : d.themeWeights().getOrDefault(themeId == null ? "" : themeId, 1.0);
    }

    /**
     * The native faction of something placed in a region of this theme, drawn from {@code seed}: a weighted pick over the factions in sorted-id
     * order, so the same seed gives the same answer on every machine. A faction with weight 0 never wins; if every faction has weight 0 for the
     * theme (a broken data set) the draw is uniform instead of failing. Null when there are no factions at all.
     */
    public String pickNative(long seed, String themeId) {
        if (defs.isEmpty()) return null;
        double total = 0.0;
        for (String id : defs.keySet()) total += themeWeight(id, themeId);
        double u = Mix.unit(seed);
        if (total <= 0.0) {
            int index = (int) Math.min(defs.size() - 1, Math.floor(u * defs.size()));
            return new ArrayList<>(defs.keySet()).get(index);
        }
        double r = u * total;
        String last = null;
        for (String id : defs.keySet()) {
            double w = themeWeight(id, themeId);
            if (w <= 0.0) continue;
            last = id;
            r -= w;
            if (r < 0.0) return id;
        }
        return last;
    }
}
