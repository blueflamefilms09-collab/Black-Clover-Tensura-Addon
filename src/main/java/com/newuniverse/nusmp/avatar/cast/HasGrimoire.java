package com.newuniverse.nusmp.avatar.cast;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;

/**
 * Why: which grimoire an Avatar casts from is the creation system's decision (the creator's book, or one the race grants). AvatarEntity may
 * implement this to hand {@link SimulatedPlayerCaster} that book instance directly; when it does not, the caster falls back to the creator's own
 * grimoire. The instance is only COPIED by the caster, never mutated.
 */
public interface HasGrimoire {
    /** The grimoire skill instance this Avatar casts from, or null for none. */
    ManasSkillInstance avatarGrimoire();
}
