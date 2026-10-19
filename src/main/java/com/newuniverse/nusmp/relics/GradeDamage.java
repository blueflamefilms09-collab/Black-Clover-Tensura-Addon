package com.newuniverse.nusmp.relics;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.client.relics.item.WeaponClasses;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The base attack damage of a weapon by grade and weapon type (docs/grade_damage.md). Pure: no Minecraft types, so it is unit-tested.
 * <p>
 * Each grade owns a disjoint range (normal 20-30, special 35-55, rare 60-85, unique 90-115, legend 130-175, god 190-240, genesis 260-350).
 * The weapon type picks the place inside the range from {@link #factor}: light, fast daggers and rapiers sit low (they make it up in attack speed),
 * swords and spears in the middle, greatswords, hammers, scythes and halberds high; bows, crossbows and staves use their own ranged scale.
 * The type names are the class names of {@code assets/nusmp/weapon_classes.json} plus the plain Tensura names (sword, axe, lance ...).
 */
public final class GradeDamage {
    private GradeDamage() {}

    private static final int[][] RANGES = {{20, 30}, {35, 55}, {60, 85}, {90, 115}, {130, 175}, {190, 240}, {260, 350}};
    private static final String[] NAMES = {"normal", "special", "rare", "unique", "legend", "god", "genesis"};

    /** Position inside the range (0 = minimum, 1 = maximum) by weapon type. */
    private static final Map<String, Double> FACTORS = Map.ofEntries(
            Map.entry("dagger", 0.00), Map.entry("knife", 0.00), Map.entry("wand", 0.05), Map.entry("rapier", 0.10), Map.entry("coilwhip", 0.15),
            Map.entry("whip", 0.20), Map.entry("shield", 0.20), Map.entry("shortsword", 0.30), Map.entry("sabre", 0.35), Map.entry("cleaver", 0.40),
            Map.entry("handaxe", 0.40), Map.entry("katana", 0.45), Map.entry("staff", 0.45), Map.entry("bow", 0.50), Map.entry("sword", 0.50),
            Map.entry("longsword", 0.50), Map.entry("crossbow", 0.55), Map.entry("club", 0.55), Map.entry("spear", 0.55), Map.entry("trident", 0.55),
            Map.entry("lance", 0.55), Map.entry("mace", 0.65), Map.entry("axe", 0.65), Map.entry("battleaxe", 0.70), Map.entry("crescent", 0.70),
            Map.entry("hammer", 0.75), Map.entry("glaive", 0.80), Map.entry("halberd", 0.85), Map.entry("dadao", 0.85), Map.entry("scythe", 0.90),
            Map.entry("greataxe", 0.90), Map.entry("greathammer", 1.00), Map.entry("greatsword", 1.00),
            Map.entry("item", 0.50), Map.entry("handitem", 0.50));

    /** The grade's ordinal 0..6 (normal, special, rare, unique, legend, god, genesis); "common" is normal; any case. Unknown names give normal. */
    public static int ordinal(String grade) {
        if (grade == null) return 0;
        String g = grade.trim().toLowerCase(Locale.ROOT);
        if (g.endsWith(" grade")) g = g.substring(0, g.length() - 6).trim();
        if (g.equals("common")) return 0;
        for (int i = 0; i < NAMES.length; i++) if (NAMES[i].equals(g)) return i;
        return 0;
    }

    /** {min, max} of the grade (a fresh array). */
    public static int[] range(String grade) { return RANGES[ordinal(grade)].clone(); }

    /** {min, max} of the grade by ordinal 0..6 (clamped). */
    public static int[] range(int ordinal) { return RANGES[Math.max(0, Math.min(6, ordinal))].clone(); }

    /** The place (0..1) of a weapon type inside the range; an unknown type is the middle. */
    public static double factor(String weaponType) {
        if (weaponType == null) return 0.5;
        Double f = FACTORS.get(weaponType.trim().toLowerCase(Locale.ROOT));
        return f == null ? 0.5 : f;
    }

    /** Total attack damage (the tooltip figure) of a weapon of this grade and type, always inside {@link #range}; rounded to half points. */
    public static double damage(String grade, String weaponType) { return damage(ordinal(grade), weaponType); }

    public static double damage(int ordinal, String weaponType) {
        int[] r = range(ordinal);
        double d = r[0] + factor(weaponType) * (r[1] - r[0]);
        return Math.max(r[0], Math.min(r[1], Math.round(d * 2.0) / 2.0));
    }

    /** True if the damage lies inside the grade's range. */
    public static boolean inRange(String grade, double damage) {
        int[] r = range(grade);
        return damage >= r[0] && damage <= r[1];
    }

    /** The damage forced into the range of the grade ordinal. */
    public static double clamp(int ordinal, double damage) {
        int[] r = range(ordinal);
        return Math.max(r[0], Math.min(r[1], damage));
    }

    /**
     * Tier ascension: the damage of a weapon of base grade {@code baseOrdinal}, once raised to {@code effectiveOrdinal}. It keeps the weapon's
     * place inside the range and lands inside the effective grade's range (an unraised weapon is returned untouched).
     */
    public static double raised(int baseOrdinal, int effectiveOrdinal, double baseDamage, String weaponType) {
        if (effectiveOrdinal <= baseOrdinal) return baseDamage;
        int[] b = range(baseOrdinal), e = range(effectiveOrdinal);
        double t = (baseDamage - b[0]) / (double) (b[1] - b[0]);
        if (Double.isNaN(t)) t = factor(weaponType);
        t = Math.max(0.0, Math.min(1.0, t));
        return clamp(effectiveOrdinal, e[0] + t * (e[1] - e[0]));
    }

    /** Elsdocia is the one exception (400 base): it keeps its own number. */
    public static boolean exemptId(String path) { return path != null && path.equals("elsdocia"); }

    // ---------------------------------------------------------------- weapon type of an item id
    private static volatile WeaponClasses classes;

    private static WeaponClasses classes() {
        WeaponClasses c = classes;
        if (c != null) return c;
        Map<String, String> ids = new HashMap<>();
        try (InputStream in = GradeDamage.class.getResourceAsStream("/assets/nusmp/weapon_classes.json")) {
            if (in != null) {
                JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                if (o.has("weapons")) for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("weapons").entrySet()) ids.put(e.getKey(), e.getValue().getAsString());
            }
        } catch (Exception ignored) {
            // the keyword guess still works
        }
        c = new WeaponClasses(null, ids);
        classes = c;
        return c;
    }

    /** The weapon type (a class name of weapon_classes.json) of a registry path: the reviewed table first, else the keyword guess. */
    public static String typeOfId(String path) {
        return path == null ? "item" : classes().className(path);
    }
}
