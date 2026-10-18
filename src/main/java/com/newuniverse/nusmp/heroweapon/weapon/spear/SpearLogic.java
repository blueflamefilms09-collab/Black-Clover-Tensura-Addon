package com.newuniverse.nusmp.heroweapon.weapon.spear;

import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.skill.HeroLogic;
import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;

import java.util.List;

/**
 * Why: the behaviour of the {@link HeroKind#SPEAR} weapon (its skills, what it does every few ticks, what it adds to a blow). Owned by this weapon's
 * builder, who replaces this stub with the real logic; until then the weapon has no skills, so the contract compiles and the game runs.
 */
public final class SpearLogic implements HeroLogic {
    @Override
    public HeroKind kind() { return HeroKind.SPEAR; }

    @Override
    public List<HeroSkill> skills() { return List.of(); }
}
