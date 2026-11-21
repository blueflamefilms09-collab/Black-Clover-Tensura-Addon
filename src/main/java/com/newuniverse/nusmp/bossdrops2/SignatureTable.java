package com.newuniverse.nusmp.bossdrops2;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Why: what a boss pays besides its old drops is DATA ({@code data/anime_multiverse/boss_signature/<entity path>.json}), so an operator or a data pack changes it without code. This is the pure half (no
 * Minecraft type, unit tested): it reads one file into a {@link Table} and refuses a bad one with a message that names the field. Bounds: at most {@link #MAX_EXTRAS} personal rows, counts of at most
 * {@link #MAX_COUNT}, at most {@link #MAX_BOON} effects of at most {@link #MAX_SECONDS} seconds and amplifier {@link #MAX_AMPLIFIER}, so a file can never make a kill pay an unbounded list or a boon of days.
 * <pre>
 * { "boss": "meliodas", "tier": "apex",
 *   "signature": { "chance": 1.0, "min": 1, "max": 1, "boon": [ { "effect": "minecraft:strength", "seconds": 900, "amplifier": 1 } ] },
 *   "extras": [ { "item": "anime_multiverse:magic_stone", "chance": 1.0, "min": 12, "max": 24 } ],
 *   "one_of_one": { "relic": "hellblaze_the_demon_sin", "echo_chance": 0.2, "echo_boon": [ ... ] } }
 * </pre>
 * {@code one_of_one} is optional: a relic id of the mod (an existing unique relic, issued through the God of Order's ledger, so it exists once per world) and the chance that every OTHER fighter
 * receives an echo of it.
 */
public final class SignatureTable {
    private SignatureTable() {}

    public static final int MAX_EXTRAS = 6, MAX_COUNT = 64, MAX_BOON = 4, MAX_SECONDS = 3600, MIN_SECONDS = 10, MAX_AMPLIFIER = 3;
    private static final Pattern ITEM_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Pattern PATH_ID = Pattern.compile("[a-z0-9_]+");

    /** Boss strength classes, weakest first: they decide the rarity colour of the signature and the size of the rows. */
    public enum Tier {
        LESSER, MID, HIGH, APEX;

        public static Tier parse(String s) {
            try {
                return valueOf(s.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException | NullPointerException bad) {
                throw new IllegalArgumentException("bad tier '" + s + "' (lesser, mid, high, apex)");
            }
        }

        public String id() { return name().toLowerCase(Locale.ROOT); }
    }

    public record Boon(String effect, int seconds, int amplifier) {}

    public record Row(String item, double chance, int min, int max) {}

    public record Signature(double chance, int min, int max, List<Boon> boon) {}

    public record Unique(String relic, double echoChance, List<Boon> echoBoon) {}

    /** {@code unique} is null for a boss without a one-of-one. */
    public record Table(String boss, Tier tier, Signature signature, List<Row> extras, Unique unique) {
        public boolean hasUnique() { return unique != null; }
    }

    /** Reads one file; {@code IllegalArgumentException} (with the reason) when it is not valid. */
    public static Table parse(JsonObject o) {
        if (o == null || !o.has("boss") || !o.get("boss").isJsonPrimitive()) throw new IllegalArgumentException("missing 'boss'");
        String boss = o.get("boss").getAsString();
        if (!PATH_ID.matcher(boss).matches()) throw new IllegalArgumentException("bad boss id '" + boss + "'");
        Tier tier = Tier.parse(o.has("tier") ? o.get("tier").getAsString() : null);
        JsonObject sig = obj(o, "signature", boss);
        double chance = chance(sig, boss + " signature");
        int[] count = counts(sig, boss + " signature");
        Signature signature = new Signature(chance, count[0], count[1], boon(sig, "boon", boss + " signature", true));
        List<Row> extras = new ArrayList<>();
        if (o.has("extras")) {
            if (!o.get("extras").isJsonArray()) throw new IllegalArgumentException(boss + ": 'extras' is not a list");
            JsonArray arr = o.getAsJsonArray("extras");
            if (arr.size() > MAX_EXTRAS) throw new IllegalArgumentException(boss + ": at most " + MAX_EXTRAS + " extras");
            for (int i = 0; i < arr.size(); i++) {
                JsonElement e = arr.get(i);
                String at = boss + " extra " + i;
                if (!e.isJsonObject()) throw new IllegalArgumentException(at + ": not an object");
                JsonObject r = e.getAsJsonObject();
                String item = r.has("item") ? r.get("item").getAsString() : "";
                if (!ITEM_ID.matcher(item).matches()) throw new IllegalArgumentException(at + ": bad item id '" + item + "'");
                int[] c = counts(r, at);
                extras.add(new Row(item, chance(r, at), c[0], c[1]));
            }
        }
        Unique unique = null;
        if (o.has("one_of_one")) {
            JsonObject u = obj(o, "one_of_one", boss);
            String relic = u.has("relic") ? u.get("relic").getAsString() : "";
            if (!PATH_ID.matcher(relic).matches()) throw new IllegalArgumentException(boss + " one_of_one: bad relic id '" + relic + "'");
            double echo = u.has("echo_chance") ? u.get("echo_chance").getAsDouble() : 0.0;
            if (!(echo >= 0.0 && echo <= 1.0)) throw new IllegalArgumentException(boss + " one_of_one: echo_chance must be in [0, 1]");
            unique = new Unique(relic, echo, boon(u, "echo_boon", boss + " one_of_one", true));
        }
        return new Table(boss, tier, signature, List.copyOf(extras), unique);
    }

    private static JsonObject obj(JsonObject o, String key, String boss) {
        if (!o.has(key) || !o.get(key).isJsonObject()) throw new IllegalArgumentException(boss + ": missing '" + key + "'");
        return o.getAsJsonObject(key);
    }

    private static double chance(JsonObject r, String at) {
        double c = r.has("chance") ? r.get("chance").getAsDouble() : 1.0;
        if (!(c > 0.0 && c <= 1.0)) throw new IllegalArgumentException(at + ": chance must be in (0, 1]");
        return c;
    }

    private static int[] counts(JsonObject r, String at) {
        int min = r.has("min") ? r.get("min").getAsInt() : 1, max = r.has("max") ? r.get("max").getAsInt() : min;
        if (min < 1 || max < min || max > MAX_COUNT) throw new IllegalArgumentException(at + ": need 1 <= min <= max <= " + MAX_COUNT);
        return new int[]{min, max};
    }

    private static List<Boon> boon(JsonObject parent, String key, String at, boolean required) {
        if (!parent.has(key)) {
            if (required) throw new IllegalArgumentException(at + ": missing '" + key + "'");
            return List.of();
        }
        if (!parent.get(key).isJsonArray()) throw new IllegalArgumentException(at + ": '" + key + "' is not a list");
        JsonArray arr = parent.getAsJsonArray(key);
        if (arr.isEmpty() || arr.size() > MAX_BOON) throw new IllegalArgumentException(at + ": " + key + " needs 1.." + MAX_BOON + " effects");
        List<Boon> out = new ArrayList<>();
        for (JsonElement e : arr) {
            if (!e.isJsonObject()) throw new IllegalArgumentException(at + ": a boon entry is not an object");
            JsonObject b = e.getAsJsonObject();
            String effect = b.has("effect") ? b.get("effect").getAsString() : "";
            if (!ITEM_ID.matcher(effect).matches()) throw new IllegalArgumentException(at + ": bad effect id '" + effect + "'");
            int seconds = b.has("seconds") ? b.get("seconds").getAsInt() : 0, amp = b.has("amplifier") ? b.get("amplifier").getAsInt() : 0;
            if (seconds < MIN_SECONDS || seconds > MAX_SECONDS) throw new IllegalArgumentException(at + ": seconds must be " + MIN_SECONDS + ".." + MAX_SECONDS);
            if (amp < 0 || amp > MAX_AMPLIFIER) throw new IllegalArgumentException(at + ": amplifier must be 0.." + MAX_AMPLIFIER);
            out.add(new Boon(effect, seconds, amp));
        }
        return List.copyOf(out);
    }
}
