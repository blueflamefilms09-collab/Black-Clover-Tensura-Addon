package com.newuniverse.nusmp.heroweapon.gem;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Why: a Vassal Seal is crafted from a Gem Fragment plus the weapon's signature material, and a bare "amethyst-coloured shard" in a recipe viewer says
 * nothing about what it is for, so the fragment is a plain item with one tooltip line that names its purpose. Nothing else: it has no behaviour of its own.
 */
public class GemFragmentItem extends Item {
    public GemFragmentItem(Properties props) { super(props); }

    @Override
    public Component getName(ItemStack stack) {
        return GemText.msg(GemMessages.ITEM_FRAGMENT);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tip, TooltipFlag flag) {
        tip.add(GemText.msg(GemMessages.TIP_FRAGMENT).withStyle(ChatFormatting.GRAY));
    }
}
