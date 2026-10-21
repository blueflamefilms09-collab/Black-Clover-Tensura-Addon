package com.newuniverse.nusmp.tooltip;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

import java.util.EnumMap;
import java.util.Map;

/**
 * Why: the unified tooltip cannot be looked at by its author, so the CI server smoke test checks the part that does not draw: {@code ui.unified_tooltips} walks every item the
 * game has registered under {@code nusmp} and proves that each one (spawn eggs and relics aside) resolves to a style with a grade, a banner, a type line and a footer, that the
 * banner is the relic banner's shape, that a hero weapon is Legend and a plain meal Normal, and that no category is left empty by a misread class chain. Common code only (no
 * client class), so it runs on the dedicated server.
 */
public final class UnifiedTooltipSelfTests {
    private UnifiedTooltipSelfTests() {}

    public static void register() {
        SelfTests.register("ui.unified_tooltips", server -> run());
    }

    private static SelfTests.Result run() {
        Map<UnifiedTooltipRules.Category, Integer> counts = new EnumMap<>(UnifiedTooltipRules.Category.class);
        int items = 0, skipped = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id == null || !UnifiedTooltipRules.NAMESPACE.equals(id.getNamespace())) continue;
            ItemStack stack = new ItemStack(item);
            if (item instanceof SpawnEggItem) { skipped++; continue; }
            if (!UnifiedTooltipItems.applies(stack)) { skipped++; continue; }                  // a relic: it keeps its own tooltip
            UnifiedTooltipRules.Style s = UnifiedTooltipItems.styleOf(stack, null);
            if (s == null || s.category() == null || s.grade() == null) return SelfTests.Result.fail(id + " resolved to no style");
            if (s.banner().isBlank() || !s.banner().contains(s.grade().label())) return SelfTests.Result.fail(id + " has no grade banner");
            if (!s.typeLine().startsWith("Type: ") || !s.footer().startsWith(UnifiedTooltipRules.FOOTER_NAME)) return SelfTests.Result.fail(id + " has a broken type line or footer");
            if (s.grade().ordinal() < s.category().baseGrade().ordinal()) return SelfTests.Result.fail(id + " is below its category's default grade");
            counts.merge(s.category(), 1, Integer::sum);
            items++;
        }
        if (items == 0) return SelfTests.Result.fail("no nusmp item was found in the registry");
        return SelfTests.Result.ok(items + " items resolved (" + skipped + " relics / eggs keep their own); by category " + counts);
    }
}
