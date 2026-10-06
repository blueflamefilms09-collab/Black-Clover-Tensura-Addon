package com.newuniverse.nusmp.item;

import com.newuniverse.nusmp.NUConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Magic Tool Bow: arrows conduct your grimoire's attribute. */
public class MagicToolBow extends BowItem {
    public MagicToolBow(Item.Properties props) { super(props); }

    @Override
    public AbstractArrow customArrow(AbstractArrow arrow, ItemStack projectile, ItemStack weapon) {
        arrow.getPersistentData().putBoolean("nusmp_conduct", true);
        return arrow;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("Attribute conduction: +" + NUConfig.GEAR_CONDUCTION_DAMAGE.get() + " damage of your grimoire's attribute per arrow")
                .withStyle(ChatFormatting.GOLD));
    }
}
