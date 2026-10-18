package com.newuniverse.nusmp.heroweapon.forms;

import java.util.Locale;

/**
 * Why: the Weapon Book, the chat lines and the commands all print the same few things (a stat with its sign and unit, a slot, a duration, an id turned into words),
 * and a number printed two ways in two places looks like a bug. The wording rules are pure string functions here, with a test, in plain English; the screen
 * wraps them in translatable components with these same strings as the fallback, so a missing lang entry still shows readable text.
 */
public final class FormText {
    private FormText() {}

    /** {@code leaf_shield} becomes {@code Leaf Shield}; {@code minecraft:oak_leaves} becomes {@code Oak Leaves}. */
    public static String pretty(String id) {
        if (id == null || id.isEmpty()) return "";
        String path = id.substring(id.indexOf(':') + 1);
        int slash = path.lastIndexOf('/');
        if (slash >= 0 && slash < path.length() - 1) path = path.substring(slash + 1);
        StringBuilder b = new StringBuilder();
        boolean up = true;
        for (char c : path.toCharArray()) {
            if (c == '_' || c == '/' || c == '-') {
                b.append(' ');
                up = true;
            } else {
                b.append(up ? Character.toUpperCase(c) : c);
                up = false;
            }
        }
        return b.toString();
    }

    /** The English name of a stat key. */
    public static String statLabel(String key) {
        return switch (key) {
            case "attack_damage" -> "Attack damage";
            case "attack_speed" -> "Attack speed";
            case "armor" -> "Armor";
            case "armor_toughness" -> "Armor toughness";
            case "max_health" -> "Max health";
            case "movement_speed" -> "Movement speed";
            case "knockback_resistance" -> "Knockback resistance";
            case "reach" -> "Reach";
            case "luck" -> "Luck";
            default -> pretty(key);
        };
    }

    /** A stat value with its sign and unit: {@code +2}, {@code -0.4}, {@code +4%} (movement speed and knockback resistance are shares). */
    public static String statValue(String key, double v) {
        boolean percent = "movement_speed".equals(key) || "knockback_resistance".equals(key);
        double shown = percent ? v * 100.0 : v;
        return (shown >= 0 ? "+" : "-") + number(Math.abs(shown)) + (percent ? "%" : "");
    }

    /** {@code Armor +2}. */
    public static String statLine(String key, double v) { return statLabel(key) + " " + statValue(key, v); }

    /** A number with at most two decimals and no trailing zeros: {@code 2}, {@code 0.25}. */
    public static String number(double v) {
        if (Math.abs(v - Math.rint(v)) < 1e-9) return String.valueOf((long) Math.rint(v));
        String s = String.format(Locale.ROOT, "%.2f", v);
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == '0') end--;
        if (end > 0 && s.charAt(end - 1) == '.') end--;
        return s.substring(0, end);
    }

    /** The English label of a trigger slot word. */
    public static String slotLabel(String slot) {
        return switch (slot) {
            case "use" -> "Use";
            case "sneak" -> "Sneak + Use";
            case "key1" -> "Key 1";
            case "key2" -> "Key 2";
            case "key3" -> "Key 3";
            default -> pretty(slot);
        };
    }

    /** Ticks as seconds: {@code 8 s}, {@code 1.5 s}. */
    public static String seconds(int ticks) { return number(ticks / 20.0) + " s"; }

    /** A share as a percentage: 0.02 becomes {@code 2%}. */
    public static String percent(double share) { return number(share * 100.0) + "%"; }

    /** The label of a tag requirement: {@code c:ingots/iron} becomes {@code Any Iron}. */
    public static String tagLabel(String tagId) { return "Any " + pretty(tagId); }
}
