package com.newuniverse.nusmp.franchise;

import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.RelicSlot;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Optional;

/**
 * The franchise engravings on the items. An item of the mod that has a franchise in {@code data/nusmp/relics/franchise_map.json} carries that franchise's engravings (the 2 or 3
 * datapack enchantments {@code nusmp:fr_*} of {@link FranchiseData}), put on the stack once, the first time it sits in a player's inventory, like the Tensura engravings of
 * {@code item.WeaponEngravings}; it has its OWN marker ({@value #FLAG}) so the older marker ("NUEngraved") and the older engravings are untouched. A relic's engraving level follows
 * its grade (1 up to rare, 2 unique and legend, 3 from god), every other item gets level 1. Food, potions and spawn eggs are never engraved.
 * <p>
 * Server side only. Also builds the franchise line of the tooltip (a plain text component; the client just prints it).
 */
public final class FranchiseEngravings {
    private FranchiseEngravings() {}

    /** The stack marker: set once the franchise engravings were put on. */
    public static final String FLAG = "NUFranchise";

    /** Marks "this item has no franchise" in {@link #BY_ITEM} (a ConcurrentHashMap cannot hold null). */
    private static final String NONE = "";
    /**
     * 1.112.0 (lag audit): the franchise of each item, worked out once. It was a registry lookup, a namespace test and a walk over every prefix of franchise_map.json for every item of every
     * inventory every second (and once per frame for every hovered tooltip). The map is bounded by the number of registered items.
     */
    private static final java.util.Map<Item, String> BY_ITEM = new java.util.concurrent.ConcurrentHashMap<>();

    /** The franchise id of a stack's item, or null (not this mod's, or no franchise). */
    public static String franchiseOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        Item item = stack.getItem();
        String f = BY_ITEM.get(item);
        if (f == null) {
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
            String found = "nusmp".equals(key.getNamespace()) ? FranchiseMap.franchiseOf(key.getPath()) : null;
            f = found == null ? NONE : found;
            BY_ITEM.put(item, f);
        }
        return f.isEmpty() ? null : f;
    }

    /** Forgets the item answers (a data reload may change the map). */
    public static void clearCaches() {
        BY_ITEM.clear();
        LINES.clear();
    }

    /** Whether this kind of stack can carry an engraving at all (not something you eat or drink or spawn from). */
    public static boolean engravable(ItemStack stack) {
        if (stack.isEmpty() || stack.has(DataComponents.FOOD) || stack.has(DataComponents.POTION_CONTENTS) || stack.getItem() instanceof SpawnEggItem) return false;
        RelicDef def = Relics.defOf(stack);
        return def == null || def.slot() != RelicSlot.CONSUMABLE;
    }

    /** The level this stack's engravings get. */
    public static int levelOf(ItemStack stack) {
        RelicDef def = Relics.defOf(stack);
        return def == null ? 1 : FranchiseRules.levelForGrade(def.grade().ordinal());
    }

    /** Engraves the stack with its franchise's engravings if it has a franchise and has not been done yet; true if it changed. */
    public static boolean engrave(ItemStack stack, HolderLookup.Provider registries) {
        String fid = franchiseOf(stack);
        if (fid == null) return false;
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd != null && cd.contains(FLAG)) return false;                       // 1.112.0: no copy of the whole tag for a question that is asked every second
        if (!engravable(stack)) return false;
        HolderLookup.RegistryLookup<Enchantment> lookup = registries.lookupOrThrow(Registries.ENCHANTMENT);
        int level = levelOf(stack);
        for (FranchiseData.Engraving en : FranchiseData.table().of(fid)) {
            Optional<? extends Holder<Enchantment>> h = lookup.get(ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath("nusmp", en.id())));
            if (h.isEmpty()) continue;                                              // a datapack without it: skipped, nothing breaks
            int lv = Math.max(1, Math.min(level, h.get().value().getMaxLevel()));
            if (stack.getEnchantments().getLevel(h.get()) < lv) stack.enchant(h.get(), lv);
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, (CompoundTag t) -> t.putBoolean(FLAG, true));
        return true;
    }

    /** The franchise line of the tooltip ("Franchise: Black Clover", in the franchise's colour), or null when the item has none. */
    public static Component tooltipLine(ItemStack stack) {
        String fid = franchiseOf(stack);
        if (fid == null) return null;
        Component line = LINES.get(fid);
        if (line == null) {
            FranchiseData.Franchise f = FranchiseData.table().franchise(fid);
            if (f == null) return null;
            Component name = Component.translatableWithFallback("franchise.nusmp." + fid, f.name());
            line = Component.translatableWithFallback("tooltip.nusmp.franchise", "Franchise: %s", name).withColor(f.colour());
            LINES.put(fid, line);
        }
        return line;
    }

    /**
     * 1.112.0 (lag audit): the tooltip line of each franchise, built once. The tooltip of a hovered item is gathered again every frame; this used to search the franchise table and build two
     * components each time. The line is translatable text, so a language change still shows (the component translates when it is drawn). Never changed after it is stored.
     */
    private static final java.util.Map<String, Component> LINES = new java.util.concurrent.ConcurrentHashMap<>();
}
