package com.newuniverse.nusmp.boss.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.Map;

/**
 * Why: Leon study fix 9. The numbers of a boss are Java constants; this is the pure half (no Minecraft type, unit tested) of letting a data pack override them
 * without a rebuild: {@code data/anime_multiverse/arc_bosses/<boss id>.json}.
 * <pre>
 * { "health": 4200, "attack": 30, "speed": 0.30, "armor": 18, "move_cooldown_scale": 0.9 }
 * </pre>
 * Every key is optional, but at least one must be there; a key must be a finite number (anything else, an unknown key or an empty file makes the whole file invalid, and an invalid file
 * changes nothing). Keys that start with an underscore are comments. A number outside its range is clamped, never rejected: health {@link #HEALTH_MIN}..{@link #HEALTH_MAX},
 * attack 0..{@link #ATTACK_MAX}, speed {@link #SPEED_MIN}..{@link #SPEED_MAX}, armor 0..{@link #ARMOR_MAX}, move cooldown scale {@link #COOLDOWN_MIN}..{@link #COOLDOWN_MAX}.
 * The Java number stays the default and the fallback: the file replaces it only for the keys it names.
 */
public final class BossDataTable {
    private BossDataTable() {}

    public static final double HEALTH_MIN = 1, HEALTH_MAX = 1_000_000, ATTACK_MAX = 1_000, SPEED_MIN = 0.01, SPEED_MAX = 1.0, ARMOR_MAX = 30, COOLDOWN_MIN = 0.25, COOLDOWN_MAX = 4.0;
    /** Most warnings one data reload writes to the log; the rest is one summary line. */
    public static final int MAX_LOGGED = 8;

    /** The numbers a file sets (clamped); a null field is not overridden. */
    public record Override(Double health, Double attack, Double speed, Double armor, Double moveCooldownScale) {}

    private static final Map<String, double[]> RANGES = Map.of(
            "health", new double[]{HEALTH_MIN, HEALTH_MAX}, "attack", new double[]{0, ATTACK_MAX}, "speed", new double[]{SPEED_MIN, SPEED_MAX},
            "armor", new double[]{0, ARMOR_MAX}, "move_cooldown_scale", new double[]{COOLDOWN_MIN, COOLDOWN_MAX});

    public static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

    /** Reads one file; {@code IllegalArgumentException} (with the reason) when it is not valid. */
    public static Override parse(JsonObject o) {
        if (o == null) throw new IllegalArgumentException("not an object");
        Double[] v = new Double[5];
        String[] keys = {"health", "attack", "speed", "armor", "move_cooldown_scale"};
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            String k = e.getKey();
            if (k.startsWith("_")) continue;
            int i = -1;
            for (int j = 0; j < keys.length; j++) if (keys[j].equals(k)) i = j;
            if (i < 0) throw new IllegalArgumentException("unknown key '" + k + "'");
            JsonElement x = e.getValue();
            if (!x.isJsonPrimitive() || !((JsonPrimitive) x).isNumber()) throw new IllegalArgumentException("'" + k + "' must be a number");
            double d = x.getAsDouble();
            if (Double.isNaN(d) || Double.isInfinite(d)) throw new IllegalArgumentException("'" + k + "' must be finite");
            double[] r = RANGES.get(k);
            v[i] = clamp(d, r[0], r[1]);
        }
        if (v[0] == null && v[1] == null && v[2] == null && v[3] == null && v[4] == null) throw new IllegalArgumentException("no value set");
        return new Override(v[0], v[1], v[2], v[3], v[4]);
    }

    /** The factor that turns the Java default 'base' into the file's number: 1 when the file does not set it or the default is not positive. */
    public static double factor(Double wanted, double base) {
        if (wanted == null || !(base > 0.0)) return 1.0;
        return wanted / base;
    }

    /** A valid boss id as a file name: snake_case, no folder. */
    public static boolean validId(String id) { return id != null && id.matches("[a-z][a-z0-9_]*"); }
}
