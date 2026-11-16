package com.newuniverse.nusmp.saiyan.armor;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Why: one piece of the craftable Saiyan alloy set. An ordinary {@code ArmorItem} (equips, shows its armour bar, wears out, mends at an anvil with an alloy ingot); the set bonus is read from the worn
 * pieces by {@link SaiyanArmorSet}. Its worn look is the mod's worn-model library (an armour item of the {@code anime_multiverse} namespace is drawn from the catalog entry of its path:
 * {@code relic_worn/catalog.txt}), and the vanilla layer texture of the material is the plain version for the places the library does not draw (a mob, an armour stand).
 */
public class SaiyanArmorItem extends ArmorItem {
    public SaiyanArmorItem(Holder<ArmorMaterial> material, Type type) {
        super(material, type, new Item.Properties().stacksTo(1).durability(type.getDurability(SaiyanArmorRules.DURABILITY_FACTOR)).rarity(Rarity.UNCOMMON));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatableWithFallback("tooltip.anime_multiverse.saiyan_alloy", "Saiyan alloy battle armour. Mend it with an alloy ingot.").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatableWithFallback("tooltip.anime_multiverse.saiyan_alloy.set", "Full set (4 pieces): a Saiyan regains Ki faster.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
