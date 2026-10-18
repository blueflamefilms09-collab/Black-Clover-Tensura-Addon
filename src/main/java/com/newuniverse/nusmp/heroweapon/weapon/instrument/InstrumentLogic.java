package com.newuniverse.nusmp.heroweapon.weapon.instrument;

import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.skill.HeroLogic;
import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;

import java.util.List;

/**
 * Why: the behaviour of the {@link HeroKind#INSTRUMENT} weapon (its skills, what it does every few ticks, what it adds to a blow). Owned by this weapon's
 * builder, who replaces this stub with the real logic; until then the weapon has no skills, so the contract compiles and the game runs.
 */
public final class InstrumentLogic implements HeroLogic {
    @Override
    public HeroKind kind() { return HeroKind.INSTRUMENT; }

    @Override
    public List<HeroSkill> skills() { return List.of(); }
}
