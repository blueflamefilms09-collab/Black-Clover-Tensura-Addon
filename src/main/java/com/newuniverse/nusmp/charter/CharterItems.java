package com.newuniverse.nusmp.charter;

import com.newuniverse.nusmp.item.NUItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The seven Charter Grimoire items {@code charter_grimoire_<bossId>}, registered with the mod's other items. {@link #init()} must be called
 * before {@code NUItems.ITEMS.register(modBus)}.
 */
public final class CharterItems {
    private CharterItems() {}

    private static final Map<String, DeferredItem<CharterGrimoireItem>> BY_BOSS = new LinkedHashMap<>();

    static {
        for (CharterTable.Boss b : CharterTable.all()) {
            BY_BOSS.put(b.id(), NUItems.ITEMS.register(b.itemId(),
                    () -> new CharterGrimoireItem(b, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant())));
        }
    }

    public static void init() {}

    /** The item of a boss id, or null. */
    public static Item item(String bossId) {
        DeferredItem<CharterGrimoireItem> d = BY_BOSS.get(bossId);
        return d == null ? null : d.get();
    }

    /** All seven, for the creative tab. */
    public static List<Item> all() {
        List<Item> out = new ArrayList<>();
        for (DeferredItem<CharterGrimoireItem> d : BY_BOSS.values()) out.add(d.get());
        return out;
    }
}
