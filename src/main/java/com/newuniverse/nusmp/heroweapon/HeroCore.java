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
        modBus.addListener(HeroNet::register);
        HeroBondEvents.register(NeoForge.EVENT_BUS);
        HeroLoanEvents.register(NeoForge.EVENT_BUS);
        HeroCombat.register(NeoForge.EVENT_BUS);
        HeroRuleEvents.register(NeoForge.EVENT_BUS);
        HeroBondSelfTests.register();                    // keys hero.bond, hero.use, hero.rules (the smoke test expects the prefix "hero")
        HeroUseSelfTests.register();
        HeroRulesSelfTests.register();
    }
}
