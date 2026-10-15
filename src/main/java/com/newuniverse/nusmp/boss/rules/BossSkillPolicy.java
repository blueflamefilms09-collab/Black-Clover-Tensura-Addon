package com.newuniverse.nusmp.boss.rules;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

/**
 * Why: the single, pure source of truth for "may a boss own or use this skill?". The owner's rule is two sentences: a boss never heals
 * itself through a skill (the regeneration family), and a boss never uses a Unique or an Ultimate skill (it fights with standard skills,
 * innate traits and heavy spellcasting). Both halves live here, with no Minecraft types, so every enforcement point (the grant path, the
 * sweep, the activation veto, the Usurper lease, the Tower library) asks the same question and a JUnit test can pin the answer. The glue
 * ({@link BossSkillRules}) only turns a skill object into the two strings this class needs: the registry id and the category.
 * <p>
 * Why words and not an id list for regeneration: Tensura names the family inconsistently ({@code self_regeneration},
 * {@code ultraspeed_regeneration}, {@code infinite_regeneration}, the effect {@code instant_regeneration}, {@code full_recovery},
 * {@code healing_rain}), and other mods add their own; a word match catches the next one too. The word list is extensible from the
 * server config as a comma separated string (a plain {@code define}, no list type). Why unique/ultimate by category and not by id:
 * the category is what the skill IS in Tensura's own model (Skill.SkillType), so a skill added by a later Tensura version or by an
 * addon is covered without touching this class; the known-id table is only the fail-closed net for a skill whose class cannot be read.
 */
public final class BossSkillPolicy {
    private BossSkillPolicy() {}

    /** The switches and the extra words, as the config hands them over. */
    public record Settings(boolean stripRegeneration, boolean banUniqueUltimate, String extraWords) {
        public static final Settings DEFAULT = new Settings(true, true, DEFAULT_WORDS);
        public Settings {
            extraWords = extraWords == null ? "" : extraWords;
        }
    }

    /** The default value of the config's {@code forbiddenWords}. */
    public static final String DEFAULT_WORDS = "regenerat,heal,recover,revive,resurrect";

    /** Words that are always part of the regeneration family, whatever the config says (a config can add words, never lose these). */
    private static final List<String> BUILT_IN_WORDS = List.of("regenerat", "heal", "recover", "revive", "resurrect",
            "second_wind", "secondwind", "absorb_heal", "lifesteal", "life_steal", "rejuvenat");

    /** The real ids of the family (documentation and tests; the words already catch every one). */
    public static final List<String> REGENERATION_IDS = List.of("tensura:self_regeneration", "tensura:ultraspeed_regeneration",
            "tensura:ultra_speed_regeneration", "tensura:infinite_regeneration", "tensura:instant_regeneration", "tensura:full_recovery",
            "tensura:recovery", "tensura:healer", "tensura:healing", "tensura:healing_rain", "tensura:healthcare", "minecraft:regeneration");

    /** Effects that heal without a word of the family in their id. */
    private static final Set<String> EXTRA_EFFECT_IDS = Set.of("minecraft:instant_health");

    /**
     * The Unique skills of Tensura 2.0.1.3 (and the Ultimate ones that share the list): the fail-closed net used only when a Tensura
     * skill's category cannot be read. The category string is the normal way.
     */
    private static final Set<String> KNOWN_UNIQUE_OR_ULTIMATE = Set.of("absolute_severance", "analyst", "anti_skill", "berserk", "berserker",
            "bewilder", "chef", "chosen_one", "commander", "cook", "creator", "degenerate", "divine_berserker", "engorger", "envy", "falsifier",
            "fighter", "fusionist", "gluttony", "godly_craftsman", "gourmand", "gourmet", "great_sage", "greed", "guardian", "healer",
            "infinity_prison", "lust", "martial_master", "mathematician", "merciless", "murderer", "musician", "observer", "oppressor",
            "predator", "pride", "reaper", "reflector", "researcher", "reverser", "royal_beast", "seeker", "seer", "severer", "shadow_striker",
            "sloth", "sniper", "spearhead", "starved", "suppressor", "survivor", "thrower", "traveler", "tuner", "unyielding", "usurper",
            "villain", "wrath", "raphael", "beelzebuth", "azathoth", "ciel", "uriel", "michael", "gabriel", "faust", "shiva", "belial", "mammon",
            "leviathan", "asmodeus", "satan", "lucifer", "belphegor", "rimuru", "vishnu");

    /** The category strings the glue sends. Anything else (including null) is "unknown". */
    public static final String RESISTANCE = "resistance", INTRINSIC = "intrinsic", COMMON = "common", EXTRA = "extra", UNIQUE = "unique",
            ULTIMATE = "ultimate", MAGIC = "magic", BATTLEWILL = "battlewill", OTHER = "other", UNKNOWN = "unknown";

    /** Lower case, with '-' and spaces turned into '_' so "Second Wind", "second-wind" and "second_wind" are one word. */
    public static String normalise(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    /** The words in force: the built-in ones plus the config's comma separated ones, normalised, blanks dropped, in a stable order. */
    public static List<String> words(String extraWords) {
        Set<String> out = new LinkedHashSet<>(BUILT_IN_WORDS);
        if (extraWords != null) for (String w : extraWords.split(",")) {
            String n = normalise(w);
            if (!n.isEmpty()) out.add(n);
        }
        return List.copyOf(out);
    }

    /** True when the id (or any text) holds a word of the regeneration family. */
    public static boolean matchesWord(String text, String extraWords) {
        String t = normalise(text);
        if (t.isEmpty()) return false;
        for (String w : words(extraWords)) if (t.contains(w)) return true;
        return false;
    }

    private static String path(String id) {
        String n = normalise(id);
        int c = n.indexOf(':');
        return c < 0 ? n : n.substring(c + 1);
    }

    private static boolean isTensura(String id) { return normalise(id).startsWith("tensura:"); }

    /** Is a skill a Unique or an Ultimate one, by category, and for an unknown category by the fail-closed table (Tensura ids only)? */
    public static boolean isUniqueOrUltimate(String skillId, String category) {
        String c = normalise(category);
        if (c.equals(UNIQUE) || c.equals(ULTIMATE)) return true;
        boolean unreadable = c.isEmpty() || c.equals(UNKNOWN);
        return unreadable && isTensura(skillId) && KNOWN_UNIQUE_OR_ULTIMATE.contains(path(skillId));
    }

    /** Is the skill part of the regeneration / healing family (regardless of category)? */
    public static boolean isRegeneration(String skillId, String extraWords) {
        String id = normalise(skillId);
        if (id.isEmpty()) return false;
        return matchesWord(id, extraWords);
    }

    /** May a boss NOT have or use this skill? 'category' is one of the constants above (null = unknown), with the default settings. */
    public static boolean isForbidden(String skillId, String category) { return isForbidden(skillId, category, Settings.DEFAULT); }

    /** The same with the owner's switches: each half of the rule applies only while its switch is on. */
    public static boolean isForbidden(String skillId, String category, Settings s) {
        if (skillId == null || skillId.isBlank()) return false;
        Settings st = s == null ? Settings.DEFAULT : s;
        if (st.stripRegeneration() && isRegeneration(skillId, st.extraWords())) return true;
        return st.banUniqueUltimate() && isUniqueOrUltimate(skillId, category);
    }

    /** May a boss NOT carry this effect (the permanent regeneration effects of the old engine, Regeneration, Instant Health)? */
    public static boolean isForbiddenEffect(String effectId, Settings s) {
        Settings st = s == null ? Settings.DEFAULT : s;
        if (!st.stripRegeneration() || effectId == null || effectId.isBlank()) return false;
        String id = normalise(effectId);
        return EXTRA_EFFECT_IDS.contains(id) || matchesWord(id, st.extraWords());
    }

    /** The ids of 'ids' a boss may have, in order; 'categoryOf' gives the category of an id (null = unknown). */
    public static List<String> allowed(java.util.Collection<String> ids, Function<String, String> categoryOf, Settings s) {
        List<String> out = new ArrayList<>();
        if (ids == null) return out;
        for (String id : ids) if (!isForbidden(id, categoryOf == null ? null : categoryOf.apply(id), s)) out.add(id);
        return out;
    }
}
