package com.newuniverse.nusmp.relics.wave;

/**
 * The Tensura skill ids wave 7 lends (the owner's directive: where a card's rule has no Minecraft equivalent, use Tensura). Lending goes through
 * {@code Fx.lendSkill(s)} (a saved lease, taken back when the time is up, skipped silently when an id is not in the skill registry) or
 * {@code Passive.skill} (kept lent while the armour is worn), so a wrong or missing id never breaks anything; where a skill is the only
 * source of an effect, the armour also has a Minecraft or Tensura-effect fallback.
 * <p>
 * VERIFIED in this repo (already lent by the Dungeon's meals): magic_sense, danger_sense, physical_attack_resistance.
 * NOT verified (named after Tensura's skill registry fields in the API dump, lower case): thought_acceleration, thought_communication,
 * abnormal_condition_resistance, flame_attack_resistance, heat_resistance, dragon_skin, magic_resistance, magic_aura, shadow_motion,
 * hero_haki, chant_annulment.
 */
final class Wave7Skills {
    private Wave7Skills() {}

    // verified
    static final String MAGIC_SENSE = "tensura:magic_sense";
    static final String DANGER_SENSE = "tensura:danger_sense";
    static final String PHYSICAL_RESIST = "tensura:physical_attack_resistance";
    // not verified (gated by the registry at run time)
    static final String THOUGHT_ACCELERATION = "tensura:thought_acceleration";
    static final String THOUGHT_COMMUNICATION = "tensura:thought_communication";
    static final String ABNORMAL_RESIST = "tensura:abnormal_condition_resistance";
    static final String FLAME_RESIST = "tensura:flame_attack_resistance";
    static final String HEAT_RESIST = "tensura:heat_resistance";
    static final String DRAGON_SKIN = "tensura:dragon_skin";
    static final String MAGIC_RESIST = "tensura:magic_resistance";
    static final String MAGIC_AURA = "tensura:magic_aura";
    static final String SHADOW_MOTION = "tensura:shadow_motion";
    static final String HERO_HAKI = "tensura:hero_haki";
    static final String CHANT_ANNULMENT = "tensura:chant_annulment";

    /** Lease of the skills the tick keeps fresh (renewed every second). */
    static final int LEASE = 60;
}
