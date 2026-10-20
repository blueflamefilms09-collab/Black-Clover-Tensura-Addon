package com.newuniverse.nusmp.labyrinth.loot;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Why: a piece of dungeon armour is an ordinary {@code ArmorItem} (so it equips, shows its armour bar, takes damage, mends at an anvil and is drawn by the worn-model library
 * like every other armour of the mod) that also carries a {@link GearRoll}. Its material gives the base armour, toughness and knockback resistance; the roll adds two to four
 * lines (armour first) as vanilla modifiers of its own slot group. Like the weapons it is rolled on the first tick it sits in an inventory if it came from a recipe or a
 * command. Its worn look is the library's (the mod's armour renderer draws any armour of the {@code nusmp} namespace), and the vanilla layer texture is a plain pattern in the set's
 * colours for the places the library does not draw (a mob, an armour stand).
 */
public class LootArmorItem extends ArmorItem implements LootGear {
    private final LootRoster.ArmourDef def;

    public LootArmorItem(Holder<ArmorMaterial> material, Type type, Properties props, LootRoster.ArmourDef def) {
        super(material, type, props);
        this.def = def;
    }

    public LootRoster.ArmourDef def() { return def; }

    /** The armour type of a piece name of the roster ({@code head}, {@code chest}, {@code legs}, {@code feet}). */
    public static Type typeOf(String piece) {
        return switch (piece) {
            case "head" -> Type.HELMET;
            case "chest" -> Type.CHESTPLATE;
            case "legs" -> Type.LEGGINGS;
            default -> Type.BOOTS;
        };
    }

    /** The properties of a piece: one in a stack, durable by the set's material. */
    public static Properties props(Type type, int durabilityFactor) { return new Properties().stacksTo(1).durability(type.getDurability(durabilityFactor)).rarity(Rarity.COMMON); }

    // ---------------------------------------------------------------- LootGear
    @Override public GearKind gearKind() { return GearKind.ARMOUR; }

    @Override public String lootId() { return def.id(); }

    @Override public StatKind primary() { return StatKind.ARMOR; }

    @Override public EquipmentSlotGroup slotGroup() { return EquipmentSlotGroup.bySlot(getEquipmentSlot()); }

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
        GearTooltip.gear(stack, this, tip);
    }
}
