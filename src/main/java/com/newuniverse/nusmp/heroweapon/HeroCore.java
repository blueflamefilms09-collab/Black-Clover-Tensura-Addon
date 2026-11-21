package com.newuniverse.nusmp.heroweapon;

import com.newuniverse.nusmp.heroweapon.bond.HeroBondEvents;
import com.newuniverse.nusmp.heroweapon.bond.HeroBondSelfTests;
import com.newuniverse.nusmp.heroweapon.bond.HeroLoanEvents;
import com.newuniverse.nusmp.heroweapon.bond.HeroNet;
import com.newuniverse.nusmp.heroweapon.rules.HeroRuleEvents;
import com.newuniverse.nusmp.heroweapon.rules.HeroRulesSelfTests;
import com.newuniverse.nusmp.heroweapon.skill.HeroCombat;
import com.newuniverse.nusmp.heroweapon.skill.HeroUseSelfTests;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Why: the one entry point of the core system (the bond sync, the loan of the weapon into the hand, the input handler and the dispatcher, the restrictions, the
 * experience rule, the item behaviour, the config {@code nusmp-heroweapon-server.toml}, the self-tests {@code hero.bond}, {@code hero.use}, {@code hero.rules}),
 * called once from {@code HeroWeaponSystem.init} on both sides. It registers the core's own config file (so the shared start-up never needs a
 * {@code registerConfig} line for it), the four payloads on the mod bus, and the listeners of the bond, the loan, the fights and the restrictions on the game bus
 * (they ignore the client's copy of a player). Safe to call twice.
 */
public final class HeroCore {
    private HeroCore() {}

    private static boolean done;

    public static synchronized void init(IEventBus modBus, ModContainer modContainer) {
        if (done) return;
        done = true;
        modContainer.registerConfig(ModConfig.Type.SERVER, HeroConfig.SPEC, "nusmp-heroweapon-server.toml");
        com.newuniverse.nusmp.heroweapon.skill.Dur.source(HeroConfig::durationScale);          // the one reader of heroweapon.durationScale (the client reads the synced copy)
        modBus.addListener(HeroNet::register);
        HeroBondEvents.register(NeoForge.EVENT_BUS);
        HeroLoanEvents.register(NeoForge.EVENT_BUS);
        HeroCombat.register(NeoForge.EVENT_BUS);
        com.newuniverse.nusmp.heroweapon.weapon.shield.ShieldReflect.init();           // 4.71.0: a raised hero Shield turns dangerous shots from the front back
        HeroRuleEvents.register(NeoForge.EVENT_BUS);
        com.newuniverse.nusmp.heroweapon.rules.UtilityYieldEvents.register(NeoForge.EVENT_BUS);   // 4.89.4: a weapon in the main hand hands the use key to a hold-and-use item in the off hand
        com.newuniverse.nusmp.heroweapon.rules.UtilityKeepSelfTests.register();                   // key utilkeep.rules
        HeroBondSelfTests.register();                    // keys hero.bond, hero.use, hero.rules (the smoke test expects the prefix "hero")
        HeroUseSelfTests.register();
        com.newuniverse.nusmp.heroweapon.skill.HeroDurSelfTests.register();              // key hero.durations
        HeroRulesSelfTests.register();
        com.newuniverse.nusmp.heroweapon.tracker.WeaponTrackerBoot.init(modBus);       // the Weapon Tracker: /multiverse weapons, two payloads, self-tests weapons.tracker.*
    }
}
