package com.newuniverse.nusmp.dungeon.sentient;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** One of the Dungeon's rare synthesis materials: a plain item that says what it is and which creature leaves it behind. */
public class MaterialItem extends Item {
    private final String material;

    public MaterialItem(String material, Item.Properties props) {
        super(props);
        this.material = material;
    }

    /** The item's path ("goblin_dagger"). */
    public String material() { return material; }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.literal("Synthesis material").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Dropped by " + MaterialTable.source(material)).withStyle(ChatFormatting.DARK_GRAY));
    }
}
