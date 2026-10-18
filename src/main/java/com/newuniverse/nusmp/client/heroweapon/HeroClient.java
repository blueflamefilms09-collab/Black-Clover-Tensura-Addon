package com.newuniverse.nusmp.client.heroweapon;

import com.newuniverse.nusmp.client.heroweapon.curse.CurseClient;
import com.newuniverse.nusmp.client.heroweapon.forms.FormsClient;
import net.neoforged.bus.api.IEventBus;

/**
 * Why: the one client entry point of the hero weapons (the keybinds, the HUD, the look of a summoned weapon, the payload receivers of the core), called once from
 * the mod constructor's client block; it also starts the forms and curse client parts so that the mod constructor has one line. Owned by the core builder, who
 * adds its client code here; {@code FormsClient} and {@code CurseClient} are theirs.
 */
public final class HeroClient {
    private HeroClient() {}

    public static void init(IEventBus modBus) {
        FormsClient.init(modBus);
        CurseClient.init(modBus);
    }
}
