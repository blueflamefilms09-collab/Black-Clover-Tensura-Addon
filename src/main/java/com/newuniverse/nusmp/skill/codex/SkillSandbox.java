package com.newuniverse.nusmp.skill.codex;

import java.util.Set;

/** The allow-list of effect primitives a codex skill may use. A skill with any other primitive is rejected at load; no code ever comes from data. */
public final class SkillSandbox {
    private SkillSandbox() {}

    public static final Set<String> PRIMITIVES = Set.of("projectile", "melee_arc", "magic_damage", "physical_damage", "heal", "shield", "blink", "pull",
            "silence", "slow", "summon_construct", "song_buff", "song_debuff", "copy_codex_skill");

    /** Animation clips of riven_remake.animation.json a skill may name. */
    public static final Set<String> CLIPS = Set.of("cast_grimoire", "cast_song", "eldritch_blast", "shadow_step", "manifest_weapon", "manifest_shield",
            "soul_bond", "sword_combo_1", "sword_combo_2", "sword_combo_3", "talk");

    public static boolean allowed(String primitive) { return PRIMITIVES.contains(primitive); }
}
