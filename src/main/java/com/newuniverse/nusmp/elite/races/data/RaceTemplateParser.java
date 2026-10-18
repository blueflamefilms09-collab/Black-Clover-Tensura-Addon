package com.newuniverse.nusmp.elite.races.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.newuniverse.nusmp.elite.races.rule.RaceRuleParser;
import com.newuniverse.nusmp.elite.races.rule.RaceRuleSet;
import com.newuniverse.nusmp.elite.races.state.EntryCondition;
import com.newuniverse.nusmp.elite.races.state.MoonRule;
import com.newuniverse.nusmp.elite.races.state.StateModifier;
import com.newuniverse.nusmp.elite.races.state.TransformSpec;
import com.newuniverse.nusmp.elite.races.state.TransformState;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.newuniverse.nusmp.elite.races.rule.RaceRuleParser.id;
import static com.newuniverse.nusmp.elite.races.rule.RaceRuleParser.num;
import static com.newuniverse.nusmp.elite.races.rule.RaceRuleParser.str;

/**
 * Why: the race files are data a datapack can replace, so the parser is the gate. It is strict and says where it stopped ("lesser_saiyan.transformations.great_ape.entries[0]:
 * 'moon' ..."): an unknown state, trait, attribute or rule type, a range turned round (min above max), a number outside a sane band, an attribute the mod does not know, an extra attribute the stats
 * already set, a natural skill that is not one of the race's skills, and an {@code auto} entry that has no condition (it would fire at every tick). Pure (Gson),
 * tested against the bundled files by {@code BundledRaceTemplatesTest}. Durations in the files are seconds; the template holds ticks.
 */
public final class RaceTemplateParser {
    private RaceTemplateParser() {}

    public static final Set<String> TRAITS = Set.of("zenkai");
    private static final Set<String> DIFFICULTIES = Set.of("easy", "intermediate", "hard", "extreme");
    private static final Set<String> ALIGNMENTS = Set.of("default", "majin", "holy", "chaos");

    public static RaceTemplate parse(String id, JsonObject o) {
        if (id == null || !id.matches("[a-z][a-z0-9_]{1,40}")) throw new IllegalArgumentException("race id '" + id + "' must be lowercase letters, digits and underscores");
        String w = id;
        String line = str(o, "line", w);
        int tier = (int) num(o, "tier", w, 1, 5);
        String difficulty = o.has("difficulty") ? str(o, "difficulty", w).toLowerCase(Locale.ROOT) : "intermediate";
        if (!DIFFICULTIES.contains(difficulty)) throw new IllegalArgumentException(w + ": difficulty '" + difficulty + "' is not one of " + DIFFICULTIES);
        String alignment = o.has("alignment") ? str(o, "alignment", w).toLowerCase(Locale.ROOT) : "default";
        if (!ALIGNMENTS.contains(alignment)) throw new IllegalArgumentException(w + ": alignment '" + alignment + "' is not one of " + ALIGNMENTS);

        RaceTemplate.Stats stats = stats(obj(o, "stats", w), w + ".stats");
        List<RaceTemplate.Extra> extras = extras(o.has("attributes") ? arr(o, "attributes", w) : new JsonArray(), w);
        List<String> intrinsic = ids(o, "intrinsic_skills", w, "skill");
        List<String> resistances = ids(o, "resistances", w, "skill");
        String natural = id(str(o, "natural_skill", w), w, "natural_skill");
        if (!intrinsic.contains(natural) && !resistances.contains(natural))
            throw new IllegalArgumentException(w + ": natural_skill '" + natural + "' must also be listed in intrinsic_skills or resistances");
        String evolvesTo = o.has("evolves_to") && !o.get("evolves_to").getAsString().isBlank() ? str(o, "evolves_to", w) : "";
        if (evolvesTo.equals(id)) throw new IllegalArgumentException(w + ": a race cannot evolve into itself");
        RaceRuleSet evolution = RaceRuleParser.parseSet(o.has("evolution") ? arr(o, "evolution", w) : null, w + ".evolution");
        RaceRuleSet entry = RaceRuleParser.parseSet(o.has("entry") ? arr(o, "entry", w) : null, w + ".entry");
        Set<String> traits = new HashSet<>();
        if (o.has("traits")) for (JsonElement e : arr(o, "traits", w)) {
            String t = e.getAsString();
            if (!TRAITS.contains(t)) throw new IllegalArgumentException(w + ": unknown trait '" + t + "' (" + TRAITS + ")");
            traits.add(t);
        }
        Map<TransformState, TransformSpec> states = new EnumMap<>(TransformState.class);
        if (o.has("transformations")) {
            JsonObject t = obj(o, "transformations", w);
            for (Map.Entry<String, JsonElement> e : t.entrySet()) {
                TransformState s = TransformState.byId(e.getKey());
                if (s == null || s == TransformState.BASE) throw new IllegalArgumentException(w + ".transformations: '" + e.getKey() + "' is not a transformation state");
                if (!e.getValue().isJsonObject()) throw new IllegalArgumentException(w + ".transformations." + e.getKey() + ": must be an object");
                states.put(s, spec(s, e.getValue().getAsJsonObject(), tier, w + ".transformations." + e.getKey()));
            }
        }
        return new RaceTemplate(id, line, tier, difficulty, alignment, stats, extras, intrinsic, resistances, natural, evolvesTo, evolution, entry, traits, states);
    }

    // ---------------------------------------------------------------- stats
    private static RaceTemplate.Stats stats(JsonObject s, String w) {
        double auraMin = num(s, "aura_min", w, 0, 1e9), auraMax = num(s, "aura_max", w, 0, 1e9);
        double magMin = num(s, "magicule_min", w, 0, 1e10), magMax = num(s, "magicule_max", w, 0, 1e10);
        if (auraMin > auraMax) throw new IllegalArgumentException(w + ": aura_min is above aura_max");
        if (magMin > magMax) throw new IllegalArgumentException(w + ": magicule_min is above magicule_max");
        return new RaceTemplate.Stats(auraMin, auraMax, magMin, magMax, num(s, "size", w, 0, 10), num(s, "max_health", w, 0, 10000), num(s, "spiritual_health", w, 0, 1e6),
                num(s, "attack", w, 0, 1000), num(s, "attack_speed", w, 0, 10), num(s, "knockback_resistance", w, 0, 1), num(s, "movement_speed", w, 0, 0.5),
                num(s, "swim_speed", w, 0, 5));
    }

    private static List<RaceTemplate.Extra> extras(JsonArray a, String w) {
        List<RaceTemplate.Extra> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < a.size(); i++) {
            JsonObject m = asObj(a.get(i), w + ".attributes[" + i + "]");
            String where = w + ".attributes[" + i + "]";
            String key = attribute(m, where);
            if (RaceAttributeKeys.RESERVED_BY_STATS.contains(key)) throw new IllegalArgumentException(where + ": '" + key + "' is set by 'stats'; do not repeat it here");
            if (!seen.add(key)) throw new IllegalArgumentException(where + ": '" + key + "' appears twice");
            out.add(new RaceTemplate.Extra(key, num(m, "amount", where, -1000, 1000), op(m, where)));
        }
        return out;
    }

    private static String attribute(JsonObject m, String where) {
        String key = str(m, "attribute", where);
        if (!RaceAttributeKeys.known(key)) throw new IllegalArgumentException(where + ": unknown attribute '" + key + "'");
        return key;
    }

    private static StateModifier.Op op(JsonObject m, String where) {
        StateModifier.Op op = StateModifier.Op.byId(str(m, "op", where));
        if (op == null) throw new IllegalArgumentException(where + ": 'op' must be add, mult_base or mult_total");
        return op;
    }

    // ---------------------------------------------------------------- transformation states
    private static TransformSpec spec(TransformState state, JsonObject o, int raceTier, String w) {
        Set<TransformState> from = EnumSet.noneOf(TransformState.class);
        if (o.has("from")) {
            for (JsonElement e : arr(o, "from", w)) {
                TransformState f = TransformState.byId(e.getAsString());
                if (f == null) throw new IllegalArgumentException(w + ".from: unknown state '" + e.getAsString() + "'");
                if (f == state) throw new IllegalArgumentException(w + ".from: a state cannot be entered from itself");
                from.add(f);
            }
        } else {
            from.add(TransformState.BASE);
        }
        if (from.isEmpty()) throw new IllegalArgumentException(w + ".from: must list at least one state");
        int minTier = o.has("min_tier") ? (int) num(o, "min_tier", w, 1, 5) : raceTier;
        int duration = (int) Math.round(num(o, "duration_seconds", w, 0, 3600) * 20);
        int cooldown = (int) Math.round(num(o, "cooldown_seconds", w, 0, 86400) * 20);
        double drain = o.has("aura_drain") ? num(o, "aura_drain", w, 0, 0.2) : 0.0;
        boolean dawn = flag(o, "ends_at_dawn"), ki = flag(o, "ki_scaled");
        String rukh = o.has("rukh") ? str(o, "rukh", w).toLowerCase(Locale.ROOT) : "light";
        if (!rukh.equals("light") && !rukh.equals("dark")) throw new IllegalArgumentException(w + ": rukh must be 'light' or 'dark'");
        int color = color(o.has("color") ? str(o, "color", w) : "#FFD84A", w);

        List<EntryCondition> entries = new ArrayList<>();
        if (o.has("entries")) {
            JsonArray ea = arr(o, "entries", w);
            for (int i = 0; i < ea.size(); i++) entries.add(entry(asObj(ea.get(i), w + ".entries[" + i + "]"), w + ".entries[" + i + "]"));
        }
        if (entries.isEmpty()) entries.add(EntryCondition.ALWAYS);
        List<StateModifier> mods = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        if (o.has("modifiers")) {
            JsonArray ma = arr(o, "modifiers", w);
            for (int i = 0; i < ma.size(); i++) {
                String where = w + ".modifiers[" + i + "]";
                JsonObject m = asObj(ma.get(i), where);
                String key = attribute(m, where);
                if (!seen.add(key)) throw new IllegalArgumentException(where + ": '" + key + "' appears twice");
                mods.add(new StateModifier(key, num(m, "amount", where, -100, 100), op(m, where)));
            }
        }
        return new TransformSpec(state, from, minTier, entries, duration, cooldown, drain, dawn, ki, rukh.equals("dark"), color, mods);
    }

    private static EntryCondition entry(JsonObject e, String w) {
        double epMin = e.has("ep_min") ? num(e, "ep_min", w, 0, 1e15) : 0;
        double hp = e.has("hp_below") ? num(e, "hp_below", w, 0.01, 1.0) : 1.0;
        MoonRule moon = MoonRule.ANY;
        if (e.has("moon")) {
            moon = MoonRule.byId(str(e, "moon", w));
            if (moon == null) throw new IllegalArgumentException(w + ": 'moon' must be any, night or full_moon");
        }
        boolean sky = flag(e, "sky");
        String item = e.has("item") ? id(str(e, "item", w), w, "item") : "";
        int count = item.isEmpty() ? 0 : (e.has("item_count") ? (int) num(e, "item_count", w, 1, 4096) : 1);
        boolean consume = flag(e, "consume");
        if (consume && item.isEmpty()) throw new IllegalArgumentException(w + ": 'consume' needs an 'item'");
        int minTier = e.has("min_tier") ? (int) num(e, "min_tier", w, 1, 5) : 1;
        boolean auto = flag(e, "auto");
        boolean bare = epMin <= 0 && hp >= 1.0 && moon == MoonRule.ANY && !sky && item.isEmpty();
        if (auto && bare) throw new IllegalArgumentException(w + ": an 'auto' entry needs a real condition (ep_min, hp_below, moon or item) or it would fire at every tick");
        return new EntryCondition(epMin, hp, moon, sky, item, count, consume, minTier, auto);
    }

    // ---------------------------------------------------------------- small readers
    private static boolean flag(JsonObject o, String key) { return o.has(key) && o.get(key).isJsonPrimitive() && o.get(key).getAsBoolean(); }

    private static int color(String s, String w) {
        String t = s.startsWith("#") ? s.substring(1) : s;
        if (!t.matches("[0-9a-fA-F]{6}")) throw new IllegalArgumentException(w + ": color '" + s + "' must look like #FFD84A");
        return 0xFF000000 | Integer.parseInt(t, 16);
    }

    private static JsonObject obj(JsonObject o, String key, String w) {
        if (!o.has(key) || !o.get(key).isJsonObject()) throw new IllegalArgumentException(w + ": missing object '" + key + "'");
        return o.getAsJsonObject(key);
    }

    private static JsonArray arr(JsonObject o, String key, String w) {
        if (!o.has(key) || !o.get(key).isJsonArray()) throw new IllegalArgumentException(w + ": '" + key + "' must be an array");
        return o.getAsJsonArray(key);
    }

    private static JsonObject asObj(JsonElement e, String w) {
        if (!e.isJsonObject()) throw new IllegalArgumentException(w + ": must be an object");
        return e.getAsJsonObject();
    }

    private static List<String> ids(JsonObject o, String key, String w, String what) {
        List<String> out = new ArrayList<>();
        if (!o.has(key)) return out;
        for (JsonElement e : arr(o, key, w)) {
            String s = id(e.getAsString().trim(), w + "." + key, what);
            if (!out.contains(s)) out.add(s);
        }
        return out;
    }
}
