package com.newuniverse.nusmp.relics.socket;

import com.newuniverse.nusmp.item.NUItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The fifteen Cores, registered with the mod's other items: {@code spirit_core_<element>} (six) and {@code devil_core_<devil>} (nine). Call
 * {@link #init()} from the mod constructor before {@code NUItems.ITEMS} is put on the mod bus. The 16x16 icons are tools/relics/gen_core_icons.py's.
 */
public final class SocketItems {
    private SocketItems() {}

    private static final Map<String, DeferredItem<Item>> CORES = new LinkedHashMap<>();

    static {
        for (SocketDef d : SocketCatalog.all()) {
            Rarity r = d.kind() == SocketKind.DEVIL ? Rarity.EPIC : Rarity.RARE;
            CORES.put(d.id(), NUItems.ITEMS.register(d.coreItemId(), () -> new SocketCoreItem(d, new Item.Properties().stacksTo(16).rarity(r).fireResistant())));
        }
    }

    public static void init() {}

    /** The Core item of a socket id ("spirit:fire"), or null. */
    public static Item item(String socketId) {
        DeferredItem<Item> h = socketId == null ? null : CORES.get(socketId);
        return h == null ? null : h.get();
    }

    public static ItemStack stack(String socketId) {
        Item i = item(socketId);
        return i == null ? ItemStack.EMPTY : new ItemStack(i);
    }

    /** The socket a Core stack stands for, or null if it is not a Core. */
    public static SocketDef defOf(ItemStack s) {
        return s != null && !s.isEmpty() && s.getItem() instanceof SocketCoreItem c ? c.socket() : null;
    }

    /** All fifteen, in catalog order: for the creative tab. */
    public static List<DeferredItem<Item>> all() { return new ArrayList<>(CORES.values()); }
}
