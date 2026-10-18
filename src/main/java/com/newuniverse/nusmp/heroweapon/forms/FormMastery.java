package com.newuniverse.nusmp.heroweapon.forms;

import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;
import net.minecraft.server.level.ServerPlayer;

/**
 * Why: mastery (the series' proficiency of a form) is earned by using the form, and the things that count are done in the core's code (a blow, a technique, a block), not in the
 * forms system. The core calls these three methods at those moments; the forms system adds the mastery to the ACTIVE form of the bond (a point per hit, two per technique, one per block, by
 * its config), finishes a form when its mastery is full (the permanent ability bonus, the quest hook, a title) and refreshes the wielder. Owned by the forms builder, who replaces this
 * stub; the stub does nothing.
 */
public final class FormMastery {
    private FormMastery() {}

    /** The wielder landed a melee hit with the summoned weapon. */
    public static void hit(ServerPlayer player) {}

    /** The wielder cast a technique (after it succeeded). */
    public static void skill(ServerPlayer player, HeroSkill skill) {}

    /** The wielder's guard stopped a blow. */
    public static void block(ServerPlayer player) {}
}
