package com.newuniverse.nusmp.elite.records.rule;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: one unlock condition as data: a {@link CondType} plus its parameters (the rest of the JSON object, for example {@code {"type":"kill_entity","entity":"minecraft:ender_dragon","count":1}}).
 * The parameters stay a Gson object so a datapack can add a key a later version understands without a schema change here; the typed getters never throw. The same class is used by a record's
 * condition and by the "condition" requirement of a title. Pure and immutable (the object is copied on the way in, and only read afterwards).
 */
public final class Condition {
    private final CondType type;
    private final JsonObject params;

    public Condition(CondType type, JsonObject params) {
        this.type = type;
        this.params = params == null ? new JsonObject() : params.deepCopy();
    }

    public CondType type() { return type; }

    public String str(String key, String def) { return J.str(params, key, def); }

    public double num(String key, double def) { return J.num(params, key, def); }

    public List<String> strings(String key) { return J.strings(params, key); }

    public boolean has(String key) { return params.has(key); }

    /** The JSON form, {@code type} first. */
    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("type", type.id());
        for (var e : params.entrySet()) if (!e.getKey().equals("type")) o.add(e.getKey(), e.getValue());
        return o;
    }

    /** Parses {@code {"type": ..., ...}}; null (with a line in {@code problems}) when it has no known type. */
    public static Condition parse(JsonElement e, String where, List<String> problems) {
        if (e == null || !e.isJsonObject()) {
            problems.add(where + ": the condition must be an object");
            return null;
        }
        JsonObject o = e.getAsJsonObject();
        CondType t = CondType.parse(J.str(o, "type", ""));
        if (t == null) {
            problems.add(where + ": unknown condition type '" + J.str(o, "type", "") + "'");
            return null;
        }
        Condition c = new Condition(t, o);
        List<String> bad = ConditionEvaluator.validate(c);
        if (!bad.isEmpty()) {
            problems.addAll(prefix(where, bad));
            return null;
        }
        return c;
    }

    private static List<String> prefix(String where, List<String> in) {
        List<String> out = new ArrayList<>();
        for (String s : in) out.add(where + ": " + s);
        return out;
    }

    @Override public String toString() { return toJson().toString(); }
}
