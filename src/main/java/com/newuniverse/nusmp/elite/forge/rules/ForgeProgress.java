package com.newuniverse.nusmp.elite.forge.rules;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: "Crafting Progress Storage" of the inventory. What a player has done at the forge: how many items they made in total and per station tier, how many of each
 * quality, the best quality, which recipes they have forged at least once (the station knowledge that opens the list), the highest station tier they have used,
 * their specialization and how many times they changed it. The class is pure Java and knows nothing of NBT: it converts to and from a plain {@code Map} of
 * numbers, strings and lists ({@link #toMap}, {@link #fromMap}) which a small Minecraft adapter ({@code ProgressTag}) turns into the attachment's CompoundTag.
 * Reading is TOLERANT: a missing key, a key of the wrong type, a negative number or an unknown quality count falls back to the default, so a save written by
 * an older or newer version loads. Writing leaves the empty progress as an empty map, so a player who never forged adds nothing to their save.
 */
public final class ForgeProgress {
    public static final int MAX_KNOWN = 512;

    private int crafted;
    private final int[] byTier = new int[StationTier.values().length];
    private final int[] byQuality = new int[Quality.values().length];
    private int best;
    private int topStation;
    private int specChanges;
    private String specialization = "";
    private final Set<String> known = new LinkedHashSet<>();

    public int crafted() { return crafted; }

    public int craftedAt(StationTier t) { return byTier[t.index()]; }

    public int craftedOf(Quality q) { return byQuality[q.level() - 1]; }

    /** 0 none, 1..6 the best quality ever forged. */
    public int best() { return best; }

    /** The highest station tier index used, 0 if none (Basic). */
    public int topStation() { return topStation; }

    public int specChanges() { return specChanges; }

    /** The specialization id or an empty string. */
    public String specializationId() { return specialization; }

    public Specialization specialization() { return Specialization.byId(specialization); }

    public Set<String> known() { return java.util.Collections.unmodifiableSet(known); }

    public boolean knows(String recipeId) { return known.contains(recipeId); }

    public boolean isEmpty() { return crafted == 0 && best == 0 && specialization.isEmpty() && specChanges == 0 && known.isEmpty() && topStation == 0; }

    /** Records one forged item. */
    public void recordForge(StationTier tier, String recipeId, Quality quality) {
        crafted++;
        byTier[tier.index()]++;
        byQuality[quality.level() - 1]++;
        best = Math.max(best, quality.level());
        topStation = Math.max(topStation, tier.index());
        if (recipeId != null && !recipeId.isEmpty() && known.size() < MAX_KNOWN) known.add(recipeId);
    }

    /**
     * Sets the specialization. The first choice is free ({@code specChanges} stays 0); a later change counts. Returns false when it is the same one or unknown.
     */
    public boolean setSpecialization(Specialization s) {
        if (s == null || s.id().equals(specialization)) return false;
        if (!specialization.isEmpty()) specChanges++;
        specialization = s.id();
        return true;
    }

    /** Removes the specialization (an operator's tool); false when there was none. The change counter is kept. */
    public boolean clearSpecialization() {
        if (specialization.isEmpty()) return false;
        specialization = "";
        return true;
    }

    /** True when choosing a specialization now would be a CHANGE (and so costs something). */
    public boolean changeCosts() { return !specialization.isEmpty(); }

    public ForgeProgress copy() { return fromMap(toMap()); }

    // ------------------------------------------------------------------ plain-map form
    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        if (isEmpty()) return m;
        m.put("crafted", crafted);
        m.put("by_tier", intList(byTier));
        m.put("by_quality", intList(byQuality));
        m.put("best", best);
        m.put("top_station", topStation);
        m.put("spec_changes", specChanges);
        m.put("spec", specialization);
        m.put("known", new ArrayList<>(known));
        return m;
    }

    private static List<Integer> intList(int[] a) {
        List<Integer> l = new ArrayList<>(a.length);
        for (int v : a) l.add(v);
        return l;
    }

    /** Reads the map form; tolerant of anything. Never returns null. */
    public static ForgeProgress fromMap(Map<String, ?> m) {
        ForgeProgress p = new ForgeProgress();
        if (m == null) return p;
        p.crafted = Math.max(0, intOf(m.get("crafted")));
        readInts(m.get("by_tier"), p.byTier);
        readInts(m.get("by_quality"), p.byQuality);
        p.best = Math.max(0, Math.min(Quality.values().length, intOf(m.get("best"))));
        p.topStation = Math.max(0, Math.min(StationTier.values().length - 1, intOf(m.get("top_station"))));
        p.specChanges = Math.max(0, intOf(m.get("spec_changes")));
        Object spec = m.get("spec");
        if (spec instanceof String s && Specialization.byId(s) != null) p.specialization = Specialization.byId(s).id();
        Object known = m.get("known");
        if (known instanceof List<?> l) {
            for (Object o : l) {
                if (p.known.size() >= MAX_KNOWN) break;
                if (o instanceof String s && !s.isEmpty() && s.length() <= 96) p.known.add(s);
            }
        }
        return p;
    }

    private static int intOf(Object o) { return o instanceof Number n ? n.intValue() : 0; }

    private static void readInts(Object o, int[] into) {
        if (!(o instanceof List<?> l)) return;
        for (int i = 0; i < into.length && i < l.size(); i++) into[i] = Math.max(0, intOf(l.get(i)));
    }
}
