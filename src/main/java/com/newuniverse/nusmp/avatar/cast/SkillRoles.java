package com.newuniverse.nusmp.avatar.cast;

import java.util.List;
import java.util.Locale;

/**
 * Why: the Soul Forge lets the creator give an Avatar a Tensura Unique and Ultimate skill, and the Avatar must "press" it the way a player would, but a mob cannot
 * use a skill that opens a screen, reads the camera, or teleports it away from its creator. Which skill is an attack, which a buff the Avatar should raise at the
 * start of a fight, which one heals it, and which one it must leave alone is judged here from the skill's registry path (Tensura's ids are plain words:
 * {@code absolute_severance}, {@code berserk}, {@code healer}, {@code creator}), a pure table with a default of "attack" because the skills a creator picks for an
 * Avatar are fighting skills. The words are checked in the order NONE, HEAL, BUFF; everything else is an attack.
 */
public final class SkillRoles {
    private SkillRoles() {}

    /** What a skill is used for in a fight. */
    public enum Role { ATTACK, BUFF, HEAL, NONE }

    /** Skills an Avatar never presses: they open screens, craft, read the camera or the map, teleport, or only matter to a player. */
    private static final List<String> NONE_WORDS = List.of("creator", "degenerate", "godly_craftsman", "researcher", "mathematician", "fusionist", "traveler",
            "suppressor", "spatial_motion", "spatial_manipulation", "spatial_domination", "telepathy", "thought_communication", "farsight", "all_seeing_eye", "sense",
            "universal_perception", "analytical_appraisal", "analyst", "seer", "observer", "falsifier", "danger_sense", "godwolf", "heavenly_eye", "great_sage",
            "seeker", "sage", "majesty", "law_manipulation", "molecular_manipulation", "weather", "water_current", "hydraulic", "gravity_flight", "shadow_motion",
            "possession", "absorb_and_dissolve", "sticky_steel_thread", "multilayer_barrier", "ranged_barrier", "spiritual", "summon");
    private static final List<String> HEAL_WORDS = List.of("healer", "chef", "regeneration", "survivor", "tuner");
    private static final List<String> BUFF_WORDS = List.of("berserk", "royal_beast", "engorger", "martial_master", "fighter", "commander", "chosen_one", "strength",
            "steel", "strengthen", "thought_acceleration", "ultra_instinct", "magic_aura", "haki", "mortal_fear", "guardian", "unyielding", "mana_manipulation",
            "shadow_striker", "wrath");

    /** The role of a Tensura skill by its registry path ("gluttony", "tensura:berserk" both work). Unknown and empty paths are attacks / none. */
    public static Role role(String idOrPath) {
        if (idOrPath == null || idOrPath.isBlank()) return Role.NONE;
        String p = idOrPath.toLowerCase(Locale.ROOT);
        int colon = p.indexOf(':');
        if (colon >= 0) p = p.substring(colon + 1);
        for (String w : NONE_WORDS) if (p.contains(w)) return Role.NONE;
        for (String w : HEAL_WORDS) if (p.contains(w)) return Role.HEAL;
        for (String w : BUFF_WORDS) if (p.contains(w)) return Role.BUFF;
        return Role.ATTACK;
    }

    /** Ticks an Avatar waits before it presses the same skill again, by role (before the INT / WIS / personality / frenzy scaling): a fight lasts longer than a buff. */
    public static int baseCooldown(Role role, boolean ultimate) {
        return switch (role) {
            case BUFF -> 1200;
            case HEAL -> 600;
            case ATTACK -> ultimate ? 400 : 160;
            case NONE -> Integer.MAX_VALUE / 4;
        };
    }
}
