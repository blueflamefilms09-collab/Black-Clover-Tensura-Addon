package com.newuniverse.nusmp.dragonballs;

import com.newuniverse.nusmp.skill.ResetRules;
import com.newuniverse.nusmp.skilltree.unique.LegacyIds;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Why: which skills a wish may teach, as pure functions with no game type, so the decisions are unit tested with stand-ins ({@code SkillRulesTest}). The tier of a skill is read from Tensura's own
 * {@code Skill.SkillType} (given here as its serialized name, so a test needs no Tensura class); a skill that has no such type (a spell, a battle-will art, a plain ManasCore skill) has no tier and is not
 * offered. The ceiling per dragon: Super Shenron reaches Unique and, while {@code dragonBalls.overpoweredUltimateSkills} is on, Ultimate; Shenron stops at the setting {@code overpoweredEarthSkillTier} (default
 * Extra). What is never offered: Intrinsic skills (they belong to a race), the inert stubs of removed skills, and this mod's own skills that a grant service of the mod manages (grimoire and its pages, weapon
 * skills, sin weapon skills, Shishou, Spirit Lord, Wizard King), because the service would take them back.
 */
public final class SkillRules {
    private SkillRules() {}

    /** The tiers a wish may teach, lowest first; the ordinal is the order the ceiling is compared in. */
    public enum Tier {
        RESISTANCE, COMMON, EXTRA, UNIQUE, ULTIMATE;

        public static Tier byName(String s) {
            if (s == null) return null;
            for (Tier t : values()) if (t.name().equalsIgnoreCase(s.trim())) return t;
            return null;
        }
    }

    /** The tier of a skill from the serialized name of Tensura's {@code Skill.SkillType} ("unique", "ultimate", ...). Null: no tier (intrinsic, or not a typed skill), so it is never offered. */
    public static Tier tierOf(String skillType) {
        if (skillType == null) return null;
        return switch (skillType.toLowerCase(Locale.ROOT)) {
            case "resistance" -> Tier.RESISTANCE;
            case "common" -> Tier.COMMON;
            case "extra" -> Tier.EXTRA;
            case "unique" -> Tier.UNIQUE;
            case "ultimate" -> Tier.ULTIMATE;
            default -> null;
        };
    }

    /** The highest tier a dragon may teach. Super: Ultimate (or Unique when {@code ultimates} is off). Earth: the configured tier, never above Super's and never Ultimate unless Ultimates are on. */
    public static Tier ceiling(BallSet set, Tier earthTier, boolean ultimates) {
        Tier top = ultimates ? Tier.ULTIMATE : Tier.UNIQUE;
        if (set == BallSet.SUPER) return top;
        Tier e = earthTier == null ? Tier.EXTRA : earthTier;
        return e.ordinal() > top.ordinal() ? top : e;
    }

    public static boolean allowed(Tier tier, Tier ceiling) { return tier != null && ceiling != null && tier.ordinal() <= ceiling.ordinal(); }

    /** Skills one skill wish teaches: Earth one, Super the setting (1..5). */
    public static int perWish(BallSet set, int configured) { return set == BallSet.SUPER ? Math.max(1, Math.min(5, configured)) : 1; }

    // ---------------------------------------------------------------- what is never taught
    /** The mod's own ids that a grant service manages (the service forgets or re-gives them), by registry path. */
    public static final List<String> MANAGED_PATHS = List.of("shishou", "lord_of_gluttonous_greed", "lord_of_slothful_weight", "lord_of_green_eyed_envy", "lord_of_empty_vanity", "lord_of_the_twin_rings",
            "seifort_holy_judgement", "spirit_lord", "wizard_king", "grimoire_magic");

    private static final Set<String> REMOVED = Set.copyOf(concat(LegacyIds.REMOVED_SKILLS, LegacyIds.RETIRED_TECHNIQUES, LegacyIds.REMOVED_TECHNIQUES));

    @SafeVarargs
    private static List<String> concat(List<String>... lists) {
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        for (List<String> l : lists) out.addAll(l);
        return out;
    }

    /** True when the id must never be taught: a removed skill's stub, a soul-tied or managed skill of this mod, a weapon skill ({@code ws_}). Other mods' ids are never blocked by path. */
    public static boolean blockedId(String id) {
        if (id == null) return true;
        int c = id.indexOf(':');
        String ns = c < 0 ? "" : id.substring(0, c), path = c < 0 ? id : id.substring(c + 1);
        if (!ResetRules.isOurs(ns)) return false;
        return REMOVED.contains(path) || ResetRules.soulTied(path) || path.startsWith("ws_") || MANAGED_PATHS.contains(path);
    }

    /** True when a class of this simple name is an inert placeholder (a removed or retired skill): never taught, whatever its id. */
    public static boolean stubClass(String simpleName) {
        if (simpleName == null) return false;
        String n = simpleName.toLowerCase(Locale.ROOT);
        return n.contains("legacy") || n.contains("retired") || n.contains("removed");
    }

    /** The whole decision for one skill: it has a tier at or below the ceiling and is not blocked. */
    public static boolean teachable(String id, String skillType, String className, Tier ceiling) {
        return !blockedId(id) && !stubClass(className) && allowed(tierOf(skillType), ceiling);
    }
}
