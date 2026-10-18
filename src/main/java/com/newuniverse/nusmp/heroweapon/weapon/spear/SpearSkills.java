package com.newuniverse.nusmp.heroweapon.weapon.spear;

import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;
import com.newuniverse.nusmp.heroweapon.skill.SimpleSkill;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.SkillRow;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Tasks;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the nineteen techniques of the Legendary Spear are rows in {@link SpearNumbers} (id, text, cooldown, cost, budget) and bodies in the classes of
 * this package; this is the one place that joins each row to its body, so a row without a body or a body without a row is a visible mistake in one short
 * list, and the order of the skills is the order of {@link SpearIds#ALL}. Every body is guarded: one that throws is logged once and refused, never
 * thrown into the dispatcher. An ultimate row becomes an ultimate skill.
 */
final class SpearSkills {
    private SpearSkills() {}

    static List<HeroSkill> build() {
        List<HeroSkill> out = new ArrayList<>();
        add(out, SpearNumbers.PIERCING_THRUST, SpearStrikes::thrust);
        add(out, SpearNumbers.LUNGE, SpearMotion::lunge);
        add(out, SpearNumbers.VAULT, SpearMotion::vault);
        add(out, SpearNumbers.JAVELIN, SpearStrikes::javelin);
        add(out, SpearNumbers.WHIRLWIND, SpearGuard::whirl);
        add(out, SpearNumbers.RIPOSTE, SpearGuard::riposte);
        add(out, SpearNumbers.CHAOS_SPEAR, SpearStrikes::chaos);
        add(out, SpearNumbers.HOOKED_PIERCE, SpearMotion::hook);
        add(out, SpearNumbers.METEOR_SPEAR, SpearStrikes::meteor);
        add(out, SpearNumbers.PORTAL_SPEAR, SpearMotion::portal);
        add(out, SpearNumbers.FILO_RUSH, SpearMotion::rush);
        add(out, SpearNumbers.TRIAINA, SpearStrikes::triaina);
        add(out, SpearNumbers.MINUTE_HAND, SpearGuard::minuteHand);
        add(out, SpearNumbers.BRIONAC, SpearStrikes::brionac);
        add(out, SpearNumbers.TIME_REVERSAL, SpearTime::rewind);
        add(out, SpearNumbers.TEMPTATION, SpearLust::temptation);
        add(out, SpearNumbers.RAPTURE, SpearLust::rapture);
        add(out, SpearNumbers.THORN_CHAINS, SpearLust::chains);
        add(out, SpearNumbers.LUSTS_EMBRACE, SpearLust::embrace);
        return List.copyOf(out);
    }

    private static void add(List<HeroSkill> out, SkillRow row, SimpleSkill.Body body) {
        SimpleSkill skill = SimpleSkill.of(row.id(), row.name(), row.desc(), row.cooldown(), row.cost(), Tasks.guard(row.id(), body));
        out.add(row.kind() == SkillRow.Kind.ULTIMATE ? skill.asUltimate() : skill);
    }
}
