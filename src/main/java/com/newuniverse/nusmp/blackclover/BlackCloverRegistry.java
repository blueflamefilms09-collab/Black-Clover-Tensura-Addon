package com.newuniverse.nusmp.blackclover;

import com.newuniverse.nusmp.NUSMP;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BlackCloverRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NUSMP.MODID);
    public static final DeferredItem<GrimoireItem> GRIMOIRE = ITEMS.register("grimoire", GrimoireItem::new);

    private BlackCloverRegistry() {}
}
