package com.newuniverse.nusmp.heroweapon.forms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: a form's numbers reach the wielder in two ways, and the rules for which way are easy to get subtly wrong: the attack damage and attack speed of the ACTIVE
 * form belong on the item (so they only count in the main hand), everything else is an attribute of the player; the permanent bonuses of finished forms are always
 * on the player, added together across forms. This class turns forms into the list of modifiers the game classes must apply, with no Minecraft types, so the choice
 * of target, the operation (movement speed is a share of the base) and the summing are tested once. {@link FormLoadout} then only maps a key to an attribute.
 */
public final class FormStats {
    private FormStats() {}

    /** Id path prefix of the modifiers of the active form ({@code nusmp:hero_form_<key>}), applied only while the weapon is summoned. */
    public static final String FORM_PREFIX = "hero_form_";
    /** Id path prefix of the permanent bonuses of finished forms ({@code nusmp:hero_bonus_<key>}), applied while the bond exists. */
    public static final String BONUS_PREFIX = "hero_bonus_";

    /** Where a modifier goes. */
    public enum Target { STACK, PLAYER }

    /** One modifier to apply: the stat key, the amount, where it goes and whether it multiplies the base value (movement speed) or adds to it. */
    public record Mod(String key, double amount, Target target, boolean multiplied) {}

    /** True for the two keys that belong on the item stack while the form is active. */
    public static boolean onStack(String key) { return "attack_damage".equals(key) || "attack_speed".equals(key); }

    /** True for the key whose value is a share of the base (0.05 is plus five percent). */
    public static boolean multiplied(String key) { return "movement_speed".equals(key); }

    /** The modifier id path of a key under a prefix, e.g. {@code hero_form_armor}. */
    public static String path(String prefix, String key) { return prefix + key; }

    /** The modifiers of the ACTIVE form's {@code stats}, in {@link HeroForm#STAT_KEYS} order; zeros are left out. */
    public static List<Mod> formMods(HeroForm form) {
        List<Mod> out = new ArrayList<>();
        for (String key : HeroForm.STAT_KEYS) {
            double v = form.stat(key);
            if (Math.abs(v) < 1e-9) continue;
            out.add(new Mod(key, v, onStack(key) ? Target.STACK : Target.PLAYER, multiplied(key)));
        }
        return out;
    }

    /** The sum of the {@code abilityBonus} of every form in {@code granted} (the finished ones), per key; zeros are left out. */
    public static Map<String, Double> bonusTotals(Collection<HeroForm> forms, Collection<String> granted) {
        Map<String, Double> sum = new LinkedHashMap<>();
        for (String key : HeroForm.STAT_KEYS) sum.put(key, 0.0);
        for (HeroForm f : forms) {
            if (!granted.contains(f.id())) continue;
            for (Map.Entry<String, Double> e : f.abilityBonus().entrySet()) sum.merge(e.getKey(), e.getValue(), Double::sum);
        }
        sum.values().removeIf(v -> Math.abs(v) < 1e-9);
        return sum;
    }

    /** The permanent bonuses as modifiers; all of them go on the player. */
    public static List<Mod> bonusMods(Collection<HeroForm> forms, Collection<String> granted) {
        List<Mod> out = new ArrayList<>();
        Map<String, Double> totals = bonusTotals(forms, granted);
        for (String key : HeroForm.STAT_KEYS) {
            Double v = totals.get(key);
            if (v != null) out.add(new Mod(key, v, Target.PLAYER, multiplied(key)));
        }
        return out;
    }
}
