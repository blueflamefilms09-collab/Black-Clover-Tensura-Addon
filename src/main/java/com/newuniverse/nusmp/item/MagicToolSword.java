package com.newuniverse.nusmp.item;

import com.newuniverse.nusmp.NUConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Magic Tool Sword / Spear: hits conduct your grimoire's attribute. */
public class MagicToolSword extends SwordItem {
    public MagicToolSword(Tier tier, Item.Properties props) { super(tier, props); }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("Attribute conduction: +" + NUConfig.GEAR_CONDUCTION_DAMAGE.get() + " damage of your grimoire's attribute per hit")
                .withStyle(ChatFormatting.GOLD));
    }
}
