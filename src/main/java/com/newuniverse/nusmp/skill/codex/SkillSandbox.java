package com.newuniverse.nusmp.skill.codex;

import java.util.Set;

/**
 * The allow-list every codex skill is checked against at load. A skill can only be built from these primitives; an unknown
 * primitive, native hook, animation or out-of-range number rejects the whole skill. No code ever comes from a skill file.
 */
public final class SkillSandbox {
    private SkillSandbox() {}

    public static final Set<String> PRIMITIVES = Set.of("projectile", "melee_arc", "magic_damage", "physical_damage", "heal", "shield",
            "blink", "pull", "silence", "slow", "summon_construct", "song_buff", "song_debuff", "copy_codex_skill");
    public static final Set<String> NATIVES = Set.of("", "hex", "soul_bond", "inspire", "page_flip", "page_tear", "severance_aria",
            "island_fall", "maw_of_the_rift", "final_page", "unwritten_ending", "bull_ward", "soul_note", "legion_knight",
            "discord", "gold_ring", "two_moons", "doom_gate", "audience_collapse", "crown_break", "rewrite_round");
    public static final Set<String> ANIMATIONS = Set.of("idle", "talk", "cast_grimoire", "cast_song", "eldritch_blast", "shadow_step",
            "manifest_weapon", "manifest_shield", "soul_bond", "sword_combo_1", "sword_combo_2", "sword_combo_3", "final_form");
    public static final Set<String> CONSTRUCTS = Set.of("weapon", "shield", "clone");

    /** Null when the skill is acceptable, otherwise why it was rejected. */
    public static String validate(AnimeSkill s) {
        if (!s.id().matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) return "bad id '" + s.id() + "'";
        if (s.name().isBlank() || s.name().length() > 48) return "bad name";
        if (s.line().length() > 120) return "line too long";
        if (s.tier() < 1 || s.tier() > 5) return "tier must be 1..5";
        if (s.castTicks() < 1 || s.castTicks() > 80) return "cast_ticks must be 1..80";
        if (s.range() < 2 || s.range() > 32) return "range must be 2..32";
        if (s.cooldownTicks() < 10 || s.cooldownTicks() > 1200) return "cooldown_ticks must be 10..1200";
        if (s.primitives().isEmpty() || s.primitives().size() > 6) return "1..6 primitives";
        for (String p : s.primitives()) if (!PRIMITIVES.contains(p)) return "unknown primitive '" + p + "'";
        if (!NATIVES.contains(s.nativeId())) return "unknown native hook '" + s.nativeId() + "'";
        if (!ANIMATIONS.contains(s.animation())) return "unknown animation '" + s.animation() + "'";
        if (!CONSTRUCTS.contains(s.constructKind())) return "unknown construct '" + s.constructKind() + "'";
        return null;
    }
}
