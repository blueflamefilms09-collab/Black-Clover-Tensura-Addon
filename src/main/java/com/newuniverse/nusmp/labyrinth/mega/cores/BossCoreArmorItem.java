package com.newuniverse.nusmp.labyrinth.mega.cores;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Why: one piece of a Boss Core armour set. It is an ordinary {@code ArmorItem} (equips, shows its armour bar, wears out, mends at an anvil) whose material gives the armour points and toughness; what
 * makes it a set is the kit it belongs to, which {@link BossCoreEvents} reads from the four worn slots (the health or attack damage it adds is an attribute modifier of its own slot, added there so the numbers
 * follow the server config). It keeps the id of that modifier so the event builds no id per call.
 */
public class BossCoreArmorItem extends ArmorItem {
    private final BossCoreRules.Kit kit;
    private final BossCoreRules.Piece piece;
    private final ResourceLocation modifierId;

    public BossCoreArmorItem(Holder<ArmorMaterial> material, Type type, BossCoreRules.Kit kit, BossCoreRules.Piece piece, ResourceLocation modifierId) {
        super(material, type, new Item.Properties().stacksTo(1).durability(type.getDurability(45)).rarity(Rarity.EPIC).fireResistant());
        this.kit = kit;
        this.piece = piece;
        this.modifierId = modifierId;
    }

    public BossCoreRules.Kit kit() { return kit; }

    public BossCoreRules.Piece piece() { return piece; }

    ResourceLocation modifierId() { return modifierId; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatableWithFallback("tooltip.anime_multiverse.boss_set." + kit.id(), "A Boss Core set.").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatableWithFallback("tooltip.anime_multiverse.boss_set.bonus", "Two pieces and four pieces of one set add a bonus; mixed sets share a cap.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
