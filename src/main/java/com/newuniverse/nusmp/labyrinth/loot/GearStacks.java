package com.newuniverse.nusmp.labyrinth.loot;

import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Why: a roll is only worth something if the game APPLIES it, in any slot, and in 1.21 that is the vanilla attribute-modifier component of the stack. So when a piece is made,
 * this writes the roll into the {@code nusmp:gear_roll} component AND turns every stat line into a modifier of the stack, so the numbers in the tooltip are the numbers on
 * the wearer. A weapon gets its base damage and speed as the two vanilla "base" modifiers (the damage is the owner's grade table plus the rolled line, never above the top
 * of the grade's range), so the item reads and fights like any weapon; a piece of armour keeps the armour, toughness and knockback resistance of its material (setting the
 * component REPLACES the item's own modifiers, so the factory starts from the item's default list) and adds its lines; a trinket gets no vanilla modifiers at all, because
 * nothing in vanilla applies a modifier for an item in a Curios slot: {@link TrinketBridge} reads the roll and applies it. Every modifier id carries the slot group
 * ({@code nusmp:loot/<slot>/<stat>}) so four armour pieces worn together each keep their own line. The critical chance has no vanilla attribute, so it is read from the roll
 * by {@link CritHandler}. Server side; no client code.
 */
public final class GearStacks {
    private GearStacks() {}

    /** The attack damage and attack speed every player has before any item: the weapon's modifiers are written relative to them. */
    static final double PLAYER_DAMAGE = 1.0, PLAYER_SPEED = 4.0;

    // ---------------------------------------------------------------- reading
    public static GearRoll rollOf(ItemStack stack) { return stack.isEmpty() ? null : stack.get(LootComponents.GEAR_ROLL.get()); }

    public static LootGear gearOf(ItemStack stack) { return !stack.isEmpty() && stack.getItem() instanceof LootGear g ? g : null; }

    // ---------------------------------------------------------------- making
    /** A new stack of the item carrying this roll. */
    public static ItemStack create(Item item, GearRoll roll) {
        ItemStack stack = new ItemStack(item);
        apply(stack, roll);
        return stack;
    }

    /** Rolls a piece from a seed, a floor and a rarity and builds the stack. */
    public static ItemStack create(Item item, LootRarity rarity, long seed, int depth) {
        if (!(item instanceof LootGear g)) return new ItemStack(item);
        return create(item, roll(g, rarity, seed, depth));
    }

    /** The roll of a piece (pure maths; the item says what it is). */
    public static GearRoll roll(LootGear g, LootRarity rarity, long seed, int depth) {
        double headroom = g.gearKind() == GearKind.WEAPON ? GearRollMath.headroom(rarity, g.weaponType()) : 0.0;
        StatKind primary = g.gearKind() == GearKind.WEAPON ? GearRollMath.weaponPrimary(headroom) : g.primary();
        return GearRollMath.roll(seed, depth, rarity, g.gearKind(), primary, headroom);
    }

    /** Puts the roll and its modifiers on an existing stack (its enchantments, damage and name are kept). */
    public static void apply(ItemStack stack, GearRoll roll) {
        LootGear g = gearOf(stack);
        stack.set(LootComponents.GEAR_ROLL.get(), roll);
        stack.set(DataComponents.RARITY, vanilla(roll.rarity()));
        if (g != null) stack.set(DataComponents.ATTRIBUTE_MODIFIERS, modifiers(stack, g, roll));
    }

    static Rarity vanilla(LootRarity r) {
        return switch (r) {
            case COMMON -> Rarity.COMMON;
            case UNCOMMON -> Rarity.UNCOMMON;
            case RARE -> Rarity.RARE;
            case EPIC, LEGENDARY -> Rarity.EPIC;
        };
    }

    /** Gives an unrolled stack of loot gear a first roll (a crafted piece, a creative-menu piece, a /give); true when it did. */
    public static boolean ensureRolled(ItemStack stack, int depth, long salt) {
        LootGear g = gearOf(stack);
        if (g == null || stack.has(LootComponents.GEAR_ROLL.get())) return false;
        SplittableRandom r = new SplittableRandom(salt ^ g.lootId().hashCode() * 0x9E3779B97F4A7C15L);
        LootRarity rarity = LootRarity.pick(r.nextDouble(), 0, 0.0);
        apply(stack, roll(g, rarity, r.nextLong(), depth));
        return true;
    }

    // ---------------------------------------------------------------- the modifiers
    static ItemAttributeModifiers modifiers(ItemStack stack, LootGear g, GearRoll roll) {
        List<ItemAttributeModifiers.Entry> out = new ArrayList<>();
        EquipmentSlotGroup group = g.slotGroup();
        switch (g.gearKind()) {
            case WEAPON -> {
                double damage = GearRollMath.totalDamage(roll.rarity(), g.weaponType(), roll) - PLAYER_DAMAGE;
                out.add(new ItemAttributeModifiers.Entry(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, damage, AttributeModifier.Operation.ADD_VALUE), group));
                double speed = g.attackSpeed() - PLAYER_SPEED + roll.sum(StatKind.ATTACK_SPEED);
                out.add(new ItemAttributeModifiers.Entry(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, speed, AttributeModifier.Operation.ADD_VALUE), group));
            }
            case ARMOUR -> out.addAll(defaults(stack));
            case TRINKET -> {
                return new ItemAttributeModifiers(List.of(), false);
            }
        }
        for (GearRoll.Line l : roll.lines()) {
            if (l.kind() == StatKind.CRIT_CHANCE || (g.gearKind() == GearKind.WEAPON && (l.kind() == StatKind.ATTACK_DAMAGE || l.kind() == StatKind.ATTACK_SPEED))) continue;
            Holder<Attribute> a = attribute(l.kind());
            if (a == null) continue;
            out.add(new ItemAttributeModifiers.Entry(a, new AttributeModifier(modifierId(group, l.kind()), l.value(), operation(l.kind())), group));
        }
        return new ItemAttributeModifiers(out, false);
    }

    /** The item's OWN modifiers (armour keeps its numbers in the item class, not in the component); never the stack's current list, so a second roll does not stack on the first. */
    private static List<ItemAttributeModifiers.Entry> defaults(ItemStack stack) {
        return new ArrayList<>(stack.getItem().getDefaultAttributeModifiers().modifiers());
    }

    /** The vanilla or Tensura attribute a stat becomes, or null for the critical chance. */
    public static Holder<Attribute> attribute(StatKind k) {
        return switch (k) {
            case ATTACK_DAMAGE -> Attributes.ATTACK_DAMAGE;
            case ATTACK_SPEED -> Attributes.ATTACK_SPEED;
            case ARMOR -> Attributes.ARMOR;
            case MAX_HEALTH -> Attributes.MAX_HEALTH;
            case MOVE_SPEED -> Attributes.MOVEMENT_SPEED;
            case MANA -> TensuraAttributes.MAX_MAGICULE;
            case MANA_REGEN -> TensuraAttributes.MAGICULE_REGENERATION_MULTIPLIER;
            case CRIT_CHANCE -> null;
        };
    }

    public static AttributeModifier.Operation operation(StatKind k) {
        return switch (k.op()) {
            case ADD, CHANCE -> AttributeModifier.Operation.ADD_VALUE;
            case MULTIPLY_BASE -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            case MULTIPLY_TOTAL -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
        };
    }

    static ResourceLocation modifierId(EquipmentSlotGroup group, StatKind k) {
        return ResourceLocation.fromNamespaceAndPath("nusmp", "loot/" + group.getSerializedName() + "/" + k.id());
    }
}
