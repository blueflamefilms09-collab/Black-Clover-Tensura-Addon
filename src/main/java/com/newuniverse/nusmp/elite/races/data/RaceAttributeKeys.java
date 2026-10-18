package com.newuniverse.nusmp.elite.races.data;

import java.util.Set;

/**
 * Why: a race file names attributes by short key, and the parser has to refuse a typo without a running game. This is the pure list of keys the mod understands: vanilla
 * attributes by their plain name, Tensura's by {@code tensura:name}. {@code runtime/RaceAttributes} maps each key to the real attribute holder, and the self-test
 * {@code elite.races.templates} proves that every key here maps to one, so the two lists cannot drift apart.
 */
public final class RaceAttributeKeys {
    private RaceAttributeKeys() {}

    public static final Set<String> KNOWN = Set.of(
            // vanilla
            "max_health", "attack_damage", "attack_speed", "movement_speed", "armor", "armor_toughness", "knockback_resistance", "jump_strength", "step_height", "scale",
            "luck", "safe_fall_distance", "fall_damage_multiplier", "entity_interaction_range", "block_interaction_range", "attack_knockback",
            // Tensura
            "tensura:max_aura", "tensura:max_magicule", "tensura:aura_regeneration_multiplier", "tensura:magicule_regeneration_multiplier",
            "tensura:max_spiritual_health", "tensura:spiritual_health_regeneration", "tensura:magic_resistance", "tensura:flame_resistance", "tensura:water_resistance",
            "tensura:wind_resistance", "tensura:earth_resistance", "tensura:light_resistance", "tensura:darkness_resistance", "tensura:space_resistance",
            "tensura:ability_mastery_gain", "tensura:dodge_strength", "tensura:auto_melee_dodge_chance", "tensura:presence_sense_radius");

    /** Attributes the {@code stats} block of a race sets through Tensura's own default-race machinery; a race's extra {@code attributes} may not repeat them. */
    public static final Set<String> RESERVED_BY_STATS = Set.of("max_health", "attack_damage", "attack_speed", "knockback_resistance", "movement_speed", "tensura:max_spiritual_health");

    public static boolean known(String key) { return KNOWN.contains(key); }
}
