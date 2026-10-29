package com.newuniverse.nusmp.client.skilltree;

import net.neoforged.bus.api.IEventBus;

/**
 * Why: the client side of what is left of the skill book package is the Black Market's screen in {@link ShopClient}, and the mod constructor should have ONE line for it, called only on a client so a
 * dedicated server never loads a client class. The Skill Books (cover colour, tooltip, unlock banner) were removed with the books; see {@code docs/skill_books_removed.md}.
 */
public final class SkillBookClient {
    private SkillBookClient() {}

    public static void init(IEventBus modBus) {
        ShopClient.init(modBus);
    }
}
