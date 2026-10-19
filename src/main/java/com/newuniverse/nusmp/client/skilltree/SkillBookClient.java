package com.newuniverse.nusmp.client.skilltree;

import net.neoforged.bus.api.IEventBus;

/**
 * Why: the client side of the skill books is two small parts (the book item's cover colour, tooltip and unlock banner in {@link ItemsClient}, and the Black Market's screen in {@link ShopClient}) and the mod
 * constructor should have ONE line for both, called only on a client so a dedicated server never loads a client class. 1.96.0: the Skill Tree screen, the Arts bar, the Passives panel and the Tensura menu
 * panel are gone; a unique skill is shown and switched by Tensura's own Ability Selection screen, so the client needs nothing else.
 */
public final class SkillBookClient {
    private SkillBookClient() {}

    public static void init(IEventBus modBus) {
        ItemsClient.init(modBus);
        ShopClient.init(modBus);
    }
}
