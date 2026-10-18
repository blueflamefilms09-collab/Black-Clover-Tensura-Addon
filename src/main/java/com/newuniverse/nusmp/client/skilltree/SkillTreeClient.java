package com.newuniverse.nusmp.client.skilltree;

import net.neoforged.bus.api.IEventBus;

/**
 * Why: the one client entry point of the Skill Tree (the screen, the button that is added to the existing Four Kingdoms and Multiverse Status menus, the tooltips); it also starts the client parts of the items and
 * the shop so that the mod constructor has one line. Owned by the GUI builder, who replaces this stub and keeps the two calls below.
 */
public final class SkillTreeClient {
    private SkillTreeClient() {}

    public static void init(IEventBus modBus) {
        ItemsClient.init(modBus);
        ShopClient.init(modBus);
    }
}
