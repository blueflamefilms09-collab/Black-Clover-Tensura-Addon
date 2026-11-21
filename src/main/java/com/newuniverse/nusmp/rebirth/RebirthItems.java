package com.newuniverse.nusmp.rebirth;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/** The two sigils (docs/rebirth.md). Their own register, so no shared item list changes; the creative tab reads {@link #creativeItems()}. */
public final class RebirthItems {
    private RebirthItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NUSMP.MODID);

    public static final DeferredItem<SigilItem> SIGIL_OF_REBIRTH = ITEMS.register("sigil_of_rebirth",
            () -> new SigilItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant(), RebirthRules.Rite.REBIRTH));
    public static final DeferredItem<SigilItem> SIGIL_OF_THE_SECOND_BOOK = ITEMS.register("sigil_of_the_second_book",
            () -> new SigilItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant(), RebirthRules.Rite.SECOND_BOOK));

    public static List<DeferredItem<SigilItem>> creativeItems() { return List.of(SIGIL_OF_REBIRTH, SIGIL_OF_THE_SECOND_BOOK); }
}
