package com.newuniverse.nusmp.heroweapon;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
 * by {@code HeroWeaponSystem.init}). The properties are {@link HeroWeaponItem#props}: one in a stack, epic, fire-proof, unbreakable, with the weapon's melee
 * numbers from {@link HeroBaseStats} (damage, attack speed and reach as attribute modifiers), so the numbers a player reads in the tooltip are the table's.
 */
public final class HeroItems {
    private HeroItems() {}

    public static final DeferredRegister.Items REGISTER = DeferredRegister.createItems(NUSMP.MODID);

    private static final Map<HeroKind, DeferredItem<HeroWeaponItem>> ITEMS = new EnumMap<>(HeroKind.class);

    static {
        for (HeroKind k : HeroKind.values())
            ITEMS.put(k, REGISTER.register(k.itemId(), () -> new HeroWeaponItem(k, HeroWeaponItem.props(k))));
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
