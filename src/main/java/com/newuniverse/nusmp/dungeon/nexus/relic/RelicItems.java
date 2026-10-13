package com.newuniverse.nusmp.dungeon.nexus.relic;

import com.newuniverse.nusmp.item.NUItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.EnumMap;
import java.util.Map;

/** The five relic items (ids fixed by the Nexus contract), registered with the mod's other items. Their art is the relic-art pass's. */
public final class RelicItems {
    private RelicItems() {}

    private static final Map<Relic, DeferredItem<RelicItem>> BY_RELIC = new EnumMap<>(Relic.class);

    public static final DeferredItem<RelicItem> GLUTTON_CHALICE = reg(Relic.GLUTTONS_CHALICE, Rarity.RARE);
    public static final DeferredItem<RelicItem> MANTLE_OF_THE_SCAVENGER = reg(Relic.MANTLE_OF_THE_SCAVENGER, Rarity.RARE);
    public static final DeferredItem<RelicItem> BAND_OF_THE_SPATIAL_ANCHOR = reg(Relic.BAND_OF_THE_SPATIAL_ANCHOR, Rarity.RARE);
    public static final DeferredItem<RelicItem> DEVIL_BOUND_SHEATH = reg(Relic.DEVIL_BOUND_SHEATH, Rarity.EPIC);
    public static final DeferredItem<RelicItem> GAMBLERS_GRIMOIRE_PAGE = reg(Relic.GAMBLERS_GRIMOIRE_PAGE, Rarity.EPIC);

    private static DeferredItem<RelicItem> reg(Relic relic, Rarity rarity) {
        DeferredItem<RelicItem> d = NUItems.ITEMS.register(relic.id, () -> new RelicItem(relic, new Item.Properties().stacksTo(1).rarity(rarity).fireResistant()));
        BY_RELIC.put(relic, d);
        return d;
    }

    public static Item item(Relic relic) { return BY_RELIC.get(relic).get(); }

    public static void init() {}
}
