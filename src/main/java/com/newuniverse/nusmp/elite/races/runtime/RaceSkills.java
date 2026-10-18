package com.newuniverse.nusmp.elite.races.runtime;

import com.newuniverse.nusmp.elite.races.EliteRacesConfig;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the races name skills by registry id (their intrinsic and resistance skills, the skill whose mastery an evolution asks for, the Kaioken skill whose overlap is scaled).
 * Looking an id up must never throw and must tolerate a skill that does not exist (a renamed Tensura skill, the Kaioken skill of another builder that is not in this build): it
 * answers "nothing" and the race simply has one skill fewer. The self-test {@code elite.races.templates} lists every id of the five race files that does not resolve.
 */
public final class RaceSkills {
    private RaceSkills() {}

    public static ResourceLocation id(String s) { return ResourceLocation.tryParse(s); }

    /** The registered skill, or null. */
    public static ManasSkill find(String id) {
        try {
            ResourceLocation rl = id(id);
            return rl == null ? null : SkillAPI.getSkillRegistry().get(rl);
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    /** The skills of a list of ids that exist, in order, without repeats. */
    public static List<ManasSkill> resolve(List<String> ids) {
        List<ManasSkill> out = new ArrayList<>();
        for (String s : ids) {
            ManasSkill sk = find(s);
            if (sk != null && !out.contains(sk)) out.add(sk);
        }
        return out;
    }

    /** The player's instance of a skill, or null. */
    public static ManasSkillInstance instance(LivingEntity e, String id) {
        try {
            ResourceLocation rl = id(id);
            if (rl == null) return null;
            return SkillAPI.getSkillsFrom(e).getSkill(rl).orElse(null);
        } catch (RuntimeException | LinkageError ex) {
            return null;
        }
    }

    /** Mastery of a skill as 0..1 (0 when the player does not have it). */
    public static double mastery(LivingEntity e, String id) {
        ManasSkillInstance inst = instance(e, id);
        if (inst == null) return 0.0;
        try {
            double max = inst.getMaxMastery();
            if (max <= 0) return 1.0;
            return Math.max(0.0, Math.min(1.0, inst.getMastery() / max));
        } catch (RuntimeException ex) {
            return 0.0;
        }
    }

    /** Is any configured Kaioken skill toggled on for this entity? */
    public static boolean kaiokenActive(LivingEntity e) {
        for (String id : EliteRacesConfig.kaiokenSkillIds()) {
            ManasSkillInstance inst = instance(e, id);
            try {
                if (inst != null && inst.isToggled()) return true;
            } catch (RuntimeException ignored) {
                // a skill that cannot be asked is not on
            }
        }
        return false;
    }
}
