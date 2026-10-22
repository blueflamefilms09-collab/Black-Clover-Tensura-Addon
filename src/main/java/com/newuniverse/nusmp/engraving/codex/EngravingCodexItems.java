package com.newuniverse.nusmp.engraving.codex;

import com.newuniverse.nusmp.item.NUItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;

/** The item of the Engravement Codex. Call {@link #init()} from the mod constructor before {@code NUItems.ITEMS.register(modBus)}. */
public final class EngravingCodexItems {
    private EngravingCodexItems() {}

    /** {@code nusmp:engraving_codex}: a grimoire-style book that opens the list of every engraving of the mod. */
    public static final DeferredItem<EngravingCodexItem> CODEX = NUItems.ITEMS.register("engraving_codex",
            () -> new EngravingCodexItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static void init() {}

    public static ItemStack codexStack() { return new ItemStack(CODEX.get()); }
}
