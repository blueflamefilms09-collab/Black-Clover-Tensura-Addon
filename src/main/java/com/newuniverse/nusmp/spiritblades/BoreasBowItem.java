package com.newuniverse.nusmp.spiritblades;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Why: Spirit of Boreas, the leaf bow of the Wind Spirit Dive, as a creative-display item. It is a vanilla bow in every rule that matters (the same draw time, power, arrows and Infinity/Power
 * handling, because it is a plain {@link BowItem}); the one addition is the glowing trail its arrows leave, which the server draws for a short time through {@link SpiritBladesTrail} (capped:
 * see {@link SpiritBladesRules}). No key, no packet, no special magic. Its engraving (magic interference) is applied once by {@code item/WeaponEngravings}. In hand it is a 3D model with the
 * draw stages, drawn by {@code client/WeaponRenderer}.
 */
public class BoreasBowItem extends BowItem {
    public BoreasBowItem() {
        super(new Item.Properties().durability(SpiritBladesRules.BOW_DURABILITY).rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public AbstractArrow customArrow(AbstractArrow arrow, ItemStack projectile, ItemStack weapon) {
        SpiritBladesTrail.track(arrow);                                       // does nothing on a client level; capped per owner and over the server
        return arrow;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatableWithFallback("item.nusmp.spirit_of_boreas.lore", "The leaf bow of the Wind Spirit Boreas; its arrows trail light.").withStyle(ChatFormatting.GREEN));
    }
}
