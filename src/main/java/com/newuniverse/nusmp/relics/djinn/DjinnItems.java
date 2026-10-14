package com.newuniverse.nusmp.relics.djinn;

import com.newuniverse.nusmp.item.NUItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The twelve Djinn Seals ({@code djinn_seal_baal} ... {@code djinn_seal_vassago}), registered with the mod's other items. The art is
 * tools/relics/gen_djinn_art.py's. Call {@link #init()} from the mod constructor before the item register is put on the mod bus.
 */
public final class DjinnItems {
    private DjinnItems() {}

    private static final Map<Djinn, DeferredItem<Item>> SEALS = new EnumMap<>(Djinn.class);

    static {
        for (Djinn d : Djinn.values())
            SEALS.put(d, NUItems.ITEMS.register("djinn_seal_" + d.id(), () -> new DjinnSealItem(d, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant())));
    }

    /** The Dark Rukh Shard (relics.djinn.dark): black rukh that turns a bound Djinn dark. Registered after the Seals; art by tools/relics/dark_item_icon.py. */
    private static final DeferredItem<Item> DARK_SHARD = NUItems.ITEMS.register("dark_rukh_shard",
            () -> new com.newuniverse.nusmp.relics.djinn.dark.DarkRukhShardItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)));

    public static void init() { com.newuniverse.nusmp.relics.djinn.weapon.DjinnWeaponItems.init(); }       // the 24 Djinn weapons register in the same item register, before it goes on the bus

    public static Item seal(Djinn d) { return SEALS.get(d).get(); }

    public static ItemStack sealStack(Djinn d) { return new ItemStack(seal(d)); }

    /** The Djinn a stack is the Seal of, or null. */
    public static Djinn djinnOf(ItemStack s) {
        return s != null && !s.isEmpty() && s.getItem() instanceof DjinnSealItem seal ? seal.djinn() : null;
    }

    /** The Dark Rukh Shard item. */
    public static Item darkShard() { return DARK_SHARD.get(); }

    public static ItemStack darkShardStack() { return new ItemStack(DARK_SHARD.get()); }

    public static boolean isDarkShard(ItemStack s) { return s != null && !s.isEmpty() && s.is(DARK_SHARD.get()); }

    /** The Dark Rukh Shard as a creative-tab entry (the lead adds it next to the Seals). */
    public static DeferredItem<Item> darkShardEntry() { return DARK_SHARD; }

    /** All twelve, in roster order: for the creative tab. */
    public static List<DeferredItem<Item>> allSeals() { return new ArrayList<>(SEALS.values()); }
}
