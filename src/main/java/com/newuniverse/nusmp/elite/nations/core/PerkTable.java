package com.newuniverse.nusmp.elite.nations.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Why: the perks per nation level ("perks per level (data perks.json)"), pure and parsed from JSON by the reload listener. Perks are cumulative: a level-4 nation has the perks of levels 2, 3 and 4.
 * File shape: {@code {"levels": [ {"level": 2, "perks": [ {"type": "treasury_cap", "value": 50000}, {"type": "attribute", "arg": "minecraft:generic.max_health", "value": 2} ]} ]}}. A bad entry is
 * skipped and reported in {@code warnings}, never fatal. {@link #defaults()} is the built-in table used when the datapack has none.
 */
public final class PerkTable {
    private final TreeMap<Integer, List<Perk>> byLevel = new TreeMap<>();
    private final List<String> warnings = new ArrayList<>();

    public PerkTable() {}

    public PerkTable add(int level, Perk perk) {
        if (level >= 2 && perk != null) byLevel.computeIfAbsent(level, k -> new ArrayList<>()).add(perk);
        return this;
    }

    public List<String> warnings() { return warnings; }

    public int maxLevelWithPerks() { return byLevel.isEmpty() ? 1 : byLevel.lastKey(); }

    public int perkCount() { int n = 0; for (List<Perk> l : byLevel.values()) n += l.size(); return n; }

    /** Every perk a nation of this level has (cumulative), in level order. */
    public List<Perk> upTo(int level) {
        List<Perk> out = new ArrayList<>();
        for (Map.Entry<Integer, List<Perk>> e : byLevel.headMap(level, true).entrySet()) out.addAll(e.getValue());
        return out;
    }

    /** The perks first granted AT this level (for "next level gives" displays). */
    public List<Perk> at(int level) { return byLevel.getOrDefault(level, List.of()); }

    /** Sum of all perks of a type for a level. */
    public double sum(String type, int level) {
        double s = 0.0;
        for (Perk p : upTo(level)) if (p.is(type)) s += p.value();
        return s;
    }

    public List<Perk> ofType(String type, int level) {
        List<Perk> out = new ArrayList<>();
        for (Perk p : upTo(level)) if (p.is(type)) out.add(p);
        return out;
    }

    // ---------------------------------------------------------------- parsing
    public static PerkTable parse(JsonObject root) {
        PerkTable t = new PerkTable();
        if (root == null || !root.has("levels") || !root.get("levels").isJsonArray()) {
            t.warnings.add("no 'levels' array");
            return t;
        }
        JsonArray arr = root.getAsJsonArray("levels");
        for (JsonElement el : arr) {
            try {
                JsonObject lv = el.getAsJsonObject();
                int level = lv.get("level").getAsInt();
                if (level < 2 || level > 100) { t.warnings.add("level out of range: " + level); continue; }
                for (JsonElement pe : lv.getAsJsonArray("perks")) {
                    try {
                        JsonObject po = pe.getAsJsonObject();
                        String type = po.get("type").getAsString();
                        if (!knownType(type)) { t.warnings.add("level " + level + ": unknown perk type '" + type + "'"); continue; }
                        String arg = po.has("arg") ? po.get("arg").getAsString() : "";
                        if (Perk.ATTRIBUTE.equals(type) && arg.isEmpty()) { t.warnings.add("level " + level + ": attribute perk without arg"); continue; }
                        t.add(level, new Perk(type, arg, po.get("value").getAsDouble()));
                    } catch (RuntimeException bad) {
                        t.warnings.add("level " + level + ": bad perk (" + bad.getMessage() + ")");
                    }
                }
            } catch (RuntimeException bad) {
                t.warnings.add("bad level entry (" + bad.getMessage() + ")");
            }
        }
        return t;
    }

    public static boolean knownType(String type) {
        return Perk.ATTRIBUTE.equals(type) || Perk.SHOP_DISCOUNT.equals(type) || Perk.TREASURY_CAP.equals(type) || Perk.MEMBER_CAP.equals(type)
                || Perk.GUARD_COUNT.equals(type) || Perk.CONTRACT_BONUS.equals(type) || Perk.BOARD_SLOTS.equals(type);
    }

    /** The built-in table (same numbers as the shipped perks.json). */
    public static PerkTable defaults() {
        PerkTable t = new PerkTable();
        for (int lv = 2; lv <= 10; lv++) {
            t.add(lv, new Perk(Perk.TREASURY_CAP, "", 25_000.0 * lv));
            if (lv % 2 == 0) t.add(lv, new Perk(Perk.MEMBER_CAP, "", 5));
            if (lv % 3 == 0) t.add(lv, new Perk(Perk.GUARD_COUNT, "", 1));
            if (lv >= 3 && lv % 3 == 0) t.add(lv, new Perk(Perk.SHOP_DISCOUNT, "", 2));
        }
        t.add(2, new Perk(Perk.ATTRIBUTE, "minecraft:generic.max_health", 2.0));
        t.add(5, new Perk(Perk.ATTRIBUTE, "minecraft:generic.armor", 1.0));
        t.add(8, new Perk(Perk.ATTRIBUTE, "minecraft:generic.attack_damage", 1.0));
        t.add(4, new Perk(Perk.BOARD_SLOTS, "", 2));
        t.add(6, new Perk(Perk.CONTRACT_BONUS, "", 10));
        return t;
    }
}
