package com.newuniverse.nusmp.labyrinth.loot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: "loot tables by mob family, depth band and rarity" is a table, and a table built by hand for eleven families and five bands is eleven times five chances to forget one.
 * This builds it from the roster: every item lists the families it drops from, with a base chance and the first depth band it appears in, so the table of a family is just "the
 * rows that name it, plus the rows that name {@code any}". A row that names both a family and {@code any} counts once, at the better chance. The boss pools (the rare materials
 * of the family first, then every rare material; the gear of the family, then all gear; the strong tonics and elixirs) are built the same way. Pure.
 */
public final class LootTables {
    public enum Kind { MATERIAL, FOOD, GEAR }

    /** One row: the item, what it is, the base chance per kill, the first band it can drop in, the family it came from. */
    public record Entry(String id, Kind kind, double chance, int minBand, String family) {}

    private final Map<String, List<Entry>> byFamily = new LinkedHashMap<>();
    private final Map<String, Kind> kinds = new HashMap<>();
    private final List<String> bossFoods = new ArrayList<>(), bossMaterials = new ArrayList<>(), allGear = new ArrayList<>();
    private final LootRoster roster;

    private LootTables(LootRoster roster) { this.roster = roster; }

    public static LootTables of(LootRoster r) {
        LootTables t = new LootTables(r);
        for (LootRoster.FoodDef f : r.foods()) t.add(f.id(), Kind.FOOD, f.drops());
        for (LootRoster.MaterialDef m : r.materials()) t.add(m.id(), Kind.MATERIAL, m.drops());
        for (LootRoster.WeaponDef w : r.weapons()) {
            t.add(w.id(), Kind.GEAR, w.drops());
            t.allGear.add(w.id());
        }
        for (LootRoster.ArmourDef a : r.armour()) {
            t.add(a.id(), Kind.GEAR, a.drops());
            t.allGear.add(a.id());
        }
        for (LootRoster.TrinketDef x : r.trinkets()) {
            t.add(x.id(), Kind.GEAR, x.drops());
            t.allGear.add(x.id());
        }
        for (String id : r.bossFoods()) if (r.food(id) != null) t.bossFoods.add(id);
        for (String id : r.bossMaterials()) if (r.material(id) != null) t.bossMaterials.add(id);
        return t;
    }

    private void add(String id, Kind kind, List<LootRoster.DropRow> rows) {
        kinds.put(id, kind);
        for (LootRoster.DropRow d : rows) byFamily.computeIfAbsent(d.family(), k -> new ArrayList<>()).add(new Entry(id, kind, d.chance(), d.minBand(), d.family()));
    }

    /** The rows that can drop from a monster of this family: its own and the generic ones, one row per item (the better chance wins). */
    public List<Entry> entries(String family) {
        Map<String, Entry> out = new LinkedHashMap<>();
        for (Entry e : byFamily.getOrDefault(family == null ? LootFamilies.ANY : family, List.of())) out.merge(e.id(), e, LootTables::better);
        if (!LootFamilies.ANY.equals(family)) for (Entry e : byFamily.getOrDefault(LootFamilies.ANY, List.of())) out.merge(e.id(), e, LootTables::better);
        return new ArrayList<>(out.values());
    }

    private static Entry better(Entry a, Entry b) { return b.chance() > a.chance() ? b : a; }

    public Kind kindOf(String id) { return kinds.get(id); }

    public List<String> bossMaterials() { return bossMaterials; }

    public List<String> bossFoods() { return bossFoods; }

    public List<String> allGear() { return allGear; }

    /** The rare materials a boss of this family drops from: its own family's first, then every rare material. */
    public List<String> bossMaterialsFor(String family) {
        List<String> own = new ArrayList<>();
        for (String id : bossMaterials) {
            LootRoster.MaterialDef m = roster.material(id);
            if (m != null && m.family().equals(family)) own.add(id);
        }
        List<String> out = new ArrayList<>(own);
        for (String id : bossMaterials) if (!own.contains(id)) out.add(id);
        return out;
    }

    /** The gear that drops from this family (its rows), or all gear when it has none. */
    public List<String> gearFor(String family) {
        List<String> out = new ArrayList<>();
        for (Entry e : entries(family)) if (e.kind() == Kind.GEAR) out.add(e.id());
        return out.isEmpty() ? allGear : out;
    }

    public LootRoster roster() { return roster; }
}
