package com.newuniverse.nusmp.relics.order;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

/**
 * The Notice of Order: a sheet of paper the God of Order hands to the person it took a duplicate from. Pure flavour, no use: it names the relics that
 * were struck from the ledger (in its lore) and says, politely, that no fault is recorded.
 */
public class NoticeOfOrderItem extends Item {
    public NoticeOfOrderItem(Properties properties) { super(properties); }

    /** A notice for these relics, as a stack. 'registered' is the item (OrderEntities.NOTICE). */
    static ItemStack make(Item registered, List<String> relicIds) {
        ItemStack stack = new ItemStack(registered);
        List<Component> lore = new ArrayList<>();
        for (String id : relicIds)
            lore.add(Component.literal("Two of ").withStyle(ChatFormatting.GRAY).append(OrderRelics.name(id).copy().withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(" cannot be.").withStyle(ChatFormatting.GRAY)));
        if (!lore.isEmpty()) stack.set(DataComponents.LORE, new ItemLore(lore));
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.literal(OrderText.NOTICE_LINE_1).withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        tooltip.add(Component.literal(OrderText.NOTICE_LINE_2).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
