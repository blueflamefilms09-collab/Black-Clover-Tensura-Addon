package com.newuniverse.nusmp.relics.wave;

/**
 * Tensura skills and effects the wave 6 relics lend or apply (the owner's directive: where Minecraft cannot do what a card says, use Tensura, and merge
 * several skills into one relic ability). Every id is resolved at run time by the core ({@code Fx.lendSkills}, {@code Passive.skill},
 * {@code Fx.addEffect}) and skipped silently when the registry does not know it, so a wrong id costs nothing but the bonus: the Minecraft half of
 * each ability always works on its own.
 * <p>
 * Skill ids CONFIRMED to exist in this repo: magic_sense, danger_sense, physical_attack_resistance, poison_resistance (the ones the Meals use). All the
 * others are named after the fields of Tensura's skill registries (ExtraSkills, CommonSkills, IntrinsicSkills, ResistanceSkills in the API dump) and are NOT
 * verified in game.
 */
final class Wave6Skills {
    private Wave6Skills() {}

    // confirmed
    static final String MAGIC_SENSE = "tensura:magic_sense";
    static final String DANGER_SENSE = "tensura:danger_sense";
    static final String PHYSICAL_ATTACK_RESISTANCE = "tensura:physical_attack_resistance";

    // resistances (ResistanceSkills)
    static final String MAGIC_RESISTANCE = "tensura:magic_resistance";
    static final String MAGIC_NULLIFICATION = "tensura:magic_nullification";
    static final String ABNORMAL_CONDITION_RESISTANCE = "tensura:abnormal_condition_resistance";
    static final String PHYSICAL_ATTACK_NULLIFICATION = "tensura:physical_attack_nullification";
    static final String PAIN_NULLIFICATION = "tensura:pain_nullification";
    static final String PAIN_RESISTANCE = "tensura:pain_resistance";
    static final String PIERCE_RESISTANCE = "tensura:pierce_resistance";
    static final String PARALYSIS_RESISTANCE = "tensura:paralysis_resistance";
    static final String FLAME_ATTACK_RESISTANCE = "tensura:flame_attack_resistance";
    static final String COLD_RESISTANCE = "tensura:cold_resistance";
    static final String ELECTRICITY_RESISTANCE = "tensura:electricity_resistance";
    static final String WATER_ATTACK_RESISTANCE = "tensura:water_attack_resistance";
    static final String DARKNESS_ATTACK_RESISTANCE = "tensura:darkness_attack_resistance";
    static final String GRAVITY_ATTACK_RESISTANCE = "tensura:gravity_attack_resistance";
    static final String SPATIAL_ATTACK_RESISTANCE = "tensura:spatial_attack_resistance";

    // active and extra skills
    static final String SELF_REGENERATION = "tensura:self_regeneration";
    static final String MULTILAYER_BARRIER = "tensura:multilayer_barrier";
    static final String MAGIC_JAMMING = "tensura:magic_jamming";
    static final String THOUGHT_ACCELERATION = "tensura:thought_acceleration";
    static final String ALL_SEEING_EYE = "tensura:all_seeing_eye";
    static final String SHADOW_MOTION = "tensura:shadow_motion";
    static final String SPATIAL_MOTION = "tensura:spatial_motion";
    static final String GRAVITY_FLIGHT = "tensura:gravity_flight";
    static final String WIND_TRANSFORM = "tensura:wind_transform";
    static final String WIND_MANIPULATION = "tensura:wind_manipulation";

    // effects and damage types
    static final String FX_FLASHED_BLINDNESS = "tensura:flashed_blindness";
    static final String FX_ALLY_BOOST = "tensura:ally_boost";
    static final String FX_FATE_CHANGE = "tensura:fate_change";
    static final String FX_PRESENCE_CONCEALMENT = "tensura:presence_concealment";
    static final String DMG_LIGHT = "tensura:light_elemental";
    static final String DMG_HOLY = "tensura:holy_damage";
    static final String DMG_DARKNESS = "tensura:darkness_elemental";
}
