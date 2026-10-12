package com.newuniverse.nusmp.dungeon.nexus.nav;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** A Venom Gland (docs/living_dungeon.md section 6): cut out of a Basilisk or a spider with the Butcher Cleaver, striking high on the flank of the carcass. Only an ingredient. */
final class VenomGlandItem extends Item {
    VenomGlandItem(Item.Properties props) { super(props); }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("A potent poison, and a cure for nothing.").withStyle(ChatFormatting.DARK_GREEN));
        tip.add(Component.literal("Cut from a basilisk or a spider: strike the upper flank of the carcass with a Butcher Cleaver.").withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("An ingredient for the cooks of the deep.").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
