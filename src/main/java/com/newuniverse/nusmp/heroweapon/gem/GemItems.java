package com.newuniverse.nusmp.heroweapon.gem;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Why: nineteen items (the Holy Calling Gem, the Gem Fragment and one Vassal Seal for each weapon that is not Holy) are registered in a loop over the weapon
 * catalogue, so that a weapon appended to the enum gets its Seal item without anyone listing it by hand, and so that the creative tab, the tests and the
 * self-test iterate the same set. Each Seal is an item of its own and not one item with a data component naming the weapon (as the arc spawn egg does for
 * the characters): a component-carrying stack is a single entry to a recipe viewer, so seventeen Seals would be seventeen look-alike recipes under one
 * name, a quest reward could not name one as a plain item id, and the creative tab (which takes items, not stacks, through
 * {@code HeroWeaponSystem.creativeItems}) could not list them without a new line in a lead-owned class. The register is the system's own, attached to the
 * mod bus by {@link GemSystem#init}.
 */
public final class GemItems {
    private GemItems() {}

    public static final DeferredRegister.Items REGISTER = DeferredRegister.createItems(NUSMP.MODID);

    private static final Map<HeroKind, DeferredItem<CallingGemItem>> SEALS = new EnumMap<>(HeroKind.class);

    /** {@code nusmp:calling_gem}: the Holy Calling Gem, which makes one of the four Legendary Weapons choose its holder. */
    public static final DeferredItem<CallingGemItem> CALLING_GEM;
    /** {@code nusmp:gem_fragment}: the shard every Vassal Seal is made from. */
    public static final DeferredItem<GemFragmentItem> GEM_FRAGMENT;

    static {
        CALLING_GEM = REGISTER.register(GemCatalog.HOLY_ID, () -> new CallingGemItem(null, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
        GEM_FRAGMENT = REGISTER.register(GemCatalog.FRAGMENT_ID, () -> new GemFragmentItem(new Item.Properties().rarity(Rarity.UNCOMMON)));
        for (HeroKind k : GemCatalog.sealKinds())
            SEALS.put(k, REGISTER.register(GemCatalog.sealId(k), () -> new CallingGemItem(k, new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant())));
    }

    /** The Seal item of a weapon, or null for a Holy Weapon (which only the Holy Calling Gem can choose for a player). */
    public static CallingGemItem seal(HeroKind kind) {
        DeferredItem<CallingGemItem> d = SEALS.get(kind);
        return d == null ? null : d.get();
    }

    /** A fresh Holy Calling Gem. */
    public static ItemStack holyStack() { return new ItemStack(CALLING_GEM.get()); }

    /** A fresh Seal of a weapon; an empty stack for a Holy Weapon. */
    public static ItemStack sealStack(HeroKind kind) {
        CallingGemItem item = seal(kind);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    /** Gem Fragments. */
    public static ItemStack fragmentStack(int count) { return new ItemStack(GEM_FRAGMENT.get(), Math.max(1, Math.min(64, count))); }

    /** True when the stack is a Holy Calling Gem or a Seal. */
    public static boolean isGem(ItemStack stack) { return !stack.isEmpty() && stack.getItem() instanceof CallingGemItem; }

    /** The weapon a Seal stack calls, or null for anything else (the Holy Calling Gem calls whichever Legendary Weapon is free). */
    public static HeroKind sealKind(ItemStack stack) { return isGem(stack) ? ((CallingGemItem) stack.getItem()).kind() : null; }

    /** All nineteen items in creative-tab order: the Holy Calling Gem, the fragment, then the Seals in catalogue order. */
    public static List<Item> all() {
        List<Item> out = new ArrayList<>();
        out.add(CALLING_GEM.get());
        out.add(GEM_FRAGMENT.get());
        for (HeroKind k : GemCatalog.sealKinds()) out.add(seal(k));
        return out;
    }
}
