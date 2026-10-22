package com.newuniverse.nusmp.firecast;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.List;

/**
 * Why: Magna Swing casts his Fire spells with a bat, as in the show: the spell is a ball tossed in front and the bat sends it. This is that bat, "Magna's Soul Bat": a dark wooden club studded
 * with flame, a real 3D weapon (tools/gen_magna_bat_model.py, drawn by client.WeaponRenderer) with Tensura engravings (item.WeaponEngravings) and a Better Combat club profile. In melee it is
 * an ordinary heavy club; its magic is the casting style in {@link FireCasting} (the Fire pages of a Magna grimoire cannot be cast without it in the main hand). It does nothing on use: the
 * server decides everything in the cast path, the item only has to exist and be held.
 */
public class MagnasSoulBatItem extends Item {
    public MagnasSoulBatItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant().attributes(ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 6.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -2.8, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build()));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatableWithFallback("item.nusmp.magnas_soul_bat.lore", "A flame-studded club. Magna Swing casts through it: the spell is a ball tossed ahead, the swing sends it.")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tip.add(Component.translatableWithFallback("item.nusmp.magnas_soul_bat.use", "Hold it in the main hand to cast the Fire pages of Magna's grimoire.").withStyle(ChatFormatting.GOLD));
    }
}
