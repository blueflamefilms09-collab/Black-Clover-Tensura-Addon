package com.newuniverse.nusmp.labyrinth.mega.decennial;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Why: a boss file names things the code must know how to do (a lair action, a signature mechanic, a kind of add), and a typo in a name must fail a test and not make a boss silently do nothing.
 * So the vocabulary is one table: {@code data/nusmp/mega/decennial/lairs.json} (every lair action: how it is laid out, what it does to whoever stands in it, how hard, how long it warns, and the
 * line the party reads), {@code signatures.json} (the signature mechanics) and the closed lists below (effects, patterns, adds). The lair table is data so a server owner can retune a number
 * (damage share, radius, warning time) without a rebuild, but its ids are the code's own: an id the code has no pattern for is refused when the file is read. Every number is clamped through
 * {@link DecennialRules}, which is what keeps "NO hunger" a property of the TABLE: the effect list has no hunger entry and no entry here can invent one.
 */
public final class DecennialVocab {
    private DecennialVocab() {}

    /** How a lair action lays itself out over the hall. */
    public enum Pattern {
        /** A telegraphed circle under up to {@code count} of the players. */
        AT_PLAYERS,
        /** {@code count} circles at random spots of the floor (the hall's own hazard spots first). */
        RANDOM_SPOTS,
        /** {@code count} lines from the boss towards players, {@code radius} wide, 14 blocks long. */
        LINE_TO_PLAYER,
        /** A ring that grows out from the boss to {@code radius} and hurts what it crosses. */
        RING_PULSE,
        /** {@code count} zones that stay for {@code duration} ticks and act every second. */
        ZONE,
        /** Everyone within {@code radius} is dragged to the boss (or the hall's middle). */
        PULL,
        /** A pillar of the hall falls on the players (needs a destructible pillar; falls back to a circle when the hall has none left). */
        TOPPLE,
        /** The hall's own grave / root / nest spots erupt (falls back to circles under the players). */
        SPOTS
    }

    /** What a lair action does to a player it touches. There is NO hunger entry, by design (the owner's global ban). */
    public enum Effect { NONE, SLOW, WEAKNESS, POISON, IGNITE, FREEZE, KNOCKBACK, LAUNCH, ROOT, CONFUSE, BLIND, DRAIN }

    /** One lair action. {@code share} is the part of the boss's attack damage one hit takes; {@code seconds} of {@code effectTicks} apply to a player hit. */
    public record Lair(String id, String side, Pattern pattern, Effect effect, int count, double radius, int telegraph, double share, int duration, int effectTicks, String particle, String zone, String text) {}

    /** One signature mechanic: {@code kind} is the code's implementation, {@code power} and {@code period} (seconds) its numbers. */
    public record Signature(String id, String kind, double power, int period, String text) {}

    /** The kinds of signature mechanic the code implements (a signatures.json entry naming another is refused). */
    public static final List<String> SIGNATURE_KINDS = List.of("shield_pulse", "thorns", "blood_pact", "enrage", "add_tithe", "blink_strike", "frost_aura", "scorch_aura", "venom_aura",
            "gravity_well", "echo_strike", "kite_ward", "mirror_guard", "mana_leech");

    /** The kinds of add (closed list: each maps to an entity type in {@code AddKinds}); none applies the Hunger effect. */
    public static final List<String> ADD_KINDS = List.of("skeleton", "zombie", "stray", "wither_skeleton", "vex", "spider", "cave_spider", "blaze", "witch", "silverfish", "drowned", "bogged",
            "living_armor", "endermite");

    /** The particles a lair action may draw with (each maps to a game particle in {@code LairEngine}). */
    public static final List<String> PARTICLES = List.of("smoke", "soul", "soul_fire_flame", "flame", "end_rod", "crit", "portal", "witch", "squid_ink", "sculk_soul", "cloud", "electric_spark",
            "snowflake", "happy_villager", "enchant", "crimson_spore");

    /** The damage categories a boss can resist. */
    public static final List<String> RESIST = List.of("fire", "freeze", "magic", "projectile", "explosion", "lightning", "poison", "physical");

    /** The creature types a profile may name. */
    public static final List<String> CREATURES = List.of("undead", "spirit", "fiend", "demon", "devil", "dragon", "construct", "aberration", "celestial", "humanoid", "fey", "elemental", "giant",
            "monstrosity", "plant", "beast", "hag", "vampire", "cultist");

    private static final String LAIRS = "/data/nusmp/mega/decennial/lairs.json", SIGNATURES = "/data/nusmp/mega/decennial/signatures.json";
    private static volatile Map<String, Lair> lairs;
    private static volatile Map<String, Signature> signatures;

    // ================================================================ lookups
    public static Map<String, Lair> lairs() {
        Map<String, Lair> m = lairs;
        if (m == null) {
            synchronized (DecennialVocab.class) {
                m = lairs;
                if (m == null) {
                    m = parseLairs(read(LAIRS));
                    lairs = m;
                }
            }
        }
        return m;
    }

    public static Map<String, Signature> signatures() {
        Map<String, Signature> m = signatures;
        if (m == null) {
            synchronized (DecennialVocab.class) {
                m = signatures;
                if (m == null) {
                    m = parseSignatures(read(SIGNATURES));
                    signatures = m;
                }
            }
        }
        return m;
    }

    public static Lair lair(String id) { return id == null ? null : lairs().get(id); }

    public static Signature signature(String id) { return id == null ? null : signatures().get(id); }

    // ================================================================ parsing (pure, strict, plain-sentence errors)
    public static Map<String, Lair> parseLairs(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        Map<String, Lair> out = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : root.entrySet()) {
            if (e.getKey().startsWith("_")) continue;
            String id = e.getKey();
            if (!id.matches("[a-z][a-z0-9_]{1,31}")) throw new IllegalArgumentException("lair id '" + id + "' is not lower-case words and underscores");
            JsonObject o = e.getValue().getAsJsonObject();
            Pattern pattern = enumOf(Pattern.class, str(o, "pattern", id), id);
            Effect effect = o.has("effect") ? enumOf(Effect.class, str(o, "effect", id), id) : Effect.NONE;
            String side = o.has("side") ? str(o, "side", id) : "any";
            if (!List.of("any", "holy", "evil").contains(side)) throw new IllegalArgumentException(id + ": side must be any, holy or evil");
            int count = (int) DecennialRules.clamp(num(o, "count", 1), 1, 8);
            double radius = DecennialRules.clamp(num(o, "radius", 3), 0.8, 20);
            int telegraph = (int) DecennialRules.clamp(num(o, "telegraph", 30), 16, 80);                       // at least 0.8 s of warning: everything the party must answer has an answer
            double share = DecennialRules.clamp(num(o, "share", 0.3), 0.0, DecennialRules.MAX_LAIR_SHARE);
            int duration = (int) DecennialRules.clamp(num(o, "duration", 0), 0, 400);
            int effectTicks = (int) DecennialRules.clamp(num(o, "effect_ticks", 40), 0, effect == Effect.BLIND ? 40 : 160);   // blindness two seconds at most (the brief's cap on blindness)
            String particle = o.has("particle") ? str(o, "particle", id) : "smoke";
            if (!PARTICLES.contains(particle)) throw new IllegalArgumentException(id + ": unknown particle '" + particle + "' (known: " + PARTICLES + ")");
            String zone = o.has("zone") ? str(o, "zone", id) : "";
            String text = str(o, "text", id);
            out.put(id, new Lair(id, side, pattern, effect, count, radius, telegraph, share, duration, effectTicks, particle, zone, text));
        }
        if (out.isEmpty()) throw new IllegalArgumentException("no lair actions in the table");
        return java.util.Collections.unmodifiableMap(out);
    }

    public static Map<String, Signature> parseSignatures(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        Map<String, Signature> out = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : root.entrySet()) {
            if (e.getKey().startsWith("_")) continue;
            String id = e.getKey();
            JsonObject o = e.getValue().getAsJsonObject();
            String kind = str(o, "kind", id);
            if (!SIGNATURE_KINDS.contains(kind)) throw new IllegalArgumentException(id + ": unknown signature kind '" + kind + "' (known: " + SIGNATURE_KINDS + ")");
            double power = DecennialRules.clamp(num(o, "power", 0.05), 0.0, 0.5);
            int period = (int) DecennialRules.clamp(num(o, "period", 12), 2, 120);
            out.put(id, new Signature(id, kind, power, period, str(o, "text", id)));
        }
        if (out.isEmpty()) throw new IllegalArgumentException("no signature mechanics in the table");
        return java.util.Collections.unmodifiableMap(out);
    }

    private static String read(String resource) {
        try (InputStream in = DecennialVocab.class.getResourceAsStream(resource)) {
            if (in == null) throw new IllegalStateException("missing resource " + resource);
            try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                StringBuilder b = new StringBuilder();
                char[] buf = new char[4096];
                int n;
                while ((n = r.read(buf)) > 0) b.append(buf, 0, n);
                return b.toString();
            }
        } catch (IOException e) {
            throw new IllegalStateException("cannot read " + resource + ": " + e.getMessage());
        }
    }

    private static String str(JsonObject o, String k, String id) {
        if (!o.has(k) || !o.get(k).isJsonPrimitive()) throw new IllegalArgumentException(id + ": missing text '" + k + "'");
        return o.get(k).getAsString();
    }

    private static double num(JsonObject o, String k, double dflt) { return o.has(k) ? o.get(k).getAsDouble() : dflt; }

    private static <E extends Enum<E>> E enumOf(Class<E> type, String name, String id) {
        try {
            return Enum.valueOf(type, name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(id + ": unknown " + type.getSimpleName().toLowerCase(Locale.ROOT) + " '" + name + "'");
        }
    }

    /** Every string of a JSON array of text (empty when absent). */
    static List<String> strings(JsonObject o, String k) {
        List<String> out = new ArrayList<>();
        if (o.has(k) && o.get(k).isJsonArray()) for (JsonElement el : (JsonArray) o.get(k)) out.add(el.getAsString());
        return List.copyOf(out);
    }
}
