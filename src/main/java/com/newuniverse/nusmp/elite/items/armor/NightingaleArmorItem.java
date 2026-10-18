package com.newuniverse.nusmp.elite.items.armor;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.function.BiPredicate;

/**
 * Why: one piece of the Nightingale set (hood, cloak, leggings, boots). A plain {@code ArmorItem} of the mod's namespace, so the mod's worn-model library draws it on a player as the generic robe
 * look (the cloak and hood words choose the robe silhouette; {@code ArmorLooks}) and as an object in hand or on the ground ({@code ArmorItemRenderer}), with the engravings of {@code ArmorEngravings}.
 * The painted vanilla armour layer is kept for everything else (a zombie or an armour stand wearing it, or the option {@code relicfx.models} off). To stop the painted layer and the 3D model being
 * drawn TOGETHER on a player, {@link #getArmorTexture} answers a fully transparent texture for the wearers the library draws ({@link #libraryDraws} is set by the client mod class; on a dedicated
 * server it stays null and nothing client-side is loaded). The set bonus is {@link NightingaleBonus}.
 */
public class NightingaleArmorItem extends ArmorItem {
    private static final ResourceLocation BLANK = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "textures/models/armor/blank_layer.png");

    /** Client only: does the model library draw this stack on this wearer (a player, with the 3D worn models on)? Null on a server. */
    public static volatile BiPredicate<Entity, ItemStack> libraryDraws;

    public NightingaleArmorItem(Holder<ArmorMaterial> material, ArmorItem.Type type) {
        super(material, type, new Item.Properties().stacksTo(1).rarity(Rarity.RARE).durability(type.getDurability(37)));
    }

    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        BiPredicate<Entity, ItemStack> p = libraryDraws;
        if (p != null && p.test(entity, stack)) return BLANK;
        return super.getArmorTexture(stack, entity, slot, layer, innerModel);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatableWithFallback("tooltip.nusmp.nightingale.set", "Nightingale set (4 pieces): Nightsong").withStyle(ChatFormatting.DARK_AQUA));
        tip.add(Component.translatableWithFallback("tooltip.nusmp.nightingale.speed", "  Speed I").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatableWithFallback("tooltip.nusmp.nightingale.sight", "  Night Vision in the dark").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatableWithFallback("tooltip.nusmp.nightingale.lullaby", "  Sneaking: the Lullaby heals you and your allies nearby").withStyle(ChatFormatting.GRAY));
    }
}
