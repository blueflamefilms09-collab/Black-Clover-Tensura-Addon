package com.newuniverse.nusmp.heroweapon;

import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import com.newuniverse.nusmp.heroweapon.curse.CurseSystem;
import com.newuniverse.nusmp.heroweapon.forms.FormsSystem;
import com.newuniverse.nusmp.heroweapon.gem.GemSystem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the weapons of The Rising of the Shield Hero are five systems (the core, the forms, the curse, the gem, and the 21 weapons' logics) that must start in
 * one fixed order, and the mod constructor should have exactly ONE line for all of it. The order: the registries (items, data components, the bond attachment)
 * on the mod bus; the weapon logics (the registry the others read); then each system's own {@code init}, in the order core, forms, curse, gem. Each system
 * registers its own config file, so no separate {@code registerConfig} line exists. The lead owns this file. Safe to call twice.
 */
public final class HeroWeaponSystem {
    private HeroWeaponSystem() {}

    private static boolean done;

    /** The single entry point, called from the mod constructor on both sides. */
    public static synchronized void init(IEventBus modBus, ModContainer modContainer) {
        if (done) return;
        done = true;
        HeroItems.REGISTER.register(modBus);
        HeroComponents.COMPONENTS.register(modBus);
        HeroBonds.ATTACHMENTS.register(modBus);
        HeroBootstrap.run();
        HeroCore.init(modBus, modContainer);
        FormsSystem.init(modBus, modContainer);
        CurseSystem.init(modBus, modContainer);
        GemSystem.init(modBus, modContainer);
        HeroSelfTests.register();                        // key hero.catalog, expected by the server smoke test (SMOKE_EXPECT prefix "hero")
    }

    /** What the creative tab offers: the 21 weapons in catalogue order, then the acquisition system's items. */
    public static List<Item> creativeItems() {
        List<Item> out = new ArrayList<>();
        for (HeroKind k : HeroKind.values()) out.add(HeroItems.get(k).get());
        out.addAll(GemSystem.creativeItems());
        return out;
    }
}
