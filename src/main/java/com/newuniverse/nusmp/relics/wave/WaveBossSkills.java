package com.newuniverse.nusmp.relics.wave;

/**
 * The Tensura ids the boss relics use. Skill ids are VERIFIED against the Tensura 2.0.1.3 id reference (scratchpad/tensura_ids/clean.json):
 * gravity_field, gravity_flight, gravity_attack_resistance, spatial_motion, warp_portal, spatial_attack_resistance. They are lent through
 * {@code Fx.lendSkill(s)} / {@code Passive.skill}, which skip an id the skill registry does not know. The damage type id is NOT in that reference:
 * {@code Fx.damage} turns an unknown id into a plain player attack, so a wrong guess costs only the damage type's flavour.
 */
final class WaveBossSkills {
    private WaveBossSkills() {}

    static final String GRAVITY_FIELD = "tensura:gravity_field";
    static final String GRAVITY_FLIGHT = "tensura:gravity_flight";
    static final String GRAVITY_RESIST = "tensura:gravity_attack_resistance";
    static final String SPATIAL_MOTION = "tensura:spatial_motion";
    static final String WARP_PORTAL = "tensura:warp_portal";
    static final String SPATIAL_RESIST = "tensura:spatial_attack_resistance";
    /** Tensura's gravity damage (run-time gated by Fx). */
    static final String GRAVITY_PRESS = "tensura:gravity_press";
}
