package com.newuniverse.nusmp.franchise;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The table of the franchise engravings: which franchises there are and the numbers of every engraving's effects. Read from {@code data/nusmp/franchise/engravings.json}, which
 * {@code tools/franchise/gen_engravings.py} writes (so the numbers, the lang text and the datapack enchantments cannot drift apart). Pure: no Minecraft types, unit-tested.
 * <p>
 * An engraving is a datapack enchantment {@code nusmp:fr_<name>}; here it is the same id without the namespace. Its {@link Effect}s are what the code does with it
 * ({@link FranchiseRules} decides, {@link FranchiseEvents} acts).
 */
public final class FranchiseData {
    private FranchiseData() {}

    /** A franchise: the id of the map, the display name, the tooltip colour and the particle theme of its proc puffs. */
    public record Franchise(String id, String name, int colour, String theme) {}

    /** One effect of an engraving: its kind and its numbers / strings. */
    public record Effect(String kind, Map<String, Double> num, Map<String, String> str) {
        public double d(String key, double fallback) {
            Double v = num.get(key);
            return v == null ? fallback : v;
        }

        public int i(String key, int fallback) {
            Double v = num.get(key);
            return v == null ? fallback : (int) Math.round(v);
        }

        public String s(String key, String fallback) {
            String v = str.get(key);
            return v == null ? fallback : v;
        }
    }

    /** An engraving: {@code id} is the path ({@code fr_grimoire_surge}); {@code scope} is main, held or any. */
    public record Engraving(String id, String franchise, String name, String scope, List<Effect> effects) {}

    /** The parsed file. */
    public record Table(List<Franchise> franchises, Map<String, Engraving> engravings) {
        public List<Engraving> of(String franchise) {
            List<Engraving> out = new ArrayList<>();
            for (Engraving e : engravings.values()) if (e.franchise().equals(franchise)) out.add(e);
            return out;
        }

        public Franchise franchise(String id) {
            for (Franchise f : franchises) if (f.id().equals(id)) return f;
            return null;
        }
    }

    private static final String RESOURCE = "/data/nusmp/franchise/engravings.json";
    private static volatile Table table;

    /** The table (loaded once; an unreadable file gives an empty table so that nothing breaks, the self-test and the unit test catch it). */
    public static Table table() {
        Table t = table;
        if (t == null) {
            synchronized (FranchiseData.class) {
                t = table;
                if (t == null) {
                    try (InputStream in = FranchiseData.class.getResourceAsStream(RESOURCE)) {
                        t = in == null ? new Table(List.of(), Map.of()) : parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                    } catch (Exception | LinkageError failed) {
                        t = new Table(List.of(), Map.of());
                    }
                    table = t;
                }
            }
        }
        return t;
    }

    /** The engraving with this path ({@code fr_falna}), or null. */
    public static Engraving engraving(String path) { return path == null ? null : table().engravings().get(path); }

    public static Table parse(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        List<Franchise> fs = new ArrayList<>();
        for (JsonElement el : root.getAsJsonArray("franchises")) {
            JsonObject o = el.getAsJsonObject();
            fs.add(new Franchise(o.get("id").getAsString(), o.get("name").getAsString(), colour(o.get("colour").getAsString()), o.get("theme").getAsString()));
        }
        Map<String, Engraving> es = new LinkedHashMap<>();
        for (JsonElement el : root.getAsJsonArray("engravings")) {
            JsonObject o = el.getAsJsonObject();
            List<Effect> effects = new ArrayList<>();
            JsonArray arr = o.getAsJsonArray("effects");
            for (JsonElement fe : arr) {
                JsonObject fo = fe.getAsJsonObject();
                Map<String, Double> num = new LinkedHashMap<>();
                Map<String, String> str = new LinkedHashMap<>();
                for (Map.Entry<String, JsonElement> en : fo.entrySet()) {
                    if (en.getKey().equals("kind")) continue;
                    if (en.getValue().getAsJsonPrimitive().isNumber()) num.put(en.getKey(), en.getValue().getAsDouble());
                    else str.put(en.getKey(), en.getValue().getAsString());
                }
                effects.add(new Effect(fo.get("kind").getAsString().toLowerCase(Locale.ROOT), Collections.unmodifiableMap(num), Collections.unmodifiableMap(str)));
            }
            String id = o.get("id").getAsString();
            es.put(id, new Engraving(id, o.get("franchise").getAsString(), o.get("name").getAsString(), o.get("scope").getAsString(), List.copyOf(effects)));
        }
        return new Table(List.copyOf(fs), Collections.unmodifiableMap(es));
    }

    private static int colour(String hex) {
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        return Integer.parseInt(h, 16) & 0xFFFFFF;
    }
}
