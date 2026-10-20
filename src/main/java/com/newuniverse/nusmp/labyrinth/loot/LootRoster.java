package com.newuniverse.nusmp.labyrinth.loot;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: there are eighty items, and each is named in a dozen places (the registry, the drop tables, the sell prices, the recipes, the icons, the models, the lang file, the
 * tests). Writing them in a dozen places is how one of them gets lost, so they are written in ONE place, {@code tools/loot/roster.py}, which generates
 * {@code data/nusmp/loot/roster.json}; this class reads that file (from the class path, the way {@code GradeDamage} reads its weapon table, so it works when the registry is
 * built at mod construction, long before any data pack loads) into plain immutable records with no Minecraft type in them. The registry, the tables, the selling code and the
 * data tests all iterate THIS roster, so adding an item to the python list adds it to every one of them. A missing or broken file gives an empty roster and one log line, never
 * an exception at start-up.
 */
public final class LootRoster {
    public static final String RESOURCE = "/data/nusmp/loot/roster.json";

    public record DropRow(String family, double chance, int minBand) {}

    public record EffectDef(String effect, int seconds, int amp) {}

    /** A meal, a tonic or an elixir. {@code kind} is {@code food} or {@code elixir}; {@code mana} and {@code aura} are the percent of the maximum an elixir restores. */
    public record FoodDef(String id, String name, String kind, int nutrition, float saturation, List<EffectDef> effects, int mana, int aura, List<DropRow> drops, int value, String lore, double eat) {
        public boolean elixir() { return mana > 0 || aura > 0; }
    }

    public record MaterialDef(String id, String name, String family, int value, int tier, List<DropRow> drops, String lore) {}

    public record CraftedDef(String id, String name, int value, String lore) {}

    public record Engraving(String id, int level) {}

    public record WeaponDef(String id, String name, String type, double speed, int durability, int value, List<DropRow> drops, String lore, List<Engraving> engravings) {}

    public record ArmourSetDef(String id, String name, String repair, double toughness, double knockback, int enchant, String family, double chance, int minBand) {}

    public record ArmourDef(String id, String name, String set, String piece, int defense, int value, List<DropRow> drops, String lore) {}

    public record TrinketDef(String id, String name, String slot, String primary, int value, List<DropRow> drops, String lore) {}

    private final List<FoodDef> foods = new ArrayList<>();
    private final List<MaterialDef> materials = new ArrayList<>();
    private final List<CraftedDef> crafted = new ArrayList<>(), ammo = new ArrayList<>();
    private final List<WeaponDef> weapons = new ArrayList<>();
    private final Map<String, ArmourSetDef> sets = new LinkedHashMap<>();
    private final List<ArmourDef> armour = new ArrayList<>();
    private final List<TrinketDef> trinkets = new ArrayList<>();
    private final List<String> bossMaterials = new ArrayList<>(), bossFoods = new ArrayList<>();
    private final Map<String, String> codeUses = new LinkedHashMap<>();
    private final List<String> families = new ArrayList<>();
    private String problem = "";

    /** Why the roster is empty, or an empty string when it was read (the registry logs it once). */
    public String problem() { return problem; }

    public List<FoodDef> foods() { return foods; }

    public List<MaterialDef> materials() { return materials; }

    public List<CraftedDef> crafted() { return crafted; }

    public List<CraftedDef> ammo() { return ammo; }

    public List<WeaponDef> weapons() { return weapons; }

    public Map<String, ArmourSetDef> armourSets() { return sets; }

    public List<ArmourDef> armour() { return armour; }

    public List<TrinketDef> trinkets() { return trinkets; }

    public List<String> bossMaterials() { return bossMaterials; }

    public List<String> bossFoods() { return bossFoods; }

    public Map<String, String> codeUses() { return codeUses; }

    public List<String> families() { return families; }

    /** Every item id of the roster, in registration order. */
    public List<String> ids() {
        List<String> out = new ArrayList<>();
        for (FoodDef f : foods) out.add(f.id());
        for (MaterialDef m : materials) out.add(m.id());
        for (CraftedDef c : crafted) out.add(c.id());
        for (CraftedDef c : ammo) out.add(c.id());
        for (WeaponDef w : weapons) out.add(w.id());
        for (ArmourDef a : armour) out.add(a.id());
        for (TrinketDef t : trinkets) out.add(t.id());
        return out;
    }

    public MaterialDef material(String id) {
        for (MaterialDef m : materials) if (m.id().equals(id)) return m;
        return null;
    }

    public FoodDef food(String id) {
        for (FoodDef f : foods) if (f.id().equals(id)) return f;
        return null;
    }

    public WeaponDef weapon(String id) {
        for (WeaponDef w : weapons) if (w.id().equals(id)) return w;
        return null;
    }

    public ArmourDef armourPiece(String id) {
        for (ArmourDef a : armour) if (a.id().equals(id)) return a;
        return null;
    }

    public TrinketDef trinket(String id) {
        for (TrinketDef t : trinkets) if (t.id().equals(id)) return t;
        return null;
    }

    /** The sell value in bronze coins of any item of the roster (base value for gear), or -1 when the id is not in it. */
    public int value(String id) {
        for (FoodDef f : foods) if (f.id().equals(id)) return f.value();
        for (MaterialDef m : materials) if (m.id().equals(id)) return m.value();
        for (CraftedDef c : crafted) if (c.id().equals(id)) return c.value();
        for (CraftedDef c : ammo) if (c.id().equals(id)) return c.value();
        for (WeaponDef w : weapons) if (w.id().equals(id)) return w.value();
        for (ArmourDef a : armour) if (a.id().equals(id)) return a.value();
        for (TrinketDef t : trinkets) if (t.id().equals(id)) return t.value();
        return -1;
    }

    // ---------------------------------------------------------------- reading
    private static volatile LootRoster loaded;

    /** The roster of the jar, read once. */
    public static LootRoster get() {
        LootRoster r = loaded;
        if (r != null) return r;
        synchronized (LootRoster.class) {
            if (loaded == null) loaded = read();
            return loaded;
        }
    }

    private static LootRoster read() {
        try (InputStream in = LootRoster.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                LootRoster r = new LootRoster();
                r.problem = RESOURCE + " is missing: no dungeon loot items are registered";
                return r;
            }
            return parse(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (Exception e) {
            LootRoster r = new LootRoster();
            r.problem = RESOURCE + " could not be read: " + e;
            return r;
        }
    }

    /** Parses a roster; used by the loader and the tests. */
    public static LootRoster parse(Reader reader) {
        JsonObject o = JsonParser.parseReader(reader).getAsJsonObject();
        LootRoster r = new LootRoster();
        for (JsonElement e : arr(o, "families")) r.families.add(e.getAsString());
        for (JsonElement e : arr(o, "foods")) {
            JsonObject f = e.getAsJsonObject();
            List<EffectDef> fx = new ArrayList<>();
            for (JsonElement x : arr(f, "effects")) {
                JsonObject xo = x.getAsJsonObject();
                fx.add(new EffectDef(str(xo, "effect"), xo.get("seconds").getAsInt(), xo.get("amp").getAsInt()));
            }
            r.foods.add(new FoodDef(str(f, "id"), str(f, "name"), str(f, "kind"), f.get("nutrition").getAsInt(), f.get("saturation").getAsFloat(), fx, f.get("mana").getAsInt(), f.get("aura").getAsInt(),
                    drops(f), f.get("value").getAsInt(), str(f, "lore"), f.get("eat").getAsDouble()));
        }
        for (JsonElement e : arr(o, "materials")) {
            JsonObject m = e.getAsJsonObject();
            r.materials.add(new MaterialDef(str(m, "id"), str(m, "name"), str(m, "family"), m.get("value").getAsInt(), m.get("tier").getAsInt(), drops(m), str(m, "lore")));
        }
        for (JsonElement e : arr(o, "crafted")) r.crafted.add(craftedOf(e.getAsJsonObject()));
        for (JsonElement e : arr(o, "ammo")) r.ammo.add(craftedOf(e.getAsJsonObject()));
        for (JsonElement e : arr(o, "weapons")) {
            JsonObject w = e.getAsJsonObject();
            List<Engraving> eng = new ArrayList<>();
            for (JsonElement x : arr(w, "engravings")) eng.add(new Engraving(str(x.getAsJsonObject(), "id"), x.getAsJsonObject().get("level").getAsInt()));
            r.weapons.add(new WeaponDef(str(w, "id"), str(w, "name"), str(w, "type"), w.get("speed").getAsDouble(), w.get("durability").getAsInt(), w.get("value").getAsInt(), drops(w), str(w, "lore"), eng));
        }
        if (o.has("armour_sets")) {
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("armour_sets").entrySet()) {
                JsonObject s = e.getValue().getAsJsonObject();
                r.sets.put(e.getKey(), new ArmourSetDef(e.getKey(), str(s, "name"), str(s, "repair"), s.get("toughness").getAsDouble(), s.get("knockback").getAsDouble(), s.get("enchant").getAsInt(),
                        str(s, "family"), s.get("chance").getAsDouble(), s.get("minBand").getAsInt()));
            }
        }
        for (JsonElement e : arr(o, "armour")) {
            JsonObject a = e.getAsJsonObject();
            r.armour.add(new ArmourDef(str(a, "id"), str(a, "name"), str(a, "set"), str(a, "piece"), a.get("defense").getAsInt(), a.get("value").getAsInt(), drops(a), str(a, "lore")));
        }
        for (JsonElement e : arr(o, "trinkets")) {
            JsonObject t = e.getAsJsonObject();
            r.trinkets.add(new TrinketDef(str(t, "id"), str(t, "name"), str(t, "slot"), str(t, "primary"), t.get("value").getAsInt(), drops(t), str(t, "lore")));
        }
        if (o.has("boss")) {
            JsonObject b = o.getAsJsonObject("boss");
            for (JsonElement e : arr(b, "materials")) r.bossMaterials.add(e.getAsString());
            for (JsonElement e : arr(b, "foods")) r.bossFoods.add(e.getAsString());
        }
        if (o.has("codeUses")) for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("codeUses").entrySet()) r.codeUses.put(e.getKey(), e.getValue().getAsString());
        return r;
    }

    private static CraftedDef craftedOf(JsonObject c) { return new CraftedDef(str(c, "id"), str(c, "name"), c.get("value").getAsInt(), str(c, "lore")); }

    private static List<DropRow> drops(JsonObject o) {
        List<DropRow> out = new ArrayList<>();
        for (JsonElement e : arr(o, "drops")) {
            JsonObject d = e.getAsJsonObject();
            out.add(new DropRow(str(d, "family"), d.get("chance").getAsDouble(), d.get("minBand").getAsInt()));
        }
        return out;
    }

    private static JsonArray arr(JsonObject o, String key) { return o.has(key) && o.get(key).isJsonArray() ? o.getAsJsonArray(key) : new JsonArray(); }

    private static String str(JsonObject o, String key) { return o.has(key) ? o.get(key).getAsString() : ""; }
}
