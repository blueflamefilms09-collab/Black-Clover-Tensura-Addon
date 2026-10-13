package com.newuniverse.nusmp.relics.djinn;

import java.util.ArrayList;
import java.util.List;

/**
 * The Tensura skills a Djinn lends while it is out (the owner's directive: use Tensura where Minecraft cannot). Each phase lends a set, merged into
 * the one named ability of that phase; the engine renews a short lease every second ({@code Fx.lendSkills}), so the skills go the moment the phase
 * does. An id the skill registry does not know is skipped by the lending itself, so a wrong guess costs nothing but the skill.
 * <ul>
 *   <li>Lesser Summon: Magic Sense, the small manifestation feeling for magic ({@code tensura:magic_sense}, verified in this repo);</li>
 *   <li>Half Equip: Danger Sense ({@code tensura:danger_sense}, verified) and the Djinn's own manipulation skill;</li>
 *   <li>Full Equip: both of those, the matching domination skill, and Thought Acceleration.</li>
 * </ul>
 * Every id here is in the owner's Tensura 2.0.1.3 id reference (scratchpad/tensura_ids/clean.json, checked by hand), so all are verified.
 */
public final class DjinnSkills {
    private DjinnSkills() {}

    /** Also used by the mod's meals and Dungeon relics. */
    public static final String MAGIC_SENSE = "tensura:magic_sense", DANGER_SENSE = "tensura:danger_sense";
    public static final String THOUGHT_ACCELERATION = "tensura:thought_acceleration";

    /** The manipulation skill of an affinity (Half Equip). Life and Strength have no manipulation skill: regeneration and body strengthening. */
    public static String manipulation(Affinity a) {
        return switch (a) {
            case HEAT -> "tensura:flame_manipulation";
            case LIGHTNING -> "tensura:lightning_manipulation";
            case WATER -> "tensura:water_manipulation";
            case WIND -> "tensura:wind_manipulation";
            case LIFE -> "tensura:self_regeneration";
            case STRENGTH -> "tensura:strengthen_body";
            case SOUND -> "tensura:sound_manipulation";
            case LIGHT -> "tensura:heavenly_eye";
        };
    }

    /** The domination skill of an affinity (Full Equip). */
    public static String domination(Affinity a) {
        return switch (a) {
            case HEAT -> "tensura:flame_domination";
            case LIGHTNING -> "tensura:lightning_domination";
            case WATER -> "tensura:water_domination";
            case WIND -> "tensura:wind_domination";
            case LIFE -> "tensura:ultraspeed_regeneration";
            case STRENGTH -> "tensura:steel_strength";
            case SOUND -> "tensura:sound_domination";
            case LIGHT -> "tensura:all_seeing_eye";
        };
    }

    /** The skills a phase (1-3) lends for this affinity, in order; empty for phase 0. */
    public static List<String> forPhase(Affinity a, int phase) {
        List<String> out = new ArrayList<>(4);
        if (phase >= DjinnPhaseRules.LESSER) out.add(MAGIC_SENSE);
        if (phase >= DjinnPhaseRules.HALF) {
            out.add(DANGER_SENSE);
            out.add(manipulation(a));
        }
        if (phase >= DjinnPhaseRules.FULL) {
            out.add(domination(a));
            out.add(THOUGHT_ACCELERATION);
        }
        return out;
    }

    /** The lease the engine renews each second: a few seconds, so a skill is gone soon after the phase ends. */
    public static final int LEASE_TICKS = 100;
}
