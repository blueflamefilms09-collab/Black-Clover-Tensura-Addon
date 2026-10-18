package com.newuniverse.nusmp.labyrinth.mega.foundry;

import com.newuniverse.nusmp.labyrinth.mega.Polarity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.ToDoubleFunction;

/**
 * Why: the immutable snapshot of every foundry mob the data packs define, with the one question the Dungeon Master asks of it: "give me a mob for THIS road, THIS tier, and if
 * you can one that belongs to THIS faction in THIS kind of region". The answer is a weighted draw: every mob of the road and tier weighs one; the faction the room (or the table
 * entry) asks for makes it four times as likely; a per-faction preference for the region's theme (the eco package's {@code themeWeights}) scales it, and a theme weight of zero keeps
 * the faction out entirely unless nothing else is left. The tier is the FLOOR's tier, but a roster with nothing at that tier falls back to the nearest tier below it, so a data pack
 * with a hole never leaves a floor without foundry mobs. Pure: the theme preference is passed in as a function. Immutable.
 */
public final class FoundryRoster {
    public static final FoundryRoster EMPTY = new FoundryRoster(List.of());
    /** How much likelier a mob of the asked-for faction is. */
    public static final double FACTION_BONUS = 4.0;

    private final Map<String, FoundryDef> byId;
    private final Map<Polarity, Map<Integer, List<FoundryDef>>> byRoadTier = new EnumMap<>(Polarity.class);

    public FoundryRoster(List<FoundryDef> defs) {
        Map<String, FoundryDef> m = new LinkedHashMap<>();
        for (FoundryDef d : defs) m.putIfAbsent(d.id(), d);
        byId = Collections.unmodifiableMap(m);
        for (Polarity p : Polarity.values()) {
            Map<Integer, List<FoundryDef>> tiers = new java.util.TreeMap<>();
            for (FoundryDef d : m.values()) if (d.road() == p) tiers.computeIfAbsent(d.tier(), k -> new ArrayList<>()).add(d);
            byRoadTier.put(p, tiers);
        }
    }

    /** Does a spawn become a foundry one? {@code weight} is a percent, 0..100 (below 0 counts as 0). */
    public static boolean chance(int weight, Random rnd) { return weight > 0 && rnd.nextInt(100) < weight; }

    public int size() { return byId.size(); }

    public FoundryDef byId(String id) { return id == null ? null : byId.get(id); }

    public List<FoundryDef> all() { return new ArrayList<>(byId.values()); }

    /** The mobs of a road and tier, in file order (empty when there are none). */
    public List<FoundryDef> of(Polarity road, int tier) {
        Map<Integer, List<FoundryDef>> t = byRoadTier.get(road);
        List<FoundryDef> l = t == null ? null : t.get(tier);
        return l == null ? List.of() : Collections.unmodifiableList(l);
    }

    public int count(Polarity road) {
        int n = 0;
        for (List<FoundryDef> l : byRoadTier.get(road).values()) n += l.size();
        return n;
    }

    /**
     * The mobs a draw for this road and tier chooses among: the tier's own, else the nearest tier below with any, else the nearest above (so a hole in the data never leaves a
     * floor empty).
     */
    public List<FoundryDef> pool(Polarity road, int tier) {
        int t = FoundryScaling.clampTier(tier);
        for (int k = t; k >= 1; k--) if (!of(road, k).isEmpty()) return of(road, k);
        for (int k = t + 1; k <= FoundryScaling.TIERS; k++) if (!of(road, k).isEmpty()) return of(road, k);
        return List.of();
    }

    /**
     * One mob for a floor of this road and tier, or null when the road has none. {@code wantFaction} is the faction the room or the table entry asks for (null = no wish);
     * {@code themeWeight} gives the region theme's liking for a faction (null = neutral, 1 for all).
     */
    public FoundryDef pick(Polarity road, int tier, String wantFaction, ToDoubleFunction<String> themeWeight, Random rnd) {
        List<FoundryDef> pool = pool(road, tier);
        if (pool.isEmpty()) return null;
        double[] w = new double[pool.size()];
        double total = 0.0;
        for (int i = 0; i < w.length; i++) {
            FoundryDef d = pool.get(i);
            double x = 1.0;
            if (wantFaction != null && wantFaction.equals(d.faction())) x *= FACTION_BONUS;
            if (themeWeight != null) x *= Math.max(0.0, themeWeight.applyAsDouble(d.faction()));
            w[i] = x;
            total += x;
        }
        if (!(total > 0.0)) {                                 // every faction of the pool is shut out by the theme: ignore the theme rather than spawn nothing
            for (int i = 0; i < w.length; i++) w[i] = 1.0;
            total = w.length;
        }
        double roll = rnd.nextDouble() * total;
        for (int i = 0; i < w.length; i++) {
            roll -= w[i];
            if (roll < 0.0) return pool.get(i);
        }
        return pool.get(pool.size() - 1);
    }

    /** The mobs of this road, tier below or equal, of a faction: what a summoner may call (the tier is lowered by the caller). */
    public List<FoundryDef> minions(Polarity road, int maxTier, String faction) {
        List<FoundryDef> out = new ArrayList<>();
        for (int t = 1; t <= Math.max(1, maxTier); t++) for (FoundryDef d : of(road, t)) if (faction == null || faction.equals(d.faction())) out.add(d);
        return out;
    }
}
