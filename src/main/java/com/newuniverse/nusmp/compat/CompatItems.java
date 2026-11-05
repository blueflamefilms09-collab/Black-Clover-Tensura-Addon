package com.newuniverse.nusmp.compat;

import com.newuniverse.nusmp.item.NUItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;

/** Items of the compatibility section. Call {@link #init()} from the mod constructor before {@code NUItems.ITEMS.register(modBus)}. */
public final class CompatItems {
    private CompatItems() {}

    /** {@code nusmp:compat_codex}: the Compatibility Codex, a grimoire-style book that opens a screen. */
    public static final DeferredItem<CompatCodexItem> CODEX = NUItems.ITEMS.register("compat_codex",
            () -> new CompatCodexItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    /** {@code nusmp:multiverse_wiki}: the craftable in-game wiki book (3.53.0); right-click opens the Codex on its Wiki tab. */
    public static final DeferredItem<WikiBookItem> WIKI = NUItems.ITEMS.register("multiverse_wiki",
            () -> new WikiBookItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static void init() {}

    public static ItemStack codexStack() { return new ItemStack(CODEX.get()); }
}
