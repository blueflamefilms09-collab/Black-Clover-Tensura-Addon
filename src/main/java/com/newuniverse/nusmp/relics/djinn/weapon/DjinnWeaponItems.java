package com.newuniverse.nusmp.relics.djinn.weapon;

import com.newuniverse.nusmp.item.NUItems;
import com.newuniverse.nusmp.relics.djinn.Djinn;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The 24 Djinn weapon items ({@code djinn_weapon_baal} ... {@code djinn_weapon_vassago} and the same with {@code _dark}), registered with the mod's
 * other items. {@link #init()} must run before the item register is put on the mod bus; {@code DjinnItems.init()} calls it, so the call the mod
 * constructor already makes is enough. The temporary weapons are not in the survival creative tab by default: {@link #creativeStacks()} is the list for
 * the admin tab.
 */
public final class DjinnWeaponItems {
    private DjinnWeaponItems() {}

    private static final Map<Djinn, DeferredItem<Item>> LIGHT = new EnumMap<>(Djinn.class), DARK = new EnumMap<>(Djinn.class);

    static {
        for (Djinn d : Djinn.values()) {
            LIGHT.put(d, NUItems.ITEMS.register(DjinnWeaponRules.itemPath(d, false), () -> new DjinnWeaponItem(d, false, DjinnWeaponItem.props(d, false))));
            DARK.put(d, NUItems.ITEMS.register(DjinnWeaponRules.itemPath(d, true), () -> new DjinnWeaponItem(d, true, DjinnWeaponItem.props(d, true))));
        }
    }

    public static void init() {}

    public static Item item(Djinn d, boolean dark) { return (dark ? DARK : LIGHT).get(d).get(); }

    /** All 24, light then dark, in roster order: for the client registrations. */
    public static List<DeferredItem<Item>> all() {
        List<DeferredItem<Item>> out = new ArrayList<>();
        for (Djinn d : Djinn.values()) {
            out.add(LIGHT.get(d));
            out.add(DARK.get(d));
        }
        return out;
    }

    /** A bare weapon of a Djinn; it is adopted as an admin weapon by whoever holds it ({@link DjinnWeaponService#vet}). */
    public static ItemStack bare(Djinn d, boolean dark) { return new ItemStack(item(d, dark)); }

    /** The 24 bare stacks for the creative tab (admin only): light then dark, in roster order. */
    public static List<ItemStack> creativeStacks() {
        List<ItemStack> out = new ArrayList<>();
        for (Djinn d : Djinn.values()) {
            out.add(bare(d, false));
            out.add(bare(d, true));
        }
        return out;
    }

    public static boolean isDjinnWeapon(ItemStack s) { return s != null && !s.isEmpty() && s.getItem() instanceof DjinnWeaponItem; }
}
