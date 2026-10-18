package com.newuniverse.nusmp.heroweapon.weapon.bow;

import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.forms.HeroForm;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.skill.HeroLogic;
import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.Tasks;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;

import java.util.List;

/**
 * Why: the Legendary Bow (Itsuki Kawasumi's weapon, whose canon sin is Pride) is the long-range weapon of the four Holy Weapons: artillery, multi-shot and
 * tactical versatility. This is its logic: the seventeen techniques ({@link BowSkills}) and the release of the drawn string ({@link BowShots}): the draw
 * time sets the power, the arrow is made of the wielder's magicules (no arrows in the pack), and the techniques on the sneak and hero keys either change
 * the next shot or act at once. Every hook is guarded (a failure is logged once and the hook gives up for that call; the game never sees an exception from
 * here). The constructor, which runs once at start-up, registers the self-test {@code hero.weapon.bow}.
 */
public final class BowLogic implements HeroLogic {
    private static boolean started;

    private final List<HeroSkill> skills = BowSkills.build();

    public BowLogic() { start(); }

    private static synchronized void start() {
        if (started) return;
        started = true;
        SelfTests.register("hero.weapon.bow", BowSelfTest::run);
    }

    @Override
    public HeroKind kind() { return HeroKind.BOW; }

    @Override
    public List<HeroSkill> skills() { return skills; }

    @Override
    public void onTick(HeroCtx c) {
        try {
            BowShots.tick(c);
        } catch (RuntimeException e) {
            Tasks.warnOnce("bow onTick", e);
        }
    }

    @Override
    public boolean onRelease(HeroCtx c, int chargeTicks) {
        try {
            return BowShots.release(c, chargeTicks);
        } catch (RuntimeException e) {
            Tasks.warnOnce("bow onRelease", e);
            return true;
        }
    }

    @Override
    public void onFormChanged(HeroCtx c, HeroForm from, HeroForm to) {
        try {
            boolean same = from != null && to != null && from.id().equals(to.id());
            if (!same) BowState.disarm(c);
        } catch (RuntimeException e) {
            Tasks.warnOnce("bow onFormChanged", e);
        }
    }

    @Override
    public void onDismiss(HeroCtx c) {
        try {
            BowState.disarm(c);
        } catch (RuntimeException e) {
            Tasks.warnOnce("bow onDismiss", e);
        }
    }
}
