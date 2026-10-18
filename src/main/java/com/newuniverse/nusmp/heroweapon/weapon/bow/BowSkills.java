package com.newuniverse.nusmp.heroweapon.weapon.bow;

import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;
import com.newuniverse.nusmp.heroweapon.skill.SimpleSkill;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.SkillRow;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Tasks;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the seventeen techniques of the Legendary Bow are rows in {@link BowNumbers} (id, text, cooldown, cost, budget) and bodies in the classes of this
 * package; this is the one place that joins each row to its body, so a row without a body or a body without a row is a visible mistake in one short list, and
 * the order of the skills is the order of {@link BowIds#ALL}. Six of them only ARM a change of the next drawn shot (the body is the same for all six: remember
 * the mode, tell the wielder); the rest act at once. Every body is guarded: one that throws is logged once and refused, never thrown into the dispatcher.
 * An ultimate row becomes an ultimate skill.
 */
final class BowSkills {
    private BowSkills() {}

    static List<HeroSkill> build() {
        List<HeroSkill> out = new ArrayList<>();
        add(out, BowNumbers.PIERCING_SHOT, arm(ArmedMode.SNIPER));
        add(out, BowNumbers.NET_ARROW, arm(ArmedMode.NET));
        add(out, BowNumbers.MARK_ARROW, arm(ArmedMode.MARK));
        add(out, BowNumbers.EXPLOSIVE_ARROW, arm(ArmedMode.BURST));
        add(out, BowNumbers.TRACKING_SHOT, arm(ArmedMode.SEEK));
        add(out, BowNumbers.SECOND_ARROW, arm(ArmedMode.SECOND));
        add(out, BowNumbers.ARROW_RAIN, BowRain::rain);
        add(out, BowNumbers.ARROW_SQUALL, BowDirect::squall);
        add(out, BowNumbers.BIRD_HUNTING, BowDirect::bird);
        add(out, BowNumbers.RETREAT_SHOT, BowDirect::retreat);
        add(out, BowNumbers.METEOR_BOW, BowRain::meteor);
        add(out, BowNumbers.FROZEN_RAIN, BowRain::frozen);
        add(out, BowNumbers.STARFALL_VOLLEY, BowRain::starfall);
        add(out, BowNumbers.LAW_FANATIC, BowDirect::law);
        add(out, BowNumbers.BULL_OF_PHALARIS, BowDirect::bull);
        add(out, BowNumbers.PRIDE_COMMAND, arm(ArmedMode.COMMAND));
        add(out, BowNumbers.AUTOCRACY_JUSTICE, BowRain::autocracy);
        return List.copyOf(out);
    }

    /** The body of an arming technique: the next drawn shot (of enough draw) will be this kind of arrow, for a while. */
    private static SimpleSkill.Body arm(ArmedMode mode) {
        return c -> {
            BowState.arm(c, mode, c.level.getGameTime() + BowNumbers.ARMED_TICKS);
            c.say(mode.label() + " ready: draw " + Math.round(mode.minFraction() * 100) + "% or more.");
            BowFx.flare(c, BowFx.muzzle(c, c.look()), 0.7f);
            BowFx.sound(c, c.eye(), SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8f, 1.6f);
            return true;
        };
    }

    private static void add(List<HeroSkill> out, SkillRow row, SimpleSkill.Body body) {
        SimpleSkill skill = SimpleSkill.of(row.id(), row.name(), row.desc(), row.cooldown(), row.cost(), Tasks.guard(row.id(), body));
        out.add(row.kind() == SkillRow.Kind.ULTIMATE ? skill.asUltimate() : skill);
    }
}
