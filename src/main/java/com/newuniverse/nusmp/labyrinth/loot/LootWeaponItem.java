package com.newuniverse.nusmp.labyrinth.loot;

import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Why: the eight dungeon weapons are held items with a damage the OWNER's table decides ({@code relics/GradeDamage}: a normal-grade dagger 20, a special-grade katana 51,
 * a rare-grade hammer 78 ...) plus the lines of their roll, so the class has to be small and honest about what it is: an {@code Item} with durability that carries a
 * {@link GearRoll} and the two base attribute modifiers, and nothing of the relic engine (these are not relics: no ledger, no limit, no tier). The default instance and any stack
 * that has no roll yet (a crafted weapon, a /give) is rolled the first time it sits in an inventory, on the floor its holder stands on, so a weapon is never unrolled in
 * play. Three-dimensional in the hand (the weapon renderer lists it), engraved once with its Tensura engravings ({@code WeaponEngravings}), and swung by Better Combat from
 * its class file. A blow costs one durability, like a sword.
 */
public class LootWeaponItem extends Item implements LootGear {
    private final LootRoster.WeaponDef def;

    public LootWeaponItem(Properties props, LootRoster.WeaponDef def) {
        super(props);
        this.def = def;
    }

    public LootRoster.WeaponDef def() { return def; }

    /** The properties of a weapon of the roster: one in a stack, durable, with the numbers of a common roll until a real one is written. */
    public static Properties props(LootRoster.WeaponDef def) {
        double damage = GearRollMath.baseDamage(LootRarity.COMMON, def.type()) - GearStacks.PLAYER_DAMAGE;
        ItemAttributeModifiers mods = ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, damage, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, def.speed() - GearStacks.PLAYER_SPEED, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
        return new Properties().stacksTo(1).durability(def.durability()).rarity(Rarity.COMMON).attributes(mods);
    }

    // ---------------------------------------------------------------- LootGear
    @Override public GearKind gearKind() { return GearKind.WEAPON; }

    @Override public String lootId() { return def.id(); }

    @Override public StatKind primary() { return StatKind.ATTACK_DAMAGE; }

    @Override public EquipmentSlotGroup slotGroup() { return EquipmentSlotGroup.MAINHAND; }

    @Override public int baseValue() { return def.value(); }

    @Override public String weaponType() { return def.type(); }

    @Override public double attackSpeed() { return def.speed(); }

    // ---------------------------------------------------------------- the item
    @Override
    public ItemStack getDefaultInstance() { return GearStacks.create(this, LootRarity.COMMON, def.id().hashCode(), 1); }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
        super.inventoryTick(stack, level, holder, slot, selected);
        if (!level.isClientSide && !stack.has(LootComponents.GEAR_ROLL.get())) GearStacks.ensureRolled(stack, depthOf(level, holder), level.getGameTime() ^ holder.getId());
    }

    /** The floor of the holder, or 0 outside the Mega-Dungeon. */
    static int depthOf(Level level, Entity holder) { return MegaDungeon.in(level) ? Math.abs(MegaDungeon.floorAt(level, holder.blockPosition())) : 0; }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatableWithFallback("item.nusmp." + def.id() + ".lore", def.lore()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        GearTooltip.gear(stack, this, tip);
    }
}
