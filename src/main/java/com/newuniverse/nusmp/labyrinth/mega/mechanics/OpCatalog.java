package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: the vocabulary of the mechanics (every primitive, trigger and counter, with its arguments, ranges, default values and the exclusive CHANNEL it occupies) is written once, in
 * {@code tools/mechanics/tables/ops.py}, and shipped as {@code data/nusmp/mega/mechanics/ops.json}. This pure class reads that catalogue so the parser can reject a typo
 * ("unknown argument secs2 of damage") instead of the engine ignoring it silently, the signature guard can merge defaults, the conflict table knows which primitive blocks
 * which channel, and the in-game self-test can demand an executor for every id. A datapack that adds a primitive ships its own ops entry and the Java executor registry.
 */
public final class OpCatalog {
    /** One argument of a primitive / trigger / counter: type is num, str or bool. */
    public record ArgSpec(String name, String type, Object def, double lo, double hi, List<String> allowed) {
        public boolean numeric() { return "num".equals(type); }
    }

    public record OpSpec(String id, String group, List<String> channels, boolean sustained, List<ArgSpec> args, String doc) {
        public ArgSpec arg(String n) {
            for (ArgSpec a : args) if (a.name().equals(n)) return a;
            return null;
        }
    }

    /** A trigger or a counter. {@code props} (counters only) says what the engine must place for it: none, mobs, pedestal, plate, lever. */
    public record KindSpec(String id, List<ArgSpec> args, String props, String doc) {
        public ArgSpec arg(String n) {
            for (ArgSpec a : args) if (a.name().equals(n)) return a;
            return null;
        }
    }

    /** Keys of an effect object that are not arguments of its primitive. */
    public static final Set<String> EFFECT_KEYS = Set.of("op", "delay", "chance", "times", "every", "if", "on");
    public static final Set<String> TRIGGER_KEYS = Set.of("kind"), COUNTER_KEYS = Set.of("kind");

    private final Map<String, OpSpec> ops;
    private final Map<String, KindSpec> triggers, counters;
    private final Set<String> exclusive;
    private final Map<String, String> effectChannels;
    private final int total;
    private final Map<String, Integer> counts;

    private OpCatalog(Map<String, OpSpec> ops, Map<String, KindSpec> triggers, Map<String, KindSpec> counters, Set<String> exclusive, Map<String, String> effectChannels, int total,
                      Map<String, Integer> counts) {
        this.total = total;
        this.counts = counts;
        this.ops = ops;
        this.triggers = triggers;
        this.counters = counters;
        this.exclusive = exclusive;
        this.effectChannels = effectChannels;
    }

    /** How many mechanics the generator wrote (the self-test demands the loaded library has exactly this many), and per category. */
    public int total() { return total; }

    public Map<String, Integer> counts() { return counts; }

    public OpSpec op(String id) { return ops.get(id); }

    public KindSpec trigger(String id) { return triggers.get(id); }

    public KindSpec counter(String id) { return counters.get(id); }

    public Map<String, OpSpec> ops() { return ops; }

    public Map<String, KindSpec> triggers() { return triggers; }

    public Map<String, KindSpec> counters() { return counters; }

    public Set<String> exclusiveChannels() { return exclusive; }

    /** The channel a mob effect id occupies ("minecraft:darkness" -> "vision"), or null when it is free. */
    public String effectChannel(String effectId) { return effectChannels.get(effectId); }

    /** Parses ops.json. Throws IllegalArgumentException with a readable message when it is unusable (the catalogue is shipped code-adjacent data, not user data). */
    public static OpCatalog parse(String json) {
        JsonObject root;
        try {
            root = JsonParser.parseString(json).getAsJsonObject();
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("ops.json is not a JSON object: " + e.getMessage());
        }
        Map<String, OpSpec> ops = new LinkedHashMap<>();
        JsonObject o = root.getAsJsonObject("ops");
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            JsonObject v = e.getValue().getAsJsonObject();
            List<String> ch = new ArrayList<>();
            for (JsonElement c : v.getAsJsonArray("channels")) ch.add(c.getAsString());
            ops.put(e.getKey(), new OpSpec(e.getKey(), v.get("group").getAsString(), List.copyOf(ch), v.get("sustained").getAsBoolean(), specs(v.getAsJsonArray("args")),
                    v.has("doc") ? v.get("doc").getAsString() : ""));
        }
        Map<String, Integer> counts = new LinkedHashMap<>();
        if (root.has("counts")) for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("counts").entrySet()) counts.put(e.getKey(), e.getValue().getAsInt());
        return new OpCatalog(ops, kinds(root.getAsJsonObject("triggers")), kinds(root.getAsJsonObject("counters")), strings(root.getAsJsonArray("exclusive")),
                map(root.getAsJsonObject("effectChannels")), root.has("total") ? root.get("total").getAsInt() : 0, Map.copyOf(counts));
    }

    private static Map<String, KindSpec> kinds(JsonObject o) {
        Map<String, KindSpec> out = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            JsonObject v = e.getValue().getAsJsonObject();
            out.put(e.getKey(), new KindSpec(e.getKey(), specs(v.getAsJsonArray("args")), v.has("props") ? v.get("props").getAsString() : "none", v.has("doc") ? v.get("doc").getAsString() : ""));
        }
        return out;
    }

    private static List<ArgSpec> specs(JsonArray a) {
        List<ArgSpec> out = new ArrayList<>();
        for (JsonElement x : a) {
            JsonObject s = x.getAsJsonObject();
            String t = s.get("t").getAsString();
            Object def;
            JsonElement d = s.get("d");
            if ("num".equals(t)) def = d.getAsDouble();
            else if ("bool".equals(t)) def = d.getAsBoolean();
            else def = d.getAsString();
            List<String> allowed = new ArrayList<>();
            if (s.has("e")) for (JsonElement q : s.getAsJsonArray("e")) allowed.add(q.getAsString());
            out.add(new ArgSpec(s.get("n").getAsString(), t, def, s.has("lo") ? s.get("lo").getAsDouble() : -1e18, s.has("hi") ? s.get("hi").getAsDouble() : 1e18, List.copyOf(allowed)));
        }
        return List.copyOf(out);
    }

    private static Set<String> strings(JsonArray a) {
        Set<String> out = new LinkedHashSet<>();
        for (JsonElement x : a) out.add(x.getAsString());
        return Set.copyOf(out);
    }

    private static Map<String, String> map(JsonObject o) {
        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : o.entrySet()) out.put(e.getKey(), e.getValue().getAsString());
        return Map.copyOf(out);
    }

    // ---------------------------------------------------------------- validation
    /** Every problem of an argument bag against its specs (unknown names, wrong types, values out of range or not allowed at floor 1 and at floor 1000). */
    public static void checkArgs(String where, Args args, List<ArgSpec> specs, List<String> out) {
        for (String k : args.raw().keySet()) {
            ArgSpec s = null;
            for (ArgSpec x : specs) if (x.name().equals(k)) s = x;
            if (s == null) { out.add(where + ": unknown argument '" + k + "'"); continue; }
            Object v = args.raw().get(k);
            switch (s.type()) {
                case "num" -> {
                    double[] p = args.pair(k);
                    if (p == null) { out.add(where + ": '" + k + "' must be a number or [a, b]"); break; }
                    for (int floor : new int[] {1, Scaling.MAX_FLOOR}) {
                        double x = Scaling.value(p[0], p[1], floor);
                        if (x < s.lo() - 1e-9 || x > s.hi() + 1e-9) { out.add(where + ": '" + k + "' = " + x + " at floor " + floor + " is outside " + s.lo() + ".." + s.hi()); break; }
                    }
                }
                case "bool" -> { if (!(v instanceof Boolean)) out.add(where + ": '" + k + "' must be true or false"); }
                default -> {
                    if (!(v instanceof String str)) out.add(where + ": '" + k + "' must be a string");
                    else if (!s.allowed().isEmpty() && !s.allowed().contains(str)) out.add(where + ": '" + k + "' = '" + str + "' is not one of " + s.allowed());
                }
            }
        }
    }

    /** Every problem of a definition against the catalogue (empty = fine). */
    public List<String> check(MechanicDef d) {
        List<String> out = new ArrayList<>();
        String w = d.id();
        KindSpec t = triggers.get(d.trigger().kind());
        if (t == null) out.add(w + ": unknown trigger '" + d.trigger().kind() + "'");
        else checkArgs(w + " trigger", d.trigger().args(), t.args(), out);
        KindSpec c = counters.get(d.counter().kind());
        if (c == null) out.add(w + ": unknown counter '" + d.counter().kind() + "'");
        else checkArgs(w + " counter", d.counter().args(), c.args(), out);
        if (d.effects().isEmpty()) out.add(w + ": no effects");
        for (int i = 0; i < d.effects().size(); i++) {
            MechanicDef.Effect e = d.effects().get(i);
            OpSpec s = ops.get(e.op());
            if (s == null) { out.add(w + ": unknown primitive '" + e.op() + "'"); continue; }
            checkArgs(w + " effect " + i + " (" + e.op() + ")", e.args(), s.args(), out);
        }
        return out;
    }
}
