package com.newuniverse.nusmp.heroweapon;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Why: 21 items, one per {@link HeroKind}, all of the one class {@link HeroWeaponItem}, registered in a loop over the catalogue so that adding a weapon to the
 * enum adds its item, and so that the renderer's list, the creative tab and the tests iterate the same set. The register is the mod's own (attached to the mod bus
 * by {@code HeroWeaponSystem.init}). The properties are the common ones of a legendary weapon (one in a stack, fire-proof, epic); the core builder sets the
 * attributes and use animation per style in {@link HeroWeaponItem}, which it owns.
 */
public final class HeroItems {
    private HeroItems() {}

    public static final DeferredRegister.Items REGISTER = DeferredRegister.createItems(NUSMP.MODID);

    private static final Map<HeroKind, DeferredItem<HeroWeaponItem>> ITEMS = new EnumMap<>(HeroKind.class);

    static {
        for (HeroKind k : HeroKind.values())
            ITEMS.put(k, REGISTER.register(k.itemId(), () -> new HeroWeaponItem(k, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant())));
    }

    /** The item of a weapon. */
    public static DeferredItem<HeroWeaponItem> get(HeroKind kind) { return ITEMS.get(kind); }

    /** All 21 items in catalogue order, as suppliers (for the renderer's list). */
    public static List<Supplier<? extends Item>> all() { return new ArrayList<>(ITEMS.values()); }

    /** The weapon a stack is, or null when it is not a hero weapon. */
    public static HeroKind kindOf(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof HeroWeaponItem w ? w.kind() : null;
    }

    /** A fresh stack of the weapon. */
    public static ItemStack stack(HeroKind kind) { return new ItemStack(ITEMS.get(kind).get()); }
}
