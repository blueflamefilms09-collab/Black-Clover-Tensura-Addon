package com.newuniverse.nusmp.heroweapon.weapon.axe;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.skill.HeroLogic;
import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;
import com.newuniverse.nusmp.heroweapon.weapon.strike.FormsCheck;
import com.newuniverse.nusmp.heroweapon.weapon.strike.Strike;
import com.newuniverse.nusmp.heroweapon.weapon.strike.StrikeFx;
import com.newuniverse.nusmp.heroweapon.weapon.strike.StrikeSelfTest;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * Why: the behaviour of the {@link HeroKind#AXE} weapon (the Vassal Axe, a Seven Star Weapon of the world of the shield hero, vassal of the Legendary Sword). It lists the nine
 * techniques ({@link AxeSkills}) and carries the canon power-up, muscle enhancement, as Brawn ({@link AxeBrawn}): a full swing, a technique that hits and a kill add stacks, and
 * every few ticks the stacks are put on the wielder's body. The other passives are small and sit here: Cleaving (a full swing also cuts the foes beside the one struck),
 * Headsman (a full swing on a foe an execution would take cuts once more), Radiance (the Shine Axe: Brawn fades half as fast and the axe glows from six stacks) and Self-repair
 * (the Curse Axe mends its wielder a little every two seconds, through the healing law). This weapon carries ONE small curse form, the Curse Axe of the canon curse list, whose
 * own technique is the Cursed Penetration. Registers its self-test {@code hero.weapon.axe} from the constructor. Server thread only.
 */
public final class AxeLogic implements HeroLogic {
    private static final FormsCheck.Known KNOWN = new FormsCheck.Known("vassal_axe", AxeNumbers.IDS, AxeNumbers.ULTIMATES, AxeNumbers.PASSIVES, 3, 5, true);

    private final List<HeroSkill> skills = AxeSkills.all();

    public AxeLogic() { StrikeSelfTest.register(HeroKind.AXE, KNOWN); }

    @Override
    public HeroKind kind() { return HeroKind.AXE; }

    @Override
    public List<HeroSkill> skills() { return skills; }

    @Override
    public void onTick(HeroCtx c) {
        AxeBrawn.apply(c);
        long now = c.level.getGameTime();
        if (c.form.has(AxeNumbers.SELF_REPAIR) && now % AxeNumbers.REPAIR_EVERY < 5 && c.player.getHealth() < c.player.getMaxHealth()) {
            c.heal(c.player, (float) (c.player.getMaxHealth() * AxeNumbers.REPAIR_SHARE));
        }
        if (c.form.has(AxeNumbers.RADIANCE) && now % 20 < 5 && AxeBrawn.stacks(c) >= AxeNumbers.RADIANCE_STACKS) {
            StrikeFx.burst(c.level, c.player.position().add(0, 1.0, 0), AxeBrawn.GREEN, 3);
        }
    }

    @Override
    public void onHit(HeroCtx c, LivingEntity target, float dealt, boolean fullSwing) {
        if (Strike.inHit() || target == null) return;                          // a technique's own blow is not a swing
        if (!fullSwing) return;
        AxeBrawn.gain(c, 1);
        if (c.form.has(AxeNumbers.CLEAVING)) cleave(c, target);
        if (c.form.has(AxeNumbers.HEADSMAN) && target.isAlive()
                && ExecuteMath.eligible(target.getHealth(), target.getMaxHealth(), BalanceLaw.isBoss(target), target instanceof Player)) {
            Strike.hit(c, target, AxeNumbers.HEADSMAN_RAW, true);
        }
    }

    /** Cleaving: a full swing also cuts the foes standing close to the one struck. */
    private static void cleave(HeroCtx c, LivingEntity target) {
        int n = 0;
        for (LivingEntity f : Strike.foesNear(c.player, target.position(), AxeNumbers.CLEAVE_RADIUS, AxeNumbers.CLEAVE_TARGETS + 1)) {
            if (f == target || n >= AxeNumbers.CLEAVE_TARGETS) continue;
            if (Strike.hit(c, f, AxeNumbers.CLEAVE_RAW, true)) n++;
        }
        if (n > 0) StrikeFx.burst(c.level, target.position().add(0, 0.8, 0), AxeBrawn.GREEN, 6);
    }

    @Override
    public void onKill(HeroCtx c, LivingEntity victim) {
        if (c.summoned()) AxeBrawn.gain(c, 2);
    }
}
