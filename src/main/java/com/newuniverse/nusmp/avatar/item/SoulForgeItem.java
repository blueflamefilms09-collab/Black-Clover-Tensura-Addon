package com.newuniverse.nusmp.avatar.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * Why: the Soul Forge is the strangest block of the mod (it was built around a Genesis Core and it wakes a ritual), so its item says what it does in the
 * tooltip: where the Avatar's creation happens now, what it takes from you, and that it is reusable. In the hand and on the ground it is the 3D block model;
 * only the inventory icon is the 16x16 sprite (the item model uses {@code neoforge:separate_transforms}, as the Genesis Core does).
 */
public class SoulForgeItem extends BlockItem {
    public SoulForgeItem(Block block, Properties props) { super(block, props); }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        super.appendHoverText(stack, ctx, tip, flag);
        tip.add(Component.translatableWithFallback("item.nusmp.soul_forge.desc1", "Place it, hold a Genesis Core and use it: the ritual of the Avatar begins.").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatableWithFallback("item.nusmp.soul_forge.desc2", "Each Avatar costs one Genesis Core. The forge itself is never used up.").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
