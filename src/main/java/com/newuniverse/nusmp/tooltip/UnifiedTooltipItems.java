package com.newuniverse.nusmp.tooltip;

import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the game-side half of the unified tooltip. It reads an item (its registry id, its class chain, the vanilla families it belongs to, its rarity) and hands the plain
 * facts to {@link UnifiedTooltipRules}. Common code with no client class, so the CI server smoke test can run it over every registered item. Nothing here changes an item.
 */
public final class UnifiedTooltipItems {
    private UnifiedTooltipItems() {}

    /** The facts about this item. */
    public static UnifiedTooltipRules.Hints hintsOf(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        List<String> names = new ArrayList<>();
        for (Class<?> c = item.getClass(); c != null && c != Object.class && c != Item.class; c = c.getSuperclass()) names.add(c.getSimpleName());
        boolean food = item.getDefaultInstance().has(DataComponents.FOOD);
        return new UnifiedTooltipRules.Hints(id == null ? "" : id.getPath(), names, item instanceof BlockItem, food, item instanceof ArmorItem,
                item instanceof SwordItem || item instanceof ProjectileWeaponItem || item instanceof TridentItem, item instanceof DiggerItem);
    }

    /** Does this stack get the unified tooltip at all? */
    public static boolean applies(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) return false;
        return UnifiedTooltipRules.applies(id.getNamespace(), stack.getItem() instanceof SpawnEggItem, Relics.defOf(stack) != null);
    }

    /** The style of this stack; {@code categoryLabel} is the translated label of its category (null = English). */
    public static UnifiedTooltipRules.Style styleOf(ItemStack stack, String categoryLabel) {
        return UnifiedTooltipRules.style(hintsOf(stack.getItem()), stack.getRarity().ordinal(), categoryLabel);
    }

    /** The category of an item (for the self-test). */
    public static UnifiedTooltipRules.Category categoryOf(Item item) { return UnifiedTooltipRules.classify(hintsOf(item)); }
}
