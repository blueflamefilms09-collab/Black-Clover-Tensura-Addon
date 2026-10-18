package com.newuniverse.nusmp.heroweapon.weapon.claw;

import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.skill.HeroLogic;
import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;
import com.newuniverse.nusmp.heroweapon.weapon.strike.FormsCheck;
import com.newuniverse.nusmp.heroweapon.weapon.strike.Strike;
import com.newuniverse.nusmp.heroweapon.weapon.strike.StrikeFx;
import com.newuniverse.nusmp.heroweapon.weapon.strike.StrikeSelfTest;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * Why: the behaviour of the {@link HeroKind#CLAW} weapon (the Vassal Claw, a Seven Star Weapon of the world of the shield hero, the slashing counterpart of the hammer). It
 * lists the eight techniques ({@link ClawSkills}) and adds what a blow of the claws does on its own: every blow opens a wound ({@link ClawBleed}, one wound, two on a full swing,
 * one more in the cursed form) and the Bone Claws' Scent of Blood strikes a heavily wounded foe once more. The other passives live in the techniques: Skill Mastery (every
 * damaging technique grows with use, the canon power-up), Rope Hook (the leap homes in on a foe), Dragon Fangs (more wounds, open for longer) and Madness (the cursed form). This weapon
 * carries ONE small curse form, the Cursed Claw of the canon curse list, whose own technique is the Wahnsinn Claw. Registers its self-test {@code hero.weapon.claw} from the
 * constructor. Server thread only.
 */
public final class ClawLogic implements HeroLogic {
    private static final FormsCheck.Known KNOWN = new FormsCheck.Known("vassal_claw", ClawNumbers.IDS, ClawNumbers.ULTIMATES, ClawNumbers.PASSIVES, 3, 5, true);

    private final List<HeroSkill> skills = ClawSkills.all();

    public ClawLogic() { StrikeSelfTest.register(HeroKind.CLAW, KNOWN); }

    @Override
    public HeroKind kind() { return HeroKind.CLAW; }

    @Override
    public List<HeroSkill> skills() { return skills; }

    @Override
    public void onHit(HeroCtx c, LivingEntity target, float dealt, boolean fullSwing) {
        if (Strike.inHit() || target == null || !target.isAlive()) return;       // a technique's own blow is not a swing
        if (c.form.has(ClawNumbers.RENDING_BLOWS)) {
            int wounds = (fullSwing ? 2 : 1) + (c.form.has(ClawNumbers.MADNESS) ? 1 : 0);
            ClawBleed.add(c, target, wounds);
        }
        if (c.form.has(ClawNumbers.SCENT_OF_BLOOD) && ClawBleed.stacks(target) >= ClawNumbers.SCENT_STACKS && Strike.hit(c, target, ClawNumbers.SCENT_RAW, true)) {
            StrikeFx.burst(c.level, Strike.center(target), ClawBleed.BLOOD, 5);
        }
    }
}
