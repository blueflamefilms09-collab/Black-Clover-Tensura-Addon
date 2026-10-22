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
        Kept k = kept(stack.getItem());
        int r = Math.max(0, Math.min(k.styles.length - 1, stack.getRarity().ordinal()));
        UnifiedTooltipRules.Style s = k.styles[r];
        if (s == null || !java.util.Objects.equals(k.labels[r], categoryLabel)) {              // first use, or the language changed
            s = UnifiedTooltipRules.style(k.category, r, categoryLabel);
            k.styles[r] = s;
            k.labels[r] = categoryLabel;
        }
        return s;
    }

    /** The category of an item (for the self-test). */
    public static UnifiedTooltipRules.Category categoryOf(Item item) { return kept(item).category; }

    /**
     * 1.112.0 (lag audit): what the tooltip works out for an item, kept. The tooltip of a hovered stack is gathered (and its frame coloured) on every frame; each time it walked the item's class
     * chain, joined and lower-cased the names, made an item stack to look for the food component and built three strings. The category depends on the item alone, the style on the item,
     * its rarity and the language of the label, so both are kept per item (one entry per registered item at most, filled when it is first hovered).
     */
    private static final class Kept {
        final UnifiedTooltipRules.Category category;
        final UnifiedTooltipRules.Style[] styles = new UnifiedTooltipRules.Style[8];
        final String[] labels = new String[8];

        Kept(UnifiedTooltipRules.Category category) { this.category = category; }
    }

    private static final java.util.Map<Item, Kept> KEPT = new java.util.concurrent.ConcurrentHashMap<>();

    private static Kept kept(Item item) {
        Kept k = KEPT.get(item);
        if (k == null) {
            k = new Kept(UnifiedTooltipRules.classify(hintsOf(item)));
            KEPT.put(item, k);
        }
        return k;
    }
}
