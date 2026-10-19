package com.newuniverse.nusmp.relics;

import java.util.Locale;
import java.util.Map;

/**
 * STAND-IN (1.97.0, the new-weapons builder): the owner's damage table for relics by grade and weapon type, reduced to the two calls the new weapons need. Pure
 * (no Minecraft types). When the full class from the damage-table builder is merged it replaces this file; the two signatures are the same.
 * <pre>
 *   normal 20-30   special 35-55   rare 60-85   unique 90-115   legend 130-175   god 190-240   genesis 260-350
 * </pre>
 * The grade picks the range; the weapon type (a class name of weapon_classes.json) picks the place inside it.
 */
public final class GradeDamage {
    private GradeDamage() {}

    private static final int[][] RANGES = {{20, 30}, {35, 55}, {60, 85}, {90, 115}, {130, 175}, {190, 240}, {260, 350}};
    private static final String[] NAMES = {"normal", "special", "rare", "unique", "legend", "god", "genesis"};

    private static final Map<String, Double> FACTORS = Map.ofEntries(
            Map.entry("dagger", 0.00), Map.entry("knife", 0.00), Map.entry("wand", 0.05), Map.entry("rapier", 0.10), Map.entry("coilwhip", 0.15),
            Map.entry("whip", 0.20), Map.entry("shield", 0.20), Map.entry("shortsword", 0.30), Map.entry("sabre", 0.35), Map.entry("cleaver", 0.40),
            Map.entry("handaxe", 0.40), Map.entry("katana", 0.45), Map.entry("staff", 0.45), Map.entry("bow", 0.50), Map.entry("sword", 0.50),
            Map.entry("longsword", 0.50), Map.entry("crossbow", 0.55), Map.entry("club", 0.55), Map.entry("spear", 0.55), Map.entry("trident", 0.55),
            Map.entry("lance", 0.55), Map.entry("mace", 0.65), Map.entry("axe", 0.65), Map.entry("battleaxe", 0.70), Map.entry("crescent", 0.70),
            Map.entry("hammer", 0.75), Map.entry("glaive", 0.80), Map.entry("halberd", 0.85), Map.entry("dadao", 0.85), Map.entry("scythe", 0.90),
            Map.entry("greataxe", 0.90), Map.entry("greathammer", 1.00), Map.entry("greatsword", 1.00), Map.entry("item", 0.50), Map.entry("handitem", 0.50));

    private static int ordinal(String grade) {
        if (grade == null) return 0;
        String g = grade.trim().toLowerCase(Locale.ROOT);
        if (g.endsWith(" grade")) g = g.substring(0, g.length() - 6).trim();
        for (int i = 0; i < NAMES.length; i++) if (NAMES[i].equals(g)) return i;
        return 0;
    }

    /** {min, max} of the grade (a fresh array); an unknown grade is normal. */
    public static int[] range(String grade) { return RANGES[ordinal(grade)].clone(); }

    /** Total attack damage (the tooltip figure) of a weapon of this grade and type, always inside {@link #range}, in half points. */
    public static double damage(String grade, String weaponType) {
        int[] r = range(grade);
        Double f = weaponType == null ? null : FACTORS.get(weaponType.trim().toLowerCase(Locale.ROOT));
        double d = r[0] + (f == null ? 0.5 : f) * (r[1] - r[0]);
        return Math.max(r[0], Math.min(r[1], Math.round(d * 2.0) / 2.0));
    }
}
