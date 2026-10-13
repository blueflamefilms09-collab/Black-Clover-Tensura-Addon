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

    public static void init() {}

    public static Item seal(Djinn d) { return SEALS.get(d).get(); }

    public static ItemStack sealStack(Djinn d) { return new ItemStack(seal(d)); }

    /** The Djinn a stack is the Seal of, or null. */
    public static Djinn djinnOf(ItemStack s) {
        return s != null && !s.isEmpty() && s.getItem() instanceof DjinnSealItem seal ? seal.djinn() : null;
    }

    /** All twelve, in roster order: for the creative tab. */
    public static List<DeferredItem<Item>> allSeals() { return new ArrayList<>(SEALS.values()); }
}
