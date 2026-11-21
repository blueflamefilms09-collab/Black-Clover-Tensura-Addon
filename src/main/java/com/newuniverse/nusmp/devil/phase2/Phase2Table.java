package com.newuniverse.nusmp.devil.phase2;

import com.newuniverse.nusmp.core.CatchLog;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.devil.DevilTier;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Why: the numbers of phase 2 that belong to a rank or to a devil are DATA, not code: the EP each rank asks for (Supreme about one billion, a tenth of that per rank down), the base summon chance of each rank,
 * how much a relic of the devil adds, and per devil: does it fight before it agrees (the trial), how likely is it to turn on the circle, and does it ask its own EP. The file is
 * {@code assets/anime_multiverse/devil_phase2.json}; a missing or damaged file falls back to the same defaults in code, so the rules never run without a table. Every number is clamped when parsed.
 * Pure (Gson only), unit tested by {@code Phase2TableTest}. The tables are instance data of one immutable record; the only static is the cached default.
 */
public final class Phase2Table {
    /** One devil's own data: whether it fights, its chance to turn on the circle (0 to 1) and an EP of its own (0 = the rank's). */
    public record DevilData(boolean trial, double betray, double ep) {
        public static final DevilData NONE = new DevilData(false, 0.0, 0.0);
    }

    private final double[] ep = new double[DevilTier.values().length], chance = new double[DevilTier.values().length];
    private final double relicBonus, minChance;
    private final Map<String, DevilData> devils;

    private Phase2Table(double[] ep, double[] chance, double relicBonus, double minChance, Map<String, DevilData> devils) {
        System.arraycopy(ep, 0, this.ep, 0, this.ep.length);
        System.arraycopy(chance, 0, this.chance, 0, this.chance.length);
        this.relicBonus = relicBonus;
        this.minChance = minChance;
        this.devils = Map.copyOf(devils);
    }

    /** The defaults: Supreme 1 000 000 000 EP, each rank down a tenth; chances 35 %, 55 %, 75 %, 90 %; a relic adds 35 points; never under 5 %. */
    public static Phase2Table defaults() {
        double[] ep = new double[DevilTier.values().length], ch = new double[DevilTier.values().length];
        ep[DevilTier.LOW.ordinal()] = 1.0E6; ep[DevilTier.MID.ordinal()] = 1.0E7; ep[DevilTier.HIGH.ordinal()] = 1.0E8; ep[DevilTier.SUPREME.ordinal()] = 1.0E9;
        ch[DevilTier.LOW.ordinal()] = 0.90; ch[DevilTier.MID.ordinal()] = 0.75; ch[DevilTier.HIGH.ordinal()] = 0.55; ch[DevilTier.SUPREME.ordinal()] = 0.35;
        return new Phase2Table(ep, ch, 0.35, 0.05, Map.of());
    }

    public static Phase2Table parse(JsonObject root) {
        Phase2Table d = defaults();
        if (root == null) return d;
        double[] ep = d.ep.clone(), ch = d.chance.clone();
        read(root, "ep", ep, 0, Phase2Rules.MAX_EP);
        read(root, "chance", ch, 0, 1);
        Map<String, DevilData> devils = new HashMap<>();
        if (root.has("devils") && root.get("devils").isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("devils").entrySet()) {
                if (!e.getValue().isJsonObject() || e.getKey().isEmpty() || e.getKey().length() > 64 || devils.size() >= 256) continue;
                JsonObject o = e.getValue().getAsJsonObject();
                devils.put(e.getKey().toLowerCase(Locale.ROOT), new DevilData(bool(o, "trial"), num(o, "betray", 0, 1), num(o, "ep", 0, Phase2Rules.MAX_EP)));
            }
        }
        return new Phase2Table(ep, ch, num(root, "relicBonus", 0, 1, d.relicBonus), num(root, "minChance", 0, 1, d.minChance), devils);
    }

    private static void read(JsonObject root, String key, double[] into, double lo, double hi) {
        if (!root.has(key) || !root.get(key).isJsonObject()) return;
        JsonObject o = root.getAsJsonObject(key);
        for (DevilTier t : DevilTier.values()) if (t != DevilTier.NONE && o.has(t.key())) into[t.ordinal()] = num(o, t.key(), lo, hi, into[t.ordinal()]);
    }

    private static boolean bool(JsonObject o, String k) { return o.has(k) && o.get(k).isJsonPrimitive() && o.get(k).getAsJsonPrimitive().isBoolean() && o.get(k).getAsBoolean(); }

    private static double num(JsonObject o, String k, double lo, double hi) { return num(o, k, lo, hi, 0.0); }

    private static double num(JsonObject o, String k, double lo, double hi, double def) {
        try {
            return o.has(k) && o.get(k).isJsonPrimitive() ? Phase2Rules.clamp(o.get(k).getAsDouble(), lo, hi) : def;
        } catch (RuntimeException notANumber) {
            return def;
        }
    }

    private static volatile Phase2Table cached;

    /** The table the game uses (read once from the jar). */
    public static Phase2Table get() {
        Phase2Table t = cached;
        if (t != null) return t;
        try (InputStream in = Phase2Table.class.getResourceAsStream("/assets/anime_multiverse/devil_phase2.json")) {
            t = in == null ? defaults() : parse(JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject());
        } catch (Exception unreadable) {
            CatchLog.once("devil_phase2.json", unreadable);
            t = defaults();
        }
        cached = t;
        return t;
    }

    public double tierEp(DevilTier t) { return t == null ? 0 : ep[t.ordinal()]; }

    public double tierChance(DevilTier t) { return t == null ? 0 : chance[t.ordinal()]; }

    public double relicBonus() { return relicBonus; }

    public double minChance() { return minChance; }

    public DevilData devil(String id) { return id == null ? DevilData.NONE : devils.getOrDefault(id.toLowerCase(Locale.ROOT), DevilData.NONE); }

    /** The EP this devil asks (before the server's scale): its own number or its rank's. */
    public double requiredEp(String id, DevilTier tier, double scale) { return Phase2Rules.requiredEp(devil(id).ep(), tierEp(tier), scale); }

    /** The chance the ritual of this devil succeeds. */
    public double summonChance(DevilTier tier, boolean relic) { return Phase2Rules.chance(tierChance(tier), relic, relicBonus, minChance); }

    public int devilCount() { return devils.size(); }
}
