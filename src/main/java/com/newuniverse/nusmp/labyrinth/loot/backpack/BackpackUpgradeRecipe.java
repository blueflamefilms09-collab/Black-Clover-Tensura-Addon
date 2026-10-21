package com.newuniverse.nusmp.labyrinth.loot.backpack;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Why: a plain shapeless recipe would make a NEW backpack and lose what was in the old one, and that is the one thing an upgrade must never do. So this is a special crafting
 * recipe: put a backpack and an upgrade item anywhere in the grid (and nothing else) and the result is that SAME backpack, copied with every component it had (its contents, its
 * name), at the bigger tier. It only matches when the upgrade is really an upgrade ({@link BackpackTier#upgraded}: a pack is never made smaller or the same size), so a netherite
 * pack cannot be "upgraded" with an iron one to spend the item for nothing. The pack and the upgrade are both used up, as in any recipe. The shipped data file is
 * {@code data/nusmp/recipe/backpack_upgrade.json} (type {@code nusmp:backpack_upgrade}); the recipes that make the backpack and the four upgrade items are ordinary shaped recipes.
 */
public class BackpackUpgradeRecipe extends CustomRecipe {
    public BackpackUpgradeRecipe(CraftingBookCategory category) { super(category); }

    /** The backpack and the upgrade in the grid, or null when it is not exactly those two. */
    private static ItemStack[] pair(CraftingInput input) {
        ItemStack pack = null, up = null;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty()) continue;
            if (s.getItem() instanceof BackpackItem && pack == null) pack = s;
            else if (s.getItem() instanceof BackpackUpgradeItem && up == null) up = s;
            else return null;
        }
        return pack == null || up == null ? null : new ItemStack[]{pack, up};
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        ItemStack[] p = pair(input);
        if (p == null) return false;
        BackpackTier now = BackpackItem.tierOf(p[0]);
        return BackpackTier.upgraded(now, ((BackpackUpgradeItem) p[1].getItem()).tier()) != now;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack[] p = pair(input);
        if (p == null) return ItemStack.EMPTY;
        BackpackTier result = BackpackTier.upgraded(BackpackItem.tierOf(p[0]), ((BackpackUpgradeItem) p[1].getItem()).tier());
        ItemStack out = p[0].copyWithCount(1);
        BackpackItem.setTier(out, result);
        return out;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) { return width * height >= 2; }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) { return BackpackItem.of(BackpackTier.IRON); }

    @Override
    public RecipeSerializer<?> getSerializer() { return BackpackRegistry.UPGRADE_RECIPE.get(); }
}
