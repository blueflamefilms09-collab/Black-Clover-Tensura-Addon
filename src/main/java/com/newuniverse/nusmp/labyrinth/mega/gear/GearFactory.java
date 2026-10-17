package com.newuniverse.nusmp.labyrinth.mega.gear;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Optional;

/**
 * Why: the roller decides WHAT a drop is and this makes it a real {@code ItemStack}: the vanilla base item, stamped with the {@code nusmp:gear_grade}
 * component (which is what makes it standard gear and nothing else: no relic component is ever added, so the Order's ledger never sees it), a durability
 * bonus, the percentage bonus as an attribute modifier, the vanilla enchantments and, when the config allows, the Tensura engravings, then the look.
 * The percentage bonus is the delicate part. In 1.21 an item's attribute modifiers are a data component, and SETTING that component REPLACES the item's
 * own modifiers (a sword would lose its base damage, and a chestplate its armour, because armour keeps its numbers in the item class and not in the
 * component): so the factory first takes the stack's current modifiers (the component, or when that is empty the item's own default list, which is
 * exactly the fallback the game itself uses) and ADDS one modifier to them with {@code withModifierAdded}. The bonus is a share of the item's own value
 * (attack damage including the player's base 1, or the armour points), added as a flat amount, so it does not depend on what else the wearer has; its id is
 * fixed per slot ({@code nusmp:mega_gear/<slot>}) because in 1.21 two equipment pieces applying the same id to the same attribute would replace each other
 * and a set of four armour pieces would keep only one bonus. Every id (item, enchantment, engraving) is resolved here at the last moment and an absent one
 * is skipped: an optional mod or an older Tensura costs one enchantment, never the drop. Server side; no client code.
 */
public final class GearFactory {
    private GearFactory() {}

    /** The attack damage the player has before any item: the base the displayed damage of a sword adds to, so "+15%" is 15 percent of what the tooltip shows. */
    static final double PLAYER_BASE_DAMAGE = 1.0;

    /**
     * The finished stack, or {@link ItemStack#EMPTY} when the base item does not exist (the caller then drops nothing).
     *
     * @param registries the registry access of the level (enchantments are data-driven in 1.21)
     */
    public static ItemStack create(GearSpec spec, HolderLookup.Provider registries) {
        ResourceLocation id = ResourceLocation.tryParse(spec.item());
        Item item = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (item == null || item == Items.AIR) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(item);
        stack.set(GearComponents.GEAR.get(), spec.info());

        int base = stack.getMaxDamage();
        if (spec.durabilityPercent() > 0 && base > 0) {
            int bonus = (int) Math.round(base * (100 + spec.durabilityPercent()) / 100.0);
            stack.set(DataComponents.MAX_DAMAGE, Math.max(base + 1, bonus));
        }
        if (spec.powerPercent() > 0 && spec.slot().hasPowerStat()) addPower(stack, spec);

        HolderLookup.RegistryLookup<Enchantment> lookup = registries.lookupOrThrow(Registries.ENCHANTMENT);
        for (GearSpec.Ench e : spec.enchants()) enchant(stack, lookup, e);
        if (GearConfig.get(GearConfig.ENGRAVINGS)) for (GearSpec.Ench e : spec.engravings()) enchant(stack, lookup, e);

        GearLook.apply(stack, spec);
        return stack;
    }

    /** Puts one enchantment on the stack if it exists, at a level no higher than its own maximum and never lowering one already there. */
    static void enchant(ItemStack stack, HolderLookup.RegistryLookup<Enchantment> lookup, GearSpec.Ench e) {
        ResourceLocation rl = ResourceLocation.tryParse(e.id());
        if (rl == null) return;
        Optional<? extends Holder<Enchantment>> h = lookup.get(ResourceKey.create(Registries.ENCHANTMENT, rl));
        if (h.isEmpty()) return;
        int level = Math.max(1, Math.min(e.level(), h.get().value().getMaxLevel()));
        if (stack.getEnchantments().getLevel(h.get()) < level) stack.enchant(h.get(), level);
    }

    // ---------------------------------------------------------------- the percentage bonus
    private static void addPower(ItemStack stack, GearSpec spec) {
        ItemAttributeModifiers current = baseModifiers(stack);
        Holder<Attribute> attribute;
        EquipmentSlotGroup group;
        double own;
        switch (spec.slot()) {
            case SWORD, AXE -> {
                attribute = Attributes.ATTACK_DAMAGE;
                group = EquipmentSlotGroup.MAINHAND;
                own = PLAYER_BASE_DAMAGE + flat(current, attribute, group);
            }
            case HELMET -> { attribute = Attributes.ARMOR; group = EquipmentSlotGroup.HEAD; own = flat(current, attribute, group); }
            case CHESTPLATE -> { attribute = Attributes.ARMOR; group = EquipmentSlotGroup.CHEST; own = flat(current, attribute, group); }
            case LEGGINGS -> { attribute = Attributes.ARMOR; group = EquipmentSlotGroup.LEGS; own = flat(current, attribute, group); }
            case BOOTS -> { attribute = Attributes.ARMOR; group = EquipmentSlotGroup.FEET; own = flat(current, attribute, group); }
            default -> { return; }
        }
        double amount = Math.round(own * spec.powerPercent()) / 100.0;
        if (amount <= 0.0) return;
        AttributeModifier modifier = new AttributeModifier(modifierId(spec.slot()), amount, AttributeModifier.Operation.ADD_VALUE);
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, current.withModifierAdded(attribute, modifier, group));
    }

    /**
     * The modifiers the stack has now: its component when that lists any, otherwise the item's own default list (what armour uses). Setting the component
     * to a list that lacks these would silently strip the item of its base damage or armour.
     */
    static ItemAttributeModifiers baseModifiers(ItemStack stack) {
        ItemAttributeModifiers current = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        return current.modifiers().isEmpty() ? stack.getItem().getDefaultAttributeModifiers() : current;
    }

    /** The sum of the flat (add-value) modifiers a list gives one attribute in one equipment slot group. */
    static double flat(ItemAttributeModifiers list, Holder<Attribute> attribute, EquipmentSlotGroup group) {
        double sum = 0.0;
        for (ItemAttributeModifiers.Entry e : list.modifiers()) {
            if (e.attribute().equals(attribute) && e.slot() == group && e.modifier().operation() == AttributeModifier.Operation.ADD_VALUE) sum += e.modifier().amount();
        }
        return sum;
    }

    /** The fixed id of the bonus of a slot: unique per slot so four armour pieces worn together each keep their own. */
    static ResourceLocation modifierId(GearSlot slot) {
        return ResourceLocation.fromNamespaceAndPath("nusmp", "mega_gear/" + slot.id());
    }
}
