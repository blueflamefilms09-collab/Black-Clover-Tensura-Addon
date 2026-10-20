package com.newuniverse.nusmp.labyrinth.loot;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Locale;

/**
 * Why: the eight rings, amulets, charms, a belt and a cloak are worn, not held, and what a worn item can do in vanilla is nothing (only armour and held items have attribute
 * slots); the mod wears its trinkets in Curios slots (the {@code data/curios/tags/item/*.json} lists make them fit a ring, necklace, charm, belt or back slot) and
 * {@link TrinketBridge} applies their rolled lines while they are worn. The item itself is the same carrier as the other loot gear: one in a stack, a roll, a tooltip, the
 * first-tick roll for an unrolled stack, and no durability (a ring does not wear out). Without Curios the off hand stands in for the slot.
 */
public class LootTrinketItem extends Item implements LootGear {
    private final LootRoster.TrinketDef def;

    public LootTrinketItem(Properties props, LootRoster.TrinketDef def) {
        super(props);
        this.def = def;
    }

    public LootRoster.TrinketDef def() { return def; }

    public static Properties props() { return new Properties().stacksTo(1).rarity(Rarity.COMMON); }

    /** The Curios slot id this trinket is worn in. */
    public String curiosSlot() { return def.slot(); }

    // ---------------------------------------------------------------- LootGear
    @Override public GearKind gearKind() { return GearKind.TRINKET; }

    @Override public String lootId() { return def.id(); }

    @Override public StatKind primary() { return StatKind.fromId(def.primary()) == null ? StatKind.MAX_HEALTH : StatKind.fromId(def.primary()); }

    @Override public EquipmentSlotGroup slotGroup() { return EquipmentSlotGroup.ANY; }

    @Override public int baseValue() { return def.value(); }

    // ---------------------------------------------------------------- the item
    @Override
    public ItemStack getDefaultInstance() { return GearStacks.create(this, LootRarity.COMMON, def.id().hashCode(), 1); }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
        super.inventoryTick(stack, level, holder, slot, selected);
        if (!level.isClientSide && !stack.has(LootComponents.GEAR_ROLL.get())) GearStacks.ensureRolled(stack, LootWeaponItem.depthOf(level, holder), level.getGameTime() ^ holder.getId());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatableWithFallback("item.nusmp." + def.id() + ".lore", def.lore()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tip.add(LootText.text("tooltip.nusmp.loot.worn_in", def.slot().substring(0, 1).toUpperCase(Locale.ROOT) + def.slot().substring(1)).withStyle(ChatFormatting.DARK_AQUA));
        GearTooltip.gear(stack, this, tip);
    }
}
