package com.newuniverse.nusmp.heroweapon;

import com.newuniverse.nusmp.heroweapon.skill.HeroLogics;
import com.newuniverse.nusmp.heroweapon.weapon.shield.ShieldLogic;
import com.newuniverse.nusmp.heroweapon.weapon.sword.SwordLogic;
import com.newuniverse.nusmp.heroweapon.weapon.spear.SpearLogic;
import com.newuniverse.nusmp.heroweapon.weapon.bow.BowLogic;
import com.newuniverse.nusmp.heroweapon.weapon.hammer.HammerLogic;
import com.newuniverse.nusmp.heroweapon.weapon.claw.ClawLogic;
import com.newuniverse.nusmp.heroweapon.weapon.axe.AxeLogic;
import com.newuniverse.nusmp.heroweapon.weapon.whip.WhipLogic;
import com.newuniverse.nusmp.heroweapon.weapon.staff.StaffLogic;
import com.newuniverse.nusmp.heroweapon.weapon.gauntlet.GauntletLogic;
import com.newuniverse.nusmp.heroweapon.weapon.projectile.ProjectileLogic;
import com.newuniverse.nusmp.heroweapon.weapon.carriage.CarriageLogic;
import com.newuniverse.nusmp.heroweapon.weapon.fan.FanLogic;
import com.newuniverse.nusmp.heroweapon.weapon.scythe.ScytheLogic;
import com.newuniverse.nusmp.heroweapon.weapon.katana.KatanaLogic;
import com.newuniverse.nusmp.heroweapon.weapon.book.BookLogic;
import com.newuniverse.nusmp.heroweapon.weapon.mirror.MirrorLogic;
import com.newuniverse.nusmp.heroweapon.weapon.instrument.InstrumentLogic;
import com.newuniverse.nusmp.heroweapon.weapon.harpoon.HarpoonLogic;
import com.newuniverse.nusmp.heroweapon.weapon.sewingset.SewingSetLogic;
import com.newuniverse.nusmp.heroweapon.weapon.chain.ChainLogic;

/**
 * Why: the 21 weapon logics are registered in ONE place, in catalogue order, with plain constructors (no reflection), so a misspelt class is a compile error and
 * the registry is complete before anything reads it. The lead owns this file; a weapon builder only replaces the body of its own {@code <Kind>Logic} class and
 * never touches this one. Safe to call twice (the registry ignores a second registration of the same weapon).
 */
public final class HeroBootstrap {
    private HeroBootstrap() {}

    public static void run() {
        HeroLogics.register(new ShieldLogic());
        HeroLogics.register(new SwordLogic());
        HeroLogics.register(new SpearLogic());
        HeroLogics.register(new BowLogic());
        HeroLogics.register(new HammerLogic());
        HeroLogics.register(new ClawLogic());
        HeroLogics.register(new AxeLogic());
        HeroLogics.register(new WhipLogic());
        HeroLogics.register(new StaffLogic());
        HeroLogics.register(new GauntletLogic());
        HeroLogics.register(new ProjectileLogic());
        HeroLogics.register(new CarriageLogic());
        HeroLogics.register(new FanLogic());
        HeroLogics.register(new ScytheLogic());
        HeroLogics.register(new KatanaLogic());
        HeroLogics.register(new BookLogic());
        HeroLogics.register(new MirrorLogic());
        HeroLogics.register(new InstrumentLogic());
        HeroLogics.register(new HarpoonLogic());
        HeroLogics.register(new SewingSetLogic());
        HeroLogics.register(new ChainLogic());
    }
}
