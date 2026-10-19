package com.newuniverse.nusmp.requip;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.Unbreakable;

import java.util.List;

/**
 * Why: the armours of Requip: The Knight come with the weapon they are known for (Flame Empress's blade, Lightning Empress's halberd, Sea Empress's trident, Black Wing's blade, Nakagami's edge,
 * Purgatory's mace). It is a real item in the hand (drawn as a 3D model by {@code client.WeaponRenderer}, engraved with Tensura engravings by {@code item.WeaponEngravings}), made by the armour and
 * unmade with it: a stack carries the flag {@link RequipRules#WEAPON_FLAG}, and the skill removes every flagged stack when the armour is dismissed or swapped. It is unbreakable.
 */
public class RequipWeaponItem extends SwordItem {
    private final String lore;

    public RequipWeaponItem(double damage, float speed, Rarity rarity, String lore) {
        super(Tiers.NETHERITE, new Item.Properties().stacksTo(1).rarity(rarity).fireResistant()
                .component(DataComponents.UNBREAKABLE, new Unbreakable(false))
                .attributes(SwordItem.createAttributes(Tiers.NETHERITE, (float) damage, speed)));
        this.lore = lore;
    }

    /** True for a stack of any Requip weapon. */
    public static boolean is(ItemStack s) { return !s.isEmpty() && s.getItem() instanceof RequipWeaponItem; }

    /** True when the stack was requipped by the skill (and so goes with the armour). */
    public static boolean flagged(ItemStack s) {
        if (!is(s)) return false;
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        return d != null && d.copyTag().getBoolean(RequipRules.WEAPON_FLAG);
    }

    /** A fresh stack of the item, flagged as requipped. */
    public static ItemStack flagged(Item item) {
        ItemStack s = new ItemStack(item);
        CustomData.update(DataComponents.CUSTOM_DATA, s, t -> t.putBoolean(RequipRules.WEAPON_FLAG, true));
        return s;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, net.minecraft.world.item.TooltipFlag flag) {
        tip.add(Component.translatableWithFallback("nusmp.requip.weapon.lore", "Requipped with its armour; it vanishes when the armour is dismissed.").withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal(lore).withStyle(ChatFormatting.DARK_AQUA));
    }
}
