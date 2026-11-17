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
 * The Djinn weapon items, two per {@link Djinn} row (72 for the 36 Djinn of 1.93.0: {@code djinn_weapon_baal} ... {@code djinn_weapon_murmur} and the same with {@code _dark}), registered with the mod's
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

    /** Belial rework: the canon Belial's scythe (owner notes), light and dark, registered next to the old greatsword weapons (which stay). */
    private static final DeferredItem<Item> CANON_LIGHT = NUItems.ITEMS.register(DjinnWeaponRules.itemPath(Djinn.BELIAL, false, true),
            () -> new DjinnWeaponItem(Djinn.BELIAL, false, true, DjinnWeaponItem.props(Djinn.BELIAL, false, true)));
    private static final DeferredItem<Item> CANON_DARK = NUItems.ITEMS.register(DjinnWeaponRules.itemPath(Djinn.BELIAL, true, true),
            () -> new DjinnWeaponItem(Djinn.BELIAL, true, true, DjinnWeaponItem.props(Djinn.BELIAL, true, true)));

    public static void init() {}

    /** The weapon of a Djinn: for Belial, the canon scythe while the setting {@code belial.belialCanon} is on, else the old item. */
    public static Item item(Djinn d, boolean dark) { return item(d, dark, d == Djinn.BELIAL && com.newuniverse.nusmp.relics.djinn.BelialCanon.on()); }

    /** The weapon of a Djinn, the canon scythe asked for explicitly (only Belial has one; anyone else gets their one weapon). */
    public static Item item(Djinn d, boolean dark, boolean canon) {
        if (canon && d == Djinn.BELIAL) return (dark ? CANON_DARK : CANON_LIGHT).get();
        return (dark ? DARK : LIGHT).get(d).get();
    }

    /** All of them (72, light then dark, in roster order) and then the canon Belial's two: for the client registrations. */
    public static List<DeferredItem<Item>> all() {
        List<DeferredItem<Item>> out = new ArrayList<>();
        for (Djinn d : Djinn.values()) {
            out.add(LIGHT.get(d));
            out.add(DARK.get(d));
        }
        out.add(CANON_LIGHT);
        out.add(CANON_DARK);
        return out;
    }

    /** A bare weapon of a Djinn; it is adopted as an admin weapon by whoever holds it ({@link DjinnWeaponService#vet}). */
    public static ItemStack bare(Djinn d, boolean dark) { return new ItemStack(item(d, dark)); }

    /** The 72 bare stacks for the creative tab (admin only): light then dark, in roster order, and then the canon Belial's two. */
    public static List<ItemStack> creativeStacks() {
        List<ItemStack> out = new ArrayList<>();
        for (Djinn d : Djinn.values()) {
            out.add(new ItemStack(item(d, false, false)));
            out.add(new ItemStack(item(d, true, false)));
        }
        out.add(new ItemStack(item(Djinn.BELIAL, false, true)));
        out.add(new ItemStack(item(Djinn.BELIAL, true, true)));
        return out;
    }

    public static boolean isDjinnWeapon(ItemStack s) { return s != null && !s.isEmpty() && s.getItem() instanceof DjinnWeaponItem; }
}
