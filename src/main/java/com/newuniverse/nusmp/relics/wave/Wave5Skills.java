package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.Fx;
import net.minecraft.server.level.ServerPlayer;

/**
 * Tensura skills lent by wave 5's relics (the owner's directive: where Minecraft cannot do what a card says, use Tensura). The lease itself is the
 * core's ({@link Fx#lendSkill}, over the Dungeon's meal lease): it is saved, taken back when the time is up, a skill the player owns for good is left
 * alone, and an id the skill registry does not know is skipped without a word, so a wrong id never breaks anything.
 * <p>
 * Skill ids used by wave 5: tensura:magic_sense, tensura:danger_sense, tensura:physical_attack_resistance and tensura:poison_resistance are
 * confirmed to exist in this repo; tensura:presence_concealment and tensura:self_regeneration are not verified (gated by the registry lookup).
 * A skill that is only lent while a condition holds (a mask on, a form on, a stalk) is re-leased every second for 60 ticks, like the core's
 * Passive.skill does for a skill kept for as long as a relic is worn.
 */
final class Wave5Skills {
    private Wave5Skills() {}

    static final String MAGIC_SENSE = "tensura:magic_sense";
    static final String DANGER_SENSE = "tensura:danger_sense";
    static final String PHYSICAL_RESIST = "tensura:physical_attack_resistance";
    static final String POISON_RESIST = "tensura:poison_resistance";
    static final String PRESENCE_CONCEALMENT = "tensura:presence_concealment";
    static final String SELF_REGENERATION = "tensura:self_regeneration";

    /** Lends one skill for {@code ticks}; true when it is now the player's (lent or owned). */
    static boolean lend(ServerPlayer p, String skillId, int ticks) { return Fx.lendSkill(p, skillId, ticks); }

    /** Lends several skills at once (merged into one relic ability); returns how many the player has now. */
    static int lendAll(ServerPlayer p, int ticks, String... skillIds) { return Fx.lendSkills(p, ticks, skillIds); }
}
