package com.newuniverse.nusmp.elite.records.rule;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Why: what a title (or a synergy) gives. Three kinds, exactly as the owner's inventory lists them: attribute modifiers (a vanilla or Tensura attribute, an amount and an operation), effects (a
 * potion effect kept up while the title is active) and custom stats (named numbers other systems read, for example {@code xp_bonus_percent} or {@code forge_quality_bonus}). Pure data plus the
 * arithmetic that combines several sources: {@link #merge} adds attribute amounts of the same attribute and operation, keeps the highest amplifier of an effect and adds stats, and
 * {@link #scaled} applies the server's bonus-scale setting. Attribute ids stay strings here ({@code "minecraft:generic.max_health"}); the server resolves them against the real registry.
 */
public final class Bonuses {
    public static final Bonuses NONE = new Bonuses(List.of(), List.of(), Map.of());

    /** How an attribute amount is applied (the three vanilla operations). */
    public enum Op {
        ADD_VALUE, ADD_MULTIPLIED_BASE, ADD_MULTIPLIED_TOTAL;

        static Op parse(String s) {
            if (s == null) return ADD_VALUE;
            String t = s.trim().toLowerCase(Locale.ROOT);
            return switch (t) {
                case "add_multiplied_base", "multiply_base", "multiplied_base" -> ADD_MULTIPLIED_BASE;
                case "add_multiplied_total", "multiply_total", "multiplied_total" -> ADD_MULTIPLIED_TOTAL;
                default -> ADD_VALUE;
            };
        }
    }

    public record Attr(String attribute, double amount, Op op) {
        String key() { return attribute + "|" + op.name(); }
    }

    public record Effect(String effect, int amplifier) {}

    private final List<Attr> attributes;
    private final List<Effect> effects;
    private final Map<String, Double> stats;

    public Bonuses(List<Attr> attributes, List<Effect> effects, Map<String, Double> stats) {
        this.attributes = List.copyOf(attributes);
        this.effects = List.copyOf(effects);
        this.stats = Collections.unmodifiableMap(new LinkedHashMap<>(stats));
    }

    public List<Attr> attributes() { return attributes; }

    public List<Effect> effects() { return effects; }

    public Map<String, Double> stats() { return stats; }

    public boolean isEmpty() { return attributes.isEmpty() && effects.isEmpty() && stats.isEmpty(); }

    public double stat(String name) { return stats.getOrDefault(name, 0.0); }

    /** The sum of several bonus sets (see the class comment). Order of first appearance is kept. */
    public static Bonuses merge(List<Bonuses> parts) {
        Map<String, Attr> attrs = new LinkedHashMap<>();
        Map<String, Effect> eff = new LinkedHashMap<>();
        Map<String, Double> st = new LinkedHashMap<>();
        for (Bonuses b : parts) {
            if (b == null) continue;
            for (Attr a : b.attributes) attrs.merge(a.key(), a, (x, y) -> new Attr(x.attribute, x.amount + y.amount, x.op));
            for (Effect e : b.effects) eff.merge(e.effect, e, (x, y) -> x.amplifier >= y.amplifier ? x : y);
            for (var s : b.stats.entrySet()) st.merge(s.getKey(), s.getValue(), Double::sum);
        }
        return new Bonuses(new ArrayList<>(attrs.values()), new ArrayList<>(eff.values()), st);
    }

    /** Attribute amounts and stats times {@code factor} (effects are unchanged); 1.0 returns this. */
    public Bonuses scaled(double factor) {
        if (factor == 1.0) return this;
        List<Attr> a = new ArrayList<>();
        for (Attr x : attributes) a.add(new Attr(x.attribute, x.amount * factor, x.op));
        Map<String, Double> s = new LinkedHashMap<>();
        for (var e : stats.entrySet()) s.put(e.getKey(), e.getValue() * factor);
        return new Bonuses(a, effects, s);
    }

    // ------------------------------------------------------------------ JSON
    public static Bonuses parse(JsonObject o, String where, List<String> problems) {
        if (o == null) return NONE;
        List<Attr> attrs = new ArrayList<>();
        JsonArray aa = J.arr(o, "attributes");
        if (aa != null) {
            for (JsonElement e : aa) {
                if (!e.isJsonObject()) { problems.add(where + ": an attribute bonus must be an object"); continue; }
                JsonObject a = e.getAsJsonObject();
                String id = J.str(a, "attribute", "");
                if (id.isEmpty() || !a.has("amount")) { problems.add(where + ": an attribute bonus needs 'attribute' and 'amount'"); continue; }
                attrs.add(new Attr(id, J.num(a, "amount", 0), Op.parse(J.str(a, "operation", "add_value"))));
            }
        }
        List<Effect> effs = new ArrayList<>();
        JsonArray ea = J.arr(o, "effects");
        if (ea != null) {
            for (JsonElement e : ea) {
                if (!e.isJsonObject()) { problems.add(where + ": an effect bonus must be an object"); continue; }
                JsonObject x = e.getAsJsonObject();
                String id = J.str(x, "effect", "");
                if (id.isEmpty()) { problems.add(where + ": an effect bonus needs 'effect'"); continue; }
                effs.add(new Effect(id, (int) Math.max(0, Math.min(9, J.num(x, "amplifier", 0)))));
            }
        }
        Map<String, Double> st = new LinkedHashMap<>();
        JsonObject so = J.obj(o, "stats");
        if (so != null) for (var e : so.entrySet()) st.put(e.getKey(), J.num(so, e.getKey(), 0));
        return new Bonuses(attrs, effs, st);
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        JsonArray aa = new JsonArray();
        for (Attr a : attributes) {
            JsonObject x = new JsonObject();
            x.addProperty("attribute", a.attribute);
            x.addProperty("amount", a.amount);
            x.addProperty("operation", a.op.name().toLowerCase(Locale.ROOT));
            aa.add(x);
        }
        o.add("attributes", aa);
        JsonArray ea = new JsonArray();
        for (Effect e : effects) {
            JsonObject x = new JsonObject();
            x.addProperty("effect", e.effect);
            x.addProperty("amplifier", e.amplifier);
            ea.add(x);
        }
        o.add("effects", ea);
        JsonObject so = new JsonObject();
        for (var e : stats.entrySet()) so.addProperty(e.getKey(), e.getValue());
        o.add("stats", so);
        return o;
    }
}
