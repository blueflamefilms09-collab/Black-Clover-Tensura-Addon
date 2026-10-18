package com.newuniverse.nusmp.heroweapon.weapon.whip;

import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.skill.HeroLogic;
import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;
import com.newuniverse.nusmp.heroweapon.weapon.strike.FormsCheck;
import com.newuniverse.nusmp.heroweapon.weapon.strike.Strike;
import com.newuniverse.nusmp.heroweapon.weapon.strike.StrikeSelfTest;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * Why: the behaviour of the {@link HeroKind#WHIP} weapon (the Vassal Whip, a Seven Star Weapon of the world of the shield hero, made to direct the monsters that pulled the Bow
 * Hero's chariot, whose special ability is to control the power of monsters). It lists the nine techniques ({@link WhipSkills}), attaches the two listeners that make the Command
 * hold ({@link WhipCommand}) and adds what a blow of the whip does on its own: with Coil (every form) a full swing leaves the foe slowed for a second, and with the Storm
 * Whip's Static Charge it also charges the foe for the next Lightning Whip. The other passives live in the techniques: Star Weight (a bound foe takes a quarter more from the
 * Gigantic Star), Taskmaster (the monsters of a Command are strengthened) and Hourglass Flow (the movement techniques come back a fifth sooner). The Whip carries no curse series.
 * Registers its self-test {@code hero.weapon.whip} from the constructor. Server thread only.
 */
public final class WhipLogic implements HeroLogic {
    private static final FormsCheck.Known KNOWN = new FormsCheck.Known("vassal_whip", WhipNumbers.IDS, WhipNumbers.ULTIMATES, WhipNumbers.PASSIVES, 3, 5, false);

    private final List<HeroSkill> skills = WhipSkills.all();

    public WhipLogic() {
        WhipCommand.register();
        StrikeSelfTest.register(HeroKind.WHIP, KNOWN, true);                 // the Command only takes a monster that can think, so its dummy must
    }

    @Override
    public HeroKind kind() { return HeroKind.WHIP; }

    @Override
    public List<HeroSkill> skills() { return skills; }

    @Override
    public void onHit(HeroCtx c, LivingEntity target, float dealt, boolean fullSwing) {
        if (Strike.inHit() || target == null || !target.isAlive() || !fullSwing || WhipCommand.isCommanded(target)) return;   // a technique's own blow is not a swing
        if (c.form.has(WhipNumbers.COIL)) Strike.effect(target, "minecraft:slowness", WhipNumbers.COIL_TICKS, 0);
        if (c.form.has(WhipNumbers.STATIC_CHARGE)) WhipCommand.markCharged(target, WhipNumbers.CHARGE_TICKS);
    }
}
