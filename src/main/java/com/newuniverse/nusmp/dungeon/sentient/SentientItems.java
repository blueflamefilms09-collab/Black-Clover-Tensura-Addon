package com.newuniverse.nusmp.dungeon.sentient;

import com.newuniverse.nusmp.item.NUItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The twelve rare synthesis materials of the Dungeon (the owner's design: "occasionally, mobs drop rare synthesis materials, like a Direwolf
 * pelt or a Goblin dagger"), registered with the mod's other items. The ids are those of {@link MaterialTable}; what drops from whom is its
 * table, the tooltips are {@link MaterialItem}'s. The art is tools/gen_sentient_assets.py's.
 */
public final class SentientItems {
    private SentientItems() {}

    private static final Map<String, DeferredItem<Item>> BY_ID = new LinkedHashMap<>();

    public static final DeferredItem<Item> GOBLIN_DAGGER = reg("goblin_dagger");
    public static final DeferredItem<Item> KOBOLD_FANG = reg("kobold_fang");
    public static final DeferredItem<Item> SLIME_CORE = reg("slime_core");
    public static final DeferredItem<Item> AQUA_SLIME_GEL = reg("aqua_slime_gel");
    public static final DeferredItem<Item> DIREWOLF_PELT = reg("direwolf_pelt");
    public static final DeferredItem<Item> LIZARDMAN_SCALE = reg("lizardman_scale");
    public static final DeferredItem<Item> WATER_SPIRIT_ESSENCE = reg("water_spirit_essence");
    public static final DeferredItem<Item> MEGALODON_TOOTH = reg("megalodon_tooth");
    public static final DeferredItem<Item> DEVIL_HORN = reg("devil_horn");
    public static final DeferredItem<Item> DISCIPLE_SIGIL = reg("disciple_sigil");
    public static final DeferredItem<Item> BEAST_HIDE = reg("beast_hide");
    public static final DeferredItem<Item> JUGGERNAUT_HEART = reg("juggernaut_heart");

    private static DeferredItem<Item> reg(String id) {
        DeferredItem<Item> d = NUItems.ITEMS.register(id, () -> new MaterialItem(id, new Item.Properties().rarity(Rarity.UNCOMMON)));
        BY_ID.put(id, d);
        return d;
    }

    /** The item of a material by its path ("goblin_dagger"), or null if there is none. */
    public static Item item(String id) {
        DeferredItem<Item> d = BY_ID.get(id);
        return d == null ? null : d.get();
    }

    /** All twelve, in the order of the table: for the creative tab. */
    public static List<DeferredItem<Item>> all() { return new ArrayList<>(BY_ID.values()); }

    public static void init() {}
}
