package com.newuniverse.nusmp.labyrinth.loot.backpack;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Why: an upgrade is a plain item that says which tier it makes a backpack. It does nothing by itself; two things use it: the crafting grid (a backpack and an upgrade make the
 * bigger backpack, contents kept: {@link BackpackUpgradeRecipe}) and the placed block (use the upgrade on a set-down backpack: {@link BackpackBlock#useItemOn}). Both go through the
 * pure {@link BackpackTier#upgraded}, so an upgrade never shrinks a pack. A stack of sixteen, because it is a crafted part and not a container.
 */
public class BackpackUpgradeItem extends Item {
    private final BackpackTier tier;

    public BackpackUpgradeItem(Properties props, BackpackTier tier) {
        super(props);
        this.tier = tier;
    }

    /** The tier this upgrade makes a backpack. */
    public BackpackTier tier() { return tier; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatableWithFallback("item.nusmp.backpack_upgrade.tier", "Makes a backpack %s slots (%s)", tier.slots(), tier.id()).withStyle(ChatFormatting.GOLD));
        tip.add(Component.translatableWithFallback("item.nusmp.backpack_upgrade.lore", "Combine it with a backpack in the crafting grid, or use it on a set-down backpack. The contents stay.").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
